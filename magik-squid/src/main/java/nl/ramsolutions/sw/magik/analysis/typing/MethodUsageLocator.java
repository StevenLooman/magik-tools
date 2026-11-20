package nl.ramsolutions.sw.magik.analysis.typing;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.CheckForNull;
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
import java.util.function.Function;
import java.util.stream.Stream;
import nl.ramsolutions.sw.FileCharsetDeterminer;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.analysis.definitions.GlobalDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ITypeStringDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodUsage;
import nl.ramsolutions.sw.magik.analysis.definitions.ProcedureDefinition;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasonerState;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Method usage locator.
 *
 * <p>Uses typing to determine if any {@link MethodUsage} is is for this specific type.
 */
public class MethodUsageLocator {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodUsageLocator.class);

  private final IDefinitionKeeper definitionKeeper;
  private final Set<URI> unreadableUris = new HashSet<>();

  public MethodUsageLocator(final IDefinitionKeeper definitionKeeper) {
    this.definitionKeeper = definitionKeeper;
  }

  /** What {@link #getMethodUsages(MethodUsage, Set)} answers besides the calls in methods. */
  public enum Option {
    /**
     * Usages a definition declares with an {@code @invokes_method} annotation. Such a usage is no
     * call site: it carries no receiver node and no arguments.
     */
    DECLARED_USAGES,
    /** Usages of procedure definitions. */
    PROCEDURE_USAGES,
    /** Usages of global definitions: the calls in the initialiser of a global. */
    GLOBAL_USAGES,
  }

  /** A usage to test, with the location of the definition it was read from. */
  private record Candidate(MethodUsage usage, Location definitionLocation) {

    boolean isDeclared() {
      return MethodUsageLocator.isDeclaredUsage(this.usage, this.definitionLocation);
    }

    boolean hasMethodName(final String methodName) {
      final String usageMethodName = this.usage.getMethodName();
      return Objects.equals(usageMethodName, methodName);
    }
  }

  /**
   * Test if a definition declares a usage with an {@code @invokes_method} annotation. A declared
   * usage sits at its definition, a call site at its invocation.
   *
   * @param usage Usage, as its definition holds it.
   * @param definitionLocation Location of the definition holding the usage.
   * @return True if the usage is a declared one.
   */
  public static boolean isDeclaredUsage(
      final MethodUsage usage, final Location definitionLocation) {
    final Location usageLocation = usage.getLocation();
    return usageLocation != null && usageLocation.equals(definitionLocation);
  }

  /** What tells one answered usage from another: a call site twice read is one usage. */
  private record AnsweredKey(Location location, TypeString typeStr, boolean declared) {}

  private Stream<Candidate> candidateUsages(final Set<Option> options) {
    final Collection<MethodDefinition> methodDefs = this.definitionKeeper.getMethodDefinitions();
    final Stream<Candidate> methodCandidates =
        methodDefs.stream().flatMap(MethodUsageLocator::createMethodCandidates);
    final Stream<Candidate> procedureCandidates = this.procedureCandidates(options);
    final Stream<Candidate> globalCandidates = this.globalCandidates(options);
    return Stream.of(methodCandidates, procedureCandidates, globalCandidates)
        .flatMap(Function.identity());
  }

  private Stream<Candidate> procedureCandidates(final Set<Option> options) {
    if (!options.contains(Option.PROCEDURE_USAGES)) {
      return Stream.empty();
    }

    final Collection<ProcedureDefinition> procedureDefs =
        this.definitionKeeper.getProcedureDefinitions();
    return procedureDefs.stream().flatMap(MethodUsageLocator::createProcedureCandidates);
  }

  private Stream<Candidate> globalCandidates(final Set<Option> options) {
    if (!options.contains(Option.GLOBAL_USAGES)) {
      return Stream.empty();
    }

    final Collection<GlobalDefinition> globalDefs = this.definitionKeeper.getGlobalDefinitions();
    return globalDefs.stream().flatMap(MethodUsageLocator::createGlobalCandidates);
  }

  private static Stream<Candidate> createMethodCandidates(final MethodDefinition methodDef) {
    final Collection<MethodUsage> usages = methodDef.getUsedMethods();
    final Location definitionLocation = methodDef.getLocation();
    return MethodUsageLocator.createCandidates(usages, definitionLocation);
  }

  private static Stream<Candidate> createProcedureCandidates(
      final ProcedureDefinition procedureDef) {
    final Collection<MethodUsage> usages = procedureDef.getUsedMethods();
    final Location definitionLocation = procedureDef.getLocation();
    return MethodUsageLocator.createCandidates(usages, definitionLocation);
  }

  private static Stream<Candidate> createGlobalCandidates(final GlobalDefinition globalDef) {
    final Collection<MethodUsage> usages = globalDef.getUsedMethods();
    final Location definitionLocation = globalDef.getLocation();
    return MethodUsageLocator.createCandidates(usages, definitionLocation);
  }

  private static Stream<Candidate> createCandidates(
      final Collection<MethodUsage> usages, final Location definitionLocation) {
    return usages.stream().map(usage -> new Candidate(usage, definitionLocation));
  }

  /**
   * Get the method usages for the wanted method usage: the calls in method bodies whose receiver
   * may reach the wanted method, each a usage of the wanted type on its {@code METHOD_INVOCATION}
   * node.
   *
   * @param wantedMethodUsage The wanted {@link MethodUsage}.
   * @return The {@link MethodUsage}s for the wanted method usage.
   */
  public List<Map.Entry<MethodUsage, MagikTypedFile>> getMethodUsages(
      final MethodUsage wantedMethodUsage) {
    final Set<Option> options = Set.of();
    return this.getMethodUsages(wantedMethodUsage, options);
  }

  /**
   * Get the method usages for the wanted method usage, as {@link #getMethodUsages(MethodUsage)},
   * and those the options ask for.
   *
   * <p>A declared usage is answered on the node of its definition, a {@code METHOD_DEFINITION} or
   * {@code PROCEDURE_DEFINITION}, and keeps the type its annotation names. With any option set, a
   * usage two definitions hold is answered once.
   *
   * @param wantedMethodUsage The wanted {@link MethodUsage}.
   * @param options What to answer besides the calls in methods.
   * @return The {@link MethodUsage}s for the wanted method usage.
   */
  public List<Map.Entry<MethodUsage, MagikTypedFile>> getMethodUsages(
      final MethodUsage wantedMethodUsage, final Set<Option> options) {
    final String methodName = wantedMethodUsage.getMethodName();
    final boolean answerDeclared = options.contains(Option.DECLARED_USAGES);

    final Stream<Map.Entry<MethodUsage, MagikTypedFile>> answeredUsages =
        this.candidateUsages(options)
            .filter(candidate -> candidate.hasMethodName(methodName))
            .filter(candidate -> answerDeclared || !candidate.isDeclared())
            .map(candidate -> this.locateUsage(candidate, wantedMethodUsage))
            .filter(Objects::nonNull);
    if (options.isEmpty()) {
      return answeredUsages.toList();
    }

    // A call in a procedure nested in a method is a usage of both definitions.
    final Set<AnsweredKey> answeredKeys = new HashSet<>();
    return answeredUsages
        .filter(
            entry -> {
              final MethodUsage usage = entry.getKey();
              final AnsweredKey answeredKey = MethodUsageLocator.createAnsweredKey(usage);
              return answeredKeys.add(answeredKey);
            })
        .toList();
  }

  private static AnsweredKey createAnsweredKey(final MethodUsage usage) {
    final Location location = usage.getLocation();
    final TypeString typeStr = usage.getTypeName();
    final boolean declared = !MethodUsageLocator.isCallSite(usage);
    return new AnsweredKey(location, typeStr, declared);
  }

  /**
   * Test if an answered usage is a call site, on its {@code METHOD_INVOCATION} node, and not a
   * declared usage, on the node of its definition.
   *
   * @param usage Usage answered by {@link #getMethodUsages(MethodUsage, Set)}.
   * @return True if the usage is a call site.
   */
  public static boolean isCallSite(final MethodUsage usage) {
    final AstNode node = usage.getNode();
    return node != null && node.is(MagikGrammar.METHOD_INVOCATION);
  }

  @CheckForNull
  private Map.Entry<MethodUsage, MagikTypedFile> locateUsage(
      final Candidate candidate, final MethodUsage wantedMethodUsage) {
    final MethodUsage usage = candidate.usage();
    return candidate.isDeclared()
        ? this.locateDeclaredUsage(usage, wantedMethodUsage)
        : this.locateCallSite(usage, wantedMethodUsage);
  }

  /**
   * Locate a call site: reason its receiver, and test if a call on it may reach the wanted method.
   *
   * @return The usage on its invocation node, or {@code null} when it does not reach the wanted
   *     method.
   */
  @CheckForNull
  private Map.Entry<MethodUsage, MagikTypedFile> locateCallSite(
      final MethodUsage usage, final MethodUsage wantedMethodUsage) {
    final String methodName = wantedMethodUsage.getMethodName();
    final Location location = usage.getLocation();
    final MagikTypedFile magikFile = this.getMagikFile(location);
    if (magikFile == null) {
      return null;
    }

    final AstNode invocationNode = MethodUsageLocator.findInvocationNode(magikFile, location);
    if (invocationNode == null) {
      return null;
    }

    // Determine/reason the type the method is called on.
    final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();
    final MethodInvocationNodeHelper helper = new MethodInvocationNodeHelper(invocationNode);
    final AstNode receiverNode = helper.getReceiverNode();
    final ExpressionResultString result = reasonerState.getNodeType(receiverNode);
    final TypeString resultTypeStr = result.get(0, TypeString.UNDEFINED);
    final TypeString typeStr = SelfHelper.substituteSelf(resultTypeStr, invocationNode);
    if (typeStr.isUndefined()) {
      return null;
    }

    final TypeStringResolver resolver = magikFile.getTypeStringResolver();
    final TypeString wantedMethodUsageTypeStr = wantedMethodUsage.getTypeName();
    final boolean isSuperReceiver = receiverNode.getFirstChild(MagikGrammar.SUPER) != null;
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
  }

  /**
   * Locate a declared usage: test if a call on the type its annotation names may reach the wanted
   * method. An annotation names a type, never {@code _self} or {@code _super}.
   *
   * @return The usage on the node of its definition, or {@code null} when it does not reach the
   *     wanted method.
   */
  @CheckForNull
  private Map.Entry<MethodUsage, MagikTypedFile> locateDeclaredUsage(
      final MethodUsage usage, final MethodUsage wantedMethodUsage) {
    final TypeString declaredTypeStr = usage.getTypeName();
    if (declaredTypeStr.isUndefined()) {
      return null;
    }

    final Location location = usage.getLocation();
    final MagikTypedFile magikFile = this.getMagikFile(location);
    if (magikFile == null) {
      return null;
    }

    final AstNode definitionNode = MethodUsageLocator.findDefinitionNode(magikFile, location);
    if (definitionNode == null) {
      return null;
    }

    final String methodName = wantedMethodUsage.getMethodName();
    final TypeStringResolver resolver = magikFile.getTypeStringResolver();
    final TypeString wantedTypeStr = wantedMethodUsage.getTypeName();
    final boolean reachesWanted =
        MethodUsageLocator.isReachedFrom(resolver, declaredTypeStr, wantedTypeStr, methodName);
    if (!reachesWanted) {
      return null;
    }

    final MethodUsage methodUsageWithNode =
        new MethodUsage(declaredTypeStr, methodName, location, definitionNode);
    return Map.entry(methodUsageWithNode, magikFile);
  }

  /**
   * Find the definition a declared usage sits at: the innermost method or procedure definition at
   * the start of the usage.
   *
   * @param magikFile File of the usage.
   * @param location Location of the usage, which is the location of its definition.
   * @return The definition node, or {@code null} when the file no longer has one there.
   */
  @CheckForNull
  private static AstNode findDefinitionNode(
      final MagikTypedFile magikFile, final Location location) {
    final AstNode topNode = magikFile.getTopNode();
    final Range range = location.getRange();
    final Position definitionPosition = range.getStartPosition();
    final AstNode definitionTokenNode = AstQuery.nodeAt(topNode, definitionPosition);
    if (definitionTokenNode == null) {
      return null;
    }

    return definitionTokenNode.getFirstAncestor(
        MagikGrammar.METHOD_DEFINITION, MagikGrammar.PROCEDURE_DEFINITION);
  }

  /**
   * Find the invocation a usage sits on. A usage declared by an {@code @invokes_method} annotation
   * sits at its definition, which is not a call site.
   *
   * @param magikFile File of the usage.
   * @param location Location of the usage.
   * @return The invocation node, or {@code null} when the usage is not on an invocation.
   */
  @CheckForNull
  private static AstNode findInvocationNode(
      final MagikTypedFile magikFile, final Location location) {
    final AstNode topNode = magikFile.getTopNode();
    final Range range = location.getRange();
    final Position invocationPosition = range.getStartPosition();
    final AstNode invocationTokenNode = AstQuery.nodeAt(topNode, invocationPosition);
    if (invocationTokenNode == null) {
      return null;
    }

    return invocationTokenNode.getFirstAncestor(MagikGrammar.METHOD_INVOCATION);
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
        resolver.getRespondingMethodDefinitionsAsParent(superTypeStr, methodName);
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

  /**
   * Read the file of a usage.
   *
   * @param location Location of the usage.
   * @return The file, or {@code null} when it cannot be read.
   */
  @CheckForNull
  private MagikTypedFile getMagikFile(final Location location) {
    final URI calledMethodUri = location.getUri();
    if (this.unreadableUris.contains(calledMethodUri)) {
      return null;
    }

    final Path calledMethodPath = Path.of(calledMethodUri);
    final Charset charset = FileCharsetDeterminer.determineCharset(calledMethodPath);
    final String text;
    try {
      text = Files.readString(calledMethodPath, charset);
    } catch (final IOException exception) {
      this.unreadableUris.add(calledMethodUri);
      final String reason = exception.toString();
      LOGGER.warn("Cannot read {}: {}", calledMethodUri, reason);
      return null;
    }
    return new MagikTypedFile(calledMethodUri, text, this.definitionKeeper);
  }
}
