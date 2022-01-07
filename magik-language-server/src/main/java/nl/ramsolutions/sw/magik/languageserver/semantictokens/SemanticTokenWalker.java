package nl.ramsolutions.sw.magik.languageserver.semantictokens;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.Trivia;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.AstWalker;
import nl.ramsolutions.sw.magik.analysis.scope.GlobalScope;
import nl.ramsolutions.sw.magik.analysis.scope.Scope;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.types.GlobalReference;
import nl.ramsolutions.sw.magik.analysis.typing.types.MagikType;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.api.MagikKeyword;
import nl.ramsolutions.sw.magik.api.MagikOperator;
import nl.ramsolutions.sw.magik.api.NewDocGrammar;
import nl.ramsolutions.sw.magik.parser.NewDocParser;

/**
 * Semantic token walker.
 */
public class SemanticTokenWalker extends AstWalker {

    private static final String DEFAULT_PACKAGE = "user";

    private static final List<String> MAGIK_MODIFIER_VALUES = List.of(
        MagikKeyword.PRIVATE.getValue(),
        MagikKeyword.ABSTRACT.getValue(),
        MagikKeyword.ITER.getValue());

    private static final Map<NewDocGrammar, SemanticToken.Type> NEW_DOC_ELEMENT_TYPE_MAPPING = Map.of(
        NewDocGrammar.PARAM, SemanticToken.Type.PARAMETER,
        NewDocGrammar.LOOP, SemanticToken.Type.VARIABLE,
        NewDocGrammar.SLOT, SemanticToken.Type.PROPERTY);

    private static final Set<String> MAGIK_KEYWORD_VALUES = Arrays.stream(MagikKeyword.values())
        .filter(keyword ->
            keyword != MagikKeyword.SELF
            && keyword != MagikKeyword.CLONE
            && keyword != MagikKeyword.SUPER
            && keyword != MagikKeyword.TRUE
            && keyword != MagikKeyword.FALSE
            && keyword != MagikKeyword.MAYBE
            && keyword != MagikKeyword.UNSET)
        .map(MagikKeyword::getValue)
        .collect(Collectors.toUnmodifiableSet());

    private static final Set<String> MAGIK_KEYWORD_VALUES_CONSTANTS = Set.of(
        MagikKeyword.SELF.getValue(),
        MagikKeyword.CLONE.getValue(),
        MagikKeyword.SUPER.getValue(),
        MagikKeyword.TRUE.getValue(),
        MagikKeyword.FALSE.getValue(),
        MagikKeyword.MAYBE.getValue(),
        MagikKeyword.UNSET.getValue());

    private static final List<String> MAGIK_OPERATOR_VALUES = Arrays.stream(MagikOperator.values())
        .map(MagikOperator::getValue)
        .collect(Collectors.toUnmodifiableList());

    private final MagikTypedFile magikFile;
    private final List<SemanticToken> semanticTokens = new ArrayList<>();
    private String currentPakkage = DEFAULT_PACKAGE;

    /**
     * Constructor.
     * @param magikFile {@link MagikTypedFile} to operate on.
     */
    SemanticTokenWalker(final MagikTypedFile magikFile) {
        this.magikFile = magikFile;
    }

    public List<SemanticToken> getSemanticTokens() {
        return this.semanticTokens;
    }

    private void addSemanticToken(
            final Token token, final SemanticToken.Type type, final Set<SemanticToken.Modifier> modifiers) {
        final SemanticToken semanticToken = new SemanticToken(token, type, modifiers);
        this.semanticTokens.add(semanticToken);
    }

    private void addSemanticToken(final Token token, final SemanticToken.Type type) {
        Set<SemanticToken.Modifier> modifiers = Collections.emptySet();
        this.addSemanticToken(token, type, modifiers);
    }

    private void addSemanticToken(
            final AstNode node, final SemanticToken.Type type, final Set<SemanticToken.Modifier> modifiers) {
        final Token token = node.getToken();
        this.addSemanticToken(token, type, modifiers);
    }

    private void addSemanticToken(final AstNode node, final SemanticToken.Type type) {
        final Token token = node.getToken();
        this.addSemanticToken(token, type);
    }

    @Override
    protected void walkToken(final Token token) {
        final String value = token.getOriginalValue().toLowerCase();
        if (MAGIK_MODIFIER_VALUES.contains(value)) {
            this.addSemanticToken(token, SemanticToken.Type.MODIFIER);
        } else if (MAGIK_KEYWORD_VALUES.contains(value)) {
            this.addSemanticToken(token, SemanticToken.Type.KEYWORD);
        } else if (MAGIK_OPERATOR_VALUES.contains(value)) {
            this.addSemanticToken(token, SemanticToken.Type.OPERATOR);
        } else if (MAGIK_KEYWORD_VALUES_CONSTANTS.contains(value)) {
            final Set<SemanticToken.Modifier> modifier = Set.of(SemanticToken.Modifier.READONLY);
            this.addSemanticToken(token, SemanticToken.Type.VARIABLE, modifier);
        }
    }

