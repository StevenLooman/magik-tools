package nl.ramsolutions.sw.magik.languageserver.completion;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.scope.GlobalScope;
import nl.ramsolutions.sw.magik.analysis.scope.Scope;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.api.MagikKeyword;
import nl.ramsolutions.sw.magik.languageserver.Lsp4jConversion;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.Position;

/**
 * Default (fallback) completion: keywords, in-scope variables, slots and exemplar type names.
 *
 * <p>Stays silent inside a comment, preserving the "nothing to complete when in comment" behaviour.
 */
public class GlobalCompletionModule implements CompletionModule {

  @Override
  public Optional<List<CompletionItem>> tryComplete(final CompletionContext context) {
    if (CompletionUtils.inComment(context.getTopNode(), context.position())) {
      return Optional.of(List.of());
    }
    return Optional.of(
        this.globalItems(
            context.magikFile(), context.position(), context.tokenNode(), context.replaceRange()));
  }

  @SuppressWarnings("checkstyle:NestedIfDepth")
  private List<CompletionItem> globalItems(
      final MagikTypedFile magikFile,
      final Position position,
      final @Nullable AstNode tokenNode,
      final @Nullable Range replaceRange) {
    final List<CompletionItem> items = new ArrayList<>();

    // Keyword entries.
    Stream.of(MagikKeyword.values())
        .map(
            magikKeyword -> {
              final String name = magikKeyword.toString().toLowerCase();
              final CompletionItem item = new CompletionItem(name);
              item.setKind(CompletionItemKind.Keyword);
              item.setInsertText(magikKeyword.getValue());
              return item;
            })
        .forEach(items::add);

    // Scope entries.
    final AstNode topNode = magikFile.getTopNode();
    AstNode scopeNode =
        AstQuery.nodeSurrounding(topNode, Lsp4jConversion.positionFromLsp4j(position));
    if (scopeNode != null) {
      if (scopeNode.getFirstChild(MagikGrammar.BODY) != null) {
        scopeNode = scopeNode.getFirstChild(MagikGrammar.BODY);
      }
      final GlobalScope globalScope = magikFile.getGlobalScope();
      final Scope scopeForNode = globalScope.getScopeForNode(scopeNode);
      if (scopeForNode != null) {
        scopeForNode.getSelfAndAncestorScopes().stream()
            .flatMap(scope -> scope.getScopeEntriesInScope().stream())
            .filter(
                scopeEntry -> {
                  final AstNode definingNode = scopeEntry.getDefinitionNode();
                  final Range range = new Range(definingNode);
                  final nl.ramsolutions.sw.magik.Position magikPosition =
                      Lsp4jConversion.positionFromLsp4j(position);
                  return range.positionIsAfterSelf(magikPosition);
                })
            .map(
                scopeEntry -> {
                  final CompletionItem item = new CompletionItem(scopeEntry.getIdentifier());
                  item.setInsertText(scopeEntry.getIdentifier());
                  item.setDetail(scopeEntry.getIdentifier());
                  item.setKind(CompletionItemKind.Variable);
                  return item;
                })
            .forEach(items::add);
      }
    }

    // Slots.
    final IDefinitionKeeper definitionKeeper = magikFile.getDefinitionKeeper();
    if (scopeNode != null) {
      final AstNode methodDefinitionNode =
          scopeNode.getFirstAncestor(MagikGrammar.METHOD_DEFINITION);
      if (methodDefinitionNode != null) {
        final MethodDefinitionNodeHelper helper =
            new MethodDefinitionNodeHelper(methodDefinitionNode);
        final TypeString typeString = helper.getExemplarTypeString();
        magikFile.getTypeStringResolver().getSlotDefinitions(typeString).stream()
            .map(
                slot -> {
                  final String slotName = slot.getName();
                  final String fullSlotName = typeString.getFullString() + "." + slot.getName();
                  final CompletionItem item = new CompletionItem(slotName);
                  item.setInsertText(slotName);
                  item.setDetail(fullSlotName);
                  item.setKind(CompletionItemKind.Property);
                  return item;
                })
            .forEach(items::add);
      }
    }

    // Global types. The replace range (from the original source) covers any package prefix such as
    // 'sw:', so completing 'sw:rop' yields 'sw:rope' instead of 'sw:sw:rope'.
    final String identifierPart = tokenNode != null ? tokenNode.getTokenValue() : "";
    items.addAll(ExemplarCompletionItems.build(definitionKeeper, identifierPart, replaceRange));

    return items;
  }
}
