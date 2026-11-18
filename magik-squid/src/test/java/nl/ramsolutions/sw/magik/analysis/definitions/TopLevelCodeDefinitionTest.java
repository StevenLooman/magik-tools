package nl.ramsolutions.sw.magik.analysis.definitions;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonar.sslr.api.AstNode;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import nl.ramsolutions.sw.MagikToolsProperties;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import org.junit.jupiter.api.Test;

/** Test the {@link TopLevelCodeDefinition}s a file yields, and the usages they hold. */
class TopLevelCodeDefinitionTest {

  private static final String INDEX_METHOD_USAGES = "magik.typing.indexMethodUsages";
  private static final String INDEX_GLOBAL_USAGES = "magik.typing.indexGlobalUsages";
  private static final String INDEX_CONDITION_USAGES = "magik.typing.indexConditionUsages";
  private static final String INDEX_UNARY_OPERATOR_USAGES = "magik.typing.indexUnaryOperatorUsages";
  private static final String INDEX_BINARY_OPERATOR_USAGES =
      "magik.typing.indexBinaryOperatorUsages";

  private static final URI FILE_URI = URI.create("file:///thing.magik");

  private static final String SHAPE =
      """
      _package user
      _method thing.make(kind)
      	>> _clone.init(kind)
      _endmethod
      $
      thing.define_shared_constant(:default, thing.make(:plain), :public)
      $
      _block
      	thing.make(:other)
      _endblock
      $
      """;

  private static final Map<String, String> ALL_USAGES =
      Map.of(
          INDEX_METHOD_USAGES, "true",
          INDEX_GLOBAL_USAGES, "true",
          INDEX_CONDITION_USAGES, "true",
          INDEX_UNARY_OPERATOR_USAGES, "true",
          INDEX_BINARY_OPERATOR_USAGES, "true");

  private MagikFile createMagikFile(final String code, final Map<String, String> settings) {
    final MagikToolsProperties properties = new MagikToolsProperties(settings);
    return new MagikFile(properties, FILE_URI, code);
  }

  private List<TopLevelCodeDefinition> readDefinitions(
      final String code, final Map<String, String> settings) {
    final MagikFile magikFile = this.createMagikFile(code, settings);
    final List<MagikDefinition> definitions = magikFile.getMagikDefinitions();
    return definitions.stream()
        .filter(TopLevelCodeDefinition.class::isInstance)
        .map(TopLevelCodeDefinition.class::cast)
        .toList();
  }

  private List<TopLevelCodeDefinition> readDefinitions(final String code) {
    return this.readDefinitions(code, ALL_USAGES);
  }

  private TopLevelCodeDefinition readOnlyDefinition(final String code) {
    final List<TopLevelCodeDefinition> definitions = this.readDefinitions(code);
    assertThat(definitions).hasSize(1);
    return definitions.get(0);
  }

  private List<String> readMethodNames(final TopLevelCodeDefinition definition) {
    final List<MethodUsage> usages = definition.getUsedMethods();
    return usages.stream().map(MethodUsage::getMethodName).toList();
  }

  private List<String> readMethodNames(final List<TopLevelCodeDefinition> definitions) {
    return definitions.stream().map(this::readMethodNames).flatMap(List::stream).toList();
  }

  private List<String> readGlobalNames(final TopLevelCodeDefinition definition) {
    final List<GlobalUsage> usages = definition.getUsedGlobals();
    return usages.stream().map(GlobalUsage::getTypeName).map(TypeString::getFullString).toList();
  }

  private List<String> readConditionNames(final TopLevelCodeDefinition definition) {
    final List<ConditionUsage> usages = definition.getUsedConditions();
    return usages.stream().map(ConditionUsage::getConditionName).toList();
  }

  private List<String> readOperators(final TopLevelCodeDefinition definition) {
    final List<BinaryOperatorUsage> usages = definition.getUsedBinaryOperators();
    return usages.stream().map(BinaryOperatorUsage::getOperator).toList();
  }

