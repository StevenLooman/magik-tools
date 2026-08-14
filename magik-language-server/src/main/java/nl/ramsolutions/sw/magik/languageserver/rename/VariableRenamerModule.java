package nl.ramsolutions.sw.magik.languageserver.rename;

import com.sonar.sslr.api.AstNode;
import java.util.Optional;
import nl.ramsolutions.sw.magik.MagikTypedFile;

/** Rename module for {@link VariableRenamer}. */
class VariableRenamerModule implements RenameModule {

  @Override
  public Optional<Renamer> tryRenamer(final RenameContext context) {
    final MagikTypedFile magikFile = context.magikFile();
    final AstNode node = context.node();
    if (!VariableRenamer.canHandleRename(magikFile, node)) {
      return Optional.empty();
    }

    final Renamer renamer = new VariableRenamer(magikFile, node);
    return Optional.of(renamer);
  }
}
