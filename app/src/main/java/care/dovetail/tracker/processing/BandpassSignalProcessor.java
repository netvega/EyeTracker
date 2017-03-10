package care.dovetail.tracker.processing;

import android.util.Pair;

import biz.source_code.dsp.filter.FilterCharacteristicsType;
import biz.source_code.dsp.filter.FilterPassType;
import biz.source_code.dsp.filter.IirFilter;
import biz.source_code.dsp.filter.IirFilterDesignFisher;
import care.dovetail.tracker.Config;

public class BandpassSignalProcessor extends SignalProcessor {
    private static final String TAG = "BandpassSignalProcessor";

    private static final Pair<Integer, Integer> HALF_GRAPH_HEIGHT = new Pair<>(2000, 6000);
    private static final Pair<Integer, Integer> INITIAL_HALF_GRAPH_HEIGHT = new Pair<>(3000, 8000);

    private static final int WAIT_TIME_FOR_STABILITY_MILLIS = 10000;

    private final IirFilter hFilter = new IirFilter(IirFilterDesignFisher.design(
            FilterPassType.bandpass, FilterCharacteristicsType.butterworth, 2 /* order */, 0,
            0.25 / Config.SAMPLING_FREQ, 4.0 / Config.SAMPLING_FREQ));

    private final IirFilter vFilter = new IirFilter(IirFilterDesignFisher.design(
            FilterPassType.bandpass, FilterCharacteristicsType.butterworth, 2 /* order */, 0,
            0.25 / Config.SAMPLING_FREQ, 4.0 / Config.SAMPLING_FREQ));

    private final IirFilter hInitialFilter = new IirFilter(IirFilterDesignFisher.design(
            FilterPassType.bandpass, FilterCharacteristicsType.butterworth, 2 /* order */, 0,
            0.75 / Config.SAMPLING_FREQ, 4.0 / Config.SAMPLING_FREQ));

    private final IirFilter vInitialFilter = new IirFilter(IirFilterDesignFisher.design(
            FilterPassType.bandpass, FilterCharacteristicsType.butterworth, 2 /* order */, 0,
            0.75 / Config.SAMPLING_FREQ, 4.0 / Config.SAMPLING_FREQ));

    public BandpassSignalProcessor(FeatureObserver observer, int numSteps) {
        super(observer, numSteps);
    }

    @Override
    public String getDebugNumbers() {
        return String.format("%d\n%d", hHalfGraphHeight, vHalfGraphHeight);
    }

    @Override
    protected int processHorizontal(int value) {
        int filtered = (int) hFilter.step(value);
        if (goodSignalMillis < WAIT_TIME_FOR_STABILITY_MILLIS) {
            return (int) hInitialFilter.step(value);
        }
        return filtered;
    }

    @Override
    protected int processVertical(int value) {
        int filtered = (int) vFilter.step(value);
        if (goodSignalMillis < WAIT_TIME_FOR_STABILITY_MILLIS) {
            return (int) vInitialFilter.step(value);
        }
        return filtered;
    }

    @Override
    protected int horizontalBase() {
        return goodSignalMillis < WAIT_TIME_FOR_STABILITY_MILLIS ? hStats.median : 0;
    }

    @Override
    protected int verticalBase() {
        return goodSignalMillis < WAIT_TIME_FOR_STABILITY_MILLIS ? vStats.median : 0;
    }

    @Override
    protected int minGraphHeight() {
        return goodSignalMillis < WAIT_TIME_FOR_STABILITY_MILLIS
                ? INITIAL_HALF_GRAPH_HEIGHT.first : HALF_GRAPH_HEIGHT.first;
    }

    @Override
    protected int maxGraphHeight() {
        return goodSignalMillis < WAIT_TIME_FOR_STABILITY_MILLIS
                ? INITIAL_HALF_GRAPH_HEIGHT.second : HALF_GRAPH_HEIGHT.second;
    }

    @Override
    protected int waitMillisForStability() {
        return WAIT_TIME_FOR_STABILITY_MILLIS;
    }
}