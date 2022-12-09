package nl.ramsolutions.sw.magik.debugadapter;

import java.nio.file.Path;
import java.util.Map;

/**
 * Path mapper, maps paths if a mapping is found.
 */
public final class PathMapper {

    private final Map<Path, Path> mapping;

    /**
     * Constructor.
     * @param mapping Path mappings.
     */
    public PathMapper(final Map<Path, Path> mapping) {
        this.mapping = mapping;
    }

    /**
     * Apply mapping, if a mapping is found. Otherwise return path itself.
     * @param path Path to map.
     * @return Mapped path, or original if no mapping was found.
     */
    public Path applyMapping(final Path path) {
        final Map.Entry<Path, Path> pathMap = this.mapping.entrySet().stream()
            .filter(entry -> path.startsWith(entry.getKey()))
            .findAny()
            .orElse(null);
        if (pathMap == null) {
            return path;
        }

        final Path from = pathMap.getKey();
        final Path to = pathMap.getValue();
        final Path relative = from.relativize(path);
        return to.resolve(relative);
    }

}
