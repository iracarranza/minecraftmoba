# Final native catalogue integrity audit — 15 October 2026

**Complete integrity pass.** The native producer remains stopped at its original
4 GiB storage bound. No producer was restarted, no cap raised and no evidence
removed. Its singleton lock is free, SQLite WAL is empty and the recorded
checkpoint stayed unchanged during the full audit.

The saved catalogue contains **13,916 unique seeds**,
**1,391,600 distinct regions** and
**5,566,400 orientation records**. Seeds form the contiguous
range 910000001–910013916; the preserved
continuation cursor is **910013917**. The final boundary seed has all
400 orientations committed.

Every one of the 13,916 compressed seed archives was read through its gzip CRC
and every JSON record parsed. All records retained their regional vectors,
seven-band profiles, biome histograms, preference misses and unknown checks.
The audit verified 38,964,800 regional-band vectors
and every native grid's header and complete body length. Every seed has one
measurement archive, summary and original raw grid. There were no missing files,
wrong per-seed window counts, orphan index rows, duplicate feed seeds or temporary
files. SQLite quick_check returned `ok` and archive/summary/index counts agree.

Looser future searches retain **5,079,664
orientations with at least one preference miss**, including
537,888 without the coarse
end-land preference. These are approximate native measurements, not verified
gameplay layouts or READY maps. The first two smoke seeds lack the explicit
river-family field, but their raw biome IDs remain intact; compatibility names
`dry`/`land` continue to mean a non-ocean-biome proxy.

The producer's stopping estimate was 4,295,677,670 bytes, 710,374 bytes above its
4,294,967,296-byte cap after completing the final two-seed batch. Subsequent WAL
checkpointing left the static source components at 4,264,493,550
bytes, approximately 3.97 GiB. This conservative stopping estimate does not
remove the cap or authorize a restart. Producer source and native binary hashes
still match the recorded startup hashes.

[Full audit evidence](final-health-2026-10-15.json) records the complete checks;
[storage/hash evidence](final-health-storage-2026-10-15.json) distinguishes the
stopping estimate from current static file sizes. The full read took
257.2 seconds.
