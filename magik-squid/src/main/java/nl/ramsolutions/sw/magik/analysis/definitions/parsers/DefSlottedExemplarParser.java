package nl.ramsolutions.sw.magik.analysis.definitions.parsers;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import nl.ramsolutions.sw.definitions.ModuleDefFileScanner;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MagikDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ParameterDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.SlotDefinition;
import nl.ramsolutions.sw.magik.analysis.helpers.ArgumentsNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.PragmaNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.ProcedureInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.SimpleVectorNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.api.MagikOperator;
import nl.ramsolutions.sw.magik.parser.MagikCommentExtractor;
import nl.ramsolutions.sw.magik.parser.TypeDocParser;

/** {@code def_slotted_exemplar} parser. */
public class DefSlottedExemplarParser extends BaseDefParser {

  private static final String DEF_SLOTTED_EXEMPLAR = "def_slotted_exemplar";
  private static final String SW_DEF_SLOTTED_EXEMPLAR = "sw:def_slotted_exemplar";

  /**
   * Constructor.
   *
   * @param node {@code def_slotted_exemplar()} node.
   */
  public DefSlottedExemplarParser(final MagikFile magikFile, final AstNode node) {
    super(magikFile, node);
  }

  /**
   * Test if node is a {@code def_slotted_exemplar()}.
   *
   * @param node Node to test
   * @return True if node is a {@code def_slotted_exemplar()}, false otherwise.
   */
  public static boolean isDefSlottedExemplar(final AstNode node) {
    if (!node.is(MagikGrammar.PROCEDURE_INVOCATION)) {
      return false;
    }

    final ProcedureInvocationNodeHelper helper = new ProcedureInvocationNodeHelper(node);
    if (!helper.isProcedureInvocationOf(DEF_SLOTTED_EXEMPLAR)
        && !helper.isProcedureInvocationOf(SW_DEF_SLOTTED_EXEMPLAR)) {
      return false;
    }

    // Some sanity.
    final AstNode argumentsNode = node.getFirstChild(MagikGrammar.ARGUMENTS);
    final ArgumentsNodeHelper argumentsHelper = new ArgumentsNodeHelper(argumentsNode);
    final AstNode argument0Node = argumentsHelper.getArgument(0, MagikGrammar.SYMBOL);
    if (argument0Node == null) {
      return false;
    }

    final AstNode argument1Node = argumentsHelper.getArgument(1, MagikGrammar.SIMPLE_VECTOR);
    if (argument1Node == null) {
      return false;
    }

    return true;
  }

  /**
   * Parse defitions.
   *
   * @return List of parsed definitions.
   */
  @Override
  public List<MagikDefinition> parseDefinitions() {
    final AstNode argumentsNode = this.node.getFirstChild(MagikGrammar.ARGUMENTS);
    final ArgumentsNodeHelper argumentsHelper = new ArgumentsNodeHelper(argumentsNode);
    final AstNode argument0Node = argumentsHelper.getArgument(0, MagikGrammar.SYMBOL);
    if (argument0Node == null) {
      throw new IllegalStateException();
    }
    final AstNode argument1Node = argumentsHelper.getArgument(1, MagikGrammar.SIMPLE_VECTOR);
    if (argument1Node == null) {
      throw new IllegalStateException();
    }

    // Figure location.
    final URI uri = this.node.getToken().getURI();
    final Location location = new Location(uri, this.node);

    // Figure timestamp.
    final Instant timestamp = this.magikFile.getTimestamp();

    // Figure module name.
    final String moduleName = ModuleDefFileScanner.getModuleName(uri);

    // Figure statement node.
    final AstNode statementNode = this.node.getFirstAncestor(MagikGrammar.STATEMENT);

    // Figure pakkage.
    final String packageName = this.getCurrentPakkage();

    // Figure name.
    final String identifier = argument0Node.getTokenValue().substring(1);
    final TypeString name = TypeString.ofIdentifier(identifier, packageName);

    // Figure slot types.
    final AstNode parentNode = this.node.getParent();
    final TypeDocParser docParser = new TypeDocParser(parentNode);
    final Map<String, TypeString> slotTypes = docParser.getSlotTypes();

    // Figure slots.
    final List<SlotDefinition> slots = new ArrayList<>();
    final List<MethodDefinition> methodDefinitions = new ArrayList<>();
    for (final AstNode slotDefNode : // NOSONAR
        argument1Node.getChildren(MagikGrammar.EXPRESSION)) {
      final SimpleVectorNodeHelper simpleVectorHelper =
          SimpleVectorNodeHelper.fromExpressionSafe(slotDefNode);
      if (simpleVectorHelper == null) {
        continue;
      }

      final AstNode slotNameNode = simpleVectorHelper.getNth(0, MagikGrammar.SYMBOL);
      if (slotNameNode == null) {
        continue;
      }

      // Slot definitions.
      final Location slotLocation = new Location(uri, slotDefNode);
      final String slotNameSymbol = slotNameNode.getTokenValue();
      final String slotName = slotNameSymbol.substring(1);
      final TypeString slotTypeRef =
          Objects.requireNonNullElse(slotTypes.get(slotName), TypeString.UNDEFINED);
      final SlotDefinition slot =
          new SlotDefinition(
              slotLocation, timestamp, moduleName, null, slotDefNode, slotName, slotTypeRef);
      slots.add(slot);

      // Method definitions.
      final AstNode flagNode = simpleVectorHelper.getNth(2, MagikGrammar.SYMBOL);
      final AstNode flavorNode = simpleVectorHelper.getNth(3, MagikGrammar.SYMBOL);
      if (flagNode != null && flavorNode != null) {
        final String flag = flagNode.getTokenValue();
        final String flavor = flavorNode.getTokenValue();
        final TypeString exemplarName = TypeString.ofIdentifier(identifier, packageName);
        final List<MethodDefinition> slotMethodDefinitions =
            this.generateSlotMethods(
                timestamp,
                moduleName,
                slotDefNode,
                exemplarName,
                slotName,
                flag,
                flavor,
                slotTypeRef);
        methodDefinitions.addAll(slotMethodDefinitions);
      }
    }

    // Figure parents.
    final AstNode argument2Node = argumentsHelper.getArgument(2);
    final List<TypeString> parents = this.extractParents(argument2Node);

    // Figure doc.
    final String doc = MagikCommentExtractor.extractDocComment(parentNode);

    // Figure topics.
    final AstNode pragmaNode = PragmaNodeHelper.getPragmaNode(node);
    final Set<String> topics =
        pragmaNode != null
            ? new PragmaNodeHelper(pragmaNode).getAllTopics()
            : Collections.emptySet();

    final ExemplarDefinition slottedExemplarDefinition =
        new ExemplarDefinition(
            location,
            timestamp,
            moduleName,
            doc,
            statementNode,
            ExemplarDefinition.Sort.SLOTTED,
            name,
            slots,
            parents,
            topics);

    final List<MagikDefinition> definitions = new ArrayList<>();
    definitions.add(slottedExemplarDefinition);
    definitions.addAll(methodDefinitions);
    return definitions;
  }

