package nl.ramsolutions.sw.magik;

import java.net.URI;
import java.nio.file.Path;
import java.util.Map;
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

  private final Function<String, String> environment;
  private final Map<String, String> prefixMappings;

  /**
   * Constructor for a configured resolver that owns its environment and prefix mappings, so callers
   * hold a single mapper rather than threading resolution config.
   *
   * @param environment Resolver mapping a variable name to its value (or {@code null} if unset).
   * @param prefixMappings Path-prefix rewrites (recorded prefix to replacement).
   */
  public SourcePathResolver(
      final Function<String, String> environment, final Map<String, String> prefixMappings) {
    this.environment = environment;
    this.prefixMappings = prefixMappings;
  }

  /**
   * Resolve a URI using this resolver's environment and prefix mappings.
   *
   * @param uri The (possibly placeholder-containing) file URI.
   * @return The resolved URI, or {@code uri} unchanged when there is nothing to rewrite or expand.
   */
  public URI expand(final URI uri) {
    return SourcePathResolver.expand(uri, this.environment, this.prefixMappings);
  }

  /**
   * Expand {@code $NAME} occurrences in the URI's path from the given resolver.
   *
   * @param uri The (possibly placeholder-containing) file URI.
   * @param environment Resolver mapping a variable name to its value (or {@code null} if unset).
   * @return The expanded URI, or {@code uri} unchanged when there is nothing to expand.
   */
  public static URI expand(final URI uri, final Function<String, String> environment) {
    return SourcePathResolver.expand(uri, environment, Map.of());
  }

  /**
   * Rewrite the URI's path by a configured prefix mapping, then expand {@code $NAME} occurrences.
   *
   * <p>Prefix mappings rewrite a raw, installation-specific absolute path (e.g. a {@code
   * C:/projects/...} path baked into class-info by a Windows build) into a local path or a logical
   * ({@code $NAME}), the longest matching prefix winning. Any resulting {@code $NAME} is then
   * expanded from {@code environment}, so a mapping may chain into a logical.
   *
   * @param uri The (possibly placeholder-containing) file URI.
   * @param environment Resolver mapping a variable name to its value (or {@code null} if unset).
   * @param prefixMappings Path-prefix rewrites (recorded prefix to replacement).
   * @return The resolved URI, or {@code uri} unchanged when there is nothing to rewrite or expand.
   */
  public static URI expand(
      final URI uri,
      final Function<String, String> environment,
      final Map<String, String> prefixMappings) {
    final String path = uri.getPath();
    if (path == null) {
      return uri;
    }

    final String rewritten = SourcePathResolver.applyPrefixMappings(path, prefixMappings);
    final String expanded = SourcePathResolver.expandVariables(rewritten, environment);
    if (expanded.equals(path)) {
      // Nothing changed: return the original URI so Path.of(uri) stays correct across platforms
      // (a file URI's raw path -- e.g. "/C:/..." on Windows -- is not itself a valid OS path).
      return uri;
    }

    return SourcePathResolver.toOsPath(expanded).toUri();
  }

  /**
   * Replace the longest matching prefix in {@code path}. A file URI's path is rooted ({@code
   * "/..."}); a configured prefix may or may not include that leading slash, so both forms are
   * tried.
   */
  private static String applyPrefixMappings(
      final String path, final Map<String, String> prefixMappings) {
    String matchedPrefix = null;
    String replacement = null;
    for (final Map.Entry<String, String> entry : prefixMappings.entrySet()) {
      for (final String candidate : new String[] {entry.getKey(), "/" + entry.getKey()}) {
        if (path.startsWith(candidate)
            && (matchedPrefix == null || candidate.length() > matchedPrefix.length())) {
          matchedPrefix = candidate;
          replacement = entry.getValue();
        }
      }
    }

    if (matchedPrefix == null) {
      return path;
    }

    return replacement + path.substring(matchedPrefix.length());
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