    @Override
    protected void walkTrivia(final Trivia trivia) {
        if (trivia.isComment()) {
            final Token triviaToken = trivia.getToken();
            this.walkCommentToken(triviaToken);
        }
    }

    @SuppressWarnings("checkstyle:NestedIfDepth")
    private void walkCommentToken(final Token token) {
        final String value = token.getOriginalValue();
        if (value.startsWith("##")) {
            final Set<SemanticToken.Modifier> docModifier = Set.of(SemanticToken.Modifier.DOCUMENTATION);

            final List<Token> docTokens = List.of(token);
            final NewDocParser newDocParser = new NewDocParser(docTokens);
            final AstNode newDocNode = newDocParser.getNewDocNode();

            // It is either a FUNCTION node or element nodes.
            final AstNode functionNode = newDocNode.getFirstChild(NewDocGrammar.FUNCTION);
            if (functionNode != null) {
                this.addSemanticToken(token, SemanticToken.Type.COMMENT, docModifier);
            }

            final List<AstNode> elementNodes = newDocNode.getChildren(
                NewDocGrammar.PARAM, NewDocGrammar.RETURN, NewDocGrammar.LOOP, NewDocGrammar.SLOT);
            elementNodes.forEach(elementNode -> {
                // DOC_START
                final AstNode docStartNode = elementNode.getFirstChild(NewDocGrammar.DOC_START);
                this.addSemanticToken(docStartNode, SemanticToken.Type.COMMENT, docModifier);

                // Element
                final AstNode keywordNode = elementNode.getChildren().get(1);
                this.addSemanticToken(keywordNode, SemanticToken.Type.KEYWORD, docModifier);

                // TYPE
                final AstNode typeNode = elementNode.getFirstChild(NewDocGrammar.TYPE);
                if (typeNode != null) {
                    final List<AstNode> typeNodes = typeNode.getDescendants(
                        NewDocGrammar.TYPE_NAME, NewDocGrammar.TYPE_CLONE, NewDocGrammar.TYPE_SELF);
                    typeNodes.forEach(typeTypeNode -> {
                        final String identifier = typeTypeNode.getTokenValue();
                        final GlobalReference globalRef = identifier.indexOf(':') != -1
                            ? GlobalReference.of(identifier)
                            : GlobalReference.of(this.currentPakkage, identifier);
                        if (typeTypeNode.is(NewDocGrammar.TYPE_CLONE, NewDocGrammar.TYPE_SELF)) {
                            final Set<SemanticToken.Modifier> constModifier =
                                Set.of(SemanticToken.Modifier.DOCUMENTATION, SemanticToken.Modifier.READONLY);
                            this.addSemanticToken(typeTypeNode, SemanticToken.Type.CLASS, constModifier);
                        } else if (this.isKnownType(globalRef)) {
                            this.addSemanticToken(typeTypeNode, SemanticToken.Type.CLASS, docModifier);
                        }
                    });
                }

                // NAME
                final AstNode nameNode = elementNode.getFirstChild(NewDocGrammar.NAME);
                if (nameNode != null
                    && nameNode.getToken() != null) {
                    final SemanticToken.Type type = NEW_DOC_ELEMENT_TYPE_MAPPING.get(elementNode.getType());
                    this.addSemanticToken(nameNode, type, docModifier);
                }

                // DESCRIPTION
                final List<AstNode> descriptionNodes = elementNode.getChildren(NewDocGrammar.DESCRIPTION);
                descriptionNodes.forEach(descriptionNode ->
                    this.addSemanticToken(descriptionNode, SemanticToken.Type.COMMENT, docModifier));
            });
        } else {
            this.addSemanticToken(token, SemanticToken.Type.COMMENT);
        }
    }

    @Override
    protected void walkPostNumber(final AstNode node) {
        this.addSemanticToken(node, SemanticToken.Type.NUMBER);
    }

    @Override
    protected void walkPostString(final AstNode node) {
        this.addSemanticToken(node, SemanticToken.Type.STRING);
    }

    @Override
    protected void walkPostSymbol(final AstNode node) {
        this.addSemanticToken(node, SemanticToken.Type.STRING);
    }

    @Override
    protected void walkPostRegexp(final AstNode node) {
        this.addSemanticToken(node, SemanticToken.Type.REGEXP);
    }

    @Override
    protected void walkPreProcedureInvocation(AstNode node) {
        final AstNode previousSiblingNode = node.getPreviousSibling();
        if (previousSiblingNode == null
            || !previousSiblingNode.is(MagikGrammar.ATOM)) {
            return;
        }

        this.addSemanticToken(previousSiblingNode, SemanticToken.Type.FUNCTION);
    }

    @Override
    protected void walkPreMethodInvocation(final AstNode node) {
        final AstNode identifierNode = node.getFirstChild(MagikGrammar.IDENTIFIER);
        if (identifierNode == null) {
            return;
        }

        this.addSemanticToken(identifierNode, SemanticToken.Type.METHOD);
    }

