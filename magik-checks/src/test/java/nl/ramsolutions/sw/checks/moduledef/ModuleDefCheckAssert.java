package nl.ramsolutions.sw.checks.moduledef;

import java.net.URI;
import nl.ramsolutions.sw.OpenedFile;
import nl.ramsolutions.sw.checks.Check;
import nl.ramsolutions.sw.checks.CheckAssert;
import nl.ramsolutions.sw.checks.ModuleDefCheck;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.moduledef.ModuleDefFile;

/**
 * Assertions for {@link ModuleDefCheck} instances.
 *
 * <p>Use via {@link #assertThat(ModuleDefCheck)}.
 */
public class ModuleDefCheckAssert extends CheckAssert {

  protected ModuleDefCheckAssert(final Check actual) {
    super(actual);
  }

  /**
   * Get a new instance for the given {@link Check}.
   *
   * @param actual {@link Check} instance.
   * @return Self.
   */
  public static ModuleDefCheckAssert assertThat(final ModuleDefCheck actual) {
    return new ModuleDefCheckAssert(actual);
  }

  @Override
  protected OpenedFile createOpenedFile(final String code) {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper(false);
    return new ModuleDefFile(ModuleDefFile.DEFAULT_URI, code, definitionKeeper, null);
  }

  @Override
  protected OpenedFile createOpenedFile(final URI uri, final String code) {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper(false);
    return new ModuleDefFile(uri, code, definitionKeeper, null);
  }
}
