package nl.ramsolutions.sw.magik.languageserver.rename;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.TextEdit;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodUsage;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.MethodUsageLocator;
import nl.ramsolutions.sw.magik.analysis.typing.SelfHelper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasonerState;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.languageserver.Lsp4jConversion;
import org.eclipse.lsp4j.PrepareRenameResult;

/** Magik method renamer. */
class MethodRenamer extends Renamer {

  MethodRenamer(final MagikTypedFile magikFile, final AstNode node) {
    super(magikFile, node);
  }

  @Override
  PrepareRenameResult prepareRename() {
    final AstNode node = this.getNode();
    final String methodName = MethodRenamer.getMethodIdentifierFromNode(node);
    if (methodName == null) {
      return null;
    }

    final Range range = new Range(node);
    final org.eclipse.lsp4j.Range rangeLsp4j = Lsp4jConversion.rangeToLsp4j(range);
    return new PrepareRenameResult(rangeLsp4j, methodName);
  }

  @Override
  Map<URI, List<TextEdit>> provideRename(final String newName) {
    final AstNode node = this.getNode();
    final String methodName = MethodRenamer.getMethodNameFromNode(node);
    if (methodName == null) {
      return Collections.emptyMap();
    }

    final TypeString typeString = this.getTypeStringForReceiverNode();
    if (typeString.isUndefined()) {
      return Collections.emptyMap();
    }

    // Get method definition(s) to rename.
    final IDefinitionKeeper definitionKeeper = this.getMagikFile().getDefinitionKeeper();
    final Map<URI, List<TextEdit>> definitionRenames =
        definitionKeeper.getMethodDefinitions(typeString).stream()
            .filter(methodDefinition -> methodDefinition.getMethodName().equals(methodName))
            .map(
                methodDefinition -> {
                  final Location location = methodDefinition.getLocation();
                  Objects.requireNonNull(location);
                  final Range range = location.getRange();
                  Objects.requireNonNull(range);

                  final URI uri = location.getUri();
                  final TextEdit edit = new TextEdit(range, newName);
                  final List<TextEdit> edits = List.of(edit);
                  return Map.entry(uri, edits);
                })
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (a, b) -> Stream.concat(a.stream(), b.stream()).toList()));

    // Get method usages to rename.
    final MethodUsageLocator methodUsageLocator = new MethodUsageLocator(definitionKeeper);
    final MethodUsage searchedMethodUsage = new MethodUsage(typeString, methodName);
    final Map<URI, List<TextEdit>> usageRenames =
        methodUsageLocator.getMethodUsages(searchedMethodUsage).stream()
            .map(
                entry -> {
                  final MethodUsage usage = entry.getKey();

                  final Location location = usage.getLocation();
                  Objects.requireNonNull(location);
                  final Range range = location.getRange();
                  Objects.requireNonNull(range);

                  // The MethodUsage stores the METHOD_INVOCATION node (on the '.') of the call,
                  // instead of the name itself. So get the next node.
                  final AstNode methodInvocationNode = usage.getNode();
                  Objects.requireNonNull(methodInvocationNode);
                  final AstNode methodNameNode =
                      methodInvocationNode.getFirstChild(MagikGrammar.METHOD_NAME);
                  final Range methodNameRange = new Range(methodNameNode);

                  final URI uri = location.getUri();
                  final TextEdit edit = new TextEdit(methodNameRange, newName);
                  final List<TextEdit> edits = List.of(edit);
                  return Map.entry(uri, edits);
                })
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (a, b) -> Stream.concat(a.stream(), b.stream()).toList()));

    return Stream.concat(definitionRenames.entrySet().stream(), usageRenames.entrySet().stream())
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (a, b) -> Stream.concat(a.stream(), b.stream()).toList()));
  }

  private TypeString getTypeStringForReceiverNode() {
    final LocalTypeReasonerState typeReasonerState = this.getMagikFile().getTypeReasonerState();
    final AstNode node = this.getNode();
    final AstNode identifierNode = node.getParent();
    final AstNode methodNameNode = identifierNode.getParent();
    final AstNode methodNameParentNode = methodNameNode.getParent();
    if (methodNameParentNode.is(MagikGrammar.METHOD_DEFINITION)) {
      final MethodDefinitionNodeHelper helper =
          new MethodDefinitionNodeHelper(methodNameParentNode);
      return helper.getTypeString();
    } else if (methodNameParentNode.is(MagikGrammar.METHOD_INVOCATION)) {
      final MethodInvocationNodeHelper helper =
          new MethodInvocationNodeHelper(methodNameParentNode);
      final AstNode receiverNode = helper.getReceiverNode();
      final ExpressionResultString nodeResult = typeReasonerState.getNodeType(receiverNode);
      final TypeString typeStr = nodeResult.get(0, TypeString.UNDEFINED);
      return SelfHelper.substituteSelf(typeStr, receiverNode);
    }

    return TypeString.UNDEFINED;
  }

  public static boolean canHandleRename(final MagikTypedFile magikFile, final AstNode node) {
    return MethodRenamer.getMethodIdentifierFromNode(node) != null;
  }

  @CheckForNull
  public static AstNode getWantedNode(final AstNode node) {
    final AstNode identifierNode = node.getParent();
    if (identifierNode.isNot(MagikGrammar.IDENTIFIER)) {
      return null;
    }

    final AstNode methodNameNode = identifierNode.getParent();
    final AstNode methodNameParentNode = methodNameNode.getParent();
    return methodNameParentNode;
  }

  @CheckForNull
  public static String getMethodIdentifierFromNode(final AstNode node) {
    final AstNode methodNameParentNode = MethodRenamer.getWantedNode(node);
    if (methodNameParentNode == null) {
      return null;
    } else if (methodNameParentNode.is(MagikGrammar.METHOD_DEFINITION)) {
      final MethodDefinitionNodeHelper helper =
          new MethodDefinitionNodeHelper(methodNameParentNode);
      return helper.getMethodNameIdentifier();
    } else if (methodNameParentNode.is(MagikGrammar.METHOD_INVOCATION)) {
      final MethodInvocationNodeHelper helper =
          new MethodInvocationNodeHelper(methodNameParentNode);
      return helper.getMethodNameIdentifier();
    }

    return null;
  }

  @CheckForNull
  public static String getMethodNameFromNode(final AstNode node) {
    final AstNode methodNameParentNode = MethodRenamer.getWantedNode(node);
    if (methodNameParentNode == null) {
      return null;
    } else if (methodNameParentNode.is(MagikGrammar.METHOD_DEFINITION)) {
      final MethodDefinitionNodeHelper helper =
          new MethodDefinitionNodeHelper(methodNameParentNode);
      return helper.getMethodName();
    } else if (methodNameParentNode.is(MagikGrammar.METHOD_INVOCATION)) {
      final MethodInvocationNodeHelper helper =
          new MethodInvocationNodeHelper(methodNameParentNode);
      return helper.getMethodName();
    }

    return null;
  }
}
