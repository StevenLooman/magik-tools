package nl.ramsolutions.sw.magik.languageserver.callhierarchy;

import com.sonar.sslr.api.AstNode;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.analysis.definitions.parsers.AnonymousNamer;
import nl.ramsolutions.sw.magik.analysis.helpers.ProcedureDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.languageserver.Lsp4jConversion;
import org.eclipse.lsp4j.CallHierarchyItem;
import org.eclipse.lsp4j.SymbolKind;

/** Provides a call hierarchy item for a procedure definition. */
public class ProcedureDefinitionCallHierarchyModule implements CallHierarchyModule {

  @Override
  public Optional<List<CallHierarchyItem>> tryCallHierarchy(final CallHierarchyContext context) {
    final AstNode wantedNode = context.wantedNode();
    if (!wantedNode.is(MagikGrammar.PROCEDURE_DEFINITION)) {
      return Optional.empty();
    }

    final MagikTypedFile magikFile = context.magikFile();
    final String uriStr = magikFile.getUri().toString();
    final Range range = new Range(wantedNode);

    final ProcedureDefinitionNodeHelper helper = new ProcedureDefinitionNodeHelper(wantedNode);
    final String procName = helper.getProcedureName();
    final String displayName = procName != null ? procName : "<anonymous>";
    final String typeStrStr = AnonymousNamer.getNameForProcedure(wantedNode).getFullString();
    final CallHierarchyItem item =
        new CallHierarchyItem(
            displayName,
            SymbolKind.Function,
            uriStr,
            Lsp4jConversion.rangeToLsp4j(range),
            Lsp4jConversion.rangeToLsp4j(range));
    final Map<String, String> data =
        Map.of(
            CallHierarchyProvider.DATA_TYPE_STRING, typeStrStr,
            CallHierarchyProvider.DATA_URI, uriStr);
    item.setData(data);
    return Optional.of(List.of(item));
  }
}
