package au.gov.sa.environment.gift.swing;
 
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import java.io.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.*;
 
/**
 * EntryStructureTablePane - Hold name, type, size, JComponent for a form field
 * Makes an EntryTableModel from <datum>Table.properties
 */
public class EntryStructureTablePane extends EntryStructure implements ListSelectionListener {
 
 private JFrame frame = null;
 private EntryFrameStructure frameStructure = null;
 protected EntryTableModel theModel = null;
 private Object datum = null;
 private List contents = null;
 private Map objectMap = new TreeMap(); // Name = EntryStructure mapping
 private Map tableStructures = new HashMap(); // ListSelectionListener == datum cache
 
 private String className = null;
 private String propertyName = null;
 
 private String keyName = null;
 private String preferredWidth = "500";
 private String preferredHeight = "120";
 private Dimension tableSize = null;
 private Dimension labelSize = new Dimension(100,12);
 
 private List keyNames = null;
 
 
 public EntryStructureTablePane(String name, String size) {
   this.name = name;
   this.type = TABLEPANE;
   this.size = size;
   int sizeInt = 0;
   textComponent = null;
   displayComponent = new JPanel(new GridLayout(1,0));
 }
 
 public void makeTableModel(Object datum, EntryFrameStructure frameStructure, JFrame frame) {
   this.datum = datum;
   this.frame = frame;
   this.frameStructure = frameStructure;
   // Loads <datum.class>Table.properties
   Properties p = loadTableProperties(datum);
   String itsName = null;
   String modelType = null;
   String temp = null;
   StringTokenizer itsList = null;
   EntryStructure theStructure = null;
   keyNames = new ArrayList();
   String it = p.getProperty("keys");
   if (it != null) {
     itsList = new StringTokenizer(it, ", ");
     while (itsList.hasMoreTokens()) {
       keyNames.add(itsList.nextToken());
     }
   }
   it = p.getProperty("context");
   if (it != null) contextName = it;
   it = p.getProperty("model");
   if (it != null) modelType = it;
   it = p.getProperty("preferredwidth");
   if (it != null) preferredWidth = it;
   it = p.getProperty("preferredheight");
   if (it != null) {
     preferredHeight = it;
   }
   tableSize = new Dimension(Integer.parseInt(preferredWidth), Integer.parseInt(preferredHeight));
   if ("cell".equals(modelType)) {
     theModel = new EntryCellTableModel(datum, p, frame, general);
   } else {
   theModel = new EntryTableModel(datum, p, frame, general);
   }
   int columnCount = theModel.getColumnCount();
   List unresolvedLookups = new ArrayList();
   for (int i=0; i < columnCount; i++) {
     theStructure = theModel.getColumnStructure(i);
     objectMap.put(theStructure.getName(), theStructure);
     if (theStructure.isCustomList()) {
        // Might be defined in the properties file or have its own properties file for a database lookup
        String customType = theStructure.getType();
        String localType = p.getProperty(customType);
        if (localType == null) {
          unresolvedLookups.add(theStructure);
        } else {
          theStructure.setFromList(localType);
        }
     }
   }
   for (int i=0; i<unresolvedLookups.size(); i++) {
     theStructure = (EntryStructure) unresolvedLookups.get(i);
     it = theStructure.getType();
     if (general.checkNamelist(it, datum) == 0) {

        theStructure.setFromLookup(it, null);
     }
   }
 }
 
 
 /**
   * Catch up on any subsidiary object creation
   */
 public void postCreate(EntryFrameStructure esf) {
   makeTableModel(esf.getDatum(), esf, esf.getFrame());
   makeTableStructure(esf.getContextMap(), esf);
 }
 
 public void makeTableStructure(Map contextMap, EntryFrameStructure efs) {
   if (theModel != null) {
     JTable theTable = new JTable(theModel, theModel.getTableColumnModel());
     textComponent = theTable;
     theModel.setTableEditors(theTable, contextMap, efs);
     theTable.setPreferredScrollableViewportSize(tableSize);
     theTable.getTableHeader().setReorderingAllowed(false);
     theTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
     if (datum instanceof ListSelectionListener) {
       ListSelectionModel rowSM = theTable.getSelectionModel();
       tableStructures.put(rowSM, datum);
       rowSM.addListSelectionListener(this);
     }
     JScrollPane jScrollPane1 = new JScrollPane(theTable);
     jScrollPane1.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
     ((JComponent) displayComponent).add(jScrollPane1, BorderLayout.EAST);
   }
 }
 
