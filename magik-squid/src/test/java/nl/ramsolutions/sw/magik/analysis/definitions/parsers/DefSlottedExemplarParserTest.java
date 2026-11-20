package nl.ramsolutions.sw.magik.analysis.definitions.parsers;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import nl.ramsolutions.sw.IDefinition;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import org.junit.jupiter.api.Test;

/** Tests for {@link DefSlottedExemplarParser}. */
class DefSlottedExemplarParserTest {

  private List<String> readSlotMethods(final String code) {
    final MagikFile magikFile = new MagikFile(MagikFile.DEFAULT_URI, code);
    final Collection<IDefinition> definitions = magikFile.getDefinitions();
    return definitions.stream()
        .filter(MethodDefinition.class::isInstance)
        .map(MethodDefinition.class::cast)
        .map(this::formatMethod)
        .toList();
  }

  private String formatMethod(final MethodDefinition definition) {
    final String methodName = definition.getMethodName();
    final Set<MethodDefinition.Modifier> modifiers = definition.getModifiers();
    final boolean isPrivate = modifiers.contains(MethodDefinition.Modifier.PRIVATE);
    return isPrivate ? methodName + " private" : methodName;
  }

  @Test
  void testReadableFlagWithoutFlavourGeneratesPublicGetter() {
    final String code =
        """
        _package user
        def_slotted_exemplar(:my_exemplar, {{:a, _unset, :readable}})
        $
        """;
    final List<String> methods = this.readSlotMethods(code);
    assertThat(methods).containsExactly("a");
  }

  @Test
  void testReadFlagGeneratesPublicGetter() {
    final String code =
        """
        _package user
        def_slotted_exemplar(:my_exemplar, {{:a, _unset, :read}})
        $
        """;
    final List<String> methods = this.readSlotMethods(code);
    assertThat(methods).containsExactly("a");
  }

  @Test
  void testReadFlagWithPrivateFlavourGeneratesPrivateGetter() {
    final String code =
        """
        _package user
        def_slotted_exemplar(:my_exemplar, {{:a, _unset, :read, :private}})
        $
        """;
    final List<String> methods = this.readSlotMethods(code);
    assertThat(methods).containsExactly("a private");
  }

  @Test
  void testWriteFlagGeneratesPublicAccessors() {
    final String code =
        """
        _package user
        def_slotted_exemplar(:my_exemplar, {{:a, _unset, :write}})
        $
        """;
    final List<String> methods = this.readSlotMethods(code);
    assertThat(methods).containsExactlyInAnyOrder("a", "a<<", "a^<<");
  }

  @Test
  void testWriteFlagWithPrivateFlavourGeneratesPrivateAccessors() {
    final String code =
        """
        _package user
        def_slotted_exemplar(:my_exemplar, {{:a, _unset, :write, :private}})
        $
        """;
    final List<String> methods = this.readSlotMethods(code);
    assertThat(methods).containsExactlyInAnyOrder("a private", "a<< private", "a^<< private");
  }

  @Test
  void testWritableFlagWithPrivateFlavourGeneratesPrivateAccessors() {
    final String code =
        """
        _package user
        def_slotted_exemplar(:my_exemplar, {{:a, _unset, :writable, :private}})
        $
        """;
    final List<String> methods = this.readSlotMethods(code);
    assertThat(methods).containsExactlyInAnyOrder("a private", "a<< private", "a^<< private");
  }

  @Test
  void testWritableFlagWithoutFlavourGeneratesPublicAccessors() {
    final String code =
        """
        _package user
        def_slotted_exemplar(:my_exemplar, {{:a, _unset, :writable}})
        $
        """;
    final List<String> methods = this.readSlotMethods(code);
    assertThat(methods).containsExactlyInAnyOrder("a", "a<<", "a^<<");
  }

  @Test
  void testSlotWithoutFlagGeneratesNoAccessors() {
    final String code =
        """
        _package user
        def_slotted_exemplar(:my_exemplar, {{:a, _unset}})
        $
        """;
    final List<String> methods = this.readSlotMethods(code);
    assertThat(methods).isEmpty();
  }
}
