package nl.ramsolutions.sw.magik.debugadapter.slap;

/**
 * Step type.
 */
@SuppressWarnings("checkstyle:JavadocVariable")
public enum StepType {

    LINE(0),
    OUT(1),
    OVER(2),
    UNTIL_MAGIK(16);

    private final int val;

    StepType(final int val) {
        this.val = val;
    }

    public int getVal() {
        return this.val;
    }

    /**
     * Get the {{StepType}} from an interger value.
     * @param value Integer value.
     * @return StepType
     */
    public static StepType valueOf(final int value) {
        for (final StepType stepType : StepType.values()) {
            if (stepType.getVal() == value) {
                return stepType;
            }
        }

        return null;
    }
}
