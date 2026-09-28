package com.example.smartpantrymanager.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartpantrymanager.R;
import com.example.smartpantrymanager.models.Recipe;

import java.util.List;

/**
 * Binds a list of Recipe objects (either the strict "suggested" list or the
 * bonus "almost there" list) to a RecyclerView.
 */
public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.ViewHolder> {

    public interface Listener {
        void onRecipeClicked(Recipe recipe);
    }

    private final List<Recipe> recipes;
    private final Listener listener;
    private final boolean showMissingSubtitle;

    public RecipeAdapter(List<Recipe> recipes, Listener listener, boolean showMissingSubtitle) {
        this.recipes = recipes;
        this.listener = listener;
        this.showMissingSubtitle = showMissingSubtitle;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recipe, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Recipe recipe = recipes.get(position);
        holder.name.setText(recipe.getName());

        if (showMissingSubtitle && !recipe.getMissingIngredientNames().isEmpty()) {
            holder.subtitle.setText("Missing: " + recipe.getMissingIngredientNames().get(0));
        } else {
            holder.subtitle.setText("You have everything for this recipe");
        }

        holder.itemView.setOnClickListener(v -> listener.onRecipeClicked(recipe));
    }

    @Override
    public int getItemCount() {
        return recipes.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, subtitle;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.tvRecipeName);
            subtitle = itemView.findViewById(R.id.tvRecipeSubtitle);
        }
    }
}
