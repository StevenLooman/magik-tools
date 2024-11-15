# Magik-Lint

A command line linter to use in your workflow. Mostly built after [Pylint](https://www.pylint.org/).

## Usage

Magik-lint takes the following command line options:

- `--apply-fixes`: Automatically apply fixes, when possible.
- `--column-offset`: Column offset, in case your editor uses a different column numbering scheme.
- `--debug`: Enable debug logging.
- `--help`: Show help.
- `--max-infractions`: Maximum number of issues to report.
- `--msg-template`: Template for output, defaults to: `${path}:${line}:${column}: ${msg} (${symbol})`
- `--rcfile`: Path to configuration file
- `--show-checks`: Show a list of all checks and whether it is disabled
- `--version`: Show version and quit.
- A file or directory to check (can be multiple).
  - If a file is given, only this file is checked.
  - If a directory is given, all files in this directory and any sub-directories, are checked.

## Integration

The [emacs magik-mode](https://github.com/roadrunner1776/magik) readily supports integration of magik-tools through [flycheck](https://www.flycheck.org/).

VSCode integration is achieved by adding a task with a `problemMatcher`, for example:

```json
{
    "label": "magik-lint",
    "type": "shell",
    "command": "java -jar /path/to/magik-lint-LATEST.jar --msg-template \"\\${path}:\\${line}:\\${column}:\\${severity}:\\${symbol}:\\${msg}\" --watch .",
    "problemMatcher": {
        "owner": "magik",
        "fileLocation": "relative",
        "pattern": {
            "regexp": "^(.*):(\\d+):(\\d+):(.*):(.*):(.*)$",
            "file": 1,
            "line": 2,
            "column": 3,
            "severity": 4,
            "code": 5,
            "message": 6
        }
    }
}
```

## Configuration

Magik-lint can use a configuration file.

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

I.e., you can disable checks `method-complexity` and `line-length` by setting:

```text
disabled=method-complexity,line-length
```

You can configure the `line-length` check to allow up to 120 characters per line by setting:

```text
line-length.max-line-length=120
```

### Settings per check

| Check name | Setting name | Default value |
|------------|--------------|---------------|
| commented-code | min-lines | 3 |
| exemplar-slot-count | max-slot-count | 10 |
| file-method-count | max-method-count | 10 |
| forbidden-call | forbidden-calls | show(),sw:show(),print(),sw:print(),debug_print(),sw:debug_print(),.sys!perform(),.sys!slot() |
| forbidden-global-usage | forbidden-globals | !current_grs!,sw:!current_grs! |
| fobidden-inheritance | forbidden-parents |  |
| formatting | indent-character | tab |
| formatting | tab-width | 8 |
| line-length | max-line-length | 120 |
| line-length | tab-width | 8 |
| method-complexity | max-complexity | 10 |
| nesting-depth | max-nesting-depth | 3 |
| nesting-depth | count-early-return-as-nesting-depth | true |
| parameter-count | max-parameter-count | 6 |
| scope-count | max-scope-count | 25 |
| sw-method-doc | allow-blank-method-doc | false |
| todo-comment | forbidden-comment-words | TODO,FIXME,HACK,NOTE,TEMP,XXX |
| unused-variable | check-parameters | false |
| variable-count | max-variable-count | 8 |
| variable-declaration-usage-distance | max-distance | 5 |
| variable-declaration-usage-distance | ignore-constants | true |
| variable-naming | min-length | 3 |
| variable-naming | max-length | 32 |
| variable-naming | whitelist | x,y,z |
| warned-call | warned-calls | write(),sw:write(),remex(),sw:remex(),remove_exemplar(),sw:remove_exemplar() |

## Exit codes

The exit code from Magik-lint is determined by the infractions of checks. The severity of a check will set a flag in the return code:

| Major | 2 |
|-------|---|
| Minor | 4 |

When using Git, checking flags in the return code allows you to allow minor infractions being committed, but prevent a commit of a major infraction, using Git hooks.
