package au.gov.sa.environment.gift.swing;
 
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.plaf.basic.*;
import javax.swing.plaf.metal.*;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.lang.reflect.*;
import java.util.*;
 
import org.apache.commons.beanutils.PropertyUtils;
 
import au.gov.sa.environment.gift.general.*;
import au.gov.sa.environment.gift.jdbc.*;
 
/**
 * EntryStructureParamText - Logic for a parameterised text field
 */
public class EntryStructureParamText extends EntryStructure implements Runnable {
 
 private Object datum = null;
 private List contents = null;
 private JComboBox itsList;
 private JLabel itsLabel;
 private JFrame itsFrame;
 private String listName = null;
 private String controlElement = null;
 private String targetElement = null;
 private String paramDialog = null;
 private String paramDialogName = null;
 private String rebuiltText = null;
 private boolean useList = true;
 private boolean useLabel = true;
 private int itsIndex = -1;
 
 private Properties paramProperties;
 private Map controlValues = new HashMap();
 private Object controlValue;
 private EntryStructureParamEntry entry;
 
 
 public EntryStructureParamText(String name, String size) {
   this.name = name;
   this.type = PARAMTEXT;
   this.size = size;
   this.target = name;
   this.valueAttribute = name;
   int sizeInt = 0;
   contents = new ArrayList();
   listModel = new ListComboBoxModel(contents);
   // itsList = new EntryStructureParamCombo(listModel);
   itsList = new JComboBox(listModel);
   textComponent = itsList;
   displayComponent = itsList;
 }
 
 /**
   * Catch up on any subsidiary object creation
   */
 public void postCreate(EntryFrameStructure esf) {
  // displayComponent = makeTab();
  itsFrame = esf.getFrame();
  datum = esf.getDatum();
  paramProperties = loadParamProperties(esf.getDatum());
  controlElement = paramProperties.getProperty("control");
  targetElement = paramProperties.getProperty("target");
  paramDialog = paramProperties.getProperty("paramDialog");
  paramDialogName = paramDialog;
  int commaPos = (paramDialog == null) ? -1 : paramDialog.indexOf(",");
  if (commaPos > 0) paramDialogName = paramDialog.substring(0, commaPos);
  Enumeration names = paramProperties.propertyNames();
  String it;
  String itsCode;
  String itsValueList;
  String itsValue;
  String itsParam;
  List entries;
  StringTokenizer listScanner;
 
  while (names.hasMoreElements()) {
    it = (String) names.nextElement();
    if (it.startsWith("value")) {
      itsCode = it.substring(5);
      itsValueList = paramProperties.getProperty(it);
      if (itsCode != null && itsValueList != null) {
        entries = new ArrayList();
        listScanner = new StringTokenizer(itsValueList, ", ");
        while (listScanner.hasMoreTokens()) {
          entries.add(new EntryStructureParamEntry(listScanner.nextToken(), paramProperties));
        }
        controlValues.put(itsCode, entries);
      }
    }
  }
 }
 
