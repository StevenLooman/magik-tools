package nl.ramsolutions.sw.magik.metrics;

import com.sonar.sslr.api.AstNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/** File metrics extractor. */
public class FileMetrics {

  private final int numberOfStatements;
  private final int numberOfExemplars;
  private final int fileComplexity;
  private final List<Integer> methodComplexities = new ArrayList<>();
  private final List<Integer> procedureComplexities = new ArrayList<>();
  private final Set<Integer> linesOfCode;
  private final Set<Integer> commentLines;
  private final Set<Integer> nosonarLines;
  private final Set<Integer> executableLines;

  /**
   * Constructor.
   *
   * @param magikFile Magik file.
   * @param ignoreHeaderComments Ignore first (header) comment of file.
   */
  public FileMetrics(final MagikFile magikFile, final boolean ignoreHeaderComments) {
    final AstNode topNode = magikFile.getTopNode();

    final StatementCountVisitor statementCountVisitor = new StatementCountVisitor();
    statementCountVisitor.scanFile(magikFile);
    this.numberOfStatements = statementCountVisitor.getStatementCount();

    final ExemplarDefinitionVisitor exemplarDefinitionVisitor = new ExemplarDefinitionVisitor();
    exemplarDefinitionVisitor.scanFile(magikFile);
    this.numberOfExemplars = exemplarDefinitionVisitor.getCount();

    final ComplexityVisitor complexityVisitor = new ComplexityVisitor();
    complexityVisitor.scanFile(magikFile);
    this.fileComplexity = complexityVisitor.getComplexity();

    final FileLinesVisitor fileLinesVisitor = new FileLinesVisitor(ignoreHeaderComments);
    fileLinesVisitor.scanFile(magikFile);
    this.linesOfCode = fileLinesVisitor.getLinesOfCode();
    this.commentLines = fileLinesVisitor.getLinesOfComments();
    this.nosonarLines = fileLinesVisitor.getNosonarLines();
    this.executableLines = fileLinesVisitor.getExecutableLines();

    // method definitions/complexity
    for (final AstNode methodDef : topNode.getDescendants(MagikGrammar.METHOD_DEFINITION)) {
      final ComplexityVisitor methodComplexityVisitor = new ComplexityVisitor();
      methodComplexityVisitor.walkAst(methodDef);
      final int complexity = methodComplexityVisitor.getComplexity();
      this.methodComplexities.add(complexity);
    }

    // procedure definitions/complexity
    for (final AstNode procDef : topNode.getDescendants(MagikGrammar.PROCEDURE_DEFINITION)) {
      if (this.isNestedInMethodDef(procDef)) {
        continue; // only non-nested procedure definitions
      }

      final ComplexityVisitor procComplexityVisitor = new ComplexityVisitor();
      procComplexityVisitor.walkAst(procDef);
      final int complexity = procComplexityVisitor.getComplexity();
      this.procedureComplexities.add(complexity);
    }
  }

  private boolean isNestedInMethodDef(final AstNode procDef) {
    AstNode parent = procDef.getParent();
    while (parent != null) {
      if (parent.is(MagikGrammar.METHOD_DEFINITION)) {
        return true;
      }

      parent = parent.getParent();
    }

    return false;
  }

  public int numberOfExemplars() {
    return this.numberOfExemplars;
  }

  public int numberOfMethods() {
    return this.methodComplexities.size();
  }

  public int numberOfProcedures() {
    return this.procedureComplexities.size();
  }

  public int numberOfStatements() {
    return this.numberOfStatements;
  }

  public int fileComplexity() {
    return this.fileComplexity;
  }

  public Set<Integer> linesOfCode() {
    return Collections.unmodifiableSet(this.linesOfCode);
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

  public Set<Integer> executableLines() {
    return Collections.unmodifiableSet(this.executableLines);
  }
}
