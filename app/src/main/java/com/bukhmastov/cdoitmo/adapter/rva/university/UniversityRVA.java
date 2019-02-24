package com.bukhmastov.cdoitmo.adapter.rva.university;

import android.content.Context;
import androidx.annotation.IdRes;
import android.view.View;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.adapter.rva.RVA;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.model.rva.RVASingleValue;
import com.bukhmastov.cdoitmo.model.rva.RVAUniversity;
import com.bukhmastov.cdoitmo.util.Static;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.TextUtils;

import java.util.Collection;

import javax.inject.Inject;

public abstract class UniversityRVA extends RVA<RVAUniversity> {

    public static final int TYPE_INFO_ABOUT_UPDATE_TIME = 0;
    public static final int TYPE_MAIN = 1;
    public static final int TYPE_MINOR = 2;
    public static final int TYPE_STATE = 3;
    public static final int TYPE_UNIT_STRUCTURE_COMMON = 4;
    public static final int TYPE_UNIT_STRUCTURE_DEANERY = 5;
    public static final int TYPE_UNIT_STRUCTURE_HEAD = 6;
    public static final int TYPE_UNIT_DIVISIONS = 7;
    public static final int TYPE_NO_DATA = 8;

    protected static final String DATA_STATE_KEEP = "dataStateKeep";
    protected final Context context;

    @Inject
    StoragePref storagePref;
    @Inject
    Static staticUtil;
    @Inject
    TextUtils textUtils;

    public UniversityRVA(Context context) {
        this(context, null);
    }

    public UniversityRVA(Context context, Collection<Item> dataset) {
        AppComponentProvider.getComponent().inject(this);
        this.context = context;
        if (dataset != null) {
            this.dataset.clear();
            this.dataset.addAll(dataset);
        }
    }

    @SuppressWarnings("unchecked")
    public void setState(@IdRes int type) {
        removeState();
        Item item = new Item(TYPE_STATE);
        item.type = TYPE_STATE;
        item.extras.put(DATA_STATE_KEEP, type);
        addItem(item);
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

    protected void bindInfoAboutUpdateTime(View container, Item<RVASingleValue> item) {
        try {
            ((TextView) container.findViewById(R.id.update_time)).setText(item.data.getValue());
        } catch (Exception e) {
            log.exception(e);
        }
    }
}
