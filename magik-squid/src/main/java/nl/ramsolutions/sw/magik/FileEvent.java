package nl.ramsolutions.sw.magik;

import java.net.URI;
import java.nio.file.Path;
import java.util.Objects;

/** File event. Records a change to a file or directory. */
public class FileEvent {

  /** File change type. */
  public enum FileChangeType {
    CREATED,
    CHANGED,
    DELETED;
  }

  final URI uri;
  final FileChangeType fileChangeType;

  public FileEvent(final URI uri, final FileChangeType fileChangeType) {
    this.uri = uri;
    this.fileChangeType = fileChangeType;
  }

  public URI getUri() {
    return this.uri;
  }

  public FileChangeType getFileChangeType() {
    return this.fileChangeType;
  }

  public Path getPath() {
    return Path.of(this.uri);
  }

  @Override
  public String toString() {
    return String.format(
        "%s@%s(%s, %s)",
        this.getClass().getName(),
        Integer.toHexString(this.hashCode()),
        this.getUri(),
        this.getFileChangeType());
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.uri, this.fileChangeType);
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

    final FileEvent other = (FileEvent) obj;
    return Objects.equals(this.uri, other.uri)
        && Objects.equals(this.fileChangeType, other.fileChangeType);
  }
}
