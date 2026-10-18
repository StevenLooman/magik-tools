package nl.ramsolutions.sw.magik.analysis.definitions;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.time.Instant;
import java.util.Objects;
import nl.ramsolutions.sw.magik.Location;

/** Message handler use definition: a message handler which uses another message handler. */
public class MessageHandlerUseDefinition extends MagikDefinition {

  private final String handlerName;
  private final String usedHandlerName;

  /**
   * Constructor.
   *
   * @param location Location.
   * @param timestamp Timestamp.
   * @param moduleName Name of the module this definition resides in.
   * @param node Node.
   * @param handlerName Name of the message handler.
   * @param usedHandlerName Name of the message handler which is used.
   */
  public MessageHandlerUseDefinition(
      final @Nullable Location location,
      final @Nullable Instant timestamp,
      final @Nullable String moduleName,
      final @Nullable AstNode node,
      final String handlerName,
      final String usedHandlerName) {
    this(location, timestamp, moduleName, node, handlerName, usedHandlerName, Provenance.UNKNOWN);
  }

  /**
   * Constructor.
   *
   * @param location Location.
   * @param timestamp Timestamp.
   * @param moduleName Name of the module this definition resides in.
   * @param node Node.
   * @param handlerName Name of the message handler.
   * @param usedHandlerName Name of the message handler which is used.
   * @param provenance Provenance.
   */
  public MessageHandlerUseDefinition(
      final @Nullable Location location,
      final @Nullable Instant timestamp,
      final @Nullable String moduleName,
      final @Nullable AstNode node,
      final String handlerName,
      final String usedHandlerName,
      final Provenance provenance) {
    super(location, timestamp, moduleName, null, node, provenance);
    this.handlerName = handlerName;
    this.usedHandlerName = usedHandlerName;
  }

  @Override
  public String getName() {
    return this.handlerName;
  }

  public String getHandlerName() {
    return this.handlerName;
  }

  public String getUsedHandlerName() {
    return this.usedHandlerName;
  }

  @Override
  public MessageHandlerUseDefinition getBareDefinition() {
    return new MessageHandlerUseDefinition(
        this.getLocation(),
        this.getTimestamp(),
        this.getModuleName(),
        null,
        this.handlerName,
        this.usedHandlerName,
        this.getProvenance());
  }

  @Override
  public MessageHandlerUseDefinition withProvenance(final Provenance provenance) {
    return new MessageHandlerUseDefinition(
        this.getLocation(),
        this.getTimestamp(),
        this.getModuleName(),
        this.getNode(),
        this.handlerName,
        this.usedHandlerName,
        provenance);
  }

  @Override
  public String toString() {
    return "%s@%s(%s, %s)"
        .formatted(
            this.getClass().getName(),
            Integer.toHexString(this.hashCode()),
            this.getHandlerName(),
            this.getUsedHandlerName());
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        this.getLocation(), this.getModuleName(), this.handlerName, this.usedHandlerName);
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

    final MessageHandlerUseDefinition other = (MessageHandlerUseDefinition) obj;
    return Objects.equals(other.getLocation(), this.getLocation())
        && Objects.equals(other.getModuleName(), this.getModuleName())
        && Objects.equals(other.handlerName, this.handlerName)
        && Objects.equals(other.usedHandlerName, this.usedHandlerName);
  }
}
