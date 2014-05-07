package au.gov.sa.environment.gift.swing;
 
import java.util.*;
 
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.*;
import java.lang.reflect.*;
import javax.swing.*;
import javax.swing.table.*;
 
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
 
import au.gov.sa.environment.gift.jdbc.*;
 
/**
 * <p>Title: Table cell editor to bring up a dialog</p>
 * <p>Description: J2EE management</p>
 * <p >Copyright: Copyright (c) 2003</p>
 * <p>Company: DEH</p>
 * @author Peter Davey
 * @version 1.0
 */
 
public class DialogCellEditor extends AbstractCellEditor
                          implements TableCellEditor,
                                     ActionListener {
 
   // editorCache is a HashMap of <className>:DialogHandler
   protected static final HashMap editorCache = new HashMap();
   protected static final String EDIT = "edit";
   protected static final String EDIT_MENU = "Edit";
   protected static final String REMOVE_MENU = "Remove";
   protected static final String BUTTON = "button";
   protected static final String LOAD = "load";
 
   String itsClassName;
   String defaultAction=EDIT;
   EntryStructure itsStructure;
   Object datum;   // The object owning the table that we're attached to
   Object rowDatum;  // The object inhabiting the row that we're editing
   Map contextMap;
   JFrame parentFrame;
   JTable table;
   EntryTableModel model;
   int row;
   int column;
 
   List sourceAttributes = new ArrayList();
   List localAttributes = new ArrayList();
   Map beanAttributes = null;
   Map values = null;
   Object value;
   JButton button;
   Object itsEditor;
   DialogHandler itsDH = null;
   JDialog dialog;
   Class itsClass;
 
   public DialogCellEditor(EntryStructure es, JFrame parentFrame, Object datum, Map contextMap, EntryTableModel model) {
 
     this.datum = datum;
     this.model = model;
     this.contextMap = contextMap;
     this.parentFrame = parentFrame;
     itsStructure = es;
     StringTokenizer scanIt = new StringTokenizer(es.getAction(), ",");
     if (scanIt.hasMoreTokens()) {
       itsClassName = scanIt.nextToken();
       if (LOAD.equals(itsClassName)) {
         defaultAction = itsClassName;
         itsClassName = scanIt.nextToken();
       }
       while (scanIt.hasMoreTokens()) {
         stashAttribute(scanIt.nextToken());
       }
     }
 
     button = new JButton();
     button.setActionCommand(es.getType());
     button.addActionListener(this);
     button.setBorderPainted(false);
 }
 
   private void stashAttribute(String source) {
     int equalPos = source.indexOf('=');
     if (equalPos < 0) {
       sourceAttributes.add(source);
       localAttributes.add(source);
     } else if (equalPos == 0) {
       sourceAttributes.add(source.substring(1));
       localAttributes.add(source.substring(1));
     } else {
       sourceAttributes.add(source.substring(0, equalPos));
       localAttributes.add(source.substring(equalPos+1));
     }
 }
 
   public void postCreate() {

     try {
 
       StringBuffer buildName = new StringBuffer();
       String resourceName = datum.getClass().getName();
       int lastIndex = resourceName.lastIndexOf(".");
       buildName.append(resourceName.substring(0, lastIndex+1)).append(itsClassName);
       resourceName = buildName.toString();
       // Throw ClassNotFound if the class to create doesn't exist
       itsClass = Class.forName(resourceName);
       // If it isn't cached, make the dialog handler for this object
       if (editorCache.containsKey(resourceName)) {
           itsDH = (DialogHandler) editorCache.get(resourceName);
       } else {
         // Throw NoSuchMethodException if it doesn't have a conforming constructor
         Class[] parameterTypes = new Class[] {JFrame.class, Object.class, GeneralDataProcessor.class};
         Constructor createEditor = itsClass.getConstructor(parameterTypes);
         Object[] parameters = new Object[] {parentFrame, datum, itsStructure.getGeneral()};
         itsEditor = createEditor.newInstance(parameters);
         // If the editor is a DialogHandler, cache it
         if (itsEditor instanceof DialogHandler) {
           itsDH = (DialogHandler) itsEditor;
           editorCache.put(resourceName, itsDH);
         }
       }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
 
 private Map createValues(Object rowDatum) {
   if (itsDH == null) return null;
   values = new HashMap();
   String valueName = null;
   String localName = null;
   String contextName = null;
   Object attValue = null;
   if (rowDatum == null) return values;
   try {
     beanAttributes = BeanUtils.describe(rowDatum);
     int dotPos = 0;
     for (int i=0; i < sourceAttributes.size(); i++) {
       valueName = (String) sourceAttributes.get(i);
       localName = (String) localAttributes.get(i);
       dotPos = valueName.lastIndexOf('.');
       if (dotPos < 0) {
         if (beanAttributes.containsKey(valueName)) {
           attValue = PropertyUtils.getProperty(rowDatum, valueName);
         } else {
           attValue = valueName;
         }
       } else {
         contextName = valueName.substring(0, dotPos);
         valueName = valueName.substring(dotPos + 1);
         if (contextMap != null && contextMap.containsKey(contextName)) {
           Object contextDatum = contextMap.get(contextName);
           attValue = PropertyUtils.getProperty(contextDatum, valueName);
         } else {
           attValue = "";
         }
       }
       values.put(localName, attValue);
     }
   } catch (Exception e) {
     e.printStackTrace();
   }
   return values;
 }
 
   public void actionPerformed(ActionEvent e) {
     String it = e.getActionCommand();
   Object source = e.getSource();
   if (itsDH == null) return;                // Check that we have a handler
   if (source instanceof JButton) {
     if (rowDatum != null && model.getNewLine() > 0) {     // If we're allowed to remove
       JPopupMenu theMenu = new JPopupMenu();
       JMenuItem menuItem = new JMenuItem(EDIT_MENU);
       menuItem.addActionListener(this);
       theMenu.add(menuItem);
       menuItem = new JMenuItem(REMOVE_MENU);
       menuItem.addActionListener(this);
       theMenu.add(menuItem);
       Rectangle cellRect = table.getCellRect(row, column, false);
       theMenu.show(table, cellRect.x, cellRect.y);
       fireEditingCanceled(); //Make the renderer reappear.
       return;
     }
   }
     if (BUTTON.equals(it)) it = defaultAction;
   if (EDIT.equals(it) || EDIT_MENU.equals(it)) {
     if (itsDH != null) {                // Check that we have a handler
       if (rowDatum == null) {           // If theres no row object
         if (model.getNewLine() > 0) {     // and we're allowed to create
           rowDatum = itsDH.createNewDatum(); // Make a new row
           if (performEditAction(rowDatum)) { // and, if editing is OK
             model.addRowDatum(rowDatum);       // Add it to the model
             ApplicationFrame.setAppChanges(true);
           }
         }
       } else {
         if (performEditAction(rowDatum))    // If its an existing row, just edit it

           ApplicationFrame.setAppChanges(true);
      }
     }
       fireEditingCanceled(); //Make the renderer reappear.
       if (model != null) model.fireTableDataChanged();
   } else if (REMOVE_MENU.equals(it)) {
     if (((EntryFrame) parentFrame).getFrameStructure().confirmDelete(getConfirmPrompt(rowDatum))) {
       model.removeRowDatum(rowDatum);
       ApplicationFrame.setAppChanges(true);
       fireEditingCanceled(); //Make the renderer reappear.
     }
     } else if (LOAD.equals(it)) {
       if (rowDatum == null) {
 
       } else {
         fireEditingCanceled(); //Make the renderer reappear.
         EntryFrame newFrame = new EntryFrame(rowDatum, itsStructure.getGeneral());
         newFrame.postCreate(rowDatum);
         newFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
         newFrame.pack();
         newFrame.setVisible(true);
       }
     }
   }
 
   public String getConfirmPrompt(Object rowDatum) {
       StringBuffer buildKey = new StringBuffer();
       try {
           String text = model.getConfirmDelete();
           if (text != null && text.length() > 0) buildKey.append(text).append(" ");
           text = model.getConfirmDeleteAttribute();
           if (text != null && text.length() > 0) {
               text = BeanUtils.getProperty(rowDatum, text);
           }
           if (text != null && text.length() > 0) buildKey.append(text);
       } catch (Exception e) {
           buildKey.append(e.toString());
       }
       return buildKey.toString();
   }
 
   public boolean performEditAction(Object rowDatum) {
     //The user has clicked the cell, so bring up the dialog.
     values = createValues(rowDatum);
       if (values != null) itsDH.setup(values);
       dialog = itsDH.createDialog(parentFrame);
       dialog.pack();
 
     // By the time we get back from this next line, editing is all over
     dialog.setVisible(true);
     try {
       if (!itsDH.checkEditState()) return false;
           if (beanAttributes == null && rowDatum != null) beanAttributes = BeanUtils.describe(rowDatum);
           values = itsDH.returnValues();
           String valueName = null;
           String localName = null;
           Object attValue = null;
           int dotPos = 0;
           for (int i=0; i < sourceAttributes.size(); i++) {
             valueName = (String) sourceAttributes.get(i);
             localName = (String) localAttributes.get(i);
             dotPos = valueName.lastIndexOf('.');
             if (dotPos < 0) {
               if (beanAttributes.containsKey(valueName)) {
                 if (values.containsKey(localName)) {
                   attValue = values.get(localName);
                   BeanUtils.setProperty(rowDatum, valueName, attValue);
                 }
               }
             }
           }
     } catch (Exception ex) {
       ex.printStackTrace();
       return false;
     }
     return true;
   }
 
   //Implement the one CellEditor method that AbstractCellEditor doesn't.
   public Object getCellEditorValue() {
     Object result = null;
     if (itsDH != null) {
       values = itsDH.returnValues();
       if (!values.isEmpty()) {
         Iterator it = values.keySet().iterator();
         if (it.hasNext()) {
           result = values.get(it.next());
         }
       }
     }
     return result;
   }
 
   //Implement the one method defined by TableCellEditor.
   public Component getTableCellEditorComponent(JTable table,
                                                Object value,
                                                boolean isSelected,
                                                int row,
                                                int column) {
     this.table = table;

     this.row = row;
     this.column = column;
     Object theModel = table.getModel();
     rowDatum = datum;
     if (theModel instanceof EntryTableModel) {
       EntryTableModel etm = ((EntryTableModel) theModel);
       rowDatum = etm.getRowDatum(row);
       itsStructure = etm.getColumnStructure(column);
     }
     return button;
   }
 
}
 

