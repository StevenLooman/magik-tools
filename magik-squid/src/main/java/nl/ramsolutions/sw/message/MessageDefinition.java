package nl.ramsolutions.sw.message;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import nl.ramsolutions.sw.magik.Location;

/** A message in a message file. */
public class MessageDefinition {

  private static final int MAX_PARAMETER_DIGITS = 10;
  private static final Pattern PARAMETER_PATTERN =
      Pattern.compile("#(?:([1-9])|[@h]([0-9]+)|.)", Pattern.DOTALL);

  private final @Nullable Location location;
  private final String id;
  private final @Nullable String language;
  private final boolean removal;
  private final String text;
  private final List<Integer> parameters;

  /**
   * Constructor.
   *
   * @param location {@link Location} of the message.
   * @param id Id of the message, see {@link MessageSymbol#nameOf(String)}.
   * @param language Language given at the message itself, if any.
   * @param removal True if this removes the message, instead of defining it.
   * @param text Text of the message, where lines are separated by a newline.
   * @param parameters Numbers of the parameters used in the text, sorted and without duplicates.
   */
  public MessageDefinition(
      final @Nullable Location location,
      final String id,
      final @Nullable String language,
      final boolean removal,
      final String text,
      final List<Integer> parameters) {
    this.location = location;
    this.id = id;
    this.language = language;
    this.removal = removal;
    this.text = text;
    this.parameters = List.copyOf(parameters);
  }

  /**
   * Get the numbers of the parameters used in the text of a message: {@code #1}..{@code #9}, and
   * {@code #@1}/{@code #h1} with any number.
   *
   * @param text Text of a message.
   * @return Numbers of parameters, sorted and without duplicates.
   */
  public static List<Integer> parametersOf(final String text) {
    final TreeSet<Integer> parameters = new TreeSet<>();
    final Matcher matcher = PARAMETER_PATTERN.matcher(text);
    while (matcher.find()) {
      final String digits = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
      if (digits != null) {
        parameters.add(MessageDefinition.parameterNumberOf(digits));
      }
    }
    return List.copyOf(parameters);
  }

  /**
   * Get the number of a parameter from its digits.
   *
   * @param digits Digits of the parameter, such as {@code 12} for {@code #@12}.
   * @return Number of the parameter.
   */
  public static int parameterNumberOf(final String digits) {
    final String significantDigits = digits.replaceFirst("^0+(?=.)", "");
    if (significantDigits.length() > MAX_PARAMETER_DIGITS) {
      return Integer.MAX_VALUE;
    }

    final long number = Long.parseLong(significantDigits);
    return (int) Math.min(number, Integer.MAX_VALUE);
  }

  @CheckForNull
  public Location getLocation() {
    return this.location;
  }

  public String getId() {
    return this.id;
  }

  /**
   * Get the language given at the message itself.
   *
   * @return Language given at the message, if any.
   */
  @CheckForNull
  public String getLanguage() {
    return this.language;
  }

  /**
   * Test if this removes the message, instead of defining it.
   *
   * @return True if this removes the message.
   */
  public boolean isRemoval() {
    return this.removal;
  }

  public String getText() {
    return this.text;
  }

  /**
   * Get the numbers of the parameters used in the text, e.g. 1 and 3 for the text {@code #1 of
   * #@3}.
   *
   * @return Numbers of parameters, sorted and without duplicates.
   */
  public List<Integer> getParameters() {
    return Collections.unmodifiableList(this.parameters);
  }

  /**
   * Get the highest number of the parameters used in the text.
   *
   * @return Highest parameter number, or 0 if the text has no parameters.
   */
  public int getParameterCount() {
    return this.parameters.isEmpty() ? 0 : this.parameters.get(this.parameters.size() - 1);
  }

  @Override
  public String toString() {
    return "%s@%s(%s, %s)"
        .formatted(
            this.getClass().getName(),
            Integer.toHexString(this.hashCode()),
            this.getId(),
            this.getLanguage());
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj == null) {
      return false;
    }

    if (this.getClass() != obj.getClass()) {
      return false;
    }

    final MessageDefinition other = (MessageDefinition) obj;
    return Objects.equals(other.location, this.location)
        && Objects.equals(other.id, this.id)
        && Objects.equals(other.language, this.language)
        && other.removal == this.removal
        && Objects.equals(other.text, this.text)
        && Objects.equals(other.parameters, this.parameters);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        this.location, this.id, this.language, this.removal, this.text, this.parameters);
  }
}
