package nl.ramsolutions.sw.magik.analysis.definitions;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonar.sslr.api.AstNode;
import java.util.List;
import nl.ramsolutions.sw.magik.analysis.MagikAnalysisConfiguration;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.parser.MagikParser;
import org.junit.jupiter.api.Test;

/** Test DefinitionReader. */
@SuppressWarnings("checkstyle:MagicNumber")
class DefinitionReaderTest {

  private AstNode parseCode(String code) {
    final MagikParser parser = new MagikParser();
    return parser.parseSafe(code);
  }

  @Test
  void testPackageDefintion() {
    final String code = "def_package(:test)";
    final AstNode node = this.parseCode(code);
    final DefinitionReader reader =
        new DefinitionReader(MagikAnalysisConfiguration.DEFAULT_CONFIGURATION);
    reader.walkAst(node);

    final List<Definition> definitions = reader.getDefinitions();
    assertThat(definitions).hasSize(1);

    assertThat(definitions.get(0)).isInstanceOf(PackageDefinition.class);
    final PackageDefinition packageDef = (PackageDefinition) definitions.get(0);
    assertThat(packageDef.getName()).isEqualTo("test");
    assertThat(packageDef.getNode()).isEqualTo(node.getFirstDescendant(MagikGrammar.STATEMENT));
    assertThat(packageDef.getUses()).containsExactly("sw");
  }

  @Test
  void testPackageDefintionUses() {
    final String code = "def_package(:test, :uses, {:p1, :p2})";
    final AstNode node = this.parseCode(code);
    final DefinitionReader reader =
        new DefinitionReader(MagikAnalysisConfiguration.DEFAULT_CONFIGURATION);
    reader.walkAst(node);

    final List<Definition> definitions = reader.getDefinitions();
    assertThat(definitions).hasSize(1);

    assertThat(definitions.get(0)).isInstanceOf(PackageDefinition.class);
    final PackageDefinition packageDef = (PackageDefinition) definitions.get(0);
    assertThat(packageDef.getName()).isEqualTo("test");
    assertThat(packageDef.getNode()).isEqualTo(node.getFirstDescendant(MagikGrammar.STATEMENT));
    assertThat(packageDef.getUses()).containsExactly("p1", "p2");
  }

  @Test
  void testEnumeratorDefintion() {
    final String code = "sw:def_enumeration(:test_enum, _false, :a)";
    final AstNode node = this.parseCode(code);
    final DefinitionReader reader =
        new DefinitionReader(MagikAnalysisConfiguration.DEFAULT_CONFIGURATION);
    reader.walkAst(node);

    final List<Definition> definitions = reader.getDefinitions();
    assertThat(definitions).hasSize(1);

    assertThat(definitions.get(0)).isInstanceOf(ExemplarDefinition.class);
    final ExemplarDefinition definition = (ExemplarDefinition) definitions.get(0);
    assertThat(definition.getName()).isEqualTo("user:test_enum");
    assertThat(definition.getNode()).isEqualTo(node.getFirstDescendant(MagikGrammar.STATEMENT));
  }

  @Test
  void testDefSlottedExemplar() {
    final String code =
        "def_slotted_exemplar(:test_exemplar, {{:slot1, _unset}, {:slot2, _unset}})";
    final AstNode node = this.parseCode(code);
    final DefinitionReader reader =
        new DefinitionReader(MagikAnalysisConfiguration.DEFAULT_CONFIGURATION);
    reader.walkAst(node);

    final List<Definition> definitions = reader.getDefinitions();
    assertThat(definitions).hasSize(1);

    assertThat(definitions.get(0)).isInstanceOf(ExemplarDefinition.class);
    final ExemplarDefinition slottedExemplarDef = (ExemplarDefinition) definitions.get(0);
    assertThat(slottedExemplarDef.getName()).isEqualTo("user:test_exemplar");
    assertThat(slottedExemplarDef.getNode())
        .isEqualTo(node.getFirstDescendant(MagikGrammar.STATEMENT));

    assertThat(slottedExemplarDef.getSlots()).hasSize(2);
    final SlotDefinition slot1 = slottedExemplarDef.getSlots().get(0);
    assertThat(slot1.getName()).isEqualTo("slot1");

    final SlotDefinition slot2 = slottedExemplarDef.getSlots().get(1);
    assertThat(slot2.getName()).isEqualTo("slot2");
  }

