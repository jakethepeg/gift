package au.gov.sa.environment.gift.swing;
 
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Font;
import javax.swing.*;
import javax.swing.event.*;
import java.util.*;
 
import au.gov.sa.environment.gift.general.*;
import au.gov.sa.environment.gift.jdbc.*;
 
/**
 * EntryStructureListBox - Hold name, type, size, JComponent for a combo box
 * Entries
 */
public class EntryStructureListBox extends EntryStructure {
 
 private Object datum = null;
 private List contents = null;
 private JComboBox itsList;
 private JLabel itsLabel;
 private String listName = null;
 private boolean useList = true;
 private boolean useLabel = true;
 private Dimension labelSize = new Dimension(100,12);
 private String leadEntry = null;
 
 
 public EntryStructureListBox(String name, String size) {
   this.name = name;
   this.type = LISTBOX;
   this.size = size;
   this.target = name;
   this.valueAttribute = name;
   int sizeInt = 0;
   contents = new ArrayList();
   listModel = new ListCompoundBoxModel(contents);
   itsList = new JComboBox(listModel);
   textComponent = itsList;
   displayComponent = isActionable() ? itsList : new JTextField();
 }
 
 public EntryStructureListBox(String name, String size, String action) {
   this.name = name;
   this.type = LISTBOX;
   this.size = size;
   this.target = name;
   this.valueAttribute = name;
   int sizeInt = 0;
   contents = new ArrayList();
   listModel = new ListCompoundBoxModel(contents);
   itsList = new JComboBox(listModel);
   textComponent = itsList;
   displayComponent = action != null ? itsList : new JTextField();
 }
 
 
 /**
   * Catch up on any subsidiary object creation
   */
 public void postCreate(EntryFrameStructure efs) {
   super.postCreate(efs);
 }
 
 public boolean isCustomList() {
   return true;
 }
 
 public boolean isEmbedded()   {
   return false;
 }
 
 public boolean isActionable() {
   return !(action == null || action.length() == 0);
 }
 
 public boolean isActionable(Object datum)     {
   // If it is a key and is not null then it isn't actionable
   String itsKey = null;
   String itsValue = null;
   GeneralDataSet itsDs = GeneralDataSet.getDataset(datum);
   if (itsDs != null) itsKey = itsDs.getKeyName();
   if (itsKey != null && itsKey.equals(valueAttribute)) {
     itsValue = getBeanData(datum, itsKey);
     return itsValue == null;
   }
   return true;
 }
 
 public boolean isEditable()   {
   return false;
  }
 
  public boolean isEnabled()   {
   return true;
  }
 
  public boolean needsListLookup()     {
    return true;

 }
 
 public void setFromList(String listValues) {
   decodeList(listValues);
   listModel.setFreshList(labelValue, false);
   if (textComponent instanceof JComboBox) ((JComboBox) textComponent).setMaximumRowCount(labelValue.size());
 }
 
  public String getListName()  {
    return listName;
 }
 
 public String getLabelData(String data) {
   if (data == null) return data;
   if (!localDataList) return data;
   if (listModel instanceof ListCompoundBoxModel)
     for (int i = 0; (i < labelValue.size() ); i++) {
       LabelValueBean it = (LabelValueBean) labelValue.get(i);
       if (data.equals(it.getValue()))
         return ((ListCompoundBoxModel) listModel).getLabelFor(i);
     }
   return data;
 }
 
 public void setData(Object datum, Map contextMap) {
 }
 
 public void setAction(String action) {
   this.action = "data";
   if (action != null) {
     StringTokenizer it = new StringTokenizer(action, ",");
     if (it.hasMoreTokens()) {
       listName = it.nextToken();
//       if (it.hasMoreTokens()) itsList.setPrototypeDisplayValue(it.nextToken()); Java 1.3 omission
       while (it.hasMoreTokens()) {
         String token = it.nextToken();
         if ("lead".equals(token)) leadEntry = it.nextToken();;
         if ("nolabel".equals(token)) useLabel = false;
         if ("nolist".equals(token)) useList = false;
         if ("showCode".equals(token)) {
           listModel.setShowCode(ListComboBoxModel.SHOW_CODE);
         }
         if ("showBoth".equals(token)) {
           listModel.setShowCode(ListComboBoxModel.SHOW_BOTH);
         }
         if ("showLabel".equals(token)) {
           listModel.setShowCode(ListComboBoxModel.SHOW_LABEL);
         }
       }
     }
   }
 }
 
