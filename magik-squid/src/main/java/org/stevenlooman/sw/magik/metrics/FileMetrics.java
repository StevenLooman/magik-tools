package org.stevenlooman.sw.magik.metrics;

import com.sonar.sslr.api.AstNode;
import org.stevenlooman.sw.magik.MagikVisitorContext;
import org.stevenlooman.sw.magik.api.MagikGrammar;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;

public class FileMetrics {
  private int numberOfStatements;
  private int numberOfExemplars;
  private int fileComplexity;
  private List<Integer> methodComplexities = new ArrayList<>();
  private List<Integer> procedureComplexities = new ArrayList<>();
  private Set<Integer> linesOfCode;
  private Set<Integer> commentLines;
  private Set<Integer> nosonarLines;
  private Set<Integer> executableLines;

  /**
   * Constructor.
   * @param context Context to use.
   * @param ignoreHeaderComments Ignore first (header) comment of file.
   */
  public FileMetrics(MagikVisitorContext context, boolean ignoreHeaderComments) {
    AstNode rootTree = context.rootTree();
    if (rootTree == null) {
      throw new RuntimeException("Root Tree is null");
    }

    StatementCountVisitor statementCountVisitor = new StatementCountVisitor();
    statementCountVisitor.scanFile(context);
    numberOfStatements = statementCountVisitor.getStatementCount();

    ExemplarDefinitionVisitor exemplarDefinitionVisitor = new ExemplarDefinitionVisitor();
    exemplarDefinitionVisitor.scanFile(context);
    numberOfExemplars = exemplarDefinitionVisitor.getCount();

    ComplexityVisitor complexityVisitor = new ComplexityVisitor();
    complexityVisitor.scanFile(context);
    fileComplexity = complexityVisitor.getComplexity();

    FileLinesVisitor fileLinesVisitor = new FileLinesVisitor(ignoreHeaderComments);
    fileLinesVisitor.scanFile(context);
    linesOfCode = fileLinesVisitor.getLinesOfCode();
    commentLines = fileLinesVisitor.getLinesOfComments();
    nosonarLines = fileLinesVisitor.getNosonarLines();
    executableLines = fileLinesVisitor.getExecutableLines();

    // method definitions/complexity
    for (AstNode methodDef : rootTree.getDescendants(MagikGrammar.METHOD_DEFINITION)) {
      ComplexityVisitor methodComplexityVisitor = new ComplexityVisitor();
      methodComplexityVisitor.visitNode(methodDef);
      int complexity = methodComplexityVisitor.getComplexity();
      methodComplexities.add(complexity);
    }

    // procedure definitions/complexity
    for (AstNode procDef : rootTree.getDescendants(MagikGrammar.PROC_DEFINITION)) {
      if (isNestedInMethodDef(procDef)) {
        continue; // only non-nested procedure definitions
      }

      ComplexityVisitor procComplexityVisitor = new ComplexityVisitor();
      procComplexityVisitor.visitNode(procDef);
      int complexity = procComplexityVisitor.getComplexity();
      procedureComplexities.add(complexity);
    }
  }

  private boolean isNestedInMethodDef(@Nonnull  AstNode procDef) {
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
    return numberOfExemplars;
  }

  public int numberOfMethods() {
    return methodComplexities.size();
  }

  public int numberOfProcedures() {
    return procedureComplexities.size();
  }

  public int numberOfStatements() {
    return numberOfStatements;
  }

  public int fileComplexity() {
    return fileComplexity;
  }

  public Set<Integer> linesOfCode() {
    return linesOfCode;
  }

  public Set<Integer> commentLines() {
    return commentLines;
  }
  public int commentLineCount() {
    return commentLines.size();
  }

  public Set<Integer> nosonarLines() {
    return nosonarLines;
  }

  public  Set<Integer> executableLines() {
    return executableLines;
  }
}
