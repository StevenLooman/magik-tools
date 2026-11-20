package nl.ramsolutions.sw.magik.debugadapter.variables;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import nl.ramsolutions.sw.magik.debugadapter.TestSlapProtocol;
import nl.ramsolutions.sw.magik.debugadapter.slap.ISlapResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.EvalResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.StackFrameLocalsResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.StackFrameLocalsResponse.LocalType;

/**
 * Fake agent for a debuggee whose values are kinds of mixin, answering the {@link
 * VariableManager}'s describe expression as Magik would.
 */
class DescribingSlapProtocol extends TestSlapProtocol {

  static final String PRINT_STRING = ".print_string";

  private static final Pattern BRANCH =
      Pattern.compile("(?:_if|_elif) (.+?) _then >> \\((.*?)\\) \\* (\\d+) \\+ (\\d+) ");
  private static final Pattern SUBJECT = Pattern.compile("^_if \\(?(.+?)\\.is_kind_of\\?\\(");

  /** A value of the debuggee: the mixins it inherits from and its size. */
  record Value(Set<String> mixins, int size) {}

  private final List<String> localNames = new ArrayList<>();
  private final Map<String, Value> values = new HashMap<>();
  private final List<String> expressions = new ArrayList<>();
  private String slotNames = "";

  void addLocal(final String name, final Value value) {
    this.localNames.add(name);
    this.values.put(name, value);
  }

  void addValue(final String expression, final Value value) {
    this.values.put(expression, value);
  }

  void setSlotNames(final String slotNames) {
    this.slotNames = slotNames;
  }

  List<String> getExpressions() {
    return this.expressions;
  }

  long countExpressionsContaining(final String part) {
    return this.expressions.stream().filter(expression -> expression.contains(part)).count();
  }

  @Override
  public CompletableFuture<ISlapResponse> getStackFrameLocals(final long threadId, final int level)
      throws IOException {
    final List<ISlapResponse> locals = new ArrayList<>();
    for (final String name : this.localNames) {
      locals.add(
          new StackFrameLocalsResponse.Local(
              LocalType.TYPE_OBJ,
              name,
              "value of " + name,
              EnumSet.noneOf(StackFrameLocalsResponse.VariableType.class)));
    }
    final StackFrameLocalsResponse response = new StackFrameLocalsResponse(locals);
    return CompletableFuture.completedFuture(response);
  }

  @Override
  public CompletableFuture<ISlapResponse> evaluate(
      final long threadId, final int level, final String expression) throws IOException {
    this.expressions.add(expression);

    final String result = this.answer(expression);
    if (result == null) {
      final Exception failure = new IllegalStateException("Cannot evaluate: " + expression);
      return CompletableFuture.failedFuture(failure);
    }
    final EvalResponse response = new EvalResponse(result);
    return CompletableFuture.completedFuture(response);
  }

  /**
   * Answer an expression.
   *
   * @param expression Expression.
   * @return Result, or {@code null} to fail the evaluation.
   */
  String answer(final String expression) {
    if (expression.contains(" _then >> (")) {
      return this.answerDescribe(expression);
    }

    if (expression.contains(".is_kind_of?(sw:enumerated_format_mixin)")) {
      return ":slotted_format_mixin";
    }

    if (expression.contains(".sys!all_slot_names()")) {
      return this.slotNames;
    }

    if (expression.endsWith(PRINT_STRING)) {
      final int end = expression.length() - PRINT_STRING.length();
      return "value of " + expression.substring(0, end);
    }

    return "0";
  }

  private String answerDescribe(final String expression) {
    final Matcher subjectMatcher = SUBJECT.matcher(expression);
    if (!subjectMatcher.find()) {
      return null;
    }
    final String subject = subjectMatcher.group(1);
    final Value value = this.values.get(subject);
    if (value == null) {
      return "-1";
    }

    final Matcher branchMatcher = BRANCH.matcher(expression);
    while (branchMatcher.find()) {
      final String condition = branchMatcher.group(1);
      if (this.matchesCondition(condition, value)) {
        final String countExpression = branchMatcher.group(2);
        final long count = countExpression.equals(subject + ".size") ? value.size() : 0;
        final String modulesText = branchMatcher.group(3);
        final String indexText = branchMatcher.group(4);
        final long modules = Long.parseLong(modulesText);
        final long index = Long.parseLong(indexText);
        final long answer = count * modules + index;
        return Long.toString(answer);
      }
    }
    return "-1";
  }

  private boolean matchesCondition(final String condition, final Value value) {
    return value.mixins().stream().anyMatch(mixin -> condition.contains("(sw:" + mixin + ")"));
  }
}
