package com.courtstory.app;
import android.app.Activity;
import android.os.Bundle;
import android.widget.*;
import android.graphics.Color;
/** Full public policy, also readable offline from Health Connect's privacy route. */
public final class HealthPrivacyActivity extends Activity {
    @Override public void onCreate(Bundle state){super.onCreate(state);LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);int pad=Math.round(20*getResources().getDisplayMetrics().density);body.setPadding(pad,pad,pad,pad);body.setBackgroundColor(Color.rgb(221,255,40));
        addText(body,"Court Story · Privacy",22,true);
        for(String[] section:PrivacyPolicy.SECTIONS){addText(body,section[0],18,true);addText(body,section[1],16,false);}
        Button policy=new Button(this);policy.setText("Open public privacy policy");policy.setAllCaps(false);policy.setMinHeight(Math.round(48*getResources().getDisplayMetrics().density));policy.setOnClickListener(v->PrivacyPolicy.open(this));body.addView(policy);
        Button close=new Button(this);close.setText("Done");close.setAllCaps(false);close.setMinHeight(Math.round(48*getResources().getDisplayMetrics().density));close.setOnClickListener(v->finish());body.addView(close);
        ScrollView scroll=new ScrollView(this);scroll.setFitsSystemWindows(true);scroll.setAccessibilityPaneTitle("Court Story privacy policy");scroll.addView(body);setContentView(scroll);
    }
    private void addText(LinearLayout body,String value,int size,boolean heading){TextView text=new TextView(this);text.setText(value);text.setTextSize(size);text.setTextColor(Color.rgb(20,49,39));text.setPadding(0,Math.round(8*getResources().getDisplayMetrics().density),0,Math.round(8*getResources().getDisplayMetrics().density));text.setAccessibilityHeading(heading);if(heading)text.setTypeface(null,android.graphics.Typeface.BOLD);body.addView(text);}
}
