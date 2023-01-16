package nl.ramsolutions.sw.magik.debugadapter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import nl.ramsolutions.sw.magik.debugadapter.slap.ISlapResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.EvalResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.SourceFileResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.ThreadInfoResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.ThreadListResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.ThreadStackResponse;
import org.eclipse.lsp4j.debug.StackFrame;
import org.eclipse.lsp4j.debug.Thread;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for ThreadManager.
 */
@SuppressWarnings("checkstyle:MagicNumber")
class ThreadManagerTest {

    @Test
    void testThreads() throws IOException, InterruptedException, ExecutionException {
        final TestSlapProtocol slapProtocol = new TestSlapProtocol() {
            @Override
            public CompletableFuture<ISlapResponse> getThreadList() throws IOException {
                final ThreadListResponse response = new ThreadListResponse(List.of(1L, 2L));

                return CompletableFuture.completedFuture(response);
            }

            @Override
            public CompletableFuture<ISlapResponse> getThreadInfo(long threadId) throws IOException {
                final String name = "Thread: " + threadId;
                final ThreadInfoResponse response = new ThreadInfoResponse(
                        1, // priority
                        false, // daemon
                        name,
                        ThreadInfoResponse.ThreadState.RUNNABLE,
                        EnumSet.noneOf(ThreadInfoResponse.ThreadFlag.class));

                return CompletableFuture.completedFuture(response);
            }
        };

        final PathMapper pathMapper = new PathMapper(Collections.emptyMap());
        final ThreadManager manager = new ThreadManager(slapProtocol, null, pathMapper);
        final List<Thread> threads = manager.threads();
        assertThat(threads).hasSize(2);

        assertThat(threads.get(0).getId()).isEqualTo(1);
        assertThat(threads.get(0).getName()).isEqualTo("Thread: 1");
        assertThat(threads.get(1).getId()).isEqualTo(2);
        assertThat(threads.get(1).getName()).isEqualTo("Thread: 2");
    }

    @Test
    void testStackTrace() throws IOException, InterruptedException, ExecutionException {
        final TestSlapProtocol slapProtocol = new TestSlapProtocol() {
            @Override
            public CompletableFuture<ISlapResponse> getThreadStack(long threadId) throws IOException {
                final ThreadStackResponse response = new ThreadStackResponse(
                    List.of(
                        new ThreadStackResponse.StackElement(0, 0, "object.m1()", "Magik"),
                        new ThreadStackResponse.StackElement(1, 100, "java/lang/Object;m2", "Java"),
                        new ThreadStackResponse.StackElement(2, 30, "object.m2()", "Magik"),
                        new ThreadStackResponse.StackElement(3, 200, "java/lang/Object;m1", "Java")));
                return CompletableFuture.completedFuture(response);
            }

            @Override
            public CompletableFuture<ISlapResponse> evaluate(
                    final long threadId, final int level, final String expression) throws IOException {
                final EvalResponse response = new EvalResponse("sw");
                return CompletableFuture.completedFuture(response);
            }

            @Override
            public CompletableFuture<ISlapResponse> getSourceFile(final String method) throws IOException {
                String result = null;
                if ("sw:object.m1()".equals(method)) {
                    result = "/src/module/sources/file1.magik";
                } else if ("sw:object.m2()".equals(method)) {
                    result = "/src/module/sources/file2.magik";
                }
                final SourceFileResponse response = new SourceFileResponse(result);
                return CompletableFuture.completedFuture(response);
            }
        };

        final Map<Path, Path> pathMapping = Map.of(
            Path.of("/src"), Path.of("/home/user/src"));
        final PathMapper pathMapper = new PathMapper(pathMapping);
        final ThreadManager manager = new ThreadManager(slapProtocol, null, pathMapper);
        final List<StackFrame> stackFrames = manager.stackTrace(1);
        assertThat(stackFrames).hasSize(2);

        final StackFrame frame0 = stackFrames.get(0);
        assertThat(frame0.getId()).isEqualTo(Lsp4jConversion.threadIdLevelToFrameId(1, 0));
        assertThat(frame0.getName()).isEqualTo("sw:object.m1()");
        final String path0Str = frame0.getSource().getPath();
        final Path path0 = Path.of(path0Str);
        final Path path0Expected = Path.of("/home/user/src/module/sources/file1.magik");
        assertThat(path0).isEqualTo(path0Expected);

        final StackFrame frame1 = stackFrames.get(1);
        assertThat(frame1.getId()).isEqualTo(Lsp4jConversion.threadIdLevelToFrameId(1, 2));
        assertThat(frame1.getName()).isEqualTo("sw:object.m2()");
        final String path1Str = frame1.getSource().getPath();
        final Path path1 = Path.of(path1Str);
        final Path path1Expected = Path.of("/home/user/src/module/sources/file2.magik");
        assertThat(path1).isEqualTo(path1Expected);
    }

}
