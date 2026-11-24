# Patched ModularUI for the Fossil Fuel build

GregTech bundles ModularUI (`brachy.modularui:modularui-mc1.21.1`, LGPL-3.0; upstream
https://github.com/GregTechCEu/ModularUI-Modern, branch `1.21.1`). Upstream GregTech builds against the
`3.3.1-SNAPSHOT` on maven.gtceu.com. This fork bundles its own build instead, from the fork
https://github.com/EllisSK/ModularUI-Modern:

- `maven/` is a one-artifact Maven repository holding `3.3.1-ff.3` (jar and pom), built with ModularUI's own
  Gradle build from tag `v3.3.1-ff.3` (branch `ff/1.21.1` of the fork). The jar's SHA-256 is
  `e545d2ba1b7e28842f90c122cbf72f005bb0d6a853efd603ebbb09746751d42d`.
- `modularui-3.3.1-ff.3.diff` is the same change as a `git format-patch` series against upstream `035c9c8`, in
  case the fork is unavailable.

## What 3.3.1-ff.3 is

Upstream `1.21.1` at `035c9c8` (2026-11-24) plus two commits:

1. Build against JEI 19.64.0.455 and NeoForge 21.1.252, which Fossil Fuel ships, instead of JEI 19.70.0.469 and
   NeoForge 21.1.265. Upstream's jar declares its optional JEI dependency as `[19.70.0.469,)`, so with JEI 19.64
   NeoForge refuses to load it. Upstream's JEI integration (rewritten for JEI 19.70 in ModularUI-Modern #147)
   compiles against JEI 19.64 except for `SearchInputLayer#focus()`, which only JEI 19.70 has; `JeiScreenHandler`
   focuses the overlay's search field through an `IngredientListOverlay` accessor instead (a client mixin, the one
   #147 removed). The mixin targets #147 uses all exist in JEI 19.64 (checked against its bytecode).
2. `mod_version = 3.3.1-ff.3`, so the jar's JEI dependency is `[19.64.0.455,)`.

Since the previous bundle, `3.3.1-ff.2` (the 2026-10-13 snapshot `d68ef357` with a JEI 19.64 fit and #153
cherry-picked; tags `v3.3.1-ff.1` and `v3.3.1-ff.2` on branch `ff-jei-19.64` of the fork), upstream added:

- schema (multiblock preview) fixes: #153 (previews in multiplayer, already in ff.2), #139 and #140 via #154
  ("multiblock previews stop exploding clients": ray tracing only inside the structure's bounds, no chunk generated
  on a lookup miss), #155, and thread-safe preview chunks with connected textures (#130, #133);
- sync fixes: sync handler packets are serialized when sent, not when the pipeline encodes them (#130, #133), and
  typed sync handler getters (#137); `callSyncedAction(key, writer)`, removed in #156, is back (#162);
- item slots that work through `IItemHandlerModifiable` (#141, aimed at item duplication through slots);
- per-widget theme overrides (#159, which renames the theme getters), themes keeping their background after a
  resource reload (#135), tooltip lines that mix text and components (#119 via #144);
- the JEI 19.70 integration (#147, with mezz's own changes: display overrides instead of rewriting JEI's slots
  through accessors) and spotless formatting (#166).

## GregTech changes this ModularUI needs

- `ITheme#get*Theme()` is now `get*ThemeEntry()`, `Widget#getWidgetThemeInternal` is now
  `getWidgetThemeEntryInternal` and `Widget#widgetTheme(key)` is now `widgetThemeKey(key)` (#159).
- `ModularSlot` takes an `IItemHandlerModifiable` (#141): `GTMuiMachineUtil`'s slot group helpers take one too,
  as in upstream GregTech #5626.
- `JeiIngredientHandler` is gone (#147): `GTRecipeJEICategory` filters out empty stacks itself, which is all its
  `toJeiIngredient` did.

No GregTech class overrides a renamed method without `@Override` (checked against the public API diff of the
two jars), so nothing changes behaviour silently.

## Rebuilding or upgrading

```
git clone https://github.com/EllisSK/ModularUI-Modern
cd ModularUI-Modern
git checkout v3.3.1-ff.3
JAVA_HOME=<a JDK 21> ./gradlew build
```

gives `build/libs/modularui-mc1.21.1-3.3.1-ff.3.jar`. Put it in
`maven/brachy/modularui/modularui-mc1.21.1/<version>/` next to a pom (copy the existing one and change its
version; it is what `./gradlew generatePomFileForMavenJavaPublication` writes, without the Gradle-metadata
comment, and depends only on EvalEx 3.6.0), and set `mui` in `gradle/forge.versions.toml`.

To take a newer upstream: in the fork, `git remote add upstream https://github.com/GregTechCEu/ModularUI-Modern`,
`git fetch upstream`, then on a new branch from `upstream/1.21.1` cherry-pick the JEI 19.64 commit from
`ff/1.21.1` (drop it once the pack ships JEI 19.70 or newer), set `mod_version` to `<upstream version>-ff.N`,
build, check that GregTech compiles against it, tag `v<mod_version>` and push the branch and tag.
