package nl.ramsolutions.sw.magik.analysis.typing;

import com.sonar.sslr.api.AstNode;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.definitions.BinaryOperatorDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ParameterDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasoner;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.CombinedType;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResult;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.types.Method;
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

    private static final URI TEST_URI = URI.create("tests://unittest");

    private MagikTypedFile createMagikFile(final String code, final IDefinitionKeeper definitionKeeper) {
        return new MagikTypedFile(TEST_URI, code, definitionKeeper);
    }

    @Test
    void testReasonMethodReturnNone() {
        final String code = ""
            + "_package sw\n"
            + "_method object.test\n"
            + "    _return\n"
            + "_endmethod\n";

        // Set up TypeKeeper/TypeReasoner.
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
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
                Collections.emptyList()));
        definitionKeeper.add(
            new MethodDefinition(
                null,
                null,
                null,
                null,
                ropeRef,
                "new()",
                EnumSet.noneOf(MethodDefinition.Modifier.class),
                Collections.emptyList(),
                null,
                new ExpressionResultString(TypeString.SELF),
                ExpressionResultString.EMPTY));

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
        final ITypeKeeper typeKeeper = magikFile.getTypeKeeper();
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final AbstractType resultType = result.get(0, null);
        final AbstractType ropeType = typeKeeper.getType(ropeRef);
        assertThat(resultType).isEqualTo(ropeType);
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
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
        final ITypeKeeper typeKeeper = magikFile.getTypeKeeper();
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final AbstractType integerType = typeKeeper.getType(TypeString.ofIdentifier("integer", "sw"));
        final AbstractType resultType = (AbstractType) result.get(0, null);
        assertThat(resultType).isEqualTo(integerType);
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
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
        final ITypeKeeper typeKeeper = magikFile.getTypeKeeper();
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final AbstractType combinedType = new CombinedType(
            typeKeeper.getType(TypeString.ofIdentifier("integer", "sw")),
            typeKeeper.getType(TypeString.ofIdentifier("symbol", "sw")));
        final AbstractType resultType = (AbstractType) result.get(0, null);
        assertThat(resultType).isEqualTo(combinedType);
    }

    @Test
    void testReasonMethodSelf() {
        final String code = ""
            + "_method object.test\n"
            + "    _return _self\n"
            + "_endmethod\n";

        // Set up TypeKeeper/TypeReasoner.
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final SelfType resultType = (SelfType) result.get(0, null);
        assertThat(resultType).isEqualTo(SelfType.INSTANCE);
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
                Collections.emptyList()));
        definitionKeeper.add(
            new MethodDefinition(
                null,
                null,
                null,
                null,
                ropeRef,
                "new()",
                EnumSet.noneOf(MethodDefinition.Modifier.class),
                Collections.emptyList(),
                null,
                new ExpressionResultString(TypeString.SELF),
                ExpressionResultString.EMPTY));
        definitionKeeper.add(
            new MethodDefinition(
                null,
                null,
                null,
                null,
                ropeRef,
                "add()",
                EnumSet.noneOf(MethodDefinition.Modifier.class),
                Collections.emptyList(),
                null,
                ExpressionResultString.UNDEFINED,
                ExpressionResultString.EMPTY));

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final AbstractType resultType = result.get(0, null);
        assertThat(resultType.getTypeString()).isEqualTo(ropeRef);
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
                Collections.emptyList()));
        definitionKeeper.add(
            new MethodDefinition(
                null,
                null,
                null,
                null,
                ropeRef,
                "new()",
                EnumSet.noneOf(MethodDefinition.Modifier.class),
                Collections.emptyList(),
                null,
                new ExpressionResultString(TypeString.SELF),
                ExpressionResultString.EMPTY));
        definitionKeeper.add(
            new MethodDefinition(
                null,
                null,
                null,
                null,
                ropeRef,
                "add()",
                EnumSet.noneOf(MethodDefinition.Modifier.class),
                Collections.emptyList(),
                null,
                ExpressionResultString.UNDEFINED,
                ExpressionResultString.EMPTY));

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        // Assert user:object.test call result determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final AbstractType resultType = result.get(0, null);
        assertThat(resultType.getTypeString()).isEqualTo(ropeRef);
    }

    @Test
    void testUnaryOperator() {
        final String code = ""
            + "_method object.test\n"
            + "    _return _not _true\n"
            + "_endmethod\n";

        // Set up TypeKeeper/TypeReasoner.
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final TypeString falseRef = TypeString.ofIdentifier("false", "sw");
        definitionKeeper.add(
            new MethodDefinition(
                null,
                null,
                null,
                null,
                falseRef,
                "not",
                EnumSet.noneOf(MethodDefinition.Modifier.class),
                Collections.emptyList(),
                null,
                new ExpressionResultString(TypeString.SELF),
                ExpressionResultString.EMPTY));

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final AbstractType resultType = result.get(0, null);
        assertThat(resultType.getTypeString()).isEqualTo(falseRef);
    }

    @Test
    void testBinaryOperator() {
        final String code = ""
            + "_method object.test\n"
            + "    _return 'a' + 'b'\n"
            + "_endmethod\n";

        // Set up TypeKeeper/TypeReasoner.
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final TypeString char16VectorRef = TypeString.ofIdentifier("char16_vector", "sw");
        definitionKeeper.add(
            new BinaryOperatorDefinition(
                null,
                null,
                code,
                null,
                "+",
                char16VectorRef,
                char16VectorRef,
                char16VectorRef));

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final AbstractType resultType = result.get(0, null);
        assertThat(resultType.getTypeString()).isEqualTo(char16VectorRef);
    }

    @Test
    void testBinaryOperatorChained() {
        final String code = ""
            + "_method object.test\n"
            + "    _return 1 + :a + 'a'\n"
            + "_endmethod\n";

        // Set up TypeKeeper/TypeReasoner.
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final TypeString char16VectorRef = TypeString.ofIdentifier("char16_vector", "sw");
        final TypeString integerRef = TypeString.ofIdentifier("integer", "sw");
        final TypeString symbolRef = TypeString.ofIdentifier("symbol", "sw");
        definitionKeeper.add(
            new BinaryOperatorDefinition(
                null,
                null,
                code,
                null,
                "+",
                integerRef,
                symbolRef,
                symbolRef));
        definitionKeeper.add(
            new BinaryOperatorDefinition(
                null,
                null,
                code,
                null,
                "+",
                symbolRef,
                char16VectorRef,
                char16VectorRef));

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final AbstractType resultType = result.get(0, null);
        assertThat(resultType.getTypeString()).isEqualTo(char16VectorRef);
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
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(3);

        final TypeString integerRef = TypeString.ofIdentifier("integer", "sw");
        final AbstractType resultType1 = result.get(0, null);
        assertThat(resultType1.getTypeString()).isEqualTo(integerRef);

        final AbstractType resultType2 = result.get(1, null);
        assertThat(resultType2.getTypeString()).isEqualTo(integerRef);

        final AbstractType resultType3 = result.get(2, null);
        assertThat(resultType3.getTypeString()).isEqualTo(integerRef);
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
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final TypeString integerRef = TypeString.ofIdentifier("integer", "sw");
        final AbstractType resultType = result.get(0, null);
        assertThat(resultType.getTypeString()).isEqualTo(integerRef);
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
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final TypeString integerRef = TypeString.ofIdentifier("integer", "sw");
        definitionKeeper.add(
            new MethodDefinition(
                null,
                null,
                null,
                null,
                integerRef,
                "upto()",
                EnumSet.noneOf(MethodDefinition.Modifier.class),
                Collections.emptyList(),
                null,
                ExpressionResultString.UNDEFINED,
                new ExpressionResultString(integerRef)));

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final AbstractType resultType = result.get(0, null);
        assertThat(resultType.getTypeString()).isEqualTo(integerRef);
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
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final TypeString integerRef = TypeString.ofIdentifier("integer", "sw");
        definitionKeeper.add(
            new MethodDefinition(
                null,
                null,
                null,
                null,
                integerRef,
                "upto()",
                EnumSet.noneOf(MethodDefinition.Modifier.class),
                Collections.emptyList(),
                null,
                ExpressionResultString.UNDEFINED,
                new ExpressionResultString(integerRef)));

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final AbstractType resultType = result.get(0, null);
        assertThat(resultType.getTypeString()).isEqualTo(integerRef);
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
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final TypeString integerRef = TypeString.ofIdentifier("integer", "sw");
        definitionKeeper.add(
            new MethodDefinition(
                null,
                null,
                null,
                null,
                integerRef,
                "upto()",
                EnumSet.noneOf(MethodDefinition.Modifier.class),
                Collections.emptyList(),
                null,
                ExpressionResultString.UNDEFINED,
                new ExpressionResultString(integerRef)));

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
        final ITypeKeeper typeKeeper = magikFile.getTypeKeeper();
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final CombinedType expectedType = new CombinedType(
            typeKeeper.getType(TypeString.ofIdentifier("integer", "sw")),
            typeKeeper.getType(TypeString.ofIdentifier("unset", "sw")));
        final CombinedType resultType = (CombinedType) result.get(0, null);
        assertThat(resultType).isEqualTo(expectedType);
    }

    @Test
    void testUnknownMethodCall() {
        final String code = ""
            + "_method object.test\n"
            + "    _return object.unknown()\n"
            + "_endmethod\n";

        // Set up TypeKeeper/TypeReasoner.
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        // Assert user:object.test type determined.resul
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(methodNode);
        assertThat(result).isEqualTo(ExpressionResult.UNDEFINED);
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
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final ProcedureInstance resultType = (ProcedureInstance) result.get(0, null);
        assertThat(resultType.getProcedureName()).isEqualTo("test");

        final Collection<Method> invokeMethods = resultType.getMethods("invoke()");
        assertThat(invokeMethods).isNotEmpty();
        final TypeString integerRef = TypeString.ofIdentifier("sw:integer", "sw");
        invokeMethods.forEach(procMethod -> {
            final ExpressionResultString procResult = procMethod.getCallResult();
            assertThat(procResult.size()).isEqualTo(1);

            final TypeString procResultTypeString = procResult.get(0, null);
            assertThat(procResultTypeString).isEqualTo(integerRef);
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
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final AbstractType resultType = result.get(0, null);
        final TypeString integerRef = TypeString.ofIdentifier("integer", "sw");
        assertThat(resultType.getTypeString()).isEqualTo(integerRef);
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
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final TypeString propertyListRef = TypeString.ofIdentifier("property_list", "sw");
        definitionKeeper.add(
            new ExemplarDefinition(
                null,
                null,
                null,
                null,
                ExemplarDefinition.Sort.INDEXED,
                propertyListRef,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()));
        definitionKeeper.add(
            new MethodDefinition(
                null,
                null,
                null,
                null,
                propertyListRef,
                "new()",
                EnumSet.noneOf(MethodDefinition.Modifier.class),
                Collections.emptyList(),
                null,
                new ExpressionResultString(TypeString.SELF),
                ExpressionResultString.EMPTY));

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final AbstractType resultType = result.get(0, null);
        assertThat(resultType.getTypeString()).isEqualTo(propertyListRef);
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
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final TypeString objectRef = TypeString.ofIdentifier("object", "sw");
        definitionKeeper.add(
            new MethodDefinition(
                null,
                null,
                null,
                null,
                objectRef,
                "test1",
                EnumSet.noneOf(MethodDefinition.Modifier.class),
                Collections.emptyList(),
                null,
                new ExpressionResultString(TypeString.SELF),
                ExpressionResultString.EMPTY));

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final SelfType resultType = (SelfType) result.get(0, null);
        assertThat(resultType).isEqualTo(SelfType.INSTANCE);
    }

    @Test
    void testSelfNewInit() {
        final String code = ""
            + "_method object.test\n"
            + "    _return rope.new()\n"
            + "_endmethod\n";

        // Set up TypeKeeper/TypeReasoner.
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
                Collections.emptyList()));
        definitionKeeper.add(
            new MethodDefinition(
                null,
                null,
                null,
                null,
                ropeRef,
                "new()",
                EnumSet.noneOf(MethodDefinition.Modifier.class),
                Collections.emptyList(),
                null,
                new ExpressionResultString(TypeString.SELF),
                ExpressionResultString.EMPTY));

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final AbstractType resultType = result.get(0, null);
        assertThat(resultType.getTypeString()).isEqualTo(ropeRef);
    }
    // endregion

    @Test
    void testExpressionTypeAnnotation() throws IOException {
        final String code = ""
            + "_method object.test\n"
            + "    _return :a  # type: sw:integer\n"
            + "_endmethod\n";

        // Set up TypeKeeper/TypeReasoner.
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final AbstractType resultType = result.get(0, null);
        final TypeString integerRef = TypeString.ofIdentifier("integer", "sw");
        assertThat(resultType.getTypeString()).isEqualTo(integerRef);
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
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode forVariablesNode = topNode.getFirstDescendant(MagikGrammar.FOR_VARIABLES);

        final AstNode aNode = forVariablesNode.getDescendants(MagikGrammar.IDENTIFIER).get(0);
        final ExpressionResult aResult = reasoner.getNodeType(aNode);
        assertThat(aResult.size()).isEqualTo(1);
        final AbstractType aResultType = aResult.get(0, null);
        final TypeString integerRef = TypeString.ofIdentifier("integer", "sw");
        assertThat(aResultType.getTypeString()).isEqualTo(integerRef);

        final AstNode bNode = forVariablesNode.getDescendants(MagikGrammar.IDENTIFIER).get(1);
        final ExpressionResult bResult = reasoner.getNodeType(bNode);
        assertThat(bResult.size()).isEqualTo(1);
        final AbstractType bResultType = bResult.get(0, null);
        final TypeString floatRef = TypeString.ofIdentifier("float", "sw");
        assertThat(bResultType.getTypeString()).isEqualTo(floatRef);
    }

    @Test
    void testEmptyLocalDefinition() {
        final String code = ""
            + "_method object.test\n"
            + "    _local a\n"
            + "    _return a\n"
            + "_endmethod\n";

        // Set up TypeKeeper/TypeReasoner.
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final AbstractType resultType = result.get(0, null);
        final TypeString unsetRef = TypeString.ofIdentifier("unset", "sw");
        assertThat(resultType.getTypeString()).isEqualTo(unsetRef);
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
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final TypeString integerRef = TypeString.ofIdentifier("integer", "sw");
        final TypeString floatRef = TypeString.ofIdentifier("float", "sw");
        definitionKeeper.add(
            new BinaryOperatorDefinition(
                null,
                null,
                code,
                null,
                "+",
                integerRef,
                floatRef,
                floatRef));

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final AbstractType resultType = result.get(0, null);
        assertThat(resultType.getTypeString()).isEqualTo(floatRef);
    }

    @Test
    void testGatherParameters() {
        final String code = ""
            + "_method object.test(_gather args)\n"
            + "_endmethod\n";

        // Set up TypeKeeper/TypeReasoner.
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode parameterNode = topNode
            .getFirstDescendant(MagikGrammar.PARAMETER)
            .getFirstChild(MagikGrammar.IDENTIFIER);
        final ExpressionResult result = reasoner.getNodeType(parameterNode);

        final AbstractType actualResultType = result.get(0, null);
        final TypeString simpleVectorRef = TypeString.ofIdentifier("simple_vector", "sw");
        assertThat(actualResultType.getTypeString()).isEqualTo(simpleVectorRef);
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
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode procNode = topNode.getFirstDescendant(MagikGrammar.PROCEDURE_DEFINITION);
        final AstNode importNode = procNode.getFirstDescendant(MagikGrammar.VARIABLE_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(importNode);

        final TypeString integerRef = TypeString.ofIdentifier("integer", "sw");
        final AbstractType resultType = result.get(0, null);
        assertThat(resultType.getTypeString()).isEqualTo(integerRef);
    }

    @Test
    void testAssignmentChain() {
        final String code = ""
            + "_method object.test\n"
            + "    _local x << y << 10\n"
            + "    _return x, y\n"
            + "_endmethod\n";

        // Set up TypeKeeper/TypeReasoner.
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(2);

        final AbstractType resultType1 = result.get(0, null);
        final TypeString integerRef = TypeString.ofIdentifier("integer", "sw");
        assertThat(resultType1.getTypeString()).isEqualTo(integerRef);

        final AbstractType resultType2 = result.get(1, null);
        assertThat(resultType2.getTypeString()).isEqualTo(integerRef);
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
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        final AstNode topNode = magikFile.getTopNode();
        final AstNode tryVarNode = topNode.getFirstDescendant(MagikGrammar.TRY_VARIABLE);
        final ExpressionResult result = reasoner.getNodeType(tryVarNode);
        assertThat(result).isNotNull();

        final AbstractType resultType = result.get(0, null);
        final TypeString conditionRef = TypeString.ofIdentifier("condition", "sw");
        assertThat(resultType.getTypeString()).isEqualTo(conditionRef);
    }

    @Test
    void testMergeReturnTypes() {
        final String code = ""
            + "_method e1.m1\n"
            + "  _return 1\n"
            + "  _return x()\n"
            + "_endmethod";

        // Set up TypeKeeper/TypeReasoner.
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
        final ITypeKeeper typeKeeper = magikFile.getTypeKeeper();
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodDefNode = topNode.getFirstDescendant(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(methodDefNode);
        assertThat(result).isNotNull();

        // First is integer + undefined.
        final TypeString integerRef = TypeString.ofIdentifier("integer", "sw");
        final AbstractType intAndUndefinedType = new CombinedType(
            typeKeeper.getType(integerRef),
            UndefinedType.INSTANCE);
        final AbstractType resultType0 = result.get(0, null);
        assertThat(resultType0).isEqualTo(intAndUndefinedType);

        // The rest is unset + undefined, as merging the two yields unset for the rest.
        final TypeString unsetRef = TypeString.ofIdentifier("unset", "sw");
        final AbstractType unsetAndUndefinedType = new CombinedType(
            typeKeeper.getType(unsetRef),
            UndefinedType.INSTANCE);
        result.getTypes().stream()
            .skip(1)
            .forEach(type -> {
                assertThat(type).isEqualTo(unsetAndUndefinedType);
            });
    }

    @Test
    void testSingleSuperType() {
        final String code = ""
            + "_method t.m\n"
            + "  _super.m\n"
            + "_endmethod";

        // Set up TypeKeeper/TypeReasoner.
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final TypeString sRef = TypeString.ofIdentifier("s", "sw");
        definitionKeeper.add(
            new ExemplarDefinition(
                null,
                null,
                null,
                null,
                ExemplarDefinition.Sort.SLOTTED,
                sRef,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()));
        final TypeString tRef = TypeString.ofIdentifier("t", "sw");
        definitionKeeper.add(
            new ExemplarDefinition(
                null,
                null,
                null,
                null,
                ExemplarDefinition.Sort.SLOTTED,
                tRef,
                Collections.emptyList(),
                List.of(sRef),
                Collections.emptyList()));

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        final AstNode topNode = magikFile.getTopNode();
        final AstNode superNode = topNode.getFirstDescendant(MagikGrammar.SUPER).getParent();
        final ExpressionResult result = reasoner.getNodeType(superNode);
        assertThat(result).isNotNull();

        final AbstractType resultType0 = result.get(0, null);
        assertThat(resultType0.getTypeString()).isEqualTo(sRef);
    }

    @Test
    void testSingleNamedSuperType() {
        final String code = ""
            + "_method t.m\n"
            + "  _super(r).m\n"
            + "_endmethod";

        // Set up TypeKeeper/TypeReasoner.
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final TypeString rRef = TypeString.ofIdentifier("r", "sw");
        definitionKeeper.add(
            new ExemplarDefinition(
                null,
                null,
                null,
                null,
                ExemplarDefinition.Sort.SLOTTED,
                rRef,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()));

        final TypeString tRef = TypeString.ofIdentifier("t", "sw");
        definitionKeeper.add(
            new ExemplarDefinition(
                null,
                null,
                null,
                null,
                ExemplarDefinition.Sort.SLOTTED,
                tRef,
                Collections.emptyList(),
                List.of(rRef),
                Collections.emptyList()));

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        final AstNode topNode = magikFile.getTopNode();
        final AstNode superNode = topNode.getFirstDescendant(MagikGrammar.SUPER).getParent();
        final ExpressionResult result = reasoner.getNodeType(superNode);
        assertThat(result).isNotNull();

        final AbstractType resultType0 = result.get(0, null);
        assertThat(resultType0.getTypeString()).isEqualTo(rRef);
    }

    @Test
    void testParameterType() {
        final String code = ""
            + "_method a.b(p1)\n"
            + "  ## @param {sw:float} p1\n"
            + "  show(p1)"
            + "_endmethod";

        // Set up TypeKeeper/TypeReasoner.
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        final TypeString floatRef = TypeString.ofIdentifier("float", "sw");
        final AstNode topNode = magikFile.getTopNode();

        // Test parameter definition.
        final AstNode parameterNode = topNode
            .getFirstDescendant(MagikGrammar.PARAMETER)
            .getFirstChild(MagikGrammar.IDENTIFIER);
        final ExpressionResult actualParameterResult = reasoner.getNodeType(parameterNode);
        final AbstractType actualParameterType = actualParameterResult.get(0, null);
        assertThat(actualParameterType.getTypeString()).isEqualTo(floatRef);

        // Test parameter usage.
        final AstNode procInvocationNode = topNode.getFirstDescendant(MagikGrammar.PROCEDURE_INVOCATION);
        final AstNode atomNode = procInvocationNode.getFirstDescendant(MagikGrammar.ATOM);
        final ExpressionResult actualAtomResult = reasoner.getNodeType(atomNode);
        final AbstractType actualAtomType = actualAtomResult.get(0, null);
        assertThat(actualAtomType.getTypeString()).isEqualTo(floatRef);
    }

    @Test
    void testAssignmentParameterType() {
        final String code = ""
            + "_method a.b << p1\n"
            + "  ## @param {sw:float} p1\n"
            + "  show(p1)"
            + "_endmethod";

        // Set up TypeKeeper/TypeReasoner.
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        final TypeString floatRef = TypeString.ofIdentifier("float", "sw");
        final AstNode topNode = magikFile.getTopNode();

        // Test parameter definition.
        final AstNode parameterNode = topNode
            .getFirstDescendant(MagikGrammar.PARAMETER)
            .getFirstChild(MagikGrammar.IDENTIFIER);
        final ExpressionResult actualParameterResult = reasoner.getNodeType(parameterNode);
        final AbstractType actualParameterType = actualParameterResult.get(0, null);
        assertThat(actualParameterType.getTypeString()).isEqualTo(floatRef);

        // Test parameter usage.
        final AstNode procInvocationNode = topNode.getFirstDescendant(MagikGrammar.PROCEDURE_INVOCATION);
        final AstNode atomNode = procInvocationNode.getFirstDescendant(MagikGrammar.ATOM);
        final ExpressionResult actualAtomResult = reasoner.getNodeType(atomNode);
        final AbstractType actualAtomType = actualAtomResult.get(0, null);
        assertThat(actualAtomType.getTypeString()).isEqualTo(floatRef);
    }

    @Test
    void testParameterTypeCombined() {
        final String code = ""
            + "_method a.b(p1)\n"
            + "  ## @param {sw:float|sw:integer} p1\n"
            + "_endmethod";

        // Set up TypeKeeper/TypeReasoner.
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        final AstNode topNode = magikFile.getTopNode();
        final AstNode parameterNode = topNode
            .getFirstDescendant(MagikGrammar.PARAMETER)
            .getFirstChild(MagikGrammar.IDENTIFIER);
        final ExpressionResult result = reasoner.getNodeType(parameterNode);
        final ITypeKeeper typeKeeper = magikFile.getTypeKeeper();
        final TypeString floatRef = TypeString.ofIdentifier("float", "sw");
        final AbstractType floatType = typeKeeper.getType(floatRef);
        final TypeString integerRef = TypeString.ofIdentifier("integer", "sw");
        final AbstractType integerType = typeKeeper.getType(integerRef);
        final AbstractType expectedType = CombinedType.combine(floatType, integerType);
        final AbstractType actualType = result.get(0, null);
        assertThat(actualType).isEqualTo(expectedType);
    }

    @Test
    void testParameterReferenceUsage() {
        final String code = ""
            + "_method a.b\n"
            + "  _return _self.returns_param(10)\n"
            + "_endmethod";

        // Set up TypeKeeper/TypeReasoner.
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final TypeString aRef = TypeString.ofIdentifier("a", "user");
        definitionKeeper.add(
            new ExemplarDefinition(
                null,
                null,
                null,
                null,
                ExemplarDefinition.Sort.SLOTTED,
                aRef,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()));
        final TypeString param1Ref = TypeString.ofParameterRef("p1");
        definitionKeeper.add(
            new MethodDefinition(
                null,
                null,
                null,
                null,
                aRef,
                "returns_param()",
                EnumSet.noneOf(MethodDefinition.Modifier.class),
                List.of(
                    new ParameterDefinition(
                        null,
                        null,
                        null,
                        null,
                        "p1",
                        ParameterDefinition.Modifier.NONE,
                        param1Ref)),
                null,
                new ExpressionResultString(param1Ref),
                ExpressionResultString.EMPTY));

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodDefinitionNode = topNode.getFirstDescendant(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(methodDefinitionNode);
        final AbstractType actualType = result.get(0, null);
        final TypeString integerRef = TypeString.ofIdentifier("integer", "sw");
        assertThat(actualType.getTypeString()).isEqualTo(integerRef);
    }

    @Test
    void testParameterReferenceNested() {
        final String code = ""
            + "_method a.b\n"
            + "  _return _self.returns_param(_self.returns_param2(10))\n"
            + "_endmethod";

        // Set up TypeKeeper/TypeReasoner.
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final TypeString aRef = TypeString.ofIdentifier("a", "user");
        definitionKeeper.add(
            new ExemplarDefinition(
                null,
                null,
                null,
                null,
                ExemplarDefinition.Sort.SLOTTED,
                aRef,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()));
        final TypeString param1Ref = TypeString.ofParameterRef("p1");
        definitionKeeper.add(
            new MethodDefinition(
                null,
                null,
                null,
                null,
                aRef,
                "returns_param()",
                EnumSet.noneOf(MethodDefinition.Modifier.class),
                List.of(
                    new ParameterDefinition(
                        null,
                        null,
                        null,
                        null,
                        "p1",
                        ParameterDefinition.Modifier.NONE,
                        param1Ref)),
                null,
                new ExpressionResultString(param1Ref),
                ExpressionResultString.EMPTY));
        definitionKeeper.add(
            new MethodDefinition(
                null,
                null,
                null,
                null,
                aRef,
                "returns_param2()",
                EnumSet.noneOf(MethodDefinition.Modifier.class),
                List.of(
                    new ParameterDefinition(
                        null,
                        null,
                        null,
                        null,
                        "p1",
                        ParameterDefinition.Modifier.NONE,
                        param1Ref)),
                null,
                new ExpressionResultString(param1Ref),
                ExpressionResultString.EMPTY));

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodDefinitionNode = topNode.getFirstDescendant(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(methodDefinitionNode);
        final AbstractType actualType = result.get(0, null);
        final TypeString integerRef = TypeString.ofIdentifier("integer", "sw");
        assertThat(actualType.getTypeString()).isEqualTo(integerRef);
    }

    @Test
    void testParameterReferenceOptional() {
        final String code = ""
            + "_method a.b\n"
            + "  _return _self.returns_param()\n"
            + "_endmethod";

        // Set up TypeKeeper/TypeReasoner.
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final TypeString aRef = TypeString.ofIdentifier("a", "user");
        definitionKeeper.add(
            new ExemplarDefinition(
                null,
                null,
                null,
                null,
                ExemplarDefinition.Sort.SLOTTED,
                aRef,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()));
        final TypeString param1Ref = TypeString.ofParameterRef("p1");
        definitionKeeper.add(
            new MethodDefinition(
                null,
                null,
                null,
                null,
                aRef,
                "returns_param()",
                EnumSet.noneOf(MethodDefinition.Modifier.class),
                List.of(
                    new ParameterDefinition(
                        null,
                        null,
                        null,
                        null,
                        "p1",
                        ParameterDefinition.Modifier.NONE,
                        param1Ref)),
                null,
                new ExpressionResultString(param1Ref),
                ExpressionResultString.EMPTY));

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodDefinitionNode = topNode.getFirstDescendant(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasoner.getNodeType(methodDefinitionNode);
        final AbstractType actualType = result.get(0, null);
        final TypeString unsetRef = TypeString.ofIdentifier("unset", "sw");
        assertThat(actualType.getTypeString()).isEqualTo(unsetRef);
    }

    // @Test
    // void testGenericMethodInvocation1() {
    //     final String code = ""
    //         + "_method object.m\n"
    //         + "  _local r << sw:rope.new()  # type: sw:rope<sw:integer>\n"
    //         + "  _return r.an_element()\n"
    //         + "_endmethod\n";

    //     // Set up TypeKeeper/TypeReasoner.
    //     final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    //     final TypeString ropeRef = TypeString.ofIdentifier("rope", "sw");
    //     final MagikType ropeType = new MagikType(typeKeeper, null, null, Sort.SLOTTED, ropeRef);
    //     typeKeeper.addType(ropeType);
    //     ropeType.addGeneric(null, "E");
    //     ropeType.addMethod(
    //         null,
    //         null,
    //         EnumSet.noneOf(Method.Modifier.class),
    //         "new()",
    //         Collections.emptyList(),
    //         null,
    //         null,
    //         new ExpressionResultString(
    //             TypeString.SELF),
    //         ExpressionResultString.EMPTY);
    //     ropeType.addMethod(
    //         null,
    //         null,
    //         EnumSet.noneOf(Method.Modifier.class),
    //         "an_element()",
    //         Collections.emptyList(),
    //         null,
    //         null,
    //         new ExpressionResultString(
    //             TypeString.ofGeneric("E")),  // Possibly also `sw:unset`, but for testing purposes...
    //         ExpressionResultString.EMPTY);

    //     // Do analysis.
    //     final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
    //     final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

    //     final AstNode topNode = magikFile.getTopNode();
    //     final AstNode methodDefinitionNode = topNode.getFirstDescendant(MagikGrammar.METHOD_DEFINITION);
    //     final ExpressionResult result = reasoner.getNodeType(methodDefinitionNode);
    //     final TypeString integerRef = TypeString.ofIdentifier("integer", "sw");
    //     final AbstractType integerType = typeKeeper.getType(integerRef);
    //     final AbstractType actualType = result.get(0, null);
    //     assertThat(actualType).isEqualTo(integerType);
    // }

    // @Test
    // void testGenericMethodInvocation2() {
    //     final String code = ""
    //         + "_method object.m\n"
    //         + "  _local pl << sw:property_list.new()  # type: sw:property_list<sw:symbol, sw:integer>\n"
    //         + "  _return pl.a_key(), pl.an_element()\n"
    //         + "_endmethod\n";

    //     // Set up TypeKeeper/TypeReasoner.
    //     final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    //     final TypeString propertyListRef = TypeString.ofIdentifier("property_list", "sw");
    //     final MagikType propertyListType = new MagikType(typeKeeper, null, null, Sort.SLOTTED, propertyListRef);
    //     typeKeeper.addType(propertyListType);
    //     propertyListType.addGeneric(null, "K");
    //     propertyListType.addGeneric(null, "E");
    //     propertyListType.addMethod(
    //         null,
    //         null,
    //         EnumSet.noneOf(Method.Modifier.class),
    //         "new()",
    //         Collections.emptyList(),
    //         null,
    //         null,
    //         new ExpressionResultString(
    //             TypeString.SELF),
    //         ExpressionResultString.EMPTY);
    //     propertyListType.addMethod(
    //         null,
    //         null,
    //         EnumSet.noneOf(Method.Modifier.class),
    //         "a_key()",
    //         Collections.emptyList(),
    //         null,
    //         null,
    //         new ExpressionResultString(
    //             TypeString.ofGeneric("K")),  // Possibly also `sw:unset`, but for testing purposes...
    //         ExpressionResultString.EMPTY);
    //     propertyListType.addMethod(
    //         null,
    //         null,
    //         EnumSet.noneOf(Method.Modifier.class),
    //         "an_element()",
    //         Collections.emptyList(),
    //         null,
    //         null,
    //         new ExpressionResultString(
    //             TypeString.ofGeneric("E")),  // Possibly also `sw:unset`, but for testing purposes...
    //         ExpressionResultString.EMPTY);

    //     // Do analysis.
    //     final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
    //     final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

    //     final AstNode topNode = magikFile.getTopNode();
    //     final AstNode methodDefinitionNode = topNode.getFirstDescendant(MagikGrammar.METHOD_DEFINITION);
    //     final ExpressionResult result = reasoner.getNodeType(methodDefinitionNode);

    //     final TypeString symbolRef = TypeString.ofIdentifier("symbol", "sw");
    //     final AbstractType symbolType = typeKeeper.getType(symbolRef);
    //     final AbstractType actualType0 = result.get(0, null);
    //     assertThat(actualType0).isEqualTo(symbolType);

    //     final TypeString integerRef = TypeString.ofIdentifier("integer", "sw");
    //     final AbstractType integerType = typeKeeper.getType(integerRef);
    //     final AbstractType actualType1 = result.get(1, null);
    //     assertThat(actualType1).isEqualTo(integerType);
    // }

    // @Test
    // void testGenericIterMethodInvocation() {
    //     final String code = ""
    //         + "_method object.m\n"
    //         + "  _local pl << sw:property_list.new()  # type: sw:property_list<sw:symbol, sw:integer>\n"
    //         + "  _for k, e _over pl.fast_keys_and_elements()\n"
    //         + "  _loop\n"
    //         + "  _endloop\n"
    //         + "_endmethod\n";

    //     // Set up TypeKeeper/TypeReasoner.
    //     final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    //     final TypeString propertyListRef = TypeString.ofIdentifier("property_list", "sw");
    //     final MagikType propertyListType = new MagikType(typeKeeper, null, null, Sort.SLOTTED, propertyListRef);
    //     typeKeeper.addType(propertyListType);
    //     propertyListType.addGeneric(null, "K");
    //     propertyListType.addGeneric(null, "E");
    //     propertyListType.addMethod(
    //         null,
    //         null,
    //         EnumSet.noneOf(Method.Modifier.class),
    //         "new()",
    //         Collections.emptyList(),
    //         null,
    //         null,
    //         new ExpressionResultString(
    //             TypeString.SELF),
    //         ExpressionResultString.EMPTY);
    //     propertyListType.addMethod(
    //         null,
    //         null,
    //         EnumSet.noneOf(Method.Modifier.class),
    //         "fast_keys_and_elements()",
    //         Collections.emptyList(),
    //         null,
    //         null,
    //         ExpressionResultString.EMPTY,
    //         new ExpressionResultString(
    //             TypeString.ofGeneric("K"),
    //             TypeString.ofGeneric("E")));

    //     // Do analysis.
    //     final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
    //     final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

    //     final AstNode topNode = magikFile.getTopNode();
    //     final AstNode forVariablesNode = topNode.getFirstDescendant(MagikGrammar.FOR_VARIABLES);
    //     final List<AstNode> identifierNodes = forVariablesNode.getDescendants(MagikGrammar.IDENTIFIER);

    //     final AstNode kIdentifierNode = identifierNodes.get(0);
    //     final ExpressionResult kResult = reasoner.getNodeType(kIdentifierNode);
    //     final AbstractType actualType0 = kResult.get(0, null);
    //     final TypeString symbolRef = TypeString.ofIdentifier("symbol", "sw");
    //     final AbstractType symbolType = typeKeeper.getType(symbolRef);
    //     assertThat(actualType0).isEqualTo(symbolType);

    //     final AstNode eIdentifierNode = identifierNodes.get(1);
    //     final ExpressionResult eResult = reasoner.getNodeType(eIdentifierNode);
    //     final TypeString integerRef = TypeString.ofIdentifier("integer", "sw");
    //     final AbstractType integerType = typeKeeper.getType(integerRef);
    //     final AbstractType actualType1 = eResult.get(0, null);
    //     assertThat(actualType1).isEqualTo(integerType);
    // }

    // @Test
    // void testGenericSlot() {
    //     final String code = ""
    //         + "_method exemplar.m\n"
    //         + "  _return .stack.pop()\n"
    //         + "_endmethod\n";

    //     // Set up TypeKeeper/TypeReasoner.
    //     final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();

    //     final TypeString stackRef = TypeString.ofIdentifier("stack", "sw");
    //     final MagikType stackType = new MagikType(typeKeeper, null, null, Sort.SLOTTED, stackRef);
    //     typeKeeper.addType(stackType);
    //     stackType.addGeneric(null, "E");
    //     stackType.addMethod(
    //         null,
    //         null,
    //         EnumSet.noneOf(Method.Modifier.class),
    //         "pop()",
    //         Collections.emptyList(),
    //         null,
    //         null,
    //         new ExpressionResultString(
    //             TypeString.ofGeneric("E")),
    //         ExpressionResultString.EMPTY);

    //     final TypeString exemplarRef = TypeString.ofIdentifier("exemplar", "sw");
    //     final MagikType exemplarType = new MagikType(typeKeeper, null, null, Sort.SLOTTED, exemplarRef);
    //     typeKeeper.addType(exemplarType);
    //     final TypeString slotTypeRef = TypeStringParser.parseTypeString("sw:stack<sw:integer>", "sw");
    //     exemplarType.addSlot(null, "stack", slotTypeRef);

    //     // Do analysis.
    //     final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
    //     final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

    //     final AstNode topNode = magikFile.getTopNode();
    //     final AstNode methodDefinitionNode = topNode.getFirstDescendant(MagikGrammar.METHOD_DEFINITION);
    //     final ExpressionResult result = reasoner.getNodeType(methodDefinitionNode);
    //     final TypeString integerRef = TypeString.ofIdentifier("integer", "sw");
    //     final AbstractType integerType = typeKeeper.getType(integerRef);
    //     final AbstractType actualType = result.get(0, null);
    //     assertThat(actualType).isEqualTo(integerType);
    // }

}
