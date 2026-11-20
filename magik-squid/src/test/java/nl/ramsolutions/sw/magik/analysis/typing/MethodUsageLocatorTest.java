package nl.ramsolutions.sw.magik.analysis.typing;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.analysis.SmallworldProjectExtension;
import nl.ramsolutions.sw.magik.analysis.definitions.GlobalDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodUsage;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;

/** Tests for {@link MethodUsageLocator}. */
class MethodUsageLocatorTest {

  @RegisterExtension
  final SmallworldProjectExtension smallworldProject = new SmallworldProjectExtension();

  @Test
  void testLocateMethodUsage() throws IOException {
    final String code =
        """
        def_slotted_exemplar(:a, {})

        _method a.method_name
          _self.method_name
        _endmethod
        """;
    final Path path = this.smallworldProject.pathOf("/source.magik");
    final MagikTypedFile magikFile = this.smallworldProject.addMagikFile(path, code);

    final IDefinitionKeeper definitionKeeper = this.smallworldProject.getDefinitionKeeper();
    final MethodUsageLocator methodUsageLocator = new MethodUsageLocator(definitionKeeper);
    final TypeString typeStr = TypeString.ofIdentifier("a", "user");
    final MethodUsage wantedMethodUsage = new MethodUsage(typeStr, "method_name");
    final List<Entry<MethodUsage, MagikTypedFile>> locatedMethodUsages =
        methodUsageLocator.getMethodUsages(wantedMethodUsage);

    assertThat(locatedMethodUsages).hasSize(1);
    final Entry<MethodUsage, MagikTypedFile> entry = locatedMethodUsages.get(0);
    final MethodUsage locatedMethodUsage = entry.getKey();
    assertThat(locatedMethodUsage).isEqualTo(new MethodUsage(typeStr, "method_name"));
    final MagikTypedFile locatedMagikFile = entry.getValue();
    assertThat(locatedMagikFile.getUri()).isEqualTo(magikFile.getUri());
  }

  @Test
  void testLocateMethodUsageOtherFile() throws IOException {
    final String codeA =
        """
        def_slotted_exemplar(:a, {})

        _method a.method_name
          _self.method_name
        _endmethod
        """;
    final Path pathA = this.smallworldProject.pathOf("/source_a.magik");
    final MagikTypedFile magikFileA = this.smallworldProject.addMagikFile(pathA, codeA);

    final String codeB =
        """
        _method b.method_name
          a.method_name
        _endmethod
        """;
    final Path pathB = this.smallworldProject.pathOf("/source_b.magik");
    final MagikTypedFile magikFileB = this.smallworldProject.addMagikFile(pathB, codeB);

    final IDefinitionKeeper definitionKeeper = this.smallworldProject.getDefinitionKeeper();
    final MethodUsageLocator methodUsageLocator = new MethodUsageLocator(definitionKeeper);
    final TypeString typeStr = TypeString.ofIdentifier("a", "user");
    final MethodUsage wantedMethodUsage = new MethodUsage(typeStr, "method_name");
    final List<Entry<MethodUsage, MagikTypedFile>> locatedMethodUsages =
        methodUsageLocator.getMethodUsages(wantedMethodUsage);

    assertThat(locatedMethodUsages).hasSize(2);

    // Returned order is random, so test as a set.
    final Set<MethodUsage> methodUsages =
        locatedMethodUsages.stream().map(entry -> entry.getKey()).collect(Collectors.toSet());
    assertThat(methodUsages).isEqualTo(Set.of(new MethodUsage(typeStr, "method_name")));

    final Set<URI> uris =
        locatedMethodUsages.stream()
            .map(entry -> entry.getValue().getUri())
            .collect(Collectors.toSet());
    assertThat(uris).isEqualTo(Set.of(magikFileA.getUri(), magikFileB.getUri()));
  }

