package au.gov.sa.environment.gift.swing;
 
import java.awt.*;
import java.io.*;
import javax.swing.*;
import java.util.*;
 
import au.gov.sa.environment.gift.jdbc.*;
 
/**
 * <p>Title: AppFrameStructure</p>
 * <p>Description: Load frame structure from properties and express it</p>
 * <p>Copyright: Copyright (c) 2004</p>
 * <p>Company: DEH</p>
 * @author Peter Davey
 * @version 1.0
 */
 
public class AppFrameStructure {
 private static final Map frameCache = new HashMap(); // Frame Name = EFS
 private static final Map contentCache = new HashMap(); // Frame Name = Swing container
 private static final Map targetCache = new HashMap(); // Frame Name = Data object
 private static AppFrameStructure singleton = null;
 
 private Object datum;
 private ApplicationFrame frame;
 private GeneralDataProcessor overseer;
 private String firstName;
 
 /** Create a frame structure
   * datumFrame.properties provides the details.
   * <PRE>frames=Tasks
  *  class=EditTool
  *  action=preload
  *  first=Tasks
  *  package=au.gov.sa.environment.saveg.planner.edit
  * </PRE>
  * frames=comma-separated list of view names
  * class=comma-separated list of class names:
  *   for preload types, qualify with package
  *   for import types, qualify with view name=package
  *
  * @param   datum Object which has ?Frame.properties
  * @param   frame ApplicationFrame that owns us
  * @param   overseer For later development
   */
 public AppFrameStructure(Object datum, ApplicationFrame frame, GeneralDataProcessor overseer) {
   this.overseer = overseer;
   this.frame = frame;
   this.datum = datum;
   if (datum != null) {
     Properties p = loadFrameProperties(datum);
     makeFrameStructure(p);
   }
 }
 
 /**
   * Returns the next token from a list
   * Returns null for [no list] or [no more tokens]
   * @param content tokenizer on content
   * @return String the next token, or null if there isn't one
   */
 private static String tokenizerNext(StringTokenizer content) {
   String result = null;
   if (content != null) {
     if (content.hasMoreTokens()) {
       result = content.nextToken();
     }
   }
   return result;
 }
 
 /**
   * Sets the data object for a frame
   * @param name the name of the frame
   * @param target the data object
   */
 public void setFrameObject(String name, Object target) {
   targetCache.put(name, target);
 }
 
 /**
   * Gets the data object for a frame
   * @param name the name of the frame
   * @return Object the data object
   */
 public Object getFrameObject(String name) {
   if (targetCache.containsKey(name)) {
     return targetCache.get(name);
   }
   return null;
 }
 
 /**
   * Gets a new data object for a frame
   * Used in setup to make a preload or import fqcn
   * @param prefix the package prefix
   * @param className the object name
   */
 public Object loadDatumClass(String prefix, String className) {

   Object result = null;
   String fqcn = prefix + className;
   try {
    result = (datum != null && datum.getClass().getName().equals(fqcn)) ? datum : Class.forName(fqcn).newInstance();
   } catch (Exception e) {
     e.printStackTrace();
   }
   return result;
 }
 
 /**
   * Reads a properties file and constructs the frame structure
   * For each frame in the structure that is loaded at startup,
   * derives a class name, makes an object, and lets the
   * EntryFrameStructure construct the frame and its contents from
   * objectEntry.properties
   * @param p the properties
   */
 public void makeFrameStructure(Properties p) {
  String itsList = p.getProperty("frames");
   if (itsList == null) return;
   String packagePrefix = p.getProperty("package");
   if (packagePrefix == null) {
     packagePrefix = "";
   } else if (!packagePrefix.endsWith(".")) {
     packagePrefix = packagePrefix + ".";
   }
   firstName = p.getProperty("first");
   String itsName = null;
   String itsAction = null;
   String itsClass = null;
   String it = null;
   Object frameObject;
   EntryFrameStructure frameStructure;
   StringTokenizer entries = new StringTokenizer(itsList, ",");
   StringTokenizer classes = null;
   StringTokenizer actions = null;
   it = p.getProperty("class");
   if (it != null) classes = new StringTokenizer(it, ",");
   it = p.getProperty("action");
   if (it != null) actions = new StringTokenizer(it, ",");
 
   while (entries.hasMoreTokens()) {
     frameObject = null;
     frameStructure = null;
     itsName = entries.nextToken();
     itsClass = tokenizerNext(classes);
     itsAction = tokenizerNext(actions);
     if ("preload".equals(itsAction)) {
       frameObject = loadDatumClass(packagePrefix, itsClass);
     } else if ("import".equals(itsAction)) {
       String importPackage = p.getProperty(itsName);
       if (importPackage == null) importPackage = p.getProperty(itsClass);
       if (importPackage == null) importPackage = packagePrefix;
       if (!importPackage.endsWith(".")) importPackage = importPackage + ".";
       frameObject = loadDatumClass(importPackage, itsClass);
     }
     if (frameObject != null) {
       targetCache.put(itsName, frameObject);
       frameStructure = new EntryFrameStructure(frameObject, frame, overseer);
     }
     if (frameStructure != null) frameCache.put(itsName, frameStructure);
   }
 }
 
 
 public Iterator getFrames() {
   return frameCache.keySet().iterator();
 }
 
