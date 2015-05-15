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

package org.apache.stratos.mock.iaas.config;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.commons.lang3.StringUtils;
import org.apache.stratos.common.util.AxiomXpathParserUtil;
import org.apache.stratos.mock.iaas.services.impl.MockScalingFactor;
import org.apache.stratos.mock.iaas.statistics.StatisticsPatternMode;
import org.apache.stratos.mock.iaas.statistics.generator.MockHealthStatisticsPattern;

import javax.xml.namespace.QName;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Mock health statistics configuration parser.
 */
public class MockIaasConfigParser {

    private static final QName ENABLED_ATTRIBUTE = new QName("enabled");
    private static final QName TYPE_ATTRIBUTE = new QName("type");
    private static final QName FACTOR_ATTRIBUTE = new QName("factor");
    private static final QName MODE_ATTRIBUTE = new QName("mode");
    private static final String HEALTH_STATISTICS_ELEMENT = "health-statistics";
    private static final String SAMPLE_VALUES_ELEMENT = "sampleValues";
    private static final String SAMPLE_DURATION_ELEMENT = "sampleDuration";

    /**
     * Parse mock iaas configuration and return configuration object.
     *
     * @param filePath
     * @return
     */
    public static MockIaasConfig parse(String filePath) {
        try {
            MockIaasConfig mockIaasConfig = new MockIaasConfig();
            MockHealthStatisticsConfig mockHealthStatisticsConfig = new MockHealthStatisticsConfig();
            mockIaasConfig.setMockHealthStatisticsConfig(mockHealthStatisticsConfig);

            OMElement document = AxiomXpathParserUtil.parse(new File(filePath));
            String enabledStr = document.getAttributeValue(ENABLED_ATTRIBUTE);
            if (StringUtils.isEmpty(enabledStr)) {
                throw new RuntimeException("Enabled attribute not found in mock-iaas element");
            }
            mockIaasConfig.setEnabled(Boolean.parseBoolean(enabledStr));

            Iterator statisticsIterator = document.getChildElements();

            while (statisticsIterator.hasNext()) {
                OMElement statisticsElement = (OMElement) statisticsIterator.next();

                if (HEALTH_STATISTICS_ELEMENT.equals(statisticsElement.getQName().getLocalPart())) {
                    Iterator cartridgeIterator = statisticsElement.getChildElements();

                    while (cartridgeIterator.hasNext()) {
                        OMElement cartridgeElement = (OMElement) cartridgeIterator.next();
                        OMAttribute typeAttribute = cartridgeElement.getAttribute(TYPE_ATTRIBUTE);
                        if (typeAttribute == null) {
                            throw new RuntimeException("Type attribute not found in cartridge element");
                        }
                        String cartridgeType = typeAttribute.getAttributeValue();
                        Iterator patternIterator = cartridgeElement.getChildElements();

                        while (patternIterator.hasNext()) {
                            OMElement patternElement = (OMElement) patternIterator.next();

                            OMAttribute factorAttribute = patternElement.getAttribute(FACTOR_ATTRIBUTE);
                            if (factorAttribute == null) {
                                throw new RuntimeException("Factor attribute not found in pattern element: " +
                                        "[cartridge-type] " + cartridgeType);
                            }
                            String factorStr = factorAttribute.getAttributeValue();
                            MockScalingFactor scalingFactor = convertScalingFactor(factorStr);

                            OMAttribute modeAttribute = patternElement.getAttribute(MODE_ATTRIBUTE);
                            if (modeAttribute == null) {
                                throw new RuntimeException("Mode attribute not found in pattern element: " +
                                        "[cartridge-type] " + cartridgeType);
                            }
                            String modeStr = modeAttribute.getAttributeValue();
                            StatisticsPatternMode mode = convertMode(modeStr);

                            String sampleValuesStr = null;
                            String sampleDurationStr = null;
                            Iterator patternChildIterator = patternElement.getChildElements();

                            while (patternChildIterator.hasNext()) {
                                OMElement patternChild = (OMElement) patternChildIterator.next();
                                if (SAMPLE_VALUES_ELEMENT.equals(patternChild.getQName().getLocalPart())) {
                                    sampleValuesStr = patternChild.getText();
                                } else if (SAMPLE_DURATION_ELEMENT.equals(patternChild.getQName().getLocalPart())) {
                                    sampleDurationStr = patternChild.getText();
                                }
                            }

                            if (sampleValuesStr == null) {
                                throw new RuntimeException("Sample values not found in pattern [factor] " + factorStr);
                            }
                            if (sampleDurationStr == null) {
                                throw new RuntimeException("Sample duration not found in pattern [factor] " + factorStr);
                            }

                            String[] sampleValuesArray = sampleValuesStr.split(",");
                            List<Integer> sampleValues = convertStringArrayToIntegerList(sampleValuesArray);
                            int sampleDuration = Integer.parseInt(sampleDurationStr);

                            MockHealthStatisticsPattern mockHealthStatisticsPattern = new MockHealthStatisticsPattern
                                    (cartridgeType, scalingFactor, mode, sampleValues, sampleDuration);
                            mockHealthStatisticsConfig.addStatisticsPattern(mockHealthStatisticsPattern);
                        }
                    }
                }
            }
            return mockIaasConfig;
        } catch (Exception e) {
            throw new RuntimeException("Could not parse mock health statistics configuration", e);
        }
    }

    /**
     * Convert mode string to its enumeration
     *
     * @param modeStr mode string
     * @return statistics pattern enumeration
     */
    private static StatisticsPatternMode convertMode(String modeStr) {
        if ("loop".equals(modeStr)) {
            return StatisticsPatternMode.Loop;
        } else if ("continue".equals(modeStr)) {
            return StatisticsPatternMode.Continue;
        } else if ("stop".equals(modeStr)) {
            return StatisticsPatternMode.Stop;
        }
        throw new RuntimeException("An unknown statistics pattern mode found: " + modeStr);
    }

    /**
     * Convert scaling factor string to its enumeration
     *
     * @param scalingFactorStr scaling factor string
     * @return scaling factor enumeration
     */
    private static MockScalingFactor convertScalingFactor(String scalingFactorStr) {
        if ("memory-consumption".equals(scalingFactorStr)) {
            return MockScalingFactor.MemoryConsumption;
        } else if ("load-average".equals(scalingFactorStr)) {
            return MockScalingFactor.LoadAverage;
        } else if ("requests-in-flight".equals(scalingFactorStr)) {
            return MockScalingFactor.RequestsInFlight;
        }
        throw new RuntimeException("An unknown scaling factor found: " + scalingFactorStr);
    }

    /**
     * Convert string array to integer list
     *
     * @param stringArray string array
     * @return integer list
     */
    private static List<Integer> convertStringArrayToIntegerList(String[] stringArray) {
        List<Integer> integerList = new ArrayList<Integer>();
        for (String value : stringArray) {
            integerList.add(Integer.parseInt(value));
        }
        return integerList;
    }
}
