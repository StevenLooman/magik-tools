package nl.ramsolutions.sw.magik.analysis.typing.reasoner;

import com.sonar.sslr.api.AstNode;
import java.util.List;
import nl.ramsolutions.sw.magik.analysis.definitions.SlotDefinition;
import nl.ramsolutions.sw.magik.analysis.helpers.ContinueLeaveStatementNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.api.MagikNumberParser;

/** Atom handler. */
class AtomHandler extends LocalTypeReasonerHandler {

  @SuppressWarnings("checkstyle:MagicNumber")
  private static final long BIGNUM_START = 1 << 29;

  /**
   * Constructor.
   *
   * @param state Reasoner state.
   */
  AtomHandler(final LocalTypeReasonerState state) {
    super(state);
  }

  /**
   * Handle ATOM. For the parens-wrapped form {@code (EXPRESSION)} the ATOM has no dedicated
   * child-kind handler that would set its type, so propagate the inner expression's type up to the
   * ATOM here. Other ATOM forms (NUMBER, SYMBOL, BLOCK, LOOP, ...) have their own walkPost handlers
   * that already set the ATOM's type via {@code assignAtom}; for those the inner EXPRESSION lookup
   * is null and this method is a no-op.
   *
   * @param node ATOM node.
   */
  void handleAtom(final AstNode node) {
    final AstNode expressionNode = node.getFirstChild(MagikGrammar.EXPRESSION);
    if (expressionNode == null) {
      return;
    }

    final ExpressionResultString innerResult = this.state.getNodeType(expressionNode);
    if (this.state.hasNodeType(expressionNode) && innerResult != ExpressionResultString.UNDEFINED) {
      this.state.setNodeType(node, innerResult);
    }

    final ExpressionResultString innerIterResult = this.state.getNodeIterType(expressionNode);
    if (this.state.hasNodeIterType(expressionNode)
        && innerIterResult != ExpressionResultString.UNDEFINED) {
      this.state.setNodeIterType(node, innerIterResult);
    }
  }

  /**
   * Handle number.
   *
   * @param node NUMBER node.
   */
  void handleNumber(final AstNode node) {
    final String tokenValue = node.getTokenValue();
    final Number number = MagikNumberParser.parseMagikNumberSafe(tokenValue);
    if (number instanceof final Integer numberInt) {
      if (numberInt < AtomHandler.BIGNUM_START) {
        this.assignAtom(node, TypeString.SW_INTEGER);
      } else {
        this.assignAtom(node, TypeString.SW_BIGNUM);
      }
    } else if (number instanceof Long) {
      this.assignAtom(node, TypeString.SW_BIGNUM);
    } else if (number instanceof Double) {
      this.assignAtom(node, TypeString.SW_FLOAT);
    }
  }

  /**
   * Handle self.
   *
   * @param node SELF node.
   */
  void handleSelf(final AstNode node) {
    this.assignAtom(node, TypeString.SELF);
  }

  /**
   * Handle private.
   *
   * @param node PRIVATE node.
   */
  void handlePrivate(final AstNode node) {
    final ExpressionResultString resultStr = new ExpressionResultString(TypeString.PRIVATE);
    // TODO: Should use assignAtom?
    this.state.setNodeType(node, resultStr);
  }

  /**
   * Handle clone.
   *
   * @param node CLONE node.
   */
  void handleClone(final AstNode node) {
    this.handleSelf(node);
  }

  /**
   * Handle true/false.
   *
   * @param node TRUE/FALSE node.
   */
  void handleFalse(final AstNode node) {
    this.assignAtom(node, TypeString.SW_FALSE);
  }

  /**
   * Handle maybe.
   *
   * @param node MAYBE node.
   */
  void handleMaybe(final AstNode node) {
    this.assignAtom(node, TypeString.SW_MAYBE);
  }

  /**
   * Handle unset.
   *
   * @param node UNSET node.
   */
  void handleUnset(final AstNode node) {
    this.assignAtom(node, TypeString.SW_UNSET);
  }

  /**
   * Handle character.
   *
   * @param node CHARACTER node.
   */
  void handleCharacter(final AstNode node) {
    this.assignAtom(node, TypeString.SW_CHARACTER);
  }

  /**
   * Handle regexp.
   *
   * @param node SW_REGEXP node.
   */
  void handleRegexp(final AstNode node) {
    this.assignAtom(node, TypeString.SW_SW_REGEXP);
  }

  /**
   * Handle string.
   *
   * @param node STRING node.
   */
  void handleString(final AstNode node) {
    this.assignAtom(node, TypeString.SW_CHAR16_VECTOR_WITH_GENERICS);
  }

  /**
   * Handle symbol.
   *
   * @param node SYMBOL node.
   */
  void handleSymbol(final AstNode node) {
    this.assignAtom(node, TypeString.SW_SYMBOL);
  }

