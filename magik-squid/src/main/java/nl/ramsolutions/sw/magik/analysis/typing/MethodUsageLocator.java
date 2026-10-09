package nl.ramsolutions.sw.magik.analysis.typing;

import com.sonar.sslr.api.AstNode;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import nl.ramsolutions.sw.FileCharsetDeterminer;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.analysis.definitions.GlobalDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ITypeStringDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodUsage;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasonerState;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/**
 * Method usage locator.
 *
 * <p>Uses typing to determine if any {@link MethodUsage} is is for this specific type.
 */
public class MethodUsageLocator {

  private final IDefinitionKeeper definitionKeeper;

  public MethodUsageLocator(final IDefinitionKeeper definitionKeeper) {
    this.definitionKeeper = definitionKeeper;
  }

  public List<Map.Entry<MethodUsage, MagikTypedFile>> getMethodUsages(
      final MethodUsage wantedMethodUsage) {
    final String methodName = wantedMethodUsage.getMethodName();

    return this.definitionKeeper.getMethodDefinitions().stream()
        .flatMap(methodDef -> methodDef.getUsedMethods().stream())
        .filter(usage -> usage.getMethodName().equals(methodName))
        .map(
            usage -> {
              final Location location = usage.getLocation();
              final MagikTypedFile magikFile = this.getMagikFile(location);

              // Determine/reason the type the method is called on.
              final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();
              final AstNode node = magikFile.getTopNode();
              final Position invocationPosition = location.getRange().getStartPosition();
              final AstNode invocationTokenNode = AstQuery.nodeAt(node, invocationPosition);
              final AstNode invocationNode =
                  invocationTokenNode.getFirstAncestor(MagikGrammar.METHOD_INVOCATION);
              final MethodInvocationNodeHelper helper =
                  new MethodInvocationNodeHelper(invocationNode);
              final AstNode receiverNode = helper.getReceiverNode();
              final ExpressionResultString result = reasonerState.getNodeType(receiverNode);
              final TypeString resultTypeStr = result.get(0, TypeString.UNDEFINED);
              final TypeString typeStr = SelfHelper.substituteSelf(resultTypeStr, invocationNode);
              if (typeStr.isUndefined()) {
                return null;
              }

              final TypeStringResolver resolver = magikFile.getTypeStringResolver();
              final TypeString wantedMethodUsageTypeStr = wantedMethodUsage.getTypeName();
              final boolean isSuperReceiver =
                  receiverNode.getFirstChild(MagikGrammar.SUPER) != null;
              final boolean reachesWanted =
                  isSuperReceiver
                      ? MethodUsageLocator.isSuperDispatchTo(
                          resolver, typeStr, wantedMethodUsageTypeStr, methodName)
                      : MethodUsageLocator.isReachedFrom(
                          resolver, typeStr, wantedMethodUsageTypeStr, methodName);
              if (!reachesWanted) {
                return null;
              }

              final MethodUsage methodUsageWithNode =
                  new MethodUsage(wantedMethodUsageTypeStr, methodName, location, invocationNode);
              return Map.entry(methodUsageWithNode, magikFile);
            })
        .filter(Objects::nonNull)
        .toList();
  }

  /**
   * Test if a call on some member of a receiver may reach the wanted method.
   *
   * @param resolver Resolver of the call site's file.
   * @param receiverTypeStr Type of the receiver.
   * @param wantedTypeStr Type owning the wanted method.
   * @param methodName Name of the wanted method.
   * @return True if a member may reach the wanted method.
   */
  private static boolean isReachedFrom(
      final TypeStringResolver resolver,
      final TypeString receiverTypeStr,
      final TypeString wantedTypeStr,
      final String methodName) {
    return receiverTypeStr.getCombinedTypes().stream()
        .map(TypeString::getWithoutGenerics)
        .flatMap(
            memberTypeStr ->
                MethodUsageLocator.resolveGlobalAliases(resolver, memberTypeStr).stream())
        .anyMatch(
            typeStr ->
                MethodUsageLocator.isReachedFromType(resolver, typeStr, wantedTypeStr, methodName));
  }

