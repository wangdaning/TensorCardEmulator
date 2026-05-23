package ru.extreames.tensorcardemulator.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

import ru.extreames.tensorcardemulator.R;
import ru.extreames.tensorcardemulator.model.SavedCard;

public class SavedCardsAdapter extends RecyclerView.Adapter<SavedCardsAdapter.ViewHolder> {

    public interface Listener {
        void onSimulate(SavedCard card);
        void onDelete(SavedCard card);
        void onRename(SavedCard card);
    }

    private final List<SavedCard> cards = new ArrayList<>();
    private int activeCardId = -1;
    private final Listener listener;

    public SavedCardsAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setCards(List<SavedCard> newCards) {
        cards.clear();
        cards.addAll(newCards);
        notifyDataSetChanged();
    }

    public void setActiveCardId(int id) {
        this.activeCardId = id;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_saved_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SavedCard card = cards.get(position);
        boolean isActive = card.id == activeCardId;

        holder.cardName.setText(card.name);
        holder.cardUid.setText(card.uid);
        holder.activeBadge.setVisibility(isActive ? View.VISIBLE : View.GONE);

        holder.btnSimulate.setText(isActive ? R.string.RESTORE : R.string.SIMULATE);
        holder.btnSimulate.setOnClickListener(v -> listener.onSimulate(card));
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(card));
        holder.cardName.setOnLongClickListener(v -> {
            listener.onRename(card);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return cards.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView cardName, cardUid, activeBadge;
        MaterialButton btnSimulate, btnDelete;

        ViewHolder(View view) {
            super(view);
            cardName = view.findViewById(R.id.cardName);
            cardUid = view.findViewById(R.id.cardUid);
            activeBadge = view.findViewById(R.id.activeBadge);
            btnSimulate = view.findViewById(R.id.btnSimulate);
            btnDelete = view.findViewById(R.id.btnDelete);
        }
    }
}