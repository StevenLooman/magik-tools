package nl.ramsolutions.sw.magik.languageserver.semantictokens;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.Trivia;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.AstWalker;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.TypeStringDefinition;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.PackageNodeHelper;
import nl.ramsolutions.sw.magik.analysis.scope.GlobalScope;
import nl.ramsolutions.sw.magik.analysis.scope.Scope;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeStringResolver;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasonerState;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.api.MagikKeyword;
import nl.ramsolutions.sw.magik.api.MagikOperator;
import nl.ramsolutions.sw.magik.api.TypeDocGrammar;
import nl.ramsolutions.sw.magik.api.TypeStringGrammar;
import nl.ramsolutions.sw.magik.parser.TypeDocParser;
import nl.ramsolutions.sw.magik.parser.TypeStringParser;

/** Semantic token walker. */
public class SemanticTokenWalker extends AstWalker {

  private static final String DEFAULT_PACKAGE = "user";
  private static final String TOPIC_DEPRECATED = "deprecated";

  private static final List<String> MAGIK_MODIFIER_VALUES =
      List.of(
          MagikKeyword.PRIVATE.getValue(),
          MagikKeyword.ABSTRACT.getValue(),
          MagikKeyword.ITER.getValue());

  private static final Map<TypeDocGrammar, SemanticToken.Type> TYPE_DOC_ELEMENT_TYPE_MAPPING =
      Map.of(
          TypeDocGrammar.PARAM, SemanticToken.Type.PARAMETER,
          TypeDocGrammar.LOOP, SemanticToken.Type.VARIABLE,
          TypeDocGrammar.SLOT, SemanticToken.Type.PROPERTY);

  private static final Set<String> MAGIK_KEYWORD_VALUES =
      Arrays.stream(MagikKeyword.values())
          .filter(
              keyword ->
                  keyword != MagikKeyword.SELF
                      && keyword != MagikKeyword.CLONE
                      && keyword != MagikKeyword.SUPER
                      && keyword != MagikKeyword.TRUE
                      && keyword != MagikKeyword.FALSE
                      && keyword != MagikKeyword.MAYBE
                      && keyword != MagikKeyword.UNSET)
          .map(MagikKeyword::getValue)
          .collect(Collectors.toUnmodifiableSet());

  private static final Set<String> MAGIK_KEYWORD_VALUES_CONSTANTS =
      Set.of(
          MagikKeyword.SELF.getValue(),
          MagikKeyword.CLONE.getValue(),
          MagikKeyword.SUPER.getValue(),
          MagikKeyword.TRUE.getValue(),
          MagikKeyword.FALSE.getValue(),
          MagikKeyword.MAYBE.getValue(),
          MagikKeyword.UNSET.getValue());

  private static final List<String> MAGIK_OPERATOR_VALUES =
      Arrays.stream(MagikOperator.values()).map(MagikOperator::getValue).toList();

  private final MagikTypedFile magikFile;
  private final List<SemanticToken> semanticTokens = new ArrayList<>();
  private String currentPakkage = DEFAULT_PACKAGE;

  /**
   * Constructor.
   *
   * @param magikFile {@link MagikTypedFile} to operate on.
   */
  SemanticTokenWalker(final MagikTypedFile magikFile) {
    this.magikFile = magikFile;
  }

  public List<SemanticToken> getSemanticTokens() {
    return this.semanticTokens;
  }

  private void addSemanticToken(
      final Token token,
      final SemanticToken.Type type,
      final Set<SemanticToken.Modifier> modifiers) {
    final SemanticToken semanticToken = new SemanticToken(token, type, modifiers);
    this.semanticTokens.add(semanticToken);
  }

  private void addSemanticToken(final Token token, final SemanticToken.Type type) {
    Set<SemanticToken.Modifier> modifiers = Collections.emptySet();
    this.addSemanticToken(token, type, modifiers);
  }

