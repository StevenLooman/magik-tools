package nl.ramsolutions.sw.magik.analysis.typing.reasoner;

import com.sonar.sslr.api.AstNode;
import java.util.List;
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
    final ProcedureDefinitionParser parser = new ProcedureDefinitionParser(node);
    final List<MagikDefinition> definitions = parser.parseDefinitions();
    final ProcedureDefinition procDef = (ProcedureDefinition) definitions.iterator().next();

    // Try to determine result if none was given via type-doc.
    final ProcedureDefinition finalProcDef =
        procDef.getReturnTypes().equals(ExpressionResultString.UNDEFINED)
            ? new ProcedureDefinition(
                procDef.getLocation(),
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

    // Store definition itself.
    this.state.setTypeStringDefinition(typeStr, finalProcDef);
  }
}
