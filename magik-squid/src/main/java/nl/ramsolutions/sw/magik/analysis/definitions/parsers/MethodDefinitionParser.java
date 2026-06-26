package nl.ramsolutions.sw.magik.analysis.definitions.parsers;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.MagikToolsProperties;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.analysis.MagikAnalysisSettings;
import nl.ramsolutions.sw.magik.analysis.definitions.ConditionUsage;
import nl.ramsolutions.sw.magik.analysis.definitions.GlobalUsage;
import nl.ramsolutions.sw.magik.analysis.definitions.MagikDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodUsage;
import nl.ramsolutions.sw.magik.analysis.definitions.ParameterDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.Pragma;
import nl.ramsolutions.sw.magik.analysis.definitions.SlotUsage;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.PackageNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.ParameterNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.PragmaNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.parser.MagikCommentExtractor;
import nl.ramsolutions.sw.magik.parser.TypeDocParser;
import nl.ramsolutions.sw.moduledef.ModuleDefFile;

/** Method definition parser, creating a {@link MethodDefinition}. */
public class MethodDefinitionParser {

  private final MagikFile magikFile;
  private final AstNode node;

  /**
   * Constructor.
   *
   * @param node Method definition node.
   */
  public MethodDefinitionParser(final MagikFile magikFile, final AstNode node) {
    if (node.isNot(MagikGrammar.METHOD_DEFINITION)) {
      throw new IllegalArgumentException();
    }

    this.magikFile = magikFile;
    this.node = node;
  }

  /**
   * Parse definitions.
   *
   * @return List of parsed definitions.
   */
  public List<MagikDefinition> parseDefinitions() {
    // Don't burn ourselves on syntax errors.
    final AstNode syntaxErrorNode = this.node.getFirstChild(MagikGrammar.SYNTAX_ERROR);
    if (syntaxErrorNode != null) {
      return Collections.emptyList();
    }

    // Figure location.
    final URI uri = this.node.getToken().getURI();
    final MethodDefinitionNodeHelper helper = new MethodDefinitionNodeHelper(this.node);
    final AstNode methodNameNode = helper.getMethodNameNode();
    final Location location = new Location(uri, methodNameNode);

    // Figure timestamp.
    final Instant timestamp = this.magikFile.getTimestamp();

    // Figure module name.
    final ModuleDefFile moduleDefFile = this.magikFile.getModuleDefFile();
    final String moduleName =
        moduleDefFile != null ? moduleDefFile.getModuleDefinition().getName() : null;

    // Figure exemplar name & method name.
    final TypeString exemplarName = helper.getExemplarTypeString();
    final String methodName = helper.getMethodName();

    // Figure modifiers.
    final Set<MethodDefinition.Modifier> modifiers = new HashSet<>();
    if (helper.isPrivateMethod()) {
      modifiers.add(MethodDefinition.Modifier.PRIVATE);
    }
    if (helper.isIterMethod()) {
      modifiers.add(MethodDefinition.Modifier.ITER);
    }
    if (helper.isAbstractMethod()) {
      modifiers.add(MethodDefinition.Modifier.ABSTRACT);
    }

    // Figure parameters.
    final TypeDocParser typeDocParser = new TypeDocParser(this.node);
    final Map<String, TypeString> parameterTypes = typeDocParser.getParameterTypes();
    final AstNode parametersNode = this.node.getFirstChild(MagikGrammar.PARAMETERS);
    final List<ParameterDefinition> parameters =
        this.createParameterDefinitions(timestamp, moduleName, parametersNode, parameterTypes);
    final AstNode assignmentParameterNode = node.getFirstChild(MagikGrammar.ASSIGNMENT_PARAMETER);
    final ParameterDefinition assignmentParameter =
        this.createAssignmentParameterDefinition(
            timestamp, moduleName, assignmentParameterNode, parameterTypes);

    // Get return types from method docs.
    final List<TypeString> returnTypes = typeDocParser.getReturnTypes();
    final List<TypeString> callResultDocs =
        MethodDefinitionParser.normalizeVariadicTail(returnTypes);
    // Ensure we can believe the docs, sort of.
    final boolean returnsAnything = helper.returnsAnything();
    final ExpressionResultString callResult =
        !callResultDocs.isEmpty() || callResultDocs.isEmpty() && !returnsAnything
            ? new ExpressionResultString(callResultDocs)
            : ExpressionResultString.UNDEFINED;

    // Get iterator types from method docs.
    final List<TypeString> loopTypes = typeDocParser.getLoopTypes();
    final List<TypeString> loopResultDocs = MethodDefinitionParser.normalizeVariadicTail(loopTypes);
    // Ensure method docs match actual loopbody, sort of.
    final boolean hasLoopbody = helper.hasLoopbody();
    final ExpressionResultString loopResult =
        !loopResultDocs.isEmpty() || loopResultDocs.isEmpty() && !hasLoopbody
            ? new ExpressionResultString(loopResultDocs)
            : ExpressionResultString.UNDEFINED;

    // Method doc.
    final String doc =
        MagikCommentExtractor.extractDocCommentTokens(node)
            .map(Token::getValue)
            .map(line -> line.substring(2)) // Strip '##'
            .map(String::trim)
            .collect(Collectors.joining("\n"));

    // Figure pragma.
    final PragmaNodeHelper pragmaHelper = PragmaNodeHelper.newSafe(this.node);
    final Pragma pragma =
        pragmaHelper != null
            ? new Pragma(
                pragmaHelper.getNode(),
                pragmaHelper.getClassifyLevels(),
                pragmaHelper.getTopics(),
                pragmaHelper.getUsages())
            : null;

    // Parse usages from code.
    final DefinitionUsageParser usageParser = new DefinitionUsageParser(this.magikFile, this.node);
    final MagikToolsProperties properties = this.magikFile.getProperties();
    final MagikAnalysisSettings settings = new MagikAnalysisSettings(properties);
    final List<GlobalUsage> usedGlobals =
        settings.getTypingIndexGlobalUsages()
            ? usageParser.getUsedGlobals()
            : Collections.emptyList();
    final List<MethodUsage> usedMethods =
        settings.getTypingIndexMethodUsages()
            ? usageParser.getUsedMethods()
            : Collections.emptyList();
    final List<SlotUsage> usedSlots =
        settings.getTypingIndexSlotUsages() ? usageParser.getUsedSlots() : Collections.emptyList();
    final List<ConditionUsage> usedConditions =
        settings.getTypingIndexConditionUsages()
            ? usageParser.getUsedConditions()
            : Collections.emptyList();

    // Parse @invokes_method annotations from TypeDoc.
    final String currentPackage = new PackageNodeHelper(this.node).getCurrentPackage();
    final List<String> invokesMethodAnnotations = typeDocParser.getInvokesMethodCalls();
    final List<MethodUsage> invokesMethodUsages =
        this.parseInvokesMethodAnnotations(invokesMethodAnnotations, location, currentPackage);

    // Combine method usages from code analysis and @invokes_method annotations.
    final List<MethodUsage> allUsedMethods = new ArrayList<>(usedMethods);
    allUsedMethods.addAll(invokesMethodUsages);

    final MethodDefinition methodDefinition =
        new MethodDefinition(
            location,
            timestamp,
            moduleName,
            doc,
            this.node,
            exemplarName,
            methodName,
            modifiers,
            parameters,
            assignmentParameter,
            pragma,
            callResult,
            loopResult,
            usedGlobals,
            allUsedMethods,
            usedSlots,
            usedConditions);
    return List.of(methodDefinition);
  }

