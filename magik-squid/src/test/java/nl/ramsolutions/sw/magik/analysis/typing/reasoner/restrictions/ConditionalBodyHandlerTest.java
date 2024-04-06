package nl.ramsolutions.sw.magik.analysis.typing.reasoner.restrictions;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonar.sslr.api.AstNode;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.definitions.ConditionDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasonerState;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import org.junit.jupiter.api.Test;

/** Tests for {@link ConditionalBodyHandler}. */
class ConditionalBodyHandlerTest {

  private MagikTypedFile createMagikFile(String code, IDefinitionKeeper definitionKeeper) {
    final URI uri = URI.create("tests://unittest");
    return new MagikTypedFile(uri, code, definitionKeeper);
  }

  @Test
  void testHandleIfIsUnsetOfUndefined() {
    final String code =
        """
        _method object.method(param1)
          _if param1 _is _unset
          _then
            show(param1)
          _endif
        _endmethod
        """;

    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
    final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

    final AstNode topNode = magikFile.getTopNode();
    final AstNode argumentNode = topNode.getFirstDescendant(MagikGrammar.ARGUMENT);
    final AstNode atomNode = argumentNode.getFirstDescendant(MagikGrammar.ATOM);
    final ExpressionResultString result = reasonerState.getNodeType(atomNode);
    assertThat(result).isEqualTo(new ExpressionResultString(TypeString.SW_UNSET));
  }

  @Test
  void testHandleIfIsntUnsetOfUndefined() {
    final String code =
        """
        _method object.method(param1)
          _if param1 _isnt _unset
          _then
            show(param1)
          _endif
        _endmethod
        """;

    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
    final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

    final AstNode topNode = magikFile.getTopNode();
    final AstNode argumentNode = topNode.getFirstDescendant(MagikGrammar.ARGUMENT);
    final AstNode atomNode = argumentNode.getFirstDescendant(MagikGrammar.ATOM);
    final ExpressionResultString result = reasonerState.getNodeType(atomNode);
    assertThat(result).isEqualTo(new ExpressionResultString(TypeString.UNDEFINED));
  }

  @Test
  void testHandleIfIsIntegerValueOfUndefined() {
    final String code =
        """
        _method object.method(param1)
          _if param1 _is 5
          _then
            show(param1)
          _endif
        _endmethod
        """;

    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
    final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

    final AstNode topNode = magikFile.getTopNode();
    final AstNode argumentNode = topNode.getFirstDescendant(MagikGrammar.ARGUMENT);
    final AstNode atomNode = argumentNode.getFirstDescendant(MagikGrammar.ATOM);
    final ExpressionResultString result = reasonerState.getNodeType(atomNode);
    assertThat(result).isEqualTo(new ExpressionResultString(TypeString.SW_INTEGER));
  }

  @Test
  void testHandleIfOr() {
    // Or-expression are not supported.
    final String code =
        """
        _method object.method(param1)
          _if param1 _is _unset _orif
              param1 _is :a
          _then
            show(param1)
          _else
            show(param1)
          _endif
        _endmethod
        """;

    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
    final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

    final AstNode topNode = magikFile.getTopNode();
    final List<AstNode> argumentNodes = topNode.getDescendants(MagikGrammar.ARGUMENT);

    final AstNode argumentNode0 = argumentNodes.get(0);
    final AstNode atomNode0 = argumentNode0.getFirstDescendant(MagikGrammar.ATOM);
    final ExpressionResultString result0 = reasonerState.getNodeType(atomNode0);
    assertThat(result0).isEqualTo(ExpressionResultString.UNDEFINED);

    final AstNode argumentNode1 = argumentNodes.get(1);
    final AstNode atomNode1 = argumentNode1.getFirstDescendant(MagikGrammar.ATOM);
    final ExpressionResultString result1 = reasonerState.getNodeType(atomNode1);
    assertThat(result1).isEqualTo(ExpressionResultString.UNDEFINED);
  }

