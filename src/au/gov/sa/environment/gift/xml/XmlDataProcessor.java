package au.gov.sa.environment.gift.xml;
 
import java.util.*;
 
import java.io.*;
import java.awt.FileDialog;
import javax.swing.*;
 
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
 
import au.gov.sa.environment.gift.jdbc.GeneralDataProcessor;
import au.gov.sa.environment.gift.jdbc.GeneralFileFilter;
import au.gov.sa.environment.gift.general.StringUtil;
 
/**
 * <p>Title: Generic XML processor</p>
 * <p>Description: Driven by property files named for the JavaBeans its handed</p>
 * <p >Copyright: Copyright (c) 2003</p>
 * <p>Company: DEH</p>
 * @author Peter Davey
 * @version 1.1
 */
 
public class XmlDataProcessor {
 
 protected static final boolean DEBUG=Boolean.getBoolean("XMLDEBUG");
 protected static Map propertyCache = new HashMap();
 protected static Map descriptorsCache = new HashMap();
 protected static File priorPath = null;
 
 protected JFrame owningFrame = null;
 
 protected Map usedNames = null;
 
 protected String filePath;
 
 // Simple constructor
 public XmlDataProcessor() {
 }
 
 public void setOwningFrame(JFrame owningFrame) {
   this.owningFrame = owningFrame;
 }
 
 public JFrame getOwningFrame() {
   return owningFrame;
 }
 
 // Choose an output file
 protected boolean selectOutputFile() {
            JFileChooser chooser = new JFileChooser(priorPath);
            GeneralFileFilter filter = new GeneralFileFilter("xml>");
            filter.setDescription("XML Files");
            chooser.setFileFilter(filter);
            int returnVal = chooser.showSaveDialog(owningFrame);
            filePath = (returnVal == JFileChooser.APPROVE_OPTION) ? chooser.getSelectedFile().getAbsolutePath() : null;
    if (filePath == null) return false;
    if (filePath.indexOf('.') < 0)
        filePath = filePath + ".xml";
 
    priorPath = chooser.getCurrentDirectory();
    return true;
 }
 
 // Choose an output file
 protected boolean selectInputFile(String filePrompt) {
    FileDialog theDialog = new FileDialog(owningFrame, filePrompt);
    theDialog.setVisible(true);
    filePath = theDialog.getDirectory() + theDialog.getFile();
    if (DEBUG) System.out.println("File path is " + filePath);
    if ("nullnull".equals(filePath)) filePath = null;
    return (filePath != null);
 }
 
 // Choose an output file
 protected boolean selectInputFile() {
    return selectInputFile("Select an XML file");
 }
 
 
 /**
   * Write the given object as XML to a solicited output destination
   * @param datum JavaBean containing data
   */
 public void writeXml(Object datum) {
   try {
     if (selectOutputFile()) {
       if (filePath == null) {
         System.out.println("no file path");
         return;
       }
       writeXml(datum, filePath);
     }
   } catch (Exception e) {
     e.printStackTrace();
   }
 }
 
 /**

   * Write the given object as XML to the given file
   * @param datum JavaBean containing data
   * @param filePath Path to output file
   */
 public void writeXml(Object datum, String filePath) {
   try {
       Properties p = loadProperties(datum);
       writeXml(datum, filePath, p);
   } catch (Exception e) {
     e.printStackTrace();
   }
 }
 
 /**
   * Write the given object as XML to the given file
   * @param datum JavaBean containing data
     * @param useXml name to replace datum for Xml.properties
   */
   public void writeXmlFrom(Object datum, String useXml) {
   try {
         if (selectOutputFile()) {
           if (filePath == null) {
             System.out.println("no file path");
             return;
           }
           writeXml(datum, filePath, loadProperties(datum, useXml));
     }
   } catch (Exception e) {
     e.printStackTrace();
   }
 }
 
