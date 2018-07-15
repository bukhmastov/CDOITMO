package com.bukhmastov.cdoitmo.adapter.rva;

import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bukhmastov.cdoitmo.util.Log;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public abstract class RVA extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    protected final ArrayList<Item> dataset = new ArrayList<>();
    protected class Item {
        protected int type;
        protected JSONObject data;
        protected Map<String, Object> extras = new HashMap<>();
        protected Item(int type, JSONObject data) {
            this.type = type;
            this.data = data;
        }
    }
    protected class ViewHolder extends RecyclerView.ViewHolder {
        protected final View container;
        protected ViewHolder(@NonNull View container) {
            super(container);
            this.container = container;
        }
    }

    //@Inject
    protected Log log = Log.instance();

    @Override
    public int getItemCount() {
        return dataset.size();
    }

    @Override
    public int getItemViewType(int position) {
        return dataset.get(position).type;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        try {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(onGetLayout(viewType), parent, false));
        } catch (NullPointerException e) {
            return null;
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        onBind(((ViewHolder) holder).container, dataset.get(position));
    }

    protected abstract @LayoutRes int onGetLayout(int type) throws NullPointerException;
    protected abstract void onBind(View container, Item item);
    protected Item getNewItem(int type, JSONObject data) {
        return new Item(type, data);
    }

    protected final ArrayMap<Integer, OnElementClickListener> onElementClickListeners = new ArrayMap<>();
    public interface OnElementClickListener {
        void onClick(View v, Map<String, Object> data);
    }
    public void setOnElementClickListener(@IdRes int layout, @NonNull OnElementClickListener onElementClickListener) {
        onElementClickListeners.put(layout, onElementClickListener);
    }
    protected Map<String, Object> getMap(@NonNull String key, @Nullable Object value) {
        Map<String, Object> data = new HashMap<>();
        data.put(key, value);
        return data;
    }

    public void addItem(@NonNull Item item) {
        this.dataset.add(item);
        this.notifyItemInserted(this.dataset.size() - 1);
    }
    public void addItems(@NonNull ArrayList<Item> dataset) {
        int itemStart = this.dataset.size() - 1;
        this.dataset.addAll(dataset);
        this.notifyItemRangeInserted(itemStart, dataset.size() - 1);
    }
    public void removeItem(int position) {
        this.dataset.remove(position);
        this.notifyItemRemoved(position);
        this.notifyItemRangeChanged(position, this.dataset.size() - 1);
    }
}
