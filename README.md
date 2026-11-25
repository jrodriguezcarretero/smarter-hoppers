# Smarter Hoppers

Fabric mod for Minecraft Java 26.2 that makes hopper-based storage towers route items correctly and never clog.

- A hopper that can push either into a chest or down into the next hopper sends each item to the chest that **already holds it**; the next hopper is the last resort.
- **Filter containers**: any container with an item frame holding a **comparator** pulls matching items from the hopper line above. Items only enter a tower if a chest inside it has room for them; everything else continues to the overflow.
- Hoppers with a single output behave exactly like vanilla.
- Server-side only. Compatible with Lithium and Fast Item Frames.

> Work in progress. Design notes (Spanish): [PLAN.md](PLAN.md), [FASE0.md](FASE0.md).

## Building

Requires JDK 25.

```bash
./gradlew build
```

The jar is written to `build/libs/`. Server game tests run as part of the build.
