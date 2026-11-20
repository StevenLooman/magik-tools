package nl.ramsolutions.sw.magik.languageserver.rename;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodUsage;
import nl.ramsolutions.sw.magik.analysis.definitions.parsers.MethodInvocationStringParser;
import nl.ramsolutions.sw.magik.analysis.helpers.PackageNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.api.TypeDocGrammar;
import nl.ramsolutions.sw.magik.parser.TypeDocParser;

/**
 * Locates the method names in the {@code @invokes_method} annotations of a definition.
 *
 * <p>A usage such an annotation declares sits at its definition, so the text of the annotation is
 * found in the doc comment of that definition.
 */
final class InvokesMethodAnnotationLocator {

  private static final String BRACKET_START = "[";
  private static final char ARGUMENTS_START = '(';

  private InvokesMethodAnnotationLocator() {
    // Utility class.
  }

  /**
   * Find the method names in the annotations of a definition which declare the usage.
   *
   * @param definitionNode METHOD_DEFINITION or PROCEDURE_DEFINITION node.
   * @param usage Usage, with the type and the method name an annotation names.
   * @return Ranges of the method names, each without the dot and without parentheses. An indexer
   *     annotation, as {@code {type[]}}, has no name.
   */
  static List<Range> findMethodNameRanges(final AstNode definitionNode, final MethodUsage usage) {
    final PackageNodeHelper packageHelper = new PackageNodeHelper(definitionNode);
    final String currentPackage = packageHelper.getCurrentPackage();
    final TypeDocParser typeDocParser = new TypeDocParser(definitionNode);
    final AstNode typeDocNode = typeDocParser.getTypeDocNode();
    return typeDocNode.getChildren(TypeDocGrammar.INVOKES_METHOD).stream()
        .map(annotationNode -> annotationNode.getFirstChild(TypeDocGrammar.METHOD_INVOCATION))
        .filter(Objects::nonNull)
        .map(invocationNode -> invocationNode.getFirstChild(TypeDocGrammar.METHOD_INVOCATION_TYPE))
        .filter(
            invocationTypeNode ->
                InvokesMethodAnnotationLocator.declaresUsage(
                    invocationTypeNode, currentPackage, usage))
        .map(InvokesMethodAnnotationLocator::findMethodNameRange)
        .filter(Objects::nonNull)
        .toList();
  }

  private static boolean declaresUsage(
      final AstNode invocationTypeNode, final String currentPackage, final MethodUsage usage) {
    final List<Token> tokens = invocationTypeNode.getTokens();
    final String invocationString =
        tokens.stream().map(Token::getValue).collect(Collectors.joining());
    final MethodUsage declaredUsage =
        MethodInvocationStringParser.parseInvocationString(invocationString, null, currentPackage);
    if (declaredUsage == null) {
      return false;
    }

    // The annotation is parsed without a location, so compare what it names.
    final TypeString typeName = usage.getTypeName();
    final TypeString declaredTypeName = declaredUsage.getTypeName();
    final String methodName = usage.getMethodName();
    final String declaredMethodName = declaredUsage.getMethodName();
    return typeName.equals(declaredTypeName) && methodName.equals(declaredMethodName);
  }

  @CheckForNull
  private static Range findMethodNameRange(final AstNode invocationTypeNode) {
    final AstNode methodNameNode =
        invocationTypeNode.getFirstChild(TypeDocGrammar.METHOD_INVOCATION_METHOD_NAME);
    final Token nameToken = methodNameNode.getLastToken();
    final String name = nameToken.getValue();
    if (name.startsWith(BRACKET_START)) {
      return null;
    }

    final int argumentsIndex = name.indexOf(ARGUMENTS_START);
    final int identifierLength = argumentsIndex == -1 ? name.length() : argumentsIndex;
    final int line = nameToken.getLine();
    final int column = nameToken.getColumn();
    final Position startPosition = new Position(line, column);
    final Position endPosition = new Position(line, column + identifierLength);
    return new Range(startPosition, endPosition);
  }
}
