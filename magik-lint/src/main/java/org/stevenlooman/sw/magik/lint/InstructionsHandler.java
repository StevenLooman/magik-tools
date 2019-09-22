package org.stevenlooman.sw.magik.lint;

import org.stevenlooman.sw.magik.MagikVisitorContext;
import org.stevenlooman.sw.magik.analysis.scope.GlobalScope;
import org.stevenlooman.sw.magik.analysis.scope.Scope;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

/**
 * Read mlint: a=b;c=d instructions at lines and scopes.
 */
public class InstructionsHandler {

  private static final Pattern MLINT_PATTERN_EOL = Pattern.compile(".*# ?mlint: ?(.*)");
  private static final Pattern MLINT_PATTERN_SINGLE = Pattern.compile("^\\s*# ?mlint: ?(.*)");

  private MagikVisitorContext context;
  private Map<Scope, Map<String, String>> scopeInstructions = new HashMap<>();
  private Map<Integer, Map<String, String>> lineInstructions = new HashMap<>();

  /**
   * Constructor.
   * @param context {{MagikVisitorContext}} to use.
   */
  public InstructionsHandler(MagikVisitorContext context) {
    this.context = context;
    parseInstructionsInScopes();
    parseInstructionsFromLines();
  }

  // #region: parsing
  /**
   * Extract instructions-string (at the end) from {{str}}.
   * @param str String to extract from.
   * @return Unparsed instructions-string.
   */
  @Nullable
  private String extractInstructionsInStr(String str, boolean single) {
    Pattern pattern = MLINT_PATTERN_EOL;
    if (single) {
      pattern = MLINT_PATTERN_SINGLE;
    }

    Matcher matcher = pattern.matcher(str);
    if (!matcher.find()) {
      return null;
    }

    return matcher.group(1);
  }

  /**
   * Parse all mlint instructions.
   * Mlint instructions are key=value pairs, each pair being ;-separated.
   * @param str String to parse.
   * @return Map with parsed instructions.
   */
  private Map<String, String> parseMLintInstructions(String str) {
    Map<String, String> instructions = new HashMap<>();

    List<String> items = Arrays.stream(str.split(";"))
        .map(instruction -> instruction.trim())
        .collect(Collectors.toList());
    for (String item : items) {
      String[] parts = item.split("=");
      if (parts.length != 2) {
        continue;
      }

      String key = parts[0].trim();
      String value = parts[1].trim();
      instructions.put(key, value);
    }

    return instructions;
  }

  // #endregion

  // #region: scopes
  /**
   * Get instructions in {{Scope}} and any ancestor {{Scope}}s.
   * @param line Line in file
   * @param column Column in file
   * @return Map with instructions for the Scope at line/column
   */
  public Map<String, String> getInstructionsInScope(int line, int column) {
    Map<String, String> instructions = new HashMap<>();
    GlobalScope globalScope = context.getGlobalScope();
    if (globalScope == null) {
      return instructions;
    }

    // ensure we can find a Scope
    Scope fromScope = globalScope.getScopeForLineColumn(line, column);
    if (fromScope == null) {
      return instructions;
    }

    // iterate over all (ancestor) scopes, see if the check is disabled in any scope
    List<Scope> scopes = fromScope.getSelfAndAncestorScopes();
    // Reverse such that if a narrower scope overrides a broader scope instruction,
    // the narrower instruction is overridden.
    Collections.reverse(scopes);
    for (Scope scope : scopes) {
      Map<String, String> scopeInstuctions = this.scopeInstructions.get(scope);
      if (scopeInstuctions != null) {
        instructions.putAll(scopeInstuctions);
      }
    }

    return instructions;
  }

  private void parseInstructionsInScopes() {
    String fileContents = context.fileContent();
    if (fileContents == null
        || fileContents.isEmpty()) {
      return;
    }

    GlobalScope globalScope = context.getGlobalScope();
    String[] lines = fileContents.split("\r\n|\n|\r");  // match BufferedReader.readLine()
    for (int lineNo = 0; lineNo < lines.length; ++lineNo) {
      String line = lines[lineNo];
      String str = extractInstructionsInStr(line, true);
      if (str == null) {
        continue;
      }

      Map<String, String> instructions = parseMLintInstructions(str);
      Scope scope = globalScope.getScopeForLineColumn(lineNo + 1, 0);
      if (scope == null) {
        continue;
      }

      Map<String, String> instructionsInScope = scopeInstructions.get(scope);
      if (instructionsInScope == null) {
        scopeInstructions.put(scope, instructions);
      } else {
        instructionsInScope.putAll(instructions);
      }
    }
  }
  // #endregion

  // #region: lines
  /**
   * Get instructions at (the end of) {{line}}.
   * @param line Line number to extract from.
   * @return Instructions at {{line}}.
   */
  public Map<String, String> getInstructionsAtLine(int line) {
    Map<String, String> instructions = lineInstructions.get(line);
    if (instructions == null) {
      return new HashMap<>();
    }
    return instructions;
  }

  /**
   * Read all instructions from all lines.
   */
  private void parseInstructionsFromLines() {
    String fileContents = context.fileContent();
    if (fileContents == null
        || fileContents.isEmpty()) {
      return;
    }

    String[] lines = fileContents.split("\r\n|\n|\r");  // match BufferedReader.readLine()
    for (int lineNo = 0; lineNo < lines.length; ++lineNo) {
      String line = lines[lineNo];
      String str = extractInstructionsInStr(line, false);
      if (str == null) {
        continue;
      }

      Map<String, String> instructions = parseMLintInstructions(str);
      lineInstructions.put(lineNo + 1, instructions);
    }
  }
  // #endregion

}