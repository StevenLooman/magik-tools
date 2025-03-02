package nl.ramsolutions.sw.magik.analysis.typing.reasoner.recursive;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ParameterDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.SlotDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import org.junit.jupiter.api.Test;

class RecursiveDefinitionReasonerTest {

  /**
   * VSCode runs from module directory, mvn runs from project directory.
   *
   * @return Proper {@link Path} to file.
   */
  private Path getPath(final Path relativePath) {
    final Path path = Path.of(".").toAbsolutePath().getParent();
    if (path.endsWith("magik-squid")) {
      return Path.of("..").resolve(relativePath);
    }

    return Path.of(".").resolve(relativePath);
  }

  private void addFileToDefinitionKeeper(final IDefinitionKeeper definitionKeeper, final Path path)
      throws IOException {
    final Path fixedPath = this.getPath(path);
    final URI uri = fixedPath.toUri();
    final String content = Files.readString(fixedPath, Charset.defaultCharset());
    final MagikTypedFile magikTypedFile = new MagikTypedFile(uri, content, definitionKeeper);
    magikTypedFile.getDefinitions().forEach(definitionKeeper::add);
  }

  private void addDefineSharedConstantDefinition(
      final IDefinitionKeeper definitionKeeper, final TypeString typeStr) {
    definitionKeeper.add(
        new MethodDefinition(
            null,
            null,
            null,
            null,
            null,
            typeStr,
            "define_shared_constant()",
            Collections.emptySet(),
            List.of(
                new ParameterDefinition(
                    null,
                    null,
                    null,
                    null,
                    null,
                    "nameSymbol",
                    ParameterDefinition.Modifier.NONE,
                    TypeString.UNDEFINED),
                new ParameterDefinition(
                    null,
                    null,
                    null,
                    null,
                    null,
                    "value",
                    ParameterDefinition.Modifier.NONE,
                    TypeString.UNDEFINED),
                new ParameterDefinition(
                    null,
                    null,
                    null,
                    null,
                    null,
                    "privat",
                    ParameterDefinition.Modifier.NONE,
                    TypeString.UNDEFINED)),
            null,
            Collections.emptySet(),
            new ExpressionResultString(TypeString.ofParameterRef("value")),
            ExpressionResultString.EMPTY));
  }

  // region: Method reasoning.
  @Test
  void testRecursiveMethodReasoning() throws IOException {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final Path path = Path.of("magik-squid/src/test/resources/test_recursive_reasoner.magik");
    this.addFileToDefinitionKeeper(definitionKeeper, path);

    // Ensure typing isn't known currently.
    final TypeString typeStr = TypeString.ofIdentifier("test_exemplar", "user");
    final MethodDefinition methodDefinition =
        definitionKeeper.getMethodDefinitions(typeStr).stream()
            .filter(methodDef -> methodDef.getMethodName().equals("depth3()"))
            .findFirst()
            .orElseThrow();
    final ExpressionResultString resultStr = methodDefinition.getReturnTypes();
    assertThat(resultStr).isEqualTo(ExpressionResultString.UNDEFINED);

    // Recusively reason the method definition.
    final RecursiveDefinitionReasoner recursiveReasoner =
        new RecursiveDefinitionReasoner(definitionKeeper, 3);
    recursiveReasoner.reason(methodDefinition);

    // Test if the method definition now has a return type.
    final MethodDefinition updatedMethodDefinition =
        definitionKeeper.getMethodDefinitions(typeStr).stream()
            .filter(methodDef -> methodDef.getMethodName().equals("depth3()"))
            .findFirst()
            .orElseThrow();
    final ExpressionResultString updatedResultStr = updatedMethodDefinition.getReturnTypes();
    assertThat(updatedResultStr).isEqualTo(new ExpressionResultString(TypeString.SW_INTEGER));
  }