  private void addSemanticToken(
      final AstNode node,
      final SemanticToken.Type type,
      final Set<SemanticToken.Modifier> modifiers) {
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
      final TypeDocParser typeDocParser = new TypeDocParser(docTokens, this.currentPakkage);
      final AstNode typeDocNode = typeDocParser.getTypeDocNode();

      // It is either a FUNCTION node or element nodes.
      final AstNode functionNode = typeDocNode.getFirstChild(TypeDocGrammar.FUNCTION);
      if (functionNode != null) {
        this.addSemanticToken(token, SemanticToken.Type.COMMENT, docModifier);
      }

      final List<AstNode> elementNodes =
          typeDocNode.getChildren(
              TypeDocGrammar.PARAM,
              TypeDocGrammar.RETURN,
              TypeDocGrammar.LOOP,
              TypeDocGrammar.SLOT);
      elementNodes.forEach(
          elementNode -> {
            // DOC_START
            final AstNode docStartNode = elementNode.getFirstChild(TypeDocGrammar.DOC_START);
            this.addSemanticToken(docStartNode, SemanticToken.Type.COMMENT, docModifier);

            // Element
            final AstNode keywordNode = elementNode.getChildren().get(1);
            this.addSemanticToken(keywordNode, SemanticToken.Type.KEYWORD, docModifier);

            // TYPE
            final AstNode typeNode = elementNode.getFirstChild(TypeDocGrammar.TYPE);
            if (typeNode != null) {
              this.walkCommentType(typeNode, docModifier);
            }

            // NAME
            final AstNode nameNode = elementNode.getFirstChild(TypeDocGrammar.NAME);
            if (nameNode != null && nameNode.getToken() != null) {
              final SemanticToken.Type type =
                  TYPE_DOC_ELEMENT_TYPE_MAPPING.get(elementNode.getType());
              this.addSemanticToken(nameNode, type, docModifier);
            }

            // DESCRIPTION
            final List<AstNode> descriptionNodes =
                elementNode.getChildren(TypeDocGrammar.DESCRIPTION);
            descriptionNodes.forEach(
                descriptionNode ->
                    this.addSemanticToken(
                        descriptionNode, SemanticToken.Type.COMMENT, docModifier));
          });
    } else {
      this.addSemanticToken(token, SemanticToken.Type.COMMENT);
    }
  }

  private void walkCommentType(
      final AstNode typeNode, final Set<SemanticToken.Modifier> docModifier) {
    final Set<SemanticToken.Modifier> constModifier =
        Set.of(SemanticToken.Modifier.DOCUMENTATION, SemanticToken.Modifier.READONLY);
    final List<AstNode> typeValueNodes = typeNode.getChildren(TypeDocGrammar.TYPE_VALUE);
    if (!typeValueNodes.isEmpty()) {
      final AstNode typeValueNode = typeValueNodes.get(0);
      final AstNode typeStringNode = TypeStringParser.getParsedNodeForTypeString(typeValueNode);
      final List<AstNode> typeNodes =
          typeStringNode.getDescendants(
              TypeStringGrammar.TYPE_IDENTIFIER,
              TypeStringGrammar.TYPE_CLONE,
              TypeStringGrammar.TYPE_SELF,
              TypeStringGrammar.TYPE_PARAMETER_REFERENCE,
              TypeStringGrammar.TYPE_GENERIC_DEFINITION,
              TypeStringGrammar.TYPE_GENERIC_REFERENCE);
      typeNodes.forEach(
          typeTypeNode -> {
            final String identifier = typeTypeNode.getTokenValue();
            final TypeString typeString = TypeString.ofIdentifier(identifier, this.currentPakkage);
            if (typeTypeNode.is(TypeStringGrammar.TYPE_CLONE, TypeStringGrammar.TYPE_SELF)) {
              this.addSemanticToken(typeTypeNode, SemanticToken.Type.CLASS, constModifier);
            } else if (typeTypeNode.is(TypeStringGrammar.TYPE_PARAMETER_REFERENCE)) {
              this.addSemanticToken(typeTypeNode, SemanticToken.Type.KEYWORD, docModifier);

              // Color the parameter name.
              final AstNode refNode = typeTypeNode.getChildren().get(2);
              this.addSemanticToken(refNode, SemanticToken.Type.PARAMETER, docModifier);
            } else if (typeTypeNode.is(TypeStringGrammar.TYPE_GENERIC_DEFINITION)) {
              final AstNode nameNode = typeTypeNode.getChildren().get(0);
              this.addSemanticToken(nameNode, SemanticToken.Type.TYPE_PARAMETER, docModifier);

              final AstNode genericTypeNode = typeTypeNode.getChildren().get(2);
              this.addSemanticToken(genericTypeNode, SemanticToken.Type.CLASS, docModifier);
            } else if (typeTypeNode.is(TypeStringGrammar.TYPE_GENERIC_REFERENCE)) {
              final AstNode nameNode = typeTypeNode.getChildren().get(0);
              this.addSemanticToken(nameNode, SemanticToken.Type.TYPE_PARAMETER, docModifier);
            } else if (this.isKnownType(typeString)) {
              this.addSemanticToken(typeTypeNode, SemanticToken.Type.CLASS, docModifier);
            }
          });
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
    if (previousSiblingNode == null || !previousSiblingNode.is(MagikGrammar.ATOM)) {
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

    // Test for deprecation.
    final MethodInvocationNodeHelper helper = new MethodInvocationNodeHelper(node);
    final AstNode receiverNode = helper.getReceiverNode();
    final LocalTypeReasonerState reasonerState = this.magikFile.getTypeReasonerState();
    final ExpressionResultString result = reasonerState.getNodeType(receiverNode);
    final TypeString typeStr = result.get(0, TypeString.UNDEFINED);
    final String methodName = helper.getMethodName();
    final TypeStringResolver resolver = magikFile.getTypeStringResolver();
    final Set<SemanticToken.Modifier> modifiers =
        resolver.getMethodDefinitions(typeStr, methodName).stream()
                .anyMatch(method -> method.getTopics().contains(TOPIC_DEPRECATED))
            ? Set.of(SemanticToken.Modifier.DEPRECATED)
            : Collections.emptySet();

    this.addSemanticToken(identifierNode, SemanticToken.Type.METHOD, modifiers);
  }

  @Override
  protected void walkPostIdentifier(final AstNode node) {
    final AstNode parentNode = node.getParent();
    if (parentNode == null) {
      return;
    }

    if (parentNode.is(MagikGrammar.EXEMPLAR_NAME)) {
      this.addSemanticToken(node, SemanticToken.Type.CLASS);
    } else if (parentNode.is(MagikGrammar.METHOD_NAME)) {
      this.addSemanticToken(node, SemanticToken.Type.METHOD);
    } else if (parentNode.is(MagikGrammar.CONDITION_NAME)) {
      this.addSemanticToken(node, SemanticToken.Type.CLASS);
    } else if (parentNode.is(MagikGrammar.ATOM) && !this.isPartOfProcedureInvocation(node)) {
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
    if (atomNode == null || !atomNode.is(MagikGrammar.ATOM)) {
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

      case DEFINITION, IMPORT, LOCAL, RECURSIVE:
        this.addSemanticToken(node, SemanticToken.Type.VARIABLE);
        break;

      case GLOBAL, DYNAMIC:
        final TypeString typeString = TypeString.ofIdentifier(identifier, this.currentPakkage);
        final TypeStringResolver resolver = this.magikFile.getTypeStringResolver();
        final Collection<TypeStringDefinition> typeDefs = resolver.resolve(typeString);
        final ExemplarDefinition exemplarDef =
            typeDefs.stream()
                .filter(ExemplarDefinition.class::isInstance)
                .map(ExemplarDefinition.class::cast)
                .findAny()
                .orElse(null);
        if (exemplarDef != null) {
          final Set<SemanticToken.Modifier> modifiers =
              exemplarDef.getTopics().contains(TOPIC_DEPRECATED)
                  ? Set.of(
                      SemanticToken.Modifier.VARIABLE_GLOBAL, SemanticToken.Modifier.DEPRECATED)
                  : Set.of(SemanticToken.Modifier.VARIABLE_GLOBAL);
          this.addSemanticToken(node, SemanticToken.Type.CLASS, modifiers);
        } else {
          this.addSemanticToken(
              node, SemanticToken.Type.VARIABLE, Set.of(SemanticToken.Modifier.VARIABLE_GLOBAL));
        }
        break;

      case CONSTANT:
        this.addSemanticToken(
            node, SemanticToken.Type.VARIABLE, Set.of(SemanticToken.Modifier.READONLY));
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
    final PackageNodeHelper helper = new PackageNodeHelper(node);
    this.currentPakkage = helper.getCurrentPackage();
  }

  private boolean isKnownType(final TypeString typeString) {
    final TypeStringResolver resolver = this.magikFile.getTypeStringResolver();
    final Collection<TypeStringDefinition> typeDefs = resolver.resolve(typeString);
    return typeDefs.stream().anyMatch(ExemplarDefinition.class::isInstance);
  }
}
