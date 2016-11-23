package io.sodaoud.eventtracker.sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;

import java.util.HashMap;
import java.util.Map;

import io.sodaoud.eventtracker.EventTracker;

public class MainActivity extends AppCompatActivity {

    private EditText editText;
    private EditText editText2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        EventTracker.init(this, "kilctuhEeONbf-V1JMH7");
        EventTracker.getInstance().setPostType(EventTracker.POST_TYPE_PERIODIC);
        EventTracker.getInstance().setPeriodicTime(5000);

        editText = (EditText) findViewById(R.id.editText);
        editText2 = (EditText) findViewById(R.id.editText2);
    }

    public void buttonClick(View v) {
        String name = editText.getText().toString();
        if (EventTracker.getInstance().validateName(name)) {
            EventTracker.getInstance().sendEvent(name);
        } else {
            editText.setError("The Name does not match the pattern");
        }
    }

    public void button2Click(View v) {
        String name = editText2.getText().toString();
        if (EventTracker.getInstance().validateName(name)) {
            Map<String, String> params = new HashMap<>();
            params.put("key", "value");
            EventTracker.getInstance().sendEvent(name, params);
        } else {
            editText2.setError("The Name does not match the pattern");
        }
    }
}
