# Memory measurement

`MemoryProbe.java` measures byte arrays and transient BitSets with Java Instrumentation.
`PaperMemoryProbe.java` measures Paper 1.21.11's actual
`DirtyCraftPersistentDataContainer`, used by CraftChunk, plus the complete reachable
raw-map graph: map/table/nodes, key string and bytes, ByteArrayTag, and payload array.
Shared type-registry objects and the enclosing Minecraft chunk are excluded.

Reproduce the Paper measurement after bootstrapping a Paper 1.21.11 server:

```sh
python3 paper_memory_probe.py --java-home "$JAVA_HOME" --server /path/to/paper-server \
  --output paper-pdc-memory-layout.csv 185 4148 4504 4902 12288
```

This launches an isolated JVM; it does not attach to or modify the running server.
`afterRemovalGraphBytes` intentionally includes the retained HashMap table after
its entry is removed. Deleting the PDC key frees its payload, not the already
allocated map table. Results are JVM-layout specific. The instrumentation ledger
in Provenance is separate from these per-chunk PDC measurements, as are temporary
copy allocations while handling an event.
