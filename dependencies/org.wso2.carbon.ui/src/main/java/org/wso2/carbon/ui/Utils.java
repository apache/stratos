/*
 * Copyright 2004,2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.carbon.ui;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonException;
import org.wso2.carbon.base.ServerConfiguration;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 *
 */
public class Utils {

    private static Log log = LogFactory.getLog(Utils.class);

    public static void transform(InputStream xmlStream, InputStream xslStream,
                                 OutputStream outputStream) throws TransformerException {
        Source xmlStreamSource = new StreamSource(xmlStream);
        Source xslStreamSource = new StreamSource(xslStream);
        Result result = new StreamResult(outputStream);
        Transformer transformer = TransformerFactory.newInstance().newTransformer(xslStreamSource);
        transformer.transform(xmlStreamSource, result);
    }

    public static void copyDirectory(File sourceLocation, File targetLocation) throws IOException {

        if (sourceLocation.isDirectory()) {
            if (!targetLocation.exists()) {
                targetLocation.mkdir();
            }

            String[] children = sourceLocation.list();
            for (String aChildren : children) {
                copyDirectory(new File(sourceLocation, aChildren),
                        new File(targetLocation, aChildren));
            }
        } else {
            InputStream in = new FileInputStream(sourceLocation);
            OutputStream out = new FileOutputStream(targetLocation);

            // Copy the bits from instream to outstream
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        }
    }


    /**
     * For a given Zip file, process each entry.
     *
     * @param zipFileLocation zipFileLocation
     * @param targetLocation  targetLocation
     * @throws org.wso2.carbon.core.CarbonException
     *          CarbonException
     */
    public static void deployZipFile(File zipFileLocation, File targetLocation)
            throws CarbonException {
        try {
            SortedSet<String> dirsMade = new TreeSet<String>();
            JarFile jarFile = new JarFile(zipFileLocation);
            Enumeration all = jarFile.entries();
            while (all.hasMoreElements()) {
                getFile((ZipEntry) all.nextElement(), jarFile, targetLocation, dirsMade);
            }
        } catch (IOException e) {
            log.error("Error while copying component", e);
            throw new CarbonException(e);
        }
    }

    /**
     * Process one file from the zip, given its name.
     * Either print the name, or create the file on disk.
     *
     * @param e              zip entry
     * @param zippy          jarfile
     * @param targetLocation target
     * @param dirsMade       dir
     * @throws java.io.IOException will be thrown
     */
    private static void getFile(ZipEntry e, JarFile zippy, File targetLocation,
                                SortedSet<String> dirsMade)
            throws IOException {
        byte[] b = new byte[1024];
        String zipName = e.getName();

        if (zipName.startsWith("/")) {
            zipName = zipName.substring(1);
        }
        //Process only fliles that start with "ui"
        if (!zipName.startsWith("ui")) {
            return;
        }
        // Strip off the ui bit
        zipName = zipName.substring(2);
        // if a directory, just return. We mkdir for every file,
        // since some widely-used Zip creators don't put out
        // any directory entries, or put them in the wrong place.
        if (zipName.endsWith("/")) {
            return;
        }
        // Else must be a file; open the file for output
        // Get the directory part.
        int ix = zipName.lastIndexOf('/');
        if (ix > 0) {
            String dirName = zipName.substring(0, ix);
            if (!dirsMade.contains(dirName)) {
                File d = new File(targetLocation, dirName);
                // If it already exists as a dir, don't do anything
                if (!(d.exists() && d.isDirectory())) {
                    // Try to create the directory, warn if it fails
                    if (log.isDebugEnabled()) {
                        log.debug("Deploying Directory: " + dirName);
                    }
                    if (!d.mkdirs()) {
                        log.warn("Warning: unable to mkdir " + dirName);
                    }
                    dirsMade.add(dirName);
                }
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Deploying " + zipName);
        }
        File file = new File(targetLocation, zipName);
        FileOutputStream os = new FileOutputStream(file);
        InputStream is = zippy.getInputStream(e);
        int n;
        while ((n = is.read(b)) > 0) {
            os.write(b, 0, n);
        }
        is.close();
        os.close();
    }

    /**
     * Deletes all files and subdirectories under dir.
     * Returns true if all deletions were successful.
     * If a deletion fails, the method stops attempting to delete and returns false.
     *
     * @param dir directory to delete
     * @return status
     */
    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        // The directory is now empty so delete it
        return dir.delete();
    }

    /**
     * Read context name from carbon.xml
     * "carbon" will be the default value
     *
     * @return webcontext name
     */
    public static String getWebContextName() {
        String webContext = "carbon";
        ServerConfiguration sc = ServerConfiguration.getInstance();
        if (sc != null) {
            String value = sc.getFirstProperty("WebContext");
            if (value != null) {
                webContext = value;
            }
        }
        return webContext;
    }

}
