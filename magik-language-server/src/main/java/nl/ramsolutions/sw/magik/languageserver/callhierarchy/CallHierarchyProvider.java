package nl.ramsolutions.sw.magik.languageserver.callhierarchy;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sonar.sslr.api.AstNode;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import nl.ramsolutions.sw.FileCharsetDeterminer;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodUsage;
import nl.ramsolutions.sw.magik.analysis.definitions.parsers.AnonymousNamer;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.ProcedureDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.MethodUsageLocator;
import nl.ramsolutions.sw.magik.analysis.typing.SelfHelper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeStringResolver;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasonerState;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.languageserver.Lsp4jConversion;
import nl.ramsolutions.sw.magik.parser.TypeStringParser;
import org.eclipse.lsp4j.CallHierarchyIncomingCall;
import org.eclipse.lsp4j.CallHierarchyItem;
import org.eclipse.lsp4j.CallHierarchyOutgoingCall;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.SymbolKind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Call hierarchy provider. */
public class CallHierarchyProvider {

  private static final String DATA_URI = "uri";
  private static final String DATA_METHOD_NAME = "methodName";
  private static final String DATA_TYPE_STRING = "typeString";

  private static final Logger LOGGER = LoggerFactory.getLogger(CallHierarchyProvider.class);

  private final IDefinitionKeeper definitionKeeper;

  public CallHierarchyProvider(final IDefinitionKeeper definitionKeeper) {
    this.definitionKeeper = definitionKeeper;
  }

  /**
   * Set server capabilities.
   *
   * @param capabilities Server capabilities.
   */
  public void setCapabilities(final ServerCapabilities capabilities) {
    capabilities.setCallHierarchyProvider(true);
  }

  public List<CallHierarchyItem> prepareCallHierarchy(
      final MagikTypedFile magikFile, final Position position) {
    final AstNode node = magikFile.getTopNode();
    final AstNode positionTokenNode = AstQuery.nodeSurrounding(node, position);
    if (positionTokenNode == null) {
      return Collections.emptyList();
    }

    final AstNode wantedNode =
        positionTokenNode.is(MagikGrammar.METHOD_DEFINITION)
            ? positionTokenNode
            : positionTokenNode.getFirstAncestor(MagikGrammar.METHOD_DEFINITION);
    LOGGER.trace("Wanted node: {}", wantedNode);
    if (wantedNode == null) {
      return null; // NOSONAR: LSP requires null.
    }

    final MethodDefinitionNodeHelper helper = new MethodDefinitionNodeHelper(wantedNode);
    final String fullMethodName = helper.getFullExemplarMethodName();
    final URI uri = magikFile.getUri();
    final Location location = new Location(uri);
    final Range range = new Range(wantedNode);
    final CallHierarchyItem item =
        new CallHierarchyItem(
            fullMethodName,
            SymbolKind.Method,
            location.getUri().toString(),
            Lsp4jConversion.rangeToLsp4j(range),
            Lsp4jConversion.rangeToLsp4j(range));
    final Map<String, String> data =
        Map.of(
            DATA_TYPE_STRING, helper.getTypeString().getFullString(),
            DATA_METHOD_NAME, helper.getMethodName(),
            DATA_URI, location.getUri().toString());
    item.setData(data);
    return List.of(item);
  }

  public List<CallHierarchyIncomingCall> callHierarchyIncomingCalls(final CallHierarchyItem item) {
    final JsonElement element = (JsonElement) item.getData();
    final JsonObject object = element.getAsJsonObject();
    final String methodName = object.getAsJsonPrimitive(DATA_METHOD_NAME).getAsString();
    final String typeStringStr = object.getAsJsonPrimitive(DATA_TYPE_STRING).getAsString();
    final TypeString typeString = TypeStringParser.parseTypeString(typeStringStr);

    final MethodUsageLocator methodUsageLocator = new MethodUsageLocator(this.definitionKeeper);
    final MethodUsage searchedMethodUsage = new MethodUsage(typeString, methodName);
    return methodUsageLocator.getMethodUsages(searchedMethodUsage).stream()
        .map(
            entry -> {
              final MethodUsage usage = entry.getKey();

              final Location location = usage.getLocation();
              Objects.requireNonNull(location);
              final AstNode usageNode = usage.getNode();
              Objects.requireNonNull(usageNode);

              // Construct the CallHierarchyItem/CallHierarchyIncomingCall for
              // method/procedure definition.
              final URI calledMethodUri = location.getUri();
              final String uriStr = calledMethodUri.toString();
              final AstNode definitionNode =
                  usageNode.getFirstAncestor(
                      MagikGrammar.METHOD_DEFINITION, MagikGrammar.PROCEDURE_DEFINITION);
              if (definitionNode == null) {
                return null;
              } else if (definitionNode.is(MagikGrammar.METHOD_DEFINITION)) {
                return this.createIncomingCallForMethod(usageNode, uriStr, definitionNode);
              } else if (definitionNode.is(MagikGrammar.PROCEDURE_DEFINITION)) {
                return this.createIncomingCallForProcedure(usageNode, uriStr, definitionNode);
              }

              throw new IllegalStateException("Unknown thing");
            })
        .filter(Objects::nonNull)
        .toList();
  }

