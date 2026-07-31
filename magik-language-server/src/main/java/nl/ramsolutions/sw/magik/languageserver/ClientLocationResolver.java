package nl.ramsolutions.sw.magik.languageserver;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.SourcePathResolver;

/**
 * Prepares definition/reference locations for the editor: expands any {@code $NAME} in each
 * location's URI (so a class-info location recorded against {@code $SMALLWORLD_GIS} becomes an
 * openable path), and removes the range-less class-info duplicate of a definition that another
 * source also provides with a real range.
 *
 * <p>It drops a range-less location only when a ranged location exists for the same (resolved) URI.
 * It never merges two ranged locations at one URI, since {@code references} legitimately returns
 * many hits in a single file at different ranges.
 */
public final class ClientLocationResolver {

  private ClientLocationResolver() {}

  /**
   * Resolve and dedup locations for the client.
   *
   * @param locations The locations to prepare.
   * @param sourcePathResolver Configured source-path resolver (env-variable expansion + prefix
   *     rewrites).
   * @return Resolved, deduped locations in first-seen order.
   */
  public static List<Location> resolveAndDedup(
      final List<Location> locations, final SourcePathResolver sourcePathResolver) {
    final List<Location> resolved =
        locations.stream()
            .map(
                location ->
                    new Location(sourcePathResolver.expand(location.getUri()), location.getRange()))
            .toList();

    final Set<URI> urisWithRange =
        resolved.stream()
            .filter(location -> location.getRange() != null)
            .map(Location::getUri)
            .collect(Collectors.toSet());

    final Set<Location> deduped = new LinkedHashSet<>();
    for (final Location location : resolved) {
      if (location.getRange() == null && urisWithRange.contains(location.getUri())) {
        // Range-less class-info duplicate of a location another source provides with a real range.
        continue;
      }
      deduped.add(location);
    }
    return List.copyOf(deduped);
  }
}
