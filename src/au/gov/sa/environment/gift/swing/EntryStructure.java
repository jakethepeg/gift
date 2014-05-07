package au.gov.sa.environment.gift.swing;
 
import java.awt.Dimension;
import java.lang.reflect.*;
import javax.swing.*;
import javax.swing.text.*;
import java.util.*;
 
import org.apache.commons.beanutils.BeanUtils;
 
import au.gov.sa.environment.gift.general.*;
import au.gov.sa.environment.gift.jdbc.*;
 
/**
 * EntryStructure - Hold name, type, size, JComponent for a form field
 * Entries
 */
public class EntryStructure {
 
 protected static final int textHeight = 12;
 protected static final int buttonHeight = 16;
 
 protected static final String TEXT="text";
 protected static final String EDIT="edit";
 protected static final String LOAD="load";
 protected static final String POPUP="popup";
 protected static final String TEXTEDIT="textedit";
 protected static final String DISPLAYTEXT="displaytext";
 protected static final String LOWERTEXT="lowercase";
 protected static final String PARAMTEXT="paramtext";
 protected static final String NUMERICTEXT="numeric";
 protected static final String DATA="data";
 protected static final String LITERAL="literal";
 protected static final String TEXTBOX="textbox";
 protected static final String DISPLAYBOX="displaybox";
 protected static final String BUTTON="button";
 protected static final String ICONBUTTON="iconbutton";
 protected static final String CHECKBOX="checkbox";
 protected static final String LISTBOX="listbox";
 protected static final String FRAMEDLINE="framedline";
 protected static final String FRAMEDSET="framedset";
 protected static final String SWITCHEDPANE="switchedpane";
 protected static final String TABBEDPANE="tabbedpane";
 protected static final String TABLEPANE="tablepane";
 protected static final String FLATTABLE="table";
 protected static final String TREEPANE="treepane";
 protected static final String FRAMEPANE="framepane";
 protected static final String HIDDEN="hidden";
 protected static final String DATALIST="datalist";
 
 protected GeneralDataProcessor general = null;
 
 protected String name;
 protected String type;
 protected String size;
 protected String tab;
 protected String label;
 protected String labelSize;
 protected String action;
 protected String order;
 protected String target;
 protected String text;
 protected String source;
 protected boolean integerField = false;
 protected boolean glueAfter = false;
 protected boolean localDataList = false;
 protected Object displayComponent;
 protected Object textComponent;
 protected ListComboBoxModel listModel;
 protected List labelValue = null; // List of LabelValueBean for advanced operation
 protected List labels = null;
 protected List values = null;
 protected String valueAttribute = null;
 protected Icon buttonIcon = null;
 protected Icon alternateIcon = null;
 protected Object listReducer = null;
 
 protected String contextName = null;
 
 public EntryStructure() {
 }
 
 public EntryStructure(String name, String type, String size) {
   this.name = name;
   this.type = type;
   this.size = size;
   this.target = name;
   this.valueAttribute = name;
   int sizeInt = StringUtil.safeToNum(size);
   if (sizeInt < 0) sizeInt = 0;
   if (type.equals(TEXT) || type.equals(DISPLAYTEXT) || type.equals(LOWERTEXT) || type.equals(DATA)) {
     JTextField jText1 = new JTextField(sizeInt);
     textComponent = jText1;
     displayComponent = jText1;
   } else if (type.equals(TEXTEDIT)) {
     JTextField jText1 = new JTextField(sizeInt);
     textComponent = jText1;
     displayComponent = jText1;
   } else if (type.equals(LITERAL)) {
     JLabel jLabel = new JLabel();

     textComponent = jLabel;
     if (sizeInt > 0) {
       jLabel.setMinimumSize(new Dimension(sizeInt, textHeight));
     }
     displayComponent = jLabel;
   } else if (type.equals(HIDDEN)) {
     textComponent = null;
     displayComponent = null;
   } else if (type.equals(EDIT) || type.equals(LOAD)) {
     JTextField jText1 = new JTextField();
     textComponent = jText1;
     displayComponent = jText1;
   } else if (type.equals(NUMERICTEXT)) {
     JTextField jText1 = new JTextField();
     textComponent = jText1;
     displayComponent = jText1;
   } else if (type.equals(TEXTBOX) || type.equals(DISPLAYBOX)) {
     JTextArea descriptionText = new JTextArea();
     descriptionText.setLineWrap(true);
     descriptionText.setWrapStyleWord(true);
     textComponent = descriptionText;
     displayComponent = null;
   } else if (type.equals(BUTTON) || type.equals(ICONBUTTON) || type.equals(POPUP)) {
     JButton actionButton = new JButton();
     if (sizeInt > 0) actionButton.setMinimumSize(new Dimension(sizeInt, buttonHeight));
     textComponent = actionButton;
     displayComponent = actionButton;
   } else if (type.equals(CHECKBOX)) {
     JCheckBox checkBox = new JCheckBox();
     textComponent = checkBox;
     displayComponent = checkBox;
   } else if (type.equals(FRAMEDLINE)) {
     textComponent = null;
     displayComponent = null;
   } else if (type.equals(FRAMEDSET)) {
     textComponent = null;
     displayComponent = null;
   } else if (type.equals(TABBEDPANE)) {
     textComponent = new JTabbedPane();
     displayComponent = textComponent;
   } else if (type.equals(TABLEPANE)) {
     textComponent = new JPanel();
     displayComponent = textComponent;
   } else {
     listModel = new ListComboBoxModel(new ArrayList());
     JComboBox combo = new JComboBox(listModel);
     textComponent = combo;
     displayComponent = combo;
     labels = new ArrayList();
     values = new ArrayList();
   }
 }
 
