package nl.ramsolutions.sw.magik.analysis.scope;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.List;
import nl.ramsolutions.sw.magik.MagikFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test ScopeBuilderVisitor. */
@SuppressWarnings("checkstyle:MagicNumber")
class ScopeBuilderVisitorTest {

  private ScopeBuilderVisitor buildCode(String code) {
    final MagikFile magikFile = new MagikFile(MagikFile.DEFAULT_URI, code);
    final ScopeBuilderVisitor visitor = new ScopeBuilderVisitor();
    visitor.scanFile(magikFile);
    return visitor;
  }

  private void assertScopeEntry(
      final ScopeEntry scopeEntry,
      final ScopeEntry.Type type,
      final String identifier,
      @Nullable final ScopeEntry importedEntry) {
    assertThat(scopeEntry).isNotNull();

    final ScopeEntry.Type entryType = scopeEntry.getType();
    assertThat(entryType).isEqualTo(type);

    final String entryIdentifier = scopeEntry.getIdentifier();
    assertThat(entryIdentifier).isEqualTo(identifier);

    final ScopeEntry entryImportedEntry = scopeEntry.getImportedEntry();
    assertThat(entryImportedEntry).isSameAs(importedEntry);
  }

  private List<Integer> getUsageLines(final ScopeEntry scopeEntry) {
    final List<AstNode> usageNodes = scopeEntry.getUsages();
    return usageNodes.stream().map(AstNode::getTokenLine).toList();
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
  void testDefinitionSerialAssigmentKnownFirstTarget() {
    final String code =
        """
        _method a.b()
            l_a << 1
            l_a << l_b << x.y
        _endmethod""";
    final ScopeBuilderVisitor visitor = this.buildCode(code);

    final Scope globalScope = visitor.getGlobalScope();
    final Scope methodScope = globalScope.getSelfAndDescendantScopes().get(1);

    final ScopeEntry entryB = methodScope.getScopeEntry("l_b");
    assertThat(entryB).isEqualTo(new ScopeEntry(ScopeEntry.Type.DEFINITION, "l_b", null, null));
  }

  @Test
  void testDefinitionSerialAssigmentLocalFirstTarget() {
    final String code =
        """
        _method a.b()
            _local l_a
            l_a << l_b << x.y
        _endmethod""";
    final ScopeBuilderVisitor visitor = this.buildCode(code);

    final Scope globalScope = visitor.getGlobalScope();
    final Scope methodScope = globalScope.getSelfAndDescendantScopes().get(1);

    final ScopeEntry entryB = methodScope.getScopeEntry("l_b");
    assertThat(entryB).isEqualTo(new ScopeEntry(ScopeEntry.Type.DEFINITION, "l_b", null, null));
  }

  @Test
  void testDefinitionSerialAssigmentKnownFirstTargetInBlock() {
    final String code =
        """
        _method a.b()
            l_a << 1
            _block
                l_a << l_b << x.y
            _endblock
        _endmethod""";
    final ScopeBuilderVisitor visitor = this.buildCode(code);

    final Scope globalScope = visitor.getGlobalScope();
    final Scope methodScope = globalScope.getSelfAndDescendantScopes().get(1);
    final Scope blockScope = globalScope.getSelfAndDescendantScopes().get(2);

    // A DEFINITION is hoisted to the procedure scope, a GLOBAL would sit on the block scope.
    final ScopeEntry entryB = methodScope.getScopeEntry("l_b");
    assertThat(entryB).isEqualTo(new ScopeEntry(ScopeEntry.Type.DEFINITION, "l_b", null, null));

    final ScopeEntry blockEntryB = blockScope.getLocalScopeEntry("l_b");
    assertThat(blockEntryB).isNull();
  }

  @Test
  void testDefinitionSerialAssigmentPackageSecondTarget() {
    final String code =
        """
        _method a.b()
            l_a << 1
            l_a << sw:l_b << x.y
        _endmethod""";
    final ScopeBuilderVisitor visitor = this.buildCode(code);

    final Scope globalScope = visitor.getGlobalScope();
    final Scope methodScope = globalScope.getSelfAndDescendantScopes().get(1);

    // Same as when it is the first target, see testAssignmentPackage.
    final ScopeEntry entryB = methodScope.getScopeEntry("l_b");
    assertThat(entryB).isEqualTo(new ScopeEntry(ScopeEntry.Type.GLOBAL, "l_b", null, null));
  }

  @Test
  void testGlobalSerialAssigmentKnownFirstTarget() {
    final String code =
        """
        a << 1
        a << b << 2
        """;
    final ScopeBuilderVisitor visitor = this.buildCode(code);

    final Scope globalScope = visitor.getGlobalScope();

    // Same as when it is the first target, see testGlobalDefinition: a GLOBAL without a usage.
    final ScopeEntry entryB = globalScope.getScopeEntry("b");
    assertThat(entryB).isEqualTo(new ScopeEntry(ScopeEntry.Type.GLOBAL, "b", null, null));

    final List<AstNode> usagesB = entryB.getUsages();
    assertThat(usagesB).isEmpty();
  }

  @Test
  void testReadBeforeAssignmentIsALocalOfTheBody() {
    final String code =
        """
        _method a.b()
            show(l_z)
            l_z << 1
        _endmethod""";
    final ScopeBuilderVisitor visitor = this.buildCode(code);

    final Scope globalScope = visitor.getGlobalScope();
    final Scope methodScope = globalScope.getSelfAndDescendantScopes().get(1);

    // A name assigned anywhere in the body is a local of the body, read first or not.
    final ScopeEntry entryZ = methodScope.getScopeEntry("l_z");
    this.assertScopeEntry(entryZ, ScopeEntry.Type.DEFINITION, "l_z", null);
    final AstNode definitionNode = entryZ.getDefinitionNode();
    assertThat(definitionNode.getTokenLine()).isEqualTo(3);
    final List<Integer> usageLines = this.getUsageLines(entryZ);
    assertThat(usageLines).containsExactly(2);

    final ScopeEntry globalEntryZ = globalScope.getScopeEntry("l_z");
    assertThat(globalEntryZ).isNull();
  }

  @Test
  void testReadInABlockBeforeAnAssignmentInTheBodyIsOneLocal() {
    final String code =
        """
        _method a.b(p)
            _if p
            _then
                show(x)
            _endif
            x << 1
        _endmethod""";
    final ScopeBuilderVisitor visitor = this.buildCode(code);

    final Scope globalScope = visitor.getGlobalScope();
    final Scope methodScope = globalScope.getSelfAndDescendantScopes().get(1);
    final Scope ifScope = methodScope.getSelfAndDescendantScopes().get(1);

    final ScopeEntry methodEntryX = methodScope.getScopeEntry("x");
    this.assertScopeEntry(methodEntryX, ScopeEntry.Type.DEFINITION, "x", null);
    final ScopeEntry ifEntryX = ifScope.getScopeEntry("x");
    assertThat(ifEntryX).isSameAs(methodEntryX);
  }

  @Test
  void testAssignmentInAnArmMakesALocalOfTheBody() {
    final String code =
        """
        _method a.b(p)
            show(x)
            _if p
            _then
                _block
                    x << 1
                _endblock
            _endif
            _return x
        _endmethod""";
    final ScopeBuilderVisitor visitor = this.buildCode(code);

    final Scope globalScope = visitor.getGlobalScope();
    final Scope methodScope = globalScope.getSelfAndDescendantScopes().get(1);

    final ScopeEntry entryX = methodScope.getLocalScopeEntry("x");
    this.assertScopeEntry(entryX, ScopeEntry.Type.DEFINITION, "x", null);
    final AstNode definitionNode = entryX.getDefinitionNode();
    assertThat(definitionNode.getTokenLine()).isEqualTo(6);
    final List<Integer> usageLines = this.getUsageLines(entryX);
    assertThat(usageLines).containsExactly(2, 9);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "x +<< 1",
        "x ^<< 1",
        "x +^<< 1",
        "(x, y) << (1, 2)",
        "(_gather x) << (1, 2)",
        "y << x << 1",
      })
  void testEveryAssignmentFormMakesALocalOfTheBody(final String assignment) {
    final String code =
        """
        _method a.b(p)
            show(x)
            _if p
            _then
                %s
            _endif
        _endmethod"""
            .formatted(assignment);
    final ScopeBuilderVisitor visitor = this.buildCode(code);

    final Scope globalScope = visitor.getGlobalScope();
    final Scope methodScope = globalScope.getSelfAndDescendantScopes().get(1);

    final ScopeEntry entryX = methodScope.getLocalScopeEntry("x");
    this.assertScopeEntry(entryX, ScopeEntry.Type.DEFINITION, "x", null);
  }

