package au.gov.sa.environment.gift.jdbc;
 
import java.util.*;
import java.lang.reflect.*;
import java.text.*;
 
import java.io.*;
 
import org.apache.commons.beanutils.BeanUtils;
 
import au.gov.sa.environment.gift.http.*;
 
/**
 * <p>Title: Generic database processor</p>
 * <p>Description: Driven by property files named for the JavaBeans it's handed</p>
 * <p >Copyright: Copyright (c) 2003</p>
 * <p>Company: DEH</p>
 * @author Peter Davey
 * @version 1.1
 */
 
public class GeneralDataProcessor {
 
 protected static final Map staticLvCache = new HashMap();
 protected static final Map staticNameCache = new HashMap();
 protected static final Map staticValueCache = new HashMap();
 
 protected String accessLevel = null;
 protected String connectionName = null;
 protected String tableName = null;
 protected String keyName = null;
 protected String byName = null;
 protected String dataName = null;
 protected Set integers = null;
 protected Map attributes = null;
 protected GeneralDataSet dataSet = null;
 protected GeneralDataProcessor jdp = null;
 
 protected PrintWriter changeLog = null;
 
 static final SimpleDateFormat logDateFormat = new SimpleDateFormat("H-m-s dd/MM/yyyy");
 
 protected String filePath;
 protected String suffix;
 
 // Simple constructor
 public GeneralDataProcessor() {
 }
 
 
 // Constructor with suffix for database.properties
 public GeneralDataProcessor(String suffix) {
   this.suffix = suffix;
 }
 
 
 // Open a log file if not already open
 protected void writeChangeLog(String text, Object datum) {
   try {
     changeLog = new PrintWriter(new FileOutputStream("SAVEG_app.log", true), true);
     if (dataSet != null) {
       String keyNames = dataSet.getKeyName();
       StringBuffer keyText = new StringBuffer();
       StringTokenizer iter = new StringTokenizer(keyNames, ",");
       String sep = "keys:";
       while (iter.hasMoreTokens()) {
         String it = iter.nextToken();
         keyText.append(sep).append(it).append("=").append(BeanUtils.getProperty(datum, it));
         sep = ", ";
       }
       changeLog.println(logDateFormat.format(new java.util.Date()) + " " + text + " " + dataSet.getTableName() + ", key is " + keyText.toString());
 
     }
     changeLog.close();
   } catch (Exception e) {
     e.printStackTrace();
   }
 }
 
 
 /**
   * Return a List attribute from a data object
   * @param datum the object containing the attribute
   * @param attribute the attribute name
   * @return List the contained list if there is one
   */
 public List getContainedData(Object datum, String attribute) {
   GeneralDataSet dataSet = GeneralDataSet.getDataset(datum);
   List result = null;
   Map contains = dataSet.getContains();
   if (contains.containsKey(attribute)) {
     StringBuffer workName = new StringBuffer("get");
     workName.append(attribute.substring(0,1).toUpperCase()).append(attribute.substring(1));
     try {
       Class parentClass = datum.getClass();
       Method getMethod = parentClass.getMethod(workName.toString(), new Class[] {});
       Object currentField = getMethod.invoke(datum, new Object[] {});
       if (currentField instanceof java.util.List) {
           result = (java.util.List) currentField;
       } else if (currentField instanceof java.util.Map) {

           result = new ArrayList();
           Set keys = ((java.util.Map) currentField).keySet();
           for (Object it : keys) result.add(((java.util.Map) currentField).get(it));
       }
     } catch (Exception e) {
       e.printStackTrace();
     }
   }
   return result;
 }
 
 /**
   * Names any nested attributes for a data object
   * @param datum the object to load nested data into
   * @return List the contained list if there is one
   */
 public List getContainedNames(Object datum) {
   GeneralDataSet dataSet = GeneralDataSet.getDataset(datum);
   if (dataSet == null) return new ArrayList();
   Map contains = dataSet.getContains();
   return new ArrayList(contains.keySet());
 }
 
