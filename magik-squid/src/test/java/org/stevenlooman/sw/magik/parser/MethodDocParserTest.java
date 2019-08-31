package org.stevenlooman.sw.magik.parser;

import static org.fest.assertions.Assertions.assertThat;

import java.nio.charset.Charset;

import com.sonar.sslr.api.AstNode;

import org.junit.Test;
import org.stevenlooman.sw.magik.api.MagikGrammar;

public class MethodDocParserTest {

  @Test
  public void testExtractDoc() {
    String code =
      "_method a.b(param1, param2, param3?)\n" +
      "  ## Function  : example\n" +
      "  ## Parameters: PARAM1: example parameter 1\n" +
      "  ##             PARAM2: example parameter 2\n" +
      "  ##             PARAM3?: example parameter 3\n" +
      "  ## Returns   : -\n" +
      "  do_something()\n" +
      "  _if a\n" +
      "  _then\n" +
      "    write(a)\n" +
      "  _endif\n" +
      "_endmethod";
    MagikParser parser = new MagikParser(Charset.forName("utf-8"));
    AstNode rootNode = parser.parseSafe(code);
    AstNode methodNode = rootNode.getFirstChild(MagikGrammar.METHOD_DEFINITION);
    MethodDocParser docParser = new MethodDocParser(methodNode);

    assertThat(docParser.getSection("function")).isEqualTo("example");
    assertThat(docParser.getLineForSection("function")).isEqualTo(2);

    assertThat(docParser.getSection("parameters")).isEqualTo(
        "PARAM1: example parameter 1\n" +
        "PARAM2: example parameter 2\n" +
        "PARAM3?: example parameter 3");
    assertThat(docParser.getLineForSection("parameters")).isEqualTo(3);

    assertThat(docParser.getSection("returns")).isEqualTo("-");
    assertThat(docParser.getLineForSection("returns")).isEqualTo(6);

    assertThat(docParser.getParameters().get("param1")).isEqualTo("example parameter 1");
    assertThat(docParser.getLineForParameter("param1")).isEqualTo(3);

    assertThat(docParser.getParameters().get("param2")).isEqualTo("example parameter 2");
    assertThat(docParser.getLineForParameter("param2")).isEqualTo(4);

    assertThat(docParser.getParameters().get("param3?")).isEqualTo("example parameter 3");
    assertThat(docParser.getLineForParameter("param3?")).isEqualTo(5);
  }

}