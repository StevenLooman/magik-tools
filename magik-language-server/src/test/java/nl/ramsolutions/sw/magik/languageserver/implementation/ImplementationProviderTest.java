package nl.ramsolutions.sw.magik.languageserver.implementation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.InheritanceDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import org.junit.jupiter.api.Test;

/** Test ImplementationProvider. */
@SuppressWarnings("checkstyle:MagicNumber")
class ImplementationProviderTest {

  private static final Location EMPTY_LOCATION =
      new Location(MagikTypedFile.DEFAULT_URI, new Range(new Position(0, 0), new Position(0, 0)));

  private static final TypeString A_REF = TypeString.ofIdentifier("a", "user");
  private static final TypeString B_REF = TypeString.ofIdentifier("b", "user");

  @Test
  void testProvideAbstractMethodImplementation() {
    final IDefinitionKeeper definitionKeeper = this.createDefinitionKeeperWithHierarchy();
    definitionKeeper.add(this.createMethodDefinition(A_REF, "abstract()", true, 0));
    definitionKeeper.add(this.createMethodDefinition(B_REF, "abstract()", false, 50));

    final String code =
        """
        _abstract _method a.abstract()
        _endmethod""";
    final Position position = new Position(1, 26); // On `abstract()`.

    final List<Location> implementations =
        this.provideImplementations(definitionKeeper, code, position);
    final Location expected = this.createMethodLocation(50);
    assertThat(implementations).containsOnly(expected);
  }

  @Test
  void testProvideImplementationsOnlyForMethodUnderCursor() {
    final IDefinitionKeeper definitionKeeper = this.createDefinitionKeeperWithHierarchy();
    definitionKeeper.add(this.createMethodDefinition(A_REF, "m1()", true, 0));
    definitionKeeper.add(this.createMethodDefinition(B_REF, "m1()", false, 50));
    definitionKeeper.add(this.createMethodDefinition(B_REF, "m2()", false, 60));

    final String code =
        """
        _abstract _method a.m1()
        _endmethod""";
    final Position position = new Position(1, 20); // On `m1()`.

    final List<Location> implementations =
        this.provideImplementations(definitionKeeper, code, position);
    final Location expected = this.createMethodLocation(50);
    assertThat(implementations).containsOnly(expected);
  }

  @Test
  void testProvideNoImplementationsForConcreteMethod() {
    final IDefinitionKeeper definitionKeeper = this.createDefinitionKeeperWithHierarchy();
    definitionKeeper.add(this.createMethodDefinition(A_REF, "m1()", true, 0));
    definitionKeeper.add(this.createMethodDefinition(A_REF, "m2()", false, 10));
    definitionKeeper.add(this.createMethodDefinition(B_REF, "m1()", false, 50));
    definitionKeeper.add(this.createMethodDefinition(B_REF, "m2()", false, 60));

    final String code =
        """
        _method a.m2()
        _endmethod""";
    final Position position = new Position(1, 10); // On `m2()`.

    final List<Location> implementations =
        this.provideImplementations(definitionKeeper, code, position);
    assertThat(implementations).isEmpty();
  }

  @Test
  void testProvideImplementationsDistinguishesAssignmentMethod() {
    final IDefinitionKeeper definitionKeeper = this.createDefinitionKeeperWithHierarchy();
    definitionKeeper.add(this.createMethodDefinition(A_REF, "bar<<", true, 0));
    definitionKeeper.add(this.createMethodDefinition(A_REF, "bar()", true, 10));
    definitionKeeper.add(this.createMethodDefinition(B_REF, "bar<<", false, 50));
    definitionKeeper.add(this.createMethodDefinition(B_REF, "bar()", false, 60));

    final String code =
        """
        _abstract _method a.bar<<value
        _endmethod""";
    final Position position = new Position(1, 21); // On `bar<<`.

    final List<Location> implementations =
        this.provideImplementations(definitionKeeper, code, position);
    final Location expected = this.createMethodLocation(50);
    assertThat(implementations).containsOnly(expected);
  }

  @Test
  void testProvideNoImplementationsForInvocationInMethod() {
    final IDefinitionKeeper definitionKeeper = this.createDefinitionKeeperWithHierarchy();
    definitionKeeper.add(this.createMethodDefinition(A_REF, "m1()", true, 0));
    definitionKeeper.add(this.createMethodDefinition(B_REF, "m1()", false, 50));

    final String code =
        """
        _abstract _method a.m1()
            _self.m1()
        _endmethod""";
    final Position position = new Position(2, 10); // On the invocation `m1()`.

    final List<Location> implementations =
        this.provideImplementations(definitionKeeper, code, position);
    assertThat(implementations).isEmpty();
  }

  @Test
  void testProvideNoImplementationsForInvocationOutsideMethod() {
    final IDefinitionKeeper definitionKeeper = this.createDefinitionKeeperWithHierarchy();
    definitionKeeper.add(this.createMethodDefinition(A_REF, "m1()", true, 0));
    definitionKeeper.add(this.createMethodDefinition(B_REF, "m1()", false, 50));

    final String code =
        """
        _block
            x.m1()
        _endblock""";
    final Position position = new Position(2, 6); // On the invocation `m1()`.

    final List<Location> implementations =
        this.provideImplementations(definitionKeeper, code, position);
    assertThat(implementations).isEmpty();
  }

  private List<Location> provideImplementations(
      final IDefinitionKeeper definitionKeeper, final String code, final Position position) {
    final MagikTypedFile magikFile =
        new MagikTypedFile(MagikTypedFile.DEFAULT_URI, code, definitionKeeper);
    final ImplementationProvider provider = new ImplementationProvider();
    return provider.provideImplementations(magikFile, position);
  }

  /** Keeper holding exemplars `user:a` and `user:b`, where `b` inherits from `a`. */
  private IDefinitionKeeper createDefinitionKeeperWithHierarchy() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(this.createExemplarDefinition(A_REF));
    definitionKeeper.add(this.createExemplarDefinition(B_REF));
    definitionKeeper.add(new InheritanceDefinition(null, null, null, null, null, B_REF, A_REF));
    return definitionKeeper;
  }

  private ExemplarDefinition createExemplarDefinition(final TypeString typeName) {
    return new ExemplarDefinition(
        EMPTY_LOCATION, null, null, null, null, ExemplarDefinition.Sort.SLOTTED, typeName, null);
  }

  private MethodDefinition createMethodDefinition(
      final TypeString typeName,
      final String methodName,
      final boolean isAbstract,
      final int line) {
    final Set<MethodDefinition.Modifier> modifiers =
        isAbstract
            ? Set.of(MethodDefinition.Modifier.ABSTRACT)
            : Collections.<MethodDefinition.Modifier>emptySet();
    final Location location = this.createMethodLocation(line);
    return new MethodDefinition(
        location,
        null,
        null,
        null,
        null,
        typeName,
        methodName,
        modifiers,
        Collections.emptyList(),
        null,
        null,
        ExpressionResultString.UNDEFINED,
        ExpressionResultString.EMPTY);
  }

  private Location createMethodLocation(final int line) {
    return new Location(
        MagikTypedFile.DEFAULT_URI, new Range(new Position(line, 0), new Position(line, 10)));
  }
}
