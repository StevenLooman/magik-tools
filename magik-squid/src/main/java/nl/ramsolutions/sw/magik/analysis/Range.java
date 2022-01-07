package nl.ramsolutions.sw.magik.analysis;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;

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

}
