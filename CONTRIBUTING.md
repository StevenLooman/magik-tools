Contributing
============

The `sonar-magik`-plugin is a work in progress and currently developed in my spare time. I cannot guarantee this project is bug-free and/or will work according to your expectations. You can contribute by using this plugin and report results.

For example:

- If you find any problems, have suggestions, or otherwise, report these at the [issue tracker](https://github.com/StevenLooman/sonar-swmagik/issues).
- If you report an issue, please report what is happening, expected result, actual result, used versions, etc.
- If you have a fix, please create pull request.


Creating checks
---------------

Creating additional checks can be done as follows. To make this more concrete, the test will be called `ExampleCheck`. All steps are done in the `magik-checks` project.

- Create a test-class called `org.stevenlooman.sw.magik.checks.ExampleCheckTest`, in `src/test/java`. You can use the other classes in the package as examples. Be sure to add several tests, testing both check-passes and check-failures.
- Create a check-class called `org.stevenlooman.sw.magik.checks.ExampleCheck`, in `src/main/java`. You can use the other classes in the package as examples.
- Create `ExampleCheck.html` and `ExampleCheck.json` in `src/main/java/resources/org/stevenlooman/sw/sonar/l10n/magik/rules`. The first contains the text shown in Sonar, the latter contains meta-data about the check.
- Please make sure to follow the naming conventions already present.
