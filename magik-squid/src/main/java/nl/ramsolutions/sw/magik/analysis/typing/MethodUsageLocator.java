package nl.ramsolutions.sw.magik.analysis.typing;

import com.sonar.sslr.api.AstNode;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import nl.ramsolutions.sw.FileCharsetDeterminer;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
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
                      : resolver.isKindOf(wantedMethodUsageTypeStr, typeStr);
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
    final TypeString resolvedWantedTypeStr = resolver.getResolvedTypeString(wantedTypeStr);
    final TypeString bareWantedTypeStr = resolvedWantedTypeStr.getWithoutGenerics();
    final Collection<MethodDefinition> respondingDefinitions =
        resolver.getRespondingMethodDefinitions(superTypeStr, methodName);
    return respondingDefinitions.stream()
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