 /**
   * Catch up on any subsidiary object creation
   */
 public void postCreate(EntryFrameStructure efs) {
   List containedLine = null;
   if (FRAMEDSET.equals(type)) {
     for (int i = 0; i < values.size(); i++) {
       containedLine = (List) values.get(i);
       for (int j = 0; j < containedLine.size(); j++) {
         ((EntryStructure) containedLine.get(j)).postCreate(efs);
       }
     }
   }
 }
 
 public void setFrame(JFrame theFrame) {
 }
 
 public boolean isCustomList() {
   return !(TEXT.equals(type) || TEXTEDIT.equals(type) || EDIT.equals(type) || LITERAL.equals(type) || LOAD.equals(type)
   || TEXTBOX.equals(type) || NUMERICTEXT.equals(type) || LOWERTEXT.equals(type) || DATA.equals(type)
   || DISPLAYTEXT.equals(type) || DISPLAYBOX.equals(type) || FRAMEDLINE.equals(type) || FRAMEDSET.equals(type)
   || BUTTON.equals(type) || ICONBUTTON.equals(type) || POPUP.equals(type)
   || CHECKBOX.equals(type) || TABBEDPANE.equals(type) || TABLEPANE.equals(type));
 }
 
 public boolean isEmbedded()   {
   return (TABBEDPANE.equals(type) || TABLEPANE.equals(type));
 }
 
 public boolean isActionable() {
   if (TEXT.equals(type) || TEXTBOX.equals(type) || LITERAL.equals(type)
   || NUMERICTEXT.equals(type) || LOWERTEXT.equals(type) || DATA.equals(type)
   || DISPLAYTEXT.equals(type) || DISPLAYBOX.equals(type) || FRAMEDLINE.equals(type)) {
     return false;
   }
   return !(action == null || action.length() == 0);
 }
 
 public boolean isActionable(Object datum)     {
   if (TEXT.equals(type) || TEXTBOX.equals(type) || LITERAL.equals(type)
   || NUMERICTEXT.equals(type) || LOWERTEXT.equals(type) || DATA.equals(type)
   || DISPLAYTEXT.equals(type) || DISPLAYBOX.equals(type) || FRAMEDLINE.equals(type)) {
     return false;
   }
   if (action != null && action.length() > 0) {
     if ("data".equals(action)) return true;

     if (action.startsWith("method")) return true;
     String actionValue = getBeanData(datum, name);
     return (actionValue == null || actionValue.length() == 0 || "0".equals(actionValue));
   }
   return false;
 }
 
 public boolean isEditable()   {
   if (DISPLAYTEXT.equals(type) || DISPLAYBOX.equals(type) || FRAMEDLINE.equals(type) || LITERAL.equals(type)) return false;
   if (TEXT.equals(type) || TEXTBOX.equals(type) || NUMERICTEXT.equals(type) || LOWERTEXT.equals(type)) return true;
   if (CHECKBOX.equals(type)) return true;
   // All the rest deliver a result through some sort of action and can't be edited
   return false;
 }
 
