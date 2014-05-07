package au.gov.sa.environment.gift.xml;
 
import java.io.*;
import java.util.*;
 
import au.gov.sa.environment.gift.general.StringUtil;
import org.apache.commons.beanutils.BeanUtils;
 
/**
 * This class is used to generate an XML document from the data model. This XML document is
 * then used to generate PDF outputs.
 */
public class XmlExtProcessor extends XmlDataProcessor {
 
  private Map breaks = new HashMap();
 
 protected void writeXml(ArrayList data, Object datum) {
   System.out.println("Entered writeXml with datum " + datum + " and " + data.size() + " entries");
 
   breaks.clear();
   try {
     Properties p = loadXmlProperties(datum, null);
     String xmlHeader = p.getProperty("xmlheader");
     if (xmlHeader == null) {
       System.out.println("no xmlheader property");
       return;
     }
     String mainTag = p.getProperty("maintag");
     if (mainTag == null) {
       System.out.println("no maintag property");
       return;
     }
 
     if (selectOutputFile() && filePath == null) {
       System.out.println("no file path");
       return;
     }
 
     File outputFile = new File(filePath);
     if (DEBUG) System.out.println("Output is " + outputFile.getAbsolutePath());
     PrintWriter printer = new PrintWriter(new FileOutputStream(outputFile));
 
     XmlWriter output = new XmlWriter(printer);
 
     output.writeXmlVersion("1.0", "UTF-8", null);
     StringTokenizer itsList = new StringTokenizer(mainTag, ",");
     String it = null;
     String itsValue = null;
     if (itsList.hasMoreTokens()) {
       output.writeEntity(itsList.nextToken());
       while (itsList.hasMoreTokens()) {
         it = itsList.nextToken();
         if ("noNamespace".equals(it)) {
           it = itsList.nextToken();
           output.writeAttribute("xsi:noNamespaceSchemaLocation", it);
           output.writeAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
         } else if ("extract-date".equals(it)) {
           output.writeAttribute("extract-date", StringUtil.toFormattedDate("dd MMMMMMMMM yyyy"));
         }
       }
       for (int i=0; i < data.size(); i++) {
         datum = data.get(i);
         p = loadXmlProperties(datum, null);
         writeToXml(datum, p, output);
       }
       output.endEntity();
     }
     output.close();
     printer.close();
   } catch (Exception e) {
     e.printStackTrace();
   }
 }
 
