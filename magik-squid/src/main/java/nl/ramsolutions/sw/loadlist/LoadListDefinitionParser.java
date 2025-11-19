package nl.ramsolutions.sw.loadlist;

import com.sonar.sslr.api.AstNode;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import nl.ramsolutions.sw.loadlist.api.LoadListGrammar;
import nl.ramsolutions.sw.loadlist.parser.LoadListParser;
import nl.ramsolutions.sw.magik.Location;

/** Parser for Smallworld load_list.txt/patch_list.txt files. */
public class LoadListDefinitionParser {

  /**
   * Parse a load_list.txt/patch_list.txt file.
   *
   * @param loadListFile The file to parse.
   * @return The parsed load list definition.
   */
  public LoadListDefinition parseDefinition(final LoadListFile loadListFile) {
    final LoadListParser parser = new LoadListParser();
    final String source = loadListFile.getSource();
    final URI uri = loadListFile.getUri();
    final AstNode node = parser.parse(source, uri);

    final List<String> filePaths =
        node.getDescendants(LoadListGrammar.FILE_PATH).stream()
            .map(filePathNode -> filePathNode.getTokenValue().trim())
            .toList();

    final Location location = new Location(uri);
    final Instant timestamp = loadListFile.getTimestamp();
    return new LoadListDefinition(location, timestamp, filePaths);
  }
}
