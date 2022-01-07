package nl.ramsolutions.sw.magik.analysis.typing.types;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import nl.ramsolutions.sw.magik.analysis.Location;
import nl.ramsolutions.sw.magik.api.MagikKeyword;

/**
 * Magik method.
 */
public class Method {

    /**
     * Method modifier.
     */
    @SuppressWarnings("checkstyle:JavadocVariable")
    public enum Modifier {

        ABSTRACT(MagikKeyword.ABSTRACT),
        PRIVATE(MagikKeyword.PRIVATE),
        ITER(MagikKeyword.ITER);

        private final MagikKeyword keyword;

        Modifier(final @Nullable MagikKeyword keyword) {
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

    private final EnumSet<Modifier> modifiers;
    private final Location location;
    private final MagikType owner;
    private final List<Parameter> parameters;
    private final Parameter assignmentParameter;
    private final ExpressionResult callResult;
    private final ExpressionResult loopbodyResult;
    private final String name;
    private String doc;
    private final Set<String> usedTypes;
    private final Set<String> calledMethods;
    private final Set<String> usedSlots;

    /**
     * Constructor.
     * @param name Name of method.
     * @param parameters Parameters of method.
     * @param callResult Result of call.
     * @param loopbodyResult Result of iterator call.
     */
    @SuppressWarnings({"java:S1319", "checkstyle:ParameterNumber"})
    public Method(
            final EnumSet<Modifier> modifiers,
            final @Nullable Location location,
            final MagikType owner,
            final String name,
            final List<Parameter> parameters,
            final @Nullable Parameter assignmentParameter,
            final ExpressionResult callResult,
            final ExpressionResult loopbodyResult) {
        this.modifiers = modifiers;
        this.location = location;
        this.owner = owner;
        this.name = name;
        this.parameters = parameters;
        this.assignmentParameter = assignmentParameter;
        this.callResult = callResult;
        this.loopbodyResult = loopbodyResult;
        this.usedTypes = new HashSet<>();
        this.calledMethods = new HashSet<>();
        this.usedSlots = new HashSet<>();
    }

    /**
     * Get method modifiers.
     * @return Method modifiers.
     */
    public EnumSet<Modifier> getModifiers() {
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
            signature = signature.substring(bracketIndex);
        }
        int dotIndex = signature.indexOf(".");
        if (dotIndex != -1) {
            signature = signature.substring(dotIndex + 1);
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
    public ExpressionResult getCallResult() {
        return this.callResult;
    }

    /**
     * Get result of iterator call.
     * @return Result of iterator call.
     */
    public ExpressionResult getLoopbodyResult() {
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
    public String getDoc() {
        return this.doc;
    }

    /**
     * Add a used type.
     * @param typeName Type name, including package.
     */
    public void addUsedType(final String typeName) {
        this.usedTypes.add(typeName);
    }

    /**
     * Add a used type.
     * @param type Used type.
     */
    public void addUsedType(final AbstractType type) {
        final String typeName = type.getFullName();
        this.addUsedType(typeName);
    }

    /**
     * Get the used types.
     * @return All used types by this method.
     */
    public Set<String> getUsedTypes() {
        return Collections.unmodifiableSet(this.usedTypes);
    }

    /**
     * Add a called method.
     * @param methodName Name of called method.
     */
    public void addCalledMethod(final String methodName) {
        this.calledMethods.add(methodName);
    }

    /**
     * Get the method names by this method.
     * @return Called method names.
     */
    public Set<String> getCalledMethods() {
        return Collections.unmodifiableSet(this.calledMethods);
    }

    public void addUsedSlot(final String slotName) {
        this.usedSlots.add(slotName);
    }

    public Set<String> getUsedSlots() {
        return Collections.unmodifiableSet(this.usedSlots);
    }

    @Override
    public String toString() {
        return String.format(
            "%s@%s(%s.%s)",
            this.getClass().getName(), Integer.toHexString(this.hashCode()),
            this.getOwner().getFullName(), this.getName());
    }

}
