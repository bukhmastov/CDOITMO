package com.bukhmastov.cdoitmo.adapter.rva;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.model.scholarship.paid.SSPaid;
import com.bukhmastov.cdoitmo.model.scholarship.paid.SSPaidList;
import com.bukhmastov.cdoitmo.util.Time;
import com.bukhmastov.cdoitmo.util.singleton.CollectionUtils;
import com.bukhmastov.cdoitmo.util.singleton.NumberUtils;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;

public class ScholarshipPaidRVA extends RVA<SSPaid> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_PAID_ITEM = 1;
    private static final int TYPE_NO_PAID_ITEMS = 2;

    protected final Context context;

    @Inject
    Time time;

    public ScholarshipPaidRVA(@NonNull Context context, @NonNull SSPaidList ssPaidList) {
        super();
        AppComponentProvider.getComponent().inject(this);
        this.context = context;
        addItems(entity2dataset(ssPaidList));
    }

    @Override
    protected int onGetLayout(int type) throws NullPointerException {
        @LayoutRes int layout;
        switch (type) {
            case TYPE_HEADER: layout = R.layout.layout_scholarship_paid_header; break;
            case TYPE_PAID_ITEM: layout = R.layout.layout_scholarship_paid_item; break;
            case TYPE_NO_PAID_ITEMS: layout = R.layout.state_nothing_to_display_compact; break;
            default: throw new NullPointerException("Invalid type provided");
        }
        return layout;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void onBind(View container, Item item) {
        switch (item.type) {
            case TYPE_HEADER: bindHeader(container); break;
            case TYPE_PAID_ITEM: bindPaidItem(container, item); break;
            case TYPE_NO_PAID_ITEMS: bindNoPaidItems(container); break;
        }
    }

    private void bindHeader(View container) {
        try {
            tryRegisterClickListener(container, R.id.scholarship_assigned_container, null);
        } catch (Exception e) {
            log.get().exception(e);
        }
    }

    private void bindPaidItem(View container, Item<SSPaid> item) {
        try {
            String title = time.getMonth(context, item.data.getMonth()) + " " + item.data.getYear();
            String info = context.getString(R.string.scholarship_paid_amount).replace("%amount%", item.data.getValue());
            try {
                String appendix;
                switch (StringUtils.getWordDeclinationByNumber(NumberUtils.toDoubleInteger(item.data.getValue()))) {
                    case 1: appendix = context.getString(R.string.ruble1); break;
                    case 2: appendix = context.getString(R.string.ruble2); break;
                    case 3: appendix = context.getString(R.string.ruble3); break;
                    default: appendix = ""; break;
                }
                info += " " + appendix.toLowerCase();
            } catch (Exception ignore) {
                // ignore
            }
            ((TextView) container.findViewById(R.id.date)).setText(title);
            ((TextView) container.findViewById(R.id.amount)).setText(info);
            tryRegisterClickListener(container, R.id.scholarship_paid_item, item.data);
        } catch (Exception e) {
            log.get().exception(e);
        }
    }

    private void bindNoPaidItems(View container) {
        try {
            ((TextView) container.findViewById(R.id.ntd_text)).setText(R.string.no_paid_scholarship);
        } catch (Exception e) {
            log.get().exception(e);
        }
    }

    private ArrayList<Item> entity2dataset(@NonNull SSPaidList ssPaidList) {
        final ArrayList<Item> dataset = new ArrayList<>();
        try {
            dataset.add(new Item(TYPE_HEADER));
            List<SSPaid> ssPaidArr = ssPaidList.getList();
            if (CollectionUtils.isEmpty(ssPaidArr)) {
                dataset.add(new Item(TYPE_NO_PAID_ITEMS));
                return dataset;
            }
            for (SSPaid ssPaid : ssPaidArr) {
                dataset.add(new Item<>(TYPE_PAID_ITEM, ssPaid));
            }
        } catch (Exception e) {
            log.get().exception(e);
        }
        return dataset;
    }
}
