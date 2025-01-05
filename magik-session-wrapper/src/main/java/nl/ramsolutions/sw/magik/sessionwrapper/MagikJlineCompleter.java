package nl.ramsolutions.sw.magik.sessionwrapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import nl.ramsolutions.sw.magik.api.MagikKeyword;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

/** JLine completer for Magik. */
class MagikJlineCompleter implements Completer {

  // Electric magik patterns for completion.
  private static final Map<String, String> ELECTRIC_MAGIK_COMPLETIONS = new HashMap<>();

  static {
    ELECTRIC_MAGIK_COMPLETIONS.put("iter", "_iter _method\n_endmethod");
    ELECTRIC_MAGIK_COMPLETIONS.put("private", "_private _method\n_endmethod");
    ELECTRIC_MAGIK_COMPLETIONS.put("abstract", "_abstract _method\n_endmethod");
    ELECTRIC_MAGIK_COMPLETIONS.put("method", "_method\n_endmethod");
    ELECTRIC_MAGIK_COMPLETIONS.put(
        "pragma", "_pragma(classify_level=restricted, topic={}, usage={})");
    ELECTRIC_MAGIK_COMPLETIONS.put("def_slotted_exemplar", "def_slotted_exemplar()");
    ELECTRIC_MAGIK_COMPLETIONS.put("def_mixin", "def_mixin()");
    ELECTRIC_MAGIK_COMPLETIONS.put("remex", "remex()");
    ELECTRIC_MAGIK_COMPLETIONS.put("message_handler", "message_handler.new()");
    ELECTRIC_MAGIK_COMPLETIONS.put("define_condition", "condition.define_condition()");
    ELECTRIC_MAGIK_COMPLETIONS.put("define_binary_operator_case", "define_binary_operator_case()");

    ELECTRIC_MAGIK_COMPLETIONS.put("if", "_if \n_then\n\n_endif");
    ELECTRIC_MAGIK_COMPLETIONS.put("over", "_over \n_loop\n_endloop");
    ELECTRIC_MAGIK_COMPLETIONS.put("catch", "_catch\n_endcatch");
    ELECTRIC_MAGIK_COMPLETIONS.put("block", "_block\n_endblock");
    ELECTRIC_MAGIK_COMPLETIONS.put("protect", "_protect\n_protection\n_endprotect");
    ELECTRIC_MAGIK_COMPLETIONS.put("lock", "_lock\n_unlock");
    ELECTRIC_MAGIK_COMPLETIONS.put("try", "_try\n_when\n_endtry");
    ELECTRIC_MAGIK_COMPLETIONS.put("proc", "_proc()\n_endproc");
    ELECTRIC_MAGIK_COMPLETIONS.put("loop", "_loop\n_endloop");
    ELECTRIC_MAGIK_COMPLETIONS.put("while", "_while \n_loop\n_endloop");
    ELECTRIC_MAGIK_COMPLETIONS.put("for", "_for  _over \n_loop\n_endloop");
  }

  @Override
  public void complete(
      final LineReader reader, final ParsedLine line, final List<Candidate> candidates) {
    final String word = line.word();
    this.completeElectricMagik(candidates, word);
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

  private void completeElectricMagik(List<Candidate> candidates, String word) {
    MagikJlineCompleter.ELECTRIC_MAGIK_COMPLETIONS.entrySet().stream()
        .filter(
            entry -> {
              final String keyword = entry.getKey();
              final int index = keyword.indexOf(word);
              return index == 0 || index == 1;
            })
        .map(Map.Entry::getValue)
        .map(Candidate::new)
        .forEach(candidates::add);
  }
}
