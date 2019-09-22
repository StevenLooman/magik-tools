package org.stevenlooman.sw.magik.lint;

import static org.fest.assertions.Assertions.assertThat;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import com.sonar.sslr.api.AstNode;

import org.junit.Test;
import org.stevenlooman.sw.magik.MagikVisitorContext;
import org.stevenlooman.sw.magik.parser.MagikParser;

public class InstructionsHandlerTest {

  protected static MagikVisitorContext createContext(String code)
      throws IllegalArgumentException {
    MagikParser parser = new MagikParser(Charset.forName("UTF-8"));
    AstNode root = parser.parseSafe(code);
    return new MagikVisitorContext(code, root);
  }

  @Test
  public void testReadGlobalScopeInstruction() {
    String code =
        "# mlint: a=test1\n" +
        "_method a.b\n" +
        "\twrite(1)\n" +
        "_endmethod";
    MagikVisitorContext context = createContext(code);
    InstructionsHandler instructionsHandler = new InstructionsHandler(context);

    Map<String, String> instructionsGlobal = instructionsHandler.getInstructionsInScope(1, 0);
    Map<String, String> expectedGlobal = new HashMap<>();
    expectedGlobal.put("a", "test1");
    assertThat(instructionsGlobal).isEqualTo(expectedGlobal);

    Map<String, String> instructionsMethod = instructionsHandler.getInstructionsInScope(3, 0);
    Map<String, String> expectedMethod = new HashMap<>();
    expectedMethod.put("a", "test1");
    assertThat(instructionsMethod).isEqualTo(expectedMethod);
  }

  @Test
  public void testReadMethodScopeInstruction() {
    String code =
        "_method a.b\n" +
        "\t# mlint: b=test2\n" +
        "\twrite(1)\n" +
        "_endmethod";
    MagikVisitorContext context = createContext(code);
    InstructionsHandler instructionsHandler = new InstructionsHandler(context);

    Map<String, String> instructionsGlobal = instructionsHandler.getInstructionsInScope(1, 0);
    Map<String, String> expectedGlobal = new HashMap<>();
    assertThat(instructionsGlobal).isEqualTo(expectedGlobal);

    Map<String, String> instructionsMethod = instructionsHandler.getInstructionsInScope(3, 0);
    Map<String, String> expectedMethod = new HashMap<>();
    expectedMethod.put("b", "test2");
    assertThat(instructionsMethod).isEqualTo(expectedMethod);
  }

  @Test
  public void testReadCombinedScopeInstruction() {
    String code =
        "# mlint: a=test1\n" +
        "_method a.b\n" +
        "\t# mlint: b=test2\n" +
        "\twrite(1)\n" +
        "_endmethod";
    MagikVisitorContext context = createContext(code);
    InstructionsHandler instructionsHandler = new InstructionsHandler(context);

    Map<String, String> instructionsGlobal = instructionsHandler.getInstructionsInScope(1, 0);
    Map<String, String> expectedGlobal = new HashMap<>();
    expectedGlobal.put("a", "test1");
    assertThat(instructionsGlobal).isEqualTo(expectedGlobal);

    Map<String, String> instructionsMethod = instructionsHandler.getInstructionsInScope(4, 0);
    Map<String, String> expectedMethod = new HashMap<>();
    expectedMethod.put("a", "test1");
    expectedMethod.put("b", "test2");
    assertThat(instructionsMethod).isEqualTo(expectedMethod);
  }

  @Test
  public void testReadLineInstruction() {
    String code =
        "_method a.b\n" +
        "\twrite(1) # mlint: c=test3\n" +
        "_endmethod";
    MagikVisitorContext context = createContext(code);
    InstructionsHandler instructionsHandler = new InstructionsHandler(context);

    Map<String, String> instructionsGlobal = instructionsHandler.getInstructionsInScope(1, 0);
    Map<String, String> expectedGlobal = new HashMap<>();
    assertThat(instructionsGlobal).isEqualTo(expectedGlobal);

    Map<String, String> instructionsMethod = instructionsHandler.getInstructionsInScope(2, 0);
    Map<String, String> expectedMethod = new HashMap<>();
    assertThat(instructionsMethod).isEqualTo(expectedMethod);

    Map<String, String> instructions = instructionsHandler.getInstructionsAtLine(2);
    Map<String, String> expected = new HashMap<>();
    expected.put("c", "test3");
    assertThat(instructions).isEqualTo(expected);
  }

}