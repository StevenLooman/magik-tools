package nl.ramsolutions.sw.magik.debugadapter.variables;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import nl.ramsolutions.sw.magik.debugadapter.Lsp4jConversion;
import nl.ramsolutions.sw.magik.debugadapter.TestSlapProtocol;
import nl.ramsolutions.sw.magik.debugadapter.slap.ISlapResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.EvalResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.StackFrameLocalsResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.StackFrameLocalsResponse.LocalType;
import nl.ramsolutions.sw.magik.debugadapter.variables.VariableManager.MagikVariable;
import org.eclipse.lsp4j.debug.Scope;
import org.junit.jupiter.api.Test;

/** Tests for VariableManager. */
@SuppressWarnings("checkstyle:MagicNumber")
class VariableManagerTest {

  @Test
  void testGetScopes() {
    final TestSlapProtocol slapProtocol = new TestSlapProtocol();
    final List<VariableExpander> expanders = VariableManager.createBasicExpanders();
    final VariableManager manager = new VariableManager(slapProtocol, expanders);

    final int frameId = Lsp4jConversion.threadIdLevelToFrameId(20, 0);
    final Scope[] scopes = manager.getScopes(frameId);
    assertThat(scopes).hasSize(1);

    final Scope localScope = scopes[0];
    assertThat(localScope.getName()).isEqualTo("LOCALS");
  }

  @Test
  void testVariablesInScope() throws IOException, InterruptedException, ExecutionException {
    final TestSlapProtocol slapProtocol =
        new TestSlapProtocol() {
          @Override
          public CompletableFuture<ISlapResponse> getStackFrameLocals(long threadId, int level)
              throws IOException {
            final List<ISlapResponse> subResponses = new ArrayList<>();
            subResponses.add(
                new StackFrameLocalsResponse.Local(
                    LocalType.TYPE_INT,
                    "var1",
                    "value1",
                    EnumSet.noneOf(StackFrameLocalsResponse.VariableType.class)));
            subResponses.add(
                new StackFrameLocalsResponse.Local(
                    LocalType.TYPE_INT,
                    "var2",
                    "value2",
                    EnumSet.noneOf(StackFrameLocalsResponse.VariableType.class)));
            final StackFrameLocalsResponse response = new StackFrameLocalsResponse(subResponses);
            return CompletableFuture.completedFuture(response);
          }

          @Override
          public CompletableFuture<ISlapResponse> evaluate(
              long threadId, int level, String expression) {
            final EvalResponse response = new EvalResponse("-1");
            return CompletableFuture.completedFuture(response);
          }
        };
    final List<VariableExpander> expanders = VariableManager.createBasicExpanders();
    final VariableManager manager = new VariableManager(slapProtocol, expanders);

    final int frameId = Lsp4jConversion.threadIdLevelToFrameId(20, 0);
    final Scope[] scopes = manager.getScopes(frameId);
    final Scope localScope = scopes[0];

    final int reference = localScope.getVariablesReference();
    final List<MagikVariable> variables = manager.getVariables(reference, null, 0, 0);
    assertThat(variables).hasSize(2);

    final MagikVariable variable0 = variables.get(0);
    assertThat(variable0.getName()).isEqualTo("var1");
    assertThat(variable0.getValue()).isEqualTo("value1");

    final MagikVariable variable1 = variables.get(1);
    assertThat(variable1.getName()).isEqualTo("var2");
    assertThat(variable1.getValue()).isEqualTo("value2");
  }

  /** Fake agent that answers evaluations of a debuggee holding indexed-format objects. */
  private static final class CollectionSlapProtocol extends TestSlapProtocol {

    private static final String PRINT_STRING = ".print_string";

    private final List<String> localNames = new ArrayList<>();
    private final Map<String, String> typeResults = new HashMap<>();

    void addLocal(final String name) {
      this.localNames.add(name);
    }

    void addTypeResult(final String expression, final String type) {
      this.typeResults.put(expression, type);
    }

