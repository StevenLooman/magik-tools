Sonar Magik plugin
==================

`Sonar-Magik` is a code analyzer for Smallworld projects, which uses the Magik programming language.

By no means is this product fully tested and production-ready. Use at your own risk, your mileage may vary.


Status
------

[![Build Status](https://travis-ci.org/StevenLooman/sonar-magik.svg?branch=develop)](https://travis-ci.org/StevenLooman/sonar-magik)


Installation
------------

Copy the plugin (`sonar-magik-plugin-<version>.jar`) to your `sonarqube/extensions/plugins` directory. (Re)start Sonar to activate the plugin.


Analyzing projects
------------------

Use [sonar-scanner](https://docs.sonarqube.org/display/SCAN/Analyzing+with+SonarQube+Scanner) to analyze your projects. An example configuration, stored in `sonar-project.properties`, would be:

```
sonar.projectKey=test:test_project
sonar.projectName=Test project
sonar.sources=modules/
sonar.language=magik
sonar.coverageReportPaths=coverage.xml
```


Building
--------

You can build the plugin using maven, like so:

```
$ mvn clean package
```

An artifact/jar will be created at `sonar-magik-plugin/target/sonar-magik-plugin-<version>.jar`.


Coverage
--------

A module has been built to report coverage from within Smallworld 4.3. See `sw_code_coverage`. It includes a way to export line coverage to an XML file which `Sonar` supports. See `sw_code_coverage/README.md` on how to use this.

To use the coverage report, set property `sonar.coverageReportPaths` in your `sonar-project.properties`-file.


XPath rule
----------

An XPath-template-rule is available to quickly build custom rules. Follow these steps to create and use a custom rule:

- Log in at you Sonar-installation;
- Go to `Quality Profiles`;
- Create a copy of the standard Magik Sonar Way profile;
- Go the copied profile;
- Find the `Track breaches of an XPath rule` Rule Template;
- Create a Custom Rule by clicking on the Create-button.


Magik linter
------------

A linter for magik is available in the `magik-lint` directory. See `magik-link/README.md` for more information.


Contributing
------------

See `CONTRIBUTING.md` for instruction on how to contribute.


License
-------

This project is licensed under LGPLv3, see `LICENSE.md`.


Commercial use
--------------

By no means is this product fully tested and production-ready. Use at your own risk, your mileage may vary.

If you do use this, please do inform me.
