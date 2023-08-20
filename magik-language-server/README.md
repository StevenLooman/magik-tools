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

- `magik.smallworldGis`: Path to your Smallworld installation
- `magik.lint.overrideConfigFile`: Override magik-lint configuration file
- `magik.typing.typeDatabasePaths`: Path to a database contaning type/method/... definitions
- `magik.typing.enableChecks`: Enable/disable typing checks

### Additional configuration for VSCode

- `magik.javaHome`: Path to your Java installation
- `magik.aliases`: Path to aliases file
- `magik.environment`: Path to your environment file

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
