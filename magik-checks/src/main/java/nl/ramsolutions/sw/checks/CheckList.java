package nl.ramsolutions.sw.checks;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Base class for check lists. */
public abstract class CheckList<T extends Check, S extends CodeActionSupplier> {

  /**
   * Get the list of {@link Check}s.
   *
   * @return List of with {@link Check}s
   */
  public abstract List<Class<? extends T>> getChecks();

  /**
   * Get the list of {@link MagikCheck}s, casted to {@link Check}s.
   *
   * @return List of {@link Check}s.
   */
  public List<Class<? extends Check>> getBaseChecks() {
    return this.getChecks().stream()
        .map(clazz -> (Class<? extends Check>) clazz)
        .collect(Collectors.toList());
  }

  /**
   * Get the map of {@link Check}s and their {@link CodeActionSupplier}s.
   *
   * @return Map of {@link Check}s and their {@link CodeActionSupplier}s.
   */
  public abstract Map<Class<? extends T>, List<Class<? extends S>>> getFixers();

  /**
   * Get the map of {@link Check}s and their {@link CodeActionSupplier}s, casted to base types.
   *
   * @return Map of {@link Check}s and their {@link CodeActionSupplier}s.
   */
  public Map<Class<? extends Check>, List<Class<? extends CodeActionSupplier>>> getBaseFixers() {
    final Map<Class<? extends T>, List<Class<? extends S>>> fixers = this.getFixers();
    return fixers.entrySet().stream()
        .collect(
            Collectors.toMap(
                entry -> (Class<? extends Check>) entry.getKey(),
                entry ->
                    entry.getValue().stream()
                        .map(clazz -> (Class<? extends CodeActionSupplier>) clazz)
                        .collect(Collectors.toList())));
  }
}
