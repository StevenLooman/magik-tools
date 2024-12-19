package nl.ramsolutions.sw.magik.parser;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.Trivia;
import com.sonar.sslr.impl.Parser;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.analysis.helpers.PackageNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.api.TypeDocGrammar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.sslr.parser.LexerlessGrammar;
import org.sonar.sslr.parser.ParserAdapter;

/**
 * Parses magik-tools style documentation comments.
 *
 * <p>For example, for a method/procedure:
 *
 * <pre>
 * _method example.method(p1, p2)
 *     ## Example method.
 *     ## @param {integer} p1 First parameter description.
 *     ## @param {integer|char16_vector} p2 Second parameter description.
 *     ## @return {integer} First return value description.
 *     ## @return {char16_vector} Second return value description.
 *     _return 1, "str"
 * _endmethod
 * </pre>
 *
 * <p>For example, for a exemplar definition:
 *
 * <pre>
 * _pragma(...)
 * ## Example exemplar.
 * ## @slot {sw:integer} slot1 First slot description.
 * ## @slot {sw:char16_vector} slot2 Second slot description.
 * def_slotted_exemplar(
 *     :exemple,
 *     {
 *         {:slot1, _unset},
 *         {:slot2, _unset}
 *     })
 * </pre>
 */
public class TypeDocParser {

  private static final Logger LOGGER = LoggerFactory.getLogger(TypeDocParser.class);

  private final Parser<LexerlessGrammar> parser =
      new ParserAdapter<>(StandardCharsets.ISO_8859_1, TypeDocGrammar.create());
  private final List<Token> tokens;
  private final String pakkage;
  private AstNode typeDocNode;

  /**
   * Constructor.
   *
   * @param node {@link AstNode} to analyze.
   */
  public TypeDocParser(final AstNode node) {
    this(TypeDocParser.getCommentTokens(node), new PackageNodeHelper(node).getCurrentPackage());
    // No node type check, to be able to parse shared constants etc.
  }

  public TypeDocParser(final List<Token> docTokens, final String pakkage) {
    this.tokens = Collections.unmodifiableList(docTokens);
    this.pakkage = pakkage;
  }

  private static List<Token> getCommentTokens(final AstNode node) {
    final Predicate<AstNode> predicate =
        predNode -> node == predNode || predNode.isNot(MagikGrammar.PROCEDURE_DEFINITION);
    return AstQuery.dfs(node, predicate)
        .map(AstNode::getToken)
        .filter(Objects::nonNull)
        .distinct()
        .flatMap(token -> token.getTrivia().stream())
        .filter(Trivia::isComment)
        .map(Trivia::getToken)
        .filter(token -> token.getValue().startsWith("##"))
        .toList();
  }

  @SuppressWarnings("java:S3011")
  private AstNode parseTypeDoc() {
    // Build comment.
    final String comments =
        this.tokens.stream().map(Token::getValue).collect(Collectors.joining("\n"));

    // Parse TypeDoc.
    final AstNode node = this.parser.parse(comments);

    // Nothing parsed, nothing to fix.
    if (comments.isEmpty()) {
      return node;
    }

    // Update tokens, from existing tokens.
    node.getTokens()
        .forEach(
            token -> {
              // Find original token, there is only one comment-token per line.
              final int index = token.getLine() - 1;
              final Token origToken = this.tokens.get(index);

              final int newLine = origToken.getLine();
              final int newColumn = origToken.getColumn() + token.getColumn();

              try {
                final Field lineField = Token.class.getDeclaredField("line");
                lineField.setAccessible(true);
                lineField.set(token, newLine);

                final Field columnField = Token.class.getDeclaredField("column");
                columnField.setAccessible(true);
                columnField.set(token, newColumn);
              } catch (ReflectiveOperationException exception) {
                LOGGER.error(exception.getMessage(), exception);
              }
            });

    return node;
  }

  /**
   * Get TypeDoc AstNode.
   *
   * @return Parsed AstNode.
   */
  public AstNode getTypeDocNode() {
    if (this.typeDocNode == null) {
      this.typeDocNode = this.parseTypeDoc();
    }

    return this.typeDocNode;
  }

  /**
   * Get param types.
   *
   * @return Map with @param types, keyed on name, valued on type.
   */
  public Map<String, TypeString> getParameterTypes() {
    final AstNode node = this.getTypeDocNode();
    return node.getChildren(TypeDocGrammar.PARAM).stream()
        .filter(this::noEmptyName)
        .collect(
            Collectors.toMap(astNode -> this.getName(astNode).toLowerCase(), this::getTypeString));
  }

  /**
   * Get @param nodes + type strings.
   *
   * @return get parameter type nodes (list of parameter types)
   */
  public Map<AstNode, TypeString> getParameterTypeNodes() {
    final AstNode node = this.getTypeDocNode();
    return node.getChildren(TypeDocGrammar.PARAM).stream()
        .filter(this::hasTypeNode)
        .collect(Collectors.toMap(this::getTypeNode, this::getTypeString));
  }

  /**
   * Get @param name node + names.
   *
   * @return get parameter name nodes (list of parameter nodes)
   */
  public Map<AstNode, String> getParameterNameNodes() {
    final AstNode node = this.getTypeDocNode();
    return node.getChildren(TypeDocGrammar.PARAM).stream()
        .filter(this::noEmptyName)
        .collect(
            Collectors.toMap(this::getNameNode, astNode -> this.getName(astNode).toLowerCase()));
  }

  /**
   * Get generic types.
   *
   * @return generic types
   */
  public List<TypeString> getGenericTypes() {
    final AstNode node = this.getTypeDocNode();
    return node.getChildren(TypeDocGrammar.GENERIC).stream()
        .filter(this::noEmptyName)
        .map(this::getTypeString)
        .toList();
  }

