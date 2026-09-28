package com.example.smartpantrymanager.utils;

import com.example.smartpantrymanager.models.PantryItem;
import com.example.smartpantrymanager.models.Recipe;
import com.example.smartpantrymanager.models.RecipeIngredient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Contains all the "strict matching" business logic described in the assignment brief.
 *
 * A recipe is only "makeable" if EVERY required ingredient is present in the pantry in at
 * least the required quantity. To avoid trivial failures caused by real-world messiness
 * (plurals, unit synonyms, different-but-comparable units) this class:
 *   1. Normalises ingredient names (lower-case, trims, singularisation).
 *   2. Normalises units into one of three base "dimensions": WEIGHT (grams), VOLUME
 *      (millilitres) or COUNT (unitless items such as "clove" or "piece").
 *   3. Converts both pantry and recipe quantities into their base dimension before
 *      comparing, so "1 kg" of flour correctly satisfies a recipe that needs "500 g".
 */
public class MatchingUtils {

    // ---- Unit normalisation -------------------------------------------------

    private enum Dimension { WEIGHT, VOLUME, COUNT }

    // Maps a raw unit string (already lower-cased) to its canonical unit code.
    private static final Map<String, String> UNIT_SYNONYMS = new HashMap<>();
    // Maps a canonical unit code to (dimension, factor to base unit).
    private static final Map<String, Dimension> UNIT_DIMENSION = new HashMap<>();
    private static final Map<String, Double> UNIT_TO_BASE_FACTOR = new HashMap<>();

    static {
        // Weight -> base unit = grams
        addUnit("g", Dimension.WEIGHT, 1.0, "g", "gram", "grams", "gr");
        addUnit("kg", Dimension.WEIGHT, 1000.0, "kg", "kilogram", "kilograms", "kilo", "kilos");
        addUnit("oz", Dimension.WEIGHT, 28.3495, "oz", "ounce", "ounces");
        addUnit("lb", Dimension.WEIGHT, 453.592, "lb", "lbs", "pound", "pounds");

        // Volume -> base unit = millilitres
        addUnit("ml", Dimension.VOLUME, 1.0, "ml", "milliliter", "milliliters", "millilitre", "millilitres");
        addUnit("l", Dimension.VOLUME, 1000.0, "l", "liter", "liters", "litre", "litres");
        addUnit("tsp", Dimension.VOLUME, 4.929, "tsp", "teaspoon", "teaspoons");
        addUnit("tbsp", Dimension.VOLUME, 14.787, "tbsp", "tablespoon", "tablespoons");
        addUnit("cup", Dimension.VOLUME, 236.588, "cup", "cups");
        addUnit("fl_oz", Dimension.VOLUME, 29.5735, "fl_oz", "fl oz", "floz", "fluid ounce", "fluid ounces");

        // Count -> base unit = 1 item (used for "2 eggs", "3 cloves garlic", "1 onion" etc.)
        addUnit("unit", Dimension.COUNT, 1.0, "unit", "units", "piece", "pieces", "pc", "pcs",
                "clove", "cloves", "item", "items", "", "whole", "can", "cans", "slice", "slices",
                "pinch", "pinches", "head", "heads", "stalk", "stalks", "bunch", "bunches",
                "container", "containers", "pack", "packs", "package", "packages", "bag", "bags",
                "bottle", "bottles", "box", "boxes");
    }

    private static void addUnit(String canonical, Dimension dim, double factor, String... synonyms) {
        UNIT_DIMENSION.put(canonical, dim);
        UNIT_TO_BASE_FACTOR.put(canonical, factor);
        for (String s : synonyms) {
            UNIT_SYNONYMS.put(s, canonical);
        }
    }

    /** Converts a raw unit string (any case/spacing) into its canonical unit code. */
    public static String canonicalUnit(String rawUnit) {
        if (rawUnit == null) return "unit";
        String cleaned = rawUnit.trim().toLowerCase(Locale.US);
        String canonical = UNIT_SYNONYMS.get(cleaned);
        return canonical != null ? canonical : "unit"; // unknown units fall back to COUNT
    }

    /** Converts an amount expressed in rawUnit into its base-dimension value (grams/ml/count). */
    private static double toBaseValue(double amount, String rawUnit) {
        String canonical = canonicalUnit(rawUnit);
        Double factor = UNIT_TO_BASE_FACTOR.get(canonical);
        return amount * (factor != null ? factor : 1.0);
    }

