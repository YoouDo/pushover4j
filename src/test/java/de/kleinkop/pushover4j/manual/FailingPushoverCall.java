package de.kleinkop.pushover4j.manual;

import de.kleinkop.pushover4j.Message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FailingPushoverCall extends ManualPushoverCall {

    private static final Logger log = LoggerFactory.getLogger(FailingPushoverCall.class);

    void failingTest() {
        try {
            var response = failingPushover().sendMessage(
                Message.of("Bla").build()
            );
            log.info(response.toString());
        } catch (Exception e) {
            log.error("Error", e);
        }
        log.info("Done");
    }

    public static void main(String[] args) {
        new FailingPushoverCall().failingTest();
    }
}