  @Test
  void testHandleIfAnd() {
    // And-expression are not supported.
    final String code =
        """
        _method object.method(param1, param2)
          _if param1 _is _unset _andif
              param2 _is _unset
          _then
            show(param1)
            show(param2)
          _else
            show(param1)
            show(param2)
          _endif
        _endmethod
        """;

    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
    final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

    final AstNode topNode = magikFile.getTopNode();
    final List<AstNode> argumentNodes = topNode.getDescendants(MagikGrammar.ARGUMENT);

    final AstNode argumentNode0 = argumentNodes.get(0);
    final AstNode atomNode0 = argumentNode0.getFirstDescendant(MagikGrammar.ATOM);
    final ExpressionResultString result0 = reasonerState.getNodeType(atomNode0);
    assertThat(result0).isEqualTo(ExpressionResultString.UNDEFINED);

    final AstNode argumentNode1 = argumentNodes.get(1);
    final AstNode atomNode1 = argumentNode1.getFirstDescendant(MagikGrammar.ATOM);
    final ExpressionResultString result1 = reasonerState.getNodeType(atomNode1);
    assertThat(result1).isEqualTo(ExpressionResultString.UNDEFINED);

    final AstNode argumentNode2 = argumentNodes.get(2);
    final AstNode atomNode2 = argumentNode2.getFirstDescendant(MagikGrammar.ATOM);
    final ExpressionResultString result2 = reasonerState.getNodeType(atomNode2);
    assertThat(result2).isEqualTo(ExpressionResultString.UNDEFINED);

    // And-expression are not supported.
    final AstNode argumentNode3 = argumentNodes.get(3);
    final AstNode atomNode3 = argumentNode3.getFirstDescendant(MagikGrammar.ATOM);
    final ExpressionResultString result3 = reasonerState.getNodeType(atomNode3);
    assertThat(result3).isEqualTo(ExpressionResultString.UNDEFINED);
  }

  @Test
  void testHandleIfMethodCall() {
    final String code =
        """
        _method object.method(param1)
          _if param1.test?
          _then
            show(param1)
          _endif
        _endmethod
        """;

    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
    final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

    final AstNode topNode = magikFile.getTopNode();
    final AstNode argumentNode = topNode.getFirstDescendant(MagikGrammar.ARGUMENT);
    final AstNode atomNode = argumentNode.getFirstDescendant(MagikGrammar.ATOM);
    final ExpressionResultString result = reasonerState.getNodeType(atomNode);
    assertThat(result).isEqualTo(ExpressionResultString.UNDEFINED);
  }

  @Test
  void testHandleIfIsUnsetThenAssigned() {
    final String code =
        """
        _method object.method()
          _local var << _self.method2()  # type: sw:integer|sw:unset
          _if var _is _unset
          _then
            show(var)
            var << 10
            show(var)
          _endif
        _endmethod
        """;

    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
    final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

    final AstNode topNode = magikFile.getTopNode();
    final List<AstNode> argumentNodes = topNode.getDescendants(MagikGrammar.ARGUMENT);

    // After positive test for _unset.
    final AstNode argument0Node = argumentNodes.get(0);
    final AstNode atom0Node = argument0Node.getFirstDescendant(MagikGrammar.ATOM);
    final ExpressionResultString result0 = reasonerState.getNodeType(atom0Node);
    assertThat(result0).isEqualTo(new ExpressionResultString(TypeString.SW_UNSET));

    // After assigning integer.
    final AstNode argument1Node = argumentNodes.get(1);
    final AstNode atom1Node = argument1Node.getFirstDescendant(MagikGrammar.ATOM);
    final ExpressionResultString result1 = reasonerState.getNodeType(atom1Node);
    assertThat(result1).isEqualTo(new ExpressionResultString(TypeString.SW_INTEGER));
  }

  @Test
  void testHandleIfIsUnset2() {
    final String code =
        """
        _method object.method(param1)
          _if _unset _is param1
          _then
            show(param1)
          _endif
        _endmethod
        """;

    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
    final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

    final AstNode topNode = magikFile.getTopNode();
    final AstNode argumentNode = topNode.getFirstDescendant(MagikGrammar.ARGUMENT);
    final AstNode atomNode = argumentNode.getFirstDescendant(MagikGrammar.ATOM);
    final ExpressionResultString result = reasonerState.getNodeType(atomNode);
    assertThat(result).isEqualTo(new ExpressionResultString(TypeString.SW_UNSET));
  }

