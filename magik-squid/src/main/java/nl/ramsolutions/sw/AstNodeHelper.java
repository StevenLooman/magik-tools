package nl.ramsolutions.sw;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import java.lang.reflect.Field;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Helper for {@link AstNode}s. */
public final class AstNodeHelper {

  private static final Logger LOGGER = LoggerFactory.getLogger(AstNodeHelper.class);

  private AstNodeHelper() {}

  /**
   * Recusrively update URI for AstNode/Tokens.
   *
   * @param node Node to start at.
   * @param newUri New URI to set.
   */
  public static void updateUri(final AstNode node, final URI newUri) {
    final Token token = node.getToken();
    if (token != null) {
      try {
        final Field fieldUri = token.getClass().getDeclaredField("uri");
        fieldUri.setAccessible(true); // NOSONAR
        fieldUri.set(token, newUri); // NOSONAR
      } catch (final ReflectiveOperationException exception) {
        LOGGER.error(exception.getMessage(), exception);
      }
    }

    node.getChildren().forEach(childNode -> AstNodeHelper.updateUri(childNode, newUri));
  }
}
