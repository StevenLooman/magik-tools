package nl.ramsolutions.sw.magik.parser;

import com.sonar.sslr.api.AstNode;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test NewDocParser.
 */
class NewDocParserTest {

    private AstNode parseMagik(String code) {
        final MagikParser parser = new MagikParser();
        return parser.parseSafe(code);
    }

    @Test
    void testParameter() throws IOException {
        final String code = ""
            + "_method a.b(param1, param2) << param3\n"
            + "    ## @param {sw:symbol} param1 Test parameter 1.\n"
            + "    ## @param    {integer} param2 Test parameter 2.\n"
            + "    ## @param {integer} param3    Test parameter 3.\n"
            + "    ## @param {integer|float} param4 Test parameter 4.\n"
            + "_endmethod";
        final AstNode topNode = this.parseMagik(code);
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final NewDocParser docParser = new NewDocParser(methodNode);
        final Map<String, String> parameterTypes = docParser.getParameterTypes();
        assertThat(parameterTypes)
            .containsOnly(
                Map.entry("param1", "sw:symbol"),
                Map.entry("param2", "integer"),
                Map.entry("param3", "integer"),
                Map.entry("param4", "integer|float"));
    }

    @Test
    void testParameterEmpty() throws IOException {
        final String code = ""
            + "_method a.b(param1, param2) << param3\n"
            + "    ## @param \n"
            + "    ## @param param1\n"
            + "    ## @param {integer} param2\n"
            + "    ## @param {integer} param3 \n"
            + "    ## @param {integer|float} param4\n"
            + "_endmethod";
        final AstNode topNode = this.parseMagik(code);
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final NewDocParser docParser = new NewDocParser(methodNode);
        final Map<String, String> parameterTypes = docParser.getParameterTypes();
        assertThat(parameterTypes)
            .containsOnly(
                Map.entry("param1", ""),
                Map.entry("param2", "integer"),
                Map.entry("param3", "integer"),
                Map.entry("param4", "integer|float"));
    }

    @Test
    void testReturn() throws IOException {
        final String code = ""
            + "_method a.b\n"
            + "    ## @return {sw:integer} An Integer.\n"
            + "_endmethod";
        final AstNode topNode = this.parseMagik(code);
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final NewDocParser docParser = new NewDocParser(methodNode);
        final List<String> returnTypes = docParser.getReturnTypes();
        assertThat(returnTypes)
            .containsExactly("sw:integer");
    }

    @Test
    void testReturnEmpty() throws IOException {
        final String code = ""
            + "_method a.b\n"
            + "    ## @return \n"
            + "    ## @return {sw:integer}\n"
            + "    ## @return {sw:integer} \n"
            + "    ## @return {sw:integer|sw:float}\n"
            + "_endmethod";
        final AstNode topNode = this.parseMagik(code);
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final NewDocParser docParser = new NewDocParser(methodNode);
        final List<String> returnTypes = docParser.getReturnTypes();
        assertThat(returnTypes)
            .containsExactly("", "sw:integer", "sw:integer", "sw:integer|sw:float");
    }

    @Test
    void testReturnSelf() throws IOException {
        final String code = ""
            + "_method a.b\n"
            + "    ## @return {_self}\n"
            + "_endmethod";
        final AstNode topNode = this.parseMagik(code);
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final NewDocParser docParser = new NewDocParser(methodNode);
        final List<String> returnTypes = docParser.getReturnTypes();
        assertThat(returnTypes)
            .containsExactly("_self");
    }

    @Test
    void testNestedProcedure() throws IOException {
        String code = ""
            + "_method a.b\n"
            + "    ## @return {sw:integer} Top\n"
            + "    rope.map("
            + "        _proc(item)\n"
            + "            ## @return {sw:float} Nested\n"
            + "        _endproc())\n"
            + "_endmethod";
        final AstNode topNode = this.parseMagik(code);
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final NewDocParser methodDocParser = new NewDocParser(methodNode);
        final List<String> methodReturnTypes = methodDocParser.getReturnTypes();
        assertThat(methodReturnTypes)
            .containsExactly("sw:integer");

        final AstNode procNode = topNode.getFirstDescendant(MagikGrammar.PROCEDURE_DEFINITION);
        final NewDocParser procDocParser = new NewDocParser(procNode);
        final List<String> procReturnTypes = procDocParser.getReturnTypes();
        assertThat(procReturnTypes)
            .containsExactly("sw:float");
    }

    @Test
    void testSlotTypes() throws IOException {
        final String code = ""
            + "## @slot slot1 Slot 1.\n"
            + "## @slot {integer} slot2 Slot 2.\n"
            + "def_slotted_exemplar(:example,\n"
            + "    {\n"
            + "        {:slot1, _unset}\n"
            + "    })\n";
        final AstNode topNode = this.parseMagik(code);
        final AstNode definitionNode = topNode.getFirstChild(MagikGrammar.STATEMENT);
        final NewDocParser docParser = new NewDocParser(definitionNode);
        final Map<String, String> slotTypes = docParser.getSlotTypes();
        assertThat(slotTypes)
            .containsOnly(
                Map.entry("slot1", ""),
                Map.entry("slot2", "integer"));
    }

    @Test
    void testTokenLines() throws IOException {
        final String code = ""
            + "_proc@test1(param1)\n"
            + "    ## @param {sw:symbol} param1 Test parameter 1.\n"
            + "_endproc";
        final AstNode topNode = this.parseMagik(code);
        final AstNode definitionNode = topNode.getFirstDescendant(MagikGrammar.PROCEDURE_DEFINITION);
        final NewDocParser docParser = new NewDocParser(definitionNode);
        final Map<AstNode, String> parameterNameNodes = docParser.getParameterNameNodes();
        assertThat(parameterNameNodes)
            .hasSize(1);

        final List<Object> parameterNodes = List.of(parameterNameNodes.keySet().toArray());
        final AstNode parameterNode = (AstNode) parameterNodes.get(0);
        assertThat(parameterNode.getTokenLine())
            .isEqualTo(2);
    }

}
