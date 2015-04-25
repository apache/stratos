/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.load.balancer.test;

import org.apache.stratos.load.balancer.statistics.InFlightRequestDecrementCallable;
import org.apache.stratos.load.balancer.statistics.InFlightRequestIncrementCallable;
import org.apache.stratos.load.balancer.statistics.LoadBalancerStatisticsCollector;
import org.apache.stratos.load.balancer.statistics.LoadBalancerStatisticsExecutor;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

/**
 * Load balancer statistics collector tests.
 */
@RunWith(JUnit4.class)
public class LoadBalancerStatisticsCollectorTest {

    /**
     * Test in-flight request count calculation.
     */
    @Test
    public void testInFlightRequestCountCalculation() {
        String clusterId = "cluster1";
        String incrementErrorMessage = "Could not increment in-flight request count: ";
        String decrementErrorMessage = "Could not decrement in-flight request count: ";

        FutureTask<Object> task = new FutureTask<Object>(new InFlightRequestIncrementCallable(clusterId));
        executeTask(task);
        Assert.assertEquals(incrementErrorMessage, 1, LoadBalancerStatisticsCollector.getInstance().getInFlightRequestCount(clusterId));

        task = new FutureTask<Object>(new InFlightRequestIncrementCallable(clusterId));
        executeTask(task);
        Assert.assertEquals(incrementErrorMessage, 2, LoadBalancerStatisticsCollector.getInstance().getInFlightRequestCount(clusterId));

        task = new FutureTask<Object>(new InFlightRequestIncrementCallable(clusterId));
        executeTask(task);
        Assert.assertEquals(incrementErrorMessage, 3, LoadBalancerStatisticsCollector.getInstance().getInFlightRequestCount(clusterId));

        task = new FutureTask<Object>(new InFlightRequestDecrementCallable(clusterId));
        executeTask(task);
        Assert.assertEquals(decrementErrorMessage, 2, LoadBalancerStatisticsCollector.getInstance().getInFlightRequestCount(clusterId));

        task = new FutureTask<Object>(new InFlightRequestDecrementCallable(clusterId));
        executeTask(task);
        Assert.assertEquals(decrementErrorMessage, 1, LoadBalancerStatisticsCollector.getInstance().getInFlightRequestCount(clusterId));

        task = new FutureTask<Object>(new InFlightRequestDecrementCallable(clusterId));
        executeTask(task);
        Assert.assertEquals(decrementErrorMessage, 0, LoadBalancerStatisticsCollector.getInstance().getInFlightRequestCount(clusterId));

        LoadBalancerStatisticsCollector.clear();
    }

    private void executeTask(FutureTask<Object> task) {
        Future future = LoadBalancerStatisticsExecutor.getInstance().getService().submit(task);
        while (!future.isDone()) {
            // Wait until task get executed
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // Might not need to trace
            }
        }
    }
}
