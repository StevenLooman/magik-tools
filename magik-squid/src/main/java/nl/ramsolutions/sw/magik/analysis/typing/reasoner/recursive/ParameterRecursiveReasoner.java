package nl.ramsolutions.sw.magik.analysis.typing.reasoner.recursive;

/** Recursive reasoner for parameters definitions of method definitions. */
public class ParameterRecursiveReasoner {

  private final int maxDepth;

  public ParameterRecursiveReasoner(final int maxDepth) {
    this.maxDepth = maxDepth;
  }
}
