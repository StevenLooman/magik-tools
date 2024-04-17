package nl.ramsolutions.sw.magik.analysis.scope;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import nl.ramsolutions.sw.magik.MagikFile;
import org.junit.jupiter.api.Test;

/** Test ScopeBuilderVisitor. */
@SuppressWarnings("checkstyle:MagicNumber")
class ScopeBuilderVisitorTest {

  private static final URI DEFAULT_URI = URI.create("memory://source.magik");

  private ScopeBuilderVisitor buildCode(String code) {
    final MagikFile magikFile = new MagikFile(DEFAULT_URI, code);
    final ScopeBuilderVisitor visitor = new ScopeBuilderVisitor();
    visitor.scanFile(magikFile);
    return visitor;
  }

  @Test
  void testLocal() {
    final String code =
        """
        _method object.m
            _local a
        _endmethod""";
    final ScopeBuilderVisitor visitor = this.buildCode(code);

    final Scope globalScope = visitor.getGlobalScope();
    final Scope methodScope = globalScope.getSelfAndDescendantScopes().get(1);

    final ScopeEntry entryA = methodScope.getScopeEntry("a");
    assertThat(entryA).isNotNull();
    assertThat(entryA.getType()).isEqualTo(ScopeEntry.Type.LOCAL);
  }

  @Test
  void testLocalSerial() {
    final String code =
        """
        _method object.m
            _local a, b
        _endmethod""";
    final ScopeBuilderVisitor visitor = this.buildCode(code);

    final Scope globalScope = visitor.getGlobalScope();
    final Scope methodScope = globalScope.getSelfAndDescendantScopes().get(1);

    final ScopeEntry entryA = methodScope.getScopeEntry("a");
    assertThat(entryA).isNotNull();
    assertThat(entryA.getType()).isEqualTo(ScopeEntry.Type.LOCAL);

    final ScopeEntry entryB = methodScope.getScopeEntry("b");
    assertThat(entryB).isNotNull();
    assertThat(entryB.getType()).isEqualTo(ScopeEntry.Type.LOCAL);
  }

  @Test
  void testDefinitionSerialAssigment() {
    final String code =
        """
        _method a.b()
            l_a << l_b << x.y
            show(l_a, l_b)
        _endmethod""";
    final ScopeBuilderVisitor visitor = this.buildCode(code);

    final Scope globalScope = visitor.getGlobalScope();
    final Scope methodScope = globalScope.getSelfAndDescendantScopes().get(1);

    final ScopeEntry entryA = methodScope.getScopeEntry("l_a");
    assertThat(entryA).isNotNull();
    assertThat(entryA.getType()).isEqualTo(ScopeEntry.Type.DEFINITION);

    final ScopeEntry entryB = methodScope.getScopeEntry("l_b");
    assertThat(entryB).isNotNull();
    assertThat(entryB.getType()).isEqualTo(ScopeEntry.Type.DEFINITION);
  }

  @Test
  void testDefinitionMixed() {
    final String code =
        """
        _method object.m
            _local a << b << 10
        _endmethod""";
    final ScopeBuilderVisitor visitor = this.buildCode(code);

    final Scope globalScope = visitor.getGlobalScope();
    final Scope methodScope = globalScope.getSelfAndDescendantScopes().get(1);

    final ScopeEntry entryA = methodScope.getScopeEntry("a");
    assertThat(entryA).isNotNull();
    assertThat(entryA.getType()).isEqualTo(ScopeEntry.Type.LOCAL);

    final ScopeEntry entryB = methodScope.getScopeEntry("b");
    assertThat(entryB).isNotNull();
    assertThat(entryB.getType()).isEqualTo(ScopeEntry.Type.DEFINITION);
  }

  @Test
  void testDefinitionMultiple() {
    final String code =
        """
        _method object.m
            _local (a, b) << (1, 2)
        _endmethod""";
    final ScopeBuilderVisitor visitor = this.buildCode(code);

    final Scope globalScope = visitor.getGlobalScope();
    final Scope methodScope = globalScope.getSelfAndDescendantScopes().get(1);

    final ScopeEntry entryA = methodScope.getScopeEntry("a");
    assertThat(entryA).isNotNull();
    assertThat(entryA.getType()).isEqualTo(ScopeEntry.Type.LOCAL);

    final ScopeEntry entryB = methodScope.getScopeEntry("b");
    assertThat(entryB).isNotNull();
    assertThat(entryB.getType()).isEqualTo(ScopeEntry.Type.LOCAL);
  }

