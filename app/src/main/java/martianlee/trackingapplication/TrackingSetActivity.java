package martianlee.trackingapplication;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.SeekBar;
import android.widget.TextView;

public class TrackingSetActivity extends AppCompatActivity {
    ImageButton m_ReturnTrackingMethod;
    Spinner m_SelectMethod;
    SeekBar m_SeekbarParam1;
    SeekBar m_SeekbarParam2;
    SeekBar m_SeekbarParam3;
    TextView m_TextViewValue1;
    TextView m_TextViewValue2;
    TextView m_TextViewValue3;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracking_set);

        m_ReturnTrackingMethod = (ImageButton) findViewById(R.id.ReturnTrackingMethod);
        m_SelectMethod = (Spinner) findViewById(R.id.SelectMethod);

        m_SeekbarParam1 = (SeekBar) findViewById(R.id.SeekBarParam1);
        m_SeekbarParam2 = (SeekBar) findViewById(R.id.SeekBarParam2);
        m_SeekbarParam3 = (SeekBar) findViewById(R.id.SeekBarParam3);

        m_TextViewValue1 = (TextView) findViewById(R.id.TextviewValue1);
        m_TextViewValue2 = (TextView) findViewById(R.id.TextviewValue2);
        m_TextViewValue3 = (TextView) findViewById(R.id.TextviewValue3);
    }

    @Override
    protected void onStart() {
        super.onStart();

        final String[] Method = {"TemplateMatch", "MeanShift", "CamShift"};
        ArrayAdapter<String> MethodList = new ArrayAdapter<String>(TrackingSetActivity.this, R.layout.method_textview, Method);
        m_SelectMethod.setAdapter(MethodList);
    }

    @Override
    protected void onResume() {
        super.onResume();
        m_ReturnTrackingMethod.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        m_SeekbarParam1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                m_TextViewValue1.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        m_SeekbarParam2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                m_TextViewValue2.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        m_SeekbarParam3.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                m_TextViewValue3.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }
}




