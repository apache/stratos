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

import java.io.IOException;

/**
 * Health statistics reader interface.
 */
public interface IHealthStatisticsReader {

    /**
     * Called exactly once, before any other method.
     *
     * Should be used, along with {@link #delete()}, to manage "unmanaged"
     * resources, e.g. sockets. Standard objects will of course be managed by
     * GC in the usual way and should be allocated in the ctor.
     */
    public boolean init();

    /**
     * Called repeatedly to obtain memory and processor use.
     *
     * Obtained from the cartridge agent hosti.
     *
     * Can throw IOException if the required metrics were not obtainable.
     */
    public CartridgeStatistics getCartridgeStatistics() throws IOException;

    /**
     * Called exactly once, after all other methods.
     *
     * Should be used, along with {@link #init()}, to manage "unmanaged"
     * resources, e.g. sockets. Standard objects will of course be managed by
     * GC in the usual way and will be collected when no longer referenced.
     */
    public void delete();
}
