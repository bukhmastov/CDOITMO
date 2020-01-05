package com.bukhmastov.cdoitmo.adapter.rva;

import android.content.Context;
import androidx.annotation.LayoutRes;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.model.rva.RVAUniversity;
import com.bukhmastov.cdoitmo.model.university.persons.UPerson;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;
import com.bukhmastov.cdoitmo.view.CircularTransformation;
import com.squareup.picasso.Picasso;

public class UniversityPersonsRVA extends UniversityRVA {

    public UniversityPersonsRVA(Context context) {
        super(context);
    }

    @Override
    protected int onGetLayout(int type) throws NullPointerException {
        @LayoutRes int layout;
        switch (type) {
            case TYPE_INFO_ABOUT_UPDATE_TIME: layout = R.layout.layout_university_update_time; break;
            case TYPE_MAIN: layout = R.layout.layout_university_persons_list_item; break;
            case TYPE_STATE: layout = R.layout.layout_university_list_item_state; break;
            case TYPE_NO_DATA: layout = R.layout.state_nothing_to_display_compact; break;
            default: throw new NullPointerException("Invalid type provided");
        }
        return layout;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void onBind(View container, Item item) {
        switch (item.type) {
            case TYPE_INFO_ABOUT_UPDATE_TIME: bindInfoAboutUpdateTime(container, item); break;
            case TYPE_MAIN: bindMain(container, item); break;
            case TYPE_STATE: bindState(container, item); break;
            case TYPE_NO_DATA: bindNoData(container); break;
        }
    }

    private void bindMain(View container, Item<UPerson> item) {
        try {
            UPerson person = item.data;
            tryRegisterClickListener(container, R.id.person, new RVAUniversity(person));
            View nameView = container.findViewById(R.id.name);
            if (nameView != null) {
                String name = (getStringIfExists(person.getLastName()) + " " + getStringIfExists(person.getFirstName()) + " " + getStringIfExists(person.getMiddleName())).trim();
                ((TextView) nameView).setText(name);
            }
            View postView = container.findViewById(R.id.post);
            if (postView != null) {
                if (StringUtils.isNotBlank(person.getDegree())) {
                    ((TextView) postView).setText(StringUtils.capitalizeFirstLetter(person.getDegree()));
                } else {
                    staticUtil.removeView(container.findViewById(R.id.post));
                }
            }
            View avatarView = container.findViewById(R.id.avatar);
            if (avatarView != null) {
                new Picasso.Builder(context).build()
                        .load(person.getImage())
                        .error(R.drawable.ic_sentiment_very_satisfied)
                        .transform(new CircularTransformation())
                        .into((ImageView) avatarView);
            }
        } catch (Exception e) {
            log.get().exception(e);
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
            log.get().exception(e);
        }
    }
    private void bindNoData(View container) {
        try {
            ((TextView) container.findViewById(R.id.ntd_text)).setText(R.string.no_persons);
        } catch (Exception e) {
            log.get().exception(e);
        }
    }

    private String getStringIfExists(String value) {
        return StringUtils.isNotBlank(value) ? value : "";
    }
}