  @Test
  void testAssignmentInAProcedureMakesALocalOfTheProcedure() {
    final String code =
        """
        _proc()
            show(x)
            x << 1
        _endproc""";
    final ScopeBuilderVisitor visitor = this.buildCode(code);

    final Scope globalScope = visitor.getGlobalScope();
    final Scope procScope = globalScope.getSelfAndDescendantScopes().get(1);

    final ScopeEntry entryX = procScope.getLocalScopeEntry("x");
    this.assertScopeEntry(entryX, ScopeEntry.Type.DEFINITION, "x", null);
  }

  @Test
  void testAssignmentInANestedProcedureIsALocalOfThatProcedureOnly() {
    final String code =
        """
        _method a.b()
            show(x)
            _proc()
                show(x)
                x << 1
            _endproc
        _endmethod""";
    final ScopeBuilderVisitor visitor = this.buildCode(code);

    final Scope globalScope = visitor.getGlobalScope();
    final Scope methodScope = globalScope.getSelfAndDescendantScopes().get(1);
    final Scope procScope = methodScope.getSelfAndDescendantScopes().get(1);

    final ScopeEntry methodEntryX = methodScope.getScopeEntry("x");
    this.assertScopeEntry(methodEntryX, ScopeEntry.Type.GLOBAL, "x", null);
    final ScopeEntry procEntryX = procScope.getScopeEntry("x");
    this.assertScopeEntry(procEntryX, ScopeEntry.Type.DEFINITION, "x", null);
    final List<Integer> usageLines = this.getUsageLines(procEntryX);
    assertThat(usageLines).containsExactly(4);
  }

