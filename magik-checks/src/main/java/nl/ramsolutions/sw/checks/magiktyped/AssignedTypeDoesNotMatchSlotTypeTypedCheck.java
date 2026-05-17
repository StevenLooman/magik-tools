package nl.ramsolutions.sw.checks.magiktyped;

import com.sonar.sslr.api.AstNode;
import java.util.Collections;
import java.util.List;
import nl.ramsolutions.sw.checks.MagikTypedCheck;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeStringResolver;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasonerState;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import org.sonar.check.Rule;

/** Check if assigned type matches slot type. */
@Rule(key = AssignedTypeDoesNotMatchSlotTypeTypedCheck.CHECK_KEY)
public class AssignedTypeDoesNotMatchSlotTypeTypedCheck extends MagikTypedCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "AssignedTypeDoesNotMatchSlotType";

  private static final String MESSAGE = "Slot type (%s) does not match assigned type (%s)";

  @Override
  protected void walkPostAssignmentExpression(final AstNode node) {
    // Ensure we are in a method definition.
    final AstNode methodDefinitionNode = node.getFirstAncestor(MagikGrammar.METHOD_DEFINITION);
    if (methodDefinitionNode == null) {
      return;
    }

    final MethodDefinitionNodeHelper helper = new MethodDefinitionNodeHelper(methodDefinitionNode);

    // Get assigned type.
    final LocalTypeReasonerState state = this.getTypeReasonerState();
    final AstNode rightNode = node.getLastChild();
    final ExpressionResultString assignedResult = state.getNodeType(rightNode);
    final TypeString assignedType = assignedResult.get(0, TypeString.UNDEFINED);

    // Check all assigned nodes.
    final TypeString exemplarTypeStr = helper.getTypeString();
    final TypeStringResolver resolver = this.getTypeStringResolver();
    final List<AstNode> assignedNodes = node.getChildren(MagikGrammar.values());
    assignedNodes.remove(rightNode);
    Collections.reverse(assignedNodes);
    for (final AstNode assignedNode : assignedNodes) {
      // Test if this is a slot assignment.
      final AstNode slotNode = assignedNode.getFirstChild(MagikGrammar.SLOT);
      if (slotNode == null) {
        return;
      }

      final AstNode slotIdentifierNode = slotNode.getFirstChild(MagikGrammar.IDENTIFIER);
      if (slotIdentifierNode == null) {
        return; // Handle malformed slot definition
      }

      final String slotName = slotIdentifierNode.getTokenValue();
      resolver.getSlotDefinitions(exemplarTypeStr).stream()
          .filter(slotDef -> slotDef.getName().equals(slotName))
          .forEach(
              slotDef -> {
                final TypeString slotType = slotDef.getTypeName();
                if (slotType.isUndefined()) {
                  // Cannot check type.
                  return;
                }

                // Check if types match.
                if (resolver.isKindOf(assignedType, slotType)) {
                  return;
                }

                final String slotTypeFullString = slotType.getFullString();
                final String assignedTypeFullString = assignedType.getFullString();
                final String message =
                    MESSAGE.formatted(slotTypeFullString, assignedTypeFullString);
                this.addIssue(slotNode, message);
              });
    }
  }
}