 /**
   * Write the given object as XML to the given file
   * @param datum JavaBean containing data
   * @param filePath Path to output file
       * @param p Properties to condition transformation to XML
       */
     public void writeXml(Object datum, String filePath, Properties p) throws Exception {
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
 
       File outputFile = new File(filePath);
       if (DEBUG) System.out.println("Output is " + outputFile.getAbsolutePath());
       PrintWriter printer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(outputFile), "UTF-8"));
       XmlWriter output = new XmlWriter(printer);
 
       output.writeXmlVersion("1.0", "UTF-8", null);
       String[] itsList = mainTag.split("\\s*,\\s*");
       String it = null;
       if (itsList.length > 0) {
         output.writeEntity(itsList[0]);
         for (int i=1; i < itsList.length; i++) {
           it = itsList[i];
           if ("noNamespace".equals(it)) {
             it = itsList[++i];
             output.writeAttribute("xsi:noNamespaceSchemaLocation", it);
             output.writeAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
           } else if (++i < itsList.length) {
               output.writeAttribute(it, itsList[i]);
           }
         }
         writeToXml(datum, p, output);
         output.endEntity();
       }
       output.close();
       printer.close();
 }
 
 /**
   * Select and read a given XML file into the given object class
   * @param datum JavaBean prototype
   */
 public Object readXml(Object datum) {
   try {
     selectInputFile();
     if (filePath != null) {
       System.out.println("reading " + filePath);
       return readXml(datum, filePath);
     }
   } catch (Exception e) {
     e.printStackTrace();
   }
   return null;
 }
 
 /**
   * Select and read a given XML file into the given object class,
   * if a file is selected for the prompt
   * @param datum JavaBean prototype
   * @param filePrompt Text for file selection dialog
   * @return the loaded instance or null

   */
 public Object readSelectedXml(Object datum, String filePrompt) {
   Object result = null;
   try {
     if (selectInputFile(filePrompt)) {
       System.out.println("reading " + filePath);
       result = readXml(datum, filePath);
     }
   } catch (Exception e) {
     e.printStackTrace();
   }
   return result;
 }
 
 /**
   * Select and read a given XML file into the given object class
   * @param datum JavaBean prototype
   */
 public Object readXml(Object datum, String filePath) {
   Object result = null;
   XmlStructure theStructure = null;
   File inputFile = null;
   if (filePath != null) theStructure = buildXmlStructure(datum);
   if (theStructure != null) {
     CustomXmlParser theParser = new CustomXmlParser();
     inputFile = new File(filePath);
     result = theParser.parseTheXml(datum, theStructure, inputFile);
   }
   if (result != null) {
     try {
       BeanUtils.setProperty(result, "sourceFile", inputFile.getAbsolutePath());
     } catch (Exception e) {
     }
   }
 return result;
}
 
 /**
   * Select and read a given XML file into the given object class
   * @param datum JavaBean prototype
   */
 public Object readXml(Object datum, InputStream theFile) {
   Object result = null;
   XmlStructure theStructure = buildXmlStructure(datum);
   if (theStructure != null) {
     CustomXmlParser theParser = new CustomXmlParser();
     result = theParser.parseTheXml(datum, theStructure, theFile);
   }
 return result;
}
 
 /**
   * Load the properties for an object
   * @param datum the object
   */
 public Properties loadProperties(Object datum) throws Exception {
   Properties result = null;
   String datumName = datum.getClass().getName();
   String className = null;
   int lastIndex = datumName.lastIndexOf(".");
   if (lastIndex > 0) className = datumName.substring(lastIndex+1);
 
     // Prepare the property file name, and check the cache
   if (!propertyCache.containsKey(className)) {
 
     StringBuffer buildName = new StringBuffer(datumName.substring(0,lastIndex+1).replace('.', '/'));
     buildName.append(className).append("Xml.properties");
 
     result = new Properties();
     try {
       InputStream f = datum.getClass().getClassLoader().getResourceAsStream(buildName.toString());
       if (f != null) {
         result.load(f);
         f.close();
       }
     } catch (Exception e) {
         System.out.println("*** No properties found for " + className);
     }
 
     propertyCache.put(className, result);
   }
   return (Properties) propertyCache.get(className);
 }
 
 /**
   * Load the properties for an object
   * @param datum the object
   * @param useName name suffix to select properties file
   */
 public Properties loadProperties(Object datum, String useName) throws Exception {
   Properties result = null;
   String datumName = datum.getClass().getName();
   String className = null;
   String propertyName = null;
   int lastIndex = datumName.lastIndexOf(".");
   if (lastIndex > 0) {
       className = datumName.substring(lastIndex+1);
       propertyName = datumName.substring(0, lastIndex+1) + useName;
   }
 
     // Prepare the property file name, and check the cache
   if (!propertyCache.containsKey(propertyName)) {
 
     StringBuffer buildName = new StringBuffer(datumName.substring(0,lastIndex+1).replace('.', '/'));
     buildName.append(useName).append("Xml.properties");
 
     result = new Properties();
     try {
       InputStream f = datum.getClass().getClassLoader().getResourceAsStream(buildName.toString());
       if (f != null) {
         result.load(f);
         f.close();
       }
     } catch (Exception e) {

         System.out.println("*** No properties found for " + propertyName);
     }
 
     propertyCache.put(propertyName, result);
   }
   return (Properties) propertyCache.get(propertyName);
 }
 
 /**
   * Apply null and unknown rules
   * @param itsName name of the attribute
   * @param integerNames numbers have different rules
   * @param datum the object in question
   * @return String the value after any applicable rules have been applied
   */
 public String defaultedValue(String itsName, Set integerNames, Object datum, String conversion) {
   String itsValue = null;
   try {
     itsValue = BeanUtils.getProperty(datum, itsName);
     if (itsValue == null) itsValue = "null";
     if (conversion != null) itsValue = convertOutputValue(conversion, itsValue);
     if (!integerNames.contains(itsName)) {
       if (itsValue.equals("-9999")) itsValue = "unknown";
     } else {
       if (itsValue.startsWith("-9999")) itsValue = "-9999";
       if (itsValue.endsWith(".0")) itsValue = itsValue.substring(0, itsValue.length() - 2);
     }
   } catch (Exception e) {    }
   return itsValue;
 }
 
 /**
   * Check if a value is present for an atribute
   * @param itsName the attribute name
   * @param datum the object which may have the data
   * @return boolean true if there is data
   */
 public boolean hasValue(String itsName, Object datum) {
   String itsValue = null;
   try {
     itsValue = BeanUtils.getProperty(datum, itsName);
   } catch (Exception e) {    }
   return (itsValue != null && !("null".equals(itsValue)));
 }
 
 /**
   * Convert a coded value on output
   * @param conversion list definition for conversion
   * @param source string to convert
   * @return String converted value
   */
 public String convertOutputValue(String conversion, String source) {
   if (conversion != null && source != null) {
     String[] iter = conversion.split("\\s*,\\s*");
     if (iter.length > 0) {
       String mode = iter[0];
       if ("localPairs".equals(mode)) {
         for (int i=1; i < iter.length; i++) {
           String thisValue = iter[i];
           if (++i < iter.length && source.equals(iter[i])) {
             return thisValue;
           }
         }
       }
     }
   }
   return source;
 }
 
 /**
   * Use the properties file to write an XML file for the object
   * @param datum JavaBean identifying the table properties file
   * @param p Properties file
   * @param output PrintWriter for the output
   */
 public int writeToXml(Object datum, Properties p, XmlWriter output) {
   String it = null;
   String itsName = null;
   String itsValue = null;
   String[] stringPart;
   String propertyText = null;
   String objectName = null;
   String[] tagsInOrder;
   Set integerNames = new HashSet();
   try {
     Map datumMap = BeanUtils.describe(datum);
     propertyText = p.getProperty("defaulttags");
     if (propertyText != null) {
       stringPart = propertyText.split("\\s*,\\s*");
       for (int i=0; i < stringPart.length - 1; i++) {
           output.writeEntityWithText(stringPart[i], stringPart[++i]);
       }
     }
     objectName = p.getProperty("object");
     if (objectName != null) {
       stringPart = objectName.split("\\s*,\\s*");
       objectName = stringPart[0];
       output.writeEntity(objectName);
       for (int i=1; i < stringPart.length; i++) {
         it = stringPart[i];

         itsValue = (++i < stringPart.length) ? stringPart[i] : null;
         if (itsValue != null) {
           String convertIt = p.getProperty(itsValue);
           if (datumMap.containsKey(itsValue)) itsValue = BeanUtils.getProperty(datum, itsValue);
           if (itsValue != null) output.writeAttribute(it, convertOutputValue(convertIt, itsValue));
         }
       }
     }
 
     propertyText = p.getProperty("integers");
     if (propertyText != null) {
       stringPart = propertyText.split("\\s*,\\s*");
       for (int i=0; i < stringPart.length; i++) {
         integerNames.add(stringPart[i]);
       }
     }
 
     propertyText = p.getProperty("objecttags");
     if (propertyText == null) return 0;
 
     tagsInOrder = propertyText.split("\\s*,\\s*");
     for (int i=0; i<tagsInOrder.length; i++) {
       itsName = tagsInOrder[i];
       propertyText = p.getProperty(itsName);
       if (propertyText == null) continue;
       stringPart = propertyText.split("\\s*,\\s*");
       if (stringPart.length > 0) {
         it = stringPart[0];
         if ("tag".equals(it)) {
           if (datumMap.containsKey(itsName) && hasValue(itsName, datum)) {
             it = stringPart[1];
             output.writeEntity(it);
             String conversion = null;
             for (int j=2; j < stringPart.length; j++) {
               it = stringPart[j];
               if (++j < stringPart.length) {
               itsValue = stringPart[j];
               if (itsValue != null) {
               String convertIt = p.getProperty(itsValue);
                 if (datumMap.containsKey(itsValue))
                   itsValue = BeanUtils.getProperty(datum, itsValue);
                 if (itsValue != null)
                   output.writeAttribute(it,
                                         convertOutputValue(convertIt, itsValue));
               }
               } else { // There's an unpaired element - check for body conversion
                 int keyIndex = propertyText.indexOf("localPairs");
                 if (keyIndex > 0) conversion = propertyText.substring(keyIndex);
               }
             }
             output.writeText(defaultedValue(itsName, integerNames, datum, null));
             output.endEntity();
           }
         } else if ("body".equals(it)) {
           if (datumMap.containsKey(itsName)) {
             output.writeText(defaultedValue(itsName, integerNames, datum, null));
           }
         } else if ("tagselect".equals(it)) {
           it = stringPart[1];
           itsValue = BeanUtils.getProperty(datum, itsName);
           if (itsValue != null && datumMap.containsKey(itsName)) {
             if (itsValue.startsWith("-9999")) {
               itsValue = "unknown";
             } else {
               itsValue = GeneralDataProcessor.getCachedLabel(it, itsValue, datum);
             }
             it = stringPart[2];
             output.writeEntityWithText(it, itsValue);
           }
         } else if ("collection".equals(it)) {
           Object theCollection = getNamedProperty(datum, itsName);
           Iterator overCollection = getCollection(theCollection);
           String ifName = (stringPart.length > 2) ? stringPart[2] : null;
           // Iterate over the collection: for each member with an Xml.properties, call writeToXml
           while (overCollection != null && overCollection.hasNext()) {
             Object newDatum = overCollection.next();
             if (newDatum instanceof String || newDatum instanceof Integer) {
                 newDatum = ((Map) theCollection).get(newDatum);
             }
             boolean doThis = (ifName == null);
             if (!doThis) {
                Object doWhat = BeanUtils.getProperty(newDatum, ifName);
                doThis = (doWhat != null && ((doWhat instanceof String) && ((String) doWhat).equals("true"))
                                || (doWhat instanceof Boolean) && ((Boolean) doWhat.equals(true)));
             }
             if (doThis) {
                     Properties newProperties = loadProperties(newDatum);
                      if (newProperties.getProperty("object") != null) {
                        writeToXml(newDatum, newProperties, output);
                      } else if (newDatum instanceof java.util.Date){
                        output.writeEntityWithText(itsName, StringUtil.toJdbcDate((java.util.Date) newDatum));
                      } else {
                        output.writeEntityWithText(itsName, newDatum.toString());
                      }
             }
           }
         } else if ("object".equals(it)) {
           Object theObject = getNamedProperty(datum, itsName);
           if (theObject != null) {
             Properties newProperties = loadProperties(theObject);

             if (newProperties.getProperty("object") != null) {
               output.writeEntity(itsName);
               writeToXml(theObject, newProperties, output);
               output.endEntity();
             }
           }
         }
       }
     }
     if (objectName != null) output.endEntity();
   } catch (Exception e) {
     e.printStackTrace();
   }
   return 1;
 }
 
 
 /**
   * Use the properties file to build parsing structures for an input file
   * @param datum JavaBean identifying the table properties file
   * @return XmlStructure support structure for the parser
   */
 private String extrapolateClassName(Object datum, String value) {
          String result = datum.getClass().getName();
     if (value.indexOf('.') < 0) {
       int lastIndex = result.lastIndexOf(".");
       if (lastIndex > 0) result = result.substring(0, lastIndex+1) + value;
     } else {
         result = value;
     }
          return result;
 }
 
 
 /**
   * Use the properties file to build parsing structures for an input file
   * @param datum JavaBean identifying the table properties file
   * @return XmlStructure support structure for the parser
   */
 public XmlStructure buildXmlStructure(Object datum) {
   if (DEBUG) System.out.println("Entering buildXmlStructure with " + datum.toString());
   XmlStructure theStructure = new XmlStructure();
   if (datum == null) return theStructure;
   String it = null;
   String itsName = null;
   String itsValue = null;
   String stringPart[];
   String propertyText = null;
   String headName = null;
   String[] tagsInOrder;
   Map tags = theStructure.getTags();
   Map attributes = theStructure.getAttributes();
   Properties p = null;
   try {
     Map datumMap = BeanUtils.describe(datum);
     p = loadProperties(datum);
     propertyText = p.getProperty("object");
     if (propertyText == null) {
       propertyText = p.getProperty("maintag");
     }
     if (propertyText == null) return theStructure;
 
     stringPart = propertyText.split("\\s*,\\s*");
     if (stringPart.length > 0) {
       headName = stringPart[0];
       tags.put(headName, new XmlAttribute("head", headName));
       for (int i = 1; i < stringPart.length; i++) {
         itsValue = stringPart[i];
         if (++i < stringPart.length) {
           itsName = stringPart[i];
           if (datumMap.containsKey(itsName)) {
             XmlAttribute newAttribute = new XmlAttribute("attr", itsValue);
             newAttribute.setDataName(itsName);
             itsName = p.getProperty(itsName);
             newAttribute.setConvertName(itsName);
             attributes.put(headName + "." + itsValue, newAttribute);
           }
         }
       }
     } else {
       if (DEBUG) System.out.println("No tag name in object property - skipped");
       return theStructure;
     }
 
     propertyText = p.getProperty("objecttags");
     if (propertyText == null) return theStructure;
 
     if (DEBUG) System.out.println("Found object tag list " + propertyText);
     tagsInOrder = propertyText.split("\\s*,\\s*");
 
     XmlAttribute theAttribute = null;
     for (int i=0; i<tagsInOrder.length; i++) {
       itsName = tagsInOrder[i];
       propertyText = p.getProperty(itsName);
       if (propertyText == null) propertyText = "";
       stringPart = propertyText.split("\\s*,\\s*");
       if (stringPart.length > 0) {
         it = stringPart[0];
         if ("tag".equals(it)) {
             if (stringPart.length > 1) {

                 it = stringPart[1];
                 theAttribute = new XmlAttribute("tag", it);
                 theAttribute.setDataName(itsName);
             }
         } else if ("tagdate".equals(it)) {
             if (stringPart.length > 1) {
                 it = stringPart[1];
                 theAttribute = new XmlAttribute("tagdate", it);
                 theAttribute.setDataName(itsName);
             }
         } else if ("attr".equals(it)) {
             if (stringPart.length > 1) {
                 it = stringPart[1];
             theAttribute = new XmlAttribute("attr", it);
             if (stringPart.length > 2) theAttribute.setClassName(stringPart[2]);
             if (stringPart.length > 3) theAttribute.setDataName(stringPart[3]);
           }
         } else if ("body".equals(it)) {
           theAttribute = new XmlAttribute("body", "body");
           theAttribute.setDataName(itsName);
         } else if ("tagselect".equals(it)) {
           if (stringPart.length > 1) {
             itsValue = stringPart[1];
             if (stringPart.length > 2) {
               it = stringPart[2];
               theAttribute = new XmlAttribute("tagselect", it);
               theAttribute.setDataName(itsName);
               theAttribute.setClassName(itsValue);
             }
           }
         } else if ("collection".equals(it)) {
             if (stringPart.length > 1) {
               itsValue = stringPart[1];
               if (itsValue.length() > 0) {
                 String className = extrapolateClassName(datum, itsValue);
                 if (DEBUG) System.out.println("Loading properties for " + itsValue);
                 Object subDatum = Class.forName(className).newInstance();
                 XmlStructure subStructure = buildXmlStructure(subDatum);
                 theAttribute = new XmlAttribute("tagselect", subStructure.findTagnameForType("head"));
                 theAttribute.setDataName(itsName);
                 theAttribute.setClassName(itsValue);
                 theAttribute.setChildStructure(subStructure);
               } else if (stringPart.length > 2) {
                        itsValue = stringPart[2];
                        theAttribute = new XmlAttribute("tag", itsName);
                        theAttribute.setDataName(itsValue);
                        if (stringPart.length > 3) {
                                itsValue = stringPart[3];
                                theAttribute.setConvertName(itsValue);
                        }
               }
             } else {
                 theAttribute = new XmlAttribute("tag", itsName);
                 theAttribute.setDataName(itsName);
             }
         } else if ("object".equals(it)) {
             if (stringPart.length > 1) {
               itsValue = stringPart[1];
               String className = extrapolateClassName(datum, itsValue);
               if (DEBUG) System.out.println("Loading properties for " + itsValue);
               Object subDatum = Class.forName(className).newInstance();
               XmlStructure subStructure = buildXmlStructure(subDatum);
               theAttribute = new XmlAttribute("tagobject", itsName);
               theAttribute.setDataName(itsName);
               theAttribute.setClassName(itsValue);
               theAttribute.setChildStructure(subStructure);
             } else {
                 theAttribute = new XmlAttribute("tag", itsName);
                 theAttribute.setDataName(itsName);
             }
         }
         if (theAttribute != null) {
           tags.put(theAttribute.getTagName(), theAttribute);
           theAttribute = null;
         }
       }
     }
   } catch (Exception e) {
     e.printStackTrace();
   }
   return theStructure;
 }
 
 
 /**
   * <p>Retrieve the named property from an object,
   * introspecting and caching properties the first time a particular bean class
   * is encountered.</p>
   * <p>Just return null if anything goes wrong</p>
   *
   * @param bean Bean for which property is requested
   * @param name the property name
   * @return Object the property if it is found or null
   */
 public Object getNamedProperty(Object bean, String name) {
     Object result = null;
     if (bean != null && name != null) {
         try {
         result = PropertyUtils.getProperty(bean, name);
         }

       catch (Exception e) {
         if (DEBUG)
           System.out.println("Get property failed " + e.toString());
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
 
}

