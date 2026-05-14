package nl.ramsolutions.sw.checks.magiktyped;

import com.sonar.sslr.api.AstNode;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import nl.ramsolutions.sw.checks.MagikTypedCheck;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.ProcedureDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeStringResolver;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasonerState;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.api.TypeDocGrammar;
import nl.ramsolutions.sw.magik.parser.TypeDocParser;
import org.sonar.check.Rule;

/** Check if @return types from doc match reasoned return types. */
@Rule(key = CallableReturnTypesMatchDocTypedCheck.CHECK_KEY)
public class CallableReturnTypesMatchDocTypedCheck extends MagikTypedCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "CallableReturnTypesMatchDoc";

  private static final String MESSAGE_MISMATCH =
      "@return type(s) (%s) do not match callable return type(s) (%s).";
  private static final String MESSAGE_MISSING_TYPE_DOC =
      "@return type(s) missing for type(s) (%s).";
  private static final String MESSAGE_MISSING_TYPE_DOC_UNKNOWN =
      "@return type missing for a return value with unknown type.";
  private static final String MESSAGE_UNVERIFIABLE_RETURN_DOC =
      "@return type(s) cannot be verified: callable returns values of unknown type.";
  private static final String MESSAGE_UNEXPECTED_RETURN_DOC =
      "@return specified, but callable returns no value.";

  @Override
  protected void walkPostMethodDefinition(final AstNode node) {
    this.checkCallableReturnDoc(node);
  }

  @Override
  protected void walkPostProcedureDefinition(final AstNode node) {
    this.checkCallableReturnDoc(node);
  }

  private void checkCallableReturnDoc(final AstNode node) {
    if (this.isAbstractCallable(node)) {
      return;
    }

    final ExpressionResultString reasonedResult = this.extractReasonedResult(node);
    final List<Map.Entry<AstNode, TypeString>> typeDocEntries = this.extractCallableDocResult(node);

    // Determine arity. When arity is known from syntax the slot is real even if its type is
    // UNDEFINED, so we must still report missing @return doc for it. When arity comes from the
    // reasoned result it may be inflated (up to 1024 for UNDEFINED results), so we skip UNDEFINED
    // slots there and let UndefinedMethodCallResultTypedCheck report the unknown call.
    final Integer syntaxReturnCount = this.extractReturnCountFromSyntax(node);
    final int returnCount;
    if (syntaxReturnCount != null) {
      returnCount = syntaxReturnCount;
    } else if (reasonedResult.equals(ExpressionResultString.UNDEFINED)) {
      // Arity is unknown because the return is from an undefined method call.
      // If @return doc is present it cannot be verified; flag each entry.
      // Without doc, defer entirely to UndefinedMethodCallResultTypedCheck.
      typeDocEntries.forEach(
          entry -> {
            final AstNode typeValueNode = entry.getKey().getFirstChild(TypeDocGrammar.TYPE_VALUE);
            this.addIssue(typeValueNode, MESSAGE_UNVERIFIABLE_RETURN_DOC);
          });
      return;
    } else {
      returnCount = reasonedResult.size();
    }

    final TypeStringResolver resolver = this.getTypeStringResolver();
    final int docSize = typeDocEntries.size();
    final boolean tailVariadic =
        docSize > 0 && typeDocEntries.get(docSize - 1).getValue().isVariadic();
    final int leadingDocCount = tailVariadic ? docSize - 1 : docSize;
    final int effectiveDocCount = tailVariadic ? Math.max(leadingDocCount, returnCount) : docSize;
    final int entryCount = Math.max(returnCount, effectiveDocCount);
    for (int index = 0; index < entryCount; ++index) {
      this.checkReturnPosition(
          node,
          resolver,
          typeDocEntries,
          reasonedResult,
          returnCount,
          syntaxReturnCount,
          tailVariadic,
          leadingDocCount,
          index);
    }
  }

  @SuppressWarnings("checkstyle:ParameterNumber")
  private void checkReturnPosition(
      final AstNode node,
      final TypeStringResolver resolver,
      final List<Map.Entry<AstNode, TypeString>> typeDocEntries,
      final ExpressionResultString reasonedResult,
      final int returnCount,
      final Integer syntaxReturnCount,
      final boolean tailVariadic,
      final int leadingDocCount,
      final int index) {
    final TypeString callableReturnTypeString =
        index < returnCount ? reasonedResult.get(index, null) : null;
    // When arity came from the reasoned result, skip UNDEFINED return positions to prevent
    // false positives from inflated result sizes. UndefinedMethodCallResultTypedCheck covers
    // those calls.
    if (syntaxReturnCount == null
        && callableReturnTypeString != null
        && callableReturnTypeString.containsUndefined()) {
      return;
    }

    final Map.Entry<AstNode, TypeString> typeDocEntry =
        this.selectDocEntry(typeDocEntries, index, tailVariadic, leadingDocCount);
    final TypeString effectiveDocType =
        this.effectiveDocType(
            typeDocEntry, callableReturnTypeString, tailVariadic, index, leadingDocCount);
    this.handleReturnTypeEntry(
        callableReturnTypeString, typeDocEntry, effectiveDocType, node, resolver);
  }

  private Map.Entry<AstNode, TypeString> selectDocEntry(
      final List<Map.Entry<AstNode, TypeString>> typeDocEntries,
      final int index,
      final boolean tailVariadic,
      final int leadingDocCount) {
    if (tailVariadic && index >= leadingDocCount) {
      return typeDocEntries.get(typeDocEntries.size() - 1);
    }
    return index < typeDocEntries.size() ? typeDocEntries.get(index) : null;
  }

  private TypeString effectiveDocType(
      final Map.Entry<AstNode, TypeString> typeDocEntry,
      final TypeString callableReturnTypeString,
      final boolean tailVariadic,
      final int index,
      final int leadingDocCount) {
    if (typeDocEntry == null) {
      return null;
    }
    final TypeString docType = typeDocEntry.getValue();
    if (!tailVariadic || index < leadingDocCount) {
      return docType;
    }
    // Absorbed into the trailing variadic. If the reasoned position is itself variadic
    // compare against the full variadic; otherwise compare against its inner.
    if (callableReturnTypeString != null && callableReturnTypeString.isVariadic()) {
      return docType;
    }
    return docType.getVariadicInner();
  }

  private void handleReturnTypeEntry(
      final TypeString callableReturnTypeString,
      final Map.Entry<AstNode, TypeString> typeDocEntry,
      final TypeString effectiveDocType,
      final AstNode definitionNode,
      final TypeStringResolver resolver) {
    if (this.reportMissingTypeDoc(callableReturnTypeString, effectiveDocType, definitionNode)) {
      return;
    }

    if (typeDocEntry == null) {
      return;
    }

    this.reportUnexpectedOrMismatchedTypeDoc(
        callableReturnTypeString, effectiveDocType, typeDocEntry, resolver);
  }

  private boolean reportMissingTypeDoc(
      final TypeString callableReturnTypeString,
      final TypeString docReturnTypeString,
      final AstNode definitionNode) {
    if (docReturnTypeString != null || callableReturnTypeString == null) {
      return false;
    }

    final String message;
    if (callableReturnTypeString.containsUndefined()) {
      message = MESSAGE_MISSING_TYPE_DOC_UNKNOWN;
    } else {
      message = MESSAGE_MISSING_TYPE_DOC.formatted(callableReturnTypeString.getFullString());
    }
    final AstNode definitionNameNode = this.getCallableNameNode(definitionNode);
    final AstNode issueNode = Objects.requireNonNullElse(definitionNameNode, definitionNode);
    this.addIssue(issueNode, message);
    return true;
  }

  private void reportUnexpectedOrMismatchedTypeDoc(
      final TypeString callableReturnTypeString,
      final TypeString docReturnTypeString,
      final Map.Entry<AstNode, TypeString> typeDocEntry,
      final TypeStringResolver resolver) {
    final AstNode returnTypeNode = typeDocEntry.getKey();
    final AstNode typeValueNode = returnTypeNode.getFirstChild(TypeDocGrammar.TYPE_VALUE);
    if (callableReturnTypeString == null && docReturnTypeString != null) {
      this.addIssue(typeValueNode, MESSAGE_UNEXPECTED_RETURN_DOC);
      return;
    }

    if (callableReturnTypeString == null || docReturnTypeString == null) {
      return;
    }

    if (callableReturnTypeString.containsUndefined()) {
      return;
    }

    if (Objects.equals(
        resolver.resolve(callableReturnTypeString), resolver.resolve(docReturnTypeString))) {
      return;
    }

    final String message =
        MESSAGE_MISMATCH.formatted(
            docReturnTypeString.getFullString(), callableReturnTypeString.getFullString());
    this.addIssue(typeValueNode, message);
  }

  private ExpressionResultString extractReasonedResult(final AstNode node) {
    final LocalTypeReasonerState reasonerState = this.getTypeReasonerState();
    return reasonerState.getNodeType(node);
  }

  private List<Map.Entry<AstNode, TypeString>> extractCallableDocResult(final AstNode node) {
    final TypeDocParser docParser = new TypeDocParser(node);
    return List.copyOf(docParser.getReturnTypeNodes().entrySet());
  }

  private boolean isAbstractCallable(final AstNode node) {
    if (node.is(MagikGrammar.METHOD_DEFINITION)) {
      final MethodDefinitionNodeHelper helper = new MethodDefinitionNodeHelper(node);
      return helper.isAbstractMethod();
    }

    return false;
  }

  private AstNode getCallableNameNode(final AstNode node) {
    if (node.is(MagikGrammar.METHOD_DEFINITION)) {
      final MethodDefinitionNodeHelper helper = new MethodDefinitionNodeHelper(node);
      return helper.getMethodNameNode();
    }

    final ProcedureDefinitionNodeHelper helper = new ProcedureDefinitionNodeHelper(node);
    return helper.getProcedureNode();
  }

  private Integer extractReturnCountFromSyntax(final AstNode methodNode) {
    Integer returnCount = null;
    for (final AstNode returnNode : methodNode.getDescendants(MagikGrammar.RETURN_STATEMENT)) {
      final AstNode enclosingDefinition =
          returnNode.getFirstAncestor(
              MagikGrammar.METHOD_DEFINITION, MagikGrammar.PROCEDURE_DEFINITION);
      if (!methodNode.equals(enclosingDefinition)) {
        continue;
      }

      final Integer statementReturnCount = this.extractReturnCountFromReturnStatement(returnNode);
      returnCount = this.mergeReturnCounts(returnCount, statementReturnCount);
      if (returnCount == null) {
        return null;
      }
    }

    final AstNode callableBody = methodNode.getFirstChild(MagikGrammar.BODY);
    for (final AstNode emitNode : methodNode.getDescendants(MagikGrammar.EMIT_STATEMENT)) {
      if (!this.isEmitInCallableBody(callableBody, emitNode)) {
        continue;
      }

      final Integer statementReturnCount = this.extractReturnCountFromEmitStatement(emitNode);
      returnCount = this.mergeReturnCounts(returnCount, statementReturnCount);
      if (returnCount == null) {
        return null;
      }
    }

    return returnCount != null ? returnCount : 0;
  }

  private boolean isEmitInCallableBody(final AstNode callableBody, final AstNode emitNode) {
    return callableBody != null
        && callableBody.equals(emitNode.getFirstAncestor(MagikGrammar.BODY));
  }

  private Integer mergeReturnCounts(final Integer currentCount, final Integer statementCount) {
    if (statementCount == null) {
      return null;
    }

    if (currentCount == null) {
      return statementCount;
    }

    return currentCount.equals(statementCount) ? currentCount : null;
  }

  private Integer extractReturnCountFromReturnStatement(final AstNode returnNode) {
    final AstNode tupleNode = returnNode.getFirstChild(MagikGrammar.TUPLE);
    if (tupleNode == null) {
      return 0;
    }

    return this.extractReturnCountFromTuple(tupleNode);
  }

  private Integer extractReturnCountFromEmitStatement(final AstNode emitNode) {
    final AstNode tupleNode = emitNode.getFirstChild(MagikGrammar.TUPLE);
    if (tupleNode == null) {
      return 0;
    }

    return this.extractReturnCountFromTuple(tupleNode);
  }

  private Integer extractReturnCountFromTuple(final AstNode tupleNode) {

    final List<AstNode> expressionNodes = tupleNode.getChildren(MagikGrammar.EXPRESSION);
    if (expressionNodes.size() != 1) {
      return expressionNodes.size();
    }

    final AstNode expressionNode = expressionNodes.get(0);
    return this.isDefinitelySingleValueExpression(expressionNode) ? 1 : null;
  }

  private boolean isDefinitelySingleValueExpression(final AstNode expressionNode) {
    return expressionNode
        .getDescendants(MagikGrammar.METHOD_INVOCATION, MagikGrammar.PROCEDURE_INVOCATION)
        .isEmpty();
  }
}
