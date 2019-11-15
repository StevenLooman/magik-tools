package org.stevenlooman.sw.magik.checks;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import org.sonar.check.Rule;
import org.stevenlooman.sw.magik.MagikCheck;
import org.stevenlooman.sw.magik.api.MagikGrammar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Rule(key = DuplicateMethodInFileCheck.CHECK_KEY)
public class DuplicateMethodInFileCheck extends MagikCheck {

  private static final String MESSAGE = "Duplicate method definition in this file.";
  public static final String CHECK_KEY = "DuplicateMethodInFile";

  @Override
  public boolean isTemplatedCheck() {
    return false;
  }

  @Override
  public List<AstNodeType> subscribedTo() {
    return Arrays.asList(MagikGrammar.MAGIK);
  }

  @Override
  public void visitNode(AstNode node) {
    Map<String, List<AstNode>> methods = new HashMap<>();

    // convert all method definitions to strings
    List<AstNode> methodDefinitionNodes = node.getChildren(MagikGrammar.METHOD_DEFINITION);
    for (AstNode methodDefinitionNode : methodDefinitionNodes) {
      String methodName = nameForMethodNode(methodDefinitionNode);

      List<AstNode> knownNodes = methods.getOrDefault(methodName, new ArrayList<>());
      knownNodes.add(methodDefinitionNode);
      methods.put(methodName, knownNodes);
    }

    // test for duplicates
    for (Map.Entry<String, List<AstNode>> entry : methods.entrySet()) {
      List<AstNode> nodes = entry.getValue();
      if (nodes.size() < 2) {
        continue;
      }

      for (AstNode methodDefinitionNode : nodes) {
        addIssue(MESSAGE, methodDefinitionNode);
      }
    }
  }

  private String nameForMethodNode(AstNode methodDefinitionNode) {
    StringBuilder builder = new StringBuilder();

    List<AstNode> identifierNodes = methodDefinitionNode.getChildren(MagikGrammar.IDENTIFIER);

    // object
    String objectName = identifierNodes.get(0).getTokenValue();
    builder.append(objectName);

    // method name
    if (identifierNodes.size() > 1) {
      builder.append(".");
      String methodName = identifierNodes.get(1).getTokenValue();
      builder.append(methodName);
    }

    // parameters
    AstNode parametersNode = methodDefinitionNode.getFirstChild(MagikGrammar.PARAMETERS);
    if (parametersNode != null) {
      builder.append("()");
    }

    // indexer parameters
    AstNode indexerParametersNode =
          methodDefinitionNode.getFirstChild(MagikGrammar.INDEXER_PARAMETERS);
    if (indexerParametersNode != null) {
      builder.append("[");
      List<AstNode> parameterNodes = indexerParametersNode.getChildren(MagikGrammar.PARAMETER);
      String commas = String.join("", Collections.nCopies(parameterNodes.size() - 1, ","));
      builder.append(commas);
      builder.append("]");
    }


    // assignment parameter
    AstNode assignmentParameterNode =
          methodDefinitionNode.getFirstChild(MagikGrammar.ASSIGNMENT_PARAMETER);
    if (assignmentParameterNode != null) {
      builder.append("<<");
    }

    return builder.toString();
  }

}