    private static Dimension dimensionOf(String rawUnit) {
        String canonical = canonicalUnit(rawUnit);
        Dimension d = UNIT_DIMENSION.get(canonical);
        return d != null ? d : Dimension.COUNT;
    }

    // ---- Ingredient name normalisation ---------------------------------------

    /**
     * Normalisation: lower-case, trim, collapse whitespace,
     * and handle common English plural forms ("tomatoes" -> "tomato", "eggs" -> "egg")
     * without breaking words that naturally end in 's' or 'e' (e.g. "cheese", "glass").
     */
    public static String normalizeName(String rawName) {
        if (rawName == null) return "";
        String s = rawName.trim().toLowerCase(Locale.US).replaceAll("\\s+", " ");
        if (s.endsWith("ies") && s.length() > 3) {
            s = s.substring(0, s.length() - 3) + "y"; // "berries" -> "berry"
        } else if (s.endsWith("oes") && s.length() > 3) {
            s = s.substring(0, s.length() - 2); // "tomatoes" -> "tomato", "potatoes" -> "potato"
        } else if ((s.endsWith("ches") || s.endsWith("shes") || s.endsWith("xes") || s.endsWith("zes")) && s.length() > 3) {
            s = s.substring(0, s.length() - 2); // "peaches" -> "peach", "radishes" -> "radish"
        } else if (s.endsWith("s") && !s.endsWith("ss") && !s.endsWith("is") && !s.endsWith("us") && s.length() > 1) {
            s = s.substring(0, s.length() - 1); // "eggs" -> "egg", "cheeses" -> "cheese", "carrots" -> "carrot"
        }
        return s;
    }

    // ---- Core strict-matching logic -------------------------------------------

    /**
     * Result of checking a single recipe against the pantry.
     */
    public static class MatchResult {
        public final boolean fullMatch;
        public final List<String> missingIngredients;

        public MatchResult(boolean fullMatch, List<String> missingIngredients) {
            this.fullMatch = fullMatch;
            this.missingIngredients = missingIngredients;
        }
    }

    /**
     * Checks whether every ingredient required by the recipe is available in the pantry
     * in at least the required quantity, after name and unit normalisation.
     */
    public static MatchResult checkRecipe(Recipe recipe, List<PantryItem> pantry) {
        // Build a lookup of normalised pantry name -> total base-unit quantity available
        // and normalised pantry name -> primary dimension.
        Map<String, Double> pantryByNameBase = new HashMap<>();
        Map<String, Dimension> pantryDimByName = new HashMap<>();

        for (PantryItem item : pantry) {
            String key = normalizeName(item.getName());
            if (key.isEmpty()) continue;

            double baseQty = toBaseValue(item.getQuantity(), item.getUnit());
            pantryByNameBase.merge(key, baseQty, Double::sum);
            pantryDimByName.put(key, dimensionOf(item.getUnit()));
        }

        List<String> missing = new ArrayList<>();

        for (RecipeIngredient req : recipe.getIngredients()) {
            String key = normalizeName(req.getIngredientName());
            Double availableBase = pantryByNameBase.get(key);

            if (availableBase == null || availableBase <= 0) {
                missing.add(req.getIngredientName());
                continue;
            }

            Dimension reqDim = dimensionOf(req.getUnit());
            Dimension pantryDim = pantryDimByName.get(key);

            if (reqDim == pantryDim) {
                double neededBase = toBaseValue(req.getQuantity(), req.getUnit());
                if (availableBase < neededBase) {
                    missing.add(req.getIngredientName());
                }
            }
        }

        return new MatchResult(missing.isEmpty(), missing);
    }

    /**
     * Splits recipes into three buckets:
     *  - suggested: every ingredient satisfied (the strict list)
     *  - almostThere: exactly one ingredient missing (optional bonus list)
     *  - notMatched: everything else (not shown / ignored by the UI)
     */
    public static void classifyRecipes(List<Recipe> allRecipes, List<PantryItem> pantry,
                                        List<Recipe> suggestedOut, List<Recipe> almostThereOut) {
        for (Recipe recipe : allRecipes) {
            MatchResult result = checkRecipe(recipe, pantry);
            if (result.fullMatch) {
                recipe.setMissingCount(0);
                suggestedOut.add(recipe);
            } else if (result.missingIngredients.size() == 1) {
                recipe.setMissingCount(1);
                recipe.setMissingIngredientNames(result.missingIngredients);
                almostThereOut.add(recipe);
            }
        }
    }
}
