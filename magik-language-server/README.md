# Smallworld/Magik language server

A Language Server for Smallworld/Magik. This language server provides functionality such as:

- semantic token highlighting
- hover
- defintion lookup
- references lookup
- refactoring
  - variable renaming
- linting
- typing inferencing
- (real) auto complete
- ...

## Configuration

This language server has the following settings:

- `magik.smallworldGis`: Path to Smallworld Core.
- `magik.productDirs`: Paths to (compiled, containing a `libs/` directory) products.
- `magik.lint.overrideConfigFile`: Override path to magiklintrc.properties.
- `magik.typing.typeDatabasePaths`: Paths to type databases.
- `magik.typing.showTypingInlayHints`: Show typing inlay hints.
- `magik.typing.showTypingArgumentInlayHints`: Show (certain) argument name inlay hints.
- `magik.typing.enableChecks`: Enable typing checks.
- `magik.typing.indexGlobalUsages`: Enable indexing of usages of globals by methods.
- `magik.typing.indexMethodUsages`: Enable indexing of usages of methods by methods.
- `magik.typing.indexSlotUsages`: Enable indexing of usages of slots by methods.
- `magik.typing.indexConditionUsages`: Enable indexing of usages of conditions by methods.
- `magik.typing.cacheIndexedDefinitions`: Store and load the indexed definitions in the workspace folders.

### Additional configuration for VSCode

- `magik.javaHome`: Path to Java Runtime, Java 17 minimum.
- `magik.aliases`: Path to gis_aliases file.
- `magik.environment`: Path to your environment file.
- `magik.formatting.indentChar`: Indent character, 'tab' or 'space'.
- `magik.formatting.indentWidth`: Indent width (tab size or number of spaces).
- `magik.formatting.insertFinalNewline`: Insert final newline.
- `magik.formatting.trimTrailingWhitespace`: Trim trailing whitespace.
- `magik.formatting.trimFinalNewlines`: Trim final newlines.

Via the VSCode client you can start a new session, using the specified environment and aliases file. The aliases file is parsed and the entries are provided as Tasks.

## Typed Magik

The language server provides extensive reasoning/inferencing abilities about the types of variables. For this to work, the methods/procedures need to be annotated with type information. For example, the type(s) the method returns or the expected types of parameters. Using this information, functionality such as auto-complete, checking whether a called method exists, etc can be provided.

An example of a method definition, with type annotations, is as follows:

```magik
_method object.method1(param1, param2, _optional param3)
    ## An example method.
    ## @param {sw:integer|sw:float} param1 Description for parameter 1.
    ## @param {sw:rope<sw:symbol>} param2 Description for parameter 2.
    ## @param {sw:rwo_record} param3 Description for parameter 3.
    ## @return {sw:property_list<sw:symbol, sw:rope>} Description for the first returned object.
    ## @return {sw:false|sw:unset} Descriptoin for the second returned object.
    ...
_endmethod
```

### Type database

The standard Smallworld type data (types, methods, ...) can be exported using the `type_dumper.magik` script. The resulting data, called a type database, is written as JSON-lines. The data contains:

- Package definitions
- Type definitions (exemplars, etc)
- Global definitions
- Method definitions
- Procedure definitions
- Condition definitions
- Binary operator definitions

Given that Smallworld itself does not use type annotations, the resulting data has to be manually improved to provide proper information for the language server. I.e., return types for methods are undefined by default, but can be manually added to the type database. When this is done, the language server can reason about the method call and provide things such as (auto) completion and validation.

Note that the type information is only used by this language server. The Smallworld session itself does not do anything with this information. I.e., no checking of parameter types is done at runtime.
