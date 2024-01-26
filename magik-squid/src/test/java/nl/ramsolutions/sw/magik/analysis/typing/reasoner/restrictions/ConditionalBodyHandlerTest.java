package nl.ramsolutions.sw.magik.analysis.typing.reasoner.restrictions;

import com.sonar.sslr.api.AstNode;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasonerState;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResult;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConditionalBodyHandler}.
 */
class ConditionalBodyHandlerTest {

    private MagikTypedFile createMagikFile(String code, IDefinitionKeeper definitionKeeper) {
        final URI uri = URI.create("tests://unittest");
        return new MagikTypedFile(uri, code, definitionKeeper);
    }

    @Test
    void testHandleIfIsUnsetOfUndefined() {
        final String code = ""
            + "_method object.method(param1)\n"
            + "  _if param1 _is _unset\n"
            + "  _then\n"
            + "    show(param1)\n"
            + "  _endif\n"
            + "_endmethod\n";

        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        final AstNode topNode = magikFile.getTopNode();
        final AstNode argumentNode = topNode.getFirstDescendant(MagikGrammar.ARGUMENT);
        final AstNode atomNode = argumentNode.getFirstDescendant(MagikGrammar.ATOM);
        final ExpressionResult result = reasonerState.getNodeType(atomNode);
        final AbstractType type0 = result.get(0, null);
        assertThat(type0.getTypeString()).isEqualTo(TypeString.SW_UNSET);
    }

    @Test
    void testHandleIfIsntUnsetOfUndefined() {
        final String code = ""
            + "_method object.method(param1)\n"
            + "  _if param1 _isnt _unset\n"
            + "  _then\n"
            + "    show(param1)\n"
            + "  _endif\n"
            + "_endmethod\n";

        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        final AstNode topNode = magikFile.getTopNode();
        final AstNode argumentNode = topNode.getFirstDescendant(MagikGrammar.ARGUMENT);
        final AstNode atomNode = argumentNode.getFirstDescendant(MagikGrammar.ATOM);
        final ExpressionResult result = reasonerState.getNodeType(atomNode);
        final AbstractType type0 = result.get(0, null);
        assertThat(type0.getTypeString()).isEqualTo(TypeString.UNDEFINED);
    }

    @Test
    void testHandleIfIsIntegerValueOfUndefined() {
        final String code = ""
            + "_method object.method(param1)\n"
            + "  _if param1 _is 5\n"
            + "  _then\n"
            + "    show(param1)\n"
            + "  _endif\n"
            + "_endmethod\n";

        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        final AstNode topNode = magikFile.getTopNode();
        final AstNode argumentNode = topNode.getFirstDescendant(MagikGrammar.ARGUMENT);
        final AstNode atomNode = argumentNode.getFirstDescendant(MagikGrammar.ATOM);
        final ExpressionResult result = reasonerState.getNodeType(atomNode);
        final AbstractType type0 = result.get(0, null);
        assertThat(type0.getTypeString()).isEqualTo(TypeString.SW_INTEGER);
    }

    @Test
    void testHandleIfOr() {
        // Or-expression are not supported.
        final String code = ""
            + "_method object.method(param1)\n"
            + "  _if param1 _is _unset _orif\n"
            + "      param1 _is :a\n"
            + "  _then\n"
            + "    show(param1)\n"
            + "  _else\n"
            + "    show(param1)\n"
            + "  _endif\n"
            + "_endmethod\n";

        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        final AstNode topNode = magikFile.getTopNode();
        final List<AstNode> argumentNodes = topNode.getDescendants(MagikGrammar.ARGUMENT);

        final AstNode argumentNode0 = argumentNodes.get(0);
        final AstNode atomNode0 = argumentNode0.getFirstDescendant(MagikGrammar.ATOM);
        final ExpressionResult result0 = reasonerState.getNodeType(atomNode0);
        final AbstractType type0 = result0.get(0, null);
        assertThat(type0.getTypeString()).isEqualTo(TypeString.UNDEFINED);

        final AstNode argumentNode1 = argumentNodes.get(1);
        final AstNode atomNode1 = argumentNode1.getFirstDescendant(MagikGrammar.ATOM);
        final ExpressionResult result1 = reasonerState.getNodeType(atomNode1);
        final AbstractType type1 = result1.get(0, null);
        assertThat(type1.getTypeString()).isEqualTo(TypeString.UNDEFINED);
    }

