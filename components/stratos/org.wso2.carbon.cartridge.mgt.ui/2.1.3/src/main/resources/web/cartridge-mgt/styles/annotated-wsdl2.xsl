<?xml version="1.0"?>
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

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
                xmlns:wsdl="http://www.w3.org/ns/wsdl"
                xmlns:wsoap="http://www.w3.org/ns/wsdl/soap"
                xmlns:whttp="http://www.w3.org/ns/wsdl/http"
                xmlns:wrpc="http://www.w3.org/ns/wsdl/rpc"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                exclude-result-prefixes="wsdl xs wsoap">
    <!--
         Created by: Jonathan Marsh <jonathan@wso2.com>
         8 November 2006
     -->
    <xsl:import href="annotated-xsd.xsl"/>

    <xsl:output method="html" indent="yes" encoding="UTF-8"/>

    <!--
         Many schemas refer to the built-in schema types.  In order to navigate to those types, one needs
         to know where to load the schema for schemas, hopefully a local version with this stylesheet
         applied so the user can continue to navigate.
     -->
    <xsl:param name="xsd-schema-location" select="'/service-mgt/styles/XMLSchema.xsd'"/>

    <!-- QName resolving functions require the targetNamespace, so stuff it in a global variable. -->
    <xsl:variable name="targetNamespace" select="wsdl:description/@targetNamespace"/>
    <xsl:variable name="schemaTargetNamespace"
                  select="wsdl:description/wsdl:types/xs:schema/@targetNamespace"/>

    <!-- Mozilla doesn't support the namespace axis, which makes simulating namespace declarations
     problematic.  At least we can try alternate reconstruction methods if we know the functionality
     isn't there.  -->
    <xsl:variable name="supports-namespace-axis" select="count(/*/namespace::*) &gt; 0"/>

    <!-- ===  Main  ========================================
         Main template for the wsdl document
     -->
    <xsl:template match="/">
        <html>
            <head>
                <title>WSDL 2.0 for
                    <xsl:value-of select="wsdl:description/@targetNamespace"/>
                </title>
                <style type="text/css">
                    <xsl:call-template name="css"/>
                    <![CDATA[
.annotation-area {width:19em}
.schema-block {background-color:#EEE;}
.schema {padding-left:7em; text-indent:-5em}
.schema-top-level {padding-left: 8em; text-indent:-5em}
.wsdl-top-level {padding-left: 6em; text-indent:-5em}
.wsdl-second-level {padding-left: 7em; text-indent:-5em}
.wsdl-message {padding-left: 6em; text-indent:-5em}

.description {padding-left:5em; margin-bottom:1em; text-indent:-5em}
.operation   {background-color:rgb(215,206,221); text-align:right; border: 1px dashed black; padding:.5em} 
.fault       {background-color:rgb(240,206,206); text-align:right; border: 1px dashed black; padding:.5em} 
.interface   {background-color:rgb(185,218,192); text-align:right; border: 1px dashed black; padding:.5em} 
.binding     {background-color:rgb(218,208,185); text-align:right; border: 1px dashed black; padding:.5em} 
.service     {background-color:rgb(240,197,166); text-align:right; border: 1px dashed black; padding:.5em} 
]]>
                </style>
            </head>
            <body>
                <xsl:apply-templates select="wsdl:description"/>
                <p/>
                <hr/>
                <table cellpadding="0" cellspacing="0" id="index">
                    <xsl:if test="wsdl:description/wsdl:types/xs:schema">
                        <xsl:for-each select="wsdl:description/wsdl:types">
                            <xsl:call-template name="generate-xsd-index"/>
                        </xsl:for-each>
                    </xsl:if>
                    <xsl:call-template name="generate-wsdl-index"/>
                </table>
            </body>
        </html>
    </xsl:template>
    <!-- ===  Elements  ========================================
         The following templates format elements of various flavors
         (wsdl:description, children of wsdl:description, grandchildren etc. of wsdl:description,
         and extension elements
     -->
    <xsl:template match="wsdl:description">
        <table cellpadding="0" cellspacing="0">
            <tr>
                <td class="annotation-area">
                    <div class="sidebar">
                        <xsl:call-template name="index-reference"/>
                    </div>
                </td>
                <td>
                    <div class="arrow">&#160;</div>
                </td>
                <td class="source-area">
                    <div class="description">
                        <xsl:call-template name="element-start"/>
                    </div>
                </td>
            </tr>
            <xsl:apply-templates/>
            <tr>
                <td colspan="2" class="annotation-area"/>
                <td class="source-area">
                    <div class="description">
                        <xsl:call-template name="element-end"/>
                    </div>
                </td>
            </tr>
        </table>
    </xsl:template>
    <xsl:template match="wsdl:description/wsdl:*">
        <xsl:variable name="identifier">
            <xsl:if test="@name">
                <xsl:text>_</xsl:text>
                <xsl:value-of select="local-name()"/>
                <xsl:text>_</xsl:text>
                <xsl:value-of select="@name"/>
            </xsl:if>
        </xsl:variable>
        <tr>
            <xsl:choose>
                <xsl:when
                        test="not(self::wsdl:documentation or self::wsdl:import or self::wsdl:include)">
                    <td class="annotation-area">
                        <div class="sidebar {local-name()} sidebar-title">
                            <xsl:if test="$identifier">
                                <xsl:attribute name="id">
                                    <xsl:value-of select="$identifier"/>
                                </xsl:attribute>
                            </xsl:if>
                            <span class="sidebar-title-highlight">
                                <xsl:value-of select="@name"/>
                            </span>
                            <xsl:text></xsl:text>
                            <xsl:value-of select="local-name()"/>
                            <xsl:call-template name="wsdl-referenced-by"/>
                        </div>
                    </td>
                    <td>
                        <div class="arrow">&#160;</div>
                    </td>
                </xsl:when>
                <xsl:otherwise>
                    <td colspan="2" class="annotation-area"/>
                </xsl:otherwise>
            </xsl:choose>
            <td class="source-area">
                <div class="wsdl-top-level">
                    <xsl:call-template name="element-start"/>
                </div>
                <div class="wsdl-top-level">
                    <xsl:apply-templates/>
                </div>
                <div class="wsdl-top-level">
                    <xsl:call-template name="element-end"/>
                </div>
            </td>
        </tr>
    </xsl:template>
    <xsl:template match="wsdl:description/wsdl:interface">
        <tr>
            <td class="annotation-area">
                <div class="sidebar interface sidebar-title">
                    <a name="_interface_{@name}"></a>
                    <span class="sidebar-title-highlight">
                        <xsl:value-of select="@name"/>
                    </span>
                    <xsl:text></xsl:text>
                    <xsl:value-of select="local-name()"/>
                    <xsl:call-template name="wsdl-referenced-by"/>
                    <xsl:call-template name="interface-defines"/>
                </div>
            </td>
            <td>
                <div class="arrow">&#160;</div>
            </td>
            <td class="source-area">
                <div class="wsdl-top-level">
                    <xsl:call-template name="element-start"/>
                    <xsl:apply-templates select="wsdl:documentation"/>
                </div>
            </td>
        </tr>
        <xsl:apply-templates select="node()[not(self::wsdl:documentation)]"/>
        <tr>
            <td colspan="2" class="annotation-area"/>
            <td class="source-area">
                <div class="wsdl-top-level">
                    <xsl:call-template name="element-end"/>
                </div>
            </td>
        </tr>
    </xsl:template>
    <xsl:template match="wsdl:interface/wsdl:operation | wsdl:interface/wsdl:fault">
        <tr>
            <td class="annotation-area">
                <div class="sidebar {local-name()} sidebar-title">
                    <a name="_{local-name()}_{@name}"></a>
                    <span class="sidebar-title-highlight">
                        <xsl:value-of select="@name"/>
                    </span>
                    <xsl:text></xsl:text>
                    <xsl:value-of select="local-name()"/>
                    <xsl:call-template name="wsdl-referenced-by"/>
                </div>
            </td>
            <td>
                <div class="arrow">&#160;</div>
            </td>
            <td class="source-area">
                <div class="wsdl-second-level">
                    <xsl:call-template name="element-start"/>
                </div>
                <div class="wsdl-second-level">
                    <xsl:apply-templates/>
                </div>
                <div class="wsdl-second-level">
                    <xsl:call-template name="element-end"/>
                </div>
            </td>
        </tr>
    </xsl:template>
    <xsl:template match="wsdl:description/wsdl:types">
        <tr>
            <td colspan="2" class="annotation-area"/>
            <td class="source-area">
                <div class="wsdl-top-level">
                    <xsl:call-template name="element-start"/>
                </div>
            </td>
        </tr>
        <xsl:apply-templates/>
        <tr>
            <td colspan="2" class="annotation-area"/>
            <td class="source-area">
                <div class="wsdl-top-level">
                    <xsl:call-template name="element-end"/>
                </div>
            </td>
        </tr>
    </xsl:template>
    <xsl:template match="xs:schema | xs:import">
        <tr>
            <td colspan="2" class="schema-block annotation-area"/>
            <td class="schema-block source-area">
                <div class="schema">
                    <xsl:call-template name="element-start"/>
                </div>
            </td>
        </tr>
        <xsl:apply-templates/>
        <tr>
            <td colspan="2" class="schema-block annotation-area"/>
            <td class="schema-block source-area">
                <div class="schema">
                    <xsl:call-template name="element-end"/>
                </div>
            </td>
        </tr>
    </xsl:template>
    <xsl:template match="xs:*">
        <xsl:apply-imports/>
    </xsl:template>
    <xsl:template match="wsdl:* | wsoap:* | whttp:*">
        <div class="indent">
            <div>
                <xsl:call-template name="element-start"/>
            </div>
            <xsl:apply-templates/>
            <div>
                <xsl:call-template name="element-end"/>
            </div>
        </div>
    </xsl:template>

    <!-- ===  Attributes  =========================================
         The following templates format attributes of various flavors
     -->
    <xsl:template match="wsdl:binding/wsdl:operation/@name | wsdl:binding/wsdl:fault/@name"
                  priority="1">
        <xsl:call-template name="attribute">
            <xsl:with-param name="value-class">markup-name-attribute-value</xsl:with-param>
            <xsl:with-param name="reference">
                <xsl:call-template name="external-wsdl-reference"/>
                <xsl:text>#_</xsl:text>
                <xsl:value-of select="local-name(parent::*)"/>
                <xsl:text>_</xsl:text>
                <xsl:value-of select="."/>
            </xsl:with-param>
            <xsl:with-param name="native-attribute" select="true()"/>
        </xsl:call-template>
    </xsl:template>
    <xsl:template match="wsdl:*/@name">
        <xsl:call-template name="attribute">
            <xsl:with-param name="value-class">markup-name-attribute-value</xsl:with-param>
            <xsl:with-param name="native-attribute" select="true()"/>
        </xsl:call-template>
    </xsl:template>
    <xsl:template
            match="wsdl:input/@element[not(contains(.,'#'))] | wsdl:output/@element[not(contains(.,'#'))] | wsdl:fault/@element[not(contains(.,'#'))]">
        <xsl:call-template name="attribute">
            <xsl:with-param name="value-class">markup-name-attribute-value</xsl:with-param>
            <xsl:with-param name="reference">
                <xsl:call-template name="external-schema-reference"/>
                <xsl:text>#_</xsl:text>
                <xsl:value-of select="name()"/>
                <xsl:text>_</xsl:text>
                <xsl:value-of select="substring-after(.,':')"/>
            </xsl:with-param>
            <xsl:with-param name="native-attribute" select="true()"/>
        </xsl:call-template>
    </xsl:template>
    <xsl:template match="wsdl:infault/@ref | wsdl:outfault/@ref |wsdl:fault/@ref">
        <xsl:call-template name="attribute">
            <xsl:with-param name="value-class">markup-name-attribute-value</xsl:with-param>
            <xsl:with-param name="reference">
                <xsl:call-template name="external-schema-reference"/>
                <xsl:text>#_fault_</xsl:text>
                <xsl:value-of select="substring-after(.,':')"/>
            </xsl:with-param>
            <xsl:with-param name="native-attribute" select="true()"/>
        </xsl:call-template>
    </xsl:template>
    <xsl:template match="wsdl:operation/@ref">
        <xsl:call-template name="attribute">
            <xsl:with-param name="value-class">markup-name-attribute-value</xsl:with-param>
            <xsl:with-param name="reference">
                <xsl:call-template name="external-schema-reference"/>
                <xsl:text>#_operation_</xsl:text>
                <xsl:value-of select="substring-after(.,':')"/>
            </xsl:with-param>
            <xsl:with-param name="native-attribute" select="true()"/>
        </xsl:call-template>
    </xsl:template>
    <xsl:template match="wsdl:service/@interface | wsdl:interface/@extends">
        <xsl:call-template name="attribute">
            <xsl:with-param name="value-class">markup-name-attribute-value</xsl:with-param>
            <xsl:with-param name="reference">
                <xsl:call-template name="external-schema-reference"/>
                <xsl:text>#_interface_</xsl:text>
                <xsl:value-of select="substring-after(.,':')"/>
            </xsl:with-param>
            <xsl:with-param name="native-attribute" select="true()"/>
        </xsl:call-template>
    </xsl:template>
    <xsl:template match="wsdl:endpoint/@binding">
        <xsl:call-template name="attribute">
            <xsl:with-param name="value-class">markup-name-attribute-value</xsl:with-param>
            <xsl:with-param name="reference">
                <xsl:call-template name="external-schema-reference"/>
                <xsl:text>#_binding_</xsl:text>
                <xsl:value-of select="substring-after(.,':')"/>
            </xsl:with-param>
            <xsl:with-param name="native-attribute" select="true()"/>
        </xsl:call-template>
    </xsl:template>
    <xsl:template match="wsdl:*/@* | wsoap:*/@* | whttp:*/@*" priority="0">
        <xsl:call-template name="attribute">
            <xsl:with-param name="native-attribute" select="true()"/>
        </xsl:call-template>
    </xsl:template>

    <!-- ===  Comments  ========================================
         The following template formats comment nodes
     -->
    <xsl:template match="wsdl:description/comment()">
        <tr>
            <td colspan="2" class="annotation-area"/>
            <td class="source-area">
                <div class="schema-top-level markup-comment">
                    <xsl:text>&lt;!--</xsl:text>
                    <xsl:value-of select="."/>
                    <xsl:text>--&gt;</xsl:text>
                </div>
            </td>
        </tr>
    </xsl:template>
    <xsl:template match="wsdl:interface/comment()">
        <div class="markup-comment double-indent">
            <xsl:text>&lt;!--</xsl:text>
            <xsl:value-of select="."/>
            <xsl:text>--&gt;</xsl:text>
        </div>
    </xsl:template>

    <!-- ===  Library templates  ========================================
         Library of useful named templates
     -->
    <xsl:template name="insert-wsdl-reference">
        <xsl:if test="parent::wsdl:operation">
            <a href="#_operation_{../@name}">
                <xsl:value-of select="../@name"/>
            </a>
            <xsl:text>operation of the</xsl:text>
        </xsl:if>
        <xsl:for-each select="ancestor-or-self::*[last() - 1]">
            <a href="#_{local-name(.)}_{@name}">
                <xsl:value-of select="@name"/>
            </a>
            <xsl:text></xsl:text>
            <xsl:value-of select="local-name(.)"/>
        </xsl:for-each>
    </xsl:template>
    <xsl:template name="wsdl-referenced-by">
        <xsl:variable name="target" select="@name"/>
        <xsl:if test="parent::wsdl:description">
            <div class="sidebar-text">
                <xsl:variable name="extended-by" select="//wsdl:*[substring-after(@element | @binding
					| @interface,':') = $target]"/>
                <xsl:if test="count($extended-by) > 0">
                    <div class="referenced-list">Referenced by:</div>
                    <ul>
                        <xsl:for-each select="$extended-by">
                            <xsl:sort select="@element | @binding | @interface"/>
                            <li class="referenced-item">
                                <xsl:call-template name="insert-wsdl-reference"/>
                            </li>
                        </xsl:for-each>
                    </ul>
                </xsl:if>
            </div>
        </xsl:if>
    </xsl:template>
    <xsl:template name="interface-defines">
        <xsl:variable name="target" select="@name"/>
        <div class="sidebar-text">
            <xsl:if test="wsdl:fault">
                <div class="referenced-list">Defines faults:</div>
                <ul>
                    <xsl:for-each select="wsdl:fault">
                        <xsl:sort select="@name"/>
                        <li class="referenced-item">
                            <a href="#_fault_{@name}">
                                <xsl:value-of select="@name"/>
                            </a>
                        </li>
                    </xsl:for-each>
                </ul>
            </xsl:if>
            <xsl:if test="wsdl:operation">
                <div class="referenced-list">Defines operations:</div>
                <ul>
                    <xsl:for-each select="wsdl:operation">
                        <xsl:sort select="@name"/>
                        <li class="referenced-item">
                            <a href="#_operation_{@name}">
                                <xsl:value-of select="@name"/>
                            </a>
                        </li>
                    </xsl:for-each>
                </ul>
            </xsl:if>
        </div>
    </xsl:template>
    <xsl:template name="additional-references">
        <xsl:param name="target"/>
        <xsl:variable name="referenced-by"
                      select="/wsdl:description/wsdl:interface/wsdl:operation/wsdl:*[substring-after(@element,':') = $target]"/>
        <xsl:if test="count($referenced-by) > 0">
            <div class="referenced-list">Referenced from WSDL by:</div>
            <ul>
                <xsl:for-each select="$referenced-by">
                    <xsl:sort select="@element"/>
                    <li class="referenced-item">
                        <xsl:call-template name="insert-wsdl-reference"/>
                    </li>
                </xsl:for-each>
            </ul>
        </xsl:if>
    </xsl:template>
    <xsl:template name="external-wsdl-reference">
        <xsl:choose>
            <xsl:when test="$supports-namespace-axis">
                <xsl:if test="not(../namespace::*[. = $targetNamespace and name(.) =
					substring-before(current(),':')])">
                    <xsl:variable name="prefix" select="substring-before(.,':')"/>
                    <xsl:variable name="uri" select="../namespace::*[name(.) = $prefix]"/>
                    <xsl:value-of
                            select="/wsdl:description/wsdl:import[@namespace=$uri]/@location"/>
                </xsl:if>
            </xsl:when>
            <xsl:otherwise>
                <xsl:choose>
                    <xsl:when
                            test="substring-before(current(),':')= 'xs' or substring-before(current(),':')= 'xsd'">
                        <xsl:value-of select="$xsd-schema-location"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <!-- the link is broken - Bad Mozilla -->
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    <xsl:template name="external-schema-reference">
        <xsl:choose>
            <xsl:when test="$supports-namespace-axis">
                <xsl:if test="not(../namespace::*[. = $schemaTargetNamespace and name(.) =
					substring-before(current(),':')])">
                    <xsl:variable name="prefix" select="substring-before(.,':')"/>
                    <xsl:variable name="uri" select="../namespace::*[name(.) = $prefix]"/>
                    <xsl:choose>
                        <xsl:when test="$uri = 'http://www.w3.org/2001/XMLSchema'">
                            <xsl:value-of select="$xsd-schema-location"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of
                                    select="/wsdl:description/wsdl:types/xs:import[@namespace=$uri]/@schemaLocation"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:if>
            </xsl:when>
            <xsl:otherwise>
                <xsl:choose>
                    <xsl:when
                            test="substring-before(current(),':')= 'xs' or substring-before(current(),':')= 'xsd'">
                        <xsl:value-of select="$xsd-schema-location"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <!-- the link is broken - Bad Mozilla -->
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    <xsl:template name="generate-wsdl-index">
        <xsl:if test="wsdl:description/wsdl:interface">
            <tr>
                <td id="interface-list" class="annotation-area">
                    <div class="interface sidebar sidebar-title-highlight">Index of interfaces</div>
                </td>
                <td>
                    <div class="arrow">&#160;</div>
                </td>
                <td class="source-area">
                    <xsl:for-each select="wsdl:description/wsdl:interface">
                        <xsl:sort select="@name"/>
                        <xsl:call-template name="componentListItem"/>
                    </xsl:for-each>
                </td>
            </tr>
        </xsl:if>
        <xsl:if test="wsdl:description/wsdl:interface/wsdl:fault">
            <tr>
                <td id="fault-list" class="annotation-area">
                    <div class="fault sidebar sidebar-title-highlight">Index of faults</div>
                </td>
                <td>
                    <div class="arrow">&#160;</div>
                </td>
                <td class="source-area">
                    <xsl:for-each select="wsdl:description/wsdl:interface/wsdl:fault">
                        <xsl:sort select="@name"/>
                        <xsl:call-template name="componentListItem"/>
                    </xsl:for-each>
                </td>
            </tr>
        </xsl:if>
        <xsl:if test="wsdl:description/wsdl:interface/wsdl:operation">
            <tr>
                <td id="operation-list" class="annotation-area">
                    <div class="operation sidebar sidebar-title-highlight">Index of operations</div>
                </td>
                <td>
                    <div class="arrow">&#160;</div>
                </td>
                <td class="source-area">
                    <xsl:for-each select="wsdl:description/wsdl:interface/wsdl:operation">
                        <xsl:sort select="@name"/>
                        <xsl:call-template name="componentListItem"/>
                    </xsl:for-each>
                </td>
            </tr>
        </xsl:if>
        <xsl:if test="wsdl:description/wsdl:binding">
            <tr>
                <td id="binding-list" class="annotation-area">
                    <div class="binding sidebar sidebar-title-highlight">Index of bindings</div>
                </td>
                <td>
                    <div class="arrow">&#160;</div>
                </td>
                <td class="source-area">
                    <xsl:for-each select="wsdl:description/wsdl:binding">
                        <xsl:sort select="@name"/>
                        <xsl:call-template name="componentListItem"/>
                    </xsl:for-each>
                </td>
            </tr>
        </xsl:if>
        <xsl:if test="wsdl:description/wsdl:service">
            <tr>
                <td id="service-list" class="annotation-area">
                    <div class="service sidebar sidebar-title-highlight">Index of services</div>
                </td>
                <td>
                    <div class="arrow">&#160;</div>
                </td>
                <td class="source-area">
                    <xsl:for-each select="wsdl:description/wsdl:service">
                        <xsl:sort select="@name"/>
                        <xsl:call-template name="componentListItem"/>
                    </xsl:for-each>
                </td>
            </tr>
        </xsl:if>
    </xsl:template>
    <xsl:template name="index-reference">
        <div class="note sidebar-text">See the
            <a href="#index">index</a>
            of
            <xsl:variable name="ss1">
                <xsl:if test="wsdl:types/xs:schema/xs:complexType">
                    <a href="#complexType-list">complexTypes</a>
                </xsl:if>
            </xsl:variable>
            <xsl:variable name="ss2">
                <xsl:copy-of select="$ss1"/>
                <xsl:if test="wsdl:types/xs:schema/xs:simpleType">
                    <xsl:if test="$ss1!=''">,</xsl:if>
                    <a href="#simpleType-list">simpleTypes</a>
                </xsl:if>
            </xsl:variable>
            <xsl:variable name="ss3">
                <xsl:copy-of select="$ss2"/>
                <xsl:if test="wsdl:types/xs:schema/xs:element">
                    <xsl:if test="$ss2!=''">,</xsl:if>
                    <a href="#global-element-list">global elements</a>
                </xsl:if>
            </xsl:variable>
            <xsl:variable name="ss4">
                <xsl:copy-of select="$ss3"/>
                <xsl:if test="wsdl:types/xs:schema/*//xs:element[@name]">
                    <xsl:if test="$ss3!=''">,</xsl:if>
                    <a href="#local-element-list">local elements</a>
                </xsl:if>
            </xsl:variable>
            <xsl:variable name="ss5">
                <xsl:copy-of select="$ss4"/>
                <xsl:if test="wsdl:types/xs:schema/xs:attribute">
                    <xsl:if test="$ss4!=''">,</xsl:if>
                    <a href="#global-attribute-list">global attributes</a>
                </xsl:if>
            </xsl:variable>
            <xsl:variable name="ss6">
                <xsl:copy-of select="$ss5"/>
                <xsl:if test="wsdl:types/xs:schema/*//xs:attribute[@name]">
                    <xsl:if test="$ss5!=''">,</xsl:if>
                    <a href="#local-attribute-list">local attributes</a>
                </xsl:if>
            </xsl:variable>
            <xsl:variable name="ss7">
                <xsl:copy-of select="$ss6"/>
                <xsl:if test="wsdl:types/xs:schema/xs:group">
                    <xsl:if test="$ss6!=''">,</xsl:if>
                    <a href="#model-group-list">model groups</a>
                </xsl:if>
            </xsl:variable>
            <xsl:variable name="ss8">
                <xsl:copy-of select="$ss7"/>
                <xsl:if test="wsdl:types/xs:schema/xs:attributeGroup">
                    <xsl:if test="$ss7!=''">,</xsl:if>
                    <a href="#attributeGroup-list">attribute groups</a>
                </xsl:if>
            </xsl:variable>
            <xsl:variable name="ss9">
                <xsl:copy-of select="$ss8"/>
                <xsl:if test="wsdl:interface">
                    <xsl:if test="$ss8!=''">,</xsl:if>
                    <a href="#interface-list">interfaces</a>
                </xsl:if>
            </xsl:variable>
            <xsl:variable name="ss10">
                <xsl:copy-of select="$ss9"/>
                <xsl:if test="wsdl:interface/wsdl:fault">
                    <xsl:if test="$ss9!=''">,</xsl:if>
                    <a href="#fault-list">faults</a>
                </xsl:if>
            </xsl:variable>
            <xsl:variable name="ss11">
                <xsl:copy-of select="$ss10"/>
                <xsl:if test="wsdl:interface/wsdl:operation">
                    <xsl:if test="$ss10!=''">,</xsl:if>
                    <a href="#operation-list">operations</a>
                </xsl:if>
            </xsl:variable>
            <xsl:variable name="ss12">
                <xsl:copy-of select="$ss11"/>
                <xsl:if test="wsdl:binding">
                    <xsl:if test="$ss11!=''">,</xsl:if>
                    <a href="#binding-list">bindings</a>
                </xsl:if>
            </xsl:variable>
            <xsl:variable name="ss13">
                <xsl:copy-of select="$ss12"/>
                <xsl:if test="wsdl:service">
                    <xsl:if test="$ss12!=''">,</xsl:if>
                    <a href="#service-list">services</a>
                </xsl:if>
            </xsl:variable>
            <xsl:copy-of select="$ss13"/>
            <xsl:text>defined in this wsdl</xsl:text>
        </div>
    </xsl:template>
</xsl:stylesheet>
