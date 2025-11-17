package nl.ramsolutions.sw.checks.loadlist;

import static nl.ramsolutions.sw.checks.loadlist.LoadListCheckAssert.assertThat;

import nl.ramsolutions.sw.checks.LoadListCheck;
import org.junit.jupiter.api.Test;

/** Test {@link LoadListSyntaxErrorCheck}. */
class LoadListSyntaxErrorCheckTest {

  @Test
  void testNoSyntaxErrors() {
    final LoadListCheck check = new LoadListSyntaxErrorCheck();
    final String code =
        """
        # Valid load list
        file1.magik
        file2.magik # inline comment
        subdir/
        """;
    assertThat(check).reportsNoIssues(code);
  }
}