  private List<String> readHeldMethodNames(final String code) {
    final MagikFile magikFile = this.createMagikFile(code, ALL_USAGES);
    final List<MagikDefinition> definitions = magikFile.getMagikDefinitions();
    return definitions.stream()
        .flatMap(
            definition -> {
              if (definition instanceof final MethodDefinition methodDefinition) {
                return methodDefinition.getUsedMethods().stream();
              } else if (definition instanceof final ProcedureDefinition procedureDefinition) {
                return procedureDefinition.getUsedMethods().stream();
              } else if (definition instanceof final GlobalDefinition globalDefinition) {
                return globalDefinition.getUsedMethods().stream();
              }

              return Stream.empty();
            })
        .map(MethodUsage::getMethodName)
        .toList();
  }

  @Test
  void testOneDefinitionPerUnitWithTopLevelCode() {
    final List<TopLevelCodeDefinition> definitions = this.readDefinitions(SHAPE);
    assertThat(definitions).hasSize(2);
  }

  @Test
  void testEachUnitHoldsItsOwnCalls() {
    final List<TopLevelCodeDefinition> definitions = this.readDefinitions(SHAPE);
    final TopLevelCodeDefinition firstDefinition = definitions.get(0);
    final TopLevelCodeDefinition secondDefinition = definitions.get(1);
    final List<String> first = this.readMethodNames(firstDefinition);
    final List<String> second = this.readMethodNames(secondDefinition);
    assertThat(first).containsExactly("define_shared_constant()", "make()");
    assertThat(second).containsExactly("make()");
  }

  @Test
  void testNoOtherDefinitionHoldsCallsOfTopLevelStatements() {
    final List<String> held = this.readHeldMethodNames(SHAPE);
    assertThat(held).containsExactly("init()");
  }

  @Test
  void testNoDefinitionForAUnitOfMethodsOnly() {
    final String code =
        """
        _package user
        _pragma(classify_level=basic)
        _method thing.a()
        	>> thing.b()
        _endmethod
        _method thing.c()
        _endmethod
        $
        """;
    final List<TopLevelCodeDefinition> definitions = this.readDefinitions(code);
    assertThat(definitions).isEmpty();
  }

  @Test
  void testNoDefinitionForAUnitOfAGlobalOnly() {
    final String code =
        """
        _package user
        _global g << thing.make(:x)
        $
        """;
    final List<TopLevelCodeDefinition> definitions = this.readDefinitions(code);
    final List<String> held = this.readHeldMethodNames(code);
    assertThat(definitions).isEmpty();
    assertThat(held).containsExactly("make()");
  }

  @Test
  void testNoDefinitionForAUnitOfAPackageOnly() {
    final String code =
        """
        _package user
        $
        """;
    final List<TopLevelCodeDefinition> definitions = this.readDefinitions(code);
    assertThat(definitions).isEmpty();
  }

  @Test
  void testNoDefinitionForAUnitOfASyntaxErrorOnly() {
    final String code =
        """
        _package user
        $
        ) ( _endmethod
        $
        """;
    final List<TopLevelCodeDefinition> definitions = this.readDefinitions(code);
    assertThat(definitions).isEmpty();
  }

  @Test
  void testNoDefinitionForAUnitOfAProcedureOnly() {
    final String code =
        """
        _package user
        _proc@p()
        	>> other.run()
        _endproc
        $
        """;
    final List<TopLevelCodeDefinition> definitions = this.readDefinitions(code);
    final List<String> held = this.readHeldMethodNames(code);
    assertThat(definitions).isEmpty();
    assertThat(held).containsExactly("run()");
  }

