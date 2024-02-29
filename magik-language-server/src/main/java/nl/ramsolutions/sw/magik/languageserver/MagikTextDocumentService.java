package nl.ramsolutions.sw.magik.languageserver;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.MagikAnalysisConfiguration;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.languageserver.codeactions.CodeActionProvider;
import nl.ramsolutions.sw.magik.languageserver.completion.CompletionProvider;
import nl.ramsolutions.sw.magik.languageserver.definitions.DefinitionsProvider;
import nl.ramsolutions.sw.magik.languageserver.diagnostics.MagikChecksDiagnosticsProvider;
import nl.ramsolutions.sw.magik.languageserver.diagnostics.MagikTypedChecksDiagnosticsProvider;
import nl.ramsolutions.sw.magik.languageserver.documentsymbols.DocumentSymbolProvider;
import nl.ramsolutions.sw.magik.languageserver.folding.FoldingRangeProvider;
import nl.ramsolutions.sw.magik.languageserver.formatting.FormattingProvider;
import nl.ramsolutions.sw.magik.languageserver.hover.HoverProvider;
import nl.ramsolutions.sw.magik.languageserver.implementation.ImplementationProvider;
import nl.ramsolutions.sw.magik.languageserver.inlayhint.InlayHintProvider;
import nl.ramsolutions.sw.magik.languageserver.references.ReferencesProvider;
import nl.ramsolutions.sw.magik.languageserver.rename.RenameProvider;
import nl.ramsolutions.sw.magik.languageserver.selectionrange.SelectionRangeProvider;
import nl.ramsolutions.sw.magik.languageserver.semantictokens.SemanticTokenProvider;
import nl.ramsolutions.sw.magik.languageserver.signaturehelp.SignatureHelpProvider;
import nl.ramsolutions.sw.magik.languageserver.typehierarchy.TypeHierarchyProvider;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionContext;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DefinitionParams;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.DocumentFormattingParams;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.DocumentSymbolParams;
import org.eclipse.lsp4j.FoldingRange;
import org.eclipse.lsp4j.FoldingRangeRequestParams;
import org.eclipse.lsp4j.FormattingOptions;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.ImplementationParams;
import org.eclipse.lsp4j.InlayHint;
import org.eclipse.lsp4j.InlayHintParams;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PrepareRenameDefaultBehavior;
import org.eclipse.lsp4j.PrepareRenameParams;
import org.eclipse.lsp4j.PrepareRenameResult;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.ReferenceParams;
import org.eclipse.lsp4j.RenameParams;
import org.eclipse.lsp4j.SelectionRange;
import org.eclipse.lsp4j.SelectionRangeParams;
import org.eclipse.lsp4j.SemanticTokens;
import org.eclipse.lsp4j.SemanticTokensParams;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.SignatureHelp;
import org.eclipse.lsp4j.SignatureHelpParams;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.TypeHierarchyItem;
import org.eclipse.lsp4j.TypeHierarchyPrepareParams;
import org.eclipse.lsp4j.TypeHierarchySubtypesParams;
import org.eclipse.lsp4j.TypeHierarchySupertypesParams;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.jsonrpc.messages.Either3;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Magik TextDocumentService. */
public class MagikTextDocumentService implements TextDocumentService {

  // TODO: Better separation of Lsp4J and magik-tools regarding Range/Position.

  private static final Logger LOGGER = LoggerFactory.getLogger(MagikTextDocumentService.class);

  private final MagikLanguageServer languageServer;
  private final MagikAnalysisConfiguration analysisConfiguration;
  private final IDefinitionKeeper definitionKeeper;

  private final Map<TextDocumentIdentifier, MagikTypedFile> openFiles = new HashMap<>();

  private final HoverProvider hoverProvider;
  private final ImplementationProvider implementationProvider;
  private final SignatureHelpProvider signatureHelpProvider;
  private final DefinitionsProvider definitionsProvider;
  private final ReferencesProvider referencesProvider;
  private final CompletionProvider completionProvider;
  private final FormattingProvider formattingProvider;
  private final FoldingRangeProvider foldingRangeProvider;
  private final SemanticTokenProvider semanticTokenProver;
  private final RenameProvider renameProvider;
  private final DocumentSymbolProvider documentSymbolProvider;
  private final TypeHierarchyProvider typeHierarchyProvider;
  private final InlayHintProvider inlayHintProvider;
  private final CodeActionProvider codeActionProvider;
  private final SelectionRangeProvider selectionRangeProvider;

