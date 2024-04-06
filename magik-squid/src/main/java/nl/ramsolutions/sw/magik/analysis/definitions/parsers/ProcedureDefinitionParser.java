package nl.ramsolutions.sw.magik.analysis.definitions.parsers;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.definitions.ModuleDefinitionScanner;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.analysis.definitions.Definition;
import nl.ramsolutions.sw.magik.analysis.definitions.ParameterDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ProcedureDefinition;
import nl.ramsolutions.sw.magik.analysis.helpers.ParameterNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.ProcedureDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.parser.MagikCommentExtractor;
import nl.ramsolutions.sw.magik.parser.TypeDocParser;

public class ProcedureDefinitionParser {

  private final AstNode node;

  /**
   * Constructor.
   *
   * @param node {@code define_shared_constant()} node.
   */
  public ProcedureDefinitionParser(final AstNode node) {
    if (node.isNot(MagikGrammar.PROCEDURE_DEFINITION)) {
      throw new IllegalArgumentException();
    }

    this.node = node;
  }

  /**
   * Test if node is a {@code define_shared_constant()}.
   *
   * @param node Node to test
   * @return True if node is a {@code define_shared_variable()}, false otherwise.
   */
  public static boolean isProcedureDefinition(final AstNode node) {
    return node.is(MagikGrammar.PROCEDURE_DEFINITION);
  }

  /**
   * Parse defitions.
   *
   * @return List of parsed definitions.
   */
  public List<Definition> parseDefinitions() {
    final AstNode syntaxErrorNode = this.node.getFirstChild(MagikGrammar.SYNTAX_ERROR);
    if (syntaxErrorNode != null) {
      return Collections.emptyList();
    }

    // Figure location.
    final URI uri = node.getToken().getURI();
    final Location location = new Location(uri, node);

    // Figure module name.
    final String moduleName = ModuleDefinitionScanner.getModuleName(uri);

    // Figure procedure name.
    final ProcedureDefinitionNodeHelper helper = new ProcedureDefinitionNodeHelper(node);
    final String procedureName = helper.getProcedureName();

    // Figure modifiers.
    final Set<ProcedureDefinition.Modifier> modifiers = new HashSet<>();
    if (helper.isIterProc()) {
      modifiers.add(ProcedureDefinition.Modifier.ITER);
    }

    // Figure parameters.
    final AstNode parametersNode = this.node.getFirstChild(MagikGrammar.PARAMETERS);
    if (parametersNode == null) {
      // Robustness, in case of a syntax error in the procedure definition.
      return Collections.emptyList();
    }
    final TypeDocParser typeDocParser = new TypeDocParser(this.node);
    final Map<String, TypeString> parameterTypes = typeDocParser.getParameterTypes();
    final List<ParameterDefinition> parameters =
        this.createParameterDefinitions(moduleName, parametersNode, parameterTypes);

    // Get return types from method docs.
    final List<TypeString> callResultDocs = typeDocParser.getReturnTypes();
    // Ensure we can believe the docs, sort of.
    final boolean returnsAnything = helper.returnsAnything();
    final ExpressionResultString callResult =
        !callResultDocs.isEmpty() || callResultDocs.isEmpty() && !returnsAnything
            ? new ExpressionResultString(callResultDocs)
            : ExpressionResultString.UNDEFINED;

    // Get iterator types from method docs.
    final List<TypeString> loopResultDocs = typeDocParser.getLoopTypes();
    // Ensure method docs match actual loopbody, sort of.
    final boolean hasLoopbody = helper.hasLoopbody();
    final ExpressionResultString loopResult =
        !loopResultDocs.isEmpty() || loopResultDocs.isEmpty() && !hasLoopbody
            ? new ExpressionResultString(loopResultDocs)
            : ExpressionResultString.UNDEFINED;

    // Procedure doc.
    final String doc =
        MagikCommentExtractor.extractDocCommentTokens(node)
            .map(Token::getValue)
            .map(line -> line.substring(2)) // Strip '##'
            .map(String::trim)
            .collect(Collectors.joining("\n"));

    final TypeString typeString = AnonymousNamer.getNameForProcedure(this.node);
    return List.of(
        new ProcedureDefinition(
            location,
            moduleName,
            doc,
            node,
            modifiers,
            typeString,
            procedureName,
            parameters,
            callResult,
            loopResult));
  }

  private List<ParameterDefinition> createParameterDefinitions(
      final @Nullable String moduleName,
      final AstNode parametersNode,
      final Map<String, TypeString> parameterTypes) {
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
              location, moduleName, null, parameterNode, identifier, modifier, typeRef);
      parameterDefinitions.add(parameterDefinition);
    }

    return parameterDefinitions;
  }
}
