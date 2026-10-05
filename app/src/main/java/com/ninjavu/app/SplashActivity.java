package com.ninjavu.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SplashActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(Color.parseColor("#F7F4EC"));

        TextView mark = new TextView(this);
        mark.setText("N");
        mark.setTextColor(Color.parseColor("#10221A"));
        mark.setTextSize(TypedValue.COMPLEX_UNIT_SP, 42);
        mark.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        mark.setGravity(Gravity.CENTER);
        GradientDrawable disc = new GradientDrawable();
        disc.setShape(GradientDrawable.OVAL);
        disc.setColor(Color.WHITE);
        disc.setStroke(dp(3), Color.parseColor("#0F9F6E"));
        mark.setBackground(disc);
        int box = dp(92);
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(box, box);
        mark.setLayoutParams(mp);

        TextView title = new TextView(this);
        title.setText("NinjaVu");
        title.setTextColor(Color.parseColor("#10221A"));
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        title.setPadding(0, dp(18), 0, 0);

        TextView sub = new TextView(this);
        sub.setText("Movies and series, one tap");
        sub.setTextColor(Color.parseColor("#3D5348"));
        sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        sub.setPadding(0, dp(6), 0, 0);

        root.addView(mark);
        root.addView(title);
        root.addView(sub);
        setContentView(root);

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
                finish();
            }
        }, 900);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
