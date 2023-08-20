# Smallworld 5/Magik Debug Adapter

The Debug Adapter provides a standard interface to debug Smallworld/Magik code. This Debug Adapter only works with Smallworld 5.

The following functionality is provided:

- Breakpoints
- Conditional breakpoints
- Breakpoint on raised condition
- Stack trace on hit breakpoint
- Stack frame inspection
  - Inspect variables
  - Execute expressions

## Smallworld Magik Debugger Adapter in session

Before this Debug Adapter can connect to your Smallworld session, the Smallworld session needs to be started with the Magik Debugger Agent (mda). This can be done by starting the JVM running the Smallworld session with the parameter `-agentpath:<path_to_Smallworld_core>/bin/Linux.x86/libmda.so=socket` (in case you're running on Windows, the mda is named libmda.dll, most likely). The mda communicates via a TCP/IP socket. The port can be provided with the settings `port=...`.

A complete example of running a session from `runalias` on Linux, specifying port `32000`, is as follows:

```shell
$ /opt/Smallworld/core/bin/share/runalias -e /opt/Smallworld/core/config/environment -j -agentpath:/opt/Smallworld/core/bin/Linux.x86/libmda.so=socket,port=32000 swaf
Sourcing ...
...
```

Note that the default port the mda uses is `20000`.

## Configuration

The Debug Adapter requires at least a `host` and `port` setting. When running your session locally, most likely when doing Magik development, `host` can be set to `localhost`. When no port is specified, a value of `20000` for `port` should be used; otherwise use the specified port.

Furthermore, a path mapping can be provided. I.e., when using (Windows) network paths, but your IDE is using local paths the paths can be translated on the fly. The setting used is called `path_mapping` and this expects an array of objects with `from` and `to` keys.

A complete example of an example configuration, using port `32000` with two entries to translating paths, is as follows:

```json
{
    "host": "localhost",
    "port": 32000,
    "path_mapping": [
        {
            "from": "\\\\server\\sw_dev\\...",
            "to": "c:\\sw_dev\\..."
        },
        {
            "from": "$SMALLWORLD_GIS\\...",
            "to": "c:\\smallworld\\..."
        }
    ]
}
```
