package nl.ramsolutions.sw.magik.languageserver.signaturehelp;

import com.sonar.sslr.api.AstNode;
import java.util.Collections;
import java.util.List;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeStringResolver;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasonerState;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.languageserver.Lsp4jConversion;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.SignatureHelp;
import org.eclipse.lsp4j.SignatureHelpOptions;
import org.eclipse.lsp4j.SignatureInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Signature help provider. */
public class SignatureHelpProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(SignatureHelpProvider.class);

  /**
   * Set server capabilities.
   *
   * @param capabilities Server capabilities.
   */
  public void setCapabilities(final ServerCapabilities capabilities) {
    final SignatureHelpOptions signatureHelpOptions = new SignatureHelpOptions();
    signatureHelpOptions.setTriggerCharacters(List.of("."));
    capabilities.setSignatureHelpProvider(signatureHelpOptions);
  }

  /**
   * Provide a {@link SignatureHelp} for {@code position} in {@code path}.
   *
   * @param magikFile Magik file.
   * @param position Position in file.
   * @return {@link SignatureHelp}.
   */
  public SignatureHelp provideSignatureHelp(
      final MagikTypedFile magikFile, final Position position) {
    // Get intended method and called type.
    final AstNode node = magikFile.getTopNode();
    AstNode currentNode = AstQuery.nodeAt(node, Lsp4jConversion.positionFromLsp4j(position));
    if (currentNode != null && currentNode.isNot(MagikGrammar.METHOD_INVOCATION)) {
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
    final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();
    final AstNode previousSiblingNode = currentNode.getPreviousSibling();
    final ExpressionResultString result = reasonerState.getNodeType(previousSiblingNode);
    final TypeString typeStr = result.get(0, TypeString.SW_UNSET);

    LOGGER.debug("Provide signature for type: {}, method: {}", typeStr.getFullString(), methodName);

    final List<SignatureInformation> sigInfos;
    if (typeStr.isUndefined()) {
      final IDefinitionKeeper definitionKeeper = magikFile.getDefinitionKeeper();
      // Provide all methods with the name.
      sigInfos =
          definitionKeeper.getMethodDefinitions().stream()
              .filter(methodDef -> methodDef.getMethodName().startsWith(methodName))
              .map(
                  methodDef ->
                      new SignatureInformation(
                          methodDef.getNameWithParameters(), methodDef.getDoc(), null))
              .toList();
    } else {
      // Provide methods for this type with the name.
      final TypeStringResolver resolver = magikFile.getTypeStringResolver();
      sigInfos =
          resolver.getMethodDefinitions(typeStr).stream()
              .filter(methodDef -> methodDef.getMethodName().startsWith(methodName))
              .map(
                  methodDef ->
                      new SignatureInformation(
                          methodDef.getNameWithParameters(), methodDef.getDoc(), null))
              .toList();
    }

    return new SignatureHelp(sigInfos, null, null);
  }
}
