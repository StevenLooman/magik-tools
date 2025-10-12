package nl.ramsolutions.sw.moduledef.metrics;

import java.util.Collections;
import java.util.Set;
import nl.ramsolutions.sw.moduledef.ModuleDefFile;

/** File metrics extractor. */
public class FileMetrics {

  private final Set<Integer> linesOfDefinition;
  private final Set<Integer> commentLines;
  private final Set<Integer> nosonarLines;

  /**
   * Constructor.
   *
   * @param moduleDefFile module.def file.
   * @param ignoreHeaderComments Ignore first (header) comment of file.
   */
  public FileMetrics(final ModuleDefFile moduleDefFile, final boolean ignoreHeaderComments) {
    final FileLinesVisitor fileLinesVisitor = new FileLinesVisitor(ignoreHeaderComments);
    fileLinesVisitor.scanFile(moduleDefFile);
    this.linesOfDefinition = fileLinesVisitor.getLinesOfDefinition();
    this.commentLines = fileLinesVisitor.getLinesOfComments();
    this.nosonarLines = fileLinesVisitor.getNosonarLines();
  }

  public Set<Integer> linesOfDefinition() {
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
