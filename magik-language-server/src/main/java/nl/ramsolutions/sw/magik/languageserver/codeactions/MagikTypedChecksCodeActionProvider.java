package nl.ramsolutions.sw.magik.languageserver.codeactions;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import nl.ramsolutions.sw.ConfigurationLocator;
import nl.ramsolutions.sw.magik.CodeAction;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.checks.MagikCheckHolder;
import nl.ramsolutions.sw.magik.checks.MagikChecksConfiguration;
import nl.ramsolutions.sw.magik.languageserver.MagikSettings;
import nl.ramsolutions.sw.magik.typedchecks.CheckList;
import nl.ramsolutions.sw.magik.typedchecks.MagikTypedCheck;
import nl.ramsolutions.sw.magik.typedchecks.MagikTypedCheckFixer;

/**
 * Provide {@link CodeAction}s for {@link MagikTypedCheck}s.
 */
public class MagikTypedChecksCodeActionProvider {

    /**
     * Provide {@link CodeAction} for {@link MagikTypedCheck} checks.
     * @param magikFile {@link MagikTypedFile{} to check on.
     * @param range {@link Range} to get {@link CodeAction}s for.
     * @return List of {@link CodeAction}s.
     * @throws ReflectiveOperationException
     * @throws IOException -
     */
    public List<CodeAction> provideCodeActions(
            final MagikTypedFile magikFile,
            final Range range) throws ReflectiveOperationException, IOException {
        final List<CodeAction> codeActions = new ArrayList<>();
        for (final Entry<Class<?>, List<Class<?>>> entry : CheckList.getFixers().entrySet()) {
            final Class<?> checkClass = entry.getKey();
            final List<Class<?>> fixerClassses = entry.getValue();
            for (final Class<?> fixerClass : fixerClassses) {
                if (!this.isCheckEnabled(magikFile, checkClass)) {
                    continue;
                }

                final MagikTypedCheckFixer fixer =
                    (MagikTypedCheckFixer) fixerClass.getDeclaredConstructor().newInstance();
                final List<CodeAction> fixerCodeActions = fixer.provideCodeActions(magikFile, range);
                codeActions.addAll(fixerCodeActions);
            }
        }
        return codeActions;
    }

    private boolean isCheckEnabled(final MagikFile magikFile, final Class<?> checkClass) throws IOException {
        final Path searchPath = Path.of(magikFile.getUri()).getParent();
        final Path configPath = MagikSettings.INSTANCE.getChecksOverrideSettingsPath() != null
            ? MagikSettings.INSTANCE.getChecksOverrideSettingsPath()
            : ConfigurationLocator.locateConfiguration(searchPath);
        final MagikChecksConfiguration config = configPath != null
            ? new MagikChecksConfiguration(configPath)
            : new MagikChecksConfiguration();
        final List<MagikCheckHolder> allChecks = config.getAllChecks();
        for (final MagikCheckHolder checkHolder : allChecks) {
            if (checkHolder.getCheckClass().equals(checkClass)) {
                return checkHolder.isEnabled();
            }
        }

        // Check not found, so not enabled.
        return false;
    }

}