  @Test
  void testReadInANestedProcedureOfALocalOfTheBodyIsAGlobal() {
    final String code =
        """
        _method a.b()
            x << 1
            _proc()
                show(x)
            _endproc
        _endmethod""";
    final ScopeBuilderVisitor visitor = this.buildCode(code);

    final Scope globalScope = visitor.getGlobalScope();
    final Scope methodScope = globalScope.getSelfAndDescendantScopes().get(1);
    final Scope procScope = methodScope.getSelfAndDescendantScopes().get(1);

    final ScopeEntry methodEntryX = methodScope.getScopeEntry("x");
    this.assertScopeEntry(methodEntryX, ScopeEntry.Type.DEFINITION, "x", null);
    final ScopeEntry procEntryX = procScope.getScopeEntry("x");
    this.assertScopeEntry(procEntryX, ScopeEntry.Type.GLOBAL, "x", null);
  }

  @ParameterizedTest
  @ValueSource(strings = {"_local", "_global", "_dynamic"})
  void testAssignmentToADeclaredNameMakesNoLocalOfTheBody(final String modifier) {
    final String code =
        """
        _method a.b()
            show(x)
            _block
                %s x
                x << 1
            _endblock
        _endmethod"""
            .formatted(modifier);
    final ScopeBuilderVisitor visitor = this.buildCode(code);

    final Scope globalScope = visitor.getGlobalScope();
    final Scope methodScope = globalScope.getSelfAndDescendantScopes().get(1);

    final ScopeEntry entryX = methodScope.getScopeEntry("x");
    this.assertScopeEntry(entryX, ScopeEntry.Type.GLOBAL, "x", null);
    final AstNode definitionNode = entryX.getDefinitionNode();
    assertThat(definitionNode.getTokenLine()).isEqualTo(2);
  }

  @Test
  void testAssignmentToALoopVariableMakesNoLocalOfTheBody() {
    final String code =
        """
        _method a.b(c)
            show(x)
            _for x _over c.fast_elements()
            _loop
                x << 1
            _endloop
        _endmethod""";
    final ScopeBuilderVisitor visitor = this.buildCode(code);

    final Scope globalScope = visitor.getGlobalScope();
    final Scope methodScope = globalScope.getSelfAndDescendantScopes().get(1);

    final ScopeEntry entryX = methodScope.getScopeEntry("x");
    this.assertScopeEntry(entryX, ScopeEntry.Type.GLOBAL, "x", null);
  }

