package nl.ramsolutions.sw.magik.analysis.typing;

import com.sonar.sslr.api.AstNode;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import nl.ramsolutions.sw.magik.MagikTypedFile;
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
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(methodNode);
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
        final TypeString ropeRef = TypeString.of("sw:rope");
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
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(methodNode);
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
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final AbstractType integerType = typeKeeper.getType(TypeString.of("sw:integer"));
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
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final AbstractType combinedType = new CombinedType(
            typeKeeper.getType(TypeString.of("sw:integer")),
            typeKeeper.getType(TypeString.of("sw:symbol")));
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
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(methodNode);
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
        final TypeString ropeRef = TypeString.of("sw:rope");
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
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(methodNode);
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
        final TypeString ropeRef = TypeString.of("sw:rope");
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
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(methodNode);
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
        final TypeString falseRef = TypeString.of("sw:false");
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
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(methodNode);
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
        final TypeString char16VectorRef = TypeString.of("sw:char16_vector");
        final BinaryOperator binOp = new BinaryOperator(
            BinaryOperator.Operator.valueFor("+"), char16VectorRef, char16VectorRef, char16VectorRef);
        typeKeeper.addBinaryOperator(binOp);

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, typeKeeper);
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(methodNode);
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
        final TypeString char16VectorRef = TypeString.of("sw:char16_vector");
        final TypeString integerRef = TypeString.of("sw:integer");
        final TypeString symbolRef = TypeString.of("sw:symbol");
        final BinaryOperator binOp1 =
            new BinaryOperator(BinaryOperator.Operator.valueFor("+"), integerRef, symbolRef, symbolRef);
        typeKeeper.addBinaryOperator(binOp1);
        final BinaryOperator binOp2 =
            new BinaryOperator(BinaryOperator.Operator.valueFor("+"), symbolRef, char16VectorRef, char16VectorRef);
        typeKeeper.addBinaryOperator(binOp2);

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, typeKeeper);
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(methodNode);
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
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(3);

        final TypeString integerRef = TypeString.of("sw:integer");
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
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final TypeString integerRef = TypeString.of("sw:integer");
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
        final TypeString integerRef = TypeString.of("sw:integer");
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
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(methodNode);
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
        final TypeString integerRef = TypeString.of("sw:integer");
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
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(methodNode);
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
        final TypeString integerRef = TypeString.of("sw:integer");
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
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final CombinedType expectedType = new CombinedType(
            typeKeeper.getType(TypeString.of("sw:integer")),
            typeKeeper.getType(TypeString.of("sw:unset")));
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
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        // Assert user:object.test type determined.resul
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(methodNode);
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
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final ProcedureInstance resultType = (ProcedureInstance) result.get(0, null);
        assertThat(resultType).isNotNull();
        assertThat(resultType.getProcedureName()).isEqualTo("test");

        final Collection<Method> invokeMethods = resultType.getMethods("invoke()");
        assertThat(invokeMethods).isNotEmpty();
        final TypeString integerRef = TypeString.of("sw:integer");
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
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final AbstractType resultType = result.get(0, null);
        final AbstractType integerType = typeKeeper.getType(TypeString.of("sw:integer"));
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
            new MagikType(typeKeeper, Sort.INDEXED, TypeString.of("sw:property_list"));
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
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(methodNode);
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
        final TypeString objectRef = TypeString.of("sw:object");
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
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(methodNode);
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
        final TypeString ropeRef = TypeString.of("sw:rope");
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
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(methodNode);
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
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final AbstractType integerType = typeKeeper.getType(TypeString.of("sw:integer"));
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
            + "      _return a, b"
            + "    _endloop\n"
            + "_endmethod\n";

        // Do analysis.
        final TypeKeeper typeKeeper = new TypeKeeper();
        final MagikTypedFile magikFile = this.createMagikFile(code, typeKeeper);
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode forVariablesNode = topNode.getFirstDescendant(MagikGrammar.FOR_VARIABLES);

        final AstNode aNode = forVariablesNode.getDescendants(MagikGrammar.IDENTIFIER).get(0);
        final ExpressionResult aResult = reasoner.getNodeType(aNode);
        assertThat(aResult.size()).isEqualTo(1);

        final TypeString integerRef = TypeString.of("sw:integer");
        final AbstractType integerType = typeKeeper.getType(integerRef);
        final AbstractType aResultType = aResult.get(0, null);
        assertThat(aResultType)
            .isEqualTo(integerType);

        final AstNode bNode = forVariablesNode.getDescendants(MagikGrammar.IDENTIFIER).get(1);
        final ExpressionResult bResult = reasoner.getNodeType(bNode);
        assertThat(bResult.size()).isEqualTo(1);

        final TypeString floatRef = TypeString.of("sw:float");
        final AbstractType floatType = typeKeeper.getType(floatRef);
        final AbstractType bResultType = bResult.get(0, null);
        assertThat(bResultType)
            .isEqualTo(floatType);
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
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final AbstractType unsetType = typeKeeper.getType(TypeString.of("sw:unset"));
        final AbstractType resultType = result.get(0, null);
        assertThat(resultType)
            .isNotNull()
            .isEqualTo(unsetType);
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
        final TypeString integerRef = TypeString.of("sw:integer");
        final TypeString floatRef = TypeString.of("sw:float");
        final AbstractType floatType = typeKeeper.getType(floatRef);
        final BinaryOperator binaryOperator =
            new BinaryOperator(BinaryOperator.Operator.valueFor("+"), integerRef, floatRef, floatRef);
        typeKeeper.addBinaryOperator(binaryOperator);

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, typeKeeper);
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(methodNode);
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
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode parameterNode = topNode
            .getFirstDescendant(MagikGrammar.PARAMETER)
            .getFirstChild(MagikGrammar.IDENTIFIER);
        final ExpressionResult result = reasoner.getNodeType(parameterNode);

        final TypeString simpleVectorRef = TypeString.of("sw:simple_vector");
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
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode procNode = topNode.getFirstDescendant(MagikGrammar.PROCEDURE_DEFINITION);
        final AstNode importNode = procNode.getFirstDescendant(MagikGrammar.VARIABLE_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(importNode);

        final AbstractType integerType = typeKeeper.getType(TypeString.of("sw:integer"));
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
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(2);

        final AbstractType integerType = typeKeeper.getType(TypeString.of("sw:integer"));
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
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        final AstNode topNode = magikFile.getTopNode();
        final AstNode tryVarNode = topNode.getFirstDescendant(MagikGrammar.TRY_VARIABLE);
        final ExpressionResult result = reasoner.getNodeType(tryVarNode);
        assertThat(result).isNotNull();

        final AbstractType conditionType = typeKeeper.getType(TypeString.of("sw:condition"));
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
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodDefNode = topNode.getFirstDescendant(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(methodDefNode);
        assertThat(result).isNotNull();

        // First is integer + undefined.
        final TypeString integerRef = TypeString.of("sw:integer");
        final AbstractType intAndUndefinedType = new CombinedType(
            typeKeeper.getType(integerRef),
            UndefinedType.INSTANCE);
        final AbstractType resultType0 = result.get(0, null);
        assertThat(resultType0)
                .isNotNull()
                .isEqualTo(intAndUndefinedType);

        // The rest is unset + undefined, as merging the two yields unset for the rest.
        final TypeString unsetRef = TypeString.of("sw:unset");
        final AbstractType unsetAndUndefinedType = new CombinedType(
            typeKeeper.getType(unsetRef),
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
        final TypeString sRef = TypeString.of("sw:s");
        final MagikType sType = new MagikType(typeKeeper, Sort.SLOTTED, sRef);
        final TypeString tRef = TypeString.of("sw:t");
        final MagikType tType = new MagikType(typeKeeper, Sort.SLOTTED, tRef);
        tType.addParent(sRef);

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, typeKeeper);
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        final AstNode topNode = magikFile.getTopNode();
        final AstNode superNode = topNode.getFirstDescendant(MagikGrammar.SUPER).getParent();
        final ExpressionResult result = reasoner.getNodeType(superNode);
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
        final TypeString rRef = TypeString.of("sw:r");
        final MagikType rType = new MagikType(typeKeeper, Sort.SLOTTED, rRef);
        final TypeString sRef = TypeString.of("sw:s");
        new MagikType(typeKeeper, Sort.SLOTTED, sRef);
        final TypeString tRef = TypeString.of("sw:t");
        final MagikType tType = new MagikType(typeKeeper, Sort.SLOTTED, tRef);
        tType.addParent(rRef);
        tType.addParent(sRef);

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, typeKeeper);
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        final AstNode topNode = magikFile.getTopNode();
        final AstNode superNode = topNode.getFirstDescendant(MagikGrammar.SUPER).getParent();
        final ExpressionResult result = reasoner.getNodeType(superNode);
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
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        final TypeString floatRef = TypeString.of("sw:float");
        final AbstractType floatType = typeKeeper.getType(floatRef);
        final AstNode topNode = magikFile.getTopNode();

        // Test parameter definition.
        final AstNode parameterNode = topNode
            .getFirstDescendant(MagikGrammar.PARAMETER)
            .getFirstChild(MagikGrammar.IDENTIFIER);
        final ExpressionResult actualParameterResult = reasoner.getNodeType(parameterNode);
        final AbstractType actualParameterType = actualParameterResult.get(0, null);
        assertThat(actualParameterType)
            .isNotNull()
            .isEqualTo(floatType);

        // Test parameter usage.
        final AstNode procInvocationNode = topNode.getFirstDescendant(MagikGrammar.PROCEDURE_INVOCATION);
        final AstNode atomNode = procInvocationNode.getFirstDescendant(MagikGrammar.ATOM);
        final ExpressionResult actualAtomResult = reasoner.getNodeType(atomNode);
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
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        final AstNode topNode = magikFile.getTopNode();
        final AstNode parameterNode = topNode
            .getFirstDescendant(MagikGrammar.PARAMETER)
            .getFirstChild(MagikGrammar.IDENTIFIER);
        final ExpressionResult result = reasoner.getNodeType(parameterNode);
        final TypeString floatRef = TypeString.of("sw:float");
        final AbstractType floatType = typeKeeper.getType(floatRef);
        final TypeString integerRef = TypeString.of("sw:integer");
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
        final MagikType aType = new MagikType(typeKeeper, Sort.SLOTTED, TypeString.of("user:a"));
        final TypeString param1Ref = TypeString.of("_parameter(p1)");
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
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodDefinitionNode = topNode.getFirstDescendant(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(methodDefinitionNode);
        final AbstractType expectedType = typeKeeper.getType(TypeString.of("sw:integer"));
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
        final MagikType aType = new MagikType(typeKeeper, Sort.SLOTTED, TypeString.of("user:a"));
        final TypeString param1Ref = TypeString.of("_parameter(p1)");
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
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodDefinitionNode = topNode.getFirstDescendant(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(methodDefinitionNode);
        final AbstractType expectedType = typeKeeper.getType(TypeString.of("sw:integer"));
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
        final MagikType aType = new MagikType(typeKeeper, Sort.SLOTTED, TypeString.of("user:a"));
        final TypeString param1Ref = TypeString.of("_parameter(p1)");
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
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodDefinitionNode = topNode.getFirstDescendant(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(methodDefinitionNode);
        final AbstractType expectedType = typeKeeper.getType(TypeString.of("sw:unset"));
        final AbstractType actualType = result.get(0, null);
        assertThat(actualType)
            .isNotNull()
            .isEqualTo(expectedType);
    }

}
