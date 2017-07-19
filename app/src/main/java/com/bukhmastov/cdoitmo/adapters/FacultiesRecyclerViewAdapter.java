package com.bukhmastov.cdoitmo.adapters;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.DrawableRes;
import android.support.annotation.LayoutRes;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activities.UniversityPersonCardActivity;
import com.bukhmastov.cdoitmo.utils.CircularTransformation;
import com.bukhmastov.cdoitmo.utils.Static;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class FacultiesRecyclerViewAdapter extends UniversityRecyclerViewAdapter {

    public FacultiesRecyclerViewAdapter(final Context context) {
        super(context, null);
    }
    public FacultiesRecyclerViewAdapter(final Context context, final ArrayList<Item> dataset) {
        super(context, dataset);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case TYPE_UNIT_STRUCTURE_COMMON:
            case TYPE_UNIT_STRUCTURE_DEANERY:
            case TYPE_UNIT_STRUCTURE_HEAD:
            case TYPE_UNIT_DIVISIONS: {
                return new ViewHolder((ViewGroup) LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_university_faculties_structure_unit, parent, false));
            }
        }
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        Item item = dataset.get(position);
        switch (item.type) {
            case TYPE_UNIT_STRUCTURE_COMMON:
            case TYPE_UNIT_STRUCTURE_DEANERY:
            case TYPE_UNIT_STRUCTURE_HEAD: {
                bindStructure(holder, item);
                break;
            }
            case TYPE_UNIT_DIVISIONS: {
                bindDivisions(holder, item);
                break;
            }
        }
    }

    private void bindStructure(RecyclerView.ViewHolder holder, Item item) {
        try {
            ViewHolder viewHolder = (ViewHolder) holder;
            removeFirstSeparator(viewHolder.container);
            String header = item.data.getString("header");
            if (header != null) {
                ((TextView) viewHolder.container.findViewById(R.id.structure_header)).setText(Static.capitalizeFirstLetter(header.trim()));
            } else {
                Static.removeView(viewHolder.container.findViewById(R.id.structure_header));
            }
            ViewGroup structure_container = (ViewGroup) viewHolder.container.findViewById(R.id.structure_container);
            if (item.type == TYPE_UNIT_STRUCTURE_COMMON) {
                boolean is_first_container = true;
                final String address = getString(item.data, "address");
                final String phone = getString(item.data, "phone");
                final String email = getString(item.data, "email");
                final String site = getString(item.data, "site");
                final String working_hours = getString(item.data, "working_hours");
                if (address != null) {
                    structure_container.addView(getConnectContainer(R.drawable.ic_location, address.trim(), is_first_container, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            try {
                                context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + address)));
                            } catch (ActivityNotFoundException e) {
                                Static.toast(context, context.getString(R.string.failed_to_start_geo_activity));
                            }
                        }
                    }));
                    is_first_container = false;
                }
                if (phone != null) {
                    structure_container.addView(getConnectContainer(R.drawable.ic_phone, phone.trim(), is_first_container, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone.trim()));
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(intent);
                        }
                    }));
                    is_first_container = false;
                }
                if (email != null) {
                    structure_container.addView(getConnectContainer(R.drawable.ic_email, email.trim(), is_first_container, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent emailIntent = new Intent(Intent.ACTION_SEND);
                            emailIntent.setType("message/rfc822");
                            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{email.trim()});
                            context.startActivity(Intent.createChooser(emailIntent, context.getString(R.string.send_mail) + "..."));
                        }
                    }));
                    is_first_container = false;
                }
                if (site != null) {
                    structure_container.addView(getConnectContainer(R.drawable.ic_web, site.trim(), is_first_container, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(site.trim())));
                        }
                    }));
                    is_first_container = false;
                }
                if (working_hours != null) {
                    String[] days = working_hours.trim().split("\\|");
                    ArrayList<String> days_new = new ArrayList<>();
                    for (String day : days) {
                        String[] day_split = day.trim().split("/");
                        String time = "";
                        for (int i = 1; i < day_split.length; i++) {
                            time += day_split[i] + "/";
                        }
                        time = time.trim();
                        if (time.endsWith("/")) {
                            time = time.trim().substring(0, time.length() - 1);
                        }
                        time = time.replace("/", ", ");
                        if (!time.isEmpty()) {
                            time = (day_split[0] + " (" + time + ")").trim();
                        }
                        days_new.add(time);
                    }
                    structure_container.addView(getConnectContainer(R.drawable.ic_access_time, TextUtils.join("\n", days_new).trim(), is_first_container, null));
                    is_first_container = true;
                }
            }
            if (item.type == TYPE_UNIT_STRUCTURE_DEANERY) {
                boolean is_first_container = true;
                final String deanery_address = getString(item.data, "deanery_address");
                final String deanery_phone = getString(item.data, "deanery_phone");
                final String deanery_email = getString(item.data, "deanery_email");
                if (deanery_address != null && !deanery_address.trim().isEmpty()) {
                    structure_container.addView(getConnectContainer(R.drawable.ic_location, deanery_address.trim(), is_first_container, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            try {
                                context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + deanery_address)));
                            } catch (ActivityNotFoundException e) {
                                Static.toast(context, context.getString(R.string.failed_to_start_geo_activity));
                            }
                        }
                    }));
                    is_first_container = false;
                }
                if (deanery_phone != null && !deanery_phone.trim().isEmpty()) {
                    structure_container.addView(getConnectContainer(R.drawable.ic_phone, deanery_phone.trim(), is_first_container, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + deanery_phone.trim()));
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(intent);
                        }
                    }));
                    is_first_container = false;
                }
                if (deanery_email != null && !deanery_email.trim().isEmpty()) {
                    structure_container.addView(getConnectContainer(R.drawable.ic_email, deanery_email.trim(), is_first_container, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent emailIntent = new Intent(Intent.ACTION_SEND);
                            emailIntent.setType("message/rfc822");
                            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{deanery_email.trim()});
                            context.startActivity(Intent.createChooser(emailIntent, context.getString(R.string.send_mail) + "..."));
                        }
                    }));
                    is_first_container = false;
                }
            }
            if (item.type == TYPE_UNIT_STRUCTURE_HEAD) {
                boolean is_first_container = true;
                final String head_lastname = getString(item.data, "head_lastname");
                final String head_firstname = getString(item.data, "head_firstname");
                final String head_middlename = getString(item.data, "head_middlename");
                final String head_avatar = getString(item.data, "head_avatar");
                final String head_degree = getString(item.data, "head_degree");
                final String head_email = getString(item.data, "head_email");
                final int head_pid = getInt(item.data, "head_pid");
                if (head_lastname != null && head_firstname != null) {
                    final View layout_university_persons_list_item = inflate(R.layout.layout_university_persons_list_item);
                    ((TextView) layout_university_persons_list_item.findViewById(R.id.name)).setText((head_lastname + " " + head_firstname + " " + (head_middlename == null ? "" : head_middlename)).trim());
                    if (head_degree != null && !head_degree.trim().isEmpty()) {
                        ((TextView) layout_university_persons_list_item.findViewById(R.id.post)).setText(head_degree);
                    } else {
                        Static.removeView(layout_university_persons_list_item.findViewById(R.id.post));
                    }
                    if (head_avatar != null && !head_avatar.trim().isEmpty()) {
                        Picasso.with(context)
                                .load(head_avatar)
                                .error(R.drawable.ic_sentiment_very_satisfied)
                                .transform(new CircularTransformation())
                                .into((ImageView) layout_university_persons_list_item.findViewById(R.id.avatar));
                    }
                    layout_university_persons_list_item.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(context, UniversityPersonCardActivity.class);
                            intent.putExtra("pid", head_pid);
                            context.startActivity(intent);
                        }
                    });
                    structure_container.addView(layout_university_persons_list_item);
                    is_first_container = false;
                }
                if (head_email != null && !head_email.trim().isEmpty()) {
                    structure_container.addView(getConnectContainer(R.drawable.ic_email, head_email.trim(), is_first_container, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent emailIntent = new Intent(Intent.ACTION_SEND);
                            emailIntent.setType("message/rfc822");
                            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{head_email.trim()});
                            context.startActivity(Intent.createChooser(emailIntent, context.getString(R.string.send_mail) + "..."));
                        }
                    }));
                    is_first_container = false;
                }
            }
        } catch (Exception e) {
            Static.error(e);
        }
    }
    private void bindDivisions(RecyclerView.ViewHolder holder, Item item) {
        try {
            ViewHolder viewHolder = (ViewHolder) holder;
            removeFirstSeparator(viewHolder.container);
            String header = getString(item.data, "header");
            if (header != null) {
                ((TextView) viewHolder.container.findViewById(R.id.structure_header)).setText(Static.capitalizeFirstLetter(header.trim()));
            } else {
                Static.removeView(viewHolder.container.findViewById(R.id.structure_header));
            }
            ViewGroup structure_container = (ViewGroup) viewHolder.container.findViewById(R.id.structure_container);
            JSONArray divisions = getJsonArray(item.data, "divisions");
            if (divisions != null) {
                for (int i = 0; i < divisions.length(); i++) {
                    final JSONObject division = divisions.getJSONObject(i);
                    final String title = getString(division, "title");
                    final int id = getInt(division, "id");
                    if (title != null && !title.trim().isEmpty()) {
                        View view = inflate(R.layout.layout_university_faculties_divisions_list_item);
                        ((TextView) view.findViewById(R.id.title)).setText(title);
                        view.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (onDivisionClickListener != null) {
                                    onDivisionClickListener.onClick(id);
                                }
                            }
                        });
                        structure_container.addView(view);
                    }
                }
            }
        } catch (Exception e) {
            Static.error(e);
        }
    }

    public interface OnDivisionClickListener {
        void onClick(int dep_id);
    }
    private OnDivisionClickListener onDivisionClickListener = null;
    public void setOnDivisionClickListener(OnDivisionClickListener onClickListener) {
        onDivisionClickListener = onClickListener;
    }

    private boolean first_block = true;
    private void removeFirstSeparator(ViewGroup structure_container) {
        if (first_block) {
            first_block = false;
            View structure_separator = structure_container.findViewById(R.id.structure_separator);
            if (structure_separator != null) {
                Static.removeView(structure_separator);
            }
        }
    }

    private View getConnectContainer(@DrawableRes int icon, String text, boolean is_first_container, View.OnClickListener listener) {
        View layout_university_connect = inflate(R.layout.layout_university_connect);
        ((ImageView) layout_university_connect.findViewById(R.id.connect_image)).setImageResource(icon);
        ((TextView) layout_university_connect.findViewById(R.id.connect_text)).setText(text.trim());
        if (listener != null) {
            layout_university_connect.setOnClickListener(listener);
        }
        if (is_first_container) {
            Static.removeView(layout_university_connect.findViewById(R.id.separator));
        }
        return layout_university_connect;
    }
    private View inflate(@LayoutRes int layoutId) throws InflateException {
        return ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layoutId, null);
    }
}
