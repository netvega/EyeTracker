package care.dovetail.tracker.ui;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import care.dovetail.tracker.R;
import care.dovetail.tracker.Settings;

/**
 * Created by abhi on 2/6/17.
 */

public class SettingsActivity extends Activity {
    private final static String TAG = "SettingsActivity";

    private final Settings settings = new Settings(this);

    private ToggleButton dayDream;
    private ToggleButton showChart;
    private ToggleButton showBlinks;
    private ToggleButton whackAMole;
    private ToggleButton showAccel;
    private SeekBar numSteps;
    private TextView numStepsValue;
    private SeekBar blinkToGaze;
    private TextView blinkToGazeValue;
    private SeekBar verticalToHorizontal;
    private TextView vToHValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        SharedPreferences prefs = getSharedPreferences(getPackageName(), 0);

        dayDream = (ToggleButton) findViewById(R.id.dayDream);
        showChart = (ToggleButton) findViewById(R.id.showChart);
        showBlinks = (ToggleButton) findViewById(R.id.showBlinks);
        whackAMole = (ToggleButton) findViewById(R.id.whackAMole);
        showAccel = (ToggleButton) findViewById(R.id.showAccel);
        numSteps = (SeekBar) findViewById(R.id.num_steps);
        numStepsValue = (TextView)  findViewById(R.id.num_steps_value);
        blinkToGaze = (SeekBar) findViewById(R.id.blink_to_gaze);
        blinkToGazeValue = (TextView)  findViewById(R.id.blink_to_gaze_value);
        verticalToHorizontal = (SeekBar) findViewById(R.id.v_to_h);
        vToHValue = (TextView)  findViewById(R.id.v_to_h_value);

        dayDream.setChecked(settings.isDayDream());
        showChart.setChecked(settings.shouldShowChart());
        showBlinks.setChecked(settings.shouldShowBlinks());
        whackAMole.setChecked(settings.shouldWhackAMole());
        showAccel.setChecked(settings.shouldShowAccel());
        numSteps.setProgress(settings.getNumSteps());
        numStepsValue.setText(Integer.toString(settings.getNumSteps()));
        blinkToGaze.setProgress((int) (settings.getBlinkToGaze() * 10));
        blinkToGazeValue.setText(Float.toString(settings.getBlinkToGaze()));
        verticalToHorizontal.setProgress((int) (settings.getVtoH() * 10));
        vToHValue.setText(Float.toString(settings.getVtoH()));

        dayDream.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                settings.setDayDream(isChecked);
            }
        });

        showChart.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                settings.setShowChart(isChecked);
            }
        });

        showBlinks.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                settings.setShowBlinks(isChecked);
            }
        });

        whackAMole.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                settings.setWhackAMole(isChecked);
            }
        });

        showAccel.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                settings.setShowAccel(isChecked);
            }
        });

        numSteps.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                numStepsValue.setText(Integer.toString(progress));
                settings.setNumSteps(progress);
            }

            @Override  public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override  public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        blinkToGaze.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                blinkToGazeValue.setText(Float.toString((float) progress / 10));
                settings.setBlinkToGaze((float) progress / 10);
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        verticalToHorizontal.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                vToHValue.setText(Float.toString((float) progress / 10));
                settings.setvToH((float) progress / 10);
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }
}