  @Test
  void testInvokesMethodAnnotationIsNotCallSite() throws IOException {
    final String code =
        """
        def_slotted_exemplar(:a, {})

        _method a.method_name
        _endmethod

        _method a.caller(name)
          ## @invokes_method {user:a.method_name}
          _self.perform(name)
        _endmethod

        _method a.real_caller
          _self.method_name
        _endmethod
        """;
    this.addMagikFile(code);

    final List<Entry<MethodUsage, MagikTypedFile>> usages =
        this.locateMethodUsages("a", "method_name");

    assertThat(usages).hasSize(1);
    final Entry<MethodUsage, MagikTypedFile> entry = usages.get(0);
    final MethodUsage usage = entry.getKey();
    final Location location = usage.getLocation();
    final Range range = location.getRange();
    final Position startPosition = range.getStartPosition();
    final int line = startPosition.getLine();
    assertThat(line).isEqualTo(12);
  }

  @Test
  void testInvokesMethodAnnotationOnOtherReceiverIsNotCallSite() throws IOException {
    final String code =
        """
        def_slotted_exemplar(:a, {})
        def_slotted_exemplar(:b, {})

        _method a.method_name
        _endmethod

        _method b.caller(name)
          ## @invokes_method {user:b.method_name}
          _self.perform(name)
        _endmethod

        _method a.real_caller
          _self.method_name
        _endmethod
        """;
    this.addMagikFile(code);

    final List<Entry<MethodUsage, MagikTypedFile>> usages =
        this.locateMethodUsages("a", "method_name");

    assertThat(usages).hasSize(1);
  }

  @Test
  void testInvokesMethodAnnotationIsDeclaredUsage() throws IOException {
    final String code =
        """
        def_slotted_exemplar(:a, {})

        _method a.method_name
        _endmethod

        _method a.caller(name)
          ## @invokes_method {user:a.method_name}
          _self.perform(name)
        _endmethod

        _method a.real_caller
          _self.method_name
        _endmethod
        """;
    this.addMagikFile(code);
    final Set<MethodUsageLocator.Option> options =
        Set.of(MethodUsageLocator.Option.DECLARED_USAGES);

    final List<Entry<MethodUsage, MagikTypedFile>> usages =
        this.locateMethodUsages("a", "method_name", options);

    assertThat(usages).hasSize(2);
    final MethodUsage declaredUsage = this.getUsageAtLine(usages, 6);
    final boolean declaredIsCallSite = MethodUsageLocator.isCallSite(declaredUsage);
    assertThat(declaredIsCallSite).isFalse();
    final AstNode declaredNode = declaredUsage.getNode();
    final AstNodeType declaredNodeType = declaredNode.getType();
    assertThat(declaredNodeType).isEqualTo(MagikGrammar.METHOD_DEFINITION);
    final MethodUsage callUsage = this.getUsageAtLine(usages, 12);
    final boolean callIsCallSite = MethodUsageLocator.isCallSite(callUsage);
    assertThat(callIsCallSite).isTrue();
  }

  @Test
  void testInvokesMethodAnnotationOnOtherReceiverIsNotDeclaredUsage() throws IOException {
    final String code =
        """
        def_slotted_exemplar(:a, {})
        def_slotted_exemplar(:b, {})

        _method a.method_name
        _endmethod

        _method b.caller(name)
          ## @invokes_method {user:b.method_name}
          _self.perform(name)
        _endmethod
        """;
    this.addMagikFile(code);
    final Set<MethodUsageLocator.Option> options =
        Set.of(MethodUsageLocator.Option.DECLARED_USAGES);

    final List<Entry<MethodUsage, MagikTypedFile>> usages =
        this.locateMethodUsages("a", "method_name", options);

    assertThat(usages).isEmpty();
  }

  @Test
  void testInvokesMethodAnnotationOnSubtypeIsDeclaredUsage() throws IOException {
    final String code =
        """
        def_slotted_exemplar(:a, {})
        def_slotted_exemplar(:b, {}, {:a})

        _method a.method_name
        _endmethod

        _method b.caller(name)
          ## @invokes_method {user:b.method_name}
          _self.perform(name)
        _endmethod
        """;
    this.addMagikFile(code);
    final Set<MethodUsageLocator.Option> options =
        Set.of(MethodUsageLocator.Option.DECLARED_USAGES);

    final List<Entry<MethodUsage, MagikTypedFile>> usages =
        this.locateMethodUsages("a", "method_name", options);

    assertThat(usages).hasSize(1);
  }

