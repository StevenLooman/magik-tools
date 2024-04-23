package nl.ramsolutions.sw.magik.analysis.typing.reasoner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThat;

import com.sonar.sslr.api.AstNode;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.definitions.BinaryOperatorDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ITypeStringDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ParameterDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ProcedureDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.SlotDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import org.junit.jupiter.api.Test;

/** Test LocalTypeReasoner. */
@SuppressWarnings("checkstyle:MagicNumber")
class LocalTypeReasonerTest {

  private static final URI DEFAULT_URI = URI.create("memory://source.magik");

  private MagikTypedFile createMagikFile(
      final String code, final IDefinitionKeeper definitionKeeper) {
    return new MagikTypedFile(DEFAULT_URI, code, definitionKeeper);
  }

  @Test
  void testReasonMethodReturnNone() {
    final String code =
        """
        _package sw
        _method object.test
            _return
        _endmethod
        """;

    // Set up.
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();

    // Do analysis.
    final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
    final LocalTypeReasonerState state = magikFile.getTypeReasonerState();

    // Assert user:object.test type determined.
    final AstNode topNode = magikFile.getTopNode();
    final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
    final ExpressionResultString result = state.getNodeType(methodNode);
    assertThat(result).isEqualTo(ExpressionResultString.EMPTY);
  }

