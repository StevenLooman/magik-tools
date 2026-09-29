package nl.ramsolutions.sw.magik.parser;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import com.sonar.sslr.api.AstVisitor;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.api.TypeStringGrammar;

/** Visitor which constructs TypeStrings from the TypeStringGrammar. */
public final class TypeStringBuilderVisitor implements AstVisitor {

  private final Map<AstNode, TypeString> mapping = new HashMap<>();
  private final String currentPakkage;
  private AstNode topNode;

  /**
   * Constructor.
   *
   * @param currentPakkege Current package.
   */
  public TypeStringBuilderVisitor(final String currentPakkege) {
    this.currentPakkage = currentPakkege;
  }

  @CheckForNull
  public TypeString getTypeString() {
    return this.mapping.get(this.topNode);
  }

  @Override
  public List<AstNodeType> getAstNodeTypesToVisit() {
    return List.of(
        TypeStringGrammar.TYPE_UNDEFINED,
        TypeStringGrammar.TYPE_CLONE,
        TypeStringGrammar.TYPE_SELF,
        TypeStringGrammar.TYPE_INVOKABLE,
        TypeStringGrammar.TYPE_PARAMETER_REFERENCE,
        TypeStringGrammar.TYPE_SLOT_REFERENCE,
        TypeStringGrammar.TYPE_GENERIC_DEFINITION,
        TypeStringGrammar.TYPE_GENERIC_REFERENCE,
        TypeStringGrammar.TYPE_IDENTIFIER,
        TypeStringGrammar.TYPE_STRING,
        TypeStringGrammar.TYPE_TUPLE,
        TypeStringGrammar.SYNTAX_ERROR);
  }

  @Override
  public void visitFile(final @Nullable AstNode node) {
    this.topNode = node;
  }

  @Override
  public void leaveFile(final @Nullable AstNode node) {
    // Pass.
  }

  @Override
  public void visitNode(final AstNode node) {
    // Pass.
  }

  @Override
  public void leaveNode(final AstNode node) {
    if (node.is(TypeStringGrammar.TYPE_UNDEFINED)) {
      this.buildUndefined(node);
    } else if (node.is(TypeStringGrammar.TYPE_CLONE, TypeStringGrammar.TYPE_SELF)) {
      this.buildSelf(node);
    } else if (node.is(TypeStringGrammar.TYPE_INVOKABLE)) {
      this.buildInvokable(node);
    } else if (node.is(TypeStringGrammar.TYPE_PARAMETER_REFERENCE)) {
      this.buildParameterRef(node);
    } else if (node.is(TypeStringGrammar.TYPE_SLOT_REFERENCE)) {
      this.buildSlotRef(node);
    } else if (node.is(TypeStringGrammar.TYPE_GENERIC_DEFINITION)) {
      this.buildGenericDefinition(node);
    } else if (node.is(TypeStringGrammar.TYPE_GENERIC_REFERENCE)) {
      this.buildGenericReference(node);
    } else if (node.is(TypeStringGrammar.TYPE_IDENTIFIER)) {
      this.buildIdentifier(node);
    } else if (node.is(TypeStringGrammar.TYPE_STRING)) {
      this.buildTypeString(node);
    } else if (node.is(TypeStringGrammar.TYPE_TUPLE)) {
      this.buildTuple(node);
    } else if (node.is(TypeStringGrammar.SYNTAX_ERROR)) {
      this.buildUndefined(node);
    } else {
      throw new IllegalStateException("Unknown node type: " + node.getType());
    }
  }

  private void buildUndefined(final AstNode node) {
    final TypeString part = TypeString.UNDEFINED;

    this.mapping.put(node, part);
  }

  private void buildSelf(final AstNode node) {
    final List<AstNode> genericNodes =
        node.getChildren(
            TypeStringGrammar.TYPE_GENERIC_DEFINITION, TypeStringGrammar.TYPE_GENERIC_REFERENCE);
    if (genericNodes.isEmpty()) {
      this.mapping.put(node, TypeString.SELF);
      return;
    }

    final List<TypeString> genericsList = genericNodes.stream().map(this.mapping::get).toList();
    final TypeString[] genericsArr = genericsList.toArray(TypeString[]::new);
    final TypeString part =
        TypeString.ofIdentifier("_self", TypeString.ANONYMOUS_PACKAGE, genericsArr);
    this.mapping.put(node, part);
  }

  private void buildInvokable(final AstNode node) {
    final List<AstNode> genericNodes =
        node.getChildren(
            TypeStringGrammar.TYPE_GENERIC_DEFINITION, TypeStringGrammar.TYPE_GENERIC_REFERENCE);
    if (genericNodes.isEmpty()) {
      this.mapping.put(node, TypeString.INVOKABLE);
      return;
    }

    final List<TypeString> genericsList = genericNodes.stream().map(this.mapping::get).toList();
    final TypeString[] genericsArr = genericsList.toArray(TypeString[]::new);
    final TypeString part = TypeString.ofInvokable(genericsArr);
    this.mapping.put(node, part);
  }