  @Test
  void testDefinitionAssignment() {
    final String code =
        """
        _method object.m
            _local a << b << _unset
        _endmethod""";
    final ScopeBuilderVisitor visitor = this.buildCode(code);

    final Scope globalScope = visitor.getGlobalScope();
    final Scope methodScope = globalScope.getSelfAndDescendantScopes().get(1);

    final ScopeEntry entryA = methodScope.getScopeEntry("a");
    assertThat(entryA).isNotNull();
    assertThat(entryA.getType()).isEqualTo(ScopeEntry.Type.LOCAL);

    final ScopeEntry entryB = methodScope.getScopeEntry("b");
    assertThat(entryB).isNotNull();
    assertThat(entryB.getType()).isEqualTo(ScopeEntry.Type.DEFINITION);
  }

  @Test
  void testAssignment() {
    final String code =
        """
        _method object.m
            a << 1
        _endmethod""";
    final ScopeBuilderVisitor visitor = this.buildCode(code);

    final Scope globalScope = visitor.getGlobalScope();
    final Scope methodScope = globalScope.getSelfAndDescendantScopes().get(1);

    final ScopeEntry entryA = methodScope.getScopeEntry("a");
    assertThat(entryA).isNotNull();
    assertThat(entryA.getType()).isEqualTo(ScopeEntry.Type.DEFINITION);
  }

  @Test
  void testAssignmentPackage() {
    final String code =
        """
        _method object.m
            sw:a << 1
        _endmethod""";
    final ScopeBuilderVisitor visitor = this.buildCode(code);

    final Scope globalScope = visitor.getGlobalScope();
    final Scope methodScope = globalScope.getSelfAndDescendantScopes().get(1);

    final ScopeEntry entryA = methodScope.getScopeEntry("a");
    assertThat(entryA).isNotNull();
    assertThat(entryA.getType()).isEqualTo(ScopeEntry.Type.GLOBAL);
  }

  @Test
  void testMultipleAssignment() {
    final String code =
        """
        _method object.m
            (a, b) << (1, 2)
        _endmethod""";
    final ScopeBuilderVisitor visitor = this.buildCode(code);

    final Scope globalScope = visitor.getGlobalScope();
    final Scope methodScope = globalScope.getSelfAndDescendantScopes().get(1);

    final ScopeEntry entryA = methodScope.getScopeEntry("a");
    assertThat(entryA).isNotNull();
    assertThat(entryA.getType()).isEqualTo(ScopeEntry.Type.DEFINITION);

    final ScopeEntry entryB = methodScope.getScopeEntry("b");
    assertThat(entryB).isNotNull();
    assertThat(entryB.getType()).isEqualTo(ScopeEntry.Type.DEFINITION);
  }

  @Test
  void testMultipleAssignmentPackage() {
    final String code =
        """
        _method object.m
            (sw:a, b) << (1, 2)
        _endmethod""";
    final ScopeBuilderVisitor visitor = this.buildCode(code);

    final Scope globalScope = visitor.getGlobalScope();
    final Scope methodScope = globalScope.getSelfAndDescendantScopes().get(1);

    final ScopeEntry entryA = methodScope.getScopeEntry("a");
    assertThat(entryA).isNotNull();
    assertThat(entryA.getType()).isEqualTo(ScopeEntry.Type.GLOBAL);

    final ScopeEntry entryB = methodScope.getScopeEntry("b");
    assertThat(entryB).isNotNull();
    assertThat(entryB.getType()).isEqualTo(ScopeEntry.Type.DEFINITION);
  }

