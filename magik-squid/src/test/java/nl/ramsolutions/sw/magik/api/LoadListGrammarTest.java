package nl.ramsolutions.sw.magik.api;

import static org.sonar.sslr.tests.Assertions.assertThat;

import com.sonar.sslr.api.Grammar;
import nl.ramsolutions.sw.loadlist.api.LoadListGrammar;
import org.junit.jupiter.api.Test;

/** Tests for LoadListGrammar. */
class LoadListGrammarTest {
  private final Grammar grammar = LoadListGrammar.create();

  @Test
  void testFilePath() {
    assertThat(grammar.rule(LoadListGrammar.FILE_PATH))
        .matches("file.magik")
        .matches("file1.magik")
        .matches("subdir/file.magik")
        .matches("path/to/file.magik")
        .matches("file with spaces.magik")
        .matches("subdir/")
        .notMatches("#comment")
        .notMatches("");
  }

  @Test
  void testFileEntry() {
    assertThat(grammar.rule(LoadListGrammar.FILE_ENTRY))
        .matches("file.magik")
        .matches("file.magik\n")
        .matches("file.magik # comment")
        .matches("file.magik # comment\n")
        .matches("subdir/")
        .matches("subdir/\n")
        .matches("subdir/ # comment")
        .matches("subdir/ # comment\n");
  }

  @Test
  void testComment() {
    assertThat(grammar.rule(LoadListGrammar.COMMENT))
        .matches("# comment")
        .matches("# this is a comment")
        .matches("#")
        .notMatches("not a comment");
  }

  @Test
  void testBlankLine() {
    assertThat(grammar.rule(LoadListGrammar.BLANK_LINE)).matches("\n").matches("\r\n");
  }

  @Test
  void testLoadList() {
    assertThat(grammar.rule(LoadListGrammar.LOAD_LIST))
        .matches("")
        .matches("file1.magik")
        .matches("file1.magik\nfile2.magik")
        .matches("# comment\nfile1.magik")
        .matches("file1.magik # inline comment")
        .matches(
            """
            # Load list
            file1.magik
            file2.magik
            """)
        .matches(
            """
            file1.magik

            file2.magik
            """)
        .matches(
            """
            # Comment
            file1.magik # inline comment

            subdir/
            """)
        .matches(
            """
            file with spaces.magik
            path/to/file.magik
            subdir/
            """);
  }

  @Test
  void testLoadListWithDirectories() {
    assertThat(grammar.rule(LoadListGrammar.LOAD_LIST))
        .matches("subdir/")
        .matches("subdir/\nanother_dir/")
        .matches(
            """
            # Directories
            subdir/
            another/
            """);
  }

  @Test
  void testLoadListMixedContent() {
    assertThat(grammar.rule(LoadListGrammar.LOAD_LIST))
        .matches(
            """
            # Core files
            core/file1.magik
            core/file2.magik # important

            # Subdirectories
            modules/

            # Additional files
            util.magik
            """);
  }

  @Test
  void testSyntaxError() {
    // The grammar should be permissive and match syntax errors as SYNTAX_ERROR
    assertThat(grammar.rule(LoadListGrammar.LOAD_LIST))
        .matches("file1.magik")
        .matches("some random text that doesn't match");
  }
}
