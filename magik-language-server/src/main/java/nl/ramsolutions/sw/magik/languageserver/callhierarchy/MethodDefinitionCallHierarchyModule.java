package nl.ramsolutions.sw.magik.languageserver.callhierarchy;

import com.sonar.sslr.api.AstNode;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.languageserver.Lsp4jConversion;
import org.eclipse.lsp4j.CallHierarchyItem;
import org.eclipse.lsp4j.SymbolKind;

/** Provides a call hierarchy item for a method definition. */
public class MethodDefinitionCallHierarchyModule implements CallHierarchyModule {

  @Override
  public Optional<List<CallHierarchyItem>> tryCallHierarchy(final CallHierarchyContext context) {
    final AstNode wantedNode = context.wantedNode();
    if (!wantedNode.is(MagikGrammar.METHOD_DEFINITION)) {
      return Optional.empty();
    }

    final MagikTypedFile magikFile = context.magikFile();
    final String uriStr = magikFile.getUri().toString();
    final Range range = new Range(wantedNode);

    final MethodDefinitionNodeHelper helper = new MethodDefinitionNodeHelper(wantedNode);
    final String fullMethodName = helper.getFullExemplarMethodName();
    final CallHierarchyItem item =
        new CallHierarchyItem(
            fullMethodName,
            SymbolKind.Method,
            uriStr,
            Lsp4jConversion.rangeToLsp4j(range),
            Lsp4jConversion.rangeToLsp4j(range));
    final Map<String, String> data =
        Map.of(
            CallHierarchyProvider.DATA_TYPE_STRING,
            helper.getExemplarTypeString().getFullString(),
            CallHierarchyProvider.DATA_METHOD_NAME,
            helper.getMethodName(),
            CallHierarchyProvider.DATA_URI,
            uriStr);
    item.setData(data);
    return Optional.of(List.of(item));
  }
}
