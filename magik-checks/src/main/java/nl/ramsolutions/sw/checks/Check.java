package nl.ramsolutions.sw.checks;

import java.util.List;
import nl.ramsolutions.sw.OpenedFile;

/** Base for Magik/module.def/product.def code checks. */
public interface Check {

  public List<Issue> scanFileForIssues(final OpenedFile openedFile);

  public void setHolder(final CheckHolder holder);

  public CheckHolder getHolder();

  public void setParameter(final String name, final Object value) throws IllegalAccessException;
}
