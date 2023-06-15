package nl.ramsolutions.sw.magik.analysis.scope;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test Scope.
 */
class ScopeTest {

    @Test
    void testGlobalScopeDeclaration() {
        final Scope globalScope = new GlobalScope();
        globalScope.addDeclaration(ScopeEntry.Type.GLOBAL, "identifier", null, null);

        final ScopeEntry entry = globalScope.getScopeEntry("identifier");
        assertThat(entry).isNotNull();
    }

    @Test
    void testProcedureScopeDeclaration() {
        final Scope globalScope = new GlobalScope();
        final Scope procedureScope = new ProcedureScope(globalScope, null);
        procedureScope.addDeclaration(ScopeEntry.Type.LOCAL, "identifier", null, null);

        final ScopeEntry entry = procedureScope.getScopeEntry("identifier");
        assertThat(entry).isNotNull();
    }

    @Test
    void testBodyScopeDeclaration() {
        final Scope globalScope = new GlobalScope();
        final Scope bodyScope = new BodyScope(globalScope, null);
        bodyScope.addDeclaration(ScopeEntry.Type.LOCAL, "identifier", null, null);

        final ScopeEntry entry = bodyScope.getScopeEntry("identifier");
        assertThat(entry).isNotNull();
    }

    @Test
    void testGlobalScopeDefinition() {
        final Scope globalScope = new GlobalScope();
        globalScope.addDeclaration(ScopeEntry.Type.DEFINITION, "identifier", null, null);

        final ScopeEntry entry = globalScope.getScopeEntry("identifier");
        assertThat(entry).isNotNull();
    }

    @Test
    void testProcedureScopeDefinition() {
        final Scope globalScope = new GlobalScope();
        final Scope procedureScope = new ProcedureScope(globalScope, null);
        procedureScope.addDeclaration(ScopeEntry.Type.DEFINITION, "identifier", null, null);

        final ScopeEntry entry = procedureScope.getScopeEntry("identifier");
        assertThat(entry).isNotNull();
    }

    @Test
    void testBodyScopeDefinition() {
        final Scope globalScope = new GlobalScope();
        final Scope bodyScope = new BodyScope(globalScope, null);
        bodyScope.addDeclaration(ScopeEntry.Type.DEFINITION, "identifier", null, null);

        final ScopeEntry entry = bodyScope.getScopeEntry("identifier");
        assertThat(entry).isNotNull();
    }

    @Test
    void testBodyBodyScopeDeclarationOuter() {
        final Scope globalScope = new GlobalScope();
        final Scope outerBodyScope = new BodyScope(globalScope, null);
        final Scope innerBodyScopy = new BodyScope(outerBodyScope, null);

        outerBodyScope.addDeclaration(ScopeEntry.Type.LOCAL, "identifier", null, null);

        final ScopeEntry entryFromOuter = outerBodyScope.getScopeEntry("identifier");
        assertThat(entryFromOuter).isNotNull();

        final ScopeEntry entryFromInner = innerBodyScopy.getScopeEntry("identifier");
        assertThat(entryFromInner).isNotNull();
    }

    @Test
    void testBodyBodyScopeDeclarationInner() {
        final Scope globalScope = new GlobalScope();
        final Scope outerBodyScope = new BodyScope(globalScope, null);
        final Scope innerBodyScope = new BodyScope(outerBodyScope, null);

        innerBodyScope.addDeclaration(ScopeEntry.Type.LOCAL, "identifier", null, null);

        final ScopeEntry entryFromOuter = outerBodyScope.getScopeEntry("identifier");
        assertThat(entryFromOuter).isNull();

        final ScopeEntry entry = innerBodyScope.getScopeEntry("identifier");
        assertThat(entry).isNotNull();
    }

    @Test
    void testBodyBodyScopeDefinitionFromOuter() {
        final Scope globalScope = new GlobalScope();
        final Scope outerBodyScope = new BodyScope(globalScope, null);
        final Scope innerBodyScope = new BodyScope(outerBodyScope, null);

        // defined in outer scope
        outerBodyScope.addDeclaration(ScopeEntry.Type.DEFINITION, "identifier", null, null);

        final ScopeEntry entryOuter = outerBodyScope.getScopeEntry("identifier");
        assertThat(entryOuter).isNotNull();

        final ScopeEntry entryInner = innerBodyScope.getScopeEntry("identifier");
        assertThat(entryInner).isNotNull();
    }

    @Test
    void testBodyBodyScopeDefinitionFromInner() {
        final Scope globalScope = new GlobalScope();
        final Scope outerBodyScope = new BodyScope(globalScope, null);
        final Scope innerBodyScope = new BodyScope(outerBodyScope, null);

        // defined in inner scope
        innerBodyScope.addDeclaration(ScopeEntry.Type.DEFINITION, "identifier", null, null);

        final ScopeEntry entryOuter = outerBodyScope.getScopeEntry("identifier");
        assertThat(entryOuter).isNotNull();

        final ScopeEntry entryInner = innerBodyScope.getScopeEntry("identifier");
        assertThat(entryInner).isNotNull();
    }

    @Test
    void testMethodProcedureScope() {
        final Scope globalScope = new GlobalScope();
        final Scope methodScope = new ProcedureScope(globalScope, null);
        final Scope procedureScope = new ProcedureScope(methodScope, null);

        // defined in method scope
        methodScope.addDeclaration(ScopeEntry.Type.LOCAL, "identifier", null, null);

        final ScopeEntry entryProcedureScope = procedureScope.getScopeEntry("identifier");
        assertThat(entryProcedureScope).isNull();
    }

    @Test
    void testMethodProcedureScopeImport() {
        final Scope globalScope = new GlobalScope();
        final Scope methodScope = new ProcedureScope(globalScope, null);
        final Scope procedureScope = new ProcedureScope(methodScope, null);

        // defined in method scope
        methodScope.addDeclaration(ScopeEntry.Type.LOCAL, "identifier", null, null);
        // imported in procedure scope
        procedureScope.addDeclaration(ScopeEntry.Type.IMPORT, "identifier", null, null);

        final ScopeEntry entryProcedureScope = procedureScope.getScopeEntry("identifier");
        assertThat(entryProcedureScope).isNotNull();
    }

}
