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

package org.apache.stratos.cartridge.agent.data.publisher.log;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cartridge.agent.data.publisher.DataContext;
import org.apache.stratos.cartridge.agent.data.publisher.DataPublisherConfiguration;
import org.wso2.carbon.databridge.commons.StreamDefinition;

import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class FileBasedLogPublisher extends LogPublisher {

    private static final Log log = LogFactory.getLog(FileBasedLogPublisher.class);
    private String memberIp;
    private String memberId;

    public FileBasedLogPublisher(DataPublisherConfiguration dataPublisherConfig, StreamDefinition streamDefinition, String memberIp, String memberId) {
        super(dataPublisherConfig, streamDefinition);
        this.memberIp = memberIp;
        this.memberId = memberId;
    }

    public void tailFileAndPublishLogs (String filePath) {
        ExecutorService executorService = Executors.newSingleThreadExecutor(new FileBasedLogPublisherTaskThreadFactory(filePath));
        executorService.submit(new FileBasedLogPublisherTask(filePath, this, memberId, memberIp));
    }

    private class FileBasedLogPublisherTask implements Runnable {

        private String memberIp;
        private String memberId;
        private String filePath;
        private FileBasedLogPublisher fileBasedLogPublisher;

        public FileBasedLogPublisherTask (String filePath, FileBasedLogPublisher fileBasedLogPublisher, String memberId, String memberIp) {

            this.filePath = filePath;
            this.memberId = memberId;
            this.memberIp = memberIp;
            this.fileBasedLogPublisher = fileBasedLogPublisher;
        }

        @Override
        public void run() {

            Runtime r = Runtime.getRuntime();
            Process p;
            try {
                p = r.exec("tail -F " + filePath);

            } catch (IOException e) {
                log.error("Error tailing file ", e);
                throw new RuntimeException(e);
            }

            Scanner s = new Scanner(p.getInputStream());
            while (s.hasNextLine()) {
                DataContext dataContext = new DataContext();
                dataContext.setCorrelationData(null);
                dataContext.setMetaData(new Object[] {memberIp, memberId});
                dataContext.setPayloadData(new Object[] {s.nextLine()});
                fileBasedLogPublisher.publish(dataContext);
            }
        }
    }

    class FileBasedLogPublisherTaskThreadFactory implements ThreadFactory {

        private String filePath;

        public FileBasedLogPublisherTaskThreadFactory (String filePath) {
            this.filePath = filePath;
        }

        public Thread newThread(Runnable r) {
            return new Thread(r, "File based log publisher thread  - " + filePath);
        }
    }
}
