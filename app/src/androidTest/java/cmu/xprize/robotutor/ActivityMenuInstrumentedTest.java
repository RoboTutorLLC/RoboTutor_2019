package cmu.xprize.robotutor;

import android.content.Intent;
import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;

import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.isRoot;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

/**
 * RoboTutor
 * <p>
 * Created by kevindeland on 8/29/18.
 */

@RunWith(AndroidJUnit4.class)
public class ActivityMenuInstrumentedTest {

    @Rule
    public ActivityTestRule<RoboTutor> mRoboTutorRule = new ActivityTestRule<RoboTutor>(RoboTutor.class);

    @Test
    public void testSelectButton() {

        mRoboTutorRule.launchActivity(new Intent());

        onView(isRoot()).perform(waitFor(10 * 1000));

        onView(withId(R.id.Sbutton1)).perform(click());

        onView(isRoot()).perform(waitFor(10 * 1000));
    }


    /**
     * Perform action of waiting for a specific time.
     */
    private static ViewAction waitFor(final long millis) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isRoot();
            }

            @Override
            public String getDescription() {
                return "Wait for " + millis + " milliseconds.";
            }

            @Override
            public void perform(UiController uiController, final View view) {
                uiController.loopMainThreadForAtLeast(millis);
            }
        };
    }

}
