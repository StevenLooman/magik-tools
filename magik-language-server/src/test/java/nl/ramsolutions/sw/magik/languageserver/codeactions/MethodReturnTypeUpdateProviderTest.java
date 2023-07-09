package nl.ramsolutions.sw.magik.languageserver.codeactions;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeKeeper;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionContext;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentEdit;
import org.eclipse.lsp4j.TextEdit;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for MethodReturnTypeUpdateProvider.
 */
class MethodReturnTypeUpdateProviderTest {

    private List<CodeAction> getCodeActions(
            final String code,
            final Range range) {
        final URI uri = URI.create("tests://unittest");
        final ITypeKeeper typeKeeper = new TypeKeeper();
        final MagikTypedFile magikFile = new MagikTypedFile(uri, code, typeKeeper);
        final MethodReturnTypeUpdateProvider provider = new MethodReturnTypeUpdateProvider();
        final CodeActionContext context = new CodeActionContext();
        return provider.provideCodeActions(magikFile, range, context).stream()
                .map(either -> either.getRight())
                .collect(Collectors.toList());
    }

    @Test
    void testAddReturn() {
        final String code = ""
            + "_method obj.method()\n"
            + "  _return 1\n"
            + "_endmethod\n";
        final Range range = new Range(
            new Position(0, 0),
            new Position(3, 0));
        final List<CodeAction> codeactions = this.getCodeActions(code, range);
        assertThat(codeactions).hasSize(1);
        final CodeAction codeAction = codeactions.get(0);
        codeAction.getEdit().getDocumentChanges().forEach(either -> {
            final TextDocumentEdit textDocumentEdit = either.getLeft();
            final String uri = textDocumentEdit.getTextDocument().getUri();
            final List<TextEdit> changes = textDocumentEdit.getEdits();
            assertThat(uri).isEqualTo("tests://unittest");
            assertThat(changes).hasSize(1);
            assertThat(changes.get(0).getNewText()).isEqualTo("\t## @return {sw:integer} Description\n");
        });
    }

    @Test
    void testRemoveReturn() {
        final String code = ""
            + "_method obj.method()\n"
            + "  ## @return {sw:integer} Test\n"
            + "_endmethod\n";
        final Range range = new Range(
            new Position(0, 0),
            new Position(3, 0));
        final List<CodeAction> codeactions = this.getCodeActions(code, range);
        assertThat(codeactions).hasSize(1);
        final CodeAction codeAction = codeactions.get(0);
        codeAction.getEdit().getDocumentChanges().forEach(either -> {
            final TextDocumentEdit textDocumentEdit = either.getLeft();
            final String uri = textDocumentEdit.getTextDocument().getUri();
            final List<TextEdit> changes = textDocumentEdit.getEdits();
            assertThat(uri).isEqualTo("tests://unittest");
            assertThat(changes).hasSize(1);
            assertThat(changes.get(0).getNewText()).isEqualTo("");
        });
    }

    @Test
    void testUpdateReturn() {
        final String code = ""
            + "_method obj.method()\n"
            + "  ## @return {sw:float} Test\n"
            + "  _return 1\n"
            + "_endmethod\n";
        final Range range = new Range(
            new Position(0, 0),
            new Position(4, 0));
        final List<CodeAction> codeactions = this.getCodeActions(code, range);
        assertThat(codeactions).hasSize(1);
        final CodeAction codeAction = codeactions.get(0);
        codeAction.getEdit().getDocumentChanges().forEach(either -> {
            final TextDocumentEdit textDocumentEdit = either.getLeft();
            final String uri = textDocumentEdit.getTextDocument().getUri();
            final List<TextEdit> changes = textDocumentEdit.getEdits();
            assertThat(uri).isEqualTo("tests://unittest");
            assertThat(changes).hasSize(1);
            assertThat(changes.get(0).getNewText()).isEqualTo("sw:integer");
        });
    }

    @Test
    void testNoChangeReturn() {
        final String code = ""
            + "_method obj.method(param1, param2)\n"
            + "  ## @return {sw:integer}\n"
            + "  _return 1\n"
            + "_endmethod\n";
        final Range range = new Range(
            new Position(0, 0),
            new Position(4, 0));
        final List<CodeAction> codeactions = this.getCodeActions(code, range);
        assertThat(codeactions).isEmpty();
    }

}