  @Test
  void testTry() {
    final String code =
        """
        _try
            _local b
        _when error
        _endtry""";
    final ScopeBuilderVisitor visitor = this.buildCode(code);

    final Scope globalScope = visitor.getGlobalScope();
    final Scope tryScope = globalScope.getSelfAndDescendantScopes().get(1);
    final ScopeEntry entryTryB = tryScope.getScopeEntry("b");
    assertThat(entryTryB).isNotNull();
    assertThat(entryTryB.getType()).isEqualTo(ScopeEntry.Type.LOCAL);

    final Scope whenScope = globalScope.getSelfAndDescendantScopes().get(2);
    final ScopeEntry entryWhenB = whenScope.getScopeEntry("b");
    assertThat(entryWhenB).isNull();
  }

  @Test
  void testTryWith() {
    final String code =
        """
        _try _with a
            _local b
        _when error
        _endtry""";
    final ScopeBuilderVisitor visitor = this.buildCode(code);

    final Scope globalScope = visitor.getGlobalScope();
    final Scope tryScope = globalScope.getSelfAndDescendantScopes().get(1);
    final ScopeEntry entryTryA = tryScope.getScopeEntry("a");
    assertThat(entryTryA).isNull();
    final ScopeEntry entryTryB = tryScope.getScopeEntry("b");
    assertThat(entryTryB).isNotNull();
    assertThat(entryTryB.getType()).isEqualTo(ScopeEntry.Type.LOCAL);

    final Scope whenScope = globalScope.getSelfAndDescendantScopes().get(2);
    final ScopeEntry entryWhenA = whenScope.getScopeEntry("a");
    assertThat(entryWhenA).isNotNull();
    assertThat(entryWhenA.getType()).isEqualTo(ScopeEntry.Type.LOCAL);
    final ScopeEntry entryWhenB = whenScope.getScopeEntry("b");
    assertThat(entryWhenB).isNull();
  }

  @Test
  void testForLoop() {
    final String code =
        """
        _method a.b
            _for i, j _over a.fast_keys_and_elements()
            _loop
            _endloop
        _endmethod""";
    final ScopeBuilderVisitor visitor = this.buildCode(code);

    final Scope globalScope = visitor.getGlobalScope();
    final ScopeEntry entryGlobalI = globalScope.getScopeEntry("i");
    assertThat(entryGlobalI).isNull();
    final ScopeEntry entryGlobalJ = globalScope.getScopeEntry("j");
    assertThat(entryGlobalJ).isNull();

    final Scope methodScope = globalScope.getSelfAndDescendantScopes().get(1);
    final ScopeEntry entryMethodI = methodScope.getScopeEntry("i");
    assertThat(entryMethodI).isNull();
    final ScopeEntry entryMethodJ = methodScope.getScopeEntry("j");
    assertThat(entryMethodJ).isNull();

    final Scope loopScope = globalScope.getSelfAndDescendantScopes().get(2);
    final ScopeEntry entryI = loopScope.getScopeEntry("i");
    assertThat(entryI).isNotNull();
    assertThat(entryI.getType()).isEqualTo(ScopeEntry.Type.LOCAL);

    final ScopeEntry entryJ = loopScope.getScopeEntry("j");
    assertThat(entryJ).isNotNull();
    assertThat(entryJ.getType()).isEqualTo(ScopeEntry.Type.LOCAL);
  }

  @Test
  void testParameter() {
    final String code =
        """
        _method object.m(a, _optional b)
        _endmethod""";
    final ScopeBuilderVisitor visitor = this.buildCode(code);

    final Scope globalScope = visitor.getGlobalScope();
    final Scope methodScope = globalScope.getSelfAndDescendantScopes().get(1);

    final ScopeEntry entryA = methodScope.getScopeEntry("a");
    assertThat(entryA).isNotNull();
    assertThat(entryA.getType()).isEqualTo(ScopeEntry.Type.PARAMETER);

    final ScopeEntry entryB = methodScope.getScopeEntry("b");
    assertThat(entryB).isNotNull();
    assertThat(entryB.getType()).isEqualTo(ScopeEntry.Type.PARAMETER);
  }

