package nl.ramsolutions.sw.magik.metrics;

import java.net.URI;
import java.util.Set;
import nl.ramsolutions.sw.magik.MagikFile;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test FileMetrics.
 */
@SuppressWarnings("checkstyle:MagicNumber")
class FileMetricsTest {

    @Test
    void testStatements1() {
        String code = "print(1)";
        assertThat(this.metrics(code).numberOfStatements()).isEqualTo(1);
    }

    @Test
    void testMethodDefinition1() {
        String code = "_method a.b _endmethod";
        assertThat(this.metrics(code).numberOfMethods()).isEqualTo(1);
    }

    @Test
    void testProcedureDefinition1() {
        String code = "_proc() _endproc";
        assertThat(this.metrics(code).numberOfProcedures()).isEqualTo(1);
    }

    @Test
    void testExemplarDefinition1() {
        String code = "def_slotted_exemplar(:test, {})";
        assertThat(this.metrics(code).numberOfExemplars()).isEqualTo(1);
    }

    @Test
    void testExemplarDefinition2() {
        String code = "def_indexed_exemplar(:test, {})";
        assertThat(this.metrics(code).numberOfExemplars()).isEqualTo(1);
    }

    @Test
    void testFileComplexity1() {
        String code = "_if a = b _then _endif";
        assertThat(this.metrics(code).fileComplexity()).isEqualTo(2);
    }

    @Test
    void testExecutableLines1() {
        String code = "print(1)";
        Set<Integer> expected = Set.of(1);
        assertThat(this.metrics(code).executableLines()).isEqualTo(expected);
    }

    @Test
    void testExecutableLines2() {
        String code = ""
                + "_pragma(classify_level=basic, topic={test})\n"
                + "_method a.b\n"
                + "    ## method header\n"
                + "    # comment\n"
                + "    print(1)\n"
                + "    _return _self\n"
                + "_endmethod\n";
        Set<Integer> expected = Set.of(5, 6);
        assertThat(this.metrics(code).executableLines()).isEqualTo(expected);
    }

    @Test
    void testExecutableLines3() {
        String code = ""
                + "_pragma(classify_level=basic, topic={test})\n"
                + "_method a.b(_optional c)\n"
                + "    # comment\n"
                + "    ## method header\n"
                + "    _return _self.call(:symbol, c)\n"
                + "_endmethod\n";
        Set<Integer> expected = Set.of(5);
        assertThat(this.metrics(code).executableLines()).isEqualTo(expected);
    }

    @Test
    void testExecutableLines4() {
        String code = ""
                + "_if a\n"
                + "_then\n"
                + "    print(1)\n"
                + "_elif b\n"
                + "_then\n"
                + "    print(2)\n"
                + "_else\n"
                + "    print(3)\n"
                + "_endif\n";
        Set<Integer> expected = Set.of(1, 3, 4, 6, 8);
        assertThat(this.metrics(code).executableLines()).isEqualTo(expected);
    }

    @Test
    void testExecutableLines5() {
        String code = ""
                + "_method a.b\n"
                + "    _local a << _proc@test_proc()\n"
                + "        print(1)\n"
                + "    _endproc\n"
                + "_endmethod\n";
        Set<Integer> expected = Set.of(2, 3);
        assertThat(this.metrics(code).executableLines()).isEqualTo(expected);
    }

    @Test
    void testExecutableLines6() {
        String code = ""
                + "_pragma(classify_level=restricted, topic={dummy})\n"
                + "_method dummy_engine.start(props) \n"
                + "    .slot_1 << rope.new()\n"
                + "    .slot_2 << equality_property_list.new()\n"
                + "    (var1, l_code) << _self.call_me(props[:key_1])\n"
                + "    .slot_3        << var1\n"
                + "    .slot_4        << p_props[:key_2]\n"
                + "    .slot_5        << write_string(p_props[:key_3])\n"
                + "    _self.method_1()\n"
                + "    _self.method_2()\n"
                + "    _self.method_3()\n"
                + "    _self.method_4(p_props[:key_4])\n"
                + "_endmethod\n"
                + "$";
        Set<Integer> expected = Set.of(3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
        assertThat(this.metrics(code).executableLines()).isEqualTo(expected);
    }

    @Test
    void testExecutableLines7() {
        String code = ""
                + "_pragma(classify_level=restricted)\n"
                + "_iter _method a.b()\n"
                + "    # comment\n"
                + "    _for i _over v.fast_elements()\n"
                + "    _loop\n"
                + "        _loopbody(f(i))\n"
                + "    _endloop\n"
                + "_endmethod\n"
                + "$";
        Set<Integer> expected = Set.of(4, 6);
        assertThat(this.metrics(code).executableLines()).isEqualTo(expected);
    }

    private FileMetrics metrics(String code) {
        URI uri = URI.create("tests://unittest");
        MagikFile magikFile = new MagikFile(uri, code);
        return new FileMetrics(magikFile, true);
    }

}
