package au.gov.sa.environment.gift.swing;
 
import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.*;
 
import org.apache.commons.beanutils.BeanUtils;
 
import au.gov.sa.environment.gift.general.StringUtil;
import au.gov.sa.environment.gift.jdbc.*;
 
/**
 * EntryTableModel - manage a gift table
 * Processes the <datum>Table.properties to make the table structure
 */
public class EntryTableModel extends AbstractTableModel implements TableCellRenderer {
 
 protected JFrame frame;
 protected java.util.List data = new ArrayList();
 protected java.util.List attributes = new ArrayList();
 protected java.util.List structures = new ArrayList();
 protected java.util.List columnHeader = new ArrayList();
 protected java.util.List columnType = new ArrayList();
 protected java.util.List columnWidth = new ArrayList();
 protected String beanTitle = "Object List";
 protected String confirmDelete="object";
 protected String confirmDeleteAttribute=null;
 protected String preferredWidth = "500";
 protected String preferredHeight = "250";
 protected StringBuffer dialogResult = new StringBuffer();
 protected Object datum = null;
 protected Map contextMap = null;
 
 private int newLine = 0;
 
 /**
   * No-arg constructor
   */
 public EntryTableModel() {
 
 }
 /**
   * Real world constructor takes the dataObject whose <class>Table.properties
   * describes its structure and the list of data objects for the table
   */
 public EntryTableModel(Object datum, Properties p, JFrame frame, GeneralDataProcessor general) {
   this.frame = frame;
   this.datum = datum;
   String it = p.getProperty("line");
   String[] itsList;
   if (it != null) {
     itsList = it.split("\\s*,\\s*");
     attributes = Arrays.asList(itsList);
   }
   it = p.getProperty("newline");
   if (it != null) {
     try {
       newLine = Integer.parseInt(it);
     } catch (Exception e) {
       newLine = 0;
     }
   }
   it = p.getProperty("head");
   if (it != null) {
     itsList = it.split("\\s*,\\s*");
     columnHeader = Arrays.asList(itsList);
   }
   it = p.getProperty("width");
   if (it != null) {
     itsList = it.split("\\s*,\\s*");
     columnWidth = Arrays.asList(itsList);
   }
   it = p.getProperty("type");
   if (it != null) {
     itsList = it.split("\\s*,\\s*");
     columnType = Arrays.asList(itsList);
   }
   it = p.getProperty("confirmDelete");
   if (it != null) {
     itsList = it.split("\\s*,\\s*");
     if (itsList.length > 0) confirmDelete = itsList[0];
     if (itsList.length > 1) confirmDeleteAttribute = itsList[1];
   }
   String thisName = null;
   String thisType = null;
   String thisSize = null;
   String editClass = null;
   EntryStructure es = null;
 
   for (int i=0; i<attributes.size(); i++) {
     es = null;
     thisName = (String) attributes.get(i);
     thisType = null;
     if (i < columnType.size()) thisType = (String) columnType.get(i);
     thisSize = "10";
     if (i < columnWidth.size()) thisSize = (String) columnWidth.get(i);
     if (thisType != null) {
       if (thisType.equals("popup")) {
         es = new EntryStructurePopupMenu(thisName, thisSize);

       } else {
         es = new EntryStructure(thisName, thisType, thisSize);
       }
       if (thisType.equals("edit")) {
         editClass = p.getProperty(thisName + "Edit");
         es.setAction(editClass);
       } else if (thisType.equals("load")) {
         editClass = p.getProperty(thisName + "Edit");
         es.setAction(editClass);
       } else if (thisType.equals("button") || thisType.equals("popup")) {
         editClass = p.getProperty(thisName + "Icon");
         if (editClass != null) {
             itsList = editClass.split("\\s*,\\s*");
            if (itsList.length > 0) {
             es.setButtonIcon(EntryStructure.createImageIcon(itsList[0]));
           }
           if (itsList.length > 1) {
             es.setAlternateIcon(EntryStructure.createImageIcon(itsList[1]));
           }
         }
         editClass = p.getProperty(thisName + "Action");
         if (editClass != null) {
           es.setAction(editClass);
         }
       }
     }
     es.setGeneral(general);
     structures.add(es);
   }
 
 }
 
 public void setTableEditors(JTable table, Map contextMap, EntryFrameStructure efs) {
   this.contextMap = contextMap;
   EntryStructure es = null;
   for (int i=0; i<structures.size(); i++) {
     es = (EntryStructure) structures.get(i);
       TableColumn thisColumn = table.getColumnModel().getColumn(i);
     if (es.isCustomList()) {
       Object it = es.getTextComponent();
       if (it instanceof JComboBox)
       thisColumn.setCellEditor(new DefaultCellEditor((JComboBox) it));
     } else {
       if (es.getType().equals("edit") || es.getType().equals("load") || es.getType().equals("button")) {
         DialogCellEditor theEditor = new DialogCellEditor(es, frame, datum, contextMap, this);
         theEditor.postCreate();
         thisColumn.setCellEditor(theEditor);
       } else if (es.getType().equals("popup")) {
         es.postCreate(efs);
         DialogCellPopup theEditor = new DialogCellPopup(es, frame, datum, contextMap, this);
         theEditor.postCreate();
         thisColumn.setCellEditor(theEditor);
       }
     }
   }
   table.setDefaultRenderer (JButton.class, this);
 }
 
