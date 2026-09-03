# Almost Fluidified / MoreGates 4.3.2 compatibility

MoreGates 4.3.2 contains 30 `sgjourney:crystallizing` recipes written for an
older SGJourney recipe schema. They omit `input_fluid`, so current SGJourney
falls back internally to 100 mB of `sgjourney:liquid_naquadah`. Because the
fluid is absent from the JSON, Almost Fluidified has no value to transform.

This compatibility integration writes the same 30 recipe IDs into Atlantis DHD's
generated top-priority compatibility pack with their original ingredients and
outputs unchanged, adding the explicit fluid input:

```json
"input_fluid": {
  "id": "sgjourney:liquid_naquadah",
  "amount": 100
}
```

Almost Fluidified 0.1.8 already transforms the `input_fluid.id` field for
SGJourney recipe types. Therefore:

- If Naquadah is not configured for fluid unification, the recipes continue to
  use SGJourney Liquid Naquadah exactly as before.
- If Naquadah is configured and GregTech is preferred, Almost Fluidified can
  replace the exposed input with GregTech's selected Naquadah fluid.
- If MoreGates is absent, a top-level Forge `mod_loaded` condition prevents
  these recipes from being registered. The `sgjourney:crystallizing` type is
  intentionally kept at the top level so Almost Fluidified can transform it.

No Almost Fluidified or GregTech dependency is added to Atlantis DHD.
This integration is part of the locked `2.0.3-beta.3` baseline.