  private void buildParameterRef(final AstNode node) {
    final List<AstNode> childAsts = node.getChildren();
    final AstNode identifierAst = childAsts.get(2);
    final String refStr = identifierAst.getTokenValue();
    final AstNode genericRefNode = node.getFirstChild(TypeStringGrammar.TYPE_GENERIC_REFERENCE);
    final TypeString part =
        genericRefNode == null
            ? TypeString.ofParameterRef(refStr)
            : TypeString.ofParameterRef(refStr, this.mapping.get(genericRefNode));

    this.mapping.put(node, part);
  }

  private void buildSlotRef(final AstNode node) {
    final List<AstNode> childAsts = node.getChildren();
    final AstNode identifierAst = childAsts.get(2);
    final String refStr = identifierAst.getTokenValue();
    final AstNode genericRefNode = node.getFirstChild(TypeStringGrammar.TYPE_GENERIC_REFERENCE);
    final TypeString part =
        genericRefNode == null
            ? TypeString.ofSlotRef(refStr)
            : TypeString.ofSlotRef(refStr, this.mapping.get(genericRefNode));

    this.mapping.put(node, part);
  }

  private void buildGenericDefinition(final AstNode node) {
    final AstNode identifierNode = node.getFirstChild(TypeStringGrammar.TYPE_IDENTIFIER);
    final String identifier = identifierNode.getTokenValue();
    AstNode valueNode = node.getFirstChild(TypeStringGrammar.TYPE_STRING);
    if (valueNode == null) {
      valueNode = node.getFirstChild(TypeStringGrammar.TYPE_TUPLE);
    }
    final TypeString typeString = this.mapping.get(valueNode);
    final TypeString part = TypeString.ofGenericDefinition(identifier, typeString);

    this.mapping.put(node, part);
  }

  private void buildTuple(final AstNode node) {
    final List<AstNode> typeStringNodes = node.getChildren(TypeStringGrammar.TYPE_STRING);
    final List<TypeString> elements = new ArrayList<>();
    for (final AstNode typeStringNode : typeStringNodes) {
      final TypeString element = this.mapping.get(typeStringNode);
      elements.add(element);
    }
    final AstNode variadicNode = node.getFirstChild(TypeStringGrammar.Punctuator.TYPE_VARIADIC);
    if (variadicNode != null && !elements.isEmpty()) {
      final int lastIndex = elements.size() - 1;
      final TypeString last = elements.get(lastIndex);
      final TypeString variadicLast = TypeString.ofVariadic(last);
      elements.set(lastIndex, variadicLast);
    }
    final TypeString[] elementsArr = elements.toArray(TypeString[]::new);
    final TypeString part = TypeString.ofTuple(elementsArr);

    this.mapping.put(node, part);
  }

  private void buildGenericReference(final AstNode node) {
    final AstNode identifierNode = node.getFirstChild(TypeStringGrammar.SIMPLE_IDENTIFIER);
    final String identifier = identifierNode.getTokenValue();
    final TypeString part = TypeString.ofGenericReference(identifier);

    this.mapping.put(node, part);
  }

  private void buildIdentifier(final AstNode node) {
    final String str = node.getTokenValue();
    final List<AstNode> genericNodes =
        node.getChildren(
            TypeStringGrammar.TYPE_GENERIC_DEFINITION, TypeStringGrammar.TYPE_GENERIC_REFERENCE);
    final TypeString[] genericsArr =
        genericNodes.stream().map(this.mapping::get).toList().toArray(TypeString[]::new);
    final TypeString base = TypeString.ofIdentifier(str, this.currentPakkage, genericsArr);
    this.mapping.put(node, base);
  }

  private void buildTypeString(final AstNode node) {
    final List<AstNode> childNodes =
        node.getChildren(
            TypeStringGrammar.TYPE_UNDEFINED,
            TypeStringGrammar.TYPE_CLONE,
            TypeStringGrammar.TYPE_SELF,
            TypeStringGrammar.TYPE_INVOKABLE,
            TypeStringGrammar.TYPE_PARAMETER_REFERENCE,
            TypeStringGrammar.TYPE_SLOT_REFERENCE,
            TypeStringGrammar.TYPE_GENERIC_DEFINITION,
            TypeStringGrammar.TYPE_GENERIC_REFERENCE,
            TypeStringGrammar.TYPE_IDENTIFIER,
            TypeStringGrammar.SYNTAX_ERROR);
    final List<TypeString> childTypeStrings =
        childNodes.stream().map(this.mapping::get).map(Objects::requireNonNull).toList();
    if (childNodes.isEmpty()) {
      throw new IllegalStateException();
    }

    final TypeString[] childTypeStringsArr = childTypeStrings.toArray(TypeString[]::new);
    final TypeString part = TypeString.combine(childTypeStringsArr);
    this.mapping.put(node, part);
  }
}
