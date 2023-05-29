package nl.ramsolutions.sw.magik.analysis.definitions;

import com.sonar.sslr.api.AstNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import nl.ramsolutions.sw.magik.analysis.helpers.ArgumentsNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.ProcedureInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.SimpleVectorNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.api.MagikOperator;

/**
 * {@code def_slotted_exemplar} parser.
 */
public class DefSlottedExemplarParser extends TypeDefParser {

    private static final String DEF_SLOTTED_EXEMPLAR = "def_slotted_exemplar";
    private static final String SW_DEF_SLOTTED_EXEMPLAR = "sw:def_slotted_exemplar";

    /**
     * Constructor.
     * @param node {@code def_slotted_exemplar()} node.
     */
    public DefSlottedExemplarParser(final AstNode node) {
        super(node);
    }

    /**
     * Test if node is a {@code def_slotted_exemplar()}.
     * @param node Node to test
     * @return True if node is a {@code def_slotted_exemplar()}, false otherwise.
     */
    public static boolean isDefSlottedExemplar(final AstNode node) {
        if (!node.is(MagikGrammar.PROCEDURE_INVOCATION)) {
            return false;
        }

        final ProcedureInvocationNodeHelper helper = new ProcedureInvocationNodeHelper(node);
        if (!helper.isProcedureInvocationOf(DEF_SLOTTED_EXEMPLAR)
            && !helper.isProcedureInvocationOf(SW_DEF_SLOTTED_EXEMPLAR)) {
            return false;
        }

        // Some sanity.
        final AstNode argumentsNode = node.getFirstChild(MagikGrammar.ARGUMENTS);
        final ArgumentsNodeHelper argumentsHelper = new ArgumentsNodeHelper(argumentsNode);
        final AstNode argument0Node = argumentsHelper.getArgument(0, MagikGrammar.SYMBOL);
        if (argument0Node == null) {
            return false;
        }
        final AstNode argument1Node = argumentsHelper.getArgument(1, MagikGrammar.SIMPLE_VECTOR);
        return argument1Node != null;
    }

    /**
     * Parse defitions.
     * @return List of parsed definitions.
     */
    @Override
    public List<Definition> parseDefinitions() {
        final AstNode argumentsNode = node.getFirstChild(MagikGrammar.ARGUMENTS);
        final ArgumentsNodeHelper argumentsHelper = new ArgumentsNodeHelper(argumentsNode);
        final AstNode argument0Node = argumentsHelper.getArgument(0, MagikGrammar.SYMBOL);
        if (argument0Node == null) {
            throw new IllegalStateException();
        }
        final AstNode argument1Node = argumentsHelper.getArgument(1, MagikGrammar.SIMPLE_VECTOR);
        if (argument1Node == null) {
            throw new IllegalStateException();
        }

        // Figure statement node.
        final AstNode statementNode = this.node.getFirstAncestor(MagikGrammar.STATEMENT);

        // Figure pakkage.
        final String currentPakkage = this.getCurrentPakkage();

        // Figure name.
        final String identifier = argument0Node.getTokenValue().substring(1);
        final TypeString name = TypeString.ofIdentifier(identifier, currentPakkage);

        // Figure slots.
        final List<SlottedExemplarDefinition.Slot> slots = new ArrayList<>();
        final List<MethodDefinition> methodDefinitions = new ArrayList<>();
        for (final AstNode slotDefNode : argument1Node.getChildren(MagikGrammar.EXPRESSION)) {  // NOSONAR
            final SimpleVectorNodeHelper simpleVectorHelper = SimpleVectorNodeHelper.fromExpressionSafe(slotDefNode);
            if (simpleVectorHelper == null) {
                continue;
            }

            final AstNode slotNameNode = simpleVectorHelper.getNth(0, MagikGrammar.SYMBOL);
            if (slotNameNode == null) {
                continue;
            }

            // Slot definitions.
            final String slotNameSymbol = slotNameNode.getTokenValue();
            final String slotName = slotNameSymbol.substring(1);
            final SlottedExemplarDefinition.Slot slot = new SlottedExemplarDefinition.Slot(slotDefNode, slotName);
            slots.add(slot);

            // Method definitions.
            final AstNode flagNode = simpleVectorHelper.getNth(2, MagikGrammar.SYMBOL);
            final AstNode flavorNode = simpleVectorHelper.getNth(3, MagikGrammar.SYMBOL);
            if (flagNode != null
                && flavorNode != null) {
                final String flag = flagNode.getTokenValue();
                final String flavor = flavorNode.getTokenValue();
                final TypeString exemplarName = TypeString.ofIdentifier(identifier, currentPakkage);
                final List<MethodDefinition> slotMethodDefinitions =
                    this.generateSlotMethods(slotDefNode, exemplarName, slotName, flag, flavor);
                methodDefinitions.addAll(slotMethodDefinitions);
            }
        }

        // Parents.
        final AstNode argument2Node = argumentsHelper.getArgument(2);
        final List<TypeString> parents = this.extractParents(argument2Node);

        final SlottedExemplarDefinition slottedExemplarDefinition =
            new SlottedExemplarDefinition(statementNode, name, slots, parents);

        final List<Definition> definitions = new ArrayList<>();
        definitions.add(slottedExemplarDefinition);
        definitions.addAll(methodDefinitions);
        return definitions;
    }

    private List<MethodDefinition> generateSlotMethods(
            final AstNode node,
            final TypeString exemplarName,
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
                node, exemplarName, getName, getModifiers, getParameters, null);
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
                node, exemplarName, slotName, getModifiers, getParameters, null);
            methodDefinitions.add(getMethod);

            // set
            final String setName = slotName + MagikOperator.CHEVRON.getValue();
            final Set<MethodDefinition.Modifier> setModifiers = new HashSet<>();
            if (!flavor.equals(FLAVOR_PUBLIC)) {
                setModifiers.add(MethodDefinition.Modifier.PRIVATE);
            }
            final List<ParameterDefinition> setParameters = Collections.emptyList();
            final ParameterDefinition assignmentParam =
                    new ParameterDefinition(node, "val", ParameterDefinition.Modifier.NONE);
            final MethodDefinition setMethod = new MethodDefinition(
                node, exemplarName, setName, setModifiers, setParameters, assignmentParam);
            methodDefinitions.add(setMethod);

            // boot
            final String bootName = slotName + MagikOperator.BOOT_CHEVRON.getValue();
            final MethodDefinition bootMethod = new MethodDefinition(
                node, exemplarName, bootName, setModifiers, setParameters, assignmentParam);
            methodDefinitions.add(bootMethod);
        }
        return methodDefinitions;
    }

}