  @Test
  void testDefSlottedExemplarSlotAccessMethods() {
    final String code =
        "def_slotted_exemplar(:test_exemplar, {{:slot1, _unset, :writable, :read_only}})";
    final AstNode node = this.parseCode(code);
    final DefinitionReader reader =
        new DefinitionReader(MagikAnalysisConfiguration.DEFAULT_CONFIGURATION);
    reader.walkAst(node);

    final List<Definition> definitions = reader.getDefinitions();
    assertThat(definitions).hasSize(4);

    assertThat(definitions.get(0)).isInstanceOf(ExemplarDefinition.class);
    final ExemplarDefinition slottedExemplarDef = (ExemplarDefinition) definitions.get(0);
    assertThat(slottedExemplarDef.getPackage()).isEqualTo("user");
    assertThat(slottedExemplarDef.getName()).isEqualTo("user:test_exemplar");
    assertThat(slottedExemplarDef.getSlots()).hasSize(1);

    final SlotDefinition slot1 = slottedExemplarDef.getSlots().get(0);
    assertThat(slot1.getName()).isEqualTo("slot1");

    assertThat(definitions.get(1)).isInstanceOf(MethodDefinition.class);
    final MethodDefinition getMethodDefinition = (MethodDefinition) definitions.get(1);
    assertThat(getMethodDefinition.getPackage()).isEqualTo("user");
    assertThat(getMethodDefinition.getModifiers()).isEmpty();
    assertThat(getMethodDefinition.getName()).isEqualTo("user:test_exemplar.slot1");

    assertThat(definitions.get(2)).isInstanceOf(MethodDefinition.class);
    final MethodDefinition setMethodDefinition = (MethodDefinition) definitions.get(2);
    assertThat(setMethodDefinition.getPackage()).isEqualTo("user");
    assertThat(setMethodDefinition.getModifiers())
        .containsExactly(MethodDefinition.Modifier.PRIVATE);
    assertThat(setMethodDefinition.getName()).isEqualTo("user:test_exemplar.slot1<<");

    assertThat(definitions.get(3)).isInstanceOf(MethodDefinition.class);
    final MethodDefinition bootMethodDefinition = (MethodDefinition) definitions.get(3);
    assertThat(bootMethodDefinition.getPackage()).isEqualTo("user");
    assertThat(bootMethodDefinition.getModifiers())
        .containsExactly(MethodDefinition.Modifier.PRIVATE);
    assertThat(bootMethodDefinition.getName()).isEqualTo("user:test_exemplar.slot1^<<");
  }

  @Test
  void testDefSlottedExemplarParentsSingle() {
    final String code = "def_slotted_exemplar(:test_exemplar, {}, @sw:rope)";
    final AstNode node = this.parseCode(code);
    final DefinitionReader reader =
        new DefinitionReader(MagikAnalysisConfiguration.DEFAULT_CONFIGURATION);
    reader.walkAst(node);

    final List<Definition> definitions = reader.getDefinitions();
    assertThat(definitions).hasSize(1);

    assertThat(definitions.get(0)).isInstanceOf(ExemplarDefinition.class);
    final ExemplarDefinition slottedExemplarDef = (ExemplarDefinition) definitions.get(0);
    assertThat(slottedExemplarDef.getName()).isEqualTo("user:test_exemplar");
    assertThat(slottedExemplarDef.getNode())
        .isEqualTo(node.getFirstDescendant(MagikGrammar.STATEMENT));
    final TypeString ropeRef = TypeString.ofIdentifier("rope", "sw");
    assertThat(slottedExemplarDef.getParents()).containsExactly(ropeRef);
  }

