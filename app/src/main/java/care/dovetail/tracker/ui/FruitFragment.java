package care.dovetail.tracker.ui;

import android.app.Activity;
import android.content.Context;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import care.dovetail.tracker.Config;
import care.dovetail.tracker.EyeEvent;
import care.dovetail.tracker.Gesture;
import care.dovetail.tracker.R;
import care.dovetail.tracker.Settings;

/**
 * Created by abhi on 4/24/17.
 */

public class FruitFragment extends Fragment implements Gesture.Observer {
    private final Set<Gesture> gestures = new HashSet<>();

    private final Map<String, MediaPlayer> players = new HashMap<>();
    private Settings settings;

    private boolean animationRunning = false;

    private ImageView leftFruit;
    private ImageView rightFruit;

    private Timer fixationResetTimer;

    public FruitFragment() {
        gestures.add(new Gesture("left")
                .add(new EyeEvent.Criterion(EyeEvent.Type.FIXATION, 1000L))
                .add(new EyeEvent.Criterion(EyeEvent.Type.SACCADE, EyeEvent.Direction.LEFT, 1500))
                .add(new EyeEvent.Criterion(EyeEvent.Type.SACCADE, EyeEvent.Direction.RIGHT, 1500))
                .addObserver(this));
        gestures.add(new Gesture("right")
                .add(new EyeEvent.Criterion(EyeEvent.Type.FIXATION, 1000L))
                .add(new EyeEvent.Criterion(EyeEvent.Type.SACCADE, EyeEvent.Direction.RIGHT, 1500))
                .add(new EyeEvent.Criterion(EyeEvent.Type.SACCADE, EyeEvent.Direction.LEFT, 1500))
                .addObserver(this));
        gestures.add(new Gesture("fixation")
                .add(new EyeEvent.Criterion(EyeEvent.Type.FIXATION, 1000L))
                .addObserver(this));
        gestures.add(new Gesture("blink")
                .add(new EyeEvent.Criterion(EyeEvent.Type.SACCADE, EyeEvent.Direction.UP, 2000))
                .add(new EyeEvent.Criterion(EyeEvent.Type.SACCADE, EyeEvent.Direction.DOWN, 4000))
                .add(new EyeEvent.Criterion(EyeEvent.Type.SACCADE, EyeEvent.Direction.UP, 2000))
                .addObserver(this));
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        settings = new Settings(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_fruit, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        leftFruit = (ImageView) view.findViewById(R.id.leftImage);
        rightFruit = (ImageView) view.findViewById(R.id.rightImage);

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
        resetFixation();
    }

    @Override
    public void onStart() {
        super.onStart();
        players.put("left", MediaPlayer.create(getContext(), R.raw.slice));
        players.put("right", MediaPlayer.create(getContext(), R.raw.slice));
        players.put("fixation", MediaPlayer.create(getContext(), R.raw.beep));
        players.put("blink", MediaPlayer.create(getContext(), R.raw.beep));
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
    public Set<Gesture> getGestures() {
        return gestures;
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
                if (animationRunning) {
                    return;
                }
                switch (gestureName) {
                    case "left":
                        leftFruit.setImageResource(R.drawable.apple_left);
                        rightFruit.setImageResource(R.drawable.apple_left);
                        resetImage(Config.GESTURE_VISIBILITY_MILLIS);
                        resetFixation();
                        animationRunning = true;
                        break;
                    case "right":
                        leftFruit.setImageResource(R.drawable.apple_right);
                        rightFruit.setImageResource(R.drawable.apple_right);
                        resetImage(Config.GESTURE_VISIBILITY_MILLIS);
                        resetFixation();
                        animationRunning = true;
                        break;
                    case "blink":
                        leftFruit.setImageResource(R.drawable.apple_hole);
                        rightFruit.setImageResource(R.drawable.apple_hole);
                        resetImage(Config.FIXATION_VISIBILITY_MILLIS);
                        resetFixation();
                        animationRunning = true;
                        break;
                    case "fixation":
                        resetFixation();
                        setFixation(new int[] {R.id.leftLeftKnife, R.id.leftRightKnife});
                        setFixation(new int[] {R.id.rightLeftKnife, R.id.rightRightKnife});
                        resetFixation(Config.FIXATION_VISIBILITY_MILLIS);
                        break;
                }
            }
        });
    }

    private void resetImage(int delay) {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        leftFruit.setImageResource(R.drawable.apple);
                        rightFruit.setImageResource(R.drawable.apple);
                        animationRunning = false;
                    }
                });
            }
        }, delay);
    }

    private void setFixation(int knives[]) {
        for (int id : knives) {
            getView().findViewById(id).setVisibility(View.VISIBLE);
        }
    }

    private void resetFixation() {
        int knives[] = new int[] {R.id.leftLeftKnife, R.id.leftRightKnife, R.id.rightLeftKnife,
                R.id.rightRightKnife};
        for (int id : knives) {
            getView().findViewById(id).setVisibility(View.INVISIBLE);
        }
    }

    private void resetFixation(int delay) {
        if (fixationResetTimer != null) {
            fixationResetTimer.cancel();
        }
        fixationResetTimer = new Timer();
        fixationResetTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        resetFixation();
                    }
                });
            }
        }, delay);
    }
}
