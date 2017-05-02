package care.dovetail.tracker.ui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import care.dovetail.tracker.Config;
import care.dovetail.tracker.EyeEvent;
import care.dovetail.tracker.R;
import care.dovetail.tracker.Settings;

/**
 * Created by abhi on 4/24/17.
 */

public class PositionFragment extends Fragment implements EyeEvent.Observer {
    private Settings settings;

    private GridView leftCursor;
    private GridView rightCursor;

    private GridView leftMole;
    private GridView rightMole;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        settings = new Settings(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_position, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        leftCursor = (GridView) view.findViewById(R.id.leftGrid);
        rightCursor = (GridView) view.findViewById(R.id.rightGrid);

        leftMole = (GridView) view.findViewById(R.id.leftMoleGrid);
        rightMole = (GridView) view.findViewById(R.id.rightMoleGrid);

        int numSteps = settings.getNumSteps();
        leftCursor.setNumSteps(numSteps);
        rightCursor.setNumSteps(numSteps);

        if (settings.shouldWhackAMole()) {
            leftCursor.setCursorStyle(GridView.CursorStyle.values()[settings.getCursorStyle()]);
            rightCursor.setCursorStyle(GridView.CursorStyle.values()[settings.getCursorStyle()]);
            leftMole.setCursorStyle(GridView.CursorStyle.RECTANGLE);
            rightMole.setCursorStyle(GridView.CursorStyle.RECTANGLE);

            leftMole.setNumSteps(Config.MOLE_NUM_STEPS);
            rightMole.setNumSteps(Config.MOLE_NUM_STEPS);
        } else {
            leftCursor.setCursorStyle(GridView.CursorStyle.values()[settings.getCursorStyle()]);
            rightCursor.setCursorStyle(GridView.CursorStyle.values()[settings.getCursorStyle()]);
        }

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

    public void onEyeEvent(final EyeEvent event) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (event.type) {
                    case POSITION:
                        leftCursor.highlight(event.column, event.row);
                        rightCursor.highlight(event.column, event.row);
                        break;
                    case WHACKAMOLE_POSITION:
                        leftMole.highlight(event.column, event.row);
                        rightMole.highlight(event.column, event.row);
                        break;
//            case LARGE_BLINK:
//                if (settings.shouldShowBlinkmarks()) {
//                    getActivity().runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            leftCursor.mark(event.column, event.row);
//                            rightCursor.mark(event.column, event.row);
//                        }
//                    });
//                }
//                break;
                }
            }
        });
    }
}