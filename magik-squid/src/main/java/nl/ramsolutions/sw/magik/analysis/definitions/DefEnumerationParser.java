package nl.ramsolutions.sw.magik.analysis.definitions;

import com.sonar.sslr.api.AstNode;
import java.util.Collections;
import java.util.List;
import nl.ramsolutions.sw.magik.analysis.helpers.ArgumentsNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.ProcedureInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/**
 * {@code def_enumeration_from}/{@code def_enumeration} parser.
 */
public class DefEnumerationParser extends TypeDefParser {

    private static final String DEF_ENUMERATION_FROM = "def_enumeration_from";
    private static final String DEF_ENUMERATION = "def_enumeration";
    private static final String SW_DEF_ENUMERATION_FROM = "sw:def_enumeration_from";
    private static final String SW_DEF_ENUMERATION = "sw:def_enumeration";

    /**
     * Constructor.
     * @param node {@code def_enumeration_from}/{@code def_enumeration} node.
     */
    public DefEnumerationParser(final AstNode node) {
        super(node);
    }

    /**
     * Test if node is a {@code def_enumeration_from}/{@code def_enumeration}.
     * @param node Node to test
     * @return True if node is a {@code def_enumeration_from}/{@code def_enumeration}, false otherwise.
     */
    public static boolean isDefEnumeration(final AstNode node) {
        if (!node.is(MagikGrammar.PROCEDURE_INVOCATION)) {
            return false;
        }

        final ProcedureInvocationNodeHelper helper = new ProcedureInvocationNodeHelper(node);
        if (!helper.isProcedureInvocationOf(DEF_ENUMERATION)
            && !helper.isProcedureInvocationOf(DEF_ENUMERATION_FROM)
            && !helper.isProcedureInvocationOf(SW_DEF_ENUMERATION)
            && !helper.isProcedureInvocationOf(SW_DEF_ENUMERATION_FROM)) {
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
        final TypeString name = TypeString.of(identifier, pakkage);

        // Figure parents.
        final List<TypeString> parents = Collections.emptyList();

        final EnumerationDefinition enumerationDefinition =
            new EnumerationDefinition(statementNode, name, parents);
        return List.of(enumerationDefinition);
    }

}