 public TableColumnModel getTableColumnModel() {
   EntryStructure es = null;
   TableColumnModel theModel = new DefaultTableColumnModel();
   TableColumn thisColumn = null;
   String it = null;
   Object text = null;
   boolean setWidth = false;
   int width = -1;
   for (int i=0; i<structures.size(); i++) {
     es = (EntryStructure) structures.get(i);
     if (i < columnWidth.size()) {
       it = (String) columnWidth.get(i);
       width = Integer.parseInt(it);
       setWidth = width > 0;
     } else {
       width = 75;
       setWidth = false;
     }
     thisColumn = new TableColumn(i, width);
     if (setWidth) thisColumn.setMaxWidth(width);
     it = "";
     if (i < columnHeader.size()) {
       it = (String) columnHeader.get(i);
     }
     thisColumn.setHeaderValue(it);
     if (es.isCustomList()) {
       text = es.getTextComponent();
       if (text instanceof JComboBox)
       thisColumn.setCellEditor(new DefaultCellEditor((JComboBox) text));
     }
     theModel.addColumn(thisColumn);
   }
   return theModel;
 }
   public String getColumnName(int col) {
     String result = null;
     if (col < columnHeader.size())
       result = (String) columnHeader.get(col);
     return result;
   }
 
   public int getRowCount() {

     if (data == null) return newLine;
     return data.size() + newLine;
   }
 
   public int getColumnCount() {
     if (attributes == null) return 0;
     return attributes.size();
   }
 
   public Object getValueAt(int row, int col) {
     if (data == null) return null;
     Object result = null;
     try {
       if (data == null || data.size() <= row) {
         return null;
       } else {
         Object datum = data.get(row);
         EntryStructure es = (EntryStructure) structures.get(col);
         if (es == null) {
           String name = (String) attributes.get(col);
           result = BeanUtils.getProperty(datum, name);
         } else {
           if (!es.isActionable()) result = es.convertValueToName(datum);
         }
         if (result instanceof Date) {
                 result = StringUtil.toDateTimeString((Date) result);
         }
       }
     } catch (Exception e) {
       return null;
     }
     return result;
   }
 
   public Object getRowDatum(int row) {
     Object datum = null;
     if (data != null && data.size() > row) {
       datum = data.get(row);
     }
     return datum;
   }
 
   public void addRowDatum(Object datum) {
     if (data != null ) {
       data.add(datum);
       fireTableStructureChanged();
     }
   }
 
   public void removeRowDatum(Object datum) {
     if (data != null ) {
       data.remove(data.indexOf(datum));
       fireTableStructureChanged();
     }
   }
 
   public boolean isCellEditable(int row, int col)
       { return true; }
 
   public void setValueAt(Object value, int row, int col) {
     try {
       if (value != null) {
         String stringValue = value.toString();
         Object datum = data.get(row);
         String name = (String) attributes.get(col);
         BeanUtils.setProperty(datum, name, stringValue);
         fireTableCellUpdated(row, col);
       }
     } catch (Exception e) {
       e.printStackTrace();
     }
   }
 
   public void setData(java.util.List data, Map contextMap) {
     this.data = data;
     this.contextMap = contextMap;
     fireTableStructureChanged();
   }
 
   public void setNewLine(int newLine) {
     this.newLine = newLine;
   }
 
   public int getNewLine() {
     return newLine;
   }
 
   public void setConfirmDelete(String confirmDelete) {
     this.confirmDelete = confirmDelete;
   }
 
   public String getConfirmDelete() {
     return confirmDelete;
   }
 
   public void setConfirmDeleteAttribute(String confirmDeleteAttribute) {
     this.confirmDeleteAttribute = confirmDeleteAttribute;
   }
 
   public String getConfirmDeleteAttribute() {

     return confirmDeleteAttribute;
   }
 
   /*
     * JTable uses this method to determine the default renderer/
     * editor for each cell.  If we didn't implement this method,
     * then the last column would contain text ("true"/"false"),
     * rather than a check box.
     */
   public Class getColumnClass(int c) {
     String itsType = (String) columnType.get(c);
     if (EntryStructure.TEXT.equals(itsType)
      || EntryStructure.DISPLAYTEXT.equals(itsType)
      || EntryStructure.TEXTBOX.equals(itsType)
      || EntryStructure.DISPLAYBOX.equals(itsType)
      || EntryStructure.LOWERTEXT.equals(itsType))
      return String.class;
    if (EntryStructure.BUTTON.equals(itsType)
        || EntryStructure.ICONBUTTON.equals(itsType)
     || EntryStructure.POPUP.equals(itsType))
     return JButton.class;
   if (EntryStructure.CHECKBOX.equals(itsType))
    return JCheckBox.class;
     if (EntryStructure.NUMERICTEXT.equals(itsType))
      return Float.class;
    return Object.class;
   }
 
   public EntryStructure getColumnStructure(int c) {
     return (EntryStructure) structures.get(c);
   }
 
   public Component getTableCellRendererComponent(
                 JTable table, Object value,
                 boolean isSelected,
                 boolean hasFocus, int row,
                 int column) {
 
     EntryStructure it = getColumnStructure(column);
     String itsType = (String) columnType.get(column);
     if (EntryStructure.BUTTON.equals(itsType) || EntryStructure.ICONBUTTON.equals(itsType) || EntryStructure.POPUP.equals(itsType)) {
       Icon alternateIcon = it.getAlternateIcon();
       if (alternateIcon != null && getRowDatum(row) == null) {
         return new JButton(alternateIcon);
       }
       return (Component) it.getTextComponent();
     }
 
     return (JButton)value;
   }
 
}
 
 

