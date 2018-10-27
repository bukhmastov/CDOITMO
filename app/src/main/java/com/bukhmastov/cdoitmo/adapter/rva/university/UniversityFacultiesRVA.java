package com.bukhmastov.cdoitmo.adapter.rva.university;

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
import com.bukhmastov.cdoitmo.model.university.faculties.UDivision;
import com.bukhmastov.cdoitmo.model.university.faculties.UFaculties;
import com.bukhmastov.cdoitmo.model.university.faculties.UStructure;
import com.bukhmastov.cdoitmo.model.university.faculties.UStructureInfo;
import com.bukhmastov.cdoitmo.util.singleton.CollectionUtils;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;
import com.bukhmastov.cdoitmo.view.CircularTransformation;
import com.squareup.picasso.Picasso;

public class UniversityFacultiesRVA extends UniversityRVA {

    private boolean isFirstBlock = true;

    public UniversityFacultiesRVA(Context context) {
        super(context);
    }

    @Override
    protected int onGetLayout(int type) throws NullPointerException {
        @LayoutRes int layout;
        switch (type) {
            case TYPE_INFO_ABOUT_UPDATE_TIME: layout = R.layout.layout_university_update_time; break;
            case TYPE_UNIT_STRUCTURE_COMMON:
            case TYPE_UNIT_STRUCTURE_DEANERY:
            case TYPE_UNIT_STRUCTURE_HEAD:
            case TYPE_UNIT_DIVISIONS: layout = R.layout.layout_university_faculties_structure_unit; break;
            case TYPE_NO_DATA: layout = R.layout.state_nothing_to_display_compact; break;
            default: throw new NullPointerException("Invalid type provided");
        }
        return layout;
    }

    @Override
    protected void onBind(View container, Item item) {
        switch (item.type) {
            case TYPE_INFO_ABOUT_UPDATE_TIME: bindInfoAboutUpdateTime(container, item); break;
            case TYPE_UNIT_STRUCTURE_COMMON: bindStructureCommon(container, item); break;
            case TYPE_UNIT_STRUCTURE_DEANERY: bindStructureDeanery(container, item); break;
            case TYPE_UNIT_STRUCTURE_HEAD: bindStructureHead(container, item); break;
            case TYPE_UNIT_DIVISIONS: bindDivisions(container, item); break;
            case TYPE_NO_DATA: bindNoData(container); break;
        }
    }