  @Test
  void testProcedureUsagesAreAnsweredOnRequestOnly() throws IOException {
    final String code =
        """
        def_slotted_exemplar(:a, {})

        _method a.method_name
        _endmethod

        _global p << _proc@p()
          a.method_name
        _endproc
        """;
    this.addMagikFile(code);

    final List<Entry<MethodUsage, MagikTypedFile>> defaultUsages =
        this.locateMethodUsages("a", "method_name");
    final Set<MethodUsageLocator.Option> options =
        Set.of(MethodUsageLocator.Option.PROCEDURE_USAGES);
    final List<Entry<MethodUsage, MagikTypedFile>> procedureUsages =
        this.locateMethodUsages("a", "method_name", options);

    assertThat(defaultUsages).isEmpty();
    assertThat(procedureUsages).hasSize(1);
  }

  @Test
  void testGlobalUsagesAreAnsweredOnRequestOnly() throws IOException {
    final String code =
        """
        def_slotted_exemplar(:a, {})

        _method a.method_name
        _endmethod

        _global g << a.method_name
        """;
    this.addMagikFile(code);

    final List<Entry<MethodUsage, MagikTypedFile>> defaultUsages =
        this.locateMethodUsages("a", "method_name");
    final Set<MethodUsageLocator.Option> options = Set.of(MethodUsageLocator.Option.GLOBAL_USAGES);
    final List<Entry<MethodUsage, MagikTypedFile>> globalUsages =
        this.locateMethodUsages("a", "method_name", options);

    assertThat(defaultUsages).isEmpty();
    assertThat(globalUsages).hasSize(1);
  }

  @Test
  void testCallInProcedureInMethodIsAnsweredOnce() throws IOException {
    final String code =
        """
        def_slotted_exemplar(:a, {})

        _method a.method_name
        _endmethod

        _method a.runner
          _return _proc@p()
            a.method_name
          _endproc
        _endmethod
        """;
    this.addMagikFile(code);
    final Set<MethodUsageLocator.Option> options =
        Set.of(MethodUsageLocator.Option.PROCEDURE_USAGES);

    final List<Entry<MethodUsage, MagikTypedFile>> usages =
        this.locateMethodUsages("a", "method_name", options);

    assertThat(usages).hasSize(1);
  }

  @Test
  void testAnnotatedProcedureArgumentIsNotEnclosingCall() throws IOException {
    final String code =
        """
        def_slotted_exemplar(:a, {})

        _method a.method_name
        _endmethod

        _method a.do(callback)
        _endmethod

        _method a.runner
          _self.do(_proc@p(name)
            ## @invokes_method {user:a.method_name}
          _endproc)
        _endmethod
        """;
    this.addMagikFile(code);
    final Set<MethodUsageLocator.Option> procedureOptions =
        Set.of(MethodUsageLocator.Option.PROCEDURE_USAGES);
    final Set<MethodUsageLocator.Option> allOptions =
        Set.of(
            MethodUsageLocator.Option.PROCEDURE_USAGES, MethodUsageLocator.Option.DECLARED_USAGES);

    final List<Entry<MethodUsage, MagikTypedFile>> procedureUsages =
        this.locateMethodUsages("a", "method_name", procedureOptions);
    final List<Entry<MethodUsage, MagikTypedFile>> allUsages =
        this.locateMethodUsages("a", "method_name", allOptions);

    // The annotated procedure sits inside the invocation of `do()`, which is no call of the method.
    assertThat(procedureUsages).isEmpty();
    assertThat(allUsages).hasSize(1);
    final Entry<MethodUsage, MagikTypedFile> entry = allUsages.get(0);
    final MethodUsage usage = entry.getKey();
    final AstNode node = usage.getNode();
    final AstNodeType nodeType = node.getType();
    assertThat(nodeType).isEqualTo(MagikGrammar.PROCEDURE_DEFINITION);
  }

  private MethodUsage getUsageAtLine(
      final List<Entry<MethodUsage, MagikTypedFile>> usages, final int line) {
    return usages.stream()
        .map(Entry::getKey)
        .filter(usage -> this.getStartLine(usage) == line)
        .findFirst()
        .orElseThrow();
  }

