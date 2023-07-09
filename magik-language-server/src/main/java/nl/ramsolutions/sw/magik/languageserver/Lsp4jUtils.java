package nl.ramsolutions.sw.magik.languageserver;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentEdit;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

/**
 * LSP4J utilities.
 */
public final class Lsp4jUtils {

    private static final int TEXT_DOCUMENT_VERSION = -1;

    private Lsp4jUtils() {
        // Utility class.
    }

    /**
     * Comparator for {@link Position}s.
     */
    public static class PositionComparator implements Comparator<Position>, Serializable {

        @Override
        public int compare(final Position position0, final Position position1) {
            if (position0.getLine() != position1.getLine()) {
                return position0.getLine() - position1.getLine();
            }

            return position0.getCharacter() - position1.getCharacter();
        }

    }

    /**
     * Test if {@code range1} overlaps with or is included by {@code range2}.
     * @param range1 Range 1.
     * @param range2 Range 2.
     * @return True if {@code wantedRange} overlaps with or is included by {@code testedRange}.
     */
    public static boolean rangeOverlaps(final Range range1, final Range range2) {
        // 5 Cases:
        // 1. wantedRange ends before testedRange start --> false
        // 2. wantedRange before testedRange and ends in testedRange --> true
        // 3. wantedRange in testedRange --> true
        // 4. wantedRange in testedRange and after testedRange --> true
        // 5. wantedRange starts after testedRange end --> false

        // Testing case 1/5 is enough.
        final PositionComparator comparator = new PositionComparator();
        return !(
            comparator.compare(range1.getEnd(), range2.getStart()) < 0
            || comparator.compare(range1.getStart(), range2.getEnd()) > 0);
    }

    /**
     * Create a {@link CodeAction}.
     * @param magikFile The {@link MagikTypedFile}.
     * @param range The {@link nl.ramsolutions.sw.magik.analysis.Range}.
     * @param newText The new text.
     * @param description The description.
     * @return The {@link CodeAction}.
     */
    public static CodeAction createCodeAction(
            final MagikTypedFile magikFile,
            final nl.ramsolutions.sw.magik.analysis.Range range,
            final String newText,
            final String description) {
        final Range rangeLsp4j = Lsp4jConversion.rangeToLsp4j(range);
        final TextEdit textEdit = new TextEdit(rangeLsp4j, newText);
        final VersionedTextDocumentIdentifier versionedTextDocumentIdentifier =
            new VersionedTextDocumentIdentifier(magikFile.getUri().toString(), TEXT_DOCUMENT_VERSION);
        final TextDocumentEdit textDocumentEdit = new TextDocumentEdit(
            versionedTextDocumentIdentifier,
            List.of(textEdit));
        final WorkspaceEdit edit = new WorkspaceEdit(List.of(Either.forLeft(textDocumentEdit)));

        final CodeAction codeAction = new CodeAction(description);
        codeAction.setKind(CodeActionKind.QuickFix);
        codeAction.setEdit(edit);
        return codeAction;
    }

}
