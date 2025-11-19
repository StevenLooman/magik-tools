package nl.ramsolutions.sw.sslr.loadlist;

import com.sonar.sslr.impl.Parser;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import nl.ramsolutions.sw.loadlist.api.LoadListGrammar;
import nl.ramsolutions.sw.sslr.CommentTokenizer;
import org.sonar.colorizer.Tokenizer;
import org.sonar.sslr.parser.LexerlessGrammar;
import org.sonar.sslr.parser.ParserAdapter;
import org.sonar.sslr.toolkit.ConfigurationModel;
import org.sonar.sslr.toolkit.ConfigurationProperty;

/** Load list configuration model. */
public class SwLoadListConfigurationModel implements ConfigurationModel {

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
    final Charset charset = this.getCharset();
    return new ParserAdapter<>(charset, LoadListGrammar.create());
  }

  /** Get {@link Tokenizer}s. */
  public List<Tokenizer> getTokenizers() {
    return List.of(new CommentTokenizer("<span class=\"cd\">", "</span>")); // NOSONAR
  }
}
