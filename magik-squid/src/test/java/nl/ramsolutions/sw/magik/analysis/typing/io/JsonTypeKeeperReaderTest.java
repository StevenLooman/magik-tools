package nl.ramsolutions.sw.magik.analysis.typing.io;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import nl.ramsolutions.sw.magik.analysis.typing.BinaryOperator;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.AliasType;
import nl.ramsolutions.sw.magik.analysis.typing.types.Condition;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.types.GenericDeclaration;
import nl.ramsolutions.sw.magik.analysis.typing.types.MagikType;
import nl.ramsolutions.sw.magik.analysis.typing.types.Method;
import nl.ramsolutions.sw.magik.analysis.typing.types.Package;
import nl.ramsolutions.sw.magik.analysis.typing.types.Parameter;
import nl.ramsolutions.sw.magik.analysis.typing.types.ProcedureInstance;
import nl.ramsolutions.sw.magik.analysis.typing.types.Slot;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for JsonTypeKeeperReader.
 */
class JsonTypeKeeperReaderTest {

    private ITypeKeeper readTypes() throws IOException {
        final Path path = Path.of("src/test/resources/tests/type_database.jsonl");
        final ITypeKeeper typeKeeper = new TypeKeeper();
        JsonTypeKeeperReader.readTypes(path, typeKeeper);
        return typeKeeper;
    }

    @Test
    void testReadPackage() throws IOException {
        final ITypeKeeper typeKeeper = this.readTypes();

        final Package testPackage = typeKeeper.getPackage("test_package");
        assertThat(testPackage.getName()).isEqualTo("test_package");
        assertThat(testPackage.getUses()).isEqualTo(Set.of("user"));
    }

    @Test
    void testReadType() throws IOException {
        final ITypeKeeper typeKeeper = this.readTypes();

        final TypeString aRef = TypeString.ofIdentifier("a", "user");
        final AbstractType aType = typeKeeper.getType(aRef);
        assertThat(aType)
            .isNotNull()
            .isExactlyInstanceOf(MagikType.class);

        final MagikType aMagikType = (MagikType) aType;
        assertThat(aMagikType.getTypeString()).isEqualTo(aRef);
        assertThat(aMagikType.getSort()).isEqualTo(MagikType.Sort.SLOTTED);
        assertThat(aMagikType.getDoc()).isEqualTo("Test exemplar a");
        assertThat(aMagikType.getModuleName()).isEqualTo("test_module");

        final Slot slot1 = aMagikType.getSlot("slot1");
        final TypeString integerRef = TypeString.ofIdentifier("integer", "sw");
        final Slot slot1Expected = new Slot(null, "slot1", integerRef);
        assertThat(slot1).isEqualTo(slot1Expected);

        final Slot slot2 = aMagikType.getSlot("slot2");
        final TypeString floatRef = TypeString.ofIdentifier("float", "sw");
        final Slot slot2Expected = new Slot(null, "slot2", floatRef);
        assertThat(slot2).isEqualTo(slot2Expected);

        final List<GenericDeclaration> genericDeclaration = aMagikType.getGenerics();
        final GenericDeclaration kGenericDecl = genericDeclaration.get(0);
        final GenericDeclaration kGenericDeclExpected = new GenericDeclaration(null, "K");
        assertThat(kGenericDecl).isEqualTo(kGenericDeclExpected);

        final GenericDeclaration eGenericDecl = genericDeclaration.get(1);
        final GenericDeclaration eGenericDeclExpected = new GenericDeclaration(null, "E");
        assertThat(eGenericDecl).isEqualTo(eGenericDeclExpected);
    }

