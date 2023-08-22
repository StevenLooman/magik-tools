package nl.ramsolutions.sw.magik.checks.checks;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.ProcedureDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.metrics.ComplexityVisitor;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;

/**
 * Check cyclomatic complexity of method.
 */
@Rule(key = MethodComplexityCheck.CHECK_KEY)
public class MethodComplexityCheck extends MagikCheck {

    @SuppressWarnings("checkstyle:JavadocVariable")
    public static final String CHECK_KEY = "MethodComplexity";

    private static final int DEFAULT_MAXIMUM_COMPLEXITY = 10;
    private static final String MESSAGE = "Method has a complexity greater than permitted (%s/%s).";

    /**
     * Maximum complexity of method by the McCabe definition.
     */
    @RuleProperty(
        key = "maximum complexity",
        defaultValue = "" + DEFAULT_MAXIMUM_COMPLEXITY,
        description = "Maximum complexity of method by the McCabe definition",
        type = "INTEGER")
    @SuppressWarnings("checkstyle:VisibilityModifier")
    public int maximumComplexity = DEFAULT_MAXIMUM_COMPLEXITY;

    @Override
    protected void walkPostMethodDefinition(final AstNode node) {
        this.checkDefinition(node);
    }

    @Override
    protected void walkPostProcedureDefinition(final AstNode node) {
        this.checkDefinition(node);
    }

    private void checkDefinition(final AstNode node) {
        final ComplexityVisitor visitor = new ComplexityVisitor();
        visitor.walkAst(node);

        final int complexity = visitor.getComplexity();
        if (complexity > this.maximumComplexity) {
            final String message = String.format(MESSAGE, complexity, this.maximumComplexity);
            final AstNode markedNode;
            if (node.is(MagikGrammar.METHOD_DEFINITION)) {
                final MethodDefinitionNodeHelper helper = new MethodDefinitionNodeHelper(node);
                markedNode = helper.getMethodNameNode();
            } else {
                final ProcedureDefinitionNodeHelper helper = new ProcedureDefinitionNodeHelper(node);
                markedNode = helper.getProcedureNode();
            }
            this.addIssue(markedNode, message);
        }
    }

}
