package nl.ramsolutions.sw.magik.debugadapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.debugadapter.slap.ISlapProtocol;
import nl.ramsolutions.sw.magik.debugadapter.slap.events.BreakpointEvent;
import nl.ramsolutions.sw.magik.debugadapter.slap.events.StepCompletedEvent;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.EvalResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.StackFrameLocalsResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.StackFrameLocalsResponse.Local;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.StackFrameLocalsResponse.VariableType;
import org.eclipse.lsp4j.debug.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Variable manager. */
class VariableManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(VariableManager.class);

  /** Magik variable. */
  static class MagikVariable {

    private final int id;
    private final int frameId;
    private final String name;
    private final String value;
    private final String expression;

    /**
     * Constructor.
     *
     * @param id Variable ID.
     * @param frameId Frame ID.
     * @param name Name of variable.
     * @param value Value of variable.
     * @param expression Expression for varaible.
     */
    MagikVariable(
        final int id,
        final int frameId,
        final String name,
        final String value,
        final String expression) {
      this.id = id;
      this.frameId = frameId;
      this.name = name;
      this.value = value;
      this.expression = expression;
    }

    int getId() {
      return this.id;
    }

    int getFrameId() {
      return this.frameId;
    }

    String getName() {
      return this.name;
    }

    String getValue() {
      return this.value;
    }

    String getExpression() {
      return this.expression;
    }
  }

  /** Scope type. */
  enum ScopeType {
    LOCALS,
    SLOTS,
  }

  private final ISlapProtocol slapProtocol;
  private final Map<Integer, Integer> frameIds = new HashMap<>();
  private final Map<Integer, Scope> scopes = new HashMap<>();
  private final Map<Integer, MagikVariable> variables = new HashMap<>();
  private int lastId;

  /**
   * Constructor.
   *
   * @param slapProtocol Slap procotol.
   */
  VariableManager(final ISlapProtocol slapProtocol) {
    this.slapProtocol = slapProtocol;

    this.clear();
  }

  /** Clear the state. */
  void clear() {
    this.frameIds.clear();
    this.scopes.clear();
    this.variables.clear();
    this.lastId = 0;
  }

  /**
   * Get frameid for scope/variable id.
   *
   * @param id Scope/Variable id.
   * @return Frame ID.
   */
  int getFrameId(final int id) {
    return this.frameIds.get(id);
  }

  // region: Scopes
  /**
   * Get the {@link Scope}s for frame ID. Currently only returns locals scope.
   *
   * @param frameId Frame ID.
   * @return {@link Scope}s for frame ID.
   */
  Scope[] getScopes(final int frameId) {
    Scope localsScope = this.getScope(frameId);
    if (localsScope == null) {
      localsScope = this.addScope(frameId, ScopeType.LOCALS);
    }
    return new Scope[] {localsScope};
  }

  /**
   * Add a scope.
   *
   * @param frameId Frame ID.
   * @param type Scope type.
   * @return Scope.
   */
  private Scope addScope(final int frameId, final ScopeType type) {
    final int id = ++this.lastId;
    this.frameIds.put(id, frameId);

    final Scope scope = new Scope();
    scope.setVariablesReference(id);
    scope.setName(type.name());
    this.scopes.put(id, scope);
    return scope;
  }

  /**
   * Get a scope by its ID.
   *
   * @param scopeId Scope ID.
   * @return Scope.
   */
  Scope getScope(final int scopeId) {
    return this.scopes.get(scopeId);
  }

  // endregion

  // region: Variables
  /**
   * Add a new variable to be tracked.
   *
   * @param frameId Frame ID.
   * @param name Variable name.
   * @param value Variable value.
   * @return New variable.
   */
  private MagikVariable addVariable(
      final int frameId, final String name, final String value, final String expression) {
    final int id = ++this.lastId;
    this.frameIds.put(id, frameId);

    final MagikVariable variable = new MagikVariable(id, frameId, name, value, expression);
    this.variables.put(id, variable);
    return variable;
  }

  /**
   * Add a new variable from a {@link StackFrameLocalsResponse.Local}.
   *
   * @param frameId Frame ID.
   * @param local Local to convert.
   * @return New variable.
   */
  private MagikVariable addVariable(final int frameId, final Local local) {
    final String name = local.getName();
    final String value = local.getValue();
    return addVariable(frameId, name, value, name);
  }

  /**
   * Add a new variable from another {@link MagikVariable}.
   *
   * @param variable Parent variable.
   * @param name Name of variable.
   * @param value Value of variable.
   * @param expression Expression to get variable.
   * @return New variable.
   */
  private MagikVariable addVariable(
      final MagikVariable variable,
      final String name,
      final String value,
      final String expression) {
    return this.addVariable(variable.getFrameId(), name, value, expression);
  }

  /**
   * Get a variable by its ID.
   *
   * @param variableId Variable ID.
   * @return Variable.
   */
  private MagikVariable getVariable(final int variableId) {
    return this.variables.get(variableId);
  }

  /**
   * Get variables.
   *
   * @param reference Reference to use.
   * @return Variables.
   */
  List<MagikVariable> getVariables(final int reference)
      throws IOException, InterruptedException, ExecutionException {
    final List<MagikVariable> magikVariables = new ArrayList<>();

    // Call into debugger to get variables.
    final Scope scope = this.getScope(reference);
    MagikVariable variable = this.getVariable(reference);
    if (scope != null) {
      final int scopeId = scope.getVariablesReference();
      final int frameId = this.getFrameId(scopeId);
      final long threadId = Lsp4jConversion.frameIdToThreadId(frameId);
      final int level = Lsp4jConversion.frameIdToLevel(frameId);
      final StackFrameLocalsResponse stackFrameLocals =
          (StackFrameLocalsResponse) this.slapProtocol.getStackFrameLocals(threadId, level).get();

      stackFrameLocals.getLocals().stream()
          .filter(local -> !local.getVariableTypes().contains(VariableType.SLOT))
          .map(local -> this.addVariable(frameId, local))
          .forEach(magikVariables::add);
    } else if (variable != null) {
      final int frameId = variable.getFrameId();
      final long threadId = Lsp4jConversion.frameIdToThreadId(frameId);
      final int level = Lsp4jConversion.frameIdToLevel(frameId);
      final String expression = variable.getExpression();

      // Determine type:
      // - sw:enumerated_format_mixin
      // - sw:indexed_format_mixin
      // - sw:slotted_format_mixin
      final String typeExpression =
          ""
              + "_if "
              + expression
              + ".is_kind_of?(sw:enumerated_format_mixin) "
              + "_then >> :enumerated_format_mixin "
              + "_elif "
              + expression
              + ".is_kind_of?(sw:indexed_format_mixin) "
              + "_then >> :indexed_format_mixin "
              + "_elif "
              + expression
              + ".is_kind_of?(sw:slotted_format_mixin) "
              + "_then >> :slotted_format_mixin "
              + "_else >> expression.class_name "
              + "_endif";
      final EvalResponse typeEvalResponse =
          (EvalResponse) this.slapProtocol.evaluate(threadId, level, typeExpression).get();
      List<MagikVariable> subVariables = null;
      switch (typeEvalResponse.getResult()) {
        case ":enumerated_format_mixin":
          subVariables = this.variablesFromEnumerated(variable);
          break;

        case ":indexed_format_mixin":
          subVariables = this.variablesFromIndexed(variable);
          break;

        case ":slotted_format_mixin":
          subVariables = this.variablesFromSlotted(variable);
          break;

        default:
          LOGGER.warn(
              "Unknown type for expression: {}, class: {}",
              expression,
              typeEvalResponse.getResult());
          subVariables = new ArrayList<>();
          break;
      }
      magikVariables.addAll(subVariables);
    }

    // Sort variables.
    final Comparator<MagikVariable> byName = Comparator.comparing(MagikVariable::getName);
    return magikVariables.stream().sorted(byName).collect(Collectors.toList());
  }

  // endregion

  private List<MagikVariable> variablesFromSlotted(final MagikVariable variable)
      throws InterruptedException, ExecutionException, IOException {
    final int frameId = variable.getFrameId();
    final long threadId = Lsp4jConversion.frameIdToThreadId(frameId);
    final int level = Lsp4jConversion.frameIdToLevel(frameId);
    final String expression = variable.getExpression();

    // Get slots.
    final String slotsExpression =
        ""
            + expression
            + ".sys!all_slot_names()"
            + ".map(_proc(sn) _return sn.subseq(sn.index_of(%!) + 1) _endproc)"
            + ".join_as_strings(%,)";
    final EvalResponse slotsEvalResponse =
        (EvalResponse) this.slapProtocol.evaluate(threadId, level, slotsExpression).get();
    final String slotNames = slotsEvalResponse.getResult();

    // For each slot, get contents
    final List<MagikVariable> magikVariables = new ArrayList<>();
    for (final String slotName : slotNames.split(",")) {
      if ("".equals(slotName)) {
        continue;
      }

      final String slotValueExpression = expression + ".sys!slot(:" + slotName + ")";
      final EvalResponse slotValueResponse =
          (EvalResponse)
              this.slapProtocol
                  .evaluate(threadId, level, slotValueExpression + ".print_string")
                  .get();
      final String slotValue = slotValueResponse.getResult();

      final MagikVariable slotVariable =
          this.addVariable(variable, slotName, slotValue, slotValueExpression);
      magikVariables.add(slotVariable);
    }
    return magikVariables;
  }

  private List<MagikVariable> variablesFromIndexed(final MagikVariable variable)
      throws InterruptedException, ExecutionException, IOException {
    final int frameId = variable.getFrameId();
    final long threadId = Lsp4jConversion.frameIdToThreadId(frameId);
    final int level = Lsp4jConversion.frameIdToLevel(frameId);
    final String expression = variable.getExpression();

    // Get size.
    final String sizeExpression = expression + ".sys!size";
    final EvalResponse sizeEvalResponse =
        (EvalResponse) this.slapProtocol.evaluate(threadId, level, sizeExpression).get();
    final int size = Integer.parseInt(sizeEvalResponse.getResult());

    // Get contents.
    final List<MagikVariable> magikVariables = new ArrayList<>();
    for (int i = 0; i < size; ++i) {
      final String itemValueExpression = expression + ".sys!at0(" + i + ")";
      final EvalResponse itemValueResponse =
          (EvalResponse)
              this.slapProtocol
                  .evaluate(threadId, level, itemValueExpression + ".print_string")
                  .get();
      final String itemValue = itemValueResponse.getResult();

      final MagikVariable slotVariable =
          this.addVariable(variable, Integer.toString(i), itemValue, itemValueExpression);
      magikVariables.add(slotVariable);
    }

    return magikVariables;
  }

  @SuppressWarnings("java:S1172")
  private List<MagikVariable> variablesFromEnumerated(final MagikVariable variable) {
    return Collections.emptyList();
  }

  /**
   * Handle a {@link BreakpointEvent}.
   *
   * @param event Event.
   */
  void handleBreakpointEvent(final BreakpointEvent event) {
    this.clear();
  }

  /**
   * Handle a {@link StepCompletedEvent}.
   *
   * @param event Event.
   */
  void handleStepCompletedEvent(final StepCompletedEvent event) {
    this.clear();
  }
}
