package nl.ramsolutions.sw.magik.analysis.definitions.io;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import nl.ramsolutions.sw.MagikToolsProperties;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.analysis.definitions.BinaryOperatorUsage;
import nl.ramsolutions.sw.magik.analysis.definitions.ConditionUsage;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.GlobalDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.GlobalUsage;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MagikDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MagikFileDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodUsage;
import nl.ramsolutions.sw.magik.analysis.definitions.Provenance;
import nl.ramsolutions.sw.magik.analysis.definitions.TopLevelCodeDefinition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Round trips of {@link TopLevelCodeDefinition} and a global's conditions through JSON. */
class JsonTopLevelCodeRoundTripTest {

  private static final String INDEX_METHOD_USAGES = "magik.typing.indexMethodUsages";
  private static final String INDEX_GLOBAL_USAGES = "magik.typing.indexGlobalUsages";
  private static final String INDEX_CONDITION_USAGES = "magik.typing.indexConditionUsages";
  private static final String INDEX_UNARY_OPERATOR_USAGES = "magik.typing.indexUnaryOperatorUsages";
  private static final String INDEX_BINARY_OPERATOR_USAGES =
      "magik.typing.indexBinaryOperatorUsages";

  private static final URI FILE_URI = URI.create("file:///usages.magik");

  private static final String CODE =
      """
      _package user
      _global g << _try
      	>> _not condition.raise(:global_error)
      _when other_global_error
      _endtry
      $
      _try
      	thing.define_shared_constant(:default, _not thing.make(:plain) + 1, :public)
      _when my_error
      _endtry
      $
      """;

  @TempDir private Path tempDir;

  private IDefinitionKeeper createIndexedKeeper() {
    final MagikToolsProperties properties =
        new MagikToolsProperties(
            Map.of(
                INDEX_METHOD_USAGES, "true",
                INDEX_GLOBAL_USAGES, "true",
                INDEX_CONDITION_USAGES, "true",
                INDEX_UNARY_OPERATOR_USAGES, "true",
                INDEX_BINARY_OPERATOR_USAGES, "true"));
    final MagikFile magikFile = new MagikFile(properties, FILE_URI, CODE);
    final IDefinitionKeeper keeper = new DefinitionKeeper(false);
    for (final MagikDefinition definition : magikFile.getMagikDefinitions()) {
      final MagikDefinition bare = (MagikDefinition) definition.getBareDefinition();
      final MagikDefinition indexed = bare.withProvenance(Provenance.INDEXED);
      keeper.add(indexed);
    }

    return keeper;
  }

  private IDefinitionKeeper writeAndRead(final IDefinitionKeeper written) throws IOException {
    final Path path = this.tempDir.resolve("types.jsonl");
    JsonDefinitionWriter.write(path, written);
    final IDefinitionKeeper read = new DefinitionKeeper(false);
    JsonDefinitionReader.readTypes(path, read);
    return read;
  }

  @Test
  void testRoundTripKeepsTheTopLevelCode() throws IOException {
    final IDefinitionKeeper written = this.createIndexedKeeper();

    final IDefinitionKeeper read = this.writeAndRead(written);

    final Collection<TopLevelCodeDefinition> writtenDefinitions =
        written.getTopLevelCodeDefinitions();
    final Collection<TopLevelCodeDefinition> definitions = read.getTopLevelCodeDefinitions();
    assertThat(definitions).hasSize(1).containsExactlyInAnyOrderElementsOf(writtenDefinitions);
    final TopLevelCodeDefinition definition = definitions.iterator().next();
    final List<MethodUsage> usedMethods = definition.getUsedMethods();
    final List<GlobalUsage> usedGlobals = definition.getUsedGlobals();
    final List<ConditionUsage> usedConditions = definition.getUsedConditions();
    final List<BinaryOperatorUsage> usedBinaryOperators = definition.getUsedBinaryOperators();
    final Provenance provenance = definition.getProvenance();
    assertThat(usedMethods)
        .extracting(MethodUsage::getMethodName)
        .containsExactly("define_shared_constant()", "make()", "not");
    assertThat(usedGlobals).hasSize(2);
    assertThat(usedConditions).extracting(ConditionUsage::getConditionName).contains("my_error");
    assertThat(usedBinaryOperators).extracting(BinaryOperatorUsage::getOperator).contains("+");
    assertThat(provenance).isEqualTo(Provenance.INDEXED);
  }

  @Test
  void testRoundTripKeepsTheConditionsAndUnaryOperatorsOfAGlobal() throws IOException {
    final IDefinitionKeeper written = this.createIndexedKeeper();

    final IDefinitionKeeper read = this.writeAndRead(written);

    final Collection<GlobalDefinition> definitions = read.getGlobalDefinitions();
    final GlobalDefinition definition = definitions.iterator().next();
    final List<ConditionUsage> usedConditions = definition.getUsedConditions();
    final List<MethodUsage> usedMethods = definition.getUsedMethods();
    assertThat(usedConditions)
        .extracting(ConditionUsage::getConditionName)
        .containsExactly("other_global_error", "global_error");
    assertThat(usedMethods).extracting(MethodUsage::getMethodName).contains("raise()", "not");
  }

  @Test
  void testDatabaseWithoutTopLevelCodeStillReads() throws IOException {
    // A database as develop writes it: no top-level code, no global conditions.
    final Path path = this.tempDir.resolve("old.jsonl");
    final String versionLine =
        "{\"instruction\":\"schema_version\",\"value\":"
            + JsonDefinitionReader.SCHEMA_VERSION
            + "}";
    final String fileLine =
        "{\"instruction\":\"magik_file\",\"location\":{\"uri\":\"file:///old.magik\"},"
            + "\"timestamp\":[1234567890,0],\"provenance\":\"DUMPED\"}";
    final String globalLine =
        "{\"instruction\":\"global\",\"location\":{\"uri\":\"file:///old.magik\"},"
            + "\"type_name\":\"user:g\",\"aliased_type_name\":\"_undefined\","
            + "\"used_methods\":[],\"used_globals\":[],\"used_binary_operators\":[]}";
    Files.write(path, List.of(versionLine, fileLine, globalLine));
    final IDefinitionKeeper keeper = new DefinitionKeeper(false);

    JsonDefinitionReader.readTypes(path, keeper);

    final Collection<MagikFileDefinition> files = keeper.getMagikFileDefinitions();
    final Collection<GlobalDefinition> globals = keeper.getGlobalDefinitions();
    final Collection<TopLevelCodeDefinition> units = keeper.getTopLevelCodeDefinitions();
    assertThat(files).hasSize(1);
    assertThat(globals).hasSize(1);
    final GlobalDefinition global = globals.iterator().next();
    final List<ConditionUsage> usedConditions = global.getUsedConditions();
    assertThat(usedConditions).isEmpty();
    assertThat(units).isEmpty();
  }
}
