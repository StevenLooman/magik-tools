package nl.ramsolutions.sw.magik.parser;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonar.sslr.api.AstNode;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import org.junit.jupiter.api.Test;

/** Test TypeDocParser. */
class TypeDocParserTest {

  private AstNode parseMagik(final String code) {
    final MagikParser parser = new MagikParser();
    return parser.parseSafe(code);
  }

  @Test
  void testParameter() throws IOException {
    final String code =
        """
        _method a.b(param1, param2) << param3
            ## @param {sw:symbol} param1 Test parameter 1.
            ## @param    {integer} param2 Test parameter 2.
            ## @param {integer} param3    Test parameter 3.
            ## @param {integer|float} param4 Test parameter 4.
        _endmethod""";
    final AstNode topNode = this.parseMagik(code);
    final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
    final TypeDocParser docParser = new TypeDocParser(methodNode);
    final Map<String, TypeString> parameterTypes = docParser.getParameterTypes();
    assertThat(parameterTypes)
        .containsOnly(
            Map.entry("param1", TypeString.ofIdentifier("sw:symbol", TypeString.DEFAULT_PACKAGE)),
            Map.entry("param2", TypeString.ofIdentifier("integer", TypeString.DEFAULT_PACKAGE)),
            Map.entry("param3", TypeString.ofIdentifier("integer", TypeString.DEFAULT_PACKAGE)),
            Map.entry(
                "param4",
                TypeString.ofCombination(
                    TypeString.ofIdentifier("integer", TypeString.DEFAULT_PACKAGE),
                    TypeString.ofIdentifier("float", TypeString.DEFAULT_PACKAGE))));
  }

  @Test
  void testParameterEmpty() throws IOException {
    final String code =
        """
        _method a.b(param1, param2) << param3
            ## @param\s
            ## @param param1
            ## @param {integer} param2
            ## @param {integer} param3\s
            ## @param {integer|float} param4
        _endmethod""";
    final AstNode topNode = this.parseMagik(code);
    final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
    final TypeDocParser docParser = new TypeDocParser(methodNode);
    final Map<String, TypeString> parameterTypes = docParser.getParameterTypes();
    assertThat(parameterTypes)
        .containsOnly(
            Map.entry("param1", TypeString.UNDEFINED),
            Map.entry("param2", TypeString.ofIdentifier("integer", TypeString.DEFAULT_PACKAGE)),
            Map.entry("param3", TypeString.ofIdentifier("integer", TypeString.DEFAULT_PACKAGE)),
            Map.entry(
                "param4",
                TypeString.ofCombination(
                    TypeString.ofIdentifier("integer", TypeString.DEFAULT_PACKAGE),
                    TypeString.ofIdentifier("float", TypeString.DEFAULT_PACKAGE))));
  }

  @Test
  void testReturn() throws IOException {
    final String code =
        """
        _method a.b
            ## @return {sw:integer} An Integer.
        _endmethod""";
    final AstNode topNode = this.parseMagik(code);
    final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
    final TypeDocParser docParser = new TypeDocParser(methodNode);
    final List<TypeString> returnTypes = docParser.getReturnTypes();
    assertThat(returnTypes)
        .containsExactly(TypeString.ofIdentifier("sw:integer", TypeString.DEFAULT_PACKAGE));
  }

  @Test
  void testReturnEmpty() throws IOException {
    final String code =
        """
        _method a.b
            ## @return\s
            ## @return {sw:integer}
            ## @return {sw:integer}\s
            ## @return {sw:integer|sw:float}
        _endmethod""";
    final AstNode topNode = this.parseMagik(code);
    final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
    final TypeDocParser docParser = new TypeDocParser(methodNode);
    final List<TypeString> returnTypes = docParser.getReturnTypes();
    assertThat(returnTypes)
        .containsExactly(
            TypeString.UNDEFINED,
            TypeString.ofIdentifier("sw:integer", TypeString.DEFAULT_PACKAGE),
            TypeString.ofIdentifier("sw:integer", TypeString.DEFAULT_PACKAGE),
            TypeString.ofCombination(
                TypeString.ofIdentifier("sw:integer", TypeString.DEFAULT_PACKAGE),
                TypeString.ofIdentifier("sw:float", TypeString.DEFAULT_PACKAGE)));
  }

