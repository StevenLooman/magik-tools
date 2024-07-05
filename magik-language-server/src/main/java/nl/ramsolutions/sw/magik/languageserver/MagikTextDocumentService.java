package nl.ramsolutions.sw.magik.languageserver;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import nl.ramsolutions.sw.ConfigurationReader;
import nl.ramsolutions.sw.MagikToolsProperties;
import nl.ramsolutions.sw.OpenedFile;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.ModuleDefFile;
import nl.ramsolutions.sw.magik.ProductDefFile;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.languageserver.codeactions.CodeActionProvider;
import nl.ramsolutions.sw.magik.languageserver.completion.CompletionProvider;
import nl.ramsolutions.sw.magik.languageserver.definitions.DefinitionsProvider;
import nl.ramsolutions.sw.magik.languageserver.diagnostics.DiagnosticsProvider;
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
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.TypeHierarchyItem;
import org.eclipse.lsp4j.TypeHierarchyPrepareParams;
import org.eclipse.lsp4j.TypeHierarchySubtypesParams;
import org.eclipse.lsp4j.TypeHierarchySupertypesParams;
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
  private static final Logger LOGGER_DURATION =
      LoggerFactory.getLogger(MagikTextDocumentService.class.getName() + "Duration");

  private final MagikLanguageServer languageServer;
  private final MagikToolsProperties properties;
  private final IDefinitionKeeper definitionKeeper;
  private final DiagnosticsProvider diagnosticsProvider;
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
  private final Map<TextDocumentIdentifier, OpenedFile> openedFiles = new HashMap<>();

  /**
   * Constructor.
   *
   * @param languageServer Owning language server.
   * @param definitionKeeper IDefinitionKeeper to use.
   */
  public MagikTextDocumentService(
      final MagikLanguageServer languageServer,
      final MagikToolsProperties properties,
      final IDefinitionKeeper definitionKeeper) {
    this.languageServer = languageServer;
    this.properties = properties;
    this.definitionKeeper = definitionKeeper;

    this.diagnosticsProvider = new DiagnosticsProvider(this.properties);
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
    this.inlayHintProvider = new InlayHintProvider(this.properties);
    this.codeActionProvider = new CodeActionProvider(this.properties);
    this.selectionRangeProvider = new SelectionRangeProvider();
  }

  /**
   * Set capabilities.
   *
   * @param capabilities Server capabilities to set.
   */
  public void setCapabilities(final ServerCapabilities capabilities) {
    capabilities.setTextDocumentSync(TextDocumentSyncKind.Full);

    this.diagnosticsProvider.setCapabilities(capabilities);
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
    final long start = System.nanoTime();

    final TextDocumentItem textDocument = params.getTextDocument();
    LOGGER.debug("didOpen, uri: {}", textDocument.getUri());

    // Read relevant properties.
    final String uriStr = textDocument.getUri();
    final URI uri = URI.create(uriStr);
    final MagikToolsProperties fileProperties;
    try {
      fileProperties = ConfigurationReader.readProperties(uri, properties);
    } catch (final IOException exception) {
      throw new IllegalStateException(exception);
    }

    // Store file contents.
    final TextDocumentIdentifier textDocumentIdentifier = new TextDocumentIdentifier(uriStr);
    final String text = textDocument.getText();
    final OpenedFile openedFile;
    switch (textDocument.getLanguageId()) {
      case "product.def":
        {
          openedFile = new ProductDefFile(uri, text, this.definitionKeeper, null);
          break;
        }

      case "module.def":
        {
          openedFile = new ModuleDefFile(uri, text, this.definitionKeeper, null);
          break;
        }

      case "magik":
        {
          final MagikTypedFile magikFile =
              new MagikTypedFile(fileProperties, uri, text, this.definitionKeeper);
          openedFile = magikFile;

          // Publish diagnostics to client.
          this.publishDiagnostics(magikFile);
          break;
        }

      default:
        throw new UnsupportedOperationException();
    }

    this.openedFiles.put(textDocumentIdentifier, openedFile);
    if (LOGGER_DURATION.isTraceEnabled()) {
      LOGGER_DURATION.trace(
          "Duration: {} didOpen, uri: {}",
          String.format("%.2f", (System.nanoTime() - start) / 1000000000.0),
          textDocument.getUri());
    }
  }

  @Override
  public void didChange(final DidChangeTextDocumentParams params) {
    final long start = System.nanoTime();

    final TextDocumentIdentifier textDocumentIdentifier = params.getTextDocument();
    LOGGER.debug("didChange, uri: {}}", textDocumentIdentifier.getUri());

    // Read relevant properties.
    final String uriStr = textDocumentIdentifier.getUri();
    final URI uri = URI.create(uriStr);
    final MagikToolsProperties fileProperties;
    try {
      fileProperties = ConfigurationReader.readProperties(uri, properties);
    } catch (final IOException exception) {
      throw new IllegalStateException(exception);
    }

    // Update file contents.
    final List<TextDocumentContentChangeEvent> contentChangeEvents = params.getContentChanges();
    final TextDocumentContentChangeEvent contentChangeEvent = contentChangeEvents.get(0);
    final String text = contentChangeEvent.getText();

    // Find original TextDocumentIdentifier.
    final TextDocumentIdentifier realTextDocumentIdentifier = new TextDocumentIdentifier(uriStr);
    final OpenedFile existingOpenedFile = this.openedFiles.get(realTextDocumentIdentifier);
    if (existingOpenedFile == null) {
      // Race condition?
      return;
    }

    final String languageId = existingOpenedFile.getLanguageId();
    final OpenedFile openedFile;
    switch (languageId) {
      case "product.def":
        {
          openedFile = new ProductDefFile(uri, text, this.definitionKeeper, null);
          break;
        }

      case "module.def":
        {
          openedFile = new ModuleDefFile(uri, text, this.definitionKeeper, null);
          break;
        }

      case "magik":
        {
          final MagikTypedFile magikFile =
              new MagikTypedFile(fileProperties, uri, text, this.definitionKeeper);
          openedFile = magikFile;

          // Publish diagnostics to client.
          this.publishDiagnostics(magikFile);
          break;
        }

      default:
        throw new UnsupportedOperationException();
    }

    this.openedFiles.put(realTextDocumentIdentifier, openedFile);
    if (LOGGER_DURATION.isTraceEnabled()) {
      LOGGER_DURATION.trace(
          "Duration: {} didChange, uri: {}",
          String.format("%.2f", (System.nanoTime() - start) / 1000000000.0),
          textDocumentIdentifier.getUri());
    }
  }

  @Override
  public void didClose(final DidCloseTextDocumentParams params) {
    final long start = System.nanoTime();

    final TextDocumentIdentifier textDocumentIdentifier = params.getTextDocument();
    LOGGER.debug("didClose, uri: {}", textDocumentIdentifier.getUri());

    this.openedFiles.remove(textDocumentIdentifier);

    // Clear published diagnostics.
    final List<Diagnostic> diagnostics = Collections.emptyList();
    final String uriStr = textDocumentIdentifier.getUri();
    final PublishDiagnosticsParams publishParams =
        new PublishDiagnosticsParams(uriStr, diagnostics);
    final LanguageClient languageClient = this.languageServer.getLanguageClient();
    languageClient.publishDiagnostics(publishParams);

    if (LOGGER_DURATION.isTraceEnabled()) {
      LOGGER_DURATION.trace(
          "Duration: {} didClose, uri: {}",
          String.format("%.2f", (System.nanoTime() - start) / 1000000000.0),
          textDocumentIdentifier.getUri());
    }
  }

  @Override
  public void didSave(final DidSaveTextDocumentParams params) {
    final long start = System.nanoTime();

    final TextDocumentIdentifier textDocumentIdentifier = params.getTextDocument();
    LOGGER.debug("didSave, uri: {}", textDocumentIdentifier.getUri());
    if (LOGGER_DURATION.isTraceEnabled()) {
      LOGGER_DURATION.trace(
          "Duration: {} didSave, uri: {}",
          String.format("%.2f", (System.nanoTime() - start) / 1000000000.0),
          textDocumentIdentifier.getUri());
    }
  }

  private void publishDiagnostics(final MagikTypedFile magikFile) {
    final List<Diagnostic> diagnostics = this.diagnosticsProvider.provideDiagnostics(magikFile);

    // Publish to client.
    final String uri = magikFile.getUri().toString();
    final PublishDiagnosticsParams publishParams = new PublishDiagnosticsParams(uri, diagnostics);
    final LanguageClient languageClient = this.languageServer.getLanguageClient();
    languageClient.publishDiagnostics(publishParams);
  }

  @Override
  public CompletableFuture<Hover> hover(final HoverParams params) {
    final long start = System.nanoTime();

    final TextDocumentIdentifier textDocument = params.getTextDocument();
    LOGGER.debug(
        "hover: uri: {}, position: {},{}",
        textDocument.getUri(),
        params.getPosition().getLine(),
        params.getPosition().getCharacter());

    final Position position = params.getPosition();
    final OpenedFile openedFile = this.openedFiles.get(textDocument);

    return CompletableFuture.supplyAsync(
        () -> {
          final Hover hover;
          if (openedFile == null) {
            hover = null;
          } else if (openedFile instanceof ProductDefFile productDefFile) {
            hover = this.hoverProvider.provideHover(productDefFile, position);
          } else if (openedFile instanceof ModuleDefFile moduleDefFile) {
            hover = this.hoverProvider.provideHover(moduleDefFile, position);
          } else if (openedFile instanceof MagikTypedFile magikFile) {
            hover = this.hoverProvider.provideHover(magikFile, position);
          } else {
            throw new UnsupportedOperationException();
          }

          if (LOGGER_DURATION.isTraceEnabled()) {
            LOGGER_DURATION.trace(
                "Duration: {} hover: uri: {}, position: {},{}",
                String.format("%.2f", (System.nanoTime() - start) / 1000000000.0),
                textDocument.getUri(),
                params.getPosition().getLine(),
                params.getPosition().getCharacter());
          }
          return hover;
        });
  }

  @Override
  public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>>
      implementation(final ImplementationParams params) {
    final long start = System.nanoTime();

    final TextDocumentIdentifier textDocument = params.getTextDocument();
    LOGGER.debug(
        "implementation, uri: {}, position: {},{}",
        textDocument.getUri(),
        params.getPosition().getLine(),
        params.getPosition().getCharacter());

    final OpenedFile openedFile = this.openedFiles.get(textDocument);
    if (!(openedFile instanceof MagikTypedFile)) {
      return CompletableFuture.supplyAsync(() -> Either.forLeft(Collections.emptyList()));
    }

    final MagikTypedFile magikFile = (MagikTypedFile) openedFile;
    final Position lsp4jPosition = params.getPosition();
    final nl.ramsolutions.sw.magik.Position position =
        Lsp4jConversion.positionFromLsp4j(lsp4jPosition);
    return CompletableFuture.supplyAsync(
        () -> {
          final List<nl.ramsolutions.sw.magik.Location> locations =
              this.implementationProvider.provideImplementations(magikFile, position);
          final List<Location> lsp4jLocations =
              locations.stream().map(Lsp4jConversion::locationToLsp4j).toList();
          if (LOGGER_DURATION.isTraceEnabled()) {
            LOGGER_DURATION.trace(
                "Duration: {} implementation, uri: {}, position: {},{}",
                String.format("%.2f", (System.nanoTime() - start) / 1000000000.0),
                textDocument.getUri(),
                params.getPosition().getLine(),
                params.getPosition().getCharacter());
          }
          return Either.forLeft(lsp4jLocations);
        });
  }

  @Override
  public CompletableFuture<SignatureHelp> signatureHelp(final SignatureHelpParams params) {
    final long start = System.nanoTime();

    final TextDocumentIdentifier textDocument = params.getTextDocument();
    LOGGER.debug(
        "signatureHelp, uri: {}, position: {},{}",
        textDocument.getUri(),
        params.getPosition().getLine(),
        params.getPosition().getCharacter());

    final OpenedFile openedFile = this.openedFiles.get(textDocument);
    if (!(openedFile instanceof MagikTypedFile)) {
      return CompletableFuture.supplyAsync(SignatureHelp::new);
    }

    final MagikTypedFile magikFile = (MagikTypedFile) openedFile;
    final Position position = params.getPosition();
    return CompletableFuture.supplyAsync(
        () -> {
          final SignatureHelp signatureHelp =
              this.signatureHelpProvider.provideSignatureHelp(magikFile, position);
          if (LOGGER_DURATION.isTraceEnabled()) {
            LOGGER_DURATION.trace(
                "Duration: {} signatureHelp, uri: {}, position: {},{}",
                String.format("%.2f", (System.nanoTime() - start) / 1000000000.0),
                textDocument.getUri(),
                params.getPosition().getLine(),
                params.getPosition().getCharacter());
          }
          return signatureHelp;
        });
  }

  @Override
  public CompletableFuture<List<FoldingRange>> foldingRange(
      final FoldingRangeRequestParams params) {
    final long start = System.nanoTime();

    final TextDocumentIdentifier textDocument = params.getTextDocument();
    LOGGER.debug("foldingRange, uri: {}", textDocument.getUri());

    final OpenedFile openedFile = this.openedFiles.get(textDocument);
    return CompletableFuture.supplyAsync(
        () -> {
          final List<FoldingRange> foldingRanges;
          if (openedFile == null) {
            foldingRanges = Collections.emptyList();
          } else if (openedFile instanceof ProductDefFile productDefFile) {
            foldingRanges = this.foldingRangeProvider.provideFoldingRanges(productDefFile);
          } else if (openedFile instanceof ModuleDefFile moduleDefFile) {
            foldingRanges = this.foldingRangeProvider.provideFoldingRanges(moduleDefFile);
          } else if (openedFile instanceof MagikTypedFile magikFile) {
            foldingRanges = this.foldingRangeProvider.provideFoldingRanges(magikFile);
          } else {
            throw new UnsupportedOperationException();
          }

          if (LOGGER_DURATION.isTraceEnabled()) {
            LOGGER_DURATION.trace(
                "Duration: {} foldingRange, uri: {}",
                String.format("%.2f", (System.nanoTime() - start) / 1000000000.0),
                textDocument.getUri());
          }
          return foldingRanges;
        });
  }

  @Override
  public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>>
      definition(final DefinitionParams params) {
    final long start = System.nanoTime();

    final TextDocumentIdentifier textDocument = params.getTextDocument();
    LOGGER.debug("definitions, uri: {}", textDocument.getUri());

    final Position lsp4jPosition = params.getPosition();
    final nl.ramsolutions.sw.magik.Position position =
        Lsp4jConversion.positionFromLsp4j(lsp4jPosition);
    final OpenedFile openedFile = this.openedFiles.get(textDocument);
    return CompletableFuture.supplyAsync(
        () -> {
          final List<nl.ramsolutions.sw.magik.Location> locations;
          if (openedFile == null) {
            locations = Collections.emptyList();
          } else if (openedFile instanceof ProductDefFile productDefFile) {
            locations = this.definitionsProvider.provideDefinitions(productDefFile, position);
          } else if (openedFile instanceof ModuleDefFile moduleDefFile) {
            locations = this.definitionsProvider.provideDefinitions(moduleDefFile, position);
          } else if (openedFile instanceof MagikTypedFile magikFile) {
            locations = this.definitionsProvider.provideDefinitions(magikFile, position);
          } else {
            throw new UnsupportedOperationException();
          }

          final Either<List<? extends Location>, List<? extends LocationLink>> forLeft =
              Either.forLeft(locations.stream().map(Lsp4jConversion::locationToLsp4j).toList());
          if (LOGGER_DURATION.isTraceEnabled()) {
            LOGGER_DURATION.trace(
                "Duration: {} definitions, uri: {}",
                String.format("%.2f", (System.nanoTime() - start) / 1000000000.0),
                textDocument.getUri());
          }
          return forLeft;
        });
  }

  @Override
  public CompletableFuture<List<? extends Location>> references(final ReferenceParams params) {
    final long start = System.nanoTime();

    final TextDocumentIdentifier textDocument = params.getTextDocument();
    LOGGER.debug("references, uri: {}", textDocument.getUri());

    final Position lsp4jPosition = params.getPosition();
    final nl.ramsolutions.sw.magik.Position position =
        Lsp4jConversion.positionFromLsp4j(lsp4jPosition);
    final OpenedFile openedFile = this.openedFiles.get(textDocument);
    return CompletableFuture.supplyAsync(
        () -> {
          final List<Location> references;
          if (openedFile == null) {
            references = Collections.emptyList();
          } else if (openedFile instanceof ProductDefFile productDefFile) {
            references =
                this.referencesProvider.provideReferences(productDefFile, position).stream()
                    .map(Lsp4jConversion::locationToLsp4j)
                    .toList();
          } else if (openedFile instanceof ModuleDefFile moduleDefFile) {
            references =
                this.referencesProvider.provideReferences(moduleDefFile, position).stream()
                    .map(Lsp4jConversion::locationToLsp4j)
                    .toList();
          } else if (openedFile instanceof MagikTypedFile magikFile) {
            references =
                this.referencesProvider.provideReferences(magikFile, position).stream()
                    .map(Lsp4jConversion::locationToLsp4j)
                    .toList();
          } else {
            throw new UnsupportedOperationException();
          }

          if (LOGGER_DURATION.isTraceEnabled()) {
            LOGGER_DURATION.trace(
                "Duration: {} references, uri: {}",
                String.format("%.2f", (System.nanoTime() - start) / 1000000000.0),
                textDocument.getUri());
          }
          return references;
        });
  }

  @Override
  public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(
      final CompletionParams params) {
    final long start = System.nanoTime();

    final TextDocumentIdentifier textDocument = params.getTextDocument();
    LOGGER.debug(
        "completion, uri: {}, position: {},{}",
        textDocument.getUri(),
        params.getPosition().getLine(),
        params.getPosition().getCharacter());

    final OpenedFile openedFile = this.openedFiles.get(textDocument);
    if (!(openedFile instanceof MagikTypedFile)) {
      return CompletableFuture.supplyAsync(() -> Either.forLeft(Collections.emptyList()));
    }

    final MagikTypedFile magikFile = (MagikTypedFile) openedFile;
    final Position position = params.getPosition();
    return CompletableFuture.supplyAsync(
        () -> {
          final List<CompletionItem> completions =
              this.completionProvider.provideCompletions(magikFile, position);
          if (LOGGER_DURATION.isTraceEnabled()) {
            LOGGER_DURATION.trace(
                "Duration: {} completion, uri: {}, position: {},{}",
                String.format("%.2f", (System.nanoTime() - start) / 1000000000.0),
                textDocument.getUri(),
                params.getPosition().getLine(),
                params.getPosition().getCharacter());
          }
          return Either.forLeft(completions);
        });
  }

  @Override
  public CompletableFuture<List<? extends TextEdit>> formatting(
      final DocumentFormattingParams params) {
    final long start = System.nanoTime();

    final TextDocumentIdentifier textDocument = params.getTextDocument();
    LOGGER.debug("formatting, uri: {}", textDocument.getUri());

    final OpenedFile openedFile = this.openedFiles.get(textDocument);
    if (!(openedFile instanceof MagikTypedFile)) {
      return CompletableFuture.supplyAsync(Collections::emptyList);
    }

    final MagikTypedFile magikFile = (MagikTypedFile) openedFile;
    final FormattingOptions options = params.getOptions();
    return CompletableFuture.supplyAsync(
        () -> {
          if (!this.formattingProvider.canFormat(magikFile)) {
            LOGGER.warn("Cannot format due to syntax error");
            return Collections.emptyList();
          }

          final List<TextEdit> textEdits =
              this.formattingProvider.provideFormatting(magikFile, options);
          if (LOGGER_DURATION.isTraceEnabled()) {
            LOGGER_DURATION.trace(
                "Duration: {} formatting, uri: {}",
                String.format("%.2f", (System.nanoTime() - start) / 1000000000.0),
                textDocument.getUri());
          }
          return textEdits;
        });
  }

  @Override
  public CompletableFuture<SemanticTokens> semanticTokensFull(final SemanticTokensParams params) {
    final long start = System.nanoTime();

    final TextDocumentIdentifier textDocument = params.getTextDocument();
    LOGGER.debug("semanticTokensFull, uri: {}", textDocument.getUri());

    final OpenedFile openedFile = this.openedFiles.get(textDocument);
    return CompletableFuture.supplyAsync(
        () -> {
          final SemanticTokens semanticTokens;
          if (openedFile == null) {
            semanticTokens = null;
          } else if (openedFile instanceof ProductDefFile productDefFile) {
            semanticTokens = this.semanticTokenProver.provideSemanticTokensFull(productDefFile);
          } else if (openedFile instanceof ModuleDefFile moduleDefFile) {
            semanticTokens = this.semanticTokenProver.provideSemanticTokensFull(moduleDefFile);
          } else if (openedFile instanceof MagikTypedFile magikFile) {
            semanticTokens = this.semanticTokenProver.provideSemanticTokensFull(magikFile);
          } else {
            throw new UnsupportedOperationException();
          }

          if (LOGGER_DURATION.isTraceEnabled()) {
            LOGGER_DURATION.trace(
                "Duration: {} semanticTokensFull, uri: {}",
                String.format("%.2f", (System.nanoTime() - start) / 1000000000.0),
                textDocument.getUri());
          }
          return semanticTokens;
        });
  }

  @Override
  public CompletableFuture<Either3<Range, PrepareRenameResult, PrepareRenameDefaultBehavior>>
      prepareRename(final PrepareRenameParams params) {
    final long start = System.nanoTime();

    final TextDocumentIdentifier textDocument = params.getTextDocument();
    LOGGER.debug(
        "prepareRename, uri: {}, position: {},{}",
        textDocument.getUri(),
        params.getPosition().getLine(),
        params.getPosition().getCharacter());

    final OpenedFile openedFile = this.openedFiles.get(textDocument);
    if (!(openedFile instanceof MagikTypedFile)) {
      return CompletableFuture.supplyAsync(() -> null);
    }

    final MagikTypedFile magikFile = (MagikTypedFile) openedFile;
    final Position position = params.getPosition();
    return CompletableFuture.supplyAsync(
        () -> {
          final Either3<Range, PrepareRenameResult, PrepareRenameDefaultBehavior> prepareRename =
              this.renameProvider.providePrepareRename(magikFile, position);
          if (LOGGER_DURATION.isTraceEnabled()) {
            LOGGER_DURATION.trace(
                "Duration: {} prepareRename, uri: {}, position: {},{}",
                String.format("%.2f", (System.nanoTime() - start) / 1000000000.0),
                textDocument.getUri(),
                params.getPosition().getLine(),
                params.getPosition().getCharacter());
          }
          return prepareRename;
        });
  }

  @Override
  public CompletableFuture<WorkspaceEdit> rename(final RenameParams params) {
    final long start = System.nanoTime();

    final TextDocumentIdentifier textDocument = params.getTextDocument();
    LOGGER.debug(
        "rename, uri: {}, position: {},{}",
        textDocument.getUri(),
        params.getPosition().getLine(),
        params.getPosition().getCharacter());

    final OpenedFile openedFile = this.openedFiles.get(textDocument);
    if (!(openedFile instanceof MagikTypedFile)) {
      return CompletableFuture.supplyAsync(() -> null);
    }

    final MagikTypedFile magikFile = (MagikTypedFile) openedFile;
    final Position position = params.getPosition();
    final String newName = params.getNewName();
    return CompletableFuture.supplyAsync(
        () -> {
          final WorkspaceEdit rename =
              this.renameProvider.provideRename(magikFile, position, newName);
          if (LOGGER_DURATION.isTraceEnabled()) {
            LOGGER_DURATION.trace(
                "Duration: {} rename, uri: {}, position: {},{}",
                String.format("%.2f", (System.nanoTime() - start) / 1000000000.0),
                textDocument.getUri(),
                params.getPosition().getLine(),
                params.getPosition().getCharacter());
          }
          return rename;
        });
  }

  @Override
  public CompletableFuture<List<Either<org.eclipse.lsp4j.SymbolInformation, DocumentSymbol>>>
      documentSymbol(final DocumentSymbolParams params) {
    final long start = System.nanoTime();

    final TextDocumentIdentifier textDocument = params.getTextDocument();
    LOGGER.debug("documentSymbol, uri: {}", textDocument.getUri());

    final OpenedFile openedFile = this.openedFiles.get(textDocument);
    if (!(openedFile instanceof MagikTypedFile)) {
      return CompletableFuture.supplyAsync(Collections::emptyList);
    }

    final MagikTypedFile magikFile = (MagikTypedFile) openedFile;
    return CompletableFuture.supplyAsync(
        () -> {
          final List<Either<SymbolInformation, DocumentSymbol>> documentSymbols =
              this.documentSymbolProvider.provideDocumentSymbols(magikFile);
          if (LOGGER_DURATION.isTraceEnabled()) {
            LOGGER_DURATION.trace(
                "Duration: {} documentSymbol, uri: {}",
                String.format("%.2f", (System.nanoTime() - start) / 1000000000.0),
                textDocument.getUri());
          }
          return documentSymbols;
        });
  }

  @Override
  public CompletableFuture<List<SelectionRange>> selectionRange(final SelectionRangeParams params) {
    final long start = System.nanoTime();

    final TextDocumentIdentifier textDocument = params.getTextDocument();
    LOGGER.debug("selectionRange, uri: {}", textDocument.getUri());

    final OpenedFile openedFile = this.openedFiles.get(textDocument);
    if (!(openedFile instanceof MagikTypedFile)) {
      return CompletableFuture.supplyAsync(Collections::emptyList);
    }

    final MagikTypedFile magikFile = (MagikTypedFile) openedFile;
    final List<nl.ramsolutions.sw.magik.Position> positions =
        params.getPositions().stream().map(Lsp4jConversion::positionFromLsp4j).toList();
    return CompletableFuture.supplyAsync(
        () -> {
          final List<SelectionRange> selectionRanges =
              this.selectionRangeProvider.provideSelectionRanges(magikFile, positions);
          if (LOGGER_DURATION.isTraceEnabled()) {
            LOGGER_DURATION.trace(
                "Duration: {} selectionRange, uri: {}",
                String.format("%.2f", (System.nanoTime() - start) / 1000000000.0),
                textDocument.getUri());
          }
          return selectionRanges;
        });
  }

  @Override
  public CompletableFuture<List<TypeHierarchyItem>> prepareTypeHierarchy(
      final TypeHierarchyPrepareParams params) {
    final long start = System.nanoTime();

    final TextDocumentIdentifier textDocument = params.getTextDocument();
    LOGGER.debug(
        "prepareTypeHierarchy, uri: {}, position: {},{}",
        textDocument.getUri(),
        params.getPosition().getLine(),
        params.getPosition().getCharacter());

    final OpenedFile openedFile = this.openedFiles.get(textDocument);
    if (!(openedFile instanceof MagikTypedFile)) {
      return CompletableFuture.supplyAsync(() -> null);
    }

    final MagikTypedFile magikFile = (MagikTypedFile) openedFile;
    final Position position = params.getPosition();

    return CompletableFuture.supplyAsync(
        () -> {
          final List<TypeHierarchyItem> typeHierarchy =
              this.typeHierarchyProvider.prepareTypeHierarchy(magikFile, position);
          if (LOGGER_DURATION.isTraceEnabled()) {
            LOGGER_DURATION.trace(
                "Duration: {} prepareTypeHierarchy, uri: {}, position: {},{}",
                String.format("%.2f", (System.nanoTime() - start) / 1000000000.0),
                textDocument.getUri(),
                params.getPosition().getLine(),
                params.getPosition().getCharacter());
          }
          return typeHierarchy;
        });
  }

  @Override
  public CompletableFuture<List<TypeHierarchyItem>> typeHierarchySubtypes(
      final TypeHierarchySubtypesParams params) {
    final long start = System.nanoTime();

    final TypeHierarchyItem item = params.getItem();
    LOGGER.debug("typeHierarchySubtypes, item: {}", item.getName());

    return CompletableFuture.supplyAsync(
        () -> {
          final List<TypeHierarchyItem> subtypes =
              this.typeHierarchyProvider.typeHierarchySubtypes(item);
          if (LOGGER_DURATION.isTraceEnabled()) {
            LOGGER_DURATION.trace(
                "Duration: {} didOpen, typeHierarchySubtypes, item: {}",
                String.format("%.2f", (System.nanoTime() - start) / 1000000000.0),
                item.getName());
          }
          return subtypes;
        });
  }

  @Override
  public CompletableFuture<List<TypeHierarchyItem>> typeHierarchySupertypes(
      final TypeHierarchySupertypesParams params) {
    final long start = System.nanoTime();

    final TypeHierarchyItem item = params.getItem();
    LOGGER.debug("typeHierarchySupertypes, item: {}", item.getName());

    return CompletableFuture.supplyAsync(
        () -> {
          final List<TypeHierarchyItem> supertypes =
              this.typeHierarchyProvider.typeHierarchySupertypes(item);
          if (LOGGER_DURATION.isTraceEnabled()) {
            LOGGER_DURATION.trace(
                "Duration: {} didOpen, typeHierarchySupertypes, item: {}",
                String.format("%.2f", (System.nanoTime() - start) / 1000000000.0),
                item.getName());
          }
          return supertypes;
        });
  }

  @Override
  public CompletableFuture<List<InlayHint>> inlayHint(final InlayHintParams params) {
    final long start = System.nanoTime();

    final TextDocumentIdentifier textDocument = params.getTextDocument();
    final Range range = params.getRange();
    LOGGER.debug(
        "inlayHint, uri: {}, range: {},{}-{},{}",
        textDocument.getUri(),
        range.getStart().getLine(),
        range.getStart().getCharacter(),
        range.getEnd().getLine(),
        range.getEnd().getCharacter());

    final OpenedFile openedFile = this.openedFiles.get(textDocument);
    if (!(openedFile instanceof MagikTypedFile)) {
      return CompletableFuture.supplyAsync(Collections::emptyList);
    }

    final MagikTypedFile magikFile = (MagikTypedFile) openedFile;
    return CompletableFuture.supplyAsync(
        () -> {
          List<InlayHint> inlayHints = this.inlayHintProvider.provideInlayHints(magikFile, range);
          if (LOGGER_DURATION.isTraceEnabled()) {
            LOGGER_DURATION.trace(
                "Duration: {} inlayHint, uri: {}, range: {},{}-{},{}",
                String.format("%.2f", (System.nanoTime() - start) / 1000000000.0),
                textDocument.getUri(),
                range.getStart().getLine(),
                range.getStart().getCharacter(),
                range.getEnd().getLine(),
                range.getEnd().getCharacter());
          }
          return inlayHints;
        });
  }

  @Override
  public CompletableFuture<List<Either<Command, CodeAction>>> codeAction(
      final CodeActionParams params) {
    final long start = System.nanoTime();

    final TextDocumentIdentifier textDocument = params.getTextDocument();
    final Range range = params.getRange();
    LOGGER.debug(
        "codeAction, uri: {}, range: {},{}-{},{}",
        textDocument.getUri(),
        range.getStart().getLine(),
        range.getStart().getCharacter(),
        range.getEnd().getLine(),
        range.getEnd().getCharacter());

    final OpenedFile openedFile = this.openedFiles.get(textDocument);
    if (!(openedFile instanceof MagikTypedFile)) {
      return CompletableFuture.supplyAsync(Collections::emptyList);
    }

    final MagikTypedFile magikFile = (MagikTypedFile) openedFile;
    final nl.ramsolutions.sw.magik.Range magikRange = Lsp4jConversion.rangeFromLsp4j(range);
    final CodeActionContext context = params.getContext();
    return CompletableFuture.supplyAsync(
        () -> {
          final List<nl.ramsolutions.sw.magik.CodeAction> codeActions =
              this.codeActionProvider.provideCodeActions(magikFile, magikRange, context);
          final List<Either<Command, CodeAction>> codeActionsLsp4j =
              codeActions.stream()
                  .map(
                      codeAction ->
                          Lsp4jUtils.createCodeAction(
                              magikFile, codeAction.getTitle(), codeAction.getEdits()))
                  .map(Either::<Command, CodeAction>forRight)
                  .toList();
          if (LOGGER_DURATION.isTraceEnabled()) {
            LOGGER_DURATION.trace(
                "Duration: {} codeAction, uri: {}, range: {},{}-{},{}",
                String.format("%.2f", (System.nanoTime() - start) / 1000000000.0),
                textDocument.getUri(),
                range.getStart().getLine(),
                range.getStart().getCharacter(),
                range.getEnd().getLine(),
                range.getEnd().getCharacter());
          }
          return codeActionsLsp4j;
        });
  }
}
