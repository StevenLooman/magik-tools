package nl.ramsolutions.sw.magik.analysis.typing.types;

import java.util.Collections;
import java.util.EnumSet;
import nl.ramsolutions.sw.magik.analysis.typing.TypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeReader;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for MagikType.
 */
class MagikTypeTest {

    @Test
    void testCreateGenericType1() {
        final TypeKeeper typeKeeper = new TypeKeeper();
        final TypeString propertyListRef = TypeString.ofIdentifier("property_list", "sw");
        final MagikType magikType = new MagikType(typeKeeper, null, null, MagikType.Sort.SLOTTED, propertyListRef);
        final GenericDeclaration key = magikType.addGeneric(null, "K");
        final GenericDeclaration element = magikType.addGeneric(null, "E");

        final Method method = magikType.addMethod(
            null,
            null,
            EnumSet.noneOf(Method.Modifier.class),
            "fast_keys_and_elements()",
            Collections.emptyList(),
            null,
            null,
            new ExpressionResultString(),
            new ExpressionResultString(
                TypeString.ofGeneric("K"),
                TypeString.ofGeneric("E")));

        final TypeReader typeReader = new TypeReader(typeKeeper);
        final ExpressionResultString loopbodyResultString = method.getLoopbodyResult();
        final ExpressionResult loopbody = typeReader.parseExpressionResultString(loopbodyResultString);
        final GenericDeclaration genericKey = (GenericDeclaration) loopbody.get(0, null);
        assertThat(genericKey)
            .isEqualTo(key);

        final GenericDeclaration genericElement = (GenericDeclaration) loopbody.get(1, null);
        assertThat(genericElement)
            .isEqualTo(element);
    }

}
