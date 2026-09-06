# Feed format

Stable within a major version. Both readers (Kotlin and Swift) implement exactly these rules.

```
<root>/<directoryName>/
  current.json                 {"current":"<id>","previous":["<id>", ...]}
  generations/<id>/
    feed.json                  {"schemaVersion":<int>,"generatedAtEpochMs":<long>,"fingerprint":"<sha256 hex>","payload":{...}}
    assets/<fileName>
  generations/<id>.tmp/        in-progress write, ignored by readers
```

- `<root>`: Android `context.filesDir`; iOS the App Group container.
- `<id>`: `<generatedAtEpochMs>-<first 8 hex of fingerprint>`.
- `fingerprint`: sha256 over the schema version, the canonical (compact) payload JSON, and each asset's name, size and content hash, sorted by name. `publish` skips writing when it matches the current generation.
- Safe names (ids, asset file names): letters, digits, `-`, `_`, `.`; never blank. Everything else is refused on write and skipped on read.

## Reader rules

1. Candidate order: `current`, then `previous` in order, then every other non-`.tmp` generation directory sorted descending by id. Duplicates removed.
2. A candidate is accepted when `feed.json` parses and `schemaVersion` equals the reader's.
3. `assetPath` / `assetURL` return a value only when the name is safe and the resolved path stays under `generations/<id>/` and the file exists.
4. Readers never throw for corrupt or foreign files; they return the next candidate or nothing.

## Writer rules

1. Write `generations/<id>.tmp/assets/*`, then `feed.json`, then rename the folder to `generations/<id>/`.
2. Write `current.json` atomically (temp + rename on Android, `writeToFile(atomically:)` on iOS).
3. Delete every generation not referenced by the new pointer (`keepPrevious` old ones are kept).
4. Ask the OS to redraw.
