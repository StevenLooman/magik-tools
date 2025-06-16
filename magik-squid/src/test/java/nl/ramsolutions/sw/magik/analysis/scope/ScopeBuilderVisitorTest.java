package nl.ramsolutions.sw.magik.analysis.scope;

import static org.assertj.core.api.Assertions.assertThat;

import nl.ramsolutions.sw.magik.MagikFile;
import org.junit.jupiter.api.Test;

/** Test ScopeBuilderVisitor. */
@SuppressWarnings("checkstyle:MagicNumber")
class ScopeBuilderVisitorTest {

  private ScopeBuilderVisitor buildCode(String code) {
    final MagikFile magikFile = new MagikFile(MagikFile.DEFAULT_URI, code);
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
    assertThat(entryA).isEqualTo(new ScopeEntry(ScopeEntry.Type.LOCAL, "a", null, null));
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
    assertThat(entryA).isEqualTo(new ScopeEntry(ScopeEntry.Type.LOCAL, "a", null, null));

    final ScopeEntry entryB = methodScope.getScopeEntry("b");
    assertThat(entryB).isEqualTo(new ScopeEntry(ScopeEntry.Type.LOCAL, "b", null, null));
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
    assertThat(entryA).isEqualTo(new ScopeEntry(ScopeEntry.Type.DEFINITION, "l_a", null, null));

    final ScopeEntry entryB = methodScope.getScopeEntry("l_b");
    assertThat(entryB).isEqualTo(new ScopeEntry(ScopeEntry.Type.DEFINITION, "l_b", null, null));
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
    assertThat(entryA).isEqualTo(new ScopeEntry(ScopeEntry.Type.LOCAL, "a", null, null));

    final ScopeEntry entryB = methodScope.getScopeEntry("b");
    assertThat(entryB).isEqualTo(new ScopeEntry(ScopeEntry.Type.DEFINITION, "b", null, null));
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
    assertThat(entryA).isEqualTo(new ScopeEntry(ScopeEntry.Type.LOCAL, "a", null, null));

    final ScopeEntry entryB = methodScope.getScopeEntry("b");
    assertThat(entryB).isEqualTo(new ScopeEntry(ScopeEntry.Type.LOCAL, "b", null, null));
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
    assertThat(entryA).isEqualTo(new ScopeEntry(ScopeEntry.Type.LOCAL, "a", null, null));

    final ScopeEntry entryB = methodScope.getScopeEntry("b");
    assertThat(entryB).isEqualTo(new ScopeEntry(ScopeEntry.Type.DEFINITION, "b", null, null));
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
    assertThat(entryA).isEqualTo(new ScopeEntry(ScopeEntry.Type.DEFINITION, "a", null, null));
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
    assertThat(entryA).isEqualTo(new ScopeEntry(ScopeEntry.Type.GLOBAL, "a", null, null));
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
    assertThat(entryA).isEqualTo(new ScopeEntry(ScopeEntry.Type.DEFINITION, "a", null, null));

    final ScopeEntry entryB = methodScope.getScopeEntry("b");
    assertThat(entryB).isEqualTo(new ScopeEntry(ScopeEntry.Type.DEFINITION, "b", null, null));
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
    assertThat(entryA).isEqualTo(new ScopeEntry(ScopeEntry.Type.GLOBAL, "a", null, null));

    final ScopeEntry entryB = methodScope.getScopeEntry("b");
    assertThat(entryB).isEqualTo(new ScopeEntry(ScopeEntry.Type.DEFINITION, "b", null, null));
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
    assertThat(entryTryB).isEqualTo(new ScopeEntry(ScopeEntry.Type.LOCAL, "b", null, null));

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
    assertThat(entryTryB).isEqualTo(new ScopeEntry(ScopeEntry.Type.LOCAL, "b", null, null));

    final Scope whenScope = globalScope.getSelfAndDescendantScopes().get(2);
    final ScopeEntry entryWhenA = whenScope.getScopeEntry("a");
    assertThat(entryWhenA).isEqualTo(new ScopeEntry(ScopeEntry.Type.LOCAL, "a", null, null));
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
    assertThat(entryI).isEqualTo(new ScopeEntry(ScopeEntry.Type.LOCAL, "i", null, null));

    final ScopeEntry entryJ = loopScope.getScopeEntry("j");
    assertThat(entryJ).isEqualTo(new ScopeEntry(ScopeEntry.Type.LOCAL, "j", null, null));
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
    assertThat(entryA).isEqualTo(new ScopeEntry(ScopeEntry.Type.PARAMETER, "a", null, null));

    final ScopeEntry entryB = methodScope.getScopeEntry("b");
    assertThat(entryB).isEqualTo(new ScopeEntry(ScopeEntry.Type.PARAMETER, "b", null, null));
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
    assertThat(entryA).isEqualTo(new ScopeEntry(ScopeEntry.Type.PARAMETER, "a", null, null));

    final ScopeEntry entryB = methodScope.getScopeEntry("b");
    assertThat(entryB).isEqualTo(new ScopeEntry(ScopeEntry.Type.PARAMETER, "b", null, null));
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
    assertThat(entryA).isEqualTo(new ScopeEntry(ScopeEntry.Type.PARAMETER, "a", null, null));
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
    assertThat(entryCurrentGrs)
        .isEqualTo(new ScopeEntry(ScopeEntry.Type.GLOBAL, "!current_grs!", null, null));
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
    assertThat(entryA).isEqualTo(new ScopeEntry(ScopeEntry.Type.LOCAL, "a", null, null));
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
    assertThat(entryA).isEqualTo(new ScopeEntry(ScopeEntry.Type.LOCAL, "a", null, null));
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
    assertThat(entryA)
        .isEqualTo(
            new ScopeEntry(
                ScopeEntry.Type.IMPORT,
                "a",
                null,
                methodScope.getScopeEntry("a") // Imported from the method scope
                ));
    assertThat(entryA.getDefinitionNode().getTokenLine()).isEqualTo(4);
    assertThat(entryA.getUsages()).isEmpty();
  }

