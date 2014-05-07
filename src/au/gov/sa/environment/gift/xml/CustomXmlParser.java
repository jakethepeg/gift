package au.gov.sa.environment.gift.xml;
 
import java.io.*;
import java.util.*;
import javax.xml.parsers.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import java.lang.reflect.*;
import org.apache.commons.beanutils.*;
import org.apache.commons.beanutils.locale.converters.*;
 
import au.gov.sa.environment.gift.jdbc.GeneralDataProcessor;
 
/**
 * Parse an XML file according to the rules in an XmlStructure.
 * The XmlStructure encapsulates a mapping from Xml to Java objects 
 * @author not attributable
 * @version 1.0
 */
 
public class CustomXmlParser extends DefaultHandler {
 
    private XmlStructure theStructure = null;
    private Stack includeStack = new Stack();
    private Stack classStack = new Stack();
    Object target = null;
    Object datum = null;
    private String text = null;
    private String identifier = null;
    private List theList = null;
    private StringBuffer textBuffer = new StringBuffer();
 
 public CustomXmlParser() {
          try {
            ConvertUtils.register(new DateLocaleConverter(new java.util.Date(), new Locale("en"), "yyyy-MM-dd"), Class.forName("java.util.Date"));
          } catch (Exception e) {
                  e.printStackTrace();
          }
 }
 
 /**
   * Load an XML file into an object structure
   * @param datum reference class
   * @param structure Xml parse structure loaded from properties
   * @param source the file from which to read the data
   * @return Object loaded object structure
   */
 public Object parseTheXml(Object datum, XmlStructure structure, File source) {
   this.datum = datum;
   try {
     target = datum;
     theStructure = structure;
     theStructure.setCurrentObject(target);
     SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
     parser.parse(source, (DefaultHandler) this);
   } catch(Exception e) {
     e.printStackTrace();
     target = null;
   }
   return target;
 }

 /**
   * Load an XML file into an object structure
   * @param datum reference class
   * @param structure Xml parse structure loaded from properties
   * @param source the file from which to read the data
   * @return Object loaded object structure
   */
 public Object parseTheXml(Object datum, XmlStructure structure, InputStream source) {
   this.datum = datum;
   try {
     target = datum;
     theStructure = structure;
     theStructure.setCurrentObject(target);
     SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
     parser.parse(source, (DefaultHandler) this);
   } catch(Exception e) {
     e.printStackTrace();
     target = null;
   }
   return target;
 }
 /**
   * Convert a coded value on input
   * @param conversion list definition for conversion
   * @param source string to convert
   * @return String converted value
   */
 public String convertValue(String conversion, String source) {
   if (conversion != null && source != null) {
     StringTokenizer iter = new StringTokenizer(conversion, ",");
     if (iter.hasMoreTokens()) {
       String mode = iter.nextToken();
       if ("localPairs".equals(mode)) {
         while (iter.hasMoreTokens()) {
           String label = iter.nextToken();
           if (iter.hasMoreTokens()) {
             String value = iter.nextToken();
             if (source.equals(label)) {
               return value;
             }
           }
         }
       }
     }
   }
   return source;
 }
/**
 * applyMethod finds a compatible method on the target object and applies the data to it.
 * @param toObject target
 * @param methodName name of method
 * @param withValue value
 * @throws Exception
 */
 private void applyMethod(Object toObject, String methodName, Object withValue) throws Exception {
   Class parentClass = toObject.getClass();
   Class paramClass = withValue.getClass();
   Method[] possibles = parentClass.getMethods();
   for (Method possible : possibles) {

     if (methodName.equals(possible.getName())) {
       Class[] params = possible.getParameterTypes();
       if (params.length == 1) {
         if (params[0] == paramClass) {
           possible.invoke(toObject, new Object[] {withValue});
         } else if (withValue instanceof List) {
           possible.invoke(toObject, new Object[] {withValue});
         } else {
           possible.invoke(toObject, new Object[] {ConvertUtils.convert((String) withValue, params[0])});
         }
         return;
       }
     }
   }
   for (Method possible : possibles) {
     if (methodName.equals(possible.getName())) {
       Class[] params = possible.getParameterTypes();
       if (params.length == 2) {
         if (params[1] == paramClass) {
           possible.invoke(toObject, new Object[] {ConvertUtils.convert((String) withValue, params[0]), withValue});
         }
       }
     }
   }
 }
 
