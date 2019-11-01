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

  @Test
  public void testTryWith() {
    String code =
        "_try _with a\n" +
        "  _local b\n" +
        "_when error\n" +
        "_endtry";
    MagikVisitorContext context = createContext(code);
    ScopeBuilderVisitor visitor = new ScopeBuilderVisitor();
    visitor.scanFile(context);

    Scope globalScope = visitor.getGlobalScope();
    Scope tryScope = globalScope.getSelfAndDescendantScopes().get(1);
    ScopeEntry entryTryA = tryScope.getScopeEntry("a");
    assertThat(entryTryA).isNull();
    ScopeEntry entryTryB = tryScope.getScopeEntry("b");
    assertThat(entryTryB).isNotNull();
    assertThat(entryTryB.getType() == ScopeEntry.Type.LOCAL);

    Scope whenScope = globalScope.getSelfAndDescendantScopes().get(2);
    ScopeEntry entryWhenA = whenScope.getScopeEntry("a");
    assertThat(entryWhenA).isNotNull();
    assertThat(entryWhenA.getType() == ScopeEntry.Type.DEFINITION);
    ScopeEntry entryWhenB = whenScope.getScopeEntry("b");
    assertThat(entryWhenB).isNull();
  }

  @Test
  public void testForLoop() {
    String code =
        "_for i, j _over a.fast_keys_and_elements()\n" +
        "_loop\n" +
        "_endloop";
    MagikVisitorContext context = createContext(code);
    ScopeBuilderVisitor visitor = new ScopeBuilderVisitor();
    visitor.scanFile(context);

    Scope globalScope = visitor.getGlobalScope();
    Scope loopScope = globalScope.getSelfAndDescendantScopes().get(1);
    ScopeEntry entryI = loopScope.getScopeEntry("i");
    assertThat(entryI).isNotNull();
    assertThat(entryI.getType() == ScopeEntry.Type.DEFINITION);

    ScopeEntry entryJ = loopScope.getScopeEntry("j");
    assertThat(entryJ).isNotNull();
    assertThat(entryJ.getType() == ScopeEntry.Type.DEFINITION);
  }

  @Test
  public void testParameter() {
    String code =
        "_method object.m(a, _optional b)\n" +
        "_endmethod";
    MagikVisitorContext context = createContext(code);
    ScopeBuilderVisitor visitor = new ScopeBuilderVisitor();
    visitor.scanFile(context);

    Scope globalScope = visitor.getGlobalScope();
    Scope methodScope = globalScope.getSelfAndDescendantScopes().get(1);

    ScopeEntry entryA = methodScope.getScopeEntry("a");
    assertThat(entryA).isNotNull();
    assertThat(entryA.getType() == ScopeEntry.Type.PARAMETER);

    ScopeEntry entryB = methodScope.getScopeEntry("b");
    assertThat(entryB).isNotNull();
    assertThat(entryB.getType() == ScopeEntry.Type.PARAMETER);
  }

  @Test
  public void testParameterAssignment() {
    String code =
        "_method object.m << a\n" +
        "_endmethod";
    MagikVisitorContext context = createContext(code);
    ScopeBuilderVisitor visitor = new ScopeBuilderVisitor();
    visitor.scanFile(context);

    Scope globalScope = visitor.getGlobalScope();
    Scope methodScope = globalScope.getSelfAndDescendantScopes().get(1);

    ScopeEntry entryA = methodScope.getScopeEntry("a");
    assertThat(entryA).isNotNull();
    assertThat(entryA.getType() == ScopeEntry.Type.PARAMETER);
  }

  @Test
  public void testUndeclaredGlobal() {
    String code =
        "_method a.b\n" +
        "\t_return !current_grs! _is _unset\n" +
        "_endmethod";
    MagikVisitorContext context = createContext(code);
    ScopeBuilderVisitor visitor = new ScopeBuilderVisitor();
    visitor.scanFile(context);

    Scope globalScope = visitor.getGlobalScope();
    Scope methodScope = globalScope.getSelfAndDescendantScopes().get(1);
    ScopeEntry entryCurrentGrs = methodScope.getScopeEntry("!current_grs!");
    assertThat(entryCurrentGrs).isNotNull();
    assertThat(entryCurrentGrs.getType() == ScopeEntry.Type.GLOBAL);
  }

}
