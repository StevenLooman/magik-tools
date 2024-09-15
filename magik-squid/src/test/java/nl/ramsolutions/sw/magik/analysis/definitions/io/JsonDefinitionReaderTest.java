package nl.ramsolutions.sw.magik.analysis.definitions.io;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.analysis.definitions.BinaryOperatorDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ConditionDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.GlobalDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MagikFileDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.PackageDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ParameterDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.SlotDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.moduledef.ModuleDefinition;
import nl.ramsolutions.sw.productdef.ProductDefinition;
import org.junit.jupiter.api.Test;

/** Tests for JsonDefinitionReader. */
class JsonDefinitionReaderTest {

  private IDefinitionKeeper readTypes() throws IOException {
    final Path path = Path.of("src/test/resources/tests/type_database.jsonl");
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    JsonDefinitionReader.readTypes(path, definitionKeeper);
    return definitionKeeper;
  }

  @Test
  void testReadProduct() throws IOException {
    final IDefinitionKeeper definitionKeeper = this.readTypes();

    final Collection<ProductDefinition> productDefs =
        definitionKeeper.getProductDefinitions("test_product");
    assertThat(productDefs).hasSize(1);

    final ProductDefinition testProductDef = productDefs.stream().findAny().orElseThrow();
    assertThat(testProductDef)
        .isEqualTo(
            new ProductDefinition(
                null,
                null,
                "test_product",
                null,
                "1.0.0",
                null,
                "Test product",
                "Test product",
                Collections.emptyList()));
  }

  @Test
  void testReadModule() throws IOException {
    final IDefinitionKeeper definitionKeeper = this.readTypes();

    final Collection<ModuleDefinition> moduleDefs =
        definitionKeeper.getModuleDefinitions("test_module");
    assertThat(moduleDefs).hasSize(1);

    final ModuleDefinition testModuleDef = moduleDefs.stream().findAny().orElseThrow();
    assertThat(testModuleDef)
        .isEqualTo(
            new ModuleDefinition(
                null,
                null,
                "test_module",
                "test_product",
                "1",
                "2",
                "test_module",
                Collections.emptyList()));
  }

  @Test
  void testReadMagikFile() throws IOException {
    final IDefinitionKeeper definitionKeeper = this.readTypes();

    final Collection<MagikFileDefinition> magikFileDefs =
        definitionKeeper.getMagikFileDefinitions();
    assertThat(magikFileDefs).hasSize(1);

    final MagikFileDefinition magikFileDef = magikFileDefs.stream().findAny().orElseThrow();
    assertThat(magikFileDef)
        .isEqualTo(new MagikFileDefinition(new Location(URI.create("file:///test.magik")), null));
  }

  @Test
  void testReadPackage() throws IOException {
    final IDefinitionKeeper definitionKeeper = this.readTypes();

    final Collection<PackageDefinition> testPackageDefs =
        definitionKeeper.getPackageDefinitions("test_package");
    assertThat(testPackageDefs).hasSize(1);

    final PackageDefinition testPackageDef = testPackageDefs.stream().findAny().orElseThrow();
    assertThat(testPackageDef)
        .isEqualTo(
            new PackageDefinition(null, null, null, null, null, "test_package", List.of("user")));
  }

  @Test
  void testReadType() throws IOException {
    final IDefinitionKeeper definitionKeeper = this.readTypes();

    final TypeString aRef = TypeString.ofIdentifier("a", "user");
    final Collection<ExemplarDefinition> aDefs = definitionKeeper.getExemplarDefinitions(aRef);
    assertThat(aDefs).hasSize(1);
    final ExemplarDefinition aDef = aDefs.stream().findAny().orElseThrow();
    assertThat(aDef)
        .isEqualTo(
            new ExemplarDefinition(
                null,
                null,
                "test_module",
                "Test exemplar a",
                null,
                ExemplarDefinition.Sort.SLOTTED,
                aRef,
                List.of(
                    new SlotDefinition(
                        null, null, null, null, null, "slot1", TypeString.SW_INTEGER),
                    new SlotDefinition(null, null, null, null, null, "slot2", TypeString.SW_FLOAT)),
                List.of(TypeString.SW_OBJECT),
                Collections.emptySet()));
  }

