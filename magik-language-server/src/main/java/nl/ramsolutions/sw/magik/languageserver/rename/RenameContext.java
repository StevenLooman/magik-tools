package nl.ramsolutions.sw.magik.languageserver.rename;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.magik.MagikTypedFile;

/**
 * Shared, pre-computed context passed to each {@link RenameModule}.
 *
 * @param magikFile The file to rename in.
 * @param node The node at the position rename was requested for.
 */
record RenameContext(MagikTypedFile magikFile, AstNode node) {}
