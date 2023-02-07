package nl.ramsolutions.sw.magik.analysis.definitions;

import com.sonar.sslr.api.AstNode;
import java.util.List;
import nl.ramsolutions.sw.magik.analysis.helpers.ArgumentsNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.ProcedureInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/**
 * {@code def_mixin()} parser.
 */
public class DefMixinParser extends TypeDefParser {

    private static final String DEF_MIXIN = "def_mixin";
    private static final String SW_DEF_MIXIN = "sw:def_mixin";

    /**
     * Constructor.
     * @param node {@code def_mixin()} node.
     */
    public DefMixinParser(final AstNode node) {
        super(node);
    }

    /**
     * Test if node is a {@code def_mixin()}.
     * @param node Node to test.
     * @return True if node is a {@code def_mixin()}, false otherwise.
     */
    public static boolean isDefMixin(final AstNode node) {
        if (!node.is(MagikGrammar.PROCEDURE_INVOCATION)) {
            return false;
        }

        final ProcedureInvocationNodeHelper helper = new ProcedureInvocationNodeHelper(node);
        if (!helper.isProcedureInvocationOf(DEF_MIXIN)
            && !helper.isProcedureInvocationOf(SW_DEF_MIXIN)) {
            return false;
        }

        final AstNode argumentsNode = node.getFirstChild(MagikGrammar.ARGUMENTS);
        final ArgumentsNodeHelper argumentsHelper = new ArgumentsNodeHelper(argumentsNode);

        // Some sanity.
        final AstNode argument0Node = argumentsHelper.getArgument(0, MagikGrammar.SYMBOL);
        return argument0Node != null;
    }

    /**
     * Parse defitions.
     * @return List of parsed definitions.
     */
    @Override
    public List<Definition> parseDefinitions() {
        final AstNode argumentsNode = this.node.getFirstChild(MagikGrammar.ARGUMENTS);
        final ArgumentsNodeHelper argumentsHelper = new ArgumentsNodeHelper(argumentsNode);

        // Some sanity.
        final AstNode argument0Node = argumentsHelper.getArgument(0, MagikGrammar.SYMBOL);
        if (argument0Node == null) {
            throw new IllegalStateException();
        }

        // Figure statement node.
        final AstNode statementNode = this.node.getFirstAncestor(MagikGrammar.STATEMENT);

        // Figure pakkage.
        final String pakkage = this.getCurrentPakkage();

        // Figure name.
        final String identifier = argument0Node.getTokenValue().substring(1);
        final TypeString name = TypeString.ofIdentifier(identifier, pakkage);

        // Parents.
        final AstNode argument1Node = argumentsHelper.getArgument(1);
        final List<TypeString> parents = this.extractParents(argument1Node);

        final MixinDefinition mixinDefinition = new MixinDefinition(statementNode, name, parents);
        return List.of(mixinDefinition);
    }

}
