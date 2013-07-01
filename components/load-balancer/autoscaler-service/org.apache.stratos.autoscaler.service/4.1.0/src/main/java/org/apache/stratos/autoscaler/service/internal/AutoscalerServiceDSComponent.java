package org.apache.stratos.autoscaler.service.internal;

import org.apache.stratos.autoscaler.service.impl.AutoscalerServiceImpl;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.apache.stratos.autoscaler.service.IAutoscalerService;

/**
 * Registering Autoscaler Service.
 * @scr.component name="org.apache.stratos.autoscaler.service" immediate="true"
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