  /**
   * Get @generic nodes + type strings.
   *
   * @return generic type nodes
   */
  public Map<AstNode, TypeString> getGenericTypeNodes() {
    final AstNode node = this.getTypeDocNode();
    return node.getChildren(TypeDocGrammar.GENERIC).stream()
        .filter(this::hasTypeNode)
        .collect(
            Collectors.toMap(
                this::getTypeNode, this::getTypeString, (a, b) -> a, LinkedHashMap::new));
  }

  /**
   * Get @return types.
   *
   * @return List with @return types.
   */
  public List<TypeString> getReturnTypes() {
    final AstNode node = this.getTypeDocNode();
    return node.getChildren(TypeDocGrammar.RETURN).stream().map(this::getTypeString).toList();
  }

  /**
   * Get return type nodes + names, ordered via {@code LinkedHashMap}.
   *
   * @return Map with @return type nodes + type names.
   */
  public Map<AstNode, TypeString> getReturnTypeNodes() {
    final AstNode node = this.getTypeDocNode();
    return node.getChildren(TypeDocGrammar.RETURN).stream()
        .filter(this::hasTypeNode)
        .collect(
            Collectors.toMap(
                this::getTypeNode, this::getTypeString, (a, b) -> a, LinkedHashMap::new));
  }

  /**
   * Get @loop types.
   *
   * @return List with @loop types.
   */
  public List<TypeString> getLoopTypes() {
    final AstNode node = this.getTypeDocNode();
    return node.getChildren(TypeDocGrammar.LOOP).stream().map(this::getTypeString).toList();
  }

  /**
   * Get @loop type nodes + names.
   *
   * @return Map with @loop type nodes + type names.
   */
  public Map<AstNode, TypeString> getLoopTypeNodes() {
    final AstNode node = this.getTypeDocNode();
    return node.getChildren(TypeDocGrammar.LOOP).stream()
        .filter(this::hasTypeNode)
        .collect(Collectors.toMap(this::getTypeNode, this::getTypeString));
  }

  /**
   * Get @slot types.
   *
   * @return Map with @slot types, keyed on name, valued on type.
   */
  public Map<String, TypeString> getSlotTypes() {
    final AstNode node = this.getTypeDocNode();
    return node.getChildren(TypeDocGrammar.SLOT).stream()
        .filter(this::noEmptyName)
        .collect(Collectors.toMap(this::getName, this::getTypeString));
  }

  /**
   * Get @slot type nodes + types.
   *
   * @return get slot type nodes (the slots with their respective type)
   */
  public Map<AstNode, TypeString> getSlotTypeNodes() {
    final AstNode node = this.getTypeDocNode();
    return node.getChildren(TypeDocGrammar.SLOT).stream()
        .filter(this::hasTypeNode)
        .collect(Collectors.toMap(this::getTypeNode, this::getTypeString));
  }

  /**
   * Get @slot name nodes + name.
   *
   * @return the slot name nodes (the names of the slots and their corresponding node)
   */
  public Map<AstNode, String> getSlotNameNodes() {
    final AstNode node = this.getTypeDocNode();
    return node.getChildren(TypeDocGrammar.SLOT).stream()
        .filter(this::noEmptyName)
        .collect(
            Collectors.toMap(
                slotNode -> slotNode.getFirstChild(TypeDocGrammar.NAME), this::getName));
  }

  /**
   * Get documentations of the parameters
   *
   * @return a map for the documentation for every parameter
   */
  public Map<String, String> getDocumentationForParameters() {
    return getDocumentationForCommentNodes(this.getParameterNameNodes());
  }

  public Map<String, String> getDocumentationForSlots() {
    return getDocumentationForCommentNodes(this.getSlotNameNodes());
  }

  private Map<String, String> getDocumentationForCommentNodes(final Map<AstNode, String> nodes) {
    return nodes.entrySet().stream()
        .collect(
            Collectors.toMap(
                Map.Entry::getValue,
                entry ->
                    entry.getKey().getParent().getChildren(TypeDocGrammar.DESCRIPTION).stream()
                        .flatMap(
                            node ->
                                node.getChildren().stream()
                                    .filter(token -> token.isNot(TypeDocGrammar.DOC_START))
                                    .map(AstNode::getTokenValue))
                        .collect(Collectors.joining(" "))));
  }

  private boolean noEmptyName(final AstNode node) {
    return !this.getName(node).isBlank();
  }

  private String getName(final AstNode node) {
    final AstNode nameNode = node.getFirstChild(TypeDocGrammar.NAME);
    final String tokenValue = nameNode.getTokenValue();
    return Objects.requireNonNullElse(tokenValue, "");
  }

  private AstNode getNameNode(final AstNode node) {
    return node.getFirstChild(TypeDocGrammar.NAME);
  }

  private boolean hasTypeNode(final AstNode node) {
    return node.getFirstChild(TypeDocGrammar.TYPE) != null;
  }

  private TypeString getTypeString(final AstNode node) {
    final AstNode typeNode = node.getFirstChild(TypeDocGrammar.TYPE);
    if (typeNode == null) {
      return TypeString.UNDEFINED;
    }

    final String value =
        typeNode.getTokens().stream()
            .filter(token -> !token.getValue().equals("{") && !token.getValue().equals("}"))
            .map(Token::getValue)
            .collect(Collectors.joining());
    return TypeStringParser.parseTypeString(value, this.pakkage);
  }

  private AstNode getTypeNode(final AstNode node) {
    return node.getFirstChild(TypeDocGrammar.TYPE);
  }
}
