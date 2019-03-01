package com.bukhmastov.cdoitmo.adapter.rva;

import android.content.Context;
import android.os.Build;
import android.text.Html;
import android.view.View;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.model.rva.RVASingleValue;
import com.bukhmastov.cdoitmo.model.scholarship.detailed.SSDetailed;
import com.bukhmastov.cdoitmo.model.scholarship.detailed.SSDetailedList;
import com.bukhmastov.cdoitmo.util.Static;
import com.bukhmastov.cdoitmo.util.singleton.CollectionUtils;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;

public class ScholarshipPaidDetailsRVA extends RVA<SSDetailed> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_DETAIL_ITEM = 1;
    private static final int TYPE_NO_DETAILS = 2;

    public ScholarshipPaidDetailsRVA(@NonNull Context context, @NonNull SSDetailedList ssDetailedList) {
        super();
        addItems(entity2dataset(ssDetailedList));
    }

    @Override
    protected int onGetLayout(int type) throws NullPointerException {
        @LayoutRes int layout;
        switch (type) {
            case TYPE_HEADER: layout = R.layout.layout_scholarship_paid_details_header; break;
            case TYPE_DETAIL_ITEM: layout = R.layout.layout_scholarship_paid_details_item; break;
            case TYPE_NO_DETAILS: layout = R.layout.state_nothing_to_display_compact; break;
            default: throw new NullPointerException("Invalid type provided");
        }
        return layout;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void onBind(View container, Item item) {
        switch (item.type) {
            case TYPE_HEADER: bindHeader(container, item); break;
            case TYPE_DETAIL_ITEM: bindDetailsItem(container, item); break;
            case TYPE_NO_DETAILS: bindNoDetails(container); break;
        }
    }

    private void bindHeader(View container, Item<RVASingleValue> item) {
        try {
            ((TextView) container.findViewById(R.id.title)).setText(item.data.getValue());
        } catch (Exception e) {
            log.get().exception(e);
        }
    }

    private void bindDetailsItem(View container, Item<SSDetailed> item) {
        try {
            setHtmlToTextView(container.findViewById(R.id.info), StringUtils.isNotBlank(item.data.getContribution()) ?
                    item.data.getContribution().replace("_", " ") :
                    Static.GLITCH);
            if (StringUtils.isNotBlank(item.data.getStart()) && StringUtils.isNotBlank(item.data.getEnd())) {
                ((TextView) container.findViewById(R.id.dates)).setText(item.data.getStart() + " — " + item.data.getEnd());
            } else {
                container.findViewById(R.id.dates).setVisibility(View.GONE);
            }
            if (StringUtils.isNotBlank(item.data.getValue())) {
                setHtmlToTextView(container.findViewById(R.id.amount), item.data.getValue().trim());
            } else {
                container.findViewById(R.id.amount).setVisibility(View.GONE);
            }
        } catch (Exception e) {
            log.get().exception(e);
        }
    }

    private void bindNoDetails(View container) {
        try {
            ((TextView) container.findViewById(R.id.ntd_text)).setText(R.string.no_scholarship_details);
        } catch (Exception e) {
            log.get().exception(e);
        }
    }

    private ArrayList<Item> entity2dataset(@NonNull SSDetailedList ssDetailedList) {
        final ArrayList<Item> dataset = new ArrayList<>();
        try {
            String period = "";
            for (SSDetailed ssDetailed : CollectionUtils.emptyIfNull(ssDetailedList.getList())) {
                if (StringUtils.isNotBlank(ssDetailed.getMonthOfPayment())) {
                    period = ssDetailed.getMonthOfPayment();
                    break;
                }
            }
            dataset.add(new Item<>(TYPE_HEADER, new RVASingleValue(period)));
            List<SSDetailed> ssDetailedArr = ssDetailedList.getList();
            if (CollectionUtils.isEmpty(ssDetailedArr)) {
                dataset.add(new Item(TYPE_NO_DETAILS));
                return dataset;
            }
            for (SSDetailed ssDetailed : ssDetailedArr) {
                dataset.add(new Item<>(TYPE_DETAIL_ITEM, ssDetailed));
            }
        } catch (Exception e) {
            log.get().exception(e);
        }
        return dataset;
    }

    private void setHtmlToTextView(TextView textView, String data) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            textView.setText(Html.fromHtml(data, Html.FROM_HTML_MODE_COMPACT));
        } else {
            textView.setText(Html.fromHtml(data));
        }
    }
}
