
package au.gov.sa.environment.gift.jdbc;
 
import java.util.*;
import java.util.Date;
import java.lang.reflect.*;
import java.text.*;
 
import java.io.*;
 
import java.sql.*;
 
import javax.sql.DataSource;
 
import oracle.jdbc.pool.OracleDataSource;
 
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.ConvertUtils;
 
import au.gov.sa.environment.gift.general.LabelValueBean;
 
/**
 * <p>Title: Generic database processor</p>
 * <p>Description: Driven by property files named for the JavaBeans it's handed</p>
 * <p >Copyright: Copyright (c) 2003</p>
 * <p>Company: DEH</p>
 * @author Peter Davey
 * @version 1.1
 */
 
public class JdbcDataProcessor extends GeneralDataProcessor {
 
        protected static JdbcDataProcessor jdp = new JdbcDataProcessor();
        private static boolean driverLoaded = false;
        private static boolean transactionCurrent = false;
        private static boolean transactionError = false;
        protected Connection conn = null;
        private DatabaseMetaData meta = null;
        private static Statement utilityStatement = null;
 
        static final SimpleDateFormat logDateFormat = new SimpleDateFormat("H-m-s dd/MM/yyyy");
 
        // Simple constructor
        protected JdbcDataProcessor() {
                try {
                        ConvertUtils.register(new UtilDateConverter(), Class.forName("java.util.Date"));
           ConvertUtils.register(new UtilTimeConverter(), Class.forName("java.sql.Timestamp"));
                } catch (Exception e) {
                        e.printStackTrace();
                }
        }
 
        /**
         * Clear all data from a named table - USE WITH CAUTION
         * @param tableName database name of the table
         */
        public void doDirectClear(String tableName) {
                try {
                        Statement stmt = conn.createStatement();
                        stmt.execute("DELETE FROM " + tableName);
                } catch (Exception e) {
                        e.printStackTrace();
                }
        }
 
        /**
         * Clear all data from the table associated with an object - USE WITH CAUTION
         * @param datum JavaBean with a properties file
         */
        public void doTableClear(Object datum) {
                try {
                        dataSet = GeneralDataSet.getDataset(datum);
                        Statement stmt = conn.createStatement();
                        stmt.execute("DELETE FROM " + dataSet.getTableName());
                } catch (Exception e) {
                        e.printStackTrace();
                }
        }
 
