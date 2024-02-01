package nl.ramsolutions.sw.magik.checks.checks;

import com.sonar.sslr.api.AstNode;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import org.sonar.check.Rule;

/**
 * Check for duplicate method definitions in file.
 */
@Rule(key = DuplicateMethodInFileCheck.CHECK_KEY)
public class DuplicateMethodInFileCheck extends MagikCheck {

    @SuppressWarnings("checkstyle:JavadocVariable")
    public static final String CHECK_KEY = "DuplicateMethodInFile";

    private static final String MESSAGE = "Duplicate method definition in file.";

    @Override
    protected void walkPostMagik(final AstNode node) {
        // Test for duplicates.
        this.getMagikFile().getDefinitions().stream()
            .filter(MethodDefinition.class::isInstance)
            .map(MethodDefinition.class::cast)
            .collect(Collectors.groupingBy(MethodDefinition::getName))
            .entrySet().stream()
            .filter(entry -> entry.getValue().size() > 1)
            .flatMap(entry -> entry.getValue().stream())
            .forEach(definition -> {
                final AstNode definitionNode = definition.getNode();
                final MethodDefinitionNodeHelper helper = new MethodDefinitionNodeHelper(definitionNode);
                final AstNode methodNameNode = helper.getMethodNameNode();
                this.addIssue(methodNameNode, MESSAGE);
            });
    }

}
