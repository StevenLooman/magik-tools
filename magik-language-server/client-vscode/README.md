# VSCode client for Magik Language Server

TODO


## Packaging

`vsce` is used to create a package. `vsce` is run from a docker container, as Linux distributions most likely include
older version of `node` and `npm`.

Running `vsce`:
```bash
$ DOCKER_BUILDKIT=1 docker build --tag vsce "https://github.com/microsoft/vscode-vsce.git#main"
$ docker run --rm -it vsce --version
$ docker run --rm -it -v "$(pwd)":/workspace vsce package
```