    @Test
    void testHandleIfAnd() {
        // And-expression are not supported.
        final String code = ""
            + "_method object.method(param1, param2)\n"
            + "  _if param1 _is _unset _andif\n"
            + "      param2 _is _unset\n"
            + "  _then\n"
            + "    show(param1)\n"
            + "    show(param2)\n"
            + "  _else\n"
            + "    show(param1)\n"
            + "    show(param2)\n"
            + "  _endif\n"
            + "_endmethod\n";

        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        final AstNode topNode = magikFile.getTopNode();
        final List<AstNode> argumentNodes = topNode.getDescendants(MagikGrammar.ARGUMENT);

        final AstNode argumentNode0 = argumentNodes.get(0);
        final AstNode atomNode0 = argumentNode0.getFirstDescendant(MagikGrammar.ATOM);
        final ExpressionResult result0 = reasonerState.getNodeType(atomNode0);
        final AbstractType type0 = result0.get(0, null);
        assertThat(type0.getTypeString()).isEqualTo(TypeString.UNDEFINED);

        final AstNode argumentNode1 = argumentNodes.get(1);
        final AstNode atomNode1 = argumentNode1.getFirstDescendant(MagikGrammar.ATOM);
        final ExpressionResult result1 = reasonerState.getNodeType(atomNode1);
        final AbstractType type1 = result1.get(0, null);
        assertThat(type1.getTypeString()).isEqualTo(TypeString.UNDEFINED);

        final AstNode argumentNode2 = argumentNodes.get(2);
        final AstNode atomNode2 = argumentNode2.getFirstDescendant(MagikGrammar.ATOM);
        final ExpressionResult result2 = reasonerState.getNodeType(atomNode2);
        final AbstractType type2 = result2.get(0, null);
        assertThat(type2.getTypeString()).isEqualTo(TypeString.UNDEFINED);

        // And-expression are not supported.
        final AstNode argumentNode3 = argumentNodes.get(3);
        final AstNode atomNode3 = argumentNode3.getFirstDescendant(MagikGrammar.ATOM);
        final ExpressionResult result3 = reasonerState.getNodeType(atomNode3);
        final AbstractType type3 = result3.get(0, null);
        assertThat(type3.getTypeString()).isEqualTo(TypeString.UNDEFINED);
    }

    @Test
    void testHandleIfMethodCall() {
        final String code = ""
            + "_method object.method(param1)\n"
            + "  _if param1.test?\n"
            + "  _then\n"
            + "    show(param1)\n"
            + "  _endif\n"
            + "_endmethod\n";

        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        final AstNode topNode = magikFile.getTopNode();
        final AstNode argumentNode = topNode.getFirstDescendant(MagikGrammar.ARGUMENT);
        final AstNode atomNode = argumentNode.getFirstDescendant(MagikGrammar.ATOM);
        final ExpressionResult result = reasonerState.getNodeType(atomNode);
        final AbstractType type0 = result.get(0, null);
        final TypeString typeRef = type0.getTypeString();
        assertThat(typeRef).isEqualTo(TypeString.UNDEFINED);
    }

    @Test
    void testHandleIfIsUnsetThenAssigned() {
        final String code = ""
            + "_method object.method()\n"
            + "  _local var << _self.method2()  # type: sw:integer|sw:unset\n"
            + "  _if var _is _unset\n"
            + "  _then\n"
            + "    show(var)\n"
            + "    var << 10\n"
            + "    show(var)\n"
            + "  _endif\n"
            + "_endmethod\n";

        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        final AstNode topNode = magikFile.getTopNode();
        final List<AstNode> argumentNodes = topNode.getDescendants(MagikGrammar.ARGUMENT);

        // After positive test for _unset.
        final AstNode argument0Node = argumentNodes.get(0);
        final AstNode atom0Node = argument0Node.getFirstDescendant(MagikGrammar.ATOM);
        final ExpressionResult result0 = reasonerState.getNodeType(atom0Node);
        final AbstractType type0 = result0.get(0, null);
        final TypeString typeRef0 = type0.getTypeString();
        assertThat(typeRef0).isEqualTo(TypeString.SW_UNSET);

        // After assigning integer.
        final AstNode argument1Node = argumentNodes.get(1);
        final AstNode atom1Node = argument1Node.getFirstDescendant(MagikGrammar.ATOM);
        final ExpressionResult result1 = reasonerState.getNodeType(atom1Node);
        final AbstractType type1 = result1.get(0, null);
        final TypeString typeRef1 = type1.getTypeString();
        assertThat(typeRef1).isEqualTo(TypeString.SW_INTEGER);
    }

