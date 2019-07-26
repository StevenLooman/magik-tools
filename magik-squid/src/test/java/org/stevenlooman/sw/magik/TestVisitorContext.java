package org.stevenlooman.sw.magik;

import com.sonar.sslr.api.AstNode;
import org.stevenlooman.sw.magik.parser.MagikParser;

import java.nio.charset.Charset;

public class TestVisitorContext {

  public static MagikVisitorContext create(String code) {
    MagikParser parser = new MagikParser(Charset.forName("UTF-8"));
    AstNode root = parser.parse(code);
    return new MagikVisitorContext(code, root);
  }

}