  @Test
  void testHandleIfIsntUnset() {
    final String code =
        """
        _method object.method(param1)
          ## @param {sw:integer|sw:unset} param1
          _if param1 _isnt _unset
          _then
            show(param1)
          _endif
        _endmethod
        """;

    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
    final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

    final AstNode topNode = magikFile.getTopNode();
    final AstNode argumentNode = topNode.getFirstDescendant(MagikGrammar.ARGUMENT);
    final AstNode atomNode = argumentNode.getFirstDescendant(MagikGrammar.ATOM);
    final ExpressionResultString result = reasonerState.getNodeType(atomNode);
    assertThat(result).isEqualTo(new ExpressionResultString(TypeString.SW_INTEGER));
  }

  @Test
  void testHandleIfIsUnsetElseIsKindOf() {
    final String code =
        """
        _method object.method(param1)
          _if param1 _is _unset
          _then
            show(param1)
          _elif param1.is_kind_of?(integer)
          _then
            show(param1)
          _endif
        _endmethod
        """;

    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
    final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

    final AstNode topNode = magikFile.getTopNode();
    final List<AstNode> argumentNodes = topNode.getDescendants(MagikGrammar.ARGUMENT);

    final AstNode argumentNode0 = argumentNodes.get(0);
    final AstNode atomNode0 = argumentNode0.getFirstDescendant(MagikGrammar.ATOM);
    final ExpressionResultString result0 = reasonerState.getNodeType(atomNode0);
    assertThat(result0).isEqualTo(new ExpressionResultString(TypeString.SW_UNSET));

    final AstNode argumentNode2 = argumentNodes.get(2);
    final AstNode atomNode2 = argumentNode2.getFirstDescendant(MagikGrammar.ATOM);
    final ExpressionResultString result2 = reasonerState.getNodeType(atomNode2);
    assertThat(result2).isEqualTo(new ExpressionResultString(TypeString.SW_INTEGER));
  }

  @Test
  void testHandleIfNotIsKindOf() {
    final String code =
        """
        _method object.method(param1)
          ## @param {sw:integer|sw:symbol} param1
          _if _not param1.is_kind_of?(integer)
          _then
            show(param1)
          _endif
        _endmethod
        """;

    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
    final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

    final AstNode topNode = magikFile.getTopNode();
    final List<AstNode> argumentNodes = topNode.getDescendants(MagikGrammar.ARGUMENT);

    final AstNode argumentNode1 = argumentNodes.get(1);
    final AstNode atomNode1 = argumentNode1.getFirstDescendant(MagikGrammar.ATOM);
    final ExpressionResultString result1 = reasonerState.getNodeType(atomNode1);
    assertThat(result1).isEqualTo(new ExpressionResultString(TypeString.SW_SYMBOL));
  }

  @Test
  void testHandleUnreasonable() {
    final String code =
        """
        _method object.method(param1)
          ## @param {sw:integer|sw:symbol} param1
          _if param1.is_kind_of?(integer)
          _then
            show(param1)
          _elif param1.is_kind_of?(symbol)
          _then
            show(param1)
          _else
            show(param1)
          _endif
        _endmethod
        """;

    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
    final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

    final AstNode topNode = magikFile.getTopNode();
    final List<AstNode> argumentNodes = topNode.getDescendants(MagikGrammar.ARGUMENT);

    final AstNode argumentNode1 = argumentNodes.get(1);
    final AstNode atomNode1 = argumentNode1.getFirstDescendant(MagikGrammar.ATOM);
    final ExpressionResultString result1 = reasonerState.getNodeType(atomNode1);
    assertThat(result1).isEqualTo(new ExpressionResultString(TypeString.SW_INTEGER));

    final AstNode argumentNode3 = argumentNodes.get(3);
    final AstNode atomNode3 = argumentNode3.getFirstDescendant(MagikGrammar.ATOM);
    final ExpressionResultString result3 = reasonerState.getNodeType(atomNode3);
    assertThat(result3).isEqualTo(new ExpressionResultString(TypeString.SW_SYMBOL));

    // The type in the else block cannot be determined, thus take the type it was given.
    final AstNode argumentNode4 = argumentNodes.get(4);
    final AstNode atomNode4 = argumentNode4.getFirstDescendant(MagikGrammar.ATOM);
    final ExpressionResultString result4 = reasonerState.getNodeType(atomNode4);
    assertThat(result4)
        .isEqualTo(
            new ExpressionResultString(
                TypeString.ofCombination(TypeString.SW_SYMBOL, TypeString.SW_INTEGER)));
  }

