package nl.ramsolutions.sw.magik.languageserver.semantictokens;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import nl.ramsolutions.sw.IDefinition;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import org.eclipse.lsp4j.SemanticTokens;
import org.junit.jupiter.api.Test;

/** Test SemanticTokenProvider. */
@SuppressWarnings("checkstyle:MagicNumber")
class SemanticTokenProviderTest {

  private static final String PARENT_CHILD_LIBRARY =
      """
      _package user
      def_slotted_exemplar(:zz_p, {})
      def_slotted_exemplar(:zz_c, {}, {:zz_p})
      _pragma(classify_level=deprecated)
      _method zz_p.m()
      _endmethod
      _pragma(classify_level=basic)
      _method zz_c.m()
      _endmethod""";

  private static final String PARENT_CHILD_CODE =
      """
      _package user
      _method zz_c.run()
        _super.m()
        _self.m()
      _endmethod""";

  private SemanticTokens getSemanticTokens(final String code) {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedFile magikFile =
        new MagikTypedFile(MagikTypedFile.DEFAULT_URI, code, definitionKeeper);
    final SemanticTokenProvider provider = new SemanticTokenProvider();
    return provider.provideSemanticTokensFull(magikFile);
  }

  /** A semantic token at an absolute position, decoded from the relative LSP data. */
  private record DecodedToken(int line, int column, int length, int type, int modifiers) {}

  private List<DecodedToken> getSemanticTokensAt(
      final String library, final String code, final int line, final int column) {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final URI libraryUri = URI.create("tests://unittest/library.magik");
    final MagikTypedFile libraryFile = new MagikTypedFile(libraryUri, library, definitionKeeper);
    final Collection<IDefinition> definitions = libraryFile.getDefinitions();
    definitions.stream().map(IDefinition::getBareDefinition).forEach(definitionKeeper::add);

    final MagikTypedFile magikFile =
        new MagikTypedFile(MagikTypedFile.DEFAULT_URI, code, definitionKeeper);
    final SemanticTokenProvider provider = new SemanticTokenProvider();
    final SemanticTokens semanticTokens = provider.provideSemanticTokensFull(magikFile);
    final List<DecodedToken> decodedTokens = this.decodeSemanticTokens(semanticTokens);
    return decodedTokens.stream()
        .filter(token -> token.line() == line && token.column() == column)
        .toList();
  }

