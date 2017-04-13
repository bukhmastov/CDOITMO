package com.bukhmastov.cdoitmo.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;

import com.bukhmastov.cdoitmo.R;

import java.util.Random;

public class PikaActivity extends AppCompatActivity {

    private final Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pika);
        overridePendingTransition(R.anim.zoom_bottom_in, R.anim.zoom_bottom_out);
        if (random.nextInt(5) % 5 == 0) {
            ((ImageView) findViewById(R.id.image)).setImageDrawable(getDrawable(R.drawable.wuwari));
        }
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
