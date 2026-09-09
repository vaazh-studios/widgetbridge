# How it works

```mermaid
flowchart LR
    subgraph App["App process (Kotlin Multiplatform)"]
        P["WidgetBridge.publish(payload, assets)"]
    end
    P -->|"1. write generations/&lt;id&gt;.tmp/"| T[assets/ + feed.json]
    T -->|"2. rename to generations/&lt;id&gt;/"| G[(generation folder)]
    G -->|"3. current.json switches"| C{{current.json}}
    C -->|4. redraw request| OS[OS]
    subgraph Widgets["Widget processes"]
        GL["Glance widget<br/>bridge.read()"]
        WK["WidgetKit extension<br/>WidgetFeedReader (Swift)<br/><b>no Kotlin linked</b>"]
    end
    C -.-> GL
    C -.-> WK
    style WK fill:#eef6ff,stroke:#7aa7d9
    style GL fill:#eefbf0,stroke:#7fc28f
```

```
<root>/widgetbridge/
  current.json                 {"current":"<id>","previous":["<id>"]}
  generations/<id>/feed.json   {"schemaVersion":1,"generatedAtEpochMs":…,"fingerprint":"<sha256>","payload":{…}}
  generations/<id>/assets/…
  generations/<id>.tmp/        in progress, ignored by readers
```

`<root>` is the app's files dir on Android and the App Group container on iOS. Readers try the
pointer's current, then its previous list, then any other generation newest first, and take the
first that parses with the expected schema version. Details: [docs/feed-format.md](docs/feed-format.md).
