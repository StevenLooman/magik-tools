package nl.ramsolutions.sw.magik.languageserver.signaturehelp;

import com.sonar.sslr.api.AstNode;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.LocalTypeReasoner;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResult;
import nl.ramsolutions.sw.magik.analysis.typing.types.SelfType;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.types.UndefinedType;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.languageserver.Lsp4jConversion;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.SignatureHelp;
import org.eclipse.lsp4j.SignatureHelpOptions;
import org.eclipse.lsp4j.SignatureInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Signature help provider.
 */
public class SignatureHelpProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(SignatureHelpProvider.class);

    /**
     * Set server capabilities.
     * @param capabilities Server capabilities.
     */
    public void setCapabilities(final ServerCapabilities capabilities) {
        final SignatureHelpOptions signatureHelpOptions = new SignatureHelpOptions();
        signatureHelpOptions.setTriggerCharacters(List.of("."));
        capabilities.setSignatureHelpProvider(signatureHelpOptions);
    }

    /**
     * Provide a {@link SignatureHelp} for {@code position} in {@code path}.
     * @param magikFile Magik file.
     * @param position Position in file.
     * @return {@link SignatureHelp}.
     */
    public SignatureHelp provideSignatureHelp(final MagikTypedFile magikFile, final Position position) {
        // Get intended method and called type.
        final AstNode node = magikFile.getTopNode();
        AstNode currentNode = AstQuery.nodeAt(node, Lsp4jConversion.positionFromLsp4j(position));
        if (currentNode != null
            && currentNode.isNot(MagikGrammar.METHOD_INVOCATION)) {
            currentNode = currentNode.getFirstAncestor(MagikGrammar.METHOD_INVOCATION);
        }
        if (currentNode == null) {
            return new SignatureHelp(Collections.emptyList(), null, null);
        }
        final MethodInvocationNodeHelper helper = new MethodInvocationNodeHelper(currentNode);
        final String methodName = helper.getMethodName();
        if (methodName == null) {
            return new SignatureHelp(Collections.emptyList(), null, null);
        }
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();
        final AstNode previousSiblingNode = currentNode.getPreviousSibling();
        final ExpressionResult result = reasoner.getNodeType(previousSiblingNode);
        final ITypeKeeper typeKeeper = magikFile.getTypeKeeper();
        final TypeString unsetTypeString = TypeString.ofIdentifier("unset", "sw");
        final AbstractType unsetType = typeKeeper.getType(unsetTypeString);
        AbstractType type = result.get(0, unsetType);

        LOGGER.debug("Provide signature for type: {}, method: {}", type.getFullName(), methodName);

        final List<SignatureInformation> sigInfos;
        if (type == UndefinedType.INSTANCE) {
            // Provide all methods with the name.
            sigInfos = typeKeeper.getTypes().stream()
                .flatMap(signatureType -> signatureType.getMethods().stream())
                .filter(method -> method.getName().startsWith(methodName))
                .map(method -> new SignatureInformation(method.getSignature(), method.getDoc(), null))
                .collect(Collectors.toList());
        } else {
            if (type == SelfType.INSTANCE) {
                final AstNode methodDefNode = currentNode.getFirstAncestor(MagikGrammar.METHOD_DEFINITION);
                final MethodDefinitionNodeHelper methodDefHelper = new MethodDefinitionNodeHelper(methodDefNode);
                final TypeString typeString = methodDefHelper.getTypeString();
                type = typeKeeper.getType(typeString);
            }
            // Provide methods for this type with the name.
            sigInfos = type.getMethods().stream()
                .filter(method -> method.getName().startsWith(methodName))
                .map(method -> new SignatureInformation(method.getSignature(), method.getDoc(), null))
                .collect(Collectors.toList());
        }

        return new SignatureHelp(sigInfos, null, null);
    }

}
