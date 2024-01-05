package nl.ramsolutions.sw.magik.analysis.typing.reasoner;

import com.sonar.sslr.api.AstNode;
import java.util.Objects;
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

    private static final TypeString SW_FALSE = TypeString.ofIdentifier("false", "sw");
    private static final TypeString SW_MAYBE = TypeString.ofIdentifier("maybe", "sw");
    private static final TypeString SW_CHARACTER = TypeString.ofIdentifier("character", "sw");
    private static final TypeString SW_BIGNUM = TypeString.ofIdentifier("bignum", "sw");
    private static final TypeString SW_INTEGER = TypeString.ofIdentifier("integer", "sw");
    private static final TypeString SW_FLOAT = TypeString.ofIdentifier("float", "sw");
    private static final TypeString SW_SW_REGEXP = TypeString.ofIdentifier("sw_regexp", "sw");
    private static final TypeString SW_CHAR16_VECTOR = TypeString.ofIdentifier("char16_vector", "sw");
    private static final TypeString SW_SYMBOL = TypeString.ofIdentifier("symbol", "sw");
    private static final TypeString SW_SIMPLE_VECTOR = TypeString.ofIdentifier("simple_vector", "sw");
    private static final TypeString SW_HEAVY_THREAD = TypeString.ofIdentifier("heavy_thread", "sw");
    private static final TypeString SW_LIGHT_THREAD = TypeString.ofIdentifier("light_thread", "sw");
    private static final TypeString SW_GLOBAL_VARIABLE = TypeString.ofIdentifier("global_variable", "sw");
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
                this.assignAtom(node, SW_BIGNUM);
            } else {
                this.assignAtom(node, SW_INTEGER);
            }
            return;
        } catch (NumberFormatException ex) {
            // pass
        }

        // Parsable by Float?
        try {
            Float.parseFloat(tokenValue);
            this.assignAtom(node, SW_FLOAT);
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
        this.assignAtom(node, SW_FALSE);
    }

    /**
     * Handle maybe.
     * @param node MAYBE node.
     */
    void handleMaybe(final AstNode node) {
        this.assignAtom(node, SW_MAYBE);
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
        this.assignAtom(node, SW_CHARACTER);
    }

    /**
     * Handle regexp.
     * @param node SW_REGEXP node.
     */
    void handleRegexp(final AstNode node) {
        this.assignAtom(node, SW_SW_REGEXP);
    }

    /**
     * Handle string.
     * @param node STRING node.
     */
    void handleString(final AstNode node) {
        this.assignAtom(node, SW_CHAR16_VECTOR);
    }

    /**
     * Handle symbol.
     * @param node SYMBOL node.
     */
    void handleSymbol(final AstNode node) {
        this.assignAtom(node, SW_SYMBOL);
    }

    /**
     * Handle simple vector.
     * @param node SIMPLE_VECTOR node.
     */
    void handleSimpleVector(final AstNode node) {
        this.assignAtom(node, SW_SIMPLE_VECTOR);  // TODO: Generics?
    }

    /**
     * Handle global reference.
     * @param node GLOBAL_REF node.
     */
    void handleGlobalRef(final AstNode node) {
        this.assignAtom(node, SW_GLOBAL_VARIABLE);
    }

    /**
     * Handle thisthread.
     * @param node THISTHREAD node.
     */
    void handleThread(final AstNode node) {
        final AbstractType heavyThreadType = this.typeKeeper.getType(SW_HEAVY_THREAD);
        final AbstractType lightThreadType = this.typeKeeper.getType(SW_LIGHT_THREAD);
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
