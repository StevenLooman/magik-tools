package nl.ramsolutions.sw.magik.analysis.helpers;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonar.sslr.api.AstNode;
import java.util.ArrayList;
import java.util.List;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.parser.MagikParser;
import org.junit.jupiter.api.Test;

/** Tests for {@link AssignmentExpressionNodeHelper}. */
class AssignmentExpressionNodeHelperTest {

  private List<String> describeSlotTargets(final String code) {
    final MagikParser parser = new MagikParser();
    final AstNode topNode = parser.parseSafe(code);
    final List<AstNode> slotNodes = topNode.getDescendants(MagikGrammar.SLOT);
    final List<String> described = new ArrayList<>();
    for (final AstNode slotNode : slotNodes) {
      final AstNode atomNode = slotNode.getParent();
      final boolean target = AssignmentExpressionNodeHelper.isAssignmentTarget(atomNode);
      final AstNode identifierNode = slotNode.getFirstChild(MagikGrammar.IDENTIFIER);
      final String name = identifierNode.getTokenValue();
      described.add(name + "=" + target);
    }
    return described;
  }

  @Test
  void testIsAssignmentTarget() {
    final String code =
        """
        _method a.m(n)
        \t.count << .count - n
        \t.total +<< n
        \t(.first, .last) << (1, 2)
        \t.boot ^<< .items[1]
        \t.items[1] << n
        \t_return .location
        _endmethod
        """;

    final List<String> targets = this.describeSlotTargets(code);

    assertThat(targets)
        .containsExactly(
            "count=true",
            "count=false",
            "total=true",
            "first=true",
            "last=true",
            "boot=true",
            "items=false",
            "items=false",
            "location=false");
  }
}
