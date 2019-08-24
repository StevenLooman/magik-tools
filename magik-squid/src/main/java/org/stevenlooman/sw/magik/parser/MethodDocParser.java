package org.stevenlooman.sw.magik.parser;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.Trivia;
import org.stevenlooman.sw.magik.api.MagikGrammar;
import org.stevenlooman.sw.magik.api.MagikKeyword;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

public class MethodDocParser {

  private static final String SECTION_START_REGEXP = "^(\\w+\\??)[ \t]*:(.*)";

  private String doc;
  private int startLine;
  private Map<String, String> sections;
  private Map<String, Integer> sectionLines;
  private Map<String, String> parameters;
  private Map<String, Integer> parameterLines;

  /**
   * Constructor/initiator.
   * @param methodNode {{AstNode}} to analyze.
   */
  public MethodDocParser(AstNode methodNode) {
    this.doc = null;
    this.startLine = -1;
    this.sections = new Hashtable<>();
    this.sectionLines = new Hashtable<>();
    this.parameters = new Hashtable<>();
    this.parameterLines = new Hashtable<>();

    extractDocFromNode(methodNode); // sets doc and start line

    if (doc != null) {
      parseSections(doc, startLine, sections, sectionLines);

      String parametersDoc = sections.get("parameters");
      Integer parametersLine = sectionLines.get("parameters");
      if (parametersDoc != null) {
        parseSections(parametersDoc, parametersLine, parameters, parameterLines);
      }
    }
  }

  private void extractDocFromNode(AstNode node) {
    // try first child of body
    AstNode bodyNode = node.getFirstChild(MagikGrammar.BODY);
    Token bodyNodeToken = bodyNode.getToken();
    if (bodyNodeToken != null) {
      List<Trivia> trivia = bodyNodeToken.getTrivia();
      if (!trivia.isEmpty()) {
        Optional<String> methodDoc = trivia.stream()
            .filter(t -> t.isComment())
            .map(t -> t.getToken().getValue())
            .reduce((acc, arg) -> acc + "\n" + arg);
        this.doc = methodDoc.get();
        this.startLine = trivia.get(0).getToken().getLine();
        return;
      }
    }

    // try _ENDMETHOD node
    AstNode endMethodNode = node.getFirstChild(MagikKeyword.ENDMETHOD);
    Token endMethodNodeToken = endMethodNode.getToken();
    if (endMethodNodeToken != null) {
      List<Trivia> trivia = endMethodNodeToken.getTrivia();
      if (!trivia.isEmpty()) {
        Optional<String> methodDoc = trivia.stream()
            .filter(t -> t.isComment())
            .map(t -> t.getToken().getValue())
            .reduce((acc, arg) -> acc + "\n" + arg);
        this.doc = methodDoc.get();
        this.startLine = trivia.get(0).getToken().getLine();
        return;
      }
    }
  }

  private void parseSections(
      String doc, Integer lineOffset,
      Map<String, String> sections, Map<String, Integer> sectionLines) {
    String bareDoc = doc.replaceAll("## ", ""); // strip ##-prefix from doc

    Pattern pattern = Pattern.compile(SECTION_START_REGEXP);
    String currentKey = null;
    StringBuilder builder = new StringBuilder();
    int lineNo = lineOffset;
    for (String line: bareDoc.split("\n")) {
      Matcher matcher = pattern.matcher(line);
      if (matcher.find()) {
        if (currentKey != null) {
          sections.put(currentKey, builder.toString());
          builder.setLength(0);
        }

        currentKey = matcher.group(1).toLowerCase();
        String value = matcher.group(2).trim();
        builder.append(value);

        sectionLines.put(currentKey, lineNo);
      } else if (currentKey != null) {
        builder.append("\n");
        builder.append(line.trim());
      }
      lineNo += 1;
    }
    if (currentKey != null) {
      sections.put(currentKey, builder.toString());
      builder.setLength(0);
    }
  }

  @Nullable
  public String getDoc() {
    return doc;
  }

  @Nullable
  public String getSection(String name) {
    return sections.get(name);
  }

  @Nullable
  public Integer getLineForSection(String name) {
    return sectionLines.get(name);
  }

  public Map<String, String> getParameters() {
    return parameters;
  }

  public Integer getLineForParameter(String name) {
    return parameterLines.get(name);
  }

}

