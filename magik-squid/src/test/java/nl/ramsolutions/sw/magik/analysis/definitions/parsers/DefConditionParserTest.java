package nl.ramsolutions.sw.magik.analysis.definitions.parsers;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonar.sslr.api.AstNode;
import java.util.List;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.analysis.definitions.ConditionDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MagikDefinition;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import org.junit.jupiter.api.Test;

class DefConditionParserTest {

  private AstNode createInvocationNode(final MagikFile magikFile) {
    final AstNode topNode = magikFile.getTopNode();
    return topNode.getFirstDescendant(MagikGrammar.METHOD_INVOCATION);
  }

  private ConditionDefinition parseCondition(final String call) {
    final String code = "_package sw\n" + call + "\n";
    final MagikFile magikFile = new MagikFile(MagikFile.DEFAULT_URI, code);
    final AstNode invocationNode = this.createInvocationNode(magikFile);
    final boolean isCondition = DefConditionParser.isDefineCondition(invocationNode);
    assertThat(isCondition).isTrue();

    final DefConditionParser parser = new DefConditionParser(magikFile, invocationNode);
    final List<MagikDefinition> definitions = parser.parseDefinitions();
    assertThat(definitions).hasSize(1);
    return (ConditionDefinition) definitions.get(0);
  }

  @Test
  void testDefineConditionHasParent() {
    final ConditionDefinition definition =
        this.parseCondition("condition.define_condition(:cond1, :error, {:data1, :data2})");

    final String parent = definition.getParent();
    assertThat(parent).isEqualTo("error");
    final List<String> dataNames = definition.getDataNames();
    assertThat(dataNames).containsExactly("data1", "data2");
  }

  @Test
  void testDefineTopConditionWithSymbolSecondArgumentHasNoParent() {
    final ConditionDefinition definition =
        this.parseCondition(
            "condition.define_top_condition(:cond1, :not_a_parent, {:data1}, _unset)");

    final String name = definition.getName();
    assertThat(name).isEqualTo("cond1");
    final String parent = definition.getParent();
    assertThat(parent).isNull();
    final List<String> dataNames = definition.getDataNames();
    assertThat(dataNames).containsExactly("data1");
  }

  @Test
  void testDefineTopConditionWithTrueSecondArgumentIsParentlessCondition() {
    final ConditionDefinition definition =
        this.parseCondition(
            "condition.define_top_condition(:cond1, _true, {:data1, :data2}, _unset)");

    final String parent = definition.getParent();
    assertThat(parent).isNull();
    final List<String> dataNames = definition.getDataNames();
    assertThat(dataNames).containsExactly("data1", "data2");
  }

  @Test
  void testDefineTopConditionWithFalseSecondArgumentIsParentlessCondition() {
    final ConditionDefinition definition =
        this.parseCondition("sw:condition.define_top_condition(:cond1, _false, {}, _unset)");

    final String parent = definition.getParent();
    assertThat(parent).isNull();
    final List<String> dataNames = definition.getDataNames();
    assertThat(dataNames).isEmpty();
  }

  @Test
  void testDefineConditionWithNonSymbolParentIsNotACondition() {
    final String code = "_package sw\ncondition.define_condition(:cond1, _true, {})\n";
    final MagikFile magikFile = new MagikFile(MagikFile.DEFAULT_URI, code);
    final AstNode invocationNode = this.createInvocationNode(magikFile);

    final boolean isCondition = DefConditionParser.isDefineCondition(invocationNode);

    assertThat(isCondition).isFalse();
  }
}
