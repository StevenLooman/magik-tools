package org.stevenlooman.sw.magik.analysis;

import java.nio.charset.Charset;
import java.util.EnumSet;

import com.sonar.sslr.api.AstNode;

import org.junit.Assert;
import org.junit.Test;
import org.stevenlooman.sw.magik.parser.MagikParser;

public class AstCompareTest {

  private AstNode parseMagik(String code) {
    MagikParser parser = new MagikParser(Charset.forName("UTF-8"));
    return parser.parseSafe(code);
  }

  @Test
  public void testCompareEqualsRecursiveTrue() {
    String code1 = "_true";
    String code2 = "_true";
    AstNode left = parseMagik(code1);
    AstNode right = parseMagik(code2);

    boolean equals = AstCompare.astNodeEqualsRecursive(left, right);
    Assert.assertTrue(equals);
  }

  @Test
  public void testCompareEqualsRecursiveTrueFalse() {
    String code1 = "_true";
    String code2 = "_false";
    AstNode left = parseMagik(code1);
    AstNode right = parseMagik(code2);

    boolean equals = AstCompare.astNodeEqualsRecursive(left, right);
    Assert.assertFalse(equals);
  }

  @Test
  public void testCompareEqualsRecursiveAssignment() {
    String code1 = "i << 10";
    String code2 = "i << 10";
    AstNode left = parseMagik(code1);
    AstNode right = parseMagik(code2);

    boolean equals = AstCompare.astNodeEqualsRecursive(left, right);
    Assert.assertTrue(equals);
  }

  @Test
  public void testCompareEqualsRecursiveAssignmentIgnoreIdentifier() {
    String code1 = "i << 10";
    String code2 = "j << 10";
    AstNode left = parseMagik(code1);
    AstNode right = parseMagik(code2);

    EnumSet<AstCompare.Flags> flags = EnumSet.of(AstCompare.Flags.IGNORE_IDENTIFIER_NAME);
    boolean equals = AstCompare.astNodeEqualsRecursive(left, right, flags);
    Assert.assertTrue(equals);
  }

  @Test
  public void testCompareEqualsRecursiveParenthesis() {
    String code1 = "(_true)";
    String code2 = "_true";
    AstNode left = parseMagik(code1);
    AstNode right = parseMagik(code2);

    boolean equals = AstCompare.astNodeEqualsRecursive(left, right);
    Assert.assertFalse(equals);
  }

}
