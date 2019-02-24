package com.bukhmastov.cdoitmo.adapter.rva;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.model.scholarship.assigned.SSAssigned;
import com.bukhmastov.cdoitmo.model.scholarship.assigned.SSAssignedList;
import com.bukhmastov.cdoitmo.util.singleton.CollectionUtils;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;

public class ScholarshipAssignedRVA extends RVA<SSAssigned> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ASSIGNED_ITEM = 1;
    private static final int TYPE_NO_ASSIGNED_ITEMS = 2;

    protected final Context context;

    public ScholarshipAssignedRVA(@NonNull Context context, @NonNull SSAssignedList ssAssignedList) {
        super();
        this.context = context;
        addItems(entity2dataset(ssAssignedList));
    }

    @Override
    protected int onGetLayout(int type) throws NullPointerException {
        @LayoutRes int layout;
        switch (type) {
            case TYPE_HEADER: layout = R.layout.layout_scholarship_assigned_header; break;
            case TYPE_ASSIGNED_ITEM: layout = R.layout.layout_scholarship_assigned_item; break;
            case TYPE_NO_ASSIGNED_ITEMS: layout = R.layout.state_nothing_to_display_compact; break;
            default: throw new NullPointerException("Invalid type provided");
        }
        return layout;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void onBind(View container, Item item) {
        switch (item.type) {
            case TYPE_HEADER: break;
            case TYPE_ASSIGNED_ITEM: bindAssignedItem(container, item); break;
            case TYPE_NO_ASSIGNED_ITEMS: bindNoAssignedItems(container); break;
        }
    }

    private void bindAssignedItem(View container, Item<SSAssigned> item) {
        try {
            String info = item.data.getContribution();
            String dates = item.data.getStart() + " — " + item.data.getEnd();
            String amount = item.data.getSum();
            if (StringUtils.isNotBlank(item.data.getType())) {
                info += " — " + item.data.getType();
            }
            if (StringUtils.isNotBlank(item.data.getSource())) {
                info += " (" + item.data.getSource() + ")";
            }
            try {
                int amountInt = (int) Double.parseDouble(item.data.getSum());
                String appendix;
                switch (amountInt % 100) {
                    case 10: case 11: case 12: case 13: case 14: appendix = context.getString(R.string.ruble3); break;
                    default:
                        switch (amountInt % 10) {
                            case 1: appendix = context.getString(R.string.ruble1); break;
                            case 2: case 3: case 4: appendix = context.getString(R.string.ruble2); break;
                            default: appendix = context.getString(R.string.ruble3); break;
                        }
                        break;
                }
                amount += " " + appendix.toLowerCase();
            } catch (Exception ignore) {
                // ignore
            }
            ((TextView) container.findViewById(R.id.info)).setText(info);
            ((TextView) container.findViewById(R.id.dates)).setText(dates);
            ((TextView) container.findViewById(R.id.amount)).setText(amount);
        } catch (Exception e) {
            log.exception(e);
        }
    }

    private void bindNoAssignedItems(View container) {
        try {
            ((TextView) container.findViewById(R.id.ntd_text)).setText(R.string.no_assigned_scholarship);
        } catch (Exception e) {
            log.exception(e);
        }
    }

    private ArrayList<Item> entity2dataset(@NonNull SSAssignedList ssPaidList) {
        final ArrayList<Item> dataset = new ArrayList<>();
        try {
            dataset.add(new Item(TYPE_HEADER));
            List<SSAssigned> ssAssignedArr = ssPaidList.getList();
            if (CollectionUtils.isEmpty(ssAssignedArr)) {
                dataset.add(new Item(TYPE_NO_ASSIGNED_ITEMS));
                return dataset;
            }
            for (SSAssigned ssAssigned : ssAssignedArr) {
                dataset.add(new Item<>(TYPE_ASSIGNED_ITEM, ssAssigned));
            }
        } catch (Exception e) {
            log.exception(e);
        }
        return dataset;
    }
}
