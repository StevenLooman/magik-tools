package nl.ramsolutions.sw.magik.utils;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Utils for {@link Stream}s.
 */
public final class StreamUtils {

    private StreamUtils() {
    }

    /**
     * Zip two streams and apply {@code func} to the combinations.
     * @param <A> Type A.
     * @param <B> Type B.
     * @param <C> Type C.
     * @param streamA Stream A to zip.
     * @param streamB Stream B to zip.
     * @param func Function to apply to elements.
     * @return Stream of results from {@link func}.
     */
    public static <A, B, C> Stream<C> zip(
            final Stream<? extends A> streamA,
            final Stream<? extends B> streamB,
            final BiFunction<? super A, ? super B, ? extends C> func) {
        final Spliterator<? extends A> streamASpliterator = streamA.spliterator();
        final Spliterator<? extends B> streamBSpliterator = streamB.spliterator();

        // Zipping looses DISTINCT and SORTED characteristics.
        final int characteristics =
            streamASpliterator.characteristics()
            & streamBSpliterator.characteristics()
            & ~(Spliterator.DISTINCT | Spliterator.SORTED);

        final long zipSize =
            (characteristics & Spliterator.SIZED) != 0
            ? Math.min(streamASpliterator.getExactSizeIfKnown(), streamBSpliterator.getExactSizeIfKnown())
            : -1;

        final Iterator<A> streamAIterator = Spliterators.iterator(streamASpliterator);
        final Iterator<B> streamBIterator = Spliterators.iterator(streamBSpliterator);
        final Iterator<C> streamCIterator = new Iterator<C>() {

            @Override
            public boolean hasNext() {
                return streamAIterator.hasNext() && streamBIterator.hasNext();
            }

            @Override
            public C next() {
                return func.apply(streamAIterator.next(), streamBIterator.next());
            }

        };

        final Spliterator<C> spliterator = Spliterators.spliterator(streamCIterator, zipSize, characteristics);
        final boolean isParallel = streamA.isParallel() || streamB.isParallel();
        return StreamSupport.stream(spliterator, isParallel);
    }

}
