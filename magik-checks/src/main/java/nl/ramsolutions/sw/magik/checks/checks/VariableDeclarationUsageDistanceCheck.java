package nl.ramsolutions.sw.magik.checks.checks;

import com.sonar.sslr.api.AstNode;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.CheckForNull;
import nl.ramsolutions.sw.magik.analysis.scope.GlobalScope;
import nl.ramsolutions.sw.magik.analysis.scope.Scope;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;

/**
 * Check distance between variable declaration and usage.
 */
@Rule(key = VariableDeclarationUsageDistanceCheck.CHECK_KEY)
public class VariableDeclarationUsageDistanceCheck extends MagikCheck {

    @SuppressWarnings("checkstyle:JavadocVariable")
    public static final String CHECK_KEY = "VariableDeclarationUsageDistance";

    private static final int DEFAULT_MAX_DISTANCE = 5;
    private static final boolean DEFAULT_IGNORE_CONSTANTS = true;
    private static final String MESSAGE = "Distance of declared variable to first usage is too long (%s/%s).";

    /**
     * Maximum distance between declaration and usage.
     */
    @RuleProperty(
        key = "max distance",
        defaultValue = "" + DEFAULT_MAX_DISTANCE,
        description = "Maximum distance between declaration and usage",
        type = "INTEGER")
    @SuppressWarnings("checkstyle:VisibilityModifier")
    public int maxDistance = DEFAULT_MAX_DISTANCE;

    /**
     * Ignore declared constants.
     */
    @RuleProperty(
        key = "ignore constants",
        defaultValue = "" + DEFAULT_IGNORE_CONSTANTS,
        description = "Ignore declared constants",
        type = "BOOLEAN")
    @SuppressWarnings("checkstyle:VisibilityModifier")
    public boolean ignoreConstants = DEFAULT_IGNORE_CONSTANTS;

    private final Set<AstNode> seenNodes = new HashSet<>();

    @Override
    protected void walkPostMethodDefinition(final AstNode node) {
        this.clear();
    }

    @Override
    protected void walkPreIdentifier(final AstNode node) {
        // Only test variables usages, i.e., parent is an ATOM node.
        if (!this.isVariableUsage(node)) {
            return;
        }

        // Test only METHOD_DEFINITION/PROC_DEFINITION contents.
        if (!this.isProcedureOrMethodDefinition(node)) {
            return;
        }

        // Get the scope where it is used.
        final Scope scope = this.getScopeForNode(node);
        if (scope == null) {
            // No scope found.
            return;
        }

        // Only test variables.
        final String identifier = node.getTokenValue();
        final ScopeEntry entry = scope.getScopeEntry(identifier);
        if (entry == null) {
            return;
        }
        if (!entry.isType(ScopeEntry.Type.LOCAL, ScopeEntry.Type.DEFINITION, ScopeEntry.Type.CONSTANT)) {
            return;
        }

        // Ignore constants?
        if (this.ignoreConstants
            && entry.isType(ScopeEntry.Type.CONSTANT)) {
            return;
        }

        // Only test the first use.
        final AstNode declarationNode = entry.getNode();
        if (this.seenNodes.contains(declarationNode)) {
            return;
        }
        this.seenNodes.add(declarationNode);

        final int distance = this.distanceBetweenStatements(declarationNode, node);
        if (distance > this.maxDistance) {
            final String message = String.format(MESSAGE, distance, this.maxDistance);
            this.addIssue(node, message);
        }
    }

    private boolean isVariableUsage(final AstNode node) {
        return node.getParent().is(MagikGrammar.ATOM, MagikGrammar.METHOD_INVOCATION, MagikGrammar.PROCEDURE_INVOCATION)
            && !this.isLhsOfAssignment(node);
    }

    private boolean isProcedureOrMethodDefinition(final AstNode node) {
        return node.getFirstAncestor(MagikGrammar.METHOD_DEFINITION) != null
            || node.getFirstAncestor(MagikGrammar.PROCEDURE_DEFINITION) != null;
    }

    @CheckForNull
    private Scope getScopeForNode(final AstNode node) {
        final GlobalScope globalScope = this.getMagikFile().getGlobalScope();
        if (globalScope == null) {
            return null;
        }

        return globalScope.getScopeForNode(node);
    }

    @Override
    protected void walkPostMagik(final AstNode node) {
        this.clear();
    }

    private void clear() {
        this.seenNodes.clear();
    }

    private int distanceBetweenStatements(final AstNode declarationNode, final AstNode usageNode) {
        final AstNode wantedNode = declarationNode.getFirstAncestor(MagikGrammar.STATEMENT);
        int distance = 0;
        AstNode currentNode = usageNode.getFirstAncestor(MagikGrammar.STATEMENT);
        while (currentNode != null) {
            if (currentNode == wantedNode) {
                break;
            }

            distance += 1;

            AstNode previousNode = currentNode.getPreviousSibling();
            if (previousNode == null) {
                previousNode = currentNode.getFirstAncestor(MagikGrammar.STATEMENT);
            }
            currentNode = previousNode;
        }

        if (currentNode == null) {
            return -1;
        }

        return distance;
    }

    /**
     * Test if node is the Left Hand Side of an assignment.
     * @param node Node to test.
     * @return True if is LHS of assignment, false otherwise.
     */
    private boolean isLhsOfAssignment(final AstNode node) {
        final AstNode assignmentNode =
            node.getFirstAncestor(MagikGrammar.ASSIGNMENT_EXPRESSION, MagikGrammar.AUGMENTED_ASSIGNMENT_EXPRESSION);
        if (assignmentNode == null) {
            return false;
        }

        AstNode currentNode = node;
        while (currentNode != assignmentNode && currentNode != null) {
            if (currentNode == assignmentNode.getLastChild()) {
                return false;
            }

            currentNode = currentNode.getParent();
        }

        return true;
    }

}
