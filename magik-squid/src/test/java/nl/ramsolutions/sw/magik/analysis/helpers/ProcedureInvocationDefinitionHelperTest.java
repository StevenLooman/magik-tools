package nl.ramsolutions.sw.magik.analysis.helpers;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonar.sslr.api.AstNode;
import java.util.Collection;
import java.util.List;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ProcedureDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeStringResolver;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasonerState;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import org.junit.jupiter.api.Test;

/** Tests for {@link ProcedureInvocationDefinitionHelper}. */
class ProcedureInvocationDefinitionHelperTest {

  @Test
  void testInlineProcedureFallbackResolution() {
    final String code =
        """
        _block
          (_proc()
            _return 1
          _endproc)()
        _endblock
        """;

    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedFile magikFile =
        new MagikTypedFile(MagikTypedFile.DEFAULT_URI, code, definitionKeeper);
    final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();
    final TypeStringResolver resolver = magikFile.getTypeStringResolver();

    final AstNode invocationNode =
        magikFile.getTopNode().getFirstDescendant(MagikGrammar.PROCEDURE_INVOCATION);
    final Collection<ProcedureDefinition> procedureDefs =
        ProcedureInvocationDefinitionHelper.getRespondingProcedureDefinitions(
            invocationNode, reasonerState, resolver, magikFile);

    assertThat(procedureDefs).isNotEmpty();
  }

  @Test
  void testMissingReceiverReturnsNoDefinitions() {
    final String code =
        """
        _method object.test
          _return 1
        _endmethod
        """;

    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedFile magikFile =
        new MagikTypedFile(MagikTypedFile.DEFAULT_URI, code, definitionKeeper);
    final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();
    final TypeStringResolver resolver = magikFile.getTypeStringResolver();

    final AstNode methodDefNode =
        magikFile.getTopNode().getFirstDescendant(MagikGrammar.METHOD_DEFINITION);
    final Collection<ProcedureDefinition> procedureDefs =
        ProcedureInvocationDefinitionHelper.getRespondingProcedureDefinitions(
            methodDefNode, reasonerState, resolver, magikFile);

    assertThat(procedureDefs).isEmpty();
    final TypeString invokedOnType =
        ProcedureInvocationDefinitionHelper.getProcedureTypeInvokedOn(
            methodDefNode, reasonerState, resolver);
    assertThat(invokedOnType).isEqualTo(TypeString.UNDEFINED);
  }

  private Collection<ProcedureDefinition> getLastInvocationProcedureDefinitions(
      final MagikTypedFile magikFile) {
    final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();
    final TypeStringResolver resolver = magikFile.getTypeStringResolver();
    final AstNode topNode = magikFile.getTopNode();
    final List<AstNode> invocationNodes = topNode.getDescendants(MagikGrammar.PROCEDURE_INVOCATION);
    final int lastIndex = invocationNodes.size() - 1;
    final AstNode invocationNode = invocationNodes.get(lastIndex);
    return ProcedureInvocationDefinitionHelper.getRespondingProcedureDefinitions(
        invocationNode, reasonerState, resolver, magikFile);
  }

  private MagikTypedFile createMagikFile(final String code) {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    return new MagikTypedFile(MagikTypedFile.DEFAULT_URI, code, definitionKeeper);
  }

  @Test
  void testInlineProcedureFallbackFindsUnparenthesizedProcedure() {
    final String code =
        """
        _block
          _proc()
            _return 1
          _endproc()
        _endblock
        """;
    final MagikTypedFile magikFile = this.createMagikFile(code);

    final Collection<ProcedureDefinition> procedureDefs =
        this.getLastInvocationProcedureDefinitions(magikFile);

    final AstNode topNode = magikFile.getTopNode();
    final AstNode procedureNode = topNode.getFirstDescendant(MagikGrammar.PROCEDURE_DEFINITION);
    final List<AstNode> definitionNodes =
        procedureDefs.stream().map(ProcedureDefinition::getNode).toList();
    assertThat(definitionNodes).containsExactly(procedureNode);
  }

  @Test
  void testInlineProcedureFallbackLooksThroughNestedParentheses() {
    final String code =
        """
        _block
          ((_proc()
            _return 1
          _endproc))()
        _endblock
        """;
    final MagikTypedFile magikFile = this.createMagikFile(code);

    final Collection<ProcedureDefinition> procedureDefs =
        this.getLastInvocationProcedureDefinitions(magikFile);

    final AstNode topNode = magikFile.getTopNode();
    final AstNode procedureNode = topNode.getFirstDescendant(MagikGrammar.PROCEDURE_DEFINITION);
    final List<AstNode> definitionNodes =
        procedureDefs.stream().map(ProcedureDefinition::getNode).toList();
    assertThat(definitionNodes).containsExactly(procedureNode);
  }

  @Test
  void testInlineProcedureFallbackSkipsMethodArgument() {
    final String code =
        """
        _block
          x.b(_proc()
            ## @return {sw:symbol}
            _return :a
          _endproc)()
        _endblock
        """;
    final MagikTypedFile magikFile = this.createMagikFile(code);

    final Collection<ProcedureDefinition> procedureDefs =
        this.getLastInvocationProcedureDefinitions(magikFile);

    assertThat(procedureDefs).isEmpty();
  }

  @Test
  void testInlineProcedureFallbackSkipsArgumentOfEarlierCall() {
    final String code =
        """
        _block
          _proc(p)
            _return p
          _endproc(_proc()
            _return 1
          _endproc)()
        _endblock
        """;
    final MagikTypedFile magikFile = this.createMagikFile(code);

    final Collection<ProcedureDefinition> procedureDefs =
        this.getLastInvocationProcedureDefinitions(magikFile);

    assertThat(procedureDefs).isEmpty();
  }

  @Test
  void testInlineProcedureFallbackSkipsParenthesizedList() {
    final String code =
        """
        _block
          (x, _proc()
            _return 1
          _endproc)()
        _endblock
        """;
    final MagikTypedFile magikFile = this.createMagikFile(code);

    final Collection<ProcedureDefinition> procedureDefs =
        this.getLastInvocationProcedureDefinitions(magikFile);

    assertThat(procedureDefs).isEmpty();
  }
}
