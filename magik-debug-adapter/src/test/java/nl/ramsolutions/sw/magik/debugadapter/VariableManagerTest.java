package nl.ramsolutions.sw.magik.debugadapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import nl.ramsolutions.sw.magik.debugadapter.VariableManager.MagikVariable;
import nl.ramsolutions.sw.magik.debugadapter.slap.ISlapResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.StackFrameLocalsResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.StackFrameLocalsResponse.LocalType;
import org.eclipse.lsp4j.debug.Scope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for VariableManager.
 */
@SuppressWarnings("checkstyle:MagicNumber")
class VariableManagerTest {

    @Test
    void testGetScopes() {
        final TestSlapProtocol slapProtocol = new TestSlapProtocol();
        final VariableManager manager = new VariableManager(slapProtocol);

        final int frameId = Lsp4jConversion.threadIdLevelToFrameId(20, 0);
        final Scope[] scopes = manager.getScopes(frameId);
        assertThat(scopes).hasSize(1);

        final Scope localScope = scopes[0];
        assertThat(localScope.getName()).isEqualTo("LOCALS");
    }

    @Test
    void testVariablesInScope() throws IOException, InterruptedException, ExecutionException {
        final TestSlapProtocol slapProtocol = new TestSlapProtocol() {
            @Override
            public CompletableFuture<ISlapResponse> getStackFrameLocals(long threadId, int level) throws IOException {
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
        };
        final VariableManager manager = new VariableManager(slapProtocol);

        final int frameId = Lsp4jConversion.threadIdLevelToFrameId(20, 0);
        final Scope[] scopes = manager.getScopes(frameId);
        final Scope localScope = scopes[0];

        final int reference = localScope.getVariablesReference();
        final List<MagikVariable> variables = manager.getVariables(reference);
        assertThat(variables).hasSize(2);

        final MagikVariable variable0 = variables.get(0);
        assertThat(variable0.getName()).isEqualTo("var1");
        assertThat(variable0.getValue()).isEqualTo("value1");

        final MagikVariable variable1 = variables.get(1);
        assertThat(variable1.getName()).isEqualTo("var2");
        assertThat(variable1.getValue()).isEqualTo("value2");
    }

}
