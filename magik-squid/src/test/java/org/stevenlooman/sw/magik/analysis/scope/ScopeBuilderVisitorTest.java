package org.stevenlooman.sw.magik.analysis.scope;

import static org.fest.assertions.Assertions.assertThat;

import com.sonar.sslr.api.AstNode;
import org.junit.Test;
import org.stevenlooman.sw.magik.MagikVisitorContext;
import org.stevenlooman.sw.magik.parser.MagikParser;

import java.nio.charset.Charset;

public class ScopeBuilderVisitorTest {

  protected static MagikVisitorContext createContext(String code) throws IllegalArgumentException {
    MagikParser parser = new MagikParser(Charset.forName("UTF-8"));
    AstNode root = parser.parseSafe(code);
    if (root == null) {
      throw new IllegalArgumentException("Unable to parse code");
    }
    return new MagikVisitorContext(code, root);
  }

  @Test
  public void testDefinition() {
    String code =
        "_method object.m\n" +
        "\t_local a\n" +
        "_endmethod\n";
    MagikVisitorContext context = createContext(code);

    ScopeBuilderVisitor visitor = new ScopeBuilderVisitor();
    visitor.scanFile(context);

    Scope globalScope = visitor.getGlobalScope();
    Scope methodScope = globalScope.getSelfAndDescendantScopes().get(1);

    ScopeEntry entryA = methodScope.getScopeEntry("a");
    assertThat(entryA).isNotNull();
    assertThat(entryA.getType() == ScopeEntry.Type.LOCAL);
  }

  @Test
  public void testDefinitionSerial() {
    String code =
        "_method object.m\n" +
        "\t_local a, b\n" +
        "_endmethod\n";
    MagikVisitorContext context = createContext(code);

    ScopeBuilderVisitor visitor = new ScopeBuilderVisitor();
    visitor.scanFile(context);

    Scope globalScope = visitor.getGlobalScope();
    Scope methodScope = globalScope.getSelfAndDescendantScopes().get(1);

    ScopeEntry entryA = methodScope.getScopeEntry("a");
    assertThat(entryA).isNotNull();
    assertThat(entryA.getType() == ScopeEntry.Type.LOCAL);

    ScopeEntry entryB = methodScope.getScopeEntry("b");
    assertThat(entryB).isNotNull();
    assertThat(entryB.getType() == ScopeEntry.Type.LOCAL);
  }

  @Test
  public void testDefinitionMultiple() {
    String code =
        "_method object.m\n" +
        "\t_local (a, b) << (1, 2)\n" +
        "_endmethod\n";
    MagikVisitorContext context = createContext(code);

    ScopeBuilderVisitor visitor = new ScopeBuilderVisitor();
    visitor.scanFile(context);

    Scope globalScope = visitor.getGlobalScope();
    Scope methodScope = globalScope.getSelfAndDescendantScopes().get(1);

    ScopeEntry entryA = methodScope.getScopeEntry("a");
    assertThat(entryA).isNotNull();
    assertThat(entryA.getType() == ScopeEntry.Type.LOCAL);

    ScopeEntry entryB = methodScope.getScopeEntry("b");
    assertThat(entryB).isNotNull();
    assertThat(entryB.getType() == ScopeEntry.Type.LOCAL);
  }

  @Test
  public void testDefinitionAssignment() {
    String code =
        "_method object.m\n" +
        "\t_local a << b << _unset\n" +
        "_endmethod\n";
    MagikVisitorContext context = createContext(code);

    ScopeBuilderVisitor visitor = new ScopeBuilderVisitor();
    visitor.scanFile(context);

    Scope globalScope = visitor.getGlobalScope();
    Scope methodScope = globalScope.getSelfAndDescendantScopes().get(1);

    ScopeEntry entryA = methodScope.getScopeEntry("a");
    assertThat(entryA).isNotNull();
    assertThat(entryA.getType() == ScopeEntry.Type.LOCAL);

    ScopeEntry entryB = methodScope.getScopeEntry("b");
    assertThat(entryB).isNotNull();
    assertThat(entryB.getType() == ScopeEntry.Type.DEFINITION);
  }

  @Test
  public void testMultipleAssignment() {
    String code =
        "_method object.m\n" +
        "\t(a, b) << (1, 2)\n" +
        "_endmethod\n";
    MagikVisitorContext context = createContext(code);

    ScopeBuilderVisitor visitor = new ScopeBuilderVisitor();
    visitor.scanFile(context);

    Scope globalScope = visitor.getGlobalScope();
    Scope methodScope = globalScope.getSelfAndDescendantScopes().get(1);

    ScopeEntry entryA = methodScope.getScopeEntry("a");
    assertThat(entryA).isNotNull();
    assertThat(entryA.getType() == ScopeEntry.Type.DEFINITION);

    ScopeEntry entryB = methodScope.getScopeEntry("b");
    assertThat(entryB).isNotNull();
    assertThat(entryB.getType() == ScopeEntry.Type.DEFINITION);
  }

}
