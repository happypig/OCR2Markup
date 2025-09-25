<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xs="http://www.w3.org/2001/XMLSchema"
  exclude-result-prefixes="xs file arrays string object"
  version="1.0"
  xmlns:file="xalan://java.io.File" 
  xmlns:arrays="xalan://java.util.Arrays" 
  xmlns:string="xalan://java.lang.String" 
  xmlns:object="xalan://java.lang.Object" 
  >
  <xsl:output indent="yes"/>
  
  <xsl:param name="codebase">http://some.site.com/</xsl:param>
  <xsl:param name="href">http://some.site.com/test.jnlp</xsl:param>
  
  <xsl:param name="jnlpdir">D:\projects\oxygen-sdk-samples\oxygen-sdk-sample-swing-components\target\jnlp\</xsl:param>
  <xsl:param name="libs">lib</xsl:param>
  <xsl:param name="main-class">com.sample.App</xsl:param>
  <xsl:param name="main-jar">my.jar</xsl:param>
  
  
  <!-- 
    Generates the dependencies by looking into the lib folder 
  -->
  <xsl:template match="DEPENDENCIES">
    
    <jar href="{$libs}/{$main-jar}" main="true"/>
    
    <xsl:variable name="path"><xsl:value-of select="$jnlpdir"/>/<xsl:value-of select="$libs"/></xsl:variable>
    <xsl:message> Examining libraries from path: <xsl:value-of select="$path"/></xsl:message>
    
    <xsl:variable name="listing1" select="object:toString(arrays:asList(file:list(file:new($path))))"/>
    <xsl:variable name="listing2" select="normalize-space(translate($listing1,'],[','   '))"/>
    <xsl:variable name="listing3" select="translate($listing2,' ',',')"/>    
    <xsl:call-template name="tokenize">
      <xsl:with-param name="pText" select="$listing3"/>
    </xsl:call-template>
  </xsl:template>
  
  <!-- 
    Converts a list of filepath tokens into jar references.
  -->  
  <xsl:template name="tokenize">
    <xsl:param name="pText"/>
    <xsl:if test="string-length($pText) &gt; 0">
      <xsl:variable name="before" select="substring-before($pText, ',')"/>
      
      <xsl:variable name="resourceElement">
      	<xsl:choose>
      		<xsl:when test="starts-with($before, 'native-')">nativelib</xsl:when>
      		<xsl:otherwise>jar</xsl:otherwise>
      	</xsl:choose>      	
      </xsl:variable>      

      <xsl:element name="{$resourceElement}">
        <xsl:attribute name="href">
          <xsl:choose>
            <xsl:when test="$before">
              <xsl:value-of select="concat($libs,'/',$before)"/>
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="concat($libs,'/',$pText)"/>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:attribute>
      </xsl:element>
      
      <xsl:call-template name="tokenize">
        <xsl:with-param name="pText" select="substring-after($pText, ',')"/>
      </xsl:call-template>
    </xsl:if>
  </xsl:template>
  
  <!-- 
    Maps the values of the attributes.
  -->
  <xsl:template match="@*">
    <xsl:attribute name="{name()}">
      <xsl:variable name="val"><xsl:value-of select="."/></xsl:variable>
      <xsl:choose>
        <xsl:when test="$val = '@@CODEBASE@@'"  ><xsl:value-of select="$codebase"/></xsl:when>
        <xsl:when test="$val = '@@HREF@@'"      ><xsl:value-of select="$href"/></xsl:when>
        <xsl:when test="$val = '@@MAIN_CLASS@@'"><xsl:value-of select="$main-class"/></xsl:when>
        <xsl:otherwise><xsl:value-of select="$val"/></xsl:otherwise>
      </xsl:choose>
    </xsl:attribute>
  </xsl:template>
  
  <!-- 
    Generic copy template.
  -->
  <xsl:template match="node()">
    <xsl:copy>
      <xsl:apply-templates select="node() | @*"/>
    </xsl:copy>
  </xsl:template>
  
  
</xsl:stylesheet>