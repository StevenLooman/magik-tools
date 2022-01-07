package nl.ramsolutions.sw.magik.lint;

import java.net.URI;
import java.util.Map;
import nl.ramsolutions.sw.magik.MagikFile;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test LintInstructionsHandler.
 */
@SuppressWarnings("checkstyle:MagicNumber")
 class LintInstructionsHandlerTest {

    private LintInstructionsHandler getInstructions(String code) {
        final URI uri = URI.create("tests://unittest");
        final MagikFile magikFile = new MagikFile(uri, code);
        return new LintInstructionsHandler(magikFile);
    }

    @Test
    void testReadGlobalScopeInstruction() {
        final String code = ""
            + "# mlint: a=test1\n"
            + "_method a.b\n"
            + "    write(1)\n"
            + "_endmethod";
        final LintInstructionsHandler instructionsHandler = this.getInstructions(code);

        final Map<String, String> instructionsGlobal = instructionsHandler.getInstructionsInScope(1, 0);
        assertThat(instructionsGlobal).containsExactly(Map.entry("a", "test1"));

        final Map<String, String> instructionsMethod = instructionsHandler.getInstructionsInScope(3, 0);
        assertThat(instructionsMethod).containsExactly(Map.entry("a", "test1"));
    }

    @Test
    void testReadMethodScopeInstruction() {
        final String code = ""
            + "_method a.b\n"
            + "    # mlint: b=test2\n"
            + "    write(1)\n"
            + "_endmethod";
        final LintInstructionsHandler instructionsHandler = this.getInstructions(code);

        final Map<String, String> instructionsGlobal = instructionsHandler.getInstructionsInScope(1, 0);
        assertThat(instructionsGlobal).isEmpty();

        final Map<String, String> instructionsMethod = instructionsHandler.getInstructionsInScope(3, 0);
        assertThat(instructionsMethod).containsExactly(Map.entry("b", "test2"));
    }

    @Test
    void testReadCombinedScopeInstruction() {
        final String code = ""
            + "# mlint: a=test1\n"
            + "_method a.b\n"
            + "    # mlint: b=test2\n"
            + "    write(1)\n"
            + "_endmethod";
        final LintInstructionsHandler instructionsHandler = this.getInstructions(code);

        final Map<String, String> instructionsGlobal = instructionsHandler.getInstructionsInScope(1, 0);
        assertThat(instructionsGlobal).containsExactly(Map.entry("a", "test1"));

        final Map<String, String> instructionsMethod = instructionsHandler.getInstructionsInScope(4, 0);
        assertThat(instructionsMethod).containsExactly(
            Map.entry("a", "test1"),
            Map.entry("b", "test2"));
    }

    @Test
    void testReadLineInstruction() {
        final String code = ""
            + "_method a.b\n"
            + "    write(1) # mlint: c=test3\n"
            + "_endmethod";
        final LintInstructionsHandler instructionsHandler = this.getInstructions(code);

        final Map<String, String> instructionsGlobal = instructionsHandler.getInstructionsInScope(1, 0);
        assertThat(instructionsGlobal).isEmpty();

        final Map<String, String> instructionsMethod = instructionsHandler.getInstructionsInScope(2, 0);
        assertThat(instructionsMethod).isEmpty();

        final Map<String, String> instructions = instructionsHandler.getInstructionsAtLine(2);
        assertThat(instructions).containsExactly(Map.entry("c", "test3"));
    }

}
