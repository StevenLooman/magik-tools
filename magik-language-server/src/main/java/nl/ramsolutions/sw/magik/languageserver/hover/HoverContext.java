package nl.ramsolutions.sw.magik.languageserver.hover;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.OpenedFile;

/**
 * Shared, pre-computed context passed to each {@link HoverModule}.
 *
 * @param <T> Type of {@link OpenedFile} hovered on.
 * @param file The file hovered on.
 * @param hoveredTokenNode The token node at the hovered position.
 */
public record HoverContext<T extends OpenedFile>(T file, AstNode hoveredTokenNode) {

  /**
   * Get the node hovered on, i.e. the parent of the hovered token node.
   *
   * @return Hovered node.
   */
  public AstNode getHoveredNode() {
    return this.hoveredTokenNode().getParent();
  }
}
