# Magik-typed-Lint

A command line type linter to use in your workflow. Mostly built after [MyPy](https://www.mypy-lang.org/).

## Usage

Magik-typed-lint takes the following command line options:

- `--column-offset`: Column offset, in case your editor uses a different column numbering scheme.
- `--debug`: Enable debug logging.
- `--help`: Show help.
- `--max-infractions`: Maximum number of issues to report.
- `--msg-template`: Template for output, defaults to: `${path}:${line}:${column}: ${msg} (${symbol})`
- `--rcfile`: Path to configuration file
- `--show-checks`: Show a list of all checks and whether it is disabled
- `--types-db`: Types database to read before checking (can be multiple)
- `--pre-index-dir`: Index directory before checking (can be multiple)
- `--version`: Show version and quit.
- A file or directory to check (can be multiple).
  - If a file is given, only this file is checked.
  - If a directory is given, all files in this directory and any sub-directories, are checked.

## Configuration

Magik-typed-lint can use a configuration file.

The configuration file is located as follows, in order:

1. if `--rcfile` command line argument is given, use it;
2. if `magik-lint.properties` exists in the current working directory, use it;
3. if `.magik-lint.properties` exists in the current working directory, use it;
4. if `magik-lint.properties` exists in the current Smallworld product seen from the current working directory, or any parent product, use it;
5. if environment variable `MAGIKLINTRC` is given and the file exists, use it;
6. if `.magik-lint.properties` exists in your home directory, use it;
7. if `/etc/magik-lint.properties` exists, use it.

If no configuration file is found, defaults are assumed.

The following options are available in the configuration file:

- enabled=\<comma separated list of check-names\>
- disabled=\<comma separated list of check-names or `all`\>
- \<check_name\>.\<check_attribute\>=\<value\>

I.e., you can disable checks `sw-char16-vector-evaluate-invocation` and `slot-exists` by setting:

```text
disabled=sw-char16-vector-evaluate-invocation,slot-exists
```

You can configure the `module-required-for-global` check to test if the module is required for a used global by setting:

```text
module-required-for-global.always-loaded-modules=ace_core,rwo_core,super_dd,sw_core_magik_sessions
```

## Exit codes

The exit code from Magik-lint is determined by the infractions of checks. The severity of a check will set a flag in the return code:

| Severity    | Exit code |
|-------------|-----------|
| Critical    | 2         |
| Major       | 4         |
| Minor       | 8         |

When using Git, checking flags in the return code allows you to allow minor infractions being committed, but prevent a commit of a major infraction, using Git hooks.
