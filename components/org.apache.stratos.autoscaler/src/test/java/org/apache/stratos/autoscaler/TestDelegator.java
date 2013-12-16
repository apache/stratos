package org.apache.stratos.autoscaler;

/**
 * Helper class to keep the state of the consequence of a rule.
 * @author nirmal
 *
 */
public class TestDelegator {
    private static boolean isDelegated;

    public static boolean isDelegated() {
        return isDelegated;
    }

    public static void setDelegated(boolean isDelegated) {
        TestDelegator.isDelegated = isDelegated;
    }

    
}