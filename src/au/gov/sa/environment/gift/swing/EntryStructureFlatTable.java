package au.gov.sa.environment.gift.swing;
 
import java.awt.*;
import javax.swing.*;
import java.util.*;
 
/**
 * EntryStructureFlatTable - 
 * Makes an EntryTableModel without scrolling from <datum>Table.properties
 */
public class EntryStructureFlatTable extends EntryStructureTablePane {
 
 public EntryStructureFlatTable(String name, String size) {
   super(name, size);
 }
 
 public void makeTableStructure(Map contextMap, EntryFrameStructure efs) {
   if (theModel != null) {
     JTable theTable = new JTable(theModel, theModel.getTableColumnModel());
     textComponent = theTable;
     theModel.setTableEditors(theTable, contextMap, efs);
     theTable.getTableHeader().setReorderingAllowed(false);
     theTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
     ((JComponent) displayComponent).add(theTable, BorderLayout.CENTER);
   }
 }
 
}
 
 

