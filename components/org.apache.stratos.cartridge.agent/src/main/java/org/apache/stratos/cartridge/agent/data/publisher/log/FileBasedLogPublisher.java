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

public class FileBasedLogPublisher extends LogPublisher implements Runnable {

    private static final Log log = LogFactory.getLog(FileBasedLogPublisher.class);
    private ExecutorService executorService;
    private Process process;
    private Scanner scanner;

    public FileBasedLogPublisher(DataPublisherConfiguration dataPublisherConfig, StreamDefinition streamDefinition, String filePath, String memberId) {

        super(dataPublisherConfig, streamDefinition, filePath, memberId);
        this.executorService = Executors.newSingleThreadExecutor(new FileBasedLogPublisherTaskThreadFactory(filePath));
    }

    public void start () {
        executorService.submit(this);
    }

    public void stop () {

        // close the resources
        try {
            process.getInputStream().close();

        } catch (IOException e) {
            log.error("Error in closing [tail -F] input stream", e);
        }
        scanner.close();
        process.destroy();

        executorService.shutdownNow();
        terminate();

        log.info("Terminated log publisher for file: " + filePath);
    }

    @Override
    public void run() {

        Runtime r = Runtime.getRuntime();
        try {
            process = r.exec(Constants.TAIL_COMMAND + filePath);

        } catch (IOException e) {
            log.error("Error tailing file ", e);
            throw new RuntimeException(e);
        }

        log.info("Starting log publisher for file: " + filePath + ", thread: " + Thread.currentThread().getName());

        scanner = new Scanner(process.getInputStream());
        while (scanner.hasNextLine()) {

            DataContext dataContext = new DataContext();
            // set the relevant data
            dataContext.setCorrelationData(null);
            dataContext.setMetaData(new Object[] {memberId});
            dataContext.setPayloadData(new Object[] {scanner.nextLine()});
            // publish data
            publish(dataContext);
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