  @Test
  void testDefinitionForAUnitOfAnExemplarDefinition() {
    final String code =
        """
        _package user
        def_slotted_exemplar(:thing, {{:a, thing.make(:x)}})
        $
        """;
    final TopLevelCodeDefinition definition = this.readOnlyDefinition(code);
    final List<String> names = this.readMethodNames(definition);
    final List<String> globals = this.readGlobalNames(definition);
    assertThat(names).containsExactly("make()");
    assertThat(globals).containsExactly("user:def_slotted_exemplar", "user:thing");
  }

  @Test
  void testDefinitionForAnUriThatIsNoFile() {
    final MagikToolsProperties properties = new MagikToolsProperties(ALL_USAGES);
    final MagikFile magikFile = new MagikFile(properties, MagikFile.DEFAULT_URI, SHAPE);
    final List<MagikDefinition> definitions = magikFile.getMagikDefinitions();
    final long count =
        definitions.stream().filter(TopLevelCodeDefinition.class::isInstance).count();
    assertThat(count).isEqualTo(2);
  }

  @Test
  void testLocationSpansTheUnit() {
    final List<TopLevelCodeDefinition> definitions = this.readDefinitions(SHAPE);
    final TopLevelCodeDefinition definition = definitions.get(1);
    final Location location = definition.getLocation();
    final URI uri = location.getUri();
    final Range range = location.getRange();
    assertThat(uri).isEqualTo(FILE_URI);
    assertThat(range.getStartPosition()).isEqualTo(new Position(8, 0));
    assertThat(range.getEndPosition()).isEqualTo(new Position(11, 1));
  }

  @Test
  void testLocationStartsAtTheMethodOpeningTheUnit() {
    final String code =
        """
        _package user
        $
        _method thing.a()
        _endmethod
        thing.a()
        $
        """;
    final TopLevelCodeDefinition definition = this.readOnlyDefinition(code);
    final Location location = definition.getLocation();
    final Range range = location.getRange();
    assertThat(range.getStartPosition()).isEqualTo(new Position(3, 0));
  }

  @Test
  void testNamesCountTheUnitsWithTopLevelCode() {
    final List<TopLevelCodeDefinition> definitions = this.readDefinitions(SHAPE);
    final List<String> names = definitions.stream().map(TopLevelCodeDefinition::getName).toList();
    assertThat(names).containsExactly("_top__thing_magik__0", "_top__thing_magik__1");
  }

  @Test
  void testNodeIsTheFirstTopLevelStatement() {
    final List<TopLevelCodeDefinition> definitions = this.readDefinitions(SHAPE);
    final TopLevelCodeDefinition definition = definitions.get(0);
    final AstNode node = definition.getNode();
    final String tokenValue = node.getTokenValue();
    assertThat(tokenValue).isEqualTo("thing");
  }

  @Test
  void testNoCallOfAProcedureInADefineArgument() {
    final String code =
        """
        _package user
        thing.define_shared_constant(:default, _proc() >> other.run() _endproc, :public)
        $
        """;
    final TopLevelCodeDefinition definition = this.readOnlyDefinition(code);
    final List<String> names = this.readMethodNames(definition);
    final List<String> held = this.readHeldMethodNames(code);
    assertThat(names).containsExactly("define_shared_constant()");
    assertThat(held).containsExactly("run()");
  }

  @Test
  void testNoCallOfAProcedureInABlock() {
    final String code =
        """
        _package user
        _block
        	_local p << _proc() >> other.run() _endproc
        	p()
        _endblock
        $
        """;
    final TopLevelCodeDefinition definition = this.readOnlyDefinition(code);
    final List<String> names = this.readMethodNames(definition);
    assertThat(names).isEmpty();
  }

  @Test
  void testNoCallOfANamedProcedureGlobal() {
    final String code =
        """
        _package user
        _global g << _proc@g()
        	>> other.run()
        _endproc
        thing.run()
        $
        """;
    final TopLevelCodeDefinition definition = this.readOnlyDefinition(code);
    final List<String> names = this.readMethodNames(definition);
    final List<String> held = this.readHeldMethodNames(code);
    assertThat(names).containsExactly("run()");
    assertThat(held).containsExactly("run()");
  }

