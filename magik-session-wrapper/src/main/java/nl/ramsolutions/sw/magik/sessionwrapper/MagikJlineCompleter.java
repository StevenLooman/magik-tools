package nl.ramsolutions.sw.magik.sessionwrapper;

import java.util.List;
import java.util.stream.Stream;
import nl.ramsolutions.sw.magik.api.MagikKeyword;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

/** JLine completer for Magik. */
class MagikJlineCompleter implements Completer {

  @Override
  public void complete(
      final LineReader reader, final ParsedLine line, final List<Candidate> candidates) {
    final String word = line.word();

    this.completeKeywords(candidates, word);
  }

  private void completeKeywords(final List<Candidate> candidates, final String word) {
    Stream.of(MagikKeyword.keywordValues())
        .filter(
            keyword -> {
              final int index = keyword.indexOf(word);
              return index == 0 || index == 1;
            })
        .map(Candidate::new)
        .forEach(candidates::add);
  }
}