 /**
   * Returns an edit-safe version of a data object, retaining data lists
   * @param datum the object to be copied
   * @return Object the copy
   */
 public Object cloneDatum(Object datum) {
   Object result = null;
   try {
     // Use BeanUtils to copy the editable data
     result = BeanUtils.cloneBean(datum);
     // Now check for contained data and copy any lists to the clone
     GeneralDataSet dataSet = GeneralDataSet.getDataset(datum);
     String attribute = null;
     Class parentClass = datum.getClass();
     Method getMethod;
     Method setMethod;
     StringBuffer workName;
     Map contains = dataSet.getContains();
     Iterator it = contains.keySet().iterator();
     while (it.hasNext()) {
       attribute = (String) it.next();
       workName = new StringBuffer("et");
       workName.append(attribute.substring(0,1).toUpperCase()).append(attribute.substring(1));
       getMethod = parentClass.getMethod("g" + workName.toString(), new Class[] {});
       setMethod = parentClass.getMethod("s" + workName.toString(), new Class[] {Class.forName("java.util.List")});
       setMethod.invoke(result, new Object[] {getMethod.invoke(datum, new Object[] {})});
     }
   } catch (Exception e) {
     e.printStackTrace();
   }
   return result;
 }
 
 /**
   * Returns a new instance of a data object, containing new data lists
   * @param datum the object to be copied
   * @return Object the copy
   */
 public Object cloneNewDatum(Object datum) {
   Object result = null;
   try {
     // Use BeanUtils to copy the editable data
     result = BeanUtils.cloneBean(datum);
     // Fix up any cloned data that needs it
     GeneralDataSet dataSet = GeneralDataSet.getDataset(datum);
     String attribute = null;
     // Clear out the record key
     attribute = dataSet.getKeyName();
     if (dataSet.isIntegerKey()) {
       BeanUtils.setProperty(datum, attribute, "0");
     } else {
       BeanUtils.setProperty(datum, attribute, "");
     }
     List theList = null;
     Object currentData = null;
     Class parentClass = datum.getClass();
     Field reflectField = null;
     // Now check for contained data and replace any lists in the clone
     Map contains = dataSet.getContains();
     Iterator it = contains.keySet().iterator();
     while (it.hasNext()) {
       attribute = (String) it.next();
       StringBuffer workName = new StringBuffer("get");
       workName.append(attribute.substring(0,1).toUpperCase()).append(attribute.substring(1));
       Method getMethod = parentClass.getMethod(workName.toString(), new Class[] {});
       currentData = getMethod.invoke(datum, new Object[] {});
       if (currentData instanceof java.util.List) {
         workName.setCharAt(0, 's');
         Method setMethod = parentClass.getMethod(workName.toString(), new Class[] {Class.forName("java.util.List")});
         setMethod.invoke(datum, new Object[] {new ArrayList()});
       }
     }
   } catch (Exception e) {
     e.printStackTrace();
   }
   return result;
 }

 
 
 /**
   * Run any nested selects for a data object
   * @param datum the object to load nested data into
   */
 public void loadContainedData(Object datum) {
 }
 