  @Test
  void testImportLocal2() {
    final String code =
        """
        _method a.b
            _local a
            _proc@proc_1()
                _proc@proc_2()
                    _import a
                _endproc
            _endproc
        _endmethod""";
    final ScopeBuilderVisitor visitor = this.buildCode(code);

    final Scope globalScope = visitor.getGlobalScope();
    final Scope methodScope = globalScope.getSelfAndDescendantScopes().get(1);
    final Scope proc2Scope = methodScope.getSelfAndDescendantScopes().get(2);

    final ScopeEntry entryA = proc2Scope.getScopeEntry("a");
    assertThat(entryA)
        .isEqualTo(
            new ScopeEntry(
                ScopeEntry.Type.IMPORT,
                "a",
                null,
                methodScope.getScopeEntry("a") // Imported from the method scope
                ));
    assertThat(entryA.getDefinitionNode().getTokenLine()).isEqualTo(5);
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
    assertThat(entryA)
        .isEqualTo(
            new ScopeEntry(
                ScopeEntry.Type.IMPORT,
                "a",
                null,
                null // Not imported from the method scope, as it is not local
                ));
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
    assertThat(entryTracebackShowArgs)
        .isEqualTo(
            new ScopeEntry(
                ScopeEntry.Type.IMPORT,
                "!traceback_show_args?!",
                null,
                null // Not imported from the global scope, as it is not local
                ));
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

    final ScopeEntry bodyEntry = bodyScope.getScopeEntry("x");
    final ScopeEntry loopEntry = loopScope.getScopeEntry("x");
    assertThat(bodyEntry).isNotSameAs(loopEntry);
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

    final ScopeEntry bodyEntry = bodyScope.getScopeEntry("x");
    final ScopeEntry loopEntry = loopScope.getScopeEntry("x");
    assertThat(bodyEntry).isEqualTo(loopEntry);
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

    final ScopeEntry bodyEntry = bodyScope.getScopeEntry("x");
    final ScopeEntry loopEntry = loopScope.getScopeEntry("x");
    assertThat(bodyEntry).isEqualTo(loopEntry);
  }

  @Test
  void testGlobalDefinition() {
    final String code =
        """
        x << 10
        """;
    final ScopeBuilderVisitor visitor = this.buildCode(code);
    final Scope globalScope = visitor.getGlobalScope();

    final ScopeEntry entry = globalScope.getScopeEntry("x");
    assertThat(entry).isEqualTo(new ScopeEntry(ScopeEntry.Type.GLOBAL, "x", null, null));
  }
}
