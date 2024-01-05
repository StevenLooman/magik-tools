package nl.ramsolutions.sw.magik.analysis.typing.reasoner;

import com.sonar.sslr.api.AstNode;
import java.net.URI;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import nl.ramsolutions.sw.definitions.ModuleDefinitionScanner;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.analysis.helpers.ParameterNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.ProcedureDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeReader;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResult;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.types.MagikType;
import nl.ramsolutions.sw.magik.analysis.typing.types.Parameter;
import nl.ramsolutions.sw.magik.analysis.typing.types.ProcedureInstance;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.types.UndefinedType;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/**
 * Procedure definition handler.
 */
class ProcedureDefinitionHandler extends LocalTypeReasonerHandler {

    private static final TypeString SW_PROCEDURE_REF = TypeString.ofIdentifier("procedure", "sw");

    /**
     * Constructor.
     * @param state Reasoner state.
     */
    ProcedureDefinitionHandler(final LocalTypeReasonerState state) {
        super(state);
    }

    /**
     * Handle procedure definition.
     * @param node PROCEDURE_DEFINITION node.
     */
    void handleProcedureDefinition(final AstNode node) {
        final AbstractType abstractProcedureType = this.typeKeeper.getType(SW_PROCEDURE_REF);
        if (abstractProcedureType == UndefinedType.INSTANCE) {
            // Must be missing definition for sw:procedure, don't try anything.
            return;
        }

        // Location.
        final URI uri = node.getToken().getURI();
        final Location location = new Location(uri, node);

        // Get name of procedure.
        final ProcedureDefinitionNodeHelper helper = new ProcedureDefinitionNodeHelper(node);
        final String procedureName = helper.getProcedureName();

        // Parameters.
        final AstNode parametersNode = node.getFirstChild(MagikGrammar.PARAMETERS);
        if (parametersNode == null) {
            // Robustness, in case of a syntax error in the procedure definition.
            return;
        }

        // TODO: Can we move this somewhere else?
        final List<Parameter> parameters = new ArrayList<>();
        final List<AstNode> parameterNodes = parametersNode.getChildren(MagikGrammar.PARAMETER);
        for (final AstNode parameterNode : parameterNodes) {
            final Location parameterLocation = new Location(uri, parameterNode);
            final AstNode identifierNode = parameterNode.getFirstChild(MagikGrammar.IDENTIFIER);
            final String identifier = identifierNode.getTokenValue();

            final ParameterNodeHelper parameterHelper = new ParameterNodeHelper(parameterNode);
            final Parameter.Modifier modifier;
            if (parameterHelper.isOptionalParameter()) {
                modifier = Parameter.Modifier.OPTIONAL;
            } else if (parameterHelper.isGatherParameter()) {
                modifier = Parameter.Modifier.GATHER;
            } else {
                modifier = Parameter.Modifier.NONE;
            }

            final AbstractType type = this.state.getNodeType(parameterNode).get(0, UndefinedType.INSTANCE);
            final TypeString typeString = type.getTypeString();
            final Parameter parameter = new Parameter(parameterLocation, identifier, modifier, typeString);
            parameters.add(parameter);
        }

        // Result.
        final ExpressionResult procResult = this.state.getNodeType(node);
        final ExpressionResultString procResultStr = TypeReader.unparseExpressionResult(procResult);

        // Loopbody result.
        final ExpressionResult loopbodyResult = this.state.getNodeIterType(node);
        final ExpressionResultString loopbodyResultStr = TypeReader.unparseExpressionResult(loopbodyResult);

        // Create procedure instance.
        final EnumSet<ProcedureInstance.Modifier> modifiers = EnumSet.noneOf(ProcedureInstance.Modifier.class);
        final String moduleName = ModuleDefinitionScanner.getModuleName(uri);
        final MagikType procedureType = (MagikType) abstractProcedureType;
        final ProcedureInstance procType = new ProcedureInstance(
            location,
            moduleName,
            procedureType,
            procedureName,
            modifiers,
            parameters,
            null,
            procResultStr,
            loopbodyResultStr);

        // Store result.
        final ExpressionResult result = new ExpressionResult(procType);
        this.assignAtom(node, result);
    }

}
