package nl.ramsolutions.sw.magik.analysis.definitions.io;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import nl.ramsolutions.sw.magik.analysis.definitions.BinaryOperatorDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ConditionDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.GlobalDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.PackageDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ParameterDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.SlotDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for JsonDefinitionReader.
 */
class JsonDefinitionReaderTest {

    private IDefinitionKeeper readTypes() throws IOException {
        final Path path = Path.of("src/test/resources/tests/type_database.jsonl");
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        JsonDefinitionReader.readTypes(path, definitionKeeper);
        return definitionKeeper;
    }

    @Test
    void testReadPackage() throws IOException {
        final IDefinitionKeeper definitionKeeper = this.readTypes();

        final Collection<PackageDefinition> testPackageDefs = definitionKeeper.getPackageDefinitions("test_package");
        assertThat(testPackageDefs).hasSize(1);

        final PackageDefinition testPackageDef = testPackageDefs.stream().findAny().orElseThrow();
        assertThat(testPackageDef.getName()).isEqualTo("test_package");
        assertThat(testPackageDef.getUses()).isEqualTo(List.of("user"));
    }

    @Test
    void testReadType() throws IOException {
        final IDefinitionKeeper definitionKeeper = this.readTypes();

        final TypeString aRef = TypeString.ofIdentifier("a", "user");
        final Collection<ExemplarDefinition> aDefs = definitionKeeper.getExemplarDefinitions(aRef);
        assertThat(aDefs).hasSize(1);
        final ExemplarDefinition aDef = aDefs.stream().findAny().orElseThrow();

        assertThat(aDef.getTypeString()).isEqualTo(aRef);
        assertThat(aDef.getSort()).isEqualTo(ExemplarDefinition.Sort.SLOTTED);
        assertThat(aDef.getDoc()).isEqualTo("Test exemplar a");
        assertThat(aDef.getModuleName()).isEqualTo("test_module");

        final SlotDefinition slot1Def = aDef.getSlot("slot1");
        assertThat(slot1Def.getName()).isEqualTo("slot1");
        assertThat(slot1Def.getTypeName()).isEqualTo(TypeString.SW_INTEGER);

        final SlotDefinition slot2Def = aDef.getSlot("slot2");
        assertThat(slot2Def.getName()).isEqualTo("slot2");
        assertThat(slot2Def.getTypeName()).isEqualTo(TypeString.SW_FLOAT);
    }

    @Test
    void testReadMethod() throws IOException {
        final IDefinitionKeeper definitionKeeper = this.readTypes();

        final TypeString bRef = TypeString.ofIdentifier("user:b", "user");
        final Collection<MethodDefinition> mDefs = definitionKeeper.getMethodDefinitions(bRef);
        assertThat(mDefs).hasSize(2);

        final MethodDefinition m1Def = mDefs.stream()
            .filter(def -> def.getMethodName().equals("m1()"))
            .findAny()
            .orElseThrow();
        assertThat(m1Def.getModuleName()).isEqualTo("test_module");
        assertThat(m1Def.getMethodName()).isEqualTo("m1()");
        assertThat(m1Def.getModifiers()).isEmpty();
        assertThat(m1Def.getDoc()).isEqualTo("Test method m1()");

        final List<ParameterDefinition> m1Parameters = m1Def.getParameters();
        final ParameterDefinition m1Parameter0 = m1Parameters.get(0);
        assertThat(m1Parameter0.getName()).isEqualTo("param1");
        assertThat(m1Parameter0.getModifier()).isEqualTo(ParameterDefinition.Modifier.GATHER);
        assertThat(m1Parameter0.getTypeName()).isEqualTo(TypeString.SW_SYMBOL);
        assertThat(m1Def.getAssignmentParameter()).isNull();

        final ExpressionResultString m1ReturnTypes = m1Def.getReturnTypes();
        assertThat(m1ReturnTypes).isEqualTo(
            new ExpressionResultString(
                TypeString.SW_SYMBOL));
        final ExpressionResultString m1LoopTypes = m1Def.getLoopTypes();
        assertThat(m1LoopTypes).isEqualTo(
            ExpressionResultString.EMPTY);

        final MethodDefinition m2Def = mDefs.stream()
            .filter(def -> def.getMethodName().equals("m2<<"))
            .findAny()
            .orElseThrow();
        assertThat(m2Def.getModuleName()).isEqualTo("test_module");
        assertThat(m2Def.getMethodName()).isEqualTo("m2<<");
        assertThat(m2Def.getDoc()).isEqualTo("Test method m2()");

        final List<ParameterDefinition> m2Parameters = m2Def.getParameters();
        assertThat(m2Parameters).isEmpty();

        final ParameterDefinition m2AssignmentParameter = m2Def.getAssignmentParameter();
        assertThat(m2AssignmentParameter.getName()).isEqualTo("param2");
        assertThat(m2AssignmentParameter.getModifier()).isEqualTo(ParameterDefinition.Modifier.NONE);
        assertThat(m2AssignmentParameter.getTypeName()).isEqualTo(TypeString.SW_SYMBOL);

        final ExpressionResultString m2ReturnTypes = m2Def.getReturnTypes();
        assertThat(m2ReturnTypes).isEqualTo(
            new ExpressionResultString(TypeString.SW_SYMBOL));
        final ExpressionResultString m2LoopTypes = m2Def.getLoopTypes();
        assertThat(m2LoopTypes).isEqualTo(
            ExpressionResultString.EMPTY);
    }

