package nl.ramsolutions.sw.magik.analysis.typing;

import com.sonar.sslr.api.AstNode;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.CombinedType;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResult;
import nl.ramsolutions.sw.magik.analysis.typing.types.GlobalReference;
import nl.ramsolutions.sw.magik.analysis.typing.types.IndexedType;
import nl.ramsolutions.sw.magik.analysis.typing.types.MagikType;
import nl.ramsolutions.sw.magik.analysis.typing.types.Method;
import nl.ramsolutions.sw.magik.analysis.typing.types.ProcedureInstance;
import nl.ramsolutions.sw.magik.analysis.typing.types.SelfType;
import nl.ramsolutions.sw.magik.analysis.typing.types.SlottedType;
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
    void testReasonMethodReturn() {
        final String code = ""
            + "_package sw\n"
            + "_method object.test\n"
            + "    _return rope.new()\n"
            + "_endmethod\n";

        // Set up TypeKeeper/TypeReasoner.
        final TypeKeeper typeKeeper = new TypeKeeper();
        final MagikType ropeType = new SlottedType(GlobalReference.of("sw:rope"));
        ropeType.addMethod(
            EnumSet.noneOf(Method.Modifier.class),
            null,
            "new()",
            Collections.emptyList(),
            null,
            new ExpressionResult(ropeType));
        typeKeeper.addType(ropeType);

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, typeKeeper);
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final MagikType resultType = (MagikType) result.get(0, null);
        assertThat(resultType).isNotNull();
        assertThat(resultType.getFullName()).isEqualTo("sw:rope");
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

        final AbstractType resultType = (AbstractType) result.get(0, null);
        assertThat(resultType).isNotNull();
        assertThat(resultType.getFullName()).isEqualTo("sw:integer");
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

        final AbstractType resultType = (AbstractType) result.get(0, null);
        assertThat(resultType).isNotNull();
        assertThat(resultType.getFullName()).isEqualTo("sw:integer|sw:symbol");
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
        assertThat(resultType).isNotNull();
        assertThat(resultType.getFullName()).isEqualTo(SelfType.SERIALIZED_NAME);
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
        final MagikType ropeType = new SlottedType(GlobalReference.of("sw:rope"));
        ropeType.addMethod(
            EnumSet.noneOf(Method.Modifier.class),
            null,
            "new()",
            Collections.emptyList(),
            null,
            new ExpressionResult(ropeType));
        ropeType.addMethod(
            EnumSet.noneOf(Method.Modifier.class),
            null,
            "add()",
            Collections.emptyList(),
            null,
            new ExpressionResult());
        typeKeeper.addType(ropeType);

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, typeKeeper);
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final MagikType resultType = (MagikType) result.get(0, null);
        assertThat(resultType).isNotNull();
        assertThat(resultType.getFullName()).isEqualTo("sw:rope");
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
        final MagikType ropeType = new SlottedType(GlobalReference.of("sw:rope"));
        ropeType.addMethod(
            EnumSet.noneOf(Method.Modifier.class),
            null,
            "new()",
            Collections.emptyList(),
            null,
            new ExpressionResult(ropeType));
        ropeType.addMethod(
            EnumSet.noneOf(Method.Modifier.class),
            null,
            "add()",
            Collections.emptyList(),
            null,
            new ExpressionResult());
        typeKeeper.addType(ropeType);

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, typeKeeper);
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final MagikType resultType = (MagikType) result.get(0, null);
        assertThat(resultType).isNotNull();
        assertThat(resultType.getFullName()).isEqualTo("sw:rope");
    }

    @Test
    void testUnaryOperator() {
        final String code = ""
            + "_method object.test\n"
            + "    _return _not _true\n"
            + "_endmethod\n";

        // Set up TypeKeeper/TypeReasoner.
        final TypeKeeper typeKeeper = new TypeKeeper();
        final AbstractType falseType = typeKeeper.getType(GlobalReference.of("sw:false"));
        final UnaryOperator unaryOp = new UnaryOperator(UnaryOperator.Operator.valueFor("_not"), falseType, falseType);
        typeKeeper.addUnaryOperator(unaryOp);

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, typeKeeper);
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final MagikType resultType = (MagikType) result.get(0, null);
        assertThat(resultType).isNotNull();
        assertThat(resultType.getFullName()).isEqualTo("sw:false");
    }

    @Test
    void testBinaryOperator() {
        final String code = ""
            + "_method object.test\n"
            + "    _return 'a' + 'b'\n"
            + "_endmethod\n";

        // Set up TypeKeeper/TypeReasoner.
        final TypeKeeper typeKeeper = new TypeKeeper();
        final AbstractType char16VectorType = typeKeeper.getType(GlobalReference.of("sw:char16_vector"));
        final BinaryOperator binOp = new BinaryOperator(
            BinaryOperator.Operator.valueFor("+"), char16VectorType, char16VectorType, char16VectorType);
        typeKeeper.addBinaryOperator(binOp);

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, typeKeeper);
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final MagikType resultType = (MagikType) result.get(0, null);
        assertThat(resultType).isNotNull();
        assertThat(resultType.getFullName()).isEqualTo("sw:char16_vector");
    }

    @Test
    void testBinaryOperatorChained() {
        final String code = ""
            + "_method object.test\n"
            + "    _return 1 + :a + 'a'\n"
            + "_endmethod\n";

        // Set up TypeKeeper/TypeReasoner.
        final TypeKeeper typeKeeper = new TypeKeeper();
        final AbstractType char16VectorType = typeKeeper.getType(GlobalReference.of("sw:char16_vector"));
        final AbstractType symbolType = typeKeeper.getType(GlobalReference.of("sw:symbol"));
        final AbstractType integerType = typeKeeper.getType(GlobalReference.of("sw:integer"));
        final BinaryOperator binOp1 =
            new BinaryOperator(BinaryOperator.Operator.valueFor("+"), integerType, symbolType, symbolType);
        typeKeeper.addBinaryOperator(binOp1);
        final BinaryOperator binOp2 =
            new BinaryOperator(BinaryOperator.Operator.valueFor("+"), symbolType, char16VectorType, char16VectorType);
        typeKeeper.addBinaryOperator(binOp2);

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, typeKeeper);
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final MagikType resultType = (MagikType) result.get(0, null);
        assertThat(resultType).isNotNull();
        assertThat(resultType.getFullName()).isEqualTo("sw:char16_vector");
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

        final MagikType resultType1 = (MagikType) result.get(0, null);
        assertThat(resultType1).isNotNull();
        assertThat(resultType1.getFullName()).isEqualTo("sw:integer");

        final MagikType resultType2 = (MagikType) result.get(1, null);
        assertThat(resultType2).isNotNull();
        assertThat(resultType2.getFullName()).isEqualTo("sw:integer");

        final MagikType resultType3 = (MagikType) result.get(2, null);
        assertThat(resultType3).isNotNull();
        assertThat(resultType3.getFullName()).isEqualTo("sw:integer");
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

        final MagikType resultType = (MagikType) result.get(0, null);
        assertThat(resultType).isNotNull();
        assertThat(resultType.getFullName()).isEqualTo("sw:integer");
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
        final MagikType integerType = (MagikType) typeKeeper.getType(GlobalReference.of("sw:integer"));
        integerType.addMethod(
            EnumSet.noneOf(Method.Modifier.class),
            null,
            "upto()",
            Collections.emptyList(),
            null,
            new ExpressionResult(),
            new ExpressionResult(integerType));

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, typeKeeper);
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final MagikType resultType = (MagikType) result.get(0, null);
        assertThat(resultType).isNotNull();
        assertThat(resultType.getFullName()).isEqualTo("sw:integer");
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
        final MagikType integerType = (MagikType) typeKeeper.getType(GlobalReference.of("sw:integer"));
        integerType.addMethod(
            EnumSet.noneOf(Method.Modifier.class),
            null,
            "upto()",
            Collections.emptyList(),
            null,
            new ExpressionResult(),
            new ExpressionResult(integerType));

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, typeKeeper);
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final MagikType resultType = (MagikType) result.get(0, null);
        assertThat(resultType).isNotNull();
        assertThat(resultType.getFullName()).isEqualTo("sw:integer");
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
        final MagikType integerType = (MagikType) typeKeeper.getType(GlobalReference.of("sw:integer"));
        integerType.addMethod(
            EnumSet.noneOf(Method.Modifier.class),
            null,
            "upto()",
            Collections.emptyList(),
            null,
            new ExpressionResult(),
            new ExpressionResult(integerType));

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, typeKeeper);
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final CombinedType resultType = (CombinedType) result.get(0, null);
        assertThat(resultType).isNotNull();
        assertThat(resultType.getFullName()).isEqualTo("sw:integer|sw:unset");
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
        assertThat(resultType).isNotNull();
        assertThat(resultType.getFullName()).isEqualTo(UndefinedType.SERIALIZED_NAME);
    }

    @Test
    void testReasonMethodReturnProc() {
        final String code = ""
            + "_method object.test\n"
            + "    _return _proc@test()\n"
            + "                >> 10\n"
            + "            _endproc\n"
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

        final ProcedureInstance resultType = (ProcedureInstance) result.get(0, null);
        assertThat(resultType).isNotNull();
        assertThat(resultType.getProcedureName()).isEqualTo("test");

        final Collection<Method> procMethods = resultType.getMethods("invoke()");
        assertThat(procMethods).isNotEmpty();
        procMethods.forEach(procMethod -> {
            final ExpressionResult procResult = procMethod.getCallResult();
            assertThat(procResult.size()).isEqualTo(1);

            final MagikType procResultType = (MagikType) procResult.get(0, null);
            assertThat(procResultType).isNotNull();
            assertThat(procResultType.getFullName()).isEqualTo("sw:integer");
        });
    }

    @Test
    void testMultipleAssignmentStatement() {
        final String code = ""
            + "_method object.test\n"
            + "    (a, b) << (2, 3)\n"
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

        final MagikType resultType = (MagikType) result.get(0, null);
        assertThat(resultType).isNotNull();
        assertThat(resultType.getFullName()).isEqualTo("sw:integer");
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
        final MagikType propertyListType = new IndexedType(GlobalReference.of("sw:property_list"));
        propertyListType.addMethod(
            EnumSet.noneOf(Method.Modifier.class),
            null,
            "new()",
            Collections.emptyList(),
            null,
            new ExpressionResult(SelfType.INSTANCE));
        typeKeeper.addType(propertyListType);

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, typeKeeper);
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final MagikType resultType = (MagikType) result.get(0, null);
        assertThat(resultType).isNotNull();
        assertThat(resultType.getFullName()).isEqualTo("sw:property_list");
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
        final MagikType objectType = (MagikType) typeKeeper.getType(GlobalReference.of("sw:object"));
        objectType.addMethod(
            EnumSet.noneOf(Method.Modifier.class),
            null,
            "test1",
            Collections.emptyList(),
            null,
            new ExpressionResult(SelfType.INSTANCE));

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, typeKeeper);
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final SelfType resultType = (SelfType) result.get(0, null);
        assertThat(resultType).isNotNull();
        assertThat(resultType.getFullName()).isEqualTo(SelfType.SERIALIZED_NAME);
    }

    @Test
    void testSelfNewInit() {
        final String code = ""
            + "_method object.test\n"
            + "    _return rope.new()\n"
            + "_endmethod\n";

        // Set up TypeKeeper/TypeReasoner.
        final TypeKeeper typeKeeper = new TypeKeeper();
        final MagikType ropeType = new SlottedType(GlobalReference.of("sw:rope"));
        typeKeeper.addType(ropeType);
        ropeType.addMethod(
            EnumSet.noneOf(Method.Modifier.class),
            null,
            "new()",
            Collections.emptyList(),
            null,
            new ExpressionResult(SelfType.INSTANCE));

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, typeKeeper);
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final MagikType resultType = (MagikType) result.get(0, null);
        assertThat(resultType).isNotNull();
        assertThat(resultType.getFullName()).isEqualTo("sw:rope");
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

        final MagikType resultType = (MagikType) result.get(0, null);
        assertThat(resultType).isNotNull();
        assertThat(resultType.getFullName()).isEqualTo("sw:integer");
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

        // Set up TypeKeeper/TypeReasoner.
        final TypeKeeper typeKeeper = new TypeKeeper();

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, typeKeeper);
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode forVariablesNode = topNode.getFirstDescendant(MagikGrammar.FOR_VARIABLES);

        final AstNode aNode = forVariablesNode.getDescendants(MagikGrammar.IDENTIFIER).get(0);
        final ExpressionResult aResult = reasoner.getNodeType(aNode);
        assertThat(aResult.size()).isEqualTo(1);

        final MagikType aResultType = (MagikType) aResult.get(0, null);
        assertThat(aResultType).isNotNull();
        assertThat(aResultType.getFullName()).isEqualTo("sw:integer");

        final AstNode bNode = forVariablesNode.getDescendants(MagikGrammar.IDENTIFIER).get(1);
        final ExpressionResult bResult = reasoner.getNodeType(bNode);
        assertThat(bResult.size()).isEqualTo(1);

        final MagikType bResultType = (MagikType) bResult.get(0, null);
        assertThat(bResultType).isNotNull();
        assertThat(bResultType.getFullName()).isEqualTo("sw:float");
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

        final MagikType resultType = (MagikType) result.get(0, null);
        assertThat(resultType).isNotNull();
        assertThat(resultType.getFullName()).isEqualTo("sw:unset");
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
        final AbstractType integerType = typeKeeper.getType(GlobalReference.of("sw:integer"));
        final AbstractType floatType = typeKeeper.getType(GlobalReference.of("sw:float"));
        final BinaryOperator binaryOperator =
            new BinaryOperator(BinaryOperator.Operator.valueFor("+"), integerType, floatType, floatType);
        typeKeeper.addBinaryOperator(binaryOperator);

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, typeKeeper);
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final MagikType resultType = (MagikType) result.get(0, null);
        assertThat(resultType).isNotNull();
        assertThat(resultType.getFullName()).isEqualTo("sw:float");
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
        final AstNode parameterNode = topNode.getFirstDescendant(MagikGrammar.PARAMETER);
        final ExpressionResult result = reasoner.getNodeType(parameterNode);
        final MagikType resultType = (MagikType) result.get(0, null);
        assertThat(resultType).isNotNull();
        assertThat(resultType.getFullName()).isEqualTo("sw:simple_vector");
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
        final MagikType resultType = (MagikType) result.get(0, null);
        assertThat(resultType).isNotNull();
        assertThat(resultType.getFullName()).isEqualTo("sw:integer");
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

        final MagikType resultType1 = (MagikType) result.get(0, null);
        assertThat(resultType1).isNotNull();
        assertThat(resultType1.getFullName()).isEqualTo("sw:integer");

        final MagikType resultType2 = (MagikType) result.get(1, null);
        assertThat(resultType2).isNotNull();
        assertThat(resultType2.getFullName()).isEqualTo("sw:integer");
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

        final MagikType resultType = (MagikType) result.get(0, null);
        assertThat(resultType).isNotNull();
        assertThat(resultType.getFullName()).isEqualTo("sw:condition");
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
        final AbstractType resultType0 = result.get(0, null);
        assertThat(resultType0)
                .isNotNull()
                .isInstanceOf(CombinedType.class);
        assertThat(resultType0.getFullName()).isEqualTo("_undefined|sw:integer");

        // The rest is unset + undefined, as merging the two yields unset for the rest.
        result.getTypes().stream()
            .skip(1)
            .forEach(type -> {
                assertThat(type)
                    .isNotNull()
                    .isInstanceOf(CombinedType.class);
                assertThat(type.getFullName()).isEqualTo("_undefined|sw:unset");
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
        final MagikType sType = new SlottedType(GlobalReference.of("sw:s"));
        typeKeeper.addType(sType);
        final MagikType tType = new SlottedType(GlobalReference.of("sw:t"));
        tType.addParent(sType);
        typeKeeper.addType(tType);

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
        final MagikType rType = new SlottedType(GlobalReference.of("sw", "r"));
        typeKeeper.addType(rType);
        final MagikType sType = new SlottedType(GlobalReference.of("sw", "s"));
        typeKeeper.addType(sType);
        final MagikType tType = new SlottedType(GlobalReference.of("sw", "t"));
        tType.addParent(rType);
        tType.addParent(sType);
        typeKeeper.addType(tType);

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
            + "_endmethod";

        // Set up TypeKeeper/TypeReasoner.
        final TypeKeeper typeKeeper = new TypeKeeper();

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, typeKeeper);
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        final AstNode topNode = magikFile.getTopNode();
        final AstNode parameterNode = topNode.getFirstDescendant(MagikGrammar.PARAMETER);
        final ExpressionResult result = reasoner.getNodeType(parameterNode);
        assertThat(result)
            .isNotNull();

        final AbstractType floatType = typeKeeper.getType(GlobalReference.of("sw:float"));
        final AbstractType actualType = result.get(0, null);
        assertThat(actualType)
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
        final AstNode parameterNode = topNode.getFirstDescendant(MagikGrammar.PARAMETER);
        final ExpressionResult result = reasoner.getNodeType(parameterNode);
        assertThat(result)
            .isNotNull();

        final AbstractType floatType = typeKeeper.getType(GlobalReference.of("sw:float"));
        final AbstractType integerType = typeKeeper.getType(GlobalReference.of("sw:integer"));
        final AbstractType expectedType = CombinedType.combine(floatType, integerType);
        final AbstractType actualType = result.get(0, null);
        assertThat(actualType)
            .isNotNull()
            .isEqualTo(expectedType);
    }

}
