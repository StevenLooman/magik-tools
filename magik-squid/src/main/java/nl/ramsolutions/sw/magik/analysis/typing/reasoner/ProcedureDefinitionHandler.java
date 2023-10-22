package nl.ramsolutions.sw.magik.analysis.typing.reasoner;

import com.sonar.sslr.api.AstNode;
import java.net.URI;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import nl.ramsolutions.sw.definitions.SwModuleScanner;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.helpers.ParameterNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.ProcedureDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.analysis.typing.TypeReader;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResult;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.types.MagikType;
import nl.ramsolutions.sw.magik.analysis.typing.types.Method;
import nl.ramsolutions.sw.magik.analysis.typing.types.Parameter;
import nl.ramsolutions.sw.magik.analysis.typing.types.ProcedureInstance;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.types.UndefinedType;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/**
 * Procedure definition handler.
 */
class ProcedureDefinitionHandler extends LocalTypeReasonerHandler {

    private static final TypeString SW_PROCEDURE = TypeString.ofIdentifier("procedure", "sw");

    /**
     * Constructor.
     * @param magikFile MagikFile
     * @param nodeTypes Node types.
     * @param nodeIterTypes Node iter types.
     * @param currentScopeEntryNodes Current scope entry nodes.
     */
    ProcedureDefinitionHandler(
            final MagikTypedFile magikFile,
            final Map<AstNode, ExpressionResult> nodeTypes,
            final Map<AstNode, ExpressionResult> nodeIterTypes,
            final Map<ScopeEntry, AstNode> currentScopeEntryNodes) {
        super(magikFile, nodeTypes, nodeIterTypes, currentScopeEntryNodes);
    }

    /**
     * Handle procedure definition.
     * @param node PROCEDURE_DEFINITION node.
     */
    void handleProcedureDefinition(final AstNode node) {
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

            final AbstractType type = this.getNodeType(parameterNode).get(0, UndefinedType.INSTANCE);
            final TypeString typeString = type.getTypeString();
            final Parameter parameter = new Parameter(identifier, modifier, typeString);
            parameters.add(parameter);
        }

        // Result.
        final ExpressionResult procResult = this.getNodeType(node);
        final ExpressionResultString procResultStr = TypeReader.unparseExpressionResult(procResult);

        // Loopbody result.
        final ExpressionResult loopbodyResult = this.getNodeIterType(node);
        final ExpressionResultString loopbodyResultStr = TypeReader.unparseExpressionResult(loopbodyResult);

        // Create procedure instance.
        final EnumSet<Method.Modifier> modifiers = EnumSet.noneOf(Method.Modifier.class);
        final URI uri = node.getToken().getURI();
        final String moduleName = SwModuleScanner.getModuleName(uri);
        final Location location = new Location(uri, node);
        final MagikType procedureType = (MagikType) this.typeKeeper.getType(SW_PROCEDURE);
        final ProcedureInstance procType = new ProcedureInstance(
            procedureType,
            moduleName,
            location,
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
