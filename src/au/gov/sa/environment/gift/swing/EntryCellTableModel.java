package au.gov.sa.environment.gift.swing;
 
import javax.swing.*;
import java.util.*;
 
import au.gov.sa.environment.gift.general.*;
import au.gov.sa.environment.gift.jdbc.*;
 
/**
 * EntryCellTableModel - Model for a table that picks row data from selects
 * Entries
 */
public class EntryCellTableModel extends EntryTableModel {
 
 private List<String> rowNames = new ArrayList();
 private List<String> rowKeys = new ArrayList();
 private List<String> selectKeys = new ArrayList();
 private List rowData = new ArrayList();
 private List<String> rowName = new ArrayList();
 private Map calcMap = new HashMap();
 private String nameSelect = null;
 private String rowSelect = null;
 /**
   * No-arg constructor
   */
 public EntryCellTableModel() {
 
 }
 /**
   * Superclass constructor handles <class>Table.properties
   * describing the structure, then we work out the cell selects
   */
 public EntryCellTableModel(Object datum, Properties p, JFrame frame, GeneralDataProcessor general) {
   super(datum, p, frame, general);
 
   String[] itsList;
   String prop = p.getProperty("row");
   if (prop != null) {
     itsList = prop.split("\\s*,\\s*");
     for (String it : itsList) {
       applyName(it);
     }
   }
   prop = p.getProperty("rowSelect");
   if (prop != null) rowSelect = prop;
   prop = p.getProperty("selectKeys");
   if (prop != null) {
     itsList = prop.split("\\s*,\\s*");
     selectKeys = Arrays.asList(itsList);
   }
   for (int i=0; i < attributes.size(); i++) {
     String attName = (String) attributes.get(i);
     String attType = (String) columnType.get(i);
     if ("calc".equals(attType)) {
       prop = p.getProperty(attName + "Calc");
       if (prop != null) calcMap.put(attName, prop);
     } else if ("data".equals(attType)) {
       rowName.add(attName);
     } else if ("checkbox".equals(attType)) {
       rowName.add(attName);
     }
   }
 }
 
 private void applyName(String nameText) {
   int equalPos = nameText.indexOf("=");
   if (equalPos <= 0) {
     rowNames.add(nameText);
     rowKeys.add(nameText);
   } else {
     rowNames.add(nameText.substring(0,equalPos));
     rowKeys.add(nameText.substring(equalPos+1));
   }
 }
 
 public int getRowCount() {
   return rowNames.size();
 }
 
 public int getColumnCount() {
   return columnHeader.size();
 }
 
 public Object getValueAt(int row, int col) {
   JdbcDataProcessor jdp = JdbcDataProcessor.getJdp();
   if (!jdp.isConnected()) return null;
 
   if (col == 0) return rowNames.get(row);
   String colType = (String) columnType.get(col);
 
   List result = null;
   if (row < rowData.size()) {
     result = (List) rowData.get(row);
   }
   if (result == null) {
     String key = (String) rowKeys.get(row);
     result = jdp.cellSelectUtility(datum, rowSelect, selectKeys, key);
     while (row > rowData.size()) rowData.add(null);
     rowData.add(result);
   }

   if ("data".equals(colType)) {
     String colName = (String) attributes.get(col);
     int datumIndex = rowName.indexOf(colName);
     if (datumIndex >= 0 && datumIndex < result.size())
       return result.get(datumIndex);
   } else if ("checkbox".equals(colType)) {
     String colName = (String) attributes.get(col);
     int datumIndex = rowName.indexOf(colName);
     if (datumIndex >= 0 && datumIndex < result.size()) {
       String checkValue = (String) result.get(datumIndex);
       return new Boolean(!"0".equals(checkValue));
     }
   } else if ("calc".equals(colType)) {
     String attName = (String) attributes.get(col);
     String attCalc = (String) calcMap.get(attName);
     return calculate(attCalc, result);
   }
   return null;
 }
 
 public String calculate(String calc, List rowData) {
   StringBuffer result = new StringBuffer("Patience");
   Map values = new HashMap();
   for (int i=0; i < rowData.size(); i++) {
     String cellName = (String) rowName.get(i);
     String cellData = (String) rowData.get(i);
     if (cellName != null && cellData != null) {
       values.put(cellName, cellData);
     }
   }
   String parseMarks = "%()+-/*";
   StringTokenizer parse = new StringTokenizer(calc, parseMarks, true);
   String token;
   String peek;
   String data;
   String op;
   int tokenIndex;
   Stack state = new Stack();
   Float work = null;
   try {
     while (parse.hasMoreTokens()) {
       token = parse.nextToken();
       tokenIndex = parseMarks.indexOf(token);
       switch (tokenIndex) {
         case 0: state.push(token); break; // %
         case 1: state.push(token); break; // (
         case 2:                           // )
           peek = (String) state.peek();
           if ("(".equals(peek)) state.pop();
           break;
         case 3: state.push(token); break; // +
         case 4: state.push(token); break; // -
         case 5: state.push(token); break; // Slash
         case 6: state.push(token); break; // Star
         default:
           if (!values.containsKey(token)) break;
             data = (String) values.get(token);
             if (work == null) {
               work = new Float(data);
             } else {
               op = (String) state.pop();
               tokenIndex = parseMarks.indexOf(op);
               switch (tokenIndex) {
                 case 3:
                    work = new Float(work.floatValue() + Float.parseFloat(data));
                    break;
                 case 4:
                    work = new Float(work.floatValue() - Float.parseFloat(data));
                    break;
                 case 5:
                    work = new Float(work.floatValue() / Float.parseFloat(data));
                    break;
                 case 6:
                    work = new Float(work.floatValue() * Float.parseFloat(data));
                    break;
               }
             }
             break;
         }
     }
   } catch (Exception e) {
     work = new Float("-9999.99");
   }
   if (work == null) return "";
   if (!state.empty()) {
     op = (String) state.pop();
     if ("%".equals(op)) {
       return StringUtil.convertFloat(work.floatValue() * Float.parseFloat("100.0"));
     }
   }
   return work.toString();
 }
 
public boolean isCellEditable(int row, int col)
     { return false; }
 
 public void setContainedData(Object datum, Map contextMap) {
   this.datum = datum;
   this.contextMap = contextMap;
   rowData.clear();

   fireTableStructureChanged();
 }
 
   /*
     * JTable uses this method to determine the default renderer/
     * editor for each cell.
     */
   public Class getColumnClass(int col) {
     Class result = null;
     try {
     String colType = (String) columnType.get(col);
     result = ("checkbox".equals(colType)) ? Class.forName("java.lang.Boolean") : Class.forName("java.lang.String");
     } catch (Exception e) {
       e.printStackTrace();
     }
     return result;
   }
 
}
 
 

