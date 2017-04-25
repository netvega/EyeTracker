package care.dovetail.tracker.eog;

import care.dovetail.tracker.EyeEvent;

/**
 * Created by abhi on 4/25/17.
 */

public interface GestureRecognizer {
    void update(int horizontal, int vertical);
    boolean hasEyeEvent();
    EyeEvent getEyeEvent();
}
