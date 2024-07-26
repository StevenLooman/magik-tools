package nl.ramsolutions.sw.magik.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.Set;
import nl.ramsolutions.sw.magik.MagikFile;
import org.junit.jupiter.api.Test;

/** Test FileMetrics. */
@SuppressWarnings("checkstyle:MagicNumber")
class FileMetricsTest {

  private static final URI DEFAULT_URI = URI.create("memory://source.magik");

  @Test
  void testStatements1() {
    final String code = "print(1)";
    assertThat(this.metrics(code).numberOfStatements()).isEqualTo(1);
  }

  @Test
  void testMethodDefinition1() {
    final String code = "_method a.b _endmethod";
    assertThat(this.metrics(code).numberOfMethods()).isEqualTo(1);
  }

  @Test
  void testProcedureDefinition1() {
    final String code = "_proc() _endproc";
    assertThat(this.metrics(code).numberOfProcedures()).isEqualTo(1);
  }

  @Test
  void testExemplarDefinition1() {
    final String code = "def_slotted_exemplar(:test, {})";
    assertThat(this.metrics(code).numberOfExemplars()).isEqualTo(1);
  }

  @Test
  void testExemplarDefinition2() {
    final String code = "def_indexed_exemplar(:test, {})";
    assertThat(this.metrics(code).numberOfExemplars()).isEqualTo(1);
  }

  @Test
  void testFileComplexity1() {
    final String code = "_if a = b _then _endif";
    assertThat(this.metrics(code).fileComplexity()).isEqualTo(2);
  }

  @Test
  void testExecutableLines1() {
    final String code = "print(1)";
    final Set<Integer> expected = Set.of(1);
    assertThat(this.metrics(code).executableLines()).isEqualTo(expected);
  }

  @Test
  void testExecutableLines2() {
    final String code =
        """
        _pragma(classify_level=basic, topic={test})
        _method a.b
            ## method header
            # comment
            print(1)
            _return _self
        _endmethod
        """;
    final Set<Integer> expected = Set.of(5, 6);
    assertThat(this.metrics(code).executableLines()).isEqualTo(expected);
  }

  @Test
  void testExecutableLines3() {
    final String code =
        """
        _pragma(classify_level=basic, topic={test})
        _method a.b(_optional c)
            # comment
            ## method header
            _return _self.call(:symbol, c)
        _endmethod
        """;
    final Set<Integer> expected = Set.of(5);
    assertThat(this.metrics(code).executableLines()).isEqualTo(expected);
  }

  @Test
  void testExecutableLines4() {
    final String code =
        """
        _if a
        _then
            print(1)
        _elif b
        _then
            print(2)
        _else
            print(3)
        _endif
        """;
    final Set<Integer> expected = Set.of(1, 3, 4, 6, 8);
    assertThat(this.metrics(code).executableLines()).isEqualTo(expected);
  }

  @Test
  void testExecutableLines5() {
    final String code =
        """
        _method a.b
            _local a << _proc@test_proc()
                print(1)
            _endproc
        _endmethod
        """;
    final Set<Integer> expected = Set.of(2, 3);
    assertThat(this.metrics(code).executableLines()).isEqualTo(expected);
  }

  @Test
  void testExecutableLines6() {
    final String code =
        """
        _pragma(classify_level=restricted, topic={dummy})
        _method dummy_engine.start(props)\s
            .slot_1 << rope.new()
            .slot_2 << equality_property_list.new()
            (var1, l_code) << _self.call_me(props[:key_1])
            .slot_3        << var1
            .slot_4        << p_props[:key_2]
            .slot_5        << write_string(p_props[:key_3])
            _self.method_1()
            _self.method_2()
            _self.method_3()
            _self.method_4(p_props[:key_4])
        _endmethod
        $""";
    final Set<Integer> expected = Set.of(3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
    assertThat(this.metrics(code).executableLines()).isEqualTo(expected);
  }

  @Test
  void testExecutableLines7() {
    final String code =
        """
        _pragma(classify_level=restricted)
        _iter _method a.b()
            # comment
            _for i _over v.fast_elements()
            _loop
                _loopbody(f(i))
            _endloop
        _endmethod
        $""";
    final Set<Integer> expected = Set.of(4, 6);
    assertThat(this.metrics(code).executableLines()).isEqualTo(expected);
  }

  private FileMetrics metrics(String code) {
    final MagikFile magikFile = new MagikFile(DEFAULT_URI, code);
    return new FileMetrics(magikFile, true);
  }
}