  @Test
  void testDefSlottedExemplarParentsMultiple() {
    final String code = "def_slotted_exemplar(:test_exemplar, {}, {:mixin1, :rope})";
    final AstNode node = this.parseCode(code);
    final DefinitionReader reader =
        new DefinitionReader(MagikAnalysisConfiguration.DEFAULT_CONFIGURATION);
    reader.walkAst(node);

    final List<Definition> definitions = reader.getDefinitions();
    assertThat(definitions).hasSize(1);

    assertThat(definitions.get(0)).isInstanceOf(ExemplarDefinition.class);
    final ExemplarDefinition slottedExemplarDef = (ExemplarDefinition) definitions.get(0);
    assertThat(slottedExemplarDef.getName()).isEqualTo("user:test_exemplar");
    assertThat(slottedExemplarDef.getNode())
        .isEqualTo(node.getFirstDescendant(MagikGrammar.STATEMENT));
    assertThat(slottedExemplarDef.getParents())
        .containsExactly(
            TypeString.ofIdentifier("mixin1", "user"), TypeString.ofIdentifier("rope", "user"));
  }

  @Test
  void testDefIndexeExemplar() {
    final String code = "def_indexed_exemplar(:test_exemplar, _unset, {:mixin1, :integer})";
    final AstNode node = this.parseCode(code);
    final DefinitionReader reader =
        new DefinitionReader(MagikAnalysisConfiguration.DEFAULT_CONFIGURATION);
    reader.walkAst(node);

    final List<Definition> definitions = reader.getDefinitions();
    assertThat(definitions).hasSize(1);

    assertThat(definitions.get(0)).isInstanceOf(ExemplarDefinition.class);
    final ExemplarDefinition exemplarDef = (ExemplarDefinition) definitions.get(0);
    assertThat(exemplarDef.getTypeString())
        .isEqualTo(TypeString.ofIdentifier("test_exemplar", "user"));
    assertThat(exemplarDef.getParents())
        .containsExactly(
            TypeString.ofIdentifier("mixin1", "user"),
            TypeString.ofIdentifier("user:integer", "user"));
  }

  @Test
  void testDefMixin() {
    final String code = "def_mixin(:test_mixin, :mixin1)";
    final AstNode node = this.parseCode(code);
    final DefinitionReader reader =
        new DefinitionReader(MagikAnalysisConfiguration.DEFAULT_CONFIGURATION);
    reader.walkAst(node);

    final List<Definition> definitions = reader.getDefinitions();
    assertThat(definitions).hasSize(1);

    assertThat(definitions.get(0)).isInstanceOf(ExemplarDefinition.class);
    final ExemplarDefinition defintion = (ExemplarDefinition) definitions.get(0);
    assertThat(defintion.getTypeString()).isEqualTo(TypeString.ofIdentifier("test_mixin", "user"));
    assertThat(defintion.getParents()).containsExactly(TypeString.ofIdentifier("mixin1", "user"));
  }

  @Test
  void testDefineBinaryOperatorCase() {
    final String code = "define_binary_operator_case(:|>|, integer, float, _proc(a, b) _endproc)";
    final AstNode node = this.parseCode(code);
    final DefinitionReader reader =
        new DefinitionReader(MagikAnalysisConfiguration.DEFAULT_CONFIGURATION);
    reader.walkAst(node);

    final List<Definition> definitions = reader.getDefinitions();
    assertThat(definitions).hasSize(1);

    assertThat(definitions.get(0)).isInstanceOf(BinaryOperatorDefinition.class);
    final BinaryOperatorDefinition operatorDefinition =
        (BinaryOperatorDefinition) definitions.get(0);
    assertThat(operatorDefinition.getName()).isEqualTo("user:integer > user:float");
    assertThat(operatorDefinition.getOperator()).isEqualTo(">");
    assertThat(operatorDefinition.getLhsTypeName())
        .isEqualTo(TypeString.ofIdentifier("integer", "user"));
    assertThat(operatorDefinition.getRhsTypeName())
        .isEqualTo(TypeString.ofIdentifier("float", "user"));
  }

