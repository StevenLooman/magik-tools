package nl.ramsolutions.sw.magik.analysis.definitions;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Pragma. */
public class Pragma {

  public static final String CLASSIFY_LEVEL = "classify_level";
  public static final String CLASSIFY_LEVEL_BASIC = "basic";
  public static final String CLASSIFY_LEVEL_ADVANCED = "advanced";
  public static final String CLASSIFY_LEVEL_RESTRICTED = "restricted";
  public static final String CLASSIFY_LEVEL_DEBUG = "debug";
  public static final String CLASSIFY_LEVEL_DEPRECATED = "deprecated";
  public static final Set<String> CLASSIFY_LEVELS =
      Set.of(
          CLASSIFY_LEVEL_BASIC,
          CLASSIFY_LEVEL_ADVANCED,
          CLASSIFY_LEVEL_RESTRICTED,
          CLASSIFY_LEVEL_DEBUG,
          CLASSIFY_LEVEL_DEPRECATED);

  public static final String USAGE = "usage";
  public static final String USAGE_SUBCLASSABLE = "subclassable";
  public static final String USAGE_SUBCLASS = "subclass";
  public static final String USAGE_REDEFINABLE = "redefinable";
  public static final String USAGE_EXTERNAL = "external";
  public static final String USAGE_INTERNAL = "internal";
  public static final Set<String> USAGES =
      Set.of(USAGE_SUBCLASSABLE, USAGE_SUBCLASS, USAGE_REDEFINABLE, USAGE_EXTERNAL, USAGE_INTERNAL);

  public static final String TOPIC_DEPRECATED = "deprecated";

  private final @Nullable AstNode node;
  private final Set<String> classifyLevels;
  private final Set<String> topics;
  private final Set<String> usages;

  /**
   * Constructor.
   *
   * @param node Node to encapsulate.
   * @param classifyLevel Classify level.
   * @param topics Topics.
   * @param usages Usages.
   */
  public Pragma(
      final @Nullable AstNode node,
      final Collection<String> classifyLevel,
      final Collection<String> topics,
      final Collection<String> usages) {
    this.node = node;
    this.classifyLevels = Set.copyOf(classifyLevel);
    this.topics = Set.copyOf(topics);
    this.usages = Set.copyOf(usages);
  }

  /**
   * Constructor.
   *
   * <p>Use Pragma.CLASSIFY_LEVELS/Pragma.USAGE_TYPES to parse the pragmas. The rest of the pragmas
   * are considered topics.
   *
   * @param node Node to encapsulate.
   * @param items Items.
   */
  public Pragma(final @Nullable AstNode node, final Collection<String> items) {
    this.node = node;
    this.classifyLevels =
        items.stream().filter(Pragma.CLASSIFY_LEVELS::contains).collect(Collectors.toSet());
    this.usages = items.stream().filter(Pragma.USAGES::contains).collect(Collectors.toSet());
    this.topics =
        items.stream()
            .filter(item -> !Pragma.CLASSIFY_LEVELS.contains(item) && !Pragma.USAGES.contains(item))
            .collect(Collectors.toSet());
  }

  /**
   * Get node.
   *
   * @return Node.
   */
  public @Nullable AstNode getNode() {
    return this.node;
  }

  /**
   * Get classify level.
   *
   * @return Classify level.
   */
  public Set<String> getClassifyLevels() {
    return Collections.unmodifiableSet(this.classifyLevels);
  }

  /**
   * Get topics.
   *
   * @return Topics.
   */
  public Set<String> getTopics() {
    return this.topics;
  }

  /**
   * Get usages.
   *
   * @return Usages.
   */
  public Set<String> getUsages() {
    return this.usages;
  }

  /**
   * Get bare pragma.
   *
   * @return Bare pragma.
   */
  public Pragma getBarePragma() {
    return new Pragma(null, this.classifyLevels, this.topics, this.usages);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.classifyLevels, this.topics, this.usages);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj == null) {
      return false;
    }

    if (this.getClass() != obj.getClass()) {
      return false;
    }

    final Pragma other = (Pragma) obj;
    return Objects.equals(this.classifyLevels, other.classifyLevels)
        && Objects.equals(this.topics, other.topics)
        && Objects.equals(this.usages, other.usages);
  }
}
