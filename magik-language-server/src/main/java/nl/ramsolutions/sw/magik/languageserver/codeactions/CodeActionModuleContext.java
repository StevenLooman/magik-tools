package nl.ramsolutions.sw.magik.languageserver.codeactions;

import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Range;

/**
 * Shared, pre-computed context passed to each {@link CodeActionModule}.
 *
 * <p>Named {@code CodeActionModuleContext}, not {@code CodeActionContext}, to avoid colliding with
 * {@link org.eclipse.lsp4j.CodeActionContext}. Unlike that LSP type, this context carries only what
 * modules need to contribute their {@link nl.ramsolutions.sw.magik.CodeAction}s; {@code
 * getOnly()}-based kind filtering is cross-cutting and stays in {@link CodeActionProvider}.
 *
 * @param file The file code actions are provided for.
 * @param range The range code actions were requested for.
 */
public record CodeActionModuleContext(MagikTypedFile file, Range range) {}
