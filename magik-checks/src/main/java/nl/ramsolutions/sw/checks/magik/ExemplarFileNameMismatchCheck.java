package nl.ramsolutions.sw.checks.magik;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.net.URI;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.checks.MagikCheck;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MagikDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;

/** Check if exemplar-related definitions are in a matching file. */
@Rule(key = ExemplarFileNameMismatchCheck.CHECK_KEY)
public class ExemplarFileNameMismatchCheck extends MagikCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "ExemplarFileNameMismatch";

  private static final String MESSAGE = "Exemplar '%s' is not defined in a matching file.";

  private static final String DEFAULT_EXCEPTIONS = "record_exemplars.magik,glue.magik";

  /** List of ignored file names, separated by ','. */
  @RuleProperty(
      key = "exceptions",
      defaultValue = "" + DEFAULT_EXCEPTIONS,
      description = "List of ignored file names, separated by ','",
      type = "STRING")
  @SuppressWarnings("checkstyle:VisibilityModifier")
  public String exceptions = DEFAULT_EXCEPTIONS;

  private Set<String> getExceptions() {
    return Arrays.stream(this.exceptions.split(","))
        .map(String::trim)
        .map(String::toLowerCase)
        .filter(s -> !s.isEmpty())
        .collect(Collectors.toSet());
  }

  @Override
  protected void walkPostMagik(final AstNode node) {
    final String fileName = this.getFileName();
    if (fileName == null) {
      return;
    }

    final String loweredFileName = fileName.toLowerCase();
    if (this.getExceptions().contains(loweredFileName)) {
      return;
    }

    this.getMagikFile().getMagikDefinitions().stream()
        .filter(ExemplarFileNameMismatchCheck::isCheckedDefinition)
        .forEach(definition -> this.checkDefinition(definition, loweredFileName));
  }

  private static boolean isCheckedDefinition(final MagikDefinition definition) {
    return definition instanceof ExemplarDefinition
        || definition instanceof MethodDefinition methodDefinition
            && methodDefinition.isActualMethodDefinition();
  }

  private @Nullable String getFileName() {
    final URI uri = this.getMagikFile().getUri();
    if (uri == null) {
      return null;
    }

    final Path path = Path.of(uri);
    final Path fileNamePath = path.getFileName();
    return fileNamePath != null ? fileNamePath.toString() : null;
  }

  private void checkDefinition(final MagikDefinition definition, final String loweredFileName) {
    final String exemplarName = this.getExemplarName(definition);
    if (loweredFileName.contains(exemplarName.toLowerCase())) {
      return;
    }

    final AstNode issueNode = this.getIssueNode(definition);
    final String message = MESSAGE.formatted(exemplarName);
    this.addIssue(issueNode, message);
  }

  private String getExemplarName(final MagikDefinition definition) {
    final TypeString typeString;
    if (definition instanceof ExemplarDefinition exemplarDefinition) {
      typeString = exemplarDefinition.getTypeString();
    } else if (definition instanceof MethodDefinition methodDefinition) {
      typeString = methodDefinition.getTypeName();
    } else {
      throw new IllegalStateException("Unhandled definition type");
    }

    return typeString.getIdentifier();
  }

  private AstNode getIssueNode(final MagikDefinition definition) {
    final AstNode definitionNode = definition.getNode();
    Objects.requireNonNull(definitionNode);

    if (definition instanceof MethodDefinition) {
      final MethodDefinitionNodeHelper helper = new MethodDefinitionNodeHelper(definitionNode);
      return helper.getMethodNameNode();
    }

    return definitionNode;
  }
}