        /**
         * For each object appearing as a value in the map,
         * insert or update a database row
         * @param dataMap Map with target objects as its values
         */
        public void doDatabaseWrite(Map dataMap) {
                try {
                        Iterator it = dataMap.keySet().iterator();
                        String itsKey = null;
                        while (it.hasNext()) {
                                itsKey = (String) it.next();
                                Object datum = dataMap.get(itsKey);
                                dataSet = GeneralDataSet.getDataset(datum);
                                if (executeCountSelect(datum)) {
                                        executeObjectUpdate(datum);
                                } else {
                                        executeObjectInsert(datum);
                                }
                        }
                } catch (Exception e) {
                        e.printStackTrace();
                }

        }
 
 
        /**
         * Copies similarly named attributes from datum to each element of data
         * Useful for copying keys between related records, or logging.
         * @param datum JavaBean containing key and updated data
         * @param data ArrayList of related objects to receive data
         */
        public void propagateData(Object datum, List data) throws Exception {
                for (int i=0; i < data.size(); i++) {
                        BeanUtils.copyProperties(data.get(i), datum);
                }
        }
 
 
        /**
         * Insert or update the database row for the given object
         * Row collections in ArrayList attributes get propagated data
         * and saveChanges
         * @param datum JavaBean containing key and updated data
         */
        public void saveChanges(Object datum) {
                if (datum instanceof List) {
                        List dataList = (List) datum;
                        for (int i=0; i < dataList.size(); i++) {
                                saveChanges(dataList.get(i));
                        }
                } else {
                        try {
                                dataSet = GeneralDataSet.getDataset(datum);
                                if (executeCountSelect(datum)) {
                                        executeObjectUpdate(datum);
                                        writeChangeLog("Database row updated", datum);
                                } else {
                                        executeObjectInsert(datum);
                                        writeChangeLog("Database row inserted", datum);
                                }
                                Class parentClass = datum.getClass();
                                Iterator contains = dataSet.getContains().keySet().iterator();
                                while (contains.hasNext()) {
                                        String containsName = (String) contains.next();
                                        Object currentField = parentClass.getField(containsName).get(datum);
                                        if (currentField instanceof List) {
                                                propagateData(datum, (List) currentField);
                                                saveChanges(currentField);
                                        }
                                }
                        } catch (Exception e) {
                                e.printStackTrace();
                                transactionError = (transactionError || transactionCurrent);
                        }
                }
        }
 
 
        /**
         * Run a key select and any nested selects for a data object
         * @param datum the object to load nested data into
         * @param keys the keys of the top-level objects to retrieve
         * @return List of datum copies
         */
        public List loadWithContents(Object datum, Set keys) {
                GeneralDataSet dataSet = GeneralDataSet.getDataset(datum);
                List currentSet = new ArrayList();
                try {
                        dataSet.prepareForConnection(conn);
                        dataSet.executeSelectKey(datum, currentSet, keys, this);
                        for (int i=0; i < currentSet.size(); i++) {
                                loadContainedData(currentSet.get(i));
                        }
                } catch (Exception e) {
                        e.printStackTrace();
                }
                return currentSet;
        }
 
 
        /**
         * Return an ArrayList attribute from a data object
         * @param datum the object containing the attribute
         * @param attribute the attribute name
         */
        public List getContainedData(Object datum, String attribute) {
                StringBuffer workName = new StringBuffer("get");
                workName.append(attribute.substring(0,1).toUpperCase()).append(attribute.substring(1));
                List result = null;
                try {
                        Class parentClass = datum.getClass();
                        Method getMethod = parentClass.getMethod(workName.toString(), new Class[] {});
                        Object currentField = getMethod.invoke(datum, new Object[] {});
                        if (currentField instanceof List) {
                                result = (List) currentField;
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
                GeneralDataSet dataSet = GeneralDataSet.getDataset(datum);
                Map contains = dataSet.getContains();
                if (contains.size() == 0) return;
 
                List containsSets = new ArrayList();
                List currentSet = null;
 
                Iterator it = contains.keySet().iterator();
                String itsAttribute = null;
                String itsClassName = null;
                Class parentClass = datum.getClass();
                String className = parentClass.getName();
                int lastIndex = className.lastIndexOf(".");
                if (lastIndex > 0) {
                        className = className.substring(0,lastIndex+1);
                } else {
                        className = "";
                }
                Map parentProperties = null;
                Class itsClass = null;
                Object nestDatum = null;
                try {
                        parentProperties = BeanUtils.describe(datum);
                        while (it.hasNext()) {
                                itsAttribute = (String) it.next();
                                itsClassName = (String) contains.get(itsAttribute);
                                currentSet = getContainedData(datum, itsAttribute);
                                if (currentSet != null) {
                                        currentSet.clear();
                                        containsSets.add(currentSet);
                                        itsClass = Class.forName(className + itsClassName);
                                        if (itsClass != null) {
                                                nestDatum = itsClass.newInstance();
                                                BeanUtils.copyProperties(nestDatum, datum);
                                                Map datumProperties = BeanUtils.describe(nestDatum);
                                                Set datumAttributes = datumProperties.keySet();
                                                datumAttributes.retainAll(parentProperties.keySet());
                                                dataSet = GeneralDataSet.getDataset(nestDatum);
                                                dataSet.prepareForConnection(conn);
                                                dataSet.executeSelectKey(nestDatum, currentSet, datumAttributes, this);
                                        }
                                }
                        }
                } catch (Exception e) {
                        e.printStackTrace();
                }
                for (int i=0; i < containsSets.size(); i++) {
                        currentSet = (List) containsSets.get(i);
                        for (int j=0; j < currentSet.size(); j++) {
                                loadContainedData(currentSet.get(j));
                        }
                }
        }
 
        /**
         * Run any nested deletions for a data object, then delete the object itself
         * @param datum the object to remove nested data from
         * @param self if true will delete this object too
         */
        public void deleteContainedData(Object datum, boolean self) {
                GeneralDataSet dataSet = GeneralDataSet.getDataset(datum);
                Map contains = dataSet.getContains();
                if (contains.size() > 0) deleteContents(datum, dataSet);
                if (self && dataSet instanceof JdbcDataSet) {
                        try {
                                dataSet.prepareForConnection(conn);
                                ((JdbcDataSet) dataSet).executeDeleteKey(datum, dataSet.getKeys(), this);
                        }
                        catch (Exception e) {
                                e.printStackTrace();
                                transactionError = (transactionError || transactionCurrent);
                        }
                }
        }
 
        /**
         * Run any nested deletions for a data object
         * @param datum the object to remove nested data from
         */
        private void deleteContents(Object datum, GeneralDataSet dataSet) {
                Map contains = dataSet.getContains();
                if (contains.size() == 0) return;
                GeneralDataSet nestSet;
                List containsSets = new ArrayList();
                List currentSet = null;
 
                String itsAttribute = null;
                String itsClassName = null;
                Class parentClass = datum.getClass();
                String className = parentClass.getName();
                int lastIndex = className.lastIndexOf(".");
                className = (lastIndex > 0) ? className.substring(0,lastIndex+1) : "";
 
                Map parentProperties;
                Class itsClass;

                Object nestDatum;
                Iterator it;
                try {
                        parentProperties = BeanUtils.describe(datum);
                        it = contains.keySet().iterator();
                        while (it.hasNext()) {
                                itsAttribute = (String) it.next();
                                itsClassName = (String) contains.get(itsAttribute);
                                currentSet = getContainedData(datum, itsAttribute);
                                if (currentSet != null) {
                                        currentSet.clear();
                                        containsSets.add(currentSet);
                                        itsClass = Class.forName(className + itsClassName);
                                        if (itsClass != null) {
                                                nestDatum = itsClass.newInstance();
                                                BeanUtils.copyProperties(nestDatum, datum);
                                                Map datumProperties = BeanUtils.describe(nestDatum);
                                                Set datumAttributes = datumProperties.keySet();
                                                datumAttributes.retainAll(parentProperties.keySet());
                                                nestSet = GeneralDataSet.getDataset(nestDatum);
                                                nestSet.prepareForConnection(conn);
                                                nestSet.executeSelectKey(nestDatum, currentSet, datumAttributes, this);
                                        }
                                }
                        }
                } catch (Exception e) {
                        e.printStackTrace();
                        transactionError = (transactionError || transactionCurrent);
                }
                for (int i=0; i < containsSets.size(); i++) {
                        currentSet = (List) containsSets.get(i);
                        for (int j=0; j < currentSet.size(); j++) {
                                deleteContainedData(currentSet.get(j), true);
                        }
                }
        }
 
        /**
         * Run the SELECT ALL statement and load the data into its map
         * @param datum object of the class to load db data into
         * @param resultMap map to put the objects into
         */
        public void executeSelectAll(Object datum, Map resultMap) throws Exception {
                dataSet = GeneralDataSet.getDataset(datum);
                if (dataSet instanceof JdbcDataSet) {
                        dataSet.prepareForConnection(conn);
                        ResultSet selectedRows = ((JdbcDataSet) dataSet).getSelectStatement().executeQuery();
                        while (selectedRows.next()) {
                                Object newDatum = BeanUtils.cloneBean(datum);
                                String thisOnesName = dataSet.applyData(newDatum, selectedRows);
                                resultMap.put(thisOnesName, newDatum);
                        }
                        selectedRows.close();
                }
        }
 
        /**
         * Run the SELECT BY statement and load the data into its map
         * @param datum object of the class to load db data into
         * @param resultMap map to put the objects into
         * @param key value to use for the key
         */
        public void executeSelectBy(Object datum, Map resultMap, String key) {
                if (key == null) return;
                dataSet = GeneralDataSet.getDataset(datum);
                ResultSet selectedRows = null;
                Object newDatum = null;
                if (dataSet instanceof JdbcDataSet) {
                        try {
                                dataSet.prepareForConnection(conn);
                                PreparedStatement selectByStatement
                                = ( (JdbcDataSet) dataSet).getSelectByStatement(conn);
                                if (selectByStatement == null) return;
                                selectByStatement.setString(1, key);
                                selectedRows = selectByStatement.executeQuery();
                                while (selectedRows.next()) {
                                        newDatum = (newDatum == null) ? datum : BeanUtils.cloneBean(datum);
                                        String thisOnesName = dataSet.applyData(newDatum, selectedRows);
                                        resultMap.put(thisOnesName, newDatum);
                                }
                        } catch (Exception e) {
                        } finally {
                                try {
                                        if (selectedRows != null) selectedRows.close();
                                } catch (Exception e) {}
                        }
                }
        }
 
        /**
         * Set up SELECT COUNT(*) statement buffer for the current object
         * @param datum object of the class to count
         * @return boolean true if records exist
         */
        public boolean executeCountSelect(Object datum) throws Exception {
                dataSet = GeneralDataSet.getDataset(datum);
                if (dataSet instanceof JdbcDataSet) {
                        dataSet.prepareForConnection(conn);
                        return ((JdbcDataSet) dataSet).executeCountSelect(datum);
                }

                return false;
        }
 
        /**
         * Fetch the next key number for a table
         * @param datum object of the class that needs the key
         * @return int the new key number
         */
        public int executeKeySelect(Object datum) {
                try {
                        dataSet = GeneralDataSet.getDataset(datum);
                        if (dataSet instanceof JdbcDataSet) {
                                dataSet.prepareForConnection(conn);
                                return ((JdbcDataSet) dataSet).executeKeySelect(datum);
                        }
                } catch (Exception e) { }
                return -1;
        }
 
        /**
         * Insert a database row for the current object
         * @param datum the data object
         */
        public void executeObjectInsert(Object datum) throws Exception {
                dataSet = GeneralDataSet.getDataset(datum);
                if (dataSet == null) return;
                if (dataSet.getInsertName() != null) dataSet = GeneralDataSet.getNamedDataset(dataSet.getInsertName(), datum);
                if (dataSet instanceof JdbcDataSet) {
                        dataSet.prepareForConnection(conn);
                        ((JdbcDataSet) dataSet).executeObjectInsert(datum);
                }
        }
 
        /**
         * Update the database row for the current object
         * @param datum the data object
         */
        public void executeObjectUpdate(Object datum) throws Exception {
                dataSet = GeneralDataSet.getDataset(datum);
                if (dataSet.getUpdateName() != null) dataSet = GeneralDataSet.getNamedDataset(dataSet.getUpdateName(), datum);
                if (dataSet instanceof JdbcDataSet) {
                        dataSet.prepareForConnection(conn);
                        ((JdbcDataSet) dataSet).executeObjectUpdate(datum);
                }
        }
        /**
         * Load a simple select statement property file and execute it
         * @param name String identifying the properties file
         * @param key Single parameter to supply to the select
         * @param datum Object to supply a classloader
         * @return String single result field from select
         */
        public String simpleSelect(String name, String key, Object datum) {
 
                String result = simpleSelect2(name, key, null, datum);
                return result;
        }
 
        /**
         * Load a simple select statement (with two query params) property file and execute it
         * @param name String identifying the properties file
         * @param key1 parameter 1 to supply to the select
         * @param key2 parameter 2 to supply to the select
         * @param datum Object to supply a classloader
         * @return String single result field from select
         */
        public String simpleSelect2(String name, String key1, String key2, Object datum) {
 
                String result = null;
                PreparedStatement theStmt = null;
                ResultSet selectedRows = null;
 
                Properties p = GeneralDataSet.loadDbProperties(name, null, datum);
                String statement = p.getProperty("statement");
                try {
                        if (statement != null) {
                                theStmt = conn.prepareStatement(statement);
                                if (key1 != null) theStmt.setString(1, key1);
                                if (key2 != null) theStmt.setString(2, key2);
                                selectedRows = theStmt.executeQuery();
                                if (selectedRows.next()) {
                                        result = selectedRows.getString(1);
                                }
                        }
                } catch (Exception e) {
                        e.printStackTrace();
                } finally {
                        try {
                                if (selectedRows != null) selectedRows.close();
                                if (theStmt != null) theStmt.close();
                        } catch (Exception e) {
                                e.printStackTrace();
                        }
                }
                return result;
        }
        /**
         * Load a simple update statement property file and execute it
         * @param name String identifying the properties file
         * @param key Single parameter to supply to the select

         * @param datum Object to supply a classloader
         * @return int number of rows updated
         */
        public int simpleUpdate(String name, String key, Object datum) {
                int rowCount = -1;
                String result = null;
                PreparedStatement theStmt = null;
                ResultSet selectedRows = null;
 
                Properties p = GeneralDataSet.loadDbProperties(name, null, datum);
                String statement = p.getProperty("statement");
                try {
                        if (statement != null) {
                                theStmt = conn.prepareStatement(statement);
                                if (key != null) theStmt.setString(1, key);
                                rowCount = theStmt.executeUpdate();
                        }
                } catch (Exception e) {
                        e.printStackTrace();
                } finally {
                        try {
                                if (theStmt != null) theStmt.close();
                        } catch (Exception e) {
                                e.printStackTrace();
                        }
                }
                return rowCount;
        }
        /**
         * Do a (perhaps keyed) database action
         * @param key Single parameter to supply to the select
         * @param key Single parameter to supply to the select
         * @return int rows changed
         */
        private int doAction(PreparedStatement theStmt, Object datum, String[] key) throws Exception {
                if (key != null) {
                        if (datum == null) {
                                theStmt.setString(1, key[0]);
                        } else {
                                for (int i=0; i < key.length; i++) {
                                        theStmt.setString(i+1, BeanUtils.getProperty(datum, key[i]));
                                }
                        }
                }
                return theStmt.executeUpdate();
        }
        /**
         * Get a (perhaps indirectly referenced) database statement
         * @param p properties for reference
         * @param key Name of next level
         * @return String next statement
         */
        private String getStatement(Properties p, String key) throws Exception {
                String stmt = p.getProperty(key);
                if (stmt != null) {
                        String[] element = stmt.split(",");
                        if (element[0].equals("param"))
                                stmt = p.getProperty(element[1]);
                }
                return stmt;
        }
        /**
         * Get parameter list for a (perhaps indirectly referenced) database statement
         * @param p properties for reference
         * @param key Name of next level
         * @return String[] parameter attributes
         */
        private String[] getParams(Properties p, String key) throws Exception {
                String[] result = {};
                String stmt = p.getProperty(key);
                if (stmt != null) {
                        String[] element = stmt.split(",");
                        if (element[0].equals("param")) {
                                result = new String[element.length - 2];
                                for (int i=0; i < result.length; i++) result[i] = element[i+2];
                        } else if (element[0].equals("key")) {
                                result = new String[1];
                                result[0] = element[2];
                        }
                }
                return result;
        }
        /**
         * Load a script statement property file and execute it
         * script=name1,name2,...
         * name1=statement
         * name2=statement
         * namex=list,listattribute,namey,keyattribute
         * @param statement String containing a line from the properties file
         * @param key Single parameter to supply to the select
         * @param datum Object to supply a classloader
         * @return String describing actions
         */
        private String scriptedAction(String lineKey, Object datum, Properties p) {
                if (lineKey == null || lineKey.length() == 0) return "";
                String action;
                PreparedStatement theStmt = null;
                String thisLine = p.getProperty(lineKey);
                int rowsChanged;
                String[] element;

                String[] param;
                try {
                        if (thisLine != null && thisLine.length() > 0) {
                                element = thisLine.split(",");
                                if (element[0].equals("list")) {
                                        List list = getContainedData(datum, element[1]);
                                        action = getStatement(p, element[2]);
                                        param = getParams(p, element[2]);
                                        theStmt = conn.prepareStatement(action);
                                        for (int j=0; j < list.size(); j++) {
                                                rowsChanged = doAction(theStmt, list.get(j), param);
                                        }
                                } else if (element[0].equals("drill")) {
                                        List list = getContainedData(datum, element[1]);
                                        action = getStatement(p, element[3]);
                                        param = getParams(p, element[3]);
                                        theStmt = conn.prepareStatement(action);
                                        for (int j=0; j < list.size(); j++) {
                                                List innerList = getContainedData(list.get(j), element[2]);
                                                for (int k=0; k < innerList.size(); k++) {
                                                        rowsChanged = doAction(theStmt, innerList.get(k), param);
                                                }
                                        }
                                } else if (element[0].equals("exists")) {
                                        StringBuilder s = new StringBuilder();
                                        s.append("select ").append(element[2]).append(" from ").append(element[1]);
                                        s.append(" where ").append(element[2]).append(" = '").append(BeanUtils.getSimpleProperty(datum, element[3])).append("'");
                                        theStmt = conn.prepareStatement(s.toString());
                                        ResultSet r = theStmt.executeQuery();
                                        if (r.next()) {
                                                scriptedAction(element[4], datum, p);
                                        } else {
                                                scriptedAction(element[5], datum, p);
                                        }
                                } else if (element[0].equals("key")) {
                                        action = p.getProperty(element[2]);
                                        theStmt = conn.prepareStatement(action);
                                        String[] arrayKey = {element[1]};
                                        rowsChanged = doAction(theStmt, datum, arrayKey);
                                } else if (element[0].equals("param")) {
                                        action = getStatement(p, lineKey);
                                        param = getParams(p, lineKey);
                                        theStmt = conn.prepareStatement(action);
                                        rowsChanged = doAction(theStmt, datum, param);
                                } else {
                                        theStmt = conn.prepareStatement(thisLine);
                                        rowsChanged = doAction(theStmt, null, null);
                                }
                        }
                } catch (Exception e) {
                        e.printStackTrace();
                } finally {
                        try {if (theStmt != null) theStmt.close();} catch (Exception e) {e.printStackTrace();}
                }
                return null;
        }
        /**
         * Load a script statement property file and execute it
         * script=name1,name2,...
         * name1=statement
         * name2=statement
         * namex=list,listattribute,namey,keyattribute
         * @param name String identifying the properties file
         * @param key Single parameter to supply to the select
         * @param datum Object to supply a classloader
         * @return String describing actions
         */
        public String scriptedActions(String name, Object datum) {
 
                String result = null;
 
                Properties p = GeneralDataSet.loadDbProperties(name, null, datum);
                String[] script = p.getProperty("script").split(",");
                for (int i=0; i < script.length; i++) {
                        scriptedAction(script[i], datum, p);
                }
                return result;
        }
 
        /**
         * Load a utilities statement property file for a classname
         * @param datum JavaBean identifying the table properties file
         * @param dataList null, or list to contain results of select
         */
        public void databaseUtility(Object datum, List dataList) throws Exception {
                PreparedStatement theStmt = null;
                ResultSet selectedRows = null;
 
                String statement = null;
                List keyAttributes = new ArrayList();
                List resultAttributes = new ArrayList();
                List integers = new ArrayList();
                Properties p = GeneralDataSet.loadDbProperties(datum);
 
                for (Enumeration psm = p.propertyNames() ; psm.hasMoreElements() ;) {
                        String itsName = (String) psm.nextElement();
                        String it = p.getProperty(itsName);
                        StringTokenizer itsList = null;
                        if ("attribute".equals(itsName)) {
                                itsList = new StringTokenizer(it, ", ");

                                while (itsList.hasMoreTokens()) keyAttributes.add(itsList.nextToken());
                        } else if ("result".equals(itsName)) {
                                itsList = new StringTokenizer(it, ", ");
                                while (itsList.hasMoreTokens()) resultAttributes.add(itsList.nextToken());
                        } else if ("statement".equals(itsName)) {
                                statement = it;
                        } else if ("intFields".equals(itsName)) {
                                itsList = new StringTokenizer(it, ", ");
                                while (itsList.hasMoreTokens()) integers.add(itsList.nextToken());
                        }
                }
                if (statement == null) {
                        return;
                }
                theStmt = conn.prepareStatement(statement);
                Iterator it = keyAttributes.iterator();
                String attName = null;
                String attValue = null;
                int paramIndex = 1;
                while (it.hasNext()) {
                        attName = (String) it.next();
                        attValue = BeanUtils.getProperty(datum, attName);
                        if (attValue != null) {
                                if (integers.contains(attName)) {
                                        theStmt.setInt(paramIndex, Integer.parseInt(attValue));
                                } else {
                                        theStmt.setString(paramIndex, attValue);
                                }
                        }
                        paramIndex += 1;
                }
                try {
                        if (dataList == null) {
                                theStmt.executeUpdate();
                        } else {
                                selectedRows = theStmt.executeQuery();
                                while (selectedRows.next()) {
                                        Object newDatum = BeanUtils.cloneBean(datum);
                                        it = resultAttributes.iterator();
                                        paramIndex = 1;
                                        while (it.hasNext()) {
                                                attName = (String) it.next();
                                                BeanUtils.setProperty(newDatum, attName, selectedRows.getString(paramIndex));
                                                paramIndex += 1;
                                        }
                                        dataList.add(newDatum);
                                }
                        }
                } catch (Exception e) {
                        e.printStackTrace();
                } finally {
                        if (selectedRows != null) selectedRows.close();
                        if (theStmt != null) theStmt.close();
                }
        }
        /**
         * Load and run the count statement from the property file for a classname
         * @param datum JavaBean identifying the table properties file
         * @return int value of count
         */
        public int databaseCountUtility(Object datum) {
                int result = 0;
                PreparedStatement theStmt = null;
                ResultSet selectedRows = null;
                String statement = null;
                List keyAttributes = new ArrayList();
                List integers = new ArrayList();
 
                try {
 
                        Properties p = GeneralDataSet.loadDbProperties(datum);
 
                        for (Enumeration psm = p.propertyNames() ; psm.hasMoreElements() ;) {
                                String itsName = (String) psm.nextElement();
                                String it = p.getProperty(itsName);
                                StringTokenizer itsList = null;
                                if ("attribute".equals(itsName)) {
                                        itsList = new StringTokenizer(it, ", ");
                                        while (itsList.hasMoreTokens()) keyAttributes.add(itsList.nextToken());
                                } else if ("countstatement".equals(itsName)) {
                                        statement = it;
                                } else if ("intFields".equals(itsName)) {
                                        itsList = new StringTokenizer(it, ", ");
                                        while (itsList.hasMoreTokens()) integers.add(itsList.nextToken());
                                }
                        }
                        if (statement == null) {
                                return 0;
                        }
                        theStmt = conn.prepareStatement(statement);
                        Iterator it = keyAttributes.iterator();
                        String attName = null;
                        String attValue = null;
                        int paramIndex = 1;
                        while (it.hasNext()) {
                                attName = (String) it.next();
                                attValue = BeanUtils.getProperty(datum, attName);
                                if (attValue != null) {
                                        if (integers.contains(attName)) {
                                                theStmt.setInt(paramIndex, Integer.parseInt(attValue));

                                        } else {
                                                theStmt.setString(paramIndex, attValue);
                                        }
                                }
                                paramIndex += 1;
                        }
                        selectedRows = theStmt.executeQuery();
                        if (selectedRows.next()) {
                                result = selectedRows.getInt(1);
                        }
                } catch (Exception e) {
                        e.printStackTrace();
                } finally {
                        try {
                                if (selectedRows != null) selectedRows.close();
                                if (theStmt != null) theStmt.close();
                        } catch (Exception e) {
                                e.printStackTrace();
                        }
                }
                return result;
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
                PreparedStatement theStmt = null;
                ResultSet selectedRows = null;
 
                String statement = null;
                String tail = null;
                String pickName = null;
                String it = null;
                List keyAttributes = new ArrayList();
                Set integers = new HashSet();
 
                Properties p = GeneralDataSet.loadDbProperties(name, "Select", datum);
 
                for (Enumeration psm = p.propertyNames() ; psm.hasMoreElements() ;) {
                        String itsName = (String) psm.nextElement();
                        it = p.getProperty(itsName);
                        StringTokenizer itsList = null;
                        if ("attribute".equals(itsName)) {
                                itsList = new StringTokenizer(it, ", ");
                                while (itsList.hasMoreTokens()) {
                                        String thisToken = itsList.nextToken();
                                        if ("pick".equals(thisToken)) {
                                                pickName = decodePick(datum, itsList.nextToken());
                                        } else {
                                                keyAttributes.add(thisToken);
                                        }
                                }
                        } else if ("integer".equals(itsName)) {
                                itsList = new StringTokenizer(it, ", ");
                                while (itsList.hasMoreTokens()) integers.add(itsList.nextToken());
                        } else if ("statement".equals(itsName)) {
                                statement = it;
                        } else if ("tail".equals(itsName)) {
                                tail = it;
                        }
                }
                if (pickName != null) {
                        it = p.getProperty(pickName);
                        if (it != null) statement = statement + it;
                }
                if (tail != null) {
                        statement = statement + tail;
                }
                if (statement == null) {
                        return;
                }
                if (keyAttributes.size() == 0) {
                        if (staticNameCache.containsKey(name)) {
                                nameList.addAll((ArrayList) staticNameCache.get(name));
                                valueList.addAll((ArrayList) staticValueCache.get(name));
                                return;
                        }
                }
                try {
                        theStmt = conn.prepareStatement(statement);
                        Iterator iter = keyAttributes.iterator();
                        String attName = null;
                        String attValue = null;
                        int paramIndex = 1;
                        while (iter.hasNext()) {
                                attName = (String) iter.next();
                                attValue = BeanUtils.getProperty(datum, attName);
                                if (attValue == null) return;
                                if (integers.contains(attName)) {
                                        if (attValue.equals("0")) return;
                                        theStmt.setInt(paramIndex, Integer.parseInt(attValue));
                                } else {
                                        theStmt.setString(paramIndex, attValue);
                                }

                                paramIndex += 1;
                        }
                        selectedRows = theStmt.executeQuery();
                        while (selectedRows.next()) {
                                nameList.add(selectedRows.getString(1));
                                valueList.add(selectedRows.getString(2));
                        }
                        selectedRows.close();
                        if (keyAttributes.size() == 0) {
                                staticNameCache.put(name, nameList);
                                staticValueCache.put(name, valueList);
                        }
                } catch (Exception e) {
                        e.printStackTrace();
                } finally {
                        try {
                                if (selectedRows != null) selectedRows.close();
                                if (theStmt != null) theStmt.close();
                        } catch (Exception e) {
                                e.printStackTrace();
                        }
                }
        }
 
        /**
         * Load a namelist from the instructions in its property file
         * Check parameter attributes - if any are null or 0, skip the lookup and return empty lists
         * @param datum JavaBean containing source data
         * @param name String identifying a <name>select.properties file
         * @param lvList List to hold LabelValueBeans
         */
        public void namelistUtility(Object datum, String name, List lvList) {
                PreparedStatement theStmt = null;
                ResultSet selectedRows = null;
 
                String statement = null;
                String tail = null;
                String pickName = null;
                String it = null;
                List keyAttributes = new ArrayList();
                Set integers = new HashSet();
 
                Properties p = GeneralDataSet.loadDbProperties(name, "Select", datum);
 
                for (Enumeration psm = p.propertyNames() ; psm.hasMoreElements() ;) {
                        String itsName = (String) psm.nextElement();
                        it = p.getProperty(itsName);
                        StringTokenizer itsList = null;
                        if ("attribute".equals(itsName)) {
                                itsList = new StringTokenizer(it, ", ");
                                while (itsList.hasMoreTokens()) {
                                        String thisToken = itsList.nextToken();
                                        if ("pick".equals(thisToken)) {
                                                pickName = decodePick(datum, itsList.nextToken());
                                        } else {
                                                keyAttributes.add(thisToken);
                                        }
                                }
                        } else if ("integer".equals(itsName)) {
                                itsList = new StringTokenizer(it, ", ");
                                while (itsList.hasMoreTokens()) integers.add(itsList.nextToken());
                        } else if ("statement".equals(itsName)) {
                                statement = it;
                        } else if ("tail".equals(itsName)) {
                                tail = it;
                        }
                }
                if (pickName != null) {
                        it = p.getProperty(pickName);
                        if (it != null) statement = statement + " " + it;
                }
                if (tail != null) {
                        statement = statement + " " + tail;
                }
                if (statement == null) {
                        return;
                }
               // will force HeightCodeLookup & GrowthFormShort list to refresh
               // every time the user switches Stratum tab
                if (name.equals("HeightCodeLookup")
                   || name.equals("GrowthFormShort")) {
                   // do nothing
               } else {
                   if (keyAttributes.size() == 0) {
                        if (staticLvCache.containsKey(name)) {
                                lvList.addAll((List) staticLvCache.get(name));
                                return;
                        }
                    }
                }
                try {
                        theStmt = conn.prepareStatement(statement);
                        Iterator iter = keyAttributes.iterator();
                        String attName = null;
                        String attValue = null;
                        int paramIndex = 1;
                        while (iter.hasNext()) {
                                attName = (String) iter.next();
                                attValue = BeanUtils.getProperty(datum, attName);
                                if (attValue == null) return;

                                if (integers.contains(attName)) {
                                        if (attValue.equals("0")) return;
                                        theStmt.setInt(paramIndex, Integer.parseInt(attValue));
                                } else {
                                        theStmt.setString(paramIndex, attValue);
                                }
                                paramIndex += 1;
                        }
                        selectedRows = theStmt.executeQuery();
                        while (selectedRows.next()) {
                                lvList.add(new LabelValueBean(selectedRows.getString(1), selectedRows.getString(2)));
                        }
                        selectedRows.close();
                        if (keyAttributes.size() == 0) {
                                List nameList = new ArrayList();
                                List valueList = new ArrayList();
                                staticLvCache.put(name, lvList);
                                LabelValueBean.remakeList(lvList, nameList, valueList);
                                staticNameCache.put(name, nameList);
                                staticValueCache.put(name, valueList);
                        }
                } catch (Exception e) {
                        e.printStackTrace();
                } finally {
                        try {
                                if (selectedRows != null) selectedRows.close();
                                if (theStmt != null) theStmt.close();
                        } catch (Exception e) {
                                e.printStackTrace();
                        }
                }
        }
 
        /**
         * Run a simple select from the instructions in its property file
         * Check parameter attributes - if any are null or 0, skip the lookup and return empty lists
         * @param datum JavaBean containing source data
         * @param name String identifying a <name>Select.properties file
         * @param result List to hold Strings
         */
        public void simpleSelectUtility(Object datum, String name, List result) {
                PreparedStatement theStmt = null;
                ResultSet selectedRows = null;
 
                String statement = null;
                String tail = null;
                String likeName = null;
                String it = null;
                List keyAttributes = new ArrayList();
 
                Properties p = GeneralDataSet.loadDbProperties(name, "Select", datum);
 
                for (Enumeration psm = p.propertyNames() ; psm.hasMoreElements() ;) {
                        String itsName = (String) psm.nextElement();
                        it = p.getProperty(itsName);
                        StringTokenizer itsList = null;
                        if ("attribute".equals(itsName)) {
                                itsList = new StringTokenizer(it, ", ");
                                while (itsList.hasMoreTokens()) {
                                        String thisToken = itsList.nextToken();
                                        if ("like".equals(thisToken)) {
                                                likeName = decodeLike(datum, itsList.nextToken());
                                        } else {
                                                keyAttributes.add(thisToken);
                                        }
                                }
                        } else if ("statement".equals(itsName)) {
                                statement = it;
                        } else if ("tail".equals(itsName)) {
                                tail = it;
                        }
                }
                if (likeName != null) {
                        statement = statement + likeName;
                }
                if (tail != null) {
                        statement = statement + tail;
                }
                if (statement == null) {
                        return;
                }
                try {
                        theStmt = conn.prepareStatement(statement);
                        Iterator iter = keyAttributes.iterator();
                        String attName = null;
                        String attValue = null;
                        int paramIndex = 1;
                        while (iter.hasNext()) {
                                attName = (String) iter.next();
                                attValue = BeanUtils.getProperty(datum, attName);
                                if (attValue == null) return;
                                theStmt.setString(paramIndex, attValue);
                                paramIndex += 1;
                        }
                        selectedRows = theStmt.executeQuery();
                        while (selectedRows.next()) result.add(selectedRows.getString(1));
                } catch (Exception e) {
                        e.printStackTrace();
                } finally {
                        try {

                                if (selectedRows != null) selectedRows.close();
                                if (theStmt != null) theStmt.close();
                        } catch (Exception e) {
                                e.printStackTrace();
                        }
                }
        }
 
        /**
         * Run a simple select from the instructions in its property file
         * Check parameter attributes - if any are null or 0, skip the lookup and return empty lists
         * @param datum JavaBean containing source data
         * @param suffix String identifying a <class><suffix>.properties file
         * @param result List to hold retrieved data
         */
        public void simpleListUtility(Object datum, String suffix, List result) {
                PreparedStatement theStmt = null;
                ResultSet selectedRows = null;
 
                String statement = null;
                String tail = null;
                String likeName = null;
                String it = null;
                List keyAttributes = new ArrayList();
                List resultAttributes = new ArrayList();
 
                String className = datum.getClass().getName();
                int lastIndex = className.lastIndexOf(".");
                if (lastIndex > 0) className = className.substring(lastIndex+1);
 
                Properties p = GeneralDataSet.loadDbProperties(className, suffix, datum);
                String dateAttributes = p.getProperty("date");
                String[] dates = {};
                if (dateAttributes != null) {
                        dates = dateAttributes.split(",");
                }
                String timeAttributes = p.getProperty("time");
                String[] times = {};
                if (timeAttributes != null) {
                        times = timeAttributes.split(",");
                }
 
                for (Enumeration psm = p.propertyNames() ; psm.hasMoreElements() ;) {
                        String itsName = (String) psm.nextElement();
                        it = p.getProperty(itsName);
                        StringTokenizer itsList = null;
                        if ("attribute".equals(itsName)) {
                                itsList = new StringTokenizer(it, ", ");
                                while (itsList.hasMoreTokens()) {
                                        String thisToken = itsList.nextToken();
                                        if ("like".equals(thisToken)) {
                                                likeName = decodeLike(datum, itsList.nextToken());
                                        } else {
                                                keyAttributes.add(thisToken);
                                        }
                                }
                        } else if ("result".equals(itsName)) {
                                itsList = new StringTokenizer(it, ", ");
                                while (itsList.hasMoreTokens()) resultAttributes.add(itsList.nextToken());
                        } else if ("statement".equals(itsName)) {
                                statement = it;
                        } else if ("tail".equals(itsName)) {
                                tail = it;
                        }
                }
                if (likeName != null) {
                        statement = statement + likeName;
                }
                if (tail != null) {
                        statement = statement + tail;
                }
                if (statement == null) {
                        return;
                }
                try {
                        theStmt = conn.prepareStatement(statement);
                        Iterator iter = keyAttributes.iterator();
                        String attName = null;
                        String attValue = null;
                        int paramIndex = 1;
                        while (iter.hasNext()) {
                                attName = (String) iter.next();
                                attValue = BeanUtils.getProperty(datum, attName);
                                if (attValue == null) return;
                                theStmt.setString(paramIndex, attValue);
                                paramIndex += 1;
                        }
                        selectedRows = theStmt.executeQuery();
                        while (selectedRows.next()) {
                                Object newDatum = BeanUtils.cloneBean(datum);
                                iter = resultAttributes.iterator();
                                paramIndex = 1;
                                while (iter.hasNext()) {
                                        attName = (String) iter.next();
                                        if (isInList(times, attName)) {
                                                Timestamp workTime = selectedRows.getTimestamp(paramIndex);
                                                if (workTime != null) {
                                                        BeanUtils.setProperty(newDatum, attName, workTime);
                                                }
                                        } else if (isInList(dates, attName)) {

                                                BeanUtils.setProperty(newDatum, attName, selectedRows.getDate(paramIndex));
                                                Date workDate = selectedRows.getDate(paramIndex);
                                                if (workDate != null) {
                                                        BeanUtils.setProperty(newDatum, attName, workDate);
                                                }
                                        } else {
                                                BeanUtils.setProperty(newDatum, attName, selectedRows.getString(paramIndex));
                                        }
                                        paramIndex += 1;
                                }
                                result.add(newDatum);
                        }
                } catch (Exception e) {
                        e.printStackTrace();
                } finally {
                        try {
                                if (selectedRows != null) selectedRows.close();
                                if (theStmt != null) theStmt.close();
                        } catch (Exception e) {
                                e.printStackTrace();
                        }
                }
        }
 
        /**
         * Run a simple select with key substitution
         * Check parameter attributes - if any are null or 0, skip the lookup and return empty lists
         * @param datum JavaBean containing source data
         * @param statement String identifying a <name>Select.properties file
         * @param keys ArrayList to hold names for key variables in datum
         * @param key String for row key
         */
        public List cellSelectUtility(Object datum, String statement, List keys, String key) {
                List result = new ArrayList();
 
                if (statement == null) return result;
 
                PreparedStatement theStmt = null;
                ResultSet selectedRows = null;
                String it = null;
                try {
                        theStmt = conn.prepareStatement(statement);
                        Iterator iter = keys.iterator();
                        String attName = null;
                        String attValue = null;
                        int paramIndex = 1;
                        while (iter.hasNext()) {
                                attName = (String) iter.next();
                                if ("key".equals(attName)) {
                                        attValue = key;
                                } else {
                                        attValue = BeanUtils.getProperty(datum, attName);
                                }
                                if (attValue == null) return result;
                                theStmt.setString(paramIndex, attValue);
                                paramIndex += 1;
                        }
                        selectedRows = theStmt.executeQuery();
                        while (selectedRows.next()) result.add(selectedRows.getString(1));
                } catch (Exception e) {
                        e.printStackTrace();
                } finally {
                        try {
                                if (selectedRows != null) selectedRows.close();
                                if (theStmt != null) theStmt.close();
                        } catch (Exception e) {
                                e.printStackTrace();
                        }
                }
                return result;
        }
 
        /**
         * Execute a utility statement file for a classname, writing output to a Jdbc file
         * @param datum JavaBean identifying the table properties file
         */
        public void databaseListUtility(Object datum) throws Exception {
                PreparedStatement theStmt = null;
                ResultSet selectedRows = null;
 
                String statement = null;
                List keyAttributes = new ArrayList();
                List resultAttributes = new ArrayList();
                List integers = new ArrayList();
 
                // Get the properties set
                Properties p = GeneralDataSet.loadDbProperties(datum);
 
                for (Enumeration psm = p.propertyNames() ; psm.hasMoreElements() ;) {
                        String itsName = (String) psm.nextElement();
                        String it = p.getProperty(itsName);
                        StringTokenizer itsList = null;
                        if ("attribute".equals(itsName)) {
                                itsList = new StringTokenizer(it, ", ");
                                while (itsList.hasMoreTokens()) keyAttributes.add(itsList.nextToken());
                        } else if ("result".equals(itsName)) {
                                itsList = new StringTokenizer(it, ", ");
                                while (itsList.hasMoreTokens()) resultAttributes.add(itsList.nextToken());
                        } else if ("statement".equals(itsName)) {
                                statement = it;

                        } else if ("intFields".equals(itsName)) {
                                itsList = new StringTokenizer(it, ", ");
                                while (itsList.hasMoreTokens()) integers.add(itsList.nextToken());
                        }
                }
                if (statement == null) {
                        return;
                }
                theStmt = conn.prepareStatement(statement);
                Iterator it = keyAttributes.iterator();
                String attName = null;
                String attValue = null;
                int paramIndex = 1;
                while (it.hasNext()) {
                        attName = (String) it.next();
                        attValue = BeanUtils.getProperty(datum, attName);
                        if (attValue != null) {
                                if (integers.contains(attName)) {
                                        theStmt.setInt(paramIndex, Integer.parseInt(attValue));
                                } else {
                                        theStmt.setString(paramIndex, attValue);
                                }
                        }
                        paramIndex += 1;
                }
                try {
                        selectedRows = theStmt.executeQuery();
                        if (filePath == null) {
                                System.out.println("no file path");
                                return;
                        }
 
                        File outputFile = new File(filePath);
                        PrintWriter output = new PrintWriter(new FileOutputStream(outputFile));
                        output.print(execReflectMethod(datum, "getJdbcHeader"));
                        while (selectedRows.next()) {
                                Object newDatum = BeanUtils.cloneBean(datum);
                                it = resultAttributes.iterator();
                                paramIndex = 1;
                                while (it.hasNext()) {
                                        attName = (String) it.next();
                                        BeanUtils.setProperty(newDatum, attName, selectedRows.getString(paramIndex));
                                        paramIndex += 1;
                                }
                                output.print(execReflectMethod(newDatum, "getJdbcContent"));
                        }
                        selectedRows.close();
                        output.close();
                } catch (Exception e) {
                        e.printStackTrace();
                } finally {
                        if (selectedRows != null) selectedRows.close();
                        if (theStmt != null) theStmt.close();
                }
        }
 
        /**
         * Load a map with objects of the given type read from the database
         * @param dataMap to put the objects into
         * @param datum JavaBean of the class to load db data into
         */
        public void getDataFromDb(Map dataMap, Object datum) {
                try {
                        dataSet = GeneralDataSet.getDataset(datum);
                        executeSelectAll(datum, dataMap);
                } catch(Exception e) {
                        System.out.println("SQL Exception ");
                        e.printStackTrace();
                }
        }
 
        /**
         * Return the current database connection (null if not connected
         * @return Connection the database connection defined in database.properties
         */
        public Connection getDBConnection() {
                return conn;
        }
 
        /**
         * Return the current database connection (null if not connected
         * @return Connection the database connection defined in database.properties
         */
        public void disconnect() {
                try {
                        if (conn != null) conn.close();
                } catch (Exception e) {
                        e.printStackTrace();
                }
                conn = null;
        }
 
        /**
         * Initialise the database connection using parameters from a database.properties
         * @param connectionString the whole thing including user and password
         * @return Connection the database connection defined in database.properties
         */
        public Connection getDBConnection(String connectionString) {
                try {
                        System.out.println("Attempting connection to " + connectionString);

                        if (conn != null) conn.close();
                        // Load the Oracle JDBC driver
                        if (!driverLoaded) {
                                DriverManager.registerDriver(new oracle.jdbc.driver.OracleDriver());
                                driverLoaded = true;
                        }
                        connectionName = (connectionString.startsWith("jdbc:oracle:thin:")) ? connectionString :
                                "jdbc:oracle:thin:" + connectionString;
                        conn = DriverManager.getConnection(connectionName);
                        // Create Oracle DatabaseMetaData object
                        meta = conn.getMetaData();
                        // gets driver info:
                        System.out.println("JDBC driver version is " + meta.getDriverVersion());
                } catch (Exception e) {
                        e.printStackTrace();
                        conn = null;
                }
                return conn;
        }
 
        /**
         * Initialise the database connection using parameters from a database.properties
         * @param connectionString the whole thing including user and password
         * @return Connection the database connection defined in database.properties
         */
        public DataSource getDataSource(String propertyFile) {
                OracleDataSource ds = null;
                InputStream f = null;
                Properties p = new Properties();
                if (jdp == null) return null;
 
                try {
                        f = jdp.getClass().getClassLoader().getResourceAsStream(propertyFile);
                        if (f != null) {
                                p.load(f);
                                f.close();
                        }
                        String userName = p.getProperty("user");
                        String passWord = p.getProperty("password");
                        String serverName = p.getProperty("node", "jagaroth");
                        String portNo = p.getProperty("port", "1521");
                        String schemaName = p.getProperty("schema", "CL_DEV");
                        ds = new OracleDataSource();
                        ds.setDatabaseName(schemaName);
                        ds.setPortNumber(Integer.parseInt(portNo));
                        ds.setDriverType("thin");
                        ds.setServerName(serverName);
                        ds.setUser(userName);
                        ds.setPassword(passWord);
                } catch (Exception e) {
                        e.printStackTrace();
                }
                return ds;
 
        }
 
 
        /**
         * Return a connection string made from the details
         * found in the named properties file
         * @param propertyFile name for the file
         * @return String the database connection string made from database.properties
         */
        public static String getConnectionName(String propertyFile, Object location) {
                String result = "";
                InputStream f = null;
                Properties p = new Properties();
                if (jdp == null) return null;
 
                try {
                        f = location.getClass().getClassLoader().getResourceAsStream(propertyFile);
                        if (f != null) {
                                p.load(f);
                                f.close();
                        }
                        String userName = p.getProperty("user");
                        String passWord = p.getProperty("password");
                        String serverName = p.getProperty("node", "jagaroth");
                        String portNo = p.getProperty("port", "1521");
                        String schemaName = p.getProperty("schema", "CL_DEV");
                        StringBuffer connectionString = new StringBuffer();
                        connectionString.append("jdbc:oracle:thin:").append(userName).append("/").append(passWord);
                        connectionString.append("@").append(serverName).append(":").append(portNo).append(":").append(schemaName);
                        result = connectionString.toString();
                } catch (Exception e) {
                        e.printStackTrace();
                }
                return result;
        }
 
        /**
         * Return a connection string made from the details
         * found in the named properties file
         * @param propertyFile name for the file
         * @return String the database connection string made from database.properties
         */
        public static String getConnectionName(String propertyFile) {
                String result = "";
                InputStream f = null;
                Properties p = new Properties();

                if (jdp == null) return null;
 
                try {
                        f = jdp.getClass().getClassLoader().getResourceAsStream(propertyFile);
                        if (f != null) {
                                p.load(f);
                                f.close();
                        }
                        String userName = p.getProperty("user");
                        String passWord = p.getProperty("password");
                        String serverName = p.getProperty("node", "jagaroth");
                        String portNo = p.getProperty("port", "1521");
                        String schemaName = p.getProperty("schema", "CL_DEV");
                        StringBuffer connectionString = new StringBuffer();
                        connectionString.append("jdbc:oracle:thin:").append(userName).append("/").append(passWord);
                        connectionString.append("@").append(serverName).append(":").append(portNo).append(":").append(schemaName);
                        result = connectionString.toString();
                } catch (Exception e) {
                        e.printStackTrace();
                }
                return result;
        }
 
        /**
         * Return the JDBC connection status
         * @return boolean true if connection is open
         */
        public boolean isConnected() {
                return (conn != null);
        }
 
        /**
         * Start a transaction:
         */
        public void startTransaction() {
                try {
                        if (!transactionCurrent) {
                                transactionCurrent = true;
                                transactionError = false;
                                if (conn != null) conn.setAutoCommit(false);
                        }
                } catch (Exception e) {
                        e.printStackTrace();
                }
        }
 
        /**
         * Commit a transaction, or roll it back if errors occurred:
         * @return boolean true if transaction committed
         */
        public boolean completeTransaction() {
                boolean result = false;
                try {
                        if (transactionCurrent) {
                                if (transactionError) {
                                        if (conn != null) conn.rollback();
                                } else {
                                        if (conn != null) conn.commit();
                                        result = true;
                                }
                                transactionError = false;
                                transactionCurrent = false;
                        }
                } catch (Exception e) {
                        e.printStackTrace();
                }
                return result;
        }
 
        /**
         * Return the singleton JDBC processor
         * @return JdbcDataProcessor JDBC logic container
         */
        public static JdbcDataProcessor getJdp() {
                return jdp;
        }
 
        /**
         * Return the singleton JDBC processor
         * @return JdbcDataProcessor JDBC logic container
         */
        public static JdbcDataProcessor getNewJdp() {
                return new JdbcDataProcessor();
        }
 
        private Properties getProperties(Object datum, String suffix) {
 
                StringBuffer buildName = new StringBuffer();
                String resourceName = datum.getClass().getName();
                buildName.append(resourceName.replace('.', '/'));
                if (suffix != null) buildName.append(suffix);
                buildName.append(".properties");
 
                InputStream f = null;
                // Start with the default properties set
                Properties p = new Properties();
                try {
                        f = datum.getClass().getClassLoader().getResourceAsStream(buildName.toString());
                        if (f != null) {
                                p.load(f);

                                f.close();
                        }
                } catch (Exception e) {
                        System.out.println("*** No properties found for " + buildName.toString());
                        e.printStackTrace();
                }
                return p;
        }
 
        private String getProperties(Object datum, String suffix, List keyAttributes, List resultAttributes,
                        List listAttributes, List sequenceValues,
                        List statements, List integers) {
                String statement = null;
                Properties p = getProperties(datum, suffix);
                StringTokenizer itsList = null;
                String it = p.getProperty("attribute");
                if (it != null) {
                        itsList = new StringTokenizer(it, ", ");
                        while (itsList.hasMoreTokens()) keyAttributes.add(itsList.nextToken());
                }
                it = p.getProperty("result");
                if (it != null) {
                        itsList = new StringTokenizer(it, ", ");
                        while (itsList.hasMoreTokens()) {
                                String checkList = itsList.nextToken();
                                if ("list".equals(checkList)) {
                                        while (itsList.hasMoreTokens()) listAttributes.add(itsList.nextToken());
                                } else {
                                        resultAttributes.add(checkList);
                                }
                        }
                }
                it = p.getProperty("sequence");
                if (it != null) {
                        itsList = new StringTokenizer(it, ", ");
                        while (itsList.hasMoreTokens()) sequenceValues.add(itsList.nextToken());
                }
                it = p.getProperty("statement");
                if (it != null) {
                        statement = it;
                }
                it = p.getProperty("keystatement");
                if (it != null) {
                        statements.add(it);
                }
                it = p.getProperty("intFields");
                if (it != null) {
                        itsList = new StringTokenizer(it, ", ");
                        while (itsList.hasMoreTokens()) integers.add(itsList.nextToken());
                }
 
                return statement;
        }
 
        private void doXmlSelect(String statement, List results, List keyAttributes, List integers, List resultAttributes, Object datum, String sequenceValue) throws Exception {
                PreparedStatement theStmt = null;
                ResultSet selectedRows = null;
                String attName = null;
                String attValue = null;
                Iterator it = keyAttributes.iterator();
                int paramIndex = 1;
                try {
                        theStmt = conn.prepareStatement(statement);
                        while (it.hasNext()) {
                                attName = (String) it.next();
                                if ("sequence".equals(attName)) {
                                        if (sequenceValue != null) theStmt.setString(paramIndex, sequenceValue);
                                } else {
                                        attValue = BeanUtils.getProperty(datum, attName);
                                        if (attValue != null) {
                                                if (integers.contains(attName))
                                                        theStmt.setInt(paramIndex, Integer.parseInt(attValue));
                                                else
                                                        theStmt.setString(paramIndex, attValue);
                                        }
                                }
                                paramIndex++;
                        }
                        selectedRows = theStmt.executeQuery();
                        while (selectedRows.next()) {
                                Object newDatum = BeanUtils.cloneBean(datum);
                                it = resultAttributes.iterator();
                                paramIndex = 1;
                                while (it.hasNext()) {
                                        attName = (String) it.next();
                                        BeanUtils.setProperty(newDatum, attName, selectedRows.getString(paramIndex));
                                        paramIndex++;
                                }
                                results.add(newDatum);
                        }
                } catch (Exception e) {
                        e.printStackTrace();
                } finally {
                        if (selectedRows != null)       selectedRows.close();
                        if (theStmt != null) theStmt.close();
                }
        }
 
        private void doListSelect(String statement, List results, List keyAttributes, List integers,
                        List resultAttributes, List listAttributes, List statements,

                        Object datum, String sequenceValue) throws Exception {
                Object newDatum = BeanUtils.cloneBean(datum);
                PreparedStatement theStmt = null;
                ResultSet selectedRows = null;
                String attName = null;
                String attValue = null;
                try {
                        for (int i=0; i < statements.size(); i++) {
                                String thisStatement = (String) statements.get(i);
                                doStatementSelect(thisStatement, newDatum, resultAttributes, sequenceValue);
                        }
                } catch (Exception e) {
                        e.printStackTrace();
                }
                Iterator it = keyAttributes.iterator();
                int paramIndex = 1;
                try {
                        theStmt = conn.prepareStatement(statement);
                        while (it.hasNext()) {
                                attName = (String) it.next();
                                if ("sequence".equals(attName)) {
                                        if (sequenceValue != null) theStmt.setString(paramIndex, sequenceValue);
                                } else {
                                        attValue = BeanUtils.getProperty(datum, attName);
                                        if (attValue != null) {
                                                if (integers.contains(attName))
                                                        theStmt.setInt(paramIndex, Integer.parseInt(attValue));
                                                else
                                                        theStmt.setString(paramIndex, attValue);
                                        }
                                }
                                paramIndex++;
                        }
                        selectedRows = theStmt.executeQuery();
                        it = listAttributes.iterator();
                        while (selectedRows.next()) {
                                paramIndex = 1;
                                if (it.hasNext()) {
                                        attName = (String) it.next();
                                        BeanUtils.setProperty(newDatum, attName, selectedRows.getString(paramIndex));
                                        paramIndex++;
                                }
                        }
                        results.add(newDatum);
                } catch (Exception e) {
                        e.printStackTrace();
                } finally {
                        if (selectedRows != null)       selectedRows.close();
                        if (theStmt != null) theStmt.close();
                }
        }
 
        private void doStatementSelect(String statement, Object datum, List resultAttributes, String sequenceValue) throws Exception {
                PreparedStatement theStmt = null;
                ResultSet selectedRows = null;
                String attName = null;
                String attValue = null;
                int paramIndex = 1;
                try {
                        theStmt = conn.prepareStatement(statement);
                        if (sequenceValue != null) theStmt.setString(paramIndex, sequenceValue);
                        selectedRows = theStmt.executeQuery();
                        while (selectedRows.next()) {
                                paramIndex = 1;
                                Iterator it = resultAttributes.iterator();
                                while (it.hasNext()) {
                                        attName = (String) it.next();
                                        BeanUtils.setProperty(datum, attName, selectedRows.getString(paramIndex));
                                        paramIndex++;
                                }
                        }
                } catch (Exception e) {
                        e.printStackTrace();
                } finally {
                        if (selectedRows != null)       selectedRows.close();
                        if (theStmt != null) theStmt.close();
                }
        }
 
        public boolean isInList(String[] ref, String value) {
                if (ref.length > 0 && value != null)
                        for (String check : ref) {
                                if (value.trim().equals(check.trim())) return true;
                        }
                return false;
        }
 
        public Object getData(String keyValue, Object datum, String suffix) {
                System.out.println("Entering getData with " + keyValue + ", " + suffix + " and " + datum);
                Properties p = getProperties(datum, suffix);
                StringTokenizer itsList = null;
                String keyAttributes = p.getProperty("attribute");
                String resultAttributes = p.getProperty("result");
                String dateAttributes = p.getProperty("date");
                String[] dates = {};
                if (dateAttributes != null) {
                        dates = dateAttributes.split(",");
                }
                String statement = p.getProperty("statement");
                if (statement == null) return null;

                PreparedStatement theStmt = null;
                ResultSet selectedRows = null;
                String attName = null;
                String attValue = null;
                int paramIndex = 1;
                try {
                        theStmt = conn.prepareStatement(statement);
                        if (keyAttributes != null) {
                                itsList = new StringTokenizer(keyAttributes, ", ");
                                while (itsList.hasMoreTokens()) {
                                        attName = itsList.nextToken();
                                        attValue = BeanUtils.getProperty(datum, attName);
                                        if (attValue != null) {
                                                theStmt.setString(paramIndex, attValue);
                                                paramIndex++;
                                        }
                                }
                        }
                        selectedRows = theStmt.executeQuery();
                        if (selectedRows.next()) {
                                itsList = new StringTokenizer(resultAttributes, ", ");
                                paramIndex = 1;
                                while (itsList.hasMoreTokens()) {
                                        attName = itsList.nextToken();
                                        if (isInList(dates, attName)) {
                                                BeanUtils.setProperty(datum, attName, selectedRows.getDate(paramIndex));
                                        } else {
                                                BeanUtils.setProperty(datum, attName, selectedRows.getString(paramIndex));
                                        }
                                        paramIndex++;
                                }
                        }
                } catch (Exception e) {
                        e.printStackTrace();
                } finally {
                        try {
                                if (selectedRows != null)
                                        selectedRows.close();
                                if (theStmt != null)
                                        theStmt.close();
                        } catch(Exception ex) {}
                }
                return datum;
        }
        /**
         * applyMethod finds a compatible method on the target object and applies the data to it.
         * @param toObject target
         * @param methodName name of method
         * @param withValue value
         * @throws Exception
         */
        private Object applyMethod(Object toObject, String methodName) throws Exception {
                Class parentClass = toObject.getClass();
                Method[] possibles = parentClass.getMethods();
                for (Method possible : possibles) {
                        if (methodName.equals(possible.getName())) {
                                Class[] params = possible.getParameterTypes();
                                if (params.length == 0) {
                                        return possible.invoke(toObject, new Object[] {});
                                }
                        }
                }
                return null;
        }
 
        public static byte[] getOracleBlob(ResultSet result, int order) throws SQLException {
 
                ByteArrayOutputStream outputStream = null;
                try {
 
                        oracle.sql.BLOB blob = ((oracle.jdbc.OracleResultSet)result).getBLOB(order);
 
                        InputStream inputStream = blob.getBinaryStream();
                        outputStream = new ByteArrayOutputStream();
 
                        int bytesRead = 0;
 
                        while((bytesRead = inputStream.read()) != -1) {
 
                                outputStream.write(bytesRead);
 
                        }
 
                        inputStream.close();
 
                        outputStream.close();
 
                } catch(IOException e) {
 
                        e.printStackTrace(System.err);
 
                        throw new SQLException(e.getMessage());
 
                }
                if (outputStream != null) {
                        return outputStream.toByteArray();
                }
                return new byte[] {};
        }
 

        public static void setOracleBlob(ResultSet result, int order, byte[] data) throws SQLException {
 
                try {
 
                        ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
 
                        oracle.sql.BLOB blob = ((oracle.jdbc.OracleResultSet)result).getBLOB(order);
 
                        OutputStream outputStream = blob.getBinaryOutputStream();
 
                        int bytesRead = 0;
 
                        while((bytesRead = inputStream.read()) != -1) {
 
                                outputStream.write(bytesRead);
 
                        }
 
                        inputStream.close();
 
                        outputStream.close();
 
                } catch(IOException e) {
 
                        e.printStackTrace(System.err);
 
                        throw new SQLException(e.getMessage());
 
                }
 
        }
 
        public void updateBlob(String stmt, byte[] data, Object datum, String key) {
                PreparedStatement theStmt = null;
                ResultSet rs = null;
                String attValue;
                if (conn == null) {
                        conn = jdp.getDBConnection(getConnectionName("database.properties"));
                }
                try {
                        if (conn != null) {
                                conn.setAutoCommit(false);
                                theStmt = conn.prepareStatement(stmt);
                                attValue = BeanUtils.getProperty(datum, key);
                                if (attValue != null) {
                                        theStmt.setString(1, attValue);
                                        rs = theStmt.executeQuery();
                                        if (rs.next()) {
                                                setOracleBlob(rs, 1, data);
                                        }
                                }
                        }
                } catch (Exception e) {
                        e.printStackTrace();
                } finally {
                        try {
                                if (rs != null) rs.close();
                                if (theStmt != null) theStmt.close();
                                conn.commit();
                                conn.setAutoCommit(true);
                        } catch (Exception e1) {
                                e1.printStackTrace();
                        }
                }
        }
 
        /* public void updateBlob(String stmt, byte[] data, Object datum, String key) {
    PreparedStatement theStmt = null;
    String attValue;
    if (conn == null) {
      conn = jdp.getDBConnection(getConnectionName("database.properties"));
    }
    try {
      if (conn != null) {
        oracle.sql.BLOB theBlob = new oracle.sql.BLOB();
        theBlob.setBytes(data);
       theStmt = conn.prepareStatement(stmt);
       theStmt.setBlob(1, theBlob);
        attValue = BeanUtils.getProperty(datum, key);
        if (attValue != null) {
          theStmt.setString(2, attValue);
          theStmt.executeUpdate();
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      try {
        if (theStmt != null) theStmt.close();
      } catch (Exception e1) {
        e1.printStackTrace();
      }
    }
 } */
 
        public List getData(String keyValue, Object datum) {
                List result = new ArrayList();
                if (conn == null) {
                        conn = jdp.getDBConnection(getConnectionName("database.properties"));
                }

                if (conn != null) {
                        List keyAttributes = new ArrayList();
                        List resultAttributes = new ArrayList();
                        List listAttributes = new ArrayList();
                        List sequenceValues = new ArrayList();
                        List statements = new ArrayList();
                        List integers = new ArrayList();
                        String statement = getProperties(datum, null, keyAttributes, resultAttributes, listAttributes, sequenceValues, statements, integers);
                        if (statement != null) {
                                if (sequenceValues.size() == 0 && keyValue != null) {
                                        StringTokenizer it = new StringTokenizer(keyValue, ",");
                                        while (it.hasMoreTokens()) {
                                                sequenceValues.add(it.nextToken());
                                        }
                                }
                                try {
                                        if (sequenceValues.size() == 0) {
                                                if (listAttributes.size() == 0) {
                                                        doXmlSelect(statement, result, keyAttributes, integers, resultAttributes, datum, null);
                                                } else {
                                                        doListSelect(statement, result, keyAttributes, integers, resultAttributes, listAttributes, statements, datum, null);
                                                }
                                        } else {
                                                for (int i = 0; i < sequenceValues.size(); i++) {
                                                        if (listAttributes.size() == 0) {
                                                                doXmlSelect(statement, result, keyAttributes, integers, resultAttributes, datum, (String) sequenceValues.get(i));
                                                        } else {
                                                                doListSelect(statement, result, keyAttributes, integers, resultAttributes, listAttributes, statements, datum, (String) sequenceValues.get(i));
                                                        }
                                                }
                                        }
                                } catch (Exception e) {
                                        e.printStackTrace(); // Catch errors thrown by cleanup in doXmlSelect's finally clause
                                }
                        }
                }
 
                return result;
        }
 
        public List getListData(Object datum, String suffix) {
                List result = new ArrayList();
                if (conn == null) {
                        conn = jdp.getDBConnection(getConnectionName("database.properties"));
                }
                if (conn != null) {
                        List keyAttributes = new ArrayList();
                        List resultAttributes = new ArrayList();
                        List listAttributes = new ArrayList();
                        List sequenceValues = new ArrayList();
                        List statements = new ArrayList();
                        List integers = new ArrayList();
                        String statement = getProperties(datum, suffix, keyAttributes, resultAttributes, listAttributes, sequenceValues, statements, integers);
                        if (statement != null) {
                                try {
                                        if (sequenceValues.size() == 0) {
                                                if (listAttributes.size() == 0) {
                                                        doXmlSelect(statement, result, keyAttributes, integers, resultAttributes, datum, null);
                                                } else {
                                                        doListSelect(statement, result, keyAttributes, integers, resultAttributes, listAttributes, statements, datum, null);
                                                }
                                        } else {
                                                for (int i = 0; i < sequenceValues.size(); i++) {
                                                        if (listAttributes.size() == 0) {
                                                                doXmlSelect(statement, result, keyAttributes, integers, resultAttributes, datum, (String) sequenceValues.get(i));
                                                        } else {
                                                                doListSelect(statement, result, keyAttributes, integers, resultAttributes, listAttributes, statements, datum, (String) sequenceValues.get(i));
                                                        }
                                                }
                                        }
                                } catch (Exception e) {
                                        e.printStackTrace(); // Catch errors thrown by cleanup in doXmlSelect's finally clause
                                }
                        }
                }
 
                return result;
        }
 
 
}

