package nl.ramsolutions.sw.magik.languageserver.codeactions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import nl.ramsolutions.sw.magik.CodeAction;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.typedchecks.CheckList;
import nl.ramsolutions.sw.magik.typedchecks.MagikTypedCheck;
import nl.ramsolutions.sw.magik.typedchecks.MagikTypedCheckFixer;
import org.eclipse.lsp4j.CodeActionContext;

/**
 * Provide {@link CodeAction}s for {@link MagikTypedCheck}s.
 */
public class MagikTypedChecksCodeActionProvider {

    /**
     * Provide {@link CodeAction} for {@link MagikTypedCheck} checks.
     * @param magikFile {@link MagikTypedFile{} to check on.
     * @param range {@link Range} to get {@link CodeAction}s for.
     * @param context Context, not used currently.
     * @return List of {@link CodeAction}s.
     * @throws ReflectiveOperationException
     */
    public List<CodeAction> provideCodeActions(
            final MagikTypedFile magikFile,
            final Range range,
            final CodeActionContext context) throws ReflectiveOperationException {
        final List<CodeAction> codeActions = new ArrayList<>();
        for (final Entry<Class<?>, List<Class<?>>> entry : CheckList.getFixers().entrySet()) {
            final Class<?> checkClass = entry.getKey();
            final List<Class<?>> fixerClassses = entry.getValue();
            for (final Class<?> fixerClass : fixerClassses) {
                if (!this.isCheckEnabled(checkClass)) {
                    continue;
                }

                final MagikTypedCheckFixer fixer =
                    (MagikTypedCheckFixer) fixerClass.getDeclaredConstructor().newInstance();
                List<CodeAction> fixerCodeActions = fixer.provideCodeActions(magikFile, range);
                codeActions.addAll(fixerCodeActions);
            }
        }
        return codeActions;
    }

    private boolean isCheckEnabled(final Class<?> checkClass) {
        // TODO: Implement this.
        return true;
    }

}
