package nl.ramsolutions.sw.magik.analysis.helpers;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonar.sslr.api.AstNode;
import java.util.Map;
import java.util.Set;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.parser.MagikParser;
import org.junit.jupiter.api.Test;

/** Tests for {@link MethodDefinitionNodeHelper}. */
class MethodDefinitionNodeHelperTest {

  private AstNode parseMagik(final String code) {
    final MagikParser parser = new MagikParser();
    return parser.parseSafe(code);
  }

  @Test
  void testGetParameterNodes() {
    final String code =
        """
        _method a.m(p1, p2)
        _endmethod
        """;
    final AstNode topNode = this.parseMagik(code);
    final AstNode methodNode = topNode.getFirstDescendant(MagikGrammar.METHOD_DEFINITION);
    final MethodDefinitionNodeHelper helper = new MethodDefinitionNodeHelper(methodNode);
    final Map<String, AstNode> parameterNodes = helper.getParameterNodes();
    assertThat(parameterNodes).containsOnlyKeys("p1", "p2");
  }

  @Test
  void testGetParameterNodesDuplicateNames() {
    final String code =
        """
        _method a.m(p1, p1)
        _endmethod
        """;
    final AstNode topNode = this.parseMagik(code);
    final AstNode methodNode = topNode.getFirstDescendant(MagikGrammar.METHOD_DEFINITION);
    final MethodDefinitionNodeHelper helper = new MethodDefinitionNodeHelper(methodNode);
    final Map<String, AstNode> parameterNodes = helper.getParameterNodes();
    assertThat(parameterNodes).containsOnlyKeys("p1");
  }

  private MethodDefinitionNodeHelper createHelper(final String code) {
    final AstNode topNode = this.parseMagik(code);
    final AstNode methodNode = topNode.getFirstDescendant(MagikGrammar.METHOD_DEFINITION);
    return new MethodDefinitionNodeHelper(methodNode);
  }

  @Test
  void testAbstractByConventionRaiseOnly() {
    final String code =
        """
        _method a.m(p1)
          condition.raise(:subclass_should_implement)
        _endmethod
        """;
    final MethodDefinitionNodeHelper helper = this.createHelper(code);
    final boolean abstractByConvention = helper.isAbstractByConvention();
    assertThat(abstractByConvention).isTrue();
  }

  @Test
  void testAbstractByConventionRaiseAndReturnUnset() {
    final String code =
        """
        _method a.m(p1)
          ## Subclasses implement this.
          sw:condition.raise(:subclass_should_implement)
          _return _unset
        _endmethod
        """;
    final MethodDefinitionNodeHelper helper = this.createHelper(code);
    final boolean abstractByConvention = helper.isAbstractByConvention();
    assertThat(abstractByConvention).isTrue();
  }

  @Test
  void testAbstractByConventionRaiseAndBareReturn() {
    final String code =
        """
        _method a.m(p1)
          condition.raise(:subclass_should_implement)
          _return
        _endmethod
        """;
    final MethodDefinitionNodeHelper helper = this.createHelper(code);
    final boolean abstractByConvention = helper.isAbstractByConvention();
    assertThat(abstractByConvention).isTrue();
  }

  @Test
  void testAbstractByConventionCrlf() {
    final String code =
        "_method a.m(p1)\r\n"
            + "\tsw:condition.raise(:subclass_should_implement)\r\n"
            + "\t_return _unset\r\n"
            + "_endmethod\r\n";
    final MethodDefinitionNodeHelper helper = this.createHelper(code);
    final boolean abstractByConvention = helper.isAbstractByConvention();
    assertThat(abstractByConvention).isTrue();
  }

  @Test
  void testAbstractByConventionRaiseAmongLogic() {
    final String code =
        """
        _method a.m(p1)
          _if p1 _is _unset
          _then
            condition.raise(:subclass_should_implement)
          _endif
          _return p1
        _endmethod
        """;
    final MethodDefinitionNodeHelper helper = this.createHelper(code);
    final boolean abstractByConvention = helper.isAbstractByConvention();
    assertThat(abstractByConvention).isFalse();
  }

  @Test
  void testAbstractByConventionRaiseThenOtherStatement() {
    final String code =
        """
        _method a.m(p1)
          condition.raise(:subclass_should_implement)
          write(p1)
        _endmethod
        """;
    final MethodDefinitionNodeHelper helper = this.createHelper(code);
    final boolean abstractByConvention = helper.isAbstractByConvention();
    assertThat(abstractByConvention).isFalse();
  }

  @Test
  void testAbstractByConventionReturnsValue() {
    final String code =
        """
        _method a.m(p1)
          condition.raise(:subclass_should_implement)
          _return p1
        _endmethod
        """;
    final MethodDefinitionNodeHelper helper = this.createHelper(code);
    final boolean abstractByConvention = helper.isAbstractByConvention();
    assertThat(abstractByConvention).isFalse();
  }

