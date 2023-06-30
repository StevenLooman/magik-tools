package nl.ramsolutions.sw.magik.parser;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import com.sonar.sslr.api.AstVisitor;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.api.TypeStringGrammar;

/**
 * Visitor which constructs TypeStrings from the TypeStringGrammar.
 */
public final class TypeStringBuilderVisitor implements AstVisitor {

    private final Map<AstNode, TypeString> mapping = new HashMap<>();
    private final String currentPakkage;
    private AstNode topAst;

    /**
     * Constructor.
     * @param currentPakkege Current package.
     */
    public TypeStringBuilderVisitor(final String currentPakkege) {
        this.currentPakkage = currentPakkege;
    }

    @CheckForNull
    public TypeString getTypeString() {
        return this.mapping.get(this.topAst);
    }

    @Override
    public List<AstNodeType> getAstNodeTypesToVisit() {
        return List.of(
            TypeStringGrammar.TYPE_UNDEFINED,
            TypeStringGrammar.TYPE_CLONE,
            TypeStringGrammar.TYPE_SELF,
            TypeStringGrammar.TYPE_PARAMETER_REFERENCE,
            TypeStringGrammar.TYPE_GENERIC,
            TypeStringGrammar.TYPE_IDENTIFIER,
            TypeStringGrammar.TYPE_STRING,
            TypeStringGrammar.SYNTAX_ERROR);
    }

    @Override
    public void visitFile(final @Nullable AstNode ast) {
        this.topAst = ast;
    }

    @Override
    public void leaveFile(final @Nullable AstNode ast) {
        // Pass.
    }

    @Override
    public void visitNode(final AstNode ast) {
        // Pass.
    }

    @Override
    public void leaveNode(final AstNode ast) {
        if (ast.is(TypeStringGrammar.TYPE_UNDEFINED)) {
            this.buildUndefined(ast);
        } else if (ast.is(TypeStringGrammar.TYPE_CLONE, TypeStringGrammar.TYPE_SELF)) {
            this.buildSelf(ast);
        } else if (ast.is(TypeStringGrammar.TYPE_PARAMETER_REFERENCE)) {
            this.buildParameterRef(ast);
        } else if (ast.is(TypeStringGrammar.TYPE_GENERIC)) {
            this.buildGeneric(ast);
        } else if (ast.is(TypeStringGrammar.TYPE_IDENTIFIER)) {
            this.buildIdentifier(ast);
        } else if (ast.is(TypeStringGrammar.TYPE_STRING)) {
            this.buildTypeString(ast);
        } else if (ast.is(TypeStringGrammar.SYNTAX_ERROR)) {
            this.buildUndefined(ast);
        } else {
            throw new IllegalStateException("Unknown node type: " + ast.getType());
        }
    }

    private void buildUndefined(final AstNode ast) {
        final TypeString part = TypeString.UNDEFINED;

        this.mapping.put(ast, part);
    }

    private void buildSelf(final AstNode ast) {
        final TypeString part = TypeString.SELF;

        this.mapping.put(ast, part);
    }

    private void buildParameterRef(final AstNode ast) {
        final List<AstNode> childAsts = ast.getChildren();
        final AstNode identifierAst = childAsts.get(2);
        final String refStr = identifierAst.getTokenValue();
        final TypeString part = TypeString.ofParameterRef(refStr);

        this.mapping.put(ast, part);
    }

    private void buildGeneric(final AstNode ast) {
        final List<AstNode> childAsts = ast.getChildren();
        final AstNode identifierAst = childAsts.get(1);
        final String str = identifierAst.getTokenValue();
        final TypeString part = TypeString.ofGeneric(str);

        this.mapping.put(ast, part);
    }

    private void buildIdentifier(final AstNode ast) {
        final String str = ast.getTokenValue();
        final AstNode genericDefinitionsNode = ast.getFirstChild(TypeStringGrammar.TYPE_GENERIC_DEFINITIONS);
        final List<TypeString> generics = genericDefinitionsNode != null
            ? genericDefinitionsNode.getChildren(TypeStringGrammar.TYPE_STRING).stream()
                .map(AstNode::getFirstChild)
                .map(this.mapping::get)
                .map(Objects::requireNonNull)
                .toList()
            : Collections.emptyList();
        final TypeString[] genericsArr = generics.toArray(TypeString[]::new);
        final TypeString part = TypeString.ofIdentifier(str, this.currentPakkage, genericsArr);

        this.mapping.put(ast, part);
    }

    private void buildTypeString(final AstNode ast) {
        final List<AstNode> childAsts = ast.getChildren(
            TypeStringGrammar.TYPE_UNDEFINED,
            TypeStringGrammar.TYPE_CLONE,
            TypeStringGrammar.TYPE_SELF,
            TypeStringGrammar.TYPE_PARAMETER_REFERENCE,
            TypeStringGrammar.TYPE_GENERIC,
            TypeStringGrammar.TYPE_IDENTIFIER,
            TypeStringGrammar.SYNTAX_ERROR);
        final List<TypeString> childTypeStrings = childAsts.stream()
            .map(this.mapping::get)
            .map(Objects::requireNonNull)
            .toList();
        if (childAsts.isEmpty()) {
            throw new IllegalStateException();
        }

        final TypeString[] childTypeStringsArr = childTypeStrings.toArray(TypeString[]::new);
        final TypeString part = childAsts.size() == 1
            ? childTypeStrings.get(0)
            : TypeString.ofCombination(this.currentPakkage, childTypeStringsArr);
        this.mapping.put(ast, part);
    }

}
