/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.common.beans;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * TODO : Should be able to support multiple roles for user
 */
@XmlRootElement
public class UserInfoBean {

    private String userName;
    private String credential;
    private String role;
    private String firstName;
    private String lastName;
    private String email;
    private String profileName;

    public UserInfoBean() {
    }

    public UserInfoBean(UserInfoBean userInfoBean) {
        this.userName = userInfoBean.getUserName();
        this.credential = userInfoBean.getCredential();
        this.role = userInfoBean.getRole();
        this.firstName = userInfoBean.getFirstName();
        this.lastName = userInfoBean.getLastName();
        this.email = userInfoBean.getEmail();
        this.profileName = userInfoBean.getProfileName();
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getCredential() {
        return credential;
    }

    public void setCredential(String credential) {
        this.credential = credential;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getProfileName() {
        return profileName;
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

}
