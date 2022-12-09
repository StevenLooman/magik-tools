package nl.ramsolutions.sw.magik.debugadapter.slap.responses;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.debugadapter.slap.ByteBufferHelper;
import nl.ramsolutions.sw.magik.debugadapter.slap.ISlapResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.RequestType;

/**
 * Stack frame locals response.
 */
@SuppressWarnings("JavadocVariable")
public class StackFrameLocalsResponse implements ISlapResponse {

    // Response layout:
    //  0- 4: uint32, message length
    //  4- 8: uint32, response type
    //  8-12: uint32, request type
    // 12-16: uint32, status
    // 16-20: uint32, name length
    // 20-..: string, name
    // ..-..: uint32, value, if type = int/short/char/byte
    // ..-..: bool, value, if type = bool
    // ..-..: double, value, if type = double
    // ..-..: long, value, if type = long
    // ..-..: string, value, if type = obj
    // ????
    public static final int OFFSET_STATUS = 12;
    public static final int OFFSET_NAME_LENGTH = 16;
    public static final int OFFSET_NAME = 20;

    /**
     * Variable type.
     */
    @SuppressWarnings("checkstyle:JavadocVariable")
    public enum LocalType {

        TYPE_NONE(0),

        TYPE_INT('I'),
        TYPE_SHORT('S'),
        TYPE_CHAR('C'),
        TYPE_BYTE('B'),
        TYPE_BOOL('Z'),
        TYPE_LONG('J'),
        TYPE_FLOAT('F'),
        TYPE_DOUBLE('D'),
        TYPE_OBJ(0xFF);

        private final int val;

        LocalType(final int val) {
            this.val = val;
        }

        public int getVal() {
            return this.val;
        }

        /**
         * Get the {@link LocalType} from an interger value.
         * @param value Integer value.
         * @return LocalType
         */
        public static LocalType valueOf(final int value) {
            for (final LocalType localFlag : LocalType.values()) {
                if (localFlag.getVal() == value) {
                    return localFlag;
                }
            }

            return LocalType.TYPE_NONE;
        }

    }

    /**
     * Variable type.
     */
    @SuppressWarnings("checkstyle:JavadocVariable")
    public enum VariableType {

        ARGUMENT(1),
        INVALID(2),
        ANONYMOUS(4),
        SLOT(8);

        private final int val;

        VariableType(final int val) {
            this.val = val;
        }

        public int getVal() {
            return this.val;
        }

        /**
         * Get the {@link VariableType}s from an interger value.
         * @param flags Integer value.
         * @return VariableTypes
         */
        public static Set<VariableType> flagsOf(final int flags) {
            final Set<VariableType> set = EnumSet.noneOf(VariableType.class);
            for (final VariableType flag : EnumSet.allOf(VariableType.class)) {
                if ((flags & flag.getVal()) != 0) {
                    set.add(flag);
                }
            }
            return set;
        }

    }

    /**
     * Local varaible.
     */
    public static class Local implements ISlapResponse {

        private LocalType type;
        private String name;
        private String value;
        private Set<VariableType> variableTypes;

        /**
         * Constructor.
         * @param type Type.
         * @param name Name.
         * @param value Value.
         * @param variableTypes Variable types.
         */
        public Local(
                final LocalType type,
                final String name,
                final String value,
                final Set<VariableType> variableTypes) {
            this.type = type;
            this.name = name;
            this.value = value;
            this.variableTypes = variableTypes;
        }

        public LocalType getLocalType() {
            return this.type;
        }

        public String getName() {
            return this.name;
        }

        public String getValue() {
            return this.value;
        }

        public Set<VariableType> getVariableTypes() {
            return this.variableTypes;
        }

        /**
         * Decode message from buffer.
         * @param buffer Buffer containing message.
         * @return Decoded message.
         */
        @SuppressWarnings("checkstyle:AvoidNestedBlocks")
        public static Local decode(final ByteBuffer buffer) {
            final int status = (int) ByteBufferHelper.readUInt32(buffer, OFFSET_STATUS);
            final Set<VariableType> variableTypes = VariableType.flagsOf(status);
            final LocalType type = LocalType.valueOf((status & BYTE_2_MASK) >> BYTE_2_SHIFT);
            final int nameLength = (int) ByteBufferHelper.readUInt32(buffer, OFFSET_NAME_LENGTH);
            final String name = ByteBufferHelper.readString(buffer, OFFSET_NAME_LENGTH);

            String valueStr = null;
            final int valueOffset = OFFSET_NAME + nameLength;
            switch (type) {
                case TYPE_CHAR: {
                    char value = (char) buffer.getInt(valueOffset);
                    valueStr = Character.toString(value);
                    break;
                }

                case TYPE_LONG: {
                    long value = buffer.getLong(valueOffset);
                    valueStr = Long.toString(value);
                    break;
                }

                case TYPE_BYTE:
                case TYPE_SHORT:
                case TYPE_INT: {
                    int value = buffer.getInt(valueOffset);
                    valueStr = Integer.toString(value);
                    break;
                }

                case TYPE_BOOL: {
                    boolean value = buffer.getInt(valueOffset) != 0;
                    valueStr = Boolean.toString(value);
                    break;
                }

                case TYPE_FLOAT: {
                    Float value = buffer.getFloat(valueOffset);
                    valueStr = Float.toString(value);
                    break;
                }

                case TYPE_DOUBLE: {
                    Double value = buffer.getDouble(valueOffset);
                    valueStr = Double.toString(value);
                    break;
                }

                case TYPE_OBJ: {
                    valueStr = ByteBufferHelper.readString(buffer, valueOffset);
                    break;
                }

                case TYPE_NONE:
                default:
                    valueStr = "<none>";
                    break;
            }

            return new Local(type, name, valueStr, variableTypes);
        }

        @Override
        public RequestType getRequestType() {
            return RequestType.GET_FRAME_LOCALS;
        }
    }

    private final List<Local> locals;

    /**
     * Constructor.
     * @param subResponses Locals.
     */
    public StackFrameLocalsResponse(final List<ISlapResponse> subResponses) {
        this.locals = subResponses.stream()
            .map(Local.class::cast)
            .collect(Collectors.toList());
    }

    public List<Local> getLocals() {
        return Collections.unmodifiableList(this.locals);
    }

    @Override
    public RequestType getRequestType() {
        return RequestType.GET_FRAME_LOCALS;
    }

}