  @Test
  void testRecursiveMethodReasoningOverMaxDepth() throws IOException {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final Path path = Path.of("magik-squid/src/test/resources/test_recursive_reasoner.magik");
    this.addFileToDefinitionKeeper(definitionKeeper, path);

    // Ensure typing isn't known currently.
    final TypeString typeStr = TypeString.ofIdentifier("test_exemplar", "user");
    final MethodDefinition methodDefinition =
        definitionKeeper.getMethodDefinitions(typeStr).stream()
            .filter(methodDef -> methodDef.getMethodName().equals("depth3()"))
            .findFirst()
            .orElseThrow();
    final ExpressionResultString resultStr = methodDefinition.getReturnTypes();
    assertThat(resultStr).isEqualTo(ExpressionResultString.UNDEFINED);

    // Recusively reason the method definition.
    final RecursiveDefinitionReasoner recursiveReasoner =
        new RecursiveDefinitionReasoner(definitionKeeper, 1);
    recursiveReasoner.reason(methodDefinition);

    // Test if the method definition is not reasoned, as it surpasses max depth.
    final MethodDefinition updatedMethodDefinition =
        definitionKeeper.getMethodDefinitions(typeStr).stream()
            .filter(methodDef -> methodDef.getMethodName().equals("depth3()"))
            .findFirst()
            .orElseThrow();
    final ExpressionResultString updatedResultStr = updatedMethodDefinition.getReturnTypes();
    assertThat(updatedResultStr).isEqualTo(ExpressionResultString.UNDEFINED);
  }

  @Test
  void testRecursiveIterMethodReasoning() throws IOException {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final Path path = Path.of("magik-squid/src/test/resources/test_recursive_reasoner.magik");
    this.addFileToDefinitionKeeper(definitionKeeper, path);

    // Ensure typing isn't known currently.
    final TypeString typeStr = TypeString.ofIdentifier("test_exemplar", "user");
    final MethodDefinition methodDefinition =
        definitionKeeper.getMethodDefinitions(typeStr).stream()
            .filter(methodDef -> methodDef.getMethodName().equals("iter_depth3()"))
            .findFirst()
            .orElseThrow();
    final ExpressionResultString resultStr = methodDefinition.getReturnTypes();
    assertThat(resultStr).isEqualTo(ExpressionResultString.UNDEFINED);

    // Recusively reason the method definition.
    final RecursiveDefinitionReasoner recursiveReasoner =
        new RecursiveDefinitionReasoner(definitionKeeper, 3);
    recursiveReasoner.reason(methodDefinition);

    // Test if the method definition now has a return type.
    final MethodDefinition updatedMethodDefinition =
        definitionKeeper.getMethodDefinitions(typeStr).stream()
            .filter(methodDef -> methodDef.getMethodName().equals("iter_depth3()"))
            .findFirst()
            .orElseThrow();
    final ExpressionResultString updatedResultStr = updatedMethodDefinition.getReturnTypes();
    assertThat(updatedResultStr).isEqualTo(new ExpressionResultString(TypeString.SW_INTEGER));
  }

  @Test
  void testSharedConstantReasoning() throws IOException {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final Path path = Path.of("magik-squid/src/test/resources/test_recursive_reasoner.magik");
    this.addFileToDefinitionKeeper(definitionKeeper, path);

    // Ensure typing isn't known currently.
    final TypeString typeStr = TypeString.ofIdentifier("test_exemplar", "user");
    final MethodDefinition methodDefinition =
        definitionKeeper.getMethodDefinitions(typeStr).stream()
            .filter(methodDef -> methodDef.getMethodName().equals("shared_constant"))
            .findFirst()
            .orElseThrow();
    final ExpressionResultString resultStr = methodDefinition.getReturnTypes();
    assertThat(resultStr).isEqualTo(new ExpressionResultString(TypeString.UNDEFINED));

    // Add define_shared_constant() definition, which returns its second argument.
    // Normally this method is defined on sw:object, and the hierarchy would be set via the
    // exported definitions from a session.
    this.addDefineSharedConstantDefinition(definitionKeeper, typeStr);

    // Recusively reason the method definition.
    final RecursiveDefinitionReasoner recursiveReasoner =
        new RecursiveDefinitionReasoner(definitionKeeper, 3);
    recursiveReasoner.reason(methodDefinition);

    // Test if the method definition now has a return type.
    final MethodDefinition updatedMethodDefinition =
        definitionKeeper.getMethodDefinitions(typeStr).stream()
            .filter(methodDef -> methodDef.getMethodName().equals("shared_constant"))
            .findFirst()
            .orElseThrow();
    final ExpressionResultString updatedResultStr = updatedMethodDefinition.getReturnTypes();
    assertThat(updatedResultStr).isEqualTo(new ExpressionResultString(TypeString.SW_INTEGER));
  }