 /**
   * Read the parameterised text properties

   * @param datum JavaBean identifying the class loader path and default file name
   * @return Properties the parameterised text properties
   */
 private Properties loadParamProperties(Object datum) {
   if (listName == null) return null;
 
   StringBuffer buildName = new StringBuffer();
   String resourceName = datum.getClass().getName();
   int lastIndex = resourceName.lastIndexOf(".");
   resourceName = resourceName.substring(0, lastIndex+1).replace('.', '/');
   buildName.append(resourceName);
   buildName.append(listName).append(".properties");
   java.io.InputStream f = null;
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
 
 public String getListName()   {
   return listName;
}
 
 public String getRebuiltText()   {
   return rebuiltText;
}
 
 public void setRebuiltText(String rebuiltText)   {
     this.rebuiltText = rebuiltText;
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
   String theValue = (datum == null || valueAttribute == null)
     ? null : getBeanData(datum, valueAttribute);
   return theValue;
 }
 
 public int convertValueToIndex(Object datum) {
   if (datum == null || targetElement == null) return -1;
   if (controlValues == null || controlValue == null) return -1;
   EntryStructureParamEntry it;
   List controlList = getControlList();
   if (controlList == null) return -1;
   
   String theValue = getBeanData(datum, targetElement);
   if (theValue == null) return -1;
   
   boolean isUnknownValue = ("-9999".equals(theValue) || "unknown".equals(theValue));
   int result = -1;
   for (int i = 0; (i < controlList.size()) && result < 0; i++) {
       it = (EntryStructureParamEntry) controlList.get(i);
       if (theValue.equals(it.getModText()) || compareParameters(it.getRawText(), theValue)) {
           it.setModText(theValue);
           result = i;
       }
   }
   if (result < 0) {
     if (isUnknownValue) {
       result = -1;
     } else {
       it = new EntryStructureParamEntry(theValue);
       result = controlList.size();
       controlList.add(it);
     }
   }
   itsIndex = result;
   return result;
 }
 
 public int convertNameToIndex(String name) {
   int result = -1;
   if (controlValues != null && controlValue != null) {
     EntryStructureParamEntry it;
     List controlList = getControlList();
     if (controlList == null)
       return -1;
     for (int i = 0; (i < controlList.size()) && result < 0; i++) {
       it = (EntryStructureParamEntry) controlList.get(i);
       if (name.equals(it.getModText()) || compareParameters(it.getRawText(), name))
         result = i;
     }
   }
   itsIndex = result;
   return result;
 }
 
 /** Compare a parameterised text string.
   * @param rawText prototype text containing parameter names
   * @param modText string that may contain parameter values
   * @return boolean true if non-parameter portions match
   */
 public boolean compareParameters(String rawText, String modText) {
   if (rawText == null) return modText == null;
   if (modText == null) return false;
   // Fish parameter names out of param
   StringTokenizer rawTextPieces = new StringTokenizer(rawText, "@");
   List paramNames = new ArrayList();
   List paramPieces = new ArrayList();
   int state = 0;
   int valueStart = 0;
   int valueEnd = 0;
   String thisPiece;
   while (rawTextPieces.hasMoreTokens()) {
     switch (state) {
       case 0:
         thisPiece = rawTextPieces.nextToken();
         valueStart = modText.indexOf(thisPiece, valueEnd);
         if (valueStart < 0) return false;

         valueEnd = valueStart + thisPiece.length();
         state = 1;
         break;
       case 1:
         thisPiece = rawTextPieces.nextToken();
         state = 0;
         break;
     }
   }
   return true;
 }
 
 /** Pose a parameter substitution dialog if the format needs it.
   * Requires a DialogHandler named <..>.swing.<ListParamName>
   * @param param parameters for the parameters
   * @param entry the entry derived from the list properties
   * @return boolean true if a dialog is being posed
   */
 public boolean substituteParameters(String param, EntryStructureParamEntry entry) {
   if (param == null || entry == null) return false;
   if (rebuiltText != null) return false;
   new ParamWorker(this, paramDialogName).start();
   return true;
 }
 
 public boolean convertIndexToValue(int index, Object datum) {
   if (datum == null || valueAttribute == null || index < 0) return false;
  // if (index == itsIndex) return;
   itsIndex = index;
   try {
     if (controlValue != null) {
       List controlList = getControlList();
       if (controlList != null) {
         entry = (EntryStructureParamEntry) controlList.get(index);
         if (substituteParameters(entry.getParam(), entry)) return false;
       }
     }
     LabelValueBean it = (LabelValueBean) contents.get((index >= contents.size()) ? 0 : index);
     String result = (it == null) ? "unknown" : it.getLabel();
     setBeanData(datum, valueAttribute, result);
   } catch (Exception e) {
     e.printStackTrace();
   }
   return true;
 }
 
 
 /**
   * Fill the selector with values.
   * lookupName = paramtext - check the controlling element, and remake list if necessary
   *                        - then check the target element against the list - if it's already
   *                        - got values in it then stuff the changed version into the list
   * lookupName = controlling element - check the controlling element, and remake list if necessary
   *                        - then check the target element against the list - if it's already
   *                        - got values in it then stuff the changed version into the list
   * lookupName = target element - check the target element against the list - if it's already
   *                        - got values in it then stuff the changed version into the list
   * otherwise don't do anything
   * @param lookupName - keyword to control action
   * @param datum - data object for activity
   */
 public void setFromLookup(String lookupName, Object datum) {
   try {
     if (controlElement != null) {
       controlValue = PropertyUtils.getProperty(datum, controlElement);
     }
     int newIndex = convertValueToIndex(datum);
     remakeList();
     contents = labelValue;
     if (values != null && values.size() > 0) valueAttribute = target;
     JComboBox it = (JComboBox) textComponent;
     it.setSelectedIndex(newIndex);
   } catch (Exception e) {
     e.printStackTrace();
   }
 }
 
 private List getControlList() {
     List entries = null;
     String controlKey = (controlValue == null || controlValue.equals("not applicable")) ? "Na" : (String) controlValue;
     entries = (List) controlValues.get(controlKey);
     if (entries == null) entries = new ArrayList();
     return entries;
 }
 
 private void remakeList() {
   if (labelValue == null) labelValue = new ArrayList();
   List entries = getControlList();
   labelValue.clear();
   int maxEntry = entries.size();
   for (int i=0; i < maxEntry; i++) {
     EntryStructureParamEntry it = (EntryStructureParamEntry) entries.get(i);
     labelValue.add(new LabelValueBean(it.getModText()));
   }
   if (labels == null) labels = new ArrayList();
   if (values == null) values = new ArrayList();
   LabelValueBean.remakeList(labelValue, labels, values);
   listModel.setFreshList(labelValue);
 }
 

 
 public void valueChanged(ListSelectionEvent e) {
   Object changeObject = e.getSource();
   int firstIndex = e.getFirstIndex();
   int lastIndex = e.getLastIndex();
   if (contents == null) {
     return;
   }
 }
 
 public void run() {
     if (rebuiltText == null || entry == null) return;
     String holdModText = entry.getModText();          
     entry.setModText(rebuiltText);
     if (!holdModText.equals(rebuiltText)) remakeList();
     setBeanData(datum, valueAttribute, rebuiltText);
     Component itsComponent = (Component) getTextComponent();
     if (itsComponent instanceof JComboBox) {
        JComboBox theCombo = (JComboBox) itsComponent;
        theCombo.setSelectedIndex(-1);
        if (theCombo.getItemAt(itsIndex) != null) {
          theCombo.setSelectedIndex(itsIndex);
        }
     }
     rebuiltText = null;
 }
 
 protected class EntryStructureParamEntry {
   private String rawText;
   private String modText;
   private String param;
 
   public EntryStructureParamEntry(String text) {
     rawText = text;
     modText = text;
     param = null;
   }
 
   public EntryStructureParamEntry(String name, Properties p) {
     rawText = p.getProperty(name);
     setDefaultParameters();
     param = p.getProperty(name + "Param");
   }
 
   /** Make a default parameterised text string using X, Y, ... .
     */
   private void setDefaultParameters () {
     // Fish parameter names out of param
     StringTokenizer rawTextPieces = new StringTokenizer(rawText, "@");
     List paramNames = new ArrayList();
     List paramPieces = new ArrayList();
     int state = 0;
     while (rawTextPieces.hasMoreTokens()) {
       switch (state) {
         case 0: paramPieces.add(rawTextPieces.nextToken());
                 state = 1;
                 break;
         case 1: paramNames.add(rawTextPieces.nextToken());
                 state = 0;
                 break;
       }
     }
     int piece = 0;
     StringBuffer buildName = new StringBuffer();
     for (piece = 0; piece < paramPieces.size(); piece++) {
       buildName.append(paramPieces.get(piece));
       if (piece < paramNames.size()) {
         buildName.append(new String(new char[] {(char) ((int)'X' + piece)}));
       }
     }
     modText = buildName.toString();
   }
 
   public String getRawText() {
     return rawText;
   }
 
   public String getModText() {
     return modText;
   }
 
   public void setModText(String modText) {
     this.modText = modText;
   }
 
   public String getParam() {
     return param;
   }
 }
 public class ParamWorker extends Thread {
     EntryStructureParamText owner;
     String paramDialog;
     List paramNames = new ArrayList();
     List paramPieces = new ArrayList();
     List paramValues = new ArrayList();
     int piece = 0;
     int p = 0;
     int valueStart = 0;
     int valueEnd = 0;
     Map setContent = new HashMap();

 
     public ParamWorker(EntryStructureParamText owner, String paramDialog) {
         super(paramDialog);
         this.owner = owner;
         this.paramDialog = paramDialog;
     }
     public void run() {
         dissectEntry();
         String newText = poseDialog();
         if (newText != null) {
             owner.setRebuiltText(newText);
             SwingUtilities.invokeLater(owner);
         }
     }
     
     private void dissectEntry () {
       paramNames.clear();
       paramPieces.clear();
       paramValues.clear();
       setContent.clear();
         // Fish parameter names out of param
         StringTokenizer rawTextPieces = new StringTokenizer(entry.getRawText(), "@");
         int state = 0;
         while (rawTextPieces.hasMoreTokens()) {
           switch (state) {
             case 0: paramPieces.add(rawTextPieces.nextToken());
                     state = 1;
                     break;
             case 1: paramNames.add(rawTextPieces.nextToken());
                     state = 0;
                     break;
           }
         }
         // Fish parameter values out of modText
         String modText = entry.getModText();
         int piece = 0;
         int p = 0;
         int valueStart = 0;
         int valueEnd = 0;
         while (p < paramNames.size()) {
           String thisPiece = (String) paramPieces.get(piece);
           if (modText == null) {
               paramValues.add(new String(new char[] {(char) ((int)'X' + p)}));
           } else {
               valueStart = modText.indexOf(thisPiece) + thisPiece.length();
               piece += 1;
               if (piece < paramPieces.size()) {
                 thisPiece = (String) paramPieces.get(piece);
                 valueEnd = modText.indexOf(thisPiece);
                 paramValues.add(modText.substring(valueStart, valueEnd));
               } else paramValues.add(modText.substring(valueStart));
           }
           p += 1;
         }
         for (p = 0; p < paramValues.size(); p++) {
           if (((String) paramValues.get(p)).startsWith("@")) {
             paramValues.set(p, "");
           }
           setContent.put(paramNames.get(p), paramValues.get(p));
         }
 
     }
     
     private String poseDialog () {
         String newText = null;
         StringBuffer buildName = new StringBuffer();
         String resourceName = datum.getClass().getName();
         int lastIndex = resourceName.lastIndexOf(".");
         buildName.append(resourceName.substring(0, lastIndex+1));
         if (paramDialog != null) {
           buildName.append(paramDialog);
         } else {
           if (!buildName.toString().endsWith("swing."))
               buildName.append("swing.");
           buildName.append(listName);
         }
         try {
           // Throw ClassNotFound if the class to create doesn't exist
           Class itsClass = Class.forName(buildName.toString());
           // Throw NoSuchMethodException if it doesn't have a conforming constructor
           Class[] parameterTypes = new Class[] {JFrame.class, Object.class, GeneralDataProcessor.class};
           Constructor createEditor = itsClass.getConstructor(parameterTypes);
           Object[] parameters = new Object[] {itsFrame, datum, getGeneral()};
           Object itsEditor = createEditor.newInstance(parameters);
           // If the editor is a DialogHandler, use it
           if (itsEditor instanceof DialogHandler) {
             DialogHandler itsDH = (DialogHandler) itsEditor;
             JDialog dialog = itsDH.createDialog(itsFrame);
             dialog.pack();
             itsDH.setup(setContent);
             dialog.setVisible(true);
             if (!itsDH.checkCancelState()) {
               setContent = (Map) itsDH.returnValues();
                 // Substitute values back into modText
                 buildName.setLength(0);
                 for (piece = 0; piece < paramPieces.size(); piece++) {
                   buildName.append(paramPieces.get(piece));
                   if (piece < paramNames.size()) {
                     buildName.append(setContent.get(paramNames.get(piece)));
                   }

                 }
                  newText = buildName.toString();
             }
            }
          } catch (Exception e) {
            e.printStackTrace();
          }
          return newText;
     }
 }
 
 
}

