package nl.ramsolutions.sw.magik;

import java.net.URI;

/** File event. Records a change to a file or directory. */
public class FileEvent {

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
}
