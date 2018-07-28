package com.bukhmastov.cdoitmo.adapter.rva.university;

import android.content.Context;
import android.support.v4.util.ArrayMap;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.event.bus.EventBus;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Static;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import javax.inject.Inject;

public abstract class UniversityRVA extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int TYPE_INFO_ABOUT_UPDATE_TIME = 0;
    public static final int TYPE_MAIN = 1;
    public static final int TYPE_MINOR = 2;
    public static final int TYPE_STATE = 3;
    public static final int TYPE_UNIT_STRUCTURE_COMMON = 4;
    public static final int TYPE_UNIT_STRUCTURE_DEANERY = 5;
    public static final int TYPE_UNIT_STRUCTURE_HEAD = 6;
    public static final int TYPE_UNIT_DIVISIONS = 7;
    public static class Item {
        public int type;
        public JSONObject data;
        public int data_state_keep = -1;
        public Item () {}
        public Item (int type, JSONObject data) {
            this.type = type;
            this.data = data;
        }
    }
    protected static class ViewHolder extends RecyclerView.ViewHolder {
        protected final ViewGroup container;
        protected ViewHolder(ViewGroup container) {
            super(container);
            this.container = container;
        }
    }
    protected final Context context;
    protected ArrayList<Item> dataset = new ArrayList<>();

    @Inject
    Log log;
    @Inject
    EventBus eventBus;
    @Inject
    StoragePref storagePref;
    @Inject
    Static staticUtil;
    @Inject
    TextUtils textUtils;

    public UniversityRVA(final Context context) {
        this(context, null);
    }
    public UniversityRVA(final Context context, final ArrayList<Item> dataset) {
        AppComponentProvider.getComponent().inject(this);
        this.context = context;
        if (dataset != null) {
            this.dataset = new ArrayList<>();
            this.dataset = dataset;
        } else {
            this.dataset = new ArrayList<>();
        }
    }

    @Override
    public int getItemCount() {
        return dataset.size();
    }

    @Override
    public int getItemViewType(int position) {
        return dataset.get(position).type;
    }

    protected void bindInfoAboutUpdateTime(RecyclerView.ViewHolder holder, Item item) {
        try {
            ViewHolder viewHolder = (ViewHolder) holder;
            String title = getString(item.data, "title");
            if (title != null) {
                ((TextView) viewHolder.container.findViewById(R.id.update_time)).setText(title);
            }
        } catch (Exception e) {
            log.exception(e);
        }
    }

    protected final ArrayMap<Integer, View.OnClickListener> onStateClickListeners = new ArrayMap<>();
    public void setOnStateClickListener(int layout, View.OnClickListener onClickListener) {
        onStateClickListeners.put(layout, onClickListener);
    }
    public void addItem(Item item) {
        this.dataset.add(item);
        this.notifyItemInserted(this.dataset.size() - 1);
    }
    public void addItem(ArrayList<Item> dataset) {
        int itemStart = this.dataset.size() - 1;
        this.dataset.addAll(dataset);
        this.notifyItemRangeInserted(itemStart, dataset.size() - 1);
    }
    public void removeItem(int position) {
        this.dataset.remove(position);
        this.notifyItemRemoved(position);
        this.notifyItemRangeChanged(position, this.dataset.size() - 1);
    }
    public void removeState() {
        int position = -1;
        for (int i = dataset.size() - 1; i >= 0; i--) {
            if (dataset.get(i).type == TYPE_STATE) {
                position = i;
                break;
            }
        }
        if (position >= 0) {
            removeItem(position);
        }
    }
    public void setState(int type) {
        removeState();
        Item item = new Item();
        item.type = TYPE_STATE;
        item.data_state_keep = type;
        addItem(item);
    }

    protected JSONObject getJsonObject(JSONObject json, String key) throws JSONException {
        if (json.has(key)) {
            Object object = json.get(key);
            if (object == null) {
                return null;
            } else {
                try {
                    return (JSONObject) object;
                } catch (Exception e) {
                    return null;
                }
            }
        } else {
            return null;
        }
    }
    protected JSONArray getJsonArray(JSONObject json, String key) throws JSONException {
        if (json.has(key)) {
            Object object = json.get(key);
            if (object == null) {
                return null;
            } else {
                try {
                    return (JSONArray) object;
                } catch (Exception e) {
                    return null;
                }
            }
        } else {
            return null;
        }
    }
    protected String getString(JSONObject json, String key) throws JSONException {
        if (json.has(key)) {
            Object object = json.get(key);
            if (object == null) {
                return null;
            } else {
                try {
                    return (String) object;
                } catch (Exception e) {
                    return null;
                }
            }
        } else {
            return null;
        }
    }
    protected int getInt(JSONObject json, String key) {
        if (json.has(key)) {
            try {
                return json.getInt(key);
            } catch (Exception e) {
                return -1;
            }
        } else {
            return -1;
        }
    }
}
