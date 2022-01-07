package nl.ramsolutions.sw.magik.typedchecks.checks;

import java.net.URI;
import java.util.List;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;
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
    protected List<MagikIssue> runCheck(final String code, final ITypeKeeper typeKeeper, final MagikTypedCheck check)
            throws IllegalArgumentException {
        URI uri = URI.create("tests://unittest");
        MagikTypedFile magikFile = new MagikTypedFile(uri, code, typeKeeper);
        List<MagikIssue> issues = check.scanFileForIssues(magikFile);
        return issues;
    }

}
