package io.sodaoud.eventtracker;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.robolectric.annotation.Config;

/**
 * Created by sofiane on 11/23/16.
 */
@RunWith(MockitoJUnitRunner.class)
@Config(manifest = Config.DEFAULT_MANIFEST)
public class EventTrackerTest {

    @Mock
    Context context;

    @Before
    public void setup() {
//        context = ShadowApplication.getInstance().getApplicationContext();
    }

    @Test
    public void test() {

        EventTracker.init(context, "API_KEY");
        EventTracker.getInstance().sendEvent("sdsd");

    }
}
