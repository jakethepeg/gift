package au.gov.sa.environment.gift.jdbc;
 
import java.util.*;
import java.sql.*;
 
import org.apache.commons.beanutils.BeanUtils;
 
/**
 * <p>Title: Represents a dataset for the generic database processor</p>
 * <p>Description: Driven by the property file for its JavaBean</p>
 * <p >Copyright: Copyright (c) 2003</p>
 * <p>Company: DEH</p>
 * @author Peter Davey
 * @version 1.1
 */
 
public class JdbcDataSet extends GeneralDataSet {
 
 private Connection conn = null;
 
 private PreparedStatement selectStatement = null;
 private PreparedStatement selectByStatement = null;
 private PreparedStatement selectCountStatement = null;
 private PreparedStatement insertStatement = null;
 private PreparedStatement updateStatement = null;
 private Statement utilityStatement = null;
 
 // Simple constructor
 public JdbcDataSet() {
 }
 
 
 // Constructor with an object to identify the properties
 public JdbcDataSet(Object datum) {
   this.datum = datum;
   loadProperties(null, datum);
 }
 
 // Constructor with an object to identify the properties
 public JdbcDataSet(String propertyName, Object datum) {
   this.datum = datum;
   loadProperties(propertyName, datum);
 }
 
 /**
   * Load the properties for the object
   * @param datum JavaBean identifying the table properties file
   */
 public void loadProperties(String name, Object datum) {
   try {
     loadDataProperties(name, datum);
   } catch (Exception e) {
     e.printStackTrace();
   }
 }
 
 /**
   * Load the properties for the object
   * @param datum JavaBean identifying the table properties file
   */
 public void prepareForConnection(Connection conn) {
 
   if (this.conn != conn) {
     try {
     if (selectStatement != null) selectStatement.close();
     if (selectCountStatement != null) selectCountStatement.close();
     if (insertStatement != null) insertStatement.close();
     if (updateStatement != null) updateStatement.close();
     if (selectByStatement != null) selectByStatement.close();
     selectStatement = null;
     selectCountStatement = null;
     insertStatement = null;
     updateStatement = null;
     } catch (Exception e) {
       e.printStackTrace();
     }
 
     this.conn = conn;
   }
 }
 
 /**
   * If the given string is comma-separated, return its last token
   * @param String the string which may be a comma-separated list
   * @return String the last token in the string
   */
 public String lastAttribute(String source) {
   int commaPos = source.indexOf(',');
   if (commaPos < 0) return source;
   return source.substring(commaPos + 1);
 }
 
 /**
   * If the given string is comma-separated, return its last token
   * @param String the string which may be a comma-separated list
   * @return String the last token in the string
   */
 public boolean containedIn(String source, String test) {
   StringTokenizer iter = new StringTokenizer(source, ",");
   while (iter.hasMoreTokens()) {

     if (iter.nextToken().equals(test)) return true;
   }
   return false;
 }
 
 /**
   * Set up SELECT statement buffer for the current object
   * @return String the SQL statement
   */
 public String breakAttributes(String source) {
   int commaPos = source.indexOf(',');
   if (commaPos < 0) return (String) attributes.get(source);
   StringBuffer result = new StringBuffer();
   String sep = "";
   StringTokenizer it = new StringTokenizer(source, ",");
   while (it.hasMoreTokens()) {
     String itsName = (String) attributes.get(it.nextToken());
     if (itsName != null) result.append(sep).append(itsName);
     sep = ", ";
   }
   return result.toString();
 }
 
 /**
   * Set up WHERE clause
   * @return String the SQL statement
   */
 public String whereAttributes(String source) {
   StringBuffer result = new StringBuffer();
   String sep = " WHERE ";
   StringTokenizer it = new StringTokenizer(source, ",");
   while (it.hasMoreTokens()) {
     String itsName = (String) attributes.get(it.nextToken());
     result.append(sep).append(itsName).append(" = ?");
     sep = " AND ";
   }
   return result.toString();
 }
 