  @Test
  void testAssignmentToAParameterMakesNoLocalOfTheBody() {
    final String code =
        """
        _method a.b(x)
            x << 1
        _endmethod""";
    final ScopeBuilderVisitor visitor = this.buildCode(code);

    final Scope globalScope = visitor.getGlobalScope();
    final Scope methodScope = globalScope.getSelfAndDescendantScopes().get(1);

    final ScopeEntry entryX = methodScope.getScopeEntry("x");
    this.assertScopeEntry(entryX, ScopeEntry.Type.PARAMETER, "x", null);
  }

  @Test
  void testFileLevelGlobalDoesNotKeepAnAssignedNameGlobal() {
    final String code =
        """
        _global x << 1
        _method a.b(p)
            show(x)
            _if p _then x << 2 _endif
        _endmethod""";
    final ScopeBuilderVisitor visitor = this.buildCode(code);

    final Scope globalScope = visitor.getGlobalScope();
    final Scope methodScope = globalScope.getSelfAndDescendantScopes().get(1);

    final ScopeEntry entryX = methodScope.getScopeEntry("x");
    this.assertScopeEntry(entryX, ScopeEntry.Type.DEFINITION, "x", null);
  }

  @Test
  void testReadBeforeAssignmentInATopLevelBlockStaysGlobal() {
    final String code =
        """
        _block
            show(x)
            x << 1
        _endblock""";
    final ScopeBuilderVisitor visitor = this.buildCode(code);

    final Scope globalScope = visitor.getGlobalScope();
    final Scope blockScope = globalScope.getSelfAndDescendantScopes().get(1);

    final ScopeEntry entryX = blockScope.getScopeEntry("x");
    this.assertScopeEntry(entryX, ScopeEntry.Type.GLOBAL, "x", null);
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
  void testMultipleAssignmentGather() {
    final String code =
        """
        _method a.b
            (a, _gather b) << (1, 2)
        _endmethod""";
    final ScopeBuilderVisitor visitor = this.buildCode(code);

    final Scope globalScope = visitor.getGlobalScope();
    final Scope methodScope = globalScope.getSelfAndDescendantScopes().get(1);
    final ScopeEntry entryA = methodScope.getScopeEntry("a");
    this.assertScopeEntry(entryA, ScopeEntry.Type.DEFINITION, "a", null);
    final ScopeEntry entryB = methodScope.getScopeEntry("b");
    this.assertScopeEntry(entryB, ScopeEntry.Type.DEFINITION, "b", null);
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
  void testFinallyWith() {
    final String code =
        """
        _method a.b
            _over a.fast_elements()
            _loop
            _finally _with i, _gather j
                show(i, j)
            _endloop
        _endmethod""";
    final ScopeBuilderVisitor visitor = this.buildCode(code);

    final GlobalScope globalScope = visitor.getGlobalScope();
    final List<Scope> scopes = globalScope.getSelfAndDescendantScopes();
    final Scope methodScope = scopes.get(1);
    final ScopeEntry entryMethodI = methodScope.getScopeEntry("i");
    assertThat(entryMethodI).isNull();

    final Scope finallyScope = scopes.get(3);
    final ScopeEntry entryI = finallyScope.getScopeEntry("i");
    this.assertScopeEntry(entryI, ScopeEntry.Type.LOCAL, "i", null);
    final ScopeEntry entryJ = finallyScope.getScopeEntry("j");
    this.assertScopeEntry(entryJ, ScopeEntry.Type.LOCAL, "j", null);

    final AstNode identifierNode = entryI.getDefinitionNode();
    final Scope identifierScope = globalScope.getScopeForNode(identifierNode);
    assertThat(identifierScope).isSameAs(finallyScope);
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

  @Test
  void testGlobalDefinitionInBlock() {
    final String code =
        """
        _block
          x << 10
        _endblock
        """;
    final ScopeBuilderVisitor visitor = this.buildCode(code);
    final Scope globalScope = visitor.getGlobalScope();

    final ScopeEntry entry = globalScope.getScopeEntry("x");
    assertThat(entry).isEqualTo(new ScopeEntry(ScopeEntry.Type.GLOBAL, "x", null, null));
  }
}
