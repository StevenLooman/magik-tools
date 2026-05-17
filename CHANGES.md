## [0.12.0](https://github.com/StevenLooman/magik-tools/tree/0.12.0) - 2026-05-17

### Features

- Add delayed running of checks after last update, or directly on save.

  Setting `magik.lint.runChecksOnChangeDelay` to any value greater than 0 specifies
  that the checks run after the given delay, in milliseconds. If there is a run-checks-task
  pending from the last change (i.e., you have typed another character), then that
  task is cancelled and another delayed task is scheduled. This prevents running
  checks unnecessarily on an old version of the file.

  Setting `magik.lint.runChecksOnSave` can be used to specify that checks should
  run when the file is saved. Set this to `true`, together with a large value
  of `magik.lint.runChecksOnChangeDelay`, to only run the checks when the file
  is saved. ([#335](https://github.com/StevenLooman/magik-tools/issues/335))
- Only re-index workspace when analysis settings are changed.

  This includes these settings:

  * `magik.typing.indexGlobalUsages`
  * `magik.typing.indexMethodUsages`
  * `magik.typing.indexSlotUsages`
  * `magik.typing.indexConditionUsages`

  ([#337](https://github.com/StevenLooman/magik-tools/issues/337))
- `product.def` and `module.def` are now checked, like magik files. Issues will be
  raised dependent on the quality of these files on your codebase and your configuration.

  magik-lint, magik-language-server, and SonarQube plugin all support checking
  `product.def` and `module.def` files.

  Note that in SonarQube, both `product.def` and `module.def` are considered to be
  of the same language as SonarQube differentiates languages based on the file extension.
  Therefore, the profiles for both these languages are merged into one profile.

  The following checks have been added for `product.def` files:

  - `ProductDefMissingDescription`/`product-def-missing-description`
  - `ProductDefMissingTitle`/`product-def-missing-title`
  - `ProductDefNameDoesNotMatchDirectoryNameCheck`/`product-def-name-does-not-match-directory-name`
  - `ProductDefSyntaxError`/`product-def-syntax-error`

  The following checks have been added for `module.def` files:

  - `ModuleDefMissingDescription`/`module-def-missing-description`
  - `ModuleDefNameDoesNotMatchDirectoryName`/`module-def-name-does-not-match-directory-name`
  - `ModuleDefSyntaxError`/`module-def-syntax-error`

  ## Breaking changes

  This change includes a major refactoring, such as moving classes to different
  namespaces, and rebuilding support classes. Any code dependent on this code will
  have to follow these changes.

  Furthermore, `product.def` and `module.def` files are now checked as well. This
  might result in new issues in your codebase.

  ([#362](https://github.com/StevenLooman/magik-tools/issues/362))
- Renamed language identifiers:

  * `product.def` --> `sw-product-def`
  * `module.def` --> `sw-module-def`
  * `load_list.txt` --> `sw-load-list`

  ## Breaking changes

  This might cause a breaking change for any library using the language identifiers
  for `product.def`, `module.def`, and `load_list.txt` files.

  ([#366](https://github.com/StevenLooman/magik-tools/issues/366))
- Add a `module.def` check to verify there are no overlapping required modules
  in `tests_modules` and `requires`.

  The following check has been added for `module.def` files:

  - `ModuleDefRequiredModuleAlreadyInTestsModules`/`module-def-required-module-already-in-tests-modules`.

  The check is activated for SonarQube.

  ([#370](https://github.com/StevenLooman/magik-tools/issues/370))
- Add name of the check as code to the Diagnostic to provide more information.

  This information is used for example
  in Emacs for Flycheck when listing all errors in a file. ([#372](https://github.com/StevenLooman/magik-tools/issues/372))
- Add fixer for `UseValueCompare` check.

  This fixer automatically replaces `_is`/`_isnt` by `=`/`<>` when check
  `UseValueCompare` detects an comparison of strings/bignums/floats. ([#374](https://github.com/StevenLooman/magik-tools/issues/374))
- Add support for `sw-product-def` and `sw-module-def` grammars in sslr-magik-toolkit.

  Via the command line option `-g`/`--grammar` one can specify the wanted grammar.
  For example:

  ```shell
  $ java -jar sslr-magik-toolkit.jar --grammar sw-module-def
  ...
  ``` ([#378](https://github.com/StevenLooman/magik-tools/issues/378))
- Add the MagikTypedChecks, ModuleDefChecks and ProductDefChecks to the Wiki.

  Also improved the Markdown itself to have a better index if someone is searching
  for a specific check (based on the key). ([#379](https://github.com/StevenLooman/magik-tools/issues/379))
- The following checks have been added for `magik` files:

  - `PragmaDoesNotIncludeModuleNameInTopics`/`pragma-does-not-include-module-name-in-topics`
  - `PragmaDoesNotIncludeProductNameInTopics`/`pragma-does-not-include-product-name-in-topics`
  - `PragmaDoesNotIncludeTopicInTopics`/`pragma-does-not-include-topic-in-topics`

  The checks are disabled by default.
  After enabling `pragma-does-not-include-topic-in-topics`,
  you need to supply the list of topics.

  ([#384](https://github.com/StevenLooman/magik-tools/issues/384))
- Format label of proc as `_proc @label() _endproc`.

  Previously, labels for procedures were formatted as `_proc@label() _endproc`.
  This change makes it consistent with other labels. ([#409](https://github.com/StevenLooman/magik-tools/issues/409))
- `load_list.txt` and `pach_list.txt` are now checked, like magik files. Issues
  will be raised dependent on the quality of these files on your codebase and your
  configuration.

  magik-lint, magik-language-server, and SonarQube plugin all support checking
  `load_list.txt` and `patch_list.txt` files. The sslr-magik-toolkit supports the
  new `sw-load-list` grammar as well.

  The following checks have been added for `load_list.txt`/`patch_list.txt` files:

  * `LoadListEntryExists`/`load-list-entry-exists`
  * `LoadListSyntaxError`/`load-list-syntax-error`

  ## Breaking changes

  `load_list.txt` and `patch_list.txt` files are now checked as well. This
  might result in new issues in your codebase.

  ([#413](https://github.com/StevenLooman/magik-tools/issues/413))
- Add the LoadListChecks to the Wiki.

  Also removed duplicated information from the HTML itself. ([#414](https://github.com/StevenLooman/magik-tools/issues/414))
- Add `@invokes_method` instruction to TypeDoc.

  Use this for the cases where, for example, `object.perform()` is used, to record
  uses of methods which cannot be derived automatically. Example:

  ```magik
  _method object.example()
      ## Example method.
      _local thing << do_something()
      _local method_name << :|example_method()|
      ## @invokes_method {user:other_object.example_method()}
      _local result << thing.perform(method_name)
  _endmethod
  $
  ``` ([#417](https://github.com/StevenLooman/magik-tools/issues/417))
- Better handle tests_modules, regard these as required modules. ([#419](https://github.com/StevenLooman/magik-tools/issues/419))
- Better handle re-indexing workspaces. Only clear existing type information
  when needed. ([#420](https://github.com/StevenLooman/magik-tools/issues/420))
- Support getting references to slots.

  This includes generated slot accessor methods. ([#440](https://github.com/StevenLooman/magik-tools/issues/440))
- Implement Myers diff algorithm to provide better suggested text edits. ([#442](https://github.com/StevenLooman/magik-tools/issues/442))
- Rewrite RelativeIndentWalker

  RelativeIndentWalker is rewritten to make it more understandable.

  A change in the indenting of a simple_vector being assigned to a variable
  is also introduced, to make it more consistent with other indenting rules:

  ```magik
  # Indenting simple_vector from assignment, was:
  _local a << {
  		    10
  	    }

  # Becomes:
  _local a << {
  	10
  }
  ``` ([#457](https://github.com/StevenLooman/magik-tools/issues/457))
- Add check SystemCommandUseSimpleVector.

  This checks enforces that a `simple_vector` is used for
  `sw:system.do_command()` and friends. ([#458](https://github.com/StevenLooman/magik-tools/issues/458))
- Improve indenting strategy, add visual and block indenters.

  Refactor the indenter to make it rule based.

  Split the indenter into two strategies, whrer the visual indenter
  derived from the previous relative indenter. ([#493](https://github.com/StevenLooman/magik-tools/issues/493))
- Improve symbol lookup speed by doing only simple contains matching. ([#494](https://github.com/StevenLooman/magik-tools/issues/494))
- Make use of magik-session-wrapper configurable per task.

  Set `useSessionWrapper` to `false` to not start task (session) using
  the magik-session-wrapper. ([#495](https://github.com/StevenLooman/magik-tools/issues/495))
- Improve loop result gathering.

  Now the result of the iterator method (i.e. `_return`/`>>` ...) is handled
  properly in the `_over`-loop.

  Also refactor reasoner to let statement handlers store state on own node,
  collect in parents. ([#496](https://github.com/StevenLooman/magik-tools/issues/496))
- Allow parsing of a TypeString with mixed generic definitions and references.

  Also add a check to verify the mixing is intended. ([#497](https://github.com/StevenLooman/magik-tools/issues/497))
- Add and align `--version/--help` options to all applicable modules. ([#498](https://github.com/StevenLooman/magik-tools/issues/498))
- Add "Extract to method/local procedure/local variable" code actions.

  Selecting one or more statements, or a sub-expression, inside a `_method`
  body and invoking the code action replaces the selection with a call and
  inserts the extracted code below. Three variants are offered:

  - **Extract to method** — inserts `_self.extracted_method(...)` and a new
    `_private _method` below the current method.
  - **Extract to local procedure** — inserts `extracted_proc(...)` and a
    `_local extracted_proc << _proc(...)` immediately before the selection,
    keeping the helper local to the current method.
  - **Extract to local variable** — inserts a `_local extracted_variable << <expression>`
    immediately before the enclosing statement and replaces the selected expression
    with the variable reference.

  Parameters and return values are inferred automatically. If the enclosing
  method carries a `_pragma`, a copy is added to the extracted method with
  `classify_level` downgraded to `restricted`.

  **Limitations:**

  - Statements containing `_return`, `_throw`, or `>>` are rejected,
    as these would alter the control flow of the enclosing method.
  - A `_leave` or `_continue` is only allowed when the entire enclosing
    `_loop … _endloop` is included in the selection. Selecting only the body
    of a loop (without the `_loop`/`_endloop` keywords) is rejected because
    the extracted method would contain an orphaned `_leave`.

  ([#500](https://github.com/StevenLooman/magik-tools/issues/500))
- Add several new LSP features to the language server.

  **On-type formatting** (`textDocument/onTypeFormatting`) automatically
  applies formatting edits as you type:

  - Triggered by `\n` (newline) — indents the new blank line based on the keyword
    that ended the previous line (`_then`, `_loop`, `_block`, `_protect`,
    `_protection`, `_try`, `_when`, `_catch`, etc.), using the configured indent
    character and width. When the file is syntactically valid, spacing on the
    previous line is corrected.
  - Triggered by `$` (Magik statement separator) — formats the current line only.

  **Diagnostic pull model** (`textDocument/diagnostic`) allows clients to request
  diagnostics on demand rather than waiting for the server to push them. The
  server always returns a full `RelatedFullDocumentDiagnosticReport`. The push
  model (`publishDiagnostics`) is suppressed when the client advertises
  pull-diagnostic support; it continues to run for older clients that do not.

  **Execute command** (`workspace/executeCommand`) exposes standard LSP command
  dispatch with two commands:

  - `magik.reIndex` — triggers a full workspace reindex, replacing the former `custom/reIndex`.
  - `magik.munit.getTestItems` — returns the full MUnit test-item tree,
    replacing the former `custom/munit/getTestItems` JSON-RPC endpoint
    (breaking change for clients that called it directly).

  **Document highlight** (`textDocument/documentHighlight`) highlights all
  occurrences of the symbol under the cursor in the current file. Local
  variables, parameters, and constants are distinguished as Write (definition
  site) or Read (usage site) highlights; method names, slot accesses, and
  globals use the Text kind.

  **Document link** (`textDocument/documentLink`) turns each file path entry
  in a load-list file into a clickable link that opens the corresponding
  `.magik` source file directly in the editor.

  **Code lens** (`textDocument/codeLens`) shows inline "Run all tests in …"
  and "Run test" actions above MUnit test exemplars and test methods
  respectively. Clicking a lens triggers the `magik.munit.runTest` command.
  Product definition files show an "Add product …" action that runs
  `smallworld_product.add_product()` in the active session, and module
  definition files show a "Load module …" action that runs
  `sw_module_manager.load_module()`. Code lens positions are kept in sync
  with editor and external file changes via definition re-indexing on
  `didChange` and `workspace/codeLens/refresh` notifications.

  **Linked editing range** (`textDocument/linkedEditingRange`) lets editors
  rename a local variable at all its occurrences simultaneously. Applies to
  local variables, parameters, and constants; global, dynamic, and imported
  names are excluded.

  **Slot go-to-definition** — go-to-definition now resolves slot access
  expressions (`.slot_name`) to the slot declaration in the exemplar
  definition, in addition to the existing method and variable targets.

  ([#523](https://github.com/StevenLooman/magik-tools/issues/523))
- Add typed-analysis coverage and call-hierarchy improvements across checks,
  resolver, and language-server support.

  **Typed checks** (`magik-checks`) add several new rule implementations and documentation:

  - `AbstractMethodNotImplemented` detects concrete exemplars that inherit
    abstract methods without providing a concrete implementation.
  - `BinaryOperatorTypeMismatch` and `UnaryOperatorTypeMismatch` report operator usages
    that reason to undefined result types.
  - `InvocationArgumentCountMatchesParameterCount` and
    `InvocationArgumentTypeMatchesParameterType` validate invocation signatures
    for both method and procedure calls.
  - `CallableReturnTypesMatchDoc` replaces method-only return-doc validation and
    now checks both methods and procedures, including multi-return arity and
    missing/extra `@return` entries.
  - `IterCallableYieldTypesMatchDoc` validates `@loop` docs for iter
    methods/procedures and reports `@loop` usage on non-iter callables.
  - `TypeDocLoopFixer` now provides `@loop` code actions to add missing loop
    docs, update mismatched loop types, and remove invalid/non-applicable
    `@loop` entries.
  - `MultipleAssignmentCountMismatch` checks tuple/multi assignment arity mismatches.
  - `SuperMethodExists` validates `_super.method()` targets.

  Legacy method-specific argument/return-doc checks are replaced by invocation/callable-oriented
  rules, with matching Sonar rule metadata and test coverage updates.

  **Call hierarchy** (`magik-language-server`) gains broader symbol coverage
  and safer URI handling:

  - `prepareCallHierarchy` now supports both method and procedure definitions
    (`SymbolKind.Method`/`SymbolKind.Function`).
  - Outgoing-call resolution distinguishes method/procedure items and avoids
    filesystem exceptions for unsupported URI schemes (for example `memory://`).

  ## Breaking changes

  Several typed checks were renamed/replaced. Any tooling, quality profiles, suppressions,
  or automation referencing the old rule keys/names must be updated:

  - `MethodArgumentCountMatchesParameterCount` /
    `method-argument-count-matches-parameter-count`
    -> `InvocationArgumentCountMatchesParameterCount` /
    `invocation-argument-count-matches-parameter-count`
  - `MethodArgumentTypeMatchesParameterType` /
    `method-argument-type-matches-parameter-type`
    -> `InvocationArgumentTypeMatchesParameterType` /
    `invocation-argument-type-matches-parameter-type`
  - `MethodReturnTypesMatchDoc` / `method-return-types-match-doc`
    -> `CallableReturnTypesMatchDoc` / `callable-return-types-match-doc`

  ([#545](https://github.com/StevenLooman/magik-tools/issues/545))
- Add `UnnamedProcedure`/`unnamed-procedure` check for `magik` files.

  The new check reports an issue when
  a procedure (`_proc() _endproc`) has no name.

  The check is disabled by default. ([#546](https://github.com/StevenLooman/magik-tools/issues/546))
- Add procedure support to call hierarchy provider ([#558](https://github.com/StevenLooman/magik-tools/issues/558))
- Add support for variadic return and loop types.

  A trailing `...` on a `@return` or `@loop` type declares 0..1024 values of that
  type can be returned (`@return`) or yielded per iteration (`@loop`), matching
  `_scatter` / iterator semantics in Magik, used in methods such as
  `basic_collection_mixin.for_scatter()`:

  ```magik
  _method my_collection.for_scatter()
      ## @return {<E>...}
      ...
  _endmethod
  ```

  At a `_scatter` / multi-assignment call site the reasoner expands the variadic
  into per-assignee types. Each slot is `inner | sw:unset` because the runtime
  collection may be shorter than the LHS count:

  ```magik
  _block
    _local r << rope.new()  # type: sw:rope<E=sw:integer>
    # Add any number of integers to r.
    _local (a, b, c) << (_scatter r) # a, b, c are each typed sw:integer | sw:unset
  _endblock
  ```

  A `_for ... _over iter() ...` whose iterator method declares `@loop {Type...}`
  likewise expands per for-variable to `Type | sw:unset`. The gather position (when
  present) is overridden to `sw:simple_vector` as before.

  `@return {<E>|sw:unset...}` (with an explicit `sw:unset`) is idempotent, the
  materializer deduplicates and won't double-add.

  Only the last `@return` and the last `@loop` can have a variadic type. Any
  other use of variadic types is flagged by the checks:

  - `VariadicLastPosition`/`variadic-last-position`: flags a variadic
    `@return` that is not the last `@return`, or a variadic `@loop` that is
    not the last `@loop`.
  - `VariadicOnlyOnReturnOrLoop`/`variadic-only-on-return-or-loop`: flags
    variadic syntax on `@param` or `@slot`.

  ([#563](https://github.com/StevenLooman/magik-tools/issues/563))

### Bugfixes

- Fix products not being indexed. ([#338](https://github.com/StevenLooman/magik-tools/issues/338))
- Gracefully handle a duplicate parameter specification in the type doc.

  For example, this resulted in an error:

  ```magik
  _method a.b(p1)
      ## @param {sw:integer} p1 First parameter.
      ## @param {sw:integer} p1 First parameter, again.
  _endmethod
  ```

  Now, only the first parameter specification is used of a parameter.
  Any other parameter specification for the same parameter is ignored. ([#341](https://github.com/StevenLooman/magik-tools/issues/341))
- Better handle `_scatter` in simple_vectors and arguments.

  Now something like `{_scatter b, _scatter v}` is marked as a syntax error. ([#343](https://github.com/StevenLooman/magik-tools/issues/343))
- Fix administration of the following checks:

  * `MethodIsPublic`
  * `ProductDefNameDoesNotMatchDirectoryName`
  * `AssignedTypeDoesNotMatchSlotType`
  * `ComparedTypesDoNotMatch`
  * `SwChar16VectorEvaluateInvocation`

  ([#371](https://github.com/StevenLooman/magik-tools/issues/371))
- Fix splitting line when adding EOL trivia.

  This caused an error on subsequent actions when modifying the AST. ([#388](https://github.com/StevenLooman/magik-tools/issues/388))
- Fix `magik-lint --apply-fixes` not properly recognizing the current
  file as anything other than a Magik file.

  Also refactor the use of the `CheckList`s and provide better structure. ([#389](https://github.com/StevenLooman/magik-tools/issues/389))
- Fix diagnostics source.

  Changes the source, in VSCode, from:

  ```text
  mlint (undefined-method-call-result)(undefined-method-call-result)
  ```

  To:

  ```text
  mlint (undefined-method-call-result)
  ``` ([#410](https://github.com/StevenLooman/magik-tools/issues/410))
- Fix parsing loose end in `product.def` and `module.def`. ([#416](https://github.com/StevenLooman/magik-tools/issues/416))
- Miscellaneous fixes:

  * Fix ModuleDefFile missing timestamp
  * Fix DefinitionKeeper not properly clearing
  * Fix ProcedureDefinition not properly getting bare definition

  ([#421](https://github.com/StevenLooman/magik-tools/issues/421))
- Remove `optional` keyword from `ProductDef` since it is not a valid keyword. ([#434](https://github.com/StevenLooman/magik-tools/issues/434))
- Fix Markdown rule MD060 by using compact table column style correctly. ([#436](https://github.com/StevenLooman/magik-tools/issues/436))
- Fix parsing of version numbers in `ProductDefinitionGrammar`.

  For example, `1.1.0.1-1 Beta` now parses properly. ([#438](https://github.com/StevenLooman/magik-tools/issues/438))
- Improve \# type: ... handling.

  In the following code snippet, the symbol `:a` is no longer overridden by the
  `# type: ...` instruction. The type override instruction now targets the following:

  * Returns (`_return`)/emits (`>>`)
  * Assignments
  * Other expressions that are the outermost on their line

  ([#439](https://github.com/StevenLooman/magik-tools/issues/439))
- Rename parameter names in type doc.

  In the following example, when the parameter `param1` is ranamed, the
  type doc is now also updated.

  ```magik
  _method a.b(param1, param2)
      ## Example method.
      ## @param {sw:integer} param1 First parameter
      ## @param {sw:rope} param2 Second parameter
      write(param1, param2)
  _endmethod
  ``` ([#441](https://github.com/StevenLooman/magik-tools/issues/441))
- Handle load_list comments in `FileNotInLoadListCheck`. ([#463](https://github.com/StevenLooman/magik-tools/issues/463))
- Fix message when java cannot be located

  The message was missing the word "not", causing confusion.
  With a minor refactoring to remove duplication. ([#477](https://github.com/StevenLooman/magik-tools/issues/477))
- Use `maven-shade-plugin` instead of `jarjar` for `sslr-magik-toolkit`. ([#501](https://github.com/StevenLooman/magik-tools/issues/501))
- Fix instruction for binary_operator_definition ([#557](https://github.com/StevenLooman/magik-tools/issues/557))

### Misc

- [#339](https://github.com/StevenLooman/magik-tools/issues/339), [#376](https://github.com/StevenLooman/magik-tools/issues/376), [#380](https://github.com/StevenLooman/magik-tools/issues/380), [#381](https://github.com/StevenLooman/magik-tools/issues/381), [#408](https://github.com/StevenLooman/magik-tools/issues/408), [#459](https://github.com/StevenLooman/magik-tools/issues/459), [#503](https://github.com/StevenLooman/magik-tools/issues/503), [#504](https://github.com/StevenLooman/magik-tools/issues/504), [#510](https://github.com/StevenLooman/magik-tools/issues/510), [#512](https://github.com/StevenLooman/magik-tools/issues/512), [#548](https://github.com/StevenLooman/magik-tools/issues/548), [#553](https://github.com/StevenLooman/magik-tools/issues/553)


# Changes

## 0.11.0 (2025-08-28)

- Fix error when parsing `def_slotted_exemplar()`/`def_indexed_exemplar()`/`define_condition()` with unknown children/data names.
- Fix VariableCountCheck, improve message.
- Support setting environment variables from VSCode tasks.
- Index `MagikFileDefinitions`, to prevent needing to re-index magik files without `MagikDefinitions`.
- Refactor `MagikFileScanner` & friends to be shared.
- Add `magik-typed-lint` to check magik typing from CLI/during CI.
- Fix not indexing `module.def` files.
- Rename SwChar16VectorEvaluateInvocationCheck to SwChar16VectorEvaluateInvocationTypedCheck.
- Make tab width configurable for LineLengthCheck.
- Extend VariableNamingCheck to also check for a maximum of chars and make minLength/maxLength configurable.
- Add NestingDepthCheck to test if a method/procedure/if-statement/loop-statement does not exceed the maximum nesting depth.
- Support renaming of methods, in case the type of the object the method is called on can be determined.
- Add SimplifyIf check to Sonar way profile.
- Add UnsafeEvaluateInvocation check to Sonar way profile.
- Speed up looking for definitions based on path.
- Fix UseValueCompare check to handle other notations for numbers + handle floats.
- Fix magik-lint/magik-typed-lint not using located configuration file when showing checks.
- Make check properties more descriptive by adding `max-` if needed.
- Fix WarnedCallCheck and ForbiddenCallCheck to handle method invocations.
- Add `.sys!perform()`, `.sys!perform_iter()`, `.sys!slot()`, `.sys!slot()<<`, and `.sys!slot()^<<` to ForbiddenCallCheck.
- Do not check abstract method parameters in UnusedVariableCheck if `check-parameters`.
- Fix sslr-magik-toolkit's "Evaluate XPath"-button.
- Add UndefinedVariable check to Sonar way profile.
- Add ability to override aliases path and environment path in tasks in VSCode extension.
- Set default value for `whitelist` in VariableNamingCheck.
- Add FileMustStartWithPackageStatement check to test if a file starts with a `_package`-statement.
- Use responding methods, instead of all methods on exemplar.
- Only supply argument inlay hints when all methods have the same parameters.
- Fix MethodArgumentTypeMatchesParameterTypeTypedCheck not handling generics.
- Fix binary operator reasoning not using `species` method.
- Improve auto formatting.
- Fix auto formatting bug where `_pragma` caused lines to be removed.
- FormattingCheck now uses formatting code to detect issues.
- Formatter now supports ranged formatting.
- Add `magik-session-wrapper` to wrap a Magik running image/session, providing a better CLI experience.
- Add MethodIsPublicTypedCheck.
- Fix method completions by removing `_gather` and `_optional` keywords.
- Support `_private` keyword as method invocation receiver.
- Fix formatting `_self()`.
- Add `id` to `default-whitelist` in `VariableNamingCheck`.
- Add more detailed pragma registration.
- Add checks MissingPragma, PragmaInvalidClassifyLevelCheck, PragmaInvalidUsageCheck.
- Add f4 t keybinding to VSCode plugin to write a trace command.
- Fix determining assigned variable in top scope being a global.
- Fix `ScopeEntry` types when ending up in the global scope.
- Fix `HidesVariableCheck` not seeing `DEFINITION` `ScopeEntry`s.
- Add support for inline `# mlint: disable=all` to disable all checks.
- Fix `VariableDeclarationUsageDistanceCheck` to see augmented assignment (e.g. `_andif<<` or `+<< 1`) as valid usage.
- Auto-reload tests upon running tests.
- Rewrote formatting code, which most likely results in changed formatting rules.
- Fix Magik grammar not supporting special characters like `ß`.
- Several fixes.

### Breaking changes (reiterated from above)

- Extend VariableNamingCheck to also check for a maximum of chars and make minLength/maxLength configurable. This might result in more issues.
- Add NestingDepthCheck to test if a method/procedure/if-statement/loop-statement does not exceed the maximum nesting depth. This might result in more issues.
- Make check properties more descriptive by adding `max-` if needed. `magik-lint.properties` likely needs to be updated.
- Add `.sys!perform()`, `.sys!perform_iter()`, `.sys!slot()`, `.sys!slot()<<`, and `.sys!slot()^<<` to ForbiddenCallCheck. This might result in more issues.
- Add SimplifyIf check to Sonar way profile. This might result in more issues.
- Add UnsafeEvaluateInvocation check to Sonar way profile. This might result in more issues.
- Add UndefinedVariable check to Sonar way profile. This might result in more issues.
- Add FileMustStartWithPackageStatement check to test if a file starts with a `_package`-statement. This might result in more issues.
- FormattingCheck now uses formatting code to detect issues. This might result in more or different issues.
- Rewrote formatting code, which most likely results in changed formatting rules.

## 0.10.1 (2024-08-14)

- Fix CommentedCodeCheck, splitting commented code on newlines.
- Fix CommentedCodeCheck, not seeing seeing the commented line if line is top of file.
- Fix UnusedVariableCheck, not handling try/when constructs properly.
- Show path to loaded `magik-lint.properties` in (debug) logging.
- Fix sw_type_dumper not handling sub-objects properly.
- Cache `magik-tools.properties` per opened file in `magik-language-server`.

## 0.10.0 (2024-07-28)

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
- Support Call hierarchy for methods and procedures.
- Several fixes.

## 0.9.1 (2024-03-13)

- Fix actually not indexing large files in magik-language-server.
- Fix `sonar-magik-plugin` not setting property `sonar.lang.patterns.magik`.
- Fix printing order of issues in `magik-lint`.
- Fix `module.def` parser not parsing partial test entries.
- Fix `sonar-magik-plugin` crashing on syntax error token during Copy/Paste Detection-phase.
- Fix reasoning errors where types were regarded as combined, when it was actually singular.

## 0.9.0 (2024-02-26)

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

## 0.8.3 (2024-01-05)

- Fix error finding start/end line/column for Scopes, when encountering an empty block.
- Fix error when determining issue is disabled via MagikIssueDisabledChecker in certain cases.

## 0.8.2 (2023-11-14)

- Fix not finding appropriate node to register issue on when method contains a syntax error.
- `magik-lint.properties` is searched for from path of current file in magik-lint, unless `--rcfile` is used.
- Paths specified in setting `ignore` in `magik-lint.properties` in magik-lint are respected.
- Fix grammar not supporting end labels in `_loop`/`_endloop` constructs.
- Fix reading mlint-instructions in scope.
- Fix WarnedCallCheck default warned calls not seeing the `sw:`-prefixed versions.

## 0.8.1 (2023-10-15)

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

## 0.8.0 (2023-09-27)

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
- Add definition functionality to magik-language-server.
- Move functionality from implementation provider to definition provider in magik-language-server.
- Implementation provider now provides implementations of abstract methods.
- Add SelectionRangeProvider to magik-language-server.
- Drop templated check support, including checks CommentRegularExpressionCheck and XPathCheck.
- Fix CommentedCodeCheck matching too many things as Magik code.
- Drop `--untabify` option from magik-lint.
- Various small fixes.

## 0.7.1 (2023-02-21)

- Fix VariableDeclarationUsageDistance not seeing method invocations as usage
- Fix TypeDoc ruleSpecification/sqKey having old name (NewDoc)
- Fix bug during re-reading the types database using JsonTypeKeeperReader, where a duplicate type would cause an error
- Report version and settings, if applicable

## 0.7.0 (2023-02-09)

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

## 0.6.0 (2022-09-12)

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

## 0.5.4 (2022-11-07)

- Remove MethodDoc check
- Add ParameterCount check
- Update Sonar way profile

## 0.5.3 (2022-11-04)

- Fix bug in sonar plugin where all issues were applied to every file

## 0.5.2 (2022-05-26)

- Fix bug in ScopeBuilderVisitor where import in top level procedure caused a NPE
- Fix bug in magik-lint --rcfile, where java.io.File is expected but was java.io.FileInputStream
- Fix bug where grammar accepted invalid _package identifiers, causing problems later on
- Fix bug in DefSlottedExemplarParser: also see `sw:def_slotted_exemplar`
- Minor clean up in `ThreadManager`
- Add setting `magik.lint.overrideConfigFile` to override properties file for linter from Language Server
- Fix bug in HoverProvider where no hover was provided for assigned variable

## 0.5.1 (2022-01-17)

- More robustness for other LSP clients other than VScode
- Fix binary operator handling not recognizing magik keywords
- Disable check no-self-use by default
- Disable check method-doc by default, in favor of new-doc check
- Fix bug in JSON type database reader: lines with // are now regarded as comment-lines
- Fix bug causing magik-lint.jar --show-checks to not work

## 0.5.0 (2022-01-07)

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

## 0.4.0 (2020-01-29)

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
- Fix ScopeBuilderVisitor incorrectly marking ScopeEntry as GLOBAL
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

## 0.3.2 (2019-10-29)

- Add check ScopeCount
- Add check UndefinedVariable
- Fix LocalImportProcedureCheck not properly handling non-locals/definitions

## 0.3.1 (2019-09-22)

- Prevent CPD errors when SYNTAX\_ERROR token is too long
- Fix several Magik Grammar bugs

## 0.3.0 (2019-09-22)

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

## 0.2.0 (2019-08-31)

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

## 0.1.4 (2019-07-26)

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

## 0.1.3 (2019-07-07)

- Fix output of Windows paths in magik-lint

## 0.1.2 (2019-07-06)

- Report parser errors through checks
- Enforce strict keyword matching in parser
- Fix ScopeBuilder not properly handling optional parameters
- Reduce size of magik-lint jar by removing dependencies

## 0.1.1 (2019-06-30)

- Add --untabify \<n\> option to magik-lint
- Add support for \_class keyword

## 0.1.0 (2019-06-19)

- Initial release