  /**
   * Constructor.
   *
   * @param languageServer Owning language server.
   * @param definitionKeeper IDefinitionKeeper to use.
   */
  public MagikTextDocumentService(
      final MagikLanguageServer languageServer,
      final MagikAnalysisConfiguration analysisConfiguration,
      final IDefinitionKeeper definitionKeeper) {
    this.languageServer = languageServer;
    this.analysisConfiguration = analysisConfiguration;
    this.definitionKeeper = definitionKeeper;

    this.hoverProvider = new HoverProvider();
    this.implementationProvider = new ImplementationProvider();
    this.signatureHelpProvider = new SignatureHelpProvider();
    this.definitionsProvider = new DefinitionsProvider();
    this.referencesProvider = new ReferencesProvider();
    this.completionProvider = new CompletionProvider();
    this.formattingProvider = new FormattingProvider();
    this.foldingRangeProvider = new FoldingRangeProvider();
    this.semanticTokenProver = new SemanticTokenProvider();
    this.renameProvider = new RenameProvider();
    this.documentSymbolProvider = new DocumentSymbolProvider();
    this.typeHierarchyProvider = new TypeHierarchyProvider(this.definitionKeeper);
    this.inlayHintProvider = new InlayHintProvider();
    this.codeActionProvider = new CodeActionProvider();
    this.selectionRangeProvider = new SelectionRangeProvider();
  }

  /**
   * Set capabilities.
   *
   * @param capabilities Server capabilities to set.
   */
  public void setCapabilities(final ServerCapabilities capabilities) {
    capabilities.setTextDocumentSync(TextDocumentSyncKind.Full);

    this.hoverProvider.setCapabilities(capabilities);
    this.implementationProvider.setCapabilities(capabilities);
    this.signatureHelpProvider.setCapabilities(capabilities);
    this.definitionsProvider.setCapabilities(capabilities);
    this.referencesProvider.setCapabilities(capabilities);
    this.completionProvider.setCapabilities(capabilities);
    this.formattingProvider.setCapabilities(capabilities);
    this.foldingRangeProvider.setCapabilities(capabilities);
    this.semanticTokenProver.setCapabilities(capabilities);
    this.renameProvider.setCapabilities(capabilities);
    this.documentSymbolProvider.setCapabilities(capabilities);
    this.typeHierarchyProvider.setCapabilities(capabilities);
    this.inlayHintProvider.setCapabilities(capabilities);
    this.codeActionProvider.setCapabilities(capabilities);
    this.selectionRangeProvider.setCapabilities(capabilities);
  }

  @Override
  public void didOpen(final DidOpenTextDocumentParams params) {
    final TextDocumentItem textDocument = params.getTextDocument();
    LOGGER.debug("didOpen, uri: {}", textDocument.getUri());

    // Store file contents.
    final String uriStr = textDocument.getUri();
    final URI uri = URI.create(uriStr);
    final TextDocumentIdentifier textDocumentIdentifier = new TextDocumentIdentifier(uriStr);
    final String text = textDocument.getText();
    final MagikTypedFile openFile =
        new MagikTypedFile(this.analysisConfiguration, uri, text, this.definitionKeeper);
    this.openFiles.put(textDocumentIdentifier, openFile);

    // Publish diagnostics to client.
    this.publishDiagnostics(openFile);
  }

