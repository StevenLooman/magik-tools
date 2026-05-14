package nl.ramsolutions.sw.checks.magiktyped;

import com.sonar.sslr.api.AstNode;
import java.util.Map;
import java.util.Objects;
import nl.ramsolutions.sw.checks.MagikTypedCheck;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.ProcedureDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeStringResolver;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasonerState;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.api.TypeDocGrammar;
import nl.ramsolutions.sw.magik.parser.TypeDocParser;
import nl.ramsolutions.sw.magik.utils.StreamUtils;
import org.sonar.check.Rule;

/** Check if @loop types from doc match reasoned loopbody yield types. */
@Rule(key = IterCallableYieldTypesMatchDocTypedCheck.CHECK_KEY)
public class IterCallableYieldTypesMatchDocTypedCheck extends MagikTypedCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "IterCallableYieldTypesMatchDoc";

  private static final String MESSAGE =
      "@loop type(s) (%s) do not match iter callable yield type(s) (%s).";
  private static final String NON_ITER_MESSAGE = "@loop type(s) specified on a non-iter callable.";
  private static final String MISSING_LOOP_MESSAGE =
      "@loop type annotation missing for iter callable yielding type(s) (%s).";
  private static final String MISSING_LOOP_MESSAGE_UNKNOWN =
      "@loop type annotation missing for iter callable with unknown yield type(s).";

  @Override
  protected void walkPostMethodDefinition(final AstNode node) {
    this.checkIterCallableYieldDoc(node);
  }

  @Override
  protected void walkPostProcedureDefinition(final AstNode node) {
    this.checkIterCallableYieldDoc(node);
  }

  private void checkIterCallableYieldDoc(final AstNode node) {
    final Map<AstNode, TypeString> typeDocNodes = this.extractCallableDocLoopResult(node);
    if (typeDocNodes.isEmpty()) {
      if (this.isIterCallable(node) && !this.isAbstractCallable(node)) {
        this.reportMissingLoopDoc(node);
      }

      return;
    }

    if (!this.isIterCallable(node)) {
      typeDocNodes.keySet().forEach(this::reportNonIterLoopTypeDocIssue);
      return;
    }

    if (this.isAbstractCallable(node)) {
      return;
    }

    final ExpressionResultString iterResultString = this.extractReasonedIterResult(node);

    final TypeStringResolver resolver = this.getTypeStringResolver();
    StreamUtils.zip(iterResultString.stream(), typeDocNodes.entrySet().stream())
        .forEach(
            entry -> {
              if (entry.getKey() == null || entry.getValue() == null) {
                return;
              }

              final TypeString iterTypeString = entry.getKey();
              // Skip UNDEFINED slots: UndefinedMethodCallResultTypedCheck already covers these.
              if (iterTypeString.containsUndefined()) {
                return;
              }

              // Variadics don't resolve to an exemplar themselves; compare the inner type
              // when either side is variadic so e.g. variadic(int) vs variadic(symbol)
              // is reported as a mismatch rather than equal-by-virtue-of-both-being-null.
              final TypeString iterLookup =
                  iterTypeString.isVariadic() ? iterTypeString.getVariadicInner() : iterTypeString;
              final ExemplarDefinition iterExemplarDef = resolver.getExemplarDefinition(iterLookup);

              final Map.Entry<AstNode, TypeString> typeDocEntry = entry.getValue();
              final TypeString docLoopTypeString = typeDocEntry.getValue();
              final TypeString docLookup =
                  docLoopTypeString.isVariadic()
                      ? docLoopTypeString.getVariadicInner()
                      : docLoopTypeString;
              final ExemplarDefinition docExemplarDef = resolver.getExemplarDefinition(docLookup);

              if (Objects.equals(iterExemplarDef, docExemplarDef)) {
                return;
              }

              final String message =
                  MESSAGE.formatted(
                      docLoopTypeString.getFullString(), iterTypeString.getFullString());
              final AstNode loopTypeNode = typeDocEntry.getKey();
              final AstNode typeValueNode = loopTypeNode.getFirstChild(TypeDocGrammar.TYPE_VALUE);
              this.addIssue(typeValueNode, message);
            });
  }

  private void reportMissingLoopDoc(final AstNode node) {
    final ExpressionResultString iterResultString = this.extractReasonedIterResult(node);
    final AstNode nameNode = this.getCallableNameNode(node);
    final AstNode issueNode = Objects.requireNonNullElse(nameNode, node);
    if (iterResultString.isEmpty()
        || iterResultString.equals(ExpressionResultString.UNDEFINED)
        || iterResultString.stream().allMatch(TypeString::containsUndefined)) {
      this.addIssue(issueNode, MISSING_LOOP_MESSAGE_UNKNOWN);
    } else {
      this.addIssue(issueNode, MISSING_LOOP_MESSAGE.formatted(iterResultString.getTypeNames(", ")));
    }
  }

  private AstNode getCallableNameNode(final AstNode node) {
    if (node.is(MagikGrammar.METHOD_DEFINITION)) {
      final MethodDefinitionNodeHelper helper = new MethodDefinitionNodeHelper(node);
      return helper.getMethodNameNode();
    }

    final ProcedureDefinitionNodeHelper helper = new ProcedureDefinitionNodeHelper(node);
    return helper.getProcedureNode();
  }

  private void reportNonIterLoopTypeDocIssue(final AstNode loopTypeNode) {
    final AstNode typeValueNode = loopTypeNode.getFirstChild(TypeDocGrammar.TYPE_VALUE);
    final AstNode issueNode = typeValueNode != null ? typeValueNode : loopTypeNode;
    this.addIssue(issueNode, NON_ITER_MESSAGE);
  }

  private ExpressionResultString extractReasonedIterResult(final AstNode node) {
    final LocalTypeReasonerState reasonerState = this.getTypeReasonerState();
    return reasonerState.getNodeIterType(node);
  }

  private Map<AstNode, TypeString> extractCallableDocLoopResult(final AstNode node) {
    final TypeDocParser docParser = new TypeDocParser(node);
    return docParser.getLoopTypeNodes();
  }

  private boolean isIterCallable(final AstNode node) {
    if (node.is(MagikGrammar.METHOD_DEFINITION)) {
      final MethodDefinitionNodeHelper helper = new MethodDefinitionNodeHelper(node);
      return helper.isIterMethod();
    }

    final ProcedureDefinitionNodeHelper helper = new ProcedureDefinitionNodeHelper(node);
    return helper.isIterProc();
  }

  private boolean isAbstractCallable(final AstNode node) {
    if (node.is(MagikGrammar.METHOD_DEFINITION)) {
      final MethodDefinitionNodeHelper helper = new MethodDefinitionNodeHelper(node);
      return helper.isAbstractMethod();
    }

    return false;
  }
}