 public String getFirstName() {
   int commaPos = -1;
   if (firstName != null) commaPos = firstName.indexOf(",");
   if (commaPos < 0) return firstName;
   return firstName.substring(0, commaPos);
 }
 
 public String getFirstAction() {
   int commaPos = -1;
   if (firstName != null) commaPos = firstName.indexOf(",");
   if (commaPos < 0) return null;
   return firstName.substring(commaPos + 1);
 }
 
 /**
   * Get the frame structure for the first frame from the cache
   * @param frameName The name used in the frames= line of the properties
   * @return EntryFrameStructure the frame structure
   */
 public EntryFrameStructure getFirstFrame() {
   if (firstName == null) return null;
   return (EntryFrameStructure) frameCache.get(getFirstName());
 }
 
 /**
   * Get a frame structure from the cache
   * @param frameName The name used in the frames= line of the properties
   * @return EntryFrameStructure the frame structure
   */
 public EntryFrameStructure getNamedFrame(String frameName) {

   if (frameName == null) return null;
   return (EntryFrameStructure) frameCache.get(frameName);
 }
 
 /**
   * Get a frame structure from the cache
   * @param frameName The name used in the frames= line of the properties
   * @return EntryFrameStructure the frame structure
   */
 public EntryFrameStructure getObjectFrame(Object datum) {
   if (datum == null) return null;
   String useName = datum.getClass().getName();
   int lastIndex = useName.lastIndexOf(".");
   if (lastIndex > 0) useName = useName.substring(lastIndex+1);
 
   return (EntryFrameStructure) frameCache.get(useName);
 }
 
/**
   * Get the Swing structure for the frame, creating it if necessary
  * with the given parameters
   * @param datum the data object for the frame
   * @param frame The ApplicationFrame
   * @param overseer GeneralDataProcessor
   * @return AppFrameStructure the singleton instance of this class
   */
 public static AppFrameStructure getSingleton(Object datum, ApplicationFrame frame, GeneralDataProcessor overseer) {
   if (singleton == null) singleton = new AppFrameStructure(datum, frame, overseer);
   return singleton;
 }
 
/**
   * Get the singleton AppFrameStructure for this application
   * @return AppFrameStructure the singleton instance of this class
   */
 public static AppFrameStructure getSingleton() {
   return singleton;
 }
 
 /**
   * Get the Swing structure for the frame, creating it if necessary
   * @param frameStructure The EntryFrameStructure
   * @return Component the Swing structure
   */
 public Component getFrameContent(EntryFrameStructure frameStructure) {
   if (!contentCache.containsKey(frameStructure)) {
     JPanel frameBox = new JPanel();
     frameBox.setLayout(new BoxLayout(frameBox, BoxLayout.Y_AXIS));
     frameStructure.buildStructure(frameBox);
     contentCache.put(frameStructure, frameBox);
   }
   return (Component) contentCache.get(frameStructure);
 }
 
 /**
   * Get the Swing structure for the frame
   * @param frameStructure The EntryFrameStructure
   * @param inBox Container in which to build the UI components
   * @return Component the Swing structure
   */
 public Component getFrameContent(EntryFrameStructure frameStructure, Container inBox) {
   frameStructure.buildStructure(inBox);
   contentCache.put(frameStructure, inBox);
   return inBox;
 }
 
 /**
   * Get a frame structure from the cache
   * @param frameName The name used in the frames= line of the properties
   * @return EntryFrameStructure the frame structure
   */
 public EntryFrameStructure getFrame(Object frameName) {
   if (frameName == null) return null;
   return (EntryFrameStructure) frameCache.get(frameName);
 }
 
 /**
   * Read the frame setup properties
   * @param datum JavaBean identifying the menu properties file
   * @return Properties the frame setup properties file
   */
 public Properties loadFrameProperties(Object datum) {
 
   String resourceName = datum.getClass().getName();
   String className = null;
   int lastIndex = resourceName.lastIndexOf(".");
   if (lastIndex > 0) className = resourceName.substring(lastIndex+1);
 
   StringBuffer buildName = new StringBuffer();
   resourceName = resourceName.substring(0, lastIndex+1).replace('.', '/');
   buildName.append(resourceName);
 
   // check if there's a properties file
   buildName.append(className).append("Frame.properties");
   InputStream f = null;
   // Start with the default properties set
   Properties p = new Properties();
   try {
     f = this.getClass().getClassLoader().getResourceAsStream(buildName.toString());
     if (f != null) {

       // Get the properties
       p.load(f);
       f.close();
     }
   } catch (Exception e) {
     e.printStackTrace();
   }
   return p;
 }
 
}

