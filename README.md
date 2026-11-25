# Smarter Hoppers

Fabric mod for Minecraft Java 26.2 that makes hopper-based storage towers route items correctly and never clog.

- A hopper with two outputs (the container it faces, and the hopper below that pulls from it) sends each item to the output that **already holds it**: storage towers fill the right chests, and hopper sorters' filters get their items.
- **Filter containers**: any container with an item frame holding a **comparator** pulls matching items from the hopper line above. Items only enter a tower if a chest inside it has room for them; everything else continues to the overflow.
- Hoppers with a single output behave exactly like vanilla.
- `/smarterhoppers diagnose [radius]` lists items waiting because the chest that holds them is full, and counts marked containers and two-output hoppers nearby.
- Server-side only. Compatible with Lithium and Fast Item Frames.

## Configuration

`config/smarter-hoppers.json`, created on first start:

| Key | Default | Meaning |
|---|---|---|
| `filter_items_kept` | 2 | Copies of each item a marked container always keeps (its filter items). |
| `chest_margin` | 0 | Empty slots per tower chest never counted as room when deciding whether an item may enter a tower. |

> Work in progress. Design notes (Spanish): [PLAN.md](PLAN.md), [FASE0.md](FASE0.md).

## Building

Requires JDK 25.

```bash
./gradlew build
```

The jar is written to `build/libs/`. Server game tests run as part of the build, with Lithium and Fast Item Frames loaded. Add `-PnoLithium` to run them against vanilla hopper code.

## License

[MIT](LICENSE)
