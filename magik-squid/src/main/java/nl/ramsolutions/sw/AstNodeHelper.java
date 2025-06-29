package nl.ramsolutions.sw;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.Trivia;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/** Helper for {@link AstNode}s. */
public final class AstNodeHelper {

  private AstNodeHelper() {}

  /**
   * Recursively update URI for AstNode/Tokens.
   *
   * @param node Node to start at.
   * @param newUri New URI to set.
   */
  public static void setUri(final AstNode node, final URI newUri) {
    final Token token = node.getToken();
    if (token != null) {
      TokenHelper.setUri(token, newUri);
    }

    node.getChildren().forEach(childNode -> AstNodeHelper.setUri(childNode, newUri));
  }

  /**
   * Set the type of an {@link AstNode}.
   *
   * @param node Node to update.
   * @param newType Type to set.
   */
  public static void setType(final AstNode node, final AstNodeType newType) {
    try {
      final Field fieldType = AstNode.class.getDeclaredField("type");
      fieldType.setAccessible(true);
      fieldType.set(node, newType);
    } catch (final ReflectiveOperationException exception) {
      throw new RuntimeException("Failed to update AstNode type", exception);
    }
  }

  /**
   * Set the name of an AstNode.
   *
   * @param node Node to update.
   * @param newName New name to set.
   */
  public static void setName(final AstNode node, final String newName) {
    try {
      final Field fieldName = AstNode.class.getDeclaredField("name");
      fieldName.setAccessible(true);
      fieldName.set(node, newName);
    } catch (final ReflectiveOperationException exception) {
      throw new RuntimeException("Failed to update AstNode name", exception);
    }
  }

  /**
   * Deep clone an AstNode and all of its children, including its {@link Trivia}s and {@link
   * Token}s.
   *
   * @param node Node to clone.
   * @return Cloned AstNode.
   */
  public static AstNode clone(final AstNode node) {
    final Map<Token, Token> tokenMapping = new HashMap<>();
    return AstNodeHelper.clone(node, tokenMapping);
  }

  private static AstNode clone(final AstNode node, final Map<Token, Token> tokenMapping) {
    final AstNodeType type = node.getType();
    final String name = node.getName();
    final Token token = node.getToken();
    final Token newToken;
    if (token == null) {
      newToken = null;
    } else if (tokenMapping.containsKey(token)) {
      newToken = tokenMapping.get(token);
    } else {
      newToken = TokenHelper.clone(token);
      tokenMapping.put(token, newToken);
    }
    final AstNode newNode = new AstNode(type, name, newToken);

    // Add cloned children to newNode.
    node.getChildren().stream()
        .map(childNode -> AstNodeHelper.clone(childNode, tokenMapping))
        .forEach(newNode::addChild);

    // Set fromIndex/toIndex.
    final int fromIndex = node.getFromIndex();
    newNode.setFromIndex(fromIndex);
    final int toIndex = node.getToIndex();
    newNode.setToIndex(toIndex);

    return newNode;
  }
}
