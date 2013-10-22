package org.apache.stratos.autoscaler.rule.input;

/**
 * This class will keep additional parameters such as loaf average and memory consumption
 */

public class MemberContext {
    private float loadAverage;
    private float memoryConsumption;
    private String memberId;

    public MemberContext(String memberId){
        this.memberId = memberId;
    }

    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public float getMemoryConsumption() {
        return memoryConsumption;
    }

    public void setMemoryConsumption(float memoryConsumption) {
        this.memoryConsumption = memoryConsumption;
    }

    public float getLoadAverage() {
        return loadAverage;
    }

    public void setLoadAverage(float loadAverage) {
        this.loadAverage = loadAverage;
    }
}
