package nl.ramsolutions.sw.magik.analysis.typing;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.SmallworldProjectExtension;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodUsage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/** Tests for {@link MethodUsageLocator}. */
class MethodUsageLocatorTest {

  @RegisterExtension
  final SmallworldProjectExtension smallworldProject = new SmallworldProjectExtension();

  @Test
  void testLocateMethodUsage() throws IOException {
    final String code =
        """
        def_slotted_exemplar(:a, {})

        _method a.method_name
          _self.method_name
        _endmethod
        """;
    final Path path = this.smallworldProject.pathOf("/source.magik");
    final MagikTypedFile magikFile = this.smallworldProject.addMagikFile(path, code);

    final IDefinitionKeeper definitionKeeper = this.smallworldProject.getDefinitionKeeper();
    final MethodUsageLocator methodUsageLocator = new MethodUsageLocator(definitionKeeper);
    final TypeString typeStr = TypeString.ofIdentifier("a", "user");
    final MethodUsage wantedMethodUsage = new MethodUsage(typeStr, "method_name");
    final List<Entry<MethodUsage, MagikTypedFile>> locatedMethodUsages =
        methodUsageLocator.getMethodUsages(wantedMethodUsage);

    assertThat(locatedMethodUsages).hasSize(1);
    final Entry<MethodUsage, MagikTypedFile> entry = locatedMethodUsages.get(0);
    final MethodUsage locatedMethodUsage = entry.getKey();
    assertThat(locatedMethodUsage).isEqualTo(new MethodUsage(typeStr, "method_name"));
    final MagikTypedFile locatedMagikFile = entry.getValue();
    assertThat(locatedMagikFile.getUri()).isEqualTo(magikFile.getUri());
  }

  @Test
  void testLocateMethodUsageOtherFile() throws IOException {
    final String codeA =
        """
        def_slotted_exemplar(:a, {})

        _method a.method_name
          _self.method_name
        _endmethod
        """;
    final Path pathA = this.smallworldProject.pathOf("/source_a.magik");
    final MagikTypedFile magikFileA = this.smallworldProject.addMagikFile(pathA, codeA);

    final String codeB =
        """
        _method b.method_name
          a.method_name
        _endmethod
        """;
    final Path pathB = this.smallworldProject.pathOf("/source_b.magik");
    final MagikTypedFile magikFileB = this.smallworldProject.addMagikFile(pathB, codeB);

    final IDefinitionKeeper definitionKeeper = this.smallworldProject.getDefinitionKeeper();
    final MethodUsageLocator methodUsageLocator = new MethodUsageLocator(definitionKeeper);
    final TypeString typeStr = TypeString.ofIdentifier("a", "user");
    final MethodUsage wantedMethodUsage = new MethodUsage(typeStr, "method_name");
    final List<Entry<MethodUsage, MagikTypedFile>> locatedMethodUsages =
        methodUsageLocator.getMethodUsages(wantedMethodUsage);

    assertThat(locatedMethodUsages).hasSize(2);

    // Returned order is random, so test as a set.
    final Set<MethodUsage> methodUsages =
        locatedMethodUsages.stream().map(entry -> entry.getKey()).collect(Collectors.toSet());
    assertThat(methodUsages).isEqualTo(Set.of(new MethodUsage(typeStr, "method_name")));

    final Set<URI> uris =
        locatedMethodUsages.stream()
            .map(entry -> entry.getValue().getUri())
            .collect(Collectors.toSet());
    assertThat(uris).isEqualTo(Set.of(magikFileA.getUri(), magikFileB.getUri()));
  }

  @Test
  void testSuperCallInsideMethodIsNotItsUsage() throws IOException {
    final String code =
        """
        def_slotted_exemplar(:p, {})
        def_slotted_exemplar(:t, {}, {:p})

        _method p.m
        _endmethod

        _method t.m
          _super.m
        _endmethod
        """;
    this.addMagikFile(code);

    final List<Entry<MethodUsage, MagikTypedFile>> usages = this.locateMethodUsages("t", "m");

    assertThat(usages).isEmpty();
  }