 public boolean isEnabled()    {
   if (DISPLAYTEXT.equals(type) || DISPLAYBOX.equals(type) || LITERAL.equals(type) || FRAMEDLINE.equals(type)) return false;
   if (TEXT.equals(type) || TEXTEDIT.equals(type) || TEXTBOX.equals(type) || NUMERICTEXT.equals(type) || LOWERTEXT.equals(type)) return true;
   if (CHECKBOX.equals(type) || BUTTON.equals(type) || ICONBUTTON.equals(type) || POPUP.equals(type)) return true;
   // All the rest deliver a result through some sort of action and should be enabled unless they're denatured
   return (textComponent instanceof JComboBox);
 }
 
 public boolean needsListLookup()      {
   return (isCustomList() && valueAttribute == null);
 }
 
 public String getName()       {
   return name;
 }
 
 public String getType() {
   return type;
 }
 
 public String getSize() {
   return size;
 }
 
 public String getLabel() {
   return label;
 }
 
 public String getLabelData(String data) {
   return data;
 }
 
 public String getAction() {
   return action;
 }
 
 public String getOrder() {
   return order;
 }
 
 public String getTarget() {
   return target;
 }
 
 public String getText() {
   return text;
 }
 
 public String getSource() {
   return source;
 }
 
 public String getScreenValue() {
   String result = null;
   if (textComponent instanceof JComboBox) {
     result = (String) ((JComboBox) textComponent).getSelectedItem();
   } else if (textComponent instanceof JTextComponent) {
     result = ((JTextComponent) textComponent).getText();
   }
   return result;
 }
 
 public void setDataValue(Object datum) {
   if (textComponent != null) {
     if (textComponent instanceof JComboBox) {
       convertIndexToValue(((JComboBox) textComponent).getSelectedIndex(), datum);
     } else if (textComponent instanceof JTextComponent) {
       setBeanData(datum, name, ((JTextComponent) textComponent).getText());
     }
   }
 }
 
 public boolean isIntegerField() {
   return integerField;
 }
 
 public boolean isGlueAfter() {
   return glueAfter;
 }
 
 public boolean isLocalDataList() {
   return localDataList;
 }
 
 public boolean fireListChanged() {

   boolean result = (listModel != null && textComponent instanceof JComboBox);
   if (result) listModel.fireChanged();
   return result;
 }
 
 public Object getTextComponent() {
   return textComponent;
 }
 
 public Object getDisplayComponent() {
   return displayComponent;
 }
 
 public Object getListReducer() {
   return listReducer;
 }
 
 public String getLabelSize() {
   return labelSize;
 }
 
 public Icon getButtonIcon() {
   return buttonIcon;
 }
 
 public Icon getAlternateIcon() {
   return alternateIcon;
 }
 
 public List getValues() {
   return values;
 }
 
 public void setName(String name) {
   this.name = name;
 }
 
 public void setType(String type) {
   this.type = type;
 }
 
 public void setSize(String size) {
   this.size = size;
 }
 
 public String getTab() {
   return tab;
 }
 
 public void setTab(String tab) {
   this.tab = tab;
 }
 
 public void setLabel(String label) {
   this.label = label;
 }
 
 public void setAction(String action) {
   this.action = action;
 }
 
 public void setOrder(String order) {
   this.order = order;
 }
 
 public void setTarget(String target) {
   this.target = target;
 }
 
 public void setText(String text) {
   this.text = text;
   if (textComponent instanceof JLabel) {
     ((JLabel) textComponent).setText(text);
   }
 }
 
 public void setSource(String source) {
   this.source = source;
 }
 
 public void setIntegerField(boolean integerField) {
   this.integerField = integerField;
 }
 
 public void setGlueAfter(boolean glueAfter) {
   this.glueAfter = glueAfter;
 }
 
 public void setTextComponent(Object component) {
   this.textComponent = component;
 }
 
 public void setDisplayComponent(Object component) {
   this.displayComponent = component;
 }
 
 public void setListReducer(Object listReducer) {
   this.listReducer = listReducer;
 }
 

 public void setLabelSize(String labelSize) {
   this.labelSize = labelSize;
 }
 
 public void setValues(List values) {
   this.values = values;
 }
 
 public String getContextName() {
   return contextName;
 }
 public void setContextName(String contextName) {
   this.contextName = contextName;
 }
 
