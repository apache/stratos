<?xml version="1.0" encoding="utf-8"?>
<!--
  ~ Copyright 2005-2007 WSO2, Inc. (http://wso2.com)
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~ http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  -->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="html"/>

    <xsl:template match="ns1:feed" xmlns:ns1="http://www.w3.org/2005/Atom">
        <html>
            <head>
                <title>
                    <xsl:value-of select="ns2:title" xmlns:ns2="http://www.w3.org/2005/Atom"/>
                </title>
                <style media="all" lang="en" type="text/css">
                    :root:before, :root:before {
                    font: 80% "Lucida Grande", Verdana, Lucida, Helvetica, Arial, sans-serif;
                    font-size:70%;
                    content: "This data file is meant to be read in a XML Atom reader. See document
                    source."
                    }
                    .AtomTitle
                    {
                    display: block;
                    font-size:200%;
                    font-weight:bolder;
                    color:#436976;
                    text-decoration:none;
                    border-bottom: 20px solid #dee7ec;
                    }
                    .Entry
                    {
                    border-width: 2px;
                    border-color: #336699;
                    border-style: solid;
                    width: 500px;
                    }
                    .Title
                    {
                    background-color: #436976;
                    color: #FFFFFF;
                    font-size: 1.4em;
                    font-family: Verdana;
                    font-size: 9pt;
                    font-weight: bold;
                    padding-left: 5px;
                    padding-top: 5px;
                    padding-bottom: 5px;
                    }
                    .Title A:visited
                    {
                    color: #FFFFFF;
                    text-decoration: underline;
                    }
                    .Title A:link
                    {
                    color: #FFFFFF;
                    text-decoration: underline;
                    }
                    .Title A:hover
                    {
                    color: #FFFF00;
                    text-decoration: none;
                    }
                    .Summary
                    {
                    color: #000000;
                    font-family: Verdana;
                    font-size: 9pt;
                    padding-left: 5px;
                    padding-top: 5px;
                    padding-bottom: 5px;
                    padding-right: 5px;
                    }
                </style>
            </head>
            <body>
                <div class="AtomTitle">
                    <xsl:value-of select="ns2:title" xmlns:ns2="http://www.w3.org/2005/Atom"/>
                </div>
                <br/>

                <xsl:for-each select="ns2:entry" xmlns:ns2="http://www.w3.org/2005/Atom">

                    <div class="Entry">
                        <div class="Title">
                            <a>
                                <xsl:attribute name="href">
                                    <xsl:value-of select="ns2:link/@href"
                                                  xmlns:ns2="http://www.w3.org/2005/Atom"/>
                                </xsl:attribute>
                                <xsl:value-of select="ns2:title"
                                              xmlns:ns2="http://www.w3.org/2005/Atom"/>
                            </a>
                            <xsl:text>&#160;&#160;&#160;&#160;</xsl:text>
                            <xsl:value-of select="ns2:updated"
                                          xmlns:ns2="http://www.w3.org/2005/Atom"/>
                        </div>
                        <div class="Summary">
                            <xsl:choose>
                                <xsl:when test="ns2:summary"
                                          xmlns:ns2="http://www.w3.org/2005/Atom">
                                    <b>Summary:</b>
                                    <xsl:value-of select="ns2:summary"
                                                  xmlns:ns2="http://www.w3.org/2005/Atom"/>
                                </xsl:when>
                            </xsl:choose>

                        </div>
                    </div>
                    <br/>

                </xsl:for-each>
            </body>
        </html>
    </xsl:template>

</xsl:stylesheet>