    @Override
    public CompletableFuture<ISlapResponse> getStackFrameLocals(
        final long threadId, final int level) throws IOException {
      final List<ISlapResponse> locals = new ArrayList<>();
      for (final String name : this.localNames) {
        locals.add(
            new StackFrameLocalsResponse.Local(
                LocalType.TYPE_INT,
                name,
                "value of " + name,
                EnumSet.noneOf(StackFrameLocalsResponse.VariableType.class)));
      }
      final StackFrameLocalsResponse response = new StackFrameLocalsResponse(locals);
      return CompletableFuture.completedFuture(response);
    }

    @Override
    public CompletableFuture<ISlapResponse> evaluate(
        final long threadId, final int level, final String expression) throws IOException {
      final String result = this.answer(expression);
      if (result == null) {
        final Exception failure = new IllegalStateException("Cannot evaluate: " + expression);
        return CompletableFuture.failedFuture(failure);
      }
      final EvalResponse response = new EvalResponse(result);
      return CompletableFuture.completedFuture(response);
    }

    private String answer(final String expression) {
      for (final Map.Entry<String, String> entry : this.typeResults.entrySet()) {
        if (expression.contains("_if " + entry.getKey() + ".is_kind_of?(sw:enumerated")) {
          return entry.getValue();
        }
      }

      if (expression.endsWith(".sys!size")) {
        return "2";
      }

      if (expression.endsWith(PRINT_STRING)) {
        final int end = expression.length() - PRINT_STRING.length();
        return "value of " + expression.substring(0, end);
      }

      return "0";
    }
  }

  private Scope createLocalsScope(final VariableManager manager) {
    final int frameId = Lsp4jConversion.threadIdLevelToFrameId(20, 0);
    final Scope[] scopes = manager.getScopes(frameId);
    return scopes[0];
  }

  private List<String> getNames(final List<MagikVariable> variables) {
    return variables.stream().map(MagikVariable::getName).toList();
  }

  @Test
  void testVariableThatIsNoCollectionHasNoIndexedVariables()
      throws IOException, InterruptedException, ExecutionException {
    final CollectionSlapProtocol slapProtocol = new CollectionSlapProtocol();
    slapProtocol.addLocal("count");
    final List<VariableExpander> expanders = VariableManager.createBasicExpanders();
    final VariableManager manager = new VariableManager(slapProtocol, expanders);
    final Scope scope = this.createLocalsScope(manager);

    final int scopeReference = scope.getVariablesReference();
    final List<MagikVariable> locals = manager.getVariables(scopeReference, null, 0, 0);

    final MagikVariable local = locals.get(0);
    final int indexedVariables = local.getIndexedVariables();
    assertThat(indexedVariables).isEqualTo(-1);
  }

  @Test
  void testIndexedFormatObjectKeepsItsRawElements()
      throws IOException, InterruptedException, ExecutionException {
    final CollectionSlapProtocol slapProtocol = new CollectionSlapProtocol();
    slapProtocol.addLocal("table");
    slapProtocol.addTypeResult("table", ":indexed_format_mixin");
    final List<VariableExpander> expanders = VariableManager.createBasicExpanders();
    final VariableManager manager = new VariableManager(slapProtocol, expanders);
    final Scope scope = this.createLocalsScope(manager);
    final int scopeReference = scope.getVariablesReference();
    final List<MagikVariable> locals = manager.getVariables(scopeReference, null, 0, 0);
    final MagikVariable table = locals.get(0);
    final int indexedVariables = table.getIndexedVariables();
    assertThat(indexedVariables).isEqualTo(-1);

    final int tableReference = table.getId();
    final List<MagikVariable> elements = manager.getVariables(tableReference, null, 0, 0);

    final List<String> names = this.getNames(elements);
    assertThat(names).containsExactly("0", "1");
    final List<String> evaluateNames = elements.stream().map(MagikVariable::getExpression).toList();
    assertThat(evaluateNames).containsExactly("table.sys!at0(0)", "table.sys!at0(1)");
  }
}
