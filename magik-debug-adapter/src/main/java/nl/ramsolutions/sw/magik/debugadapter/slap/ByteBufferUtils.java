package nl.ramsolutions.sw.magik.debugadapter.slap;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * Byte buffer utils.
 */
@SuppressWarnings("checkstyle:MagicNumber")
public final class ByteBufferUtils {

    private static final int ELEMENTS_PER_LINE = 16;

    private ByteBufferUtils() {
    }

    /**
     * Dump a ByteBuffer to String, showing values in hexadecimal notation.
     * @param byteBuffer ByteBuffer to get data from.
     * @return Hex representation of contents.
     */
    public static String toHexDump(final ByteBuffer byteBuffer) {
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < byteBuffer.limit(); ++i) {
            if (i % ELEMENTS_PER_LINE == 0) {
                builder.append(String.format("%04X - ", i));
            }
            final byte value = byteBuffer.get(i);
            builder.append(String.format("%02X ", value));
            if (i % ELEMENTS_PER_LINE == 15) {
                builder.append("\n");
            }
        }
        return builder.toString();
    }

    /**
     * Read a uint32 value from the buffers current position.
     * @param buffer ByteBuffer to read from.
     * @return Uint32 value.
     */
    public static long readUInt32(final ByteBuffer buffer) {
        final int val = buffer.getInt();

        final ByteBuffer tmp = ByteBuffer.allocate(8);
        final ByteOrder order = buffer.order();
        tmp.order(order);
        if (order == ByteOrder.LITTLE_ENDIAN) {
            tmp.putInt(0, val);
            tmp.putInt(4, 0);
        } else {
            tmp.putInt(0, 0);
            tmp.putInt(4, val);
        }
        return tmp.getLong();
    }

    /**
     * Read a uint32 value from the buffer at the given position.
     * @param buffer ByteBuffer to read from.
     * @param position Position to read.
     * @return Uint32 value.
     */
    public static long readUInt32(final ByteBuffer buffer, final int position) {
        final ByteBuffer roBuffer = buffer.asReadOnlyBuffer();
        roBuffer.order(buffer.order());
        roBuffer.position(position);
        return ByteBufferUtils.readUInt32(roBuffer);
    }

    /**
     * Put a UInt32 value to the buffer.
     * @param buffer ByteBuffer to write to.
     * @param value Value to write.
     */
    public static void writeUInt32(final ByteBuffer buffer, final long value) {
        final ByteBuffer tmp = ByteBuffer.allocate(8);
        final ByteOrder order = buffer.order();
        tmp.order(order);
        tmp.putLong(value);

        if (order == ByteOrder.LITTLE_ENDIAN) {
            final int val = tmp.getInt(0);
            buffer.putInt(val);
        } else {
            final int val = tmp.getInt(4);
            buffer.putInt(val);
        }
    }

    /**
     * Read a uint32 + String value from the buffers current position.
     * @param buffer ByteBuffer to read from.
     * @return String value.
     */
    public static String readString(final ByteBuffer buffer) {
        final int length = (int) ByteBufferUtils.readUInt32(buffer);
        final byte[] encoded = new byte[length];
        buffer.get(encoded);
        return new String(encoded, StandardCharsets.UTF_8);
    }

    /**
     * Read a uint32 + String from the buffer at the given position.
     * @param buffer ByteBuffer to read from.
     * @param position Position to read.
     * @return String value.
     */
    public static String readString(final ByteBuffer buffer, final int position) {
        final ByteBuffer roBuffer = buffer.asReadOnlyBuffer();
        roBuffer.order(buffer.order());
        roBuffer.position(position);
        return ByteBufferUtils.readString(roBuffer);
    }

    /**
     * Write a UInt32 + String to the buffer at the current position.
     * @param buffer ByteBuffer to write to.
     * @param value String to write.
     */
    public static void writeString(final ByteBuffer buffer, final String value) {
        final byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        ByteBufferUtils.writeUInt32(buffer, bytes.length);
        buffer.put(bytes);
    }

}