  @Test
  void testReturnSelf() throws IOException {
    final String code =
        """
        _method a.b
            ## @return {_self}
        _endmethod""";
    final AstNode topNode = this.parseMagik(code);
    final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
    final TypeDocParser docParser = new TypeDocParser(methodNode);
    final List<TypeString> returnTypes = docParser.getReturnTypes();
    assertThat(returnTypes).containsExactly(TypeString.SELF);
  }

  @Test
  void testNestedProcedure() throws IOException {
    String code =
        """
        _method a.b
            ## @return {sw:integer} Top
            rope.map(\
                _proc(item)
                    ## @return {sw:float} Nested
                _endproc())
        _endmethod""";
    final AstNode topNode = this.parseMagik(code);
    final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
    final TypeDocParser methodDocParser = new TypeDocParser(methodNode);
    final List<TypeString> methodReturnTypes = methodDocParser.getReturnTypes();
    assertThat(methodReturnTypes)
        .containsExactly(TypeString.ofIdentifier("sw:integer", TypeString.DEFAULT_PACKAGE));

    final AstNode procNode = topNode.getFirstDescendant(MagikGrammar.PROCEDURE_DEFINITION);
    final TypeDocParser procDocParser = new TypeDocParser(procNode);
    final List<TypeString> procReturnTypes = procDocParser.getReturnTypes();
    assertThat(procReturnTypes)
        .containsExactly(TypeString.ofIdentifier("sw:float", TypeString.DEFAULT_PACKAGE));
  }

  @Test
  void testSlotTypes() throws IOException {
    final String code =
        """
        ## @slot slot1 Slot 1.
        ## @slot {integer} slot2 Slot 2.
        def_slotted_exemplar(:example,
            {
                {:slot1, _unset}
            })
        """;
    final AstNode topNode = this.parseMagik(code);
    final AstNode definitionNode = topNode.getFirstChild(MagikGrammar.STATEMENT);
    final TypeDocParser docParser = new TypeDocParser(definitionNode);
    final Map<String, TypeString> slotTypes = docParser.getSlotTypes();
    assertThat(slotTypes)
        .containsOnly(
            Map.entry("slot1", TypeString.UNDEFINED),
            Map.entry("slot2", TypeString.ofIdentifier("integer", TypeString.DEFAULT_PACKAGE)));
  }

  @Test
  void testTokenLines() throws IOException {
    final String code =
        """
        _proc@test1(param1)
            ## @param {sw:symbol} param1 Test parameter 1.
        _endproc""";
    final AstNode topNode = this.parseMagik(code);
    final AstNode definitionNode = topNode.getFirstDescendant(MagikGrammar.PROCEDURE_DEFINITION);
    final TypeDocParser docParser = new TypeDocParser(definitionNode);
    final Map<AstNode, String> parameterNameNodes = docParser.getParameterNameNodes();
    assertThat(parameterNameNodes).hasSize(1);

    final List<Object> parameterNodes = List.of(parameterNameNodes.keySet().toArray());
    final AstNode parameterNode = (AstNode) parameterNodes.get(0);
    assertThat(parameterNode.getTokenLine()).isEqualTo(2);
  }

  @Test
  void testReturnParameterReference() throws IOException {
    final String code =
        """
        _method a.b(p1)
            ## @return {_parameter(p1)} First parameter.
        _endmethod""";
    final AstNode topNode = this.parseMagik(code);
    final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
    final TypeDocParser docParser = new TypeDocParser(methodNode);
    final List<TypeString> returnTypes = docParser.getReturnTypes();
    assertThat(returnTypes).containsExactly(TypeString.ofParameterRef("p1"));
  }
}
