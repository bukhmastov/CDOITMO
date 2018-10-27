package com.bukhmastov.cdoitmo.adapter.rva.university;

import android.content.Context;
import android.graphics.Color;
import androidx.annotation.LayoutRes;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.model.rva.RVAUniversity;
import com.bukhmastov.cdoitmo.model.university.events.UEvent;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;

public class UniversityEventsRVA extends UniversityRVA {

    public UniversityEventsRVA(Context context) {
        super(context);
    }

    @Override
    protected int onGetLayout(int type) throws NullPointerException {
        @LayoutRes int layout;
        switch (type) {
            case TYPE_INFO_ABOUT_UPDATE_TIME: layout = R.layout.layout_university_update_time; break;
            case TYPE_MINOR: layout = R.layout.layout_university_news_card_compact; break;
            case TYPE_STATE: layout = R.layout.layout_university_list_item_state; break;
            case TYPE_NO_DATA: layout = R.layout.state_nothing_to_display_compact; break;
            default: throw new NullPointerException("Invalid type provided");
        }
        return layout;
    }

    @Override
    protected void onBind(View container, Item item) {
        switch (item.type) {
            case TYPE_INFO_ABOUT_UPDATE_TIME: bindInfoAboutUpdateTime(container, item); break;
            case TYPE_MINOR: bindMinor(container, item); break;
            case TYPE_STATE: bindState(container, item); break;
            case TYPE_NO_DATA: bindNoData(container); break;
        }
    }

    private void bindMinor(View container, Item<UEvent> item) {
        try {
            UEvent event = item.data;
            if (StringUtils.isBlank(event.getName())) {
                return;
            }
            tryRegisterClickListener(container, R.id.news_click, new RVAUniversity(event));
            View titleView = container.findViewById(R.id.title);
            if (titleView != null) {
                ((TextView) titleView).setText(textUtils.escapeString(event.getName()));
            }
            View categoriesView = container.findViewById(R.id.categories);
            if (categoriesView != null) {
                if (StringUtils.isNotBlank(event.getTypeName())) {
                    TextView categories = (TextView) categoriesView;
                    categories.setText("● " + event.getTypeName());
                    categories.setTextColor(Color.parseColor("#DF1843"));
                } else {
                    staticUtil.removeView(categoriesView);
                }
            }
            View dateView = container.findViewById(R.id.date);
            if (dateView != null) {
                boolean dateBeginExists = StringUtils.isNotBlank(event.getDateBegin());
                boolean dateEndExists = StringUtils.isNotBlank(event.getDateEnd());
                if (dateBeginExists || dateEndExists) {
                    String date;
                    if (dateBeginExists && dateEndExists) {
                        date = textUtils.cuteDate(context, storagePref, "yyyy-MM-dd HH:mm:ss", event.getDateBegin(), event.getDateEnd());
                    } else if (dateBeginExists) {
                        date = textUtils.cuteDate(context, storagePref, "yyyy-MM-dd HH:mm:ss", event.getDateBegin());
                    } else {
                        date = textUtils.cuteDate(context, storagePref, "yyyy-MM-dd HH:mm:ss", event.getDateEnd());
                    }
                    ((TextView) dateView).setText(date);
                } else {
                    staticUtil.removeView(dateView);
                }
            }
            View newsImageContainerView = container.findViewById(R.id.news_image_container);
            if (newsImageContainerView != null) {
                staticUtil.removeView(newsImageContainerView);
            }
            View countViewContainerView = container.findViewById(R.id.count_view_container);
            if (countViewContainerView != null) {
                staticUtil.removeView(countViewContainerView);
            }
        } catch (Exception e) {
            log.exception(e);
        }
    }
    private void bindState(View container, Item item) {
        try {
            tryRegisterClickListener(container, (int) item.extras.get(DATA_STATE_KEEP), null);
            if (container instanceof ViewGroup) {
                ViewGroup containerGroup = (ViewGroup) container;
                for (int i = containerGroup.getChildCount() - 1; i >= 0; i--) {
                    View child = containerGroup.getChildAt(i);
                    if (child.getId() == (int) item.extras.get(DATA_STATE_KEEP)) {
                        child.setVisibility(View.VISIBLE);
                    } else {
                        child.setVisibility(View.GONE);
                    }
                }
            }
        } catch (Exception e) {
            log.exception(e);
        }
    }
    private void bindNoData(View container) {
        try {
            ((TextView) container.findViewById(R.id.ntd_text)).setText(R.string.no_events);
        } catch (Exception e) {
            log.exception(e);
        }
    }
}
