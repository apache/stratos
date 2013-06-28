package org.wso2.carbon.autoscaler.service.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.autoscaler.service.IAutoscalerService;
import org.wso2.carbon.autoscaler.service.impl.AutoscalerServiceImpl;

/**
 * Registering Autoscaler Service.
 * @scr.component name="org.wso2.carbon.autoscaler.service" immediate="true"
 */
public class AutoscalerServiceDSComponent {

    private static final Log log = LogFactory.getLog(AutoscalerServiceDSComponent.class);

    protected void activate(ComponentContext context) {
        try {
            BundleContext bundleContext = context.getBundleContext();
            bundleContext.registerService(IAutoscalerService.class.getName(),
                                          new AutoscalerServiceImpl(), null);

            log.debug("******* Autoscaler Service bundle is activated ******* ");
        } catch (Exception e) {
            log.error("******* Autoscaler Service bundle is failed to activate ****", e);
        }
    }
}