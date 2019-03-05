package org.stevenlooman.sw.magik;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.RecognitionException;
import com.sonar.sslr.api.Token;

import java.util.List;
import javax.annotation.CheckForNull;

public class MagikVisitorContext {
  private final String fileContent;
  private final AstNode rootTree;
  private final RecognitionException parsingException;

  public MagikVisitorContext(String fileContent, AstNode tree) {
    this(fileContent, tree, null);
  }

  public MagikVisitorContext(String fileContent, RecognitionException parsingException) {
    this(fileContent, null, parsingException);
  }

  private MagikVisitorContext(String fileContent,
                              AstNode rootTree,
                              RecognitionException parsingException) {
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
}
