package org.stevenlooman.sw.magik;

import com.google.common.collect.ImmutableSet;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import com.sonar.sslr.api.Token;

import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;

public abstract class MagikVisitor {
  private MagikVisitorContext context;
  private Set<AstNodeType> subscribedKinds = null;

  public abstract List<AstNodeType> subscribedTo();

  private Set<AstNodeType> subscribedKinds() {
    if (subscribedKinds == null) {
      subscribedKinds = ImmutableSet.copyOf(subscribedTo());
    }
    return subscribedKinds;
  }

  public void visitFile(@Nullable AstNode node) {
  }

  public void leaveFile(@Nullable AstNode node) {
  }

  public void visitNode(AstNode node) {
  }

  public void leaveNode(AstNode node) {
  }

  public void visitToken(Token token) {
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
      scanNode(tree);
    }
    leaveFile(tree);
  }

  /**
   * Scan the node/tree.
   * @param node Node to scan.
   */
  public void scanNode(AstNode node) {
    boolean isSubscribedType = subscribedKinds().contains(node.getType());

    if (isSubscribedType) {
      visitNode(node);
    }

    List<AstNode> children = node.getChildren();
    if (children.isEmpty()) {
      node.getTokens().forEach(this::visitToken);
    } else {
      children.forEach(this::scanNode);
    }

    if (isSubscribedType) {
      leaveNode(node);
    }
  }
}
