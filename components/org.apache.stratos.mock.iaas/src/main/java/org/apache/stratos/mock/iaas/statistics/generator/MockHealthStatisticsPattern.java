/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.mock.iaas.statistics.generator;

import org.apache.stratos.mock.iaas.exceptions.ContinueLastSampleValueException;
import org.apache.stratos.mock.iaas.exceptions.NoSampleValuesFoundException;
import org.apache.stratos.mock.iaas.exceptions.StopStatisticsPublishingException;
import org.apache.stratos.mock.iaas.services.impl.MockScalingFactor;
import org.apache.stratos.mock.iaas.statistics.StatisticsPatternMode;

import java.util.Iterator;
import java.util.List;

/**
 * Mock health statistics pattern definition.
 */
public class MockHealthStatisticsPattern {

    private String cartridgeType;
    private MockScalingFactor factor;
    private StatisticsPatternMode mode;
    private List<Integer> sampleValues;
    private int sampleDuration;
    private Iterator sampleValuesIterator;

    public MockHealthStatisticsPattern(String cartridgeType, MockScalingFactor factor, StatisticsPatternMode mode, List<Integer> sampleValues,
                                       int sampleDuration) {
        this.cartridgeType = cartridgeType;
        this.factor = factor;
        this.mode = mode;
        this.sampleValues = sampleValues;
        this.sampleValuesIterator = this.sampleValues.iterator();
        this.sampleDuration = sampleDuration;
    }

    public String getCartridgeType() {
        return cartridgeType;
    }

    /**
     * Returns scaling factor
     *
     * @return
     */
    public MockScalingFactor getFactor() {
        return factor;
    }

    /**
     * Returns statistics pattern mode
     *
     * @return
     */
    public StatisticsPatternMode getMode() {
        return mode;
    }

    /**
     * Returns next sample value
     *
     * @return
     */
    public int getNextSample() throws NoSampleValuesFoundException, StopStatisticsPublishingException,
            ContinueLastSampleValueException {
        if ((sampleValues == null) || (sampleValues.size() < 1)) {
            throw new NoSampleValuesFoundException();
        }

        if (!sampleValuesIterator.hasNext()) {
            // Iterator has come to the end of the list
            if (getMode() == StatisticsPatternMode.Loop) {
                // Looping: reset the iterator
                sampleValuesIterator = sampleValues.iterator();
                return Integer.parseInt(sampleValuesIterator.next().toString());
            } else if (getMode() == StatisticsPatternMode.Continue) {
                // Continue: return the last value
                int lastSampleValue = Integer.parseInt(sampleValues.get(sampleValues.size() - 1).toString());
                throw new ContinueLastSampleValueException(lastSampleValue);
            } else if (getMode() == StatisticsPatternMode.Stop) {
                throw new StopStatisticsPublishingException();
            } else {
                throw new RuntimeException("An unknown statistics pattern mode found");
            }
        } else {
            return Integer.parseInt(sampleValuesIterator.next().toString());
        }
    }

    /**
     * Returns sample duration in seconds
     *
     * @return
     */
    public int getSampleDuration() {
        return sampleDuration;
    }
}
