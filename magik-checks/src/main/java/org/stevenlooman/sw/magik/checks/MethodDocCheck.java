package org.stevenlooman.sw.magik.checks;

import com.sonar.sslr.api.AstNode;
import org.sonar.check.Rule;
import org.stevenlooman.sw.magik.MagikCheck;
import org.stevenlooman.sw.magik.api.MagikGrammar;
import org.stevenlooman.sw.magik.parser.MethodDocParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Rule(key = MethodDocCheck.CHECK_KEY)
public class MethodDocCheck extends MagikCheck {

  private static final String MESSAGE = "No or invalid method doc: %s.";
  public static final String CHECK_KEY = "MethodDoc";

  @Override
  protected void walkPreMethodDefinition(AstNode node) {
    MethodDocParser docParser = new MethodDocParser(node);

    // ensure there is method doc at all
    if (docParser.getDoc() == null) {
      String message = String.format(MESSAGE, "all");
      addIssue(message, node);
      return;
    }

    // ensure sections have text
    List<String> mandatorySections = Arrays.asList("function", "returns", "parameters");
    for (String section : mandatorySections) {
      String str = docParser.getSection(section);
      if (str == null
          || str.isEmpty()) {
        String message = String.format(MESSAGE, section);
        addIssue(message, node);
      }
    }

    // compare parameters of node and doc
    List<String> methodParameters = getMethodParameters(node);
    Map<String, String> docParameters = docParser.getParameters();
    for (String methodParameter : methodParameters) {
      if (!docParameters.containsKey(methodParameter)) {
        String message = String.format(MESSAGE, "Parameter: " + methodParameter);
        addIssue(message, node);
      }
    }
    for (String docParameter : docParameters.keySet()) {
      if (!methodParameters.contains(docParameter)) {
        String message = String.format(MESSAGE, "Parameter: " + docParameter);
        addIssue(message, node);
      }
    }

    if (node.getTokenValue().equals("_iter")) {
      // Require loopbody if it is an iterator method.
      if (docParser.getSection("loopbody") == null) {
        String message = String.format(MESSAGE, "loopbody");
        addIssue(message, node);
      } else {
        // Match loopbody arguments with sub-section of loopbody.
        int loopParameterCount = getLoopbodyParameterCount(node);
        if (loopParameterCount != docParser.getLoopParameters().size()) {
          String message = String.format(MESSAGE, "Loopbody parameter " + loopParameterCount);
          addIssue(message, node);
        }
      }
    } else {
      // Disallow loopbody if it is an iterator method.
      if (docParser.getSection("loopbody") != null) {
        String message = String.format(MESSAGE, "loopbody");
        addIssue(message, node);
      }
    }
  }

  private List<String> getMethodParameters(AstNode node) {
    List<String> parameters = new ArrayList<>();

    // parameters
    AstNode parametersNode = node.getFirstChild(MagikGrammar.PARAMETERS);
    if (parametersNode != null) {
      List<AstNode> parameterNodes = parametersNode.getChildren(MagikGrammar.PARAMETER);
      List<String> names = parameterNodes.stream()
          .map(parameterNode -> parameterNode.getFirstChild(MagikGrammar.IDENTIFIER))
          .map(identifierNode -> identifierNode.getTokenValue())
          .map(identifier -> identifier.toLowerCase())
          .collect(Collectors.toList());
      parameters.addAll(names);
    }

    // indexer parameters
    AstNode indexerParametersNode = node.getFirstChild(MagikGrammar.INDEXER_PARAMETERS);
    if (indexerParametersNode != null) {
      List<AstNode> indexerParameterNodes =
            indexerParametersNode.getChildren(MagikGrammar.PARAMETER);
      List<String> names = indexerParameterNodes.stream()
          .map(parameterNode -> parameterNode.getFirstChild(MagikGrammar.IDENTIFIER))
          .map(identifierNode -> identifierNode.getTokenValue())
          .map(identifier -> identifier.toLowerCase())
          .collect(Collectors.toList());
      parameters.addAll(names);
    }

    // assignment value
    AstNode assignmentParameterNode = node.getFirstChild(MagikGrammar.ASSIGNMENT_PARAMETER);
    if (assignmentParameterNode != null) {
      AstNode identifierNode = assignmentParameterNode.getFirstChild(MagikGrammar.IDENTIFIER);
      String name = identifierNode.getTokenValue().toLowerCase();
      parameters.add(name);
    }

    return parameters;
  }

  private int getLoopbodyParameterCount(AstNode node) {
    List<AstNode> loopbodyNodes = node.getDescendants(MagikGrammar.LOOPBODY);

    int max = 0;
    for (AstNode loopbodyNode : loopbodyNodes) {
      // Ensure part part of iter proc.
      if (loopbodyNode.getFirstAncestor(MagikGrammar.PROC_DEFINITION) != null) {
        continue;
      }

      AstNode expressionsNode = loopbodyNode.getFirstChild(MagikGrammar.MULTI_VALUE_EXPRESSION);
      if (expressionsNode == null) {
        continue;
      }
      max = Math.max(expressionsNode.getChildren(MagikGrammar.EXPRESSION).size(), max);
    }
    return max;
  }

}