 /**
   * Load a namelist from the instructions in its property file
   * @param name String identifying a <name>Select.properties file or 'datalist'
   * return int -1 if there's a problem, 0 if it's a static SELECT, 1 if it uses an attribute, otherwise key parameter count
   */
 public int checkNamelist(String name, Object datum) {
   // Pick off the easy conditions first
   if ("datalist".equals(name)) return 1;
   if ("listbox".equals(name)) return 1;
   if (staticLvCache.containsKey(name)) return 0;
 
   ArrayList keyAttributes = new ArrayList();
   String statement = null;
   Properties p = GeneralDataSet.loadDbProperties(name, "Select", datum);
 
   if (p.size() == 0) {
     return -1;
   }
  for (Enumeration psm = p.propertyNames() ; psm.hasMoreElements() ;) {
     String itsName = (String) psm.nextElement();
     String it = p.getProperty(itsName);
     StringTokenizer itsList = null;
     if ("attribute".equals(itsName)) {
       itsList = new StringTokenizer(it, ", ");
       while (itsList.hasMoreTokens()) keyAttributes.add(itsList.nextToken());
     } else if ("statement".equals(itsName)) {
       statement = it;
     }
   }
   if (statement == null) {
     return -1;
   }
   return keyAttributes.size();
 }
 /**
   * Decode a data reference into the property name for a WHERE clause insertion
   * @param datum JavaBean containing source data
   * @param name String containing attribute name or attribute name[length]
   * @return String "pick" followed by the value of the attribute
   */
 public String decodePick(Object datum, String name) {
   StringBuffer result = new StringBuffer("pick");
   int squarePos = name.indexOf('[');
   String namePart = name;
   String indexPart = null;
   try {
     if (squarePos > 0) {
       namePart = name.substring(0, squarePos);
       indexPart = name.substring(squarePos + 1);
       squarePos = indexPart.indexOf(']');
       if (squarePos > 0) indexPart = indexPart.substring(0, squarePos);
     }
     String attValue = BeanUtils.getProperty(datum, namePart);
     if (null == attValue) {
         System.out.println("**** GeneralDataProcessor.decodePick(): attrValue for " + namePart + " is NULL");
         attValue = "";
     }
     int takeLength = attValue.length();
     if (indexPart != null) {
       takeLength = Integer.parseInt(indexPart);
     }
     if (takeLength >= attValue.length()) {
       result.append(attValue);
     } else {
       result.append(attValue.substring(0, takeLength));
     }
   } catch (Exception e) {
     e.printStackTrace();
   }
   return result.toString();
 }
 /**
   * Decode a data reference into the property name for a WHERE clause  LIKE insertion
   * @param datum JavaBean containing source data
   * @param name String containing attribute name or attribute name[length]
   * @return String "like 'the value of the attribute%'"
   */
 public String decodeLike(Object datum, String name) {
   StringBuffer result = new StringBuffer("like '");
   int squarePos = name.indexOf('[');
   String namePart = name;
   String indexPart = null;
   try {
     if (squarePos > 0) {
       namePart = name.substring(0, squarePos);
       indexPart = name.substring(squarePos + 1);
       squarePos = indexPart.indexOf(']');
       if (squarePos > 0) indexPart = indexPart.substring(0, squarePos);
     }
     String attValue = BeanUtils.getProperty(datum, namePart);
     int takeLength = attValue.length();

     if (indexPart != null) {
       takeLength = Integer.parseInt(indexPart);
     }
     if (takeLength >= attValue.length()) {
       result.append(attValue);
     } else {
       result.append(attValue.substring(0, takeLength));
     }
   } catch (Exception e) {
     e.printStackTrace();
   }
   result.append("%'");
   return result.toString();
 }
 
 /**
   * Execute a named no-parameter method returning a string on the given object
   * @param datum JavaBean identifying the table properties file
   * @param methodName Name of the method
   */
 protected String execReflectMethod(Object datum, String methodName) {
   String result = null;
   try {
      Class itsClass = datum.getClass();
      Method itsMethod = itsClass.getMethod(methodName, (Class[]) null);
      result = (String) itsMethod.invoke(datum, (Object[]) null);
   } catch (Exception e) {
     e.printStackTrace();
   }
   return result;
 }
 
 /**
   * Load the database connection properties
   * @return Properties Contents of the database.properties file
   */
 public Properties loadDBProperties(String suffix) {
   // check if there's a properties file
   String className = this.getClass().getName();
   int lastIndex = className.lastIndexOf(".");
   if (lastIndex > 0) className = className.substring(0, lastIndex+1);
   StringBuffer resourceName = new StringBuffer(className.replace('.', File.separatorChar));
   resourceName.append("database.properties");
   if (suffix != null && suffix.length() > 0) resourceName.append(".").append(suffix);
   InputStream f = null;
   // Start with the default properties set
   Properties p = new Properties();
   try {
     // Check if there's an individual set for this queue
     f = this.getClass().getClassLoader().getResourceAsStream(resourceName.toString());
     if (f != null) {
        // and get it if there is
        p.load(f);
        f.close();
     }
   } catch(Exception e) {
     System.out.println("No database properties file found");
   }
   return p;
 }
 
 
 /**
   * Get a list of the names of cached lists
   * @return ArrayList the list of cached list names
   */
 public static List getCachedListNames() {
   return new ArrayList(staticNameCache.keySet());
 }
 
