package nl.ramsolutions.sw.magik.analysis.definitions.parsers;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonar.sslr.api.AstNode;
import java.util.List;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodUsage;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import org.junit.jupiter.api.Test;

/** Tests for {@link DefinitionUsageParser}. */
class DefinitionUsageParserTest {

  private List<MethodUsage> parseUsedUnaryOperators(final String code) {
    final MagikFile magikFile = new MagikFile(MagikFile.DEFAULT_URI, code);
    final AstNode topNode = magikFile.getTopNode();
    final AstNode methodNode = topNode.getFirstDescendant(MagikGrammar.METHOD_DEFINITION);
    final DefinitionUsageParser parser = new DefinitionUsageParser(magikFile, methodNode);
    return parser.getUsedUnaryOperators();
  }

  @Test
  void knownUnaryOperatorsMapToTheirMethods() {
    final List<MethodUsage> usages =
        this.parseUsedUnaryOperators(
            """
            _package sw
            _method object.test(a)
              _return ~a, -a, +a, _not a
            _endmethod
            """);

    final List<String> methodNames = usages.stream().map(MethodUsage::getMethodName).toList();
    assertThat(methodNames).containsExactlyInAnyOrder("not", "negated", "unary_plus", "not");
  }

  @Test
  void allResultsProducesNoUsage() {
    final List<MethodUsage> usages =
        this.parseUsedUnaryOperators(
            """
            _package sw
            _method object.test(a)
              _return _allresults a.invoke()
            _endmethod
            """);

    // `_allresults` has no method equivalent, so it must never yield a null-named usage.
    assertThat(usages).isEmpty();
  }

  @Test
  void scatteredAllResultsProducesOnlyTheScatterUsage() {
    final List<MethodUsage> usages =
        this.parseUsedUnaryOperators(
            """
            _package sw
            _method object.test(a)
              _return _scatter _allresults a.invoke()
            _endmethod
            """);

    final List<String> methodNames = usages.stream().map(MethodUsage::getMethodName).toList();
    assertThat(methodNames).containsExactly("for_scatter()");
  }

  @Test
  void upperCaseKeywordOperatorMapsToItsMethod() {
    final List<MethodUsage> usages =
        this.parseUsedUnaryOperators(
            """
            _package sw
            _method object.test(a)
              _return _NOT a
            _endmethod
            """);

    final List<String> methodNames = usages.stream().map(MethodUsage::getMethodName).toList();
    assertThat(methodNames).containsExactly("not");
  }

  @Test
  void noUnaryOperatorProducesNoUsage() {
    final List<MethodUsage> usages =
        this.parseUsedUnaryOperators(
            """
            _package sw
            _method object.test(a)
              _return a.invoke()
            _endmethod
            """);

    assertThat(usages).isEmpty();
  }
}
