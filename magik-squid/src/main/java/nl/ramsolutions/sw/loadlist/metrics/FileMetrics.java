package nl.ramsolutions.sw.loadlist.metrics;

import java.util.Collections;
import java.util.Set;
import nl.ramsolutions.sw.loadlist.LoadListFile;

/** File metrics extractor. */
public class FileMetrics {

  private final Set<Integer> linesOfDefinition;
  private final Set<Integer> commentLines;
  private final Set<Integer> nosonarLines;

  /**
   * Constructor.
   *
   * @param loadListFile load_list file.
   * @param ignoreHeaderComments Ignore first (header) comment of file.
   */
  public FileMetrics(final LoadListFile loadListFile, final boolean ignoreHeaderComments) {
    final FileLinesVisitor fileLinesVisitor = new FileLinesVisitor(ignoreHeaderComments);
    fileLinesVisitor.scanFile(loadListFile);
    this.linesOfDefinition = fileLinesVisitor.getLinesOfEntry();
    this.commentLines = fileLinesVisitor.getLinesOfComments();
    this.nosonarLines = fileLinesVisitor.getNosonarLines();
  }

  public Set<Integer> linesOfEntries() {
    return Collections.unmodifiableSet(this.linesOfDefinition);
  }

  public Set<Integer> commentLines() {
    return Collections.unmodifiableSet(this.commentLines);
  }

  public int commentLineCount() {
    return this.commentLines.size();
  }

  public Set<Integer> nosonarLines() {
    return Collections.unmodifiableSet(this.nosonarLines);
  }
}