  /**
   * Handle simple vector.
   *
   * @param node SIMPLE_VECTOR node.
   */
  void handleSimpleVector(final AstNode node) {
    // Find all child expression types, and use that for generic <E>.
    final List<TypeString> containedTypes =
        node.getChildren(MagikGrammar.EXPRESSION).stream()
            .map(this.state::getNodeType)
            .map(result -> result.get(0, TypeString.UNDEFINED))
            .toList();
    if (!containedTypes.isEmpty()) {
      final TypeString combinedTypeStr =
          TypeString.combine(containedTypes.toArray(TypeString[]::new));

      final TypeString genericsTypeString =
          TypeString.ofIdentifier(
              TypeString.SW_SIMPLE_VECTOR.getIdentifier(),
              TypeString.SW_SIMPLE_VECTOR.getPakkage(),
              TypeString.ofGenericDefinition("E", combinedTypeStr));
      this.assignAtom(node, genericsTypeString);
      return;
    }

    this.assignAtom(node, TypeString.SW_SIMPLE_VECTOR);
  }

  /**
   * Handle global reference.
   *
   * @param node GLOBAL_REF node.
   */
  void handleGlobalRef(final AstNode node) {
    this.assignAtom(node, TypeString.SW_GLOBAL_VARIABLE);
  }

  /**
   * Handle thisthread.
   *
   * @param node THISTHREAD node.
   */
  void handleThread(final AstNode node) {
    final TypeString threadTypeStr =
        TypeString.combine(TypeString.SW_HEAVY_THREAD, TypeString.SW_LIGHT_THREAD);
    this.assignAtom(node, threadTypeStr);
  }

  /**
   * Handle slot.
   *
   * @param node SLOT node.
   */
  void handleSlot(final AstNode node) {
    // Get class type.
    final TypeString ownerTypeStr = this.getMethodOwnerType(node);
    if (ownerTypeStr == TypeString.UNDEFINED) {
      return;
    }

    // Get slot type.
    final AstNode identifierNode = node.getFirstChild(MagikGrammar.IDENTIFIER);
    final String slotName = identifierNode.getTokenValue();
    final TypeString slotTypeStr =
        this.typeResolver.getSlotDefinitions(ownerTypeStr).stream()
            .filter(slotDef -> slotDef.getName().equals(slotName))
            .map(SlotDefinition::getTypeName)
            .findAny()
            .orElse(TypeString.UNDEFINED);
    this.assignAtom(node, slotTypeStr);
  }

  /**
   * Handle block.
   *
   * @param node BLOCK node.
   */
  void handleBlock(final AstNode node) {
    final AstNode bodyNode = node.getFirstChild(MagikGrammar.BODY);
    if (bodyNode == null) {
      return;
    }

    // Collect types from EMIT_STATEMENT nodes directly in this block's body.
    node.getDescendants(MagikGrammar.EMIT_STATEMENT).stream()
        .filter(emitNode -> emitNode.getFirstAncestor(MagikGrammar.BODY).equals(bodyNode))
        .map(this.state::getNodeType)
        .forEach(result -> this.addNodeType(node, result));

    // Collect types from LEAVE_STATEMENT nodes targeting this block.
    node.getDescendants(MagikGrammar.LEAVE_STATEMENT).stream()
        .filter(
            leaveNode -> {
              final ContinueLeaveStatementNodeHelper helper =
                  new ContinueLeaveStatementNodeHelper(leaveNode);
              return helper.getRelatedBodyNode().equals(bodyNode);
            })
        .map(this.state::getNodeType)
        .forEach(result -> this.addNodeType(node, result));

    // Propagate to parent ATOM node.
    if (this.state.hasNodeType(node)) {
      this.assignAtom(node, this.state.getNodeType(node));
    }
  }