 /**
   * Set up SELECT statement buffer for the current object
   * @return String the SQL statement
   */
 public String initialiseObjectSelect() {
   StringBuffer stmt = new StringBuffer();
   String connector = "SELECT ";
   Iterator it = attributes.keySet().iterator();
   String attName = null;
   String dbName = null;
   while (it.hasNext()) {
      attName = (String) it.next();
      dbName = (String) attributes.get(attName);
      stmt.append(connector).append(dbName);
      connector = ", ";
   }
   stmt.append(" FROM ").append(tableName);
   stmt.append(" ORDER BY ").append(breakAttributes(keyName));
   return stmt.toString();
 }
 
 /**
   * Set up SELECT statement buffer for the current object
   * @return String the SQL statement
   */
 public String initialiseSelectBy() {
   StringBuffer stmt = new StringBuffer();
   String connector = "SELECT ";
   Iterator it = attributes.keySet().iterator();
   String attName = null;
   String dbName = null;
   while (it.hasNext()) {
      attName = (String) it.next();
      dbName = (String) attributes.get(attName);
      stmt.append(connector).append(dbName);
      connector = ", ";
   }
   stmt.append(" FROM ").append(tableName);
   stmt.append(" WHERE ").append((String) attributes.get(byName)).append(" = ?");
   stmt.append(" ORDER BY ").append(breakAttributes(keyName));
   return stmt.toString();
 }
 
 /**
   * Set up a SELECT statement buffer for the current object
   * @return String the SQL statement
   */
 public String initialiseSelectKey(Object datum, Set keys) throws Exception {
   StringBuffer stmt = new StringBuffer();
   String connector = "SELECT ";
   Iterator it = attributes.keySet().iterator();
   String attName = null;
   String attValue = null;
   String dbName = null;
   while (it.hasNext()) {
      attName = (String) it.next();
      dbName = (String) attributes.get(attName);
      stmt.append(connector).append(dbName);
      connector = ", ";
   }
   if (keyStatement == null) {

     stmt.append(" FROM ").append(tableName);
     connector = " WHERE ";
   } else {
     stmt.append(" ").append(keyStatement);
     connector = " AND ";
   }
   it = keys.iterator();
   while (it.hasNext()) {
      attName = (String) it.next();
      attValue = BeanUtils.getProperty(datum, attName);
      dbName = (String) attributes.get(attName);
      stmt.append(connector).append(dbName).append(" = ").append(attValue);
      connector = " AND ";
   }
   if (keyOrder == null) {
     stmt.append(" ORDER BY ").append(breakAttributes(keyName));
   } else {
     stmt.append(" ").append(keyOrder);
   }
   connector = stmt.toString();
   return stmt.toString();
 }
 
 /**
   * Set up a DELETE statement buffer for the current object
   * @return String the SQL statement
   */
 public String initialiseDeleteKey(Object datum, Set keys) throws Exception {
   StringBuffer stmt = new StringBuffer();
   stmt.append("DELETE FROM ").append(tableName);
   String connector = " WHERE ";
   String attName;
   String attValue;
   String dbName;
   Iterator it = keys.iterator();
   while (it.hasNext()) {
      attName = (String) it.next();
      attValue = BeanUtils.getProperty(datum, attName);
      dbName = (String) attributes.get(attName);
      stmt.append(connector).append(dbName).append(" = ").append(attValue);
      connector = " AND ";
   }
   return stmt.toString();
 }
 
 /**
   * Run the SELECT ALL statement and load the data into its map
   * @param datum object of the class to load db data into
   * @param dataSet map to put the objects into
   */
 public void executeSelectAll(Object datum, Map dataSet) throws Exception {
   if (dataName == null) dataName = keyName;
   if (selectStatement == null) selectStatement = conn.prepareStatement(initialiseObjectSelect());
   ResultSet selectedRows = selectStatement.executeQuery();
   while (selectedRows.next()) {
     Object newDatum = BeanUtils.cloneBean(datum);
     Iterator it = attributes.keySet().iterator();
     String attName = null;
     String attValue = null;
     int paramIndex = 1;
     while (it.hasNext()) {
       attName = (String) it.next();
       attValue = selectedRows.getString(paramIndex);
       if ("-9999.0".equals(attValue)) attValue = "-9999";
       BeanUtils.setProperty(newDatum, attName, attValue);
       paramIndex += 1;
     }
     String thisOnesName = BeanUtils.getProperty(newDatum, lastAttribute(dataName));
     dataSet.put(thisOnesName, newDatum);
   }
   selectedRows.close();
 }
 
