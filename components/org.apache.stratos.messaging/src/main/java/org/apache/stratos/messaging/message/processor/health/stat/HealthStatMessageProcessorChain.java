package org.apache.stratos.messaging.message.processor.health.stat;

import org.apache.stratos.messaging.listener.EventListener;
import org.apache.stratos.messaging.listener.health.stat.*;
import org.apache.stratos.messaging.message.processor.MessageProcessorChain;

/**
 * Defines default health stat message processor chain.
 */
public class HealthStatMessageProcessorChain extends MessageProcessorChain {

    private AverageLoadAverageMessageProcessor averageLoadAverageMessageProcessor;
    private AverageMemoryConsumptionMessageProcessor averageMemoryConsumptionMessageProcessor;
    private AverageRequestsInFlightMessageProcessor averageRequestsInFlightMessageProcessor;
    private GradientOfLoadAverageMessageProcessor gradientOfLoadAverageMessageProcessor;
    private GradientOfMemoryConsumptionMessageProcessor gradientOfMemoryConsumptionMessageProcessor;
    private GradientOfRequestsInFlightMessageProcessor gradientOfRequestsInFlightMessageProcessor;
    private SecondDerivativeOfLoadAverageMessageProcessor secondDerivativeOfLoadAverageMessageProcessor;
    private SecondDerivativeOfMemoryConsumptionMessageProcessor secondDerivativeOfMemoryConsumptionMessageProcessor;
    private SecondDerivativeOfRequestsInFlightMessageProcessor secondDerivativeOfRequestsInFlightMessageProcessor;
    private MemberAverageLoadAverageMessageProcessor memberAverageLoadAverageMessageProcessor;
    private MemberAverageMemoryConsumptionMessageProcessor memberAverageMemoryConsumptionMessageProcessor;
    private MemberGradientOfLoadAverageMessageProcessor memberGradientOfLoadAverageMessageProcessor;
    private MemberGradientOfMemoryConsumptionMessageProcessor memberGradientOfMemoryConsumptionMessageProcessor;
    private MemberSecondDerivativeOfLoadAverageMessageProcessor memberSecondDerivativeOfLoadAverageMessageProcessor;
    private MemberSecondDerivativeOfMemoryConsumptionMessageProcessor memberSecondDerivativeOfMemoryConsumptionMessageProcessor;

    private MemberFaultMessageProcessor memberFaultMessageProcessor;

    protected void initialize() {

        //Most frequent first order is defined in default
        memberAverageLoadAverageMessageProcessor = new MemberAverageLoadAverageMessageProcessor();
        add(memberAverageLoadAverageMessageProcessor);
        memberGradientOfLoadAverageMessageProcessor = new MemberGradientOfLoadAverageMessageProcessor();
        add(memberGradientOfLoadAverageMessageProcessor);
        memberSecondDerivativeOfLoadAverageMessageProcessor = new MemberSecondDerivativeOfLoadAverageMessageProcessor();
        add(memberSecondDerivativeOfLoadAverageMessageProcessor);

        memberAverageMemoryConsumptionMessageProcessor = new MemberAverageMemoryConsumptionMessageProcessor();
        add(memberAverageMemoryConsumptionMessageProcessor);
        memberGradientOfMemoryConsumptionMessageProcessor = new MemberGradientOfMemoryConsumptionMessageProcessor();
        add(memberGradientOfMemoryConsumptionMessageProcessor);
        memberSecondDerivativeOfMemoryConsumptionMessageProcessor = new MemberSecondDerivativeOfMemoryConsumptionMessageProcessor();
        add(memberSecondDerivativeOfMemoryConsumptionMessageProcessor);

        averageRequestsInFlightMessageProcessor = new AverageRequestsInFlightMessageProcessor();
        add(averageRequestsInFlightMessageProcessor);
        gradientOfRequestsInFlightMessageProcessor = new GradientOfRequestsInFlightMessageProcessor();
        add(gradientOfRequestsInFlightMessageProcessor);
        secondDerivativeOfRequestsInFlightMessageProcessor = new SecondDerivativeOfRequestsInFlightMessageProcessor();
        add(secondDerivativeOfRequestsInFlightMessageProcessor);

        averageLoadAverageMessageProcessor = new AverageLoadAverageMessageProcessor();
        add(averageLoadAverageMessageProcessor);
        gradientOfLoadAverageMessageProcessor = new GradientOfLoadAverageMessageProcessor();
        add(gradientOfLoadAverageMessageProcessor);
        secondDerivativeOfLoadAverageMessageProcessor = new SecondDerivativeOfLoadAverageMessageProcessor();
        add(secondDerivativeOfLoadAverageMessageProcessor);

        averageMemoryConsumptionMessageProcessor = new AverageMemoryConsumptionMessageProcessor();
        add(averageMemoryConsumptionMessageProcessor);
        gradientOfMemoryConsumptionMessageProcessor = new GradientOfMemoryConsumptionMessageProcessor();
        add(gradientOfMemoryConsumptionMessageProcessor);
        secondDerivativeOfMemoryConsumptionMessageProcessor = new SecondDerivativeOfMemoryConsumptionMessageProcessor();
        add(secondDerivativeOfMemoryConsumptionMessageProcessor);

        memberFaultMessageProcessor = new MemberFaultMessageProcessor();
        add(memberFaultMessageProcessor);
    }