  @Override
  public void didChange(final DidChangeTextDocumentParams params) {
    final VersionedTextDocumentIdentifier versionedTextDocumentIdentifier =
        params.getTextDocument();
    LOGGER.debug("didChange, uri: {}}", versionedTextDocumentIdentifier.getUri());

    // Store file contents.
    final List<TextDocumentContentChangeEvent> contentChangeEvents = params.getContentChanges();
    final TextDocumentContentChangeEvent contentChangeEvent = contentChangeEvents.get(0);
    final String text = contentChangeEvent.getText();
    final String uriStr = versionedTextDocumentIdentifier.getUri();
    final URI uri = URI.create(uriStr);
    final TextDocumentIdentifier textDocumentIdentifier = new TextDocumentIdentifier(uriStr);
    final MagikTypedFile openFile =
        new MagikTypedFile(this.analysisConfiguration, uri, text, this.definitionKeeper);
    this.openFiles.put(textDocumentIdentifier, openFile);

    // Publish diagnostics to client.
    this.publishDiagnostics(openFile);
  }

  @Override
  public void didClose(final DidCloseTextDocumentParams params) {
    final TextDocumentIdentifier textDocumentIdentifier = params.getTextDocument();
    LOGGER.debug("didClose, uri: {}", textDocumentIdentifier.getUri());

    // Clear stored document.
    this.openFiles.remove(textDocumentIdentifier);

    // Clear published diagnostics.
    final String uri = textDocumentIdentifier.getUri();
    final List<Diagnostic> diagnostics = Collections.emptyList();
    final PublishDiagnosticsParams publishParams = new PublishDiagnosticsParams(uri, diagnostics);
    final LanguageClient languageClient = this.languageServer.getLanguageClient();
    languageClient.publishDiagnostics(publishParams);
  }

  @Override
  public void didSave(final DidSaveTextDocumentParams params) {
    final TextDocumentIdentifier textDocumentIdentifier = params.getTextDocument();
    LOGGER.debug("didSave, uri: {}", textDocumentIdentifier.getUri());
  }

  private void publishDiagnostics(final MagikTypedFile magikFile) {
    final List<Diagnostic> diagnostics = new ArrayList<>();

    // Linter diagnostics.
    final List<Diagnostic> diagnosticsLinter = this.getDiagnosticsLinter(magikFile);
    diagnostics.addAll(diagnosticsLinter);

    // Typing diagnostics.
    final Boolean typingEnableChecks = MagikSettings.INSTANCE.getTypingEnableChecks();
    if (Boolean.TRUE.equals(typingEnableChecks)) {
      final List<Diagnostic> diagnosticsTyping = this.getDiagnosticsTyping(magikFile);
      diagnostics.addAll(diagnosticsTyping);
    }

    // Publish to client.
    final String uri = magikFile.getUri().toString();
    final PublishDiagnosticsParams publishParams = new PublishDiagnosticsParams(uri, diagnostics);
    final LanguageClient languageClient = this.languageServer.getLanguageClient();
    languageClient.publishDiagnostics(publishParams);
    LOGGER.debug("Published diagnostics: {}", diagnostics.size());
  }

  private List<Diagnostic> getDiagnosticsLinter(final MagikTypedFile magikFile) {
    final Path overrideSettingsPath = MagikSettings.INSTANCE.getChecksOverrideSettingsPath();

    final MagikChecksDiagnosticsProvider lintProvider =
        new MagikChecksDiagnosticsProvider(overrideSettingsPath);
    try {
      return lintProvider.getDiagnostics(magikFile);
    } catch (final IOException exception) {
      LOGGER.error(exception.getMessage(), exception);
    }

    return Collections.emptyList();
  }

  private List<Diagnostic> getDiagnosticsTyping(final MagikTypedFile magikFile) {
    final Path overrideSettingsPath = MagikSettings.INSTANCE.getChecksOverrideSettingsPath();

    final MagikTypedChecksDiagnosticsProvider typedDiagnosticsProvider =
        new MagikTypedChecksDiagnosticsProvider(overrideSettingsPath);
    try {
      return typedDiagnosticsProvider.getDiagnostics(magikFile);
    } catch (final IOException exception) {
      LOGGER.error(exception.getMessage(), exception);
    }

    return Collections.emptyList();
  }

  @Override
  public CompletableFuture<Hover> hover(final HoverParams params) {
    final TextDocumentIdentifier textDocument = params.getTextDocument();
    LOGGER.trace(
        "hover: uri: {}, position: {},{}",
        textDocument.getUri(),
        params.getPosition().getLine(),
        params.getPosition().getCharacter());

    final MagikTypedFile magikFile = this.openFiles.get(textDocument);
    final Position position = params.getPosition();
    return CompletableFuture.supplyAsync(
        () -> this.hoverProvider.provideHover(magikFile, position));
  }

