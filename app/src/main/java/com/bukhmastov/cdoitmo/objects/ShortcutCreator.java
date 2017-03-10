package com.bukhmastov.cdoitmo.objects;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.adapters.TeacherPickerListView;
import com.bukhmastov.cdoitmo.receivers.ShortcutReceiver;
import com.bukhmastov.cdoitmo.utils.Static;
import com.loopj.android.http.RequestHandle;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class ShortcutCreator {

    public interface response {
        void onDisplay(View view);
    }
    private Context context;
    private response delegate;
    private int step = 0;
    private boolean displayed = false;
    public static RequestHandle shortcutRequestHandle = null;
    private enum TYPES {none, e_journal, protocol_changes, rating, room101, room101create, schedule_lessons, schedule_exams, time_remaining_widget}
    private TYPES type = TYPES.none;
    private Additional additional = null;

    public ShortcutCreator(Context context, response response){
        this.context = context;
        this.delegate = response;
    }

    public void onResume(){
        if (!displayed) {
            start();
            displayed = true;
        }
    }
    public void onPause(){
        if (shortcutRequestHandle != null) shortcutRequestHandle.cancel(true);
        displayed = false;
    }

    private void start(){
        step = 0;
        next();
    }
    private void next(){
        step++;
        proceedStep();
    }
    private void back(){
        step--;
        proceedStep();
    }
    private void proceedStep(){
        switch (step) {
            case 1: step1(); break;
            case 2: step2(); break;
            case 3: step3(); break;
            default: start(); break;
        }
    }
    private void step1(){
        try {
            LinearLayout content = new LinearLayout(context);
            content.setOrientation(LinearLayout.VERTICAL);
            content.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            type = TYPES.none;
            View layout_vertical_stepper_item = inflate(R.layout.layout_vertical_stepper_item);
            ((TextView) layout_vertical_stepper_item.findViewById(R.id.stepper_step_number)).setText("1");
            ((TextView) layout_vertical_stepper_item.findViewById(R.id.stepper_header)).setText(R.string.choose_shortcut);
            ViewGroup stepper_content = (ViewGroup) inflate(R.layout.layout_shortcut_creator_step_1);
            Spinner shortcut_creator_spinner = (Spinner) stepper_content.findViewById(R.id.shortcut_creator_spinner);
            final ArrayList<String> spinner_layout_normal_labels = new ArrayList<>();
            final ArrayList<TYPES> spinner_layout_normal_values = new ArrayList<>();
            for (TYPES type : TYPES.values()) {
                spinner_layout_normal_labels.add(getTypeLabel(type));
                spinner_layout_normal_values.add(type);
            }
            shortcut_creator_spinner.setAdapter(new ArrayAdapter<>(context, R.layout.spinner_layout_normal, spinner_layout_normal_labels));
            shortcut_creator_spinner.setSelection(0);
            shortcut_creator_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                public void onItemSelected(AdapterView<?> parent, View item, int position, long selectedId) {
                    type = spinner_layout_normal_values.get(position);
                    if (type != TYPES.none) next();
                }
                public void onNothingSelected(AdapterView<?> parent) {}
            });
            ((ViewGroup) layout_vertical_stepper_item.findViewById(R.id.stepper_content)).addView(stepper_content);
            content.addView(layout_vertical_stepper_item);
            delegate.onDisplay(content);
        } catch (Exception e) {
            Static.error(e);
            error();
        }
    }
    private void step2(){
        try {
            final LinearLayout content = new LinearLayout(context);
            content.setOrientation(LinearLayout.VERTICAL);
            content.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            if (type == TYPES.none) {
                back();
                return;
            } else {
                content.addView(getDone("1", context.getString(R.string.choose_shortcut), context.getString(R.string.shortcut_choosed) + ": " + getTypeLabel(type)));
            }
            switch (type) {
                case e_journal:
                case protocol_changes:
                case rating:
                case room101:
                case room101create: {
                    View layout_vertical_stepper_item = inflate(R.layout.layout_vertical_stepper_item);
                    ((TextView) layout_vertical_stepper_item.findViewById(R.id.stepper_step_number)).setText("2");
                    ((TextView) layout_vertical_stepper_item.findViewById(R.id.stepper_header)).setText(R.string.compete);
                    ((ViewGroup) layout_vertical_stepper_item.findViewById(R.id.stepper_content)).addView(getSubmitButton());
                    content.addView(layout_vertical_stepper_item);
                    break;
                }
                case schedule_lessons:
                case time_remaining_widget: {
                    additional = null;
                    final ScheduleLessonsProvider scheduleLessonsProvider = new ScheduleLessonsProvider(context);
                    View layout_vertical_stepper_item = inflate(R.layout.layout_vertical_stepper_item);
                    ((TextView) layout_vertical_stepper_item.findViewById(R.id.stepper_step_number)).setText("2");
                    ((TextView) layout_vertical_stepper_item.findViewById(R.id.stepper_header)).setText(R.string.choose_schedule_for_display);
                    ViewGroup stepper_content = (ViewGroup) inflate(R.layout.layout_shortcut_creator_step_choose_schedule);
                    EditText shortcut_creator_input = (EditText) stepper_content.findViewById(R.id.shortcut_creator_input);
                    shortcut_creator_input.setHint(R.string.schedule_lessons_search_view_hint);
                    shortcut_creator_input.requestFocus();
                    shortcut_creator_input.setOnKeyListener(new View.OnKeyListener() {
                        public boolean onKey(View v, int keyCode, KeyEvent event) {
                            return event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER && scheduleLessonsProvider.setQuery();
                        }
                    });
                    final FrameLayout shortcut_creator_result = (FrameLayout) stepper_content.findViewById(R.id.shortcut_creator_result);
                    ((ViewGroup) layout_vertical_stepper_item.findViewById(R.id.stepper_content)).addView(stepper_content);
                    content.addView(layout_vertical_stepper_item);
                    scheduleLessonsProvider.setHandler(new ScheduleLessons.response() {
                        @Override
                        public void onProgress(int state) {
                            try {
                                if (shortcut_creator_result != null) {
                                    shortcut_creator_result.removeAllViews();
                                    shortcut_creator_result.addView(inflate(R.layout.state_loading_compact));
                                }
                            } catch (Exception e) {
                                Static.error(e);
                            }
                        }
                        @Override
                        public void onFailure(int state) {
                            try {
                                if (shortcut_creator_result != null) {
                                    shortcut_creator_result.removeAllViews();
                                    shortcut_creator_result.addView(inflate(R.layout.state_failed_compact));
                                }
                            } catch (Exception e) {
                                Static.error(e);
                            }
                        }
                        @Override
                        public void onSuccess(JSONObject json) {
                            try {
                                if (json == null) throw new Exception("json is null");
                                if (Objects.equals(json.getString("type"), "teacher_picker")) {
                                    JSONArray teachers = json.getJSONArray("list");
                                    if (teachers.length() > 0) {
                                        if (teachers.length() == 1) {
                                            JSONObject teacher = teachers.getJSONObject(0);
                                            additional = new Additional(teacher.getString("person") + " (" + teacher.getString("post")+ ")", teacher.getString("pid"));
                                            next();
                                        } else {
                                            ListView listView = new ListView(context);
                                            listView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
                                            listView.setMinimumHeight((int) (Static.destiny * 120));
                                            final ArrayList<HashMap<String, String>> teachersMap = new ArrayList<>();
                                            for (int i = 0; i < teachers.length(); i++) {
                                                JSONObject teacher = teachers.getJSONObject(i);
                                                HashMap<String, String> teacherMap = new HashMap<>();
                                                teacherMap.put("pid", String.valueOf(teacher.getInt("pid")));
                                                teacherMap.put("person", teacher.getString("person"));
                                                teacherMap.put("post", teacher.getString("post"));
                                                teachersMap.add(teacherMap);
                                            }
                                            listView.setAdapter(new TeacherPickerListView((Activity) context, teachersMap));
                                            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                                    HashMap<String, String> teacherMap = teachersMap.get(position);
                                                    additional = new Additional(teacherMap.get("person") + " (" + teacherMap.get("post")+ ")", teacherMap.get("pid"));
                                                    next();
                                                }
                                            });
                                            shortcut_creator_result.removeAllViews();
                                            shortcut_creator_result.addView(listView);
                                        }
                                    } else {
                                        shortcut_creator_result.removeAllViews();
                                        shortcut_creator_result.addView(scheduleLessonsProvider.getFailed(context.getString(R.string.schedule_not_found)));
                                    }
                                } else {
                                    additional = new Additional(json.getString("label"), json.getString("query"));
                                    next();
                                }
                            } catch (Exception e) {
                                if (shortcut_creator_result != null) {
                                    try {
                                        shortcut_creator_result.removeAllViews();
                                        shortcut_creator_result.addView(scheduleLessonsProvider.getFailed(context.getString(R.string.schedule_not_found)));
                                    } catch (Exception e1) {
                                        Static.error(e1);
                                    }
                                }
                                Static.error(e);
                            }

                        }
                        @Override
                        public void onNewHandle(RequestHandle requestHandle) {
                            shortcutRequestHandle = requestHandle;
                        }
                    });
                    break;
                }
                case schedule_exams: {
                    additional = null;
                    final ScheduleExamsProvider scheduleExamsProvider = new ScheduleExamsProvider(context);
                    View layout_vertical_stepper_item = inflate(R.layout.layout_vertical_stepper_item);
                    ((TextView) layout_vertical_stepper_item.findViewById(R.id.stepper_step_number)).setText("2");
                    ((TextView) layout_vertical_stepper_item.findViewById(R.id.stepper_header)).setText(R.string.choose_schedule_for_display);
                    ViewGroup stepper_content = (ViewGroup) inflate(R.layout.layout_shortcut_creator_step_choose_schedule);
                    EditText shortcut_creator_input = (EditText) stepper_content.findViewById(R.id.shortcut_creator_input);
                    shortcut_creator_input.setHint(R.string.schedule_lessons_search_view_hint);
                    shortcut_creator_input.requestFocus();
                    shortcut_creator_input.setOnKeyListener(new View.OnKeyListener() {
                        public boolean onKey(View v, int keyCode, KeyEvent event) {
                            return event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER && scheduleExamsProvider.setQuery();
                        }
                    });
                    final FrameLayout shortcut_creator_result = (FrameLayout) stepper_content.findViewById(R.id.shortcut_creator_result);
                    ((ViewGroup) layout_vertical_stepper_item.findViewById(R.id.stepper_content)).addView(stepper_content);
                    content.addView(layout_vertical_stepper_item);
                    scheduleExamsProvider.setHandler(new ScheduleExams.response() {
                        @Override
                        public void onProgress(int state) {
                            try {
                                if (shortcut_creator_result != null) {
                                    shortcut_creator_result.removeAllViews();
                                    shortcut_creator_result.addView(inflate(R.layout.state_loading_compact));
                                }
                            } catch (Exception e) {
                                Static.error(e);
                            }
                        }
                        @Override
                        public void onFailure(int state) {
                            try {
                                if (shortcut_creator_result != null) {
                                    shortcut_creator_result.removeAllViews();
                                    shortcut_creator_result.addView(inflate(R.layout.state_failed_compact));
                                }
                            } catch (Exception e) {
                                Static.error(e);
                            }
                        }
                        @Override
                        public void onSuccess(JSONObject json) {
                            try {
                                if (json == null) throw new Exception("json is null");
                                if (Objects.equals(json.getString("type"), "teacher_picker")) {
                                    JSONArray teachers = json.getJSONArray("teachers");
                                    if (teachers.length() > 0) {
                                        if (teachers.length() == 1) {
                                            JSONObject teacher = teachers.getJSONObject(0);
                                            additional = new Additional(teacher.getString("name"), teacher.getString("scope"));
                                            next();
                                        } else {
                                            ListView listView = new ListView(context);
                                            listView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
                                            listView.setMinimumHeight((int) (Static.destiny * 120));
                                            final ArrayList<HashMap<String, String>> teachersMap = new ArrayList<>();
                                            for (int i = 0; i < teachers.length(); i++) {
                                                JSONObject teacher = teachers.getJSONObject(i);
                                                HashMap<String, String> teacherMap = new HashMap<>();
                                                teacherMap.put("pid", teacher.getString("scope"));
                                                teacherMap.put("person", teacher.getString("name"));
                                                teacherMap.put("post", "");
                                                teachersMap.add(teacherMap);
                                            }
                                            listView.setAdapter(new TeacherPickerListView((Activity) context, teachersMap));
                                            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                                    HashMap<String, String> teacherMap = teachersMap.get(position);
                                                    additional = new Additional(teacherMap.get("person"), teacherMap.get("pid"));
                                                    next();
                                                }
                                            });
                                            shortcut_creator_result.removeAllViews();
                                            shortcut_creator_result.addView(listView);
                                        }
                                    } else {
                                        shortcut_creator_result.removeAllViews();
                                        shortcut_creator_result.addView(scheduleExamsProvider.getFailed(context.getString(R.string.schedule_not_found)));
                                    }
                                } else {
                                    JSONArray schedule = json.getJSONArray("schedule");
                                    if (schedule.length() > 0) {
                                        additional = new Additional(json.getString("scope"), json.getString("scope"));
                                        next();
                                    } else {
                                        shortcut_creator_result.removeAllViews();
                                        shortcut_creator_result.addView(scheduleExamsProvider.getFailed(context.getString(R.string.schedule_not_found)));
                                    }
                                }
                            } catch (Exception e) {
                                if (shortcut_creator_result != null) {
                                    try {
                                        shortcut_creator_result.removeAllViews();
                                        shortcut_creator_result.addView(scheduleExamsProvider.getFailed(context.getString(R.string.schedule_not_found)));
                                    } catch (Exception e1) {
                                        Static.error(e1);
                                    }
                                }
                                Static.error(e);
                            }

                        }
                        @Override
                        public void onNewHandle(RequestHandle requestHandle) {
                            shortcutRequestHandle = requestHandle;
                        }
                    });
                    break;
                }
            }
            delegate.onDisplay(content);
        } catch (Exception e) {
            Static.error(e);
            error();
        }
    }
    private void step3(){
        try {
            LinearLayout content = new LinearLayout(context);
            content.setOrientation(LinearLayout.VERTICAL);
            content.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            if (type == TYPES.none || additional == null) {
                back();
                return;
            } else {
                content.addView(getDone("1", context.getString(R.string.choose_shortcut), context.getString(R.string.shortcut_choosed) + ": " + getTypeLabel(type)));
                content.addView(getDone("2", context.getString(R.string.choose_schedule_for_display), context.getString(R.string.chose_additional_done) + ": " + additional.label));
            }
            View layout_vertical_stepper_item = inflate(R.layout.layout_vertical_stepper_item);
            ((TextView) layout_vertical_stepper_item.findViewById(R.id.stepper_step_number)).setText("3");
            ((TextView) layout_vertical_stepper_item.findViewById(R.id.stepper_header)).setText(R.string.compete);
            ((ViewGroup) layout_vertical_stepper_item.findViewById(R.id.stepper_content)).addView(getSubmitButton());
            content.addView(layout_vertical_stepper_item);
            delegate.onDisplay(content);
        } catch (Exception e) {
            Static.error(e);
            error();
        }
    }
    private void complete(){
        switch (type) {
            case e_journal: {
                addShortcut("tab", "e_journal");
                break;
            }
            case protocol_changes: {
                addShortcut("tab", "protocol_changes");
                break;
            }
            case rating: {
                addShortcut("tab", "rating");
                break;
            }
            case room101: {
                addShortcut("tab", "room101");
                break;
            }
            case room101create: {
                addShortcut("room101", "create");
                break;
            }
            case schedule_lessons:
            case time_remaining_widget:
            case schedule_exams: {
                try {
                    JSONObject json = new JSONObject();
                    json.put("label", additional.label);
                    json.put("query", additional.query);
                    switch (type) {
                        case schedule_lessons: addShortcut("schedule_lessons", json.toString()); break;
                        case time_remaining_widget: addShortcut("time_remaining_widget", json.toString()); break;
                        case schedule_exams: addShortcut("schedule_exams", json.toString()); break;
                    }
                } catch (Exception e) {
                    Static.error(e);
                }
                break;
            }
            default: {
                error();
                return;
            }
        }
        start();
    }

    private class ScheduleLessonsProvider {
        private Context context;
        private ScheduleLessons scheduleLessons;
        ScheduleLessonsProvider(Context context) {
            this.context = context;
        }
        void setHandler(ScheduleLessons.response delegate){
            this.scheduleLessons = new ScheduleLessons(context);
            this.scheduleLessons.setHandler(delegate);
        }
        boolean setQuery(){
            if (scheduleLessons == null) return false;
            EditText shortcut_creator_input = (EditText) ((Activity) context).findViewById(R.id.shortcut_creator_input);
            if (shortcut_creator_input != null) {
                String search = shortcut_creator_input.getText().toString().trim();
                if (!Objects.equals(search, "")) {
                    scheduleLessons.search(search);
                    shortcut_creator_input.clearFocus();
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
        View getFailed(String text) throws Exception {
            View state_failed_compact = inflate(R.layout.state_failed_compact);
            ((TextView) state_failed_compact.findViewById(R.id.state_failed_compact_message)).setText(text);
            return state_failed_compact;
        }
    }
    private class ScheduleExamsProvider {
        private Context context;
        private ScheduleExams scheduleExams;
        ScheduleExamsProvider(Context context) {
            this.context = context;
        }
        void setHandler(ScheduleExams.response delegate){
            this.scheduleExams = new ScheduleExams(context);
            this.scheduleExams.setHandler(delegate);
        }
        boolean setQuery(){
            if (scheduleExams == null) return false;
            EditText shortcut_creator_input = (EditText) ((Activity) context).findViewById(R.id.shortcut_creator_input);
            if (shortcut_creator_input != null) {
                String search = shortcut_creator_input.getText().toString().trim();
                if (!Objects.equals(search, "")) {
                    scheduleExams.search(search);
                    shortcut_creator_input.clearFocus();
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
        View getFailed(String text) throws Exception {
            View state_failed_compact = inflate(R.layout.state_failed_compact);
            ((TextView) state_failed_compact.findViewById(R.id.state_failed_compact_message)).setText(text);
            return state_failed_compact;
        }
    }
    private class Additional {
        String label;
        String query;
        Additional(String label, String query){
            this.label = label;
            this.query = query;
        }
    }
    private void error(){

    }

    @Nullable
    private String getTypeLabel(TYPES type){
        switch (type) {
            case none: return context.getString(R.string.shortcut_creator_need_to_choose);
            case e_journal: return context.getString(R.string.e_journal);
            case protocol_changes: return context.getString(R.string.protocol_changes);
            case rating: return context.getString(R.string.rating);
            case room101: return context.getString(R.string.room101);
            case room101create: return context.getString(R.string.room101create);
            case schedule_lessons: return context.getString(R.string.schedule_lessons);
            case schedule_exams: return context.getString(R.string.schedule_exams);
            case time_remaining_widget: return context.getString(R.string.time_remaining_widget);
            default: return null;
        }
    }
    private View getDone(String step, String header, String content) throws Exception {
        View layout_vertical_stepper_item = inflate(R.layout.layout_vertical_stepper_item);
        ((TextView) layout_vertical_stepper_item.findViewById(R.id.stepper_step_number)).setText(step);
        ((TextView) layout_vertical_stepper_item.findViewById(R.id.stepper_header)).setText(header);
        ViewGroup stepper_content = (ViewGroup) inflate(R.layout.layout_shortcut_creator_step_done);
        ((TextView) stepper_content.findViewById(R.id.shortcut_creator_result)).setText(content);
        ((ViewGroup) layout_vertical_stepper_item.findViewById(R.id.stepper_content)).addView(stepper_content);
        return layout_vertical_stepper_item;
    }
    private View getSubmitButton() throws Exception {
        View stepper_content = inflate(R.layout.layout_shortcut_creator_step_submit);
        stepper_content.findViewById(R.id.shortcut_creator_finish).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                complete();
            }
        });
        return stepper_content;
    }
    private View inflate(int layout) throws Exception {
        return ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layout, null);
    }

    private void addShortcut(String type, String data){
        Intent intent = new Intent(ShortcutReceiver.ACTION_ADD_SHORTCUT);
        intent.putExtra(ShortcutReceiver.EXTRA_TYPE, type);
        intent.putExtra(ShortcutReceiver.EXTRA_DATA, data);
        context.sendBroadcast(intent);
    }

}
