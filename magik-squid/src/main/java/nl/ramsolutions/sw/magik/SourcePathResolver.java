package nl.ramsolutions.sw.magik;

import java.net.URI;
import java.nio.file.Path;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Expands environment variables ({@code $NAME}) in a file URI's path, so a source location recorded
 * against a Smallworld logical (e.g. {@code $SMALLWORLD_GIS}) resolves to a real file when the
 * installation is present. Resolution is done at the point of consumption; the stored location
 * keeps the literal, installation-independent URI.
 */
public final class SourcePathResolver {

  private static final Pattern ENVIRONMENT_VARIABLE = Pattern.compile("\\$(\\w+)");

  // A file URI's path is rooted ("/..."); on Windows an expanded logical is a drive-letter path, so
  // the raw "/C:/..." (or "/C:\\...") must lose its leading slash to be a valid OS path.
  private static final Pattern ROOTED_WINDOWS_DRIVE = Pattern.compile("^/([A-Za-z]:[\\\\/].*)$");

  private SourcePathResolver() {}

  /**
   * Expand {@code $NAME} occurrences in the URI's path from the given resolver.
   *
   * @param uri The (possibly placeholder-containing) file URI.
   * @param environment Resolver mapping a variable name to its value (or {@code null} if unset).
   * @return The expanded URI, or {@code uri} unchanged when there is nothing to expand.
   */
  public static URI expand(final URI uri, final Function<String, String> environment) {
    final String path = uri.getPath();
    if (path == null) {
      return uri;
    }

    final String expanded = SourcePathResolver.expandVariables(path, environment);
    if (expanded.equals(path)) {
      // Nothing expanded: return the original URI so Path.of(uri) stays correct across platforms
      // (a file URI's raw path -- e.g. "/C:/..." on Windows -- is not itself a valid OS path).
      return uri;
    }

    return SourcePathResolver.toOsPath(expanded).toUri();
  }

  private static Path toOsPath(final String expandedUriPath) {
    final Matcher driveLetter = SourcePathResolver.ROOTED_WINDOWS_DRIVE.matcher(expandedUriPath);
    if (driveLetter.matches()) {
      // "/C:\\sw\\..." -> "C:\\sw\\..."; Path.of normalizes mixed separators.
      return Path.of(driveLetter.group(1));
    }
    return Path.of(expandedUriPath);
  }

  private static String expandVariables(
      final String path, final Function<String, String> environment) {
    final Matcher matcher = SourcePathResolver.ENVIRONMENT_VARIABLE.matcher(path);
    final StringBuilder builder = new StringBuilder();
    while (matcher.find()) {
      final String value = environment.apply(matcher.group(1));
      matcher.appendReplacement(
          builder, Matcher.quoteReplacement(value != null ? value : matcher.group()));
    }
    matcher.appendTail(builder);
    return builder.toString();
  }
}
