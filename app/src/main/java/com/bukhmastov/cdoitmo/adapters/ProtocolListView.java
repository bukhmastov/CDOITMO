package com.bukhmastov.cdoitmo.adapters;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.utils.Static;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class ProtocolListView extends ArrayAdapter<HashMap<String, String>> {

    private final Activity context;
    private final ArrayList<HashMap<String, String>> changes;

    public ProtocolListView(Activity context, ArrayList<HashMap<String, String>> changes) {
        super(context, R.layout.listview_protocol, changes);
        this.context = context;
        this.changes = changes;
    }

    @NonNull
    @Override
    public View getView(int position, View view, @NonNull ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        HashMap<String, String> change = changes.get(position);
        View rowView;
        rowView = inflater.inflate(R.layout.listview_protocol, null, true);
        TextView lv_protocol_name = ((TextView) rowView.findViewById(R.id.lv_protocol_name));
        TextView lv_protocol_desc = ((TextView) rowView.findViewById(R.id.lv_protocol_desc));
        TextView lv_protocol_meta = ((TextView) rowView.findViewById(R.id.lv_protocol_meta));
        TextView lv_protocol_value = ((TextView) rowView.findViewById(R.id.lv_protocol_value));
        TextView lv_protocol_delta = ((TextView) rowView.findViewById(R.id.lv_protocol_delta));
        lv_protocol_name.setText(change.get("name"));
        lv_protocol_desc.setText(change.get("desc"));
        lv_protocol_meta.setText(change.get("meta"));
        lv_protocol_value.setText(change.get("value"));
        if (Objects.equals(change.get("delta_here"), "true")) {
            lv_protocol_delta.setText(change.get("delta"));
            try {
                lv_protocol_delta.setTextColor(Static.resolveColor(context, Objects.equals(change.get("delta_negative"), "true") ? R.attr.textColorDegrade : R.attr.textColorPassed));
            } catch (Exception e) {
                Static.error(e);
            }
        } else {
            lv_protocol_delta.setWidth(0);
            lv_protocol_delta.setHeight(0);
        }
        return rowView;
    }
}