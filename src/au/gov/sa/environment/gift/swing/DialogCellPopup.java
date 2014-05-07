package au.gov.sa.environment.gift.swing;
 
import java.util.*;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.*;
import javax.swing.table.*;
 
/**
 * <p>Title: Table cell editor for a popup menu on an item</p>
 * <p>Description: J2EE management</p>
 * <p >Copyright: Copyright (c) 2003</p>
 * <p>Company: DEH</p>
 * @author Peter Davey
 * @version 1.0
 */
 
public class DialogCellPopup extends AbstractCellEditor
                          implements TableCellEditor,
                                     ActionListener {
 
   // editorCache is a HashMap of <className>:DialogHandler
   protected static final HashMap editorCache = new HashMap();
   protected static final String EDIT = "edit";
   protected static final String BUTTON = "button";
   protected static final String LOAD = "load";
   StringBuffer currentValue;
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
 
   public DialogCellPopup(EntryStructure es, JFrame parentFrame, Object datum, Map contextMap, EntryTableModel model) {
 
     this.datum = datum;
     this.model = model;
     this.contextMap = contextMap;
     this.parentFrame = parentFrame;
     itsStructure = es;
 
     button = (JButton) es.getDisplayComponent();
     button.setActionCommand(es.getType());
     button.addActionListener(this);
     button.setBorderPainted(false);
 }
 
   public void postCreate() {
  }
 
   public void actionPerformed(ActionEvent e) {
     String it = e.getActionCommand();
     if (itsStructure instanceof EntryStructurePopupMenu) {
       JPopupMenu theMenu = new JPopupMenu();
       ((EntryStructurePopupMenu) itsStructure).getMenu(theMenu, this);
       Rectangle cellRect = table.getCellRect(row, column, false);
       theMenu.show(table, cellRect.x, cellRect.y);
     }
     fireEditingCanceled(); //Make the renderer reappear.
   }
 
   //Implement the one CellEditor method that AbstractCellEditor doesn't.
   public Object getCellEditorValue() {
     Object result = null;
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

     }
     return button;
   }
 
}
 