  @Override
  public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>>
      implementation(final ImplementationParams params) {
    final TextDocumentIdentifier textDocument = params.getTextDocument();
    LOGGER.trace(
        "implementation, uri: {}, position: {},{}",
        textDocument.getUri(),
        params.getPosition().getLine(),
        params.getPosition().getCharacter());

    final MagikTypedFile magikFile = this.openFiles.get(textDocument);
    final Position lsp4jPosition = params.getPosition();
    final nl.ramsolutions.sw.magik.Position position =
        Lsp4jConversion.positionFromLsp4j(lsp4jPosition);
    return CompletableFuture.supplyAsync(
        () -> {
          final List<nl.ramsolutions.sw.magik.Location> locations =
              this.implementationProvider.provideImplementations(magikFile, position);
          LOGGER.debug("Implementations found: {}", locations.size());

          final List<Location> lsp4jLocations =
              locations.stream().map(Lsp4jConversion::locationToLsp4j).toList();
          return Either.forLeft(lsp4jLocations);
        });
  }

  @Override
  public CompletableFuture<SignatureHelp> signatureHelp(final SignatureHelpParams params) {
    final TextDocumentIdentifier textDocument = params.getTextDocument();
    LOGGER.trace(
        "signatureHelp, uri: {}, position: {},{}",
        textDocument.getUri(),
        params.getPosition().getLine(),
        params.getPosition().getCharacter());

    final MagikTypedFile magikFile = this.openFiles.get(textDocument);
    final Position position = params.getPosition();
    return CompletableFuture.supplyAsync(
        () -> {
          final SignatureHelp signatureHelp =
              this.signatureHelpProvider.provideSignatureHelp(magikFile, position);
          LOGGER.debug("Created signatures: {}", signatureHelp.getSignatures().size());
          return signatureHelp;
        });
  }

  @Override
  public CompletableFuture<List<FoldingRange>> foldingRange(
      final FoldingRangeRequestParams params) {
    final TextDocumentIdentifier textDocument = params.getTextDocument();
    LOGGER.trace("foldingRange, uri: {}", textDocument.getUri());

    final MagikTypedFile magikFile = this.openFiles.get(textDocument);
    return CompletableFuture.supplyAsync(
        () -> {
          final List<FoldingRange> ranges =
              this.foldingRangeProvider.provideFoldingRanges(magikFile);
          LOGGER.debug("Folds found: {}", ranges.size());
          return ranges;
        });
  }

  @Override
  public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>>
      definition(final DefinitionParams params) {
    final TextDocumentIdentifier textDocument = params.getTextDocument();
    LOGGER.trace("definitions, uri: {}", textDocument.getUri());

    final MagikTypedFile magikFile = this.openFiles.get(textDocument);
    final Position lsp4jPosition = params.getPosition();
    final nl.ramsolutions.sw.magik.Position position =
        Lsp4jConversion.positionFromLsp4j(lsp4jPosition);
    return CompletableFuture.supplyAsync(
        () -> {
          final List<nl.ramsolutions.sw.magik.Location> locations =
              this.definitionsProvider.provideDefinitions(magikFile, position);
          LOGGER.debug("Definitions found: {}", locations.size());

          final List<Location> lsp4jLocations =
              locations.stream().map(Lsp4jConversion::locationToLsp4j).toList();
          return Either.forLeft(lsp4jLocations);
        });
  }

  @Override
  public CompletableFuture<List<? extends Location>> references(final ReferenceParams params) {
    final TextDocumentIdentifier textDocument = params.getTextDocument();
    LOGGER.trace("references, uri: {}", textDocument.getUri());

    final MagikTypedFile magikFile = this.openFiles.get(textDocument);
    final Position lsp4jPosition = params.getPosition();
    final nl.ramsolutions.sw.magik.Position position =
        Lsp4jConversion.positionFromLsp4j(lsp4jPosition);
    return CompletableFuture.supplyAsync(
        () -> {
          final List<nl.ramsolutions.sw.magik.Location> locations =
              this.referencesProvider.provideReferences(magikFile, position);
          LOGGER.debug("References found: {}", locations.size());
          return locations.stream().map(Lsp4jConversion::locationToLsp4j).toList();
        });
  }

