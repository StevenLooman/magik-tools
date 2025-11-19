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
        .matches("file")
        .matches("file1")
        .matches("subdir/file")
        .matches("path/to/file")
        .matches("file with spaces")
        .matches("subdir/")
        .notMatches("#comment")
        .notMatches("");
  }

  @Test
  void testFileEntry() {
    assertThat(grammar.rule(LoadListGrammar.FILE_ENTRY))
        .matches("file")
        .matches("subdir/")
        .matches("subdir/file");
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
  void testLoadList() {
    assertThat(grammar.rule(LoadListGrammar.LOAD_LIST))
        .matches("")
        .matches("file1")
        .matches("file1\nfile2")
        .matches("# comment\nfile1")
        .matches("file1 # inline comment")
        .matches(
            """
            # Load list
            file1
            file2
            """)
        .matches(
            """
            file1

            file2
            """)
        .matches(
            """
            # Comment
            file1 # inline comment

            subdir/
            """)
        .matches(
            """
            file with spaces
            path/to/file
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
            core/file1
            core/file2 # important

            # Subdirectories
            modules/

            # Additional files
            util
            """);
  }
}
