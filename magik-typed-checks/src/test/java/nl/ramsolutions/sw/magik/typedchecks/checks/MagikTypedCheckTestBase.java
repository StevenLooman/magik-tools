package nl.ramsolutions.sw.magik.typedchecks.checks;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import nl.ramsolutions.sw.magik.typedchecks.MagikTypedCheck;

/**
 * Base class for MagikTypedCheck tests.
 */
public class MagikTypedCheckTestBase {

    /**
     * Run check on code.
     * @param code Code.
     * @param check Check to run.
     * @return List with issues.
     * @throws IllegalArgumentException -
     */
    protected List<MagikIssue> runCheck(
            final String code,
            final IDefinitionKeeper definitionKeeper,
            final MagikTypedCheck check)
            throws IllegalArgumentException {
        final URI uri = URI.create("tests://unittest");
        final MagikTypedFile magikFile = new MagikTypedFile(uri, code, definitionKeeper);
        return check.scanFileForIssues(magikFile);
    }

    /**
     * Run check on code.
     * @param path Path to file.
     * @param definitionKeeper {@link IDefinitionKeeper} to use.
     * @param check Check to run.
     * @return List with issues.
     * @throws IllegalArgumentException -
     * @throws IOException
     */
    protected List<MagikIssue> runCheck(
            final Path path,
            final IDefinitionKeeper definitionKeeper,
            final MagikTypedCheck check)
            throws IllegalArgumentException, IOException {
        final MagikTypedFile magikFile = new MagikTypedFile(path, definitionKeeper);
        return check.scanFileForIssues(magikFile);
    }

}