  @Override
  public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(
      final CompletionParams params) {
    final TextDocumentIdentifier textDocument = params.getTextDocument();
    LOGGER.trace(
        "completion, uri: {}, position: {},{}",
        textDocument.getUri(),
        params.getPosition().getLine(),
        params.getPosition().getCharacter());

    final MagikTypedFile magikFile = this.openFiles.get(textDocument);
    final Position position = params.getPosition();
    return CompletableFuture.supplyAsync(
        () -> {
          final List<CompletionItem> completions =
              this.completionProvider.provideCompletions(magikFile, position);
          LOGGER.debug("Completions found: {}", completions.size());
          return Either.forLeft(completions);
        });
  }

  @Override
  public CompletableFuture<List<? extends TextEdit>> formatting(
      final DocumentFormattingParams params) {
    final TextDocumentIdentifier textDocument = params.getTextDocument();
    LOGGER.trace("formatting, uri: {}", textDocument.getUri());

    final MagikTypedFile magikFile = this.openFiles.get(textDocument);
    final FormattingOptions options = params.getOptions();
    return CompletableFuture.supplyAsync(
        () -> {
          if (!this.formattingProvider.canFormat(magikFile)) {
            LOGGER.warn("Cannot format due to syntax error");
            return Collections.emptyList();
          }

          final List<TextEdit> textEdits =
              this.formattingProvider.provideFormatting(magikFile, options);
          LOGGER.debug("Text edits created: {}", textEdits.size());
          return textEdits;
        });
  }

  @Override
  public CompletableFuture<SemanticTokens> semanticTokensFull(final SemanticTokensParams params) {
    final TextDocumentIdentifier textDocument = params.getTextDocument();
    LOGGER.trace("semanticTokensFull, uri: {}", textDocument.getUri());

    final MagikTypedFile magikFile = this.openFiles.get(textDocument);
    return CompletableFuture.supplyAsync(
        () -> this.semanticTokenProver.provideSemanticTokensFull(magikFile));
  }

  @Override
  public CompletableFuture<Either3<Range, PrepareRenameResult, PrepareRenameDefaultBehavior>>
      prepareRename(PrepareRenameParams params) {
    final TextDocumentIdentifier textDocument = params.getTextDocument();
    LOGGER.trace(
        "prepareRename, uri: {}, position: {},{}",
        textDocument.getUri(),
        params.getPosition().getLine(),
        params.getPosition().getCharacter());

    final MagikTypedFile magikFile = this.openFiles.get(textDocument);
    final Position position = params.getPosition();
    return CompletableFuture.supplyAsync(
        () -> this.renameProvider.providePrepareRename(magikFile, position));
  }

  @Override
  public CompletableFuture<WorkspaceEdit> rename(final RenameParams params) {
    final TextDocumentIdentifier textDocument = params.getTextDocument();
    LOGGER.trace(
        "rename, uri: {}, position: {},{}",
        textDocument.getUri(),
        params.getPosition().getLine(),
        params.getPosition().getCharacter());

    final MagikTypedFile magikFile = this.openFiles.get(textDocument);
    final Position position = params.getPosition();
    final String newName = params.getNewName();
    return CompletableFuture.supplyAsync(
        () -> this.renameProvider.provideRename(magikFile, position, newName));
  }

  @Override
  public CompletableFuture<List<Either<org.eclipse.lsp4j.SymbolInformation, DocumentSymbol>>>
      documentSymbol(final DocumentSymbolParams params) {
    final TextDocumentIdentifier textDocument = params.getTextDocument();
    LOGGER.trace("documentSymbol, uri: {}", textDocument.getUri());

    final MagikTypedFile magikFile = this.openFiles.get(textDocument);
    return CompletableFuture.supplyAsync(
        () -> this.documentSymbolProvider.provideDocumentSymbol(magikFile));
  }

