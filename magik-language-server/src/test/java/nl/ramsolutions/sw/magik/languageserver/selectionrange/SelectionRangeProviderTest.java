package nl.ramsolutions.sw.magik.languageserver.selectionrange;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.typing.TypeKeeper;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SelectionRange;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test SelectionRangeProvider.
 */
@SuppressWarnings("checkstyle:MagicNumber")
class SelectionRangeProviderTest {

    private List<SelectionRange> getSelectionRanges(
            final String code,
            final List<nl.ramsolutions.sw.magik.Position> positions) {
        final var provider = new SelectionRangeProvider();
        final var uri = URI.create("tests://unittest");
        final var typeKeeper = new TypeKeeper();
        final var magikFile = new MagikTypedFile(uri, code, typeKeeper);
        return provider.provideSelectionRanges(magikFile, positions);
    }

    @Test
    void testGetSelectionRanges() {
        final var code = ""
            + "_method object.int!method_name(param?)\n"
            + "    _local var << :symbol\n"
            + "    show(var)\n"
            + "_endmethod\n";
        final List<nl.ramsolutions.sw.magik.Position> positions = Collections.emptyList();
        final var selectionRanges = this.getSelectionRanges(code, positions);
        assertThat(selectionRanges).isEqualTo(
            List.of(
                new SelectionRange(
                    new Range(
                        new Position(0, 0), new Position(0, 7)),
                    null),
                new SelectionRange(
                    new Range(
                        new Position(0, 8), new Position(0, 14)),
                    null),
                new SelectionRange(
                    new Range(
                        new Position(0, 14), new Position(0, 15)),
                    null),
                new SelectionRange(
                    new Range(
                        new Position(0, 15), new Position(0, 30)),
                    null),
                new SelectionRange(
                    new Range(
                        new Position(0, 30), new Position(0, 31)),
                    null),
                new SelectionRange(
                    new Range(
                        new Position(0, 31), new Position(0, 37)),
                    null),
                new SelectionRange(
                    new Range(
                        new Position(0, 37), new Position(0, 38)),
                    null),

                new SelectionRange(
                    new Range(
                        new Position(1, 4), new Position(1, 10)),
                    null),
                new SelectionRange(
                    new Range(
                        new Position(1, 11), new Position(1, 14)),
                    null),
                new SelectionRange(
                    new Range(
                        new Position(1, 15), new Position(1, 17)),
                    null),
                new SelectionRange(
                    new Range(
                        new Position(1, 18), new Position(1, 25)),
                    null),

                new SelectionRange(
                    new Range(
                        new Position(2, 4), new Position(2, 8)),
                    null),
                new SelectionRange(
                    new Range(
                        new Position(2, 8), new Position(2, 9)),
                    null),
                new SelectionRange(
                    new Range(
                        new Position(2, 9), new Position(2, 12)),
                    null),
                new SelectionRange(
                    new Range(
                        new Position(2, 12), new Position(2, 13)),
                    null),

                new SelectionRange(
                    new Range(
                        new Position(3, 0), new Position(3, 10)),
                    null)
            ));
    }

}
