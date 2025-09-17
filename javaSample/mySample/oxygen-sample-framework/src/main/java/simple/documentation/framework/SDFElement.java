package simple.documentation.framework;

import java.util.ArrayList;
import java.util.List;

import ro.sync.contentcompletion.xml.CIAttribute;
import ro.sync.contentcompletion.xml.CIElement;
import ro.sync.contentcompletion.xml.CIElementAdapter;

/**
 * Simple Documentation Framework element.
 */
public class SDFElement extends CIElementAdapter {
  /**
   * The namespace.
   */
  protected String namespace = null;

  /**
   * The element name.
   */
  protected String name = null;

  /**
   * The proxy.
   */
  protected String proxy = null;

  /**
   * The type description.
   */
  private String typeDescription = null;

  /**
   * List of attributes.
   */
  protected List<CIAttribute> attributes = null;

  /**
   * The possible values as <code>String</code> list.
   */
  private List<String> possiblesValuesList;

  /**
   * The element model description.
   */
  private String modelDescription;

  /**
   * The facets. One facet can be null if it is not defined.
   */
  private String lengthFacetValue;

  /**
   * The content type of the element.
   */
  private int contentType = CONTENT_TYPE_NOT_DETERMINED;

  /**
   * True if content is nillable. Used only for XML Schema.
   */
  private boolean nillable;

  /**
   * Facet values.
   */
  private String minLengthFacetValue;
  private String maxLengthFacetValue;
  private String whitespaceFacetValue;
  private String minInclusiveFacetValue;
  private String minExclusiveFacetValue;
  private String maxInclusiveFacetValue;
  private String maxExclusiveFacetValue;
  private String totalDigitsFacetValue;  
  private String fractionDigitsFacetValue;
  private String facetPatternValue;

  /**
   * Guess some following elements if possible
   */
  protected List<CIElement> guessElements = null;

  /**
   * @see ro.sync.contentcompletion.xml.CIElement#getGuessElements()
   */
  @Override
  public List<CIElement> getGuessElements() {
    if (guessElements != null && guessElements.isEmpty()) {
      // Return null is the list is empty.
      return null;
    }
    return guessElements;
  }

  /**
   * @see ro.sync.contentcompletion.xml.CIElement#addGuessElement(ro.sync.contentcompletion.xml.CIElement)
   */
  @Override
  public void addGuessElement(CIElement e) {
    if (guessElements == null) {
      guessElements = new ArrayList<CIElement>();
    }
    guessElements.add(e);
  }

  /**
   * @return The string representing the name or null.
   */
  @Override
  public String getName(){
    return name;
  }

  /**
   * @see ro.sync.contentcompletion.xml.CIElement#getNamespace()
   */
  @Override
  public String getNamespace(){
    return namespace;
  }

  /**
   * True if the element has a namespace.
   */
  private boolean xmlns;

  /**
   * @see ro.sync.contentcompletion.xml.CIElement#setDeclareXmlns(boolean)
   */
  @Override
  public void setDeclareXmlns(boolean xmlns) {
    this.xmlns = xmlns;
  }

  /**
   * @see ro.sync.contentcompletion.xml.CIElement#setContentType(int)
   */
  @Override
  public void setContentType(int contentType) {
    this.contentType = contentType;
  }

  /**
   * @see ro.sync.contentcompletion.xml.CIElement#isEmpty()
   */
  @Override
  public boolean isEmpty() {
    return getContentType() == CONTENT_TYPE_EMPTY;
  }

  /**
   * @see ro.sync.contentcompletion.xml.CIElement#getContentType()
   */
  @Override
  public int getContentType() {
    return contentType;
  }

  /**
   * @see ro.sync.contentcompletion.xml.CIElement#isDeclareXmlns()
   */
  @Override
  public boolean isDeclareXmlns() {
    return xmlns;
  }

  /**
   * @see ro.sync.contentcompletion.xml.CIElement#setName(java.lang.String)
   */
  @Override
  public void setName(String name) {
    this.name = name;
  }
  /**
   * @see ro.sync.contentcompletion.xml.CIElement#setPrefix(java.lang.String)
   */
  @Override
  public void setPrefix(String proxy) {
    this.proxy = proxy;
  }

