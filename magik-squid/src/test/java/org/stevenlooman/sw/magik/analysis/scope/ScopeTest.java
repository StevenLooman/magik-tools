package org.stevenlooman.sw.magik.analysis.scope;

import static org.fest.assertions.Assertions.assertThat;

import org.junit.Test;

public class ScopeTest {

  @Test
  public void testGlobalScopeDeclaration() {
    Scope globalScope = new GlobalScope();
    globalScope.addDeclaration(ScopeEntry.Type.GLOBAL, "identifier", null, null);

    ScopeEntry entry = globalScope.getScopeEntry("identifier");
    assertThat(entry).isNotEqualTo(null);
  }

  @Test
  public void testProcedureScopeDeclaration() {
    Scope globalScope = new GlobalScope();
    Scope procedureScope = new ProcedureScope(globalScope, null);
    procedureScope.addDeclaration(ScopeEntry.Type.LOCAL, "identifier", null, null);

    ScopeEntry entry = procedureScope.getScopeEntry("identifier");
    assertThat(entry).isNotEqualTo(null);
  }

  @Test
  public void testBodyScopeDeclaration() {
    Scope globalScope = new GlobalScope();
    Scope bodyScope = new BodyScope(globalScope, null);
    bodyScope.addDeclaration(ScopeEntry.Type.LOCAL, "identifier", null, null);

    ScopeEntry entry = bodyScope.getScopeEntry("identifier");
    assertThat(entry).isNotEqualTo(null);
  }

  @Test
  public void testGlobalScopeDefinition() {
    Scope globalScope = new GlobalScope();
    globalScope.addDeclaration(ScopeEntry.Type.DEFINITION, "identifier", null, null);

    ScopeEntry entry = globalScope.getScopeEntry("identifier");
    assertThat(entry).isNotEqualTo(null);
  }

  @Test
  public void testProcedureScopeDefinition() {
    Scope globalScope = new GlobalScope();
    Scope procedureScope = new ProcedureScope(globalScope, null);
    procedureScope.addDeclaration(ScopeEntry.Type.DEFINITION, "identifier", null, null);

    ScopeEntry entry = procedureScope.getScopeEntry("identifier");
    assertThat(entry).isNotEqualTo(null);
  }

  @Test
  public void testBodyScopeDefinition() {
    Scope globalScope = new GlobalScope();
    Scope bodyScope = new BodyScope(globalScope, null);
    bodyScope.addDeclaration(ScopeEntry.Type.DEFINITION, "identifier", null, null);

    ScopeEntry entry = bodyScope.getScopeEntry("identifier");
    assertThat(entry).isNotEqualTo(null);
  }

  @Test
  public void testBodyBodyScopeDeclarationOuter() {
    Scope globalScope = new GlobalScope();
    Scope outerBodyScope = new BodyScope(globalScope, null);
    Scope innerBodyScopy = new BodyScope(outerBodyScope, null);

    outerBodyScope.addDeclaration(ScopeEntry.Type.LOCAL, "identifier", null, null);

    ScopeEntry entryFromOuter = outerBodyScope.getScopeEntry("identifier");
    assertThat(entryFromOuter).isNotEqualTo(null);

    ScopeEntry entryFromInner = innerBodyScopy.getScopeEntry("identifier");
    assertThat(entryFromInner).isNotEqualTo(null);
  }

  @Test
  public void testBodyBodyScopeDeclarationInner() {
    Scope globalScope = new GlobalScope();
    Scope outerBodyScope = new BodyScope(globalScope, null);
    Scope innerBodyScope = new BodyScope(outerBodyScope, null);

    innerBodyScope.addDeclaration(ScopeEntry.Type.LOCAL, "identifier", null, null);

    ScopeEntry entryFromOuter = outerBodyScope.getScopeEntry("identifier");
    assertThat(entryFromOuter).isEqualTo(null);

    ScopeEntry entry = innerBodyScope.getScopeEntry("identifier");
    assertThat(entry).isNotEqualTo(null);
  }

  @Test
  public void testBodyBodyScopeDefinitionFromOuter() {
    Scope globalScope = new GlobalScope();
    Scope outerBodyScope = new BodyScope(globalScope, null);
    Scope innerBodyScope = new BodyScope(outerBodyScope, null);

    // defined in outer scope
    outerBodyScope.addDeclaration(ScopeEntry.Type.DEFINITION, "identifier", null, null);

    ScopeEntry entryOuter = outerBodyScope.getScopeEntry("identifier");
    assertThat(entryOuter).isNotEqualTo(null);

    ScopeEntry entryInner = innerBodyScope.getScopeEntry("identifier");
    assertThat(entryInner).isNotEqualTo(null);
  }

  @Test
  public void testBodyBodyScopeDefinitionFromInner() {
    Scope globalScope = new GlobalScope();
    Scope outerBodyScope = new BodyScope(globalScope, null);
    Scope innerBodyScope = new BodyScope(outerBodyScope, null);

    // defined in inner scope
    innerBodyScope.addDeclaration(ScopeEntry.Type.DEFINITION, "identifier", null, null);

    ScopeEntry entryOuter = outerBodyScope.getScopeEntry("identifier");
    assertThat(entryOuter).isNotEqualTo(null);

    ScopeEntry entryInner = innerBodyScope.getScopeEntry("identifier");
    assertThat(entryInner).isNotEqualTo(null);
  }

  @Test
  public void testMethodProcedureScope() {
    Scope globalScope = new GlobalScope();
    Scope methodScope = new ProcedureScope(globalScope, null);
    Scope procedureScope = new ProcedureScope(methodScope, null);

    // defined in method scope
    methodScope.addDeclaration(ScopeEntry.Type.LOCAL, "identifier", null, null);

    ScopeEntry entryProcedureScope = procedureScope.getScopeEntry("identifier");
    assertThat(entryProcedureScope).isNull();
  }

  @Test
  public void testMethodProcedureScopeImport() {
    Scope globalScope = new GlobalScope();
    Scope methodScope = new ProcedureScope(globalScope, null);
    Scope procedureScope = new ProcedureScope(methodScope, null);

    // defined in method scope
    methodScope.addDeclaration(ScopeEntry.Type.LOCAL, "identifier", null, null);
    // imported in procedure scope
    procedureScope.addDeclaration(ScopeEntry.Type.IMPORT, "identifier", null, null);

    ScopeEntry entryProcedureScope = procedureScope.getScopeEntry("identifier");
    assertThat(entryProcedureScope).isNotNull();
  }

}
