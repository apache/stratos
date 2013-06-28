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
                xmlns:xs="http://www.w3.org/2001/XMLSchema" exclude-result-prefixes="xs">
    <!--
         Created by: Jonathan Marsh <jonathan@wso2.com>
         23 October 2006
         7 November 2006 - JM: code review and cleanup
     -->
    <xsl:output method="html" indent="yes" encoding="UTF-8"/>
    <!--
         Many schemas refer to the built-in schema types.  In order to navigate to those types, one needs
         to know where to load the schema for schemas, hopefully a local version with this stylesheet
         applied so the user can continue to navigate.
      -->
    <xsl:param name="xsd-schema-location" select="'/service-mgt/styles/XMLSchema.xsd'"/>

    <!-- QName resolving functions require the targetNamespace, so stuff it in a global variable. -->
    <xsl:variable name="targetNamespace" select="xs:schema/@targetNamespace"/>

    <!-- Mozilla doesn't support the namespace axis, which makes simulating namespace declarations
         problematic.  At least we can try alternate reconstruction methods if we know the functionality
         isn't there.  -->
    <xsl:variable name="supports-namespace-axis" select="count(/*/namespace::*) &gt; 0"/>

    <!-- ===  Main  ========================================
         Main template for the schema document
     -->
    <xsl:template match="/">
        <html>
            <head>
                <title>Schema for
                    <xsl:value-of select="xs:schema/@targetNamespace"/>
                </title>
                <style type="text/css">
                    <xsl:call-template name="css"/>
                </style>
            </head>
            <body>
                <!-- use a table to ensure sidebars longer than their associated tag don't run into the following sidebar.
                        CSS would be preferable (progressive rendering would be improved) if there were a way to do this (I can't find one.) -->
                <table cellpadding="0" cellspacing="0">
                    <xsl:apply-templates select="xs:schema"/>
                </table>
                <p/>
                <hr/>
                <table cellpadding="0" cellspacing="0" id="index">
                    <xsl:call-template name="generate-xsd-index"/>
                </table>
            </body>
        </html>
    </xsl:template>
    <!-- ===  Elements  ========================================
         The following templates format elements of various flavors
         (xs:schema, children of xs:schema, grandchildren etc. of schema, and extension elements)
     -->
    <xsl:template match="xs:schema">
        <!--
              Schema element has schema-block classes, enabling a stylesheet to style the
              entire schema, especially useful when embedding schema in another langage (e.g. WSDL).
          -->
        <tr>
            <td class="schema-block annotation-area">
                <div class="sidebar">
                    <xsl:call-template name="index-reference"/>
                </div>
            </td>
            <td class="schema-block">
                <div class="arrow">&#160;</div>
            </td>
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
    <xsl:template match="xs:schema/xs:*">
        <xsl:variable name="identifier">
            <xsl:call-template name="schema-identifier"/>
        </xsl:variable>
        <tr>
            <xsl:choose>
                <!-- Decide which top-level schema elements get sidebars -->
                <xsl:when test="not(self::xs:annotation or self::xs:import or self::xs:include)">
                    <td class="schema-block annotation-area">
                        <div class="{local-name()} sidebar sidebar-title">
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
                            <xsl:call-template name="referenced-by"/>
                        </div>
                    </td>
                    <td class="schema-block">
                        <div class="arrow">&#160;</div>
                    </td>
                </xsl:when>
                <xsl:otherwise>
                    <td colspan="2" class="schema-block annotation-area"/>
                </xsl:otherwise>
            </xsl:choose>
            <td class="schema-block source-area">
                <div class="schema-top-level">
                    <xsl:call-template name="element-start"/>
                </div>
                <div class="schema-top-level">
                    <xsl:apply-templates/>
                </div>
                <div class="schema-top-level">
                    <xsl:call-template name="element-end"/>
                </div>
            </td>
        </tr>
    </xsl:template>
    <xsl:template match="xs:*">
        <!-- Third and deeper levels of schema elements (no sidebar, therefore no table row - just a div. -->
        <xsl:variable name="identifier">
            <xsl:call-template name="schema-identifier"/>
        </xsl:variable>
        <div class="indent">
            <xsl:if test="$identifier != ''">
                <xsl:attribute name="id">
                    <xsl:value-of select="$identifier"/>
                </xsl:attribute>
            </xsl:if>
            <div>
                <xsl:call-template name="element-start"/>
            </div>
            <xsl:apply-templates/>
            <div>
                <xsl:call-template name="element-end"/>
            </div>
        </div>
    </xsl:template>
    <xsl:template match="xs:schema/*[not(self::xs:*)]">
        <!-- Top-level extension elements -->
        <tr>
            <td colspan="2" class="schema-block annotation-area"/>
            <td class="schema-block source-area">
                <div class="schema-top-level extension">
                    <xsl:call-template name="element-start">
                        <xsl:with-param name="class" select="'markup-extension-element'"/>
                    </xsl:call-template>
                </div>
                <xsl:apply-templates/>
                <div class="extension">
                    <xsl:call-template name="element-end">
                        <xsl:with-param name="class" select="'markup-extension-element'"/>
                    </xsl:call-template>
                </div>
            </td>
        </tr>
    </xsl:template>
    <xsl:template match="*">
        <!-- If we've got to here, we're dealing with non-top-level extension elements.  -->
        <div class="indent">
            <div class="extension">
                <xsl:call-template name="element-start">
                    <xsl:with-param name="class" select="'markup-extension-element'"/>
                </xsl:call-template>
            </div>
            <xsl:apply-templates/>
            <div class="extension">
                <xsl:call-template name="element-end">
                    <xsl:with-param name="class" select="'markup-extension-element'"/>
                </xsl:call-template>
            </div>
        </div>
    </xsl:template>
    <!-- ===  Attributes  =========================================
         The following templates format attributes of various flavors
     -->
    <xsl:template match="xs:*/@id">
        <a name="{@id}"/>
        <xsl:call-template name="attribute"/>
    </xsl:template>
    <xsl:template match="xs:*/@name">
        <xsl:call-template name="attribute">
            <xsl:with-param name="value-class">markup-name-attribute-value</xsl:with-param>
        </xsl:call-template>
    </xsl:template>
    <xsl:template match="xs:*/@ref">
        <xsl:call-template name="attribute">
            <xsl:with-param name="value-class">markup-name-attribute-value</xsl:with-param>
            <xsl:with-param name="reference">
                <xsl:call-template name="external-reference"/>
                <xsl:text>#_</xsl:text>
                <xsl:value-of select="local-name(..)"/>
                <xsl:text>_</xsl:text>
                <xsl:value-of select="substring-after(.,':')"/>
            </xsl:with-param>
        </xsl:call-template>
    </xsl:template>
    <xsl:template match="xs:*/@type | xs:*/@base | xs:*/@itemType | xs:*/@memberTypes">
        <xsl:call-template name="attribute">
            <xsl:with-param name="value-class">markup-name-attribute-value</xsl:with-param>
            <xsl:with-param name="reference">
                <xsl:call-template name="external-reference"/>
                <xsl:text>#_type_</xsl:text>
                <xsl:value-of select="substring-after(.,':')"/>
            </xsl:with-param>
        </xsl:call-template>
    </xsl:template>
    <xsl:template match="xs:*/@source | xs:*/@schemaLocation">
        <xsl:call-template name="attribute">
            <xsl:with-param name="reference" select="."/>
        </xsl:call-template>
    </xsl:template>
    <xsl:template match="@*">
        <xsl:call-template name="attribute"/>
    </xsl:template>

    <!-- ===  Text nodes  ========================================
         The following template formats text nodes
     -->
    <xsl:template match="text()[normalize-space(.) != '']">
        <div class="markup-text-content">
            <xsl:value-of select="."/>
        </div>
    </xsl:template>

    <!-- ===  Comments  ========================================
         The following template formats comment nodes
     -->
    <xsl:template match="xs:schema/comment()">
        <tr>
            <td colspan="2" class="schema-block annotation-area"/>
            <td class="schema-block source-area">
                <div class="schema-top-level markup-comment">
                    <xsl:text>&lt;!--</xsl:text>
                    <xsl:value-of select="."/>
                    <xsl:text>--&gt;</xsl:text>
                </div>
            </td>
        </tr>
    </xsl:template>
    <xsl:template match="comment()">
        <div class="markup-comment indent">
            <xsl:text>&lt;!--</xsl:text>
            <xsl:value-of select="."/>
            <xsl:text>--&gt;</xsl:text>
        </div>
    </xsl:template>

    <!-- ===  Library templates  ========================================
         Library of useful named templates
     -->
    <xsl:template name="css"><![CDATA[
.schema {padding-left:5em; text-indent:-5em}
.schema-top-level {padding-left: 6em; text-indent:-5em}
.indent {margin-left:1em}
.double-indent {margin-left:2em}
.trivialText {color:gray}

td {vertical-align:top; font: 100%/1.3 "Lucida Grande","Lucida Sans","Lucida Sans Unicode","trebuchet ms",verdana,sans-serif}
.annotation-area {width:16em}
.source-area    {font-size:80%; padding-bottom:.6em}
.sidebar        {font-size:80%; padding-bottom:.6em; margin-bottom:2px}
.arrow          {border-top: 1px dashed black; width:2em; position:relative; top:.5em}
.note           {background-color:rgb(255,255,210); text-align:right; border: 1px dashed black; padding:.5em}
.simpleType     {background-color:rgb(255,242,210); text-align:right; border: 1px dashed black; padding:.5em} 
.complexType    {background-color:rgb(255,242,210); text-align:right; border: 1px dashed black; padding:.5em} 
.element        {background-color:rgb(225,237,246); text-align:right; border: 1px dashed black; padding:.5em}
.group          {background-color:rgb(225,237,246); text-align:right; border: 1px dashed black; padding:.5em} 
.attribute      {background-color:rgb(225,246,235); text-align:right; border: 1px dashed black; padding:.5em} 
.attributeGroup {background-color:rgb(225,246,235); text-align:right; border: 1px dashed black; padding:.5em} 
.redefine       {background-color:rgb(255,255,210); text-align:right; border: 1px dashed black; padding:.5em} 
.notation       {background-color:rgb(255,255,210); text-align:right; border: 1px dashed black; padding:.5em} 
.sidebar-title  {} 
.sidebar-title-highlight {font-weight:bold}
.sidebar-text   {padding-top:.3em}
ul {margin-left:1em; margin-top:0em; margin-bottom:0em}
.referenced-item {list-style-type:square; text-align:left; margin-left:.5em}
.referenced-list {margin-top:.5em; text-align:left}

#index {margin-top:2em; margin-bottom:2em} 

.markup {color:gray}
.markup-element {color:gray; text-indent:-2em; }
.markup-extension-element {color:navy}
.markup-attribute {color:gray}
.markup-extension-attribute {color:navy}
.markup-attribute-value {}
.markup-name-attribute-value {font-weight:bold}
.markup-namespace {color:purple}
.markup-namespace-uri {color:purple}
.markup-text-content  {margin-left:-4em; text-indent:0em}
.markup-comment  {color:green}
]]>
    </xsl:template>
    <xsl:template name="attribute">
        <xsl:param name="value-class" select="'markup-attribute-value'"/>
        <xsl:param name="reference"/>
        <xsl:param name="native-attribute" select="parent::xs:* and namespace-uri(.)=''"/>
        <xsl:text></xsl:text>
        <span>
            <xsl:choose>
                <xsl:when test="$native-attribute">
                    <xsl:attribute name="class">markup-attribute</xsl:attribute>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:attribute name="class">markup-extension-attribute</xsl:attribute>
                </xsl:otherwise>
            </xsl:choose>
            <xsl:value-of select="name(.)"/>
        </span>
        <span class="markup">
            <xsl:text>="</xsl:text>
        </span>
        <span class="{$value-class}">
            <xsl:choose>
                <xsl:when test="$reference">
                    <a href="{$reference}">
                        <xsl:value-of select="."/>
                    </a>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="."/>
                </xsl:otherwise>
            </xsl:choose>
        </span>
        <span class="markup">
            <xsl:text>"</xsl:text>
        </span>
    </xsl:template>
    <xsl:template name="namespaces">
        <xsl:variable name="current" select="current()"/>
        <!-- Unfortunately Mozilla doesn't support the namespace axis, need to check for that and simulate declarations -->
        <xsl:choose>
            <xsl:when test="$supports-namespace-axis">
                <!--
                        When the namespace axis is present (e.g. Internet Explorer), we can simulate
                        the namespace declarations by comparing the namespaces in scope on this element
                        with those in scope on the parent element.  Any difference must have been the
                        result of a namespace declaration.  Note that this doesn't reflect the actual
                        source - it will strip out redundant namespace declarations.
                    -->
                <xsl:for-each select="namespace::*[. != 'http://www.w3.org/XML/1998/namespace']">
                    <xsl:if test="not($current/parent::*[namespace::*[. = current()]])">
                        <span class="markup-namespace">
                            <xsl:text>xmlns</xsl:text>
                            <xsl:if test="name() != ''">:</xsl:if>
                            <xsl:value-of select="name()"/>
                            <xsl:text>="</xsl:text>
                        </span>
                        <span class="markup-namespace-uri">
                            <xsl:value-of select="."/>
                        </span>
                        <span class="markup-namespace">
                            <xsl:text>"</xsl:text>
                        </span>
                    </xsl:if>
                </xsl:for-each>
            </xsl:when>
            <xsl:otherwise>
                <!--
                        When the namespace axis isn't supported (e.g. Mozilla), we can simulate
                        appropriate declarations from namespace elements.
                        This currently doesn't check for namespaces on attributes.
                        In the general case we can't reliably detect the use of QNames in content, but
                        in the case of schema, we know which content could contain a QName and look
                        there too.  This mechanism is rather unpleasant though, since it records
                        namespaces where they are used rather than showing where they are declared
                        (on some parent element) in the source.  Yukk!
                    -->
                <xsl:if test="namespace-uri(.) != namespace-uri(parent::*)">
                    <span class="markup-namespace">
                        <xsl:text>xmlns</xsl:text>
                        <xsl:if test="substring-before(name(),':') != ''">:</xsl:if>
                        <xsl:value-of select="substring-before(name(),':')"/>
                        <xsl:text>="</xsl:text>
                    </span>
                    <span class="markup-namespace-uri">
                        <xsl:value-of select="namespace-uri(.)"/>
                    </span>
                    <span class="markup-namespace">
                        <xsl:text>"</xsl:text>
                    </span>
                </xsl:if>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    <xsl:template name="element-start">
        <xsl:param name="class" select="'markup-element'"/>
        <span class="markup">&lt;</span>
        <span class="{$class}">
            <xsl:value-of select="name(.)"/>
        </span>
        <xsl:apply-templates select="@*"/>
        <xsl:call-template name="namespaces"/>
        <span class="markup">
            <xsl:if test="not(node())">
                <xsl:text>/</xsl:text>
            </xsl:if>
            <xsl:text>&gt;</xsl:text>
        </span>
    </xsl:template>
    <xsl:template name="element-end">
        <xsl:param name="class" select="'markup-element'"/>
        <xsl:if test="node()">
            <span class="markup">
                <xsl:text>&lt;/</xsl:text>
            </span>
            <span class="{$class}">
                <xsl:value-of select="name(.)"/>
            </span>
            <span class="markup">
                <xsl:text>&gt;</xsl:text>
            </span>
        </xsl:if>
    </xsl:template>

    <xsl:template name="schema-identifier">
        <!-- Calculate a fragment identifier for the element this element refers to, if any -->
        <xsl:if test="@name | @ref">
            <xsl:choose>
                <xsl:when
                        test="(self::xs:attribute or self::xs:element) and not(parent::xs:schema)">
                    <xsl:variable name="top" select="ancestor::*[parent::xs:schema]"/>
                    <xsl:variable name="toptype">
                        <xsl:choose>
                            <xsl:when test="$top[self::xs:complexType or self::xs:simpleType]">type
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="local-name($top)"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:variable>
                    <xsl:text>_local</xsl:text>
                    <xsl:value-of select="local-name()"/>
                    <xsl:text>_</xsl:text>
                    <xsl:value-of select="$toptype"/>
                    <xsl:text>_</xsl:text>
                    <xsl:value-of select="$top/@name"/>
                </xsl:when>
                <xsl:when test="local-name()='complexType' or local-name()='simpleType'">_type
                </xsl:when>
                <xsl:otherwise>
                    <xsl:text>_</xsl:text>
                    <xsl:value-of select="local-name()"/>
                </xsl:otherwise>
            </xsl:choose>
            <xsl:text>_</xsl:text>
            <xsl:value-of select="@name | @ref"/>
        </xsl:if>
    </xsl:template>

    <xsl:template name="componentListItem">
        <xsl:variable name="typeIdentifier">
            <xsl:choose>
                <xsl:when test="self::xs:complexType or self::xs:simpleType">type</xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="local-name()"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:if test="position()>1">
            <xsl:text>,</xsl:text>
        </xsl:if>
        <a href="#_{$typeIdentifier}_{@name}">
            <xsl:value-of select="@name"/>
        </a>
    </xsl:template>
    <xsl:template name="localComponentListItem">
        <xsl:variable name="typeIdentifier" select="local-name()"/>
        <xsl:variable name="top" select="ancestor::*[parent::xs:schema]"/>
        <xsl:variable name="toptype">
            <xsl:choose>
                <xsl:when test="$top[self::xs:complexType or self::xs:simpleType]">type</xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="local-name($top)"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="tnsName">
            <xsl:choose>
                <xsl:when test="contains(@name, ':') and namespace::*[. = $targetNamespace and name(.) =
					substring-before(@name,':')]">
                    <xsl:value-of select="substring-after(@name,':')"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="@name"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:if test="position()>1">
            <xsl:text>,</xsl:text>
        </xsl:if>
        <a href="#_local{$typeIdentifier}_{$toptype}_{$top/@name}_{@name}">
            <xsl:value-of select="$tnsName"/>
        </a>
        <span class="trivialText">of the</span>
        <xsl:value-of select="$top/@name"/>
        <xsl:text></xsl:text>
        <span class="trivialText">
            <xsl:value-of select="local-name($top)"/>
        </span>
    </xsl:template>
    <xsl:template name="insert-reference">
        <xsl:if test="self::xs:attribute">
            <xsl:variable name="top" select="ancestor::*[parent::xs:schema]"/>
            <xsl:variable name="toptype">
                <xsl:choose>
                    <xsl:when test="$top[self::xs:complexType or self::xs:simpleType]">type
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="local-name($top)"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:variable>
            <a href="#_localattribute_{$toptype}_{$top/@name}_{@name | @ref}">
                <xsl:value-of select="@name | @ref"/>
            </a>
            <xsl:text>local attribute of the</xsl:text>
        </xsl:if>
        <xsl:for-each select="ancestor-or-self::xs:*[last() - 1]">
            <xsl:variable name="type">
                <xsl:choose>
                    <xsl:when test="self::xs:complexType or self::xs:simpleType">type</xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="local-name(.)"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:variable>
            <a href="#_{$type}_{@name}">
                <xsl:value-of select="@name"/>
            </a>
            <xsl:text></xsl:text>
            <xsl:value-of select="local-name(.)"/>
        </xsl:for-each>
    </xsl:template>
    <xsl:template name="referenced-by">
        <xsl:variable name="target" select="@name"/>
        <xsl:if test="parent::xs:schema">
            <div class="sidebar-text">
                <xsl:variable name="extended-by" select="ancestor::xs:schema//xs:extension[substring-after(@base | @type | @ref |
					@itemType,':') = $target]"/>
                <xsl:if test="count($extended-by) > 0">
                    <div class="referenced-list">Extended by:</div>
                    <ul>
                        <xsl:for-each select="$extended-by">
                            <xsl:sort select="@base | @type | @ref | @itemType"/>
                            <li class="referenced-item">
                                <xsl:call-template name="insert-reference"/>
                            </li>
                        </xsl:for-each>
                    </ul>
                </xsl:if>
                <xsl:variable name="restricted-by" select="ancestor::xs:schema//xs:restriction[substring-after(@base | @type | @ref |
					@itemType,':') = $target]"/>
                <xsl:if test="count($restricted-by) > 0">
                    <div class="referenced-list">Restricted by:</div>
                    <ul>
                        <xsl:for-each select="$restricted-by">
                            <xsl:sort select="@base | @type | @ref | @itemType"/>
                            <li class="referenced-item">
                                <xsl:call-template name="insert-reference"/>
                            </li>
                        </xsl:for-each>
                    </ul>
                </xsl:if>
                <xsl:variable name="referenced-by" select="ancestor::xs:schema//xs:*[not(self::xs:extension) and
					not(self::xs:restriction)][substring-after(@base | @type | @ref | @itemType,':')
					= $target]"/>
                <xsl:if test="count($referenced-by) > 0">
                    <div class="referenced-list">Referenced by:</div>
                    <ul>
                        <xsl:for-each select="$referenced-by">
                            <xsl:sort select="@base | @type | @ref | @itemType"/>
                            <li class="referenced-item">
                                <xsl:call-template name="insert-reference"/>
                            </li>
                        </xsl:for-each>
                    </ul>
                </xsl:if>
                <xsl:call-template name="additional-references">
                    <xsl:with-param name="target" select="$target"/>
                </xsl:call-template>
            </div>
        </xsl:if>
    </xsl:template>
    <xsl:template name="additional-references">
        <xsl:param name="target"/>
        <!-- override this template to list additonal references -->
    </xsl:template>
    <xsl:template name="external-reference">
        <xsl:choose>
            <xsl:when test="$supports-namespace-axis">
                <xsl:if test="not(../namespace::*[. = $targetNamespace and name(.) =
					substring-before(current(),':')])">
                    <xsl:variable name="prefix" select="substring-before(.,':')"/>
                    <xsl:variable name="uri" select="../namespace::*[name(.) = $prefix]"/>
                    <xsl:choose>
                        <xsl:when test="$uri = 'http://www.w3.org/2001/XMLSchema'">
                            <xsl:value-of select="$xsd-schema-location"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of
                                    select="ancestor::xs:schema/xs:import[@namespace=$uri]/@schemaLocation"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:if>
            </xsl:when>
            <xsl:otherwise>
                <xsl:choose>
                    <!-- Take a wild guess at the most commonly used type prefixes -->
                    <xsl:when
                            test="substring-before(current(),':') = 'xs' or substring-before(current(),':') = 'xsd'">
                        <xsl:value-of select="$xsd-schema-location"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <!-- The link will be broken.  Bad Mozilla! -->
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    <xsl:template name="generate-xsd-index">
        <xsl:if test="xs:schema/xs:complexType">
            <tr>
                <td id="complexType-list" class="annotation-area">
                    <div class="complexType sidebar sidebar-title-highlight">Index of complexTypes
                    </div>
                </td>
                <td>
                    <div class="arrow">&#160;</div>
                </td>
                <td class="source-area">
                    <xsl:for-each select="xs:schema/xs:complexType">
                        <xsl:sort select="@name"/>
                        <xsl:call-template name="componentListItem"/>
                    </xsl:for-each>
                </td>
            </tr>
        </xsl:if>
        <xsl:if test="xs:schema/xs:simpleType">
            <tr>
                <td id="simpleType-list" class="annotation-area">
                    <div class="simpleType sidebar sidebar-title-highlight">Index of simpleTypes
                    </div>
                </td>
                <td>
                    <div class="arrow">&#160;</div>
                </td>
                <td class="source-area">
                    <xsl:for-each select="xs:schema/xs:simpleType">
                        <xsl:sort select="@name"/>
                        <xsl:call-template name="componentListItem"/>
                    </xsl:for-each>
                </td>
            </tr>
        </xsl:if>
        <xsl:if test="xs:schema/xs:element">
            <tr>
                <td id="global-element-list" class="annotation-area">
                    <div class="element sidebar sidebar-title-highlight">Index of global elements
                    </div>
                </td>
                <td>
                    <div class="arrow">&#160;</div>
                </td>
                <td class="source-area">
                    <xsl:for-each select="xs:schema/xs:element">
                        <xsl:sort select="@name"/>
                        <xsl:call-template name="componentListItem"/>
                    </xsl:for-each>
                </td>
            </tr>
        </xsl:if>
        <xsl:if test="xs:schema/*//xs:element[@name]">
            <tr>
                <td id="local-element-list" class="annotation-area">
                    <div class="element sidebar sidebar-title-highlight">Index of local elements
                    </div>
                </td>
                <td>
                    <div class="arrow">&#160;</div>
                </td>
                <td class="source-area">
                    <xsl:for-each select="xs:schema/*//xs:element[@name]">
                        <xsl:sort select="@name"/>
                        <xsl:call-template name="localComponentListItem"/>
                    </xsl:for-each>
                </td>
            </tr>
        </xsl:if>
        <xsl:if test="xs:schema/xs:attribute">
            <tr>
                <td id="global-attribute-list" class="annotation-area">
                    <div class="attribute sidebar sidebar-title-highlight">Index of global
                        attributes
                    </div>
                </td>
                <td>
                    <div class="arrow">&#160;</div>
                </td>
                <td class="source-area">
                    <xsl:for-each select="xs:schema/xs:attribute">
                        <xsl:sort select="@name"/>
                        <xsl:call-template name="componentListItem"/>
                    </xsl:for-each>
                </td>
            </tr>
        </xsl:if>
        <xsl:if test="xs:schema/*//xs:attribute[@name]">
            <tr>
                <td id="local-attribute-list" class="annotation-area">
                    <div class="attribute sidebar sidebar-title-highlight">Index of local attributes
                    </div>
                </td>
                <td>
                    <div class="arrow">&#160;</div>
                </td>
                <td class="source-area">
                    <xsl:for-each select="xs:schema/*//xs:attribute[@name]">
                        <xsl:sort select="@name"/>
                        <xsl:call-template name="localComponentListItem"/>
                    </xsl:for-each>
                </td>
            </tr>
        </xsl:if>
        <xsl:if test="xs:schema/xs:group">
            <tr>
                <td id="model-group-list" class="annotation-area">
                    <div class="group sidebar sidebar-title-highlight">Index of model groups</div>
                </td>
                <td>
                    <div class="arrow">&#160;</div>
                </td>
                <td class="source-area">
                    <xsl:for-each select="xs:schema/xs:group">
                        <xsl:sort select="@name"/>
                        <xsl:call-template name="componentListItem"/>
                    </xsl:for-each>
                </td>
            </tr>
        </xsl:if>
        <xsl:if test="xs:schema/xs:attributeGroup">
            <tr>
                <td id="attributeGroup-list" class="annotation-area">
                    <div class="attributeGroup sidebar sidebar-title-highlight">Index of attribute
                        groups
                    </div>
                </td>
                <td>
                    <div class="arrow">&#160;</div>
                </td>
                <td class="source-area">
                    <xsl:for-each select="xs:schema/xs:attributeGroup">
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
                <xsl:if test="xs:complexType">
                    <a href="#complexType-list">complexTypes</a>
                </xsl:if>
            </xsl:variable>
            <xsl:variable name="ss2">
                <xsl:copy-of select="$ss1"/>
                <xsl:if test="xs:simpleType">
                    <xsl:if test="$ss1!=''">,</xsl:if>
                    <a href="#simpleType-list">simpleTypes</a>
                </xsl:if>
            </xsl:variable>
            <xsl:variable name="ss3">
                <xsl:copy-of select="$ss2"/>
                <xsl:if test="xs:element">
                    <xsl:if test="$ss2!=''">,</xsl:if>
                    <a href="#global-element-list">global elements</a>
                </xsl:if>
            </xsl:variable>
            <xsl:variable name="ss4">
                <xsl:copy-of select="$ss3"/>
                <xsl:if test="*//xs:element[@name]">
                    <xsl:if test="$ss3!=''">,</xsl:if>
                    <a href="#local-element-list">local elements</a>
                </xsl:if>
            </xsl:variable>
            <xsl:variable name="ss5">
                <xsl:copy-of select="$ss4"/>
                <xsl:if test="xs:attribute">
                    <xsl:if test="$ss4!=''">,</xsl:if>
                    <a href="#global-attribute-list">global attributes</a>
                </xsl:if>
            </xsl:variable>
            <xsl:variable name="ss6">
                <xsl:copy-of select="$ss5"/>
                <xsl:if test="*//xs:attribute[@name]">
                    <xsl:if test="$ss5!=''">,</xsl:if>
                    <a href="#local-attribute-list">local attributes</a>
                </xsl:if>
            </xsl:variable>
            <xsl:variable name="ss7">
                <xsl:copy-of select="$ss6"/>
                <xsl:if test="xs:group">
                    <xsl:if test="$ss6!=''">,</xsl:if>
                    <a href="#model-group-list">model groups</a>
                </xsl:if>
            </xsl:variable>
            <xsl:variable name="ss8">
                <xsl:copy-of select="$ss7"/>
                <xsl:if test="xs:attributeGroup">
                    <xsl:if test="$ss7!=''">,</xsl:if>
                    <a href="#attributeGroup-list">attribute groups</a>
                </xsl:if>
            </xsl:variable>
            <xsl:copy-of select="$ss8"/>
            <xsl:text>defined in this schema.</xsl:text>
        </div>
    </xsl:template>
</xsl:stylesheet>