  @Test
  void testAbstractByConventionOtherCondition() {
    final String code =
        """
        _method a.m(p1)
          condition.raise(:not_implemented)
        _endmethod
        """;
    final MethodDefinitionNodeHelper helper = this.createHelper(code);
    final boolean abstractByConvention = helper.isAbstractByConvention();
    assertThat(abstractByConvention).isFalse();
  }

  @Test
  void testAbstractByConventionEmptyBody() {
    final String code =
        """
        _method a.m(p1)
          _return _unset
        _endmethod
        """;
    final MethodDefinitionNodeHelper helper = this.createHelper(code);
    final boolean abstractByConvention = helper.isAbstractByConvention();
    assertThat(abstractByConvention).isFalse();
  }

  @Test
  void testRaisesOnlyConfiguredCondition() {
    final String code =
        """
        _method a.m(p1)
          condition.raise(:not_implemented)
        _endmethod
        """;
    final MethodDefinitionNodeHelper helper = this.createHelper(code);
    final Set<String> conditionNames = Set.of("not_implemented");
    final boolean raisesOnly = helper.raisesOnly(conditionNames);
    assertThat(raisesOnly).isTrue();
  }

  @Test
  void testAbstractByConventionReturnsRaise() {
    final String code =
        """
        _method a.m(p1)
          _return sw:condition.raise(:subclass_should_implement, :name, "m()", :class, _self)
        _endmethod
        """;
    final MethodDefinitionNodeHelper helper = this.createHelper(code);
    final boolean abstractByConvention = helper.isAbstractByConvention();
    assertThat(abstractByConvention).isTrue();
  }

  @Test
  void testAbstractByConventionReturnsRaiseThenMore() {
    final String code =
        """
        _method a.m(p1)
          _return sw:condition.raise(:subclass_should_implement), p1
        _endmethod
        """;
    final MethodDefinitionNodeHelper helper = this.createHelper(code);
    final boolean abstractByConvention = helper.isAbstractByConvention();
    assertThat(abstractByConvention).isFalse();
  }

  @Test
  void testAbstractByConventionRaiseWithArguments() {
    final String code =
        """
        _method a.m(p1)
          sw:condition.raise(:subclass_should_implement, :name, :m, :class, _self.class_name)
          _return _unset
        _endmethod
        """;
    final MethodDefinitionNodeHelper helper = this.createHelper(code);
    final boolean abstractByConvention = helper.isAbstractByConvention();
    assertThat(abstractByConvention).isTrue();
  }

  @Test
  void testAbstractByConventionNotARaiseOnBehalfOfCaller() {
    final String code =
        """
        _private _method a.require_override(a_name)
          sw:condition.raise(:subclass_should_implement, :name, a_name)
          _return _unset
        _endmethod
        """;
    final MethodDefinitionNodeHelper helper = this.createHelper(code);
    final boolean abstractByConvention = helper.isAbstractByConvention();
    assertThat(abstractByConvention).isFalse();
  }

  @Test
  void testAbstractByConventionNotAReturnedRaiseOnBehalfOfCaller() {
    final String code =
        """
        _private _method a.require_override(a_name, _optional a_reason)
          _return sw:condition.raise(:subclass_should_implement, :name, a_name, :reason, a_reason)
        _endmethod
        """;
    final MethodDefinitionNodeHelper helper = this.createHelper(code);
    final boolean abstractByConvention = helper.isAbstractByConvention();
    assertThat(abstractByConvention).isFalse();
  }

  @Test
  void testIsAbstractMethodKeyword() {
    final String code =
        """
        _abstract _method a.m(p1)
        _endmethod
        """;
    final MethodDefinitionNodeHelper helper = this.createHelper(code);
    final boolean abstractMethod = helper.isAbstractMethod();
    assertThat(abstractMethod).isTrue();
  }

  @Test
  void testIsAbstractMethodByConvention() {
    final String code =
        """
        _method a.m(p1)
          condition.raise(:subclass_should_implement)
        _endmethod
        """;
    final MethodDefinitionNodeHelper helper = this.createHelper(code);
    final boolean abstractMethod = helper.isAbstractMethod();
    assertThat(abstractMethod).isTrue();
  }

  @Test
  void testIsAbstractMethodConcrete() {
    final String code =
        """
        _method a.m(p1)
          _return p1
        _endmethod
        """;
    final MethodDefinitionNodeHelper helper = this.createHelper(code);
    final boolean abstractMethod = helper.isAbstractMethod();
    assertThat(abstractMethod).isFalse();
  }
}
