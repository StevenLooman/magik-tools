package nl.ramsolutions.sw.magik.analysis.typing;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.SmallworldProjectExtension;
import nl.ramsolutions.sw.magik.analysis.definitions.GlobalDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodUsage;
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
}
