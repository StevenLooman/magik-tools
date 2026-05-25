Distinguish mixin exemplars with a dedicated `Sort.MIXIN` value.

Previously, `def_mixin` definitions and the built-in `sw:slotted_format_mixin` /
`sw:indexed_format_mixin` were tagged as `Sort.INTRINSIC`, conflating them with
non-mixin intrinsic types. They are now reported as `Sort.MIXIN`, which is also
serialized as `"mixin"` in the JSON type database. As a result, the type database
will need to be re-created.