 public String convertValueToName(Object datum) {
   String theValue = null;
   if (datum != null && valueAttribute != null) {
     theValue = getBeanData(datum, valueAttribute);
   }
   if (theValue != null) {
     for (int i = 0; i < contents.size(); i++) {
       LabelValueBean it = (LabelValueBean) contents.get(i);
       if (name.equals(it.getValue())) return it.getLabel();
     }
   }
   return null;
 }
 
 public int convertValueToIndex(Object datum) {
   String theValue = null;
   int result = -1;
   boolean isUnknownValue = false;
   if (datum != null && valueAttribute != null) {
     theValue = getBeanData(datum, valueAttribute);
     isUnknownValue = ("-9999".equals(theValue) || "unknown".equals(theValue));
     if (theValue != null) {
       for (int i = 0; (i < contents.size() && result < 0); i++) {
         LabelValueBean it = (LabelValueBean) contents.get(i);
         if (theValue.equals(it.getValue())) result = i;
       }
       if (result < 0 && isUnknownValue) {
         result = convertNameToIndex("unknown");
       }
     }
   }
   if (itsLabel != null)
     itsLabel.setText(((ListCompoundBoxModel) listModel).getLabelFor(result));
   return result;
 }
 
 public int convertNameToIndex(String name) {
   contents = GeneralDataProcessor.getCachedLvList(listName);
   int result = -1;
   LabelValueBean it;
   for (int i = 0; (i < contents.size() && result < 0); i++) {
     it = (LabelValueBean) contents.get(i);
     if (name.equals(it.getLabel())) result = i;
   }
   if (result < 0) {
     for (int i = 0; (i < contents.size() && result < 0); i++) {
       it = (LabelValueBean) contents.get(i);

       if (name.equals(it.getValue())) result = i;
     }
   }
   return result;
 }
 
 public boolean convertIndexToValue(int index, Object datum) {
   boolean changed = false;
   if (datum != null && valueAttribute != null && index >= 0 && index < contents.size()) {
     LabelValueBean it = (LabelValueBean) contents.get(index);
     String result = "unknown";
     if (it != null) {
       result = it.getLabel();
       result = (result == null) ? "unknown" : result.toLowerCase();
     }
     if ("unknown".equals(result)) {
       result = "-9999";
     } else {
       result = it.getValue();
     }
     setBeanData(datum, valueAttribute, result);
     changed = true;
     if (itsLabel != null)
       itsLabel.setText(((ListCompoundBoxModel) listModel).getLabelFor(index));
   }
   return changed;
 }
 
 
 /**
   * Fill a selector with values.
   * datalist - the list is in the datum
   * otherwise - check the GDP's static caches
   *  - if its not found there, and there's a database connection use that
   * @param lookupName
   * @param datum
   */
 public void setFromLookup(String lookupName, Object datum) {
   labelValue = GeneralDataProcessor.getCachedLvList(listName);
   if (labels == null) labels = new ArrayList();
   if (values == null) values = new ArrayList();
   if (listName.equals("HeightCodeLookup")
       || listName.equals("GrowthFormShort")) {
       // reset labelValue to NULL to force lookup list to refresh every time
       // the user switches Stratum tab
       labelValue = null;
   }
   if (labelValue == null) {
     JdbcDataProcessor jdp = JdbcDataProcessor.getJdp();
     if (jdp.getDBConnection() != null) {
       labelValue = new ArrayList();
       if (leadEntry != null) {
               labelValue.add(new LabelValueBean(leadEntry, "0"));
       }
       jdp.namelistUtility(datum, listName, labelValue);
     }
   }
   if (labelValue != null) {
     if (listReducer != null) {
       ListReducer theReducer = (ListReducer) listReducer;
       labelValue = theReducer.reduceList(listName, labelValue, labels, values);
     } else {
       LabelValueBean.remakeList(labelValue, labels, values);
     }
   }
 
   contents = labelValue;
   if (values.size() > 0) valueAttribute = target;
   listModel.setFreshList(labelValue);
   JComboBox it = (JComboBox) textComponent;
   it.setSelectedIndex(convertValueToIndex(datum));
 }
 
 
 public void valueChanged(ListSelectionEvent e) {
   Object changeObject = e.getSource();
   int firstIndex = e.getFirstIndex();
   int lastIndex = e.getLastIndex();
   if (contents == null) {
     return;
   }
 }
}

