package nl.ramsolutions.sw.magik.analysis.typing.reasoner;

import com.sonar.sslr.api.AstNode;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.typing.BinaryOperator;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.CombinedType;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResult;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.types.MagikType;
import nl.ramsolutions.sw.magik.analysis.typing.types.MagikType.Sort;
import nl.ramsolutions.sw.magik.analysis.typing.types.Method;
import nl.ramsolutions.sw.magik.analysis.typing.types.Parameter;
import nl.ramsolutions.sw.magik.analysis.typing.types.ProcedureInstance;
import nl.ramsolutions.sw.magik.analysis.typing.types.SelfType;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.types.UndefinedType;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.parser.TypeStringParser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test LocalTypeReasoner.
 */
@SuppressWarnings("checkstyle:MagicNumber")
class LocalTypeReasonerTest {

    private MagikTypedFile createMagikFile(String code, ITypeKeeper typeKeeper) {
        final URI uri = URI.create("tests://unittest");
        return new MagikTypedFile(uri, code, typeKeeper);
    }

    @Test
    void testReasonMethodReturnNone() {
        final String code = ""
            + "_package sw\n"
            + "_method object.test\n"
            + "    _return\n"
            + "_endmethod\n";

        // Set up TypeKeeper/TypeReasoner.
        final TypeKeeper typeKeeper = new TypeKeeper();

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, typeKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasonerState.getNodeType(methodNode);
        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    void testReasonMethodReturnRope() {
        final String code = ""
            + "_package sw\n"
            + "_method object.test\n"
            + "    _return rope.new()\n"
            + "_endmethod\n";

        // Set up TypeKeeper/TypeReasoner.
        final TypeKeeper typeKeeper = new TypeKeeper();
        final TypeString ropeRef = TypeString.ofIdentifier("rope", "sw");
        final MagikType ropeType = new MagikType(typeKeeper, Sort.SLOTTED, ropeRef);
        ropeType.addMethod(
            null,
            EnumSet.noneOf(Method.Modifier.class),
            "new()",
            Collections.emptyList(),
            null,
            null,
            new ExpressionResultString(TypeString.SELF),
            new ExpressionResultString());

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, typeKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasonerState.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final AbstractType resultType = result.get(0, null);
        assertThat(resultType)
            .isNotNull()
            .isEqualTo(ropeType);
    }

    @Test
    void testReasonMethodMultiReturnSame() {
        final String code = ""
            + "_package sw\n"
            + "_method object.test\n"
            + "    _return 1\n"
            + "    _return 2\n"
            + "_endmethod\n";

        // Set up TypeKeeper/TypeReasoner.
        final TypeKeeper typeKeeper = new TypeKeeper();

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, typeKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasonerState.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final AbstractType integerType = typeKeeper.getType(TypeString.ofIdentifier("integer", "sw"));
        final AbstractType resultType = (AbstractType) result.get(0, null);
        assertThat(resultType)
            .isNotNull()
            .isEqualTo(integerType);
    }

    @Test
    void testReasonMethodMultiReturnDifferent() {
        final String code = ""
            + "_package sw\n"
            + "_method object.test\n"
            + "    _block\n"
            + "        _return 1\n"
            + "    _endblock\n"
            + "    _return :a\n"
            + "_endmethod\n";

        // Set up TypeKeeper/TypeReasoner.
        final TypeKeeper typeKeeper = new TypeKeeper();

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, typeKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasonerState.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final AbstractType combinedType = new CombinedType(
            typeKeeper.getType(TypeString.ofIdentifier("integer", "sw")),
            typeKeeper.getType(TypeString.ofIdentifier("symbol", "sw")));
        final AbstractType resultType = (AbstractType) result.get(0, null);
        assertThat(resultType)
            .isNotNull()
            .isEqualTo(combinedType);
    }

    @Test
    void testReasonMethodSelf() {
        final String code = ""
            + "_method object.test\n"
            + "    _return _self\n"
            + "_endmethod\n";

        // Set up TypeKeeper/TypeReasoner.
        final TypeKeeper typeKeeper = new TypeKeeper();

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, typeKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasonerState.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final SelfType resultType = (SelfType) result.get(0, null);
        assertThat(resultType)
            .isNotNull()
            .isEqualTo(SelfType.INSTANCE);
    }

    @Test
    void testReasonVariable() {
        final String code = ""
            + "_method object.test\n"
            + "    _local var << rope.new(1)\n"
            + "    var.add(:a)\n"
            + "    _return var\n"
            + "_endmethod\n";

        // Set up TypeKeeper/TypeReasoner.
        final TypeKeeper typeKeeper = new TypeKeeper();
        final TypeString ropeRef = TypeString.ofIdentifier("rope", "sw");
        final MagikType ropeType = new MagikType(typeKeeper, Sort.SLOTTED, ropeRef);
        ropeType.addMethod(
            null,
            EnumSet.noneOf(Method.Modifier.class),
            "new()",
            Collections.emptyList(),
            null,
            null,
            new ExpressionResultString(TypeString.SELF),
            new ExpressionResultString());
        ropeType.addMethod(
            null,
            EnumSet.noneOf(Method.Modifier.class),
            "add()",
            Collections.emptyList(),
            null,
            null,
            new ExpressionResultString(),
            new ExpressionResultString());

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, typeKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasonerState.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final AbstractType resultType = result.get(0, null);
        assertThat(resultType)
            .isNotNull()
            .isEqualTo(ropeType);
    }

    @Test
    void testReasonVariable2() {
        final String code = ""
            + "_method object.test\n"
            + "    _local (var1, var2) << rope.new(1)\n"
            + "    var1.add(:a)\n"
            + "    _return var1\n"
            + "_endmethod\n";

        // Set up TypeKeeper/TypeReasoner.
        final TypeKeeper typeKeeper = new TypeKeeper();
        final TypeString ropeRef = TypeString.ofIdentifier("rope", "sw");
        final MagikType ropeType = new MagikType(typeKeeper, Sort.SLOTTED, ropeRef);
        ropeType.addMethod(
            null,
            EnumSet.noneOf(Method.Modifier.class),
            "new()",
            Collections.emptyList(),
            null,
            null,
            new ExpressionResultString(TypeString.SELF),
            new ExpressionResultString());
        ropeType.addMethod(
            null,
            EnumSet.noneOf(Method.Modifier.class),
            "add()",
            Collections.emptyList(),
            null,
            null,
            new ExpressionResultString(),
            new ExpressionResultString());

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, typeKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasonerState.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final AbstractType resultType = result.get(0, null);
        assertThat(resultType)
            .isNotNull()
            .isEqualTo(ropeType);
    }

    @Test
    void testUnaryOperator() {
        final String code = ""
            + "_method object.test\n"
            + "    _return _not _true\n"
            + "_endmethod\n";

        // Set up TypeKeeper/TypeReasoner.
        final TypeKeeper typeKeeper = new TypeKeeper();
        final TypeString falseRef = TypeString.ofIdentifier("false", "sw");
        final MagikType falseType = (MagikType) typeKeeper.getType(falseRef);
        falseType.addMethod(
            null,
            EnumSet.noneOf(Method.Modifier.class),
            "not",
            Collections.emptyList(),
            null,
            null,
            new ExpressionResultString(TypeString.SELF),
            new ExpressionResultString());

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, typeKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasonerState.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final AbstractType resultType = result.get(0, null);
        assertThat(resultType)
            .isNotNull()
            .isEqualTo(falseType);
    }

    @Test
    void testBinaryOperator() {
        final String code = ""
            + "_method object.test\n"
            + "    _return 'a' + 'b'\n"
            + "_endmethod\n";

        // Set up TypeKeeper/TypeReasoner.
        final TypeKeeper typeKeeper = new TypeKeeper();
        final TypeString char16VectorRef = TypeString.ofIdentifier("char16_vector", "sw");
        final BinaryOperator binOp = new BinaryOperator(
            BinaryOperator.Operator.valueFor("+"), char16VectorRef, char16VectorRef, char16VectorRef);
        typeKeeper.addBinaryOperator(binOp);

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, typeKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasonerState.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final AbstractType char16VectorType = typeKeeper.getType(char16VectorRef);
        final AbstractType resultType = result.get(0, null);
        assertThat(resultType)
            .isNotNull()
            .isEqualTo(char16VectorType);
    }

    @Test
    void testBinaryOperatorChained() {
        final String code = ""
            + "_method object.test\n"
            + "    _return 1 + :a + 'a'\n"
            + "_endmethod\n";

        // Set up TypeKeeper/TypeReasoner.
        final TypeKeeper typeKeeper = new TypeKeeper();
        final TypeString char16VectorRef = TypeString.ofIdentifier("char16_vector", "sw");
        final TypeString integerRef = TypeString.ofIdentifier("integer", "sw");
        final TypeString symbolRef = TypeString.ofIdentifier("symbol", "sw");
        final BinaryOperator binOp1 =
            new BinaryOperator(BinaryOperator.Operator.valueFor("+"), integerRef, symbolRef, symbolRef);
        typeKeeper.addBinaryOperator(binOp1);
        final BinaryOperator binOp2 =
            new BinaryOperator(BinaryOperator.Operator.valueFor("+"), symbolRef, char16VectorRef, char16VectorRef);
        typeKeeper.addBinaryOperator(binOp2);

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, typeKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasonerState.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final AbstractType char16VectorType = typeKeeper.getType(char16VectorRef);
        final AbstractType resultType = result.get(0, null);
        assertThat(resultType)
            .isNotNull()
            .isEqualTo(char16VectorType);
    }

    @Test
    void testEmitStatement() {
        final String code = ""
            + "_method object.test\n"
            + "    _return _block\n"
            + "        >> 1, 2, 3\n"
            + "    _endblock\n"
            + "_endmethod\n";

        // Set up TypeKeeper/TypeReasoner.
        final TypeKeeper typeKeeper = new TypeKeeper();

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, typeKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasonerState.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(3);

        final TypeString integerRef = TypeString.ofIdentifier("integer", "sw");
        final AbstractType integerType = typeKeeper.getType(integerRef);
        final AbstractType resultType1 = result.get(0, null);
        assertThat(resultType1)
            .isNotNull()
            .isEqualTo(integerType);

        final AbstractType resultType2 = result.get(1, null);
        assertThat(resultType2)
            .isNotNull()
            .isEqualTo(integerType);

        final AbstractType resultType3 = result.get(2, null);
        assertThat(resultType3)
            .isNotNull()
            .isEqualTo(integerType);
    }

    @Test
    void testIfElseStatement() {
        final String code = ""
            + "_method object.test\n"
            + "    a << _true\n"
            + "    _return _if a\n"
            + "            _then\n"
            + "                >> 1\n"
            + "            _else\n"
            + "                >> 2\n"
            + "            _endif\n"
            + "_endmethod\n";

        // Set up TypeKeeper/TypeReasoner.
        final TypeKeeper typeKeeper = new TypeKeeper();

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, typeKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasonerState.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final TypeString integerRef = TypeString.ofIdentifier("integer", "sw");
        final AbstractType integerType = typeKeeper.getType(integerRef);
        final AbstractType resultType = result.get(0, null);
        assertThat(resultType)
            .isNotNull()
            .isEqualTo(integerType);
    }

    @Test
    void testLoopStatement() {
        final String code = ""
            + "_method object.test\n"
            + "    _return _for i _over 1.upto(10)\n"
            + "            _loop\n"
            + "                _leave _with 1\n"
            + "            _endloop\n"
            + "_endmethod\n";

        // Set up TypeKeeper/TypeReasoner.
        final TypeKeeper typeKeeper = new TypeKeeper();
        final TypeString integerRef = TypeString.ofIdentifier("integer", "sw");
        final MagikType integerType = (MagikType) typeKeeper.getType(integerRef);
        integerType.addMethod(
            null,
            EnumSet.noneOf(Method.Modifier.class),
            "upto()",
            Collections.emptyList(),
            null,
            null,
            new ExpressionResultString(),
            new ExpressionResultString(integerRef));

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, typeKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasonerState.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final AbstractType resultType = result.get(0, null);
        assertThat(resultType)
            .isNotNull()
            .isEqualTo(integerType);
    }

    @Test
    void testLoopStatementUseIterator() {
        final String code = ""
            + "_method object.test\n"
            + "    _return _for i _over 1.upto(10)\n"
            + "            _loop\n"
            + "                _leave _with i\n"
            + "            _endloop\n"
            + "_endmethod\n";

        // Set up TypeKeeper/TypeReasoner.
        final TypeKeeper typeKeeper = new TypeKeeper();
        final TypeString integerRef = TypeString.ofIdentifier("integer", "sw");
        final MagikType integerType = (MagikType) typeKeeper.getType(integerRef);
        integerType.addMethod(
            null,
            EnumSet.noneOf(Method.Modifier.class),
            "upto()",
            Collections.emptyList(),
            null,
            null,
            new ExpressionResultString(),
            new ExpressionResultString(integerRef));

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, typeKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasonerState.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final AbstractType resultType = result.get(0, null);
        assertThat(resultType)
            .isNotNull()
            .isEqualTo(integerType);
    }

    @Test
    void testLoopResultOptional() {
        final String code = ""
            + "_method object.test\n"
            + "    _return _for i _over 1.upto(10)\n"
            + "            _loop\n"
            + "                _if a\n"
            + "                _then\n"
            + "                    _leave _with i\n"
            + "                _endif\n"
            + "            _endloop\n"
            + "_endmethod\n";

        // Set up TypeKeeper/TypeReasoner.
        final TypeKeeper typeKeeper = new TypeKeeper();
        final TypeString integerRef = TypeString.ofIdentifier("integer", "sw");
        final MagikType integerType = (MagikType) typeKeeper.getType(integerRef);
        integerType.addMethod(
            null,
            EnumSet.noneOf(Method.Modifier.class),
            "upto()",
            Collections.emptyList(),
            null,
            null,
            new ExpressionResultString(),
            new ExpressionResultString(integerRef));

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, typeKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasonerState.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final CombinedType expectedType = new CombinedType(
            typeKeeper.getType(TypeString.ofIdentifier("integer", "sw")),
            typeKeeper.getType(TypeString.SW_UNSET));
        final CombinedType resultType = (CombinedType) result.get(0, null);
        assertThat(resultType)
            .isNotNull()
            .isEqualTo(expectedType);
    }

    @Test
    void testUnknownMethodCall() {
        final String code = ""
            + "_method object.test\n"
            + "    _return object.unknown()\n"
            + "_endmethod\n";

        // Set up TypeKeeper/TypeReasoner.
        final TypeKeeper typeKeeper = new TypeKeeper();

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, typeKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        // Assert user:object.test type determined.resul
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasonerState.getNodeType(methodNode);
        assertThat(result).isEqualTo(ExpressionResult.UNDEFINED);

        final UndefinedType resultType = (UndefinedType) result.get(0, null);
        assertThat(resultType)
            .isNotNull()
            .isEqualTo(UndefinedType.INSTANCE);
    }

    @Test
    void testReasonMethodReturnProc() {
        final String code = ""
            + "_method object.test\n"
            + "    _return _proc@test()\n"
            + "                >> 10\n"
            + "            _endproc\n"
            + "_endmethod\n";

        // Do analysis.
        final TypeKeeper typeKeeper = new TypeKeeper();
        final MagikTypedFile magikFile = this.createMagikFile(code, typeKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasonerState.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final ProcedureInstance resultType = (ProcedureInstance) result.get(0, null);
        assertThat(resultType).isNotNull();
        assertThat(resultType.getProcedureName()).isEqualTo("test");

        final Collection<Method> invokeMethods = resultType.getMethods("invoke()");
        assertThat(invokeMethods).isNotEmpty();
        final TypeString integerRef = TypeString.ofIdentifier("sw:integer", "sw");
        invokeMethods.forEach(procMethod -> {
            final ExpressionResultString procResult = procMethod.getCallResult();
            assertThat(procResult.size()).isEqualTo(1);

            final TypeString procResultTypeString = procResult.get(0, null);
            assertThat(procResultTypeString)
                .isNotNull()
                .isEqualTo(integerRef);
        });
    }

    @Test
    void testMultipleAssignmentStatement() {
        final String code = ""
            + "_method object.test\n"
            + "    (a, b) << (:a, 3)\n"
            + "    _return b\n"
            + "_endmethod\n";

        // Set up TypeKeeper/TypeReasoner.
        final TypeKeeper typeKeeper = new TypeKeeper();

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, typeKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasonerState.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final AbstractType resultType = result.get(0, null);
        final AbstractType integerType = typeKeeper.getType(TypeString.ofIdentifier("integer", "sw"));
        assertThat(resultType)
            .isNotNull()
            .isEqualTo(integerType);
    }

    @Test
    void testAssignmentMethod() {
        final String code = ""
            + "_method object.test\n"
            + "    _local a << property_list.new()\n"
            + "    a[:1] << 10\n"
            + "    _return a\n"
            + "_endmethod\n";

        // Set up TypeKeeper/TypeReasoner.
        final TypeKeeper typeKeeper = new TypeKeeper();
        final MagikType propertyListType =
            new MagikType(typeKeeper, Sort.INDEXED, TypeString.ofIdentifier("property_list", "sw"));
        propertyListType.addMethod(
            null,
            EnumSet.noneOf(Method.Modifier.class),
            "new()",
            Collections.emptyList(),
            null,
            null,
            new ExpressionResultString(TypeString.SELF),
            new ExpressionResultString());

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, typeKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasonerState.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final AbstractType resultType = result.get(0, null);
        assertThat(resultType)
            .isNotNull()
            .isEqualTo(propertyListType);
    }

    // region: self
    @Test
    void testSelf() {
        final String code = ""
            + "_package sw\n"
            + "_method object.test\n"
            + "    _return _self.test1\n"
            + "_endmethod\n";

        // Set up TypeKeeper/TypeReasoner.
        final TypeKeeper typeKeeper = new TypeKeeper();
        final TypeString objectRef = TypeString.ofIdentifier("object", "sw");
        final MagikType objectType = (MagikType) typeKeeper.getType(objectRef);
        objectType.addMethod(
            null,
            EnumSet.noneOf(Method.Modifier.class),
            "test1",
            Collections.emptyList(),
            null,
            null,
            new ExpressionResultString(TypeString.SELF),
            new ExpressionResultString());

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, typeKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasonerState.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final SelfType resultType = (SelfType) result.get(0, null);
        assertThat(resultType)
            .isNotNull()
            .isEqualTo(SelfType.INSTANCE);
    }

    @Test
    void testSelfNewInit() {
        final String code = ""
            + "_method object.test\n"
            + "    _return rope.new()\n"
            + "_endmethod\n";

        // Set up TypeKeeper/TypeReasoner.
        final TypeKeeper typeKeeper = new TypeKeeper();
        final TypeString ropeRef = TypeString.ofIdentifier("rope", "sw");
        final MagikType ropeType = new MagikType(typeKeeper, Sort.SLOTTED, ropeRef);
        ropeType.addMethod(
            null,
            EnumSet.noneOf(Method.Modifier.class),
            "new()",
            Collections.emptyList(),
            null,
            null,
            new ExpressionResultString(TypeString.SELF),
            new ExpressionResultString());

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, typeKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasonerState.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final AbstractType resultType = result.get(0, null);
        assertThat(resultType)
            .isNotNull()
            .isEqualTo(ropeType);
    }
    // endregion

    @Test
    void testExpressionTypeAnnotation() throws IOException {
        final String code = ""
            + "_method object.test\n"
            + "    _return :a  # type: sw:integer\n"
            + "_endmethod\n";

        // Set up TypeKeeper/TypeReasoner.
        final TypeKeeper typeKeeper = new TypeKeeper();

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, typeKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasonerState.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final AbstractType integerType = typeKeeper.getType(TypeString.ofIdentifier("integer", "sw"));
        final AbstractType resultType = result.get(0, null);
        assertThat(resultType)
            .isNotNull()
            .isEqualTo(integerType);
    }

    @Test
    void testIterableExpressionIterTypeAnnotation() throws IOException {
        final String code = ""
            + "_method object.test\n"
            + "    _for a, b _over fn()  # iter-type: sw:integer, sw:float\n"
            + "    _loop\n"
            + "      _return a, b\n"
            + "    _endloop\n"
            + "_endmethod\n";

        // Do analysis.
        final TypeKeeper typeKeeper = new TypeKeeper();
        final MagikTypedFile magikFile = this.createMagikFile(code, typeKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode forVariablesNode = topNode.getFirstDescendant(MagikGrammar.FOR_VARIABLES);

        final AstNode aNode = forVariablesNode.getDescendants(MagikGrammar.IDENTIFIER).get(0);
        final ExpressionResult aResult = reasonerState.getNodeType(aNode);
        assertThat(aResult.size()).isEqualTo(1);
        final AbstractType aResultType = aResult.get(0, null);
        final TypeString integerRef = TypeString.ofIdentifier("integer", "sw");
        final AbstractType integerType = typeKeeper.getType(integerRef);
        assertThat(aResultType).isEqualTo(integerType);

        final AstNode bNode = forVariablesNode.getDescendants(MagikGrammar.IDENTIFIER).get(1);
        final ExpressionResult bResult = reasonerState.getNodeType(bNode);
        assertThat(bResult.size()).isEqualTo(1);
        final AbstractType bResultType = bResult.get(0, null);
        final TypeString floatRef = TypeString.ofIdentifier("float", "sw");
        final AbstractType floatType = typeKeeper.getType(floatRef);
        assertThat(bResultType).isEqualTo(floatType);
    }

    @Test
    void testEmptyLocalDefinition() {
        final String code = ""
            + "_method object.test\n"
            + "    _local a\n"
            + "    _return a\n"
            + "_endmethod\n";

        // Set up TypeKeeper/TypeReasoner.
        final TypeKeeper typeKeeper = new TypeKeeper();

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, typeKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasonerState.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final AbstractType resultType = result.get(0, null);
        assertThat(resultType)
            .isNotNull()
            .isEqualTo(TypeString.SW_UNSET);
    }

    @Test
    void testAugmentedAssignment() {
        final String code = ""
            + "_method object.test\n"
            + "    _local x << 1\n"
            + "    x +<< 1.0\n"
            + "    _return x\n"
            + "_endmethod\n";

        // Set up TypeKeeper/TypeReasoner.
        final TypeKeeper typeKeeper = new TypeKeeper();
        final TypeString integerRef = TypeString.ofIdentifier("integer", "sw");
        final TypeString floatRef = TypeString.ofIdentifier("float", "sw");
        final AbstractType floatType = typeKeeper.getType(floatRef);
        final BinaryOperator binaryOperator =
            new BinaryOperator(BinaryOperator.Operator.valueFor("+"), integerRef, floatRef, floatRef);
        typeKeeper.addBinaryOperator(binaryOperator);

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, typeKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasonerState.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final AbstractType resultType = result.get(0, null);
        assertThat(resultType)
            .isNotNull()
            .isEqualTo(floatType);
    }

    @Test
    void testGatherParameters() {
        final String code = ""
            + "_method object.test(_gather args)\n"
            + "_endmethod\n";

        // Set up TypeKeeper/TypeReasoner.
        final TypeKeeper typeKeeper = new TypeKeeper();

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, typeKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode parameterNode = topNode
            .getFirstDescendant(MagikGrammar.PARAMETER)
            .getFirstChild(MagikGrammar.IDENTIFIER);
        final ExpressionResult result = reasonerState.getNodeType(parameterNode);

        final TypeString simpleVectorRef = TypeString.ofIdentifier("simple_vector", "sw");
        final AbstractType simpleVectorType = typeKeeper.getType(simpleVectorRef);
        final AbstractType actualResultType = result.get(0, null);
        assertThat(actualResultType)
            .isNotNull()
            .isEqualTo(simpleVectorType);
    }

    @Test
    void testImport() {
        final String code = ""
            + "_method object.test\n"
            + "    _local x << 1\n"
            + "    _proc()\n"
            + "        _import x\n"
            + "    _endproc\n"
            + "_endmethod\n";

        // Set up TypeKeeper/TypeReasoner.
        final TypeKeeper typeKeeper = new TypeKeeper();

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, typeKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode procNode = topNode.getFirstDescendant(MagikGrammar.PROCEDURE_DEFINITION);
        final AstNode importNode = procNode.getFirstDescendant(MagikGrammar.VARIABLE_DEFINITION);
        final ExpressionResult result = reasonerState.getNodeType(importNode);

        final TypeString integerRef = TypeString.ofIdentifier("integer", "sw");
        final AbstractType integerType = typeKeeper.getType(integerRef);
        final AbstractType resultType = result.get(0, null);
        assertThat(resultType)
            .isNotNull()
            .isEqualTo(integerType);
    }

    @Test
    void testAssignmentChain() {
        final String code = ""
            + "_method object.test\n"
            + "    _local x << y << 10\n"
            + "    _return x, y\n"
            + "_endmethod\n";

        // Set up TypeKeeper/TypeReasoner.
        final TypeKeeper typeKeeper = new TypeKeeper();

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, typeKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasonerState.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(2);

        final AbstractType integerType = typeKeeper.getType(TypeString.ofIdentifier("integer", "sw"));
        final AbstractType resultType1 = result.get(0, null);
        assertThat(resultType1)
            .isNotNull()
            .isEqualTo(integerType);

        final AbstractType resultType2 = result.get(1, null);
        assertThat(resultType2)
            .isNotNull()
            .isEqualTo(integerType);
    }

    @Test
    void testTryWith() {
        final String code = ""
            + "_method object.test\n"
            + "    _try _with cond\n"
            + "    _when warning\n"
            + "    _when error\n"
            + "    _endtry\n"
            + "_endmethod\n";

        // Set up TypeKeeper/TypeReasoner.
        final TypeKeeper typeKeeper = new TypeKeeper();

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, typeKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        final AstNode topNode = magikFile.getTopNode();
        final AstNode tryVarNode = topNode.getFirstDescendant(MagikGrammar.TRY_VARIABLE);
        final ExpressionResult result = reasonerState.getNodeType(tryVarNode);
        assertThat(result).isNotNull();

        final AbstractType conditionType = typeKeeper.getType(TypeString.ofIdentifier("condition", "sw"));
        final AbstractType resultType = result.get(0, null);
        assertThat(resultType)
            .isNotNull()
            .isEqualTo(conditionType);
    }

    @Test
    void testMergeReturnTypes() {
        final String code = ""
            + "_method e1.m1\n"
            + "  _return 1\n"
            + "  _return x()\n"
            + "_endmethod";

        // Set up TypeKeeper/TypeReasoner.
        final TypeKeeper typeKeeper = new TypeKeeper();

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, typeKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodDefNode = topNode.getFirstDescendant(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasonerState.getNodeType(methodDefNode);
        assertThat(result).isNotNull();

        // First is integer + undefined.
        final TypeString integerRef = TypeString.ofIdentifier("integer", "sw");
        final AbstractType intAndUndefinedType = new CombinedType(
            typeKeeper.getType(integerRef),
            UndefinedType.INSTANCE);
        final AbstractType resultType0 = result.get(0, null);
        assertThat(resultType0)
                .isNotNull()
                .isEqualTo(intAndUndefinedType);

        // The rest is unset + undefined, as merging the two yields unset for the rest.
        final AbstractType unsetAndUndefinedType = new CombinedType(
            typeKeeper.getType(TypeString.SW_UNSET),
            UndefinedType.INSTANCE);
        result.getTypes().stream()
            .skip(1)
            .forEach(type -> {
                assertThat(type)
                    .isNotNull()
                    .isEqualTo(unsetAndUndefinedType);
            });
    }

    @Test
    void testSingleSuperType() {
        final String code = ""
            + "_method t.m\n"
            + "  _super.m\n"
            + "_endmethod";

        // Set up TypeKeeper/TypeReasoner.
        final TypeKeeper typeKeeper = new TypeKeeper();
        final TypeString sRef = TypeString.ofIdentifier("s", "sw");
        final MagikType sType = new MagikType(typeKeeper, Sort.SLOTTED, sRef);
        final TypeString tRef = TypeString.ofIdentifier("t", "sw");
        final MagikType tType = new MagikType(typeKeeper, Sort.SLOTTED, tRef);
        tType.addParent(sRef);

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, typeKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        final AstNode topNode = magikFile.getTopNode();
        final AstNode superNode = topNode.getFirstDescendant(MagikGrammar.SUPER).getParent();
        final ExpressionResult result = reasonerState.getNodeType(superNode);
        assertThat(result).isNotNull();

        final AbstractType resultType0 = result.get(0, null);
        assertThat(resultType0)
                .isNotNull()
                .isEqualTo(sType);
    }

    @Test
    void testSingleNamedSuperType() {
        final String code = ""
            + "_method t.m\n"
            + "  _super(r).m\n"
            + "_endmethod";

        // Set up TypeKeeper/TypeReasoner.
        final TypeKeeper typeKeeper = new TypeKeeper();
        final TypeString rRef = TypeString.ofIdentifier("r", "sw");
        final MagikType rType = new MagikType(typeKeeper, Sort.SLOTTED, rRef);
        final TypeString sRef = TypeString.ofIdentifier("s", "sw");
        new MagikType(typeKeeper, Sort.SLOTTED, sRef);
        final TypeString tRef = TypeString.ofIdentifier("t", "sw");
        final MagikType tType = new MagikType(typeKeeper, Sort.SLOTTED, tRef);
        tType.addParent(rRef);
        tType.addParent(sRef);

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, typeKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        final AstNode topNode = magikFile.getTopNode();
        final AstNode superNode = topNode.getFirstDescendant(MagikGrammar.SUPER).getParent();
        final ExpressionResult result = reasonerState.getNodeType(superNode);
        assertThat(result).isNotNull();

        final AbstractType resultType0 = result.get(0, null);
        assertThat(resultType0)
                .isNotNull()
                .isEqualTo(rType);
    }

    @Test
    void testParameterType() {
        final String code = ""
            + "_method a.b(p1)\n"
            + "  ## @param {sw:float} p1\n"
            + "  show(p1)"
            + "_endmethod";

        // Set up TypeKeeper/TypeReasoner.
        final TypeKeeper typeKeeper = new TypeKeeper();

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, typeKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        final TypeString floatRef = TypeString.ofIdentifier("float", "sw");
        final AbstractType floatType = typeKeeper.getType(floatRef);
        final AstNode topNode = magikFile.getTopNode();

        // Test parameter definition.
        final AstNode parameterNode = topNode
            .getFirstDescendant(MagikGrammar.PARAMETER)
            .getFirstChild(MagikGrammar.IDENTIFIER);
        final ExpressionResult actualParameterResult = reasonerState.getNodeType(parameterNode);
        final AbstractType actualParameterType = actualParameterResult.get(0, null);
        assertThat(actualParameterType)
            .isNotNull()
            .isEqualTo(floatType);

        // Test parameter usage.
        final AstNode procInvocationNode = topNode.getFirstDescendant(MagikGrammar.PROCEDURE_INVOCATION);
        final AstNode atomNode = procInvocationNode.getFirstDescendant(MagikGrammar.ATOM);
        final ExpressionResult actualAtomResult = reasonerState.getNodeType(atomNode);
        final AbstractType actualAtomType = actualAtomResult.get(0, null);
        assertThat(actualAtomType)
            .isNotNull()
            .isEqualTo(floatType);
    }

    @Test
    void testParameterTypeCombined() {
        final String code = ""
            + "_method a.b(p1)\n"
            + "  ## @param {sw:float|sw:integer} p1\n"
            + "_endmethod";

        // Set up TypeKeeper/TypeReasoner.
        final TypeKeeper typeKeeper = new TypeKeeper();

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, typeKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        final AstNode topNode = magikFile.getTopNode();
        final AstNode parameterNode = topNode
            .getFirstDescendant(MagikGrammar.PARAMETER)
            .getFirstChild(MagikGrammar.IDENTIFIER);
        final ExpressionResult result = reasonerState.getNodeType(parameterNode);
        final TypeString floatRef = TypeString.ofIdentifier("float", "sw");
        final AbstractType floatType = typeKeeper.getType(floatRef);
        final TypeString integerRef = TypeString.ofIdentifier("integer", "sw");
        final AbstractType integerType = typeKeeper.getType(integerRef);
        final AbstractType expectedType = CombinedType.combine(floatType, integerType);
        final AbstractType actualType = result.get(0, null);
        assertThat(actualType)
            .isNotNull()
            .isEqualTo(expectedType);
    }

    @Test
    void testParameterReferenceUsage() {
        final String code = ""
            + "_method a.b\n"
            + "  _return _self.returns_param(10)\n"
            + "_endmethod";

        // Set up TypeKeeper/TypeReasoner.
        final TypeKeeper typeKeeper = new TypeKeeper();
        final MagikType aType = new MagikType(typeKeeper, Sort.SLOTTED, TypeString.ofIdentifier("a", "user"));
        final TypeString param1Ref = TypeString.ofParameterRef("p1");
        aType.addMethod(
            null,
            EnumSet.noneOf(Method.Modifier.class),
            "returns_param()",
            List.of(
                new Parameter("p1", Parameter.Modifier.NONE)),
            null,
            null,
            new ExpressionResultString(param1Ref),
            new ExpressionResultString());

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, typeKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodDefinitionNode = topNode.getFirstDescendant(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasonerState.getNodeType(methodDefinitionNode);
        final AbstractType expectedType = typeKeeper.getType(TypeString.ofIdentifier("integer", "sw"));
        final AbstractType actualType = result.get(0, null);
        assertThat(actualType)
            .isNotNull()
            .isEqualTo(expectedType);
    }

    @Test
    void testParameterReferenceNested() {
        final String code = ""
            + "_method a.b\n"
            + "  _return _self.returns_param(_self.returns_param2(10))\n"
            + "_endmethod";

        // Set up TypeKeeper/TypeReasoner.
        final TypeKeeper typeKeeper = new TypeKeeper();
        final MagikType aType = new MagikType(typeKeeper, Sort.SLOTTED, TypeString.ofIdentifier("a", "user"));
        final TypeString param1Ref = TypeString.ofParameterRef("p1");
        aType.addMethod(
            null,
            EnumSet.noneOf(Method.Modifier.class),
            "returns_param()",
            List.of(
                new Parameter("p1", Parameter.Modifier.NONE)),
            null,
            null,
            new ExpressionResultString(param1Ref),
            new ExpressionResultString());
        aType.addMethod(
            null,
            EnumSet.noneOf(Method.Modifier.class),
            "returns_param2()",
            List.of(
                new Parameter("p1", Parameter.Modifier.NONE)),
            null,
            null,
            new ExpressionResultString(param1Ref),
            new ExpressionResultString());

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, typeKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodDefinitionNode = topNode.getFirstDescendant(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasonerState.getNodeType(methodDefinitionNode);
        final AbstractType expectedType = typeKeeper.getType(TypeString.ofIdentifier("integer", "sw"));
        final AbstractType actualType = result.get(0, null);
        assertThat(actualType)
            .isNotNull()
            .isEqualTo(expectedType);
    }

    @Test
    void testParameterReferenceOptional() {
        final String code = ""
            + "_method a.b\n"
            + "  _return _self.returns_param()\n"
            + "_endmethod";

        // Set up TypeKeeper/TypeReasoner.
        final TypeKeeper typeKeeper = new TypeKeeper();
        final TypeString aRef = TypeString.ofIdentifier("a", "user");
        final MagikType aType = new MagikType(typeKeeper, Sort.SLOTTED, aRef);
        final TypeString param1Ref = TypeString.ofParameterRef("p1");
        aType.addMethod(
            null,
            EnumSet.noneOf(Method.Modifier.class),
            "returns_param()",
            List.of(
                new Parameter("p1", Parameter.Modifier.OPTIONAL)),
            null,
            null,
            new ExpressionResultString(param1Ref),
            new ExpressionResultString());

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, typeKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodDefinitionNode = topNode.getFirstDescendant(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasonerState.getNodeType(methodDefinitionNode);
        final AbstractType expectedType = typeKeeper.getType(TypeString.SW_UNSET);
        final AbstractType actualType = result.get(0, null);
        assertThat(actualType)
            .isNotNull()
            .isEqualTo(expectedType);
    }

    @Test
    void testGenericMethodInvocation1() {
        final String code = ""
            + "_method object.m\n"
            + "  _local r << sw:rope.new()  # type: sw:rope<sw:integer>\n"
            + "  _return r.an_element()\n"
            + "_endmethod\n";

        // Set up TypeKeeper/TypeReasoner.
        final TypeKeeper typeKeeper = new TypeKeeper();
        final TypeString ropeRef = TypeString.ofIdentifier("rope", "sw");
        final MagikType ropeType = new MagikType(typeKeeper, Sort.SLOTTED, ropeRef);
        ropeType.addGeneric(null, "E");
        ropeType.addMethod(
            null,
            EnumSet.noneOf(Method.Modifier.class),
            "new()",
            Collections.emptyList(),
            null,
            null,
            new ExpressionResultString(
                TypeString.SELF),
            new ExpressionResultString());
        ropeType.addMethod(
            null,
            EnumSet.noneOf(Method.Modifier.class),
            "an_element()",
            Collections.emptyList(),
            null,
            null,
            new ExpressionResultString(
                TypeString.ofGeneric("E")),  // Possibly also `sw:unset`, but for testing purposes...
            new ExpressionResultString());

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, typeKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodDefinitionNode = topNode.getFirstDescendant(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasonerState.getNodeType(methodDefinitionNode);
        final TypeString integerRef = TypeString.ofIdentifier("integer", "sw");
        final AbstractType integerType = typeKeeper.getType(integerRef);
        final AbstractType actualType = result.get(0, null);
        assertThat(actualType)
            .isNotNull()
            .isEqualTo(integerType);
    }

    @Test
    void testGenericMethodInvocation2() {
        final String code = ""
            + "_method object.m\n"
            + "  _local pl << sw:property_list.new()  # type: sw:property_list<sw:symbol, sw:integer>\n"
            + "  _return pl.a_key(), pl.an_element()\n"
            + "_endmethod\n";

        // Set up TypeKeeper/TypeReasoner.
        final TypeKeeper typeKeeper = new TypeKeeper();
        final TypeString propertyListRef = TypeString.ofIdentifier("property_list", "sw");
        final MagikType propertyListType = new MagikType(typeKeeper, Sort.SLOTTED, propertyListRef);
        propertyListType.addGeneric(null, "K");
        propertyListType.addGeneric(null, "E");
        propertyListType.addMethod(
            null,
            EnumSet.noneOf(Method.Modifier.class),
            "new()",
            Collections.emptyList(),
            null,
            null,
            new ExpressionResultString(
                TypeString.SELF),
            new ExpressionResultString());
        propertyListType.addMethod(
            null,
            EnumSet.noneOf(Method.Modifier.class),
            "a_key()",
            Collections.emptyList(),
            null,
            null,
            new ExpressionResultString(
                TypeString.ofGeneric("K")),  // Possibly also `sw:unset`, but for testing purposes...
            new ExpressionResultString());
        propertyListType.addMethod(
            null,
            EnumSet.noneOf(Method.Modifier.class),
            "an_element()",
            Collections.emptyList(),
            null,
            null,
            new ExpressionResultString(
                TypeString.ofGeneric("E")),  // Possibly also `sw:unset`, but for testing purposes...
            new ExpressionResultString());

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, typeKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodDefinitionNode = topNode.getFirstDescendant(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasonerState.getNodeType(methodDefinitionNode);

        final TypeString symbolRef = TypeString.ofIdentifier("symbol", "sw");
        final AbstractType symbolType = typeKeeper.getType(symbolRef);
        final AbstractType actualType0 = result.get(0, null);
        assertThat(actualType0)
            .isNotNull()
            .isEqualTo(symbolType);

        final TypeString integerRef = TypeString.ofIdentifier("integer", "sw");
        final AbstractType integerType = typeKeeper.getType(integerRef);
        final AbstractType actualType1 = result.get(1, null);
        assertThat(actualType1)
            .isNotNull()
            .isEqualTo(integerType);
    }

    @Test
    void testGenericIterMethodInvocation() {
        final String code = ""
            + "_method object.m\n"
            + "  _local pl << sw:property_list.new()  # type: sw:property_list<sw:symbol, sw:integer>\n"
            + "  _for k, e _over pl.fast_keys_and_elements()\n"
            + "  _loop\n"
            + "  _endloop\n"
            + "_endmethod\n";

        // Set up TypeKeeper/TypeReasoner.
        final TypeKeeper typeKeeper = new TypeKeeper();
        final TypeString propertyListRef = TypeString.ofIdentifier("property_list", "sw");
        final MagikType propertyListType = new MagikType(typeKeeper, Sort.SLOTTED, propertyListRef);
        propertyListType.addGeneric(null, "K");
        propertyListType.addGeneric(null, "E");
        propertyListType.addMethod(
            null,
            EnumSet.noneOf(Method.Modifier.class),
            "new()",
            Collections.emptyList(),
            null,
            null,
            new ExpressionResultString(
                TypeString.SELF),
            new ExpressionResultString());
        propertyListType.addMethod(
            null,
            EnumSet.noneOf(Method.Modifier.class),
            "fast_keys_and_elements()",
            Collections.emptyList(),
            null,
            null,
            new ExpressionResultString(),
            new ExpressionResultString(
                TypeString.ofGeneric("K"),
                TypeString.ofGeneric("E")));

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, typeKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        final AstNode topNode = magikFile.getTopNode();
        final AstNode forVariablesNode = topNode.getFirstDescendant(MagikGrammar.FOR_VARIABLES);
        final List<AstNode> identifierNodes = forVariablesNode.getDescendants(MagikGrammar.IDENTIFIER);

        final AstNode kIdentifierNode = identifierNodes.get(0);
        final ExpressionResult kResult = reasonerState.getNodeType(kIdentifierNode);
        final AbstractType actualType0 = kResult.get(0, null);
        final TypeString symbolRef = TypeString.ofIdentifier("symbol", "sw");
        final AbstractType symbolType = typeKeeper.getType(symbolRef);
        assertThat(actualType0)
            .isNotNull()
            .isEqualTo(symbolType);

        final AstNode eIdentifierNode = identifierNodes.get(1);
        final ExpressionResult eResult = reasonerState.getNodeType(eIdentifierNode);
        final TypeString integerRef = TypeString.ofIdentifier("integer", "sw");
        final AbstractType integerType = typeKeeper.getType(integerRef);
        final AbstractType actualType1 = eResult.get(0, null);
        assertThat(actualType1)
            .isNotNull()
            .isEqualTo(integerType);
    }

    @Test
    void testGenericSlot() {
        final String code = ""
            + "_method exemplar.m\n"
            + "  _return .stack.pop()\n"
            + "_endmethod\n";

        // Set up TypeKeeper/TypeReasoner.
        final TypeKeeper typeKeeper = new TypeKeeper();

        final TypeString stackRef = TypeString.ofIdentifier("stack", "sw");
        final MagikType stackType = new MagikType(typeKeeper, Sort.SLOTTED, stackRef);
        stackType.addGeneric(null, "E");
        stackType.addMethod(
            null,
            EnumSet.noneOf(Method.Modifier.class),
            "pop()",
            Collections.emptyList(),
            null,
            null,
            new ExpressionResultString(
                TypeString.ofGeneric("E")),
            new ExpressionResultString());

        final TypeString exemplarRef = TypeString.ofIdentifier("exemplar", "sw");
        final MagikType exemplarType = new MagikType(typeKeeper, Sort.SLOTTED, exemplarRef);
        final TypeString slotTypeRef = TypeStringParser.parseTypeString("sw:stack<sw:integer>", "sw");
        exemplarType.addSlot(null, "stack", slotTypeRef);

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, typeKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodDefinitionNode = topNode.getFirstDescendant(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasonerState.getNodeType(methodDefinitionNode);
        final TypeString integerRef = TypeString.ofIdentifier("integer", "sw");
        final AbstractType integerType = typeKeeper.getType(integerRef);
        final AbstractType actualType = result.get(0, null);
        assertThat(actualType)
            .isNotNull()
            .isEqualTo(integerType);
    }

}