  /**
   * Test if a call on a type may reach the wanted method: the wanted owner is a kind of it, so the
   * call may dispatch down; or it responds with the wanted definition, as a subtype inheriting it
   * does. A subtype that overrides it does not.
   */
  private static boolean isReachedFromType(
      final TypeStringResolver resolver,
      final TypeString typeStr,
      final TypeString wantedTypeStr,
      final String methodName) {
    if (resolver.isKindOf(wantedTypeStr, typeStr)) {
      return true;
    }

    final Collection<MethodDefinition> respondingDefinitions =
        resolver.getRespondingMethodDefinitions(typeStr, methodName);
    return MethodUsageLocator.isWantedAmong(resolver, respondingDefinitions, wantedTypeStr);
  }

  /**
   * The types a value of {@code typeStr} has: a global's aliased types, following a chain of
   * aliases, and any other type as it is. An alias cycle leaves {@code typeStr} as it is.
   */
  private static Set<TypeString> resolveGlobalAliases(
      final TypeStringResolver resolver, final TypeString typeStr) {
    final Set<TypeString> resolvedTypeStrs = new LinkedHashSet<>();
    final Set<TypeString> visited = new HashSet<>();
    final Deque<TypeString> pending = new ArrayDeque<>();
    pending.push(typeStr);
    while (!pending.isEmpty()) {
      final TypeString currentTypeStr = pending.pop();
      if (!visited.add(currentTypeStr)) {
        continue;
      }

      final Collection<ITypeStringDefinition> definitions = resolver.resolve(currentTypeStr);
      if (definitions.isEmpty()) {
        resolvedTypeStrs.add(currentTypeStr);
      }

      for (final ITypeStringDefinition definition : definitions) {
        if (definition instanceof final GlobalDefinition globalDefinition) {
          final TypeString aliasedTypeStr = globalDefinition.getAliasedTypeName();
          final List<TypeString> aliasedMembers = aliasedTypeStr.getCombinedTypes();
          aliasedMembers.forEach(pending::push);
        } else {
          resolvedTypeStrs.add(currentTypeStr);
        }
      }
    }

    return resolvedTypeStrs.isEmpty() ? Set.of(typeStr) : resolvedTypeStrs;
  }

  /**
   * Test if a {@code _super} call dispatches to the wanted method. {@code _super} names a parent's
   * implementation, so the call reaches the wanted method only when that is the one the parent
   * responds with: never the enclosing method itself, a sibling's, or one a nearer parent shadows.
   *
   * @param resolver Resolver of the call site's file.
   * @param superTypeStr Type of the {@code _super} receiver.
   * @param wantedTypeStr Type owning the wanted method.
   * @param methodName Name of the wanted method.
   * @return True if the call dispatches to the wanted method.
   */
  private static boolean isSuperDispatchTo(
      final TypeStringResolver resolver,
      final TypeString superTypeStr,
      final TypeString wantedTypeStr,
      final String methodName) {
    final Collection<MethodDefinition> respondingDefinitions =
        resolver.getRespondingMethodDefinitions(superTypeStr, methodName);
    return MethodUsageLocator.isWantedAmong(resolver, respondingDefinitions, wantedTypeStr);
  }

  /** Test if one of the definitions is owned by the wanted type, generics aside. */
  private static boolean isWantedAmong(
      final TypeStringResolver resolver,
      final Collection<MethodDefinition> definitions,
      final TypeString wantedTypeStr) {
    final TypeString resolvedWantedTypeStr = resolver.getResolvedTypeString(wantedTypeStr);
    final TypeString bareWantedTypeStr = resolvedWantedTypeStr.getWithoutGenerics();
    return definitions.stream()
        .map(MethodDefinition::getTypeName)
        .map(resolver::getResolvedTypeString)
        .map(TypeString::getWithoutGenerics)
        .anyMatch(bareWantedTypeStr::equals);
  }

  private MagikTypedFile getMagikFile(final Location location) {
    final URI calledMethodUri = location.getUri();
    final Path calledMethodPath = Path.of(calledMethodUri);
    final Charset charset = FileCharsetDeterminer.determineCharset(calledMethodPath);
    final String text;
    try {
      text = Files.readString(calledMethodPath, charset);
    } catch (final IOException exception) {
      throw new IllegalStateException(exception);
    }
    return new MagikTypedFile(calledMethodUri, text, this.definitionKeeper);
  }
}