  private int getStartLine(final MethodUsage usage) {
    final Location location = usage.getLocation();
    final Range range = location.getRange();
    final Position startPosition = range.getStartPosition();
    return startPosition.getLine();
  }

  private List<Entry<MethodUsage, MagikTypedFile>> locateMethodUsages(
      final String exemplarName,
      final String methodName,
      final Set<MethodUsageLocator.Option> options) {
    final IDefinitionKeeper definitionKeeper = this.smallworldProject.getDefinitionKeeper();
    final MethodUsageLocator methodUsageLocator = new MethodUsageLocator(definitionKeeper);
    final TypeString typeStr = TypeString.ofIdentifier(exemplarName, "user");
    final MethodUsage wantedMethodUsage = new MethodUsage(typeStr, methodName);
    return methodUsageLocator.getMethodUsages(wantedMethodUsage, options);
  }

  @Test
  void testSuperCallInsideMethodIsNotItsUsage() throws IOException {
    final String code =
        """
        def_slotted_exemplar(:p, {})
        def_slotted_exemplar(:t, {}, {:p})

        _method p.m
        _endmethod

        _method t.m
          _super.m
        _endmethod
        """;
    this.addMagikFile(code);

    final List<Entry<MethodUsage, MagikTypedFile>> usages = this.locateMethodUsages("t", "m");

    assertThat(usages).isEmpty();
  }

  @Test
  void testSuperCallInsideSiblingIsNotUsage() throws IOException {
    final String code =
        """
        def_slotted_exemplar(:p, {})
        def_slotted_exemplar(:t, {}, {:p})
        def_slotted_exemplar(:s, {}, {:p})

        _method p.m
        _endmethod

        _method t.m
        _endmethod

        _method s.m
          _super.m
        _endmethod
        """;
    this.addMagikFile(code);

    final List<Entry<MethodUsage, MagikTypedFile>> usages = this.locateMethodUsages("t", "m");

    assertThat(usages).isEmpty();
  }

  @Test
  void testSuperCallShadowedByNearerParentIsNotUsage() throws IOException {
    final String code =
        """
        def_slotted_exemplar(:t, {})
        def_slotted_exemplar(:u, {}, {:t})
        def_slotted_exemplar(:s, {}, {:u})

        _method t.m
        _endmethod

        _method u.m
        _endmethod

        _method s.m
          _super.m
        _endmethod
        """;
    this.addMagikFile(code);

    final List<Entry<MethodUsage, MagikTypedFile>> tUsages = this.locateMethodUsages("t", "m");
    final List<Entry<MethodUsage, MagikTypedFile>> uUsages = this.locateMethodUsages("u", "m");

    assertThat(tUsages).isEmpty();
    assertThat(uUsages).hasSize(1);
  }

  @Test
  void testSuperCallInsideChildIsParentUsage() throws IOException {
    final String code =
        """
        def_slotted_exemplar(:t, {})
        def_slotted_exemplar(:s, {}, {:t})

        _method t.m(p)
        _endmethod

        _method s.m(p)
          _super.m(1)
        _endmethod
        """;
    this.addMagikFile(code);

    final List<Entry<MethodUsage, MagikTypedFile>> usages = this.locateMethodUsages("t", "m()");

    assertThat(usages).hasSize(1);
  }

  @Test
  void testSuperCallThroughNonOverridingParentIsUsage() throws IOException {
    final String code =
        """
        def_slotted_exemplar(:t, {})
        def_slotted_exemplar(:u, {}, {:t})
        def_slotted_exemplar(:s, {}, {:u})

        _method t.m(p)
        _endmethod

        _method s.m(p)
          _super.m(1)
        _endmethod
        """;
    this.addMagikFile(code);

    final List<Entry<MethodUsage, MagikTypedFile>> usages = this.locateMethodUsages("t", "m()");

    assertThat(usages).hasSize(1);
  }