 /**
   * Run the SELECT BY statement and load the data into its map
   * @param datum object of the class to load db data into
   * @param dataSet map to put the objects into
   * @param key value to use for the key
   */
 public void executeSelectBy(Object datum, Map dataSet, String key) throws Exception {
   loadProperties(datum);
   if (byName == null || key == null) return;
   if (dataName == null) dataName = keyName;
   if (selectByStatement == null)
     selectByStatement = conn.prepareStatement(initialiseSelectBy());
   selectByStatement.setString(1, key);
   ResultSet selectedRows = selectByStatement.executeQuery();
   while (selectedRows.next()) {
     Object newDatum = BeanUtils.cloneBean(datum);
     Iterator it = attributes.keySet().iterator();
     String attName = null;
     String attValue = null;
     int paramIndex = 1;
     while (it.hasNext()) {
       attName = (String) it.next();
       attValue = selectedRows.getString(paramIndex);
       if ("-9999.0".equals(attValue)) attValue = "-9999";
       BeanUtils.setProperty(newDatum, attName, attValue);
       paramIndex += 1;
     }

     String thisOnesName = BeanUtils.getProperty(newDatum, lastAttribute(dataName));
     dataSet.put(thisOnesName, newDatum);
   }
   selectedRows.close();
 }
 
 /**
   * Run the SELECT BY statement and load the data into its map
   * @param datum object of the class to load db data into
   * @param dataSet list to put the objects into
   * @param keys values to use for the keys
   * @param overseer for reference
   */
 public void executeSelectKey(Object datum, List dataSet, Set keys, GeneralDataProcessor overseer) throws Exception {
   keys.retainAll(attributes.keySet());
   if (keyNames.size() > 0) {
     keys.retainAll(keyNames);
   }
   Statement selectStatement = conn.createStatement();
   if (selectStatement == null) return;
   Object newDatum = null;
   ResultSet selectedRows = selectStatement.executeQuery(initialiseSelectKey(datum, keys));
   while (selectedRows.next()) {
     // Make a new copy with fresh contained data lists if we can
     if (overseer != null) {
       newDatum = overseer.cloneNewDatum(datum);
     } else {
       newDatum = BeanUtils.cloneBean(datum);
     }
     Iterator it = attributes.keySet().iterator();
     String attName = null;
     String attValue = null;
     int paramIndex = 1;
     while (it.hasNext()) {
       attName = (String) it.next();
       attValue = selectedRows.getString(paramIndex);
       if ("-9999.0".equals(attValue)) attValue = "-9999";
       BeanUtils.setProperty(newDatum, attName, attValue);
       paramIndex += 1;
     }
     dataSet.add(newDatum);
   }
   selectedRows.close();
   selectStatement.close();
 }
 
 /**
   * Run the DELETE statement to remove rows matching keys from the database
   * @param datum object of the class to delete rows for
   * @param keys values to use for the keys
   * @param overseer for reference
   * @return int number of rows deleted
   */
 public int executeDeleteKey(Object datum, Set keys, GeneralDataProcessor overseer) throws Exception {
   keys.retainAll(attributes.keySet());
   if (keyNames.size() > 0) {
     keys.retainAll(keyNames);
   }
   Statement deleteStatement = conn.createStatement();
   if (deleteStatement == null || keys.size() == 0) return 0;
   int deletedRows = deleteStatement.executeUpdate(initialiseDeleteKey(datum, keys));
   deleteStatement.close();
   return deletedRows;
 }
 
 /**
   * Set up SELECT COUNT(*) statement buffer for the current object
   * @return String to contain the SQL statement
   */
 public String initialiseCountSelect() {
   StringBuffer stmt = new StringBuffer();
   stmt.append("SELECT COUNT(*) FROM ").append(tableName);
   stmt.append(whereAttributes(keyName));
   return stmt.toString();
 }
 