  @Test
  void testReasonMethodReturnRope() {
    final String code =
        """
        _package sw
        _method object.test
            _return rope.new()
        _endmethod
        """;

    // Set up.
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
            Collections.emptySet()));
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
            Collections.emptySet(),
            new ExpressionResultString(TypeString.SELF),
            ExpressionResultString.EMPTY));

    // Do analysis.
    final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
    final LocalTypeReasonerState state = magikFile.getTypeReasonerState();

    // Assert user:object.test type determined.
    final AstNode topNode = magikFile.getTopNode();
    final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
    final ExpressionResultString result = state.getNodeType(methodNode);
    assertThat(result).isEqualTo(new ExpressionResultString(ropeRef));
  }

  @Test
  void testReasonMethodMultiReturnSame() {
    final String code =
        """
        _package sw
        _method object.test
            _return 1
            _return 2
        _endmethod
        """;

    // Set up.
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();

    // Do analysis.
    final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
    final LocalTypeReasonerState state = magikFile.getTypeReasonerState();

    // Assert user:object.test type determined.
    final AstNode topNode = magikFile.getTopNode();
    final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
    final ExpressionResultString result = state.getNodeType(methodNode);
    assertThat(result).isEqualTo(new ExpressionResultString(TypeString.SW_INTEGER));
  }

  @Test
  void testReasonMethodMultiReturnDifferent() {
    final String code =
        """
        _package sw
        _method object.test
            _block
                _return 1
            _endblock
            _return :a
        _endmethod
        """;

    // Set up.
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();

    // Do analysis.
    final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
    final LocalTypeReasonerState state = magikFile.getTypeReasonerState();

    // Assert user:object.test type determined.
    final AstNode topNode = magikFile.getTopNode();
    final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
    final ExpressionResultString result = state.getNodeType(methodNode);
    assertThat(result)
        .isEqualTo(
            new ExpressionResultString(
                TypeString.combine(TypeString.SW_INTEGER, TypeString.SW_SYMBOL)));
  }

  @Test
  void testReasonMethodSelf() {
    final String code =
        """
        _method object.test
            _return _self
        _endmethod
        """;

    // Set up.
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();

    // Do analysis.
    final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
    final LocalTypeReasonerState state = magikFile.getTypeReasonerState();

    // Assert user:object.test type determined.
    final AstNode topNode = magikFile.getTopNode();
    final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
    final ExpressionResultString result = state.getNodeType(methodNode);
    assertThat(result).isEqualTo(new ExpressionResultString(TypeString.SELF));
  }

  @Test
  void testReasonVariable() {
    final String code =
        """
        _method object.test
            _local var << rope.new(1)
            var.add(:a)
            _return var
        _endmethod
        """;

    // Set up.
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
            Collections.emptySet()));
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
            Collections.emptySet(),
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
            Collections.emptySet(),
            ExpressionResultString.UNDEFINED,
            ExpressionResultString.EMPTY));

    // Do analysis.
    final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
    final LocalTypeReasonerState state = magikFile.getTypeReasonerState();

    // Assert user:object.test type determined.
    final AstNode topNode = magikFile.getTopNode();
    final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
    final ExpressionResultString result = state.getNodeType(methodNode);
    assertThat(result).isEqualTo(new ExpressionResultString(ropeRef));
  }

  @Test
  void testReasonVariable2() {
    final String code =
        """
        _method object.test
            _local (var1, var2) << rope.new(1)
            var1.add(:a)
            _return var1
        _endmethod
        """;

    // Set up.
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
            Collections.emptySet()));
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
            Collections.emptySet(),
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
            Collections.emptySet(),
            ExpressionResultString.UNDEFINED,
            ExpressionResultString.EMPTY));

    // Do analysis.
    final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
    final LocalTypeReasonerState state = magikFile.getTypeReasonerState();

    // Assert user:object.test call result determined.
    final AstNode topNode = magikFile.getTopNode();
    final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
    final ExpressionResultString result = state.getNodeType(methodNode);
    assertThat(result).isEqualTo(new ExpressionResultString(ropeRef));
  }

  @Test
  void testUnaryOperator() {
    final String code =
        """
        _method object.test
            _return _not _true
        _endmethod
        """;

    // Set up.
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
            Collections.emptySet(),
            new ExpressionResultString(TypeString.SELF),
            ExpressionResultString.EMPTY));

    // Do analysis.
    final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
    final LocalTypeReasonerState state = magikFile.getTypeReasonerState();

    // Assert user:object.test type determined.
    final AstNode topNode = magikFile.getTopNode();
    final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
    final ExpressionResultString result = state.getNodeType(methodNode);
    assertThat(result).isEqualTo(new ExpressionResultString(TypeString.SW_FALSE));
  }

  @Test
  void testBinaryOperator() {
    final String code =
        """
        _method object.test
            _return 'a' + 'b'
        _endmethod
        """;

    // Set up.
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
    final LocalTypeReasonerState state = magikFile.getTypeReasonerState();

    // Assert user:object.test type determined.
    final AstNode topNode = magikFile.getTopNode();
    final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
    final ExpressionResultString result = state.getNodeType(methodNode);
    assertThat(result).isEqualTo(new ExpressionResultString(TypeString.SW_CHAR16_VECTOR));
  }

  @Test
  void testBinaryOperatorChained() {
    final String code =
        """
        _method object.test
            _return 1 + :a + 'a'
        _endmethod
        """;

    // Set up.
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
    final LocalTypeReasonerState state = magikFile.getTypeReasonerState();

    // Assert user:object.test type determined.
    final AstNode topNode = magikFile.getTopNode();
    final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
    final ExpressionResultString result = state.getNodeType(methodNode);
    assertThat(result).isEqualTo(new ExpressionResultString(TypeString.SW_CHAR16_VECTOR));
  }

  @Test
  void testEmitStatement() {
    final String code =
        """
        _method object.test
            _return _block
                >> 1, 2, 3
            _endblock
        _endmethod
        """;

    // Set up.
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();

    // Do analysis.
    final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
    final LocalTypeReasonerState state = magikFile.getTypeReasonerState();

    // Assert user:object.test type determined.
    final AstNode topNode = magikFile.getTopNode();
    final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
    final ExpressionResultString result = state.getNodeType(methodNode);
    assertThat(result)
        .isEqualTo(
            new ExpressionResultString(
                TypeString.SW_INTEGER, TypeString.SW_INTEGER, TypeString.SW_INTEGER));
  }

  @Test
  void testIfElseStatement() {
    final String code =
        """
        _method object.test
            a << _true
            _return _if a
                    _then
                        >> 1
                    _else
                        >> 2
                    _endif
        _endmethod
        """;

    // Set up.
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();

    // Do analysis.
    final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
    final LocalTypeReasonerState state = magikFile.getTypeReasonerState();

    // Assert user:object.test type determined.
    final AstNode topNode = magikFile.getTopNode();
    final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
    final ExpressionResultString result = state.getNodeType(methodNode);
    assertThat(result).isEqualTo(new ExpressionResultString(TypeString.SW_INTEGER));
  }

  @Test
  void testLoopStatement() {
    final String code =
        """
        _method object.test
            _return _for i _over 1.upto(10)
                    _loop
                        _leave _with 1
                    _endloop
        _endmethod
        """;

    // Set up.
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
            Collections.emptySet(),
            ExpressionResultString.UNDEFINED,
            new ExpressionResultString(TypeString.SW_INTEGER)));

    // Do analysis.
    final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
    final LocalTypeReasonerState state = magikFile.getTypeReasonerState();

    // Assert user:object.test type determined.
    final AstNode topNode = magikFile.getTopNode();
    final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
    final ExpressionResultString result = state.getNodeType(methodNode);
    assertThat(result).isEqualTo(new ExpressionResultString(TypeString.SW_INTEGER));
  }

  @Test
  void testLoopStatementUseIterator() {
    final String code =
        """
        _method object.test
            _return _for i _over 1.upto(10)
                    _loop
                        _leave _with i
                    _endloop
        _endmethod
        """;

    // Set up.
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
            Collections.emptySet(),
            ExpressionResultString.UNDEFINED,
            new ExpressionResultString(TypeString.SW_INTEGER)));

    // Do analysis.
    final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
    final LocalTypeReasonerState state = magikFile.getTypeReasonerState();

    // Assert user:object.test type determined.
    final AstNode topNode = magikFile.getTopNode();
    final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
    final ExpressionResultString result = state.getNodeType(methodNode);
    assertThat(result).isEqualTo(new ExpressionResultString(TypeString.SW_INTEGER));
  }

  @Test
  void testLoopResultOptional() {
    final String code =
        """
        _method object.test
            _return _for i _over 1.upto(10)
                    _loop
                        _if a
                        _then
                            _leave _with i
                        _endif
                    _endloop
        _endmethod
        """;

    // Set up.
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
            Collections.emptySet(),
            ExpressionResultString.UNDEFINED,
            new ExpressionResultString(TypeString.SW_INTEGER)));

    // Do analysis.
    final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
    final LocalTypeReasonerState state = magikFile.getTypeReasonerState();

    // Assert user:object.test type determined.
    final AstNode topNode = magikFile.getTopNode();
    final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
    final ExpressionResultString result = state.getNodeType(methodNode);
    assertThat(result)
        .isEqualTo(
            new ExpressionResultString(
                TypeString.combine(TypeString.SW_INTEGER, TypeString.SW_UNSET)));
  }

  @Test
  void testUnknownMethodCall() {
    final String code =
        """
        _method object.test
            _return object.unknown()
        _endmethod
        """;

    // Set up.
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();

    // Do analysis.
    final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
    final LocalTypeReasonerState state = magikFile.getTypeReasonerState();

    // Assert user:object.test type determined.resul
    final AstNode topNode = magikFile.getTopNode();
    final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
    final ExpressionResultString result = state.getNodeType(methodNode);
    assertThat(result).isEqualTo(ExpressionResultString.UNDEFINED);
  }

  @Test
  void testReasonMethodReturnProc() {
    final String code =
        """
        _method object.test
            _return _proc@test()
                        >> 10
                    _endproc
        _endmethod
        """;

    // Do analysis.
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
    final LocalTypeReasonerState state = magikFile.getTypeReasonerState();

    // Assert user:object.test type determined.
    final AstNode topNode = magikFile.getTopNode();
    final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
    final ExpressionResultString result = state.getNodeType(methodNode);
    assertThat(result)
        .isEqualTo(
            new ExpressionResultString(
                TypeString.ofIdentifier(TypeString.ANONYMOUS_PACKAGE, "_proc_in_memory_0")));

    final TypeString procTypeStr = result.get(0, null);
    final ITypeStringDefinition typeStringDefinition = state.getTypeStringDefinition(procTypeStr);
    assertThat(typeStringDefinition).isExactlyInstanceOf(ProcedureDefinition.class);
    final ProcedureDefinition procDef = (ProcedureDefinition) typeStringDefinition;
    final ExpressionResultString procResult = procDef.getReturnTypes();
    assertThat(procResult).isEqualTo(new ExpressionResultString(TypeString.SW_INTEGER));
  }

  @Test
  void testMultipleAssignmentStatement() {
    final String code =
        """
        _method object.test
            (a, b) << (:a, 3)
            _return b
        _endmethod
        """;

    // Set up.
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();

    // Do analysis.
    final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
    final LocalTypeReasonerState state = magikFile.getTypeReasonerState();

    // Assert user:object.test type determined.
    final AstNode topNode = magikFile.getTopNode();
    final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
    final ExpressionResultString result = state.getNodeType(methodNode);
    assertThat(result).isEqualTo(new ExpressionResultString(TypeString.SW_INTEGER));
  }

  @Test
  void testAssignmentMethod() {
    final String code =
        """
        _method object.test
            _local a << property_list.new()
            a[:1] << 10
            _return a
        _endmethod
        """;

    // Set up.
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
            Collections.emptySet()));
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
            Collections.emptySet(),
            new ExpressionResultString(TypeString.SELF),
            ExpressionResultString.EMPTY));

    // Do analysis.
    final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
    final LocalTypeReasonerState state = magikFile.getTypeReasonerState();

    // Assert user:object.test type determined.
    final AstNode topNode = magikFile.getTopNode();
    final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
    final ExpressionResultString result = state.getNodeType(methodNode);
    assertThat(result).isEqualTo(new ExpressionResultString(propertyListRef));
  }

  // region: self
  @Test
  void testSelf() {
    final String code =
        """
        _package sw
        _method object.test
            _return _self.test1
        _endmethod
        """;

    // Set up.
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
            Collections.emptySet(),
            new ExpressionResultString(TypeString.SELF),
            ExpressionResultString.EMPTY));

    // Do analysis.
    final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
    final LocalTypeReasonerState state = magikFile.getTypeReasonerState();

    // Assert user:object.test type determined.
    final AstNode topNode = magikFile.getTopNode();
    final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
    final ExpressionResultString result = state.getNodeType(methodNode);
    assertThat(result).isEqualTo(new ExpressionResultString(TypeString.SELF));
  }

  @Test
  void testSelfNewInit() {
    final String code =
        """
        _method object.test
            _return rope.new()
        _endmethod
        """;

    // Set up.
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
            Collections.emptySet()));
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
            Collections.emptySet(),
            new ExpressionResultString(TypeString.SELF),
            ExpressionResultString.EMPTY));

    // Do analysis.
    final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
    final LocalTypeReasonerState state = magikFile.getTypeReasonerState();

    // Assert user:object.test type determined.
    final AstNode topNode = magikFile.getTopNode();
    final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
    final ExpressionResultString result = state.getNodeType(methodNode);
    assertThat(result).isEqualTo(new ExpressionResultString(ropeRef));
  }

  // endregion

  @Test
  void testExpressionTypeAnnotation() throws IOException {
    final String code =
        """
        _method object.test
            _return :a  # type: sw:integer
        _endmethod
        """;

    // Set up.
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();

    // Do analysis.
    final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
    final LocalTypeReasonerState state = magikFile.getTypeReasonerState();

    // Assert user:object.test type determined.
    final AstNode topNode = magikFile.getTopNode();
    final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
    final ExpressionResultString result = state.getNodeType(methodNode);
    assertThat(result).isEqualTo(new ExpressionResultString(TypeString.SW_INTEGER));
  }

  @Test
  void testIterableExpressionIterTypeAnnotation() throws IOException {
    final String code =
        """
        _method object.test
            _for a, b _over fn()  # iter-type: sw:integer, sw:float
            _loop
              _return a, b
            _endloop
        _endmethod
        """;

    // Do analysis.
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
    final LocalTypeReasonerState state = magikFile.getTypeReasonerState();

    // Assert user:object.test type determined.
    final AstNode topNode = magikFile.getTopNode();
    final AstNode forVariablesNode = topNode.getFirstDescendant(MagikGrammar.FOR_VARIABLES);

    final AstNode aNode = forVariablesNode.getDescendants(MagikGrammar.IDENTIFIER).get(0);
    final ExpressionResultString aResult = state.getNodeType(aNode);
    assertThat(aResult).isEqualTo(new ExpressionResultString(TypeString.SW_INTEGER));

    final AstNode bNode = forVariablesNode.getDescendants(MagikGrammar.IDENTIFIER).get(1);
    final ExpressionResultString bResult = state.getNodeType(bNode);
    assertThat(bResult).isEqualTo(new ExpressionResultString(TypeString.SW_FLOAT));
  }

  @Test
  void testEmptyLocalDefinition() {
    final String code =
        """
        _method object.test
            _local a
            _return a
        _endmethod
        """;

    // Set up.
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();

    // Do analysis.
    final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
    final LocalTypeReasonerState state = magikFile.getTypeReasonerState();

    // Assert user:object.test type determined.
    final AstNode topNode = magikFile.getTopNode();
    final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
    final ExpressionResultString result = state.getNodeType(methodNode);
    assertThat(result).isEqualTo(new ExpressionResultString(TypeString.SW_UNSET));
  }

  @Test
  void testAugmentedAssignment() {
    final String code =
        """
        _method object.test
            _local x << 1
            x +<< 1.0
            _return x
        _endmethod
        """;

    // Set up.
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
    final LocalTypeReasonerState state = magikFile.getTypeReasonerState();

    // Assert user:object.test type determined.
    final AstNode topNode = magikFile.getTopNode();
    final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
    final ExpressionResultString result = state.getNodeType(methodNode);
    assertThat(result).isEqualTo(new ExpressionResultString(TypeString.SW_FLOAT));
  }

  @Test
  void testGatherParameters() {
    final String code =
        """
        _method object.test(_gather args)
        _endmethod
        """;

    // Set up.
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();

    // Do analysis.
    final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
    final LocalTypeReasonerState state = magikFile.getTypeReasonerState();

    // Assert user:object.test type determined.
    final AstNode topNode = magikFile.getTopNode();
    final AstNode parameterNode =
        topNode.getFirstDescendant(MagikGrammar.PARAMETER).getFirstChild(MagikGrammar.IDENTIFIER);
    final ExpressionResultString result = state.getNodeType(parameterNode);
    assertThat(result)
        .isEqualTo(
            new ExpressionResultString(
                TypeString.ofIdentifier(
                    "simple_vector",
                    "sw",
                    TypeString.ofGenericDefinition("E", TypeString.UNDEFINED))));
  }

  @Test
  void testGatherParametersGeneric() {
    final String code =
        """
        _method object.test(_gather args)
          ## @param {sw:symbol} args
        _endmethod
        """;

    // Set up.
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();

    // Do analysis.
    final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
    final LocalTypeReasonerState state = magikFile.getTypeReasonerState();

    // Assert user:object.test type determined.
    final AstNode topNode = magikFile.getTopNode();
    final AstNode parameterNode =
        topNode.getFirstDescendant(MagikGrammar.PARAMETER).getFirstChild(MagikGrammar.IDENTIFIER);
    final ExpressionResultString result = state.getNodeType(parameterNode);
    assertThat(result)
        .isEqualTo(
            new ExpressionResultString(
                TypeString.ofIdentifier(
                    "simple_vector",
                    "sw",
                    TypeString.ofGenericDefinition("E", TypeString.SW_SYMBOL))));
  }

  @Test
  void testImport() {
    final String code =
        """
        _method object.test
            _local x << 1
            _proc()
                _import x
            _endproc
        _endmethod
        """;

    // Set up.
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();

    // Do analysis.
    final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
    final LocalTypeReasonerState state = magikFile.getTypeReasonerState();

    // Assert user:object.test type determined.
    final AstNode topNode = magikFile.getTopNode();
    final AstNode procNode = topNode.getFirstDescendant(MagikGrammar.PROCEDURE_DEFINITION);
    final AstNode importNode = procNode.getFirstDescendant(MagikGrammar.VARIABLE_DEFINITION);
    final ExpressionResultString result = state.getNodeType(importNode);
    assertThat(result).isEqualTo(new ExpressionResultString(TypeString.SW_INTEGER));
  }

  @Test
  void testAssignmentChain() {
    final String code =
        """
        _method object.test
            _local x << y << 10
            _return x, y
        _endmethod
        """;

    // Set up.
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();

    // Do analysis.
    final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
    final LocalTypeReasonerState state = magikFile.getTypeReasonerState();

    // Assert user:object.test type determined.
    final AstNode topNode = magikFile.getTopNode();
    final AstNode methodNode = topNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
    final ExpressionResultString result = state.getNodeType(methodNode);
    assertThat(result)
        .isEqualTo(new ExpressionResultString(TypeString.SW_INTEGER, TypeString.SW_INTEGER));
  }

  @Test
  void testTryWith() {
    final String code =
        """
        _method object.test
            _try _with cond
            _when warning
            _when error
            _endtry
        _endmethod
        """;

    // Set up.
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();

    // Do analysis.
    final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
    final LocalTypeReasonerState state = magikFile.getTypeReasonerState();

    final AstNode topNode = magikFile.getTopNode();
    final AstNode tryVarNode = topNode.getFirstDescendant(MagikGrammar.TRY_VARIABLE);
    final ExpressionResultString result = state.getNodeType(tryVarNode);
    assertThat(result).isEqualTo(new ExpressionResultString(TypeString.SW_CONDITION));
  }

  @Test
  void testMergeReturnTypes() {
    final String code =
        """
        _method e1.m1
          _return 1
          _return x()
        _endmethod""";

    // Set up.
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();

    // Do analysis.
    final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
    final LocalTypeReasonerState state = magikFile.getTypeReasonerState();

    final AstNode topNode = magikFile.getTopNode();
    final AstNode methodDefNode = topNode.getFirstDescendant(MagikGrammar.METHOD_DEFINITION);
    final ExpressionResultString result = state.getNodeType(methodDefNode);
    assertThat(result)
        .isEqualTo(
            new ExpressionResultString(
                new ExpressionResultString(TypeString.SW_INTEGER),
                ExpressionResultString.UNDEFINED));
  }

  @Test
  void testSingleSuperType() {
    final String code =
        """
        _method t.m
          _super.m
        _endmethod""";

    // Set up.
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
            Collections.emptySet()));
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
            Collections.emptySet()));

    // Do analysis.
    final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
    final LocalTypeReasonerState state = magikFile.getTypeReasonerState();

    final AstNode topNode = magikFile.getTopNode();
    final AstNode superNode = topNode.getFirstDescendant(MagikGrammar.SUPER).getParent();
    final ExpressionResultString result = state.getNodeType(superNode);
    assertThat(result).isEqualTo(new ExpressionResultString(sRef));
  }

  @Test
  void testSingleNamedSuperType() {
    final String code =
        """
        _method t.m
          _super(r).m
        _endmethod""";

    // Set up.
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
            Collections.emptySet()));

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
            Collections.emptySet()));

    // Do analysis.
    final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
    final LocalTypeReasonerState state = magikFile.getTypeReasonerState();

    final AstNode topNode = magikFile.getTopNode();
    final AstNode superNode = topNode.getFirstDescendant(MagikGrammar.SUPER).getParent();
    final ExpressionResultString result = state.getNodeType(superNode);
    assertThat(result).isEqualTo(new ExpressionResultString(rRef));
  }

  @Test
  void testParameterType() {
    final String code =
        """
        _method a.b(p1)
          ## @param {sw:float} p1
          show(p1)\
        _endmethod""";

    // Set up.
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();

    // Do analysis.
    final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
    final LocalTypeReasonerState state = magikFile.getTypeReasonerState();

    final AstNode topNode = magikFile.getTopNode();

    // Test parameter definition.
    final AstNode parameterNode =
        topNode.getFirstDescendant(MagikGrammar.PARAMETER).getFirstChild(MagikGrammar.IDENTIFIER);
    final ExpressionResultString actualParameterResult = state.getNodeType(parameterNode);
    assertThat(actualParameterResult).isEqualTo(new ExpressionResultString(TypeString.SW_FLOAT));

    // Test parameter usage.
    final AstNode procInvocationNode =
        topNode.getFirstDescendant(MagikGrammar.PROCEDURE_INVOCATION);
    final AstNode atomNode = procInvocationNode.getFirstDescendant(MagikGrammar.ATOM);
    final ExpressionResultString actualAtomResult = state.getNodeType(atomNode);
    assertThat(actualAtomResult).isEqualTo(new ExpressionResultString(TypeString.SW_FLOAT));
  }

  @Test
  void testAssignmentParameterType() {
    final String code =
        """
        _method a.b << p1
          ## @param {sw:float} p1
          show(p1)
        _endmethod""";

    // Set up.
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();

    // Do analysis.
    final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
    final LocalTypeReasonerState state = magikFile.getTypeReasonerState();

    final AstNode topNode = magikFile.getTopNode();

    // Test parameter definition.
    final AstNode parameterNode =
        topNode.getFirstDescendant(MagikGrammar.PARAMETER).getFirstChild(MagikGrammar.IDENTIFIER);
    final ExpressionResultString actualParameterResult = state.getNodeType(parameterNode);
    assertThat(actualParameterResult).isEqualTo(new ExpressionResultString(TypeString.SW_FLOAT));

    // Test parameter usage.
    final AstNode procInvocationNode =
        topNode.getFirstDescendant(MagikGrammar.PROCEDURE_INVOCATION);
    final AstNode atomNode = procInvocationNode.getFirstDescendant(MagikGrammar.ATOM);
    final ExpressionResultString actualAtomResult = state.getNodeType(atomNode);
    assertThat(actualAtomResult).isEqualTo(new ExpressionResultString(TypeString.SW_FLOAT));
  }

  @Test
  void testParameterTypeCombined() {
    final String code =
        """
        _method a.b(p1)
          ## @param {sw:float|sw:integer} p1
        _endmethod""";

    // Set up.
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();

    // Do analysis.
    final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
    final LocalTypeReasonerState state = magikFile.getTypeReasonerState();

    final AstNode topNode = magikFile.getTopNode();
    final AstNode parameterNode =
        topNode.getFirstDescendant(MagikGrammar.PARAMETER).getFirstChild(MagikGrammar.IDENTIFIER);
    final ExpressionResultString result = state.getNodeType(parameterNode);
    assertThat(result)
        .isEqualTo(
            new ExpressionResultString(
                TypeString.combine(TypeString.SW_FLOAT, TypeString.SW_INTEGER)));
  }

  @Test
  void testParameterReferenceUsage() {
    final String code =
        """
        _method a.b
          _return _self.returns_param(10)
        _endmethod""";

    // Set up.
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
            Collections.emptySet()));
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
                    null, null, null, null, "p1", ParameterDefinition.Modifier.NONE, param1Ref)),
            null,
            Collections.emptySet(),
            new ExpressionResultString(param1Ref),
            ExpressionResultString.EMPTY));

    // Do analysis.
    final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
    final LocalTypeReasonerState state = magikFile.getTypeReasonerState();

    final AstNode topNode = magikFile.getTopNode();
    final AstNode methodDefinitionNode = topNode.getFirstDescendant(MagikGrammar.METHOD_DEFINITION);
    final ExpressionResultString result = state.getNodeType(methodDefinitionNode);
    assertThat(result).isEqualTo(new ExpressionResultString(TypeString.SW_INTEGER));
  }

  @Test
  void testParameterReferenceNested() {
    final String code =
        """
        _method a.b
          _return _self.returns_param(_self.returns_param2(10))
        _endmethod""";

    // Set up.
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
            Collections.emptySet()));
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
                    null, null, null, null, "p1", ParameterDefinition.Modifier.NONE, param1Ref)),
            null,
            Collections.emptySet(),
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
                    null, null, null, null, "p1", ParameterDefinition.Modifier.NONE, param1Ref)),
            null,
            Collections.emptySet(),
            new ExpressionResultString(param1Ref),
            ExpressionResultString.EMPTY));

    // Do analysis.
    final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
    final LocalTypeReasonerState state = magikFile.getTypeReasonerState();

    final AstNode topNode = magikFile.getTopNode();
    final AstNode methodDefinitionNode = topNode.getFirstDescendant(MagikGrammar.METHOD_DEFINITION);
    final ExpressionResultString result = state.getNodeType(methodDefinitionNode);
    assertThat(result).isEqualTo(new ExpressionResultString(TypeString.SW_INTEGER));
  }

  @Test
  void testParameterReferenceOptional() {
    final String code =
        """
        _method a.b
          _return _self.returns_param()
        _endmethod""";

    // Set up.
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
            Collections.emptySet()));
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
                    null, null, null, null, "p1", ParameterDefinition.Modifier.NONE, param1Ref)),
            null,
            Collections.emptySet(),
            new ExpressionResultString(param1Ref),
            ExpressionResultString.EMPTY));

    // Do analysis.
    final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
    final LocalTypeReasonerState state = magikFile.getTypeReasonerState();

    final AstNode topNode = magikFile.getTopNode();
    final AstNode methodDefinitionNode = topNode.getFirstDescendant(MagikGrammar.METHOD_DEFINITION);
    final ExpressionResultString result = state.getNodeType(methodDefinitionNode);
    assertThat(result).isEqualTo(new ExpressionResultString(TypeString.SW_UNSET));
  }

  @Test
  void testGenericMethodInvocation1() {
    final String code =
        """
        _method object.m
          _local r << sw:rope.new()  # type: sw:rope<E=sw:integer>
          _return r.an_element()
        _endmethod
        """;

    // Set up.
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
            Collections.emptySet()));
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
            Collections.emptySet(),
            new ExpressionResultString(TypeString.SELF),
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
            Collections.emptySet(),
            new ExpressionResultString(
                TypeString.ofGenericReference(
                    "E")), // Possibly also `sw:unset`, but for testing purposes...
            ExpressionResultString.EMPTY));

    // Do analysis.
    final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
    final LocalTypeReasonerState state = magikFile.getTypeReasonerState();

    final AstNode topNode = magikFile.getTopNode();
    final AstNode methodDefinitionNode = topNode.getFirstDescendant(MagikGrammar.METHOD_DEFINITION);
    final ExpressionResultString result = state.getNodeType(methodDefinitionNode);
    assertThat(result).isEqualTo(new ExpressionResultString(TypeString.SW_INTEGER));
  }

  @Test
  void testGenericMethodInvocation2() {
    final String code =
        """
        _method object.m
          _local pl << sw:property_list.new()  # type: sw:property_list<K=sw:symbol, E=sw:integer>
          _return pl.a_key(), pl.an_element()
        _endmethod
        """;

    // Set up.
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
            Collections.emptySet()));
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
            Collections.emptySet(),
            new ExpressionResultString(TypeString.SELF),
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
            Collections.emptySet(),
            new ExpressionResultString(
                TypeString.ofGenericReference(
                    "K")), // Possibly also `sw:unset`, but for testing purposes...
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
            Collections.emptySet(),
            new ExpressionResultString(
                TypeString.ofGenericReference(
                    "E")), // Possibly also `sw:unset`, but for testing purposes...
            ExpressionResultString.EMPTY));

    // Do analysis.
    final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
    final LocalTypeReasonerState state = magikFile.getTypeReasonerState();

    final AstNode topNode = magikFile.getTopNode();
    final AstNode methodDefinitionNode = topNode.getFirstDescendant(MagikGrammar.METHOD_DEFINITION);
    final ExpressionResultString result = state.getNodeType(methodDefinitionNode);
    assertThat(result)
        .isEqualTo(new ExpressionResultString(TypeString.SW_SYMBOL, TypeString.SW_INTEGER));
  }

  @Test
  void testGenericMethodInvocation3() {
    final String code =
        """
        _method object.m
          _local r << sw:rope.new()  # type: sw:rope<E=sw:integer>
          _return r.as_simple_vector()
        _endmethod
        """;

    // Set up.
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
            Collections.emptySet()));
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
            Collections.emptySet(),
            new ExpressionResultString(TypeString.SELF),
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
            Collections.emptySet(),
            new ExpressionResultString(
                TypeString.ofIdentifier(
                    TypeString.SW_SIMPLE_VECTOR.getIdentifier(),
                    TypeString.SW_SIMPLE_VECTOR.getPakkage(),
                    TypeString.ofGenericDefinition("E", TypeString.ofGenericReference("E")))),
            ExpressionResultString.EMPTY));

    // Do analysis.
    final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
    final LocalTypeReasonerState state = magikFile.getTypeReasonerState();

    final AstNode topNode = magikFile.getTopNode();
    final AstNode methodDefinitionNode = topNode.getFirstDescendant(MagikGrammar.METHOD_DEFINITION);
    final ExpressionResultString result = state.getNodeType(methodDefinitionNode);
    assertThat(result)
        .isEqualTo(
            new ExpressionResultString(
                TypeString.ofIdentifier(
                    "simple_vector",
                    "sw",
                    TypeString.ofGenericDefinition("E", TypeString.SW_INTEGER))));
  }

  @Test
  void testGenericMethodInvocation4() {
    final String code =
        """
        _method object.m
          _local str << 'abc'
          _return str.keys, str.elements
        _endmethod
        """;

    // Set up.
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final ExemplarDefinition char16VectorDef =
        definitionKeeper.getExemplarDefinitions(TypeString.SW_CHAR16_VECTOR).iterator().next();
    definitionKeeper.remove(char16VectorDef);
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.INDEXED,
            TypeString.ofIdentifier(
                TypeString.SW_CHAR16_VECTOR.getIdentifier(),
                TypeString.SW_CHAR16_VECTOR.getPakkage(),
                TypeString.ofGenericDefinition("K", TypeString.SW_INTEGER),
                TypeString.ofGenericDefinition("E", TypeString.SW_CHARACTER)),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptySet()));
    definitionKeeper.add(
        new MethodDefinition(
            null,
            null,
            null,
            null,
            TypeString.SW_CHAR16_VECTOR,
            "keys",
            EnumSet.noneOf(MethodDefinition.Modifier.class),
            Collections.emptyList(),
            null,
            Collections.emptySet(),
            new ExpressionResultString(
                TypeString.ofIdentifier(
                    TypeString.SW_SIMPLE_VECTOR.getIdentifier(),
                    TypeString.SW_SIMPLE_VECTOR.getPakkage(),
                    TypeString.ofGenericDefinition("E", TypeString.ofGenericReference("K")))),
            ExpressionResultString.EMPTY));
    definitionKeeper.add(
        new MethodDefinition(
            null,
            null,
            null,
            null,
            TypeString.SW_CHAR16_VECTOR,
            "elements",
            EnumSet.noneOf(MethodDefinition.Modifier.class),
            Collections.emptyList(),
            null,
            Collections.emptySet(),
            new ExpressionResultString(
                TypeString.ofIdentifier(
                    TypeString.SW_SIMPLE_VECTOR.getIdentifier(),
                    TypeString.SW_SIMPLE_VECTOR.getPakkage(),
                    TypeString.ofGenericDefinition("E", TypeString.ofGenericReference("E")))),
            ExpressionResultString.EMPTY));

    // Do analysis.
    final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
    final LocalTypeReasonerState state = magikFile.getTypeReasonerState();

    final AstNode topNode = magikFile.getTopNode();
    final AstNode methodDefinitionNode = topNode.getFirstDescendant(MagikGrammar.METHOD_DEFINITION);
    final ExpressionResultString result = state.getNodeType(methodDefinitionNode);
    assertThat(result)
        .isEqualTo(
            new ExpressionResultString(
                TypeString.ofIdentifier(
                    TypeString.SW_SIMPLE_VECTOR.getIdentifier(),
                    TypeString.SW_SIMPLE_VECTOR.getPakkage(),
                    TypeString.ofGenericDefinition("E", TypeString.SW_INTEGER)),
                TypeString.ofIdentifier(
                    TypeString.SW_SIMPLE_VECTOR.getIdentifier(),
                    TypeString.SW_SIMPLE_VECTOR.getPakkage(),
                    TypeString.ofGenericDefinition("E", TypeString.SW_CHARACTER))));
  }

  @Test
  void testGenericIterMethodInvocation() {
    final String code =
        """
        _method object.m
          _local pl << sw:property_list.new()  # type: sw:property_list<K=sw:symbol, E=sw:integer>
          _for k, e _over pl.fast_keys_and_elements()
          _loop
          _endloop
        _endmethod
        """;

    // Set up.
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
            Collections.emptySet()));
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
            Collections.emptySet(),
            new ExpressionResultString(TypeString.SELF),
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
            Collections.emptySet(),
            ExpressionResultString.EMPTY,
            new ExpressionResultString(
                TypeString.ofGenericReference("K"), TypeString.ofGenericReference("E"))));

    // Do analysis.
    final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
    final LocalTypeReasonerState state = magikFile.getTypeReasonerState();

    final AstNode topNode = magikFile.getTopNode();
    final AstNode forVariablesNode = topNode.getFirstDescendant(MagikGrammar.FOR_VARIABLES);
    final List<AstNode> identifierNodes = forVariablesNode.getDescendants(MagikGrammar.IDENTIFIER);

    final AstNode kIdentifierNode = identifierNodes.get(0);
    final ExpressionResultString kResult = state.getNodeType(kIdentifierNode);
    assertThat(kResult).isEqualTo(new ExpressionResultString(TypeString.SW_SYMBOL));

    final AstNode eIdentifierNode = identifierNodes.get(1);
    final ExpressionResultString eResult = state.getNodeType(eIdentifierNode);
    assertThat(eResult).isEqualTo(new ExpressionResultString(TypeString.SW_INTEGER));
  }

  @Test
  void testGenericSlot() {
    final String code =
        """
        _method exemplar.m
          _return .stack.pop()
        _endmethod
        """;

    // Set up.
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
            Collections.emptyList(),
            Collections.emptySet()));
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
            Collections.emptySet(),
            new ExpressionResultString(
                TypeString.ofGenericReference(
                    "E")), // Possibly also `sw:unset`, but for testing purposes...
            ExpressionResultString.EMPTY));

    final TypeString exemplarRef = TypeString.ofIdentifier("exemplar", "sw");
    final TypeString slotTypeRef =
        TypeString.ofIdentifier(
            "stack", "sw", TypeString.ofGenericDefinition("E", TypeString.SW_INTEGER));
    final SlotDefinition slotDefinition =
        new SlotDefinition(null, null, null, null, "stack", slotTypeRef);
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.SLOTTED,
            exemplarRef,
            List.of(slotDefinition),
            Collections.emptyList(),
            Collections.emptySet()));

    // Do analysis.
    final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
    final LocalTypeReasonerState state = magikFile.getTypeReasonerState();

    final AstNode topNode = magikFile.getTopNode();
    final AstNode methodDefinitionNode = topNode.getFirstDescendant(MagikGrammar.METHOD_DEFINITION);
    final ExpressionResultString result = state.getNodeType(methodDefinitionNode);
    assertThat(result).isEqualTo(new ExpressionResultString(TypeString.SW_INTEGER));
  }

  @Test
  void testGenericSimpleVector() {
    final String code =
        """
        _method exemplar.m
          _return {1, :a}
        _endmethod
        """;

    // Set up.
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();

    // Do analysis.
    final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
    final LocalTypeReasonerState state = magikFile.getTypeReasonerState();

    final AstNode topNode = magikFile.getTopNode();
    final AstNode methodDefinitionNode = topNode.getFirstDescendant(MagikGrammar.METHOD_DEFINITION);
    final ExpressionResultString result = state.getNodeType(methodDefinitionNode);
    assertThat(result)
        .isEqualTo(
            new ExpressionResultString(
                TypeString.ofIdentifier(
                    TypeString.SW_SIMPLE_VECTOR.getIdentifier(),
                    TypeString.SW_SIMPLE_VECTOR.getPakkage(),
                    TypeString.ofGenericDefinition(
                        "E", TypeString.combine(TypeString.SW_INTEGER, TypeString.SW_SYMBOL)))));
  }
}
