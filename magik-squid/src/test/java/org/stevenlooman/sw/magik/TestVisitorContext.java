package org.stevenlooman.sw.magik;

import com.google.common.base.Charsets;
import com.sonar.sslr.api.AstNode;
import org.stevenlooman.sw.magik.parser.MagikParser;

public class TestVisitorContext {

  public static MagikVisitorContext create(String code) {
    MagikParser parser = new MagikParser(Charsets.UTF_8);
    AstNode root = parser.parse(code);
    return new MagikVisitorContext(code, root);
  }

}
