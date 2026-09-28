package com.example.smartpantrymanager;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.smartpantrymanager.models.PantryItem;
import com.example.smartpantrymanager.models.Recipe;
import com.example.smartpantrymanager.models.RecipeIngredient;

import java.util.ArrayList;
import java.util.List;

/**
 * Single SQLiteOpenHelper for the whole app. Handles schema creation, seeding the
 * recipe collection on first run, and all CRUD operations for pantry items and recipes.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "smart_pantry.db";
    private static final int DB_VERSION = 1;

    public static final String TABLE_PANTRY = "pantry";
    public static final String TABLE_RECIPES = "recipes";
    public static final String TABLE_RECIPE_INGREDIENTS = "recipe_ingredients";

    private static DatabaseHelper instance;

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_PANTRY + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "quantity REAL NOT NULL, " +
                "unit TEXT NOT NULL, " +
                "expiry_date TEXT)");

        db.execSQL("CREATE TABLE " + TABLE_RECIPES + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "steps TEXT NOT NULL)");

        db.execSQL("CREATE TABLE " + TABLE_RECIPE_INGREDIENTS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "recipe_id INTEGER NOT NULL, " +
                "ingredient_name TEXT NOT NULL, " +
                "quantity REAL NOT NULL, " +
                "unit TEXT NOT NULL, " +
                "FOREIGN KEY(recipe_id) REFERENCES " + TABLE_RECIPES + "(id))");

        seedRecipes(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECIPE_INGREDIENTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECIPES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PANTRY);
        onCreate(db);
    }

    // =========================================================================
    // PANTRY CRUD
    // =========================================================================

    public long addPantryItem(PantryItem item) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", item.getName());
        cv.put("quantity", item.getQuantity());
        cv.put("unit", item.getUnit());
        cv.put("expiry_date", item.getExpiryDate());
        return db.insert(TABLE_PANTRY, null, cv);
    }

    public int updatePantryItem(PantryItem item) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", item.getName());
        cv.put("quantity", item.getQuantity());
        cv.put("unit", item.getUnit());
        cv.put("expiry_date", item.getExpiryDate());
        return db.update(TABLE_PANTRY, cv, "id=?", new String[]{String.valueOf(item.getId())});
    }

    public void deletePantryItem(long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_PANTRY, "id=?", new String[]{String.valueOf(id)});
    }

    public PantryItem getPantryItem(long id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_PANTRY, null, "id=?", new String[]{String.valueOf(id)}, null, null, null);
        PantryItem item = null;
        if (c != null) {
            if (c.moveToFirst()) {
                item = pantryItemFromCursor(c);
            }
            c.close();
        }
        return item;
    }

    public List<PantryItem> getAllPantryItems() {
        List<PantryItem> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_PANTRY, null, null, null, null, null, "name COLLATE NOCASE ASC");
        if (c != null) {
            while (c.moveToNext()) {
                list.add(pantryItemFromCursor(c));
            }
            c.close();
        }
        return list;
    }

    private PantryItem pantryItemFromCursor(Cursor c) {
        return new PantryItem(
                c.getLong(c.getColumnIndexOrThrow("id")),
                c.getString(c.getColumnIndexOrThrow("name")),
                c.getDouble(c.getColumnIndexOrThrow("quantity")),
                c.getString(c.getColumnIndexOrThrow("unit")),
                c.getString(c.getColumnIndexOrThrow("expiry_date"))
        );
    }

    // =========================================================================
    // RECIPE READS (recipes are seeded once; no create/edit UI needed for them)
    // =========================================================================

    public List<Recipe> getAllRecipesWithIngredients() {
        List<Recipe> recipes = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_RECIPES, null, null, null, null, null, "name COLLATE NOCASE ASC");
        if (c != null) {
            while (c.moveToNext()) {
                Recipe recipe = new Recipe(
                        c.getLong(c.getColumnIndexOrThrow("id")),
                        c.getString(c.getColumnIndexOrThrow("name")),
                        c.getString(c.getColumnIndexOrThrow("steps"))
                );
                recipes.add(recipe);
            }
            c.close();
        }

        // Fallback: if database was initialized without recipes, auto-seed now.
        if (recipes.isEmpty()) {
            SQLiteDatabase writableDb = getWritableDatabase();
            seedRecipes(writableDb);
            c = db.query(TABLE_RECIPES, null, null, null, null, null, "name COLLATE NOCASE ASC");
            if (c != null) {
                while (c.moveToNext()) {
                    Recipe recipe = new Recipe(
                            c.getLong(c.getColumnIndexOrThrow("id")),
                            c.getString(c.getColumnIndexOrThrow("name")),
                            c.getString(c.getColumnIndexOrThrow("steps"))
                    );
                    recipes.add(recipe);
                }
                c.close();
            }
        }

        for (Recipe recipe : recipes) {
            recipe.setIngredients(getIngredientsForRecipe(recipe.getId()));
        }

        return recipes;
    }

    public Recipe getRecipeById(long recipeId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_RECIPES, null, "id=?", new String[]{String.valueOf(recipeId)}, null, null, null);
        Recipe recipe = null;
        if (c != null) {
            if (c.moveToFirst()) {
                recipe = new Recipe(
                        c.getLong(c.getColumnIndexOrThrow("id")),
                        c.getString(c.getColumnIndexOrThrow("name")),
                        c.getString(c.getColumnIndexOrThrow("steps"))
                );
                recipe.setIngredients(getIngredientsForRecipe(recipeId));
            }
            c.close();
        }
        return recipe;
    }

    private List<RecipeIngredient> getIngredientsForRecipe(long recipeId) {
        List<RecipeIngredient> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_RECIPE_INGREDIENTS, null, "recipe_id=?",
                new String[]{String.valueOf(recipeId)}, null, null, "ingredient_name ASC");
        if (c != null) {
            while (c.moveToNext()) {
                list.add(new RecipeIngredient(
                        c.getLong(c.getColumnIndexOrThrow("id")),
                        c.getLong(c.getColumnIndexOrThrow("recipe_id")),
                        c.getString(c.getColumnIndexOrThrow("ingredient_name")),
                        c.getDouble(c.getColumnIndexOrThrow("quantity")),
                        c.getString(c.getColumnIndexOrThrow("unit"))
                ));
            }
            c.close();
        }
        return list;
    }

    // =========================================================================
    // SEED DATA - 18 recipes, pre-loaded on first run
    // =========================================================================

    private void seedRecipes(SQLiteDatabase db) {
        seedRecipe(db, "Scrambled Eggs on Toast",
                "1. Whisk the eggs with a splash of milk.\n2. Melt butter in a pan and cook eggs on low heat, stirring gently.\n3. Toast the bread.\n4. Serve eggs on top of the toast, season with salt and pepper.",
                new Object[][]{{"egg", 2, "unit"}, {"bread", 2, "unit"}, {"butter", 10, "g"}, {"milk", 20, "ml"}});

        seedRecipe(db, "Tomato Pasta",
                "1. Boil pasta until al dente.\n2. Sauté garlic and onion in olive oil.\n3. Add chopped tomatoes and simmer 10 minutes.\n4. Toss the pasta through the sauce and serve.",
                new Object[][]{{"pasta", 200, "g"}, {"tomato", 300, "g"}, {"garlic", 2, "unit"}, {"onion", 1, "unit"}, {"olive oil", 15, "ml"}});

        seedRecipe(db, "Grilled Cheese Sandwich",
                "1. Butter the outside of two slices of bread.\n2. Place cheese between the unbuttered sides.\n3. Grill in a pan until golden on both sides.",
                new Object[][]{{"bread", 2, "unit"}, {"cheese", 60, "g"}, {"butter", 10, "g"}});

        seedRecipe(db, "Vegetable Stir Fry",
                "1. Heat oil in a wok.\n2. Add chopped carrot, onion, and pepper, stir fry 5 minutes.\n3. Add soy sauce and cook 2 more minutes.\n4. Serve with rice.",
                new Object[][]{{"carrot", 100, "g"}, {"onion", 1, "unit"}, {"pepper", 1, "unit"}, {"soy sauce", 15, "ml"}, {"rice", 150, "g"}});

        seedRecipe(db, "Chicken Rice Bowl",
                "1. Cook rice according to package instructions.\n2. Season and pan-fry chicken until cooked through.\n3. Slice chicken and serve over rice with a drizzle of soy sauce.",
                new Object[][]{{"chicken", 200, "g"}, {"rice", 150, "g"}, {"soy sauce", 10, "ml"}});

        seedRecipe(db, "Simple Omelette",
                "1. Whisk eggs with salt and pepper.\n2. Add chopped onion and cheese.\n3. Cook in a buttered pan, folding once set.",
                new Object[][]{{"egg", 3, "unit"}, {"cheese", 40, "g"}, {"onion", 1, "unit"}, {"butter", 10, "g"}});

        seedRecipe(db, "Garlic Butter Rice",
                "1. Melt butter in a pot and fry garlic until fragrant.\n2. Add rice and stock, bring to a boil.\n3. Cover and simmer until rice is tender.",
                new Object[][]{{"rice", 200, "g"}, {"garlic", 3, "unit"}, {"butter", 20, "g"}});

        seedRecipe(db, "Tomato and Onion Salad",
                "1. Slice tomato and onion thinly.\n2. Toss with olive oil, salt, and pepper.",
                new Object[][]{{"tomato", 2, "unit"}, {"onion", 1, "unit"}, {"olive oil", 15, "ml"}});

        seedRecipe(db, "Cheesy Baked Potato",
                "1. Bake or microwave the potato until soft.\n2. Cut open and top with butter and grated cheese.",
                new Object[][]{{"potato", 1, "unit"}, {"cheese", 50, "g"}, {"butter", 10, "g"}});

        seedRecipe(db, "Banana Pancakes",
                "1. Mash the banana and mix with flour, milk, and egg into a batter.\n2. Fry spoonfuls of batter in a buttered pan until golden on both sides.",
                new Object[][]{{"banana", 1, "unit"}, {"flour", 100, "g"}, {"milk", 100, "ml"}, {"egg", 1, "unit"}, {"butter", 10, "g"}});

        seedRecipe(db, "Carrot and Potato Soup",
                "1. Sauté onion until soft.\n2. Add chopped carrot and potato with stock, simmer until tender.\n3. Blend until smooth.",
                new Object[][]{{"carrot", 150, "g"}, {"potato", 2, "unit"}, {"onion", 1, "unit"}});

        seedRecipe(db, "Peanut Butter Toast",
                "1. Toast the bread.\n2. Spread peanut butter generously and top with sliced banana.",
                new Object[][]{{"bread", 2, "unit"}, {"peanut butter", 30, "g"}, {"banana", 1, "unit"}});

        seedRecipe(db, "Fried Rice",
                "1. Heat oil in a pan or wok.\n2. Add egg and scramble.\n3. Add cooked rice, onion, and soy sauce, stir fry until heated through.",
                new Object[][]{{"rice", 200, "g"}, {"egg", 2, "unit"}, {"onion", 1, "unit"}, {"soy sauce", 15, "ml"}});

        seedRecipe(db, "Chicken and Vegetable Soup",
                "1. Sauté onion and carrot.\n2. Add chicken and stock, simmer until chicken is cooked.\n3. Season to taste.",
                new Object[][]{{"chicken", 150, "g"}, {"carrot", 100, "g"}, {"onion", 1, "unit"}});

        seedRecipe(db, "Cheese and Tomato Toastie",
                "1. Layer cheese and sliced tomato between bread.\n2. Grill or pan-fry until golden and the cheese melts.",
                new Object[][]{{"bread", 2, "unit"}, {"cheese", 50, "g"}, {"tomato", 1, "unit"}});

        seedRecipe(db, "Garlic Mashed Potatoes",
                "1. Boil the potatoes until soft.\n2. Mash with butter, milk, and crushed garlic.\n3. Season with salt and pepper.",
                new Object[][]{{"potato", 3, "unit"}, {"garlic", 2, "unit"}, {"butter", 20, "g"}, {"milk", 50, "ml"}});

        seedRecipe(db, "Pepper and Onion Pasta",
                "1. Boil pasta until al dente.\n2. Sauté sliced pepper and onion in olive oil until soft.\n3. Toss through the pasta with cheese.",
                new Object[][]{{"pasta", 200, "g"}, {"pepper", 1, "unit"}, {"onion", 1, "unit"}, {"olive oil", 15, "ml"}, {"cheese", 30, "g"}});

        seedRecipe(db, "Egg Fried Rice with Carrot",
                "1. Scramble eggs in a hot pan, then set aside.\n2. Stir fry carrot and onion.\n3. Add rice and egg back in, season with soy sauce.",
                new Object[][]{{"rice", 200, "g"}, {"egg", 2, "unit"}, {"carrot", 80, "g"}, {"onion", 1, "unit"}, {"soy sauce", 15, "ml"}});
    }

    private void seedRecipe(SQLiteDatabase db, String name, String steps, Object[][] ingredients) {
        ContentValues recipeCv = new ContentValues();
        recipeCv.put("name", name);
        recipeCv.put("steps", steps);
        long recipeId = db.insert(TABLE_RECIPES, null, recipeCv);

        for (Object[] ing : ingredients) {
            ContentValues cv = new ContentValues();
            cv.put("recipe_id", recipeId);
            cv.put("ingredient_name", (String) ing[0]);
            cv.put("quantity", ((Number) ing[1]).doubleValue());
            cv.put("unit", (String) ing[2]);
            db.insert(TABLE_RECIPE_INGREDIENTS, null, cv);
        }
    }
}