 public void characters(char[] ch, int start, int length) throws SAXException {
   textBuffer.append(ch, start, length);
 }
 public void endElement(String uri, String localName, String qName) throws SAXException {
   String text = textBuffer.toString();
   XmlAttribute targetAttr = theStructure.findAttributeForTagname(qName);
   if (targetAttr != null) {
     try {
       String targetType = targetAttr.getType();
       if (targetType.equals("head")) {
         // Check for body text storage
         String bodyName = theStructure.findTagnameForType("body");
         if (bodyName != null) {
           XmlAttribute attr = theStructure.findAttributeForTagname(bodyName);
           if (attr.getClassName() == null) {
             String dataName = attr.getDataName();
             if (dataName != null) {
               BeanUtils.setProperty(target, dataName, text);
             }
           }
         }
         // Add the datum to the collection on the parent datum
         // Pop back to the parent
         Object popped = popStructure(target, targetAttr);
         if (popped != null) {
           target = popped;
           endElement(uri, localName, qName);
         }
       }
       if (targetType.equals("tag")) {
         // Stash the text in the appropriate attribute
         if (targetAttr.getClassName() == null) {
             String dataName = targetAttr.getDataName();
             String convertName = targetAttr.getConvertName();
             if (convertName != null) {
               applyMethod(target, convertName, text);
             } else if (dataName != null) {
             BeanUtils.setProperty(target, dataName, text);
           }
         }
       }
       if (targetType.equals("tagselect")) {
         // Convert the name to a code and stash it
         String className = targetAttr.getClassName();
         String dataName = targetAttr.getDataName();
         if (className != null && dataName != null) {
           String lookupName = GeneralDataProcessor.getCachedValue(className, text, target);
           if (lookupName != null) {
             BeanUtils.setProperty(target, dataName, lookupName);
           } else {
             StringBuffer workName = new StringBuffer("set");
             workName.append(dataName.substring(0,1).toUpperCase()).append(dataName.substring(1));
             applyMethod(target, workName.toString(), theStructure.getData(dataName));
           }
         }
       }
       if (targetType.equals("tagobject")) {
           // Stash the object
           String className = targetAttr.getClassName();
           String dataName = targetAttr.getDataName();
           if (className != null && dataName != null) {
               StringBuffer workName = new StringBuffer("set");
               workName.append(dataName.substring(0,1).toUpperCase()).append(dataName.substring(1));
               List content = theStructure.getData(dataName);
               if (content.size() > 0) {
                       Object it = content.get(0);
                       if (it != null) {
                         applyMethod(target, workName.toString(), it);
                       }
               }
           }
         }
     }
     catch (Exception e) {

       e.printStackTrace();
     }
   }
   textBuffer.setLength(0);
 }
 
 public Object popStructure(Object datum, XmlAttribute attr) {
   Object target = null;
   List data = null;
   try {
     if (!includeStack.empty()) {
       theStructure.resetData();
       theStructure = (XmlStructure) includeStack.pop();
       String dataName = (String) classStack.pop();
       target = theStructure.getCurrentObject();
       data = theStructure.getData(dataName);
       data.add(datum);
     }
   } catch (Exception e) {
     e.printStackTrace();
   }
   return target;
 }
 
 public Object subStructure(Object datum, XmlAttribute attr) {
   Object target = null;
   try {
     String className = datum.getClass().getName();
     String newClass = attr.getClassName();
     int lastIndex = newClass.lastIndexOf(".");
     if (lastIndex < 0) {
       lastIndex = className.lastIndexOf(".");
       if (lastIndex > 0) className = className.substring(0, lastIndex+1) + attr.getClassName();
     } else className = newClass;
     target = Class.forName(className).newInstance();
     includeStack.push(theStructure);
     classStack.push(attr.getDataName());
     theStructure = attr.getChildStructure();
     theStructure.setCurrentObject(target);
   } catch (Exception e) {
     e.printStackTrace();
   }
   return target;
 }
 
 public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
   textBuffer.setLength(0);
   Iterator it = null;
   XmlAttribute targetAttr = theStructure.findAttributeForTagname(qName);
   if (targetAttr != null) {
     try {
       String targetType = targetAttr.getType();
       if (targetType.equals("head")) {
         // Add any nominated attributes to the datum
         Map attList = theStructure.getAttributes();
         it = attList.keySet().iterator();
         while (it.hasNext()) {
           XmlAttribute attr = (XmlAttribute) attList.get(it.next());
           if ("attr".equals(attr.getType())) {
             String aValue = attributes.getValue(attr.getTagName());
             BeanUtils.copyProperty(target, attr.getDataName(), convertValue(attr.getConvertName(), aValue));
           }
         }
       }
       if (targetType.equals("attr")) {
         // Add the nominated attribute to the datum
         Map attList = theStructure.getAttributes();
         String aValue = attributes.getValue(targetAttr.getClassName());
         BeanUtils.copyProperty(target, targetAttr.getDataName(), convertValue(targetAttr.getConvertName(), aValue));
       }
       if (targetType.equals("tagselect") || targetType.equals("tagobject")) {
         String lookupName = GeneralDataProcessor.getCachedValue(targetAttr.getClassName(), null, target);
         if (lookupName == null) {
           // Push in to the childStructure
           Object newTarget = subStructure(target, targetAttr);
           if (newTarget != null) {
             target = newTarget;
             startElement(uri, localName, qName, attributes);
           }
         }
       }
     }
     catch (Exception e) {
       e.printStackTrace();
     }
   }
 }
 
 
}

