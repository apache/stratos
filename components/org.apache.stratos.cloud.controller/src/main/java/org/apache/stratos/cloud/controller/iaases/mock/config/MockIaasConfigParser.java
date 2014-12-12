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

package org.apache.stratos.cloud.controller.iaases.mock.config;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.commons.lang3.StringUtils;
import org.apache.stratos.cloud.controller.iaases.mock.MockAutoscalingFactor;
import org.apache.stratos.cloud.controller.iaases.mock.statistics.StatisticsPatternMode;
import org.apache.stratos.cloud.controller.iaases.mock.statistics.generator.MockHealthStatisticsPattern;
import org.apache.stratos.cloud.controller.util.AxiomXpathParserUtil;

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
            if(StringUtils.isEmpty(enabledStr)) {
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
                            MockAutoscalingFactor autoscalingFactor = convertAutoscalingFactor(factorStr);

                            OMAttribute modeAttribute = patternElement.getAttribute(MODE_ATTRIBUTE);
                            if(modeAttribute == null) {
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
                                    (cartridgeType, autoscalingFactor, mode, sampleValues, sampleDuration);
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

    private static StatisticsPatternMode convertMode(String modeStr) {
        if("loop".equals(modeStr)) {
            return StatisticsPatternMode.Loop;
        }
        else if("continue".equals(modeStr)) {
            return StatisticsPatternMode.Continue;
        }
        else if("stop".equals(modeStr)) {
            return StatisticsPatternMode.Stop;
        }
        throw new RuntimeException("An unknown statistics pattern mode found: " + modeStr);
    }

    private static MockAutoscalingFactor convertAutoscalingFactor(String factorStr) {
        if("memory-consumption".equals(factorStr)) {
            return MockAutoscalingFactor.MemoryConsumption;
        }
        else if("load-average".equals(factorStr)) {
            return MockAutoscalingFactor.LoadAverage;
        }
        else if("request-in-flight".equals(factorStr)) {
            return MockAutoscalingFactor.RequestInFlight;
        }
        throw new RuntimeException("An unknown autoscaling factor found: " + factorStr);
    }

    private static List<Integer> convertStringArrayToIntegerList(String[] stringArray) {
        List<Integer> integerList = new ArrayList<Integer>();
        for (String value : stringArray) {
            integerList.add(Integer.parseInt(value));
        }
        return integerList;
    }
}
