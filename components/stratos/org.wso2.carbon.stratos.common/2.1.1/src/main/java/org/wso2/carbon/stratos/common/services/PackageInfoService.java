package org.wso2.carbon.stratos.common.services;

import java.util.List;

import org.wso2.carbon.stratos.common.internal.CloudCommonServiceComponent;
import org.wso2.carbon.stratos.common.packages.PackageInfo;

public class PackageInfoService {

	public PackageInfo[] getPackageInfos() throws Exception {
		List<PackageInfo> list = CloudCommonServiceComponent.getPackageInfos().
		                                                     getMultitenancyPackages();
		PackageInfo[] packageInfos = list.toArray(new PackageInfo[list.size()]);
		return packageInfos;
	}
}
