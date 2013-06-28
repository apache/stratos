package org.wso2.carbon.usage.beans;


public class CartridgeStatistics {
    
    private String key;
    private String instanceId;
    private long cartridgeHours;
    
    public CartridgeStatistics(){
        //Default constructor
    }

    public CartridgeStatistics(String key){
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public long getCartridgeHours() {
        return cartridgeHours;
    }

    public void setCartridgeHours(long cartridgeHours) {
        this.cartridgeHours = cartridgeHours;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }
}
