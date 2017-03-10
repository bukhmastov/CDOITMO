package com.bukhmastov.cdoitmo.adapters;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;

import java.util.ArrayList;
import java.util.HashMap;

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
        lv_protocol_name.setText(change.get("name"));
        lv_protocol_desc.setText(change.get("desc"));
        lv_protocol_meta.setText(change.get("meta"));
        lv_protocol_value.setText(change.get("value"));
        return rowView;
    }
}