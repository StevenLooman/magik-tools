package nl.ramsolutions.sw.magik.debugadapter.slap;

/**
 * Modify breakpoitn.
 */
@SuppressWarnings("checkstyle:JavadocVariable")
public enum ModifyBreakpoint {

    DELETE(0),
    DISABLE(1),
    ENABLE(2);

    private final int val;

    ModifyBreakpoint(final int val) {
        this.val = val;
    }

    public int getVal() {
        return this.val;
    }

    /**
     * Get the {{ModifyBreakpoint}} from an interger value.
     * @param value Integer value.
     * @return ModifyBreakpoint
     */
    public static ModifyBreakpoint valueOf(final int value) {
        for (final ModifyBreakpoint breakpointModifier : ModifyBreakpoint.values()) {
            if (breakpointModifier.getVal() == value) {
                return breakpointModifier;
            }
        }

        return null;
    }

}