  private CallHierarchyIncomingCall createIncomingCallForMethod(
      final AstNode calledNode, final String uriStr, final AstNode definitionNode) {
    final MethodDefinitionNodeHelper helper = new MethodDefinitionNodeHelper(definitionNode);
    final String fromMethodName = helper.getFullExemplarMethodName();
    final Range range = new Range(definitionNode);
    final CallHierarchyItem fromItem =
        new CallHierarchyItem(
            fromMethodName,
            SymbolKind.Method,
            uriStr,
            Lsp4jConversion.rangeToLsp4j(range),
            Lsp4jConversion.rangeToLsp4j(range));
    final Map<String, String> data =
        Map.of(
            DATA_TYPE_STRING, helper.getTypeString().getFullString(),
            DATA_METHOD_NAME, helper.getMethodName(),
            DATA_URI, uriStr);
    fromItem.setData(data);
    final Range fromRange = new Range(calledNode);
    return new CallHierarchyIncomingCall(
        fromItem, List.of(Lsp4jConversion.rangeToLsp4j(fromRange)));
  }

  private CallHierarchyIncomingCall createIncomingCallForProcedure(
      final AstNode calledNode, final String uriStr, final AstNode definitionNode) {
    final ProcedureDefinitionNodeHelper helper = new ProcedureDefinitionNodeHelper(definitionNode);
    final String fromProcName = helper.getProcedureName();
    final Range range = new Range(definitionNode);
    final CallHierarchyItem fromItem =
        new CallHierarchyItem(
            fromProcName,
            SymbolKind.Function,
            uriStr,
            Lsp4jConversion.rangeToLsp4j(range),
            Lsp4jConversion.rangeToLsp4j(range));
    final String typeStrStr = AnonymousNamer.getNameForProcedure(definitionNode).getFullString();
    final Map<String, String> data =
        Map.of(
            DATA_TYPE_STRING, typeStrStr,
            DATA_URI, uriStr);
    fromItem.setData(data);
    final Range fromRange = new Range(calledNode);
    return new CallHierarchyIncomingCall(
        fromItem, List.of(Lsp4jConversion.rangeToLsp4j(fromRange)));
  }

  public List<CallHierarchyOutgoingCall> callHierarchyOutgoingCalls(final CallHierarchyItem item) {
    final JsonElement element = (JsonElement) item.getData();
    final JsonObject object = element.getAsJsonObject();
    final String uriStr = object.getAsJsonPrimitive(DATA_URI).getAsString();
    final URI uri = URI.create(uriStr);

    // TODO: This can give multiple files! Should we store path in data as well?
    final Path path = Path.of(uri); // TODO: What about memory:// URIs? These should blackhole.
    final String text;
    try {
      text = Files.readString(path, FileCharsetDeterminer.determineCharset(path));
    } catch (final IOException exception) {
      throw new IllegalStateException(exception);
    }

    final MagikTypedFile magikFile = new MagikTypedFile(uri, text, this.definitionKeeper);
    final String itemName = item.getName();
    final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();
    return magikFile.getMagikDefinitions().stream()
        .filter(MethodDefinition.class::isInstance)
        .map(MethodDefinition.class::cast)
        .filter(methodDef -> methodDef.getName().equals(itemName))
        .map(MethodDefinition::getNode)
        .flatMap(node -> node.getDescendants(MagikGrammar.METHOD_INVOCATION).stream())
        .flatMap(
            methodInvocationNode -> {
              // Determine/reason the type the method is called on.
              final MethodInvocationNodeHelper helper =
                  new MethodInvocationNodeHelper(methodInvocationNode);
              final AstNode receiverNode = helper.getReceiverNode();
              final ExpressionResultString result = reasonerState.getNodeType(receiverNode);
              final TypeString resultTypeStr = result.get(0, TypeString.UNDEFINED);
              final TypeString typeStr = SelfHelper.substituteSelf(resultTypeStr, receiverNode);
              if (typeStr.isUndefined()) {
                return null;
              }

              // Construct the CallHierarchyItem/CallHierarchyOutgoingCall for method
              // definition.
              final String calledMethodName = helper.getMethodName();
              final TypeStringResolver resolver = magikFile.getTypeStringResolver();
              return resolver.getMethodDefinitions(typeStr, calledMethodName).stream()
                  .map(
                      calledMethodDef ->
                          this.createOutgoingCall(methodInvocationNode, calledMethodDef));
            })
        .toList();
  }

  private CallHierarchyOutgoingCall createOutgoingCall(
        final AstNode methodInvocationNode, final MethodDefinition calledMethodDef) {
    final Location calledMethodLocation = calledMethodDef.getLocation();
    final Location validCalledMethodLocation = Location.validLocation(calledMethodLocation);
    final Range calledMethodRange = validCalledMethodLocation.getRange();
    final CallHierarchyItem toItem =
        new CallHierarchyItem(
            calledMethodDef.getName(),
            SymbolKind.Method,
            validCalledMethodLocation.getUri().toString(),
            Lsp4jConversion.rangeToLsp4j(calledMethodRange),
            Lsp4jConversion.rangeToLsp4j(calledMethodRange));
    final Map<String, String> data =
        Map.of(
            DATA_TYPE_STRING,
            calledMethodDef.getTypeName().getFullString(),
            DATA_METHOD_NAME,
            calledMethodDef.getMethodName(),
            DATA_URI,
            validCalledMethodLocation.getUri().toString());
    toItem.setData(data);
    final Range fromRange = new Range(methodInvocationNode);
    return new CallHierarchyOutgoingCall(toItem, List.of(Lsp4jConversion.rangeToLsp4j(fromRange)));
  }
}