  /**
   * @see ro.sync.contentcompletion.xml.CIElement#setNamespace(java.lang.String)
   */
  @Override
  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }


  /**
   * @see ro.sync.contentcompletion.xml.CIElement#getQName()
   */
  @Override
  public String getQName() {
    if (getPrefix() != null && !"".equals(getPrefix())) {
      return getPrefix() + ":" + getName();
    } else {
      return getName();
    }
  }

  /***
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object o) {
    boolean result = false;
    if (o instanceof SDFElement) {
      CIElement cie = (CIElement) o;
      result = compareTo(cie) == 0;
    }
    return result;
  }

  /**
   * @see ro.sync.contentcompletion.xml.CIElement#getAttributes()
   */
  @Override
  public List<CIAttribute> getAttributes() {
    return attributes;
  }

  /**
   * @see ro.sync.contentcompletion.xml.CIElement#setAttributes(java.util.List)
   */
  @Override
  public void setAttributes(List<CIAttribute> attributes) {
    this.attributes = attributes;
  }

  /**
   * Concatenates the name and the namespace and compares it with the other name and namespace,
   * as strings.
   * @param other The object to compare to.
   * @return The value <code>0</code> if the argument object is equal to this object.
   */
  @Override
  public int compareTo(CIElement other){
    String n1 = getName() == null ? "": getName();
    String nm1 = getNamespace() == null ? "": getNamespace();

    String n2 = other.getName() == null ? "": other.getName();
    String nm2 = other.getNamespace() == null ? "": other.getNamespace();

    int result = n1.compareTo(n2);
    if(result == 0) {
      result = nm1.compareTo(nm2);
    }
    return result;
  }

  /**
   * Return the name.
   * 
   * @return The name.
   */
  @Override
  public String toString(){
    String toRet = String.valueOf(getName());
    return toRet;
  }

  /**
   * @see ro.sync.contentcompletion.xml.CIElement#hasPrefix()
   */
  @Override
  public boolean hasPrefix() {
    return getPrefix() != null && !"".equals(getPrefix());
  }

  /**
   * @see ro.sync.contentcompletion.xml.CIElement#getPrefix()
   */
  @Override
  public String getPrefix() {
    return proxy;
  }

  /**
   * @param modelDescription The modelDescription to set.
   */
  @Override
  public void setModelDescription(String modelDescription) {
    this.modelDescription = modelDescription;
  }

  /**
   * @param fractionDigitsFacetValue The fractionDigitsFacetValue to set.
   */
  @Override
  public void setFacetFractionDigitsValue(String fractionDigitsFacetValue) {
    this.fractionDigitsFacetValue = fractionDigitsFacetValue;
  }
  /**
   * @param maxExclusiveFacetValue The maxExclusiveFacetValue to set.
   */
  @Override
  public void setFacetMaxExclusiveValue(String maxExclusiveFacetValue) {
    this.maxExclusiveFacetValue = maxExclusiveFacetValue;
  }
  /**
   * @param maxInclusiveFacetValue The maxInclusiveFacetValue to set.
   */
  @Override
  public void setFacetMaxInclusiveValue(String maxInclusiveFacetValue) {
    this.maxInclusiveFacetValue = maxInclusiveFacetValue;
  }
  /**
   * @param maxLengthFacetValue The maxLengthFacetValue to set.
   */
  @Override
  public void setFacetMaxLengthValue(String maxLengthFacetValue) {
    this.maxLengthFacetValue = maxLengthFacetValue;
  }
  /**
   * @param minInclusiveFacetValue The minInclusiveFacetValue to set.
   */
  @Override
  public void setFacetMinInclusiveValue(String minInclusiveFacetValue) {
    this.minInclusiveFacetValue = minInclusiveFacetValue;
  }
  /**
   * @param possiblesValuesList The possiblesValuesList to set.
   */
  @Override
  public void setPossiblesValues(List<String> possiblesValuesList) {
    this.possiblesValuesList = possiblesValuesList;
  }
  /**
   * @param totalDigitsFacetValue The totalDigitsFacetValue to set.
   */
  @Override
  public void setFacetTotalDigitsValue(String totalDigitsFacetValue) {
    this.totalDigitsFacetValue = totalDigitsFacetValue;
  }
  /**
   * @param whitespaceFacetValue The whitespaceFacetValue to set.
   */
  @Override
  public void setFacetWhitespaceValue(String whitespaceFacetValue) {
    this.whitespaceFacetValue = whitespaceFacetValue;
  }

  /**
   * @param lengthFacetValue The lengthFacetValue to set.
   */
  @Override
  public void setFacetLengthValue(String lengthFacetValue) {
    this.lengthFacetValue = lengthFacetValue;
  }
  /**
   * @param minLengthFacetValue The minLengthFacetValue to set.
   */
  @Override
  public void setFacetMinLengthValue(String minLengthFacetValue) {
    this.minLengthFacetValue = minLengthFacetValue;
  }
  /**
   * @param minExclusiveFacetValue The minExclusiveFacetValue to set.
   */
  @Override
  public void setFacetMinExclusiveValue(String minExclusiveFacetValue) {
    this.minExclusiveFacetValue = minExclusiveFacetValue;
  }

  /**
   * @see ro.sync.contentcompletion.xml.NodeDescription#getFacetFractionDigitsValue()
   */
  @Override
  public String getFacetFractionDigitsValue() {
    return fractionDigitsFacetValue;
  }


  /**
   * @see ro.sync.contentcompletion.xml.NodeDescription#getFacetTotalDigitsValue()
   */
  @Override
  public String getFacetTotalDigitsValue() {
    return totalDigitsFacetValue;
  }


  /**
   * @see ro.sync.contentcompletion.xml.NodeDescription#getFacetMaxInclusiveValue()
   */
  @Override
  public String getFacetMaxInclusiveValue() {
    return maxInclusiveFacetValue;
  }

  /**
   * @see ro.sync.contentcompletion.xml.NodeDescription#getFacetMaxExclusiveValue()
   */
  @Override
  public String getFacetMaxExclusiveValue() {
    return maxExclusiveFacetValue;
  }


  /**
   * Get the value of MIN_INCLUSIVE facet, can be null if it is not defined.
   * 
   * @return  The value of MIN_INCLUSIVE facet.
   */
  @Override
  public String getFacetMinInclusiveValue() {
    return minInclusiveFacetValue;
  }

  /**
   * Get the value of MIN_EXCLUSIVE facet, can be null if it is not defined.
   * 
   * @return  The value of MIN_EXCLUSIVE facet.
   */
  @Override
  public String getFacetMinExclusiveValue() {
    return minExclusiveFacetValue;
  }

  /**
   * Get the possible values as <code>String</code> list.
   * 
   * @return  The possible values.
   */
  @Override
  public List<String> getPossibleValues() {
    return possiblesValuesList;
  }

  /**
   * Get the model description.
   * @return  The model description.
   */
  @Override
  public String getModelDescription() {
    return modelDescription;
  }
  /**
   * Get the value of LENGTH facet, can be null if it is not defined.
   * 
   * @return  The value of length facet.
   */
  @Override
  public String getFacetLengthValue() {
    return lengthFacetValue;
  }
  /**
   * Get the value of MIN LENGTH facet, can be null if it is not defined.
   * 
   * @return  The value of MIN LENGTH facet.
   */
  @Override
  public String getFacetMinLengthValue() {
    return minLengthFacetValue;
  }
  /**
   * Get the value of MAX LENGTH facet, can be null if it is not defined.
   * 
   * @return  The value of MAX LENGTH facet.
   */
  @Override
  public String getFacetMaxLengthValue() {
    return maxLengthFacetValue;
  }

  /**
   * Get the value of WHITESPACE facet, can be null if it is not defined.
   * 
   * @return  The value of WHITESPACE facet.
   */
  @Override
  public String getFacetWhitespaceValue() {
    return whitespaceFacetValue;
  }  

  /**
   * Get the list with pattern facets.
   * 
   * @return  The list with pattern facets, can be null if no FACET_PATTERN defined.
   */
  @Override
  public String getFacetPattern(){
    return facetPatternValue;
  }

  /**
   * Set the list with pattern facets.
   * 
   * @param patternFacets The list with pattern facets.
   */
  @Override
  public void setFacetPattern(String patternFacets){
    this.facetPatternValue = patternFacets;
  }

  /**
   * The annotation string, null as default.
   */
  protected String annotation;

  /**
   * Get the annotation for the element.
   * 
   * @return A text that explain how to use the attribute, or null.
   */
  @Override
  public String getAnnotation() {
    return annotation;
  }

  /**
   * @see ro.sync.contentcompletion.xml.CIElement#setAnnotation(java.lang.String)
   */
  @Override
  public void setAnnotation(String annotation){
    this.annotation = annotation;
  }

  /**
   * @see ro.sync.contentcompletion.xml.CIElement#getTypeDescription()
   */
  @Override
  public String getTypeDescription() {
    return typeDescription;
  }

  /**
   * @see ro.sync.contentcompletion.xml.CIElement#setTypeDescription(java.lang.String)
   */
  @Override
  public void setTypeDescription(String typeDescription) {
    this.typeDescription = typeDescription;
  }

  /**
   * @see ro.sync.contentcompletion.xml.CIElement#setNillable(boolean)
   */
  @Override
  public void setNillable(boolean nillable) {
    this.nillable = nillable;
  }

  /**
   * @see ro.sync.contentcompletion.xml.CIElement#isNillable()
   */
  @Override
  public boolean isNillable() {
    return nillable;
  }
}