  @Test
  void testSharedConstantBlockReasoning() throws IOException {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final Path path = Path.of("magik-squid/src/test/resources/test_recursive_reasoner.magik");
    this.addFileToDefinitionKeeper(definitionKeeper, path);

    // Ensure typing isn't known currently.
    final TypeString typeStr = TypeString.ofIdentifier("test_exemplar", "user");
    final MethodDefinition methodDefinition =
        definitionKeeper.getMethodDefinitions(typeStr).stream()
            .filter(methodDef -> methodDef.getMethodName().equals("shared_constant_block"))
            .findFirst()
            .orElseThrow();
    final ExpressionResultString resultStr = methodDefinition.getReturnTypes();
    assertThat(resultStr).isEqualTo(new ExpressionResultString(TypeString.UNDEFINED));

    // Normally this method is defined on sw:object, and the hierarchy would be set via the
    // exported definitions from a session.
    this.addDefineSharedConstantDefinition(definitionKeeper, typeStr);

    // Recusively reason the method definition.
    final RecursiveDefinitionReasoner recursiveReasoner =
        new RecursiveDefinitionReasoner(definitionKeeper, 3);
    recursiveReasoner.reason(methodDefinition);

    // Test if the method definition now has a return type.
    final MethodDefinition updatedMethodDefinition =
        definitionKeeper.getMethodDefinitions(typeStr).stream()
            .filter(methodDef -> methodDef.getMethodName().equals("shared_constant_block"))
            .findFirst()
            .orElseThrow();
    final ExpressionResultString updatedResultStr = updatedMethodDefinition.getReturnTypes();
    assertThat(updatedResultStr).isEqualTo(new ExpressionResultString(TypeString.SW_INTEGER));
  }

  // endregion

  // region: Slot reasoning.
  @Test
  void testRecursiveSlotReasoningSlot1() throws IOException {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final Path path = Path.of("magik-squid/src/test/resources/test_recursive_reasoner.magik");
    this.addFileToDefinitionKeeper(definitionKeeper, path);

    // Ensure typing isn't known currently.
    final TypeString typeStr = TypeString.ofIdentifier("test_exemplar", "user");
    final SlotDefinition slotDefinition =
        definitionKeeper.getExemplarDefinitions(typeStr).stream()
            .flatMap(def -> def.getSlots().stream())
            .filter(def -> def.getName().equals("slot1"))
            .findFirst()
            .orElseThrow();
    final TypeString slotTypeStr = slotDefinition.getTypeName();
    assertThat(slotTypeStr).isEqualTo(TypeString.UNDEFINED);

    // Recusively reason the slot definition.
    final RecursiveDefinitionReasoner recursiveReasoner =
        new RecursiveDefinitionReasoner(definitionKeeper, 3);
    recursiveReasoner.reason(slotDefinition);

    // Test if the slot definition now has a type.
    final SlotDefinition updatedSlotDefinition =
        definitionKeeper.getExemplarDefinitions(typeStr).stream()
            .flatMap(def -> def.getSlots().stream())
            .filter(def -> def.getName().equals("slot1"))
            .findFirst()
            .orElseThrow();
    final TypeString updatedSlotTypeStr = updatedSlotDefinition.getTypeName();
    assertThat(updatedSlotTypeStr)
        .isEqualTo(TypeString.combine(TypeString.SW_UNSET, TypeString.SW_INTEGER));
  }

  @Test
  void testRecursiveSlotReasoningSlot2() throws IOException {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final Path path = Path.of("magik-squid/src/test/resources/test_recursive_reasoner.magik");
    this.addFileToDefinitionKeeper(definitionKeeper, path);

    // Ensure typing isn't known currently.
    final TypeString typeStr = TypeString.ofIdentifier("test_exemplar", "user");
    final SlotDefinition slotDefinition =
        definitionKeeper.getExemplarDefinitions(typeStr).stream()
            .flatMap(def -> def.getSlots().stream())
            .filter(def -> def.getName().equals("slot2"))
            .findFirst()
            .orElseThrow();
    final TypeString slotTypeStr = slotDefinition.getTypeName();
    assertThat(slotTypeStr).isEqualTo(TypeString.UNDEFINED);

    // Recusively reason the slot definition.
    final RecursiveDefinitionReasoner recursiveReasoner =
        new RecursiveDefinitionReasoner(definitionKeeper, 3);
    recursiveReasoner.reason(slotDefinition);

    // Test if the slot definition now has a type.
    final SlotDefinition updatedSlotDefinition =
        definitionKeeper.getExemplarDefinitions(typeStr).stream()
            .flatMap(def -> def.getSlots().stream())
            .filter(def -> def.getName().equals("slot2"))
            .findFirst()
            .orElseThrow();
    final TypeString updatedSlotTypeStr = updatedSlotDefinition.getTypeName();
    assertThat(updatedSlotTypeStr).isEqualTo(TypeString.SW_INTEGER);
  }
  // endregion
}
