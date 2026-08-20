package nl.ramsolutions.sw.magik.analysis.definitions.io;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.GlobalDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.Provenance;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JsonProvenanceRoundTripTest {

  @Test
  void manualProvenanceSurvivesJsonRoundTrip(@TempDir Path dir) throws Exception {
    final Path path = dir.resolve("types.jsonl");
    final IDefinitionKeeper source = new DefinitionKeeper(false);
    final GlobalDefinition definition =
        new GlobalDefinition(
            null, null, null, null, null, TypeString.ofIdentifier("g", "sw"), TypeString.SW_UNSET);
    final GlobalDefinition provenancedDefinition = definition.withProvenance(Provenance.MANUAL);
    source.add(provenancedDefinition);
    JsonDefinitionWriter.write(path, source);

    final IDefinitionKeeper target = new DefinitionKeeper(false);
    JsonDefinitionReader.readTypes(path, target, Provenance.DUMPED); // default ignored: stored wins
    final GlobalDefinition read =
        target.getGlobalDefinitions(TypeString.ofIdentifier("g", "sw")).iterator().next();
    assertThat(read.getProvenance()).isEqualTo(Provenance.MANUAL);
  }

  @Test
  void missingProvenancePropertyUsesDefault(@TempDir Path dir) throws Exception {
    final Path p = dir.resolve("legacy.jsonl");
    Files.writeString(
        p,
        "{\"instruction\":\"global\",\"type_name\":\"sw:g\",\"aliased_type_name\":\"sw:unset\"}\n");
    final IDefinitionKeeper keeper = new DefinitionKeeper(false);
    JsonDefinitionReader.readTypes(p, keeper, Provenance.CLASS_INFO);
    assertThat(
            keeper
                .getGlobalDefinitions(TypeString.ofIdentifier("g", "sw"))
                .iterator()
                .next()
                .getProvenance())
        .isEqualTo(Provenance.CLASS_INFO);
  }
}
