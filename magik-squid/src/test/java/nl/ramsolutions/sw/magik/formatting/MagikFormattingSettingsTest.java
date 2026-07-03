package nl.ramsolutions.sw.magik.formatting;

import static org.assertj.core.api.Assertions.assertThat;

import nl.ramsolutions.sw.MagikToolsProperties;
import org.junit.jupiter.api.Test;

/** Test {@link MagikFormattingSettings}. */
class MagikFormattingSettingsTest {

  private static MagikFormattingSettings settingsWithStrategy(final String strategy) {
    final MagikToolsProperties properties = new MagikToolsProperties();
    properties.setProperty(MagikFormattingSettings.KEY_MAGIK_FORMATTING_INDENT_STRATEGY, strategy);
    return new MagikFormattingSettings(properties);
  }

  @Test
  void testKnownStrategyIsValid() {
    final MagikFormattingSettings settings = settingsWithStrategy(VisualIndentWalker.STRATEGY_NAME);
    assertThat(settings.isIndentStrategyValid()).isTrue();
    assertThat(settings.getIndentStrategyClass()).isEqualTo(VisualIndentWalker.class);
  }

  @Test
  void testBackwardsCompatibleAliasIsValid() {
    final MagikFormattingSettings settings = settingsWithStrategy("relative");
    assertThat(settings.isIndentStrategyValid()).isTrue();
    assertThat(settings.getIndentStrategyClass()).isEqualTo(VisualIndentWalker.class);
  }

  @Test
  void testUnknownStrategyIsInvalid() {
    final MagikFormattingSettings settings = settingsWithStrategy("visua");
    assertThat(settings.isIndentStrategyValid()).isFalse();
  }

  @Test
  void testUnknownStrategyFallsBackToDefaultWalker() {
    final MagikFormattingSettings settings = settingsWithStrategy("visua");
    // B: does not throw; degrades gracefully to the default walker.
    assertThat(settings.getIndentStrategyClass()).isEqualTo(NullIndentWalker.class);
  }

  @Test
  void testUnknownStrategyErrorMessageSuggestsClosestValue() {
    final MagikFormattingSettings settings = settingsWithStrategy("visua");
    final String message = settings.getIndentStrategyErrorMessage();
    // D: suggests the closest known value and lists the allowed values.
    assertThat(message)
        .contains("Unknown indent strategy: \"visua\"")
        .contains(MagikFormattingSettings.KEY_MAGIK_FORMATTING_INDENT_STRATEGY)
        .contains("Did you mean \"visual\"?")
        .contains("\"block\", \"null\", \"visual\"");
  }

  @Test
  void testUnrelatedStrategyErrorMessageHasNoSuggestion() {
    final MagikFormattingSettings settings = settingsWithStrategy("qwerty");
    final String message = settings.getIndentStrategyErrorMessage();
    assertThat(message).doesNotContain("Did you mean");
  }
}
