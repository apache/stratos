package org.apache.stratos.messaging.message.receiver.health.stat;


import javax.jms.TextMessage;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Implements a blocking queue for managing instance notifier event messages.
 */
public class HealthStatEventMessageQueue extends LinkedBlockingQueue<TextMessage> {
    private static volatile HealthStatEventMessageQueue instance;

    private HealthStatEventMessageQueue(){
    }

    public static synchronized HealthStatEventMessageQueue getInstance() {
        if (instance == null) {
            synchronized (HealthStatEventMessageQueue.class){
                if (instance == null) {
                    instance = new HealthStatEventMessageQueue();
                }
            }
        }
        return instance;
    }
}