    public void addEventListener(EventListener eventListener) {

        if (eventListener instanceof AverageLoadAverageEventListener) {
            averageLoadAverageMessageProcessor.addEventListener(eventListener);
        } else if (eventListener instanceof AverageMemoryConsumptionEventListener) {
            averageMemoryConsumptionMessageProcessor.addEventListener(eventListener);
        } else if (eventListener instanceof AverageRequestsInFlightEventListener) {
            averageRequestsInFlightMessageProcessor.addEventListener(eventListener);
        } else if (eventListener instanceof GradientOfLoadAverageEventListener) {
            gradientOfLoadAverageMessageProcessor.addEventListener(eventListener);
        } else if (eventListener instanceof GradientOfMemoryConsumptionEventListener) {
            gradientOfMemoryConsumptionMessageProcessor.addEventListener(eventListener);
        } else if (eventListener instanceof GradientOfRequestsInFlightEventListener) {
            gradientOfRequestsInFlightMessageProcessor.addEventListener(eventListener);
        } else if (eventListener instanceof MemberAverageLoadAverageEventListener) {
            memberAverageLoadAverageMessageProcessor.addEventListener(eventListener);
        } else if (eventListener instanceof MemberAverageMemoryConsumptionEventListener) {
            memberAverageMemoryConsumptionMessageProcessor.addEventListener(eventListener);
        } else if (eventListener instanceof MemberFaultEventListener) {
            memberFaultMessageProcessor.addEventListener(eventListener);
        } else if (eventListener instanceof MemberGradientOfLoadAverageEventListener) {
            memberGradientOfLoadAverageMessageProcessor.addEventListener(eventListener);
        } else if (eventListener instanceof MemberGradientOfMemoryConsumptionEventListener) {
            memberGradientOfMemoryConsumptionMessageProcessor.addEventListener(eventListener);
        } else if (eventListener instanceof MemberSecondDerivativeOfLoadAverageEventListener) {
            memberSecondDerivativeOfLoadAverageMessageProcessor.addEventListener(eventListener);
        } else if (eventListener instanceof MemberSecondDerivativeOfMemoryConsumptionEventListener) {
            memberSecondDerivativeOfMemoryConsumptionMessageProcessor.addEventListener(eventListener);
        } else if (eventListener instanceof SecondDerivativeOfLoadAverageEventListener) {
            secondDerivativeOfLoadAverageMessageProcessor.addEventListener(eventListener);
        } else if (eventListener instanceof SecondDerivativeOfMemoryConsumptionEventListener) {
            secondDerivativeOfMemoryConsumptionMessageProcessor.addEventListener(eventListener);

        }else if (eventListener instanceof SecondDerivativeOfRequestsInFlightEventListener) {
            secondDerivativeOfRequestsInFlightMessageProcessor.addEventListener(eventListener);

        }
        else {
            throw new RuntimeException("Unknown event listener");
        }
    }
}
