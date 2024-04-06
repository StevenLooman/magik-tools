package nl.ramsolutions.sw.magik.parser;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import com.sonar.sslr.api.AstVisitor;
import edu.umd.cs.findbugs.annotations.CheckForNull;
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
        TypeStringGrammar.TYPE_PARAMETER_REFERENCE,
        TypeStringGrammar.TYPE_GENERIC_DEFINITION,
        TypeStringGrammar.TYPE_GENERIC_REFERENCE,
        TypeStringGrammar.TYPE_IDENTIFIER,
        TypeStringGrammar.TYPE_STRING,
        TypeStringGrammar.SYNTAX_ERROR);
  }

  @Override
  public void visitFile(final AstNode node) {
    this.topNode = node;
  }

  @Override
  public void leaveFile(final AstNode node) {
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
    } else if (node.is(TypeStringGrammar.TYPE_PARAMETER_REFERENCE)) {
      this.buildParameterRef(node);
    } else if (node.is(TypeStringGrammar.TYPE_GENERIC_DEFINITION)) {
      this.buildGenericDefinition(node);
    } else if (node.is(TypeStringGrammar.TYPE_GENERIC_REFERENCE)) {
      this.buildGenericReference(node);
    } else if (node.is(TypeStringGrammar.TYPE_IDENTIFIER)) {
      this.buildIdentifier(node);
    } else if (node.is(TypeStringGrammar.TYPE_STRING)) {
      this.buildTypeString(node);
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
    final TypeString part = TypeString.SELF;

    this.mapping.put(node, part);
  }

  private void buildParameterRef(final AstNode node) {
    final List<AstNode> childAsts = node.getChildren();
    final AstNode identifierAst = childAsts.get(2);
    final String refStr = identifierAst.getTokenValue();
    final TypeString part = TypeString.ofParameterRef(refStr);

    this.mapping.put(node, part);
  }

  private void buildGenericDefinition(final AstNode node) {
    final AstNode identifierNode = node.getFirstChild(TypeStringGrammar.TYPE_IDENTIFIER);
    final String identifier = identifierNode.getTokenValue();
    final AstNode typeStringNode = node.getFirstChild(TypeStringGrammar.TYPE_STRING);
    final TypeString typeString = this.mapping.get(typeStringNode);
    final TypeString part = TypeString.ofGenericDefinition(identifier, typeString);

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
    final TypeString part = TypeString.ofIdentifier(str, this.currentPakkage, genericsArr);

    this.mapping.put(node, part);
  }

  private void buildTypeString(final AstNode node) {
    final List<AstNode> childNodes =
        node.getChildren(
            TypeStringGrammar.TYPE_UNDEFINED,
            TypeStringGrammar.TYPE_CLONE,
            TypeStringGrammar.TYPE_SELF,
            TypeStringGrammar.TYPE_PARAMETER_REFERENCE,
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
    final TypeString part =
        childNodes.size() == 1
            ? childTypeStrings.get(0)
            : TypeString.ofCombination(childTypeStringsArr);
    this.mapping.put(node, part);
  }
}
