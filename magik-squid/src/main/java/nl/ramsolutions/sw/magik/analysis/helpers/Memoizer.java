package nl.ramsolutions.sw.magik.analysis.helpers;

import java.util.concurrent.*;

/**
 * memoize expensive calls and make sure that they do not get called twice by two/multiple threads
 * TODO include recompute filter computable?
 *
 * <p>Source:
 *
 * <ul>
 *   <li><a href="https://stackoverflow.com/a/2348533">StackOverflow post</a>
 *   <li><a
 *       href="https://www.javaspecialists.eu/archive/Issue125-Book-Review-Java-Concurrency-in-Practice.html">Book
 *       Review: Java Concurrency in Practice</a>
 * </ul>
 *
 * @param <A> the key for the map to compute from
 * @param <V> the type of the computed value
 */
public class Memoizer<A, V> implements Computable<A, V> {
  private final ConcurrentMap<A, Future<V>> cache = new ConcurrentHashMap<>();
  private final Computable<A, V> c;

  public Memoizer(Computable<A, V> c) {
    this.c = c;
  }

  public V compute(final A arg) throws InterruptedException {
    while (true) {
      Future<V> f = cache.get(arg);
      if (f == null) {
        Callable<V> eval = () -> c.compute(arg);
        FutureTask<V> ft = new FutureTask<>(eval);
        f = cache.putIfAbsent(arg, ft);
        if (f == null) {
          f = ft;
          ft.run();
        }
      }
      try {
        return f.get();
      } catch (CancellationException e) {
        cache.remove(arg, f);
      } catch (ExecutionException e) {
        // Kabutz: this is my addition to the code...
        try {
          throw e.getCause();
        } catch (RuntimeException | Error ex) {
          throw ex;
        } catch (Throwable t) {
          throw new IllegalStateException("Not unchecked", t);
        }
      }
    }
  }

  public void clear() {
    this.cache.forEach((k, v) -> v.cancel(true));
    this.cache.clear();
  }
}
