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
    <xsl:template match="rss/channel">
        <html>
            <head>
                <title>
                    <xsl:value-of select="title"/>
                </title>
                <style media="all" lang="en" type="text/css">
                    :root:before, :root:before {
                    font: 80% "Lucida Grande", Verdana, Lucida, Helvetica, Arial, sans-serif;
                    font-size:70%;
                    content: "This data file is meant to be read in a XML feed reader. See document
                    source."
                    }
                    .ChannelTitle
                    {
                    display: block;
                    font-size:200%;
                    font-weight:bolder;
                    color:#436976;
                    text-decoration:none;
                    border-bottom: 20px solid #dee7ec;
                    }
                    .ArticleEntry
                    {
                    border-width: 2px;
                    border-color: #336699;
                    border-style: solid;
                    width: 500px;
                    }
                    .ArticleTitle
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
                    .ArticleTitle A:visited
                    {
                    color: #FFFFFF;
                    text-decoration: underline;
                    }
                    .ArticleTitle A:link
                    {
                    color: #FFFFFF;
                    text-decoration: underline;
                    }
                    .ArticleTitle A:hover
                    {
                    color: #FFFF00;
                    text-decoration: none;
                    }
                    .ArticleDescription
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
                <xsl:apply-templates select="title"/>
                <xsl:apply-templates select="item"/>
            </body>
        </html>
    </xsl:template>
    <xsl:template match="title">
        <div class="ChannelTitle">
            <xsl:value-of select="text()"/>
        </div>
        <br/>
    </xsl:template>
    <xsl:template match="item">
        <div class="ArticleEntry">
            <div class="ArticleTitle">
                <a href="{link}">
                    <xsl:value-of select="title"/>
                </a>
                <xsl:text>&#160;&#160;&#160;&#160;</xsl:text>
                <xsl:value-of select="pubDate"/>
            </div>
            <div class="ArticleDescription">
                <b>Description:</b>
                <xsl:value-of select="description"/>
            </div>
        </div>
        <br/>
    </xsl:template>
</xsl:stylesheet>
