package simple.documentation.framework.extensions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import ro.sync.contentcompletion.xml.CIAttribute;
import ro.sync.contentcompletion.xml.CIElement;
import ro.sync.contentcompletion.xml.CIValue;
import ro.sync.contentcompletion.xml.Context;
import ro.sync.contentcompletion.xml.ContextElement;
import ro.sync.contentcompletion.xml.SchemaManagerFilter;
import ro.sync.contentcompletion.xml.WhatAttributesCanGoHereContext;
import ro.sync.contentcompletion.xml.WhatElementsCanGoHereContext;
import ro.sync.contentcompletion.xml.WhatPossibleValuesHasAttributeContext;
import simple.documentation.framework.SDFElement;

/**
 * Schema manager filter for the Simple Documentation Framework.
 * Filters custom elements, attributes, elements values and attributes values.
 *
 */
public class SDFSchemaManagerFilter implements SchemaManagerFilter {

  /**
   * Filter values of the "href" attribute of an image element.
   */
  public List<CIValue> filterAttributeValues(List<CIValue> attributeValues,
      WhatPossibleValuesHasAttributeContext context) {
    // If the element from the current context is the "image" element and the current context 
    // attribute is "href" then add a custom URL value to the list of default content completion 
    // proposals.
    if (context != null) {
      ContextElement currentElement = context.getParentElement();
      String attributeName = context.getAttributeName();
      if("image".equals(currentElement.getQName()) && "href".equals(attributeName)) {
        CIValue newValue = new CIValue("Custom_image_URL");
        if (attributeValues == null) {
          attributeValues = new ArrayList<CIValue>();
        }
        // Add the new value.
        attributeValues.add(newValue);
      }
    }
    return attributeValues;
  }

  /**
   * Filter attributes of the "table" element.
   */
  public List<CIAttribute> filterAttributes(List<CIAttribute> attributes,
      WhatAttributesCanGoHereContext context) {
    // If the element from the current context is the 'table' element add the
    // attribute named 'frame' to the list of default content completion proposals
    if (context != null) {
      ContextElement contextElement = context.getParentElement();
      if ("table".equals(contextElement.getQName())) {
        CIAttribute frameAttribute = new CIAttribute();
        frameAttribute.setName("frame");
        frameAttribute.setRequired(false);
        frameAttribute.setFixed(false);
        frameAttribute.setDefaultValue("void");
        if (attributes == null) {
          attributes = new ArrayList<CIAttribute>();
        }
        attributes.add(frameAttribute);
      }
    }
    return attributes;
  }

  /**
   * Filter the value of the "title" element from the "section". 
   */
  public List<CIValue> filterElementValues(List<CIValue> elementValues,
      Context context) {
    // If the element from the current context is the title of the section element then add the 
    // "Custom Section Title" value to the list of default content completion proposals
    if (context != null) {
      Stack<ContextElement> elementStack = context.getElementStack();
      if (elementStack != null) {
        ContextElement contextElement = elementStack.peek();
        if(contextElement != null) {
          int size = elementStack.size();
          if ("title".equals(contextElement.getQName())) {
            ContextElement parentElement = elementStack.elementAt(size - 2);
            if (parentElement != null && "section".equals(parentElement.getQName())) {
              elementValues = new ArrayList<CIValue>();
              CIValue val = new CIValue("Custom Section Title");
              elementValues.add(val);
            }
          }
        }
      }
    }
    return elementValues;
  }

  /**
   * Filter "header" elements.
   */
  public List<CIElement> filterElements(List<CIElement> elements,
      WhatElementsCanGoHereContext context) {
    // If the element from the current context is the 'header' element remove the
    // 'td' element from the list of content completion proposals and add the
    // 'th' element.
    if (context != null) {  
      Stack<ContextElement> elementStack = context.getElementStack();
      if (elementStack != null) {
        ContextElement contextElement = context.getElementStack().peek();
        if ("header".equals(contextElement.getQName())) {
          if (elements != null) {
            for (Iterator<CIElement> iterator = elements.iterator(); iterator.hasNext();) {
              CIElement element = iterator.next();
              // Remove the 'td' element
              if ("td".equals(element.getQName())) {
                elements.remove(element);
                break;
              }
            }
          } else {
            elements = new ArrayList<CIElement>();
          }
          // Insert the 'th' element in the list of content completion proposals
          CIElement thElement = new SDFElement();
          thElement.setName("th");
          elements.add(thElement);
        }
      }
    } else {
      // If the given context is null then the given list of content completion elements contains
      // global elements. 
    }
    return elements;
  }

  public String getDescription() {
    return "Implementation for the Simple Documentation Framework schema manager filter.";
  }

}