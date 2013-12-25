package org.apache.stratos.autoscaler.rule;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Drools rule log for logging inside rule files.
 */
public class RuleLog {
    private static final Log log = LogFactory.getLog(RuleLog.class);

    private static volatile RuleLog instance;

    private RuleLog() {
    }

    public static RuleLog getInstance() {
        if (instance == null) {
            synchronized (RuleLog.class) {
                if (instance == null) {
                    instance = new RuleLog();
                }
            }
        }
        return instance;
    }

    public boolean info(String value) {
        if(log.isInfoEnabled()) {
            log.info(value);
        }
        return true;
    }

    public boolean debug(String value) {
        if(log.isDebugEnabled()) {
            log.debug(value);
        }
        return true;
    }

    public boolean warn(String value) {
        if(log.isWarnEnabled()) {
            log.warn(value);
        }
        return true;
    }

    public boolean error(String value) {
        if(log.isErrorEnabled()) {
            log.error(value);
        }
        return true;
    }
}
