package care.dovetail.tracker.ui;

import android.app.Activity;
import android.content.Context;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import care.dovetail.ojo.EyeEvent;
import care.dovetail.ojo.Gesture;
import care.dovetail.tracker.Config;
import care.dovetail.tracker.R;
import care.dovetail.tracker.Settings;

/**
 * Created by abhi on 4/24/17.
 */

public class GestureFragment extends Fragment implements Gesture.Observer {

    private EyeEvent.Source eyeEventSource;

    private final Map<String, MediaPlayer> players = new HashMap<>();
    private Settings settings;

    private final Set<Gesture> directions = new HashSet<>();
    private int blinkCount = 0;
    private final Set<Integer> blinkAmplitudes = new HashSet<>();

    private GestureView leftContent;
    private GestureView rightContent;

    private TextView leftText;
    private TextView rightText;

    private Timer resetTimer;

    @Override
    public void setEyeEventSource(EyeEvent.Source eyeEventSource) {
        this.eyeEventSource = eyeEventSource;
        eyeEventSource.add(new Gesture("blink")
                .add(EyeEvent.Criterion.saccade(EyeEvent.Direction.UP, 2000))
                .add(EyeEvent.Criterion.saccade(EyeEvent.Direction.DOWN, 4000))
                .add(EyeEvent.Criterion.saccade(EyeEvent.Direction.UP, 2000))
                .addObserver(this));
        eyeEventSource.add(new Gesture("fixation")
                .add(EyeEvent.Criterion.fixation(1000))
                .addObserver(this));
        eyeEventSource.add(new Gesture("explode")
                .add(EyeEvent.Criterion.fixation(4000, 4500))
                .addObserver(this));
        replaceDirections(1500);
    }

