@echo off

rem ---------------------------------------------------------------------------
rem  Licensed to the Apache Software Foundation (ASF) under one
rem  or more contributor license agreements.  See the NOTICE file
rem  distributed with this work for additional information
rem  regarding copyright ownership.  The ASF licenses this file
rem  to you under the Apache License, Version 2.0 (the
rem  "License"); you may not use this file except in compliance
rem  with the License.  You may obtain a copy of the License at
rem
rem      http://www.apache.org/licenses/LICENSE-2.0
rem
rem  Unless required by applicable law or agreed to in writing,
rem  software distributed under the License is distributed on an
rem  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
rem  KIND, either express or implied.  See the License for the
rem  specific language governing permissions and limitations
rem  under the License.
rem ---------------------------------------------------------------------------
rem  Main Script for Apache Stratos CLI
rem
rem  Environment Variable Prequisites
rem
rem   JAVA_HOME          Java home path
rem
rem   STRATOS_URL        The URL of the Stratos Controller
rem ---------------------------------------------------------------------------

:checkStratoURL
if "%STRATOS_URL%" == "" goto noStratosURL
goto checkJava

:noStratosURL
echo "You must set the STRATOS_URL variable before running the CLI"
goto end

:checkJava
if "%JAVA_HOME%" == "" goto noJavaHome
if not exist "%JAVA_HOME%\bin\java.exe" goto noJavaHome
goto checkServer

:noJavaHome
echo "You must set the JAVA_HOME variable before running CLI."
goto end

:checkServer
rem %~sdp0 is expanded pathname of the current script under NT with spaces in the path removed
if "%CARBON_HOME%"=="" set CARBON_HOME=%~sdp0..
SET curDrive=%cd:~0,1%
SET wsasDrive=%CARBON_HOME:~0,1%
if not "%curDrive%" == "%wsasDrive%" %wsasDrive%:

rem ----- update classpath -----------------------------------------------------
:updateClasspath

setlocal EnableDelayedExpansion
cd %CARBON_HOME%
set CARBON_CLASSPATH=
FOR %%C in ("%CARBON_HOME%\lib\*.jar") DO set CARBON_CLASSPATH=!CARBON_CLASSPATH!;".\lib\%%~nC%%~xC"

set CARBON_CLASSPATH="%JAVA_HOME%\lib\tools.jar";%CARBON_CLASSPATH%;

set CMD=RUN %*

rem ----------------- Execute The Requested Command ----------------------------

:runServer
cd %CARBON_HOME%
set JAVA_ENDORSED="%JAVA_HOME%jre\lib\endorsed";"%JAVA_HOME%lib\endorsed"

:runJava
"%JAVA_HOME%\bin\java" -cp %CARBON_CLASSPATH% org.apache.stratos.cli.Main

:end
goto endlocal

:endlocal
:END

