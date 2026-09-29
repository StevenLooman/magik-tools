package nl.ramsolutions.sw.magik.analysis.definitions.parsers;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonar.sslr.api.AstNode;
import java.net.URI;
import java.util.List;
import java.util.Set;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.analysis.definitions.MagikDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import org.junit.jupiter.api.Test;

/** Test MethodDefinitionParser. */
class MethodDefinitionParserTest {

  private MethodDefinition parseMethodDefinition(final String code) {
    final URI uri = URI.create("tests://unittest");
    final MagikFile magikFile = new MagikFile(uri, code);
    final AstNode topNode = magikFile.getTopNode();
    final AstNode methodNode = topNode.getFirstDescendant(MagikGrammar.METHOD_DEFINITION);
    final MethodDefinitionParser parser = new MethodDefinitionParser(magikFile, methodNode);
    final List<MagikDefinition> definitions = parser.parseDefinitions();
    return (MethodDefinition)
        definitions.stream().filter(MethodDefinition.class::isInstance).findFirst().orElseThrow();
  }

  @Test
  void testAbstractByConventionIsAbstract() {
    final String code =
        """
        _method shape.area(units)
          sw:condition.raise(:subclass_should_implement)
          _return _unset
        _endmethod
        """;
    final MethodDefinition methodDefinition = this.parseMethodDefinition(code);

    final Set<MethodDefinition.Modifier> modifiers = methodDefinition.getModifiers();
    assertThat(modifiers).containsExactly(MethodDefinition.Modifier.ABSTRACT);
  }

  @Test
  void testAbstractByConventionRaiseOnlyResultIsUnknown() {
    final String code =
        """
        _method shape.area(units)
          condition.raise(:subclass_should_implement)
        _endmethod
        """;
    final MethodDefinition methodDefinition = this.parseMethodDefinition(code);

    final ExpressionResultString returnTypes = methodDefinition.getReturnTypes();
    assertThat(returnTypes).isEqualTo(ExpressionResultString.UNDEFINED);
  }

  @Test
  void testRaiseAmongLogicIsConcrete() {
    final String code =
        """
        _method shape.area(units)
          _if units _is _unset
          _then
            condition.raise(:subclass_should_implement)
          _endif
        _endmethod
        """;
    final MethodDefinition methodDefinition = this.parseMethodDefinition(code);

    final Set<MethodDefinition.Modifier> modifiers = methodDefinition.getModifiers();
    assertThat(modifiers).isEmpty();
  }

  @Test
  void testAbstractByConventionCrlfParsesAsLf() {
    final String lfCode =
        """
        _method shape.area(units)
        \tsw:condition.raise(:subclass_should_implement)
        \t_return _unset
        _endmethod
        """;
    final String crlfCode = lfCode.replace("\n", "\r\n");
    final MethodDefinition lfDefinition = this.parseMethodDefinition(lfCode);
    final MethodDefinition crlfDefinition = this.parseMethodDefinition(crlfCode);

    final Set<MethodDefinition.Modifier> lfModifiers = lfDefinition.getModifiers();
    final Set<MethodDefinition.Modifier> crlfModifiers = crlfDefinition.getModifiers();
    assertThat(crlfModifiers).containsExactly(MethodDefinition.Modifier.ABSTRACT);
    assertThat(crlfModifiers).isEqualTo(lfModifiers);
    final ExpressionResultString lfReturnTypes = lfDefinition.getReturnTypes();
    final ExpressionResultString crlfReturnTypes = crlfDefinition.getReturnTypes();
    assertThat(crlfReturnTypes).isEqualTo(lfReturnTypes);
  }
}