  @Test
  void testNamedSuperCallIsUsageOfNamedParentOnly() throws IOException {
    final String code =
        """
        def_slotted_exemplar(:t, {})
        def_slotted_exemplar(:q, {})
        def_slotted_exemplar(:s, {}, {:t, :q})

        _method t.m
        _endmethod

        _method q.m
        _endmethod

        _method s.m
          _super(q).m
        _endmethod
        """;
    this.addMagikFile(code);

    final List<Entry<MethodUsage, MagikTypedFile>> tUsages = this.locateMethodUsages("t", "m");
    final List<Entry<MethodUsage, MagikTypedFile>> qUsages = this.locateMethodUsages("q", "m");
    final List<Entry<MethodUsage, MagikTypedFile>> sUsages = this.locateMethodUsages("s", "m");

    assertThat(tUsages).isEmpty();
    assertThat(qUsages).hasSize(1);
    assertThat(sUsages).isEmpty();
  }

  @Test
  void testSubclassReceiverInheritingMethodIsUsage() throws IOException {
    final String code =
        """
        def_slotted_exemplar(:parent, {})
        def_slotted_exemplar(:child, {}, {:parent})

        _method parent.m(p)
          _return p
        _endmethod

        _method child.go()
          _return _self.m(1)
        _endmethod
        """;
    this.addMagikFile(code);

    final List<Entry<MethodUsage, MagikTypedFile>> usages =
        this.locateMethodUsages("parent", "m()");

    assertThat(usages).hasSize(1);
  }

  @Test
  void testOverridingSubclassReceiverIsNotUsage() throws IOException {
    final String code =
        """
        def_slotted_exemplar(:parent, {})
        def_slotted_exemplar(:child, {}, {:parent})

        _method parent.m(p)
          _return p
        _endmethod

        _method child.m(p)
          _return p
        _endmethod

        _method child.go()
          _return _self.m(1)
        _endmethod
        """;
    this.addMagikFile(code);

    final List<Entry<MethodUsage, MagikTypedFile>> parentUsages =
        this.locateMethodUsages("parent", "m()");
    final List<Entry<MethodUsage, MagikTypedFile>> childUsages =
        this.locateMethodUsages("child", "m()");

    assertThat(parentUsages).isEmpty();
    assertThat(childUsages).hasSize(1);
  }

  @Test
  void testInheritingAndOverridingSubclassesAreToldApart() throws IOException {
    final String code =
        """
        def_slotted_exemplar(:parent, {})
        def_slotted_exemplar(:heir, {}, {:parent})
        def_slotted_exemplar(:overrider, {}, {:parent})

        _method parent.m(p)
          _return p
        _endmethod

        _method overrider.m(p)
          _return p
        _endmethod

        _method heir.go()
          _return _self.m(1)
        _endmethod

        _method overrider.go()
          _return _self.m(1)
        _endmethod
        """;
    this.addMagikFile(code);

    final List<Entry<MethodUsage, MagikTypedFile>> usages =
        this.locateMethodUsages("parent", "m()");

    assertThat(usages).hasSize(1);
  }

  @Test
  void testUnionReceiverWithInheritingMemberIsUsage() throws IOException {
    final String code =
        """
        def_slotted_exemplar(:parent, {})
        def_slotted_exemplar(:child, {}, {:parent})
        def_slotted_exemplar(:other, {})

        _method parent.m(p)
          _return p
        _endmethod

        _method other.m(p)
          _return p
        _endmethod

        _method other.go(r)
          ## @param {user:child|user:other} r
          _return r.m(1)
        _endmethod
        """;
    this.addMagikFile(code);

    final List<Entry<MethodUsage, MagikTypedFile>> usages =
        this.locateMethodUsages("parent", "m()");

    assertThat(usages).hasSize(1);
  }