    @Test
    void testReadCondition() throws IOException {
        final IDefinitionKeeper definitionKeeper = this.readTypes();

        final Collection<ConditionDefinition> conditionsError = definitionKeeper.getConditionDefinitions("error");
        assertThat(conditionsError).hasSize(1);

        final ConditionDefinition conditionError = conditionsError.stream().findAny().orElseThrow();
        assertThat(conditionError.getName()).isEqualTo("error");
        assertThat(conditionError.getDataNames()).isEqualTo(List.of("string"));
        assertThat(conditionError.getDoc()).isNull();
        assertThat(conditionError.getParent()).isNull();

        final Collection<ConditionDefinition> conditionsUnknownValue =
            definitionKeeper.getConditionDefinitions("unknown_value");
        assertThat(conditionsUnknownValue).hasSize(1);

        final ConditionDefinition conditionUnknownValue = conditionsUnknownValue.stream().findAny().orElseThrow();
        assertThat(conditionUnknownValue.getName()).isEqualTo("unknown_value");
        assertThat(conditionUnknownValue.getDataNames()).isEqualTo(List.of("value", "permitted_values"));
        assertThat(conditionUnknownValue.getDoc()).isEqualTo("Unknown value");
        assertThat(conditionUnknownValue.getParent()).isEqualTo("error");
    }

    // @Test
    // void testReadProcedure() throws IOException {
    //     final IDefinitionKeeper definitionKeeper = this.readTypes();

    //     final TypeString quitRef = TypeString.ofIdentifier("quit", "sw");
    //     final AbstractType quitType = definitionKeeper.getType(quitRef);
    //     assertThat(quitType).isExactlyInstanceOf(AliasType.class);
    //     final ProcedureInstance quitInstance = (ProcedureInstance) ((AliasType) quitType).getAliasedType();
    //     assertThat(quitInstance).isExactlyInstanceOf(ProcedureInstance.class);
    //     assertThat(quitInstance.getName()).isEqualTo("quit");
    //     assertThat(quitInstance.getDoc()).isEqualTo("Quit!");
    //     assertThat(quitInstance.getModuleName()).isEqualTo("test_module");
    //     final Method quitMethod = quitInstance.getLocalMethods("invoke()").stream()
    //         .findAny()
    //         .orElseThrow();
    //     assertThat(quitMethod.getCallResult()).isEqualTo(ExpressionResultString.UNDEFINED);
    //     assertThat(quitMethod.getLoopbodyResult()).isEqualTo(ExpressionResultString.EMPTY);
    //     assertThat(quitMethod.getModifiers()).isEqualTo(Collections.emptySet());

    //     final TypeString rangeRef = TypeString.ofIdentifier("range", "sw");
    //     final AbstractType rangeType = definitionKeeper.getType(rangeRef);
    //     assertThat(rangeType).isExactlyInstanceOf(AliasType.class);
    //     final ProcedureInstance rangeInstance = (ProcedureInstance) ((AliasType) rangeType).getAliasedType();
    //     assertThat(rangeInstance).isExactlyInstanceOf(ProcedureInstance.class);
    //     assertThat(rangeInstance.getName()).isEqualTo("range");
    //     assertThat(rangeInstance.getDoc()).isEqualTo("Range iterator.");
    //     assertThat(rangeInstance.getModuleName()).isNull();

    //     final Method rangeMethod = rangeInstance.getLocalMethods("invoke()").stream()
    //         .findAny()
    //         .orElseThrow();
    //     assertThat(rangeMethod.getModifiers()).isEqualTo(Set.of(Method.Modifier.ITER));
    //     assertThat(rangeMethod.getCallResult()).isEqualTo(ExpressionResultString.EMPTY);
    //     assertThat(rangeMethod.getLoopbodyResult()).isEqualTo(new ExpressionResultString(TypeString.SW_INTEGER));
    // }

    @Test
    void testReadGlobal() throws IOException {
        final IDefinitionKeeper definitionKeeper = this.readTypes();

        final TypeString tabCharRef = TypeString.ofIdentifier("tab_char", "sw");
        final Collection<GlobalDefinition> tabCharGlobalDefs = definitionKeeper.getGlobalDefinitions(tabCharRef);
        assertThat(tabCharGlobalDefs).hasSize(1);

        final GlobalDefinition tabCharGlobalDef = tabCharGlobalDefs.stream().findAny().orElseThrow();
        assertThat(tabCharGlobalDef.getAliasedTypeName()).isEqualTo(TypeString.SW_CHARACTER);

        final TypeString printFloatPrecisionRef = TypeString.ofIdentifier("!print_float_precision!", "sw");
        final Collection<GlobalDefinition> printFloatPrecisionDefs =
            definitionKeeper.getGlobalDefinitions(printFloatPrecisionRef);
        assertThat(printFloatPrecisionDefs).hasSize(1);

        final GlobalDefinition printFloatPrecisionDef = printFloatPrecisionDefs.stream().findAny().orElseThrow();
        assertThat(printFloatPrecisionDef.getAliasedTypeName()).isEqualTo(TypeString.SW_INTEGER);
    }

    @Test
    void testReadBinaryOperator() throws IOException {
        final IDefinitionKeeper definitionKeeper = this.readTypes();

        final Collection<BinaryOperatorDefinition> binOps = definitionKeeper.getBinaryOperatorDefinitions(
            "=",
            TypeString.SW_CHAR16_VECTOR,
            TypeString.SW_SYMBOL);
        assertThat(binOps).hasSize(1);

        final BinaryOperatorDefinition binOp = binOps.stream().findAny().orElseThrow();
        assertThat(binOp.getModuleName()).isEqualTo("test_module");
        assertThat(binOp.getLhsTypeName()).isEqualTo(TypeString.SW_CHAR16_VECTOR);
        assertThat(binOp.getRhsTypeName()).isEqualTo(TypeString.SW_SYMBOL);
        assertThat(binOp.getResultTypeName()).isEqualTo(TypeString.SW_CHAR16_VECTOR);
    }

}
