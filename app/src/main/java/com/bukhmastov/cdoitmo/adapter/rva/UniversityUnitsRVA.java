package com.bukhmastov.cdoitmo.adapter.rva;

import android.content.Context;
import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.model.rva.RVAUniversity;
import com.bukhmastov.cdoitmo.model.university.units.UDivision;
import com.bukhmastov.cdoitmo.model.university.units.UUnit;
import com.bukhmastov.cdoitmo.model.university.units.UUnits;
import com.bukhmastov.cdoitmo.util.singleton.CollectionUtils;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;
import com.bukhmastov.cdoitmo.view.CircularTransformation;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UniversityUnitsRVA extends UniversityRVA {

    private static final Pattern WORKING_HOURS_PATTERN = Pattern.compile("(\\D*/.*\\|?)+");
    private boolean isFirstBlock = true;

    public UniversityUnitsRVA(Context context) {
        super(context);
    }

    @Override
    protected int onGetLayout(int type) throws NullPointerException {
        @LayoutRes int layout;
        switch (type) {
            case TYPE_INFO_ABOUT_UPDATE_TIME: layout = R.layout.layout_university_update_time; break;
            case TYPE_UNIT_STRUCTURE_COMMON:
            case TYPE_UNIT_STRUCTURE_HEAD:
            case TYPE_UNIT_DIVISIONS: layout = R.layout.layout_university_faculties_structure_unit; break;
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
            case TYPE_UNIT_STRUCTURE_COMMON: bindStructureCommon(container, item); break;
            case TYPE_UNIT_STRUCTURE_HEAD: bindStructureHead(container, item); break;
            case TYPE_UNIT_DIVISIONS: bindDivisions(container, item); break;
            case TYPE_NO_DATA: bindNoData(container); break;
        }
    }

    private void bindStructureCommon(View container, Item<UUnit> item) {
        try {
            UUnit unit = item.data;
            boolean isFirstContainer = true;
            removeFirstSeparator(container);
            ((TextView) container.findViewById(R.id.structure_header)).setText(context.getString(R.string.faculty_section_general));
            ViewGroup structureContainer = container.findViewById(R.id.structure_container);
            if (structureContainer == null) {
                return;
            }
            if (StringUtils.isNotBlank(unit.getAddress())) {
                String address = unit.getAddress().trim();
                structureContainer.addView(getConnectContainer(R.drawable.ic_location, address, true, R.id.university_tile_map, new RVAUniversity(address)));
                isFirstContainer = false;
            }
            if (StringUtils.isNotBlank(unit.getPhone())) {
                for (String phone : unit.getPhone().trim().split("[;,]")) {
                    if (StringUtils.isBlank(phone)) {
                        return;
                    }
                    structureContainer.addView(getConnectContainer(R.drawable.ic_phone, phone.trim(), isFirstContainer, R.id.university_tile_phone, new RVAUniversity(phone.trim())));
                    isFirstContainer = false;
                }
            }
            if (StringUtils.isNotBlank(unit.getEmail())) {
                for (String email : unit.getEmail().trim().split("[;,]")) {
                    if (StringUtils.isBlank(email)) {
                        return;
                    }
                    structureContainer.addView(getConnectContainer(R.drawable.ic_email, email.trim(), isFirstContainer, R.id.university_tile_mail, new RVAUniversity(email.trim())));
                    isFirstContainer = false;
                }
            }
            if (StringUtils.isNotBlank(unit.getSite())) {
                for (String site : unit.getSite().trim().split("[;,]")) {
                    if (StringUtils.isBlank(site)) {
                        return;
                    }
                    structureContainer.addView(getConnectContainer(R.drawable.ic_web, site.trim(), isFirstContainer, R.id.university_tile_web, new RVAUniversity(site.trim())));
                    isFirstContainer = false;
                }
            }
            if (StringUtils.isNotBlank(unit.getWorkingHours())) {
                String wh = unit.getWorkingHours().trim();
                Matcher m = WORKING_HOURS_PATTERN.matcher(wh);
                if (m.find()) {
                    String[] days = wh.split("\\|");
                    ArrayList<String> days_new = new ArrayList<>();
                    for (String day : days) {
                        String[] day_split = day.trim().split("/");
                        StringBuilder timeBuilder = new StringBuilder();
                        for (int i = 1; i < day_split.length; i++) {
                            timeBuilder.append(day_split[i]).append("/");
                        }
                        String time = timeBuilder.toString().trim();
                        if (time.endsWith("/")) {
                            time = time.trim().substring(0, time.length() - 1);
                        }
                        time = time.replace("/", ", ");
                        if (!time.isEmpty()) {
                            time = (day_split[0] + " (" + time + ")").trim();
                        }
                        days_new.add(time);
                    }
                    wh = android.text.TextUtils.join("\n", days_new).trim();
                }
                structureContainer.addView(getConnectContainer(R.drawable.ic_access_time, wh, isFirstContainer, null, null));
            }
        } catch (Exception e) {
            log.get().exception(e);
        }
    }
    private void bindStructureHead(View container, Item<UUnit> item) {
        try {
            UUnit unit = item.data;
            removeFirstSeparator(container);
            String post = StringUtils.isNotBlank(unit.getPost()) ?
                    StringUtils.capitalizeFirstLetter(unit.getPost().trim()) :
                    context.getString(R.string.faculty_section_head);
            ((TextView) container.findViewById(R.id.structure_header)).setText(post);
            ViewGroup structureContainer = container.findViewById(R.id.structure_container);
            if (structureContainer == null) {
                return;
            }
            if (StringUtils.isNotBlank(unit.getLastName())) {
                View layout = inflate(R.layout.layout_university_persons_list_item);
                layout.setId(R.id.university_tile_person);
                tryRegisterClickListener(layout, R.id.university_tile_person, new RVAUniversity(unit.getIfmoPersonId()));
                String personFio = unit.getLastName() + " " + unit.getFirstName() + " " +
                        (StringUtils.isNotBlank(unit.getMiddleName()) ? unit.getMiddleName() : "");
                ((TextView) layout.findViewById(R.id.name)).setText(personFio.trim());
                if (StringUtils.isNotBlank(unit.getPost())) {
                    ((TextView) layout.findViewById(R.id.post)).setText(unit.getPost());
                } else {
                    staticUtil.removeView(layout.findViewById(R.id.post));
                }
                if (StringUtils.isNotBlank(unit.getAvatar())) {
                    Picasso.with(context)
                            .load(unit.getAvatar())
                            .error(R.drawable.ic_sentiment_very_satisfied)
                            .transform(new CircularTransformation())
                            .into((ImageView) layout.findViewById(R.id.avatar));
                }
                structureContainer.addView(layout);
            }
        } catch (Exception e) {
            log.get().exception(e);
        }
    }
    private void bindDivisions(View container, Item<UUnits> item) {
        try {
            removeFirstSeparator(container);
            if (CollectionUtils.isEmpty(item.data.getDivisions())) {
                return;
            }
            ((TextView) container.findViewById(R.id.structure_header)).setText(context.getString(R.string.faculty_section_divisions));
            ViewGroup structureContainer = container.findViewById(R.id.structure_container);
            if (structureContainer == null) {
                return;
            }
            for (UDivision division : item.data.getDivisions()) {
                if (StringUtils.isNotBlank(division.getTitle())) {
                    View view = inflate(R.layout.layout_university_faculties_divisions_list_item);
                    ((TextView) view.findViewById(R.id.title)).setText(division.getTitle());
                    tryRegisterClickListener(view, R.id.division, new RVAUniversity(division));
                    structureContainer.addView(view);
                }
            }
        } catch (Exception e) {
            log.get().exception(e);
        }
    }
    private void bindNoData(View container) {
        try {
            ((TextView) container.findViewById(R.id.ntd_text)).setText(R.string.no_data);
        } catch (Exception e) {
            log.get().exception(e);
        }
    }

    private void removeFirstSeparator(View container) {
        if (isFirstBlock) {
            isFirstBlock = false;
            View structure_separator = container.findViewById(R.id.structure_separator);
            if (structure_separator != null) {
                staticUtil.removeView(structure_separator);
            }
        }
    }

    private View getConnectContainer(@DrawableRes int icon, String text, boolean isRemoveSeparator, @Nullable @IdRes Integer layoutId, @Nullable RVAUniversity entity) {

        View layout = inflate(R.layout.layout_university_connect);

        if (layoutId != null) {
            layout.setId(layoutId);
            tryRegisterClickListener(layout, layoutId, entity);
        }

        ((ImageView) layout.findViewById(R.id.connect_image)).setImageResource(icon);
        ((TextView) layout.findViewById(R.id.connect_text)).setText(text.trim());

        if (isRemoveSeparator) {
            staticUtil.removeView(layout.findViewById(R.id.separator));
        }

        return layout;
    }

    private View inflate(@LayoutRes int layoutId) throws InflateException {
        return ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layoutId, null);
    }
}
