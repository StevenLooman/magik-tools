package org.stevenlooman.sw.magik.parser;

import static org.fest.assertions.Assertions.assertThat;

import com.sonar.sslr.api.AstNode;
import org.junit.Test;
import org.stevenlooman.sw.magik.api.MagikGrammar;

import java.nio.charset.Charset;

public class MagikParserTest {

  @Test
  public void testParseIdentifier() {
    assertThat(MagikParser.parseIdentifier("abc")).isEqualTo("abc");
    assertThat(MagikParser.parseIdentifier("ABC")).isEqualTo("abc");
    assertThat(MagikParser.parseIdentifier("abc|def|")).isEqualTo("abcdef");
    assertThat(MagikParser.parseIdentifier("abc| |")).isEqualTo("abc ");
    assertThat(MagikParser.parseIdentifier("abc|def|ghi")).isEqualTo("abcdefghi");
    assertThat(MagikParser.parseIdentifier("abc|DEF|")).isEqualTo("abcDEF");
    assertThat(MagikParser.parseIdentifier("abc|DEF|ghi|JKL|")).isEqualTo("abcDEFghiJKL");
    assertThat(MagikParser.parseIdentifier("abc| |")).isEqualTo("abc ");
    assertThat(MagikParser.parseIdentifier("\\|")).isEqualTo("|");
    assertThat(MagikParser.parseIdentifier("\\|a")).isEqualTo("|a");
  }

  @Test
  public void testInvalidMagik() {
    MagikParser parser = new MagikParser(Charset.defaultCharset());
    String code =
        "_block\n" +
        "\t_loop\n" +
        "\t_endloo\n" +
        "_endblock\n";
    AstNode node = parser.parse(code);
    assertThat(node.getChildren()).hasSize(1);
    AstNode parserErrorNode = node.getChildren().get(0);
    assertThat(parserErrorNode.getType()).isEqualTo(MagikGrammar.PARSER_ERROR);
    assertThat(parserErrorNode.getToken().getLine()).isEqualTo(3);
    assertThat(parserErrorNode.getToken().getColumn()).isEqualTo(2);
  }

}
