package nl.ramsolutions.sw.magik.ramsolutions.semantictokens;

import java.net.URI;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeKeeper;
import nl.ramsolutions.sw.magik.languageserver.semantictokens.SemanticToken;
import nl.ramsolutions.sw.magik.languageserver.semantictokens.SemanticTokenProvider;
import org.eclipse.lsp4j.SemanticTokens;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test SemanticTokenProvider.
 */
@SuppressWarnings("checkstyle:MagicNumber")
class SemanticTokenProviderTest {

    private SemanticTokens getSemanticTokens(final String code) {
        final URI uri = URI.create("tests://unittest");
        final ITypeKeeper typeKeeper = new TypeKeeper();
        final MagikTypedFile magikFile = new MagikTypedFile(uri, code, typeKeeper);
        final SemanticTokenProvider provider = new SemanticTokenProvider();
        return provider.provideSemanticTokensFull(magikFile);
    }

    @Test
    void testString() {
        final String code = "\"string\"";
        final SemanticTokens semanticTokens = this.getSemanticTokens(code);
        assertThat(semanticTokens.getData())
            .containsExactly(0, 0, "\"string\"".length(), SemanticToken.Type.STRING.getTokenType(), 0);
    }

    @Test
    void testSymbol() {
        final String code = ":string";
        final SemanticTokens semanticTokens = this.getSemanticTokens(code);
        assertThat(semanticTokens.getData())
            .containsExactly(0, 0, ":string".length(), SemanticToken.Type.STRING.getTokenType(), 0);
    }

    @Test
    void testNumber() {
        final String code = "100";
        final SemanticTokens semanticTokens = this.getSemanticTokens(code);
        assertThat(semanticTokens.getData())
            .containsExactly(0, 0, "100".length(), SemanticToken.Type.NUMBER.getTokenType(), 0);
    }

    @Test
    void testKeyword() {
        final String code = "a _is b";
        final SemanticTokens semanticTokens = this.getSemanticTokens(code);
        final int varGlobalModifier = SemanticToken.Modifier.VARIABLE_GLOBAL.getModifierType();
        assertThat(semanticTokens.getData())
            .containsExactly(
                0, 0, "a".length(), SemanticToken.Type.VARIABLE.getTokenType(), varGlobalModifier,
                0, 2, "_is".length(), SemanticToken.Type.KEYWORD.getTokenType(), 0,
                0, 4, "b".length(), SemanticToken.Type.VARIABLE.getTokenType(), varGlobalModifier);
    }

    @Test
    void testOperator() {
        final String code = "a + b";
        final SemanticTokens semanticTokens = this.getSemanticTokens(code);
        final int varGlobalModifier = SemanticToken.Modifier.VARIABLE_GLOBAL.getModifierType();
        assertThat(semanticTokens.getData())
            .containsExactly(
                0, 0, "a".length(), SemanticToken.Type.VARIABLE.getTokenType(), varGlobalModifier,
                0, 2, "+".length(), SemanticToken.Type.OPERATOR.getTokenType(), 0,
                0, 2, "b".length(), SemanticToken.Type.VARIABLE.getTokenType(), varGlobalModifier);
    }

    @Test
    void testParameter() {
        final String code = "_proc(x) x _endproc";
        final SemanticTokens semanticTokens = this.getSemanticTokens(code);
        assertThat(semanticTokens.getData())
            .containsExactly(
                0, 0, "_proc".length(), SemanticToken.Type.KEYWORD.getTokenType(), 0,
                0, 6, "x".length(), SemanticToken.Type.PARAMETER.getTokenType(), 0,
                0, 3, "x".length(), SemanticToken.Type.PARAMETER.getTokenType(), 0,
                0, 2, "_endproc".length(), SemanticToken.Type.KEYWORD.getTokenType(), 0);
    }

    @Test
    void testVariableDefinition() {
        final String code = "_local xxx";
        final SemanticTokens semanticTokens = this.getSemanticTokens(code);
        assertThat(semanticTokens.getData())
            .containsExactly(
                0, 0, "_local".length(), SemanticToken.Type.KEYWORD.getTokenType(), 0,
                0, 7, "xxx".length(), SemanticToken.Type.VARIABLE.getTokenType(), 0);
    }

    @Test
    void testForVariable() {
        final String code = "_for aaa _over a() _loop _endloop";
        final SemanticTokens semanticTokens = this.getSemanticTokens(code);
        assertThat(semanticTokens.getData())
            .containsExactly(
                0, 0, "_for".length(), SemanticToken.Type.KEYWORD.getTokenType(), 0,
                0, 5, "aaa".length(), SemanticToken.Type.VARIABLE.getTokenType(), 0,
                0, 4, "_over".length(), SemanticToken.Type.KEYWORD.getTokenType(), 0,
                0, 6, "a".length(), SemanticToken.Type.FUNCTION.getTokenType(), 0,
                0, 4, "_loop".length(), SemanticToken.Type.KEYWORD.getTokenType(), 0,
                0, 6, "_endloop".length(), SemanticToken.Type.KEYWORD.getTokenType(), 0);
    }

    @Test
    void testProcedureInvocation() {
        final String code = "a()";
        final SemanticTokens semanticTokens = this.getSemanticTokens(code);
        assertThat(semanticTokens.getData())
            .containsExactly(
                0, 0, "a".length(), SemanticToken.Type.FUNCTION.getTokenType(), 0);  // TODO: Why no varGlobalModifier?
    }

