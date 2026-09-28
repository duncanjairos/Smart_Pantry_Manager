package com.example.smartpantrymanager;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartpantrymanager.adapters.RecipeAdapter;
import com.example.smartpantrymanager.models.PantryItem;
import com.example.smartpantrymanager.models.Recipe;
import com.example.smartpantrymanager.utils.MatchingUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

/**
 * Screen 3: Suggested Recipes. Runs the strict-matching rule (see MatchingUtils)
 * against the current pantry and shows only recipes the user can make right now,
 * plus an optional "Almost There" bonus list for recipes missing exactly one item.
 */
public class SuggestedRecipesActivity extends AppCompatActivity implements RecipeAdapter.Listener {

    private DatabaseHelper dbHelper;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_suggested_recipes);

        dbHelper = DatabaseHelper.getInstance(this);

        bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_suggestions);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_pantry) {
                Intent intent = new Intent(this, PantryListActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_suggestions) {
                return true;
            } else if (id == R.id.nav_settings) {
                Intent intent = new Intent(this, SettingsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_suggestions);
        }
        runMatchingAndDisplay();
    }

    private void runMatchingAndDisplay() {
        List<PantryItem> pantry = dbHelper.getAllPantryItems();
        List<Recipe> allRecipes = dbHelper.getAllRecipesWithIngredients();

        List<Recipe> suggested = new ArrayList<>();
        List<Recipe> almostThere = new ArrayList<>();
        MatchingUtils.classifyRecipes(allRecipes, pantry, suggested, almostThere);

        RecyclerView rvSuggested = findViewById(R.id.rvSuggested);
        RecyclerView rvAlmostThere = findViewById(R.id.rvAlmostThere);
        TextView emptyState = findViewById(R.id.tvEmptyState);
        TextView suggestedHeader = findViewById(R.id.tvSuggestedHeader);
        TextView almostThereHeader = findViewById(R.id.tvAlmostThereHeader);

        rvSuggested.setLayoutManager(new LinearLayoutManager(this));
        rvAlmostThere.setLayoutManager(new LinearLayoutManager(this));

        if (suggested.isEmpty()) {
            if (suggestedHeader != null) suggestedHeader.setVisibility(View.GONE);
            rvSuggested.setVisibility(View.GONE);
        } else {
            if (suggestedHeader != null) suggestedHeader.setVisibility(View.VISIBLE);
            rvSuggested.setVisibility(View.VISIBLE);
            rvSuggested.setAdapter(new RecipeAdapter(suggested, this, false));
        }

        if (almostThere.isEmpty()) {
            almostThereHeader.setVisibility(View.GONE);
            rvAlmostThere.setVisibility(View.GONE);
        } else {
            almostThereHeader.setVisibility(View.VISIBLE);
            rvAlmostThere.setVisibility(View.VISIBLE);
            rvAlmostThere.setAdapter(new RecipeAdapter(almostThere, this, true));
        }

        if (suggested.isEmpty() && almostThere.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
        } else {
            emptyState.setVisibility(View.GONE);
        }
    }

    @Override
    public void onRecipeClicked(Recipe recipe) {
        Intent intent = new Intent(this, RecipeDetailActivity.class);
        intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE_ID, recipe.getId());
        startActivity(intent);
    }
}
