package nl.ramsolutions.sw.checks;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.util.List;
import nl.ramsolutions.sw.OpenedFile;

/** Base for Magik/module.def/product.def code checks. */
public interface Check {

  List<Issue> scanFileForIssues(final OpenedFile openedFile);

  void setHolder(final CheckHolder holder);

  @CheckForNull
  CheckHolder getHolder();

  void setParameter(final String name, final Object value) throws IllegalAccessException;
}