 public String convertValueToName(Object datum) {
   String theValue = null;
   if (datum != null && valueAttribute != null) {
     theValue = getBeanData(datum, valueAttribute);
     if (theValue == null && values != null && values.size() > 0) {
       theValue = (String) values.get(0);
       setBeanData(datum, valueAttribute, theValue);
     }
   }
   if (theValue != null && values != null) {
     for (int i=0; i<values.size(); i++) {
       if (theValue.equals(values.get(i))) return (String) labels.get(i);
     }
   }
   return theValue;
 }
 
 public int convertValueToIndex(Object datum) {
   String theValue = null;
   String testValue = null;
   String testLabel = null;
   int result = -1;
   boolean isUnknownValue = false;
   if (datum != null && valueAttribute != null) {
     theValue = getBeanData(datum, valueAttribute);
     if (theValue == null) {
       isUnknownValue = true;
     } else {
       isUnknownValue = ("-9999".equals(theValue));
       result = values.indexOf(theValue);
     }
       if (result < 0 && isUnknownValue) {
         result = values.indexOf("unknown");
         if (result < 0) result = labels.indexOf("unknown");
     }
   }
   return result;
 }
 
 public int convertNameToIndex(String name) {
   int result = -1;
   if (name != null) {
    if (labels != null) result = labels.indexOf(name);
    if (result < 0 && values != null) result = values.indexOf(name);
   }
   return result;
 }
 
 public boolean convertIndexToValue(int index, Object datum) {
   boolean changed = false;
   if (datum != null && valueAttribute != null && index >= 0 && index < values.size()) {
     String result = ((String) labels.get(index)).toLowerCase();
     if ("unknown".equals(result)) {
       result = "-9999";
     } else {
       result = (String) values.get(index);
     }
     setBeanData(datum, valueAttribute, result);
     changed = true;
   }
   return changed;
 }
 
 public void setBeanData(Object it, String name, String value) {
   try {
     BeanUtils.setProperty(it, name, value);
   } catch (Exception e) {
     e.printStackTrace();
   }
 }
 
 public String getBeanData(Object it, String name) {
   String result = null;
   try {
     result = BeanUtils.getProperty(it, name);
   } catch (Exception e) {
     e.printStackTrace();
   }
   return result;
 }
 
 public void decodeList(String listValues) {
   StringTokenizer entries = new StringTokenizer(listValues, ",");
   String thisEntry = null;
   String lastEntry = null;

   labelValue = new ArrayList();
   labels = new ArrayList();
   values = new ArrayList();
   String listType = null;
   int state = 0;
   while (entries.hasMoreTokens()) {
     thisEntry = entries.nextToken();
     switch (state) {
       case 0:
         if ("localList".equals(thisEntry)) {
           listType = thisEntry;
           state = 1;
         } else if ("localPairs".equals(thisEntry)) {
           listType = thisEntry;
           state = 2;
         } else if ("localCombined".equals(thisEntry)) {
           listType = thisEntry;
           state = 4;
         } else {
           listType = "localList";
           labels.add(thisEntry);
           values.add(thisEntry);
           labelValue.add(new LabelValueBean(thisEntry));
           state = 1;
         }
         break;
       case 1:
         labels.add(thisEntry);
         values.add(thisEntry);
         labelValue.add(new LabelValueBean(thisEntry));
         break;
       case 2:
         labels.add(thisEntry);
         lastEntry = thisEntry;
         state = 3;
         break;
       case 3:
         values.add(thisEntry);
         labelValue.add(new LabelValueBean(lastEntry, thisEntry));
         state = 2;
         break;
       case 4:
         lastEntry = thisEntry;
         state = 5;
         break;
       case 5:
         labels.add(thisEntry + ": " + lastEntry);
         values.add(thisEntry);
         labelValue.add(new LabelValueBean(lastEntry, thisEntry));
         state = 4;
         break;
     }
   }
   valueAttribute = name;
   localDataList = true;
 }
 
