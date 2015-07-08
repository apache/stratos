/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.ui.transports.fileupload;

/**
 * This exception will be thrown when the maximum allowed file size limit is exceeded. This can
 * be applicable to a single file or a collection of files.
 */
public class FileSizeLimitExceededException extends Exception {

    public FileSizeLimitExceededException(long fileSizeLimitInMB) {
        super("File size limit of " + fileSizeLimitInMB + " MB has been exceeded");
    }

    public FileSizeLimitExceededException(String msg, long fileSizeLimitInMB) {
        super("File size limit of " + fileSizeLimitInMB + " MB has been exceeded. " + msg);
    }

    public FileSizeLimitExceededException(String msg, long fileSizeLimitInMB, Throwable throwable) {
        super("File size limit of " + fileSizeLimitInMB + " MB has been exceeded. " + msg, throwable);
    }

    public FileSizeLimitExceededException(Throwable throwable, long fileSizeLimitInMB) {
        super("File size limit of " + fileSizeLimitInMB + " MB has been exceeded", throwable);
    }
}