    @Test
    void testReadMethod() throws IOException {
        final ITypeKeeper typeKeeper = this.readTypes();

        final TypeString bRef = TypeString.ofIdentifier("user:b", "user");
        final AbstractType bType = typeKeeper.getType(bRef);
        final Method m1Method = bType.getLocalMethods("m1()").stream()
            .findAny()
            .orElseThrow();
        assertThat(m1Method).isNotNull();

        assertThat(m1Method.getModuleName()).isEqualTo("test_module");
        assertThat(m1Method.getName()).isEqualTo("m1()");
        assertThat(m1Method.getModifiers()).isEmpty();
        assertThat(m1Method.getDoc()).isEqualTo("Test method m1()");

        final List<Parameter> m1Parameters = m1Method.getParameters();
        final Parameter m1Parameter0 = m1Parameters.get(0);
        assertThat(m1Parameter0.getName()).isEqualTo("param1");
        assertThat(m1Parameter0.getModifier()).isEqualTo(Parameter.Modifier.GATHER);
        assertThat(m1Parameter0.getType()).isEqualTo(TypeString.ofIdentifier("symbol", "sw"));
        assertThat(m1Method.getAssignmentParameter()).isNull();

        final ExpressionResultString m1ReturnTypes = m1Method.getCallResult();
        assertThat(m1ReturnTypes).isEqualTo(
            new ExpressionResultString(
                TypeString.ofIdentifier("symbol", "sw")));
        final ExpressionResultString m1LoopTypes = m1Method.getLoopbodyResult();
        assertThat(m1LoopTypes).isEqualTo(
            new ExpressionResultString());

        final Method m2Method = bType.getLocalMethods("m2<<").stream()
            .findAny()
            .orElseThrow();
        assertThat(m2Method).isNotNull();

        assertThat(m2Method.getModuleName()).isEqualTo("test_module");
        assertThat(m2Method.getName()).isEqualTo("m2<<");
        assertThat(m2Method.getDoc()).isEqualTo("Test method m2()");

        final List<Parameter> m2Parameters = m2Method.getParameters();
        assertThat(m2Parameters).isEmpty();

        final Parameter m2AssignmentParameter = m2Method.getAssignmentParameter();
        assertThat(m2AssignmentParameter.getName()).isEqualTo("param2");
        assertThat(m2AssignmentParameter.getModifier()).isEqualTo(Parameter.Modifier.NONE);
        assertThat(m2AssignmentParameter.getType()).isEqualTo(TypeString.ofIdentifier("symbol", "sw"));

        final ExpressionResultString m2ReturnTypes = m2Method.getCallResult();
        assertThat(m2ReturnTypes).isEqualTo(
            new ExpressionResultString(
                TypeString.ofIdentifier("symbol", "sw")));
        final ExpressionResultString m2LoopTypes = m2Method.getLoopbodyResult();
        assertThat(m2LoopTypes).isEqualTo(
            new ExpressionResultString());
    }

    @Test
    void testReadCondition() throws IOException {
        final ITypeKeeper typeKeeper = this.readTypes();

        final Condition conditionError = typeKeeper.getCondition("error");
        assertThat(conditionError.getName()).isEqualTo("error");
        assertThat(conditionError.getDataNameList()).isEqualTo(List.of("string"));
        assertThat(conditionError.getDoc()).isNull();
        assertThat(conditionError.getParent()).isNull();

        final Condition conditionUnknownValue = typeKeeper.getCondition("unknown_value");
        assertThat(conditionUnknownValue.getName()).isEqualTo("unknown_value");
        assertThat(conditionUnknownValue.getDataNameList()).isEqualTo(List.of("value", "permitted_values"));
        assertThat(conditionUnknownValue.getDoc()).isEqualTo("Unknown value");
        assertThat(conditionUnknownValue.getParent()).isEqualTo("error");
    }

