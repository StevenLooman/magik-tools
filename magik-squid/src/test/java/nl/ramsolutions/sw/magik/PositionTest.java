package nl.ramsolutions.sw.magik;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Test Position. */
class PositionTest {

  @Test
  void testPositionCompareTo() {
    assertThat(new Position(1, 4).compareTo(new Position(2, 4))).isEqualTo(-1);
    assertThat(new Position(2, 3).compareTo(new Position(2, 4))).isEqualTo(-1);
    assertThat(new Position(2, 4).compareTo(new Position(2, 4))).isEqualTo(0);
    assertThat(new Position(2, 4).compareTo(new Position(1, 4))).isEqualTo(1);
    assertThat(new Position(2, 4).compareTo(new Position(2, 3))).isEqualTo(1);
  }
}
