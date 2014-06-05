/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jclouds.docker.compute.features.internal;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Iterables.getLast;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

import com.google.common.base.Splitter;

/**
 * @author Andrea Turli
 */
public class Archives {

   public static File tar(File baseDir, String archivePath) throws IOException {
      // Check that the directory is a directory, and get its contents
      checkArgument(baseDir.isDirectory(), "%s is not a directory", baseDir);
      File[] files = baseDir.listFiles();
      File tarFile = new File(archivePath);

      String token = getLast(Splitter.on("/").split(archivePath.substring(0, archivePath.lastIndexOf("/"))));

      byte[] buf = new byte[1024];
      int len;
      TarArchiveOutputStream tos = new TarArchiveOutputStream(new FileOutputStream(tarFile));
      tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
      for (File file : files) {
         TarArchiveEntry tarEntry = new TarArchiveEntry(file);
         tarEntry.setName("/" + getLast(Splitter.on(token).split(file.toString())));
         tos.putArchiveEntry(tarEntry);
         if (!file.isDirectory()) {
            FileInputStream fin = new FileInputStream(file);
            BufferedInputStream in = new BufferedInputStream(fin);
            while ((len = in.read(buf)) != -1) {
               tos.write(buf, 0, len);
            }
            in.close();
         }
         tos.closeArchiveEntry();
      }
      tos.close();
      return tarFile;
   }

   public static File tar(File baseDir, File tarFile) throws IOException {
      // Check that the directory is a directory, and get its contents
      checkArgument(baseDir.isDirectory(), "%s is not a directory", baseDir);
      File[] files = baseDir.listFiles();

      String token = getLast(Splitter.on("/").split(baseDir.getAbsolutePath()));

      byte[] buf = new byte[1024];
      int len;
      TarArchiveOutputStream tos = new TarArchiveOutputStream(new FileOutputStream(tarFile));
      tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
      for (File file : files) {
         TarArchiveEntry tarEntry = new TarArchiveEntry(file);
         tarEntry.setName("/" + getLast(Splitter.on(token).split(file.toString())));
         tos.putArchiveEntry(tarEntry);
         if (!file.isDirectory()) {
            FileInputStream fin = new FileInputStream(file);
            BufferedInputStream in = new BufferedInputStream(fin);
            while ((len = in.read(buf)) != -1) {
               tos.write(buf, 0, len);
            }
            in.close();
         }
         tos.closeArchiveEntry();
      }
      tos.close();
      return tarFile;
   }

}
