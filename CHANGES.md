Changes
=======

0.5.1 (unreleased)

- More robustness for other LSP clients other than VScode


0.5.0 (2022-01-07)

- MessagePatchGrammar no longer needs a $ at the end of the message patch
- Strip org.sonarsource.sonarqube:sonar-plugin-api from magik-lint, shaving size
- Update MagikGrammar for easier processing
- Introduce AstWalker
- Add check NoStatementAfterBodyExit
- Show scope count/max scope count in ScopeCountCheck
- Add check VariableDeclarationUsageDistance
- Add magik-language-server, with
  - source indexing
  - type inferencing
  - hover provider
  - implementation provider
  - foldeing range provider
  - references provider
  - formatting provider
  - signature help provider
  - completion provider
- Add magik-debug-adapter
- Add check ImportMissingDefinition
- Add check NoSelfUse
- Remove --watch option from magik-lint
- Add check HidesVariable


0.4.0 (2020-01-29)

- Make MagikGrammar more consistent
- Add check EmptyBlock
- Dont check ##-comments in CommentedCodeCheck + handle SYNTAX\_ERRORS
- Refactor getting templated checks to CheckList.getTemplatedChecks()
- Add --debug option to magik-lint
- Fix ScopeBuilderVisitor for \_try without an identifier
- Refactorings in MagikLint
- Add check DuplicateMethodInFile
- Fix ScopeCountCheck, UndefinedVariableCheck always using global scope
- Add check UseValueCompare
- ScopeBuilderVisitor now also tracks usage
- Fix ScopeBuilderVisitor uncorrectly marking ScopeEntry as GLOBAL
- Fix locating magik-lint.properties in products
- Make parsing/checking files parallel in magik-lint
- Check Formatting now requires empty line after TRANSMIT
- Fix scoping for variables defined at \_for loops
- MethodDocCheck now needs Loopbody-section for \_iter methods
- Add check SwMethodDoc (disabled by default in linter)
- Fixes and improvements to MagikGrammar
- Improvements to AstCompare
- Add setting FormattingCheck.indent\_character (tab/space)
- Use annotation to mark MagikCheck as templated check


0.3.2 (2019-10-29)

- Add check ScopeCount
- Add check UndefinedVariable
- Fix LocalImportProcedureCheck not properly handling non-locals/definitions


0.3.1 (2019-09-22)

- Prevent CPD errors when SYNTAX\_ERROR token is too long
- Fix several Magik Grammar bugs


0.3.0 (2019-09-22)

- Add WarnedCallCheck
- Fix ScopeBuilderVisitor for \_try \_with identifier scoping
- Actually fail and give SYNTAX\_ERROR when input cannot be entirely parsed
- Add --max-infractions option to magik-lint
- FileNotInLoadListCheck trims lines from load\_list
- Fix bug where FormattingCheck does not handle augmented assignments properly
- Make AST more an AST, not including terminals
- Fix lines not matching due to \r\r\n
- Don't crash on usage of vec() in def\_slotted\_exemplar()
- Properly support \_primitive
- Move MLint-specific instruction handling to magik-lint + support mlint instructions per scope
- Unify file contents/line splitting


0.2.0 (2019-08-31)

- Add --watch option to magik-lint
- Add --help option to magik-lint
- Provide safe and unsafe versions of MagikParser.parse()
- Update README (@sebastiaanspeck)
- Fix error in FormattingCheck when trying to check SYNTAX\_ERROR tokens
- Let MagikVisitorContext handle Scope building
- Add ${tag} to MessageFormatReporter
- Changes to MagikGrammar
- Give FormattingCheck proper key
- Fix magik-lint --show-checks
- Fix memleak in UnusedVariableCheck
- Add MethodDocCheck
- Extend FormattingCheck to test if line starts with tabs
- Narrow Magik grammar to be more like SW43
- Fixes for Sonar


0.1.4 (2019-07-26)

- Fixes after reducing size of magik-lint jar
- Rename ParserErrorCheck to SyntaxErrorCheck
- Make LineLengthCheck a minor infraction and signal from nth column
- Better handle multiple disabled checks
- Add FormattingCheck
- Add ForbiddenCallCheck
- Make magik-lint output 0-base column numbers (removed)
- Add SimplifyIfCheck
- Add column-offset option to magik-lint
- Fix bug where rc-file options weren't properly read
- Fix bug in UnusedVariableCheck where assignment to import is improperly flagged


0.1.3 (2019-07-07)

- Fix output of Windows paths in magik-lint


0.1.2 (2019-07-06)

- Report parser errors through checks
- Enforce strict keyword matching in parser
- Fix ScopeBuilder not properly handling optional parameters
- Reduce size of magik-lint jar by removing dependencies


0.1.1 (2019-06-30)

- Add --untabify <n> option to magik-lint
- Add support for \_class keyword


0.1.0 (2019-06-19)

- Initial release
