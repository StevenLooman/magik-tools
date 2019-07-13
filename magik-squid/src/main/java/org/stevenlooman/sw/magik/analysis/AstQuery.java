package org.stevenlooman.sw.magik.analysis;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * AstNode query utility functions.
 */
public class AstQuery {

  private AstQuery() {

  }

  /**
   * Get the AstNodes which match a chain of {{AstNodeType}}s.
   * This does perform backtracking if needed.
   * @Param node {{AstNode}} to query
   * @param nodeTypes Chain of {{AstNodeType}}s
   * @return
   */
  public static List<AstNode> getFromChain(AstNode node, AstNodeType... nodeTypes) {
    List<AstNode> nodes = Arrays.asList(node);
    // loop over chain
    for (AstNodeType nodeType: nodeTypes) {
      // of current item in chain, find all children of so-far-matching nodes
      List<AstNode> currentChildren = new ArrayList<>();
      for (AstNode currentNode: nodes) {
        List<AstNode> currentNodeChildren = currentNode.getChildren(nodeType);
        currentChildren.addAll(currentNodeChildren);
      }
      // loop
      nodes = currentChildren;
    }

    return nodes;
  }


}