 /**
   * Set up SELECT COUNT(*) statement buffer for the current object
   * @param datum object of the class to count
   * @return boolean true if records exist
   */
 public boolean executeCountSelect(Object datum) throws Exception {
   if (selectCountStatement == null) selectCountStatement = conn.prepareStatement(initialiseCountSelect());
   StringTokenizer iter = new StringTokenizer(keyName, ",");
   int paramIndex = 1;
   while (iter.hasMoreTokens()) {
     String it = iter.nextToken();
    String keyValue = BeanUtils.getProperty(datum, it);
    if (integers.contains(it)) {
      selectCountStatement.setInt(paramIndex, Integer.parseInt(keyValue));
    } else {
      selectCountStatement.setString(paramIndex, keyValue);
    }
    paramIndex += 1;
   }
   ResultSet selectedRows = selectCountStatement.executeQuery();
   int result = 0;
   if (selectedRows.next()) {
     result = selectedRows.getInt(1);
   }

   selectedRows.close();
   return (result > 0);
 }
 
 
 /**
   * Set up SELECT MAX(keyName) statement buffer for the current object
   * @param keyDatum database name of the attribute to test
   * @return String to contain the SQL statement
   */
 public String initialiseKeySelect(String keyDatum) {
   StringBuffer stmt = new StringBuffer();
   stmt.append("SELECT MAX(").append(keyDatum).append(") FROM ").append(tableName);
   return stmt.toString();
 }
 
 /**
   * Fetch the next key number for a table
   * @param datum object of the class that needs the key
   * @return int the new key number
   */
 public int executeKeySelect(Object datum) throws Exception {
   if (utilityStatement == null) utilityStatement = conn.createStatement();
   loadProperties(datum);
   String it = lastAttribute(keyName);
   String keyValue = (String) attributes.get(it);
   int result = 0;
   if (integers.contains(it)) {
     ResultSet selectedRows = utilityStatement.executeQuery(initialiseKeySelect(keyValue));
     if (selectedRows.next()) {
       result = selectedRows.getInt(1) + 1;
     }
     selectedRows.close();
   }
   return result;
 }
 
 
 /**
   * Set up INSERT statement buffer for the current object
   * @return String the SQL statement
   */
 public String initialiseObjectInsert() throws Exception {
   StringBuffer stmt = new StringBuffer();
   StringBuffer queryList = new StringBuffer();
   stmt.append("INSERT INTO ").append(tableName);
   String connector = "( ";
   String conn2 = ") VALUES (";
   Iterator it = attributes.keySet().iterator();
   String attName = null;
   String dbName = null;
   while (it.hasNext()) {
      attName = (String) it.next();
      dbName = (String) attributes.get(attName);
      stmt.append(connector).append(dbName);
      queryList.append(conn2).append("?");
      connector = ", ";
      conn2 = connector;
   }
   stmt.append(queryList.toString()).append(")");
   return stmt.toString();
 }
 
 /**
   * Insert a database row for the current object
   * @param datum the data object
   */
 public void executeObjectInsert(Object datum) throws Exception {
   if (insertStatement == null) insertStatement = conn.prepareStatement(initialiseObjectInsert());
   Iterator it = attributes.keySet().iterator();
   String attName = null;
   String dbName = null;
   String attValue = null;
   int paramIndex = 1;
   while (it.hasNext()) {
      attName = (String) it.next();
      attValue = BeanUtils.getProperty(datum, attName);
      if (integers.contains(attName)) {
        if (attValue == null) {
          insertStatement.setNull(paramIndex, Types.INTEGER);
        } else {
          insertStatement.setInt(paramIndex, Integer.parseInt(attValue));
        }
      } else  if (floats.contains(attName)) {
        if (attValue == null || attValue.equals("0")) {
          insertStatement.setNull(paramIndex, Types.FLOAT);
        } else {
          insertStatement.setFloat(paramIndex, Float.parseFloat(attValue));
        }
      } else  if (blobs.contains(attName)) {
           insertStatement.setNull(paramIndex, Types.BLOB);
       } else {
        if (attValue == null) {
          insertStatement.setNull(paramIndex, Types.VARCHAR);
        } else {
          insertStatement.setString(paramIndex, attValue);
        }
      }
      paramIndex += 1;
   }

   insertStatement.executeUpdate();
 }
 
