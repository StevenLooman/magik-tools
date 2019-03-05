package org.stevenlooman.sw.magik.toolkit;

import com.google.common.collect.ImmutableList;

import com.sonar.sslr.impl.Parser;

import org.apache.commons.io.Charsets;
import org.sonar.colorizer.KeywordsTokenizer;
import org.sonar.colorizer.StringTokenizer;
import org.sonar.colorizer.Tokenizer;
import org.sonar.sslr.parser.LexerlessGrammar;
import org.sonar.sslr.parser.ParserAdapter;
import org.sonar.sslr.toolkit.ConfigurationModel;
import org.sonar.sslr.toolkit.ConfigurationProperty;
import org.stevenlooman.sw.magik.api.MagikGrammar;
import org.stevenlooman.sw.magik.api.MagikKeyword;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;

final class MagikConfigurationModel implements ConfigurationModel {
  public List<ConfigurationProperty> getProperties() {
    return Collections.emptyList();
  }

  public void setUpdatedFlag() {
    // do nothing
  }

  public Charset getCharset() {
    return Charsets.UTF_8;
  }

  public Parser getParser() {
    Charset charset = getCharset();
    Parser<LexerlessGrammar> magikParser = new ParserAdapter<>(charset, MagikGrammar.create());
    return magikParser;
  }

  @SuppressWarnings("squid:S1192")
  public List<Tokenizer> getTokenizers() {
    return ImmutableList.of(
        new StringTokenizer("<span class=\"s\">", "</span>"),
        new MagikDocTokenizer("<span class=\"cppd\">", "</span>"),
        new MagikCommentTokenizer("<span class=\"cd\">", "</span>"),
        new KeywordsTokenizer("<span class=\"k\">", "</span>", MagikKeyword.keywordValues()));
  }
}

