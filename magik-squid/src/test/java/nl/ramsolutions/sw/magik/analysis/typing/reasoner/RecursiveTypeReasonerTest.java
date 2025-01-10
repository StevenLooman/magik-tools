package nl.ramsolutions.sw.magik.analysis.typing.reasoner;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import org.junit.jupiter.api.Test;

class RecursiveTypeReasonerTest {

  /**
   * VSCode runs from module directory, mvn runs from project directory.
   *
   * @return Proper {@link Path} to file.
   */
  protected Path getPath(final Path relativePath) {
    final Path path = Path.of(".").toAbsolutePath().getParent();
    if (path.endsWith("magik-squid")) {
      return Path.of("..").resolve(relativePath);
    }

    return Path.of(".").resolve(relativePath);
  }

  protected void addFileToDefinitionKeeper(
      final IDefinitionKeeper definitionKeeper, final Path path) throws IOException {
    final URI uri = path.toUri();
    final String content = Files.readString(path, Charset.defaultCharset());
    final MagikTypedFile magikTypedFile = new MagikTypedFile(uri, content, definitionKeeper);
    magikTypedFile.getDefinitions().forEach(definitionKeeper::add);
  }

  @Test
  void testRecursiveReasoningDepth1() throws IOException {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final Path path = Path.of("magik-squid/src/test/resources/test_recursive_type_reasoner.magik");
    final Path fixedPath = this.getPath(path);
    this.addFileToDefinitionKeeper(definitionKeeper, fixedPath);

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
    final RecursiveTypeReasoner recursiveTypeReasoner =
        new RecursiveTypeReasoner(definitionKeeper, 3);
    recursiveTypeReasoner.reason(methodDefinition);

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
  void testRecursiveReasoningOverMaxDepth() throws IOException {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final Path path = Path.of("magik-squid/src/test/resources/test_recursive_type_reasoner.magik");
    final Path fixedPath = this.getPath(path);
    this.addFileToDefinitionKeeper(definitionKeeper, fixedPath);

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
    final RecursiveTypeReasoner recursiveTypeReasoner =
        new RecursiveTypeReasoner(definitionKeeper, 1);
    recursiveTypeReasoner.reason(methodDefinition);

    // Test if the method definition is not reasoned, as it surpasses max depth.
    final MethodDefinition updatedMethodDefinition =
        definitionKeeper.getMethodDefinitions(typeStr).stream()
            .filter(methodDef -> methodDef.getMethodName().equals("depth3()"))
            .findFirst()
            .orElseThrow();
    final ExpressionResultString updatedResultStr = updatedMethodDefinition.getReturnTypes();
    assertThat(updatedResultStr).isEqualTo(ExpressionResultString.UNDEFINED);
  }
}
