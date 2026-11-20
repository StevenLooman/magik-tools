package nl.ramsolutions.sw.magik.languageserver.signaturehelp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.InheritanceDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.SignatureHelp;
import org.eclipse.lsp4j.SignatureInformation;
import org.junit.jupiter.api.Test;

/** Test {@link SignatureHelpProvider}. */
@SuppressWarnings("checkstyle:MagicNumber")
class SignatureHelpProviderTest {

  private static final TypeString ZZ_M_REF = TypeString.ofIdentifier("zz_m", "user");
  private static final TypeString ZZ_E_REF = TypeString.ofIdentifier("zz_e", "user");

  /**
   * A slotted {@code user:zz_e} over the lonely mixin {@code user:zz_m}, and {@code sw:object}
   * answering {@code zz_object_method()}.
   */
  private IDefinitionKeeper createExemplarOverMixinKeeper() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new MethodDefinition(
            null,
            null,
            null,
            null,
            null,
            TypeString.SW_OBJECT,
            "zz_object_method()",
            Collections.emptySet(),
            Collections.emptyList(),
            null,
            null,
            ExpressionResultString.UNDEFINED,
            ExpressionResultString.EMPTY));
    definitionKeeper.add(
        new ExemplarDefinition(
            null, null, null, null, null, ExemplarDefinition.Sort.MIXIN, ZZ_M_REF, null));
    definitionKeeper.add(
        new ExemplarDefinition(
            null, null, null, null, null, ExemplarDefinition.Sort.SLOTTED, ZZ_E_REF, null));
    definitionKeeper.add(
        new InheritanceDefinition(null, null, null, null, null, ZZ_E_REF, ZZ_M_REF));
    return definitionKeeper;
  }

  private List<String> getSignatureLabels(final String code, final Position position) {
    final IDefinitionKeeper definitionKeeper = this.createExemplarOverMixinKeeper();
    final MagikTypedFile magikFile =
        new MagikTypedFile(MagikTypedFile.DEFAULT_URI, code, definitionKeeper);
    final SignatureHelpProvider provider = new SignatureHelpProvider();
    final SignatureHelp signatureHelp = provider.provideSignatureHelp(magikFile, position);
    final List<SignatureInformation> signatures = signatureHelp.getSignatures();
    return signatures.stream().map(SignatureInformation::getLabel).toList();
  }

  @Test
  void testObjectMethodOnMixinTypedParameterIsOffered() {
    final String code =
        """
        _package user
        _method zz_e.run(p)
            ## @param {user:zz_m} p
            p.zz_object_method()
        _endmethod""";
    final Position position = new Position(3, 10); // On `zz_object_method`.

    final List<String> labels = this.getSignatureLabels(code, position);

    assertThat(labels).hasSize(1);
  }

  @Test
  void testObjectMethodOnNamedMixinSuperIsNotOffered() {
    // A mixin's own table has no parents, so sw:object's method does not answer.
    final String code =
        """
        _package user
        _method zz_e.run()
            _super(zz_m).zz_object_method()
        _endmethod""";
    final Position position = new Position(2, 20); // On `zz_object_method`.

    final List<String> labels = this.getSignatureLabels(code, position);

    assertThat(labels).isEmpty();
  }
}
