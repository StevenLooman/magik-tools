package nl.ramsolutions.sw.magik.analysis;

import com.sonar.sslr.api.Token;
import java.util.Comparator;
import java.util.Objects;

/**
 * Position in a file.
 */
public class Position implements Comparable<Position> {

    private static final String NEWLINE_REGEXP = "(?:\\n|\\r\\n|\\r)";

    /**
     * Comparator for {@link Position}s.
     */
    public class PositionComparator implements Comparator<Position> {

        @Override
        public int compare(final Position position0, final Position position1) {
            if (position0.line != position1.line) {
                return position0.line - position1.line;
            }

            return position0.column - position1.column;
        }

    }

    private final int line;
    private final int column;

    /**
     * Constructor.
     * @param line Line.
     * @param column Column.
     */
    public Position(final int line, final int column) {
        this.line = line;
        this.column = column;
    }

    /**
     * Create a Position from Token start.
     * @param token Token.
     * @return Position from Token start.
     */
    public static Position fromTokenStart(final Token token) {
        final int line = token.getLine();
        final int column = token.getColumn();
        return new Position(line, column);
    }

    /**
     * Create a Position from Token end.
     * @param token Token.
     * @return Position from Token end.
     */
    public static Position fromTokenEnd(final Token token) {
        final String tokenValue = token.getOriginalValue();
        final Character lastChar = !tokenValue.isEmpty()
            ? tokenValue.charAt(tokenValue.length() - 1)
            : null;

        final String[] lines = tokenValue.split(NEWLINE_REGEXP);
        final String lastLine = lines[lines.length - 1];
        final int line = lines.length == 1
            ? token.getLine()
            : token.getLine() + lines.length - 1;
        final int column = lines.length == 1
            ? token.getColumn() + lastLine.length()
            : lastLine.length();

        // Last char is a \r or \r char? Then skip to next line.
        if (lastChar != null
            && (lastChar == '\n' || lastChar == '\r')) {
            return new Position(line + 1, 0);
        }
        return new Position(line, column);
    }

    /**
     * Get the line.
     * @return Line.
     */
    public int getLine() {
        return this.line;
    }

    /**
     * Get the column.
     * @return Column.
     */
    public int getColumn() {
        return this.column;
    }

    public boolean isBeforeRange(final Range range) {
        return this.compareTo(range.getStartPosition()) < 0;
    }

    public boolean isAfterRange(final Range range) {
        return this.compareTo(range.getEndPosition()) > 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.line, this.column);
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

        final Position other = (Position) obj;
        return this.compareTo(other) == 0;
    }

    @Override
    public int compareTo(final Position other) {
        if (this.line != other.line) {
            return this.line - other.line;
        }

        return this.column - other.column;
    }

    @Override
    public String toString() {
        return String.format(
            "%s@%s(%s, %s)",
            this.getClass().getName(), Integer.toHexString(this.hashCode()),
            this.line, this.column);
    }

}