  @Test
  void testMethodDefinition() { // NOSONAR
    final String code = "_method a.b _endmethod";
    final AstNode node = this.parseCode(code);
    final DefinitionReader reader =
        new DefinitionReader(MagikAnalysisConfiguration.DEFAULT_CONFIGURATION);
    reader.walkAst(node);

    final List<Definition> definitions = reader.getDefinitions();
    assertThat(definitions).hasSize(1);

    assertThat(definitions.get(0)).isInstanceOf(MethodDefinition.class);
    final MethodDefinition methodDef = (MethodDefinition) definitions.get(0);
    assertThat(methodDef.getName()).isEqualTo("user:a.b");
    assertThat(methodDef.getModifiers()).isEmpty();
    assertThat(methodDef.getParameters()).isEmpty();
  }

  @Test
  void testMethodDefinitionSyntaxError() {
    final String code = "_method a.b _a _endmethod";
    final AstNode node = this.parseCode(code);
    final DefinitionReader reader =
        new DefinitionReader(MagikAnalysisConfiguration.DEFAULT_CONFIGURATION);
    reader.walkAst(node);

    final List<Definition> definitions = reader.getDefinitions();
    assertThat(definitions).isEmpty();
  }

  @Test
  void testMethodDefinitionPrivateAbstractIter() {
    final String code = "_abstract _private _iter _method a.b() _endmethod";
    final AstNode node = this.parseCode(code);
    final DefinitionReader reader =
        new DefinitionReader(MagikAnalysisConfiguration.DEFAULT_CONFIGURATION);
    reader.walkAst(node);

    final List<Definition> definitions = reader.getDefinitions();
    assertThat(definitions).hasSize(1);

    assertThat(definitions.get(0)).isInstanceOf(MethodDefinition.class);
    final MethodDefinition methodDef = (MethodDefinition) definitions.get(0);
    assertThat(methodDef.getName()).isEqualTo("user:a.b()");
    assertThat(methodDef.getModifiers())
        .containsOnly(
            MethodDefinition.Modifier.PRIVATE,
            MethodDefinition.Modifier.ABSTRACT,
            MethodDefinition.Modifier.ITER);
    assertThat(methodDef.getParameters()).isEmpty();
  }

  @Test
  void testMethodDefinitionParameters() {
    final String code = "_method a.b(a, _optional b, c, _gather d) << z _endmethod";
    final AstNode node = this.parseCode(code);
    final DefinitionReader reader =
        new DefinitionReader(MagikAnalysisConfiguration.DEFAULT_CONFIGURATION);
    reader.walkAst(node);

    final List<Definition> definitions = reader.getDefinitions();
    assertThat(definitions).hasSize(1);

    assertThat(definitions.get(0)).isInstanceOf(MethodDefinition.class);
    final MethodDefinition methodDef = (MethodDefinition) definitions.get(0);
    assertThat(methodDef.getName()).isEqualTo("user:a.b()<<");
    assertThat(methodDef.getModifiers()).isEmpty();

    final List<ParameterDefinition> parameters = methodDef.getParameters();
    assertThat(parameters).hasSize(4);

    final ParameterDefinition parameter0 = parameters.get(0);
    assertThat(parameter0.getName()).isEqualTo("a");
    assertThat(parameter0.getModifier()).isEqualTo(ParameterDefinition.Modifier.NONE);

    final ParameterDefinition parameter1 = parameters.get(1);
    assertThat(parameter1.getName()).isEqualTo("b");
    assertThat(parameter1.getModifier()).isEqualTo(ParameterDefinition.Modifier.OPTIONAL);

    final ParameterDefinition parameter2 = parameters.get(2);
    assertThat(parameter2.getName()).isEqualTo("c");
    assertThat(parameter2.getModifier()).isEqualTo(ParameterDefinition.Modifier.OPTIONAL);

    final ParameterDefinition parameter3 = parameters.get(3);
    assertThat(parameter3.getName()).isEqualTo("d");
    assertThat(parameter3.getModifier()).isEqualTo(ParameterDefinition.Modifier.GATHER);

    final ParameterDefinition assignmentParameter = methodDef.getAssignmentParameter();
    assertThat(assignmentParameter).isNotNull();
    assertThat(assignmentParameter.getName()).isEqualTo("z");
    assertThat(assignmentParameter.getModifier()).isEqualTo(ParameterDefinition.Modifier.NONE);
  }

