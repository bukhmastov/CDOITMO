package com.bukhmastov.cdoitmo.adapter.rva;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bukhmastov.cdoitmo.model.JsonEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class RVA<J extends JsonEntity> extends RVABase {

    @FunctionalInterface
    public interface OnElementClickListener<J extends JsonEntity> {
        void onClick(View v, J entity);
    }

    public static class Item<T extends JsonEntity> {
        public int type;
        public T data;
        public Map<String, Object> extras = new HashMap<>();
        public Item(int type) {
            this.type = type;
        }
        public Item(int type, T data) {
            this.type = type;
            this.data = data;
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final View container;
        public ViewHolder(@NonNull View container) {
            super(container);
            this.container = container;
        }
    }

    protected final List<Item> dataset = new ArrayList<>();
    protected final Map<Integer, OnElementClickListener<J>> onElementClickListeners = new ArrayMap<>();

    public RVA() {
        super();
    }

    protected abstract @LayoutRes int onGetLayout(int type) throws NullPointerException;

    protected abstract void onBind(View container, Item item);


    @Override
    public int getItemCount() {
        return dataset.size();
    }

    @Override
    public int getItemViewType(int position) {
        return dataset.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        try {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(onGetLayout(viewType), parent, false));
        } catch (NullPointerException npe) {
            log.exception(npe);
            throw npe;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (!(holder instanceof ViewHolder)) {
            return;
        }
        if (position < 0 || position >= dataset.size()) {
            return;
        }
        onBind(((ViewHolder) holder).container, dataset.get(position));
    }


    public void setClickListener(@IdRes int layout, @NonNull OnElementClickListener<J> onElementClickListener) {
        onElementClickListeners.put(layout, onElementClickListener);
    }

    public void clearClickListeners() {
        onElementClickListeners.clear();
    }

    protected void tryRegisterClickListener(@NonNull View container, @IdRes int element, @Nullable J entity) {
        if (!onElementClickListeners.containsKey(element)) {
            return;
        }
        View view = container.findViewById(element);
        if (view == null) {
            return;
        }
        view.setOnClickListener(v -> {
            try {
                OnElementClickListener<J> listener = onElementClickListeners.get(element);
                if (listener != null) {
                    listener.onClick(v, entity);
                }
            } catch (Exception e) {
                log.exception(e);
            }
        });
    }


    public void addItem(@NonNull Item item) {
        dataset.add(item);
        notifyItemInserted(dataset.size() - 1);
    }

    public void addItems(@NonNull Collection<Item> items) {
        int itemStart = dataset.size() - 1;
        dataset.addAll(items);
        notifyItemRangeInserted(itemStart, items.size() - 1);
    }

    public void removeItem(int position) {
        dataset.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, dataset.size() - 1);
    }
}