  private List<DecodedToken> decodeSemanticTokens(final SemanticTokens semanticTokens) {
    final List<Integer> data = semanticTokens.getData();
    final List<DecodedToken> decodedTokens = new ArrayList<>();
    int line = 0;
    int column = 0;
    for (int i = 0; i < data.size(); i += 5) {
      final int deltaLine = data.get(i);
      final int deltaColumn = data.get(i + 1);
      column = deltaLine == 0 ? column + deltaColumn : deltaColumn;
      line += deltaLine;
      final DecodedToken decodedToken =
          new DecodedToken(line, column, data.get(i + 2), data.get(i + 3), data.get(i + 4));
      decodedTokens.add(decodedToken);
    }
    return decodedTokens;
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
            0,
            0,
            "a".length(),
            SemanticToken.Type.VARIABLE.getTokenType(),
            varGlobalModifier,
            0,
            2,
            "_is".length(),
            SemanticToken.Type.KEYWORD.getTokenType(),
            0,
            0,
            4,
            "b".length(),
            SemanticToken.Type.VARIABLE.getTokenType(),
            varGlobalModifier);
  }

  @Test
  void testOperator() {
    final String code = "a + b";
    final SemanticTokens semanticTokens = this.getSemanticTokens(code);
    final int varGlobalModifier = SemanticToken.Modifier.VARIABLE_GLOBAL.getModifierType();
    assertThat(semanticTokens.getData())
        .containsExactly(
            0,
            0,
            "a".length(),
            SemanticToken.Type.VARIABLE.getTokenType(),
            varGlobalModifier,
            0,
            2,
            "+".length(),
            SemanticToken.Type.OPERATOR.getTokenType(),
            0,
            0,
            2,
            "b".length(),
            SemanticToken.Type.VARIABLE.getTokenType(),
            varGlobalModifier);
  }

  @Test
  void testParameter() {
    final String code = "_proc(x) x _endproc";
    final SemanticTokens semanticTokens = this.getSemanticTokens(code);
    assertThat(semanticTokens.getData())
        .containsExactly(
            0,
            0,
            "_proc".length(),
            SemanticToken.Type.KEYWORD.getTokenType(),
            0,
            0,
            6,
            "x".length(),
            SemanticToken.Type.PARAMETER.getTokenType(),
            0,
            0,
            3,
            "x".length(),
            SemanticToken.Type.PARAMETER.getTokenType(),
            0,
            0,
            2,
            "_endproc".length(),
            SemanticToken.Type.KEYWORD.getTokenType(),
            0);
  }

  @Test
  void testVariableDefinition() {
    final String code = "_local xxx";
    final SemanticTokens semanticTokens = this.getSemanticTokens(code);
    assertThat(semanticTokens.getData())
        .containsExactly(
            0,
            0,
            "_local".length(),
            SemanticToken.Type.KEYWORD.getTokenType(),
            0,
            0,
            7,
            "xxx".length(),
            SemanticToken.Type.VARIABLE.getTokenType(),
            0);
  }

  @Test
  void testForVariable() {
    final String code = "_for aaa _over a() _loop _endloop";
    final SemanticTokens semanticTokens = this.getSemanticTokens(code);
    assertThat(semanticTokens.getData())
        .containsExactly(
            0,
            0,
            "_for".length(),
            SemanticToken.Type.KEYWORD.getTokenType(),
            0,
            0,
            5,
            "aaa".length(),
            SemanticToken.Type.VARIABLE.getTokenType(),
            0,
            0,
            4,
            "_over".length(),
            SemanticToken.Type.KEYWORD.getTokenType(),
            0,
            0,
            6,
            "a".length(),
            SemanticToken.Type.FUNCTION.getTokenType(),
            0,
            0,
            4,
            "_loop".length(),
            SemanticToken.Type.KEYWORD.getTokenType(),
            0,
            0,
            6,
            "_endloop".length(),
            SemanticToken.Type.KEYWORD.getTokenType(),
            0);
  }

  @Test
  void testProcedureInvocation() {
    final String code = "a()";
    final SemanticTokens semanticTokens = this.getSemanticTokens(code);
    assertThat(semanticTokens.getData())
        .containsExactly(0, 0, "a".length(), SemanticToken.Type.FUNCTION.getTokenType(), 0);
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
            0,
            0,
            "a".length(),
            SemanticToken.Type.VARIABLE.getTokenType(),
            varGlobalModifier,
            0,
            2,
            "method".length(),
            SemanticToken.Type.METHOD.getTokenType(),
            0);
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
            0,
            1,
            "## doc comment".length(),
            SemanticToken.Type.COMMENT.getTokenType(),
            docModifier);
  }

  @Test
  void testDocCommentParam() {
    final String code = " ## @param {sw:char16_vector} p1 Description of p1";
    final SemanticTokens semanticTokens = this.getSemanticTokens(code);
    final int docModifier = SemanticToken.Modifier.DOCUMENTATION.getModifierType();
    assertThat(semanticTokens.getData())
        .containsExactly(
            0,
            1,
            "##".length(),
            SemanticToken.Type.COMMENT.getTokenType(),
            docModifier,
            0,
            3,
            "@param".length(),
            SemanticToken.Type.KEYWORD.getTokenType(),
            docModifier,
            0,
            8,
            "sw:char16_vector".length(),
            SemanticToken.Type.CLASS.getTokenType(),
            docModifier,
            0,
            18,
            "p1".length(),
            SemanticToken.Type.PARAMETER.getTokenType(),
            docModifier,
            0,
            3,
            "Description of p1".length(),
            SemanticToken.Type.COMMENT.getTokenType(),
            docModifier);
  }

  @Test
  void testDocCommentParamCombinedType() {
    final String code = " ## @param {sw:float|sw:integer} p1 Description of p1";
    final SemanticTokens semanticTokens = this.getSemanticTokens(code);
    final int docModifier = SemanticToken.Modifier.DOCUMENTATION.getModifierType();
    assertThat(semanticTokens.getData())
        .containsExactly(
            0,
            1,
            "##".length(),
            SemanticToken.Type.COMMENT.getTokenType(),
            docModifier,
            0,
            3,
            "@param".length(),
            SemanticToken.Type.KEYWORD.getTokenType(),
            docModifier,
            0,
            8,
            "sw:float".length(),
            SemanticToken.Type.CLASS.getTokenType(),
            docModifier,
            0,
            9,
            "sw:integer".length(),
            SemanticToken.Type.CLASS.getTokenType(),
            docModifier,
            0,
            12,
            "p1".length(),
            SemanticToken.Type.PARAMETER.getTokenType(),
            docModifier,
            0,
            3,
            "Description of p1".length(),
            SemanticToken.Type.COMMENT.getTokenType(),
            docModifier);
  }

  @Test
  void testDocCommentGeneric() {
    final String code = " ## @generic E Elements of the vector";
    final SemanticTokens semanticTokens = this.getSemanticTokens(code);
    final int docModifier = SemanticToken.Modifier.DOCUMENTATION.getModifierType();
    assertThat(semanticTokens.getData())
        .containsExactly(
            0,
            1,
            "##".length(),
            SemanticToken.Type.COMMENT.getTokenType(),
            docModifier,
            0,
            3,
            "@generic".length(),
            SemanticToken.Type.KEYWORD.getTokenType(),
            docModifier,
            0,
            9,
            "E".length(),
            SemanticToken.Type.TYPE_PARAMETER.getTokenType(),
            docModifier,
            0,
            2,
            "Elements of the vector".length(),
            SemanticToken.Type.COMMENT.getTokenType(),
            docModifier);
  }

  @Test
  void testDocCommentGenericWithType() {
    final String code = " ## @generic {sw:simple_vector<E>} E Elements of the vector";
    final SemanticTokens semanticTokens = this.getSemanticTokens(code);
    final int docModifier = SemanticToken.Modifier.DOCUMENTATION.getModifierType();
    assertThat(semanticTokens.getData())
        .containsExactly(
            0,
            1,
            "##".length(),
            SemanticToken.Type.COMMENT.getTokenType(),
            docModifier,
            0,
            3,
            "@generic".length(),
            SemanticToken.Type.KEYWORD.getTokenType(),
            docModifier,
            0,
            10,
            "sw:simple_vector".length(),
            SemanticToken.Type.CLASS.getTokenType(),
            docModifier,
            0,
            17,
            "E".length(),
            SemanticToken.Type.TYPE_PARAMETER.getTokenType(),
            docModifier,
            0,
            4,
            "E".length(),
            SemanticToken.Type.TYPE_PARAMETER.getTokenType(),
            docModifier,
            0,
            2,
            "Elements of the vector".length(),
            SemanticToken.Type.COMMENT.getTokenType(),
            docModifier);
  }

  @Test
  void testMethodAndSlot() {
    final String code = "_method aaa.bbb .slot _endmethod";
    final SemanticTokens semanticTokens = this.getSemanticTokens(code);
    assertThat(semanticTokens.getData())
        .containsExactly(
            0,
            0,
            "_method".length(),
            SemanticToken.Type.KEYWORD.getTokenType(),
            0,
            0,
            8,
            "aaa".length(),
            SemanticToken.Type.CLASS.getTokenType(),
            0,
            0,
            4,
            "bbb".length(),
            SemanticToken.Type.METHOD.getTokenType(),
            0,
            0,
            5,
            "slot".length(),
            SemanticToken.Type.PROPERTY.getTokenType(),
            0,
            0,
            5,
            "_endmethod".length(),
            SemanticToken.Type.KEYWORD.getTokenType(),
            0);
  }

  @Test
  void testLocalsDefinition() {
    final String code = "_local (a, b) << (1, 2)";
    final SemanticTokens semanticTokens = this.getSemanticTokens(code);
    assertThat(semanticTokens.getData())
        .containsExactly(
            0,
            0,
            "_local".length(),
            SemanticToken.Type.KEYWORD.getTokenType(),
            0,
            0,
            8,
            "a".length(),
            SemanticToken.Type.VARIABLE.getTokenType(),
            0,
            0,
            3,
            "b".length(),
            SemanticToken.Type.VARIABLE.getTokenType(),
            0,
            0,
            3,
            "<<".length(),
            SemanticToken.Type.OPERATOR.getTokenType(),
            0,
            0,
            4,
            "1".length(),
            SemanticToken.Type.NUMBER.getTokenType(),
            0,
            0,
            3,
            "2".length(),
            SemanticToken.Type.NUMBER.getTokenType(),
            0);
  }

  @Test
  void testMethodSyntaxError() {
    final String code = "_method a.b\n_a\n_endmethod";
    final SemanticTokens semanticTokens = this.getSemanticTokens(code);
    assertThat(semanticTokens.getData())
        .containsExactly(
            0,
            0,
            "_method".length(),
            SemanticToken.Type.KEYWORD.getTokenType(),
            0,
            2,
            0,
            "_endmethod".length(),
            SemanticToken.Type.KEYWORD.getTokenType(),
            0);
  }

  @Test
  void testMethodInvocationDeprecated() {
    final String library =
        """
        _package sw
        _pragma(classify_level=deprecated)
        _method object.m()
        _endmethod""";
    final String code = "object.m()";
    final List<DecodedToken> tokens = this.getSemanticTokensAt(library, code, 0, 7);
    final int deprecatedModifier = SemanticToken.Modifier.DEPRECATED.getModifierType();
    assertThat(tokens)
        .containsExactly(
            new DecodedToken(
                0, 7, "m".length(), SemanticToken.Type.METHOD.getTokenType(), deprecatedModifier));
  }

  @Test
  void testMethodInvocationNotDeprecated() {
    final String library =
        """
        _package sw
        _pragma(classify_level=basic)
        _method object.m()
        _endmethod""";
    final String code = "object.m()";
    final List<DecodedToken> tokens = this.getSemanticTokensAt(library, code, 0, 7);
    assertThat(tokens)
        .containsExactly(
            new DecodedToken(0, 7, "m".length(), SemanticToken.Type.METHOD.getTokenType(), 0));
  }

  @Test
  void testSelfMethodInvocationDeprecated() {
    final String library =
        """
        _package user
        def_slotted_exemplar(:zz_e, {})
        _pragma(classify_level=deprecated)
        _method zz_e.m()
        _endmethod""";
    final String code =
        """
        _package user
        _method zz_e.run()
          _self.m()
        _endmethod""";
    final List<DecodedToken> tokens = this.getSemanticTokensAt(library, code, 2, 8);
    final int deprecatedModifier = SemanticToken.Modifier.DEPRECATED.getModifierType();
    assertThat(tokens)
        .containsExactly(
            new DecodedToken(
                2, 8, "m".length(), SemanticToken.Type.METHOD.getTokenType(), deprecatedModifier));
  }

  @Test
  void testSuperMethodInvocationDeprecatedOnParent() {
    final List<DecodedToken> tokens =
        this.getSemanticTokensAt(PARENT_CHILD_LIBRARY, PARENT_CHILD_CODE, 2, 9);
    final int deprecatedModifier = SemanticToken.Modifier.DEPRECATED.getModifierType();
    assertThat(tokens)
        .containsExactly(
            new DecodedToken(
                2, 9, "m".length(), SemanticToken.Type.METHOD.getTokenType(), deprecatedModifier));
  }

  @Test
  void testSelfMethodInvocationOverrideNotDeprecated() {
    final List<DecodedToken> tokens =
        this.getSemanticTokensAt(PARENT_CHILD_LIBRARY, PARENT_CHILD_CODE, 3, 8);
    assertThat(tokens)
        .containsExactly(
            new DecodedToken(3, 8, "m".length(), SemanticToken.Type.METHOD.getTokenType(), 0));
  }

  @Test
  void testSuperMethodInvocationOnMixinHasNoObjectFallback() {
    // A mixin's own table has no parents, so sw:object's deprecated method does not answer.
    // The named form leaves out the implicit sw:slotted_format_mixin parent.
    final String library =
        """
        _package sw
        _pragma(classify_level=deprecated)
        _method object.m()
        _endmethod
        _package user
        def_mixin(:zz_m)
        def_slotted_exemplar(:zz_e, {}, {:zz_m})""";
    final String code =
        """
        _package user
        _method zz_e.run()
          _super(zz_m).m()
        _endmethod""";
    final List<DecodedToken> tokens = this.getSemanticTokensAt(library, code, 2, 15);
    assertThat(tokens)
        .containsExactly(
            new DecodedToken(2, 15, "m".length(), SemanticToken.Type.METHOD.getTokenType(), 0));
  }

  @Test
  void testIndexedMethodInvocation() {
    final String library =
        """
        _package sw
        _pragma(classify_level=deprecated)
        _method object[index]
        _endmethod""";
    final String code = "object[1]";
    final List<DecodedToken> tokens = this.getSemanticTokensAt(library, code, 0, 7);
    assertThat(tokens)
        .containsExactly(
            new DecodedToken(0, 7, "1".length(), SemanticToken.Type.NUMBER.getTokenType(), 0));
  }

  @Test
  void testDeprecatedMethodDefinitionName() {
    final String code =
        """
        _package sw
        _pragma(classify_level=deprecated)
        _method object.m()
        _endmethod""";
    final List<DecodedToken> tokens = this.getSemanticTokensAt(code, code, 2, 15);
    assertThat(tokens)
        .containsExactly(
            new DecodedToken(2, 15, "m".length(), SemanticToken.Type.METHOD.getTokenType(), 0));
  }
}
