package ru.extreames.tensorcardemulator.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.android.material.materialswitch.MaterialSwitch;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import java.util.Objects;

import ru.extreames.tensorcardemulator.R;
import ru.extreames.tensorcardemulator.model.SavedCard;

public class SavedCardsAdapter extends ListAdapter<SavedCard, SavedCardsAdapter.CardViewHolder> {

    public interface Listener {
        void onCardSelected(SavedCard card);
        void onCardDeselected(SavedCard card);
        void onDelete(SavedCard card);
        void onRename(SavedCard card);
    }

    private final Listener listener;
    private int selectedCardId = -1;

    private static final DiffUtil.ItemCallback<SavedCard> DIFF_CALLBACK = new DiffUtil.ItemCallback<SavedCard>() {
        @Override
        public boolean areItemsTheSame(@NonNull SavedCard oldItem, @NonNull SavedCard newItem) {
            return oldItem.id == newItem.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull SavedCard oldItem, @NonNull SavedCard newItem) {
            return Objects.equals(oldItem.name, newItem.name) && 
                   Objects.equals(oldItem.uid, newItem.uid);
        }
    };

    public SavedCardsAdapter(Listener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    public void setSelectedCardId(int id) {
        int previousSelectedId = this.selectedCardId;
        this.selectedCardId = id;
        
        for (int i = 0; i < getItemCount(); i++) {
            SavedCard item = getItem(i);
            if (item.id == previousSelectedId || item.id == selectedCardId) {
                notifyItemChanged(i);
            }
        }
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_saved_card, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        holder.bind(getItem(position), listener, selectedCardId);
    }

    static class CardViewHolder extends RecyclerView.ViewHolder {
        private final TextView cardNameText;
        private final TextView cardUidText;
        private final ImageView btnDelete;
        private final MaterialSwitch cardSwitch;
        private final View activeBadge;

        public CardViewHolder(@NonNull View itemView) {
            super(itemView);
            cardNameText = itemView.findViewById(R.id.cardName);
            cardUidText = itemView.findViewById(R.id.cardUid);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            cardSwitch = itemView.findViewById(R.id.cardSwitch);
            activeBadge = itemView.findViewById(R.id.activeBadge);
        }

        public void bind(final SavedCard card, final Listener listener, int selectedCardId) {
            cardNameText.setText(card.name);
            cardUidText.setText(card.uid);

            boolean isSelected = (card.id == selectedCardId);
            
            if (activeBadge != null) {
                activeBadge.setVisibility(isSelected ? View.VISIBLE : View.GONE);
            }
            
            if (cardSwitch != null) {
                cardSwitch.setChecked(isSelected);
                cardSwitch.setOnClickListener(v -> {
                    if (listener != null) {
                        if (isSelected) {
                            listener.onCardDeselected(card);
                        } else {
                            listener.onCardSelected(card);
                        }
                    }
                });
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    if (isSelected) {
                        listener.onCardDeselected(card);
                    } else {
                        listener.onCardSelected(card);
                    }
                }
            });

            if (btnDelete != null) {
                btnDelete.setOnClickListener(v -> {
                    if (listener != null) listener.onDelete(card);
                });
            }
        }
    }
}