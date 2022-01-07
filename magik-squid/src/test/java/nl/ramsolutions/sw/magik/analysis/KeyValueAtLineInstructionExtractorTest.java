package nl.ramsolutions.sw.magik.analysis;

import com.sonar.sslr.api.AstNode;
import java.util.Map;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.parser.MagikParser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test KeyValueAtLineInstructionExtractor.
 */
@SuppressWarnings("checkstyle:MagicNumber")
class KeyValueAtLineInstructionExtractorTest {

    private AstNode parse(String code) {
        MagikParser parser = new MagikParser();
        return parser.parseSafe(code);
    }

    @Test
    void testExtraction() {
        String code = ""
                + "_method object.m\n"
                + "    _local a << 10    # test: a=b; c =d; e= f\n"
                + "_endmethod";
        AstNode node = parse(code);
        KeyValueAtLineInstructionExtractor extractor = new KeyValueAtLineInstructionExtractor(node, "test");
        AstNode instructionNode =
                node.getFirstChild(MagikGrammar.METHOD_DEFINITION)
                .getFirstChild(MagikGrammar.BODY)
                .getFirstDescendant(MagikGrammar.IDENTIFIER);
        Map<String, String> instructions = extractor.getInstructions(instructionNode);
        assertThat(instructions)
                .hasSize(3)
                .contains(
                    Map.entry("a", "b"),
                    Map.entry("c", "d"),
                    Map.entry("e", "f"));
    }

}
