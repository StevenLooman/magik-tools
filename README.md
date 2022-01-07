# Sonar Magik plugin

Magik analyzed!

`Magik-tools` is a collection of tools for the Magik programming language used by the Smallworld 5 platform. It provides the following tools:

* SonarQube plugin
* Linter
* Language server
* Debug adapter

By no means is this product fully tested and production-ready. Use at your own risk, your mileage may vary.


## Status

No build status for now.


## SonarQube plugin

### Installation

After building, the artifact/jar will be created at `sonar-magik-plugin/target/sonar-magik-plugin-<version>.jar`.

Copy the plugin (`sonar-magik-plugin-<version>.jar`) to your `sonarqube/extensions/plugins` directory. (Re)start Sonar to activate the plugin.

Pre-built artifacts/jars can be found at [`magik-tools/releases`](https://github.com/StevenLooman/magik-tools/releases).


### Analyzing projects

Use [sonar-scanner](https://docs.sonarqube.org/display/SCAN/Analyzing+with+SonarQube+Scanner) to analyze your projects. An example configuration, stored in `sonar-project.properties`, would be:

```
sonar.projectKey=test:test_project
sonar.projectName=Test project
sonar.sources=modules/
sonar.language=magik
sonar.coverageReportPaths=coverage.xml
```


## Magik Linter

A linter for magik is available in the [`magik-lint`](magik-lint) directory. See [`magik-lint/README.md`](magik-lint/README.md) for more information.


## Language server

TODO


## Debug adapter

TODO


## Building

You can build the plugin using maven, like so:

```
$ mvn clean package
```


## Unit tests

You can run the unit tests using maven, like so:

```
$ mvn clean test
```

Results will be shown on the console.


## Contributing

See [`CONTRIBUTING.md`](CONTRIBUTING.md) for instruction on how to contribute.


## License

This project is licensed under GPLv3, see [`LICENSE.md`](LICENSE.md).

Note that this has been changed from LGPLv3!


## Commercial use

By no means is this product fully tested and production-ready. Use at your own risk, your mileage may vary.

If you do use this, please do inform me.