 public void setFromList(String listValues) {
   decodeList(listValues);
   listModel.setFreshList(labels, false);
   if (textComponent instanceof JComboBox) ((JComboBox) textComponent).setMaximumRowCount(labels.size());
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
   if (DATALIST.equals(lookupName)) {
     setFromLocalList(datum);
   } else if (!localDataList) {
     labelValue = GeneralDataProcessor.getCachedLvList(lookupName);
     if (labelValue == null) {
       labels = new ArrayList();
       values = new ArrayList();
         JdbcDataProcessor jdp = JdbcDataProcessor.getJdp();
         if (jdp.getDBConnection() != null) {
           labelValue = new ArrayList();
           if (lookupName.equals("StaffLookup")) {
               labelValue.add(new LabelValueBean("-- No Entry --", "0"));
           }
           jdp.namelistUtility(datum, lookupName, labelValue);
         }
     }
     if (labelValue != null) {
       if (listReducer != null) {
           ListReducer theReducer = (ListReducer) listReducer;
           theReducer.reduceList(lookupName, labelValue, labels, values);
       } else {
           LabelValueBean.remakeList(labelValue, labels, values);
       }
     }
   }
   if (values.size() > 0) valueAttribute = target;
   listModel.setFreshList(labels, true);
   JComboBox it = (JComboBox) textComponent;

   it.setSelectedIndex(-1);
   if (DATALIST.equals(lookupName)) {
     if (labels.size() > 0) it.setSelectedIndex(0);
   } else {
     it.setSelectedIndex(convertValueToIndex(datum));
   }
 }
 
 
 /** setFromLocalList sets up a list structure from its attribute in datum
   * @param datum object containing the source data
   */
 public void setFromLocalList(Object datum) {
   try {
     StringBuffer workName = new StringBuffer("get");
     workName.append(name.substring(0,1).toUpperCase()).append(name.substring(1));
     Class parentClass = datum.getClass();
     Method getMethod = parentClass.getMethod(workName.toString(), new Class[] {});
     Object currentField = getMethod.invoke(datum, new Object[] {});
     if (currentField instanceof ArrayList) {
       labels = (ArrayList) currentField;
       values = (ArrayList) currentField;
     }
     valueAttribute = target;
     if (values.size() > 0) setBeanData(datum, valueAttribute, (String) values.get(0));
   } catch (Exception e) {
     e.printStackTrace();
     labels = new ArrayList();
     values = new ArrayList();
   }
 }
 
 // Method to set the data from context
 public void setData(Object datum, Map contextMap) {
 }
 
 // Method to add a new element to a collection
 public int addDatum(Object datum) {
   return -1;
 }
 
 public static String tokenizerNext(StringTokenizer content) {
   String result = null;
   if (content != null) {
     if (content.hasMoreTokens()) {
       result = content.nextToken();
     }
   }
   return result;
 }
 
 public void setButtonIcon(Icon buttonIcon) {
   this.buttonIcon = buttonIcon;
   if (textComponent != null) {
     if (textComponent instanceof JButton) {
       ((JButton) textComponent).setIcon(buttonIcon);
     }
   }
 }
 
 public void setAlternateIcon(Icon alternateIcon) {
   this.alternateIcon = alternateIcon;
 }
 
 public void setGeneral(GeneralDataProcessor general) {
   this.general = general;
 }
 
 public GeneralDataProcessor getGeneral() {
   return general;
 }
 
 public void setPartner(EntryStructure partner) {
 }
 
 public EntryStructure getPartner() {
   return null;
 }
 
 /** Returns an ImageIcon, or null if the path was invalid. */
 protected static ImageIcon createImageIcon(String path) {
   java.net.URL imgURL = EntryStructure.class.getResource(path);
   if (imgURL != null) {
       return new ImageIcon(imgURL);
   } else {
       System.err.println("Couldn't find file: " + path);
       return null;
   }
 }
 
