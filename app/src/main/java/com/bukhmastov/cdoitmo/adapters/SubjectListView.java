package com.bukhmastov.cdoitmo.adapters;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.utils.Static;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.regex.Pattern;

public class SubjectListView extends ArrayAdapter<JSONObject> {

    private static Pattern patternExamOrCredit = Pattern.compile("^зач[её]т$|^экзамен$", Pattern.CASE_INSENSITIVE);
    private final Activity context;
    private final ArrayList<JSONObject> subj;
    private int colorOnGoing;
    private int colorPassed;

    public SubjectListView(Activity context, ArrayList<JSONObject> subj) {
        super(context, R.layout.listview_subject, subj);
        this.context = context;
        this.subj = subj;
        try {
            this.colorOnGoing = Static.resolveColor(context, android.R.attr.textColorPrimary);
        } catch (Exception e) {
            this.colorOnGoing = -1;
        }
        try {
            this.colorPassed = Static.resolveColor(context, R.attr.colorPositiveTrend);
        } catch (Exception e) {
            this.colorPassed = -1;
        }
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        try {
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.listview_subject, parent, false);
            }
            final JSONObject data = subj.get(position);
            final JSONObject subject = data.getJSONObject("subject");
            final int term = data.getInt("term");
            // define variables
            final String name = subject.getString("name");
            final JSONArray attestations = subject.getJSONArray("attestations");
            final ArrayList<String> attestationsArray = new ArrayList<>();
            int attestationIndex = 0;
            for (int i = 0; i < attestations.length(); i++) {
                final JSONObject attestation = attestations.getJSONObject(i);
                String aName = attestation.getString("name");
                if (aName == null) continue;
                aName = aName.trim();
                if (aName.isEmpty()) continue;
                attestationsArray.add(aName);
                if (patternExamOrCredit.matcher(aName).find()) {
                    attestationIndex = i;
                }
            }
            final double points = attestations.getJSONObject(attestationIndex).getDouble("value");
            final String value = double2string(points);
            final StringBuilder about = new StringBuilder(term + " " + context.getString(R.string.semester));
            if (attestationsArray.size() > 0) {
                about.append(" | ");
                int i = 0;
                for (String attestationString : attestationsArray) {
                    about.append(i++ > 0 ? ", " : "").append(attestationString);
                }
            }
            // set UI
            ((TextView) convertView.findViewById(R.id.name)).setText(name);
            ((TextView) convertView.findViewById(R.id.about)).setText(about.toString());
            ((TextView) convertView.findViewById(R.id.value)).setText(value);
            if (points >= 60.0 && colorPassed != -1) {
                ((TextView) convertView.findViewById(R.id.name)).setTextColor(colorPassed);
                ((TextView) convertView.findViewById(R.id.about)).setTextColor(colorPassed);
                ((TextView) convertView.findViewById(R.id.value)).setTextColor(colorPassed);
            } else if (colorOnGoing != -1) {
                ((TextView) convertView.findViewById(R.id.name)).setTextColor(colorOnGoing);
                ((TextView) convertView.findViewById(R.id.about)).setTextColor(colorOnGoing);
                ((TextView) convertView.findViewById(R.id.value)).setTextColor(colorOnGoing);
            }
            return convertView;
        } catch (Exception e) {
            Static.error(e);
            return super.getView(position, convertView, parent);
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
