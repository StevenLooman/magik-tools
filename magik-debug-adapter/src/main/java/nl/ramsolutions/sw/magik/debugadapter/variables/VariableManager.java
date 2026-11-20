package nl.ramsolutions.sw.magik.debugadapter.variables;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import nl.ramsolutions.sw.magik.debugadapter.Lsp4jConversion;
import nl.ramsolutions.sw.magik.debugadapter.slap.ISlapProtocol;
import nl.ramsolutions.sw.magik.debugadapter.slap.ISlapResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.events.BreakpointEvent;
import nl.ramsolutions.sw.magik.debugadapter.slap.events.StepCompletedEvent;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.EvalResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.StackFrameLocalsResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.StackFrameLocalsResponse.Local;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.StackFrameLocalsResponse.VariableType;
import org.eclipse.lsp4j.debug.Scope;
import org.eclipse.lsp4j.debug.VariablesArgumentsFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Variable manager. */
public class VariableManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(VariableManager.class);

  /** Value of {@link MagikVariable#getIndexedVariables()} for a variable without elements. */
  public static final int NO_INDEXED_VARIABLES = -1;

  /** Magik variable. */
  public static class MagikVariable {

    private final int id;
    private final int frameId;
    private final String name;
    private final String value;
    private final String expression;
    private final VariableExpander expander;
    private final int indexedVariables;

    /**
     * Constructor.
     *
     * @param id Variable ID.
     * @param frameId Frame ID.
     * @param name Name of variable.
     * @param value Value of variable.
     * @param expression Expression for variable.
     * @param expander Expander that lists the children of the variable.
     * @param indexedVariables Number of elements of an indexed collection, or {@link
     *     VariableManager#NO_INDEXED_VARIABLES}.
     */
    MagikVariable(
        final int id,
        final int frameId,
        final String name,
        final String value,
        final String expression,
        final VariableExpander expander,
        final int indexedVariables) {
      this.id = id;
      this.frameId = frameId;
      this.name = name;
      this.value = value;
      this.expression = expression;
      this.expander = expander;
      this.indexedVariables = indexedVariables;
    }

    public int getId() {
      return this.id;
    }

    int getFrameId() {
      return this.frameId;
    }

    public String getName() {
      return this.name;
    }

    public String getValue() {
      return this.value;
    }

    public String getExpression() {
      return this.expression;
    }

    VariableExpander getExpander() {
      return this.expander;
    }

    /**
     * Get the number of elements.
     *
     * @return Number of elements, or {@link VariableManager#NO_INDEXED_VARIABLES} if not an indexed
     *     collection.
     */
    public int getIndexedVariables() {
      return this.indexedVariables;
    }

    /**
     * Get the number of named children shown next to the elements.
     *
     * @return Number of named children.
     */
    public int getNamedVariables() {
      return this.expander.getNamedVariables();
    }

    /**
     * Test if the variable has children.
     *
     * @return {@code true} if the variable can be expanded.
     */
    public boolean isExpandable() {
      return this.expander.isExpandable();
    }
  }

  /** Scope type. */
  enum ScopeType {
    LOCALS,
    SLOTS,
  }

  /** The expander that claimed a value and the number of its elements. */
  private record Description(VariableExpander expander, int indexedVariables) {}

  private final ISlapProtocol slapProtocol;
  private final List<VariableExpander> expanders;
  private final VariableExpander fallbackExpander = new ObjectExpander();
  // Request threads and event handler threads use this manager at the same time.
  // The maps and `lastId` are guarded by `lock`, which is never held during a SLAP round trip.
  private final Object lock = new Object();
  private final Map<Integer, Integer> frameIds = new HashMap<>();
  private final Map<Integer, Scope> scopes = new HashMap<>();
  private final Map<Integer, MagikVariable> variables = new HashMap<>();
  private int lastId;

  /**
   * Constructor.
   *
   * @param slapProtocol Slap protocol.
   */
  public VariableManager(final ISlapProtocol slapProtocol) {
    this.slapProtocol = slapProtocol;
    this.expanders = this.createExpanders();

    this.clear();
  }

  /**
   * Constructor.
   *
   * @param slapProtocol Slap protocol.
   * @param expanders Ordered expanders.
   */
  VariableManager(final ISlapProtocol slapProtocol, final List<VariableExpander> expanders) {
    this.slapProtocol = slapProtocol;
    this.expanders = List.copyOf(expanders);

    this.clear();
  }

  /**
   * Create the ordered expanders. The first expander whose condition holds claims a value; a value
   * none of them claims is shown by its format, its slots or its raw storage.
   *
   * @return Ordered expanders.
   */
  protected List<VariableExpander> createExpanders() {
    return VariableManager.createBasicExpanders();
  }

  /**
   * Create the basic expanders. There are none: every value is shown by its format, its slots or
   * its raw storage.
   *
   * @return Ordered basic expanders.
   */
  static List<VariableExpander> createBasicExpanders() {
    return List.of();
  }

  /** Clear the state. */
  public void clear() {
    synchronized (this.lock) {
      this.frameIds.clear();
      this.scopes.clear();
      this.variables.clear();
      this.lastId = 0;
    }
  }

  /**
   * Get frameid for scope/variable id.
   *
   * @param id Scope/Variable id.
   * @return Frame ID.
   */
  int getFrameId(final int id) {
    synchronized (this.lock) {
      return this.frameIds.get(id);
    }
  }

  // region: Scopes
  /**
   * Get the {@link Scope}s for frame ID. Currently only returns locals scope.
   *
   * @param frameId Frame ID.
   * @return {@link Scope}s for frame ID.
   */
  public Scope[] getScopes(final int frameId) {
    synchronized (this.lock) {
      Scope localsScope = this.getScope(frameId);
      if (localsScope == null) {
        localsScope = this.addScope(frameId, ScopeType.LOCALS);
      }
      return new Scope[] {localsScope};
    }
  }

  /**
   * Add a scope.
   *
   * @param frameId Frame ID.
   * @param type Scope type.
   * @return Scope.
   */
  private Scope addScope(final int frameId, final ScopeType type) {
    synchronized (this.lock) {
      final int id = ++this.lastId;
      this.frameIds.put(id, frameId);

      final Scope scope = new Scope();
      scope.setVariablesReference(id);
      scope.setName(type.name());
      this.scopes.put(id, scope);
      return scope;
    }
  }

  /**
   * Get a scope by its ID.
   *
   * @param scopeId Scope ID.
   * @return Scope.
   */
  Scope getScope(final int scopeId) {
    synchronized (this.lock) {
      return this.scopes.get(scopeId);
    }
  }

  // endregion

  // region: Variables
  /**
   * Add a new variable to be tracked, claimed by one of the expanders.
   *
   * @param frameId Frame ID.
   * @param name Variable name.
   * @param value Variable value.
   * @param expression Expression to get variable.
   * @return New variable.
   */
  private MagikVariable addVariable(
      final int frameId, final String name, final String value, final String expression)
      throws IOException, InterruptedException {
    // Describe before taking the lock, it is a SLAP round trip.
    final Description description = this.describeVariable(frameId, expression);
    final VariableExpander expander = description.expander();
    final int indexedVariables = description.indexedVariables();
    final String shownValue = this.describeValue(frameId, expander, expression, value);
    return this.addVariable(frameId, name, shownValue, expression, expander, indexedVariables);
  }

  /**
   * Get the value to show, from the expander when it supplies one. The agent's value stays when the
   * expander supplies none, or its evaluation fails or answers nothing.
   *
   * @param frameId Frame ID.
   * @param expander Expander that claimed the variable.
   * @param expression Expression of the variable.
   * @param agentValue Value the agent reports.
   * @return Value to show.
   */
  private String describeValue(
      final int frameId,
      final VariableExpander expander,
      final String expression,
      final String agentValue)
      throws IOException, InterruptedException {
    final String valueExpression = expander.createValueExpression(expression);
    if (valueExpression == null) {
      return agentValue;
    }

    final String answer;
    try {
      answer = this.evaluateInFrame(frameId, valueExpression);
    } catch (final ExecutionException exception) {
      LOGGER.debug("Could not get value: {}", expression, exception);
      return agentValue;
    }
    if (answer == null || answer.isBlank()) {
      return agentValue;
    }
    return answer;
  }

  private MagikVariable addVariable(
      final int frameId,
      final String name,
      final String value,
      final String expression,
      final VariableExpander expander,
      final int indexedVariables) {
    synchronized (this.lock) {
      final int id = ++this.lastId;
      this.frameIds.put(id, frameId);

      final MagikVariable variable =
          new MagikVariable(id, frameId, name, value, expression, expander, indexedVariables);
      this.variables.put(id, variable);
      return variable;
    }
  }

  /**
   * Add a new variable from a {@link StackFrameLocalsResponse.Local}.
   *
   * @param frameId Frame ID.
   * @param local Local to convert.
   * @return New variable.
   */
  private MagikVariable addVariable(final int frameId, final Local local)
      throws IOException, InterruptedException {
    final String name = local.getName();
    final String value = local.getValue();
    return this.addVariable(frameId, name, value, name);
  }

  /**
   * Add a child of a variable, claimed by one of the expanders.
   *
   * @param variable Parent variable.
   * @param name Name of variable.
   * @param value Value of variable.
   * @param expression Expression to get variable.
   * @return New variable.
   */
  MagikVariable addVariable(
      final MagikVariable variable, final String name, final String value, final String expression)
      throws IOException, InterruptedException {
    final int frameId = variable.getFrameId();
    return this.addVariable(frameId, name, value, expression);
  }

  /**
   * Add a child of a variable that is shown by its format, its slots or its raw storage, without
   * asking the expanders.
   *
   * @param variable Parent variable.
   * @param name Name of variable.
   * @param value Value of variable.
   * @param expression Expression to get variable.
   * @return New variable.
   */
  MagikVariable addRawVariable(
      final MagikVariable variable,
      final String name,
      final String value,
      final String expression) {
    return this.addBoundVariable(
        variable, name, value, expression, this.fallbackExpander, NO_INDEXED_VARIABLES);
  }

  /**
   * Add a child of a variable that the given expander lists, without asking the expanders.
   *
   * @param variable Parent variable.
   * @param name Name of variable.
   * @param value Value of variable.
   * @param expression Expression to get variable.
   * @param expander Expander that lists the children of the new variable.
   * @param indexedVariables Number of indexed children of the new variable, or {@link
   *     #NO_INDEXED_VARIABLES}.
   * @return New variable.
   */
  MagikVariable addBoundVariable(
      final MagikVariable variable,
      final String name,
      final String value,
      final String expression,
      final VariableExpander expander,
      final int indexedVariables) {
    final int frameId = variable.getFrameId();
    return this.addVariable(frameId, name, value, expression, expander, indexedVariables);
  }

  /**
   * Get a variable by its ID.
   *
   * @param variableId Variable ID.
   * @return Variable.
   */
  private MagikVariable getVariable(final int variableId) {
    synchronized (this.lock) {
      return this.variables.get(variableId);
    }
  }

  /**
   * Evaluate an expression in the frame of a variable.
   *
   * @param variable Variable.
   * @param expression Expression to evaluate.
   * @return Result.
   */
  String evaluate(final MagikVariable variable, final String expression)
      throws IOException, InterruptedException, ExecutionException {
    final int frameId = variable.getFrameId();
    return this.evaluateInFrame(frameId, expression);
  }

  private String evaluateInFrame(final int frameId, final String expression)
      throws IOException, InterruptedException, ExecutionException {
    final long threadId = Lsp4jConversion.frameIdToThreadId(frameId);
    final int level = Lsp4jConversion.frameIdToLevel(frameId);
    final CompletableFuture<ISlapResponse> future =
        this.slapProtocol.evaluate(threadId, level, expression);
    final EvalResponse response = (EvalResponse) future.get();
    return response.getResult();
  }

  /**
   * Find the expander that claims a value, and the number of its elements, in one evaluation.
   *
   * @param frameId Frame ID.
   * @param expression Expression of the variable.
   * @return Description.
   */
  private Description describeVariable(final int frameId, final String expression)
      throws IOException, InterruptedException {
    final Description fallback = new Description(this.fallbackExpander, NO_INDEXED_VARIABLES);
    if (this.expanders.isEmpty()) {
      return fallback;
    }

    final long threadId = Lsp4jConversion.frameIdToThreadId(frameId);
    final int level = Lsp4jConversion.frameIdToLevel(frameId);
    final String describeExpression = this.createDescribeExpression(expression);
    final String result;
    try {
      final CompletableFuture<ISlapResponse> describeFuture =
          this.slapProtocol.evaluate(threadId, level, describeExpression);
      final EvalResponse describeResponse = (EvalResponse) describeFuture.get();
      result = describeResponse.getResult();
    } catch (final ExecutionException exception) {
      LOGGER.debug("Could not describe: {}", expression, exception);
      return fallback;
    }

    final long answer = VariableManager.parseAnswer(result);
    if (answer < 0) {
      return fallback;
    }
    final int modules = this.expanders.size();
    final int index = (int) (answer % modules);
    final VariableExpander expander = this.expanders.get(index);
    final String countExpression = expander.createCountExpression(expression);
    final int indexedVariables =
        countExpression != null ? (int) (answer / modules) : NO_INDEXED_VARIABLES;
    return new Description(expander, indexedVariables);
  }

  /**
   * Create the expression that finds the expander claiming a value. The first expander whose
   * condition holds answers its count times the number of expanders plus its index, so one expander
   * answers just its count; no expander answers {@link #NO_INDEXED_VARIABLES}.
   */
  private String createDescribeExpression(final String expression) {
    final int modules = this.expanders.size();
    final StringBuilder builder = new StringBuilder();
    for (int index = 0; index < modules; ++index) {
      final VariableExpander expander = this.expanders.get(index);
      final String condition = expander.createCondition(expression);
      final String countExpression = expander.createCountExpression(expression);
      final String count = countExpression != null ? countExpression : "0";
      final String keyword = index == 0 ? "_if " : "_elif ";
      final String branch =
          keyword + condition + " _then >> (" + count + ") * " + modules + " + " + index + " ";
      builder.append(branch);
    }
    final String otherwise = "_else >> " + NO_INDEXED_VARIABLES + " _endif";
    builder.append(otherwise);
    return builder.toString();
  }

  private static long parseAnswer(final String result) {
    try {
      final String trimmed = result.trim();
      return Long.parseLong(trimmed);
    } catch (final NumberFormatException exception) {
      return NO_INDEXED_VARIABLES;
    }
  }

  /**
   * Get variables.
   *
   * @param reference Reference to use.
   * @param filter Kind of variables to return, or {@code null} for both.
   * @param start Index of the first element to return, for an indexed collection.
   * @param count Number of elements to return, or 0 for all, for an indexed collection.
   * @return Variables.
   */
  public List<MagikVariable> getVariables(
      final int reference, final VariablesArgumentsFilter filter, final int start, final int count)
      throws IOException, InterruptedException, ExecutionException {
    final Scope scope = this.getScope(reference);
    if (scope != null) {
      return this.variablesFromScope(scope);
    }

    final MagikVariable variable = this.getVariable(reference);
    if (variable == null) {
      return Collections.emptyList();
    }

    final VariableExpander expander = variable.getExpander();
    return expander.getVariables(this, variable, filter, start, count);
  }

  // endregion

  private List<MagikVariable> variablesFromScope(final Scope scope)
      throws IOException, InterruptedException, ExecutionException {
    final int scopeId = scope.getVariablesReference();
    final int frameId = this.getFrameId(scopeId);
    final long threadId = Lsp4jConversion.frameIdToThreadId(frameId);
    final int level = Lsp4jConversion.frameIdToLevel(frameId);
    final CompletableFuture<ISlapResponse> localsFuture =
        this.slapProtocol.getStackFrameLocals(threadId, level);
    final StackFrameLocalsResponse stackFrameLocals = (StackFrameLocalsResponse) localsFuture.get();

    final List<MagikVariable> magikVariables = new ArrayList<>();
    for (final Local local : stackFrameLocals.getLocals()) {
      final Set<VariableType> variableTypes = local.getVariableTypes();
      if (!variableTypes.contains(VariableType.SLOT)) {
        final MagikVariable magikVariable = this.addVariable(frameId, local);
        magikVariables.add(magikVariable);
      }
    }
    return ObjectExpander.sortByName(magikVariables);
  }

  /**
   * Handle a {@link BreakpointEvent}.
   *
   * @param event Event.
   */
  public void handleBreakpointEvent(final BreakpointEvent event) {
    this.clear();
  }

  /**
   * Handle a {@link StepCompletedEvent}.
   *
   * @param event Event.
   */
  public void handleStepCompletedEvent(final StepCompletedEvent event) {
    this.clear();
  }
}
