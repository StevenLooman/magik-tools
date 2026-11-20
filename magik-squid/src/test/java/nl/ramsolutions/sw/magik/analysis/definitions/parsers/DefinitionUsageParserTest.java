package nl.ramsolutions.sw.magik.analysis.definitions.parsers;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import java.util.List;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.analysis.definitions.GlobalUsage;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodUsage;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
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

  private List<GlobalUsage> parseUsedGlobals(final String code, final AstNodeType definitionType) {
    final MagikFile magikFile = new MagikFile(MagikFile.DEFAULT_URI, code);
    final AstNode topNode = magikFile.getTopNode();
    final List<AstNode> definitionNodes = topNode.getDescendants(definitionType);
    final AstNode definitionNode = definitionNodes.get(definitionNodes.size() - 1);
    final DefinitionUsageParser parser = new DefinitionUsageParser(magikFile, definitionNode);
    return parser.getUsedGlobals();
  }

  private static List<String> formatNamesAndLines(final List<GlobalUsage> usages) {
    return usages.stream()
        .map(
            usage -> {
              final TypeString typeName = usage.getTypeName();
              final String name = typeName.getFullString();
              final AstNode node = usage.getNode();
              final int line = node.getTokenLine();
              return name + "@" + line;
            })
        .toList();
  }

  @Test
  void methodSeesGlobalFirstUsedAtTopLevel() {
    final List<GlobalUsage> usages =
        this.parseUsedGlobals(
            """
            _package sw
            _global g1 << a.x
            $
            _method b.m()
              _return a.z
            _endmethod
            $
            """,
            MagikGrammar.METHOD_DEFINITION);

    final List<String> namesAndLines = DefinitionUsageParserTest.formatNamesAndLines(usages);
    assertThat(namesAndLines).containsExactly("sw:a@5");
  }

  @Test
  void methodSeesGlobalDeclaredAtTopLevel() {
    final List<GlobalUsage> usages =
        this.parseUsedGlobals(
            """
            _package sw
            _global g << 1
            _dynamic !d! << 2
            $
            _method b.m()
              _return g + !d!
            _endmethod
            $
            """,
            MagikGrammar.METHOD_DEFINITION);

    final List<String> namesAndLines = DefinitionUsageParserTest.formatNamesAndLines(usages);
    assertThat(namesAndLines).containsExactlyInAnyOrder("sw:g@6", "sw:!d!@6");
  }

  @Test
  void procedureSeesGlobalFirstUsedAtTopLevel() {
    final List<GlobalUsage> usages =
        this.parseUsedGlobals(
            """
            _package sw
            a.x
            $
            _global p << _proc @p()
              _return a.z
            _endproc
            $
            """,
            MagikGrammar.PROCEDURE_DEFINITION);

    final List<String> namesAndLines = DefinitionUsageParserTest.formatNamesAndLines(usages);
    assertThat(namesAndLines).containsExactly("sw:a@5");
  }

  @Test
  void nestedProcedureSeesGlobalFirstUsedByItsMethod() {
    final List<GlobalUsage> usages =
        this.parseUsedGlobals(
            """
            _package sw
            _method b.m()
              a.x
              _return _proc @inner()
                _return a.z
              _endproc
            _endmethod
            $
            """,
            MagikGrammar.PROCEDURE_DEFINITION);

    final List<String> namesAndLines = DefinitionUsageParserTest.formatNamesAndLines(usages);
    assertThat(namesAndLines).containsExactly("sw:a@5");
  }

  @Test
  void topLevelGlobalYieldsAUsagePerOccurrence() {
    final List<GlobalUsage> usages =
        this.parseUsedGlobals(
            """
            _package sw
            a.x
            $
            _method b.m()
              a.y
              _return a.z
            _endmethod
            $
            """,
            MagikGrammar.METHOD_DEFINITION);

    final List<String> namesAndLines = DefinitionUsageParserTest.formatNamesAndLines(usages);
    assertThat(namesAndLines).containsExactly("sw:a@5", "sw:a@6");
  }

  @Test
  void methodNotUsingTopLevelGlobalDoesNotSeeIt() {
    final List<GlobalUsage> usages =
        this.parseUsedGlobals(
            """
            _package sw
            a.x
            $
            _method b.m()
              _return 1
            _endmethod
            $
            """,
            MagikGrammar.METHOD_DEFINITION);

    assertThat(usages).isEmpty();
  }

  @Test
  void shadowingVariableHidesTopLevelGlobal() {
    final List<GlobalUsage> usages =
        this.parseUsedGlobals(
            """
            _package sw
            a.x
            l.x
            $
            _method b.m(a)
              _local l << 1
              _return a.z + l
            _endmethod
            $
            """,
            MagikGrammar.METHOD_DEFINITION);

    assertThat(usages).isEmpty();
  }

  @Test
  void methodsOwnGlobalYieldsAUsagePerOccurrence() {
    final List<GlobalUsage> usages =
        this.parseUsedGlobals(
            """
            _package sw
            _method b.m()
              a.y
              _return a.z
            _endmethod
            $
            """,
            MagikGrammar.METHOD_DEFINITION);

    final List<String> namesAndLines = DefinitionUsageParserTest.formatNamesAndLines(usages);
    assertThat(namesAndLines).containsExactly("sw:a@3", "sw:a@4");
  }

  @Test
  void occurrenceInNestedBlockIsAUsage() {
    final List<GlobalUsage> usages =
        this.parseUsedGlobals(
            """
            _package sw
            _method b.m()
              a.x
              _if _true
              _then
                a.y
              _endif
              _return a.z
            _endmethod
            $
            """,
            MagikGrammar.METHOD_DEFINITION);

    final List<String> namesAndLines = DefinitionUsageParserTest.formatNamesAndLines(usages);
    assertThat(namesAndLines).containsExactly("sw:a@3", "sw:a@6", "sw:a@8");
  }

  @Test
  void declaredDynamicYieldsItsDeclarationAndEachRead() {
    final List<GlobalUsage> usages =
        this.parseUsedGlobals(
            """
            _package sw
            _method b.m()
              _dynamic !d! << 1
              !d!.x
              _return !d!.y
            _endmethod
            $
            """,
            MagikGrammar.METHOD_DEFINITION);

    final List<String> namesAndLines = DefinitionUsageParserTest.formatNamesAndLines(usages);
    assertThat(namesAndLines).containsExactly("sw:!d!@3", "sw:!d!@4", "sw:!d!@5");
  }

  @Test
  void bodyDeclaringATopLevelGlobalKeepsItsEarlierOccurrence() {
    final List<GlobalUsage> usages =
        this.parseUsedGlobals(
            """
            _package sw
            a.x
            $
            _method b.m()
              a.y
              _block
                _global a
                _return a.z
              _endblock
            _endmethod
            $
            """,
            MagikGrammar.METHOD_DEFINITION);

    final List<String> namesAndLines = DefinitionUsageParserTest.formatNamesAndLines(usages);
    assertThat(namesAndLines).containsExactlyInAnyOrder("sw:a@5", "sw:a@7", "sw:a@8");
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
