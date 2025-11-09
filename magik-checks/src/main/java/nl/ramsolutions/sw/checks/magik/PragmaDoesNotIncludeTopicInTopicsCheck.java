package nl.ramsolutions.sw.checks.magik;

import com.sonar.sslr.api.AstNode;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.checks.DisabledByDefault;
import nl.ramsolutions.sw.checks.MagikCheck;
import nl.ramsolutions.sw.magik.analysis.helpers.PragmaNodeHelper;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;

/** Check if the pragma topics includes the list of topics. */
@DisabledByDefault
@Rule(key = PragmaDoesNotIncludeTopicInTopicsCheck.CHECK_KEY)
public class PragmaDoesNotIncludeTopicInTopicsCheck extends MagikCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "PragmaDoesNotIncludeTopicInTopics";

  private static final String TOPICS_MESSAGE = "'%s' is missing in pragma topics.";
  private static final String DEFAULT_TOPICS = "";

  /** List of topics, separated by ','. */
  @RuleProperty(
      key = "topics",
      defaultValue = "" + DEFAULT_TOPICS,
      description = "List of topics, separated by ','",
      type = "STRING")
  @SuppressWarnings("checkstyle:VisibilityModifier")
  public String topics = DEFAULT_TOPICS;

  @Override
  protected void walkPostPragma(final AstNode node) {
    this.checkTopics(node);
  }

  private void checkTopics(final AstNode node) {
    final Set<String> TopicsItems = this.getTopicsItems();
    if (TopicsItems.isEmpty()) {
      return;
    }

    final PragmaNodeHelper helper = new PragmaNodeHelper(node);
    final Set<String> topics = helper.getTopics();
    for (final String Topic : TopicsItems) {
      if (!topics.contains(Topic)) {
        final String message = TOPICS_MESSAGE.formatted(Topic);
        this.addIssue(node, message);
      }
    }
  }

  private Set<String> getTopicsItems() {
    return Arrays.stream(this.topics.split(","))
        .map(String::trim)
        .filter(topic -> !topic.isEmpty())
        .collect(Collectors.toSet());
  }
}
