package com.indooratlas.android.sdk.examples;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;


/**
 *
 */
public class LocationEditorActivity extends AppCompatActivity {

    private EditText mCompleteValue;

    private EditText mMapText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_uri_editor);
        mCompleteValue = (EditText) findViewById(R.id.uri);
        mMapText = (EditText) findViewById(R.id.map);

        TextWatcher textChangeWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                updateUri();
            }
        };

        mMapText.addTextChangedListener(textChangeWatcher);
        mMapText.setText(getString(R.string.indooratlas_floor_plan_id));

    }

    public void update(View view) {
        setResult(RESULT_OK, new Intent()
            .putExtra(Intent.EXTRA_TEXT, mMapText.getText().toString()));
        finish();
    }


    private void updateUri() {
        mCompleteValue.setText(mMapText.getText());
    }
}
