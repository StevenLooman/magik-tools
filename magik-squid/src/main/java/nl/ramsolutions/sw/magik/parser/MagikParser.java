package nl.ramsolutions.sw.magik.parser;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.impl.Parser;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import nl.ramsolutions.sw.FileCharsetDeterminer;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.sslr.parser.LexerlessGrammar;
import org.sonar.sslr.parser.ParserAdapter;

/**
 * Magik Parser.
 *
 * <p>
 * Parses sections, separated by $, one by one, like the Magik parser itself.
 * </p>
 */
@SuppressWarnings("java:S3011")
public class MagikParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(MagikParser.class);

    private static final Map<MagikGrammar, MagikGrammar> RULE_MAPPING = new EnumMap<>(MagikGrammar.class);

    static {
        RULE_MAPPING.put(MagikGrammar.BLOCK_SYNTAX_ERROR, MagikGrammar.SYNTAX_ERROR);
        RULE_MAPPING.put(MagikGrammar.CATCH_SYNTAX_ERROR, MagikGrammar.SYNTAX_ERROR);
        RULE_MAPPING.put(MagikGrammar.LOCK_SYNTAX_ERROR, MagikGrammar.SYNTAX_ERROR);
        RULE_MAPPING.put(MagikGrammar.LOOP_SYNTAX_ERROR, MagikGrammar.SYNTAX_ERROR);
        RULE_MAPPING.put(MagikGrammar.PROTECT_SYNTAX_ERROR, MagikGrammar.SYNTAX_ERROR);
        RULE_MAPPING.put(MagikGrammar.TRY_SYNTAX_ERROR, MagikGrammar.SYNTAX_ERROR);
        RULE_MAPPING.put(MagikGrammar.PROCEDURE_DEFINITION_SYNTAX_ERROR, MagikGrammar.SYNTAX_ERROR);
        RULE_MAPPING.put(MagikGrammar.METHOD_DEFINITION_SYNTAX_ERROR, MagikGrammar.SYNTAX_ERROR);
        RULE_MAPPING.put(MagikGrammar.IF_SYNTAX_ERROR, MagikGrammar.SYNTAX_ERROR);
        RULE_MAPPING.put(MagikGrammar.ARGUMENTS_PAREN_SYNTAX_ERROR, MagikGrammar.SYNTAX_ERROR);
        RULE_MAPPING.put(MagikGrammar.ARGUMENTS_SQUARE_SYNTAX_ERROR, MagikGrammar.SYNTAX_ERROR);
        RULE_MAPPING.put(MagikGrammar.PARAMETERS_PAREN_SYNTAX_ERROR, MagikGrammar.SYNTAX_ERROR);
        RULE_MAPPING.put(MagikGrammar.PARAMETERS_SQUARE_SYNTAX_ERROR, MagikGrammar.SYNTAX_ERROR);
        RULE_MAPPING.put(MagikGrammar.SIMPLE_VECTOR_SYNTAX_ERROR, MagikGrammar.SYNTAX_ERROR);
        RULE_MAPPING.put(MagikGrammar.STATEMENT_SYNTAX_ERROR, MagikGrammar.SYNTAX_ERROR);

        RULE_MAPPING.put(MagikGrammar.ARGUMENTS_PAREN, MagikGrammar.ARGUMENTS);
        RULE_MAPPING.put(MagikGrammar.ARGUMENTS_SQUARE, MagikGrammar.ARGUMENTS);
        RULE_MAPPING.put(MagikGrammar.PARAMETERS_PAREN, MagikGrammar.PARAMETERS);
        RULE_MAPPING.put(MagikGrammar.PARAMETERS_SQUARE, MagikGrammar.PARAMETERS);
    }

    private final Parser<LexerlessGrammar> parser;

    /**
     * Constructor with default charset.
     */
    public MagikParser() {
        this.parser = new ParserAdapter<>(StandardCharsets.ISO_8859_1, MagikGrammar.create());
    }

    /**
     * Parse a string and return the AstNode.
     * IOExceptions are caught, not handled.
     *
     * @param source Source to parse
     * @return Tree
     */
    public AstNode parseSafe(final String source) {
        try {
            return this.parse(source);
        } catch (final IOException exception) {
            LOGGER.error(exception.getMessage(), exception);
        }
        return null;
    }

    /**
     * Parse a file and return the AstNode.
     * IOExceptions are caught, not handled.
     *
     * @param path Path to file
     * @return Tree
     * @throws IOException -
     */
    public AstNode parseSafe(final Path path) {
        try {
            return this.parse(path);
        } catch (final IOException exception) {
            LOGGER.error(exception.getMessage(), exception);
        }
        return null;
    }

    /**
     * Parse a string and return the AstNode.
     *
     * @param source Source to parse
     * @return Tree
     * @throws IOException -
     */
    public AstNode parse(final String source) throws IOException {
        // final StringReader reader = new StringReader(source);
        // return this.parse(reader);
        final AstNode magikNode = this.parser.parse(source);

        // Update identifiers.
        this.updateIdentifiersSymbolsCasing(magikNode);

        // Apply RULE_MAPPING.
        this.applyRuleMapping(magikNode);

        return magikNode;
    }

    /**
     * Parse a file and return the AstNode.
     *
     * @param path Path to file
     * @return Tree
     * @throws IOException -
     */
    public AstNode parse(final Path path) throws IOException {
        final File file = path.toFile();
        final Charset charset = FileCharsetDeterminer.determineCharset(path);
        try (FileReader reader = new FileReader(file, charset)) {
            final AstNode magikNode = this.parser.parse(file);

            // Update identifiers.
            this.updateIdentifiersSymbolsCasing(magikNode);

            // Apply RULE_MAPPING.
            this.applyRuleMapping(magikNode);

            return magikNode;
        }
    }

    @SuppressWarnings("checkstyle:NestedIfDepth")
    private void applyRuleMapping(final AstNode node) {
        final AstNodeType type = node.getType();
        if (MagikParser.RULE_MAPPING.containsKey(type)) {
            final AstNodeType newType = MagikParser.RULE_MAPPING.get(type);
            final String newName = newType.toString();

            // Convert node to new type.
            try {
                final Field fieldType = AstNode.class.getDeclaredField("type");
                fieldType.setAccessible(true);
                fieldType.set(node, newType);

                final Field fieldName = AstNode.class.getDeclaredField("name");
                fieldName.setAccessible(true);
                fieldName.set(node, newName);
            } catch (final ReflectiveOperationException exception) {
                LOGGER.error(exception.getMessage(), exception);
            }
        }

        node.getChildren().forEach(this::applyRuleMapping);
    }

    /**
     * Recusrively update URI for AstNode/Tokens.
     * @param node Node to start at.
     * @param newUri New URI to set.
     */
    public static void updateUri(final AstNode node, final URI newUri) {
        final Token token = node.getToken();
        if (token != null) {
            try {
                final Field fieldUri = token.getClass().getDeclaredField("uri");
                fieldUri.setAccessible(true);
                fieldUri.set(token, newUri);
            } catch (final ReflectiveOperationException exception) {
                LOGGER.error(exception.getMessage(), exception);
            }
        }

        node.getChildren().forEach(childNode -> MagikParser.updateUri(childNode, newUri));
    }

    /**
     * Update token value.
     *
     * @param node Node to update.
     */
    private void updateIdentifiersSymbolsCasing(final AstNode node) {
        try {
            final Field field = Token.class.getDeclaredField("value");
            field.setAccessible(true);
            if (node.is(MagikGrammar.IDENTIFIER)
                || node.is(MagikGrammar.SYMBOL)) {
                final Token token = node.getToken();
                final String value = token.getValue();
                final String newValue = MagikParser.parseIdentifier(value);
                field.set(token, newValue);
            }
        } catch (final ReflectiveOperationException exception) {
            LOGGER.error(exception.getMessage(), exception);
        }

        node.getChildren()
            .forEach(this::updateIdentifiersSymbolsCasing);
    }

    /**
     * Parse an identifier.
     *
     * @param value Identifier to parse.
     * @return Parsed identifier.
     */
    @SuppressWarnings({"java:S127", "checkstyle:ModifiedControlVariable"})
    static String parseIdentifier(final String value) {
        // if |, read until next |
        // if \\., read .
        // else read lowercase
        final StringBuilder builder = new StringBuilder(value.length());

        for (int i = 0; i < value.length(); ++i) {
            char chr = value.charAt(i);
            if (chr == '|') {
                // piped segment
                ++i; // skip first |
                // read until next |
                for (; i < value.length(); ++i) {
                    chr = value.charAt(i);
                    if (chr == '|') {
                        break;
                    }
                    builder.append(chr);
                }
            } else if (chr == '\\') {
                // escaped character
                ++i; // skip \
                chr = value.charAt(i);
                builder.append(chr);
            } else {
                // normal character
                chr = Character.toLowerCase(chr);
                builder.append(chr);
            }
        }

        return builder.toString();
    }

}
