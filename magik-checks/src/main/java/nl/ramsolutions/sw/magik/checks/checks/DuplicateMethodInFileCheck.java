package nl.ramsolutions.sw.magik.checks.checks;

import com.sonar.sslr.api.AstNode;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import nl.ramsolutions.sw.magik.analysis.definitions.Definition;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.parsers.MethodDefinitionParser;
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

    private static final String MESSAGE = "Duplicate method definition in this file.";

    private final Set<MethodDefinition> methodDefinitions = new HashSet<>();

    @Override
    protected void walkPostMethodDefinition(final AstNode node) {
        final MethodDefinitionParser parser = new MethodDefinitionParser(node);
        final List<Definition> parsedDefinitions = parser.parseDefinitions();
        if (parsedDefinitions.isEmpty()) {
            // How does this happen?
            return;
        }
        final MethodDefinition methodDefinition = (MethodDefinition) parsedDefinitions.get(0);
        this.methodDefinitions.add(methodDefinition);
    }

    @Override
    protected void walkPostMagik(final AstNode node) {
        // Test for duplicates.
        final Map<String, List<MethodDefinition>> definitions = this.methodDefinitions.stream()
            .collect(Collectors.toMap(
                MethodDefinition::getName,
                List::of,
                (list1, list2) -> Stream.concat(
                        list1.stream(),
                        list2.stream())
                    .collect(Collectors.toList())));
        definitions.entrySet().stream()
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
