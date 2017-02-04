package care.dovetail.blinker;

import android.app.Activity;
import android.content.Context;
import android.hardware.SensorManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.WindowManager;

import java.util.Timer;
import java.util.TimerTask;

import care.dovetail.blinker.bluetooth.ShimmerClient;
import care.dovetail.blinker.bluetooth.ShimmerClient.BluetoothDeviceListener;
import care.dovetail.blinker.processing.AccelerationProcessor;
import care.dovetail.blinker.processing.Feature;
import care.dovetail.blinker.processing.SignalProcessor;
import care.dovetail.blinker.ui.ChartFragment;
import care.dovetail.blinker.ui.GridView;

public class MainActivity extends Activity implements BluetoothDeviceListener,
        SignalProcessor.FeatureObserver, AccelerationProcessor.ShakingObserver {
    private static final String TAG = "MainActivity";

    private ShimmerClient patchClient;
    private SignalProcessor signals = new SignalProcessor(this);
    private AccelerationProcessor accelerometer;

    private FileDataWriter writer = null;

    private Timer chartUpdateTimer;
    private Timer sectorUpdateTimer;

    private Ringtone ringtone;

    private int bkgIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        findViewById(R.id.binocular).setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        findViewById(R.id.binocular).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        boolean show = findViewById(R.id.leftChart).getVisibility() == View.VISIBLE;
                        findViewById(R.id.leftChart).setVisibility(show ? View.INVISIBLE : View.VISIBLE);
                        findViewById(R.id.rightChart).setVisibility(show ? View.INVISIBLE : View.VISIBLE);
                    }
                });

        accelerometer = new AccelerationProcessor(
                (SensorManager) getSystemService(Context.SENSOR_SERVICE), this);

        findViewById(R.id.leftGrid).setVisibility(View.INVISIBLE);
        findViewById(R.id.rightGrid).setVisibility(View.INVISIBLE);
//        findViewById(R.id.leftChart).setVisibility(View.INVISIBLE);
//        findViewById(R.id.rightChart).setVisibility(View.INVISIBLE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        startBluetooth();
        findViewById(R.id.progress).setVisibility(View.VISIBLE);
        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        ringtone = RingtoneManager.getRingtone(getApplicationContext(), notification);
        accelerometer.start();
    }

    @Override
    protected void onStop() {
        stopBluetooth();
        ringtone.stop();
        accelerometer.stop();
        super.onStop();
    }

    @Override
    public void onConnect(String name) {
        Log.i(TAG, String.format("Connected to %s", name));
        writer = new FileDataWriter(this);
        showGrid(true);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.progress).setVisibility(View.INVISIBLE);
            }
        });
    }

    @Override
    public void onDisconnect(String name) {
        Log.i(TAG, String.format("Disconnected from %s", name));
        writer.close();
        writer = null;
        showGrid(false);
        chartUpdateTimer.cancel();
        sectorUpdateTimer.cancel();
        if (patchClient != null) {
            patchClient.connect();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    findViewById(R.id.progress).setVisibility(View.VISIBLE);
                }
            });
        }
    }

    private class ChartUpdater extends TimerTask {
        @Override
        public void run() {
            final ChartFragment leftChart =
                    (ChartFragment) getFragmentManager().findFragmentById(R.id.leftChart);
            final ChartFragment rightChart =
                    (ChartFragment) getFragmentManager().findFragmentById(R.id.rightChart);
            leftChart.clear();
            rightChart.clear();

            leftChart.updateChannel1(signals.channel1(), signals.range1());
            leftChart.updateChannel2(signals.channel2(), signals.range2());
            rightChart.updateChannel1(signals.channel1(), signals.range1());
            rightChart.updateChannel2(signals.channel2(), signals.range2());

            leftChart.updateFeature1(signals.feature1(), signals.range2());
            rightChart.updateFeature1(signals.feature1(), signals.range2());

            leftChart.updateFeature2(signals.feature2(), signals.range2());
            rightChart.updateFeature2(signals.feature2(), signals.range2());

            if (Config.SHOW_ACCEL) {
                leftChart.updateChannel3(accelerometer.getY(), Pair.create(-100, 100));
                rightChart.updateChannel3(accelerometer.getY(), Pair.create(-100, 100));
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (leftChart.isResumed() && rightChart.isResumed()) {
                        leftChart.updateUI();
                        rightChart.updateUI();
                    }
                }
            });
        }
    }

    private class SectorUpdater extends TimerTask {
        @Override
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Pair<Integer, Integer> sector = signals.getSector();
                    GridView leftGrid = (GridView) findViewById(R.id.leftGrid);
                    leftGrid.highlight(sector.first, sector.second);
                    GridView rightGrid = (GridView) findViewById(R.id.rightGrid);
                    rightGrid.highlight(sector.first, sector.second);
                }
            });
        }
    }

    @Override
    public void onFeature(Feature feature) {
        if (Feature.Type.BLINK == feature.type) {
            ringtone.play();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    GridView leftGrid = (GridView) findViewById(R.id.leftGrid);
                    leftGrid.background(bkgIndex);
                    GridView rightGrid = (GridView) findViewById(R.id.rightGrid);
                    rightGrid.background(bkgIndex);
                    bkgIndex = bkgIndex + 1 < GridView.BACKGROUNDS.length ? bkgIndex + 1 : 0;
                }
            });
        } else if (Feature.Type.BAD_SIGNAL == feature.type) {
            Log.e(TAG, "Bad Signal detected, reconnecting...");
            patchClient.connect();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    findViewById(R.id.progress).setVisibility(View.VISIBLE);
                }
            });
        }
    }

    @Override
    public void onNewValues(int channel1, int channel2) {
        if (!accelerometer.isShaking()) {
            signals.update(channel1, channel2);
        }
        if (writer != null) {
            writer.write(channel1, channel2);
        }
    }

    @Override
    public void onShakingChange(final boolean isShaking) {
        if (isShaking) {
            signals = new SignalProcessor(this);
            stopBluetooth();
            startBluetooth();
        }
        showGrid(!isShaking);
    }

    private void showGrid(final boolean show) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (MainActivity.this.isDestroyed() || patchClient == null
                        || !patchClient.isConnected()) {
                    return;
                }
                findViewById(R.id.leftGrid).setVisibility(show ? View.VISIBLE : View.INVISIBLE);
                findViewById(R.id.rightGrid).setVisibility(show ? View.VISIBLE : View.INVISIBLE);
            }
        });
    }

    private void startBluetooth() {
        // TODO(abhi): Create patchClient in onActivityResult if BT enable activity started.
        patchClient = new ShimmerClient(this, this);
        patchClient.connect();

        chartUpdateTimer = new Timer();
        chartUpdateTimer.schedule(new ChartUpdater(), 0, Config.GRAPH_UPDATE_MILLIS);

        sectorUpdateTimer = new Timer();
        sectorUpdateTimer.schedule(new SectorUpdater(), 0, Config.GAZE_UPDATE_MILLIS);
    }

    private void stopBluetooth() {
        if (patchClient != null) {
            patchClient.close();
            patchClient = null;
        }
        if (chartUpdateTimer != null) {
            chartUpdateTimer.cancel();
        }
        if (sectorUpdateTimer != null) {
            sectorUpdateTimer.cancel();
        }
    }
}
