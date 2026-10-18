package nl.ramsolutions.sw.message;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import nl.ramsolutions.sw.IDefinition;
import nl.ramsolutions.sw.magik.Location;

/** Smallworld message file. */
public class MessageFileDefinition implements IDefinition {

  private final @Nullable Location location;
  private final @Nullable Instant timestamp;
  private final @Nullable String moduleName;
  private final String handlerName;
  private final @Nullable String language;
  private final List<MessageDefinition> messages;

  /**
   * Constructor.
   *
   * @param location {@link Location} of the message file.
   * @param timestamp Timestamp of the message file.
   * @param moduleName Name of the module the message file is in.
   * @param handlerName Name of the message handler the messages are part of.
   * @param language Language of the message file, as determined by its directory.
   * @param messages Messages in the file, in order of the file.
   */
  public MessageFileDefinition(
      final @Nullable Location location,
      final @Nullable Instant timestamp,
      final @Nullable String moduleName,
      final String handlerName,
      final @Nullable String language,
      final List<MessageDefinition> messages) {
    this.location = location;
    this.timestamp = timestamp;
    this.moduleName = moduleName;
    this.handlerName = handlerName;
    this.language = language;
    this.messages = List.copyOf(messages);
  }

  @Override
  @CheckForNull
  public Location getLocation() {
    return this.location;
  }

  @Override
  @CheckForNull
  public Instant getTimestamp() {
    return this.timestamp;
  }

  @Override
  public IDefinition getBareDefinition() {
    return this;
  }

  @CheckForNull
  public String getModuleName() {
    return this.moduleName;
  }

  /**
   * Get the name of the message handler the messages in this file are part of: the name of the
   * file, without its extension.
   *
   * @return Name of the message handler.
   */
  public String getHandlerName() {
    return this.handlerName;
  }

  /**
   * Get the language of the file, as determined by its directory.
   *
   * @return Language, or null if it cannot be determined.
   */
  @CheckForNull
  public String getLanguage() {
    return this.language;
  }

  /**
   * Get the messages in this file, in order of the file.
   *
   * @return Messages in this file.
   */
  public List<MessageDefinition> getMessages() {
    return Collections.unmodifiableList(this.messages);
  }

  /**
   * Get the messages in this file with the given id.
   *
   * @param id Id of the message, see {@link MessageSymbol#nameOf(String)}.
   * @return Messages with the id, in order of the file.
   */
  public List<MessageDefinition> getMessages(final String id) {
    return this.messages.stream().filter(message -> message.getId().equals(id)).toList();
  }

  @Override
  public String toString() {
    return "%s@%s(%s, %s, %s)"
        .formatted(
            this.getClass().getName(),
            Integer.toHexString(this.hashCode()),
            this.getHandlerName(),
            this.getLanguage(),
            this.getModuleName());
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

    final MessageFileDefinition other = (MessageFileDefinition) obj;
    return Objects.equals(other.location, this.location)
        && Objects.equals(other.moduleName, this.moduleName)
        && Objects.equals(other.handlerName, this.handlerName)
        && Objects.equals(other.language, this.language)
        && Objects.equals(other.messages, this.messages);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        this.location, this.moduleName, this.handlerName, this.language, this.messages);
  }
}