  private static List<TypeString> normalizeVariadicTail(final List<TypeString> types) {
    if (types.size() < 2) {
      return types;
    }

    final List<TypeString> result = new ArrayList<>(types.size());
    for (int i = 0; i < types.size() - 1; ++i) {
      final TypeString typeStr = types.get(i);
      final TypeString realTypeStr = typeStr.isVariadic() ? typeStr.getVariadicInner() : typeStr;
      result.add(realTypeStr);
    }

    final int lastIdx = types.size() - 1;
    final TypeString lastTypeStr = types.get(lastIdx);
    result.add(lastTypeStr);

    return result;
  }

  private List<ParameterDefinition> createParameterDefinitions(
      final @Nullable Instant timestamp,
      final @Nullable String moduleName,
      final @Nullable AstNode parametersNode,
      final Map<String, TypeString> parameterTypes) {
    if (parametersNode == null) {
      return Collections.emptyList();
    }

    final URI uri = this.node.getToken().getURI();
    final List<ParameterDefinition> parameterDefinitions = new ArrayList<>();
    for (final AstNode parameterNode : parametersNode.getChildren(MagikGrammar.PARAMETER)) {
      final Location location = new Location(uri, parameterNode);

      final AstNode identifierNode = parameterNode.getFirstChild(MagikGrammar.IDENTIFIER);
      final String identifier = identifierNode.getTokenValue();

      final ParameterNodeHelper helper = new ParameterNodeHelper(parameterNode);
      final ParameterDefinition.Modifier modifier;
      if (helper.isOptionalParameter()) {
        modifier = ParameterDefinition.Modifier.OPTIONAL;
      } else if (helper.isGatherParameter()) {
        modifier = ParameterDefinition.Modifier.GATHER;
      } else {
        modifier = ParameterDefinition.Modifier.NONE;
      }

      final TypeString typeRef = parameterTypes.getOrDefault(identifier, TypeString.UNDEFINED);
      final ParameterDefinition parameterDefinition =
          new ParameterDefinition(
              location, timestamp, moduleName, null, parameterNode, identifier, modifier, typeRef);
      parameterDefinitions.add(parameterDefinition);
    }

    return parameterDefinitions;
  }

  @CheckForNull
  private ParameterDefinition createAssignmentParameterDefinition(
      final @Nullable Instant timestamp,
      final @Nullable String moduleName,
      final @Nullable AstNode assignmentParameterNode,
      final Map<String, TypeString> parameterTypes) {
    if (assignmentParameterNode == null) {
      return null;
    }

    final AstNode parameterNode = assignmentParameterNode.getFirstChild(MagikGrammar.PARAMETER);
    final URI uri = this.node.getToken().getURI();
    final Location location = new Location(uri, parameterNode);
    final String identifier = parameterNode.getTokenValue();
    final TypeString typeRef = parameterTypes.getOrDefault(identifier, TypeString.UNDEFINED);
    return new ParameterDefinition(
        location,
        timestamp,
        moduleName,
        null,
        parameterNode,
        identifier,
        ParameterDefinition.Modifier.NONE,
        typeRef);
  }

  /**
   * Parse @invokes_method annotations into MethodUsage objects.
   *
   * @param invokesAnnotations List of method invocation strings from @invokes_method annotations.
   * @param location Location for the method usages.
   * @param currentPackage Current package for parsing types without explicit package qualifiers.
   * @return List of MethodUsage objects.
   */
  private List<MethodUsage> parseInvokesMethodAnnotations(
      final List<String> invokesAnnotations, final Location location, final String currentPackage) {
    final List<MethodUsage> methodUsages = new ArrayList<>();

    for (final String invocationString : invokesAnnotations) {
      final MethodUsage methodUsage =
          MethodInvocationStringParser.parseInvocationString(
              invocationString, location, currentPackage);
      if (methodUsage != null) {
        methodUsages.add(methodUsage);
      }
    }

    return methodUsages;
  }
}
