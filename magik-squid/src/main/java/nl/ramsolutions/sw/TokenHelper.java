package nl.ramsolutions.sw;

import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.Trivia;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/** Helper for {@link Token}s. */
public final class TokenHelper {

  private TokenHelper() {}

  /**
   * Sets a new value for the token.
   *
   * @param newValue The new value to set for the token.
   */
  public static void setValue(final Token token, final String newValue) {
    try {
      final Field field = Token.class.getDeclaredField("value");
      field.setAccessible(true);
      field.set(token, newValue);
    } catch (final ReflectiveOperationException exception) {
      throw new RuntimeException("Failed to update token value", exception);
    }
  }

  /**
   * Sets a new original value for the token.
   *
   * @param newValue The new value to set for the token.
   */
  public static void setOriginalValue(final Token token, final String newValue) {
    try {
      final Field field = Token.class.getDeclaredField("originalValue");
      field.setAccessible(true);
      field.set(token, newValue);
    } catch (final ReflectiveOperationException exception) {
      throw new RuntimeException("Failed to update token original value", exception);
    }
  }

  /**
   * Set the URI of a token.
   *
   * @param token Token to update.
   * @param newUri New URI to set.
   */
  public static void setUri(final Token token, final URI newUri) {
    try {
      final Field fieldUri = token.getClass().getDeclaredField("uri");
      fieldUri.setAccessible(true); // NOSONAR
      fieldUri.set(token, newUri); // NOSONAR
    } catch (final ReflectiveOperationException exception) {
      throw new RuntimeException("Failed to update token URI", exception);
    }
  }

  /**
   * Set the line number of a token.
   *
   * @param token Token to update.
   * @param newLine Line number to set.
   */
  public static void setLine(final Token token, final int newLine) {
    try {
      final Field fieldLine = token.getClass().getDeclaredField("line");
      fieldLine.setAccessible(true); // NOSONAR
      fieldLine.set(token, newLine); // NOSONAR
    } catch (final ReflectiveOperationException exception) {
      throw new RuntimeException("Failed to update token line", exception);
    }
  }

  /**
   * Set the column number of a token.
   *
   * @param token Token to update.
   * @param newColumn Column number to set.
   */
  public static void setColumn(final Token token, final int newColumn) {
    try {
      final Field fieldColumn = token.getClass().getDeclaredField("column");
      fieldColumn.setAccessible(true); // NOSONAR
      fieldColumn.set(token, newColumn); // NOSONAR
    } catch (final ReflectiveOperationException exception) {
      throw new RuntimeException("Failed to update token column", exception);
    }
  }

  /**
   * Deep clone a token.
   *
   * @param token Token to clone.
   * @return Cloned token.
   */
  public static Token clone(final Token token) {
    final List<Trivia> clonedTrivias = token.getTrivia().stream().map(TriviaHelper::clone).toList();
    return Token.builder(token).setTrivia(clonedTrivias).build();
  }

  /**
   * Add {@link Trivia} before the {@code beforeTrivia}.
   *
   * @param token Token to which to add the trivia.
   * @param beforeTrivia Trivia to add before, or {@code null} to add as last.
   * @param newTrivia Trivia to add to the token.
   */
  public static void addTrivia(
      final Token token, final @Nullable Trivia beforeTrivia, final Trivia newTrivia) {
    final List<Trivia> tokenTrivias = token.getTrivia();
    final List<Trivia> newTrivias = new ArrayList<>(tokenTrivias);
    if (beforeTrivia != null) {
      final int indexOfTrivia = token.getTrivia().indexOf(beforeTrivia);
      if (indexOfTrivia == -1) {
        throw new IllegalArgumentException("beforeTrivia not found in token");
      }

      newTrivias.add(indexOfTrivia, newTrivia);
    } else {
      newTrivias.add(newTrivia);
    }

    try {
      final Field fieldTrivia = token.getClass().getDeclaredField("trivia");
      fieldTrivia.setAccessible(true); // NOSONAR
      fieldTrivia.set(token, newTrivias); // NOSONAR
    } catch (final ReflectiveOperationException exception) {
      throw new RuntimeException("Failed to update token line", exception);
    }
  }

  /**
   * Remove a specific {@link Trivia} from a {@link Token}.
   *
   * <p>Note that this does not update any lines/columns from any of the remaining {@link Trivia}
   * {@link Token}s, or the {@link Token} itself.
   *
   * @param token Token from which to remove the trivia.
   * @param trivia Trivia to remove from the token.
   */
  public static void removeTrivia(final Token token, final Trivia trivia) {
    final List<Trivia> tokenTrivia = token.getTrivia();
    if (!tokenTrivia.contains(trivia)) {
      throw new IllegalStateException("Trivia not found in token trivia");
    }

    // Remove the specified trivia from the existing trivia.
    final List<Trivia> newTrivias = tokenTrivia.stream().filter(t -> !t.equals(trivia)).toList();

    try {
      final Field fieldTrivia = token.getClass().getDeclaredField("trivia");
      fieldTrivia.setAccessible(true); // NOSONAR
      fieldTrivia.set(token, newTrivias); // NOSONAR
    } catch (final ReflectiveOperationException exception) {
      throw new RuntimeException("Failed to update token line", exception);
    }
  }
}