  @Test
  void testNoCallOfAGlobalInitialiserBesideOtherCode() {
    final String code =
        """
        _package user
        _global g << thing.make(:x)
        thing.other()
        $
        """;
    final TopLevelCodeDefinition definition = this.readOnlyDefinition(code);
    final List<String> names = this.readMethodNames(definition);
    assertThat(names).containsExactly("other()");
  }

  @Test
  void testNoCallOfAGlobalInABlock() {
    final String code =
        """
        _package user
        _block
        	_global g << thing.make(:x)
        _endblock
        $
        """;
    final List<TopLevelCodeDefinition> definitions = this.readDefinitions(code);
    final List<String> names = this.readMethodNames(definitions);
    final List<String> held = this.readHeldMethodNames(code);
    assertThat(names).isEmpty();
    assertThat(held).containsExactly("make()");
  }

  @Test
  void testTheCallOfALocalInitialiser() {
    final String code =
        """
        _package user
        _local x << thing.make(:a)
        $
        """;
    final TopLevelCodeDefinition definition = this.readOnlyDefinition(code);
    final List<String> names = this.readMethodNames(definition);
    assertThat(names).containsExactly("make()");
  }

  @Test
  void testTheGlobalsOfTopLevelStatements() {
    final List<TopLevelCodeDefinition> definitions = this.readDefinitions(SHAPE);
    final TopLevelCodeDefinition firstDefinition = definitions.get(0);
    final TopLevelCodeDefinition secondDefinition = definitions.get(1);
    final List<String> first = this.readGlobalNames(firstDefinition);
    final List<String> second = this.readGlobalNames(secondDefinition);
    assertThat(first).containsExactly("user:thing", "user:thing");
    assertThat(second).containsExactly("user:thing");
  }

  @Test
  void testTheGlobalOfTheCurrentPackage() {
    final String code =
        """
        _package sw
        thing.run()
        $
        _package user
        other.run()
        $
        """;
    final List<TopLevelCodeDefinition> definitions = this.readDefinitions(code);
    final TopLevelCodeDefinition firstDefinition = definitions.get(0);
    final TopLevelCodeDefinition secondDefinition = definitions.get(1);
    final List<String> first = this.readGlobalNames(firstDefinition);
    final List<String> second = this.readGlobalNames(secondDefinition);
    assertThat(first).containsExactly("sw:thing");
    assertThat(second).containsExactly("user:other");
  }

  @Test
  void testNoGlobalOfAMethodNameOrAConditionName() {
    final String code =
        """
        _package user
        write("a")
        !output!.write("b")
        _try
        	thing << 1
        _when thing
        _endtry
        $
        """;
    final TopLevelCodeDefinition definition = this.readOnlyDefinition(code);
    final List<String> names = this.readGlobalNames(definition);
    assertThat(names).containsExactly("user:write", "user:!output!", "user:thing");
  }

  @Test
  void testTheBinaryOperatorsOfTopLevelStatements() {
    final String code =
        """
        _package user
        thing.define_shared_constant(:default, 1 + 2, :public)
        $
        """;
    final TopLevelCodeDefinition definition = this.readOnlyDefinition(code);
    final List<String> operators = this.readOperators(definition);
    assertThat(operators).containsExactly("+");
  }

  @Test
  void testTheAugmentedOperatorOfATopLevelStatement() {
    final String code =
        """
        _package user
        _local x << 1
        x +<< 2
        $
        """;
    final TopLevelCodeDefinition definition = this.readOnlyDefinition(code);
    final List<String> operators = this.readOperators(definition);
    assertThat(operators).containsExactly("+");
  }

