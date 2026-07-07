package nl.ramsolutions.sw.magik.analysis.helpers;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonar.sslr.api.AstNode;
import java.util.Map;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.parser.MagikParser;
import org.junit.jupiter.api.Test;

/** Tests for {@link MethodDefinitionNodeHelper}. */
class MethodDefinitionNodeHelperTest {

  private AstNode parseMagik(final String code) {
    final MagikParser parser = new MagikParser();
    return parser.parseSafe(code);
  }

  @Test
  void testGetParameterNodes() {
    final String code =
        """
        _method a.m(p1, p2)
        _endmethod
        """;
    final AstNode topNode = this.parseMagik(code);
    final AstNode methodNode = topNode.getFirstDescendant(MagikGrammar.METHOD_DEFINITION);
    final MethodDefinitionNodeHelper helper = new MethodDefinitionNodeHelper(methodNode);
    final Map<String, AstNode> parameterNodes = helper.getParameterNodes();
    assertThat(parameterNodes).containsOnlyKeys("p1", "p2");
  }

  @Test
  void testGetParameterNodesDuplicateNames() {
    final String code =
        """
        _method a.m(p1, p1)
        _endmethod
        """;
    final AstNode topNode = this.parseMagik(code);
    final AstNode methodNode = topNode.getFirstDescendant(MagikGrammar.METHOD_DEFINITION);
    final MethodDefinitionNodeHelper helper = new MethodDefinitionNodeHelper(methodNode);
    final Map<String, AstNode> parameterNodes = helper.getParameterNodes();
    assertThat(parameterNodes).containsOnlyKeys("p1");
  }
}
