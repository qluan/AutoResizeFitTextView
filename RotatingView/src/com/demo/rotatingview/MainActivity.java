package com.demo.rotatingview;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity
{
    private EditText mInputEditView;
    private TextView mShowTextView;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        init();
    }

    private void init(){
        mInputEditView = (EditText) findViewById(R.id.input);
        mShowTextView = (TextView) findViewById(R.id.show);

        mInputEditView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                //do nothing
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                //do nothing
                mShowTextView.setText(charSequence);
            }

            @Override
            public void afterTextChanged(Editable editable) {
                //do nothing
            }
        });
    }
}