  @Test
  void testTheUnaryOperatorsFoldedIntoTheMethodUsages() {
    final String code =
        """
        _package user
        thing.define_shared_constant(:flag, _not thing.ready?, :public)
        $
        """;
    final TopLevelCodeDefinition definition = this.readOnlyDefinition(code);
    final List<String> names = this.readMethodNames(definition);
    assertThat(names).containsExactly("define_shared_constant()", "ready?", "not");
  }

  @Test
  void testTheConditionsOfTopLevelStatements() {
    final String code =
        """
        _package user
        _try
        	condition.raise(:my_error)
        _when my_other_error
        _endtry
        $
        """;
    final TopLevelCodeDefinition definition = this.readOnlyDefinition(code);
    final List<String> names = this.readConditionNames(definition);
    assertThat(names).containsExactly("my_other_error", "my_error");
  }

  @Test
  void testNoConditionOfAProcedure() {
    final String code =
        """
        _package user
        thing.define_shared_constant(:p, _proc() condition.raise(:my_error) _endproc, :public)
        $
        """;
    final TopLevelCodeDefinition definition = this.readOnlyDefinition(code);
    final List<String> names = this.readConditionNames(definition);
    assertThat(names).isEmpty();
  }

  @Test
  void testDefinitionHoldsNothingWhenUsagesAreNotIndexed() {
    final Map<String, String> settings =
        Map.of(
            INDEX_METHOD_USAGES, "false",
            INDEX_GLOBAL_USAGES, "false",
            INDEX_CONDITION_USAGES, "false",
            INDEX_UNARY_OPERATOR_USAGES, "false",
            INDEX_BINARY_OPERATOR_USAGES, "false");
    final String code =
        """
        _package user
        _try
        	thing.define_shared_constant(:flag, _not thing.ready? _and 1 + 2, :public)
        _when my_error
        _endtry
        $
        """;
    final List<TopLevelCodeDefinition> definitions = this.readDefinitions(code, settings);
    assertThat(definitions).hasSize(1);
    final TopLevelCodeDefinition definition = definitions.get(0);
    final List<MethodUsage> usedMethods = definition.getUsedMethods();
    final List<GlobalUsage> usedGlobals = definition.getUsedGlobals();
    final List<ConditionUsage> usedConditions = definition.getUsedConditions();
    final List<BinaryOperatorUsage> usedBinaryOperators = definition.getUsedBinaryOperators();
    assertThat(usedMethods).isEmpty();
    assertThat(usedGlobals).isEmpty();
    assertThat(usedConditions).isEmpty();
    assertThat(usedBinaryOperators).isEmpty();
  }

  @Test
  void testUnaryOperatorsAreLeftOutWhenNotIndexed() {
    final Map<String, String> settings =
        Map.of(
            INDEX_METHOD_USAGES, "true",
            INDEX_UNARY_OPERATOR_USAGES, "false");
    final String code =
        """
        _package user
        thing.define_shared_constant(:flag, _not thing.ready?, :public)
        $
        """;
    final List<TopLevelCodeDefinition> definitions = this.readDefinitions(code, settings);
    final List<String> names = this.readMethodNames(definitions);
    assertThat(names).containsExactly("define_shared_constant()", "ready?");
  }

  @Test
  void testOneKindOfUsageWhenOnlyThatKindIsIndexed() {
    final Map<String, String> settings =
        Map.of(
            INDEX_METHOD_USAGES, "true",
            INDEX_GLOBAL_USAGES, "false",
            INDEX_BINARY_OPERATOR_USAGES, "false");
    final List<TopLevelCodeDefinition> definitions = this.readDefinitions(SHAPE, settings);
    final List<String> names = this.readMethodNames(definitions);
    final TopLevelCodeDefinition firstDefinition = definitions.get(0);
    final List<String> globals = this.readGlobalNames(firstDefinition);
    assertThat(names).hasSize(3);
    assertThat(globals).isEmpty();
  }