  @Override
  public CompletableFuture<List<SelectionRange>> selectionRange(final SelectionRangeParams params) {
    final TextDocumentIdentifier textDocument = params.getTextDocument();
    LOGGER.trace("selectionRange, uri: {}", textDocument.getUri());

    final MagikTypedFile magikFile = this.openFiles.get(textDocument);
    final List<nl.ramsolutions.sw.magik.Position> positions =
        params.getPositions().stream().map(Lsp4jConversion::positionFromLsp4j).toList();
    return CompletableFuture.supplyAsync(
        () -> this.selectionRangeProvider.provideSelectionRanges(magikFile, positions));
  }

  @Override
  public CompletableFuture<List<TypeHierarchyItem>> prepareTypeHierarchy(
      final TypeHierarchyPrepareParams params) {
    final TextDocumentIdentifier textDocument = params.getTextDocument();
    LOGGER.trace(
        "prepareTypeHierarchy, uri: {}, position: {},{}",
        textDocument.getUri(),
        params.getPosition().getLine(),
        params.getPosition().getCharacter());

    final MagikTypedFile magikFile = this.openFiles.get(textDocument);
    final Position position = params.getPosition();

    return CompletableFuture.supplyAsync(
        () -> this.typeHierarchyProvider.prepareTypeHierarchy(magikFile, position));
  }

  @Override
  public CompletableFuture<List<TypeHierarchyItem>> typeHierarchySubtypes(
      final TypeHierarchySubtypesParams params) {
    final TypeHierarchyItem item = params.getItem();
    LOGGER.trace("typeHierarchySubtypes, item: {}", item.getName());

    return CompletableFuture.supplyAsync(
        () -> this.typeHierarchyProvider.typeHierarchySubtypes(item));
  }

  @Override
  public CompletableFuture<List<TypeHierarchyItem>> typeHierarchySupertypes(
      final TypeHierarchySupertypesParams params) {
    final TypeHierarchyItem item = params.getItem();
    LOGGER.trace("typeHierarchySupertypes, item: {}", item.getName());

    return CompletableFuture.supplyAsync(
        () -> this.typeHierarchyProvider.typeHierarchySupertypes(item));
  }

  @Override
  public CompletableFuture<List<InlayHint>> inlayHint(final InlayHintParams params) {
    final TextDocumentIdentifier textDocument = params.getTextDocument();
    final Range range = params.getRange();
    LOGGER.trace(
        "inlayHint, uri: {}, range: {},{}-{},{}",
        textDocument.getUri(),
        range.getStart().getLine(),
        range.getStart().getCharacter(),
        range.getEnd().getLine(),
        range.getEnd().getCharacter());

    final MagikTypedFile magikFile = this.openFiles.get(textDocument);
    return CompletableFuture.supplyAsync(
        () -> this.inlayHintProvider.provideInlayHints(magikFile, range));
  }

  @Override
  public CompletableFuture<List<Either<Command, CodeAction>>> codeAction(
      final CodeActionParams params) {
    final TextDocumentIdentifier textDocument = params.getTextDocument();
    final Range range = params.getRange();
    LOGGER.trace(
        "codeAction, uri: {}, range: {},{}-{},{}",
        textDocument.getUri(),
        range.getStart().getLine(),
        range.getStart().getCharacter(),
        range.getEnd().getLine(),
        range.getEnd().getCharacter());

    final MagikTypedFile magikFile = this.openFiles.get(textDocument);
    final nl.ramsolutions.sw.magik.Range magikRange = Lsp4jConversion.rangeFromLsp4j(range);
    final CodeActionContext context = params.getContext();
    return CompletableFuture.supplyAsync(
        () -> {
          final List<nl.ramsolutions.sw.magik.CodeAction> codeActions =
              this.codeActionProvider.provideCodeActions(magikFile, magikRange, context);
          return codeActions.stream()
              .map(
                  codeAction ->
                      Lsp4jUtils.createCodeAction(
                          magikFile, codeAction.getTitle(), codeAction.getEdits()))
              .map(Either::<Command, CodeAction>forRight)
              .toList();
        });
  }
}
