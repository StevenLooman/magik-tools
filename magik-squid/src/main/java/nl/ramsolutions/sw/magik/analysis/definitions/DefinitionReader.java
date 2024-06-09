package nl.ramsolutions.sw.magik.analysis.definitions;

import com.sonar.sslr.api.AstNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.analysis.MagikAstWalker;
import nl.ramsolutions.sw.magik.analysis.definitions.parsers.DefConditionParser;
import nl.ramsolutions.sw.magik.analysis.definitions.parsers.DefEnumerationParser;
import nl.ramsolutions.sw.magik.analysis.definitions.parsers.DefIndexedExemplarParser;
import nl.ramsolutions.sw.magik.analysis.definitions.parsers.DefMixinParser;
import nl.ramsolutions.sw.magik.analysis.definitions.parsers.DefPackageParser;
import nl.ramsolutions.sw.magik.analysis.definitions.parsers.DefSlottedExemplarParser;
import nl.ramsolutions.sw.magik.analysis.definitions.parsers.DefineBinaryOperatorCaseParser;
import nl.ramsolutions.sw.magik.analysis.definitions.parsers.DefineSharedConstantParser;
import nl.ramsolutions.sw.magik.analysis.definitions.parsers.DefineSharedVariableParser;
import nl.ramsolutions.sw.magik.analysis.definitions.parsers.DefineSlotAccessParser;
import nl.ramsolutions.sw.magik.analysis.definitions.parsers.GlobalDefinitionParser;
import nl.ramsolutions.sw.magik.analysis.definitions.parsers.MethodDefinitionParser;
import nl.ramsolutions.sw.magik.analysis.definitions.parsers.ProcedureDefinitionParser;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/**
 * Class to easily read/parse AST for. Supported constructs: - def_package (only takes symbols for
 * name) - def_enumeration - def_slotted_exemplar (only takes symbols for name + slot names) -
 * def_indexed_exemplar - def_mixin - method definition - define_slot_access -
 * define_shared_variable - define_shared_constant - define_binary_operator_case
 */
public class DefinitionReader extends MagikAstWalker {

  private final MagikFile magikFile;
  private final List<MagikDefinition> definitions = new ArrayList<>();

  public DefinitionReader(final MagikFile magikFile) {
    this.magikFile = magikFile;
  }

  public List<MagikDefinition> getDefinitions() {
    return Collections.unmodifiableList(this.definitions);
  }

  @Override
  protected void walkPostMethodDefinition(final AstNode node) {
    final MethodDefinitionParser parser = new MethodDefinitionParser(this.magikFile, node);
    final List<MagikDefinition> parsedDefinitions = parser.parseDefinitions();
    this.definitions.addAll(parsedDefinitions);
  }

  @Override
  protected void walkPostVariableDefinitionStatement(final AstNode node) {
    if (GlobalDefinitionParser.isGlobalDefinition(node)) {
      this.handleGlobalDefinition(node);
    }
  }

  private void handleGlobalDefinition(final AstNode node) {
    final GlobalDefinitionParser parser = new GlobalDefinitionParser(this.magikFile, node);
    final List<MagikDefinition> parsedDefinitions = parser.parseDefinitions();
    this.definitions.addAll(parsedDefinitions);
  }

  @Override
  protected void walkPostProcedureInvocation(final AstNode node) {
    if (DefPackageParser.isDefPackage(node)) {
      this.handleDefPackage(node);
    } else if (DefEnumerationParser.isDefEnumeration(node)) {
      this.handleDefEnumeration(node);
    } else if (DefSlottedExemplarParser.isDefSlottedExemplar(node)) {
      this.handleDefSlottedExemplar(node);
    } else if (DefIndexedExemplarParser.isDefIndexedExemplar(node)) {
      this.handleDefIndexedExemplar(node);
    } else if (DefMixinParser.isDefMixin(node)) {
      this.handleDefMixin(node);
    } else if (DefineBinaryOperatorCaseParser.isBinaryOperatorCase(node)) {
      this.handleDefineBinaryOperatorCase(node);
    }
  }