  @Test
  void testUsageLocationsPointAtTheCalls() {
    final List<TopLevelCodeDefinition> definitions = this.readDefinitions(SHAPE);
    final TopLevelCodeDefinition definition = definitions.get(1);
    final List<MethodUsage> usages = definition.getUsedMethods();
    final MethodUsage usage = usages.get(0);
    final Location location = usage.getLocation();
    final URI uri = location.getUri();
    final Range range = location.getRange();
    final Position start = range.getStartPosition();
    assertThat(uri).isEqualTo(FILE_URI);
    assertThat(start.getLine()).isEqualTo(9);
  }

  @Test
  void testCallsOfAProcedureWithASyntaxErrorAreHeldByNobody() {
    // Mid-edit state: the procedure yields no definition, and the next index corrects it.
    final String code =
        """
        _package user
        thing.define_shared_constant(:p, _proc(a) a.foo( _endproc, :public)
        $
        """;
    final List<TopLevelCodeDefinition> definitions = this.readDefinitions(code);
    final List<String> names = this.readMethodNames(definitions);
    final List<String> held = this.readHeldMethodNames(code);
    assertThat(names).doesNotContain("foo()");
    assertThat(held).doesNotContain("foo()");
  }

  @Test
  void testCallAfterTheProcedureOfAGlobalIsHeldByTheGlobal() {
    final String code =
        """
        _package user
        _global f << _proc@f()
        	>> other.run()
        _endproc.bar()
        $
        """;
    final List<TopLevelCodeDefinition> definitions = this.readDefinitions(code);
    final List<String> held = this.readHeldMethodNames(code);
    assertThat(definitions).isEmpty();
    assertThat(held).contains("bar()");
  }

  private TopLevelCodeDefinition createUnit(final MethodUsage usage) {
    final Location location = new Location(FILE_URI, Range.DEFAULT_RANGE);
    return new TopLevelCodeDefinition(
        location,
        null,
        null,
        null,
        null,
        "n",
        List.of(usage),
        List.of(),
        List.of(),
        List.of(),
        Provenance.UNKNOWN);
  }

  /** Equality leaves the usages out, as for a global or a method. */
  @Test
  void testEqualsLeavesTheCallsOfTheUnitOut() {
    final Location location = new Location(FILE_URI, Range.DEFAULT_RANGE);
    final MethodUsage make = new MethodUsage(TypeString.UNDEFINED, "make()", location, null);
    final MethodUsage init = new MethodUsage(TypeString.UNDEFINED, "init()", location, null);
    final TopLevelCodeDefinition withMake = this.createUnit(make);
    final TopLevelCodeDefinition withInit = this.createUnit(init);
    assertThat(withMake).isEqualTo(withInit).hasSameHashCodeAs(withInit);
  }

  @Test
  void testEqualsLeavesTheReceiverTypesOut() {
    final Location location = new Location(FILE_URI, Range.DEFAULT_RANGE);
    final TypeString thing = TypeString.ofIdentifier("thing", "user");
    final MethodUsage untyped = new MethodUsage(TypeString.UNDEFINED, "make()", location, null);
    final MethodUsage typed = new MethodUsage(thing, "make()", location, null);
    final TopLevelCodeDefinition withUntyped = this.createUnit(untyped);
    final TopLevelCodeDefinition withTyped = this.createUnit(typed);
    assertThat(withUntyped).isEqualTo(withTyped).hasSameHashCodeAs(withTyped);
  }

  @Test
  void testBareDefinitionHoldsNoNodes() {
    final List<TopLevelCodeDefinition> definitions = this.readDefinitions(SHAPE);
    final TopLevelCodeDefinition definition = definitions.get(0);
    final TopLevelCodeDefinition bare = definition.getBareDefinition();
    final List<MethodUsage> usages = bare.getUsedMethods();
    final AstNode node = bare.getNode();
    assertThat(node).isNull();
    assertThat(usages).isNotEmpty().allMatch(usage -> usage.getNode() == null);
  }
}