 /**
   * Update the database row for the current object
   * @param datum the data object
   */
 public void executeObjectUpdate(Object datum) throws Exception {
   if (updateStatement == null) updateStatement = conn.prepareStatement(initialiseObjectUpdate());
   Iterator it = attributes.keySet().iterator();
   String attName = null;
   String dbName = null;
   String attValue = null;
   ArrayList keyValue = new ArrayList();
   int paramIndex = 1;
   while (it.hasNext()) {
      attName = (String) it.next();
      attValue = BeanUtils.getProperty(datum, attName);
      if (changeName != null && attName.equals(changeName)) {
       continue;
       } else if (containedIn(keyName, attName)) {
        keyValue.add(attValue);
      } else {
        if (integers.contains(attName)) {
          if (attValue == null) {
            updateStatement.setNull(paramIndex, Types.INTEGER);
          } else {
            if (attValue.length() == 0) attValue = " ";
            updateStatement.setInt(paramIndex, Integer.parseInt(attValue));
          }
        } else if (floats.contains(attName)) {
        if (attValue == null || attValue.equals("0")) {
          updateStatement.setNull(paramIndex, Types.FLOAT);
        } else {
          updateStatement.setFloat(paramIndex, Float.parseFloat(attValue));
        }
        } else {
          if (attValue == null) {
            updateStatement.setNull(paramIndex, Types.VARCHAR);
          } else {
            if (attValue.length() == 0) attValue = " ";
            updateStatement.setString(paramIndex, attValue);
          }
        }
        paramIndex += 1;
      }
   }
   StringTokenizer iter = new StringTokenizer(keyName, ",");
   int valueIndex = 0;
   while (iter.hasMoreTokens()) {
     String name = iter.nextToken();
     String value = (String) keyValue.get(valueIndex);
     if (integers.contains(name)) {
       updateStatement.setInt(paramIndex, Integer.parseInt(value));
     }
     else {
       updateStatement.setString(paramIndex, value);
     }
     paramIndex += 1;
     valueIndex += 1;
   }
   updateStatement.executeUpdate();
 }
 
 /**
   * Set up Update statement buffer for the current object
   * @return String the SQL statement
   */
 public String initialiseObjectUpdate() {
   StringBuffer stmt = new StringBuffer();
   StringBuffer queryList = new StringBuffer();
   stmt.append("UPDATE ").append(tableName);
   String connector = " SET ";
   Iterator it = attributes.keySet().iterator();
   String attName = null;
   String dbName = null;
   String dbKeyName = null;
   while (it.hasNext()) {
      attName = (String) it.next();
      dbName = (String) attributes.get(attName);
      if (!containedIn(keyName, attName)) {
          if (changeName != null && attName.equals(changeName)) {
            stmt.append(connector).append(dbName).append(" = ").append("SYSDATE");
          } else {
            stmt.append(connector).append(dbName).append(" = ").append("?");
          }
         connector = ", ";
      }
   }
   stmt.append(whereAttributes(keyName));
   return stmt.toString();
 }
 
 public PreparedStatement getSelectStatement () {
   return selectStatement;
 }
 
 public PreparedStatement getSelectByStatement () {
   return selectByStatement;
 }

 
 public PreparedStatement getSelectCountStatement () {
   return selectCountStatement;
 }
 
 public PreparedStatement getInsertStatement () {
   return insertStatement;
 }
 
 public PreparedStatement getUpdateStatement () {
   return updateStatement;
 }
 
 public PreparedStatement getSelectStatement (Connection conn) throws Exception {
   if (conn != null && selectStatement == null)
     selectStatement = conn.prepareStatement(initialiseObjectSelect());
   return selectStatement;
 }
 
 public PreparedStatement getSelectByStatement (Connection conn) throws Exception {
   if (conn != null && selectByStatement == null && byName != null)
     selectByStatement = conn.prepareStatement(initialiseSelectBy());
   return selectByStatement;
 }
 
 public PreparedStatement getSelectCountStatement (Connection conn) throws Exception {
   if (conn != null && selectStatement == null)
     selectCountStatement = conn.prepareStatement(initialiseCountSelect());
   return selectCountStatement;
 }
 
}

