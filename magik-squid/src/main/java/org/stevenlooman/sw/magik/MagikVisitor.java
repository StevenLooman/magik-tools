package org.stevenlooman.sw.magik;

import com.sonar.sslr.api.AstNode;

import org.stevenlooman.sw.magik.analysis.AstWalker;

import javax.annotation.Nullable;

public abstract class MagikVisitor extends AstWalker {
  private MagikVisitorContext context;

  public void visitFile(@Nullable AstNode node) {
  }

  public void leaveFile(@Nullable AstNode node) {
  }

  public MagikVisitorContext getContext() {
    return context;
  }

  /**
   * Scan the file.
   * @param context Context to use.
   */
  public void scanFile(MagikVisitorContext context) {
    this.context = context;

    AstNode tree = context.rootTree();
    visitFile(tree);
    if (tree != null) {
      // scanNode(tree);
      walkAst(tree);
    }
    leaveFile(tree);
  }

}
