package nl.ramsolutions.sw.magik.parser;

import com.sonar.sslr.api.AstNode;
import java.io.IOException;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test MethodDocParser.
 */
class MethodDocParserTest {

    @Test
    void testExtractDoc() throws IOException {
        String code = ""
            + "_method a.b(param1, param2, param3?)\n"
            + "    ## Function  : example\n"
            + "    ## Parameters: PARAM1: example parameter 1\n"
            + "    ##             PARAM2: example parameter 2\n"
            + "    ##             PARAM3?: example parameter 3\n"
            + "    ## Returns   : -\n"
            + "    do_something()\n"
            + "    _if a\n"
            + "    _then\n"
            + "        write(a)\n"
            + "    _endif\n"
            + "_endmethod";
        MagikParser parser = new MagikParser();
        AstNode rootNode = parser.parseSafe(code);
        AstNode methodNode = rootNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        MethodDocParser docParser = new MethodDocParser(methodNode);

        assertThat(docParser.getSectionText("function")).isEqualTo("example");

        assertThat(docParser.getSectionText("parameters")).isEqualTo(""
                + "PARAM1: example parameter 1\n"
                + "PARAM2: example parameter 2\n"
                + "PARAM3?: example parameter 3");

        assertThat(docParser.getSectionText("returns")).isEqualTo("-");

        assertThat(docParser.getParameterTexts().get("param1")).isEqualTo("example parameter 1");

        assertThat(docParser.getParameterTexts().get("param2")).isEqualTo("example parameter 2");

        assertThat(docParser.getParameterTexts().get("param3?")).isEqualTo("example parameter 3");
    }

}