  @Test
  void testHandleUnreasonable2() {
    final String code =
        """
        _method object.method(param1)
          ## @param {sw:integer|sw:symbol} param1
          _if param1 > 10
          _then
            show(param1)
          _else
            show(param1)
          _endif
        _endmethod
        """;

    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
    final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

    final AstNode topNode = magikFile.getTopNode();
    final List<AstNode> argumentNodes = topNode.getDescendants(MagikGrammar.ARGUMENT);

    final AstNode argumentNode0 = argumentNodes.get(0);
    final AstNode atomNode0 = argumentNode0.getFirstDescendant(MagikGrammar.ATOM);
    final ExpressionResultString result0 = reasonerState.getNodeType(atomNode0);
    assertThat(result0)
        .isEqualTo(
            new ExpressionResultString(
                TypeString.ofCombination(TypeString.SW_SYMBOL, TypeString.SW_INTEGER)));

    final AstNode argumentNode1 = argumentNodes.get(1);
    final AstNode atomNode1 = argumentNode1.getFirstDescendant(MagikGrammar.ATOM);
    final ExpressionResultString result1 = reasonerState.getNodeType(atomNode1);
    assertThat(result1)
        .isEqualTo(
            new ExpressionResultString(
                TypeString.ofCombination(TypeString.SW_SYMBOL, TypeString.SW_INTEGER)));
  }

  @Test
  void testHandleIfIsUnsetElse() {
    final String code =
        """
        _method object.method(_optional param1)
          ## @param {sw:integer} param1
          _if _unset _is param1
          _then
            show(param1)
          _else
            show(param1)
          _endif
        _endmethod
        """;

    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
    final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

    final AstNode topNode = magikFile.getTopNode();
    final List<AstNode> argumentNodes = topNode.getDescendants(MagikGrammar.ARGUMENT);

    final AstNode argumentNode0 = argumentNodes.get(0);
    final AstNode atomNode0 = argumentNode0.getFirstDescendant(MagikGrammar.ATOM);
    final ExpressionResultString result0 = reasonerState.getNodeType(atomNode0);
    assertThat(result0).isEqualTo(new ExpressionResultString(TypeString.SW_UNSET));

    final AstNode argumentNode1 = argumentNodes.get(1);
    final AstNode atomNode1 = argumentNode1.getFirstDescendant(MagikGrammar.ATOM);
    final ExpressionResultString result1 = reasonerState.getNodeType(atomNode1);
    assertThat(result1).isEqualTo(new ExpressionResultString(TypeString.SW_INTEGER));
  }

  @Test
  void testHandleIfIsUnsetElifIsKindOfElse() {
    final String code =
        """
        _method object.method(_optional param1)
          ## @param {sw:integer|sw:symbol} param1
          _if param1.is_kind_of?(sw:integer)
          _then
            show(param1)
          _elif param1.is_kind_of?(sw:symbol)
          _then
            show(param1)
          _else
            show(param1)
          _endif
        _endmethod
        """;

    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
    final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

    final AstNode topNode = magikFile.getTopNode();
    final List<AstNode> argumentNodes = topNode.getDescendants(MagikGrammar.ARGUMENT);

    final AstNode argumentNode1 = argumentNodes.get(1);
    final AstNode atomNode1 = argumentNode1.getFirstDescendant(MagikGrammar.ATOM);
    final ExpressionResultString result1 = reasonerState.getNodeType(atomNode1);
    assertThat(result1).isEqualTo(new ExpressionResultString(TypeString.SW_INTEGER));

    final AstNode argumentNode3 = argumentNodes.get(3);
    final AstNode atomNode3 = argumentNode3.getFirstDescendant(MagikGrammar.ATOM);
    final ExpressionResultString result3 = reasonerState.getNodeType(atomNode3);
    assertThat(result3).isEqualTo(new ExpressionResultString(TypeString.SW_SYMBOL));

    final AstNode argumentNode4 = argumentNodes.get(4);
    final AstNode atomNode4 = argumentNode4.getFirstDescendant(MagikGrammar.ATOM);
    final ExpressionResultString result4 = reasonerState.getNodeType(atomNode4);
    assertThat(result4).isEqualTo(new ExpressionResultString(TypeString.SW_UNSET));
  }

