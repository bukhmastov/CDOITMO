package com.bukhmastov.cdoitmo.adapter.rva;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.model.eregister.ERMark;
import com.bukhmastov.cdoitmo.model.eregister.ERPoint;
import com.bukhmastov.cdoitmo.model.eregister.ERSubject;
import com.bukhmastov.cdoitmo.model.rva.RVASubject;
import com.bukhmastov.cdoitmo.util.singleton.CollectionUtils;
import com.bukhmastov.cdoitmo.util.singleton.NumberUtils;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;

import java.util.ArrayList;
import java.util.TreeSet;
import java.util.regex.Pattern;

public class ERegisterSubjectsRVA extends RVA<ERSubject> {

    private static final int TYPE_SUBJECT = 0;
    private static final int TYPE_SUBJECT_PASSED = 1;
    private static final int TYPE_NO_SUBJECTS = 2;

    private static final Pattern PATTERN_ATTESTATION = Pattern.compile("^.*зач[её]т$|^экзамен$|^тестирование$|^общий\\sрейтинг$", Pattern.CASE_INSENSITIVE);

    public ERegisterSubjectsRVA(@NonNull Context context, @NonNull TreeSet<ERSubject> subjects) {
        super();
        addItems(entity2dataset(context, subjects));
    }

    @Override
    protected int onGetLayout(int type) throws NullPointerException {
        @LayoutRes int layout;
        switch (type) {
            case TYPE_SUBJECT: layout = R.layout.layout_eregister_subject_item; break;
            case TYPE_SUBJECT_PASSED: layout = R.layout.layout_eregister_subject_item_passed; break;
            case TYPE_NO_SUBJECTS: layout = R.layout.state_nothing_to_display_compact; break;
            default: throw new NullPointerException("Invalid type provided");
        }
        return layout;
    }

    @Override
    protected void onBind(View container, Item item) {
        switch (item.type) {
            case TYPE_SUBJECT: bindSubject(container, item); break;
            case TYPE_SUBJECT_PASSED: bindSubject(container, item); break;
            case TYPE_NO_SUBJECTS: bindNoSubjects(container); break;
        }
    }

    private void bindSubject(View container, Item<RVASubject> item) {
        try {
            ((TextView) container.findViewById(R.id.name)).setText(item.data.getName());
            ((TextView) container.findViewById(R.id.about)).setText(item.data.getAbout());
            ((TextView) container.findViewById(R.id.value)).setText(item.data.getValue());
            tryRegisterClickListener(container, R.id.subject, item.data.getSubject());
        } catch (Exception e) {
            log.exception(e);
        }
    }
    private void bindNoSubjects(View container) {
        try {
            ((TextView) container.findViewById(R.id.ntd_text)).setText(R.string.no_subjects);
        } catch (Exception e) {
            log.exception(e);
        }
    }

    private ArrayList<Item> entity2dataset(@NonNull Context context, @NonNull TreeSet<ERSubject> subjects) {
        final ArrayList<Item> dataset = new ArrayList<>();
        try {
            if (CollectionUtils.isEmpty(subjects)) {
                dataset.add(new Item(TYPE_NO_SUBJECTS));
                return dataset;
            }
            for (ERSubject subject : subjects) {
                Double points = null;
                if (CollectionUtils.isNotEmpty(subject.getPoints())) {
                    for (ERPoint point : subject.getPoints()) {
                        Double max = point.getMax();
                        if (max != null && max == 100.0) {
                            points = point.getValue();
                            if (points != null) {
                                break;
                            }
                        }
                    }
                    if (points == null) {
                        for (ERPoint point : subject.getPoints()) {
                            if (StringUtils.isBlank(point.getName()) || point.getValue() == null) {
                                continue;
                            }
                            if (PATTERN_ATTESTATION.matcher(point.getName()).find()) {
                                points = point.getValue();
                                break;
                            }
                        }
                    }
                }
                StringBuilder about = new StringBuilder(subject.getTerm() + " " + context.getString(R.string.semester));
                if (CollectionUtils.isNotEmpty(subject.getMarks())) {
                    about.append(" | ");
                    int j = 0;
                    for (ERMark mark : subject.getMarks()) {
                        about.append(j++ > 0 ? ", " : "").append(mark.getWorkType());
                    }
                }
                RVASubject rvaSubject = new RVASubject();
                rvaSubject.setName(subject.getName());
                rvaSubject.setAbout(about.toString());
                rvaSubject.setValue(NumberUtils.prettyDouble(points));
                rvaSubject.setSubject(subject);
                // save to dataset
                dataset.add(new Item<>(
                        points != null && points >= 60.0 ? TYPE_SUBJECT_PASSED : TYPE_SUBJECT,
                        rvaSubject
                ));
            }
        } catch (Exception e) {
            log.exception(e);
        }
        return dataset;
    }
}
