package com.bukhmastov.cdoitmo.converter;

import com.bukhmastov.cdoitmo.firebase.FirebasePerformanceProvider;
import com.bukhmastov.cdoitmo.util.Time;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ERegisterConverter extends Converter {

    private final JSONObject eregister;

    private static Pattern patternExamOrCredit = Pattern.compile("^.*зач[её]т$|^экзамен$", Pattern.CASE_INSENSITIVE); // |^дифференцированный\sзач[её]т$|^дифф\.\sзач[её]т$
    private static Pattern patternSkipCriteria = Pattern.compile("^другие\\sвиды\\sработ$", Pattern.CASE_INSENSITIVE);
    private static Pattern patternSubjectSummary = Pattern.compile("^семестр\\s\\d+$", Pattern.CASE_INSENSITIVE);
    private static Pattern patternAttestations = Pattern.compile("^семестр\\s\\d+$|^курсовая\\sработа$|^курсовой\\sпроект$|^КР$|^КП$", Pattern.CASE_INSENSITIVE);
    private static Pattern patternCutNameOfCriteria = Pattern.compile("^(.*)\\(мод(уль)?\\.?.?\\d+\\)(.*)$", Pattern.CASE_INSENSITIVE);

    public ERegisterConverter(JSONObject eregister, Response delegate) {
        super(delegate);
        this.eregister = eregister;
    }

    @Override
    protected JSONObject convert() throws Throwable {
        final JSONArray years = eregister.getJSONArray("years");
        final JSONArray groups = new JSONArray();
        for (int i = 0; i < years.length(); i++) {
            final JSONObject year = years.getJSONObject(i);
            final String group = year.getString("group");
            final String[] studyyear = year.getString("studyyear").split("/");
            final JSONArray subjects = year.getJSONArray("subjects");
            final int year1 = Integer.parseInt(studyyear.length > 0 ? (!studyyear[0].isEmpty() ? studyyear[0] : "0") : "0");
            final int year2 = Integer.parseInt(studyyear.length > 1 ? (!studyyear[1].isEmpty() ? studyyear[1] : "0") : "0");
            final JSONArray terms = getTermsTemplates(subjects);
            for (int j = 0; j < subjects.length(); j++) {
                final JSONObject subject = subjects.getJSONObject(j);
                final int number = Integer.parseInt(subject.getString("semester"));
                final String name = subject.getString("name");
                final JSONArray marks = subject.has("marks") ? subject.getJSONArray("marks") : new JSONArray();
                final JSONArray points = subject.has("points") ? subject.getJSONArray("points") : new JSONArray();
                // skip invalid subject
                if (name == null || name.trim().isEmpty()) {
                    continue;
                }
                // create attestations from raw data
                boolean attestationsHaveExamOrCredit = false;
                JSONArray attestations = new JSONArray();
                if (marks.length() == 0) {
                    attestations.put(new JSONObject()
                            .put("name", "")
                            .put("mark", "")
                            .put("markdate", "")
                            .put("value", -1.0)
                            .put("points", new JSONArray())
                    );
                } else {
                    for (int k = 0; k < marks.length(); k++) {
                        final JSONObject mark = marks.getJSONObject(k);
                        final String worktype = mark.getString("worktype").trim();
                        attestations.put(new JSONObject()
                                .put("name", worktype)
                                .put("mark", mark.getString("mark"))
                                .put("markdate", mark.getString("markdate"))
                                .put("value", -1.0)
                                .put("points", new JSONArray())
                        );
                        if (!attestationsHaveExamOrCredit && patternExamOrCredit.matcher(worktype).find()) {
                            attestationsHaveExamOrCredit = true;
                        }
                    }
                }
                // distribute each criteria to corresponding attestation
                for (int k = 0; k < attestations.length(); k++) {
                    JSONObject attestation = attestations.getJSONObject(k);
                    String attestationName = attestation.getString("name");
                    final Pattern patternStartFromThisCriteria;
                    if (attestationName.isEmpty() || patternExamOrCredit.matcher(attestationName).find()) {
                        patternStartFromThisCriteria = patternSubjectSummary;
                    } else {
                        patternStartFromThisCriteria = Pattern.compile("^" + attestationName.toLowerCase().replaceAll("\\s", "\\\\s") + "$", Pattern.CASE_INSENSITIVE);
                    }
                    boolean keepGoing = false;
                    for (int a = 0; a < points.length(); a++) {
                        final JSONObject point = points.getJSONObject(a);
                        final String variable = point.getString("variable").trim();
                        final String value = point.getString("value").trim();
                        final String limit = point.getString("limit").trim();
                        final String max = point.getString("max").trim();
                        if (variable.isEmpty() || patternSkipCriteria.matcher(variable).find()) {
                            continue;
                        }
                        if (!keepGoing && patternStartFromThisCriteria.matcher(variable).find()) {
                            keepGoing = true;
                            attestation.put("value", mark2double(value));
                        } else if (keepGoing) {
                            if (patternAttestations.matcher(variable).find()) {
                                break;
                            } else {
                                Matcher m = patternCutNameOfCriteria.matcher(variable);
                                attestation.getJSONArray("points").put(new JSONObject()
                                        .put("name", m.find() ? (m.group(1) + m.group(3)).trim() : variable)
                                        .put("value", value.isEmpty() ? 0.0 : mark2double(value))
                                        .put("limit", limit.isEmpty() ? 0.0 : mark2double(limit))
                                        .put("max", max.isEmpty() ? 0.0 : mark2double(max))
                                );
                            }
                        }
                    }
                }
                // place subject to corresponding term array
                for (int k = 0; k < terms.length(); k++) {
                    JSONObject term = terms.getJSONObject(k);
                    if (term.getInt("number") == number) {
                        term.getJSONArray("subjects").put(new JSONObject()
                                .put("name", name.trim())
                                .put("attestations", attestations)
                        );
                        break;
                    }
                }
            }
            // sort subjects at each term
            for (int j = 0; j < terms.length(); j++) {
                terms.getJSONObject(j).put("subjects", sortSubjects(terms.getJSONObject(j).getJSONArray("subjects")));
            }
            // place terms with subjects to groups array
            groups.put(new JSONObject()
                    .put("name", group)
                    .put("years", new JSONArray()
                            .put(Math.min(year1, year2))
                            .put(Math.max(year1, year2))
                    )
                    .put("terms", terms)
            );
        }
        return new JSONObject()
                .put("groups", groups)
                .put("timestamp", time.getCalendar().getTimeInMillis());
    }

    @Override
    protected String getTraceName() {
        return FirebasePerformanceProvider.Trace.Convert.EREGISTER;
    }

    private JSONArray getTermsTemplates(JSONArray subjects) throws Exception {
        int term1 = -1, term2 = -1;
        for (int i = 0; i < subjects.length(); i++) {
            try {
                int term = Integer.parseInt(subjects.getJSONObject(i).getString("semester"));
                if (term1 < 1 && term != term1) {
                    term1 = term;
                } else if (term2 < 1 && term != term1 && term != term2) {
                    term2 = term;
                }
                if (term1 > 0 && term2 > 0) {
                    break;
                }
            } catch (Exception ignore) {/* ignore */}
        }
        int first = Math.min(term1, term2);
        int second = Math.max(term1, term2);
        if (first == -1 && second != -1) {
            if (second % 2 == 0) {
                first = second - 1;
            } else {
                first = second + 1;
            }
        }
        return new JSONArray()
                .put(new JSONObject().put("number", Math.min(first, second)).put("subjects", new JSONArray()))
                .put(new JSONObject().put("number", Math.max(first, second)).put("subjects", new JSONArray()));
    }
    private double mark2double(String string) {
        return string2double(markConverter(string));
    }
    private double string2double(String string) {
        try {
            return Double.parseDouble(string);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    private String markConverter(String value) {
        Matcher m;
        value = value.replace(",", ".").trim();
        m = Pattern.compile("^\\.(.+)").matcher(value);
        if (m.find()) value = "0." + m.group(1);
        m = Pattern.compile("(.+)\\.0$").matcher(value);
        if (m.find()) value = m.group(1);
        return value;
    }
    private JSONArray sortSubjects(JSONArray subjects) throws JSONException {
        ArrayList<JSONObject> sort = new ArrayList<>();
        for (int i = 0; i < subjects.length(); i++) {
            sort.add(subjects.getJSONObject(i));
        }
        Collections.sort(sort, (o1, o2) -> {
            try {
                return o1.getString("name").compareTo(o2.getString("name"));
            } catch (JSONException e) {
                return 0;
            }
        });
        return new JSONArray(sort);
    }
}
