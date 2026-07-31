package nl.ramsolutions.sw.magik.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import nl.ramsolutions.sw.MagikToolsProperties;
import org.junit.jupiter.api.Test;

/** Tests for {@link MagikAnalysisSettings}. */
class MagikAnalysisSettingsTest {

  @Test
  void testSourcePathMappingsReadsFlattenedObject() {
    // The client sends `magik.sourcePathMappings` as an object; the converter flattens it to
    // `magik.sourcePathMappings.<from>` = `<to>`.
    final MagikToolsProperties properties =
        new MagikToolsProperties(
            Map.of(
                "magik.sourcePathMappings.$SOMS_DIR", "/opt/soms",
                "magik.sourcePathMappings.C:/projects/hg/gma", "$SMALLWORLD_GIS/gma"));

    final Map<String, String> mappings =
        new MagikAnalysisSettings(properties).getSourcePathMappings();

    assertThat(mappings)
        .containsOnly(
            Map.entry("$SOMS_DIR", "/opt/soms"),
            Map.entry("C:/projects/hg/gma", "$SMALLWORLD_GIS/gma"));
  }

  @Test
  void testSourcePathMappingsEmptyWhenUnset() {
    final MagikToolsProperties properties = new MagikToolsProperties(Map.of());
    assertThat(new MagikAnalysisSettings(properties).getSourcePathMappings()).isEmpty();
  }
}
