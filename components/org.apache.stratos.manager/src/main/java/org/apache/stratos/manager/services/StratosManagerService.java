package org.apache.stratos.manager.services;

import org.apache.stratos.manager.domain.ApplicationSignUp;
import org.apache.stratos.manager.exception.ApplicationSignUpException;

import java.util.List;

/**
 * Stratos manager service interface.
 */
public interface StratosManagerService {

    /**
     * Add application signup
     * @param applicationSignUp
     * @return signup id
     * @throws ApplicationSignUpException
     */
    public String addApplicationSignUp(ApplicationSignUp applicationSignUp) throws ApplicationSignUpException;

    /**
     * Remove application signup.
     * @param signUpId
     */
    public void removeApplicationSignUp(String signUpId) throws ApplicationSignUpException;

    /**
     * Get application signup.
     * @param signUpId
     * @return
     */
    public ApplicationSignUp getApplicationSignUp(String signUpId) throws ApplicationSignUpException;

    /**
     * Get application signups.
     * @return
     */
    public List<ApplicationSignUp> getApplicationSignUps(String applicationId) throws ApplicationSignUpException;
}
