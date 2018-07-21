package com.bukhmastov.cdoitmo.adapter.rva;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.regex.Pattern;

public class ERegisterSubjectsRVA extends RVA {

    private static final int TYPE_SUBJECT = 0;
    private static final int TYPE_SUBJECT_PASSED = 1;
    private static final int TYPE_NO_SUBJECTS = 2;

    private static Pattern patternExamOrCredit = Pattern.compile("^.*зач[её]т$|^экзамен$", Pattern.CASE_INSENSITIVE);

    public ERegisterSubjectsRVA(@NonNull Context context, @NonNull JSONArray subjects) {
        super();
        AppComponentProvider.getComponent().inject(this);
        addItems(json2dataset(context, subjects));
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
            case TYPE_NO_SUBJECTS: bindNoSubjects(container, item); break;
        }
    }

    private ArrayList<Item> json2dataset(@NonNull final Context context, @NonNull final JSONArray subjects) {
        final ArrayList<Item> dataset = new ArrayList<>();
        try {
            if (subjects.length() == 0) {
                dataset.add(getNewItem(TYPE_NO_SUBJECTS, null));
            } else {
                for (int i = 0; i < subjects.length(); i++) {
                    final JSONObject data = subjects.getJSONObject(i);
                    final JSONObject subject = data.getJSONObject("subject");
                    final int term = data.getInt("term");
                    // define variables
                    final String name = subject.getString("name");
                    final JSONArray attestations = subject.getJSONArray("attestations");
                    final ArrayList<String> attestationsArray = new ArrayList<>();
                    int attestationIndex = 0;
                    for (int j = 0; j < attestations.length(); j++) {
                        final JSONObject attestation = attestations.getJSONObject(j);
                        String aName = attestation.getString("name");
                        if (aName == null) continue;
                        aName = aName.trim();
                        if (aName.isEmpty()) continue;
                        attestationsArray.add(aName);
                        if (patternExamOrCredit.matcher(aName).find()) {
                            attestationIndex = j;
                        }
                    }
                    final double points = attestations.getJSONObject(attestationIndex).getDouble("value");
                    final String value = double2string(points);
                    final StringBuilder about = new StringBuilder(term + " " + context.getString(R.string.semester));
                    if (attestationsArray.size() > 0) {
                        about.append(" | ");
                        int j = 0;
                        for (String attestationString : attestationsArray) {
                            about.append(j++ > 0 ? ", " : "").append(attestationString);
                        }
                    }
                    // save to dataset
                    dataset.add(getNewItem(points >= 60.0 ? TYPE_SUBJECT_PASSED : TYPE_SUBJECT, new JSONObject()
                            .put("name", name)
                            .put("about", about)
                            .put("value", value)
                            .put("data", data)
                    ));
                }
            }
        } catch (Exception e) {
            log.exception(e);
        }
        return dataset;
    }

    private void bindSubject(View container, Item item) {
        try {
            ((TextView) container.findViewById(R.id.name)).setText(item.data.getString("name"));
            ((TextView) container.findViewById(R.id.about)).setText(item.data.getString("about"));
            ((TextView) container.findViewById(R.id.value)).setText(item.data.getString("value"));
            final JSONObject data = item.data.getJSONObject("data");
            if (onElementClickListeners.containsKey(R.id.subject)) {
                container.findViewById(R.id.subject).setOnClickListener(v -> onElementClickListeners.get(R.id.subject).onClick(v, getMap("data", data)));
            }
        } catch (Exception e) {
            log.exception(e);
        }
    }
    private void bindNoSubjects(View container, Item item) {
        try {
            ((TextView) container.findViewById(R.id.ntd_text)).setText(R.string.no_subjects);
        } catch (Exception e) {
            log.exception(e);
        }
    }
    private String double2string(Double value) {
        String valueStr = String.valueOf(value);
        if (value != -1.0) {
            if (value == Double.parseDouble(value.intValue() + ".0")) {
                valueStr = String.valueOf(value.intValue());
            }
        } else {
            valueStr = "";
        }
        return valueStr;
    }
}
