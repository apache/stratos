package org.apache.stratos.manager.services.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.manager.application.signups.ApplicationSignUpManager;
import org.apache.stratos.manager.domain.ApplicationSignUp;
import org.apache.stratos.manager.exception.ApplicationSignUpException;
import org.apache.stratos.manager.services.StratosManagerService;

import java.util.List;

/**
 * Stratos manager service implementation.
 */
public class StratosManagerServiceImpl implements StratosManagerService {

    private static final Log log = LogFactory.getLog(StratosManagerServiceImpl.class);

    private ApplicationSignUpManager signUpManager = new ApplicationSignUpManager();

    @Override
    public String addApplicationSignUp(ApplicationSignUp applicationSignUp) throws ApplicationSignUpException {
        return signUpManager.addApplicationSignUp(applicationSignUp);
    }

    @Override
    public void removeApplicationSignUp(String signUpId) throws ApplicationSignUpException {
        signUpManager.removeApplicationSignUp(signUpId);
    }

    @Override
    public ApplicationSignUp getApplicationSignUp(String signUpId) throws ApplicationSignUpException {
        return signUpManager.getApplicationSignUp(signUpId);
    }

    @Override
    public List<ApplicationSignUp> getApplicationSignUps(String applicationId) throws ApplicationSignUpException {
        return signUpManager.getApplicationSignUps(applicationId);
    }
}
