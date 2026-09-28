# Smart Pantry Manager

Android app (Java) for Mobile App Development 700. Tracks pantry ingredients and
suggests recipes the user can make **right now**, using the strict-matching rule.

## How to open in Android Studio

1. Unzip this project.
2. Android Studio → **Open** → select the `SmartPantryManager` folder (the one
   containing `settings.gradle`).
3. Let Gradle sync (it will download AndroidX/Material dependencies — needs
   internet the first time).
4. Run on an emulator or device (minSdk 24 / Android 7.0+).

No API keys, Maps SDK, or extra setup needed — everything is local SQLite.

## What's implemented, mapped to the brief

- **4+ screens**: Pantry List (`PantryListActivity`, launcher), Add/Edit
  Ingredient, Suggested Recipes, Recipe Detail, Settings.
- **Navigation**: Bottom navigation bar (Pantry / Suggestions / Settings) +
  Intents with extras (`EXTRA_ITEM_ID`, `EXTRA_RECIPE_ID`) for the
  add/edit and detail flows.
- **RecyclerView + custom Adapter**: `PantryAdapter` (pantry list) and
  `RecipeAdapter` (suggested / almost-there lists).
- **Database (SQLite via SQLiteOpenHelper)**: `DatabaseHelper.java`. Full CRUD
  on the `pantry` table; 18 recipes auto-seeded into `recipes` +
  `recipe_ingredients` the first time the app runs. Data survives app restarts
  (verified by closing/reopening — SQLite file persists on disk).
- **Input validation**: `AddEditIngredientActivity.validateInputs()` — name
  required, quantity required/numeric/greater-than-zero, inline `setError()`
  messages.
- **Strict-matching rule (`MatchingUtils.java`)** — the core logic the brief
  asks you to be able to defend:
  - A recipe only appears in *Suggested* if **every** ingredient is present in
    at least the required quantity.
  - Ingredient names are normalised (lower-case, trimmed, naive pluralisation
    handling: "tomatoes"→"tomato", "eggs"→"egg", "berries"→"berry") so trivial
    string differences don't break matching.
  - Units are normalised into three dimensions — weight (→ grams), volume
    (→ ml), and count — with a conversion table, so "1 kg" correctly satisfies
    a recipe needing "500 g" of the same ingredient.
  - **Bonus**: an "Almost There" list shows recipes missing exactly one
    ingredient, kept clearly separate from the strict suggestions list (see
    `MatchingUtils.classifyRecipes`).
  - Zero-match feedback: "No recipes match your pantry yet - add more
    ingredients" instead of a blank screen.
- **No Maps/GPS**: none used anywhere, per the explicit restriction.

## Suggested things to test for your demo video

1. Add a few ingredients (e.g. 2 eggs, 200 g pasta, 300 g tomato, 1 onion,
   2 garlic, 15 ml olive oil) → open Suggestions → "Tomato Pasta" appears.
2. Remove/reduce one required ingredient below what a recipe needs → confirm
   it disappears from Suggested (and appears in Almost There if only one
   ingredient is short).
3. Edit an ingredient's quantity and unit (e.g. change 200 g to 0.1 kg) →
   confirm the recipe is still matched (proves unit conversion works).
4. Add "tomatoes" (plural) to the pantry and confirm it still matches a
   recipe that lists "tomato" (singular) — proves name normalisation.
5. Close and reopen the app → pantry data is still there (SQLite persistence).
6. Toggle a Settings option, restart the app, confirm it's remembered
   (SharedPreferences persistence).

## Project structure

```
app/src/main/java/com/example/smartpantrymanager/
  PantryListActivity.java          Screen 1 (launcher)
  AddEditIngredientActivity.java   Screen 2
  SuggestedRecipesActivity.java    Screen 3
  RecipeDetailActivity.java        Screen 4
  SettingsActivity.java            Screen 5
  DatabaseHelper.java              SQLite schema, CRUD, recipe seed data
  models/                          PantryItem, Recipe, RecipeIngredient
  adapters/                        PantryAdapter, RecipeAdapter
  utils/MatchingUtils.java         Strict-matching + normalisation logic
```
