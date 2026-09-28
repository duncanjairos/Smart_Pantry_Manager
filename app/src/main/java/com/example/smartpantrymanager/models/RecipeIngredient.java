package com.example.smartpantrymanager.models;

/**
 * A single ingredient requirement inside a recipe (e.g. "200 g flour").
 */
public class RecipeIngredient {

    private long id;
    private long recipeId;
    private String ingredientName;
    private double quantity;
    private String unit;

    public RecipeIngredient() {
    }

    public RecipeIngredient(long id, long recipeId, String ingredientName, double quantity, String unit) {
        this.id = id;
        this.recipeId = recipeId;
        this.ingredientName = ingredientName;
        this.quantity = quantity;
        this.unit = unit;
    }

    public long getId() {
        return id;
    }

    public long getRecipeId() {
        return recipeId;
    }

    public String getIngredientName() {
        return ingredientName;
    }

    public double getQuantity() {
        return quantity;
    }

    public String getUnit() {
        return unit;
    }
}