    @Test
    void testHandleIfIsUnset2() {
        final String code = ""
            + "_method object.method(param1)\n"
            + "  _if _unset _is param1\n"
            + "  _then\n"
            + "    show(param1)\n"
            + "  _endif\n"
            + "_endmethod\n";

        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        final AstNode topNode = magikFile.getTopNode();
        final AstNode argumentNode = topNode.getFirstDescendant(MagikGrammar.ARGUMENT);
        final AstNode atomNode = argumentNode.getFirstDescendant(MagikGrammar.ATOM);
        final ExpressionResult result = reasonerState.getNodeType(atomNode);
        final AbstractType type0 = result.get(0, null);
        final TypeString typeRef = type0.getTypeString();
        assertThat(typeRef).isEqualTo(TypeString.SW_UNSET);
    }

    @Test
    void testHandleIfIsntUnset() {
        final String code = ""
            + "_method object.method(param1)\n"
            + "  ## @param {sw:integer|sw:unset} param1\n"
            + "  _if param1 _isnt _unset\n"
            + "  _then\n"
            + "    show(param1)\n"
            + "  _endif\n"
            + "_endmethod\n";

        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        final AstNode topNode = magikFile.getTopNode();
        final AstNode argumentNode = topNode.getFirstDescendant(MagikGrammar.ARGUMENT);
        final AstNode atomNode = argumentNode.getFirstDescendant(MagikGrammar.ATOM);
        final ExpressionResult result = reasonerState.getNodeType(atomNode);
        final AbstractType type0 = result.get(0, null);
        assertThat(type0.getTypeString()).isEqualTo(TypeString.SW_INTEGER);
    }

    @Test
    void testHandleIfIsUnsetElseIsKindOf() {
        final String code = ""
            + "_method object.method(param1)\n"
            + "  _if param1 _is _unset\n"
            + "  _then\n"
            + "    show(param1)\n"
            + "  _elif param1.is_kind_of?(integer)\n"
            + "  _then\n"
            + "    show(param1)\n"
            + "  _endif\n"
            + "_endmethod\n";

        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        final AstNode topNode = magikFile.getTopNode();
        final List<AstNode> argumentNodes = topNode.getDescendants(MagikGrammar.ARGUMENT);

        final AstNode argumentNode0 = argumentNodes.get(0);
        final AstNode atomNode0 = argumentNode0.getFirstDescendant(MagikGrammar.ATOM);
        final ExpressionResult result0 = reasonerState.getNodeType(atomNode0);
        final AbstractType type0 = result0.get(0, null);
        assertThat(type0.getTypeString()).isEqualTo(TypeString.SW_UNSET);

        final AstNode argumentNode2 = argumentNodes.get(2);
        final AstNode atomNode2 = argumentNode2.getFirstDescendant(MagikGrammar.ATOM);
        final ExpressionResult result2 = reasonerState.getNodeType(atomNode2);
        final AbstractType type2 = result2.get(0, null);
        assertThat(type2.getTypeString()).isEqualTo(TypeString.SW_INTEGER);
    }

    @Test
    void testHandleIfNotIsKindOf() {
        final String code = ""
            + "_method object.method(param1)\n"
            + "  ## @param {sw:integer|sw:symbol} param1\n"
            + "  _if _not param1.is_kind_of?(integer)\n"
            + "  _then\n"
            + "    show(param1)\n"
            + "  _endif\n"
            + "_endmethod\n";

        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        final AstNode topNode = magikFile.getTopNode();
        final List<AstNode> argumentNodes = topNode.getDescendants(MagikGrammar.ARGUMENT);

        final AstNode argumentNode1 = argumentNodes.get(1);
        final AstNode atomNode1 = argumentNode1.getFirstDescendant(MagikGrammar.ATOM);
        final ExpressionResult result1 = reasonerState.getNodeType(atomNode1);
        final AbstractType type1 = result1.get(0, null);
        assertThat(type1.getTypeString()).isEqualTo(TypeString.SW_SYMBOL);
    }

