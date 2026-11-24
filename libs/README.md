# Patched ModularUI for the Fossil Fuel build

GregTech bundles ModularUI (`brachy.modularui:modularui-mc1.21.1`, LGPL-3.0, source at
https://github.com/GregTechCEu/ModularUI-Modern). Its latest build, `3.3.1-SNAPSHOT` (2026-10-13, branch `1.21.1`,
commit `d68ef357`), has two problems in Fossil Fuel:

- It was compiled against JEI 19.25.1.328, and its JEI integration does not fit JEI 19.64, which Fossil Fuel ships:
  `RecipeGuiLogicMixin` finds no `recipeHeight` to modify, so JEI fails to start and the first screen a player opens
  disconnects or crashes the client, and `RecipeSlotAccessor` names two `RecipeSlot` fields JEI 19.64 no longer has.
- Schema previews fail on a client joined to a dedicated server. `SchemaLevel.INSTANCE`, built by `SchemaLevel`'s
  static initializer, takes its registries from `RegistryAccessContainer.current()`, which there is still the builtin
  set with no `minecraft:dimension_type`. The class fails to initialize, so every GregTech multiblock preview (JEI
  multiblock info pages, the terminal, an unformed controller's in-world preview) throws in multiplayer.
  Singleplayer is unaffected.

`maven/` is a one-artifact Maven repository holding `3.3.1-ff.2`: the `3.3.1-SNAPSHOT` jar with the classes these
patches change recompiled, its version set to `3.3.1-ff.2` and its optional JEI dependency to `[19.64.0.455,)`.
`modularui-3.3.1-ff.2.diff` is the whole source patch against `d68ef357`:

- JEI 19.64 (`3.3.1-ff.1`; its 23 JEI classes, `brachy/modularui/core/mixins/jei` and
  `brachy/modularui/integration/jei`, recompiled against JEI 19.64.0.455):
  - `RecipeGuiLogicMixin` modifies JEI 19.64's `recipeSizeWithButtons` (an `ImmutableSize2i`) instead of
    `recipeHeight`, keeping its width and using the category's maximum height, as before.
  - `RecipeSlotAccessor` replaces a slot's `RecipeSlotIngredients` (JEI 19.64 keeps all and displayed ingredients in
    one object) and invokes `onDisplayOverridesChanged` to clear the slot's caches.
  - `JeiRecipeViewerSlot` builds the output tooltip callback with JEI 19.64's constructor (with the source recipe
    ids).
  - `JeiScreenHandler.ContainerScreen` overrides the factory form of `getClickableIngredientUnderMouse`, which
    `IGuiContainerHandler` and `IScreenHandler` both default in JEI 19.64.
- Multiplayer schemas (`3.3.1-ff.2`): upstream commit `ed3bb988` (ModularUI-Modern #153, on `1.21.1` since
  2026-11-10), cherry-picked unchanged. It removes `SchemaLevel.INSTANCE`; `new SchemaLevel()` takes its registries
  from `SidedAccessHelper.getRegistries()` (the connection's on the client, the server's on the server), which now
  falls back to `RegistryAccessContainer.current()` when there is no connection or server. GregTech's
  `MutableSchema` builds its level with `new SchemaLevel()`, and nothing else used `INSTANCE`. Recompiled:
  `SchemaLevel` (with `SchemaLevel$1` and `SchemaLevel$EntityCallbacks`, whose code is unchanged),
  `SidedAccessHelper` and `ClientCallWrapper`.

To rebuild: check out `d68ef357`, apply the diff, set `jei = "19.64.0.455"` in `gradle/forge.versions.toml` and
`mod_version = 3.3.1-ff.2`, and build. The jar here was made the other way: the changed classes compiled with javac
(JDK 21, `-g`, Lombok 1.18.38 as the annotation processor) against GregTech's compile classpath, which holds the
NeoForge 21.1.248 game jar, JEI 19.64.0.455 and the previous jar, then put in place of the old ones in that jar,
keeping its entry order and timestamps. Compiled the same way from `d68ef357`, the five class files of the three
sources `ed3bb988` changes match the `3.3.1-SNAPSHOT` jar's byte for byte.
