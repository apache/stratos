package org.wso2.carbon.stratos.common.packages;

/*
 * Deserialize following XML
<packages xmlns="http://wso2.com/carbon/multitenancy/billing/pacakges">
    <package name="multitenancy-free">
        <!--<subscriptionCharge>0</subscriptionCharge>--> <!-- $ per month -->
        <users>
            <limit>5</limit>
            <charge>0</charge> <!-- charge per month -->
        </users>
        <resourceVolume>
            <limit>10</limit> <!--mb per user -->
        </resourceVolume>
        <bandwidth>
            <limit>1000</limit> <!-- mb per user -->
            <overuseCharge>0</overuseCharge> <!-- $ per user per month -->
        </bandwidth>
    </package>
    <package name="multitenancy-small">
        ...
    </package>
</packages>
 */
public class PackageInfo {
	
	private String name;
	private int usersLimit;
	private int subscriptionCharge;
	private int chargePerUser;
	private int resourceVolumeLimit;
	private int bandwidthLimit;
	private int bandwidthOveruseCharge;

	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getUsersLimit() {
		return usersLimit;
	}

	public void setUsersLimit(int usersLimit) {
		this.usersLimit = usersLimit;
	}

	public int getSubscriptionCharge() {
		return subscriptionCharge;
	}

	public void setSubscriptionCharge(int subscriptionCharge) {
		this.subscriptionCharge = subscriptionCharge;
	}

	public int getChargePerUser() {
		return chargePerUser;
	}

	public void setChargePerUser(int chargePerUser) {
		this.chargePerUser = chargePerUser;
	}

	public int getResourceVolumeLimit() {
		return resourceVolumeLimit;
	}

	public void setResourceVolumeLimit(int resourceVolumeLimit) {
		this.resourceVolumeLimit = resourceVolumeLimit;
	}

	public int getBandwidthLimit() {
		return bandwidthLimit;
	}

	public void setBandwidthLimit(int bandwidthLimit) {
		this.bandwidthLimit = bandwidthLimit;
	}

	public int getBandwidthOveruseCharge() {
		return bandwidthOveruseCharge;
	}

	public void setBandwidthOveruseCharge(int bandwidthOveruseCharge) {
		this.bandwidthOveruseCharge = bandwidthOveruseCharge;
	}

    
}
