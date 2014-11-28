package org.apache.stratos.autoscaler.exception.partition;
/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
*/


/**
 *
 */
public class PartitionValidationException extends Exception {

	private static final long serialVersionUID = -3904452358279522141L;
	private String message;

	public PartitionValidationException(String message, Exception exception){
        super(message, exception);
        this.setMessage(message);
    }

	public PartitionValidationException(String msg) {
		super(msg);
		this.message = msg;
	}
    public PartitionValidationException(Exception exception){
        super(exception);
    }


	public String getMessage() {
		return message;
	}


	public void setMessage(String message) {
		this.message = message;
	}
}