  @Test
  void testReadMethod() throws IOException {
    final IDefinitionKeeper definitionKeeper = this.readTypes();

    final TypeString bRef = TypeString.ofIdentifier("user:b", "user");
    final Collection<MethodDefinition> mDefs = definitionKeeper.getMethodDefinitions(bRef);
    assertThat(mDefs).hasSize(2);

    final MethodDefinition m1Def =
        mDefs.stream().filter(def -> def.getMethodName().equals("m1()")).findAny().orElseThrow();
    assertThat(m1Def)
        .isEqualTo(
            new MethodDefinition(
                null,
                null,
                "test_module",
                "Test method m1()",
                null,
                bRef,
                "m1()",
                Collections.emptySet(),
                List.of(
                    new ParameterDefinition(
                        null,
                        null,
                        null,
                        null,
                        null,
                        "param1",
                        ParameterDefinition.Modifier.GATHER,
                        TypeString.SW_SYMBOL),
                    new ParameterDefinition(
                        null,
                        null,
                        null,
                        null,
                        null,
                        "param2",
                        ParameterDefinition.Modifier.NONE,
                        TypeString.UNDEFINED)),
                null,
                Collections.emptySet(),
                new ExpressionResultString(TypeString.SW_SYMBOL),
                ExpressionResultString.EMPTY));

    final MethodDefinition m2Def =
        mDefs.stream().filter(def -> def.getMethodName().equals("m2<<")).findAny().orElseThrow();
    assertThat(m2Def)
        .isEqualTo(
            new MethodDefinition(
                null,
                null,
                "test_module",
                "Test method m2()",
                null,
                bRef,
                "m2<<",
                Set.of(MethodDefinition.Modifier.PRIVATE),
                Collections.emptyList(),
                new ParameterDefinition(
                    null,
                    null,
                    null,
                    null,
                    null,
                    "param2",
                    ParameterDefinition.Modifier.NONE,
                    TypeString.SW_SYMBOL),
                Collections.emptySet(),
                new ExpressionResultString(TypeString.SW_SYMBOL),
                ExpressionResultString.EMPTY));
  }

  @Test
  void testReadCondition() throws IOException {
    final IDefinitionKeeper definitionKeeper = this.readTypes();

    final Collection<ConditionDefinition> conditionsError =
        definitionKeeper.getConditionDefinitions("error");
    assertThat(conditionsError).hasSize(1);

    final ConditionDefinition conditionError = conditionsError.stream().findAny().orElseThrow();
    assertThat(conditionError)
        .isEqualTo(
            new ConditionDefinition(
                null, null, null, null, null, "error", null, List.of("string")));

    final Collection<ConditionDefinition> conditionsUnknownValue =
        definitionKeeper.getConditionDefinitions("unknown_value");
    assertThat(conditionsUnknownValue).hasSize(1);

    final ConditionDefinition conditionUnknownValue =
        conditionsUnknownValue.stream().findAny().orElseThrow();
    assertThat(conditionUnknownValue)
        .isEqualTo(
            new ConditionDefinition(
                null,
                null,
                null,
                "Unknown value",
                null,
                "unknown_value",
                "error",
                List.of("value", "permitted_values")));
  }

  @Test
  void testReadGlobal() throws IOException {
    final IDefinitionKeeper definitionKeeper = this.readTypes();

    final TypeString tabCharRef = TypeString.ofIdentifier("tab_char", "sw");
    final Collection<GlobalDefinition> tabCharGlobalDefs =
        definitionKeeper.getGlobalDefinitions(tabCharRef);
    assertThat(tabCharGlobalDefs).hasSize(1);

    final GlobalDefinition tabCharGlobalDef = tabCharGlobalDefs.stream().findAny().orElseThrow();
    assertThat(tabCharGlobalDef)
        .isEqualTo(
            new GlobalDefinition(
                null, null, null, null, null, tabCharRef, TypeString.SW_CHARACTER));

    final TypeString printFloatPrecisionRef =
        TypeString.ofIdentifier("!print_float_precision!", "sw");
    final Collection<GlobalDefinition> printFloatPrecisionDefs =
        definitionKeeper.getGlobalDefinitions(printFloatPrecisionRef);
    assertThat(printFloatPrecisionDefs).hasSize(1);

    final GlobalDefinition printFloatPrecisionDef =
        printFloatPrecisionDefs.stream().findAny().orElseThrow();
    assertThat(printFloatPrecisionDef)
        .isEqualTo(
            new GlobalDefinition(
                null, null, null, null, null, printFloatPrecisionRef, TypeString.SW_INTEGER));
  }

  @Test
  void testReadBinaryOperator() throws IOException {
    final IDefinitionKeeper definitionKeeper = this.readTypes();

    final Collection<BinaryOperatorDefinition> binOps =
        definitionKeeper.getBinaryOperatorDefinitions(
            "=", TypeString.SW_CHAR16_VECTOR, TypeString.SW_SYMBOL);
    assertThat(binOps).hasSize(1);

    final BinaryOperatorDefinition binOp = binOps.stream().findAny().orElseThrow();
    assertThat(binOp)
        .isEqualTo(
            new BinaryOperatorDefinition(
                null,
                null,
                "test_module",
                null,
                null,
                "=",
                TypeString.SW_CHAR16_VECTOR,
                TypeString.SW_SYMBOL,
                TypeString.SW_CHAR16_VECTOR));
  }
}
