package nl.ramsolutions.sw.magik.analysis.helpers;

public interface Computable<A, V> {
  V compute(A arg) throws InterruptedException;
}