  @Test
  void testDefineSlotAccess() { // NOSONAR
    final String code = "test_exemplar.define_slot_access(:slot1, :readable, :public)";
    final AstNode node = this.parseCode(code);
    final DefinitionReader reader =
        new DefinitionReader(MagikAnalysisConfiguration.DEFAULT_CONFIGURATION);
    reader.walkAst(node);

    final List<Definition> definitions = reader.getDefinitions();
    assertThat(definitions).hasSize(1);

    assertThat(definitions.get(0)).isInstanceOf(MethodDefinition.class);
    final MethodDefinition methodDef = (MethodDefinition) definitions.get(0);
    assertThat(methodDef.getName()).isEqualTo("user:test_exemplar.slot1");
    assertThat(methodDef.getModifiers()).isEmpty();
    assertThat(methodDef.getParameters()).isEmpty();
  }

  @Test
  void testDefineSlotExternallyReadable() { // NOSONAR
    final String code = "test_exemplar.define_slot_externally_readable(:slot1)";
    final AstNode node = this.parseCode(code);
    final DefinitionReader reader =
        new DefinitionReader(MagikAnalysisConfiguration.DEFAULT_CONFIGURATION);
    reader.walkAst(node);

    final List<Definition> definitions = reader.getDefinitions();
    assertThat(definitions).hasSize(1);

    assertThat(definitions.get(0)).isInstanceOf(MethodDefinition.class);
    final MethodDefinition methodDef = (MethodDefinition) definitions.get(0);
    assertThat(methodDef.getName()).isEqualTo("user:test_exemplar.slot1");
    assertThat(methodDef.getModifiers()).isEmpty();
    assertThat(methodDef.getParameters()).isEmpty();
  }

  @Test
  void testDefineSlotExternallyReadablePublic() { // NOSONAR
    final String code = "test_exemplar.define_slot_externally_readable(:slot1, :public)";
    final AstNode node = this.parseCode(code);
    final DefinitionReader reader =
        new DefinitionReader(MagikAnalysisConfiguration.DEFAULT_CONFIGURATION);
    reader.walkAst(node);

    final List<Definition> definitions = reader.getDefinitions();
    assertThat(definitions).hasSize(1);

    assertThat(definitions.get(0)).isInstanceOf(MethodDefinition.class);
    final MethodDefinition methodDef = (MethodDefinition) definitions.get(0);
    assertThat(methodDef.getName()).isEqualTo("user:test_exemplar.slot1");
    assertThat(methodDef.getModifiers()).isEmpty();
    assertThat(methodDef.getParameters()).isEmpty();
  }

