package com.bukhmastov.cdoitmo.adapter.rva;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.model.group.GGroup;
import com.bukhmastov.cdoitmo.model.group.GList;
import com.bukhmastov.cdoitmo.model.group.GPerson;
import com.bukhmastov.cdoitmo.model.rva.RVADualValue;
import com.bukhmastov.cdoitmo.util.singleton.CollectionUtils;
import com.bukhmastov.cdoitmo.view.CircularTransformation;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;

public class GroupRVA extends RVA<GPerson> {

    private static final int TYPE_GROUP = 0;
    private static final int TYPE_PERSON = 1;
    private static final int TYPE_NO_GROUPS = 2;
    private static final int TYPE_NO_PERSONS = 3;

    protected final Context context;

    public GroupRVA(@NonNull Context context, @NonNull GList gList) {
        super();
        this.context = context;
        addItems(entity2dataset(gList));
    }

    @Override
    protected int onGetLayout(int type) throws NullPointerException {
        @LayoutRes int layout;
        switch (type) {
            case TYPE_GROUP: layout = R.layout.layout_group_header; break;
            case TYPE_PERSON: layout = R.layout.layout_group_person; break;
            case TYPE_NO_GROUPS: layout = R.layout.state_nothing_to_display_compact; break;
            case TYPE_NO_PERSONS: layout = R.layout.state_nothing_to_display_compact; break;
            default: throw new NullPointerException("Invalid type provided");
        }
        return layout;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void onBind(View container, Item item) {
        switch (item.type) {
            case TYPE_GROUP: bindGroup(container, item); break;
            case TYPE_PERSON: bindPerson(container, item); break;
            case TYPE_NO_GROUPS: bindNoGroups(container); break;
            case TYPE_NO_PERSONS: bindNoPersons(container); break;
        }
    }

    private void bindGroup(View container, Item<RVADualValue> item) {
        try {
            ((TextView) container.findViewById(R.id.header)).setText(item.data.getFirst());
            ((TextView) container.findViewById(R.id.info)).setText(item.data.getSecond());
        } catch (Exception e) {
            log.get().exception(e);
        }
    }

    private void bindPerson(View container, Item<GPerson> item) {
        try {
            ((TextView) container.findViewById(R.id.personNumber)).setText(String.valueOf(item.data.getNumber()));
            ((TextView) container.findViewById(R.id.personName)).setText(item.data.getFio());
            ((TextView) container.findViewById(R.id.personId)).setText(String.valueOf(item.data.getPersonId()));
            new Picasso.Builder(context).build()
                    .load(item.data.getPhotoUrl())
                    .error(R.drawable.ic_sentiment_very_satisfied)
                    .transform(new CircularTransformation())
                    .into((ImageView) container.findViewById(R.id.avatar));
            tryRegisterClickListener(container, R.id.person, item.data);
        } catch (Exception e) {
            log.get().exception(e);
        }
    }

    private void bindNoGroups(View container) {
        try {
            ((TextView) container.findViewById(R.id.ntd_text)).setText(R.string.no_groups);
        } catch (Exception e) {
            log.get().exception(e);
        }
    }

    private void bindNoPersons(View container) {
        try {
            ((TextView) container.findViewById(R.id.ntd_text)).setText(R.string.no_group_persons);
        } catch (Exception e) {
            log.get().exception(e);
        }
    }

    private ArrayList<Item> entity2dataset(@NonNull GList gList) {
        final ArrayList<Item> dataset = new ArrayList<>();
        try {
            List<GGroup> groups = gList.getList();
            if (CollectionUtils.isEmpty(groups)) {
                dataset.add(new Item(TYPE_NO_GROUPS));
                return dataset;
            }
            for (GGroup gGroup : groups) {
                boolean isGroupHasPersons = CollectionUtils.isNotEmpty(gGroup.getPersons());
                int personCount = isGroupHasPersons ? gGroup.getPersons().size() : 0;
                String group = context.getString(R.string.group) + " " + gGroup.getGroup();
                String personCountString = String.valueOf(personCount) + " ";
                String appendix;
                switch (personCount % 100) {
                    case 10: case 11: case 12: case 13: case 14: appendix = context.getString(R.string.student3); break;
                    default:
                        switch (personCount % 10) {
                            case 1: appendix = context.getString(R.string.student1); break;
                            case 2: case 3: case 4: appendix = context.getString(R.string.student2); break;
                            default: appendix = context.getString(R.string.student3); break;
                        }
                        break;
                }
                personCountString += appendix;
                dataset.add(new Item<>(TYPE_GROUP, new RVADualValue(group, personCountString.toLowerCase())));
                if (!isGroupHasPersons) {
                    dataset.add(new Item<>(TYPE_NO_PERSONS));
                    continue;
                }
                for (GPerson gPerson : gGroup.getPersons()) {
                    dataset.add(new Item<>(TYPE_PERSON, gPerson));
                }
            }
        } catch (Exception e) {
            log.get().exception(e);
        }
        return dataset;
    }
}
