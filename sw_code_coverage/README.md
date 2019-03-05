sw_code_coverage
================

The `sw_code_covarage` module provides (hopefully better, but at least faster) a means to report code coverage of your (tested) code.

By no means is the fully tested and production-ready. Use at your own risk, your mileage may vary.

Usage
-----

Usage of this module, using annotated code:

```
_package user

_block
    # Load the line_coverage_recorder and sonar_generic_coverage_reporter modules
    sw_module_manager.load_module(:line_coverage_recorder)
    sw_module_manager.load_module(:sonar_generic_coverage_reporter)

    # Activate the line_coverage_compiler_tracker
    _local lcct << line_coverage_compiler_tracker.new()
    lcct.activate()

    # Load your own modules
    sw_module_manager.load_module(:my_image_module)

    # Deactivate the line_coverage_compiler_tracker
    lcct.deactivate()

    # Activate the line_coverage_tracker for a specific exemplar
    _local lct << line_coverage_tracker.new()
    lct.enable_breakpoints_exemplar(my_engine)

    # Run your tests for this exemplar
    test_runner.run_in_foreground(my_engine_test_case.suite())

    # Deactivate the line_coverage_tracker
    lct.deactivate()
    lct.disable_breakpoints_exemplar(my_engine)

    # Export the covered lines using the sonar_generic_coverage_exporter
    _local exporter << sonar_generic_coverage_exporter.new(lct.coverage)
    exporter.run("coverage.xml")
_endblock
$
```


Workings
--------

The `line_coverage_compiler_tracker` makes itself dependent on the `magik_rep` exemplar. By doing so, it is informed of compiled code. When a method is compiled, a map is built to keep track of which line is represented by the byte code.

In turn, the `line_coverage_tracker` created a `sysevent`-handler which is notified when a breakpoint is hit. Given the previously built line-to-byte_code-map, the tracker can record the execution of a specific line.
