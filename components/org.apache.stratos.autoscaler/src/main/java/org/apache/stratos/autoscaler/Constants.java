package org.apache.stratos.autoscaler;

/**
 *
 */
public class Constants {

    public static final String ROUND_ROBIN_ALGORITHM_ID = "round-robin";
    public static final String ONE_AFTER_ANOTHER_ALGORITHM_ID = "one-after-another";
    public static final String MEMBER_FAULT_EVENT_NAME = "member_fault";

    //scheduler
    public static final int SCHEDULE_DEFAULT_INITIAL_DELAY = 30;
    public static final int SCHEDULE_DEFAULT_PERIOD = 15;

    public static final String AUTOSCALER_CONFIG_FILE_NAME = "autoscaler.xml";

    public static final String CLOUD_CONTROLLER_SERVICE_SFX = "services/CloudControllerService";
    public static final int CLOUD_CONTROLLER_DEFAULT_PORT = 9444;
    
    // partition properties
    public static final String REGION_PROPERTY = "region";

    public static final String AVERAGE_LOAD_AVERAGE = "average_load_average";
    public static final String AVERAGE_MEMORY_CONSUMPTION = "average_memory_consumption";
    public static final String AVERAGE_REQUESTS_IN_FLIGHT = "average_in_flight_requests";

    public static final String GRADIENT_LOAD_AVERAGE = "gradient_load_average";
    public static final String GRADIENT_MEMORY_CONSUMPTION = "gradient_memory_consumption";
    public static final String GRADIENT_OF_REQUESTS_IN_FLIGHT = "gradient_in_flight_requests";

    public static final String SECOND_DERIVATIVE_OF_REQUESTS_IN_FLIGHT = "second_derivative_in_flight_requests";
    public static final String SECOND_DERIVATIVE_OF_MEMORY_CONSUMPTION = "second_derivative_memory_consumption";
    public static final String SECOND_DERIVATIVE_OF_LOAD_AVERAGE = "second_derivative_load_average";
}
