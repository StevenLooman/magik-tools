package nl.ramsolutions.sw.magik.analysis;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import java.net.URI;
import java.nio.file.Path;
import java.util.Comparator;

/**
 * Location within a file.
 */
public class Location {

    /**
     * Location/Range comparator.
     */
    public static class LocationRangeComparator implements Comparator<Location> {

        @Override
        public int compare(final Location location0, final Location location1) {
            // Compare URI.
            final URI uri0 = location0.getUri();
            final URI uri1 = location1.getUri();
            if (!uri0.equals(uri1)) {
                return uri0.compareTo(uri1);
            }

            // compare start of range
            final Range range0 = location0.getRange();
            final Range range1 = location1.getRange();

            final Position position0 = range0.getStartPosition();
            final Position position1 = range1.getStartPosition();
            return position0.compareTo(position1);
        }

    }

    private final URI uri;
    private final Range range;

    /**
     * Constructor.
     * @param uri Path to file.
     * @param range Range in file.
     */
    public Location(final URI uri, final Range range) {
        this.uri = uri;
        this.range = range;
    }

    /**
     * Constructor from {@link AstNode}.
     * @param uri Path to file.
     * @param node {@link AstNode} to create {@link Location} from.
     */
    public Location(final URI uri, final AstNode node) {
        this.uri = uri;
        this.range = new Range(node);
    }

    /**
     * Constructor from {@link AstNode}.
     * @param uri Path to file.
     * @param token {@link Token} to create {@link Location} from.
     */
    public Location(final URI uri, final Token token) {
        this.uri = uri;
        this.range = new Range(token);
    }

    /**
     * Constructor, only providing a path.
     * @param uri Path to file.
     */
    public Location(final URI uri) {
        this.uri = uri;
        this.range = new Range(new Position(1, 0), new Position(1, 0));
    }

    /**
     * Get URI.
     * @return URI to file.
     */
    public URI getUri() {
        return this.uri;
    }

    /**
     * Get Range.
     * @return Range in file.
     */
    public Range getRange() {
        return this.range;
    }

    /**
     * Get path.
     * @return Path to file.
     */
    public Path getPath() {
        return Path.of(this.uri);
    }

}
