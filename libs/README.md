# Patched ModularUI for the Fossil Fuel build

GregTech bundles ModularUI (`brachy.modularui:modularui-mc1.21.1`, LGPL-3.0, source at
https://github.com/GregTechCEu/ModularUI-Modern). Its latest build, `3.3.1-SNAPSHOT` (2026-10-13, branch `1.21.1`,
commit `d68ef357`), was compiled against JEI 19.25.1.328, and its JEI integration does not fit JEI 19.64, which
Fossil Fuel ships: `RecipeGuiLogicMixin` finds no `recipeHeight` to modify, so JEI fails to start and the first screen
a player opens disconnects or crashes the client, and `RecipeSlotAccessor` names two `RecipeSlot` fields JEI 19.64
no longer has.

`maven/` is a one-artifact Maven repository holding `3.3.1-ff.1`: the `3.3.1-SNAPSHOT` jar with its 23 JEI classes
(`brachy/modularui/core/mixins/jei`, `brachy/modularui/integration/jei`) recompiled against JEI 19.64.0.455 after
applying `modularui-3.3.1-ff.1.diff`, its version set to `3.3.1-ff.1` and its optional JEI dependency to
`[19.64.0.455,)`. The patch, against `d68ef357`:

- `RecipeGuiLogicMixin` modifies JEI 19.64's `recipeSizeWithButtons` (an `ImmutableSize2i`) instead of `recipeHeight`,
  keeping its width and using the category's maximum height, as before.
- `RecipeSlotAccessor` replaces a slot's `RecipeSlotIngredients` (JEI 19.64 keeps all and displayed ingredients in
  one object) and invokes `onDisplayOverridesChanged` to clear the slot's caches.
- `JeiRecipeViewerSlot` builds the output tooltip callback with JEI 19.64's constructor (with the source recipe ids).
- `JeiScreenHandler.ContainerScreen` overrides the factory form of `getClickableIngredientUnderMouse`, which
  `IGuiContainerHandler` and `IScreenHandler` both default in JEI 19.64.

To rebuild: check out `d68ef357`, apply the patch, set `jei = "19.64.0.455"` in `gradle/forge.versions.toml` and
`mod_version = 3.3.1-ff.1`, and build; or compile the two packages with javac against JEI 19.64.0.455 and the
`3.3.1-SNAPSHOT` jar (Lombok as the annotation processor) and replace the classes in that jar.
