package org.apache.stratos.autoscaler;

/**
 *
 */
public class Constants {

    public static String ROUND_ROBIN_ALGORITHM_ID = "round-robin";
    public static String ONE_AFTER_ANOTHER_ALGORITHM_ID = "one-after-another";

    public static String GRADIENT_OF_REQUESTS_IN_FLIGHT = "gradient_of_requests_in_flight";
    public static String AVERAGE_REQUESTS_IN_FLIGHT = "average_requests_in_flight";
    public static String SECOND_DERIVATIVE_OF_REQUESTS_IN_FLIGHT = "second_derivative_of_requests_in_flight";
    
    public static String MEMBER_FAULT_EVENT_NAME = "member_fault";

    //scheduler
    public static final int SCHEDULE_DEFAULT_INITIAL_DELAY = 30;
    public static final int SCHEDULE_DEFAULT_PERIOD = 15;

    public static final String AUTOSCALER_CONFIG_FILE_NAME = "autoscaler.xml";

    public static final String CLOUD_CONTROLLER_SERVICE_SFX = "services/CloudControllerService";
    public static final int CLOUD_CONTROLLER_DEFAULT_PORT = 9444;

}
