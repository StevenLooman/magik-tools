package nl.ramsolutions.sw.magik.languageserver.typehierarchy;

import java.net.URI;
import java.util.List;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.types.MagikType;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SymbolKind;
import org.eclipse.lsp4j.TypeHierarchyItem;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for TypeHierarchyProvider.
 */
public class TypeHierarchyProviderTest {

    private static final URI TEST_URI = URI.create("tests://unittest");

    private List<TypeHierarchyItem> getPrepareTypeHierarchy(
            final String code, final Position position, final ITypeKeeper typeKeeper) {
        final MagikTypedFile magikFile = new MagikTypedFile(TEST_URI, code, typeKeeper);
        final TypeHierarchyProvider provider = new TypeHierarchyProvider(typeKeeper);
        return provider.prepareTypeHierarchy(magikFile, position);
    }

    @Test
    void testPrepareTypeHierarchyMethodDefinitionExemplarName() {
        final ITypeKeeper typeKeeper = new TypeKeeper();
        final TypeString exemplarRef = TypeString.ofIdentifier("exemplar", "user");
        new MagikType(typeKeeper, MagikType.Sort.SLOTTED, exemplarRef);

        final String code = ""
            + "_method exemplar.method\n"
            + "_endmethod\n";
        final Position position = new Position(0, 10);    // On 'exemplar'.

        final List<TypeHierarchyItem> items = this.getPrepareTypeHierarchy(code, position, typeKeeper);
        assertThat(items)
            .isNotNull()
            .hasSize(1);
    }

    @Test
    void testPrepareTypeHierarchyGlobal() {
        final ITypeKeeper typeKeeper = new TypeKeeper();
        final TypeString ropeRef = TypeString.ofIdentifier("rope", "sw");
        new MagikType(typeKeeper, MagikType.Sort.SLOTTED, ropeRef);

        final String code = ""
            + "_method exemplar.method\n"
            + "  rope.new()\n"
            + "_endmethod\n";
        final Position position = new Position(1, 4);    // On 'rope'.

        final List<TypeHierarchyItem> items = this.getPrepareTypeHierarchy(code, position, typeKeeper);
        assertThat(items)
            .isNotNull()
            .hasSize(1);
    }

    @Test
    void testGetSubtypes() {
        final ITypeKeeper typeKeeper = new TypeKeeper();
        final TypeString exemplarRef = TypeString.ofIdentifier("exemplar", "user");
        new MagikType(typeKeeper, MagikType.Sort.SLOTTED, exemplarRef);
        final TypeString subExemplarRef = TypeString.ofIdentifier("sub_exemplar", "user");
        final MagikType subExemplarType = new MagikType(typeKeeper, MagikType.Sort.SLOTTED, subExemplarRef);
        subExemplarType.addParent(exemplarRef);

        final TypeHierarchyItem item = new TypeHierarchyItem(
            "user:exemplar",
            SymbolKind.Class,
            TEST_URI.toString(),
            new Range(),
            new Range());
        final TypeHierarchyProvider provider = new TypeHierarchyProvider(typeKeeper);
        final List<TypeHierarchyItem> subtypes = provider.typeHierarchySubtypes(item);
        assertThat(subtypes)
            .isNotNull()
            .hasSize(1);
    }

    @Test
    void testGetSupertypes() {
        final ITypeKeeper typeKeeper = new TypeKeeper();
        final TypeString exemplarRef = TypeString.ofIdentifier("exemplar", "user");
        final MagikType exemplarType = new MagikType(typeKeeper, MagikType.Sort.SLOTTED, exemplarRef);
        final TypeString objectRef = TypeString.ofIdentifier("object", "sw");
        exemplarType.addParent(objectRef);

        final TypeHierarchyItem item = new TypeHierarchyItem(
            "user:exemplar",
            SymbolKind.Class,
            TEST_URI.toString(),
            new Range(),
            new Range());

        final TypeHierarchyProvider provider = new TypeHierarchyProvider(typeKeeper);
        final List<TypeHierarchyItem> supertypes =  provider.typeHierarchySupertypes(item);
        assertThat(supertypes)
            .isNotNull()
            .hasSize(1);
    }

}