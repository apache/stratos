package org.apache.stratos.manager.application.signups;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.manager.registry.RegistryManager;
import org.apache.stratos.manager.domain.ApplicationSignUp;
import org.apache.stratos.manager.exception.ApplicationSignUpException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Application signup manager.
 */
public class ApplicationSignUpManager {

    private static final Log log = LogFactory.getLog(ApplicationSignUpManager.class);

    private static final String APPLICATION_SIGNUP_RESOURCE_PATH = "/stratos.manager/application.signups/";

    /**
     * Add application signup.
     * @param applicationSignUp
     * @throws ApplicationSignUpException
     */
    public String addApplicationSignUp(ApplicationSignUp applicationSignUp) throws ApplicationSignUpException {
        try {
            if(applicationSignUp == null) {
                throw new RuntimeException("Application signup is null");
            }

            if(log.isInfoEnabled()) {
                log.info(String.format("Adding application signup: [application-id] %s",
                        applicationSignUp.getApplicationId()));
            }

            String signUpId = UUID.randomUUID().toString();
            applicationSignUp.setSignUpId(signUpId);
            String resourcePath = APPLICATION_SIGNUP_RESOURCE_PATH + applicationSignUp.getSignUpId();
            RegistryManager.getInstance().persist(resourcePath, applicationSignUp);

            if(log.isInfoEnabled()) {
                log.info(String.format("Application signup added successfully: [application-id] %s [signup-id] %s",
                        applicationSignUp.getApplicationId(), applicationSignUp.getSignUpId()));
            }
            return signUpId;
        } catch (Exception e) {
            String message = "Could not add application signup";
            log.error(message, e);
            throw new ApplicationSignUpException(message, e);
        }
    }

    /**
     * Remove application signup by signup id.
     * @param signUpId
     * @throws ApplicationSignUpException
     */
    public void removeApplicationSignUp(String signUpId) throws ApplicationSignUpException {
        try {
            if(log.isInfoEnabled()) {
                log.info(String.format("Removing application signup: [signup-id] %s", signUpId));
            }

            String resourcePath = APPLICATION_SIGNUP_RESOURCE_PATH + signUpId;
            ApplicationSignUp applicationSignUp = (ApplicationSignUp) RegistryManager.getInstance().read(resourcePath);
            if(applicationSignUp == null) {
                throw new RuntimeException(String.format("Application signup not found: [signup-id] %s", signUpId));
            }

            RegistryManager.getInstance().remove(resourcePath);

            if(log.isInfoEnabled()) {
                log.info(String.format("Application signup removed successfully: [application-id] %s [signup-id] %s",
                        applicationSignUp.getApplicationId(), applicationSignUp.getSignUpId()));
            }
        } catch (Exception e) {
            String message = "Could not add application signup";
            log.error(message, e);
            throw new ApplicationSignUpException(message, e);
        }
    }

    /**
     * Get application signup by signup id.
     * @param signUpId
     * @return
     * @throws ApplicationSignUpException
     */
    public ApplicationSignUp getApplicationSignUp(String signUpId) throws ApplicationSignUpException {
        try {
            if(log.isDebugEnabled()) {
                log.debug(String.format("Reading application signup: [signup-id] %s", signUpId));
            }

            String resourcePath = APPLICATION_SIGNUP_RESOURCE_PATH + signUpId;
            ApplicationSignUp applicationSignUp = (ApplicationSignUp) RegistryManager.getInstance().read(resourcePath);
            return applicationSignUp;
        } catch (Exception e) {
            String message = "Could not get application signup";
            log.error(message, e);
            throw new ApplicationSignUpException(message, e);
        }
    }

    /**
     * Get application singups of an application by application id.
     * @param applicationId
     * @return
     * @throws ApplicationSignUpException
     */
    public List<ApplicationSignUp> getApplicationSignUps(String applicationId) throws ApplicationSignUpException {
        try {
            if(log.isDebugEnabled()) {
                log.debug(String.format("Reading application signups: [application-id] %s", applicationId));
            }

            if(StringUtils.isBlank(applicationId)) {
                throw new RuntimeException("Application id is null");
            }
            List<ApplicationSignUp> applicationSignUps = new ArrayList<ApplicationSignUp>();

            String[] resourcePaths = (String[]) RegistryManager.getInstance().read(APPLICATION_SIGNUP_RESOURCE_PATH);
            if(resourcePaths != null) {
                for (String resourcePath : resourcePaths) {
                    if(resourcePath != null) {
                        ApplicationSignUp applicationSignUp = (ApplicationSignUp)
                                RegistryManager.getInstance().read(resourcePath);
                        if (applicationId.equals(applicationSignUp.getApplicationId())) {
                            applicationSignUps.add(applicationSignUp);
                        }
                    }
                }
            }

            return applicationSignUps;
        } catch (Exception e) {
            String message = "Could not get application signups";
            log.error(message, e);
            throw new ApplicationSignUpException(message, e);
        }
    }
}
