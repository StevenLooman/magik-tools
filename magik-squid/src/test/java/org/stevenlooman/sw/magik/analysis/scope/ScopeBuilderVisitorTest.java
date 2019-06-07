package org.stevenlooman.sw.magik.analysis.scope;

import static org.fest.assertions.Assertions.assertThat;

import com.google.common.base.Charsets;
import com.sonar.sslr.api.AstNode;
import org.junit.Test;
import org.stevenlooman.sw.magik.MagikVisitorContext;
import org.stevenlooman.sw.magik.parser.MagikParser;

public class ScopeBuilderVisitorTest {

  protected static MagikVisitorContext createContext(String code) throws IllegalArgumentException {
    MagikParser parser = new MagikParser(Charsets.UTF_8);
    AstNode root = parser.parse(code);
    if (root.getChildren().isEmpty()) {
      throw new IllegalArgumentException("Unable to parse code");
    }
    return new MagikVisitorContext(code, root);
  }

  @Test
  public void testSerialAssignment() {
    String code =
        "_method object.m\n" +
        "\t_local a << b << _unset\n" +
        "_endmethod\n";
    MagikVisitorContext context = createContext(code);

    ScopeBuilderVisitor visitor = new ScopeBuilderVisitor();
    visitor.scanFile(context);

    Scope globalScope = visitor.getScope();
    Scope methodScope = globalScope.getSelfAndDescendantScopes().get(1);

    ScopeEntry entryA = methodScope.getScopeEntry("a");
    assertThat(entryA).isNotNull();
    assertThat(entryA.getType() == ScopeEntry.Type.LOCAL);

    ScopeEntry entryB = methodScope.getScopeEntry("b");
    assertThat(entryB).isNotNull();
    assertThat(entryB.getType() == ScopeEntry.Type.LOCAL);
  }

}
