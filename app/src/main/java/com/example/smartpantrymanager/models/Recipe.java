package com.example.smartpantrymanager.models;

import java.util.ArrayList;
import java.util.List;

/**
 * A recipe: its name, preparation steps, and the list of ingredients it requires.
 * The ingredients list is only populated when explicitly loaded (see DatabaseHelper).
 */
public class Recipe {

    private long id;
    private String name;
    private String steps;
    private List<RecipeIngredient> ingredients = new ArrayList<>();

    // Fields used only after strict-matching has run, for display purposes.
    private int missingCount = -1;
    private List<String> missingIngredientNames = new ArrayList<>();

    public Recipe() {
    }

    public Recipe(long id, String name, String steps) {
        this.id = id;
        this.name = name;
        this.steps = steps;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSteps() {
        return steps;
    }

    public void setSteps(String steps) {
        this.steps = steps;
    }

    public List<RecipeIngredient> getIngredients() {
        return ingredients;
    }

    public void setIngredients(List<RecipeIngredient> ingredients) {
        this.ingredients = ingredients;
    }

    public int getMissingCount() {
        return missingCount;
    }

    public void setMissingCount(int missingCount) {
        this.missingCount = missingCount;
    }

    public List<String> getMissingIngredientNames() {
        return missingIngredientNames;
    }

    public void setMissingIngredientNames(List<String> missingIngredientNames) {
        this.missingIngredientNames = missingIngredientNames;
    }
}
