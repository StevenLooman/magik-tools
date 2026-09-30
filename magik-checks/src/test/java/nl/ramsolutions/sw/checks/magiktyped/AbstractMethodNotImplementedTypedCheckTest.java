package nl.ramsolutions.sw.checks.magiktyped;

import static nl.ramsolutions.sw.checks.magiktyped.MagikTypedCheckAssert.assertThat;

import java.net.URI;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import nl.ramsolutions.sw.checks.MagikTypedCheck;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.InheritanceDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MagikDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import org.junit.jupiter.api.Test;

/** Tests for {@link AbstractMethodNotImplementedTypedCheck}. */
class AbstractMethodNotImplementedTypedCheckTest {

  private static final TypeString TYPE_PARENT = TypeString.ofIdentifier("parent", "sw");
  private static final TypeString TYPE_CHILD = TypeString.ofIdentifier("child", "sw");

  private void addExemplar(
      final IDefinitionKeeper definitionKeeper,
      final TypeString typeStr,
      final ExemplarDefinition.Sort sort,
      final List<TypeString> parents) {
    definitionKeeper.add(new ExemplarDefinition(null, null, null, null, null, sort, typeStr, null));
    parents.forEach(
        parentTypeStr ->
            definitionKeeper.add(
                new InheritanceDefinition(null, null, null, null, null, typeStr, parentTypeStr)));
  }

  private void addMethod(
      final IDefinitionKeeper definitionKeeper,
      final TypeString typeStr,
      final String methodName,
      final Set<MethodDefinition.Modifier> modifiers) {
    definitionKeeper.add(
        new MethodDefinition(
            null,
            null,
            null,
            null,
            null,
            typeStr,
            methodName,
            modifiers,
            Collections.emptyList(),
            null,
            null,
            ExpressionResultString.EMPTY,
            ExpressionResultString.EMPTY));
  }

