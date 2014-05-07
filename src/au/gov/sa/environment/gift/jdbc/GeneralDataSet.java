package au.gov.sa.environment.gift.jdbc;
 
import java.sql.*;
import java.util.*;
 
import java.io.*;
 
import org.apache.commons.beanutils.BeanUtils;
 
/**
 * <p>Title: Represents a dataset for the generic database processor</p>
 * <p>Description: Driven by the property file for its JavaBean</p>
 * <p >Copyright: Copyright (c) 2003</p>
 * <p>Company: DEH</p>
 * @author Peter Davey
 * @version 1.1
 */
 
public class GeneralDataSet {
 
 protected static final Map datasetCache = new HashMap();
 protected static final Map propertyCache = new HashMap();
 
 protected Object datum = null;
 
 protected String tableName = null;
 protected String keyName = null;
 protected String byName = null;
 protected String dataName = null;
 protected String changeName = null;
 protected Set integers = null;
 protected Set floats = null;
 protected Set blobs = null;
 protected Set bytes = null;
 protected Map attributes = null;
 
 protected String insertName = null;
 protected String updateName = null;
 protected String keyStatement = null;
 protected String keyOrder = null;
 protected List keyNames = new ArrayList();
 
 protected List byNames = new ArrayList();
 protected Map contains = new HashMap();
 
 protected String filePath;
 
 // Simple constructor
 public GeneralDataSet() {
 }
 
 
 // Constructor with an object to identify the properties
 public GeneralDataSet(Object datum) {
   this.datum = datum;
   loadProperties(datum);
 }
 
 /**
   * Load the properties for the object
   * @param conn Connection object identifying the table properties file
   */
 public void prepareForConnection(Connection conn) throws Exception {
   throw new Exception("Unimplemented function");
 }
 
 /**
   * Run the SELECT BY statement and load the data into its map
   * @param datum object of the class to load db data into
   * @param dataSet list to put the objects into
   * @param keys values to use for the keys
   */
 public void executeSelectKey(Object datum, List dataSet, Set keys, GeneralDataProcessor overseer) throws Exception {
   throw new Exception("Unimplemented function");
 }
 
 /**
   * Load the properties for an object
   * @param datum the reference object
   */
 public void loadDataProperties(String name, Object datum) throws Exception {
   // Get the properties set
   Properties p;
   if (name == null) {
     p = loadDbProperties(datum);
   } else {
     p = loadDbProperties(name, null, datum);
   }
 
   attributes = new TreeMap();
   integers = new TreeSet();
   floats = new TreeSet();
   blobs = new TreeSet();
   bytes = new TreeSet();
   dataName = null;
   keyName = null;
   keyStatement = null;
   keyOrder = null;
   keyNames.clear();
   contains.clear();

   byName = null;
   StringTokenizer itsList = null;
   if (p != null) {
      for (Enumeration psm = p.propertyNames() ; psm.hasMoreElements() ;) {
         String itsName = (String) psm.nextElement();
         String it = p.getProperty(itsName);
         if ("tableName".equals(itsName)) {
           tableName = it;
         } else if ("keyName".equals(itsName)) {
           keyName = it;
         } else if ("keyStatement".equals(itsName)) {
           keyStatement = it;
         } else if ("keyOrder".equals(itsName)) {
           keyOrder = it;
         } else if ("keyAttributes".equals(itsName)) {
           itsList = new StringTokenizer(it, ", ");
           while (itsList.hasMoreTokens()) {
             keyNames.add(itsList.nextToken());
           }
         } else if ("byName".equals(itsName)) {
           byName = it;
         } else if ("dataName".equals(itsName)) {
             dataName = it;
         } else if ("changeDate".equals(itsName)) {
             changeName = it;
         } else if ("insert".equals(itsName)) {
           insertName = it;
         } else if ("update".equals(itsName)) {
           updateName = it;
         } else if ("intFields".equals(itsName)) {
           itsList = new StringTokenizer(it, ", ");
           while (itsList.hasMoreTokens()) {
             integers.add(itsList.nextToken());
           }
         } else if ("floatFields".equals(itsName)) {
             itsList = new StringTokenizer(it, ", ");
             while (itsList.hasMoreTokens()) {
               floats.add(itsList.nextToken());
             }
         } else if ("byteFields".equals(itsName)) {
           itsList = new StringTokenizer(it, ", ");
           while (itsList.hasMoreTokens()) {
             bytes.add(itsList.nextToken());
           }
         } else if ("blobFields".equals(itsName)) {
           itsList = new StringTokenizer(it, ", ");
           while (itsList.hasMoreTokens()) {
             blobs.add(itsList.nextToken());
           }
         } else if ("contains".equals(itsName)) {
           itsList = new StringTokenizer(it, ", ");
           while (itsList.hasMoreTokens()) {
             String containsAttribute = itsList.nextToken();
             if (itsList.hasMoreTokens()) {
               contains.put(containsAttribute, itsList.nextToken());
             }
           }
         } else {
           attributes.put(itsName, it);
         }
      }
   }
 }
 
 /**
   * Load the properties for the object
   * @param datum JavaBean identifying the table properties file
   */
 public void loadProperties(Object datum) {
 
   try {
     loadDataProperties(null, datum);
   } catch (Exception e) {
     e.printStackTrace();
   }
 }
 