 /**
   * Get the label for a value in a cached list
   * @return String the value found
   */
 public static String getCachedLabel(String formatName, String forValue, Object datum) {
   String result = "Unknown";
   if (!staticLvCache.containsKey(formatName)) {
     Properties p = GeneralDataSet.loadDbProperties(formatName, null, datum);
     formatName = p.getProperty("list");
   }
   if (staticLvCache.containsKey(formatName)) {
     List values = (List) staticValueCache.get(formatName);
     List labels = (List) staticNameCache.get(formatName);
     int i = values.indexOf(forValue);
     if (i >= 0) result = (String) labels.get(i);
   }
   return result;
 }
 
 /**
   * Get the label for a value in a cached list
   * @return String the value found
   */
 public static String getCachedValue(String formatName, String forLabel, Object datum) {
   String result = null;
   if (!staticNameCache.containsKey(formatName)) {
     Properties p = GeneralDataSet.loadDbProperties(formatName, null, datum);
     formatName = p.getProperty("list");
   }
   if (staticNameCache.containsKey(formatName)) {

     if (forLabel == null || forLabel.length() == 0) return formatName;
     result = "Unknown";
     List values = (List) staticValueCache.get(formatName);
     List labels = (List) staticNameCache.get(formatName);
     int i = labels.indexOf(forLabel);
     if (i >= 0) result = (String) values.get(i);
   }
   return result;
 }
 
 /**
   * Get a cached name list
   * @param String the name of the list
   * @return ArrayList the cached list of labels for the name
   */
 public static List getCachedList(String listName) {
   List result = null;
   if (staticNameCache.containsKey(listName)) result = (List) staticNameCache.get(listName);
   return result;
 }
 
 /**
   * Get a cached label-value list
   * @param String the name of the list
   * @return ArrayList the cached list of LabelValueBeans for the name
   */
 public static List getCachedLvList(String listName) {
   List result = null;
   if (staticLvCache.containsKey(listName)) result = (List) staticLvCache.get(listName);
   return result;
 }
 
 /**
   * Get a cached value list
   * @param String the name of the list
   * @return List the cached list of labels for the name
   */
 public static List getCachedValues(String listName) {
   List result = null;
   if (staticValueCache.containsKey(listName)) result = (ArrayList) staticValueCache.get(listName);
   return result;
 }
 
 /**
   * Store a name list in the cache
   * @param String the name of the list
   * @param ArrayList the cached list of labels for the name
   */
 public static void setCachedList(String listName, List names) {
   staticNameCache.put(listName, names);
 }
 
 /**
   * Store a name list in the cache
   * @param listName the name of the list
   * @param beans the cached list of label-value beans for the name
   */
 public static void setCachedLvList(String listName, List beans) {
   staticLvCache.put(listName, beans);
 }
 
 /**
   * Store a value list in the cache
   * @param String the name of the list
   * @param ArrayList the cached list of labels for the name
   */
 public static void setCachedValues(String listName, List values) {
   staticValueCache.put(listName, values);
 }
 
 /**
   * Refresh a value list in the cache
   * @param String the name of the list
   * @param ArrayList the cached list of labels for the name
   */
 public static void refreshNamedList(String listName) {
   staticLvCache.remove(listName);
   staticNameCache.remove(listName);
   staticValueCache.remove(listName);
 }
 
 /**
   * Get the JDBC connection string for the database
   * @return String the JDBC connection string
   */
 public String getConnectionName() {
   return connectionName;
 }
 