    @Test
    void testHandleUnreasonable() {
        final String code = ""
            + "_method object.method(param1)\n"
            + "  ## @param {sw:integer|sw:symbol} param1\n"
            + "  _if param1.is_kind_of?(integer)\n"
            + "  _then\n"
            + "    show(param1)\n"
            + "  _elif param1.is_kind_of?(symbol)\n"
            + "  _then\n"
            + "    show(param1)\n"
            + "  _else\n"
            + "    show(param1)\n"
            + "  _endif\n"
            + "_endmethod\n";

        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        final AstNode topNode = magikFile.getTopNode();
        final List<AstNode> argumentNodes = topNode.getDescendants(MagikGrammar.ARGUMENT);

        final AstNode argumentNode1 = argumentNodes.get(1);
        final AstNode atomNode1 = argumentNode1.getFirstDescendant(MagikGrammar.ATOM);
        final ExpressionResult result1 = reasonerState.getNodeType(atomNode1);
        final AbstractType type1 = result1.get(0, null);
        assertThat(type1.getTypeString()).isEqualTo(TypeString.SW_INTEGER);

        final AstNode argumentNode3 = argumentNodes.get(3);
        final AstNode atomNode3 = argumentNode3.getFirstDescendant(MagikGrammar.ATOM);
        final ExpressionResult result3 = reasonerState.getNodeType(atomNode3);
        final AbstractType type3 = result3.get(0, null);
        assertThat(type3.getTypeString()).isEqualTo(TypeString.SW_SYMBOL);

        // The type in the else block cannot be determined, thus take the type it was given.
        final AstNode argumentNode4 = argumentNodes.get(4);
        final AstNode atomNode4 = argumentNode4.getFirstDescendant(MagikGrammar.ATOM);
        final ExpressionResult result4 = reasonerState.getNodeType(atomNode4);
        final AbstractType type4 = result4.get(0, null);
        assertThat(type4.getTypeString()).isEqualTo(
            TypeString.ofCombination(
                "sw",
                TypeString.SW_SYMBOL, TypeString.SW_INTEGER));
    }

    @Test
    void testHandleUnreasonable2() {
        final String code = ""
            + "_method object.method(param1)\n"
            + "  ## @param {sw:integer|sw:symbol} param1\n"
            + "  _if param1 > 10\n"
            + "  _then\n"
            + "    show(param1)\n"
            + "  _else\n"
            + "    show(param1)\n"
            + "  _endif\n"
            + "_endmethod\n";

        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        final AstNode topNode = magikFile.getTopNode();
        final List<AstNode> argumentNodes = topNode.getDescendants(MagikGrammar.ARGUMENT);

        final AstNode argumentNode0 = argumentNodes.get(0);
        final AstNode atomNode0 = argumentNode0.getFirstDescendant(MagikGrammar.ATOM);
        final ExpressionResult result0 = reasonerState.getNodeType(atomNode0);
        final AbstractType type0 = result0.get(0, null);
        assertThat(type0.getTypeString()).isEqualTo(
            TypeString.ofCombination(
                "sw",
                TypeString.SW_SYMBOL, TypeString.SW_INTEGER));

        final AstNode argumentNode1 = argumentNodes.get(1);
        final AstNode atomNode1 = argumentNode1.getFirstDescendant(MagikGrammar.ATOM);
        final ExpressionResult result1 = reasonerState.getNodeType(atomNode1);
        final AbstractType type1 = result1.get(0, null);
        assertThat(type1.getTypeString()).isEqualTo(
            TypeString.ofCombination(
                "sw",
                TypeString.SW_SYMBOL, TypeString.SW_INTEGER));
    }

    @Test
    void testHandleIfIsUnsetElse() {
        final String code = ""
            + "_method object.method(_optional param1)\n"
            + "  ## @param {sw:integer} param1\n"
            + "  _if _unset _is param1\n"
            + "  _then\n"
            + "    show(param1)\n"
            + "  _else\n"
            + "    show(param1)\n"
            + "  _endif\n"
            + "_endmethod\n";

        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        final AstNode topNode = magikFile.getTopNode();
        final List<AstNode> argumentNodes = topNode.getDescendants(MagikGrammar.ARGUMENT);

        final AstNode argumentNode0 = argumentNodes.get(0);
        final AstNode atomNode0 = argumentNode0.getFirstDescendant(MagikGrammar.ATOM);
        final ExpressionResult result0 = reasonerState.getNodeType(atomNode0);
        final AbstractType type0 = result0.get(0, null);
        assertThat(type0.getTypeString()).isEqualTo(TypeString.SW_UNSET);

        final AstNode argumentNode1 = argumentNodes.get(1);
        final AstNode atomNode1 = argumentNode1.getFirstDescendant(MagikGrammar.ATOM);
        final ExpressionResult result1 = reasonerState.getNodeType(atomNode1);
        final AbstractType type1 = result1.get(0, null);
        assertThat(type1.getTypeString()).isEqualTo(TypeString.SW_INTEGER);
    }

