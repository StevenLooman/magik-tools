package nl.ramsolutions.sw.magik.languageserver.completion;

import com.sonar.sslr.api.AstNode;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.SelfHelper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeStringResolver;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasonerState;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.CompletionItemTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Completes method invocations after {@code '.'} or within a method-invocation chain. */
public class MethodInvocationCompletionModule implements CompletionModule {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(MethodInvocationCompletionModule.class);
  private static final String TOPIC_DEPRECATED = "deprecated";

  @Override
  public Optional<List<CompletionItem>> tryComplete(final CompletionContext context) {
    final AstNode tokenNode = context.tokenNode();
    if (tokenNode == null) {
      return Optional.empty();
    }
    final String removedPart = context.removedPart();
    final AstNode methodInvocationNode =
        AstQuery.getParentFromChain(
            tokenNode,
            MagikGrammar.IDENTIFIER,
            MagikGrammar.METHOD_NAME,
            MagikGrammar.METHOD_INVOCATION);
    if (!removedPart.startsWith(".") && methodInvocationNode == null) {
      return Optional.empty();
    }
    return Optional.of(this.methodItems(context.magikFile(), tokenNode, removedPart));
  }

  @Override
  public List<String> getTriggerCharacters() {
    return List.of(".");
  }

  private List<CompletionItem> methodItems(
      final MagikTypedFile magikFile, final AstNode tokenNode, final String tokenValue) {
    // Token -->
    // - parent: any --> parent: ATOM
    // - parent: IDENTIFIER --> parent: METHOD_INVOCATION --> previous sibling: ATOM
    // - parent: IDENTIFIER --> parent: METHOD_INVOCATION --> previous sibling: METHOD_INVOCATION
    final AstNode node = tokenNode.getParent();
    final AstNode parentNode = node.getParent();
    final AstNode parentParentNode = parentNode.getParent();
    final AstNode wantedNode;
    if (parentNode != null && parentNode.is(MagikGrammar.ATOM)) {
      // Asking the ATOM node.
      wantedNode = parentNode;
    } else if (parentParentNode != null
        && (parentParentNode.is(MagikGrammar.METHOD_INVOCATION)
            || parentParentNode.is(MagikGrammar.PROCEDURE_INVOCATION))) {
      // Asking the previous invocation.
      wantedNode = parentParentNode.getPreviousSibling();
    } else {
      return Collections.emptyList();
    }

    final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();
    final ExpressionResultString result = reasonerState.getNodeType(wantedNode);
    final TypeString typeStrSelf = result.get(0, TypeString.UNDEFINED);
    final TypeString typeStr = SelfHelper.substituteSelf(typeStrSelf, wantedNode);

    // Convert all known methods to CompletionItems.
    LOGGER.debug("Providing method completions for type: {}", typeStr.getFullString());
    final String methodNamePart = tokenValue.startsWith(".") ? tokenValue.substring(1) : tokenValue;
    final TypeStringResolver resolver = magikFile.getTypeStringResolver();
    return resolver.getRespondingMethodDefinitions(typeStr).stream()
        .filter(methodDef -> methodDef.getMethodName().contains(methodNamePart))
        .map(
            methodDef -> {
              final String label = methodDef.getMethodNameWithParameters();
              final String insertText =
                  methodDef
                      .getMethodNameWithParameters()
                      .replaceAll("\\b_optional \\b", "")
                      .replaceAll("\\b_gather \\b", "");
              final CompletionItem item = new CompletionItem(label);
              item.setInsertText(insertText);
              item.setDetail(methodDef.getTypeName().getFullString());
              item.setDocumentation(methodDef.getDoc());
              item.setKind(CompletionItemKind.Method);
              if (methodDef.getPragma() != null
                  && methodDef.getPragma().getTopics().contains(TOPIC_DEPRECATED)) {
                item.setTags(List.of(CompletionItemTag.Deprecated));
              }
              return item;
            })
        .toList();
  }
}