  @Override
  protected void walkPostMethodInvocation(final AstNode node) {
    final AstNode statementNode = node.getFirstAncestor(MagikGrammar.STATEMENT);
    if (statementNode != null
        && statementNode.getParent() != null
        && statementNode.getParent().is(MagikGrammar.MAGIK)) {
      // Some more sanity: Directly under top level.
      if (DefineSlotAccessParser.isDefineSlotAccess(node)
          || DefineSlotAccessParser.isDefineSlotExternallyReadable(node)
          || DefineSlotAccessParser.isDefineSlotExternallyWritable(node)) {
        this.handleDefineSlotAccess(node);
      } else if (DefineSharedVariableParser.isDefineSharedVariable(node)) {
        this.handleDefineSharedVariable(node);
      } else if (DefineSharedConstantParser.isDefineSharedConstant(node)) {
        this.handleDefineSharedConstant(node);
      }
    }

    // Anything goes.
    if (DefConditionParser.isDefineCondition(node)) {
      this.handleDefineCondition(node);
    }
  }

  @Override
  protected void walkPostProcedureDefinition(final AstNode node) {
    if (ProcedureDefinitionParser.isProcedureDefinition(node)) {
      this.handleDefineProcedure(node);
    }
  }

  private void handleDefineCondition(final AstNode node) {
    final DefConditionParser parser = new DefConditionParser(this.magikFile, node);
    final List<MagikDefinition> parsedDefinitions = parser.parseDefinitions();
    this.definitions.addAll(parsedDefinitions);
  }

  private void handleDefPackage(final AstNode node) {
    final DefPackageParser parser = new DefPackageParser(this.magikFile, node);
    final List<MagikDefinition> parsedDefinitions = parser.parseDefinitions();
    this.definitions.addAll(parsedDefinitions);
  }

  private void handleDefEnumeration(final AstNode node) {
    final DefEnumerationParser parser = new DefEnumerationParser(this.magikFile, node);
    final List<MagikDefinition> parsedDefinitions = parser.parseDefinitions();
    this.definitions.addAll(parsedDefinitions);
  }

  private void handleDefSlottedExemplar(final AstNode node) {
    final DefSlottedExemplarParser parser = new DefSlottedExemplarParser(this.magikFile, node);
    final List<MagikDefinition> parsedDefinitions = parser.parseDefinitions();
    this.definitions.addAll(parsedDefinitions);
  }

  private void handleDefIndexedExemplar(final AstNode node) {
    final DefIndexedExemplarParser parser = new DefIndexedExemplarParser(this.magikFile, node);
    final List<MagikDefinition> parsedDefinitions = parser.parseDefinitions();
    this.definitions.addAll(parsedDefinitions);
  }

  private void handleDefMixin(final AstNode node) {
    final DefMixinParser parser = new DefMixinParser(this.magikFile, node);
    final List<MagikDefinition> parsedDefinitions = parser.parseDefinitions();
    this.definitions.addAll(parsedDefinitions);
  }

  private void handleDefineBinaryOperatorCase(final AstNode node) {
    final DefineBinaryOperatorCaseParser parser =
        new DefineBinaryOperatorCaseParser(this.magikFile, node);
    final List<MagikDefinition> parsedDefinitions = parser.parseDefinitions();
    this.definitions.addAll(parsedDefinitions);
  }

  private void handleDefineSlotAccess(final AstNode node) {
    final DefineSlotAccessParser parser = new DefineSlotAccessParser(this.magikFile, node);
    final List<MagikDefinition> parsedDefinitions = parser.parseDefinitions();
    this.definitions.addAll(parsedDefinitions);
  }

  private void handleDefineSharedVariable(final AstNode node) {
    final DefineSharedVariableParser parser = new DefineSharedVariableParser(this.magikFile, node);
    final List<MagikDefinition> parsedDefinitions = parser.parseDefinitions();
    this.definitions.addAll(parsedDefinitions);
  }

  private void handleDefineSharedConstant(final AstNode node) {
    final DefineSharedConstantParser parser = new DefineSharedConstantParser(this.magikFile, node);
    final List<MagikDefinition> parsedDefinitions = parser.parseDefinitions();
    this.definitions.addAll(parsedDefinitions);
  }

  private void handleDefineProcedure(final AstNode node) {
    final ProcedureDefinitionParser parser = new ProcedureDefinitionParser(this.magikFile, node);
    final List<MagikDefinition> parsedDefinitions = parser.parseDefinitions();
    this.definitions.addAll(parsedDefinitions);
  }
}
