package de.kleinkop.pushover4j.manual;

import de.kleinkop.pushover4j.PushoverClient;
import de.kleinkop.pushover4j.TestUtils;
import de.kleinkop.pushover4j.http.PushoverHttpClient;

import java.io.File;

abstract public class ManualPushoverTest {
    final private PushoverClient pushoverClient;

    final private PushoverClient failingPushoverClient;

    protected String device = System.getenv("PUSHOVER_DEVICE");
    protected String sound = System.getenv("PUSHOVER_SOUND");

    public ManualPushoverTest() {
        pushoverClient = new PushoverHttpClient(
            System.getenv("PUSHOVER_TOKEN"),
            System.getenv("PUSHOVER_USER")
        );
        failingPushoverClient = new PushoverHttpClient(
            "wrongtoken",
            "wronguser"
        );
    }

    protected PushoverClient pushover() {
        return pushoverClient;
    }

    protected PushoverClient failingPushover() {
        return failingPushoverClient;
    }

    protected File file(String filename) {
        return new File(TestUtils.getResource(filename).getFile());
    }

    protected void waiting(Long timeInSeconds) {
        try {
            Thread.sleep(timeInSeconds * 1000L);
        } catch (InterruptedException e) {
            // ignore
        }
    }
}
