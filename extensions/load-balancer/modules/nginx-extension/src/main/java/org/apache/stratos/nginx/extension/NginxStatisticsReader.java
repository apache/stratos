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

package org.apache.stratos.nginx.extension;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.stratos.common.constants.StratosConstants;
import org.apache.stratos.load.balancer.common.domain.Cluster;
import org.apache.stratos.load.balancer.common.domain.Port;
import org.apache.stratos.load.balancer.common.domain.Service;
import org.apache.stratos.load.balancer.common.statistics.LoadBalancerStatisticsReader;
import org.apache.stratos.load.balancer.common.topology.TopologyProvider;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Nginx statistics reader.
 */
public class NginxStatisticsReader implements LoadBalancerStatisticsReader {

    private static final Log log = LogFactory.getLog(NginxStatisticsReader.class);

    private TopologyProvider topologyProvider;
    private String clusterInstanceId;

    public NginxStatisticsReader(TopologyProvider topologyProvider) {
        this.topologyProvider = topologyProvider;
        this.clusterInstanceId = System.getProperty(StratosConstants.CLUSTER_INSTANCE_ID, StratosConstants.NOT_DEFINED);
    }

    @Override
    public String getClusterInstanceId() {
        return clusterInstanceId;
    }

    @Override
    public int getInFlightRequestCount(String clusterId) {
        Cluster cluster = topologyProvider.getClusterByClusterId(clusterId);
        if(cluster != null) {
            String serviceName = cluster.getServiceName();
            Service service = topologyProvider.getTopology().getService(serviceName);
            if(service != null) {
                int inFlightRequestCount = 0;
                for(Port port : service.getPorts()) {
                    inFlightRequestCount += findWritingCount(port.getProxy());
                }
                if(log.isDebugEnabled()) {
                    log.debug(String.format("In-flight request count: [cluster-id] %s [value] %d",
                            clusterId, inFlightRequestCount));
                }
                return inFlightRequestCount;
            }
        }
        return 0;
    }

    /**
     * Make a http request to http://127.0.0.1:<proxy-port>/nginx_status and find writing count.
     * @param proxyPort
     * @return
     */
    private int findWritingCount(int proxyPort) {
        try {
            URL url = new URL("http", "127.0.0.1", proxyPort, "/nginx_status");
            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpUriRequest request = new HttpGet(url.toURI());
            HttpResponse response = httpClient.execute(request);
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new RuntimeException("http://127.0.0.1:" + proxyPort + "/nginx_status was not found");
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    (response.getEntity().getContent())));
            String output, result = "";
            while ((output = reader.readLine()) != null) {
                result += output;
            }
            Pattern pattern = Pattern.compile("(Writing: )([0-1]*)");
            Matcher matcher = pattern.matcher(result);
            if (matcher.find()) {
                // Deduct one to remove the above request
                int writingCount = Integer.parseInt(matcher.group(2)) - 1;
                if(log.isDebugEnabled()) {
                    log.debug(String.format("Writing count: [proxy] %d [value] %d", proxyPort, writingCount));
                }
                return writingCount;
            }
            throw new RuntimeException("Writing block was not found in nginx_status response");
        } catch (HttpHostConnectException ignore) {
            if(ignore.getMessage().contains("Connection refused")) {
                log.warn("Could not find in-flight request count, connection refused: " +
                        "http://127.0.0.1:" + proxyPort + "/nginx_status");
            }
        } catch (Exception e) {
            log.error("Could not find in-flight request count: http://127.0.0.1:" + proxyPort + "/nginx_status", e);
        }
        return 0;
    }
}
