package nl.ramsolutions.sw.magik.analysis.definitions;

import com.sonar.sslr.api.AstNode;
import java.util.List;
import nl.ramsolutions.sw.magik.analysis.helpers.ArgumentsNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.ProcedureInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/**
 * {@code def_indexed_exemplar} parser.
 */
public class DefIndexedExemplarParser extends TypeDefParser {

    private static final String DEF_INDEXED_EXEMPLAR = "def_indexed_exemplar";
    private static final String SW_DEF_INDEXED_EXEMPLAR = "sw:def_indexed_exemplar";

    /**
     * Constructor.
     * @param node {@code def_indexed_exemplar()} node.
     */
    public DefIndexedExemplarParser(final AstNode node) {
        super(node);
    }

    /**
     * Test if node is a {@code def_indexed_exemplar()}.
     * @param node Node to test
     * @return True if node is a {@code def_indexed_exemplar()}, false otherwise.
     */
    public static boolean isDefIndexedExemplar(final AstNode node) {
        if (!node.is(MagikGrammar.PROCEDURE_INVOCATION)) {
            return false;
        }

        final ProcedureInvocationNodeHelper helper = new ProcedureInvocationNodeHelper(node);
        if (!helper.isProcedureInvocationOf(DEF_INDEXED_EXEMPLAR)
            && !helper.isProcedureInvocationOf(SW_DEF_INDEXED_EXEMPLAR)) {
            return false;
        }

        // Some sanity.
        final AstNode argumentsNode = node.getFirstChild(MagikGrammar.ARGUMENTS);
        final ArgumentsNodeHelper argumentsHelper = new ArgumentsNodeHelper(argumentsNode);
        final AstNode argument0Node = argumentsHelper.getArgument(0, MagikGrammar.SYMBOL);
        return argument0Node != null;
    }

    /**
     * Parse defitions.
     * @return List of parsed definitions.
     */
    @Override
    public List<Definition> parseDefinitions() {
        final AstNode argumentsNode = node.getFirstChild(MagikGrammar.ARGUMENTS);
        final ArgumentsNodeHelper argumentsHelper = new ArgumentsNodeHelper(argumentsNode);

        // Some sanity.
        final AstNode argument0Node = argumentsHelper.getArgument(0, MagikGrammar.SYMBOL);
        if (argument0Node == null) {
            throw new IllegalStateException();
        }

        // Figure statement node.
        final AstNode statementNode = node.getFirstAncestor(MagikGrammar.STATEMENT);

        // Figure pakkage.
        final String pakkage = this.getCurrentPakkage();

        // Figure name.
        final String identifier = argument0Node.getTokenValue().substring(1);
        final TypeString name = TypeString.ofIdentifier(identifier, pakkage);

        // Parents.
        final AstNode argument2Node = argumentsHelper.getArgument(2);
        final List<TypeString> parents = this.extractParents(argument2Node);

        final IndexedExemplarDefinition indexedExemplarDefinition =
            new IndexedExemplarDefinition(statementNode, name, parents);
        return List.of(indexedExemplarDefinition);
    }

}
