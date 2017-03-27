package com.bukhmastov.cdoitmo.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.bukhmastov.cdoitmo.R;

public class PikaActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pika);
        overridePendingTransition(R.anim.zoom_bottom_in, R.anim.zoom_bottom_out);
        View pika_container = findViewById(R.id.pika_container);
        if (pika_container != null) {
            pika_container.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                    overridePendingTransition(R.anim.zoom_bottom_in, R.anim.zoom_bottom_out);
                }
            });
        }
    }

}
