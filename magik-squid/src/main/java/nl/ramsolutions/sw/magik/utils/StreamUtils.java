package nl.ramsolutions.sw.magik.utils;

import java.util.AbstractMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/** Utils for {@link Stream}s. */
public final class StreamUtils {

  private StreamUtils() {}

  /**
   * Zip two streams.
   *
   * @param <A> Type A.
   * @param <B> Type B.
   * @param streamA Stream A to zip.
   * @param streamB Stream B to zip.
   * @return Stream of {@code Map.Entry}s with elements from both streams.
   */
  public static <A, B> Stream<Map.Entry<A, B>> zip(
      final Stream<? extends A> streamA, final Stream<? extends B> streamB) {
    final Spliterator<? extends A> streamASpliterator = streamA.spliterator();
    final Spliterator<? extends B> streamBSpliterator = streamB.spliterator();

    // Zipping looses DISTINCT and SORTED characteristics.
    final int characteristics =
        streamASpliterator.characteristics()
            & streamBSpliterator.characteristics()
            & ~(Spliterator.DISTINCT | Spliterator.SORTED);

    final long zipSize =
        (characteristics & Spliterator.SIZED) != 0
            ? Math.max(
                streamASpliterator.getExactSizeIfKnown(), streamBSpliterator.getExactSizeIfKnown())
            : -1;

    final Iterator<A> streamAIterator = Spliterators.iterator(streamASpliterator);
    final Iterator<B> streamBIterator = Spliterators.iterator(streamBSpliterator);
    final Iterator<Map.Entry<A, B>> streamCIterator =
        new Iterator<Map.Entry<A, B>>() {

          @Override
          public boolean hasNext() {
            return streamAIterator.hasNext() || streamBIterator.hasNext();
          }

          @Override
          public Map.Entry<A, B> next() {
            final A valueA = streamAIterator.hasNext() ? streamAIterator.next() : null;
            final B valueB = streamBIterator.hasNext() ? streamBIterator.next() : null;
            // Use a AbstractMap.SimpleImmutableEntry<> to allow for null key values.
            return new AbstractMap.SimpleImmutableEntry<>(valueA, valueB);
          }
        };

    final Spliterator<Map.Entry<A, B>> spliterator =
        Spliterators.spliterator(streamCIterator, zipSize, characteristics);
    final boolean isParallel = streamA.isParallel() || streamB.isParallel();
    return StreamSupport.stream(spliterator, isParallel);
  }
}
