package org.apache.stratos.rest.endpoint.mock;/*
*  Copyright (c) 2005-2012, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

import org.apache.stratos.adc.mgt.dto.Cartridge;
import org.apache.stratos.adc.mgt.dto.SubscriptionInfo;
import org.apache.stratos.rest.endpoint.bean.CartridgeInfoBean;
import org.apache.stratos.rest.endpoint.bean.cartridge.definition.CartridgeDefinitionBean;

import java.util.*;

public class MockContext {
    private static MockContext mockContext = new MockContext(); // singleton
    private List<CartridgeDefinitionBean> cartridgeDefinitionBeanList = new LinkedList<CartridgeDefinitionBean>();
    private Map<String,Cartridge> availableSingleTenantCartridges = new HashMap<String,Cartridge>();
    private Map<String,Cartridge> availableMultiTenantCartridges = new HashMap<String,Cartridge>();
    private Map<String,Cartridge> subscribedCartridges = new HashMap<String,Cartridge>();

    private Set<Cartridge> temp = new HashSet<Cartridge>();


    private MockContext(){} // do not allow to initialize

    public static MockContext getInstance(){
        return mockContext;
    }


    public void addCartirdgeDefinition(CartridgeDefinitionBean cartridgeDefinitionBean){
        this.cartridgeDefinitionBeanList.add(cartridgeDefinitionBean);
        Cartridge cartridge = new Cartridge();
        cartridge.setCartridgeType(cartridgeDefinitionBean.type);
        cartridge.setDescription(cartridgeDefinitionBean.description);
        cartridge.setDisplayName(cartridgeDefinitionBean.displayName);
        cartridge.setMultiTenant(cartridgeDefinitionBean.multiTenant);
        cartridge.setProvider(cartridgeDefinitionBean.provider);
        cartridge.setVersion(cartridgeDefinitionBean.version);

        if(cartridge.isMultiTenant()){
            availableMultiTenantCartridges.put(cartridge.getCartridgeType(), cartridge);
        }else{
            availableSingleTenantCartridges.put(cartridge.getCartridgeType(), cartridge);
        }
    }



    public Cartridge[] getAvailableMultiTenantCartridges(){
       return availableMultiTenantCartridges.values().toArray(new Cartridge[0]);
    }


    public Cartridge[] getAvailableSingleTenantCartridges(){
        return availableSingleTenantCartridges.values().toArray(new Cartridge[0]);
    }

    public Cartridge[] getSubscribedCartridges(){
        return subscribedCartridges.values().toArray(new Cartridge[0]);
    }

    public SubscriptionInfo subscribeToCartridge(CartridgeInfoBean cartridgeInfoBean){
         String cartridgeType = cartridgeInfoBean.getCartridgeType();
         String alias = cartridgeInfoBean.getAlias();
         Cartridge subscribedCartridge;
         // retrieve the cartridge from available ones
         if(availableSingleTenantCartridges.containsKey(alias)){
            subscribedCartridge = availableSingleTenantCartridges.get(cartridgeType);

         }else if(availableMultiTenantCartridges.containsKey(alias)){
             subscribedCartridge = availableMultiTenantCartridges.get(cartridgeType);
         }else {
             throw new RuntimeException("Wrong programme sequence"); // TODO; handle properly
         }
        //Proper way is copy construrctor
        Cartridge copy = new Cartridge();
        copy.setCartridgeType(subscribedCartridge.getCartridgeType());
        copy.setDescription(subscribedCartridge.getDescription());
        copy.setDisplayName(subscribedCartridge.getDisplayName());
        copy.setMultiTenant(subscribedCartridge.isMultiTenant());
        copy.setProvider(subscribedCartridge.getProvider());
        copy.setVersion(subscribedCartridge.getVersion());
        copy.setCartridgeAlias(alias);
        copy.setHostName("dummy.stratos.com");
        copy.setRepoURL("http://dummy.stratos.com/myrepo.git");

        subscribedCartridges.put(alias,copy);

        SubscriptionInfo subscriptionInfo = new SubscriptionInfo();
        subscriptionInfo.setHostname(copy.getHostName());
        subscriptionInfo.setRepositoryURL(copy.getRepoURL());

        return subscriptionInfo;
    }

    public void unsubscribe(String alias) {
        if(subscribedCartridges.containsKey(alias)){
            subscribedCartridges.remove(alias);
        }
    }
}
