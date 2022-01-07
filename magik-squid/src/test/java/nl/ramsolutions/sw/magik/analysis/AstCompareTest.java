package nl.ramsolutions.sw.magik.analysis;

import com.sonar.sslr.api.AstNode;
import java.util.EnumSet;
import nl.ramsolutions.sw.magik.parser.MagikParser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test AstCompare.
 */
class AstCompareTest {

    private AstNode parseMagik(final String code) {
        final MagikParser parser = new MagikParser();
        return parser.parseSafe(code);
    }

    @Test
    void testCompareEqualsRecursiveTrue() {
        final String code1 = "_true";
        final String code2 = "_true";
        final AstNode left = this.parseMagik(code1);
        final AstNode right = this.parseMagik(code2);

        final boolean equals = AstCompare.astNodeEqualsRecursive(left, right);
        assertThat(equals).isTrue();
    }

    @Test
    void testCompareEqualsRecursiveTrueFalse() {
        final String code1 = "_true";
        final String code2 = "_false";
        final AstNode left = this.parseMagik(code1);
        final AstNode right = this.parseMagik(code2);

        final boolean equals = AstCompare.astNodeEqualsRecursive(left, right);
        assertThat(equals).isFalse();
    }

    @Test
    void testCompareEqualsRecursiveAssignment() {
        final String code1 = "i << 10";
        final String code2 = "i << 10";
        final AstNode left = this.parseMagik(code1);
        final AstNode right = this.parseMagik(code2);

        final boolean equals = AstCompare.astNodeEqualsRecursive(left, right);
        assertThat(equals).isTrue();
    }

    @Test
    void testCompareEqualsRecursiveAssignmentIgnoreIdentifier() {
        final String code1 = "i << 10";
        final String code2 = "j << 10";
        final AstNode left = this.parseMagik(code1);
        final AstNode right = this.parseMagik(code2);

        final EnumSet<AstCompare.Flags> flags = EnumSet.of(AstCompare.Flags.IGNORE_IDENTIFIER_NAME);
        final boolean equals = AstCompare.astNodeEqualsRecursive(left, right, flags);
        assertThat(equals).isTrue();
    }

    @Test
    void testCompareEqualsRecursiveParenthesis() {
        final String code1 = "(_true)";
        final String code2 = "_true";
        final AstNode left = this.parseMagik(code1);
        final AstNode right = this.parseMagik(code2);

        final boolean equals = AstCompare.astNodeEqualsRecursive(left, right);
        assertThat(equals).isFalse();
    }

}