 /**
   * Read the table layout properties
   * @param datum JavaBean identifying the class loader path and default file name
   * @return Properties the table layout properties file
   */
 private Properties loadTableProperties(Object datum) {
 
   StringBuffer buildName = new StringBuffer();
   String resourceName = datum.getClass().getName();
   int lastIndex = resourceName.lastIndexOf(".");
   if (className == null) {
     className = resourceName.substring(lastIndex+1);
   } else {
     int lastDot = className.lastIndexOf('.');
     if (lastDot > 0) {
       resourceName = className;
       className = className.substring(lastDot+1);
       lastIndex = lastDot;
     }
   }
   resourceName = resourceName.substring(0, lastIndex+1).replace('.', '/');
   buildName.append(resourceName);
 
   if (propertyName != null) {
     buildName.append(propertyName).append(".properties");
   } else {
     buildName.append(className).append("Table.properties");
   }
   InputStream f = null;
   // Start with the default properties set
   Properties p = new Properties();
   try {
     f = datum.getClass().getClassLoader().getResourceAsStream(buildName.toString());
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
 
 
        public boolean isCustomList()   {
                return false;
 }
 
        public boolean isEmbedded()     {
                return true;
 }
 
        public boolean isActionable()   {
          return !(action == null || action.length() == 0);
 }
 
        public boolean isActionable(Object datum)       {
   if (action != null && action.length() > 0) {
     String actionValue = getBeanData(datum, name);
     return (actionValue == null || actionValue.length() == 0 || "0".equals(actionValue));
   }
   return false;
 }
 
        public boolean isEditable()     {
   return false;

  }
 
        public boolean isEnabled()      {
   return true;
  }
 
        public boolean needsListLookup()        {
                return false;
 }
 
 public EntryTableModel getTheModel() {
   return theModel;
 }
 
 public void setFrame(JFrame theFrame) {
   this.frame = (EntryFrame) theFrame;
   if (frameStructure != null) {
     frameStructure.setFrame(theFrame);
   }
 }
 
/**
  * setData plugs the Collection on datum identified by attribute
  * into the table model
  * @param datum the parent data object
  * @param contextMap contains context for the table
  */
 public void setData(Object datum, Map contextMap) {
   if (contextName != null) contextMap.put(contextName, datum);
   if (textComponent != null && this.datum != datum) {
     ListSelectionModel rowSM = ((JTable) textComponent).getSelectionModel();
     if (this.datum instanceof ListSelectionListener) {
       tableStructures.remove(rowSM);
     }
     if (datum instanceof ListSelectionListener) {
       tableStructures.put(rowSM, datum);
     }
   }
   try {
     contents = general.getContainedData(datum, name);
     if (theModel instanceof EntryCellTableModel) {
       EntryCellTableModel ctm = (EntryCellTableModel) theModel;
       if (contents == null) {
         ctm.setContainedData(datum, contextMap);
       } else {
         ctm.setContainedData(contents, contextMap);
       }
     } else if (theModel != null) {
       theModel.setData(contents, contextMap);
     }
   } catch (Exception e) {
     e.printStackTrace();
   }
 }
 
 public void setAction(String action) {
   this.action = action;
   if (action != null) {
     StringTokenizer it = new StringTokenizer(action, ",");
     if (it.hasMoreTokens()) {
       className = it.nextToken();
       if (it.hasMoreTokens()) propertyName = it.nextToken();
     }
   }
 }
 
 public void valueChanged(ListSelectionEvent e) {
   Object it = e.getSource();
   if (tableStructures.containsKey(it) && !e.getValueIsAdjusting()) {
     ListSelectionListener datum = (ListSelectionListener) tableStructures.get(it);
     datum.valueChanged(new ListSelectionEvent(this, e.getFirstIndex(), e.getLastIndex(), e.getValueIsAdjusting()));
     frameStructure.loadData(datum);
   }
 }
 
 public Object getContentDatum(int index) {
   Object result = null;
   if (contents != null && index >= 0 && index < contents.size())
       result = contents.get(index);
   return result;
 }
 
 
}
 
 

