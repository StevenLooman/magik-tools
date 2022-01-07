package nl.ramsolutions.sw.magik.analysis.definitions;

import com.sonar.sslr.api.AstNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import nl.ramsolutions.sw.magik.analysis.helpers.ArgumentsNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.PackageNodeHelper;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.api.MagikOperator;

/**
 * {@code define_shared_variable()} parser.
 */
public class DefineSharedVariableParser {

    private static final String DEFINE_SHARED_VARIABLE = "define_shared_variable()";
    private static final String FLAVOR_PUBLIC = ":public";
    private static final String FLAVOR_READONLY = ":readonly";

    private final AstNode node;

    /**
     * Constructor.
     * @param node {@code define_shared_variable()} node.
     */
    public DefineSharedVariableParser(final AstNode node) {
        if (node.isNot(MagikGrammar.METHOD_INVOCATION)) {
            throw new IllegalArgumentException();
        }

        this.node = node;
    }

    /**
     * Test if node is a {@code define_shared_variable()}.
     * @param node Node to test
     * @return True if node is a {@code define_shared_variable()}, false otherwise.
     */
    public static boolean isDefineSharedVariable(final AstNode node) {
        final MethodInvocationNodeHelper helper = new MethodInvocationNodeHelper(node);
        if (!helper.isMethodInvocationOf(DEFINE_SHARED_VARIABLE)) {
            return false;
        }

        // Some sanity.
        final AstNode parentNode = node.getParent();
        final AstNode atomNode = parentNode.getFirstChild();
        if (atomNode.isNot(MagikGrammar.ATOM)) {
            return false;
        }
        final String exemplarName = atomNode.getTokenValue();    // Assume this is an exemplar.
        if (exemplarName == null) {
            return false;
        }

        final AstNode argumentsNode = node.getFirstChild(MagikGrammar.ARGUMENTS);
        final ArgumentsNodeHelper argumentsHelper = new ArgumentsNodeHelper(argumentsNode);
        final AstNode argument0Node = argumentsHelper.getArgument(0, MagikGrammar.SYMBOL);
        final AstNode argument2Node = argumentsHelper.getArgument(2, MagikGrammar.SYMBOL);
        return argument0Node != null
            && argument2Node != null;
    }

    /**
     * Parse defitions.
     * @return List of parsed definitions.
     */
    public List<Definition> parseDefinitions() {
        // Some sanity.
        final AstNode parentNode = node.getParent();
        final AstNode atomNode = parentNode.getFirstChild();
        if (atomNode.isNot(MagikGrammar.ATOM)) {
            throw new IllegalStateException();
        }
        final String exemplarName = atomNode.getTokenValue();    // Assume this is an exemplar.
        if (exemplarName == null) {
            throw new IllegalStateException();
        }

        final AstNode argumentsNode = node.getFirstChild(MagikGrammar.ARGUMENTS);
        final ArgumentsNodeHelper argumentsHelper = new ArgumentsNodeHelper(argumentsNode);
        final AstNode argument0Node = argumentsHelper.getArgument(0, MagikGrammar.SYMBOL);
        final AstNode argument2Node = argumentsHelper.getArgument(2, MagikGrammar.SYMBOL);
        if (argument0Node == null
            || argument2Node == null) {
            throw new IllegalStateException();
        }

        // Figure statement node.
        final AstNode statementNode = node.getFirstAncestor(MagikGrammar.STATEMENT);

        // Figure pakkage.
        final String pakkage = this.getCurrentPakkage();

        final String variableNameSymbol = argument0Node.getTokenValue();
        final String variableName = variableNameSymbol.substring(1);
        final String flavor = argument2Node.getTokenValue();
        final List<MethodDefinition> methodDefinitions =
            this.generateVariableMethods(statementNode, pakkage, exemplarName, variableName, flavor);
        return List.copyOf(methodDefinitions);
    }

    private List<MethodDefinition> generateVariableMethods(
            final AstNode definitionNode,
            final String pakkage,
            final String exemplarName,
            final String variableName,
            final String flavor) {
        final List<MethodDefinition> methodDefinitions = new ArrayList<>();

        // get
        final Set<MethodDefinition.Modifier> getModifiers = new HashSet<>();
        if (!flavor.equals(FLAVOR_READONLY) && !flavor.equals(FLAVOR_PUBLIC)) {
            getModifiers.add(MethodDefinition.Modifier.PRIVATE);
        }
        final List<ParameterDefinition> getParameters = Collections.emptyList();
        final MethodDefinition getMethod = new MethodDefinition(
            definitionNode, pakkage, exemplarName, variableName, getModifiers, getParameters, null);
        methodDefinitions.add(getMethod);

        // set
        final String setName = variableName + MagikOperator.CHEVRON.getValue();
        final Set<MethodDefinition.Modifier> setModifiers = new HashSet<>();
        if (!flavor.equals(FLAVOR_PUBLIC)) {
            setModifiers.add(MethodDefinition.Modifier.PRIVATE);
        }
        final List<ParameterDefinition> setParameters = Collections.emptyList();
        final ParameterDefinition assignmentParam =
            new ParameterDefinition(definitionNode, "val", ParameterDefinition.Modifier.NONE);
        final MethodDefinition setMethod = new MethodDefinition(
            definitionNode, pakkage, exemplarName, setName, setModifiers, setParameters, assignmentParam);
        methodDefinitions.add(setMethod);

        // boot
        final String bootName = variableName + MagikOperator.BOOT_CHEVRON.getValue();
        final MethodDefinition bootMethod = new MethodDefinition(
            definitionNode, pakkage, exemplarName, bootName, setModifiers, setParameters, assignmentParam);
        methodDefinitions.add(bootMethod);

        return methodDefinitions;
    }

    private String getCurrentPakkage() {
        final PackageNodeHelper helper = new PackageNodeHelper(this.node);
        return helper.getCurrentPackage();
    }

}
