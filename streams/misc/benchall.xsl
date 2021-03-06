<?xml version="1.0" ?>
<!--
  benchall.xsl: convert an XML listing of StreamIt benchmarks to HTML
  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
  $Id: benchall.xsl,v 1.8 2006-09-08 18:51:52 thies Exp $

  Notes for the uninitiated: this is an XSL Transform stylesheet.  Use
  an XSLT processor, such as xsltproc, to convert XML to XML using this;
  the input is the file generated by build-bench-xml.py, the output is
  XHTML.

  XSLT stylesheets are structured as a series of templates.  Each template
  is essentially a function call, with local (immutable) variables and
  (immutable) parameters, both with default values; it is called when the
  input XML tag matches the specified expression.  xsl:apply-templates
  calls appropriate templates for children of the current node.  XML tags
  that aren't xsl: tags, and things inside xsl:text, are copied verbatim
  to the output.
-->

<xsl:stylesheet version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:file="java.io.File"
  xmlns:string="java.lang.String"
  xmlns:str="http://exslt.org/strings">

  <xsl:template match="/benchset">
    <html>
      <head>
        <title>StreamIt Benchmarks</title>
        <style fprolloverstyle="0">A:hover {color: #990000}</style>
      </head>
      <body>
        <table border="0" width="700" cellpadding="2" align="center">
          <colgroup> 
            <col align="left" />
            <col align="char" char="."/> 
          </colgroup> 
          <tbody> 
              <tr height="150">
              <h3>StreamIt Benchmarks</h3>
            </tr>
          </tbody> 
        </table>
        <xsl:apply-templates/>
      </body>
    </html>
  </xsl:template>

  <xsl:template match="dir">
    <xsl:param name="subdir">.</xsl:param>
    <xsl:param name="depth">1</xsl:param>
    <!-- Create an HTML header, labelled with the name attribute of the
         directory node, but only if there are subdirectories. -->
    <xsl:if test="dir and @name != '.'">
      <xsl:element name="h{$depth}">
          <!-- <xsl:value-of select="@name"/> -->
      </xsl:element>
    </xsl:if>
    <xsl:apply-templates>
      <xsl:with-param name="subdir">
        <xsl:value-of select="$subdir"/>/<xsl:value-of select="@name"/>
      </xsl:with-param>
      <xsl:with-param name="depth" select="$depth+1"/>
    </xsl:apply-templates>
  </xsl:template>

  <xsl:template match="benchmark">
    <xsl:param name="subdir">.</xsl:param>
    <xsl:param name="depth">3</xsl:param>
    <xsl:element name="h{$depth}">
      <table border="1" width="700" align="center"> 
        <colgroup> 
          <col align="left" />
          <col align="char" char="."/> 
        </colgroup> 
        <thead> 
          <tr>
            <th colspan="2" bgcolor="#C0C0C0" align="left">
              <xsl:value-of select="name"/> - <xsl:value-of select="desc"/>
            </th> 
          </tr> 
        </thead> 
        <tbody> 
          <tr>
            <td width="15%" valign="top" align="left"><b>Description</b></td> 
            <td align="justify"><xsl:value-of select="description"/></td>
          </tr> 
          <xsl:apply-templates select="reference"/>
          <xsl:apply-templates select="implementations">
            <xsl:with-param name="subdir" select="$subdir"/>
          </xsl:apply-templates>
        </tbody> 
      </table>
    </xsl:element>
  </xsl:template>

  <xsl:template match="reference">
    <tr>
      <td valign="top" align="left"><b>Reference</b></td> 
      <td><xsl:apply-templates/></td>
    </tr> 
  </xsl:template>

  <xsl:template match="implementations">
    <xsl:param name="subdir">.</xsl:param>
    <!-- <p><b>Implementations:</b><br/> -->
    <xsl:apply-templates>
      <xsl:with-param name="subdir" select="$subdir"/>
    </xsl:apply-templates>
    <!-- </p> -->
  </xsl:template>

  <xsl:template match="impl">
    <!-- Save the declared subdirectory, if any. -->
    <xsl:param name="subdir">.</xsl:param>
    <xsl:variable name="path">
      <xsl:value-of select="$subdir"/>
      <xsl:if test="@dir">
        <xsl:text>/</xsl:text><xsl:value-of select="@dir"/>
      </xsl:if>
      <xsl:text>/</xsl:text>
    </xsl:variable>
    <!-- Do not describe files that don't exist -->
    <xsl:variable name="allFiles" select="file"/>
    <!-- We only want one filename because we're going to be testing for its existence -->
    <xsl:variable name="firstFile" select="substring-before(concat($allFiles,' '),' ')"/>
    <xsl:variable name="fullName" select="concat($path,$firstFile)"/>
    <xsl:if test="file:exists(file:new($fullName))">
      <tr>
        <td valign="top" align="left"><b><xsl:value-of select="@lang"/></b></td>
        <td valign="top" align="left">
        <xsl:for-each select="file">
          <!-- some benchmarks have multiple files.  deal with each separately. -->
          <xsl:for-each select="str:tokenize(., ' ')">
            <!-- do not print .out files -->
            <xsl:if test="not(string:endsWith(string(.), '.out'))">
              <xsl:element name="a">
                <xsl:attribute name="href">
                  <xsl:value-of select="concat($path,.)"/>
                </xsl:attribute>
                <xsl:value-of select="string:substring(string(.),1+string:lastIndexOf(string(.),'/'))"/>
              </xsl:element>
              <xsl:text> </xsl:text>
            </xsl:if>
          </xsl:for-each>
        </xsl:for-each>
          <xsl:if test="desc">(<xsl:value-of select="desc"/>)<br/></xsl:if>
        </td>
      </tr>
    </xsl:if>
  </xsl:template>
</xsl:stylesheet>
