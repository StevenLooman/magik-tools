package nl.ramsolutions.sw.magik.languageserver.typehierarchy;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SymbolKind;
import org.eclipse.lsp4j.TypeHierarchyItem;
import org.junit.jupiter.api.Test;

/** Tests for TypeHierarchyProvider. */
class TypeHierarchyProviderTest {

  private static final URI TEST_URI = URI.create("memory://source.magik");

  private List<TypeHierarchyItem> getPrepareTypeHierarchy(
      final String code, final Position position, final IDefinitionKeeper definitionKeeper) {
    final MagikTypedFile magikFile = new MagikTypedFile(TEST_URI, code, definitionKeeper);
    final TypeHierarchyProvider provider = new TypeHierarchyProvider(definitionKeeper);
    return provider.prepareTypeHierarchy(magikFile, position);
  }

  @Test
  void testPrepareTypeHierarchyMethodDefinitionExemplarName() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final TypeString exemplarRef = TypeString.ofIdentifier("exemplar", "user");
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.SLOTTED,
            exemplarRef,
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptySet()));

    final String code =
        """
        _method exemplar.method
        _endmethod
        """;
    final Position position = new Position(0, 10); // On 'exemplar'.

    final List<TypeHierarchyItem> items =
        this.getPrepareTypeHierarchy(code, position, definitionKeeper);
    assertThat(items).isNotNull().hasSize(1);
  }

  @Test
  void testPrepareTypeHierarchyGlobal() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final TypeString ropeRef = TypeString.ofIdentifier("rope", "sw");
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.SLOTTED,
            ropeRef,
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptySet()));

    final String code =
        """
        _method exemplar.method
          rope.new()
        _endmethod
        """;
    final Position position = new Position(1, 4); // On 'rope'.

    final List<TypeHierarchyItem> items =
        this.getPrepareTypeHierarchy(code, position, definitionKeeper);
    assertThat(items).hasSize(1);
  }

  @Test
  void testGetSubtypes() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final TypeString exemplarRef = TypeString.ofIdentifier("exemplar", "user");
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.SLOTTED,
            exemplarRef,
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptySet()));
    final TypeString subExemplarRef = TypeString.ofIdentifier("sub_exemplar", "user");
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.SLOTTED,
            subExemplarRef,
            Collections.emptyList(),
            List.of(exemplarRef),
            Collections.emptySet()));

    final TypeHierarchyItem item =
        new TypeHierarchyItem(
            "user:exemplar", SymbolKind.Class, TEST_URI.toString(), new Range(), new Range());
    final TypeHierarchyProvider provider = new TypeHierarchyProvider(definitionKeeper);
    final List<TypeHierarchyItem> subTypes = provider.typeHierarchySubtypes(item);
    assertThat(subTypes).isNotNull().hasSize(1);
    final TypeHierarchyItem subType = subTypes.get(0);
    assertThat(subType.getName()).isEqualTo("user:sub_exemplar");
    assertThat(subType.getKind()).isEqualTo(SymbolKind.Class);
    assertThat(subType.getUri()).isEqualTo(TEST_URI.toString());
  }

  @Test
  void testGetSupertypes() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final TypeString exemplarRef = TypeString.ofIdentifier("exemplar", "user");
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.SLOTTED,
            exemplarRef,
            Collections.emptyList(),
            List.of(TypeString.ofIdentifier("slotted_format_mixin", "sw")),
            Collections.emptySet()));

    final TypeHierarchyItem item =
        new TypeHierarchyItem(
            "user:exemplar", SymbolKind.Class, TEST_URI.toString(), new Range(), new Range());

    final TypeHierarchyProvider provider = new TypeHierarchyProvider(definitionKeeper);
    final List<TypeHierarchyItem> superTypes = provider.typeHierarchySupertypes(item);
    assertThat(superTypes).isNotNull().hasSize(1);
    final TypeHierarchyItem superType = superTypes.get(0);
    assertThat(superType.getName()).isEqualTo("sw:slotted_format_mixin");
    assertThat(superType.getKind()).isEqualTo(SymbolKind.Class);
    assertThat(superType.getUri()).isEqualTo(TEST_URI.toString());
  }
}