  @Test
  void testGlobalAliasReceiverReachesTheAliasedExemplarsOverride() throws IOException {
    final String code =
        """
        def_slotted_exemplar(:parent, {})
        def_slotted_exemplar(:child, {}, {:parent})

        _method parent.m(p)
          _return p
        _endmethod

        _method child.m(p)
          _return p
        _endmethod

        _method parent.go(r)
          ## @param {user:alias} r
          _return r.m(1)
        _endmethod
        """;
    this.addMagikFile(code);
    final IDefinitionKeeper definitionKeeper = this.smallworldProject.getDefinitionKeeper();
    final TypeString aliasTypeStr = TypeString.ofIdentifier("alias", "user");
    final TypeString childTypeStr = TypeString.ofIdentifier("child", "user");
    final GlobalDefinition aliasDefinition =
        new GlobalDefinition(null, null, null, null, null, aliasTypeStr, childTypeStr);
    definitionKeeper.add(aliasDefinition);

    final List<Entry<MethodUsage, MagikTypedFile>> parentUsages =
        this.locateMethodUsages("parent", "m()");
    final List<Entry<MethodUsage, MagikTypedFile>> childUsages =
        this.locateMethodUsages("child", "m()");

    assertThat(parentUsages).isEmpty();
    assertThat(childUsages).hasSize(1);
  }

  @Test
  void testInheritedAbstractDeclarationIsUsage() throws IOException {
    final String code =
        """
        def_slotted_exemplar(:base, {})
        def_slotted_exemplar(:mid, {}, {:base})
        def_slotted_exemplar(:impl, {}, {:mid})

        _abstract _method base.m(p)
        _endmethod

        _method impl.m(p)
          _return p
        _endmethod

        _method impl.go(x)
          ## @param {user:mid} x
          _return x.m(:s)
        _endmethod
        """;
    this.addMagikFile(code);

    final List<Entry<MethodUsage, MagikTypedFile>> baseUsages =
        this.locateMethodUsages("base", "m()");
    final List<Entry<MethodUsage, MagikTypedFile>> implUsages =
        this.locateMethodUsages("impl", "m()");

    assertThat(baseUsages).hasSize(1);
    assertThat(implUsages).hasSize(1);
  }

  @Test
  void testGlobalAliasChainReachesTheAliasedExemplarsOverride() throws IOException {
    final String code =
        """
        def_slotted_exemplar(:parent, {})
        def_slotted_exemplar(:child, {}, {:parent})

        _method parent.m(p)
          _return p
        _endmethod

        _method child.m(p)
          _return p
        _endmethod

        _method parent.go(r)
          ## @param {user:outer_alias} r
          _return r.m(1)
        _endmethod
        """;
    this.addMagikFile(code);
    final IDefinitionKeeper definitionKeeper = this.smallworldProject.getDefinitionKeeper();
    final TypeString outerAliasTypeStr = TypeString.ofIdentifier("outer_alias", "user");
    final TypeString innerAliasTypeStr = TypeString.ofIdentifier("inner_alias", "user");
    final TypeString childTypeStr = TypeString.ofIdentifier("child", "user");
    final GlobalDefinition outerAliasDefinition =
        new GlobalDefinition(null, null, null, null, null, outerAliasTypeStr, innerAliasTypeStr);
    final GlobalDefinition innerAliasDefinition =
        new GlobalDefinition(null, null, null, null, null, innerAliasTypeStr, childTypeStr);
    definitionKeeper.add(outerAliasDefinition);
    definitionKeeper.add(innerAliasDefinition);

    final List<Entry<MethodUsage, MagikTypedFile>> parentUsages =
        this.locateMethodUsages("parent", "m()");
    final List<Entry<MethodUsage, MagikTypedFile>> childUsages =
        this.locateMethodUsages("child", "m()");

    assertThat(parentUsages).isEmpty();
    assertThat(childUsages).hasSize(1);
  }

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
  void testGlobalAliasCyclesReachNothing() throws IOException {
    final String code =
        """
        def_slotted_exemplar(:parent, {})

        _method parent.m(p)
          _return p
        _endmethod

        _method parent.go(r, s)
          ## @param {user:self_alias} r
          ## @param {user:cycle_alias_a} s
          r.m(1)
          s.m(1)
        _endmethod
        """;
    this.addMagikFile(code);
    final IDefinitionKeeper definitionKeeper = this.smallworldProject.getDefinitionKeeper();
    final TypeString selfAliasTypeStr = TypeString.ofIdentifier("self_alias", "user");
    final TypeString cycleAliasATypeStr = TypeString.ofIdentifier("cycle_alias_a", "user");
    final TypeString cycleAliasBTypeStr = TypeString.ofIdentifier("cycle_alias_b", "user");
    final GlobalDefinition selfAliasDefinition =
        new GlobalDefinition(null, null, null, null, null, selfAliasTypeStr, selfAliasTypeStr);
    final GlobalDefinition cycleAliasADefinition =
        new GlobalDefinition(null, null, null, null, null, cycleAliasATypeStr, cycleAliasBTypeStr);
    final GlobalDefinition cycleAliasBDefinition =
        new GlobalDefinition(null, null, null, null, null, cycleAliasBTypeStr, cycleAliasATypeStr);
    definitionKeeper.add(selfAliasDefinition);
    definitionKeeper.add(cycleAliasADefinition);
    definitionKeeper.add(cycleAliasBDefinition);

    final List<Entry<MethodUsage, MagikTypedFile>> usages =
        this.locateMethodUsages("parent", "m()");

    assertThat(usages).isEmpty();
  }