    private void bindStructureCommon(View container, Item<UStructure> item) {
        try {
            UStructureInfo structureInfo = item.data.getStructureInfo();
            boolean isFirstContainer = true;
            removeFirstSeparator(container);
            ((TextView) container.findViewById(R.id.structure_header)).setText(context.getString(R.string.faculty_section_general));
            ViewGroup structureContainer = container.findViewById(R.id.structure_container);
            if (structureContainer == null) {
                return;
            }
            if (StringUtils.isNotBlank(structureInfo.getAddress())) {
                String address = structureInfo.getAddress().trim();
                structureContainer.addView(getConnectContainer(R.drawable.ic_location, address, true, R.id.university_tile_map, new RVAUniversity(address)));
                isFirstContainer = false;
            }
            if (StringUtils.isNotBlank(structureInfo.getPhone())) {
                for (String phone : structureInfo.getPhone().trim().split("[;,]")) {
                    if (StringUtils.isBlank(phone)) {
                        return;
                    }
                    structureContainer.addView(getConnectContainer(R.drawable.ic_phone, phone.trim(), isFirstContainer, R.id.university_tile_phone, new RVAUniversity(phone.trim())));
                    isFirstContainer = false;
                }
            }
            if (StringUtils.isNotBlank(structureInfo.getSite())) {
                for (String site : structureInfo.getSite().trim().split("[;,]")) {
                    if (StringUtils.isBlank(site)) {
                        return;
                    }
                    structureContainer.addView(getConnectContainer(R.drawable.ic_web, site.trim(), isFirstContainer, R.id.university_tile_web, new RVAUniversity(site.trim())));
                    isFirstContainer = false;
                }
            }
        } catch (Exception e) {
            log.exception(e);
        }
    }
    private void bindStructureDeanery(View container, Item<UStructure> item) {
        try {
            UStructureInfo structureInfo = item.data.getStructureInfo();
            boolean isFirstContainer = true;
            removeFirstSeparator(container);
            ((TextView) container.findViewById(R.id.structure_header)).setText(context.getString(R.string.faculty_section_deanery));
            ViewGroup structureContainer = container.findViewById(R.id.structure_container);
            if (structureContainer == null) {
                return;
            }
            if (StringUtils.isNotBlank(structureInfo.getDeaneryAddress())) {
                String address = structureInfo.getDeaneryAddress().trim();
                structureContainer.addView(getConnectContainer(R.drawable.ic_location, address, true, R.id.university_tile_map, new RVAUniversity(address)));
                isFirstContainer = false;
            }
            if (StringUtils.isNotBlank(structureInfo.getDeaneryPhone())) {
                for (String phone : structureInfo.getDeaneryPhone().trim().split("[;,]")) {
                    if (StringUtils.isBlank(phone)) {
                        return;
                    }
                    structureContainer.addView(getConnectContainer(R.drawable.ic_phone, phone.trim(), isFirstContainer, R.id.university_tile_phone, new RVAUniversity(phone.trim())));
                    isFirstContainer = false;
                }
            }
            if (StringUtils.isNotBlank(structureInfo.getDeaneryEmail())) {
                for (String email : structureInfo.getDeaneryEmail().trim().split("[;,]")) {
                    if (StringUtils.isBlank(email)) {
                        return;
                    }
                    structureContainer.addView(getConnectContainer(R.drawable.ic_email, email.trim(), isFirstContainer, R.id.university_tile_mail, new RVAUniversity(email.trim())));
                    isFirstContainer = false;
                }
            }
        } catch (Exception e) {
            log.exception(e);
        }
    }
    private void bindStructureHead(View container, Item<UStructure> item) {
        try {
            UStructureInfo structureInfo = item.data.getStructureInfo();
            boolean isFirstContainer = true;
            removeFirstSeparator(container);
            if (StringUtils.isNotBlank(structureInfo.getPersonPost())) {
                ((TextView) container.findViewById(R.id.structure_header)).setText(textUtils.capitalizeFirstLetter(structureInfo.getPersonPost().trim()));
            } else {
                staticUtil.removeView(container.findViewById(R.id.structure_header));
            }
            ViewGroup structureContainer = container.findViewById(R.id.structure_container);
            if (structureContainer == null) {
                return;
            }
            if (StringUtils.isNotBlank(structureInfo.getLastName())) {
                View layout = inflate(R.layout.layout_university_persons_list_item);
                layout.setId(R.id.university_tile_person);
                tryRegisterClickListener(layout, R.id.university_tile_person, new RVAUniversity(structureInfo.getIfmoPersonId()));
                String personFio = structureInfo.getLastName() + " " + structureInfo.getFirstName() + " " +
                        (StringUtils.isNotBlank(structureInfo.getMiddleName()) ? structureInfo.getMiddleName() : "");
                ((TextView) layout.findViewById(R.id.name)).setText(personFio.trim());
                if (StringUtils.isNotBlank(structureInfo.getPersonDegree())) {
                    ((TextView) layout.findViewById(R.id.post)).setText(structureInfo.getPersonDegree());
                } else {
                    staticUtil.removeView(layout.findViewById(R.id.post));
                }
                if (StringUtils.isNotBlank(structureInfo.getPersonAvatar())) {
                    Picasso.with(context)
                            .load(structureInfo.getPersonAvatar())
                            .error(R.drawable.ic_sentiment_very_satisfied)
                            .transform(new CircularTransformation())
                            .into((ImageView) layout.findViewById(R.id.avatar));
                }
                structureContainer.addView(layout);
                isFirstContainer = false;
            }
            if (StringUtils.isNotBlank(structureInfo.getEmail())) {
                for (String email : structureInfo.getEmail().trim().split("[;,]")) {
                    if (StringUtils.isBlank(email)) {
                        return;
                    }
                    structureContainer.addView(getConnectContainer(R.drawable.ic_email, email.trim(), isFirstContainer, R.id.university_tile_mail, new RVAUniversity(email.trim())));
                    isFirstContainer = false;
                }
            }
        } catch (Exception e) {
            log.exception(e);
        }
    }
    private void bindDivisions(View container, Item<UFaculties> item) {
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
                if (StringUtils.isNotBlank(division.getName())) {
                    View view = inflate(R.layout.layout_university_faculties_divisions_list_item);
                    ((TextView) view.findViewById(R.id.title)).setText(division.getName());
                    tryRegisterClickListener(view, R.id.division, new RVAUniversity(division));
                    structureContainer.addView(view);
                }
            }
        } catch (Exception e) {
            log.exception(e);
        }
    }
    private void bindNoData(View container) {
        try {
            ((TextView) container.findViewById(R.id.ntd_text)).setText(R.string.no_data);
        } catch (Exception e) {
            log.exception(e);
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
