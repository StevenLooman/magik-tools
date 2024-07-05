package nl.ramsolutions.sw.magik.analysis.typing.reasoner;

import com.sonar.sslr.api.AstNode;
import java.util.List;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.definitions.MagikDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ProcedureDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.parsers.ProcedureDefinitionParser;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;

/** Procedure definition handler. */
class ProcedureDefinitionHandler extends LocalTypeReasonerHandler {

  /**
   * Constructor.
   *
   * @param state Reasoner state.
   */
  ProcedureDefinitionHandler(final LocalTypeReasonerState state) {
    super(state);
  }

  /**
   * Handle procedure definition.
   *
   * @param node PROCEDURE_DEFINITION node.
   */
  void handleProcedureDefinition(final AstNode node) {
    final MagikTypedFile magikFile = state.getMagikFile();
    final ProcedureDefinitionParser parser = new ProcedureDefinitionParser(magikFile, node);
    final List<MagikDefinition> definitions = parser.parseDefinitions();
    if (definitions.isEmpty()) {
      // Possibly a syntax error or incomplete MagikGrammar.
      this.assignAtom(node, ExpressionResultString.UNDEFINED);
      return;
    }

    final ProcedureDefinition procDef = (ProcedureDefinition) definitions.iterator().next();

    // Try to determine result if none was given via type-doc.
    final ProcedureDefinition finalProcDef =
        procDef.getReturnTypes().equals(ExpressionResultString.UNDEFINED)
            ? new ProcedureDefinition(
                procDef.getLocation(),
                procDef.getTimestamp(),
                procDef.getModuleName(),
                procDef.getDoc(),
                procDef.getNode(),
                procDef.getModifiers(),
                procDef.getTypeString(),
                procDef.getProcedureName(),
                procDef.getParameters(),
                this.state.getNodeType(node),
                this.state.getNodeIterType(node))
            : procDef;

    // Store result.
    final TypeString typeStr = finalProcDef.getTypeString();
    final ExpressionResultString result = new ExpressionResultString(typeStr);
    this.assignAtom(node, result);
  }
}
