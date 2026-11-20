package nl.ramsolutions.sw.magik.debugadapter;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import nl.ramsolutions.sw.magik.debugadapter.slap.ISlapResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.SourceFileResponse;
import nl.ramsolutions.sw.magik.debugadapter.source.SourceModule;
import org.junit.jupiter.api.Test;

/** Tests for {@link FrameSourceResolver}. */
class FrameSourceResolverTest {

  /** Answers SOURCE_FILE with a fixed filename, and records the methods asked. */
  private static final class RecordingSlapProtocol extends TestSlapProtocol {

    private final List<String> methods = new ArrayList<>();

    @Override
    public CompletableFuture<ISlapResponse> getSourceFile(final String method) {
      this.methods.add(method);
      final SourceFileResponse response = new SourceFileResponse("/srv/sw/a.magik");
      return CompletableFuture.completedFuture(response);
    }

    private List<String> getMethodsForTest() {
      return this.methods;
    }
  }

  @Test
  void testResolverAsksTheAgentByDefault() throws Exception {
    final RecordingSlapProtocol slapProtocol = new RecordingSlapProtocol();
    final PathMapper pathMapper = new PathMapper(Map.of());
    final FrameSourceResolver resolver = new FrameSourceResolver(slapProtocol, pathMapper);

    final Path path = resolver.determinePath("user:a.m()");

    assertThat(path).isEqualTo(Path.of("/srv/sw/a.magik"));
    final List<String> methods = slapProtocol.getMethodsForTest();
    assertThat(methods).containsExactly("user:a.m()");
  }

  @Test
  void testResolverAsksModulesInOrder() throws Exception {
    final RecordingSlapProtocol slapProtocol = new RecordingSlapProtocol();
    final PathMapper pathMapper = new PathMapper(Map.of());
    final SourceModule firstModule = methodName -> Optional.of(Path.of("/x.magik"));
    final FrameSourceResolver resolver =
        new FrameSourceResolver(slapProtocol, pathMapper) {
          @Override
          protected List<SourceModule> createModules() {
            final List<SourceModule> basicModules = super.createModules();
            final List<SourceModule> modules = new ArrayList<>();
            modules.add(firstModule);
            modules.addAll(basicModules);
            return modules;
          }
        };

    final Path path = resolver.determinePath("user:a.m()");

    assertThat(path).isEqualTo(Path.of("/x.magik"));
    final List<String> methods = slapProtocol.getMethodsForTest();
    assertThat(methods).isEmpty();
  }

  @Test
  void testResolverFallsThroughADecliningModuleAndAnswersNullWhenNoneAnswers() throws Exception {
    final RecordingSlapProtocol slapProtocol = new RecordingSlapProtocol();
    final PathMapper pathMapper = new PathMapper(Map.of());
    final SourceModule decliningModule = methodName -> Optional.empty();
    final FrameSourceResolver resolver =
        new FrameSourceResolver(slapProtocol, pathMapper) {
          @Override
          protected List<SourceModule> createModules() {
            return List.of(decliningModule);
          }
        };

    final Path path = resolver.determinePath("user:a.m()");

    assertThat(path).isNull();
  }
}