    @Test
    void testReadProcedure() throws IOException {
        final ITypeKeeper typeKeeper = this.readTypes();

        final TypeString quitRef = TypeString.ofIdentifier("quit", "sw");
        final AbstractType quitType = typeKeeper.getType(quitRef);
        assertThat(quitType).isExactlyInstanceOf(AliasType.class);
        final ProcedureInstance quitInstance = (ProcedureInstance) ((AliasType) quitType).getAliasedType();
        assertThat(quitInstance).isExactlyInstanceOf(ProcedureInstance.class);
        assertThat(quitInstance.getName()).isEqualTo("quit");
        assertThat(quitInstance.getDoc()).isEqualTo("Quit!");
        assertThat(quitInstance.getModuleName()).isEqualTo("test_module");
        final Method quitMethod = quitInstance.getLocalMethods("invoke()").stream()
            .findAny()
            .orElseThrow();
        assertThat(quitMethod.getCallResult()).isEqualTo(ExpressionResultString.UNDEFINED);
        assertThat(quitMethod.getLoopbodyResult()).isEqualTo(new ExpressionResultString());
        assertThat(quitMethod.getModifiers()).isEqualTo(Collections.emptySet());

        final TypeString rangeRef = TypeString.ofIdentifier("range", "sw");
        final AbstractType rangeType = typeKeeper.getType(rangeRef);
        assertThat(rangeType).isExactlyInstanceOf(AliasType.class);
        final ProcedureInstance rangeInstance = (ProcedureInstance) ((AliasType) rangeType).getAliasedType();
        assertThat(rangeInstance).isExactlyInstanceOf(ProcedureInstance.class);
        assertThat(rangeInstance.getName()).isEqualTo("range");
        assertThat(rangeInstance.getDoc()).isEqualTo("Range iterator.");
        assertThat(rangeInstance.getModuleName()).isNull();

        final Method rangeMethod = rangeInstance.getLocalMethods("invoke()").stream()
            .findAny()
            .orElseThrow();
        final TypeString integerRef = TypeString.ofIdentifier("integer", "sw");
        assertThat(rangeMethod.getModifiers()).isEqualTo(Set.of(Method.Modifier.ITER));
        assertThat(rangeMethod.getCallResult()).isEqualTo(new ExpressionResultString());
        assertThat(rangeMethod.getLoopbodyResult()).isEqualTo(new ExpressionResultString(integerRef));
    }

    @Test
    void testReadGlobal() throws IOException {
        final ITypeKeeper typeKeeper = this.readTypes();

        final TypeString tabCharRef = TypeString.ofIdentifier("tab_char", "sw");
        final AliasType tabCharType = (AliasType) typeKeeper.getType(tabCharRef);
        assertThat(tabCharType).isExactlyInstanceOf(AliasType.class);
        final TypeString characterRef = TypeString.ofIdentifier("character", "sw");
        assertThat(tabCharType.getAliasedType().getTypeString()).isEqualTo(characterRef);

        final TypeString printFloatPrecisionRef = TypeString.ofIdentifier("!print_float_precision!", "sw");
        final AliasType printFloatPrecisionType = (AliasType) typeKeeper.getType(printFloatPrecisionRef);
        assertThat(printFloatPrecisionType).isExactlyInstanceOf(AliasType.class);
        final TypeString integerRef = TypeString.ofIdentifier("integer", "sw");
        assertThat(printFloatPrecisionType.getAliasedType().getTypeString()).isEqualTo(integerRef);
    }

    @Test
    void testReadBinaryOperator() throws IOException {
        final ITypeKeeper typeKeeper = this.readTypes();

        final TypeString char16VectorRef = TypeString.ofIdentifier("char16_vector", "sw");
        final TypeString symbolRef = TypeString.ofIdentifier("symbol", "sw");
        final BinaryOperator binOp1 = typeKeeper.getBinaryOperator(
            BinaryOperator.Operator.EQ,
            char16VectorRef,
            symbolRef);
        assertThat(binOp1.getModuleName()).isEqualTo("test_module");
        assertThat(binOp1.getLeftType()).isEqualTo(char16VectorRef);
        assertThat(binOp1.getRightType()).isEqualTo(symbolRef);
        assertThat(binOp1.getResultType()).isEqualTo(char16VectorRef);
    }

}
