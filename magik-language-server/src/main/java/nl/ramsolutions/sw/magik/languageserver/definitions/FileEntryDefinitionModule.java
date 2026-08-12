package nl.ramsolutions.sw.magik.languageserver.definitions;

import com.sonar.sslr.api.AstNode;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import nl.ramsolutions.sw.loadlist.LoadListFile;
import nl.ramsolutions.sw.magik.Location;

/** Provides definitions for a file entry in a load_list.txt file. */
public class FileEntryDefinitionModule implements DefinitionModule<LoadListFile> {

  @Override
  public Optional<List<Location>> tryDefinitions(final DefinitionContext<LoadListFile> context) {
    final AstNode fileEntryNode = context.positionNode();
    final String fileEntryValue = fileEntryNode.getTokenValue().strip();
    if (fileEntryValue.endsWith("/")) {
      // Don't do anything for directory entries.
      return Optional.of(List.of());
    }

    final LoadListFile loadListFile = context.file();
    final URI uri = loadListFile.getUri();
    final String appendExtension = fileEntryValue.endsWith(".magik") ? "" : ".magik";
    final URI fileEntryUri = uri.resolve(fileEntryValue + appendExtension);
    final Location location = new Location(fileEntryUri);
    return Optional.of(List.of(location));
  }
}
