package nl.ramsolutions.sw.magik.debugadapter.slap;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.jupiter.api.Test;

/** Tests for ByteBufferUtils. */
@SuppressWarnings("checkstyle:MagicNumber")
class ByteBufferUtilsTest {

  @Test
  void testReadUInt32LE() {
    // unsigned 132/0x84 equals bitwise signed -128 + 4 = -124
    byte[] data = new byte[] {-124, 0, 0, 0}; // unsigned: 132, 0, 0, 0
    ByteBuffer buffer = ByteBuffer.wrap(data);
    buffer.order(ByteOrder.LITTLE_ENDIAN);

    long actual = ByteBufferHelper.readUInt32(buffer);
    long expected = 132;
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  void testReadUInt32BE() {
    // unsigned 132/0x84 equals bitwise signed -128 + 4 = -124
    byte[] data = new byte[] {0, 0, 0, -124}; // unsigned: 0, 0, 0, 132
    ByteBuffer buffer = ByteBuffer.wrap(data);
    buffer.order(ByteOrder.BIG_ENDIAN);

    long actual = ByteBufferHelper.readUInt32(buffer);
    long expected = 132;
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  void testWriteUInt32LE() {
    ByteBuffer buffer = ByteBuffer.allocate(4);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    long writeValue = 0x0A0B0C0D;
    ByteBufferHelper.writeUInt32(buffer, writeValue);

    assertThat(buffer.array()).isEqualTo(new byte[] {0x0D, 0x0C, 0x0B, 0x0A});

    buffer.flip();
    long readValue = ByteBufferHelper.readUInt32(buffer, 0);
    assertThat(readValue).isEqualTo(0x0A0B0C0D);
  }

  @Test
  void testWriteUInt32BE() {
    ByteBuffer buffer = ByteBuffer.allocate(4);
    buffer.order(ByteOrder.BIG_ENDIAN);
    long writeValue = 0x0A0B0C0D;
    ByteBufferHelper.writeUInt32(buffer, writeValue);

    assertThat(buffer.array()).isEqualTo(new byte[] {0x0A, 0x0B, 0x0C, 0x0D});

    buffer.flip();
    long readValue = ByteBufferHelper.readUInt32(buffer, 0);
    assertThat(readValue).isEqualTo(0x0A0B0C0D);
  }

  @Test
  void testWriteUInt32LE1() {
    ByteBuffer buffer = ByteBuffer.allocate(4);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    long writeValue = 1;
    ByteBufferHelper.writeUInt32(buffer, writeValue);

    assertThat(buffer.array()).isEqualTo(new byte[] {0x01, 0x00, 0x00, 0x00});

    buffer.flip();
    long readValue = ByteBufferHelper.readUInt32(buffer, 0);
    assertThat(readValue).isEqualTo(1);
  }

  @Test
  void testWriteUInt32BE1() {
    ByteBuffer buffer = ByteBuffer.allocate(4);
    buffer.order(ByteOrder.BIG_ENDIAN);
    long value = 1;
    ByteBufferHelper.writeUInt32(buffer, value);

    assertThat(buffer.array()).isEqualTo(new byte[] {0x00, 0x00, 0x00, 0x01});

    buffer.flip();
    long readValue = ByteBufferHelper.readUInt32(buffer, 0);
    assertThat(readValue).isEqualTo(1);
  }

  @Test
  void testWriteUInt32LEOver() {
    ByteBuffer buffer = ByteBuffer.allocate(4);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    long writeValue = 1L << 32;
    ByteBufferHelper.writeUInt32(buffer, writeValue);

    assertThat(buffer.array()).isEqualTo(new byte[] {0x00, 0x00, 0x00, 0x00});

    buffer.flip();
    long readValue = ByteBufferHelper.readUInt32(buffer, 0);
    assertThat(readValue).isZero();
  }

  @Test
  void testWriteUInt32BEOver() {
    ByteBuffer buffer = ByteBuffer.allocate(4);
    buffer.order(ByteOrder.BIG_ENDIAN);
    long value = 1L << 32;
    ByteBufferHelper.writeUInt32(buffer, value);

    assertThat(buffer.array()).isEqualTo(new byte[] {0x00, 0x00, 0x00, 0x00});

    buffer.flip();
    long readValue = ByteBufferHelper.readUInt32(buffer, 0);
    assertThat(readValue).isZero();
  }
}
