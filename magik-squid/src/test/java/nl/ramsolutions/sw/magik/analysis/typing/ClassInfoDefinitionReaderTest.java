package nl.ramsolutions.sw.magik.analysis.typing;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.analysis.definitions.ConditionDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.GlobalDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ParameterDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.SlotDefinition;
import org.junit.jupiter.api.Test;

/** Tests for {@link ClassInfoDefinitionReader}. */
class ClassInfoDefinitionReaderTest {

  @Test
  void testRead() throws IOException {
    final Path path = Path.of("src/test/resources/magik_tools.class_definition_reader_test.1.jar");
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    ClassInfoDefinitionReader.readTypes(path, definitionKeeper);

    // Globals.
    final TypeString doAnotherThingRef = TypeString.ofIdentifier("!do_another_thing!", "sw");
    final Collection<GlobalDefinition> doAnotherThingGlobalDefs =
        definitionKeeper.getGlobalDefinitions(doAnotherThingRef);
    assertThat(doAnotherThingGlobalDefs)
        .containsExactly(
            new GlobalDefinition(
                new Location(
                    URI.create(
                        "file:///$SMALLWORLD_GIS/sw_core/modules/sw_core/example_module/source/globals.magik")),
                "class_definition_reader_test",
                """

            This is an example global
            to be tested.

            """,
                null,
                doAnotherThingRef,
                TypeString.UNDEFINED));
    final TypeString reportRef = TypeString.ofIdentifier("!report!", "sw");
    final Collection<GlobalDefinition> reportGlobalDefs =
        definitionKeeper.getGlobalDefinitions(reportRef);
    assertThat(reportGlobalDefs)
        .containsExactly(
            new GlobalDefinition(
                new Location(
                    URI.create(
                        "file:///$SMALLWORLD_GIS/sw_core/modules/sw_core/example_module/source/globals.magik")),
                "class_definition_reader_test",
                """
            This will report. The
            params are the things
            that are going to be reported.

            See also: !do_another_thing!
            """,
                null,
                reportRef,
                TypeString.UNDEFINED));

    // Conditions.
    final Collection<ConditionDefinition> condition1Defs =
        definitionKeeper.getConditionDefinitions("example_condition_1");
    assertThat(condition1Defs)
        .containsExactly(
            new ConditionDefinition(
                new Location(
                    URI.create(
                        "file:///$SMALLWORLD_GIS/sw_core/modules/sw_core/example_module/source/conditions.magik")),
                "class_definition_reader_test",
                "",
                null,
                "example_condition_1",
                null,
                Collections.emptyList()));

    final Collection<ConditionDefinition> condition2Defs =
        definitionKeeper.getConditionDefinitions("example_condition_2");
    assertThat(condition2Defs)
        .containsExactly(
            new ConditionDefinition(
                new Location(
                    URI.create(
                        "file:///$SMALLWORLD_GIS/sw_core/modules/sw_core/example_module/source/conditions.magik")),
                "class_definition_reader_test",
                "Datas are to be conditioned.\n",
                null,
                "example_condition_2",
                null,
                List.of("data1", "data2")));

    // Mixins.
    final TypeString exampleMixinRef = TypeString.ofIdentifier("example_mixin", "sw");
    final Collection<ExemplarDefinition> exampleMixinDefs =
        definitionKeeper.getExemplarDefinitions(exampleMixinRef);
    assertThat(exampleMixinDefs)
        .containsExactly(
            new ExemplarDefinition(
                new Location(
                    URI.create(
                        "file:///$SMALLWORLD_GIS/sw_core/modules/sw_core/example_module/source/example_mixin.magik")),
                "class_definition_reader_test",
                """

            Example mixin for class_info.

            """,
                null,
                ExemplarDefinition.Sort.INTRINSIC,
                exampleMixinRef,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptySet()));

    // Classes.
    final TypeString exampleClassRef = TypeString.ofIdentifier("example_class", "sw");
    final Collection<ExemplarDefinition> exampleClassTypes =
        definitionKeeper.getExemplarDefinitions(exampleClassRef);
    assertThat(exampleClassTypes)
        .containsExactly(
            new ExemplarDefinition(
                new Location(
                    URI.create(
                        "file:///$SMALLWORLD_GIS/sw_core/modules/sw_core/example_module/source/example_class.magik")),
                "class_definition_reader_test",
                "Example class\n",
                null,
                ExemplarDefinition.Sort.UNDEFINED,
                exampleClassRef,
                List.of(
                    new SlotDefinition(
                        null,
                        "class_definition_reader_test",
                        null,
                        null,
                        "slot1",
                        TypeString.UNDEFINED),
                    new SlotDefinition(
                        null,
                        "class_definition_reader_test",
                        null,
                        null,
                        "slot2",
                        TypeString.UNDEFINED),
                    new SlotDefinition(
                        null,
                        "class_definition_reader_test",
                        null,
                        null,
                        "slot3",
                        TypeString.UNDEFINED)),
                List.of(TypeString.ofIdentifier("model", "sw")),
                Collections.emptySet()));

    // Enumerations.
    final TypeString enumerationRef = TypeString.ofIdentifier("example_enumeration", "sw");
    final Collection<ExemplarDefinition> exampleEnumerationTypes =
        definitionKeeper.getExemplarDefinitions(enumerationRef);
    assertThat(exampleEnumerationTypes)
        .containsExactly(
            new ExemplarDefinition(
                new Location(
                    URI.create(
                        "file:///$SMALLWORLD_GIS/sw_core/modules/sw_core/example_module/source/example_enumeration.magik")),
                "class_definition_reader_test",
                "",
                null,
                ExemplarDefinition.Sort.UNDEFINED,
                enumerationRef,
                Collections.emptyList(),
                List.of(TypeString.ofIdentifier("enumerated_format_mixin", "sw")),
                Collections.emptySet()));

    // Enumerations.
    final TypeString exampleIndexedClassRef =
        TypeString.ofIdentifier("example_indexed_class", "sw");
    final Collection<ExemplarDefinition> exampleIndexedClassTypes =
        definitionKeeper.getExemplarDefinitions(exampleIndexedClassRef);
    assertThat(exampleIndexedClassTypes)
        .containsExactly(
            new ExemplarDefinition(
                new Location(
                    URI.create(
                        "file:///$SMALLWORLD_GIS/sw_core/modules/sw_core/example_module/source/example_indexed_class.magik")),
                "class_definition_reader_test",
                "Attributes that will be printed\n",
                null,
                ExemplarDefinition.Sort.UNDEFINED,
                exampleIndexedClassRef,
                Collections.emptyList(),
                List.of(TypeString.ofIdentifier("simple_index_mixin", "sw")),
                Collections.emptySet()));

    // Methods.
    final Collection<MethodDefinition> doSomethingMethodDefs =
        definitionKeeper.getMethodDefinitions(exampleMixinRef).stream()
            .filter(def -> def.getMethodName().equals("do_something()"))
            .collect(Collectors.toSet());
    assertThat(doSomethingMethodDefs)
        .containsExactly(
            new MethodDefinition(
                new Location(
                    URI.create(
                        "file:///$SMALLWORLD_GIS/sw_core/modules/sw_core/example_module/source/example_mixin.magik")),
                "class_definition_reader_test",
                "",
                null,
                exampleMixinRef,
                "do_something()",
                Set.of(MethodDefinition.Modifier.PRIVATE),
                List.of(
                    new ParameterDefinition(
                        null,
                        "class_definition_reader_test",
                        null,
                        null,
                        "param1",
                        ParameterDefinition.Modifier.NONE,
                        TypeString.UNDEFINED),
                    new ParameterDefinition(
                        null,
                        "class_definition_reader_test",
                        null,
                        null,
                        "param2",
                        ParameterDefinition.Modifier.NONE,
                        TypeString.UNDEFINED)),
                null,
                Collections.emptySet(),
                ExpressionResultString.UNDEFINED,
                ExpressionResultString.UNDEFINED));

    final Collection<MethodDefinition> doSomethingElseMethodDefs =
        definitionKeeper.getMethodDefinitions(exampleMixinRef).stream()
            .filter(def -> def.getMethodName().equals("do_something_else()"))
            .collect(Collectors.toSet());
    assertThat(doSomethingElseMethodDefs)
        .containsExactly(
            new MethodDefinition(
                new Location(
                    URI.create(
                        "file:///$SMALLWORLD_GIS/sw_core/modules/sw_core/example_module/source/example_mixin.magik")),
                "class_definition_reader_test",
                """

This is a longer method doc
to be tested.
And then some more
( things ).

PARAM1 is a rope with
{ value1, value2, value3, ... }
for examples.

""",
                null,
                exampleMixinRef,
                "do_something_else()",
                Collections.emptySet(),
                List.of(
                    new ParameterDefinition(
                        null,
                        "class_definition_reader_test",
                        null,
                        null,
                        "param1",
                        ParameterDefinition.Modifier.NONE,
                        TypeString.UNDEFINED)),
                null,
                Collections.emptySet(),
                ExpressionResultString.UNDEFINED,
                ExpressionResultString.UNDEFINED));
  }
}