  @Test
  void testHandleIfIsKindOf() {
    final String code =
        """
        _method object.method(param1)
          _if param1.is_kind_of?(integer)
          _then
            show(param1)
          _endif
        _endmethod
        """;

    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
    final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

    final AstNode topNode = magikFile.getTopNode();
    final List<AstNode> argumentNodes = topNode.getDescendants(MagikGrammar.ARGUMENT);
    final AstNode argumentNode1 = argumentNodes.get(1);
    final AstNode atomNode1 = argumentNode1.getFirstDescendant(MagikGrammar.ATOM);
    final ExpressionResultString result1 = reasonerState.getNodeType(atomNode1);
    assertThat(result1).isEqualTo(new ExpressionResultString(TypeString.SW_INTEGER));
  }

  @Test
  void testHandleIfMethodResult() {
    final String code =
        """
        _method object.method(param1)
          _if param1 _is object.unset_result
          _then
            show(param1)
          _endif
        _endmethod
        """;

    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new MethodDefinition(
            null,
            null,
            null,
            null,
            TypeString.SW_OBJECT,
            "unset_result",
            Collections.emptySet(),
            Collections.emptyList(),
            null,
            Collections.emptySet(),
            new ExpressionResultString(TypeString.SW_UNSET),
            ExpressionResultString.EMPTY));
    final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
    final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

