Changes
=======

0.1.5 (unreleased)

- Add --watch option to magik-lint
- Add --help option to magik-lint


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
