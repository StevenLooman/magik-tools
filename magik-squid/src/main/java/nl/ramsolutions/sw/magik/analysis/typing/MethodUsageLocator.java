package nl.ramsolutions.sw.magik.analysis.typing;

import com.sonar.sslr.api.AstNode;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import nl.ramsolutions.sw.FileCharsetDeterminer;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodUsage;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasonerState;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/**
 * Method usage locator.
 *
 * <p>Uses typing to determine if any {@link MethodUsage} is is for this specific type.
 */
public class MethodUsageLocator {

  private final IDefinitionKeeper definitionKeeper;

  public MethodUsageLocator(final IDefinitionKeeper definitionKeeper) {
    this.definitionKeeper = definitionKeeper;
  }

  public List<Map.Entry<MethodUsage, MagikTypedFile>> getMethodUsages(
      final MethodUsage wantedMethodUsage) {
    final String methodName = wantedMethodUsage.getMethodName();

    return this.definitionKeeper.getMethodDefinitions().stream()
        .flatMap(methodDef -> methodDef.getUsedMethods().stream())
        .filter(usage -> usage.getMethodName().equals(methodName))
        .map(
            usage -> {
              final Location location = usage.getLocation();
              final MagikTypedFile magikFile = this.getMagikFile(location);

              // Determine/reason the type the method is called on.
              final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();
              final AstNode node = magikFile.getTopNode();
              final Position calledMethodPosition = location.getRange().getStartPosition();
              final AstNode calledNode = AstQuery.nodeAt(node, calledMethodPosition);
              final AstNode parentCalledNode = calledNode.getFirstAncestor(MagikGrammar.ATOM);
              final ExpressionResultString result = reasonerState.getNodeType(parentCalledNode);
              final TypeString resultTypeStr = result.get(0, TypeString.UNDEFINED);
              final TypeString typeStr = SelfHelper.substituteSelf(resultTypeStr, parentCalledNode);
              if (typeStr.isUndefined()) {
                return null;
              }

              final TypeStringResolver resolver = magikFile.getTypeStringResolver();
              final TypeString wantedMethodUsageTypeStr = wantedMethodUsage.getTypeName();
              if (!resolver.isKindOf(wantedMethodUsageTypeStr, typeStr)) {
                return null;
              }

              final MethodUsage methodUsageWithNode =
                  new MethodUsage(wantedMethodUsageTypeStr, methodName, location, calledNode);
              return Map.entry(methodUsageWithNode, magikFile);
            })
        .filter(Objects::nonNull)
        .toList();
  }

  private MagikTypedFile getMagikFile(final Location location) {
    final URI calledMethodUri = location.getUri();
    final Path calledMethodPath = Path.of(calledMethodUri);
    final Charset charset = FileCharsetDeterminer.determineCharset(calledMethodPath);
    final String text;
    try {
      text = Files.readString(calledMethodPath, charset);
    } catch (final IOException exception) {
      throw new IllegalStateException(exception);
    }
    return new MagikTypedFile(calledMethodUri, text, this.definitionKeeper);
  }
}
