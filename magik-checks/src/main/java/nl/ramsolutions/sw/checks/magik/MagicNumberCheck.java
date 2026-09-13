package nl.ramsolutions.sw.checks.magik;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.checks.MagikCheck;
import nl.ramsolutions.sw.magik.analysis.definitions.parsers.DefSlottedExemplarParser;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.api.MagikKeyword;
import nl.ramsolutions.sw.magik.api.MagikNumberParser;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;

/** Check that there are no magic numbers. */
@Rule(key = MagicNumberCheck.CHECK_KEY)
public class MagicNumberCheck extends MagikCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "MagicNumber";

  private static final String DEFINE_SHARED_CONSTANT = "define_shared_constant()";
  private static final String DEFINE_SHARED_VARIABLE = "define_shared_variable()";

  private static final String DEFAULT_IGNORE_NUMBERS = "-1,0,1,2";
  private static final boolean DEFAULT_IGNORE_CONSTANT_DECLARATIONS = true;
  private static final boolean DEFAULT_IGNORE_SLOT_DEFAULT_VALUE = false;

  private static final String MESSAGE = "'%s' is a magic number.";

  /** List of ignored numbers, separated by ','. */
  @RuleProperty(
      key = "ignore numbers",
      defaultValue = "" + DEFAULT_IGNORE_NUMBERS,
      description = "List of ignored numbers, separated by ','",
      type = "STRING")
  @SuppressWarnings("checkstyle:VisibilityModifier")
  public String ignoreNumbers = DEFAULT_IGNORE_NUMBERS;

  /** Ignore constant declarations. */
  @RuleProperty(
      key = "ignore constant declarations",
      defaultValue = "" + DEFAULT_IGNORE_CONSTANT_DECLARATIONS,
      description = "Ignore constant declarations",
      type = "BOOLEAN")
  @SuppressWarnings("checkstyle:VisibilityModifier")
  public boolean ignoreConstantDeclarations = DEFAULT_IGNORE_CONSTANT_DECLARATIONS;

  /** Ignore slot default values. */
  @RuleProperty(
      key = "ignore slot default values",
      defaultValue = "" + DEFAULT_IGNORE_SLOT_DEFAULT_VALUE,
      description = "Ignore field (slot) declarations",
      type = "BOOLEAN")
  @SuppressWarnings("checkstyle:VisibilityModifier")
  public boolean ignoreSlotDefaultValues = DEFAULT_IGNORE_SLOT_DEFAULT_VALUE;

  @Override
  protected void walkPreNumber(final AstNode node) {
    if (this.isIgnoredContext(node)) {
      return;
    }

    final AstNode unaryNode = this.getUnaryExpression(node);
    final String numberStr = this.getNumberString(node, unaryNode);
    if (this.isIgnoredNumber(numberStr)) {
      return;
    }

    final AstNode issueNode = unaryNode != null ? unaryNode : node;
    final String message = MESSAGE.formatted(numberStr);
    this.addIssue(issueNode, message);
  }

  private boolean isIgnoredContext(final AstNode node) {
    if (node.getFirstAncestor(MagikGrammar.PRIMITIVE_STATEMENT) != null) {
      return true;
    }

    return this.ignoreConstantDeclarations && this.isConstantDeclaration(node)
        || this.ignoreSlotDefaultValues && this.isSlotDefaultValue(node);
  }

  private boolean isConstantDeclaration(final AstNode node) {
    final AstNode varDefStmt = node.getFirstAncestor(MagikGrammar.VARIABLE_DEFINITION_STATEMENT);
    if (varDefStmt != null) {
      final boolean isConstant =
          varDefStmt.getChildren(MagikGrammar.VARIABLE_DEFINITION_MODIFIER).stream()
              .anyMatch(
                  mod ->
                      MagikKeyword.CONSTANT.getValue().equals(mod.getTokenValue().toLowerCase()));
      if (isConstant) {
        return true;
      }
    }

    final AstNode methodInvoc = node.getFirstAncestor(MagikGrammar.METHOD_INVOCATION);
    if (methodInvoc != null) {
      final MethodInvocationNodeHelper helper = new MethodInvocationNodeHelper(methodInvoc);
      final String methodName = helper.getMethodName();
      if (MagicNumberCheck.DEFINE_SHARED_CONSTANT.equalsIgnoreCase(methodName)
          || MagicNumberCheck.DEFINE_SHARED_VARIABLE.equalsIgnoreCase(methodName)) {
        return true;
      }
    }

    return false;
  }

  private boolean isSlotDefaultValue(final AstNode node) {
    final AstNode procInvoc = node.getFirstAncestor(MagikGrammar.PROCEDURE_INVOCATION);
    return procInvoc != null && DefSlottedExemplarParser.isDefSlottedExemplar(procInvoc);
  }

  private @Nullable AstNode getUnaryExpression(final AstNode numberNode) {
    final AstNode atomNode = numberNode.getParent();
    if (atomNode == null || !atomNode.is(MagikGrammar.ATOM)) {
      return null;
    }

    final AstNode unaryNode = atomNode.getParent();
    if (unaryNode == null || !unaryNode.is(MagikGrammar.UNARY_EXPRESSION)) {
      return null;
    }

    final String operatorValue = unaryNode.getChildren().get(0).getTokenValue();
    if ("-".equals(operatorValue) || "+".equals(operatorValue)) {
      return unaryNode;
    }

    return null;
  }

  private String getNumberString(final AstNode numberNode, final @Nullable AstNode unaryNode) {
    final String tokenValue = numberNode.getTokenValue();
    if (unaryNode != null) {
      final String prefix = unaryNode.getChildren().get(0).getTokenValue();
      return prefix + tokenValue;
    }
    return tokenValue;
  }

  private boolean isIgnoredNumber(final String numberStr) {
    final Set<String> rawStrings = this.getIgnoredRawStrings();
    if (rawStrings.contains(numberStr)) {
      return true;
    }

    final Double number = this.parseNumber(numberStr);
    return number != null && this.getIgnoredDoubles(rawStrings).contains(number);
  }

  private Set<String> getIgnoredRawStrings() {
    return Arrays.stream(this.ignoreNumbers.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .collect(Collectors.toSet());
  }

  private Set<Double> getIgnoredDoubles(final Set<String> rawStrings) {
    return rawStrings.stream()
        .map(this::parseNumber)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
  }

  private @Nullable Double parseNumber(final String numberStr) {
    final String unsignedStr;
    final int sign;
    if (numberStr.startsWith("-")) {
      unsignedStr = numberStr.substring(1);
      sign = -1;
    } else if (numberStr.startsWith("+")) {
      unsignedStr = numberStr.substring(1);
      sign = 1;
    } else {
      unsignedStr = numberStr;
      sign = 1;
    }

    final Number number = MagikNumberParser.parseMagikNumberSafe(unsignedStr);
    return number != null ? sign * number.doubleValue() : null;
  }
}
