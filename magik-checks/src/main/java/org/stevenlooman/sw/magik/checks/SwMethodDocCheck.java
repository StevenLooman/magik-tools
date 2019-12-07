package org.stevenlooman.sw.magik.checks;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import com.sonar.sslr.api.Token;

import org.sonar.check.Rule;
import org.stevenlooman.sw.magik.MagikCheck;
import org.stevenlooman.sw.magik.api.MagikGrammar;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.CheckForNull;

@Rule(key = SwMethodDocCheck.CHECK_KEY)
public class SwMethodDocCheck extends MagikCheck {

  private static final String MESSAGE = "No or invalid method doc: %s.";
  public static final String CHECK_KEY = "SwMethodDoc";

  private static final String PARAMETER_REGEXP = "[ \t]?([A-Z0-9_?]+)[ \t\n\r]+";

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
    String methodDoc = extractDoc(node);
    if (methodDoc == null) {
      String message = String.format(MESSAGE, "all");
      addIssue(message, node);
      return;
    }

    Set<String> methodParameters = getMethodParameters(node);
    Set<String> docParameters = getDocParameters(node);
    methodParameters.removeAll(docParameters);
    for (String missing : methodParameters) {
      String message = String.format(MESSAGE, missing);
      addIssue(message, node);
    }
  }

  private Set<String> getMethodParameters(AstNode node) {
    Set<String> parameters = new HashSet<>();

    // parameters
    AstNode parametersNode = node.getFirstChild(MagikGrammar.PARAMETERS);
    if (parametersNode != null) {
      List<AstNode> parameterNodes = parametersNode.getChildren(MagikGrammar.PARAMETER);
      List<String> names = parameterNodes.stream()
          .map(parameterNode -> parameterNode.getFirstChild(MagikGrammar.IDENTIFIER))
          .map(identifierNode -> identifierNode.getTokenValue())
          .map(identifier -> identifier.toUpperCase())
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
          .map(identifier -> identifier.toUpperCase())
          .collect(Collectors.toList());
      parameters.addAll(names);
    }

    // assignment value
    AstNode assignmentParameterNode = node.getFirstChild(MagikGrammar.ASSIGNMENT_PARAMETER);
    if (assignmentParameterNode != null) {
      AstNode identifierNode = assignmentParameterNode.getFirstChild(MagikGrammar.IDENTIFIER);
      String name = identifierNode.getTokenValue().toUpperCase();
      parameters.add(name);
    }

    return parameters;
  }

  @CheckForNull
  private String extractDoc(AstNode node) {
    String doc = null;

    AstNode bodyNode = node.getFirstChild(MagikGrammar.BODY);
    List<Token> bodyNodeTokens = bodyNode.getTokens();
    if (!bodyNodeTokens.isEmpty()) {
      List<Token> commentTokens = bodyNodeTokens.stream()
          .flatMap(token -> token.getTrivia().stream())
          .filter(trivia -> trivia.isComment())
          .map(trivia -> trivia.getToken())
          .collect(Collectors.toList());
      if (!commentTokens.isEmpty()) {
        Optional<String> methodDoc = commentTokens.stream()
            .map(token -> token.getValue())
            .reduce((acc, arg) -> acc + "\n" + arg);
        doc = methodDoc.get();
      }
    }

    return doc;
  }

  private Set<String> getDocParameters(AstNode node) {
    String methodDoc = extractDoc(node);
    Set<String> uppercased = new HashSet<>();

    Pattern pattern = Pattern.compile(PARAMETER_REGEXP);
    Matcher matcher = pattern.matcher(methodDoc);
    while (matcher.find()) {
      String name = matcher.group(1);
      uppercased.add(name);
    }

    return uppercased;
  }

}