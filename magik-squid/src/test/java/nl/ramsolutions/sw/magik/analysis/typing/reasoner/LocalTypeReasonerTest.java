package nl.ramsolutions.sw.magik.analysis.typing.reasoner;

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
import nl.ramsolutions.sw.magik.analysis.definitions.SlotDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;
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
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasonerState.getNodeType(methodNode);
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
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasonerState.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final AbstractType integerType = typeKeeper.getType(TypeString.SW_INTEGER);
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
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasonerState.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final AbstractType combinedType = new CombinedType(
            typeKeeper.getType(TypeString.SW_INTEGER),
            typeKeeper.getType(TypeString.SW_SYMBOL));
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
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasonerState.getNodeType(methodNode);
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
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasonerState.getNodeType(methodNode);
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
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        // Assert user:object.test call result determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasonerState.getNodeType(methodNode);
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
        definitionKeeper.add(
            new MethodDefinition(
                null,
                null,
                null,
                null,
                TypeString.SW_FALSE,
                "not",
                EnumSet.noneOf(MethodDefinition.Modifier.class),
                Collections.emptyList(),
                null,
                new ExpressionResultString(TypeString.SELF),
                ExpressionResultString.EMPTY));

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasonerState.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final AbstractType resultType = result.get(0, null);
        assertThat(resultType.getTypeString()).isEqualTo(TypeString.SW_FALSE);
    }

    @Test
    void testBinaryOperator() {
        final String code = ""
            + "_method object.test\n"
            + "    _return 'a' + 'b'\n"
            + "_endmethod\n";

        // Set up TypeKeeper/TypeReasoner.
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        definitionKeeper.add(
            new BinaryOperatorDefinition(
                null,
                null,
                code,
                null,
                "+",
                TypeString.SW_CHAR16_VECTOR,
                TypeString.SW_CHAR16_VECTOR,
                TypeString.SW_CHAR16_VECTOR));

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasonerState.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final AbstractType resultType = result.get(0, null);
        assertThat(resultType.getTypeString()).isEqualTo(TypeString.SW_CHAR16_VECTOR);
    }

    @Test
    void testBinaryOperatorChained() {
        final String code = ""
            + "_method object.test\n"
            + "    _return 1 + :a + 'a'\n"
            + "_endmethod\n";

        // Set up TypeKeeper/TypeReasoner.
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        definitionKeeper.add(
            new BinaryOperatorDefinition(
                null,
                null,
                code,
                null,
                "+",
                TypeString.SW_INTEGER,
                TypeString.SW_SYMBOL,
                TypeString.SW_SYMBOL));
        definitionKeeper.add(
            new BinaryOperatorDefinition(
                null,
                null,
                code,
                null,
                "+",
                TypeString.SW_SYMBOL,
                TypeString.SW_CHAR16_VECTOR,
                TypeString.SW_CHAR16_VECTOR));

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasonerState.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final AbstractType resultType = result.get(0, null);
        assertThat(resultType.getTypeString()).isEqualTo(TypeString.SW_CHAR16_VECTOR);
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
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasonerState.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(3);

        final AbstractType resultType1 = result.get(0, null);
        assertThat(resultType1.getTypeString()).isEqualTo(TypeString.SW_INTEGER);

        final AbstractType resultType2 = result.get(1, null);
        assertThat(resultType2.getTypeString()).isEqualTo(TypeString.SW_INTEGER);

        final AbstractType resultType3 = result.get(2, null);
        assertThat(resultType3.getTypeString()).isEqualTo(TypeString.SW_INTEGER);
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
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasonerState.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final AbstractType resultType = result.get(0, null);
        assertThat(resultType.getTypeString()).isEqualTo(TypeString.SW_INTEGER);
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
        definitionKeeper.add(
            new MethodDefinition(
                null,
                null,
                null,
                null,
                TypeString.SW_INTEGER,
                "upto()",
                EnumSet.noneOf(MethodDefinition.Modifier.class),
                Collections.emptyList(),
                null,
                ExpressionResultString.UNDEFINED,
                new ExpressionResultString(TypeString.SW_INTEGER)));

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasonerState.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final AbstractType resultType = result.get(0, null);
        assertThat(resultType.getTypeString()).isEqualTo(TypeString.SW_INTEGER);
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
        definitionKeeper.add(
            new MethodDefinition(
                null,
                null,
                null,
                null,
                TypeString.SW_INTEGER,
                "upto()",
                EnumSet.noneOf(MethodDefinition.Modifier.class),
                Collections.emptyList(),
                null,
                ExpressionResultString.UNDEFINED,
                new ExpressionResultString(TypeString.SW_INTEGER)));

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasonerState.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final AbstractType resultType = result.get(0, null);
        assertThat(resultType.getTypeString()).isEqualTo(TypeString.SW_INTEGER);
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
        definitionKeeper.add(
            new MethodDefinition(
                null,
                null,
                null,
                null,
                TypeString.SW_INTEGER,
                "upto()",
                EnumSet.noneOf(MethodDefinition.Modifier.class),
                Collections.emptyList(),
                null,
                ExpressionResultString.UNDEFINED,
                new ExpressionResultString(TypeString.SW_INTEGER)));

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
        final ITypeKeeper typeKeeper = magikFile.getTypeKeeper();
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasonerState.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final CombinedType expectedType = new CombinedType(
            typeKeeper.getType(TypeString.SW_INTEGER),
            typeKeeper.getType(TypeString.SW_UNSET));
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
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        // Assert user:object.test type determined.resul
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasonerState.getNodeType(methodNode);
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
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasonerState.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final ProcedureInstance resultType = (ProcedureInstance) result.get(0, null);
        assertThat(resultType.getProcedureName()).isEqualTo("test");

        final Collection<Method> invokeMethods = resultType.getMethods("invoke()");
        assertThat(invokeMethods).isNotEmpty();
        invokeMethods.forEach(procMethod -> {
            final ExpressionResultString procResult = procMethod.getCallResult();
            assertThat(procResult.size()).isEqualTo(1);

            final TypeString procResultTypeString = procResult.get(0, null);
            assertThat(procResultTypeString).isEqualTo(TypeString.SW_INTEGER);
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
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasonerState.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final AbstractType resultType = result.get(0, null);
        assertThat(resultType.getTypeString()).isEqualTo(TypeString.SW_INTEGER);
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
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasonerState.getNodeType(methodNode);
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
        definitionKeeper.add(
            new MethodDefinition(
                null,
                null,
                null,
                null,
                TypeString.SW_OBJECT,
                "test1",
                EnumSet.noneOf(MethodDefinition.Modifier.class),
                Collections.emptyList(),
                null,
                new ExpressionResultString(TypeString.SELF),
                ExpressionResultString.EMPTY));

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasonerState.getNodeType(methodNode);
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
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasonerState.getNodeType(methodNode);
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
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasonerState.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final AbstractType resultType = result.get(0, null);
        assertThat(resultType.getTypeString()).isEqualTo(TypeString.SW_INTEGER);
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
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode forVariablesNode = topNode.getFirstDescendant(MagikGrammar.FOR_VARIABLES);

        final AstNode aNode = forVariablesNode.getDescendants(MagikGrammar.IDENTIFIER).get(0);
        final ExpressionResult aResult = reasonerState.getNodeType(aNode);
        assertThat(aResult.size()).isEqualTo(1);
        final AbstractType aResultType = aResult.get(0, null);
        assertThat(aResultType.getTypeString()).isEqualTo(TypeString.SW_INTEGER);

        final AstNode bNode = forVariablesNode.getDescendants(MagikGrammar.IDENTIFIER).get(1);
        final ExpressionResult bResult = reasonerState.getNodeType(bNode);
        assertThat(bResult.size()).isEqualTo(1);
        final AbstractType bResultType = bResult.get(0, null);
        assertThat(bResultType.getTypeString()).isEqualTo(TypeString.SW_FLOAT);
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
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasonerState.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final AbstractType resultType = result.get(0, null);
        final TypeString unsetRef = TypeString.SW_UNSET;
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
        definitionKeeper.add(
            new BinaryOperatorDefinition(
                null,
                null,
                code,
                null,
                "+",
                TypeString.SW_INTEGER,
                TypeString.SW_FLOAT,
                TypeString.SW_FLOAT));

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasonerState.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(1);

        final AbstractType resultType = result.get(0, null);
        assertThat(resultType.getTypeString()).isEqualTo(TypeString.SW_FLOAT);
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
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode parameterNode = topNode
            .getFirstDescendant(MagikGrammar.PARAMETER)
            .getFirstChild(MagikGrammar.IDENTIFIER);
        final ExpressionResult result = reasonerState.getNodeType(parameterNode);

        final AbstractType actualResultType = result.get(0, null);
        final TypeString simpleVectorRef = TypeString.ofIdentifier(
            "simple_vector", "sw",
            TypeString.ofGenericDefinition("E", TypeString.UNDEFINED));
        assertThat(actualResultType.getTypeString()).isEqualTo(simpleVectorRef);
    }

    @Test
    void testGatherParametersGeneric() {
        final String code = ""
            + "_method object.test(_gather args)\n"
            + "  ## @param {sw:symbol} args\n"
            + "_endmethod\n";

        // Set up TypeKeeper/TypeReasoner.
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode parameterNode = topNode
            .getFirstDescendant(MagikGrammar.PARAMETER)
            .getFirstChild(MagikGrammar.IDENTIFIER);
        final ExpressionResult result = reasonerState.getNodeType(parameterNode);

        final AbstractType actualResultType = result.get(0, null);
        final TypeString simpleVectorRef = TypeString.ofIdentifier(
            "simple_vector", "sw",
            TypeString.ofGenericDefinition("E", TypeString.SW_SYMBOL));
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
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode procNode = topNode.getFirstDescendant(MagikGrammar.PROCEDURE_DEFINITION);
        final AstNode importNode = procNode.getFirstDescendant(MagikGrammar.VARIABLE_DEFINITION);
        final ExpressionResult result = reasonerState.getNodeType(importNode);

        final AbstractType resultType = result.get(0, null);
        assertThat(resultType.getTypeString()).isEqualTo(TypeString.SW_INTEGER);
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
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        // Assert user:object.test type determined.
        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasonerState.getNodeType(methodNode);
        assertThat(result.size()).isEqualTo(2);

        final AbstractType resultType1 = result.get(0, null);
        assertThat(resultType1.getTypeString()).isEqualTo(TypeString.SW_INTEGER);

        final AbstractType resultType2 = result.get(1, null);
        assertThat(resultType2.getTypeString()).isEqualTo(TypeString.SW_INTEGER);
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
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        final AstNode topNode = magikFile.getTopNode();
        final AstNode tryVarNode = topNode.getFirstDescendant(MagikGrammar.TRY_VARIABLE);
        final ExpressionResult result = reasonerState.getNodeType(tryVarNode);
        assertThat(result).isNotNull();

        final AbstractType resultType = result.get(0, null);
        assertThat(resultType.getTypeString()).isEqualTo(TypeString.SW_CONDITION);
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
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodDefNode = topNode.getFirstDescendant(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasonerState.getNodeType(methodDefNode);
        assertThat(result).isNotNull();

        // First is integer + undefined.
        final AbstractType intAndUndefinedType = new CombinedType(
            typeKeeper.getType(TypeString.SW_INTEGER),
            UndefinedType.INSTANCE);
        final AbstractType resultType0 = result.get(0, null);
        assertThat(resultType0).isEqualTo(intAndUndefinedType);

        // The rest is unset + undefined, as merging the two yields unset for the rest.
        final AbstractType unsetAndUndefinedType = new CombinedType(
            typeKeeper.getType(TypeString.SW_UNSET),
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
                List.of(sRef)));

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        final AstNode topNode = magikFile.getTopNode();
        final AstNode superNode = topNode.getFirstDescendant(MagikGrammar.SUPER).getParent();
        final ExpressionResult result = reasonerState.getNodeType(superNode);
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
                List.of(rRef)));

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        final AstNode topNode = magikFile.getTopNode();
        final AstNode superNode = topNode.getFirstDescendant(MagikGrammar.SUPER).getParent();
        final ExpressionResult result = reasonerState.getNodeType(superNode);
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
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        final AstNode topNode = magikFile.getTopNode();

        // Test parameter definition.
        final AstNode parameterNode = topNode
            .getFirstDescendant(MagikGrammar.PARAMETER)
            .getFirstChild(MagikGrammar.IDENTIFIER);
        final ExpressionResult actualParameterResult = reasonerState.getNodeType(parameterNode);
        final AbstractType actualParameterType = actualParameterResult.get(0, null);
        assertThat(actualParameterType.getTypeString()).isEqualTo(TypeString.SW_FLOAT);

        // Test parameter usage.
        final AstNode procInvocationNode = topNode.getFirstDescendant(MagikGrammar.PROCEDURE_INVOCATION);
        final AstNode atomNode = procInvocationNode.getFirstDescendant(MagikGrammar.ATOM);
        final ExpressionResult actualAtomResult = reasonerState.getNodeType(atomNode);
        final AbstractType actualAtomType = actualAtomResult.get(0, null);
        assertThat(actualAtomType.getTypeString()).isEqualTo(TypeString.SW_FLOAT);
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
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        final AstNode topNode = magikFile.getTopNode();

        // Test parameter definition.
        final AstNode parameterNode = topNode
            .getFirstDescendant(MagikGrammar.PARAMETER)
            .getFirstChild(MagikGrammar.IDENTIFIER);
        final ExpressionResult actualParameterResult = reasonerState.getNodeType(parameterNode);
        final AbstractType actualParameterType = actualParameterResult.get(0, null);
        assertThat(actualParameterType.getTypeString()).isEqualTo(TypeString.SW_FLOAT);

        // Test parameter usage.
        final AstNode procInvocationNode = topNode.getFirstDescendant(MagikGrammar.PROCEDURE_INVOCATION);
        final AstNode atomNode = procInvocationNode.getFirstDescendant(MagikGrammar.ATOM);
        final ExpressionResult actualAtomResult = reasonerState.getNodeType(atomNode);
        final AbstractType actualAtomType = actualAtomResult.get(0, null);
        assertThat(actualAtomType.getTypeString()).isEqualTo(TypeString.SW_FLOAT);
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
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        final AstNode topNode = magikFile.getTopNode();
        final AstNode parameterNode = topNode
            .getFirstDescendant(MagikGrammar.PARAMETER)
            .getFirstChild(MagikGrammar.IDENTIFIER);
        final ExpressionResult result = reasonerState.getNodeType(parameterNode);
        final ITypeKeeper typeKeeper = magikFile.getTypeKeeper();
        final AbstractType floatType = typeKeeper.getType(TypeString.SW_FLOAT);
        final AbstractType integerType = typeKeeper.getType(TypeString.SW_INTEGER);
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
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodDefinitionNode = topNode.getFirstDescendant(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasonerState.getNodeType(methodDefinitionNode);
        final AbstractType actualType = result.get(0, null);
        assertThat(actualType.getTypeString()).isEqualTo(TypeString.SW_INTEGER);
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
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodDefinitionNode = topNode.getFirstDescendant(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasonerState.getNodeType(methodDefinitionNode);
        final AbstractType actualType = result.get(0, null);
        assertThat(actualType.getTypeString()).isEqualTo(TypeString.SW_INTEGER);
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
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodDefinitionNode = topNode.getFirstDescendant(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasonerState.getNodeType(methodDefinitionNode);
        final AbstractType actualType = result.get(0, null);
        assertThat(actualType.getTypeString()).isEqualTo(TypeString.SW_UNSET);
    }

    @Test
    void testGenericMethodInvocation1() {
        final String code = ""
            + "_method object.m\n"
            + "  _local r << sw:rope.new()  # type: sw:rope<E=sw:integer>\n"
            + "  _return r.an_element()\n"
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
                new ExpressionResultString(
                    TypeString.SELF),
                ExpressionResultString.EMPTY));
        definitionKeeper.add(
            new MethodDefinition(
                null,
                null,
                null,
                null,
                ropeRef,
                "an_element()",
                EnumSet.noneOf(MethodDefinition.Modifier.class),
                Collections.emptyList(),
                null,
                new ExpressionResultString(
                    TypeString.ofGenericReference("E")),  // Possibly also `sw:unset`, but for testing purposes...
                ExpressionResultString.EMPTY));

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodDefinitionNode = topNode.getFirstDescendant(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasonerState.getNodeType(methodDefinitionNode);
        final AbstractType actualType = result.get(0, null);
        assertThat(actualType.getTypeString()).isEqualTo(TypeString.SW_INTEGER);
    }

    @Test
    void testGenericMethodInvocation2() {
        final String code = ""
            + "_method object.m\n"
            + "  _local pl << sw:property_list.new()  # type: sw:property_list<K=sw:symbol, E=sw:integer>\n"
            + "  _return pl.a_key(), pl.an_element()\n"
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
                new ExpressionResultString(
                    TypeString.SELF),
                ExpressionResultString.EMPTY));
        definitionKeeper.add(
            new MethodDefinition(
                null,
                null,
                null,
                null,
                propertyListRef,
                "a_key()",
                EnumSet.noneOf(MethodDefinition.Modifier.class),
                Collections.emptyList(),
                null,
                new ExpressionResultString(
                    TypeString.ofGenericReference("K")),  // Possibly also `sw:unset`, but for testing purposes...
                ExpressionResultString.EMPTY));
        definitionKeeper.add(
            new MethodDefinition(
                null,
                null,
                null,
                null,
                propertyListRef,
                "an_element()",
                EnumSet.noneOf(MethodDefinition.Modifier.class),
                Collections.emptyList(),
                null,
                new ExpressionResultString(
                    TypeString.ofGenericReference("E")),  // Possibly also `sw:unset`, but for testing purposes...
                ExpressionResultString.EMPTY));

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodDefinitionNode = topNode.getFirstDescendant(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasonerState.getNodeType(methodDefinitionNode);

        final AbstractType actualType0 = result.get(0, null);
        assertThat(actualType0.getTypeString()).isEqualTo(TypeString.SW_SYMBOL);

        final AbstractType actualType1 = result.get(1, null);
        assertThat(actualType1.getTypeString()).isEqualTo(TypeString.SW_INTEGER);
    }

    @Test
    void testGenericMethodInvocation3() {
        final String code = ""
            + "_method object.m\n"
            + "  _local r << sw:rope.new()  # type: sw:rope<E=sw:integer>\n"
            + "  _return r.as_simple_vector()\n"
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
                new ExpressionResultString(
                    TypeString.SELF),
                ExpressionResultString.EMPTY));
        definitionKeeper.add(
            new MethodDefinition(
                null,
                null,
                null,
                null,
                ropeRef,
                "as_simple_vector()",
                EnumSet.noneOf(MethodDefinition.Modifier.class),
                Collections.emptyList(),
                null,
                new ExpressionResultString(
                    TypeString.ofIdentifier("simple_vector", "sw",
                        TypeString.ofGenericReference("E"))),
                ExpressionResultString.EMPTY));

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodDefinitionNode = topNode.getFirstDescendant(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasonerState.getNodeType(methodDefinitionNode);
        final AbstractType actualType = result.get(0, null);
        final TypeString integerRef = TypeString.ofIdentifier("integer", "sw");
        final TypeString simpleVectorRefWithGeneric =
            TypeString.ofIdentifier("simple_vector", "sw",
                TypeString.ofGenericDefinition("E", integerRef));
        assertThat(actualType.getTypeString()).isEqualTo(simpleVectorRefWithGeneric);
    }

    @Test
    void testGenericIterMethodInvocation() {
        final String code = ""
            + "_method object.m\n"
            + "  _local pl << sw:property_list.new()  # type: sw:property_list<K=sw:symbol, E=sw:integer>\n"
            + "  _for k, e _over pl.fast_keys_and_elements()\n"
            + "  _loop\n"
            + "  _endloop\n"
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
                new ExpressionResultString(
                    TypeString.SELF),
                ExpressionResultString.EMPTY));
        definitionKeeper.add(
            new MethodDefinition(
                null,
                null,
                null,
                null,
                propertyListRef,
                "fast_keys_and_elements()",
                EnumSet.noneOf(MethodDefinition.Modifier.class),
                Collections.emptyList(),
                null,
                ExpressionResultString.EMPTY,
                new ExpressionResultString(
                    TypeString.ofGenericReference("K"),
                    TypeString.ofGenericReference("E"))));

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        final AstNode topNode = magikFile.getTopNode();
        final AstNode forVariablesNode = topNode.getFirstDescendant(MagikGrammar.FOR_VARIABLES);
        final List<AstNode> identifierNodes = forVariablesNode.getDescendants(MagikGrammar.IDENTIFIER);

        final AstNode kIdentifierNode = identifierNodes.get(0);
        final ExpressionResult kResult = reasonerState.getNodeType(kIdentifierNode);
        final AbstractType actualType0 = kResult.get(0, null);
        assertThat(actualType0.getTypeString()).isEqualTo(TypeString.SW_SYMBOL);

        final AstNode eIdentifierNode = identifierNodes.get(1);
        final ExpressionResult eResult = reasonerState.getNodeType(eIdentifierNode);
        final AbstractType actualType1 = eResult.get(0, null);
        assertThat(actualType1.getTypeString()).isEqualTo(TypeString.SW_INTEGER);
    }

    @Test
    void testGenericSlot() {
        final String code = ""
            + "_method exemplar.m\n"
            + "  _return .stack.pop()\n"
            + "_endmethod\n";

        // Set up TypeKeeper/TypeReasoner.
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();

        final TypeString stackRef = TypeString.ofIdentifier("stack", "sw");
        definitionKeeper.add(
            new ExemplarDefinition(
                null,
                null,
                null,
                null,
                ExemplarDefinition.Sort.SLOTTED,
                stackRef,
                Collections.emptyList(),
                Collections.emptyList()));
        definitionKeeper.add(
            new MethodDefinition(
                null,
                null,
                null,
                null,
                stackRef,
                "pop()",
                EnumSet.noneOf(MethodDefinition.Modifier.class),
                Collections.emptyList(),
                null,
                new ExpressionResultString(
                    TypeString.ofGenericReference("E")),  // Possibly also `sw:unset`, but for testing purposes...
                ExpressionResultString.EMPTY));

        final TypeString exemplarRef = TypeString.ofIdentifier("exemplar", "sw");
        final TypeString slotTypeRef =
            TypeString.ofIdentifier("stack", "sw",
                TypeString.ofGenericDefinition("E", TypeString.SW_INTEGER));
        definitionKeeper.add(
            new ExemplarDefinition(
                null,
                null,
                null,
                null,
                ExemplarDefinition.Sort.SLOTTED,
                exemplarRef,
                List.of(
                    new SlotDefinition(
                        null,
                        null,
                        null,
                        null,
                        "stack",
                        slotTypeRef)),
                Collections.emptyList()));

        // Do analysis.
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        final AstNode topNode = magikFile.getTopNode();
        final AstNode methodDefinitionNode = topNode.getFirstDescendant(MagikGrammar.METHOD_DEFINITION);
        final ExpressionResult result = reasonerState.getNodeType(methodDefinitionNode);
        final TypeString integerRef = TypeString.ofIdentifier("integer", "sw");
        final AbstractType actualType = result.get(0, null);
        assertThat(actualType.getTypeString()).isEqualTo(integerRef);
    }

}