  @Test
  void testParameterIndexer() {
    final String code =
        """
        _method object[a, b]
        _endmethod""";
    final ScopeBuilderVisitor visitor = this.buildCode(code);

    final Scope globalScope = visitor.getGlobalScope();
    final Scope methodScope = globalScope.getSelfAndDescendantScopes().get(1);

    final ScopeEntry entryA = methodScope.getScopeEntry("a");
    assertThat(entryA).isNotNull();
    assertThat(entryA.getType()).isEqualTo(ScopeEntry.Type.PARAMETER);

    final ScopeEntry entryB = methodScope.getScopeEntry("b");
    assertThat(entryB).isNotNull();
    assertThat(entryB.getType()).isEqualTo(ScopeEntry.Type.PARAMETER);
  }

  @Test
  void testParameterAssignment() {
    final String code =
        """
        _method object.m << a
        _endmethod""";
    final ScopeBuilderVisitor visitor = this.buildCode(code);

    final Scope globalScope = visitor.getGlobalScope();
    final Scope methodScope = globalScope.getSelfAndDescendantScopes().get(1);

    final ScopeEntry entryA = methodScope.getScopeEntry("a");
    assertThat(entryA).isNotNull();
    assertThat(entryA.getType()).isEqualTo(ScopeEntry.Type.PARAMETER);
  }

  @Test
  void testUndeclaredGlobal() {
    final String code =
        """
        _method a.b
            _return !current_grs! _is _unset
        _endmethod""";
    final ScopeBuilderVisitor visitor = this.buildCode(code);

    final Scope globalScope = visitor.getGlobalScope();
    final Scope methodScope = globalScope.getSelfAndDescendantScopes().get(1);
    final ScopeEntry entryCurrentGrs = methodScope.getScopeEntry("!current_grs!");
    assertThat(entryCurrentGrs).isNotNull();
    assertThat(entryCurrentGrs.getType()).isEqualTo(ScopeEntry.Type.GLOBAL);
  }

  @Test
  void testUsage() {
    final String code =
        """
        _method a.b
            _local a << 10
            show(a)
        _endmethod""";
    final ScopeBuilderVisitor visitor = this.buildCode(code);

    final Scope globalScope = visitor.getGlobalScope();
    final Scope methodScope = globalScope.getSelfAndDescendantScopes().get(1);
    final ScopeEntry entryA = methodScope.getScopeEntry("a");
    assertThat(entryA).isNotNull();
    assertThat(entryA.getType()).isEqualTo(ScopeEntry.Type.LOCAL);
    assertThat(entryA.getDefinitionNode().getTokenLine()).isEqualTo(2);
    assertThat(entryA.getUsages()).hasSize(1);
    assertThat(entryA.getUsages().get(0).getTokenLine()).isEqualTo(3);
  }

  @Test
  void testUsageMethodAssignment() {
    final String code =
        """
        _block
            _local a
            a.b << 10
        _endblock""";
    final ScopeBuilderVisitor visitor = this.buildCode(code);

    final Scope globalScope = visitor.getGlobalScope();
    final Scope methodScope = globalScope.getSelfAndDescendantScopes().get(1);

    final ScopeEntry entryA = methodScope.getScopeEntry("a");
    assertThat(entryA).isNotNull();
    assertThat(entryA.getType()).isEqualTo(ScopeEntry.Type.LOCAL);
    assertThat(entryA.getDefinitionNode().getTokenLine()).isEqualTo(2);
    assertThat(entryA.getUsages()).hasSize(1);
    assertThat(entryA.getUsages().get(0).getTokenLine()).isEqualTo(3);

    final ScopeEntry entryB = methodScope.getScopeEntry("b");
    assertThat(entryB).isNull();
  }

  @Test
  void testImportLocal() {
    final String code =
        """
        _method a.b
            _local a
            _proc()
                _import a
            _endproc
        _endmethod""";
    final ScopeBuilderVisitor visitor = this.buildCode(code);

    final Scope globalScope = visitor.getGlobalScope();
    final Scope methodScope = globalScope.getSelfAndDescendantScopes().get(1);
    final Scope procScope = methodScope.getSelfAndDescendantScopes().get(1);

    final ScopeEntry entryA = procScope.getScopeEntry("a");
    assertThat(entryA).isNotNull();
    assertThat(entryA.getType()).isEqualTo(ScopeEntry.Type.IMPORT);
    assertThat(entryA.getImportedEntry()).isNotNull();
    assertThat(entryA.getDefinitionNode().getTokenLine()).isEqualTo(4);
    assertThat(entryA.getUsages()).isEmpty();
  }

