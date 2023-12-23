package nl.ramsolutions.sw.magik.languageserver.completion;

import com.sonar.sslr.api.AstNode;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.scope.GlobalScope;
import nl.ramsolutions.sw.magik.analysis.scope.Scope;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasoner;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasonerState;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResult;
import nl.ramsolutions.sw.magik.analysis.typing.types.SelfType;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.types.UndefinedType;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.api.MagikKeyword;
import nl.ramsolutions.sw.magik.api.MagikOperator;
import nl.ramsolutions.sw.magik.api.MagikPunctuator;
import nl.ramsolutions.sw.magik.languageserver.Lsp4jConversion;
import nl.ramsolutions.sw.magik.parser.MagikCommentExtractor;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.CompletionOptions;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.ServerCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Completion provider.
 */
public class CompletionProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompletionProvider.class);
    private static final Set<Character> REMOVAL_STOP_CHARS = new HashSet<>();

    static {
        REMOVAL_STOP_CHARS.add(' ');
        REMOVAL_STOP_CHARS.add('\t');

        @SuppressWarnings("java:S1612")
        final Set<Character> punctuatorChars = Arrays.stream(MagikPunctuator.values())
            .map(MagikPunctuator::getValue)
            .flatMap(value -> value.chars().mapToObj(i -> (char) i))
            .collect(Collectors.toSet());
        REMOVAL_STOP_CHARS.addAll(punctuatorChars);

        @SuppressWarnings("java:S1612")
        final Set<Character> operatorChars = Arrays.stream(MagikOperator.values())
            .map(MagikOperator::getValue)
            .flatMap(value -> value.chars().mapToObj(i -> (char) i))
            .collect(Collectors.toSet());
        REMOVAL_STOP_CHARS.addAll(operatorChars);
    }

    /**
     * Set server capabilities.
     * @param capabilities Server capabilities.
     */
    public void setCapabilities(final ServerCapabilities capabilities) {
        final CompletionOptions completionOptions = new CompletionOptions();
        completionOptions.setTriggerCharacters(List.of("."));
        capabilities.setCompletionProvider(completionOptions);
    }

    /**
     * Get a list of completions.
     * @param magikFile Magik file.
     * @param position Position in file.
     * @return List of completions.
     */
    public List<CompletionItem> provideCompletions(final MagikTypedFile magikFile, final Position position) {
        // Do our best to get a token value, and clean up the source while we're at it.
        final Map.Entry<MagikTypedFile, String> usables = this.getUsableMagikFile(magikFile, position);
        final MagikTypedFile newMagikFile = usables.getKey();
        final String removedPart = usables.getValue();
        final Position newPosition = new Position(
            position.getLine(), position.getCharacter() - removedPart.length());
        final AstNode node = newMagikFile.getTopNode();
        final AstNode tokenNode = AstQuery.nodeAt(node, Lsp4jConversion.positionFromLsp4j(newPosition));
        LOGGER.trace("Current token: {}", removedPart);

        // Ensure not in comment.
        if (this.inComment(node, position)) {
            return Collections.emptyList();
        }

        // Keyword completion: '_'.
        if (removedPart.startsWith("_")) {
            LOGGER.debug("Providing keyword completions");
            return this.provideKeywordCompletions();
        }

        // Method completion: METHOD_INVOCATION or '.'.
        if (tokenNode != null) {
            final AstNode methodInvocationNode = AstQuery.getParentFromChain(
                tokenNode, MagikGrammar.IDENTIFIER, MagikGrammar.METHOD_INVOCATION);
            if (removedPart.startsWith(".")
                || methodInvocationNode != null) {
                return this.provideMethodInvocationCompletion(newMagikFile, tokenNode, removedPart);
            }
        }

        return this.provideGlobalCompletion(newMagikFile, position, tokenNode);
    }

    /**
     * Test if position is in comment.
     * @param node Top node.
     * @param position Position in file.
     * @return Returns
     */
    private boolean inComment(final AstNode node, final Position position) {
        final nl.ramsolutions.sw.magik.Position nativePosition = Lsp4jConversion.positionFromLsp4j(position);
        return MagikCommentExtractor.extractComments(node)
            .anyMatch(token -> nativePosition.getLine() == token.getLine()
                                && nativePosition.getColumn() >= token.getColumn());
    }

    /**
     * Provide global completion.
     * @param magikFile MagikFile.
     * @param position Position in source.
     * @param tokenNode Current node.
     * @return Completions items.
     */
    @SuppressWarnings("checkstyle:NestedIfDepth")
    private List<CompletionItem> provideGlobalCompletion(
            final MagikTypedFile magikFile, final Position position, final @Nullable AstNode tokenNode) {
        final List<CompletionItem> items = new ArrayList<>();

        // Keyword entries.
        Stream.of(MagikKeyword.values())
            .map(magikKeyword -> {
                final String name = magikKeyword.toString().toLowerCase();
                final CompletionItem item = new CompletionItem(name);
                item.setKind(CompletionItemKind.Keyword);
                item.setInsertText(magikKeyword.getValue());
                return item;
            })
            .forEach(items::add);

        // Scope entries.
        final AstNode topNode = magikFile.getTopNode();
        AstNode scopeNode = AstQuery.nodeSurrounding(topNode, Lsp4jConversion.positionFromLsp4j(position));
        if (scopeNode != null) {
            if (scopeNode.getFirstChild(MagikGrammar.BODY) != null) {
                scopeNode = scopeNode.getFirstChild(MagikGrammar.BODY);
            }
            final GlobalScope globalScope = magikFile.getGlobalScope();
            final Scope scopeForNode = globalScope.getScopeForNode(scopeNode);
            if (scopeForNode != null) {
                scopeForNode.getSelfAndAncestorScopes().stream()
                    .flatMap(scope -> scope.getScopeEntriesInScope().stream())
                    .filter(scopeEntry -> {
                        final AstNode definingNode = scopeEntry.getDefinitionNode();
                        final Range range = new Range(definingNode);
                        return Lsp4jConversion.positionFromLsp4j(position).isAfterRange(range);
                    })
                    .map(scopeEntry -> {
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
        final ITypeKeeper typeKeeper = magikFile.getTypeKeeper();
        if (scopeNode != null) {
            final AstNode methodDefinitionNode = scopeNode.getFirstAncestor(MagikGrammar.METHOD_DEFINITION);
            if (methodDefinitionNode != null) {
                final MethodDefinitionNodeHelper helper = new MethodDefinitionNodeHelper(methodDefinitionNode);
                final TypeString typeString = helper.getTypeString();
                final AbstractType type = typeKeeper.getType(typeString);
                type.getSlots().stream()
                    .map(slot -> {
                        final String slotName = slot.getName();
                        final String fullSlotName = type.getFullName() + "." + slot.getName();
                        final CompletionItem item = new CompletionItem(slotName);
                        item.setInsertText(slotName);
                        item.setDetail(fullSlotName);
                        item.setKind(CompletionItemKind.Property);
                        return item;
                    })
                    .forEach(items::add);
            }
        }

        // Global types.
        final String identifierPart = tokenNode != null
            ? tokenNode.getTokenValue()
            : "";
        typeKeeper.getTypes().stream()
            .filter(type -> type.getFullName().indexOf(identifierPart) != -1)
            .map(type -> {
                final CompletionItem item = new CompletionItem(type.getFullName());
                item.setInsertText(type.getFullName());
                item.setDetail(type.getFullName());
                item.setDocumentation(type.getDoc());
                item.setKind(CompletionItemKind.Class);
                return item;
            })
            .forEach(items::add);

        return items;
    }

    /**
     * Provide method invocation completions.
     * @param magikFile MagikFile.
     * @param tokenNode Token node.
     * @param tokenValue Token value.
     * @return List with {@link CompletionItem}s.
     */
    private List<CompletionItem> provideMethodInvocationCompletion(
            final MagikTypedFile magikFile,
            final AstNode tokenNode,
            final String tokenValue) {
        // Reason (on newly parsed source, thus not from magikFile) and get type.
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();

        // Token -->
        // - parent: any --> parent: ATOM
        // - parent: IDENTIFIER --> parent: METHOD_INVOCATION --> previous sibling: ATOM
        // - parent: IDENTIFIER --> parent: METHOD_INVOCATION --> previous sibling: METHOD_INVOCATION
        final AstNode node = tokenNode.getParent();
        final AstNode parentNode = node.getParent();
        final AstNode wantedNode;
        if (parentNode != null
            && parentNode.is(MagikGrammar.ATOM)) {
            // Asking the ATOM node.
            wantedNode = parentNode;
        } else if (parentNode != null
                   && (parentNode.is(MagikGrammar.METHOD_INVOCATION)
                       || parentNode.is(MagikGrammar.PROCEDURE_INVOCATION))) {
            // Asking the previous invocation.
            wantedNode = parentNode.getPreviousSibling();
        } else {
            return Collections.emptyList();
        }

        final LocalTypeReasonerState reasonerState = reasoner.getState();
        final ExpressionResult result = reasonerState.getNodeType(wantedNode);
        AbstractType type = result.get(0, UndefinedType.INSTANCE);
        if (type == SelfType.INSTANCE) {
            final AstNode methodDefNode = tokenNode.getFirstAncestor(MagikGrammar.METHOD_DEFINITION);
            final MethodDefinitionNodeHelper helper = new MethodDefinitionNodeHelper(methodDefNode);
            final TypeString typeString = helper.getTypeString();
            final ITypeKeeper typeKeeper = magikFile.getTypeKeeper();
            type = typeKeeper.getType(typeString);
        }

        // Convert all known methods to CompletionItems.
        LOGGER.debug("Providing method completions for type: {}", type.getFullName());
        final String methodNamePart = tokenValue.startsWith(".")
            ? tokenValue.substring(1)
            : tokenValue;
        return type.getMethods().stream()
            .filter(method -> method.getName().contains(methodNamePart))
            .map(method -> {
                final String methodName = method.getNameWithParameters();
                final CompletionItem item = new CompletionItem(methodName);
                item.setInsertText(methodName);
                item.setDetail(method.getOwner().getFullName());
                item.setDocumentation(method.getDoc());
                item.setKind(CompletionItemKind.Method);
                return item;
            })
            .collect(Collectors.toList());
    }

    /**
     * Strip the current token at position.
     *
     * @param text Text to strip from.
     * @param position Position to strip.
     * @return Cleared source, removed token.
     */
    private String[] cleanSource(final String source, final Position position) {
        final int lineNo = position.getLine();
        final String[] lines = source.split("\n");
        final String line = lines[lineNo];

        // Replace current token.
        // Scan left up to, including: whitespace, MagikOperator, MagikPunctuator
        // Scan right up to, excluding: whitespace, MagikOperator, MagikPunctuator
        final int characterNo = position.getCharacter() >= line.length()
            ? line.length() - 1
            : position.getCharacter();
        int beginIndex = characterNo;
        for (; beginIndex >= 0; --beginIndex) {
            final char chr = line.charAt(beginIndex);
            if (CompletionProvider.REMOVAL_STOP_CHARS.contains(chr)) {
                break;
            }
        }
        beginIndex = Math.max(beginIndex, 0);
        int endIndex = characterNo;
        for (; endIndex < line.length(); ++endIndex) {
            final char chr = line.charAt(endIndex);
            if (CompletionProvider.REMOVAL_STOP_CHARS.contains(chr)) {
                ++endIndex;
                break;
            }
        }

        // Clean up by replacing the scanned part with whitespace.
        final String stripped = line.substring(beginIndex, endIndex);
        lines[lineNo] = ""
            + line.substring(0, beginIndex)
            + " ".repeat(stripped.length())
            + line.substring(endIndex);
        return new String[] {
            Arrays.stream(lines).collect(Collectors.joining("\n")),
            stripped.trim()
        };
    }

    /**
     * Provide keyword {@link CompletionItem}s.
     *
     * @return {@link CompletionItem}s.
     */
    private List<CompletionItem> provideKeywordCompletions() {
        return Arrays.stream(MagikKeyword.values())
            .map(MagikKeyword::getValue)
            .map(value -> {
                final CompletionItem item = new CompletionItem(value);
                item.setKind(CompletionItemKind.Keyword);
                return item;
            })
            .collect(Collectors.toList());
    }

    /**
     * Get the current character at {@code position} in {@code text}.
     * @param text Text to use.
     * @param position Position to get character from.
     * @return Character at {@code position}.
     */
    @CheckForNull
    private Character getCurrentChar(final String text, final Position position) {
        final int line = position.getLine();
        int character = position.getCharacter();
        final Optional<String> optionalLineStr = text.lines().skip(line).findFirst();
        if (optionalLineStr.isEmpty()) {
            return null;
        }
        final String lineStr = optionalLineStr.get();
        if (character >= lineStr.length()) {
            character = lineStr.length() - 1;
        }
        return lineStr.charAt(character);
    }

    private Map.Entry<MagikTypedFile, String> getUsableMagikFile(
            final MagikTypedFile magikFile, final Position position) {
        MagikTypedFile newMagikFile = magikFile;

        final AstNode node = magikFile.getTopNode();
        final AstNode tokenNode = AstQuery.nodeAt(node, Lsp4jConversion.positionFromLsp4j(position));
        String cleanedToken = "";
        if (tokenNode != null
            && tokenNode.getParent() != null
            && tokenNode.getParent().is(MagikGrammar.SYNTAX_ERROR)) {
            // Clean it up a bit and try to re-parse.
            final String source = magikFile.getSource();
            final String[] items = this.cleanSource(source, position);
            final String cleanedSource = items[0];
            cleanedToken = items[1];
            final URI uri = magikFile.getUri();
            final IDefinitionKeeper definitionKeeper = magikFile.getDefinitionKeeper();
            newMagikFile = new MagikTypedFile(uri, cleanedSource, definitionKeeper);
        }

        return Map.entry(newMagikFile, cleanedToken);
    }

}
