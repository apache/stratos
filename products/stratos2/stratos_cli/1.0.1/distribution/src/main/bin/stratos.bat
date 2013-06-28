@echo off
REM ---------------------------------------------------------------------------
REM        Copyright 2013 WSO2, Inc. http://www.wso2.org
REM
REM  Licensed under the Apache License, Version 2.0 (the "License");
REM  you may not use this file except in compliance with the License.
REM  You may obtain a copy of the License at
REM
REM      http://www.apache.org/licenses/LICENSE-2.0
REM
REM  Unless required by applicable law or agreed to in writing, software
REM  distributed under the License is distributed on an "AS IS" BASIS,
REM  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
REM  See the License for the specific language governing permissions and
REM  limitations under the License.

rem ---------------------------------------------------------------------------
rem Main Script for WSO2 Carbon
rem
rem Environment Variable Prequisites
rem
rem   STRATOS_CLI_HOME Home of Stratos CLI Tool
rem
rem   STRATOS_URL      The URL of the Stratos Controller
rem ---------------------------------------------------------------------------

rem ----- Only set STRATOS_CLI_HOME if not already set ----------------------------

if "%STRATOS_CLI_HOME%"=="" set STRATOS_CLI_HOME=%CD%

cd %STRATOS_CLI_HOME%

java -jar "org.wso2.carbon.adc.mgt.cli-2.1.3-Tool.jar" %*

