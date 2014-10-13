/**
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at

 *  http://www.apache.org/licenses/LICENSE-2.0

 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.stratos.cli.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.stratos.cli.exception.ExceptionMapper;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.SocketException;
import java.text.MessageFormat;
import java.util.ResourceBundle;

public class CliUtils {
    private static Log log = LogFactory.getLog(CliUtils.class);

    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("Resources");

	public static <T> void printTable(T[] data, RowMapper<T> mapper, String... headers) {
		if (data == null) {
			return;
		}
		// The maximum number of columns
		// All data String[] length must be equal to this
		int columns = headers.length;
		int rows = data.length + 1;

		String[][] table = new String[rows][columns];
		table[0] = headers;

		for (int i = 0; i < data.length; i++) {
			T t = data[i];
			table[i + 1] = mapper.getData(t);
		}

		// Find the maximum length of a string in each column
		int[] lengths = new int[columns];
		for (int i = 0; i < table.length; i++) {
			for (int j = 0; j < table[i].length; j++) {
				lengths[j] = Math.max(table[i][j].length(), lengths[j]);
			}
		}

		// The border rows
		String borders[] = new String[lengths.length];
		// Generate a format string for each column
		String[] formats = new String[lengths.length];
		for (int i = 0; i < lengths.length; i++) {
			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append("+");
			for (int j = 0; j < lengths[i] + 2; j++) {
				stringBuilder.append("-");
			}
			boolean finalColumn = (i + 1 == lengths.length);
			if (finalColumn) {
				stringBuilder.append("+\n");
			}
			borders[i] = stringBuilder.toString();
			formats[i] = "| %1$-" + lengths[i] + "s " + (finalColumn ? "|\n" : "");
		}

		// Print the table
		for (int i = 0; i < table.length; i++) {
			for (int j = 0; j < table[i].length; j++) {
				System.out.print(borders[j]);
			}
			for (int j = 0; j < table[i].length; j++) {
				System.out.format(formats[j], table[i][j]);
			}
			if (i + 1 == table.length) {
				for (int j = 0; j < table[i].length; j++) {
					System.out.print(borders[j]);
				}
			}
		}
	}
	
	public static String getMessage(String key, Object... args) {
		String message = BUNDLE.getString(key);
		if (args != null && args.length > 0) {
			message = MessageFormat.format(message, args);
		}
		return message;
	}

    public static String readResource(String fileName) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(fileName));
        try {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append("\n");
                line = br.readLine();
            }
            return sb.toString();
        } finally {
            br.close();
        }
    }

    /**
     * Extract HTTP response body as a string
     * @param response
     * @return
     */
    public static String getHttpResponseString (HttpResponse response) {
        try {
            String output;
            String result = "";

            if((response != null) && (response.getEntity() != null) && (response.getEntity().getContent() != null)) {
                BufferedReader reader = new BufferedReader(new InputStreamReader((response.getEntity().getContent())));
                while ((output = reader.readLine()) != null) {
                    result += output;
                }
            }
            return result;
        } catch (SocketException e) {
            String message = "A connection error occurred while reading response message: " + e.getMessage();
            System.out.println(message);
            log.error(message, e);
            return null;
        } catch (IOException e) {
            String message = "An IO error occurred while reading response message: " + e.getMessage();
            System.out.println(message);
            log.error(message, e);
            return null;
        } catch(Exception e) {
            String message = "An unknown error occurred while reading response message: " + e.getMessage();
            System.out.println(message);
            log.error(message, e);
            return null;
        }
    }

    public static void printError(HttpResponse response) {
        String resultString = CliUtils.getHttpResponseString(response);
        if (StringUtils.isNotBlank(resultString)) {
            // Response body found, try to extract exception information
            boolean exceptionMapperInstanceFound = false;
            try {
                GsonBuilder gsonBuilder = new GsonBuilder();
                Gson gson = gsonBuilder.create();
                ExceptionMapper exception = gson.fromJson(resultString, ExceptionMapper.class);
                if (exception != null) {
                    System.out.println(exception);
                    exceptionMapperInstanceFound = true;
                }
            } catch (Exception ignore) {
                // Could not find an ExceptionMapper instance
            } finally {
                if (!exceptionMapperInstanceFound) {
                    System.out.println(response.getStatusLine().toString());
                }
            }
        } else {
            // No response body found
            System.out.println(response.getStatusLine().toString());
        }
    }
}