    private void replaceDirections(int amplitude) {
        amplitude = Math.min(Math.max(amplitude, 800), 2000);
        for (Gesture direction : directions) {
            eyeEventSource.remove(direction);
        }
        directions.add(new Gesture("left")
                .add(EyeEvent.Criterion.fixation(1000, 4000))
                .add(EyeEvent.Criterion.saccade(EyeEvent.Direction.LEFT, amplitude))
                .add(EyeEvent.Criterion.saccade(EyeEvent.Direction.RIGHT, amplitude))
                .addObserver(this));
        directions.add(new Gesture("right")
                .add(EyeEvent.Criterion.fixation(1000, 4000))
                .add(EyeEvent.Criterion.saccade(EyeEvent.Direction.RIGHT, amplitude))
                .add(EyeEvent.Criterion.saccade(EyeEvent.Direction.LEFT, amplitude))
                .addObserver(this));
        directions.add(new Gesture("up")
                .add(EyeEvent.Criterion.fixation(1000, 4000))
                .add(EyeEvent.Criterion.saccade(EyeEvent.Direction.UP, amplitude))
                .add(EyeEvent.Criterion.saccade(EyeEvent.Direction.DOWN, amplitude))
                .addObserver(this));
        directions.add(new Gesture("down")
                .add(EyeEvent.Criterion.fixation(1000, 4000))
                .add(EyeEvent.Criterion.saccade(EyeEvent.Direction.DOWN, amplitude))
                .add(EyeEvent.Criterion.saccade(EyeEvent.Direction.UP, amplitude))
                .addObserver(this));
        for (Gesture direction : directions) {
            eyeEventSource.add(direction);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        settings = new Settings(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_gesture, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        leftContent = (GestureView) view.findViewById(R.id.leftContent);
        rightContent = (GestureView) view.findViewById(R.id.rightContent);
        leftText = (TextView) view.findViewById(R.id.leftText);
        rightText = (TextView) view.findViewById(R.id.rightText);

        if (settings.isDayDream()) {
            view.findViewById(R.id.left).setPadding(
                    getResources().getDimensionPixelOffset(R.dimen.daydream_padding_left),
                    getResources().getDimensionPixelOffset(R.dimen.daydream_padding_top),
                    getResources().getDimensionPixelOffset(R.dimen.daydream_padding_middle),
                    getResources().getDimensionPixelOffset(R.dimen.daydream_padding_bottom));
            view.findViewById(R.id.right).setPadding(
                    getResources().getDimensionPixelOffset(R.dimen.daydream_padding_middle),
                    getResources().getDimensionPixelOffset(R.dimen.daydream_padding_top),
                    getResources().getDimensionPixelOffset(R.dimen.daydream_padding_right),
                    getResources().getDimensionPixelOffset(R.dimen.daydream_padding_bottom));
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        players.put("left", MediaPlayer.create(getContext(), R.raw.slice));
        players.put("right", MediaPlayer.create(getContext(), R.raw.slice));
        players.put("fixation", MediaPlayer.create(getContext(), R.raw.beep));
        players.put("blink", MediaPlayer.create(getContext(), R.raw.beep));
        players.put("explode", MediaPlayer.create(getContext(), R.raw.explode));
    }

    @Override
    public void onStop() {
        for (MediaPlayer player : players.values()) {
            player.release();
        }
        players.clear();
        super.onStop();
    }

    @Override
    public void onGesture(final String gestureName, final List<EyeEvent> events) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        MediaPlayer player = players.get(gestureName);
        if (player != null) {
            if (player.isPlaying()) {
                player.stop();
            }
            player.start();
        }
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String debug = "";
                switch (gestureName) {
                    case "blink":
                        leftContent.showSquare(true);
                        rightContent.showSquare(true);
                        debug = String.format("%d\n%d\n%d", events.get(0).amplitude,
                                events.get(1).amplitude, events.get(2).amplitude);
                        reset(2000);
                        maybeUpdateDirections(events);
                        break;
                    case "explode":
                        debug = String.format("%d", events.get(0).durationMillis);
                        break;
                    case "fixation":
                        debug = String.format("%d", events.get(0).durationMillis);
                        leftContent.showCircle(true);
                        rightContent.showCircle(true);
                        reset(Config.FIXATION_VISIBILITY_MILLIS);
                        break;
                    case "left":
                        debug = String.format("%d\n%d", events.get(0).durationMillis,
                                events.get(1).amplitude);
                        leftContent.showArrow(EyeEvent.Direction.LEFT, false);
                        rightContent.showArrow(EyeEvent.Direction.LEFT, false);
                        reset(Config.GESTURE_VISIBILITY_MILLIS);
                        break;
                    case "right":
                        debug = String.format("%d\n%d", events.get(0).durationMillis,
                                events.get(1).amplitude);
                        leftContent.showArrow(EyeEvent.Direction.RIGHT, false);
                        rightContent.showArrow(EyeEvent.Direction.RIGHT, false);
                        reset(Config.GESTURE_VISIBILITY_MILLIS);
                        break;
                    case "up":
                        debug = String.format("%d\n%d", events.get(0).durationMillis,
                                events.get(1).amplitude);
                        leftContent.showArrow(EyeEvent.Direction.UP, false);
                        rightContent.showArrow(EyeEvent.Direction.UP, false);
                        reset(Config.GESTURE_VISIBILITY_MILLIS);
                        break;
                    case "down":
                        debug = String.format("%d\n%d", events.get(0).durationMillis,
                                events.get(1).amplitude);
                        leftContent.showArrow(EyeEvent.Direction.DOWN, false);
                        rightContent.showArrow(EyeEvent.Direction.DOWN, false);
                        reset(Config.GESTURE_VISIBILITY_MILLIS);
                        break;
                }
                leftText.setText(debug);
                rightText.setText(debug);
            }
        });
    }

    private void maybeUpdateDirections(List<EyeEvent> events) {
        blinkCount++;
        if (blinkCount % 10 == 0 || blinkAmplitudes.size() > 0) {
            if (blinkAmplitudes.size() < 9) {
                blinkAmplitudes.add(events.get(0).amplitude);
                blinkAmplitudes.add(events.get(1).amplitude / 2);
                blinkAmplitudes.add(events.get(2).amplitude);
            } else {
                int sum = 0;
                for (Integer amplitude : blinkAmplitudes) {
                    sum += amplitude;
                }
                replaceDirections((sum / blinkAmplitudes.size()) / 3);
                blinkAmplitudes.clear();
            }
        }
    }

    private void reset(int delay) {
        if (resetTimer != null) {
            resetTimer.cancel();
        }
        resetTimer = new Timer();
        resetTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Activity activity = getActivity();
                if (activity == null) {
                    return;
                }
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        leftContent.clear();
                        rightContent.clear();
                        leftText.setText(null);
                        rightText.setText(null);
                    }
                });
            }
        }, delay);
    }
}
