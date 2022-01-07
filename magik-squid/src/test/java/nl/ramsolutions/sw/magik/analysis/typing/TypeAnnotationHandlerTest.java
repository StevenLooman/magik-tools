package nl.ramsolutions.sw.magik.analysis.typing;

import com.sonar.sslr.api.AstNode;
import java.io.IOException;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.parser.MagikParser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test TypeAnnotationHandler.
 */
class TypeAnnotationHandlerTest {

    private AstNode parseMagik(String code) {
        final MagikParser parser = new MagikParser();
        return parser.parseSafe(code);
    }

    @Test
    void testTypeForExpression() throws IOException {
        final String code = ""
            + "_method a.b\n"
            + "  _local x << fn()  # type: sw:integer\n"
            + "_endmethod";
        final AstNode topNode = this.parseMagik(code);
        final AstNode expressionNode = topNode.getFirstDescendant(MagikGrammar.EXPRESSION);

        final String typeAnnotation = TypeAnnotationHandler.typeAnnotationForExpression(expressionNode);
        assertThat(typeAnnotation).isEqualTo("sw:integer");
        final String iterTypeAnnotation = TypeAnnotationHandler.iterTypeAnnotationForExpression(expressionNode);
        assertThat(iterTypeAnnotation).isNull();
    }

    @Test
    void testTypeForExpressionMultiple() throws IOException {
        final String code = ""
            + "_method a.b\n"
            + "    _local x << fn() + fn2()  # type: sw:integer\n"
            + "_endmethod";
        final AstNode topNode = this.parseMagik(code);
        final AstNode expressionNode = topNode.getFirstDescendant(MagikGrammar.EXPRESSION);

        final String typeAnnotation = TypeAnnotationHandler.typeAnnotationForExpression(expressionNode);
        assertThat(typeAnnotation).isEqualTo("sw:integer");
        final String iterTypeAnnotation = TypeAnnotationHandler.iterTypeAnnotationForExpression(expressionNode);
        assertThat(iterTypeAnnotation).isNull();
    }

    @Test
    void testTypeForExpressionMultipleLines1() throws IOException {
        // This is not supported, thus should return null.
        final String code = ""
            + "_method a.b\n"
            + "  _local x <<\n"
            + "    fn() +  # type: sw:integer\n"
            + "    fn2()\n"
            + "_endmethod";
        final AstNode topNode = this.parseMagik(code);
        final AstNode expressionNode = topNode.getFirstDescendant(MagikGrammar.EXPRESSION);

        final String typeAnnotation = TypeAnnotationHandler.typeAnnotationForExpression(expressionNode);
        assertThat(typeAnnotation).isEqualTo("sw:integer");
        final String iterTypeAnnotation = TypeAnnotationHandler.iterTypeAnnotationForExpression(expressionNode);
        assertThat(iterTypeAnnotation).isNull();
    }

    @Test
    void testTypeForExpressionMultipleLines2() throws IOException {
        final String code = ""
            + "_method a.b\n"
            + "  _local x <<\n"
            + "    fn() + \n"
            + "    fn2()    # type: sw:integer\n"
            + "_endmethod\n";
        final AstNode topNode = this.parseMagik(code);
        final AstNode expressionNode = topNode.getFirstDescendant(MagikGrammar.EXPRESSION);

        final String typeAnnotation = TypeAnnotationHandler.typeAnnotationForExpression(expressionNode);
        assertThat(typeAnnotation).isNull();
        final String iterTypeAnnotation = TypeAnnotationHandler.iterTypeAnnotationForExpression(expressionNode);
        assertThat(iterTypeAnnotation).isNull();
    }

    @Test
    void testIterTypeForForExpression() throws IOException {
        final String code = ""
            + "_method a.b\n"
            + "  _for x, y _over fn()  # iter-type: sw:integer, sw:float\n"
            + "  _loop\n"
            + "  _endloop\n"
            + "_endmethod";
        final AstNode topNode = this.parseMagik(code);
        final AstNode iterableExpressionNode = topNode.getFirstDescendant(MagikGrammar.ITERABLE_EXPRESSION);

        final String typeAnnotation = TypeAnnotationHandler.typeAnnotationForExpression(iterableExpressionNode);
        assertThat(typeAnnotation).isNull();
        final String iterTypeAnnotation = TypeAnnotationHandler.iterTypeAnnotationForExpression(iterableExpressionNode);
        assertThat(iterTypeAnnotation).isEqualTo("sw:integer, sw:float");
    }

}