    final AstNode topNode = magikFile.getTopNode();
    final List<AstNode> argumentNodes = topNode.getDescendants(MagikGrammar.ARGUMENT);
    final AstNode argumentNode0 = argumentNodes.get(0);
    final AstNode atomNode0 = argumentNode0.getFirstDescendant(MagikGrammar.ATOM);
    final ExpressionResultString result0 = reasonerState.getNodeType(atomNode0);
    assertThat(result0).isEqualTo(new ExpressionResultString(TypeString.SW_UNSET));
  }

  @Test
  void testHandleIfMethodResultBothSides() {
    final String code =
        """
        _method object.method(param1)
          _if object.unset_result _is object.unset_result
          _then
            show(param1)
          _endif
        _endmethod
        """;

    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new MethodDefinition(
            null,
            null,
            null,
            null,
            TypeString.SW_OBJECT,
            "unset_result",
            Collections.emptySet(),
            Collections.emptyList(),
            null,
            Collections.emptySet(),
            new ExpressionResultString(TypeString.SW_UNSET),
            ExpressionResultString.EMPTY));
    final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
    final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

    final AstNode topNode = magikFile.getTopNode();
    final List<AstNode> argumentNodes = topNode.getDescendants(MagikGrammar.ARGUMENT);
    final AstNode argumentNode0 = argumentNodes.get(0);
    final AstNode atomNode0 = argumentNode0.getFirstDescendant(MagikGrammar.ATOM);
    final ExpressionResultString result0 = reasonerState.getNodeType(atomNode0);
    assertThat(result0).isEqualTo(ExpressionResultString.UNDEFINED);
  }

  @Test
  void testHandleIfReturn() {
    final String code =
        """
        _method object.method(_optional param1)
          ## @param {sw:symbol} param1
          _if param1 _is _unset
          _then
            show(param1)
            _return
          _endif
          show(param1)
        _endmethod
        """;

    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
    final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

    final AstNode topNode = magikFile.getTopNode();
    final List<AstNode> argumentNodes = topNode.getDescendants(MagikGrammar.ARGUMENT);

    final AstNode argumentNode0 = argumentNodes.get(0);
    final AstNode atomNode0 = argumentNode0.getFirstDescendant(MagikGrammar.ATOM);
    final ExpressionResultString result0 = reasonerState.getNodeType(atomNode0);
    assertThat(result0).isEqualTo(new ExpressionResultString(TypeString.SW_UNSET));

    final AstNode argumentNode1 = argumentNodes.get(1);
    final AstNode atomNode1 = argumentNode1.getFirstDescendant(MagikGrammar.ATOM);
    final ExpressionResultString result1 = reasonerState.getNodeType(atomNode1);
    assertThat(result1).isEqualTo(new ExpressionResultString(TypeString.SW_SYMBOL));
  }

  @Test
  void testHandleIfRaisesError() {
    final String code =
        """
        _method object.method(_optional param1)
          ## @param {sw:symbol} param1
          _if param1 _is _unset
          _then
            show(param1)
            condition.raise(:error)
          _endif
          show(param1)
        _endmethod
        """;

    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new ConditionDefinition(null, null, null, null, "error", null, Collections.emptyList()));

    final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
    final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

    final AstNode topNode = magikFile.getTopNode();
    final List<AstNode> argumentNodes = topNode.getDescendants(MagikGrammar.ARGUMENT);

    final AstNode argumentNode0 = argumentNodes.get(0);
    final AstNode atomNode0 = argumentNode0.getFirstDescendant(MagikGrammar.ATOM);
    final ExpressionResultString result0 = reasonerState.getNodeType(atomNode0);
    assertThat(result0).isEqualTo(new ExpressionResultString(TypeString.SW_UNSET));

    final AstNode argumentNode2 = argumentNodes.get(2);
    final AstNode atomNode2 = argumentNode2.getFirstDescendant(MagikGrammar.ATOM);
    final ExpressionResultString result2 = reasonerState.getNodeType(atomNode2);
    assertThat(result2).isEqualTo(new ExpressionResultString(TypeString.SW_SYMBOL));
  }

  @Test
  void testHandleIfElseRaisesError() {
    final String code =
        """
        _method object.method(_optional param1)
          ## @param {sw:symbol|sw:character} param1
          _if param1 _is _unset
          _then
            show(param1)
          _else
            show(param1)
            condition.raise(:child_error)
          _endif
          show(param1)
        _endmethod
        """;

    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new ConditionDefinition(null, null, null, null, "error", null, Collections.emptyList()));
    definitionKeeper.add(
        new ConditionDefinition(
            null, null, null, null, "child_error", "error", Collections.emptyList()));

    final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
    final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

    final AstNode topNode = magikFile.getTopNode();
    final List<AstNode> argumentNodes = topNode.getDescendants(MagikGrammar.ARGUMENT);

    final AstNode argumentNode0 = argumentNodes.get(0);
    final AstNode atomNode0 = argumentNode0.getFirstDescendant(MagikGrammar.ATOM);
    final ExpressionResultString result0 = reasonerState.getNodeType(atomNode0);
    assertThat(result0).isEqualTo(new ExpressionResultString(TypeString.SW_UNSET));

    final AstNode argumentNode1 = argumentNodes.get(1);
    final AstNode atomNode1 = argumentNode1.getFirstDescendant(MagikGrammar.ATOM);
    final ExpressionResultString result1 = reasonerState.getNodeType(atomNode1);
    assertThat(result1)
        .isEqualTo(
            new ExpressionResultString(
                TypeString.ofCombination(TypeString.SW_CHARACTER, TypeString.SW_SYMBOL)));

    final AstNode argumentNode3 = argumentNodes.get(3);
    final AstNode atomNode3 = argumentNode3.getFirstDescendant(MagikGrammar.ATOM);
    final ExpressionResultString result3 = reasonerState.getNodeType(atomNode3);
    assertThat(result3).isEqualTo(new ExpressionResultString(TypeString.SW_UNSET));
  }
}
