# Changes

0.10.0 (unreleased)

- Move to Java 17.
- Add base functionality to apply fixes from magik-lint.
- Add UnsafeEvaluateInvocationCheck to test for unsafe `unsafe_evaluate()` method calls.
- Add SwChar16VectorEvaluateInvocationCheck to test for `sw:char16_vector.evaluate()` method calls.
- Fix setting `magik.typing.index*Usages` internally in `magik-language-server`.
- Add `do_not_translate` to SwModuleDefinitionGrammar and SwProductDefinitionGrammar.
- Add `required_by` to SwModuleDefinitionGrammar.
- Add FormattingFixer to `magik-lint --apply-fixes`.
- Store exemplar and method topics at ExemplarDefinition and MethodDefinitions.
- Parse exemplar and method topics.
- Add DeprecatedTypeUsageTypedCheck check to mark deprecated exemplars.
- Add DeprecatedMethodUsageTypedCheck check to mark deprecated methods.
- Semantic tokens now mark deprecated types and methods.
- Completion provider now marks deprecated types and methods.
- Handle directory renames properly in `magik-language-server`.
- Improve handling of product.def/module.def in `magik-language-server` in VSCode.
  - Semantic tokens.
  - Folding.
  - Hovering.
  - Definition.
  - References.
  - More robust grammar.
- Remove old typing classes and use the new typing classes.
- Fix providing Formatting CodeActions on all locations, instead of asked range.
- Fix hovering over atoms.
- Make `unused-variable.check-parameters` configurable.
- Support `indexed_class`/`enumerated_class`/`delete_class` from class_info.
- Make indent (tab/space) better configurable, by using configuration:
  - `magik.formatting.indentChar`
  - `magik.formatting.indentWidth`
  - `magik.formatting.insertFinalNewline`
  - `magik.formatting.trimTrailingWhitespace`
  - `magik.formatting.trimFinalNewlines`
- Typing inlay hint provide also provides inlayhints for method/procedure invocations.
- Rename setting `magik.typing.showAtomInlayHints` to `magik.typing.showTypingInlayHints`.
- Replace `magik.libsDirs` with `magik.productDirs`, register products instead of libs directories.
- Dump types database (per workspace folder) on shutdown, read it on start up and re-index any changed files, for quicker initialization.
- Transmit a product.def/module.def/load_list.txt with F4-b from VSCode.
- Fix VSCode UNC path handling on Windows.
- Fix TodoComment check checking whole word.
- Also search references in procedures.
- Add `remex` and `remove_exemplar` to the list of default warned calls.
- Several fixes.

0.9.1 (2024-03-13)

- Fix actually not indexing large files in magik-language-server.
- Fix `sonar-magik-plugin` not setting property `sonar.lang.patterns.magik`.
- Fix printing order of issues in `magik-lint`.
- Fix `module.def` parser not parsing partial test entries.
- Fix `sonar-magik-plugin` crashing on syntax error token during Copy/Paste Detection-phase.
- Fix reasoning errors where types were regarded as combined, when it was actually singular.

0.9.0 (2024-02-26)

- Support configurable libs dirs.
- Read and register in which module a definition lives.
- Use com.google.gson instead of org.json.
- Refactor MagikIndexer/JsonTypeKeeperReader/JsonTypeKeeperWriter/ClassInfoTypeKeeperReader to share more functionality, via *Definition classes.
- Extend FormattingCheck to require at most 2 successive empty lines.
- Use DefinitionKeeper to store *Definition classes.
- Add DefinitionKeeperTypeKeeperAdapter for compatibility with previous type system.
- Show inlayhints for ATOM nodes, as a configurable option.
- Refactor LocalTypeReasoner state to own class.
- Improve handling of/reasoning with generics.
- Index products and modules.
- Add conditional expression reasoning, where an expression might limit the type of the variable(s).
- Add TodoComment check.
- Fixes to checks metadata.
- Refactoring/renaming of typed checks.
- Make indexing usages toggable via `magik.typing.indexUsages`.
- Add ConditionalExpressionIsFalseTypedCheck to test if a conditional expression results in a `sw:false` type.
- Add ModuleRequiredForGlobalTypedCheck to test if the source module is required, when a global is used.
- Minor fixes.

0.8.3 (2024-01-05)

- Fix error finding start/end line/column for Scopes, when encountering an empty block.
- Fix error when determining issue is disabled via MagikIssueDisabledChecker in certain cases.

0.8.2 (2023-11-14)

- Fix not finding appropriate node to register issue on when method contains a syntax error.
- `magik-lint.properties` is searched for from path of current file in magik-lint, unless `--rcfile` is used.
- Paths specified in setting `ignore` in `magik-lint.properties` in magik-lint are respected.
- Fix grammar not supporting end labels in `_loop`/`_endloop` constructs.
- Fix reading mlint-instructions in scope.
- Fix WarnedCallCheck default warned calls not seeing the `sw:`-prefixed versions.

0.8.1 (2023-10-15)

- Better handle syntax errors in Copy/Paste Detection step in sonar-magik-plugin.
- Fix sslr-magik-toolkit pointing to wrong Main class.
- Add datamodel_type_dumper.magik.
- Fix ForbiddenCallCheck default forbidden calls not seeing the `sw:`-prefixed versions.
- Fix LocalTypeReasoner error on assignment parameter.
- Fix source name in language server diagnostics.
- Fix language server not showing typed Magik diagnostics.
- Fix misnamed class TypeDocTypeExistsTypeCheck --> TypeDocTypeExistsTypedCheck.
- Fix to allow comments after tokens in FormattingCheck.
- Fix default/example to also include package-prefix global in ForbiddenGlobalUsageCheck.