 /**
   * Use the properties file to write an XML file for the object
   * @param datum JavaBean identifying the table properties file
   * @param p Properties file
   * @param output PrintWriter for the output
   */
 protected int writePartToXml(Object datum, Properties p, XmlWriter output) {
   String it = null;
   String itsName = null;
   String itsValue = null;
   StringTokenizer itsList = null;
   String propertyText = null;
   String objectName = null;
   List tagsInOrder = new ArrayList();
   Set integerNames = new HashSet();
   try {
     Map datumMap = BeanUtils.describe(datum);
     propertyText = p.getProperty("defaulttags");
     if (propertyText != null) {
       itsList = new StringTokenizer(propertyText, ",");
       while (itsList.hasMoreTokens()) {
         it = itsList.nextToken();
         if (itsList.hasMoreTokens()) {
           output.writeEntityWithText(it, itsList.nextToken());
         }
       }

     }
     objectName = p.getProperty("object");
     if (objectName != null) {
       itsList = new StringTokenizer(objectName, ",");
       if (itsList.hasMoreTokens()) {
         objectName = itsList.nextToken();
         output.writeEntity(objectName);
         while (itsList.hasMoreTokens()) {
           it = itsList.nextToken();
           if (itsList.hasMoreTokens()) {
             itsValue = itsList.nextToken();
             if (itsValue != null) {
               String convertIt = p.getProperty(itsValue);
               if (datumMap.containsKey(itsValue)) itsValue = (String) datumMap.get(itsValue);
               if (itsValue != null) output.writeAttribute(it, convertOutputValue(convertIt, itsValue));
             }
           }
         }
       }
     }
 
     propertyText = p.getProperty("integers");
     if (propertyText != null) {
       itsList = new StringTokenizer(propertyText, ",");
       while (itsList.hasMoreTokens()) {
         integerNames.add(itsList.nextToken());
       }
     }
 
     propertyText = p.getProperty("objecttags");
     if (propertyText == null) return 0;
 
     itsList = new StringTokenizer(propertyText, ",");
     while (itsList.hasMoreTokens()) {
         tagsInOrder.add(itsList.nextToken());
     }
 
     for (int i=0; i<tagsInOrder.size(); i++) {
       itsName = (String) tagsInOrder.get(i);
       processTag(datumMap, integerNames, itsName, datum, p, output);
     }
     if (objectName != null) output.endEntity();
   } catch (Exception e) {
     e.printStackTrace();
   }
   return breaks.size();
 }
 
 
 /**
   * Use the properties file to write XML content for the object
   * @param datum JavaBean identifying the table properties file
   * @param p Properties file
   * @param output PrintWriter for the output
   */
 protected void processTag(Map datumMap, Set integerNames, String itsName, Object datum, Properties p, XmlWriter output)
   throws Exception {
   String it = null;
   String itsValue = null;
   StringTokenizer itsList = null;
   String propertyText = null;
   String objectName = null;
   propertyText = p.getProperty(itsName);
   itsList = new StringTokenizer(propertyText, ",");
   if (itsList.hasMoreTokens()) {
     it = itsList.nextToken();
     if ("tag".equals(it)) {
       if (datumMap.containsKey(itsName)) {
         String useIfNull = simpleTag(output, datumMap, p, itsList);
         output.writeText(defaultedValue(itsName, integerNames, datumMap, useIfNull));
         output.endEntity();
       }
     } else if ("tag-content".equals(it)) {
       if (datumMap.containsKey(itsName) && datumMap.get(itsName) != null) {
         String contentTag = simpleTag(output, datumMap, p, itsList);
         output.writeEntity(contentTag);
         output.writeText(defaultedValue(itsName, integerNames, datumMap, null));
         output.endEntity();
         output.endEntity();
       }
     } else if ("tag-null".equals(it)) {
       if (datumMap.get(itsName) != null) {
         String testTag = itsList.nextToken();
         if (datumMap.get(testTag) == null) {
           String contentAttribute = simpleTag(output, datumMap, p, itsList);
           itsValue = defaultedValue(itsName, integerNames, datumMap, null);
           if (contentAttribute == null) {
             output.writeText(itsValue);
           } else {
             output.writeAttribute(contentAttribute, itsValue);
           }
           output.endEntity();
         }
       }
     } else if ("tag-notnull".equals(it)) {
       String testTag = itsList.nextToken();
       if (datumMap.get(itsName) == null) {
         propertyText = p.getProperty(testTag);
         if (propertyText != null) processTag(datumMap, integerNames, testTag, datum, p, output);
       } else {

         String contentAttribute = simpleTag(output, datumMap, p, itsList);
         itsValue = defaultedValue(itsName, integerNames, datumMap, null);
         if (contentAttribute == null) {
           output.writeText(itsValue);
         } else {
           output.writeEntityWithText(contentAttribute, itsValue);
         }
         output.endEntity();
       }
     } else if ("body".equals(it)) {
       if (datumMap.containsKey(itsName)) {
         output.writeText(defaultedValue(itsName, integerNames, datumMap, null));
       }
     } else if ("tag-break".equals(it)) {
       it = itsList.nextToken();
       if (it != null) {
         // Might be a /-separated list of break items
         StringTokenizer breakIt = new StringTokenizer(it, "/");
         int broken = 0;
         while (breakIt.hasMoreTokens()) {
           itsName = breakIt.nextToken();
           itsValue = (String) datumMap.get(itsName);
           if (breaks.containsKey(itsName)) {
             propertyText = (String) breaks.get(itsName);
             if (propertyText != null && !propertyText.equals(itsValue)) {
               if (broken == 0) broken = 2;
             }
           } else broken = 1;
           breaks.put(itsName, itsValue);
         }
         if (broken > 1) {
           for (int j=0; j < breaks.size(); j++) output.endEntity();
         }
         if (broken > 0) {
           while (itsList.hasMoreTokens()) {
             itsName = itsList.nextToken();
             itsValue = p.getProperty(itsName);
             if (itsValue == null) {
               output.writeEntity(itsName);
             } else {
               StringTokenizer breakTags = new StringTokenizer(itsValue, ",");
               if (breakTags.hasMoreTokens()) {
                 it = breakTags.nextToken();
                 if ("tag".equals(it)) {
                   simpleTag(output, datumMap, p, breakTags);
                 } else {
                   processTag(datumMap, integerNames, itsName, datum, p, output);
                 }
               }
             }
           }
         }
       }
     } else if ("tag-switch".equals(it)) {
       if (datumMap.containsKey(itsName)) {
         itsValue = BeanUtils.getProperty(datum, itsName);
         while (itsList.hasMoreTokens()) {
           it = itsList.nextToken();
           propertyText = itsList.nextToken();
           if (it != null && it.equals(itsValue)) {
             Properties newProperties = loadXmlProperties(datum, propertyText);
             if (newProperties.getProperty("object") != null) {
               writeToXml(datum, newProperties, output);
             }
           }
         }
       }
     } else if ("collection".equals(it)) {
       boolean enclosed = itsList.hasMoreTokens();
       Object theCollection = getNamedProperty(datum, itsName);
       Iterator overCollection = getCollection(theCollection);
       enclosed = enclosed && overCollection.hasNext();
       if (enclosed) simpleTag(output, datumMap, p, itsList);
       // Iterate over the collection: for each member with an Xml.properties, call writeToXml
       int unclosedBreaks = 0;
       while (overCollection.hasNext()) {
         Object newDatum = overCollection.next();
         Properties newProperties = loadXmlProperties(newDatum, null);
         if (newProperties.getProperty("objecttags") != null) {
           unclosedBreaks = writePartToXml(newDatum, newProperties, output);
         }
       }
       for (int j=0; j < unclosedBreaks; j++) output.endEntity();
       if (enclosed) output.endEntity();
     }
   }
 }
 
 
 /**
   * Generate a tag with attributes
   * Otherwise returns null
   * @param output the XmlWriter
   * @param datumMap properties of the data object
   * @param p the XML properties
   * @param itsList tokenizer for the attribute list
   */
 protected String simpleTag(XmlWriter output, Map datumMap, Properties p, StringTokenizer itsList) throws Exception {
   String result = null;
   if (itsList.hasMoreTokens()) {

     String it = itsList.nextToken();
     output.writeEntity(it);
     while (itsList.hasMoreTokens()) {
       it = itsList.nextToken();
       if (itsList.hasMoreTokens()) {
         String itsValue = itsList.nextToken();
         if (itsValue != null) {
           String convertIt = p.getProperty(itsValue);
             if (datumMap.containsKey(itsValue))
               itsValue = (String) datumMap.get(itsValue);
             if (itsValue != null)
               output.writeAttribute(it, convertOutputValue(convertIt, itsValue));
         }
       } else result = it;
     }
   }
   return result;
 }
 /**
   * If the object is a collection of some sort, returns an Iterator for it
   * Otherwise returns null
   * @param collection the Object which may be an array, a Map, a Collection, an Iterator or an Enumeration
   *                   or some other sort of object
   * @return Iterator if not null, will iterate over the collection
   */
 protected Iterator getCollection(Object collection) {
   Iterator iterator = null;
   // Construct an iterator for this collection
   if (collection instanceof Collection) {
       iterator = ((Collection) collection).iterator();
   } else if (collection instanceof Iterator) {
       iterator = (Iterator) collection;
   } else if (collection instanceof Map) {
       iterator = ((Map) collection).keySet().iterator();
       // We need commons-collections to be available for this - and we ain't gonna need it
//    } else if (collection instanceof Enumeration) {
//        iterator = IteratorUtils.asIterator((Enumeration) collection);
   }
   return iterator;
 }
 
 
 /**
   * Get a data value, allowing substitute on null value, "null" for null value,
   * "unknown" for -9999 code, zero decimal stripping from numeric field
   * @param itsName attribute within the bean
   * @param integerNames attributes to treat as numbers
   * @param datumMap name-value map from BeanUtils.describe
   * @param useIfNull attribute to use if itsName doesn't have a value
   */
 protected String defaultedValue(String itsName, HashSet integerNames, Map datumMap, String useIfNull) {
   String itsValue = null;
   if (datumMap.containsKey(itsName)) {
     itsValue = (String) datumMap.get(itsName);
   }
   if (itsValue == null || itsValue.length() == 0) {
     if (useIfNull != null && datumMap.containsKey(useIfNull)) {
       itsValue = (String) datumMap.get(useIfNull);
     }
   }
     if (itsValue == null) itsValue = "null";
     if (!integerNames.contains(itsName)) {
       if (itsValue.equals("-9999")) itsValue = "unknown";
     } else {
       if (itsValue.startsWith("-9999")) itsValue = "-9999";
       if (itsValue.endsWith(".0")) itsValue = itsValue.substring(0, itsValue.length() - 2);
     }
   return itsValue;
 }
 
 /**
   * Load the properties that describe the XML form for an object
   * @param datum the object
   * @return Properties the property set
   */
 protected Properties loadXmlProperties(Object datum, String suffix) {
   // Start with the default properties set
   Properties result = new Properties();
 
   // Make the <name>Xml.properties name
   StringBuffer buildName = new StringBuffer();
   String resourceName = datum.getClass().getName();
   buildName.append(resourceName.replace('.', '/'));
   if (suffix != null) buildName.append(suffix);
   buildName.append("Xml.properties");
 
   InputStream f = null;
   try {
     f = datum.getClass().getClassLoader().getResourceAsStream(buildName.toString());
     if (f != null) {
       result.load(f);
       f.close();
     }
   } catch (Exception e) {
       System.out.println("*** No properties found for " + buildName.toString());
   }
   return result;
 }
 
}

 