  @Test
  void testAbstractMethodImplemented() {
    final String code = "def_slotted_exemplar(:child, {}, :parent)\n";
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    this.addExemplar(
        definitionKeeper, TYPE_PARENT, ExemplarDefinition.Sort.SLOTTED, Collections.emptyList());
    this.addMethod(
        definitionKeeper,
        TYPE_PARENT,
        "do_something()",
        EnumSet.of(MethodDefinition.Modifier.ABSTRACT));
    this.addExemplar(
        definitionKeeper, TYPE_CHILD, ExemplarDefinition.Sort.SLOTTED, List.of(TYPE_PARENT));
    this.addMethod(
        definitionKeeper,
        TYPE_CHILD,
        "do_something()",
        EnumSet.noneOf(MethodDefinition.Modifier.class));
    final MagikTypedCheck check = new AbstractMethodNotImplementedTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testAbstractMethodNotImplemented() {
    final String code = "def_slotted_exemplar(:child, {}, :parent)\n";
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    this.addExemplar(
        definitionKeeper, TYPE_PARENT, ExemplarDefinition.Sort.SLOTTED, Collections.emptyList());
    this.addMethod(
        definitionKeeper,
        TYPE_PARENT,
        "do_something()",
        EnumSet.of(MethodDefinition.Modifier.ABSTRACT));
    this.addExemplar(
        definitionKeeper, TYPE_CHILD, ExemplarDefinition.Sort.SLOTTED, List.of(TYPE_PARENT));
    final MagikTypedCheck check = new AbstractMethodNotImplementedTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @Test
  void testMixinAbstractMethodNotImplemented() {
    final String code = "def_mixin(:child, :parent)\n";
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    this.addExemplar(
        definitionKeeper, TYPE_PARENT, ExemplarDefinition.Sort.SLOTTED, Collections.emptyList());
    this.addMethod(
        definitionKeeper,
        TYPE_PARENT,
        "do_something()",
        EnumSet.of(MethodDefinition.Modifier.ABSTRACT));
    this.addExemplar(
        definitionKeeper, TYPE_CHILD, ExemplarDefinition.Sort.MIXIN, List.of(TYPE_PARENT));
    final MagikTypedCheck check = new AbstractMethodNotImplementedTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @Test
  void testNoParents() {
    final String code = "def_slotted_exemplar(:child, {})\n";
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    this.addExemplar(
        definitionKeeper, TYPE_CHILD, ExemplarDefinition.Sort.SLOTTED, Collections.emptyList());
    final MagikTypedCheck check = new AbstractMethodNotImplementedTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  private static final String PSEUDO_ABSTRACT_PARENT_CODE =
      """
      _package sw
      def_slotted_exemplar(:parent, {})
      $
      _method parent.do_something()
        condition.raise(:subclass_should_implement)
        _return _unset
      _endmethod
      $
      """;

  private static final String CONCRETE_PARENT_CODE =
      """
      _package sw
      def_slotted_exemplar(:parent, {})
      $
      _method parent.do_something()
        _return 1
      _endmethod
      $
      """;

  private IDefinitionKeeper createIndexedKeeper(final String... codes) {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    for (int index = 0; index < codes.length; ++index) {
      final URI uri = URI.create("tests://indexed" + index);
      final MagikFile magikFile = new MagikFile(uri, codes[index]);
      final List<MagikDefinition> definitions = magikFile.getMagikDefinitions();
      definitions.forEach(definitionKeeper::add);
    }
    return definitionKeeper;
  }

  @Test
  void testPseudoAbstractMethodNotImplemented() {
    final String code =
        """
        _package sw
        def_slotted_exemplar(:child, {}, :parent)
        $
        """;
    final IDefinitionKeeper definitionKeeper =
        this.createIndexedKeeper(PSEUDO_ABSTRACT_PARENT_CODE, code);
    final MagikTypedCheck check = new AbstractMethodNotImplementedTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @Test
  void testPseudoAbstractMethodImplemented() {
    final String code =
        """
        _package sw
        def_slotted_exemplar(:child, {}, :parent)
        $
        _method child.do_something()
          _return 2
        _endmethod
        $
        """;
    final IDefinitionKeeper definitionKeeper =
        this.createIndexedKeeper(PSEUDO_ABSTRACT_PARENT_CODE, code);
    final MagikTypedCheck check = new AbstractMethodNotImplementedTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testPseudoAbstractMethodReraised() {
    final String code =
        """
        _package sw
        def_slotted_exemplar(:child, {}, :parent)
        $
        _method child.do_something()
          condition.raise(:subclass_should_implement)
        _endmethod
        $
        """;
    final IDefinitionKeeper definitionKeeper =
        this.createIndexedKeeper(PSEUDO_ABSTRACT_PARENT_CODE, code);
    final MagikTypedCheck check = new AbstractMethodNotImplementedTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testAbstractMethodRedeclaredAbstract() {
    final String code = "def_slotted_exemplar(:child, {}, :parent)\n";
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    this.addExemplar(
        definitionKeeper, TYPE_PARENT, ExemplarDefinition.Sort.SLOTTED, Collections.emptyList());
    this.addMethod(
        definitionKeeper,
        TYPE_PARENT,
        "do_something()",
        EnumSet.of(MethodDefinition.Modifier.ABSTRACT));
    this.addExemplar(
        definitionKeeper, TYPE_CHILD, ExemplarDefinition.Sort.SLOTTED, List.of(TYPE_PARENT));
    this.addMethod(
        definitionKeeper,
        TYPE_CHILD,
        "do_something()",
        EnumSet.of(MethodDefinition.Modifier.ABSTRACT));
    final MagikTypedCheck check = new AbstractMethodNotImplementedTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testAbstractMethodRedeclaredAbstractGrandchildNotImplemented() {
    final String code = "def_slotted_exemplar(:grandchild, {}, :child)\n";
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    this.addExemplar(
        definitionKeeper, TYPE_PARENT, ExemplarDefinition.Sort.SLOTTED, Collections.emptyList());
    this.addMethod(
        definitionKeeper,
        TYPE_PARENT,
        "do_something()",
        EnumSet.of(MethodDefinition.Modifier.ABSTRACT));
    this.addExemplar(
        definitionKeeper, TYPE_CHILD, ExemplarDefinition.Sort.SLOTTED, List.of(TYPE_PARENT));
    this.addMethod(
        definitionKeeper,
        TYPE_CHILD,
        "do_something()",
        EnumSet.of(MethodDefinition.Modifier.ABSTRACT));
    final TypeString grandchildTypeStr = TypeString.ofIdentifier("grandchild", "sw");
    this.addExemplar(
        definitionKeeper, grandchildTypeStr, ExemplarDefinition.Sort.SLOTTED, List.of(TYPE_CHILD));
    final MagikTypedCheck check = new AbstractMethodNotImplementedTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @Test
  void testKeywordAbstractParentNotReportedTwice() {
    final String parentCode =
        """
        _package sw
        def_slotted_exemplar(:parent, {})
        $
        _abstract _method parent.do_something()
          condition.raise(:subclass_should_implement)
        _endmethod
        $
        """;
    final String code =
        """
        _package sw
        def_slotted_exemplar(:child, {}, :parent)
        $
        """;
    final IDefinitionKeeper definitionKeeper = this.createIndexedKeeper(parentCode, code);
    final MagikTypedCheck check = new AbstractMethodNotImplementedTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @Test
  void testRefusingChildOfConcreteParentNotReported() {
    final String code =
        """
        _package sw
        def_slotted_exemplar(:child, {}, :parent)
        $
        _method child.do_something()
          condition.raise(:subclass_should_implement)
        _endmethod
        $
        """;
    final IDefinitionKeeper definitionKeeper = this.createIndexedKeeper(CONCRETE_PARENT_CODE, code);
    final MagikTypedCheck check = new AbstractMethodNotImplementedTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testRefusingChildDescendantNotImplemented() {
    final String childCode =
        """
        _package sw
        def_slotted_exemplar(:child, {}, :parent)
        $
        _method child.do_something()
          condition.raise(:subclass_should_implement)
        _endmethod
        $
        """;
    final String code =
        """
        _package sw
        def_slotted_exemplar(:grandchild, {}, :child)
        $
        """;
    final IDefinitionKeeper definitionKeeper =
        this.createIndexedKeeper(CONCRETE_PARENT_CODE, childCode, code);
    final MagikTypedCheck check = new AbstractMethodNotImplementedTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @Test
  void testAbstractMethodOfAParentDefinedTwiceNotImplemented() {
    final String code = "def_slotted_exemplar(:child, {}, :parent)\n";
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    for (final String moduleName : List.of("module_a", "module_b")) {
      definitionKeeper.add(
          new ExemplarDefinition(
              null,
              null,
              moduleName,
              null,
              null,
              ExemplarDefinition.Sort.SLOTTED,
              TYPE_PARENT,
              null));
    }
    this.addMethod(
        definitionKeeper,
        TYPE_PARENT,
        "do_something()",
        EnumSet.of(MethodDefinition.Modifier.ABSTRACT));
    this.addExemplar(
        definitionKeeper, TYPE_CHILD, ExemplarDefinition.Sort.SLOTTED, List.of(TYPE_PARENT));
    final MagikTypedCheck check = new AbstractMethodNotImplementedTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }
}