  /**
   * Handle loop.
   *
   * @param node LOOP node.
   */
  void handleLoop(final AstNode node) {
    final AstNode atomNode = node.getFirstAncestor(MagikGrammar.ATOM);

    // no leave, no finally, then 1: Assign iter call result to loop
    //    leave,          *, then 2: Assign leave tuple to loop
    // no leave,    finally, no leave, then 3: Assign empty to loop
    // no leave,    finally,    leave, then 4: Assign leave tuple to loop

    final AstNode loopBodyNode = node.getFirstChild(MagikGrammar.BODY);
    if (loopBodyNode == null) {
      return;
    }

    // Collect leave statements targeting this loop's body.
    final List<AstNode> loopLeaveStatements =
        loopBodyNode.getDescendants(MagikGrammar.LEAVE_STATEMENT).stream()
            .filter(
                leaveNode -> {
                  final ContinueLeaveStatementNodeHelper helper =
                      new ContinueLeaveStatementNodeHelper(leaveNode);
                  return helper.getRelatedBodyNode().equals(loopBodyNode);
                })
            .toList();
    final AstNode finallyNode = node.getFirstChild(MagikGrammar.FINALLY);
    if (loopLeaveStatements.isEmpty() && finallyNode == null) {
      // 1: No leaves and no finally: assign iter call result.
      final AstNode overNode = node.getParent();
      if (overNode.is(MagikGrammar.OVER)) {
        final AstNode iterableExpressionNode =
            overNode.getFirstChild(MagikGrammar.ITERABLE_EXPRESSION);
        final AstNode iterableExpressionExpressionNode =
            iterableExpressionNode.getFirstChild(MagikGrammar.EXPRESSION);
        final ExpressionResultString callResult =
            this.state.getNodeType(iterableExpressionExpressionNode);
        this.addNodeType(atomNode, callResult);
      }
    } else if (!loopLeaveStatements.isEmpty()) {
      // 2: Has leaves: collect leave types.
      loopLeaveStatements.stream()
          .map(this.state::getNodeType)
          .forEach(result -> this.addNodeType(atomNode, result));

      // Check if any leaves are guaranteed to execute (direct children of the loop body).
      // If all leaves are inside nested constructs (e.g., _if), the loop might complete
      // without hitting a leave, so include EMPTY to account for the sw:unset possibility.
      final boolean hasGuaranteedLeave =
          loopLeaveStatements.stream()
              .anyMatch(
                  leaveNode -> leaveNode.getFirstAncestor(MagikGrammar.BODY).equals(loopBodyNode));
      if (!hasGuaranteedLeave) {
        this.addNodeType(atomNode, ExpressionResultString.EMPTY);
      }
    } else if (finallyNode != null) {
      final AstNode finallyBodyNode = finallyNode.getFirstChild(MagikGrammar.BODY);
      // Collect leave statements targeting this loop from within the finally block.
      final List<AstNode> finallyLeaveStatements =
          finallyBodyNode.getDescendants(MagikGrammar.LEAVE_STATEMENT).stream()
              .filter(
                  leaveNode -> {
                    final ContinueLeaveStatementNodeHelper helper =
                        new ContinueLeaveStatementNodeHelper(leaveNode);
                    return helper.getRelatedBodyNode().equals(loopBodyNode);
                  })
              .toList();
      if (finallyLeaveStatements.isEmpty()) {
        // 3: Finally without leaves: assign empty.
        this.addNodeType(atomNode, ExpressionResultString.EMPTY);
      } else {
        // 4: Finally with leaves: collect leave types.
        finallyLeaveStatements.stream()
            .map(this.state::getNodeType)
            .forEach(result -> this.addNodeType(atomNode, result));
      }
    }
  }

  /**
   * Handle if expression.
   *
   * @param node IF node.
   */
  void handleIf(final AstNode node) {
    this.collectBodyEmits(node);
  }

  /**
   * Handle protect expression.
   *
   * @param node PROTECT node.
   */
  void handleProtect(final AstNode node) {
    this.collectBodyEmits(node);
  }

  /**
   * Handle try expression.
   *
   * @param node TRY node.
   */
  void handleTry(final AstNode node) {
    this.collectBodyEmits(node);
  }

  /**
   * Handle catch expression.
   *
   * @param node CATCH node.
   */
  void handleCatch(final AstNode node) {
    this.collectBodyEmits(node);
  }

  /**
   * Handle lock expression.
   *
   * @param node LOCK node.
   */
  void handleLock(final AstNode node) {
    this.collectBodyEmits(node);
  }

  /**
   * Collect emit results from body descendants of a body-containing atom (IF, PROTECT, TRY, CATCH,
   * LOCK). Emits in nested blocks/loops are excluded as those constructs handle their own emits.
   *
   * <p>The filter checks that the emit's first ancestor BODY belongs directly to this node (at most
   * one intermediate node like ELIF, ELSE, PROTECTION, WHEN between the BODY and this node).
   *
   * @param node The body-containing atom node.
   */
  private void collectBodyEmits(final AstNode node) {
    node.getDescendants(MagikGrammar.EMIT_STATEMENT).stream()
        .filter(
            emitNode -> {
              final AstNode bodyNode = emitNode.getFirstAncestor(MagikGrammar.BODY);
              final AstNode bodyParent = bodyNode.getParent();
              return bodyParent.equals(node)
                  || (bodyParent.getParent() != null && bodyParent.getParent().equals(node));
            })
        .map(this.state::getNodeType)
        .forEach(result -> this.addNodeType(node, result));

    // Propagate to parent ATOM node.
    if (this.state.hasNodeType(node)) {
      this.assignAtom(node, this.state.getNodeType(node));
    }
  }
}
