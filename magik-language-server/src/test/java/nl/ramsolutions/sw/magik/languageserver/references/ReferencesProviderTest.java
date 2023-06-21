package nl.ramsolutions.sw.magik.languageserver.references;

import java.net.URI;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.Location;
import nl.ramsolutions.sw.magik.analysis.Position;
import nl.ramsolutions.sw.magik.analysis.Range;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.types.MagikType;
import nl.ramsolutions.sw.magik.analysis.typing.types.Method;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test ReferencesProvider.
 */
@SuppressWarnings("checkstyle:MagicNumber")
class ReferencesProviderTest {

    private static final URI TEST_URI = URI.create("tests://unittest");
    private static final Location EMPTY_LOCATION = new Location(
        URI.create("tests://unittest"),
        new Range(new Position(0, 0), new Position(0, 0)));

    private List<org.eclipse.lsp4j.Location> getReferences(
                final String code, final org.eclipse.lsp4j.Position position, final ITypeKeeper typeKeeper) {
        final MagikTypedFile magikFile = new MagikTypedFile(TEST_URI, code, typeKeeper);
        final ReferencesProvider provider = new ReferencesProvider();
        return provider.provideReferences(magikFile, position);
    }

    @Test
    void testProvideMethodReferenceFromMethodInvocation() {
        final ITypeKeeper typeKeeper = new TypeKeeper();
        final TypeString integerRef = TypeString.ofIdentifier("integer", "sw");
        final MagikType integerType = (MagikType) typeKeeper.getType(integerRef);
        final Method referingMethod = integerType.addMethod(
            EMPTY_LOCATION,
            EnumSet.noneOf(Method.Modifier.class),
            "refering",
            Collections.emptyList(),
            null,
            null,
            ExpressionResultString.UNDEFINED,
            new ExpressionResultString());
        final TypeString undefinedTypeRef = TypeString.UNDEFINED;
        final Method.MethodUsage calledMethod =
            new Method.MethodUsage(undefinedTypeRef, "refering", EMPTY_LOCATION);
        referingMethod.addCalledMethod(calledMethod);

        final String code = ""
            + "_method integer.refering\n"
            + "    _self.refering\n"
            + "_endmethod\n";
        final org.eclipse.lsp4j.Position position = new org.eclipse.lsp4j.Position(1, 12);  // On `refering`.
        final List<org.eclipse.lsp4j.Location> references = this.getReferences(code, position, typeKeeper);
        assertThat(references).hasSize(1);
    }

    @Test
    void testProvideMethodReferenceFromMethodDefintion() {
        final ITypeKeeper typeKeeper = new TypeKeeper();
        final TypeString integerRef = TypeString.ofIdentifier("integer", "sw");
        final MagikType integerType = (MagikType) typeKeeper.getType(integerRef);
        final Method referingMethod = integerType.addMethod(
            EMPTY_LOCATION,
            EnumSet.noneOf(Method.Modifier.class),
            "refering",
            Collections.emptyList(),
            null,
            null,
            ExpressionResultString.UNDEFINED,
            new ExpressionResultString());
        final TypeString undefinedTypeRef = TypeString.UNDEFINED;
        final Method.MethodUsage calledMethod =
            new Method.MethodUsage(undefinedTypeRef, "refering", EMPTY_LOCATION);
        referingMethod.addCalledMethod(calledMethod);

        final String code = ""
            + "_method integer.refering\n"
            + "    _self.refering\n"
            + "_endmethod\n";
        final org.eclipse.lsp4j.Position position = new org.eclipse.lsp4j.Position(0, 20);    // On `refering`.
        final List<org.eclipse.lsp4j.Location> references = this.getReferences(code, position, typeKeeper);
        assertThat(references).hasSize(1);
    }

    @Test
    void testProvideTypeReferenceFromAtom() {
        final ITypeKeeper typeKeeper = new TypeKeeper();
        final TypeString integerRef = TypeString.ofIdentifier("integer", "sw");
        final MagikType integerType = (MagikType) typeKeeper.getType(integerRef);
        final Method referingMethod = integerType.addMethod(
            EMPTY_LOCATION,
            EnumSet.noneOf(Method.Modifier.class),
            "refering",
            Collections.emptyList(),
            null,
            null,
            ExpressionResultString.UNDEFINED,
            new ExpressionResultString());
        final Method.GlobalUsage typeUsage = new Method.GlobalUsage(integerRef, EMPTY_LOCATION);
        referingMethod.addUsedGlobal(typeUsage);

        final String code = ""
            + "_method integer.refering\n"
            + "    integer\n"
            + "_endmethod\n";
        final org.eclipse.lsp4j.Position position = new org.eclipse.lsp4j.Position(1, 4);    // On `integer`.
        final List<org.eclipse.lsp4j.Location> references = this.getReferences(code, position, typeKeeper);
        assertThat(references).hasSize(1);
    }

    @Test
    void testProvideTypeReferenceFromMethodDefinition() {
        final ITypeKeeper typeKeeper = new TypeKeeper();
        final TypeString integerRef = TypeString.ofIdentifier("integer", "sw");
        final MagikType integerType = (MagikType) typeKeeper.getType(integerRef);
        final Method referingMethod = integerType.addMethod(
            EMPTY_LOCATION,
            EnumSet.noneOf(Method.Modifier.class),
            "refering",
            Collections.emptyList(),
            null,
            null,
            ExpressionResultString.UNDEFINED,
            new ExpressionResultString());
        final Method.GlobalUsage typeUsage = new Method.GlobalUsage(integerRef, EMPTY_LOCATION);
        referingMethod.addUsedGlobal(typeUsage);

        final String code = ""
            + "_method integer.refering\n"
            + "    print(integer)\n"
            + "_endmethod\n";
        final org.eclipse.lsp4j.Position position = new org.eclipse.lsp4j.Position(0, 10);    // On `integer`.
        final List<org.eclipse.lsp4j.Location> references = this.getReferences(code, position, typeKeeper);
        assertThat(references).hasSize(1);
    }

}
