package nl.ramsolutions.sw.magik.ramsolutions;

import com.sonar.sslr.impl.Parser;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.api.MagikKeyword;
import org.sonar.colorizer.KeywordsTokenizer;
import org.sonar.colorizer.StringTokenizer;
import org.sonar.colorizer.Tokenizer;
import org.sonar.sslr.parser.LexerlessGrammar;
import org.sonar.sslr.parser.ParserAdapter;
import org.sonar.sslr.toolkit.ConfigurationModel;
import org.sonar.sslr.toolkit.ConfigurationProperty;

/** Magik configuration model. */
final class MagikConfigurationModel implements ConfigurationModel {

  /** Get properties. */
  public List<ConfigurationProperty> getProperties() {
    return Collections.emptyList();
  }

  /** Set update flag. */
  public void setUpdatedFlag() {
    // do nothing
  }

  /** Get default charset. */
  public Charset getCharset() {
    return StandardCharsets.ISO_8859_1;
  }

  /** Get parser. */
  public Parser<LexerlessGrammar> getParser() {
    Charset charset = getCharset();
    return new ParserAdapter<>(charset, MagikGrammar.create());
  }

  /** Get {@link Tokenizer}s. */
  public List<Tokenizer> getTokenizers() {
    return List.of(
        new StringTokenizer("<span class=\"s\">", "</span>"), // NOSONAR
        new MagikDocTokenizer("<span class=\"cppd\">", "</span>"), // NOSONAR
        new MagikCommentTokenizer("<span class=\"cd\">", "</span>"), // NOSONAR
        new KeywordsTokenizer(
            "<span class=\"k\">", "</span>", MagikKeyword.keywordValues())); // NOSONAR
  }
}
