package nl.ramsolutions.sw.magik.languageserver.callhierarchy;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.SmallworldProjectExtension;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import org.eclipse.lsp4j.CallHierarchyIncomingCall;
import org.eclipse.lsp4j.CallHierarchyItem;
import org.eclipse.lsp4j.CallHierarchyOutgoingCall;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SymbolKind;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/** Tests for {@link CallHierarchyProvider}. */
@SuppressWarnings("checkstyle:MagicNumber")
class CallHierarchyProviderTest {

  private static final Gson GSON = new Gson();

  @RegisterExtension
  final SmallworldProjectExtension smallworldProject = new SmallworldProjectExtension();

  // ---- prepareCallHierarchy ----

  @Test
  void testPrepareCallHierarchyMethodDefinition() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final String code =
        """
        _method exemplar.method_name
        _endmethod
        """;
    final MagikTypedFile magikFile =
        new MagikTypedFile(MagikTypedFile.DEFAULT_URI, code, definitionKeeper);
    final CallHierarchyProvider provider = new CallHierarchyProvider(definitionKeeper);
    final Position position = new Position(1, 10); // On 'exemplar' (lines are 1-based)

    final List<CallHierarchyItem> items = provider.prepareCallHierarchy(magikFile, position);

    assertThat(items).isNotNull().hasSize(1);
    final CallHierarchyItem item = items.get(0);
    assertThat(item.getKind()).isEqualTo(SymbolKind.Method);
    assertThat(item.getName()).contains("method_name");
  }

  @Test
  void testPrepareCallHierarchyNamedProcedureDefinition() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final String code =
        """
        x << _proc @my_proc()
        _endproc
        """;
    final MagikTypedFile magikFile =
        new MagikTypedFile(MagikTypedFile.DEFAULT_URI, code, definitionKeeper);
    final CallHierarchyProvider provider = new CallHierarchyProvider(definitionKeeper);
    final Position position = new Position(1, 15); // Inside '@my_proc' (lines are 1-based)

    final List<CallHierarchyItem> items = provider.prepareCallHierarchy(magikFile, position);

    assertThat(items).isNotNull().hasSize(1);
    final CallHierarchyItem item = items.get(0);
    assertThat(item.getKind()).isEqualTo(SymbolKind.Function);
    assertThat(item.getName()).isEqualTo("my_proc");
  }

  @Test
  void testPrepareCallHierarchyAnonymousProcedureDefinition() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final String code =
        """
        _proc()
        _endproc
        """;
    final MagikTypedFile magikFile =
        new MagikTypedFile(MagikTypedFile.DEFAULT_URI, code, definitionKeeper);
    final CallHierarchyProvider provider = new CallHierarchyProvider(definitionKeeper);
    final Position position = new Position(1, 3); // Inside '_proc' (lines are 1-based)

    final List<CallHierarchyItem> items = provider.prepareCallHierarchy(magikFile, position);

    assertThat(items).isNotNull().hasSize(1);
    final CallHierarchyItem item = items.get(0);
    assertThat(item.getKind()).isEqualTo(SymbolKind.Function);
    assertThat(item.getName()).isEqualTo("<anonymous>");
  }

  @Test
  void testPrepareCallHierarchyNothingAtPosition() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final String code =
        """
        # just a comment
        """;
    final MagikTypedFile magikFile =
        new MagikTypedFile(MagikTypedFile.DEFAULT_URI, code, definitionKeeper);
    final CallHierarchyProvider provider = new CallHierarchyProvider(definitionKeeper);
    final Position position = new Position(1, 5); // Inside comment (lines are 1-based)

    final List<CallHierarchyItem> items = provider.prepareCallHierarchy(magikFile, position);

    // When nodeSurrounding finds no grammar node or no definition ancestor, returns null
    assertThat(items).isNull();
  }

  // ---- callHierarchyIncomingCalls ----

  @Test
  void testCallHierarchyIncomingCallsCallerIsMethod() throws IOException {
    final String code =
        """
        def_slotted_exemplar(:my_type, {})
        _method my_type.target_method
        _endmethod
        _method my_type.caller_method
          _self.target_method
        _endmethod
        """;
    final Path path = this.smallworldProject.pathOf("/source.magik");
    this.smallworldProject.addMagikFile(path, code);
    final IDefinitionKeeper definitionKeeper = this.smallworldProject.getDefinitionKeeper();
    final CallHierarchyProvider provider = new CallHierarchyProvider(definitionKeeper);

    final JsonObject data = new JsonObject();
    data.addProperty("uri", path.toUri().toString());
    data.addProperty("typeString", "user:my_type");
    data.addProperty("methodName", "target_method");
    final CallHierarchyItem item = new CallHierarchyItem();
    item.setData(data);

    final List<CallHierarchyIncomingCall> calls = provider.callHierarchyIncomingCalls(item);

    assertThat(calls).hasSize(1);
    assertThat(calls.get(0).getFrom().getKind()).isEqualTo(SymbolKind.Method);
    assertThat(calls.get(0).getFrom().getName()).contains("caller_method");
  }

  @Test
  void testCallHierarchyIncomingCallsAnswersInvokesMethodAnnotation() throws IOException {
    final String code =
        """
        def_slotted_exemplar(:a, {})
        _method a.method_name
        _endmethod
        _method a.caller(name)
          ## @invokes_method {user:a.method_name}
          _self.perform(name)
        _endmethod
        _method a.real_caller
          _self.method_name
        _endmethod
        """;

    final List<CallHierarchyIncomingCall> calls = this.findIncomingCalls(code);

    final List<String> fromNames = this.getFromNames(calls);
    assertThat(fromNames).containsExactlyInAnyOrder("user:a.caller()", "user:a.real_caller");
    final CallHierarchyIncomingCall annotatedCall = this.getCallFrom(calls, "user:a.caller()");
    final List<Range> fromRanges = annotatedCall.getFromRanges();
    // The name `caller` of the annotated definition.
    final org.eclipse.lsp4j.Position nameStart = new org.eclipse.lsp4j.Position(3, 10);
    final org.eclipse.lsp4j.Position nameEnd = new org.eclipse.lsp4j.Position(3, 16);
    final Range nameRange = new Range(nameStart, nameEnd);
    assertThat(fromRanges).containsExactly(nameRange);
    final CallHierarchyItem from = annotatedCall.getFrom();
    final SymbolKind fromKind = from.getKind();
    assertThat(fromKind).isEqualTo(SymbolKind.Method);
  }

  @Test
  void testCallHierarchyIncomingCallsSkipsInvokesMethodAnnotationOnOtherReceiver()
      throws IOException {
    final String code =
        """
        def_slotted_exemplar(:a, {})
        def_slotted_exemplar(:b, {})
        _method a.method_name
        _endmethod
        _method b.caller(name)
          ## @invokes_method {user:b.method_name}
          _self.perform(name)
        _endmethod
        _method a.real_caller
          _self.method_name
        _endmethod
        """;

    final List<CallHierarchyIncomingCall> calls = this.findIncomingCalls(code);

    final List<String> fromNames = this.getFromNames(calls);
    assertThat(fromNames).containsExactly("user:a.real_caller");
  }

  @Test
  void testCallHierarchyIncomingCallsAnswersInvokesMethodAnnotationOnProcedure()
      throws IOException {
    final String code =
        """
        def_slotted_exemplar(:a, {})
        _method a.method_name
        _endmethod
        _global p << _proc@p(name)
          ## @invokes_method {user:a.method_name}
          a.perform(name)
        _endproc
        """;

    final List<CallHierarchyIncomingCall> calls = this.findIncomingCalls(code);

    final List<String> fromNames = this.getFromNames(calls);
    assertThat(fromNames).containsExactly("p");
    final CallHierarchyIncomingCall call = calls.get(0);
    final CallHierarchyItem from = call.getFrom();
    final SymbolKind fromKind = from.getKind();
    assertThat(fromKind).isEqualTo(SymbolKind.Function);
  }

  @Test
  void testCallHierarchyIncomingCallsCallerIsTopLevelProcedure() throws IOException {
    final String code =
        """
        def_slotted_exemplar(:a, {})
        _method a.method_name
        _endmethod
        _global p << _proc@p()
          a.method_name
        _endproc
        q << _proc@q()
          a.method_name
        _endproc
        """;

    final List<CallHierarchyIncomingCall> calls = this.findIncomingCalls(code);

    final List<String> fromNames = this.getFromNames(calls);
    assertThat(fromNames).containsExactlyInAnyOrder("p", "q");
  }

  @Test
  void testCallHierarchyIncomingCallsCallerIsProcedureInMethodOnce() throws IOException {
    final String code =
        """
        def_slotted_exemplar(:a, {})
        _method a.method_name
        _endmethod
        _method a.runner
          _return _proc@p()
            a.method_name
          _endproc
        _endmethod
        """;

    final List<CallHierarchyIncomingCall> calls = this.findIncomingCalls(code);

    final List<String> fromNames = this.getFromNames(calls);
    assertThat(fromNames).containsExactly("p");
  }

  @Test
  void testCallHierarchyIncomingCallsAnnotatedProcedureArgumentIsNotEnclosingCall()
      throws IOException {
    final String code =
        """
        def_slotted_exemplar(:a, {})
        _method a.method_name
        _endmethod
        _method a.do(callback)
        _endmethod
        _method a.runner
          _self.do(_proc@p(name)
            ## @invokes_method {user:a.method_name}
          _endproc)
        _endmethod
        """;

    final List<CallHierarchyIncomingCall> calls = this.findIncomingCalls(code);

    final List<String> fromNames = this.getFromNames(calls);
    assertThat(fromNames).containsExactly("p");
  }

  private List<CallHierarchyIncomingCall> findIncomingCalls(final String code) throws IOException {
    final Path path = this.smallworldProject.pathOf("/source.magik");
    this.smallworldProject.addMagikFile(path, code);
    final IDefinitionKeeper definitionKeeper = this.smallworldProject.getDefinitionKeeper();
    final CallHierarchyProvider provider = new CallHierarchyProvider(definitionKeeper);

    final JsonObject data = new JsonObject();
    final URI uri = path.toUri();
    final String uriStr = uri.toString();
    data.addProperty("uri", uriStr);
    data.addProperty("typeString", "user:a");
    data.addProperty("methodName", "method_name");
    final CallHierarchyItem item = new CallHierarchyItem();
    item.setData(data);

    return provider.callHierarchyIncomingCalls(item);
  }

  private List<String> getFromNames(final List<CallHierarchyIncomingCall> calls) {
    return calls.stream()
        .map(CallHierarchyIncomingCall::getFrom)
        .map(CallHierarchyItem::getName)
        .toList();
  }

  private CallHierarchyIncomingCall getCallFrom(
      final List<CallHierarchyIncomingCall> calls, final String fromName) {
    return calls.stream().filter(call -> this.isCallFrom(call, fromName)).findFirst().orElseThrow();
  }

  private boolean isCallFrom(final CallHierarchyIncomingCall call, final String fromName) {
    final CallHierarchyItem from = call.getFrom();
    final String name = from.getName();
    return name.equals(fromName);
  }

  // ---- callHierarchyOutgoingCalls ----

  @Test
  void testCallHierarchyOutgoingCallsFromMethod() throws IOException {
    final String codeTarget =
        """
        def_slotted_exemplar(:my_type, {})
        _method my_type.target_method
        _endmethod
        """;
    final Path pathTarget = this.smallworldProject.pathOf("/target.magik");
    this.smallworldProject.addMagikFile(pathTarget, codeTarget);

    final String codeCaller =
        """
        _method my_type.caller_method
          _self.target_method
        _endmethod
        """;
    final Path pathCaller = this.smallworldProject.pathOf("/caller.magik");
    final MagikTypedFile callerFile = this.smallworldProject.addMagikFile(pathCaller, codeCaller);

    final IDefinitionKeeper definitionKeeper = this.smallworldProject.getDefinitionKeeper();
    final CallHierarchyProvider provider = new CallHierarchyProvider(definitionKeeper);

    // Position on 'caller_method' (line 1: "_method my_type.caller_method", col 20, 1-based)
    final Position position = new Position(1, 20);
    final List<CallHierarchyItem> items = provider.prepareCallHierarchy(callerFile, position);
    assertThat(items).hasSize(1);
    final CallHierarchyItem item = items.get(0);

    // lsp4j stores data as Map<String, String> in-process; convert for callHierarchyOutgoingCalls
    final JsonObject jsonData = GSON.toJsonTree(item.getData()).getAsJsonObject();
    item.setData(jsonData);

    final List<CallHierarchyOutgoingCall> calls = provider.callHierarchyOutgoingCalls(item);

    assertThat(calls).isNotEmpty();
    assertThat(calls.get(0).getTo().getName()).contains("target_method");
  }

  @Test
  void testCallHierarchyOutgoingCallsAnswersInvokesMethodAnnotation() throws IOException {
    final String code =
        """
        def_slotted_exemplar(:a, {})
        _method a.method_name
        _endmethod
        _method a.caller(name)
          ## @invokes_method {user:a.method_name}
        _endmethod
        """;
    final Path path = this.smallworldProject.pathOf("/source.magik");
    final MagikTypedFile magikFile = this.smallworldProject.addMagikFile(path, code);
    final IDefinitionKeeper definitionKeeper = this.smallworldProject.getDefinitionKeeper();
    final CallHierarchyProvider provider = new CallHierarchyProvider(definitionKeeper);
    final Position position = new Position(4, 12); // On `caller`.
    final List<CallHierarchyItem> items = provider.prepareCallHierarchy(magikFile, position);
    final CallHierarchyItem item = items.get(0);
    final Object itemData = item.getData();
    final JsonElement jsonData = GSON.toJsonTree(itemData);
    item.setData(jsonData);

    final List<CallHierarchyOutgoingCall> calls = provider.callHierarchyOutgoingCalls(item);

    assertThat(calls).hasSize(1);
    final CallHierarchyOutgoingCall call = calls.get(0);
    final CallHierarchyItem to = call.getTo();
    final String toName = to.getName();
    assertThat(toName).isEqualTo("user:a.method_name");
    final List<Range> fromRanges = call.getFromRanges();
    // The name `caller` of the annotated definition.
    final org.eclipse.lsp4j.Position nameStart = new org.eclipse.lsp4j.Position(3, 10);
    final org.eclipse.lsp4j.Position nameEnd = new org.eclipse.lsp4j.Position(3, 16);
    final Range nameRange = new Range(nameStart, nameEnd);
    assertThat(fromRanges).containsExactly(nameRange);
  }

  @Test
  void testCallHierarchyOutgoingCallsFromProcedure() throws IOException {
    final String codeTarget =
        """
        def_slotted_exemplar(:my_type, {})
        _method my_type.target_method
        _endmethod
        """;
    final Path pathTarget = this.smallworldProject.pathOf("/target.magik");
    this.smallworldProject.addMagikFile(pathTarget, codeTarget);

    final String codeCaller =
        """
        x << _proc @my_proc()
          my_type.new().target_method
        _endproc
        """;
    final Path pathCaller = this.smallworldProject.pathOf("/caller_proc.magik");
    final MagikTypedFile callerFile = this.smallworldProject.addMagikFile(pathCaller, codeCaller);

    final IDefinitionKeeper definitionKeeper = this.smallworldProject.getDefinitionKeeper();
    final CallHierarchyProvider provider = new CallHierarchyProvider(definitionKeeper);

    // Position inside '@my_proc' (line 1: "x << _proc @my_proc()", col 15, 1-based)
    final Position position = new Position(1, 15);
    final List<CallHierarchyItem> items = provider.prepareCallHierarchy(callerFile, position);
    assertThat(items).hasSize(1);
    final CallHierarchyItem item = items.get(0);
    assertThat(item.getKind()).isEqualTo(SymbolKind.Function);

    // Convert data map to JsonObject (DATA_METHOD_NAME absent → procedure path taken)
    final JsonObject jsonData = GSON.toJsonTree(item.getData()).getAsJsonObject();
    item.setData(jsonData);

    // The item must carry the procedure's real name, not an anonymous positional name.
    final String typeStringStr =
        jsonData.getAsJsonPrimitive(CallHierarchyProvider.DATA_TYPE_STRING).getAsString();
    assertThat(typeStringStr).isEqualTo("user:x");

    // Verify the procedure body is scanned without error (type of receiver is unresolved here).
    final List<CallHierarchyOutgoingCall> calls = provider.callHierarchyOutgoingCalls(item);
    assertThat(calls).isNotNull();
  }
}