    @Test
    void testSelfReadonly() {
        final String code = "_self";
        final SemanticTokens semanticTokens = this.getSemanticTokens(code);
        final int readonlyModifier = SemanticToken.Modifier.READONLY.getModifierType();
        assertThat(semanticTokens.getData())
            .containsExactly(
                0, 0, "_self".length(), SemanticToken.Type.VARIABLE.getTokenType(), readonlyModifier);
    }

    @Test
    void testMethodInvocation() {
        final String code = "a.method()";
        final SemanticTokens semanticTokens = this.getSemanticTokens(code);
        final int varGlobalModifier = SemanticToken.Modifier.VARIABLE_GLOBAL.getModifierType();
        assertThat(semanticTokens.getData())
            .containsExactly(
                0, 0, "a".length(), SemanticToken.Type.VARIABLE.getTokenType(), varGlobalModifier,
                0, 2, "method".length(), SemanticToken.Type.METHOD.getTokenType(), 0);
    }

    @Test
    void testComment() {
        final String code = " # comment";
        final SemanticTokens semanticTokens = this.getSemanticTokens(code);
        assertThat(semanticTokens.getData())
            .containsExactly(0, 1, "# comment".length(), SemanticToken.Type.COMMENT.getTokenType(), 0);
    }

    @Test
    void testDocCommentFunction() {
        final String code = " ## doc comment";
        final SemanticTokens semanticTokens = this.getSemanticTokens(code);
        final int docModifier = SemanticToken.Modifier.DOCUMENTATION.getModifierType();
        assertThat(semanticTokens.getData())
            .containsExactly(
                0, 1, "## doc comment".length(), SemanticToken.Type.COMMENT.getTokenType(), docModifier);
    }

    @Test
    void testDocCommentParam() {
        final String code = " ## @param {sw:char16_vector} p1 Description of p1";
        final SemanticTokens semanticTokens = this.getSemanticTokens(code);
        final int docModifier = SemanticToken.Modifier.DOCUMENTATION.getModifierType();
        assertThat(semanticTokens.getData())
            .containsExactly(
                0, 1, "##".length(), SemanticToken.Type.COMMENT.getTokenType(), docModifier,
                0, 3, "@param".length(), SemanticToken.Type.KEYWORD.getTokenType(), docModifier,
                0, 8, "sw:char16_vector".length(), SemanticToken.Type.CLASS.getTokenType(), docModifier,
                0, 18, "p1".length(), SemanticToken.Type.PARAMETER.getTokenType(), docModifier,
                0, 3, "Description of p1".length(), SemanticToken.Type.COMMENT.getTokenType(), docModifier);
    }

    @Test
    void testDocCommentParamCombinedType() {
        final String code = " ## @param {sw:float|sw:integer} p1 Description of p1";
        final SemanticTokens semanticTokens = this.getSemanticTokens(code);
        final int docModifier = SemanticToken.Modifier.DOCUMENTATION.getModifierType();
        assertThat(semanticTokens.getData())
            .containsExactly(
                0, 1, "##".length(), SemanticToken.Type.COMMENT.getTokenType(), docModifier,
                0, 3, "@param".length(), SemanticToken.Type.KEYWORD.getTokenType(), docModifier,
                0, 8, "sw:float".length(), SemanticToken.Type.CLASS.getTokenType(), docModifier,
                0, 9, "sw:integer".length(), SemanticToken.Type.CLASS.getTokenType(), docModifier,
                0, 12, "p1".length(), SemanticToken.Type.PARAMETER.getTokenType(), docModifier,
                0, 3, "Description of p1".length(), SemanticToken.Type.COMMENT.getTokenType(), docModifier);
    }

    @Test
    void testMethodAndSlot() {
        final String code = "_method aaa.bbb .slot _endmethod";
        final SemanticTokens semanticTokens = this.getSemanticTokens(code);
        assertThat(semanticTokens.getData())
            .containsExactly(
                0, 0, "_method".length(), SemanticToken.Type.KEYWORD.getTokenType(), 0,
                0, 8, "aaa".length(), SemanticToken.Type.CLASS.getTokenType(), 0,
                0, 4, "bbb".length(), SemanticToken.Type.METHOD.getTokenType(), 0,
                0, 5, "slot".length(), SemanticToken.Type.PROPERTY.getTokenType(), 0,
                0, 5, "_endmethod".length(), SemanticToken.Type.KEYWORD.getTokenType(), 0);
    }

    @Test
    void testLocalsDefinition() {
        final String code = "_local (a, b) << (1, 2)";
        final SemanticTokens semanticTokens = this.getSemanticTokens(code);
        assertThat(semanticTokens.getData())
            .containsExactly(
                0, 0, "_local".length(), SemanticToken.Type.KEYWORD.getTokenType(), 0,
                0, 8, "a".length(), SemanticToken.Type.VARIABLE.getTokenType(), 0,
                0, 3, "b".length(), SemanticToken.Type.VARIABLE.getTokenType(), 0,
                0, 3, "<<".length(), SemanticToken.Type.OPERATOR.getTokenType(), 0,
                0, 4, "1".length(), SemanticToken.Type.NUMBER.getTokenType(), 0,
                0, 3, "2".length(), SemanticToken.Type.NUMBER.getTokenType(), 0);
    }

    @Test
    void testMethodSyntaxError() {
        final String code = "_method a.b\n_a\n_endmethod";
        final SemanticTokens semanticTokens = this.getSemanticTokens(code);
        assertThat(semanticTokens.getData())
            .containsExactly(
                0, 0, "_method".length(), SemanticToken.Type.KEYWORD.getTokenType(), 0,
                2, 0, "_endmethod".length(), SemanticToken.Type.KEYWORD.getTokenType(), 0);
    }

}
