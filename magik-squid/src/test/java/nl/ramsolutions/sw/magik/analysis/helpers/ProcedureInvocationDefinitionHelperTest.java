package nl.ramsolutions.sw.magik.analysis.helpers;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonar.sslr.api.AstNode;
import java.util.Collection;
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
}
