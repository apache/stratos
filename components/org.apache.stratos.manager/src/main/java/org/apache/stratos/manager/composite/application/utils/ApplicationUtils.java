package org.apache.stratos.manager.composite.application.utils;

import java.util.concurrent.Executor;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.manager.exception.InvalidCartridgeAliasException;
import org.apache.stratos.messaging.broker.publish.EventPublisher;
import org.apache.stratos.messaging.broker.publish.EventPublisherPool;
import org.apache.stratos.messaging.util.Constants;


// Grouping
public class ApplicationUtils {
	
	private static Log log = LogFactory.getLog(ApplicationUtils.class);

    public static boolean isAliasValid (String alias) {

        String patternString = "([a-z0-9]+([-][a-z0-9])*)+";
        Pattern pattern = Pattern.compile(patternString);
        
        return pattern.matcher(alias).matches();
    }


}