  @Test
  void testSuperCallInsideSiblingIsNotUsage() throws IOException {
    final String code =
        """
        def_slotted_exemplar(:p, {})
        def_slotted_exemplar(:t, {}, {:p})
        def_slotted_exemplar(:s, {}, {:p})

        _method p.m
        _endmethod

        _method t.m
        _endmethod

        _method s.m
          _super.m
        _endmethod
        """;
    this.addMagikFile(code);

    final List<Entry<MethodUsage, MagikTypedFile>> usages = this.locateMethodUsages("t", "m");

    assertThat(usages).isEmpty();
  }

  @Test
  void testSuperCallShadowedByNearerParentIsNotUsage() throws IOException {
    final String code =
        """
        def_slotted_exemplar(:t, {})
        def_slotted_exemplar(:u, {}, {:t})
        def_slotted_exemplar(:s, {}, {:u})

        _method t.m
        _endmethod

        _method u.m
        _endmethod

        _method s.m
          _super.m
        _endmethod
        """;
    this.addMagikFile(code);

    final List<Entry<MethodUsage, MagikTypedFile>> tUsages = this.locateMethodUsages("t", "m");
    final List<Entry<MethodUsage, MagikTypedFile>> uUsages = this.locateMethodUsages("u", "m");

    assertThat(tUsages).isEmpty();
    assertThat(uUsages).hasSize(1);
  }

  @Test
  void testSuperCallInsideChildIsParentUsage() throws IOException {
    final String code =
        """
        def_slotted_exemplar(:t, {})
        def_slotted_exemplar(:s, {}, {:t})

        _method t.m(p)
        _endmethod

        _method s.m(p)
          _super.m(1)
        _endmethod
        """;
    this.addMagikFile(code);

    final List<Entry<MethodUsage, MagikTypedFile>> usages = this.locateMethodUsages("t", "m()");

    assertThat(usages).hasSize(1);
  }

  @Test
  void testSuperCallThroughNonOverridingParentIsUsage() throws IOException {
    final String code =
        """
        def_slotted_exemplar(:t, {})
        def_slotted_exemplar(:u, {}, {:t})
        def_slotted_exemplar(:s, {}, {:u})

        _method t.m(p)
        _endmethod

        _method s.m(p)
          _super.m(1)
        _endmethod
        """;
    this.addMagikFile(code);

    final List<Entry<MethodUsage, MagikTypedFile>> usages = this.locateMethodUsages("t", "m()");

    assertThat(usages).hasSize(1);
  }

  @Test
  void testNamedSuperCallIsUsageOfNamedParentOnly() throws IOException {
    final String code =
        """
        def_slotted_exemplar(:t, {})
        def_slotted_exemplar(:q, {})
        def_slotted_exemplar(:s, {}, {:t, :q})

        _method t.m
        _endmethod

        _method q.m
        _endmethod

        _method s.m
          _super(q).m
        _endmethod
        """;
    this.addMagikFile(code);

    final List<Entry<MethodUsage, MagikTypedFile>> tUsages = this.locateMethodUsages("t", "m");
    final List<Entry<MethodUsage, MagikTypedFile>> qUsages = this.locateMethodUsages("q", "m");
    final List<Entry<MethodUsage, MagikTypedFile>> sUsages = this.locateMethodUsages("s", "m");

    assertThat(tUsages).isEmpty();
    assertThat(qUsages).hasSize(1);
    assertThat(sUsages).isEmpty();
  }

  private void addMagikFile(final String code) throws IOException {
    final Path path = this.smallworldProject.pathOf("/source.magik");
    this.smallworldProject.addMagikFile(path, code);
  }

  private List<Entry<MethodUsage, MagikTypedFile>> locateMethodUsages(
      final String exemplarName, final String methodName) {
    final IDefinitionKeeper definitionKeeper = this.smallworldProject.getDefinitionKeeper();
    final MethodUsageLocator methodUsageLocator = new MethodUsageLocator(definitionKeeper);
    final TypeString typeStr = TypeString.ofIdentifier(exemplarName, "user");
    final MethodUsage wantedMethodUsage = new MethodUsage(typeStr, methodName);
    return methodUsageLocator.getMethodUsages(wantedMethodUsage);
  }
}