    @Override
    protected void walkPostIdentifier(final AstNode node) {
        final AstNode parentNode = node.getParent();
        if (parentNode == null) {
            return;
        }

        if (parentNode.is(MagikGrammar.METHOD_DEFINITION)) {
            this.walkPostIdentifierMethodDefintion(node);
        } else if (parentNode.is(MagikGrammar.ATOM)
                   && !this.isPartOfProcedureInvocation(node)) {
            this.walkPostIdentifierAtom(node);
        } else if (parentNode.is(MagikGrammar.PARAMETER)) {
            this.walkPostIdentifierParameter(node);
        } else if (parentNode.is(MagikGrammar.VARIABLE_DEFINITION)) {
            this.walkPostIdentifierVariableDefinition(node);
        } else if (parentNode.getParent() != null
                   && parentNode.getParent().is(MagikGrammar.VARIABLE_DEFINITION_MULTI)) {
            this.walkPostIdentifierVariableDefinitionMulti(node);
        }

        if (node.getFirstAncestor(MagikGrammar.FOR_VARIABLES) != null) {
            this.walkPostIdentifierFor(node);
        }
    }

    private boolean isPartOfProcedureInvocation(final AstNode node) {
        final AstNode atomNode = node.getParent();
        if (atomNode == null
            || !atomNode.is(MagikGrammar.ATOM)) {
            return false;
        }
        return atomNode.getNextSibling() != null
               && atomNode.getNextSibling().is(MagikGrammar.PROCEDURE_INVOCATION);
    }

    private void walkPostIdentifierVariableDefinition(final AstNode node) {
        this.addSemanticToken(node, SemanticToken.Type.VARIABLE);
    }

    private void walkPostIdentifierVariableDefinitionMulti(final AstNode node) {
        this.addSemanticToken(node, SemanticToken.Type.VARIABLE);
    }

    private void walkPostIdentifierFor(final AstNode node) {
        this.addSemanticToken(node, SemanticToken.Type.VARIABLE);
    }

    private void walkPostIdentifierMethodDefintion(final AstNode node) {
        final AstNode methodDefinitionNode = node.getParent();
        final List<AstNode> identifierNodes = methodDefinitionNode.getChildren(MagikGrammar.IDENTIFIER);
        final int index = identifierNodes.indexOf(node);
        if (index == 0) {
            final AstNode typeIdentifierNode = identifierNodes.get(0);
            this.addSemanticToken(typeIdentifierNode, SemanticToken.Type.CLASS);
        } else if (index == 1) {
            final AstNode methodIdentifierNode = identifierNodes.get(1);
            this.addSemanticToken(methodIdentifierNode, SemanticToken.Type.METHOD);
        }
    }

    private void walkPostIdentifierAtom(final AstNode node) {
        final GlobalScope globalScope = this.magikFile.getGlobalScope();
        final Scope scope = globalScope.getScopeForNode(node);
        Objects.requireNonNull(scope);

        final String identifier = node.getTokenValue();
        final ScopeEntry scopeEntry = scope.getScopeEntry(identifier);
        if (scopeEntry == null) {
            return;
        }

        switch (scopeEntry.getType()) {
            case PARAMETER:
                this.addSemanticToken(node, SemanticToken.Type.PARAMETER);
                break;

            case DEFINITION:
            case IMPORT:
            case LOCAL:
            case RECURSIVE:
                this.addSemanticToken(node, SemanticToken.Type.VARIABLE);
                break;

            case GLOBAL:
            case DYNAMIC:
                final GlobalReference globalRef = identifier.indexOf(':') != -1
                    ? GlobalReference.of(identifier)
                    : GlobalReference.of(this.currentPakkage, identifier);
                if (this.isKnownType(globalRef)) {
                    this.addSemanticToken(
                        node, SemanticToken.Type.CLASS, Set.of(SemanticToken.Modifier.VARIABLE_GLOBAL));
                } else {
                    this.addSemanticToken(
                        node, SemanticToken.Type.VARIABLE, Set.of(SemanticToken.Modifier.VARIABLE_GLOBAL));
                }
                break;

            case CONSTANT:
                this.addSemanticToken(node, SemanticToken.Type.VARIABLE, Set.of(SemanticToken.Modifier.READONLY));
                break;

            default:
                break;
        }
    }

    private void walkPostIdentifierParameter(final AstNode node) {
        this.addSemanticToken(node, SemanticToken.Type.PARAMETER);
    }

    @Override
    protected void walkPostSlot(final AstNode node) {
        final AstNode identifierNode = node.getFirstChild(MagikGrammar.IDENTIFIER);
        if (identifierNode == null) {
            return;
        }

        this.addSemanticToken(identifierNode, SemanticToken.Type.PROPERTY);
    }

    @Override
    protected void walkPostPackageSpecification(final AstNode node) {
        final AstNode identifierNode = node.getFirstDescendant(MagikGrammar.IDENTIFIER);
        this.currentPakkage = identifierNode.getTokenValue();
    }

    private boolean isKnownType(final GlobalReference globalReference) {
        final ITypeKeeper typeKeeper = this.magikFile.getTypeKeeper();
        return typeKeeper.getType(globalReference) instanceof MagikType;
    }

}
