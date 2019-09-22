package org.stevenlooman.sw.magik.checks;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
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
  public boolean isTemplatedCheck() {
    return false;
  }

  @Override
  public List<AstNodeType> subscribedTo() {
    return Arrays.asList(MagikGrammar.METHOD_DEFINITION);
  }

  @Override
  public void visitNode(AstNode node) {
    MethodDocParser docParser = new MethodDocParser(node);

    if (docParser.getDoc() == null) {
      String message = String.format(MESSAGE, "all");
      addIssue(message, node);
      return;
    }

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

    // assignment value
    AstNode assignmentParameterNode = node.getFirstChild(MagikGrammar.ASSIGNMENT_PARAMETER);
    if (assignmentParameterNode != null) {
      AstNode identifierNode = assignmentParameterNode.getFirstChild(MagikGrammar.IDENTIFIER);
      String name = identifierNode.getTokenValue();
      parameters.add(name);
    }

    return parameters;
  }

}