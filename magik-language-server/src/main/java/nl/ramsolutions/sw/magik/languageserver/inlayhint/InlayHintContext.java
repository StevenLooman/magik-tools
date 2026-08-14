package nl.ramsolutions.sw.magik.languageserver.inlayhint;

import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Range;

/**
 * Shared, pre-computed context passed to each {@link InlayHintModule}.
 *
 * @param file The file inlay hints are provided for.
 * @param range The range inlay hints were requested for.
 */
public record InlayHintContext(MagikTypedFile file, Range range) {}
