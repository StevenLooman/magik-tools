package nl.ramsolutions.sw.magik.parser;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.GenericTokenType;
import com.sonar.sslr.api.RecognitionException;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.Trivia;
import com.sonar.sslr.impl.Parser;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.api.ExtendedTokenType;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.api.NewDocGrammar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.sslr.parser.LexerlessGrammar;
import org.sonar.sslr.parser.ParserAdapter;

/**
 * Parses magik-tools style documentation comments.
 *
 * <p>
 * For example, for a method/procedure:
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
 * </p>
 *
 * <p>
 * For example, for a exemplar definition:
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
 * </p>
 */
public class NewDocParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(NewDocParser.class);

    private final Parser<LexerlessGrammar> parser = new ParserAdapter<>(StandardCharsets.UTF_8, NewDocGrammar.create());
    private final List<Token> tokens;
    private AstNode newDocNode;

    /**
     * Constructor.
     * @param node {{AstNode}} to analyze.
     */
    public NewDocParser(final AstNode node) {
        this(NewDocParser.getCommentTokens(node));
        // No node type check, to be able to parse shared constants etc.
    }

    public NewDocParser(final List<Token> docTokens) {
        this.tokens = Collections.unmodifiableList(docTokens);
    }

    private static List<Token> getCommentTokens(final AstNode node) {
        final Predicate<AstNode> predicate =
            predNode -> node == predNode
                        || predNode.isNot(MagikGrammar.PROCEDURE_DEFINITION);
        return AstQuery.dfs(node, predicate)
                .map(AstNode::getToken)
                .filter(Objects::nonNull)
                .distinct()
                .flatMap(token -> token.getTrivia().stream())
                .filter(Trivia::isComment)
                .map(Trivia::getToken)
                .filter(token -> token.getValue().startsWith("##"))
                .collect(Collectors.toUnmodifiableList());
    }

    private AstNode parseNewDocSafely() {
        try {
            return this.parseNewDoc();
        } catch (RecognitionException exception) {
            LOGGER.debug(exception.getMessage(), exception);
            return this.buildSyntaxErrorNode(exception);
        }
    }

    @SuppressWarnings("java:S3011")
    private AstNode parseNewDoc() {
        // Build comment.
        final String comments = this.tokens.stream()
            .map(Token::getValue)
            .collect(Collectors.joining("\n"));

        // Parse newdoc.
        final AstNode node = this.parser.parse(comments);

        // Nothing parsed, nothing to fix.
        if (comments.isEmpty()) {
            return node;
        }

        // Update tokens line + column.
        node.getTokens().forEach(token -> {
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

    private AstNode buildSyntaxErrorNode(RecognitionException recognitionException) {
        // Build comment.
        final String comments = this.tokens.stream()
            .map(Token::getOriginalValue)
            .collect(Collectors.joining("\n"));

        int line = recognitionException.getLine();
        final String[] lines = comments.split("\n");
        if (lines.length <= line) {
            line = lines.length;
        }

        // parse message, as the exception doesn't provide the raw value
        final String message = recognitionException.getMessage();
        final Pattern pattern = Pattern.compile("Parse error at line (\\d+) column (\\d+):.*");
        final Matcher matcher = pattern.matcher(message);
        if (!matcher.find()) {
            throw new IllegalStateException("Unrecognized RecognitionException message");
        }
        final String columnStr = matcher.group(2);
        int column = Integer.parseInt(columnStr) - 1;
        final String offendingLineStr = lines[line - 1];
        if (column >= offendingLineStr.length()) {
            // Don't break things!
            column = offendingLineStr.length() - 1;
        }

        final URI uri = URI.create("magik://syntax_error");    // This is later updated.
        final Token syntaxErrorToken = Token.builder()
                .setValueAndOriginalValue(comments)
                .setURI(uri)
                .setLine(line)
                .setColumn(column)
                .setType(ExtendedTokenType.SYNTAX_ERROR)
                .build();

        final AstNode dummyNode = new AstNode(MagikGrammar.MAGIK, "NEW_DOC", null);

        final AstNode errorNode = new AstNode(MagikGrammar.SYNTAX_ERROR, "SYNTAX_ERROR", syntaxErrorToken);
        dummyNode.addChild(errorNode);

        int eofLine = lines.length;
        int eofColumn = 0;
        if (comments.endsWith("\n")) {
            eofLine += 1;
        } else {
            eofColumn = lines[lines.length - 1].length();
        }
        final Token eofToken = Token.builder()
                .setValueAndOriginalValue("")
                .setURI(uri)
                .setLine(eofLine)
                .setColumn(eofColumn)
                .setType(ExtendedTokenType.SYNTAX_ERROR)
                .build();
        final AstNode eofNode = new AstNode(GenericTokenType.EOF, "EOF", eofToken);
        dummyNode.addChild(eofNode);

        return dummyNode;
    }

    /**
     * Get NewDoc AstNode.
     * @return Parsed AstNode.
     */
    public AstNode getNewDocNode() {
        if (this.newDocNode == null) {
            this.newDocNode = this.parseNewDocSafely();
        }

        return this.newDocNode;
    }

    /**
     * Get @param types.
     * @return Map with @param types, keyed on @param name, values on @param type.
     */
    public Map<String, String> getParameterTypes() {
        final AstNode node = this.getNewDocNode();
        return node.getChildren(NewDocGrammar.PARAM).stream()
            .filter(this::noEmptyName)
            .collect(Collectors.toMap(
                this::getName,
                this::getTypeName));
    }

    /**
     * Get @param nodes + @param type names.
     * @return
     */
    public Map<AstNode, String> getParameterTypeNodes() {
        final AstNode node = this.getNewDocNode();
        return node.getChildren(NewDocGrammar.PARAM).stream()
            .filter(this::hasTypeNode)
            .collect(Collectors.toMap(
                this::getTypeNode,
                this::getTypeName));
    }

    /**
     * Get parameter name node + names.
     * @return
     */
    public Map<AstNode, String> getParameterNameNodes() {
        final AstNode node = this.getNewDocNode();
        return node.getChildren(NewDocGrammar.PARAM).stream()
            .filter(this::noEmptyName)
            .collect(Collectors.toMap(
                this::getNameNode,
                this::getName));
    }

    /**
     * Get @return types.
     * @return List with @return types.
     */
    public List<String> getReturnTypes() {
        final AstNode node = this.getNewDocNode();
        return node.getChildren(NewDocGrammar.RETURN).stream()
            .map(this::getTypeName)
            .collect(Collectors.toList());
    }

    /**
     * Get @return type nodes + names.
     * @return Map with @return type nodes + type names.
     */
    public Map<AstNode, String> getReturnTypeNodes() {
        final AstNode node = this.getNewDocNode();
        return node.getChildren(NewDocGrammar.RETURN).stream()
            .filter(this::hasTypeNode)
            .collect(Collectors.toMap(
                this::getTypeNode,
                this::getTypeName));
    }

    /**
     * Get @loop types.
     * @return List with @loop types.
     */
    public List<String> getLoopTypes() {
        final AstNode node = this.getNewDocNode();
        return node.getChildren(NewDocGrammar.LOOP).stream()
            .map(this::getTypeName)
            .collect(Collectors.toList());
    }

    /**
     * Get @loop type nodes + names.
     * @return List with @loop type nodes + type names.
     */
    public Map<AstNode, String> getLoopTypeNodes() {
        final AstNode node = this.getNewDocNode();
        return node.getChildren(NewDocGrammar.LOOP).stream()
            .filter(this::hasTypeNode)
            .collect(Collectors.toMap(
                this::getTypeNode,
                this::getTypeName));
    }

    /**
     * Get slot types.
     * @return Map with slot types, keyed on slot name, values on slot type.
     */
    public Map<String, String> getSlotTypes() {
        final AstNode node = this.getNewDocNode();
        return node.getChildren(NewDocGrammar.SLOT).stream()
            .filter(this::noEmptyName)
            .collect(Collectors.toMap(
                this::getName,
                this::getTypeName));
    }

    /**
     * Get slot type nodes + name.
     * @return
     */
    public Map<AstNode, String> getSlotTypeNodes() {
        final AstNode node = this.getNewDocNode();
        return node.getChildren(NewDocGrammar.SLOT).stream()
            .filter(this::hasTypeNode)
            .collect(Collectors.toMap(
                this::getTypeNode,
                this::getTypeName));
    }

    /**
     * Get slot name nodes + name.
     * @return
     */
    public Map<String, AstNode> getSlotNameNodes() {
        final AstNode node = this.getNewDocNode();
        return node.getChildren(NewDocGrammar.SLOT).stream()
            .filter(this::noEmptyName)
            .collect(Collectors.toMap(
                this::getName,
                slotNode -> slotNode.getFirstChild(NewDocGrammar.NAME)));
    }

    private boolean noEmptyName(final AstNode node) {
        return !this.getName(node).isBlank();
    }

    private String getName(final AstNode node) {
        final AstNode nameNode = node.getFirstChild(NewDocGrammar.NAME);
        final String tokenValue = nameNode.getTokenValue();
        return Objects.requireNonNullElse(tokenValue, "");
    }

    private AstNode getNameNode(final AstNode node) {
        return node.getFirstChild(NewDocGrammar.NAME);
    }

    private boolean hasTypeNode(final AstNode node) {
        return node.getFirstChild(NewDocGrammar.TYPE) != null;
    }

    private String getTypeName(final AstNode node) {
        final AstNode typeNode = node.getFirstChild(NewDocGrammar.TYPE);
        if (typeNode == null) {
            return "";
        }

        return typeNode.getTokens().stream()
            .filter(token -> !token.getValue().equals("{"))
            .filter(token -> !token.getValue().equals("}"))
            .map(Token::getValue)
            .collect(Collectors.joining());
    }

    private AstNode getTypeNode(final AstNode node) {
        return node.getFirstChild(NewDocGrammar.TYPE);
    }

}
