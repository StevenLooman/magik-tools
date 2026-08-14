package nl.ramsolutions.sw.magik.languageserver.callhierarchy;

import java.util.List;
import java.util.Optional;
import org.eclipse.lsp4j.CallHierarchyItem;

/**
 * A single call hierarchy module. Detects whether it applies to a context and, if so, provides its
 * call hierarchy items.
 */
public interface CallHierarchyModule {

  /**
   * Try to provide call hierarchy items for the given context.
   *
   * @param context Call hierarchy context.
   * @return {@code Optional.of} the items if this module claims the context; {@code
   *     Optional.empty()} if it does not apply.
   */
  Optional<List<CallHierarchyItem>> tryCallHierarchy(CallHierarchyContext context);
}
