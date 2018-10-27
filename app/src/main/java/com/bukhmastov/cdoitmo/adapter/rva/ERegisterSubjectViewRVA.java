package com.bukhmastov.cdoitmo.adapter.rva;

import android.content.Context;
import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import android.view.View;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.model.eregister.ERMark;
import com.bukhmastov.cdoitmo.model.eregister.ERPoint;
import com.bukhmastov.cdoitmo.model.eregister.ERSubject;
import com.bukhmastov.cdoitmo.model.rva.RVASubjectPoint;
import com.bukhmastov.cdoitmo.util.singleton.CollectionUtils;
import com.bukhmastov.cdoitmo.util.singleton.NumberUtils;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ERegisterSubjectViewRVA extends RVA<ERSubject> {

    private static final int TYPE_ATTESTATION = 0;
    private static final int TYPE_POINT_HIGHLIGHT = 1;
    private static final int TYPE_POINT = 2;
    private static final int TYPE_NO_POINTS = 3;

    private static final Pattern PATTERN_EXAM_OR_CREDIT = Pattern.compile("^.*зач[её]т$|^экзамен$|^тестирование$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_HIGHLIGHT = Pattern.compile("^.*зач[её]т$|^экзамен$|^модуль\\s\\d+$|^промежуточная\\sаттестация$|^защита\\s(кп/кр|кп|кр|курсового\\sпроекта|курсовой\\sработы|курсового\\sпроекта/курсовой\\sработы)$|^тесты$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_ATTESTATION = Pattern.compile("^семестр\\s\\d+$|^курсовая\\sработа$|^курсовой\\sпроект$|^КР$|^КП$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_SUBJECT_SUMMARY = Pattern.compile("^семестр\\s\\d+$|^общий\\sрейтинг$|^тесты$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_CUT_NAME_OF_CRITERIA = Pattern.compile("^(.*)\\(мод(уль)?\\.?.?\\d+\\)(.*)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_SKIP_CRITERIA = Pattern.compile("^другие\\sвиды\\sработ$", Pattern.CASE_INSENSITIVE);

    public ERegisterSubjectViewRVA(@NonNull Context context, @NonNull ERSubject subject, boolean advancedMode) {
        super();
        addItems(entity2dataset(context, subject, advancedMode));
    }

    @Override
    protected int onGetLayout(int type) throws NullPointerException {
        @LayoutRes int layout;
        switch (type) {
            case TYPE_ATTESTATION: layout = R.layout.layout_subject_show_mark; break;
            case TYPE_POINT_HIGHLIGHT: layout = R.layout.layout_subject_show_item_highlight; break;
            case TYPE_POINT: layout = R.layout.layout_subject_show_item; break;
            case TYPE_NO_POINTS: layout = R.layout.state_nothing_to_display_compact; break;
            default: throw new NullPointerException("Invalid type provided");
        }
        return layout;
    }

    @Override
    protected void onBind(View container, Item item) {
        switch (item.type) {
            case TYPE_ATTESTATION: bindAttestation(container, item); break;
            case TYPE_POINT_HIGHLIGHT: bindPoint(container, item, true); break;
            case TYPE_POINT: bindPoint(container, item, false); break;
            case TYPE_NO_POINTS: bindNoPoints(container); break;
        }
    }

    private void bindAttestation(View container, Item<RVASubjectPoint> item) {
        try {
            setTextToTextView(container, R.id.name, item.data.getName());
            setTextToTextView(container, R.id.term, item.data.getDesc());
            setTextToTextView(container, R.id.mark, item.data.getMark());
            setTextToTextView(container, R.id.value, item.data.getValue());
            container.findViewById(R.id.separator_top).setVisibility((boolean) item.extras.get("separator_top") ? View.VISIBLE : View.GONE);
            container.findViewById(R.id.separator_bottom).setVisibility((boolean) item.extras.get("separator_bottom") ? View.VISIBLE : View.GONE);
            container.findViewById(R.id.margin_top).setVisibility((boolean) item.extras.get("margin_top") ? View.VISIBLE : View.GONE);
        } catch (Exception e) {
            log.exception(e);
        }
    }
    private void bindPoint(View container, Item<RVASubjectPoint> item, boolean highlight) {
        try {
            setTextToTextView(container, R.id.name, item.data.getName());
            setTextToTextView(container, R.id.about, item.data.getDesc());
            setTextToTextView(container, R.id.value, item.data.getValue());
            if (highlight) {
                container.findViewById(R.id.separator_top).setVisibility((boolean) item.extras.get("separator_top") ? View.VISIBLE : View.GONE);
            }
            container.findViewById(R.id.separator_bottom).setVisibility((boolean) item.extras.get("separator_bottom") ? View.VISIBLE : View.GONE);
        } catch (Exception e) {
            log.exception(e);
        }
    }
    private void bindNoPoints(View container) {
        try {
            ((TextView) container.findViewById(R.id.ntd_text)).setText(R.string.no_points);
        } catch (Exception e) {
            log.exception(e);
        }
    }

    private ArrayList<Item> entity2dataset(@NonNull Context context, @NonNull ERSubject subject, boolean advancedMode) {
        final ArrayList<Item> dataset = new ArrayList<>();
        try {
            List<ERMark> marks = CollectionUtils.emptyIfNull(subject.getMarks());
            List<ERPoint> points = CollectionUtils.emptyIfNull(subject.getPoints());
            if (CollectionUtils.isEmpty(points)) {
                for (ERMark mark : marks) {
                    dataset.add(makeItem(new Item<>(TYPE_ATTESTATION, makeMark(context, subject, mark, null))));
                }
                dataset.add(makeItem(new Item(TYPE_NO_POINTS)));
            } else {
                if (!advancedMode) {
                    // simple mode
                    for (ERMark mark : marks) {
                        dataset.add(makeItem(new Item<>(TYPE_ATTESTATION, makeMark(context, subject, mark, null))));
                    }
                    for (ERPoint point : points) {
                        dataset.add(makeItem(new Item<>(TYPE_POINT, makePoint(point, false))));
                    }
                } else {
                    // post processing mode
                    List<String> attestationNames = new ArrayList<>();
                    for (ERMark mark : marks) {
                        if (StringUtils.isNotBlank(mark.getWorkType())) {
                            attestationNames.add(mark.getWorkType());
                        }
                    }
                    Collection<Attestation> attestations = makeAttestations(marks, points);
                    for (Attestation attestation : attestations) {
                        dataset.add(makeItem(new Item<>(TYPE_ATTESTATION, makeMark(context, subject, attestation.name, attestation.mark, attestation.value))));
                        for (ERPoint point : attestation.points) {
                            dataset.add(makeItem(new Item<>(makePointType(attestationNames, point), makePoint(point, true))));
                        }
                    }
                }
            }
            applyCosmeticsToDataset(dataset);
        } catch (Exception e) {
            log.exception(e);
        }
        return dataset;
    }

    private RVASubjectPoint makeMark(Context context, ERSubject subject, ERMark mark, Double points) {
        return makeMark(context, subject, mark.getWorkType(), mark.getMark(), points);
    }

    private RVASubjectPoint makeMark(Context context, ERSubject subject, String name, String mark, Double points) {
        RVASubjectPoint rvaSubjectPoint = new RVASubjectPoint();
        rvaSubjectPoint.setName(name);
        rvaSubjectPoint.setDesc(subject.getTerm() + " " + context.getString(R.string.semester));
        rvaSubjectPoint.setMark(mark);
        rvaSubjectPoint.setValue(NumberUtils.prettyDouble(points));
        return rvaSubjectPoint;
    }

    private RVASubjectPoint makePoint(ERPoint point, boolean advancedMode) {
        String name = point.getName();
        if (advancedMode && StringUtils.isNotBlank(name)) {
            Matcher m = PATTERN_CUT_NAME_OF_CRITERIA.matcher(point.getName());
            if (m.find()) {
                name = m.group(1) + m.group(3);
            }
        }
        RVASubjectPoint rvaSubjectPoint = new RVASubjectPoint();
        rvaSubjectPoint.setName(name);
        rvaSubjectPoint.setDesc("[0 / " + NumberUtils.prettyDouble(point.getLimit(), "0") + " / " + NumberUtils.prettyDouble(point.getMax(), "0") + "]");
        rvaSubjectPoint.setValue(NumberUtils.prettyDouble(point.getValue(), "0"));
        return rvaSubjectPoint;
    }

    private int makePointType(List<String> attestationNames, ERPoint point) {
        if (StringUtils.isBlank(point.getName())) {
            return TYPE_POINT;
        }
        if (attestationNames.contains(point.getName())) {
            return TYPE_POINT_HIGHLIGHT;
        }
        if (PATTERN_HIGHLIGHT.matcher(point.getName()).find()) {
            return TYPE_POINT_HIGHLIGHT;
        }
        return TYPE_POINT;
    }

    private Item<?> makeItem(Item<?> item) {
        item.extras.put("separator_top", true);
        item.extras.put("separator_bottom", true);
        item.extras.put("margin_top", true);
        return item;
    }

    private Collection<Attestation> makeAttestations(Collection<ERMark> marks, Collection<ERPoint> points) {
        Collection<Attestation> attestations = new ArrayList<>();
        boolean attestationsHaveExamOrCredit = false;
        if (CollectionUtils.isEmpty(marks)) {
            attestations.add(new Attestation());
        } else {
            for (ERMark mark : marks) {
                Attestation attestation = new Attestation();
                attestation.name = mark.getWorkType();
                attestation.mark = mark.getMark();
                attestations.add(attestation);
                if (!attestationsHaveExamOrCredit && PATTERN_EXAM_OR_CREDIT.matcher(mark.getWorkType()).find()) {
                    attestationsHaveExamOrCredit = true;
                }
            }
        }
        for (Attestation attestation : attestations) {
            Pattern patternStartFromThisCriteria;
            if (StringUtils.isBlank(attestation.name) || PATTERN_EXAM_OR_CREDIT.matcher(attestation.name).find()) {
                patternStartFromThisCriteria = PATTERN_SUBJECT_SUMMARY;
            } else {
                patternStartFromThisCriteria = Pattern.compile("^" + attestation.name.toLowerCase().replaceAll("\\s", "\\\\s") + "$", Pattern.CASE_INSENSITIVE);
            }
            boolean keepGoing = false;
            for (ERPoint point : points) {
                if (StringUtils.isBlank(point.getName()) || PATTERN_SKIP_CRITERIA.matcher(point.getName()).find()) {
                    continue;
                }
                if (!keepGoing && patternStartFromThisCriteria.matcher(point.getName()).find()) {
                    attestation.value = point.getValue();
                    keepGoing = true;
                } else if (keepGoing) {
                    if (PATTERN_ATTESTATION.matcher(point.getName()).find()) {
                        break;
                    } else {
                        attestation.points.add(point);
                    }
                }
            }
        }
        return attestations;
    }

    private void applyCosmeticsToDataset(List<Item> dataset) {
        int size = dataset.size();
        for (int i = 0; i < size; i++) {
            Item<?> item = dataset.get(i);
            if (i == 0) {
                item.extras.put("separator_top", false);
            }
            if (i + 1 < size) {
                Item<?> itemNext = dataset.get(i + 1);
                if (item.type == TYPE_ATTESTATION && itemNext.type == TYPE_ATTESTATION) {
                    item.extras.put("separator_bottom", false);
                    itemNext.extras.put("separator_bottom", false);
                }
                if (item.type == TYPE_ATTESTATION && itemNext.type == TYPE_POINT_HIGHLIGHT) {
                    item.extras.put("separator_bottom", false);
                }
                if (item.type == TYPE_POINT_HIGHLIGHT && itemNext.type == TYPE_ATTESTATION) {
                    item.extras.put("separator_bottom", false);
                }
                if (item.type == TYPE_POINT && (itemNext.type == TYPE_ATTESTATION || itemNext.type == TYPE_POINT_HIGHLIGHT)) {
                    item.extras.put("separator_bottom", false);
                }
                if (item.type != TYPE_NO_POINTS && itemNext.type == TYPE_NO_POINTS) {
                    item.extras.put("separator_bottom", false);
                }
            }
            if (i - 1 >= 0) {
                Item<?> itemPrevious = dataset.get(i - 1);
                if (item.type != TYPE_NO_POINTS && itemPrevious.type == TYPE_NO_POINTS) {
                    item.extras.put("separator_top", false);
                }
            }
            if (i + 2 < size && i - 1 >= 0) {
                Item<?> itemPrevious = dataset.get(i - 1);
                Item<?> itemNext = dataset.get(i + 1);
                Item<?> itemAfterNext = dataset.get(i + 2);
                if (itemPrevious.type == TYPE_ATTESTATION && item.type == TYPE_NO_POINTS && itemNext.type == TYPE_ATTESTATION && itemAfterNext.type == TYPE_NO_POINTS) {
                    item.extras.put("margin_top", false);
                    dataset.remove(i);
                    i--;
                    size--;
                }
            }
        }
    }

    private void setTextToTextView(View container, @IdRes int id, String text) {
        TextView tv = container.findViewById(id);
        if (StringUtils.isNotBlank(text)) {
            tv.setText(text);
            tv.setVisibility(View.VISIBLE);
        } else {
            tv.setVisibility(View.GONE);
        }
    }

    private class Attestation {
        private String name = "";
        private String mark = "";
        private Double value = -1.0;
        private ArrayList<ERPoint> points = new ArrayList<>();
    }
}