  @Test
  void testDefineSlotExternallyWritable() {
    final String code = "test_exemplar.define_slot_externally_writable(:slot1, :public)";
    final AstNode node = this.parseCode(code);
    final DefinitionReader reader =
        new DefinitionReader(MagikAnalysisConfiguration.DEFAULT_CONFIGURATION);
    reader.walkAst(node);

    final List<Definition> definitions = reader.getDefinitions();
    assertThat(definitions).hasSize(3);

    assertThat(definitions.get(0)).isInstanceOf(MethodDefinition.class);
    final MethodDefinition getMethodDef = (MethodDefinition) definitions.get(0);
    assertThat(getMethodDef.getName()).isEqualTo("user:test_exemplar.slot1");
    assertThat(getMethodDef.getModifiers()).isEmpty();
    assertThat(getMethodDef.getParameters()).isEmpty();

    assertThat(definitions.get(1)).isInstanceOf(MethodDefinition.class);
    final MethodDefinition setMethodDef = (MethodDefinition) definitions.get(1);
    assertThat(setMethodDef.getName()).isEqualTo("user:test_exemplar.slot1<<");
    assertThat(setMethodDef.getModifiers()).isEmpty();
    assertThat(setMethodDef.getParameters()).isEmpty();

    assertThat(definitions.get(0)).isInstanceOf(MethodDefinition.class);
    final MethodDefinition bootMethodDef = (MethodDefinition) definitions.get(2);
    assertThat(bootMethodDef.getName()).isEqualTo("user:test_exemplar.slot1^<<");
    assertThat(bootMethodDef.getModifiers()).isEmpty();
    assertThat(bootMethodDef.getParameters()).isEmpty();
  }

  @Test
  void testDefineSharedVariable() {
    final String code = "test_exemplar.define_shared_variable(:var1, 1, :readonly)";
    final AstNode node = this.parseCode(code);
    final DefinitionReader reader =
        new DefinitionReader(MagikAnalysisConfiguration.DEFAULT_CONFIGURATION);
    reader.walkAst(node);

    final List<Definition> definitions = reader.getDefinitions();
    assertThat(definitions).hasSize(3);

    assertThat(definitions.get(0)).isInstanceOf(MethodDefinition.class);
    final MethodDefinition getMethodDef = (MethodDefinition) definitions.get(0);
    assertThat(getMethodDef.getName()).isEqualTo("user:test_exemplar.var1");
    assertThat(getMethodDef.getModifiers()).isEmpty();
    assertThat(getMethodDef.getParameters()).isEmpty();
    assertThat(getMethodDef.getAssignmentParameter()).isNull();

    assertThat(definitions.get(1)).isInstanceOf(MethodDefinition.class);
    final MethodDefinition setMethodDef = (MethodDefinition) definitions.get(1);
    assertThat(setMethodDef.getName()).isEqualTo("user:test_exemplar.var1<<");
    assertThat(setMethodDef.getModifiers()).containsExactly(MethodDefinition.Modifier.PRIVATE);
    assertThat(setMethodDef.getParameters()).isEmpty();
    final ParameterDefinition setAssignParam = setMethodDef.getAssignmentParameter();
    assertThat(setAssignParam.getName()).isEqualTo("val");
    assertThat(setAssignParam.getModifier()).isEqualTo(ParameterDefinition.Modifier.NONE);

    assertThat(definitions.get(2)).isInstanceOf(MethodDefinition.class);
    final MethodDefinition bootMethodDef = (MethodDefinition) definitions.get(2);
    assertThat(bootMethodDef.getName()).isEqualTo("user:test_exemplar.var1^<<");
    assertThat(bootMethodDef.getModifiers()).containsExactly(MethodDefinition.Modifier.PRIVATE);
    assertThat(bootMethodDef.getParameters()).isEmpty();
    final ParameterDefinition bootAssignParam = bootMethodDef.getAssignmentParameter();
    assertThat(bootAssignParam.getName()).isEqualTo("val");
    assertThat(bootAssignParam.getModifier()).isEqualTo(ParameterDefinition.Modifier.NONE);
  }

