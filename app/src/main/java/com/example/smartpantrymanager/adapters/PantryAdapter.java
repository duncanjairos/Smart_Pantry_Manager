package com.example.smartpantrymanager.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartpantrymanager.R;
import com.example.smartpantrymanager.models.PantryItem;

import java.util.List;
import java.util.Locale;

/**
 * Binds the list of PantryItem objects to the pantry RecyclerView.
 */
public class PantryAdapter extends RecyclerView.Adapter<PantryAdapter.ViewHolder> {

    public interface Listener {
        void onItemClicked(PantryItem item);
        void onDeleteClicked(PantryItem item);
    }

    private List<PantryItem> items;
    private final Listener listener;

    public PantryAdapter(List<PantryItem> items, Listener listener) {
        this.items = items;
        this.listener = listener;
    }

    public void updateData(List<PantryItem> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pantry, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PantryItem item = items.get(position);
        holder.name.setText(item.getName());

        String qty = formatQuantity(item.getQuantity()) + " " + item.getUnit();
        holder.quantity.setText(qty);

        if (item.getExpiryDate() != null && !item.getExpiryDate().trim().isEmpty()) {
            holder.expiry.setVisibility(View.VISIBLE);
            holder.expiry.setText("Expires: " + item.getExpiryDate());
        } else {
            holder.expiry.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> listener.onItemClicked(item));
        holder.deleteButton.setOnClickListener(v -> listener.onDeleteClicked(item));
    }

    private String formatQuantity(double value) {
        if (value == Math.floor(value)) {
            return String.format(Locale.US, "%.0f", value);
        }
        return String.format(Locale.US, "%.2f", value);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, quantity, expiry;
        View deleteButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.tvIngredientName);
            quantity = itemView.findViewById(R.id.tvIngredientQuantity);
            expiry = itemView.findViewById(R.id.tvIngredientExpiry);
            deleteButton = itemView.findViewById(R.id.btnDeleteItem);
        }
    }
}
