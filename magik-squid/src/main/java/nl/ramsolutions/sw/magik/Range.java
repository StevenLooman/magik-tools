package nl.ramsolutions.sw.magik;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import java.util.Objects;

/**
 * Range containing a start position and an end position.
 */
public class Range {

    private final Position startPosition;
    private final Position endPosition;

    public Range(final Position startPosition, final Position endPosition) {
        this.startPosition = startPosition;
        this.endPosition = endPosition;
    }

    public Range(final AstNode node) {
        this(node.getToken());
    }

    public Range(final Token token) {
        this.startPosition = Position.fromTokenStart(token);
        this.endPosition = Position.fromTokenEnd(token);
    }

    public Position getStartPosition() {
        return this.startPosition;
    }

    public Position getEndPosition() {
        return this.endPosition;
    }

    /**
     * Extract range from full tree, i.e., all tokens.
     * @param node Node to extract {@link Range} from.
     * @return Range of the node tree.
     */
    public static Range fromTree(final AstNode node) {
        final Token firstToken = node.getToken();
        final Token lastToken = node.getLastToken();
        final Position startPosition = Position.fromTokenStart(firstToken);
        final Position endPosition = Position.fromTokenEnd(lastToken);
        return new Range(startPosition, endPosition);
    }

    @Override
    public String toString() {
        return String.format(
            "%s@%s(%s, %s)",
            this.getClass().getName(), Integer.toHexString(this.hashCode()),
            this.getStartPosition(), this.getEndPosition());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.startPosition, this.endPosition);
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

        final Range other = (Range) obj;
        return Objects.equals(this.startPosition, other.startPosition)
            && Objects.equals(this.endPosition, other.endPosition);
    }

}
