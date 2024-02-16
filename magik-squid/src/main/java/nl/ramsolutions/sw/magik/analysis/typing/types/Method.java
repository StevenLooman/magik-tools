package nl.ramsolutions.sw.magik.analysis.typing.types;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.api.MagikKeyword;

/**
 * Magik method.
 */
public class Method {

    /**
     * Global usage.
     */
    public static class GlobalUsage {

        private final TypeString ref;
        private final Location location;

        /**
         * Constructor.
         * @param ref Global reference.
         * @param location Location of use.
         */
        public GlobalUsage(final TypeString ref, final @Nullable Location location) {
            this.ref = ref;
            this.location = location;
        }

        public TypeString getGlobal() {
            return this.ref;
        }

        @CheckForNull
        public Location getLocation() {
            return this.location;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.ref);
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj == null) {
                return false;
            }

            if (this.getClass() != obj.getClass()) {
                return false;
            }

            final GlobalUsage otherTypeUsage = (GlobalUsage) obj;
            return Objects.equals(otherTypeUsage.getGlobal(), this.getGlobal());
        }

        @Override
        public String toString() {
            return String.format(
                "%s@%s(%s)",
                this.getClass().getName(), Integer.toHexString(this.hashCode()),
                this.getGlobal());
        }

    }

    /**
     * Method usage.
     */
    public static class MethodUsage {

        private final TypeString typeRef;
        private final String methodName;
        private final Location location;

        /**
         * Constructor.
         * @param typeRef Type reference.
         * @param methodName Name of method.
         * @param location Location of use.
         */
        public MethodUsage(final TypeString typeRef, final String methodName, final @Nullable Location location) {
            this.typeRef = typeRef;
            this.methodName = methodName;
            this.location = location;
        }

        /**
         * Constructor.
         * @param typeRef Type reference.
         * @param methodName Name of method.
         */
        public MethodUsage(final TypeString typeRef, final String methodName) {
            this(typeRef, methodName, null);
        }

        public TypeString getType() {
            return this.typeRef;
        }

        public String getMethodName() {
            return this.methodName;
        }

        @CheckForNull
        public Location getLocation() {
            return this.location;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.typeRef, this.methodName);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj == null) {
                return false;
            }

            if (this.getClass() != obj.getClass()) {
                return false;
            }

            final MethodUsage otherMethodUsage = (MethodUsage) obj;
            return Objects.equals(otherMethodUsage.getType(), this.getType())
                && Objects.equals(otherMethodUsage.getMethodName(), this.getMethodName());
        }

    }

    /**
     * Slot usage.
     */
    public static class SlotUsage {

        private final String slotName;
        private final Location location;

        /**
         * Constructor.
         * @param slotName Name of slot.
         * @param location Location of use.
         */
        public SlotUsage(final String slotName, final @Nullable Location location) {
            this.slotName = slotName;
            this.location = location;
        }

        /**
         * Constructor.
         * @param slotName Name of slot.
         */
        public SlotUsage(final String slotName) {
            this(slotName, null);
        }

        public String getSlotName() {
            return this.slotName;
        }

        public Location getLocation() {
            return this.location;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.slotName);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj == null) {
                return false;
            }

            if (this.getClass() != obj.getClass()) {
                return false;
            }

            final SlotUsage otherSlotUsage = (SlotUsage) obj;
            return Objects.equals(otherSlotUsage.getSlotName(), this.getSlotName());
        }

    }

    /**
     * Condition usage.
     */
    public static class ConditionUsage {

        private final String conditionName;
        private final Location location;

        /**
         * Constructor.
         * @param conditionName Name of condition.
         * @param location Location of use.
         */
        public ConditionUsage(final String conditionName, final @Nullable Location location) {
            this.conditionName = conditionName;
            this.location = location;
        }

        /**
         * Constructor.
         * @param conditionName Name of condition.
         */
        public ConditionUsage(final String conditionName) {
            this(conditionName, null);
        }

        public String getConditionName() {
            return this.conditionName;
        }

        public Location getLocation() {
            return this.location;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.conditionName);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj == null) {
                return false;
            }

            if (this.getClass() != obj.getClass()) {
                return false;
            }

            final ConditionUsage otherConditionUsage = (ConditionUsage) obj;
            return Objects.equals(otherConditionUsage.getConditionName(), this.getConditionName());
        }

    }

    /**
     * Method modifier.
     */
    @SuppressWarnings("checkstyle:JavadocVariable")
    public enum Modifier {

        ABSTRACT(MagikKeyword.ABSTRACT),
        PRIVATE(MagikKeyword.PRIVATE),
        ITER(MagikKeyword.ITER);

        private final MagikKeyword keyword;

        Modifier(final MagikKeyword keyword) {
            this.keyword = keyword;
        }

        /**
         * Get modifier value.
         * @return Modifier value.
         */
        public String getValue() {
            return this.keyword.getValue();
        }

    }

    private final Location location;
    private final EnumSet<Modifier> modifiers;
    private final MagikType owner;
    private final List<Parameter> parameters;
    private final Parameter assignmentParameter;
    private final ExpressionResultString callResult;
    private final ExpressionResultString loopbodyResult;
    private final String name;
    private final Set<GlobalUsage> usedGlobals;
    private final Set<MethodUsage> calledMethods;
    private final Set<SlotUsage> usedSlots;
    private final Set<ConditionUsage> usedConditions;
    private final String moduleName;
    private String doc;

    /**
     * Constructor.
     * @param location Location of definition.
     * @param moduleName Module name where this method is defined.
     * @param modifiers Modifiers.
     * @param owner Owner of method.
     * @param name Name of method.
     * @param parameters Parameters of method.
     * @param assignmentParameter Assignment parameter.
     * @param methodDoc Method doc.
     * @param callResult Result of call.
     * @param loopbodyResult Result of iterator call.
     */
    @SuppressWarnings({"java:S1319", "checkstyle:ParameterNumber"})
    public Method(//NOSONAR
            final @Nullable Location location,
            final @Nullable String moduleName,
            final Set<Modifier> modifiers,
            final MagikType owner,
            final String name,
            final List<Parameter> parameters,
            final @Nullable Parameter assignmentParameter,
            final @Nullable String methodDoc,
            final ExpressionResultString callResult,
            final ExpressionResultString loopbodyResult) {
        this.location = location;
        this.modifiers = !modifiers.isEmpty()
            ? EnumSet.copyOf(modifiers)
            : EnumSet.noneOf(Modifier.class);
        this.owner = owner;
        this.name = name;
        this.parameters = parameters;
        this.assignmentParameter = assignmentParameter;
        this.doc = methodDoc;
        this.callResult = callResult;
        this.loopbodyResult = loopbodyResult;
        this.moduleName = moduleName;
        this.usedGlobals = new HashSet<>();
        this.calledMethods = new HashSet<>();
        this.usedSlots = new HashSet<>();
        this.usedConditions = new HashSet<>();
    }

    /**
     * Get method modifiers.
     * @return Method modifiers.
     */
    public Set<Modifier> getModifiers() {
        return this.modifiers;
    }

    /**
     * Get Location of the file containing this method.
     * @return Location to/in file.
     */
    @CheckForNull
    public Location getLocation() {
        return this.location;
    }

    /**
     * Get owner of method.
     * @return Owner of method.
     */
    public MagikType getOwner() {
        return this.owner;
    }

    /**
     * Get name of method.
     * @return Name of method.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Get name of method, with parameters.
     * @return Name of method, with parameters.
     */
    public String getNameWithParameters() {
        String signature = this.getSignature();
        final int bracketIndex = signature.indexOf("[");
        if (bracketIndex != -1) {
            return signature.substring(bracketIndex);
        }

        final int dotIndex = signature.indexOf(".");
        if (dotIndex != -1) {
            return signature.substring(dotIndex + 1);
        }

        return signature;
    }

    /**
     * Get the signature of method.
     * @return Signature of method.
     */
    public String getSignature() {
        final StringBuilder builder = new StringBuilder();

        // Type name.
        final String ownerName = this.owner.getFullName();
        builder.append(ownerName);

        // Determine method name with parameters.
        final String methodName = this.getName();
        final StringBuilder parametersBuilder = new StringBuilder();
        boolean firstParameter = true;
        Parameter.Modifier currentModifier = Parameter.Modifier.NONE;
        for (final Parameter parameter : this.parameters) {
            if (firstParameter) {
                firstParameter = false;
            } else {
                parametersBuilder.append(", ");
            }

            final Parameter.Modifier newModifier = parameter.getModifier();
            if (currentModifier != newModifier) {
                parametersBuilder.append(newModifier.getValue());
                parametersBuilder.append(" ");
            }
            currentModifier = newModifier;

            parametersBuilder.append(parameter.getName());
        }
        final String parametersStr = parametersBuilder.toString();

        if (methodName.startsWith("[")) {
            builder.append("[");
            builder.append(parametersStr);
            builder.append(methodName.substring(1)); // "]<<" or "]^<<""
        } else {
            builder.append(".");
            int bracketIndex = methodName.indexOf("(");
            if (bracketIndex != -1) {
                builder.append(methodName.substring(0, bracketIndex + 1));
                builder.append(parametersStr);
                builder.append(methodName.substring(bracketIndex + 1));
            } else {
                builder.append(methodName);
            }
        }

        final String assignmentParameterName = this.assignmentParameter != null
                ? this.assignmentParameter.getName()
                : null;
        if (assignmentParameterName != null) {
            builder.append(assignmentParameterName);
        }

        return builder.toString();
    }

    /**
     * Get parameters of method.
     * @return Parameters.
     */
    public List<Parameter> getParameters() {
        return Collections.unmodifiableList(this.parameters);
    }

    @CheckForNull
    public Parameter getAssignmentParameter() {
        return assignmentParameter;
    }

    /**
     * Get result of call.
     * @return Result of call.
     */
    public ExpressionResultString getCallResult() {
        return this.callResult;
    }

    /**
     * Get result of iterator call.
     * @return Result of iterator call.
     */
    public ExpressionResultString getLoopbodyResult() {
        return this.loopbodyResult;
    }

    /**
     * Set the method doc/headers.
     * @param methodDoc Method doc.
     */
    public void setDoc(final String methodDoc) {
        this.doc = methodDoc;
    }

    /**
     * Get the method doc/headers.
     * @return Method doc.
     */
    @CheckForNull
    public String getDoc() {
        return this.doc;
    }

    /**
     * Get name of module this method is defined in.
     * @return
     */
    @CheckForNull
    public String getModuleName() {
        return this.moduleName;
    }

    /**
     * Add a used global.
     * @param globalUsage Global usage.
     */
    public void addUsedGlobal(final GlobalUsage globalUsage) {
        this.usedGlobals.add(globalUsage);
    }

    /**
     * Get the used types.
     * @return All used types by this method.
     */
    public Set<GlobalUsage> getGlobalUsages() {
        return Collections.unmodifiableSet(this.usedGlobals);
    }

    /**
     * Add a called method.
     * @param calledMethod Name of called method.
     */
    public void addCalledMethod(final MethodUsage calledMethod) {
        this.calledMethods.add(calledMethod);
    }

    /**
     * Get the method usages by this method.
     * @return Method usages.
     */
    public Set<Method.MethodUsage> getMethodUsages() {
        return Collections.unmodifiableSet(this.calledMethods);
    }

    /**
     * Add used slot.
     * @param slotUsage Name of used slot.
     */
    public void addUsedSlot(final SlotUsage slotUsage) {
        this.usedSlots.add(slotUsage);
    }

    /**
     * Get the slot usages by this method.
     * @return Slot usages.
     * @return
     */
    public Set<SlotUsage> getUsedSlots() {
        return Collections.unmodifiableSet(this.usedSlots);
    }

    public void addUsedCondition(final ConditionUsage conditionUsage) {
        this.usedConditions.add(conditionUsage);
    }

    /**
     * Get the condition usages by this method.
     * @return Condition usages.
     */
    public Set<Method.ConditionUsage> getConditionUsages() {
        return Collections.unmodifiableSet(this.usedConditions);
    }

    @Override
    public String toString() {
        return String.format(
            "%s@%s(%s.%s)",
            this.getClass().getName(), Integer.toHexString(this.hashCode()),
            this.getOwner().getFullName(), this.getName());
    }

}