  @Test
  void testGlobalAliasToUnionExpandsItsMembers() throws IOException {
    final String code =
        """
        def_slotted_exemplar(:parent, {})
        def_slotted_exemplar(:child, {}, {:parent})
        def_slotted_exemplar(:other, {})

        _method parent.m(p)
          _return p
        _endmethod

        _method child.m(p)
          _return p
        _endmethod

        _method parent.go(r)
          ## @param {user:union_alias} r
          _return r.m(1)
        _endmethod
        """;
    this.addMagikFile(code);
    final IDefinitionKeeper definitionKeeper = this.smallworldProject.getDefinitionKeeper();
    final TypeString unionAliasTypeStr = TypeString.ofIdentifier("union_alias", "user");
    final TypeString innerAliasTypeStr = TypeString.ofIdentifier("inner_alias", "user");
    final TypeString otherTypeStr = TypeString.ofIdentifier("other", "user");
    final TypeString childTypeStr = TypeString.ofIdentifier("child", "user");
    final TypeString unionTypeStr = TypeString.combine(innerAliasTypeStr, otherTypeStr);
    final GlobalDefinition unionAliasDefinition =
        new GlobalDefinition(null, null, null, null, null, unionAliasTypeStr, unionTypeStr);
    final GlobalDefinition innerAliasDefinition =
        new GlobalDefinition(null, null, null, null, null, innerAliasTypeStr, childTypeStr);
    definitionKeeper.add(unionAliasDefinition);
    definitionKeeper.add(innerAliasDefinition);

    final List<Entry<MethodUsage, MagikTypedFile>> parentUsages =
        this.locateMethodUsages("parent", "m()");
    final List<Entry<MethodUsage, MagikTypedFile>> childUsages =
        this.locateMethodUsages("child", "m()");

    assertThat(parentUsages).isEmpty();
    assertThat(childUsages).hasSize(1);
  }

  @Test
  void testIntermediateOverrideHidesTheGrandparentsMethod() throws IOException {
    final String code =
        """
        def_slotted_exemplar(:grandparent, {})
        def_slotted_exemplar(:middle, {}, {:grandparent})
        def_slotted_exemplar(:leaf, {}, {:middle})

        _method grandparent.m(p)
          _return p
        _endmethod

        _method middle.m(p)
          _return p
        _endmethod

        _method leaf.go()
          _return _self.m(1)
        _endmethod
        """;
    this.addMagikFile(code);

    final List<Entry<MethodUsage, MagikTypedFile>> grandparentUsages =
        this.locateMethodUsages("grandparent", "m()");
    final List<Entry<MethodUsage, MagikTypedFile>> middleUsages =
        this.locateMethodUsages("middle", "m()");

    assertThat(grandparentUsages).isEmpty();
    assertThat(middleUsages).hasSize(1);
  }

  @Test
  void testIncluderCallIsUsageOfMixinOwnedMethod() throws IOException {
    final String code =
        """
        def_mixin(:helper_mixin)
        def_slotted_exemplar(:child, {}, {:helper_mixin})

        _method helper_mixin.m(p)
          _return p
        _endmethod

        _method child.go()
          _return _self.m(1)
        _endmethod
        """;
    this.addMagikFile(code);

    final List<Entry<MethodUsage, MagikTypedFile>> usages =
        this.locateMethodUsages("helper_mixin", "m()");

    assertThat(usages).hasSize(1);
  }

