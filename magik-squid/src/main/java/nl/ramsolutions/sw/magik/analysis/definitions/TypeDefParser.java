package nl.ramsolutions.sw.magik.analysis.definitions;

import com.sonar.sslr.api.AstNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.analysis.helpers.ExpressionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.PackageNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/**
 * Base type parser.
 */
@SuppressWarnings("checkstyle:VisibilityModifier")
abstract class TypeDefParser {

    static final String FLAG_READ = ":read";
    static final String FLAG_READABLE = ":readable";
    static final String FLAG_WRITE = ":read";
    static final String FLAG_WRITABLE = ":writable";
    static final String FLAVOR_PUBLIC = ":public";
    static final String FLAVOR_READONLY = ":readonly";
    static final String FLAVOR_READ_ONLY = ":read_only";

    /**
     * Node to operate on.
     */
    protected final AstNode node;

    /**
     * Constructor.
     * @param node Definition node.
     */
    protected TypeDefParser(final AstNode node) {
        if (node.isNot(MagikGrammar.PROCEDURE_INVOCATION)) {
            throw new IllegalArgumentException();
        }

        this.node = node;
    }

    /**
     * Parse the type definitions.
     * @return List of {@code Definitions}.
     */
    public abstract List<Definition> parseDefinitions();

    /**
     * Extract parents.
     * @param definitionNode Definition node.
     * @return List of parents.
     */
    protected List<TypeString> extractParents(final @Nullable AstNode definitionNode) {
        if (definitionNode == null) {
            return Collections.emptyList();
        }

        final List<TypeString> parents = new ArrayList<>();
        final ExpressionNodeHelper expressionHelper = new ExpressionNodeHelper(definitionNode);
        final String singleParent = expressionHelper.getConstant();
        final AstNode multiParentNode =
            AstQuery.getOnlyFromChain(definitionNode, MagikGrammar.ATOM, MagikGrammar.SIMPLE_VECTOR);
        if (singleParent != null) {
            final TypeString parent = this.getFullParent(singleParent);
            parents.add(parent);
        } else if (multiParentNode != null) {
            for (final AstNode parentNode : multiParentNode.getChildren(MagikGrammar.EXPRESSION)) {
                final ExpressionNodeHelper parentExpressionHelper = new ExpressionNodeHelper(parentNode);
                final String parentStr = parentExpressionHelper.getConstant();
                Objects.requireNonNull(parentStr);
                final TypeString parent = this.getFullParent(parentStr);
                parents.add(parent);
            }
        }
        return parents;
    }

    protected String getCurrentPakkage() {
        final PackageNodeHelper helper = new PackageNodeHelper(this.node);
        return helper.getCurrentPackage();
    }

    private TypeString getFullParent(final String parentStr) {
        final String parent = parentStr.startsWith(":") || parentStr.startsWith("@")
            ? parentStr.substring(1)
            : parentStr;
        return TypeString.ofIdentifier(parent, this.getCurrentPakkage());
    }

}
