package nl.ramsolutions.sw.magik.analysis.definitions.parsers;

import com.sonar.sslr.api.AstNode;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import nl.ramsolutions.sw.definitions.ModuleDefinitionScanner;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.analysis.definitions.Definition;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.helpers.ArgumentsNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.ProcedureInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.parser.MagikCommentExtractor;

/**
 * {@code def_mixin()} parser.
 */
public class DefMixinParser extends BaseDefParser {

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

        // Figure location.
        final URI uri = this.node.getToken().getURI();
        final Location location = new Location(uri, this.node);

        // Figure module name.
        final String moduleName = ModuleDefinitionScanner.getModuleName(uri);

        // Figure statement node.
        final AstNode statementNode = this.node.getFirstAncestor(MagikGrammar.STATEMENT);

        // Figure name.
        final String packageName = this.getCurrentPakkage();
        final String identifier = argument0Node.getTokenValue().substring(1);
        final TypeString name = TypeString.ofIdentifier(identifier, packageName);

        // Figure parents.
        final AstNode argument1Node = argumentsHelper.getArgument(1);
        final List<TypeString> parents = this.extractParents(argument1Node);

        // Figure doc.
        final AstNode parentNode = this.node.getParent();
        final String doc = MagikCommentExtractor.extractDocComment(parentNode);

        final ExemplarDefinition mixinDefinition = new ExemplarDefinition(
            location,
            moduleName,
            doc,
            statementNode,
            ExemplarDefinition.Sort.INTRINSIC,
            name,
            Collections.emptyList(),
            parents);
        return List.of(mixinDefinition);
    }

}