 /**
   * Load a namelist from the instructions in its property file
   * Check parameter attributes - if any are null or 0, skip the lookup and return empty lists
   * @param datum JavaBean containing source data
   * @param name String identifying a <name>select.properties file
   * @param nameList List to hold first select result field
   * @param valueList List to hold second select result field
   */
 public void namelistUtility(Object datum, String name, List nameList, List valueList) {
   nameList.clear();
   valueList.clear();

   if (staticNameCache.containsKey(name)) {
       nameList.addAll((ArrayList) staticNameCache.get(name));
       valueList.addAll((ArrayList) staticValueCache.get(name));
   }
 }
 
 /**
   * Load codes from JDBC - if there is a JDBC connection, find CodeList.properties
   * in the same package as datum, read it, and pass the Properties to the JDBC handler
   * @param datum the reference object
   */
 public void loadCodesFromJdbc(Object datum) {
   JdbcDataProcessor jdp = JdbcDataProcessor.getJdp();
   if (jdp.getDBConnection() != null) {
     Properties p = GeneralDataSet.loadDbProperties("CodeList", null, datum);
     for (Enumeration psm = p.propertyNames() ; psm.hasMoreElements() ;) {
       String itsName = (String) psm.nextElement();
       String it = p.getProperty(itsName);
       if (!staticLvCache.containsKey(itsName)) {
         List lvList = new ArrayList();
         jdp.namelistUtility(datum, itsName, lvList);
       }
     }
   }
 }
 
 /**
   * Load a list from JDBC - if there is a JDBC connection, pass the call to it
   * This is where local list behaviour should go if there isn't a connection
   * @param datum the reference object
   * @param itsName name of the list
   * @param nameList for the labels
   * @param valueList for the values
   */
 public void loadListFromJdbc(Object datum, String itsName, List nameList, List valueList) {
   JdbcDataProcessor jdp = JdbcDataProcessor.getJdp();
   if (jdp.getDBConnection() != null) {
     jdp.namelistUtility(datum, itsName, nameList, valueList);
   }
 }
 
 /**
   * Load data from JDBC - if there is a JDBC connection, pass the call to it
   * This is where local behaviour should go if there isn't a connection
   * @param datum the reference object
   * @param itsName name of the list
   * @param nameList for the labels
   * @param valueList for the values
   */
 public void loadDataFromJdbc(Object datum, List resultList) {
   JdbcDataProcessor jdp = JdbcDataProcessor.getJdp();
   if (jdp.getDBConnection() != null) {
     try {
       jdp.databaseUtility(datum, resultList);
     } catch (Exception e) {
       e.printStackTrace();
     }
   }
 }
 
 /**
   * Get names of code tables to save to XML - find CodeList.properties
   * in the same package as datum, read it, and return the list of names that
   * have an xml parameter
   * @param datum the reference object
   */
 public List getCodesForXml(Object datum) {
   List result = new ArrayList();
   Properties p = GeneralDataSet.loadDbProperties("CodeList", null, datum);
   for (Enumeration psm = p.propertyNames() ; psm.hasMoreElements() ;) {
     String itsName = (String) psm.nextElement();
     String it = p.getProperty(itsName);
     if ("xml".equals(it)) result.add(itsName);
   }
   return result;
 }
 
 
 /**
   * Get names of available connections from the path of datum
   * If datum is null, use the GeneralDataProcessor
   * @param datum the reference object
   * @return List list of names for connections
   */
 public String getConnection() {
   EJBProxy proxy = new EJBProxy();
   BaseMessage theMessage = new BaseMessage();
   theMessage = proxy.performFunction(theMessage);
   accessLevel = theMessage.getAuthority();
   return theMessage.getDatabaseString();
 }
 
 public String getAccessLevel() {
   return accessLevel;
 }
 
 
 public void setAccessLevel(String accessLevel) {
   this.accessLevel = accessLevel;
 }

 
}

