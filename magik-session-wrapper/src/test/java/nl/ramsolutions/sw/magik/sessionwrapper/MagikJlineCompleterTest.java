package nl.ramsolutions.sw.magik.sessionwrapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.jline.reader.Candidate;
import org.jline.reader.ParsedLine;
import org.junit.jupiter.api.Test;

/** Tests for {@link MagikJlineCompleter}. */
class MagikJlineCompleterTest {

  @Test
  void testCompleteKeywordWithUnderscore() {
    final String code = "_hand";
    final MagikJlineParser parser = new MagikJlineParser();
    final ParsedLine parsedLine = parser.parse(code, 5);

    final MagikJlineCompleter completer = new MagikJlineCompleter();
    final List<Candidate> candidates = new ArrayList<>();
    completer.complete(null, parsedLine, candidates);

    assertThat(candidates).containsOnly(new Candidate("_handling"));
  }

  @Test
  void testCompleteKeywordWithoutUnderscore() {
    final String code = "hand";
    final MagikJlineParser parser = new MagikJlineParser();
    final ParsedLine parsedLine = parser.parse(code, 4);

    final MagikJlineCompleter completer = new MagikJlineCompleter();
    final List<Candidate> candidates = new ArrayList<>();
    completer.complete(null, parsedLine, candidates);

    assertThat(candidates).containsOnly(new Candidate("_handling"));
  }
}
