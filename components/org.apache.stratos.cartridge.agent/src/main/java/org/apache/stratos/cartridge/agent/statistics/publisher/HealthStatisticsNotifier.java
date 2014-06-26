/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.cartridge.agent.statistics.publisher;

import org.apache.stratos.cartridge.agent.statistics.publisher.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cartridge.agent.config.CartridgeAgentConfiguration;
import org.apache.stratos.cartridge.agent.event.publisher.CartridgeAgentEventPublisher;
import org.apache.stratos.cartridge.agent.util.CartridgeAgentConstants;
import org.apache.stratos.cartridge.agent.util.CartridgeAgentUtils;

import java.io.File;
import java.util.List;

/**
 * Health statistics notifier thread for publishing statistics periodically to CEP.
 */
public class HealthStatisticsNotifier implements Runnable {
    private static final Log log = LogFactory.getLog(HealthStatisticsNotifier.class);

    private final HealthStatisticsPublisher statsPublisher;
    private IHealthStatisticsReader statsReader;
    private long statsPublisherInterval = 15000;
    private boolean terminated;

    public HealthStatisticsNotifier() {
        this.statsPublisher = new HealthStatisticsPublisher();

        /* Find all jars in the current working directory */
        String pluginFileName = System.getProperty("health.stats.reader.plugin");
        if (pluginFileName != null)
        {
            File pluginFile = new File(pluginFileName);
            if (    (pluginFile != null)
                 && (pluginFile.exists())) {
                List<Class> pluginClass = PluginLoader.loadPluginClassesFromJar(pluginFile, IHealthStatisticsReader.class);
                if (!pluginClass.isEmpty())
                {
                    try
                    {
                        log.trace("Instantiating new instance of plugin type " + pluginClass);
                        this.statsReader = (IHealthStatisticsReader)pluginClass.get(0).newInstance( );
                    }
                    catch(InstantiationException e)
                    {
                        log.error("Unable to instantiate plugin " + pluginClass, e);
                    }
                    catch(IllegalAccessException e)
                    {
                        log.error("Unable to instantiate plugin " + pluginClass, e);
                    }
                }
            }
            else
            {
                log.error("Plugin not found or malformed: " + pluginFileName + ((pluginFile == null)? " NULL": "Doesn't exist"));
            }
        }
        if (this.statsReader == null)
        {
            this.statsReader = new HealthStatisticsReader();
        }

        String interval = System.getProperty("stats.notifier.interval");
        if (interval != null) {
            statsPublisherInterval = Long.getLong(interval);
        }
    }

    @Override
    public void run() {
        if (this.statsReader.init() == false)
        {
            log.error("Health statistics reader "+this.statsReader.getClass().getName()+" could not initialise");
        }
        else
        {
            while (!terminated) {
                try {
                    try {
                        Thread.sleep(statsPublisherInterval);
                    } catch (InterruptedException ignore) {
                    }

                    if (statsPublisher.isEnabled()) {

                        CartridgeStatistics stats = statsReader.getCartridgeStatistics();

                        if(log.isDebugEnabled()) {
                            log.debug(String.format("Publishing memory consumption: %f", stats.getMemoryUsage()));
                        }
                        statsPublisher.publish(
                                CartridgeAgentConfiguration.getInstance().getClusterId(),
                                CartridgeAgentConfiguration.getInstance().getNetworkPartitionId(),
                                CartridgeAgentConfiguration.getInstance().getMemberId(),
                                CartridgeAgentConfiguration.getInstance().getPartitionId(),
                                CartridgeAgentConstants.MEMORY_CONSUMPTION,
                                stats.getMemoryUsage()
                        );

                        if(log.isDebugEnabled()) {
                            log.debug(String.format("Publishing load average: %f", stats.getProcessorUsage()));
                        }
                        statsPublisher.publish(
                                CartridgeAgentConfiguration.getInstance().getClusterId(),
                                CartridgeAgentConfiguration.getInstance().getNetworkPartitionId(),
                                CartridgeAgentConfiguration.getInstance().getMemberId(),
                                CartridgeAgentConfiguration.getInstance().getPartitionId(),
                                CartridgeAgentConstants.LOAD_AVERAGE,
                                stats.getProcessorUsage()
                        );
                    } else if (log.isWarnEnabled()) {
                        log.warn("Statistics publisher is disabled");
                    }
                } catch (Exception e) {
                    if (log.isErrorEnabled()) {
                        log.error("Could not publish health statistics", e);
                    }
                }
            }
            this.statsReader.delete();
        }
    }

    /**
     * Terminate load balancer statistics notifier thread.
     */
    public void terminate() {
        terminated = true;
    }
}
