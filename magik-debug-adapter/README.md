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

Before this Debug Adapter can connect to your Smallworld session, the Smallworld session needs to be started with the Magik Debugger Agent (mda).
This can be done by starting the JVM running the Smallworld session with the parameter `-agentpath:<path_to_Smallworld_core>/bin/Linux.x86/libmda.so=socket` (in case you're running on Windows, the mda is named mda.dll).
The mda communicates via a TCP/IP socket. The port can be given after the `socket` option, separated by a colon: `socket:<port>`.

A complete example of running a session from `runalias` on Linux, specifying port `32000`, is as follows:

```shell
$ /opt/Smallworld/core/bin/share/runalias -e /opt/Smallworld/core/config/environment -j -agentpath:/opt/Smallworld/core/bin/Linux.x86/libmda.so=socket:32000 swaf
Sourcing ...
...
```

Note that the default port the mda uses is `32000`.

The mda listens on all network interfaces and does not ask for a password, so anyone who can reach the port can attach and evaluate Magik in the session.
Only start a session with the mda on a machine whose port is not reachable from untrusted networks.

## Configuration

The Debug Adapter requires at least a `host` and `port` setting. When running your session locally, most likely when doing Magik development, `host` can be set to `localhost`.
When no port is specified, a value of `32000` for `port` should be used; otherwise use the specified port.

Furthermore, a path mapping can be provided. I.e., when using (Windows) network paths, but your IDE is using local paths the paths can be translated on the fly.
The setting used is called `path_mapping` and this expects an array of objects with `from` and `to` keys.

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

## Known limitations of the mda

The following was observed with the mda of Smallworld 5.3.6.1 on Linux. Other versions and Windows have not been checked.

These affect users:

- **Step over and step out are slow over calls that do work.**
  The mda single-steps through all the code the called method runs, until control returns to the line after the call.
  Stepping over a single `show()` call takes about 30 seconds. Over application code, such as a query or GUI code, it can take minutes or longer.
- **The mda accepts no new connection while it is stepping.** A debug adapter that connects during a long step is refused until the step has finished.
- **A disconnect removes all breakpoints.** When the debug adapter disconnects, the mda removes every breakpoint and a suspended thread continues.
  Breakpoints are not kept for the next connection; the IDE sets them again when the debug adapter attaches.
- **Some locals cannot be inspected.** A local variable that the Magik compiler optimises away, for example one that is assigned and then only returned, is not reported by the mda.
  It is missing from the variables view, and evaluating it gives `unset`, or the value of a global of the same name.
- **The port option is not checked.** An option the mda does not understand, such as `socket,port=20000`, is silently ignored, and the mda listens on its default port 32000.
- **There is no access control.** As said above, the mda listens on all network interfaces without a password.

These matter when working on the debug adapter itself:

- A breakpoint is set by the package-qualified method name, such as `user:my_exemplar.my_method()`, and the absolute line number in the source file.
  A line where the method has no code, such as a line outside it, is refused with `INVALID_LINE_NUMBER`.
  A method name without its package is refused with `METHOD_NOT_FOUND`.
- Suspending a thread that is already suspended gives an error reply that carries no error code: the field that holds the error code in other replies holds the thread id.
- Setting a breakpoint on a line that already holds one gives `BREAKPOINT_ALREADY_SET_AT_LOCATION`, but the mda still uses up a breakpoint id.
  Later hits on that line report the used up id, which the mda never gave to the client.
  A client has to cope with a hit on a breakpoint id it does not know.
- Replies are not limited in size.
  An evaluation result or a local's value is sent whole, so a long string gives a reply of more than 100 KB.
- The mda's step over and step out do complete; there is just no upper bound on how long they take.