  @Test
  void testEveryRespondingParentPathIsUsage() throws IOException {
    final String code =
        """
        def_slotted_exemplar(:base, {})
        def_slotted_exemplar(:heir, {}, {:base})
        def_slotted_exemplar(:overrider, {}, {:base})
        def_slotted_exemplar(:leaf, {}, {:heir, :overrider})

        _method base.m(p)
          _return p
        _endmethod

        _method overrider.m(p)
          _return p
        _endmethod

        _method leaf.go()
          _return _self.m(1)
        _endmethod
        """;
    this.addMagikFile(code);

    final List<Entry<MethodUsage, MagikTypedFile>> baseUsages =
        this.locateMethodUsages("base", "m()");
    final List<Entry<MethodUsage, MagikTypedFile>> overriderUsages =
        this.locateMethodUsages("overrider", "m()");

    assertThat(baseUsages).hasSize(1);
    assertThat(overriderUsages).hasSize(1);
  }

  private void addMagikFile(final String code) throws IOException {
    final Path path = this.smallworldProject.pathOf("/source.magik");
    this.smallworldProject.addMagikFile(path, code);
  }

  private List<Entry<MethodUsage, MagikTypedFile>> locateMethodUsages(
      final String exemplarName, final String methodName) {
    final IDefinitionKeeper definitionKeeper = this.smallworldProject.getDefinitionKeeper();
    final MethodUsageLocator methodUsageLocator = new MethodUsageLocator(definitionKeeper);
    final TypeString typeStr = TypeString.ofIdentifier(exemplarName, "user");
    final MethodUsage wantedMethodUsage = new MethodUsage(typeStr, methodName);
    return methodUsageLocator.getMethodUsages(wantedMethodUsage);
  }

  /** A file deleted after the keeper was filled, holding two calls to the wanted method. */
  private MethodUsageLocator createLocatorWithDeletedCaller() throws IOException {
    final String code =
        """
        def_slotted_exemplar(:a, {})

        _method a.method_name
        _endmethod
        """;
    final String deletedCode =
        """
        _method a.caller
          _self.method_name
          _self.method_name
        _endmethod
        """;
    final Path path = this.smallworldProject.pathOf("/source.magik");
    this.smallworldProject.addMagikFile(path, code);
    final Path deletedPath = this.smallworldProject.pathOf("/deleted.magik");
    this.smallworldProject.addMagikFile(deletedPath, deletedCode);
    Files.delete(deletedPath);
    final IDefinitionKeeper keeper = this.smallworldProject.getDefinitionKeeper();
    return new MethodUsageLocator(keeper);
  }

  @Test
  void testGetMethodUsagesSkipsCallSitesInUnreadableFileOnceLogged() throws IOException {
    final MethodUsageLocator locator = this.createLocatorWithDeletedCaller();
    final List<LogRecord> warnings = new ArrayList<>();
    final Handler handler = this.createWarningCollector(warnings);
    final Logger logger = Logger.getLogger(MethodUsageLocator.class.getName());
    logger.addHandler(handler);
    final TypeString typeStr = TypeString.ofIdentifier("a", "user");
    final MethodUsage wantedMethodUsage = new MethodUsage(typeStr, "method_name");

    final List<Entry<MethodUsage, MagikTypedFile>> usages;
    try {
      usages = locator.getMethodUsages(wantedMethodUsage);
    } finally {
      logger.removeHandler(handler);
    }

    assertThat(usages).isEmpty();
    assertThat(warnings).hasSize(1);
  }

  private Handler createWarningCollector(final List<LogRecord> warnings) {
    return new Handler() {
      @Override
      public void publish(final LogRecord logRecord) {
        if (logRecord.getLevel() == Level.WARNING) {
          warnings.add(logRecord);
        }
      }

      @Override
      public void flush() {}

      @Override
      public void close() {}
    };
  }
}