  @Test
  void testImportDefined() {
    final String code =
        """
        _method a.b
            a << 1
            _proc()
                _import a
            _endproc
        _endmethod""";
    final ScopeBuilderVisitor visitor = this.buildCode(code);

    final Scope globalScope = visitor.getGlobalScope();
    final Scope methodScope = globalScope.getSelfAndDescendantScopes().get(1);
    final Scope procScope = methodScope.getSelfAndDescendantScopes().get(1);

    final ScopeEntry entryA = procScope.getScopeEntry("a");
    assertThat(entryA).isNotNull();
    assertThat(entryA.getType()).isEqualTo(ScopeEntry.Type.IMPORT);
    assertThat(entryA.getImportedEntry()).isNull();
    assertThat(entryA.getDefinitionNode().getTokenLine()).isEqualTo(4);
    assertThat(entryA.getUsages()).isEmpty();
  }

  @Test
  void testTopLevelProcImport() {
    final String code =
        """
        _proc()
          _import !traceback_show_args?!
        _endproc""";
    final ScopeBuilderVisitor visitor = this.buildCode(code);
    final Scope globalScope = visitor.getGlobalScope();
    final Scope procScope = globalScope.getSelfAndDescendantScopes().get(1);

    final ScopeEntry entryTracebackShowArgs = procScope.getScopeEntry("!traceback_show_args?!");
    assertThat(entryTracebackShowArgs.getType()).isEqualTo(ScopeEntry.Type.IMPORT);
    assertThat(entryTracebackShowArgs.getImportedEntry()).isNull();
    assertThat(entryTracebackShowArgs.getDefinitionNode().getTokenLine()).isEqualTo(2);
    assertThat(entryTracebackShowArgs.getUsages()).isEmpty();
  }

  @Test
  void testHidingScopeEntryLocal() {
    final String code =
        """
        _method a.b
          _local x << 10
          _block
            _local x << 10
            show(x)
          _endblock
        _endmethod""";
    final ScopeBuilderVisitor visitor = this.buildCode(code);
    final Scope globalScope = visitor.getGlobalScope();
    final Scope bodyScope = globalScope.getSelfAndDescendantScopes().get(1);
    final Scope loopScope = globalScope.getSelfAndDescendantScopes().get(2);

    final ScopeEntry bodyXEntry = bodyScope.getScopeEntry("x");
    final ScopeEntry loopXEntry = loopScope.getScopeEntry("x");
    assertThat(bodyXEntry).isNotSameAs(loopXEntry);
  }

  @Test
  void testNotHidingScopeEntryDefinition() {
    final String code =
        """
        _method a.b
          x << 10
          _block
            x << 10
            show(x)
          _endblock
        _endmethod""";
    final ScopeBuilderVisitor visitor = this.buildCode(code);
    final Scope globalScope = visitor.getGlobalScope();
    final Scope bodyScope = globalScope.getSelfAndDescendantScopes().get(1);
    final Scope loopScope = globalScope.getSelfAndDescendantScopes().get(2);

    final ScopeEntry bodyXEntry = bodyScope.getScopeEntry("x");
    final ScopeEntry loopXEntry = loopScope.getScopeEntry("x");
    assertThat(bodyXEntry).isEqualTo(loopXEntry);
  }

  @Test
  void testNotHidingScopeEntryDefinitionMulti() {
    final String code =
        """
        _method a.b
          (x, y) << (10, 20)
          _block
            x << 10
            show(x)
          _endblock
        _endmethod""";
    final ScopeBuilderVisitor visitor = this.buildCode(code);
    final Scope globalScope = visitor.getGlobalScope();
    final Scope bodyScope = globalScope.getSelfAndDescendantScopes().get(1);
    final Scope loopScope = globalScope.getSelfAndDescendantScopes().get(2);

    final ScopeEntry bodyXEntry = bodyScope.getScopeEntry("x");
    final ScopeEntry loopXEntry = loopScope.getScopeEntry("x");
    assertThat(bodyXEntry).isEqualTo(loopXEntry);
  }
}
