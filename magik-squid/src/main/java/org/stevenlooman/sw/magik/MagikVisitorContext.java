package org.stevenlooman.sw.magik;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.RecognitionException;
import com.sonar.sslr.api.Token;

import org.stevenlooman.sw.magik.analysis.scope.GlobalScope;
import org.stevenlooman.sw.magik.analysis.scope.ScopeBuilderVisitor;

import java.nio.file.Path;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class MagikVisitorContext {
  private final Path path;
  private final String fileContent;
  private final AstNode rootTree;
  private final RecognitionException parsingException;
  private ScopeBuilderVisitor scopeBuilder;

  public MagikVisitorContext(Path path, String fileContent, AstNode tree) {
    this(path, fileContent, tree, null);
  }

  public MagikVisitorContext(Path path, String fileContent, RecognitionException parsingException) {
    this(path, fileContent, null, parsingException);
  }

  public MagikVisitorContext(Path path, AstNode tree) {
    this(path, null, tree, null);
  }

  public MagikVisitorContext(String fileContent, AstNode tree) {
    this(null, fileContent, tree, null);
  }

  private MagikVisitorContext(Path path,
                              String fileContent,
                              AstNode rootTree,
                              RecognitionException parsingException) {
    this.path = path;
    this.fileContent = fileContent;
    this.rootTree = rootTree;
    this.parsingException = parsingException;
  }

  @CheckForNull
  public AstNode rootTree() {
    return rootTree;
  }

  public List<Token> tokens() {
    return rootTree.getTokens();
  }

  @CheckForNull
  public RecognitionException parsingException() {
    return parsingException;
  }

  public String fileContent() {
    return fileContent;
  }

  @CheckForNull
  public Path path() {
    return path;
  }

  /**
   * Get the Global scope from this context.
   */
  @Nullable
  public GlobalScope getGlobalScope() {
    if (scopeBuilder == null
        && rootTree != null) {
      scopeBuilder = new ScopeBuilderVisitor();
      scopeBuilder.scanNode(rootTree);
    }
    if (scopeBuilder == null) {
      return null;
    }
    return scopeBuilder.getGlobalScope();
  }

}