    @Test
    void testHandleIfIsUnsetElifIsKindOfElse() {
        final String code = ""
            + "_method object.method(_optional param1)\n"
            + "  ## @param {sw:integer|sw:symbol} param1\n"
            + "  _if param1.is_kind_of?(sw:integer)\n"
            + "  _then\n"
            + "    show(param1)\n"
            + "  _elif param1.is_kind_of?(sw:symbol)\n"
            + "  _then\n"
            + "    show(param1)\n"
            + "  _else\n"
            + "    show(param1)\n"
            + "  _endif\n"
            + "_endmethod\n";

        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        final AstNode topNode = magikFile.getTopNode();
        final List<AstNode> argumentNodes = topNode.getDescendants(MagikGrammar.ARGUMENT);

        final AstNode argumentNode1 = argumentNodes.get(1);
        final AstNode atomNode1 = argumentNode1.getFirstDescendant(MagikGrammar.ATOM);
        final ExpressionResult result1 = reasonerState.getNodeType(atomNode1);
        final AbstractType type1 = result1.get(0, null);
        assertThat(type1.getTypeString()).isEqualTo(TypeString.SW_INTEGER);

        final AstNode argumentNode3 = argumentNodes.get(3);
        final AstNode atomNode3 = argumentNode3.getFirstDescendant(MagikGrammar.ATOM);
        final ExpressionResult result3 = reasonerState.getNodeType(atomNode3);
        final AbstractType type3 = result3.get(0, null);
        assertThat(type3.getTypeString()).isEqualTo(TypeString.SW_SYMBOL);

        final AstNode argumentNode4 = argumentNodes.get(4);
        final AstNode atomNode4 = argumentNode4.getFirstDescendant(MagikGrammar.ATOM);
        final ExpressionResult result4 = reasonerState.getNodeType(atomNode4);
        final AbstractType type4 = result4.get(0, null);
        assertThat(type4.getTypeString()).isEqualTo(TypeString.SW_UNSET);
    }

    @Test
    void testHandleIfIsKindOf() {
        final String code = ""
            + "_method object.method(param1)\n"
            + "  _if param1.is_kind_of?(integer)\n"
            + "  _then\n"
            + "    show(param1)\n"
            + "  _endif\n"
            + "_endmethod\n";

        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        final AstNode topNode = magikFile.getTopNode();
        final List<AstNode> argumentNodes = topNode.getDescendants(MagikGrammar.ARGUMENT);
        final AstNode argumentNode1 = argumentNodes.get(1);
        final AstNode atomNode1 = argumentNode1.getFirstDescendant(MagikGrammar.ATOM);
        final ExpressionResult result1 = reasonerState.getNodeType(atomNode1);
        final AbstractType type0 = result1.get(0, null);
        assertThat(type0.getTypeString()).isEqualTo(TypeString.SW_INTEGER);
    }

    @Test
    void testHandleIfMethodResult() {
        final String code = ""
            + "_method object.method(param1)\n"
            + "  _if param1 _is object.unset_result\n"
            + "  _then\n"
            + "    show(param1)\n"
            + "  _endif\n"
            + "_endmethod\n";

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
                new ExpressionResultString(
                    TypeString.SW_UNSET),
                ExpressionResultString.EMPTY));
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        final AstNode topNode = magikFile.getTopNode();
        final List<AstNode> argumentNodes = topNode.getDescendants(MagikGrammar.ARGUMENT);
        final AstNode argumentNode0 = argumentNodes.get(0);
        final AstNode atomNode0 = argumentNode0.getFirstDescendant(MagikGrammar.ATOM);
        final ExpressionResult result0 = reasonerState.getNodeType(atomNode0);
        final AbstractType type0 = result0.get(0, null);
        assertThat(type0.getTypeString()).isEqualTo(TypeString.SW_UNSET);
    }

    @Test
    void testHandleIfMethodResultBothSides() {
        final String code = ""
            + "_method object.method(param1)\n"
            + "  _if object.unset_result _is object.unset_result\n"
            + "  _then\n"
            + "    show(param1)\n"
            + "  _endif\n"
            + "_endmethod\n";

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
                new ExpressionResultString(
                    TypeString.SW_UNSET),
                ExpressionResultString.EMPTY));
        final MagikTypedFile magikFile = this.createMagikFile(code, definitionKeeper);
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

        final AstNode topNode = magikFile.getTopNode();
        final List<AstNode> argumentNodes = topNode.getDescendants(MagikGrammar.ARGUMENT);
        final AstNode argumentNode0 = argumentNodes.get(0);
        final AstNode atomNode0 = argumentNode0.getFirstDescendant(MagikGrammar.ATOM);
        final ExpressionResult result0 = reasonerState.getNodeType(atomNode0);
        final AbstractType type0 = result0.get(0, null);
        assertThat(type0.getTypeString()).isEqualTo(TypeString.UNDEFINED);
    }

}