 public static List makeEntryStructure(String name, Properties p, Map usedNames, GeneralDataProcessor overseer) {
   List result = new ArrayList();
   boolean isFramedSet = false;
   List framedSetValues = null;
   EntryStructure newStructure = null;
   EntryStructureTreePane treeStructure = null;
   String temp = null;
   StringTokenizer attributes = null;
   StringTokenizer types = null;
   StringTokenizer sizes = null;

   StringTokenizer labels = null;
   String itsType = null;
   String itsSize = null;
   String itsLabel = null;
   String itsName = name + "Form";
   String workName = null;
   String it = p.getProperty(itsName);
   if (it != null) {
     usedNames.put(itsName, itsName);
     attributes = new StringTokenizer(it, ", ");
     itsName = name + "Type";
     it = p.getProperty(itsName);
     if (it != null) {
       usedNames.put(itsName, itsName);
       types = new StringTokenizer(it, ", ");
     }
     itsName = name + "Size";
     it = p.getProperty(itsName);
     if (it != null) {
       usedNames.put(itsName, itsName);
       sizes = new StringTokenizer(it, ", ");
     }
     itsName = name + "Label";
     it = p.getProperty(itsName);
     if (it != null) {
       usedNames.put(itsName, itsName);
       labels = new StringTokenizer(it, ", ");
     }
     while (attributes.hasMoreTokens()) {
       itsName = attributes.nextToken();
       itsType = tokenizerNext(types);
       itsSize = tokenizerNext(sizes);
       itsLabel = tokenizerNext(labels);
       if (TABLEPANE.equals(itsType)) {
         newStructure = new EntryStructureTablePane(itsName, itsSize);
       } else if (FLATTABLE.equals(itsType)) {
         newStructure = new EntryStructureFlatTable(itsName, itsSize);
       } else if (TABBEDPANE.equals(itsType)) {
         newStructure = new EntryStructureTabbedPane(itsName, itsSize);
       } else if (TREEPANE.equals(itsType)) {
         it = p.getProperty(itsName + "Icons");
         treeStructure = new EntryStructureTreePane(itsName, itsSize, it);
         newStructure = treeStructure;
       } else if (FRAMEPANE.equals(itsType)) {
         newStructure = new EntryStructureFramePane(itsName, itsSize);
         newStructure.setPartner(treeStructure);
         if (treeStructure != null) treeStructure.setPartner(newStructure);
         treeStructure = null;
       } else if (LISTBOX.equals(itsType)) {
         newStructure = new EntryStructureListBox(itsName, itsSize, p.getProperty(itsName + "Action"));
       } else if (PARAMTEXT.equals(itsType)) {
         newStructure = new EntryStructureParamText(itsName, itsSize);
       } else if (POPUP.equals(itsType)) {
         newStructure = new EntryStructurePopupMenu(itsName, itsSize);
       } else if (FRAMEDSET.equals(itsType)) {
         newStructure = new EntryStructure(itsName, itsType, itsSize);
         framedSetValues = new ArrayList();
         newStructure.setValues(framedSetValues);
         isFramedSet = true;
       } else if (isFramedSet) {
         framedSetValues.add(makeEntryStructure(itsName, p, usedNames, overseer));
         newStructure = null;
       } else {
         newStructure = new EntryStructure(itsName, itsType, itsSize);
       }
       if (newStructure != null) {
         if (itsLabel != null) newStructure.setLabelSize(itsLabel);
         newStructure.setGeneral(overseer);
         result.add(newStructure);
         workName = itsName + "Action";
         it = p.getProperty(workName);
         if (it != null) {
           newStructure.setAction(it);
           usedNames.put(workName, workName);
         }
         workName = itsName + "Edit";
         it = p.getProperty(workName);
         if (it != null) {
           newStructure.setAction(it);
           usedNames.put(workName, workName);
         }
         workName = itsName + "Order";
         it = p.getProperty(workName);
         if (it != null) {
           newStructure.setOrder(it);
           usedNames.put(workName, workName);
         }
         workName = itsName + "Size";
         it = p.getProperty(workName);
         if (it != null) {
           newStructure.setSize(it);
           usedNames.put(workName, workName);
         }
         workName = itsName + "Tab";
         it = p.getProperty(workName);
         if (it != null) {
           newStructure.setTab(it);
           usedNames.put(workName, workName);
         }
         workName = itsName + "Icon";

         it = p.getProperty(workName);
         if (it != null) {
           newStructure.setButtonIcon(createImageIcon(it));
           usedNames.put(workName, workName);
         }
         workName = itsName + "Text";
         it = p.getProperty(workName);
         if (it != null) {
           newStructure.setText(it);
           usedNames.put(workName, workName);
         }
         workName = itsName + "Source";
         it = p.getProperty(workName);
         if (it != null) {
           newStructure.setSource(it);
           usedNames.put(workName, workName);
         }
         workName = itsName + "Glue";
         it = p.getProperty(workName);
         if (it == null) {
           newStructure.setGlueAfter(true);
         } else {
           newStructure.setGlueAfter(it.equals("yes"));
           usedNames.put(workName, workName);
         }
       }
     }
   }
   return result;
 }
 
}

