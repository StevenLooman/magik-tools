# Magik-tools

`Magik-tools` is a collection of tools for the Magik programming language used by the Smallworld 5 platform. It provides the following tools:

* Language server
* Debug adapter
* Linter
* SonarQube plugin

By no means is this product fully tested and production-ready. Use at your own risk, your mileage may vary.

## Components

This project consists of several components.

### SonarQube plugin

#### Installation

After building, the artifact/jar will be created at `sonar-magik-plugin/target/sonar-magik-plugin-<version>.jar`.

Copy the plugin (`sonar-magik-plugin-<version>.jar`) to your `sonarqube/extensions/plugins` directory. (Re)start Sonar to activate the plugin.

Pre-built artifacts/jars can be found at [`magik-tools/releases`](https://github.com/StevenLooman/magik-tools/releases).

#### Analyzing projects

Use [sonar-scanner](https://docs.sonarqube.org/display/SCAN/Analyzing+with+SonarQube+Scanner) to analyze your projects. An example configuration, stored in `sonar-project.properties`, would be:

```properties
sonar.projectKey=test:test_project
sonar.projectName=Test project
sonar.sources=modules/
sonar.language=magik
sonar.coverageReportPaths=coverage.xml
```

### Magik Linter

A linter for Magik is available in the [`magik-lint`](magik-lint) directory. See [`magik-lint/README.md`](magik-lint/README.md) for more information.

### Language server

A language server for Magik is available in the [`magik-language-server`](magik-language-server) directory. See [`magik-language-server/README.md`](magik-language-server/README.md) for more information.

### Debug adapter

A debug adapter for Smallworld 5/Magik is available in the [`magik-debug-adapter`](magik-debug-adapter) directory. See [`magik-debug-adapter/README.md`](magik-debug-adapter/README.md) for more information.

## Development

### Building

You can build the plugin using maven, like so:

```shell
$ mvn clean verify test package
[INFO] Scanning for projects...
...
```

Building without running tests:

```shell
$ mvn -Dmaven.test.skip=true clean verify test package
[INFO] Scanning for projects...
...
```

Auto-formatting of Java sources:

```shell
$ mvn spotless:apply
[INFO] Scanning for projects...
...
```

### Unit tests

You can run the unit tests using maven, like so:

```shell
$ mvn clean test
[INFO] Scanning for projects...
...
```

Results will be shown on the console.

### Releasing

You can update versions using the [Versions Maven Plugin](https://www.mojohaus.org/versions/versions-maven-plugin/index.html).

To update all projects:

```shell
$ mvn -B versions:set -DgenerateBackupPoms=false -DnewVersion=<version>
...
```

Also update these files:

* `magik-language-server/client-vscode/package.json`
* `magik-language-server/client-vscode/client/package.json`
* `magik-language-server/client-vscode/client/src/const.ts`

Then, create a release by pushing a new tag to Github.

### SonarCloud

This project can found at [SonarCloud](https://sonarcloud.io/project/overview?id=StevenLooman_magik-tools).

## Contributing

See [`CONTRIBUTING.md`](CONTRIBUTING.md) for instruction on how to contribute.

## License

This project is licensed under GPLv3, see [`LICENSE.md`](LICENSE.md).

## Commercial use

Commercial use is allowed. By no means is this product fully tested. Use at your own risk, your mileage may vary.

If you do use this - commercially or not - please do inform me.