 /**
   * Apply data from resultSet to datum and return name string
   * @return String the value of dataName in the updated datum
   */
 public String applyData(Object datum, ResultSet rows) {
   String result = null;
   Iterator it = attributes.keySet().iterator();
   String attName = null;
   int paramIndex = 1;
   try {
     while (it.hasNext()) {
       attName = (String) it.next();
       if (bytes.contains(attName)) {
           BeanUtils.setProperty(datum, attName, rows.getBytes(paramIndex));           
       } else {
         BeanUtils.setProperty(datum, attName, rows.getString(paramIndex));
       }
       paramIndex += 1;
     }
     result = BeanUtils.getProperty(datum, dataName);
   } catch (Exception e) {
     e.printStackTrace();
   }

   return result;
 }
 
 /**
   * Utility to string-format a Set
   * @param dataSet map to put the objects into
   * @return String display form of Set
   */
 public Set getKeys() {
   return new HashSet(keyNames);
 }
 
 /**
   * Utility to string-format a Set
   * @param dataSet map to put the objects into
   * @return String display form of Set
   */
 public String stringKeys(Set keys) throws Exception {
   StringBuffer result = new StringBuffer();
   String connector = "";
   Iterator it = keys.iterator();
   while (it.hasNext()) {
     Object theObject = it.next();
     result.append(connector).append(theObject.toString());
     connector = ", ";
   }
   return result.toString();
 }
 
 public String getTableName () {
   return tableName;
 }
 
 public void setTableName(String tableName) {
   this.tableName = tableName;
 }
 
 public String getKeyName () {
   return keyName;
 }
 
 public boolean isIntegerKey () {
   return integers.contains(keyName);
 }
 
 public String getInsertName () {
   return insertName;
 }
 
 public String getUpdateName () {
   return updateName;
 }
 
 public void setKeyName(String keyName) {
   this.keyName = keyName;
 }
 
 public String getByName () {
   return byName;
 }
 
 public void setByName(String byName) {
   this.byName = byName;
 }
 
 public String getDataName () {
   return dataName;
 }
 
 public void setDataName(String dataName) {
   this.dataName = dataName;
 }
 
 public Map getContains () {
   return contains;
 }
 
 /**
   * Read a properties file.
   * @param name String identifying the properties file
   * @param suffix String identifying the properties file
   * @return Properties the properties file
   */
 public static Properties loadDbProperties(String name, String suffix, Object datum) {
   // Prepare the property file name, and check the cache
   String propertyName = name;
   if (suffix != null && suffix.length() > 0) {
     propertyName = name + suffix;
   }
   if (propertyCache.containsKey(propertyName)) {
     return (Properties) propertyCache.get(propertyName);
   }
 
   StringBuffer buildName = new StringBuffer();
   String resourceName = datum.getClass().getName();
   int lastIndex = resourceName.lastIndexOf(".");
   resourceName = resourceName.substring(0,lastIndex+1).replace('.', '/');
   buildName.append(resourceName);
 
   // check if there's a properties file

   buildName.append(propertyName).append(".properties");
 
   InputStream f = null;
   // Start with the default properties set
   Properties p = new Properties();
   try {
     f = datum.getClass().getClassLoader().getResourceAsStream(buildName.toString());
     if (f != null) {
       // Get the properties
       p.load(f);
       f.close();
       propertyCache.put(propertyName, p);
     }
   } catch (Exception e) {
     System.out.println("*** No properties found for " + name);
     e.printStackTrace();
   }
   return p;
 }
 
 /**
   * Load the properties for the object
   * @param datum JavaBean identifying the table properties file
   */
  public static Properties loadDbProperties(Object datum) {
 
   String className = datum.getClass().getName();
   int lastIndex = className.lastIndexOf(".");
   if (lastIndex > 0) className = className.substring(lastIndex+1);
   return loadDbProperties(className, null, datum);
 }
 
 /**
   * Load the properties for the object
   * @param datum JavaBean identifying the table properties file
   */
 public static GeneralDataSet getDataset(Object datum) {
 
   if (datum == null) return null;
   
   String className = datum.getClass().getName();
   int lastIndex = className.lastIndexOf(".");
   if (lastIndex > 0) className = className.substring(lastIndex+1);
 
   GeneralDataSet dataSet = null;
   if (datasetCache.containsKey(className)) {
     dataSet = (GeneralDataSet) datasetCache.get(className);
   } else {
     dataSet = new JdbcDataSet(datum);
     datasetCache.put(className, dataSet);
   }
   return dataSet;
 }
 
 /**
   * Load the properties for the object
   * @param className String identifying the table properties file
   * @param datum JavaBean for the classpath
   */
 public static GeneralDataSet getNamedDataset(String className, Object datum) {
 
   GeneralDataSet dataSet = null;
   if (datasetCache.containsKey(className)) {
     dataSet = (GeneralDataSet) datasetCache.get(className);
   } else {
     dataSet = new JdbcDataSet(className, datum);
     datasetCache.put(className, dataSet);
   }
   return dataSet;
 }
 
 /**
   * Load a fresh set of properties for the object
   * @param datum JavaBean classname identifies the table properties file
   */
 public static JdbcDataSet getEjbDataset(Object datum) {
 
   String className = datum.getClass().getName();
   int lastIndex = className.lastIndexOf(".");
   if (lastIndex > 0) className = className.substring(lastIndex+1);
 
   return new JdbcDataSet(className, datum);
 }
 
}
 

