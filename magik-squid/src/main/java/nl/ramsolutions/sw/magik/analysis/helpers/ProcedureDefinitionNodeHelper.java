package nl.ramsolutions.sw.magik.analysis.helpers;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.api.MagikKeyword;

/** Helper for METHOD_DEFINITION nodes. */
public class ProcedureDefinitionNodeHelper {

  private final AstNode node;

  /**
   * Constructor.
   *
   * @param node Node to encapsulate.
   */
  public ProcedureDefinitionNodeHelper(final AstNode node) {
    if (!node.is(MagikGrammar.PROCEDURE_DEFINITION)) {
      throw new IllegalArgumentException();
    }

    this.node = node;
  }

  /**
   * Get parameters + nodes.
   *
   * @return Map with parameters + PARAMETER nodes.
   */
  public Map<String, AstNode> getParameterNodes() {
    final AstNode parametersNode = this.node.getFirstChild(MagikGrammar.PARAMETERS);
    if (parametersNode == null) {
      return Collections.emptyMap();
    }

    return parametersNode.getChildren(MagikGrammar.PARAMETER).stream()
        .collect(
            Collectors.toMap(
                parameterNode ->
                    parameterNode.getFirstChild(MagikGrammar.IDENTIFIER).getTokenValue(),
                parameterNode -> parameterNode));
  }

  /**
   * Get procedure name.
   *
   * @return Name of the procedure.
   */
  @CheckForNull
  public String getProcedureName() {
    final AstNode nameNode = node.getFirstChild(MagikGrammar.PROCEDURE_NAME);
    if (nameNode == null) {
      return null;
    }
    final AstNode labelNode = nameNode.getFirstChild(MagikGrammar.LABEL);
    return labelNode.getLastChild().getTokenOriginalValue();
  }

  public AstNode getProcedureNode() {
    return this.node.getChildren().stream()
        .filter(childNode -> childNode.isNot(MagikGrammar.values()))
        .findFirst()
        .orElseThrow();
  }

  /**
   * Test if procedure returns anything.
   *
   * @return
   */
  public boolean returnsAnything() {
    final List<AstNode> returnStatementNodes =
        this.node.getDescendants(MagikGrammar.RETURN_STATEMENT);
    final boolean hasReturn =
        returnStatementNodes.stream()
            .filter(
                statementNode ->
                    statementNode.getFirstAncestor(MagikGrammar.PROCEDURE_DEFINITION) == this.node)
            .anyMatch(statementNode -> statementNode.hasDescendant(MagikGrammar.TUPLE));

    final boolean hasEmit =
        this.node.getFirstChild(MagikGrammar.BODY).getChildren(MagikGrammar.STATEMENT).stream()
            .anyMatch(
                statementNode -> !statementNode.getChildren(MagikGrammar.EMIT_STATEMENT).isEmpty());

    return hasReturn || hasEmit;
  }

  /**
   * Test if procedure has a loopbody statement.
   *
   * @return
   */
  public boolean hasLoopbody() {
    return this.node.getDescendants(MagikGrammar.LOOPBODY).stream()
        .anyMatch(
            statementNode ->
                statementNode.getFirstAncestor(MagikGrammar.PROCEDURE_DEFINITION) == this.node);
  }

  private Collection<AstNode> getMethodModifiers() {
    final AstNode modifiersNode = this.node.getFirstChild(MagikGrammar.PROCEDURE_MODIFIERS);
    if (modifiersNode == null) {
      return Collections.emptySet();
    }

    return modifiersNode.getChildren();
  }

  public boolean isIterProc() {
    final String modifier = MagikKeyword.ITER.getValue();
    return this.getMethodModifiers().stream()
        .anyMatch(modifierNode -> modifierNode.getTokenValue().equalsIgnoreCase(modifier));
  }
}
