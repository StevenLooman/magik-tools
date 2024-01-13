package nl.ramsolutions.sw.magik.analysis.typing.reasoner;

import com.sonar.sslr.api.AstNode;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.CombinedType;
import nl.ramsolutions.sw.magik.analysis.typing.types.SelfType;
import nl.ramsolutions.sw.magik.analysis.typing.types.Slot;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.types.UndefinedType;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/**
 * Atom handler.
 */
class AtomHandler extends LocalTypeReasonerHandler {

    @SuppressWarnings("checkstyle:MagicNumber")
    private static final long BIGNUM_START = 1 << 29;

    /**
     * Constructor.
     * @param state Reasoner state.
     */
    AtomHandler(final LocalTypeReasonerState state) {
        super(state);
    }

    /**
     * Handle number.
     * @param node NUMBER node.
     */
    void handleNumber(final AstNode node) {
        final String tokenValue = node.getTokenValue();

        // Parsable by Long?
        try {
            Long value = Long.parseLong(tokenValue);
            if (value > BIGNUM_START) {
                this.assignAtom(node, TypeString.SW_BIGNUM);
            } else {
                this.assignAtom(node, TypeString.SW_INTEGER);
            }
            return;
        } catch (NumberFormatException ex) {
            // pass
        }

        // Parsable by Float?
        try {
            Float.parseFloat(tokenValue);
            this.assignAtom(node, TypeString.SW_FLOAT);
        } catch (NumberFormatException ex) {
            // pass
        }
    }

    /**
     * Handle self.
     * @param node SELF node.
     */
    void handleSelf(final AstNode node) {
        this.assignAtom(node, SelfType.INSTANCE);
    }

    /**
     * Handle clone.
     * @param node CLONE node.
     */
    void handleClone(final AstNode node) {
        this.handleSelf(node);
    }

    /**
     * Handle true/false.
     * @param node TRUE/FALSE node.
     */
    void handleFalse(final AstNode node) {
        this.assignAtom(node, TypeString.SW_FALSE);
    }

    /**
     * Handle maybe.
     * @param node MAYBE node.
     */
    void handleMaybe(final AstNode node) {
        this.assignAtom(node, TypeString.SW_MAYBE);
    }

    /**
     * Handle unset.
     * @param node UNSET node.
     */
    void handleUnset(final AstNode node) {
        this.assignAtom(node, TypeString.SW_UNSET);
    }

    /**
     * Handle character.
     * @param node CHARACTER node.
     */
    void handleCharacter(final AstNode node) {
        this.assignAtom(node, TypeString.SW_CHARACTER);
    }

    /**
     * Handle regexp.
     * @param node SW_REGEXP node.
     */
    void handleRegexp(final AstNode node) {
        this.assignAtom(node, TypeString.SW_SW_REGEXP);
    }

    /**
     * Handle string.
     * @param node STRING node.
     */
    void handleString(final AstNode node) {
        this.assignAtom(node, TypeString.SW_CHAR16_VECTOR_WITH_GENERICS);
    }

    /**
     * Handle symbol.
     * @param node SYMBOL node.
     */
    void handleSymbol(final AstNode node) {
        this.assignAtom(node, TypeString.SW_SYMBOL);
    }

    /**
     * Handle simple vector.
     * @param node SIMPLE_VECTOR node.
     */
    void handleSimpleVector(final AstNode node) {
        // Find all child expression types, and use that for generic <E>.
        final List<AbstractType> containedTypes = node.getChildren(MagikGrammar.EXPRESSION).stream()
            .map(this.state::getNodeType)
            .map(result -> result.get(0, UndefinedType.INSTANCE))
            .collect(Collectors.toList());
        if (!containedTypes.isEmpty()) {
            final AbstractType[] combinedTypesArr = containedTypes.toArray(AbstractType[]::new);
            final AbstractType combinedType = CombinedType.combine(combinedTypesArr);
            final TypeString genericsTypeString = TypeString.ofIdentifier(
                TypeString.SW_SIMPLE_VECTOR.getIdentifier(), TypeString.SW_SIMPLE_VECTOR.getPakkage(),
                TypeString.ofGenericDefinition("E", combinedType.getTypeString()));
            this.assignAtom(node, genericsTypeString);
            return;
        }

        this.assignAtom(node, TypeString.SW_SIMPLE_VECTOR);
    }

    /**
     * Handle global reference.
     * @param node GLOBAL_REF node.
     */
    void handleGlobalRef(final AstNode node) {
        this.assignAtom(node, TypeString.SW_GLOBAL_VARIABLE);
    }

    /**
     * Handle thisthread.
     * @param node THISTHREAD node.
     */
    void handleThread(final AstNode node) {
        final AbstractType heavyThreadType = this.typeKeeper.getType(TypeString.SW_HEAVY_THREAD);
        final AbstractType lightThreadType = this.typeKeeper.getType(TypeString.SW_LIGHT_THREAD);
        final AbstractType threadType = CombinedType.combine(lightThreadType, heavyThreadType);
        this.assignAtom(node, threadType);
    }

    /**
     * Handle slot.
     * @param node SLOT node.
     */
    void handleSlot(final AstNode node) {
        // Get class type.
        final AbstractType type = this.getMethodOwnerType(node);
        if (type == UndefinedType.INSTANCE) {
            return;
        }

        // Get slot type.
        final AstNode identifierNode = node.getFirstChild(MagikGrammar.IDENTIFIER);
        final String slotName = identifierNode.getTokenValue();
        final Slot slot = type.getSlot(slotName);
        final TypeString slotTypeStr = slot != null
            ? slot.getType()
            : TypeString.UNDEFINED;
        final AbstractType slotType = this.typeReader.parseTypeString(slotTypeStr);
        Objects.requireNonNull(slotType);

        this.assignAtom(node, slotType);
    }

}