  @SuppressWarnings({"checkstyle:ParameterNumber", "java:S107"})
  private List<MethodDefinition> generateSlotMethods(
      final @Nullable Instant timestamp,
      final @Nullable String moduleName,
      final AstNode node,
      final TypeString exemplarName,
      final String slotName,
      final String flag,
      final String flavor,
      final TypeString slotTypeRef) {
    final List<MethodDefinition> methodDefinitions = new ArrayList<>();

    // Figure location.
    final URI uri = node.getToken().getURI();
    final Location location = new Location(uri, node);

    if (flag.equals(FLAG_READ) || flag.equals(FLAG_READABLE)) {
      // get
      final String getName = slotName;
      final Set<MethodDefinition.Modifier> getModifiers = new HashSet<>();
      if (!flavor.equals(FLAVOR_PUBLIC)) {
        getModifiers.add(MethodDefinition.Modifier.PRIVATE);
      }
      final List<ParameterDefinition> getParameters = Collections.emptyList();
      final MethodDefinition getMethod =
          new MethodDefinition(
              location,
              timestamp,
              moduleName,
              null,
              node,
              exemplarName,
              getName,
              getModifiers,
              getParameters,
              null,
              Collections.emptySet(),
              new ExpressionResultString(slotTypeRef),
              ExpressionResultString.EMPTY);
      methodDefinitions.add(getMethod);
    } else if (flag.equals(FLAG_WRITE) || flag.equals(FLAG_WRITABLE)) {
      // get
      final Set<MethodDefinition.Modifier> getModifiers = new HashSet<>();
      if (!flavor.equals(FLAVOR_PUBLIC) && !flavor.equals(FLAVOR_READ_ONLY)) {
        getModifiers.add(MethodDefinition.Modifier.PRIVATE);
      }
      final List<ParameterDefinition> getParameters = Collections.emptyList();
      final MethodDefinition getMethod =
          new MethodDefinition(
              location,
              timestamp,
              moduleName,
              null,
              node,
              exemplarName,
              slotName,
              getModifiers,
              getParameters,
              null,
              Collections.emptySet(),
              new ExpressionResultString(slotTypeRef),
              ExpressionResultString.EMPTY);
      methodDefinitions.add(getMethod);

      // set
      final String setName = slotName + MagikOperator.CHEVRON.getValue();
      final Set<MethodDefinition.Modifier> setModifiers = new HashSet<>();
      if (!flavor.equals(FLAVOR_PUBLIC)) {
        setModifiers.add(MethodDefinition.Modifier.PRIVATE);
      }
      final List<ParameterDefinition> setParameters = Collections.emptyList();
      final ParameterDefinition assignmentParam =
          new ParameterDefinition(
              location,
              timestamp,
              moduleName,
              null,
              node,
              "val",
              ParameterDefinition.Modifier.NONE,
              slotTypeRef);
      final MethodDefinition setMethod =
          new MethodDefinition(
              location,
              timestamp,
              moduleName,
              null,
              node,
              exemplarName,
              setName,
              setModifiers,
              setParameters,
              assignmentParam,
              Collections.emptySet(),
              new ExpressionResultString(TypeString.ofParameterRef("val")),
              ExpressionResultString.EMPTY);
      methodDefinitions.add(setMethod);

      // boot
      final String bootName = slotName + MagikOperator.BOOT_CHEVRON.getValue();
      final MethodDefinition bootMethod =
          new MethodDefinition(
              location,
              timestamp,
              moduleName,
              null,
              node,
              exemplarName,
              bootName,
              setModifiers,
              setParameters,
              assignmentParam,
              Collections.emptySet(),
              new ExpressionResultString(slotTypeRef),
              ExpressionResultString.EMPTY);
      methodDefinitions.add(bootMethod);
    }
    return methodDefinitions;
  }
}
