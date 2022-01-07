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
 * {@code define_slot_access()}}parser.
 */
public class DefineSlotAccessParser {

    private static final String DEFINE_SLOT_ACCESS = "define_slot_access()";
    // TODO: define_slot_externally_readable()
    // TODO: define_slot_externally_writable()
    private static final String FLAG_READ = ":read";
    private static final String FLAG_READABLE = ":readable";
    private static final String FLAG_WRITE = ":write";
    private static final String FLAG_WRITABLE = ":writable";
    private static final String FLAVOR_PUBLIC = ":public";
    private static final String FLAVOR_READ_ONLY = ":read_only";

    private final AstNode node;

    /**
     * Constructor.
     * @param node {@code define_slot_access()} node.
     */
    public DefineSlotAccessParser(final AstNode node) {
        if (node.isNot(MagikGrammar.METHOD_INVOCATION)) {
            throw new IllegalArgumentException();
        }

        this.node = node;
    }

    /**
     * Test if node is a {@code define_slot_access()}.
     * @param node Node to test
     * @return True if node is a {@code define_slot_access()}, false otherwise.
     */
    public static boolean isDefineSlotAccess(final AstNode node) {
        if (node.isNot(MagikGrammar.METHOD_INVOCATION)) {
            return false;
        }

        final MethodInvocationNodeHelper helper = new MethodInvocationNodeHelper(node);
        if (!helper.isMethodInvocationOf(DEFINE_SLOT_ACCESS)) {
            return false;
        }

        final AstNode parentNode = node.getParent();
        final AstNode atomNode = parentNode.getFirstChild();
        if (atomNode.isNot(MagikGrammar.ATOM)) {
            return false;
        }
        final String exemplarName = atomNode.getTokenValue();
        if (exemplarName == null) {
            return false;
        }

        final AstNode argumentsNode = node.getFirstChild(MagikGrammar.ARGUMENTS);
        final ArgumentsNodeHelper argumentsHelper = new ArgumentsNodeHelper(argumentsNode);
        final AstNode argument0Node = argumentsHelper.getArgument(0, MagikGrammar.SYMBOL);
        final AstNode argument1Node = argumentsHelper.getArgument(1, MagikGrammar.SYMBOL);
        return argument0Node != null
            && argument1Node != null;
    }

    /**
     * Parse defitions.
     * @return List of parsed definitions.
     */
    public List<Definition> parseDefinitions() {
        final AstNode argumentsNode = node.getFirstChild(MagikGrammar.ARGUMENTS);
        final ArgumentsNodeHelper argumentsHelper = new ArgumentsNodeHelper(argumentsNode);

        // Some sanity.
        final AstNode parentNode = node.getParent();
        final AstNode atomNode = parentNode.getFirstChild();
        if (atomNode.isNot(MagikGrammar.ATOM)) {
            throw new IllegalStateException();
        }
        final String exemplarName = atomNode.getTokenValue();
        if (exemplarName == null) {
            throw new IllegalStateException();
        }

        final AstNode argument0Node = argumentsHelper.getArgument(0, MagikGrammar.SYMBOL);
        final AstNode argument1Node = argumentsHelper.getArgument(1, MagikGrammar.SYMBOL);
        final AstNode argument2Node = argumentsHelper.getArgument(2, MagikGrammar.SYMBOL);
        if (argument0Node == null
            || argument1Node == null) {
            throw new IllegalStateException();
        }

        // Figure statement node.
        final AstNode statementNode = node.getFirstAncestor(MagikGrammar.STATEMENT);

        // Figure pakkage.
        final String pakkage = this.getCurrentPakkage();

        final String slotNameSymbol = argument0Node.getTokenValue();
        final String slotName = slotNameSymbol.substring(1);
        final String flag = argument1Node.getTokenValue();
        final String flavor = argument2Node != null
            ? argument2Node.getTokenValue()
            : FLAVOR_PUBLIC;  // Default is public.
        final List<MethodDefinition> methodDefinitions =
                this.generateSlotMethods(statementNode, pakkage, exemplarName, slotName, flag, flavor);
        return List.copyOf(methodDefinitions);
    }

    private List<MethodDefinition> generateSlotMethods(
            final AstNode definitionNode,
            final String pakkage,
            final String exemplarName,
            final String slotName,
            final String flag,
            final String flavor) {
        final List<MethodDefinition> methodDefinitions = new ArrayList<>();
        if (flag.equals(FLAG_READ)
            || flag.equals(FLAG_READABLE)) {
            // get
            final String getName = slotName;
            final Set<MethodDefinition.Modifier> getModifiers = new HashSet<>();
            if (!flavor.equals(FLAVOR_PUBLIC)) {
                getModifiers.add(MethodDefinition.Modifier.PRIVATE);
            }
            final List<ParameterDefinition> getParameters = Collections.emptyList();
            final MethodDefinition getMethod = new MethodDefinition(
                definitionNode, pakkage, exemplarName, getName, getModifiers, getParameters, null);
            methodDefinitions.add(getMethod);
        } else if (flag.equals(FLAG_WRITE)
                   || flag.equals(FLAG_WRITABLE)) {
            // get
            final Set<MethodDefinition.Modifier> getModifiers = new HashSet<>();
            if (!flavor.equals(FLAVOR_PUBLIC) && !flavor.equals(FLAVOR_READ_ONLY)) {
                getModifiers.add(MethodDefinition.Modifier.PRIVATE);
            }
            final List<ParameterDefinition> getParameters = Collections.emptyList();
            final MethodDefinition getMethod = new MethodDefinition(
                definitionNode, pakkage, exemplarName, slotName, getModifiers, getParameters, null);
            methodDefinitions.add(getMethod);

            // set
            final String setName = slotName + MagikOperator.CHEVRON.getValue();
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
            final String bootName = slotName + MagikOperator.BOOT_CHEVRON.getValue();
            final MethodDefinition bootMethod = new MethodDefinition(
                definitionNode, pakkage, exemplarName, bootName, setModifiers, setParameters, assignmentParam);
            methodDefinitions.add(bootMethod);
        }
        return methodDefinitions;
    }

    private String getCurrentPakkage() {
        final PackageNodeHelper helper = new PackageNodeHelper(this.node);
        return helper.getCurrentPackage();
    }

}
