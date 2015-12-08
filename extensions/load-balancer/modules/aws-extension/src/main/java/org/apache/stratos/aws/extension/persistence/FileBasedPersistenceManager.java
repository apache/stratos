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

package org.apache.stratos.aws.extension.persistence;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.aws.extension.exception.PersistenceException;
import org.apache.stratos.aws.extension.persistence.dao.LBInfoDAO;
import org.apache.stratos.aws.extension.persistence.dto.LBInfoDTO;

import java.io.*;
import java.util.Set;

public class FileBasedPersistenceManager implements PersistenceManager {

    private static final Log log = LogFactory.getLog(FileBasedPersistenceManager.class);
    private String lbInfoFilePath = null;

    public FileBasedPersistenceManager () {
        lbInfoFilePath = System.getProperty("lb.info.file");
        if (lbInfoFilePath == null || lbInfoFilePath.isEmpty()) {
            throw new RuntimeException("required system property lb.info.file not found");
        }
    }

    @Override
    public synchronized void persist (LBInfoDTO lbInfoDTO) throws PersistenceException {

        LBInfoDAO retrievedLbInfoDAO = retrieveLBInfo();
        if (retrievedLbInfoDAO == null) {
            retrievedLbInfoDAO = new LBInfoDAO();
        }

        retrievedLbInfoDAO.add(lbInfoDTO);
        persistLBInfo(retrievedLbInfoDAO);
    }

    @Override
    public synchronized Set<LBInfoDTO> retrieve () throws PersistenceException {
        return retrieveLBInfo() != null && retrieveLBInfo().get() != null ? retrieveLBInfo().get() : null;
    }

    @Override
    public synchronized void remove(LBInfoDTO lbInfoDTO) throws PersistenceException {

        LBInfoDAO retrievedLbInfoDAO = retrieveLBInfo();
        if (retrievedLbInfoDAO != null) {
            retrievedLbInfoDAO.remove(lbInfoDTO);
            persistLBInfo(retrievedLbInfoDAO);
        } else {
            log.info("No persisted LB Information found, hence unable to remove information of LB: " + lbInfoDTO.getName());
        }
    }

    @Override
    public synchronized void clear () throws PersistenceException {

        LBInfoDAO retrievedLbInfoDAO = retrieveLBInfo();
        if (retrievedLbInfoDAO == null) {
            retrievedLbInfoDAO = new LBInfoDAO();
        }

        retrievedLbInfoDAO.clear();
        persistLBInfo(retrievedLbInfoDAO);
    }

    private void persistLBInfo (LBInfoDAO lbInfoDAO) throws PersistenceException {

        FileOutputStream fileOutStream = null;
        ObjectOutputStream ObjOutStream = null;
        try {
            fileOutStream = new FileOutputStream(lbInfoFilePath);
            ObjOutStream = new ObjectOutputStream(fileOutStream);
            ObjOutStream.writeObject(lbInfoDAO);

        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new PersistenceException(e.getMessage(), e);

        } finally {
            try {
                if (ObjOutStream != null) {
                    ObjOutStream.close();
                }
                if (fileOutStream != null) {
                    fileOutStream.close();
                }

            } catch (IOException e) {
                log.error(e.getMessage(), e);
                if (fileOutStream != null) {
                    try {
                        fileOutStream.close();
                    } catch (IOException e1) {}
                }
                throw new PersistenceException(e.getMessage(), e);
            }

        }
    }

    private LBInfoDAO retrieveLBInfo () throws PersistenceException {

        FileInputStream fileInStream = null;
        ObjectInputStream objInStream = null;

        try {
            fileInStream = new FileInputStream(lbInfoFilePath);
            objInStream = new ObjectInputStream(fileInStream);
            return (LBInfoDAO) objInStream.readObject();

        } catch (FileNotFoundException e) {
            log.warn("File lbinformation.ser not found, any previously persisted LB information will not be reflected");
            return null;

        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new PersistenceException(e.getMessage(), e);

        } catch (ClassNotFoundException e) {
            log.error(e.getMessage(), e);
            throw new PersistenceException(e.getMessage(), e);

        } finally {
            try {
                if (objInStream != null) {
                    objInStream.close();
                }
                if (fileInStream != null) {
                    fileInStream.close();
                }

            } catch (IOException e) {
                log.error(e.getMessage(), e);
                if (fileInStream != null) {
                    try {
                        fileInStream.close();
                    } catch (IOException e1) {}
                }
                throw new PersistenceException(e.getMessage(), e);
            }
        }
    }
}
