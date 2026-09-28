package com.example.smartpantrymanager;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.smartpantrymanager.models.Recipe;
import com.example.smartpantrymanager.models.RecipeIngredient;

import java.util.Locale;

/**
 * Screen 4: Recipe Detail. Shows the full ingredient list and preparation steps
 * for a recipe passed in via Intent extra (correct use of Intents + data passing).
 */
public class RecipeDetailActivity extends AppCompatActivity {

    public static final String EXTRA_RECIPE_ID = "extra_recipe_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_detail);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        long recipeId = getIntent().getLongExtra(EXTRA_RECIPE_ID, -1);
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(this);
        Recipe recipe = dbHelper.getRecipeById(recipeId);

        if (recipe == null) {
            finish();
            return;
        }

        toolbar.setTitle(recipe.getName());

        TextView tvName = findViewById(R.id.tvRecipeName);
        TextView tvIngredients = findViewById(R.id.tvIngredients);
        TextView tvSteps = findViewById(R.id.tvSteps);

        tvName.setText(recipe.getName());

        StringBuilder ingredientsText = new StringBuilder();
        for (RecipeIngredient ingredient : recipe.getIngredients()) {
            ingredientsText.append("• ")
                    .append(formatQuantity(ingredient.getQuantity()))
                    .append(" ")
                    .append(ingredient.getUnit())
                    .append(" ")
                    .append(ingredient.getIngredientName())
                    .append("\n");
        }
        tvIngredients.setText(ingredientsText.toString().trim());

        tvSteps.setText(recipe.getSteps());
    }

    private String formatQuantity(double value) {
        if (value == Math.floor(value)) {
            return String.format(Locale.US, "%.0f", value);
        }
        return String.format(Locale.US, "%.2f", value);
    }
}