  @Test
  void testDefineSharedConstant() {
    final String code = "test_exemplar.define_shared_constant(:const1, 1, :private)";
    final AstNode node = this.parseCode(code);
    final DefinitionReader reader =
        new DefinitionReader(MagikAnalysisConfiguration.DEFAULT_CONFIGURATION);
    reader.walkAst(node);

    final List<Definition> definitions = reader.getDefinitions();
    assertThat(definitions).hasSize(1);

    assertThat(definitions.get(0)).isInstanceOf(MethodDefinition.class);
    final MethodDefinition getMethodDef = (MethodDefinition) definitions.get(0);
    assertThat(getMethodDef.getName()).isEqualTo("user:test_exemplar.const1");
    assertThat(getMethodDef.getModifiers()).containsExactly(MethodDefinition.Modifier.PRIVATE);
    assertThat(getMethodDef.getParameters()).isEmpty();
  }

  @Test
  void testDefineSharedConstant2() {
    final String code = "test_exemplar.define_shared_constant(:const1, 1, _false)";
    final AstNode node = this.parseCode(code);
    final DefinitionReader reader =
        new DefinitionReader(MagikAnalysisConfiguration.DEFAULT_CONFIGURATION);
    reader.walkAst(node);

    final List<Definition> definitions = reader.getDefinitions();
    assertThat(definitions).hasSize(1);

    assertThat(definitions.get(0)).isInstanceOf(MethodDefinition.class);
    final MethodDefinition getMethodDef = (MethodDefinition) definitions.get(0);
    assertThat(getMethodDef.getName()).isEqualTo("user:test_exemplar.const1");
    assertThat(getMethodDef.getModifiers()).containsExactly();
    assertThat(getMethodDef.getParameters()).isEmpty();
  }

  @Test
  void testAssignGlobal() {
    final String code = "_global g << _unset";
    final AstNode node = this.parseCode(code);
    final DefinitionReader reader =
        new DefinitionReader(MagikAnalysisConfiguration.DEFAULT_CONFIGURATION);
    reader.walkAst(node);

    final List<Definition> definitions = reader.getDefinitions();
    assertThat(definitions).hasSize(1);

    assertThat(definitions.get(0)).isInstanceOf(GlobalDefinition.class);
    final GlobalDefinition globalDef = (GlobalDefinition) definitions.get(0);
    assertThat(globalDef.getName()).isEqualTo("user:g");
  }

  @Test
  void testParseCondition() {
    final String code = "condition.define_condition(:cond1, :information, {:data1, :data2})";
    final AstNode node = this.parseCode(code);
    final DefinitionReader reader =
        new DefinitionReader(MagikAnalysisConfiguration.DEFAULT_CONFIGURATION);
    reader.walkAst(node);

    final List<Definition> definitions = reader.getDefinitions();
    assertThat(definitions).hasSize(1);

    assertThat(definitions.get(0)).isInstanceOf(ConditionDefinition.class);
    final ConditionDefinition conditionDef = (ConditionDefinition) definitions.get(0);
    assertThat(conditionDef.getName()).isEqualTo("cond1");
    assertThat(conditionDef.getParent()).isEqualTo("information");
    assertThat(conditionDef.getDataNames()).containsOnly("data1", "data2");
  }

  @Test
  void testParseConditionInBlock() {
    final String code =
        ""
            + "_block\n"
            + "  condition.define_condition(:cond1, :information, {:data1, :data2})\n"
            + "_endblock";
    final AstNode node = this.parseCode(code);
    final DefinitionReader reader =
        new DefinitionReader(MagikAnalysisConfiguration.DEFAULT_CONFIGURATION);
    reader.walkAst(node);

    final List<Definition> definitions = reader.getDefinitions();
    assertThat(definitions).hasSize(1);

    assertThat(definitions.get(0)).isInstanceOf(ConditionDefinition.class);
    final ConditionDefinition conditionDef = (ConditionDefinition) definitions.get(0);
    assertThat(conditionDef.getName()).isEqualTo("cond1");
    assertThat(conditionDef.getParent()).isEqualTo("information");
    assertThat(conditionDef.getDataNames()).containsOnly("data1", "data2");
  }
}