0.8.0 (2023-09-27)

- Fix SwMethodDocCheck not properly matching uppercased parameter in method doc when it is followed by a non-whitespace character.
- Fix UnusedVariableCheck not properly handling variables of a for loop.
- Refactor JsonTypeKeeperReader/JsonTypeKeeperWriter to use defined instructions.
- Fixes/changes/refactoring for Sonar.
- Upgrade dependencies.
- Don't store actual type, but the reference at Parameter.
- Prevent traceback when reading .jar files in ClassInfoTypeKeeperReader.
- Fix LineLengthCheck not properly reporting line lengths in user message.
- Find and use magik-lint.properties based on file path, instead of just once, in magik-language-server.
- Add --enabled=... and --disabled=... to magik-lint; use --disabled=all with --enable=... to enable specific checks.
- Add type hierarchy provider to magik-language-server.
- Add inlay hint provider to magik-language-server.
- Add code actions for @parameter and @return type-doc parts.
- Code action providers for MagikChecks/MagikTypedChecks are only active if check is enabled in configuration.
- MethodReturnMatchesDocCheck points to the actual type-doc part.
- Checks now mark the complete violating part, instead of the first token.
- Show source-check for magik-typed checks in magik-language-server.
- HidesVariableCheck now allows for variable definition in ancestor scope, when defined at lower line.
- Add ForbiddenInheritanceCheck.
- TrailingWhitespaceCheck now marks the actual whitespace.
- Add defintion functionality to magik-language-server.
- Move functionality from implementation provider to definition provider in magik-language-server.
- Implementation provider now provides impementations of abstract methods.
- Add SelectionRangeProvider to magik-language-server.
- Drop templated check support, including checks CommentRegularExpressionCheck and XPathCheck.
- Fix CommentedCodeCheck matching too many things as Magik code.
- Drop `--untabify` option from magik-lint.
- Various small fixes.

0.7.1 (2023-02-21)

- Fix VariableDeclarationUsageDistance not seeing method invocations as usage
- Fix TypeDoc ruleSpecification/sqKey having old name (NewDoc)
- Fix bug during re-reading the types database using JsonTypeKeeperReader, where a duplicate type would cause an error
- Report version and settings, if applicable

0.7.0 (2023-02-09)

- Fix SwMethodDocCheck accepting `##` as a comment.
- Fix SimplifyIfCheck to not report invalid reduction.
- Also show slots on type hover.
- Properly scope package-prefixed globals.
- Add check ForbiddenGlobalUsage
- Add check MethodLineCount
- Add check VariableCount
- Fix VariableCountCheck better handling syntax errors
- Add check FileMethodCount
- Rename check NewDoc to TypeDoc.
- Add generics to the type system, which allows collections to be parameterized.
- Fix SimplifyIfCheck better handling syntax errors

0.6.0 (2022-09-12)

- Drop UnaryOperator, replaced by the proper method calls
- Minor refactoring of CLI options
- Improve MagikIndexer to determine whether a method really returns something and setting the resulting type of the method accordingly (undefined result when no new-method-doc is available, or an empty result)
- Don't overwrite already known methods in JsonTypeKeeperReader
- Add JsonTypeKeeperWriter
- Support methods returning a parameter with the `_parameter(..)` type/ParameterReferenceType
- Fix MagikGrammer better support EOLs in certain cases
- Fix showing procedure doc on hover
- Extend hover provider, now supports packages, conditions
- Support conditions
- Changes to MagikGrammar
- Rewrite parts of references to types in TypeKeeper/types. Fixes mem leaks, references to invalid/old types. MagikPreIndexer can now also be removed and methods without a type definition can be indexed
- Methods support recording used globals, called methods, used slots, used conditions. This allows for finding references and possibly method renaming in the future
- Various bug fixes
- Various new features

0.5.4 (2022-11-07)

- Remove MethodDoc check
- Add ParameterCount check
- Update Sonar way profile

0.5.3 (2022-11-04)

- Fix bug in sonar plugin where all issues were applied to every file

0.5.2 (2022-05-26)

- Fix bug in ScopeBuilderVisitor where import in top level procedure caused a NPE
- Fix bug in magik-lint --rcfile, where java.io.File is expected but was java.io.FileInputStream
- Fix bug where grammar accepted invalid _package identifiers, causing problems later on
- Fix bug in DefSlottedExemplarParser: also see `sw:def_slotted_exemplar`
- Minor clean up in `ThreadManager`
- Add setting `magik.lint.overrideConfigFile` to override properties file for linter from Language Server
- Fix bug in HoverProvider where no hover was provided for assigned variable

0.5.1 (2022-01-17)

- More robustness for other LSP clients other than VScode
- Fix binary operator handling not recognizing magik keywords
- Disable check no-self-use by default
- Disable check method-doc by default, in favor of new-doc check
- Fix bug in JSON type database reader: lines with // are now regarded as comment-lines
- Fix bug causing magik-lint.jar --show-checks to not work

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
  - completion provider
  - document symbol provider
  - folding range provider
  - formatting provider
  - formatting provider
  - hover provider
  - implementation provider
  - references provider
  - rename provider
  - signature help provider
  - somenatic token provider
  - symbol provider
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

- Add --untabify \<n\> option to magik-lint
- Add support for \_class keyword

0.1.0 (2019-06-19)

- Initial release
