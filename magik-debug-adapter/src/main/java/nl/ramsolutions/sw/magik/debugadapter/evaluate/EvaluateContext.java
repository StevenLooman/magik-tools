package nl.ramsolutions.sw.magik.debugadapter.evaluate;

import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * An evaluate request.
 *
 * @param frameId Stack frame to evaluate in, or {@code null} if the request has none.
 * @param expression Expression to evaluate.
 * @param context Where the request came from (such as {@code watch} or {@code repl}), or {@code
 *     null} if the client did not say.
 */
public record EvaluateContext(
    @Nullable Integer frameId, String expression, @Nullable String context) {}
