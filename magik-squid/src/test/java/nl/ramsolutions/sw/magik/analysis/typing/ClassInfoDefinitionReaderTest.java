package nl.ramsolutions.sw.magik.analysis.typing;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.analysis.definitions.ConditionDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.GlobalDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import org.junit.jupiter.api.Test;

/** Tests for ClassInfoDefinitionReader. */
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
    assertThat(doAnotherThingGlobalDefs).isNotEmpty();
    final TypeString reportRef = TypeString.ofIdentifier("!report!", "sw");
    final Collection<GlobalDefinition> reportGlobalDefs =
        definitionKeeper.getGlobalDefinitions(reportRef);
    assertThat(reportGlobalDefs).isNotEmpty();

    // Conditions.
    final Collection<ConditionDefinition> condition1s =
        definitionKeeper.getConditionDefinitions("example_condition_1");
    assertThat(condition1s).isNotEmpty();
    final Collection<ConditionDefinition> condition2s =
        definitionKeeper.getConditionDefinitions("example_condition_2");
    assertThat(condition2s).isNotNull();

    // Mixins.
    final TypeString exampleMixinRef = TypeString.ofIdentifier("example_mixin", "sw");
    final Collection<ExemplarDefinition> exampleMixinDefs =
        definitionKeeper.getExemplarDefinitions(exampleMixinRef);
    assertThat(exampleMixinDefs).isNotEmpty();

    // Classes.
    final TypeString exampleClassRef = TypeString.ofIdentifier("example_class", "sw");
    final Collection<ExemplarDefinition> exampleClassTypes =
        definitionKeeper.getExemplarDefinitions(exampleClassRef);
    assertThat(exampleClassTypes).isNotEmpty();

    // Methods.
    final Collection<MethodDefinition> doSomethingMethodDefs =
        definitionKeeper.getMethodDefinitions(exampleMixinRef).stream()
            .filter(def -> def.getMethodName().equals("do_something()"))
            .collect(Collectors.toSet());
    assertThat(doSomethingMethodDefs).isNotEmpty();

    final Collection<MethodDefinition> doSomethingElseMethodDefs =
        definitionKeeper.getMethodDefinitions(exampleMixinRef).stream()
            .filter(def -> def.getMethodName().equals("do_something_else()"))
            .collect(Collectors.toSet());
    assertThat(doSomethingElseMethodDefs).isNotEmpty();
  }
}
