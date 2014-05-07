package au.gov.sa.environment.gift.swing;
 
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Dimension;
import java.awt.event.*;
import java.io.*;
import java.lang.reflect.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import java.util.*;
 
import org.apache.commons.beanutils.BeanUtils;
 
import au.gov.sa.environment.gift.general.*;
import au.gov.sa.environment.gift.jdbc.*;
import au.gov.sa.environment.gift.xml.*;
 
/**
 * <p>Title: EntryFrameStructure</p>
 * <p>Description: Load structure from properties and express it</p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: DEH</p>
 * @author Peter Davey
 * @version 1.0
 */
 
public class EntryFrameStructure implements ActionListener, DocumentListener, MouseListener, java.awt.print.Printable {
 private static final boolean DEBUG=Boolean.getBoolean("SWINGDEBUG");
 
 private EntryFrame frame = null;
 private Object datum = null;
 private Map objectMap = new TreeMap(); // Name = EntryStructure mapping
 private Map contextMap = new TreeMap(); // Name = Object mapping
 private List lines = new ArrayList(); // of ArrayList of EntryStructure
 
 private GeneralDataProcessor overseer;
 
 private Map listenerMap = new HashMap(); // Document == EntryStructure mapping
 private EntryStructure loadDataStructure = null; // set by owner to implement custom editing
 
 private String formTitle = "Object Entry Form";
 private String preferredWidth = "300";
 private String preferredHeight = "500";
 private String boxWidth = "350";
 private String boxHeight = "250";
 private String labelWidth = "100";
 private String keyName = null;
 private String contextName = null;
 private String buttons = null;
 private Dimension labelSize = new Dimension(100,18);
 private List labelSizeList = new ArrayList();
 
 private JButton updateButton = null;
 private String updateButtonName = null;
 
 private Map selectors = new HashMap();
 private Map docText = new HashMap();
 private Map docLength = new HashMap();
 
 private List keyNames = null;
 private Object currentObject = null;
 private boolean unsavedChanges = false;
 private boolean loadingData = false;
 
 public EntryFrameStructure(Object datum, JFrame frame, GeneralDataProcessor overseer) {
   this(null, datum, frame, overseer);
 }
 
 public EntryFrameStructure(String name, Object datum, JFrame frame, GeneralDataProcessor overseer) {
   this.overseer = overseer;
   this.datum = datum;
   this.frame = (EntryFrame) frame;
   loadDataStructure = new EntryStructure("loadData", (name == null) ? "displaytext" : name, "0");
   if (datum != null) {
     if (datum instanceof CallbackHandler) {
       ((CallbackHandler) datum).callback(this, loadDataStructure, overseer);
     }
     Properties p = loadFrameProperties(name, datum);
     Map usedNames = new HashMap();
     getDataProperties(p, usedNames);
     getOtherProperties(p, usedNames);
   }
 }
 
 public EntryFrameStructure(Object datum, JFrame frame) {
   this.overseer = new GeneralDataProcessor();
   this.datum = datum;
   this.frame = (EntryFrame) frame;
   if (datum != null) {
     Properties p = loadFrameProperties(datum);
     Map usedNames = new HashMap();
     getDataProperties(p, usedNames);
     getOtherProperties(p, usedNames);
   }
 }
 

 public void setOverseer(GeneralDataProcessor overseer) {
   this.overseer = overseer;
 }
 
 /**
   * Create the data structure
   */
 public void getDataProperties(Properties p, Map usedNames) {
   String itsName = null;
   String temp = null;
   StringTokenizer itsList = null;
   EntryStructure theStructure = null;
   keyNames = new ArrayList();
   String it = findString("keys", p, usedNames);
   if (DEBUG) System.out.println(" -keys- is " + it);
   if (it != null) {
     itsList = new StringTokenizer(it, ", ");
     while (itsList.hasMoreTokens()) {
       keyNames.add(itsList.nextToken());
     }
   }
   Map datalistTargets = new HashMap();
   it = findString("datalist", p, usedNames);
   if (it != null) {
     if (DEBUG) System.out.println(" -datalist- is " + it);
     itsList = new StringTokenizer(it, ", ");
     while (itsList.hasMoreTokens()) {
       temp = itsList.nextToken();
       int tempMark = temp.indexOf('=');
       if (tempMark > 0) {
         datalistTargets.put(temp.substring(0, tempMark), temp.substring(tempMark+1));
       }
     }
   }
   it = findString("context", p, usedNames);
   if (it != null) {
     if (DEBUG) System.out.println(" -context- is " + it);
     contextName = it;
   }
   List unresolvedLookups = new ArrayList();
   it = findString("lines", p, usedNames);
   if (DEBUG) System.out.println(" -lines- is " + it);
   if (it != null) {
     itsList = new StringTokenizer(it, ", ");
     while (itsList.hasMoreTokens()) {
       temp = itsList.nextToken();
       List lineStructure = EntryStructure.makeEntryStructure(temp, p, usedNames, overseer);
       if (lineStructure != null) {
         lines.add(lineStructure);
         for (int i=0; i<lineStructure.size(); i++) {
           doStructurePost((EntryStructure) lineStructure.get(i), p, usedNames, datalistTargets, unresolvedLookups);
         }
       }
     }
     for (int i=0; i<unresolvedLookups.size(); i++) {
       theStructure = (EntryStructure) unresolvedLookups.get(i);
       it = theStructure.getType();
       if (overseer.checkNamelist(it, datum) == 0) {
          theStructure.setFromLookup(it, datum);
       }
     }
   }
 }
 
 /**
   * Catch up on any subsidiary object creation
   */
 public void doStructurePost(EntryStructure theStructure, Properties p, Map usedNames, Map datalistTargets, List unresolvedLookups) {
   if (contextName != null) theStructure.setContextName(contextName);
   String customName = theStructure.getName();
   String customType = theStructure.getType();
   objectMap.put(customName, theStructure);
   if (theStructure.isCustomList()) {
      // Lists can be filled from an ArrayList on the data object
      if (EntryStructure.DATALIST.equals(customType)) {
        if (datalistTargets.containsKey(customName)) {
          theStructure.setTarget((String) datalistTargets.get(customName));
        }
      } else {
      // Lists can be defined in the ...Entry.properties file or have its own properties file for a database lookup
        String localType;
        if (EntryStructure.LISTBOX.equals(customType)) {
          localType = ((EntryStructureListBox) theStructure).getListName();
        if (localType != null) localType = findString(localType, p, usedNames);
        } else
          localType = findString(customType, p, usedNames);
        if (DEBUG) System.out.println(" -" + customType + "- is " + localType);
        if (localType == null) {
          unresolvedLookups.add(theStructure);
        } else {
          theStructure.setFromList(localType);
        }
      }
   } else if (theStructure.isEmbedded()) {
      customType = customName + "Table";
      theStructure.setAction(findString(customType, p, usedNames));
      if (DEBUG) System.out.println("Embedded structure " + customType + " " + theStructure.getAction());
   } else if (EntryStructure.FRAMEDSET.equals(customType)) {
     List values = theStructure.getValues();
//      if (DEBUG) System.out.println("Framed set contains " + values.toString());

     List containedLine = null;
     for (int i = 0; i < values.size(); i++) {
       containedLine = (List) values.get(i);
       for (int j = 0; j < containedLine.size(); j++) {
         doStructurePost((EntryStructure) containedLine.get(j), p, usedNames, datalistTargets, unresolvedLookups);
       }
     }
   }
 }
 
 /**
   * Catch up on any subsidiary object creation
   */
 public void getOtherProperties(Properties p, Map usedNames) {
   String itsName = null;
   StringTokenizer itsList = null;
   String it = findString("preferredwidth", p, usedNames);
   if (it != null) preferredWidth = it;
   it = findString("preferredheight", p, usedNames);
   if (it != null) preferredHeight = it;
   it = findString("boxwidth", p, usedNames);
   if (it != null) boxWidth = it;
   it = findString("boxheight", p, usedNames);
   if (it != null) boxHeight = it;
   it = findString("labelwidth", p, usedNames);
   if (it != null) {
     itsList = new StringTokenizer(it, ", ");
     while (itsList.hasMoreTokens()) {
       labelSizeList.add(new Dimension(Integer.parseInt(itsList.nextToken()),12));
     }
   }
   it = findString("formtitle", p, usedNames);
   if (it != null) formTitle = it;
   it = findString("keyname", p, usedNames);
   if (it != null) keyName = it;
   it = findString("buttons", p, usedNames);
   if (it != null) buttons = it;
   it = findString("updateButton", p, usedNames);
   if (it != null) updateButtonName = it;
   EntryStructure theStructure = null;
   for (Enumeration psm = p.propertyNames() ; psm.hasMoreElements() ;) {
     itsName = (String) psm.nextElement();
     if (!usedNames.containsKey(itsName)) {
       it = p.getProperty(itsName);
       if (objectMap.containsKey(itsName)) {
         theStructure = (EntryStructure) objectMap.get(itsName);
         theStructure.setLabel(it);
       }
     }
   }
   if (labelSizeList.size() == 0) labelSizeList.add(labelSize);
   labelSize = (Dimension) labelSizeList.get(0);
 }
 
 /**
   * Catch up on any subsidiary object creation
   */
 public void postCreate() {
   int iLimit = lines.size();
   List lineStructure = null;
   EntryStructure theStructure = null;
   for (int line=0; line<iLimit; line++) {
     lineStructure = (List) lines.get(line);
     for (int i=0; i<lineStructure.size(); i++) {
       theStructure = (EntryStructure) lineStructure.get(i);
       theStructure.postCreate(this);
     }
   }
 }
 
 public JFrame getFrame() {
   return frame;
 }
 
 public Object getDatum() {
   return datum;
 }
 
 public Object getCurrentObject() {
   return currentObject;
 }
 
 public Map getContextMap() {
   return contextMap;
 }
 
 public void setContextMap(Map contextMap) {
   this.contextMap.putAll(contextMap);
 }
 
 public String getFormTitle() {
   return formTitle;
 }
 
 public String getPreferredWidth() {
   return preferredWidth;
 }
 
 public String getPreferredHeight() {
   return preferredHeight;

 }
 
 public String getKeyName() {
   return keyName;
 }
 
 public void setListReducers(ListReducer reducer) {
   Iterator it = objectMap.keySet().iterator();
   while (it.hasNext()) {
     String itsAttribute = (String) it.next();
     EntryStructure itsElement = (EntryStructure) objectMap.get(itsAttribute);
     Component itsComponent = (Component) itsElement.getTextComponent();
     if (itsComponent != null && itsComponent instanceof JComboBox) {
       itsElement.setListReducer(reducer);
     }
   }
 }
 
 public boolean getUnsavedChanges() {
   return unsavedChanges;
 }
 
 public void setUnsavedChanges(boolean unsavedChanges) {
   this.unsavedChanges = unsavedChanges;
 }
 
 /**
   * For each attribute in key names (in major -> minor sequence),
   * (initial implementation only handles single key pages:
   *     the key name provides a value for byName in the select)
   * @return Object the Javabean representing the single selected row
   */
 public Object checkKeyedDatum() {
   Object result = null;
   if (keyNames != null && keyNames.size() == 1) {
     String keyName = (String) keyNames.get(0);
     String keyAttribute = getBeanData(datum, keyName);
     JdbcDataProcessor db = null;
     if (overseer instanceof JdbcDataProcessor) db = (JdbcDataProcessor) overseer;
     if (db == null) {
       result = datum;
     } else {
       HashMap dataSet = new HashMap();
       try {
         if (db.getDBConnection() != null) {
           db.executeSelectBy(datum, dataSet, keyAttribute);
           Iterator keys = dataSet.keySet().iterator();
           if (keys.hasNext()) result = dataSet.get(keys.next());
         }
       } catch (Exception ex) {
         ex.printStackTrace();
       }
     }
   }
   return result;
 }
 
 private String findString(String property, Properties p, Map usedNames) {
   String it = p.getProperty(property);
   if (it != null) usedNames.put(property, property);
   return it;
 }
 
 private void applySize(String dimension, String sizeText, JComponent target) {
   int commaPos = sizeText.indexOf("/");
   String xComponent, yComponent;
   if (commaPos < 0) {
     xComponent = sizeText;
     yComponent = sizeText;
   } else if (commaPos > 0) {
     xComponent = sizeText.substring(0, commaPos);
     yComponent = sizeText.substring(commaPos+1);
   } else {
     xComponent = sizeText;
     yComponent = sizeText;
   }
   Dimension newSize;
   try {
     newSize = new Dimension(Integer.parseInt(xComponent), Integer.parseInt(yComponent));
   } catch (Exception e) {
     return;
   }
   if (dimension == null) {
     target.setSize(newSize);
   } else if ("max".equals(dimension)) {
     target.setMaximumSize(newSize);
   } else if ("min".equals(dimension)) {
     target.setMinimumSize(newSize);
   } else if ("pref".equals(dimension)) {
     target.setPreferredSize(newSize);
   }
 }
 
 private void applySize(String sizeText, Object target) {
   if (target instanceof JComponent) {
     StringTokenizer sizeSpec = new StringTokenizer(sizeText, ", ");
     while (sizeSpec.hasMoreTokens()) {
       String thisSpec = sizeSpec.nextToken();
       int equalPos = thisSpec.indexOf("=");
       if (equalPos <= 0) {

         applySize((String) null, thisSpec, (JComponent) target);
       }
       else {
         applySize(thisSpec.substring(0,equalPos), thisSpec.substring(equalPos+1), (JComponent) target);
       }
     }
   }
 }
 
 private JLabel newLabel(String labelText) throws Exception {
   JLabel result = new JLabel();
   result.setMinimumSize(labelSize);
   result.setPreferredSize(labelSize);
   result.setHorizontalAlignment(SwingConstants.RIGHT);
   result.setText(labelText);
   return result;
 }
 
 private JLabel newLabel(String labelText, String labelSize) throws Exception {
   JLabel result = null;
   try {
     Dimension thisSize = (Dimension) labelSizeList.get(Integer.parseInt(labelSize));
     int thisHeight = thisSize.height;
     int thisWidth = thisSize.width;
     int trail = 0;
     int i = labelText.indexOf("<br>", trail);
     while (i >= 0) {
       thisHeight += thisSize.height;
       trail = i + 4;
       i = labelText.indexOf("<br>", trail);
     }
     thisSize = new Dimension(thisWidth, thisHeight);
     result = new JLabel();
     result.setMinimumSize(thisSize);
     result.setPreferredSize(thisSize);
     result.setHorizontalAlignment(SwingConstants.RIGHT);
     result.setText(labelText);
   } catch (Exception e) {
     result = newLabel(labelText);
   }
   return result;
 }
 
 private void setTabBounds(Component target, String tab) throws Exception {
   int it = StringUtil.safeToNum(tab);
   int y = target.getY();
   Dimension pref = target.getPreferredSize();
   if (it >= 0 && pref != null) {
     target.setBounds(it, y, pref.width, pref.height);
   }
 }
 
 private JTextComponent newTextDoc(EntryStructure component) throws Exception {
   JTextComponent itsText = null;
   Object it = component.getTextComponent();
   if (it instanceof JTextComponent) {
     itsText = (JTextComponent) it;
     String objectSize = component.getSize();
     String objectTab = component.getTab();
     // Listen for changes to text fields
     Document d = itsText.getDocument();
     d.addDocumentListener(this);
     docText.put(d, itsText);
     docLength.put(d, component.getSize());
     selectors.put(itsText, component);
     MouseListener[] ml = itsText.getMouseListeners();
     for (int i = 0; i < ml.length; i++) {
       itsText.removeMouseListener(ml[i]);
     }
     if (objectSize != null && objectSize.indexOf("=") >= 0) applySize(objectSize, itsText);
     if (objectTab != null) setTabBounds(itsText, objectTab);
   }
   return itsText;
 }
 
 private AbstractButton newButton(EntryStructure component) throws Exception {
   AbstractButton itsButton = null;
   Object it = component.getTextComponent();
   if (it instanceof AbstractButton) {
     itsButton = (AbstractButton) it;
     itsButton.setText(component.getLabel());
     itsButton.setIcon(component.getButtonIcon());
     itsButton.addActionListener(this);
     selectors.put(itsButton, component);
   }
   return itsButton;
 }
 
 private JComponent newCombo(EntryStructure es) {
   JComponent itsComponent = null;
   String objectSize = es.getSize();
   if (es.isActionable(datum)) {
     Object it = es.getTextComponent();
     if (it instanceof JComboBox) {
       itsComponent = (JComponent) it;
       JComboBox itsCombo = (JComboBox) itsComponent;
       ActionListener[] ml = itsCombo.getActionListeners();
       for (int i = 0; i < ml.length; i++) {
         if (ml[i] instanceof EntryFrameStructure)
           itsCombo.removeActionListener(ml[i]);

       }
       itsCombo.addActionListener(this);
       selectors.put(itsComponent, es);
       if (objectSize != null && objectSize.indexOf("=") >= 0) applySize(objectSize, itsComponent);
     }
   } else {
     itsComponent = new JTextField();
   }
   return itsComponent;
 }
/** newEntryLine builds the Swing structures for a line
  * @param lineContents ArrayList of EntryStructures
  * @return JComponent containing the pieces
  */
 private JComponent newEntryLine(List lineContents) {
   JComponent result = new JPanel();
   result.setLayout(new BoxLayout(result, BoxLayout.X_AXIS));
//   result.setAlignmentX(0);
   boolean simpleLine = true;
 
   EntryStructure it = null;
   String attributeName = null;
   String objectType = null;
   String objectSize = null;
   String labelText = null;
   String labelSize = null;
   Component component = null;
   JComponent thePane = null;
   JScrollPane descriptionPane = null;
   boolean glueAfter = false;
   DialogClickEditor theListener = null;
 
   try {
     for (int i=0; i<lineContents.size(); i++) {
       it = (EntryStructure) lineContents.get(i);
       attributeName = it.getName();
       objectType = it.getType();
       objectSize = it.getSize();
       labelText = it.getLabel();
       labelSize = it.getLabelSize();
       glueAfter = it.isGlueAfter();
       thePane = null;
       if (objectType.equals(EntryStructure.TEXT) || objectType.equals(EntryStructure.NUMERICTEXT)
        || objectType.equals(EntryStructure.LOWERTEXT) || objectType.equals(EntryStructure.DISPLAYTEXT)) {
         component = newTextDoc(it);
         if (objectType.equals(EntryStructure.DISPLAYTEXT)) {
             if (theListener != null) component.addMouseListener(theListener);
         } else {
             component.addMouseListener(this);
         }
       } else if (objectType.equals(EntryStructure.FRAMEDLINE)) {
         result.setBorder(
               BorderFactory.createCompoundBorder(
                   BorderFactory.createCompoundBorder(
                                   BorderFactory.createTitledBorder(labelText),
                                   BorderFactory.createEmptyBorder(3,3,3,3)),
                   result.getBorder()));
         if ("1".equals(it.getSize())) {
           theListener = new DialogClickEditor(it, frame, datum, contextMap, this);
           theListener.postCreate();
           result.addMouseListener(theListener);
         }
         component = result;
       } else if (objectType.equals(EntryStructure.FRAMEDSET)) {
         simpleLine = false;
         result.setLayout(new BoxLayout(result, BoxLayout.Y_AXIS));
         result.setBorder(
               BorderFactory.createCompoundBorder(
                   BorderFactory.createCompoundBorder(
                                   BorderFactory.createTitledBorder(labelText),
                                   BorderFactory.createEmptyBorder(3,3,3,3)),
                   result.getBorder()));
         List contents = it.getValues();
         for (int j = 0; j < contents.size(); j++) {
           result.add(newEntryLine((List) contents.get(j)), null);
           if (j < contents.size() - 1) result.add(Box.createVerticalStrut(3));
         }
         result.add(Box.createVerticalGlue());
         component = result;
       } else if (objectType.equals(EntryStructure.TEXTBOX) || objectType.equals(EntryStructure.DISPLAYBOX)) {
         simpleLine = false;
         JTextComponent descriptionText = newTextDoc(it);
         descriptionPane = new JScrollPane(descriptionText);
         ((JTextArea) descriptionText).setLineWrap(true);
         ((JTextArea) descriptionText).setWrapStyleWord(true);
         descriptionPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
         descriptionPane.setPreferredSize(new Dimension(Integer.parseInt(boxWidth), Integer.parseInt(boxHeight)));
         descriptionPane.setBorder(
               BorderFactory.createCompoundBorder(
                   BorderFactory.createCompoundBorder(
                                   BorderFactory.createTitledBorder(labelText),
                                   BorderFactory.createEmptyBorder(3,3,3,3)),
                   descriptionPane.getBorder()));
         component = descriptionText;
         if (objectType.equals(EntryStructure.TEXTBOX)) descriptionText.addMouseListener(this);
       } else if (objectType.equals(EntryStructure.BUTTON)) {
         component = newButton(it);
       } else if (objectType.equals(EntryStructure.CHECKBOX)) {
         component = newButton(it);
       } else if (objectType.equals(EntryStructure.TABBEDPANE)

                  || objectType.equals(EntryStructure.TABLEPANE)) {
         simpleLine = false;
         thePane = (JComponent) it.getDisplayComponent();
       } else if (objectType.equals(EntryStructure.TREEPANE)) {
         simpleLine = false;
         thePane = (JComponent) it.getDisplayComponent();
         if (objectSize != null) applySize(objectSize, thePane);
       } else if (objectType.equals(EntryStructure.FRAMEPANE)) {
         simpleLine = false;
         thePane = (JComponent) it.getTextComponent();
         if (objectSize != null) applySize(objectSize, thePane);
         thePane = (JComponent) it.getDisplayComponent();
       } else if (objectType.equals(EntryStructure.HIDDEN)) {
         component = null;
       } else {
         component = newCombo(it);
       }
       if (thePane != null) {
         thePane.setEnabled(it.isEnabled());
         listenerMap.put(attributeName, thePane);
       } else if (component != null) {
         component.setEnabled(it.isEnabled());
         listenerMap.put(attributeName, component);
       }
       if (objectType.equals(EntryStructure.TEXTBOX) || objectType.equals(EntryStructure.DISPLAYBOX)) {
         result.add(Box.createGlue(), null);
         result.add(descriptionPane, null);
       } else if (objectType.equals(EntryStructure.TABBEDPANE)
                  || objectType.equals(EntryStructure.TABLEPANE)) {
         result.add(Box.createGlue(), null);
         result.add(thePane, null);
       } else if (objectType.equals(EntryStructure.TREEPANE)) {
         result.add(thePane, null);
       } else if (objectType.equals(EntryStructure.FRAMEPANE)) {
         result.add(thePane, null);
         glueAfter = true;
       } else if (objectType.equals(EntryStructure.CHECKBOX) || objectType.equals(EntryStructure.BUTTON)) {
         result.add(Box.createGlue(), null);
         result.add(component, null);
       } else if (objectType.equals(EntryStructure.LITERAL)) {
         result.add((JComponent) it.getDisplayComponent(), null);
       } else if (!objectType.equals(EntryStructure.FRAMEDLINE)
               && !objectType.equals(EntryStructure.FRAMEDSET)
               && !objectType.equals(EntryStructure.HIDDEN)) {
        if (labelText != null && labelText.length() > 0) {
         if (labelSize == null) {
           result.add(newLabel(labelText), null);
          }
          else {
           result.add(newLabel(labelText, labelSize), null);
         }
        }
         if (objectType.equals(EntryStructure.LISTBOX)
             && component instanceof JComboBox)
           component = (JComponent) it.getDisplayComponent();
         if (component != null) {
           result.add(Box.createHorizontalStrut(3));
           result.add(component, null);
         }
       }
       if (glueAfter) result.add(Box.createGlue(), null);
     }
     if (simpleLine) {
       // If the line contains a single row of fixed height objects, prevent it
       // from being stretched in adverse situations
       JPanel cont = new JPanel(new BorderLayout());
       cont.add(result, BorderLayout.NORTH);
       result = cont;
     }
   } catch (Exception e) {
     e.printStackTrace();
   }
 
   return result;
 }
 
 /**
   * Construct the button components into the given container.
   * @param container horizontal box to hold the buttons
   */
 public void buildButtons(Box container) {
 }
 
 /**
   * Construct the frames components into the given container.
   * @param container vertical box to hold the lines
   */
 public void buildStructure(Container container) {
   for (int i=0; i<lines.size(); i++) {
     container.add(Box.createVerticalStrut(3));
     container.add(newEntryLine((ArrayList) lines.get(i)), null);
   }
   container.add(Box.createVerticalGlue());
   if (updateButtonName != null) {
     if (objectMap.containsKey(updateButtonName)) {
       EntryStructure itsElement = (EntryStructure) objectMap.get(
           updateButtonName);
       Component itsComponent = (Component) itsElement.getTextComponent();
       if (itsComponent instanceof JButton) {
         updateButton = (JButton) itsComponent;

       }
     }
   }
   drillThrough(container.getComponents());
 }
 
 /**  Work through the component structure for debugging
   * @param theList a list of components
   * */
 public void drillThrough(Component[] theList) {
   Component it;
   Component[] its;
   for (int i=0; i < theList.length; i++) {
     it = theList[i];
     if (it instanceof Container) {
       its = ((Container) it).getComponents();
       if (its.length > 0 ) drillThrough(its);
     }
   }
 }
 
 /**
   * Read the frame layout properties
   * @param datum JavaBean identifying the form properties file
   * @return Properties the form layout properties file
   */
 public Properties loadFrameProperties(Object datum) {
 
   return loadFrameProperties(null, datum);
 }
 
 /**
   * Read the frame layout properties
   * @param name File name is "<datum path><name>Entry.properties"
   *             If name is null, the objects class name is used
   * @param datum JavaBean identifying the form properties file
   * @return Properties the form layout properties file
   */
 public Properties loadFrameProperties(String name, Object datum) {
 
   String resourceName = datum.getClass().getName();
   int lastIndex = resourceName.lastIndexOf(".");
   String className = (lastIndex > 0 && name == null)
       ? className = resourceName.substring(lastIndex+1) : name;
   resourceName = resourceName.substring(0,lastIndex+1).replace('.', '/');
 
   // check if there's a properties file
   InputStream f = null;
   Properties p = new Properties();
   try {
     f = this.getClass().getClassLoader().getResourceAsStream(resourceName + className + "Entry.properties");
     if (f == null) {
       if (DEBUG) System.out.println("*** No properties found for " + resourceName + className + "Entry.properties");
     } else {
 
       // Get the properties
       p.load(f);
       f.close();
     }
   } catch (Exception e) {
     e.printStackTrace();
   }
   return p;
 }
 
 public void saveChanges() {
   Iterator iter = selectors.keySet().iterator();
   while (iter.hasNext()) {
     Object itsSelf = iter.next();
     EntryStructure itsStructure = (EntryStructure) selectors.get(itsSelf);
     if (itsSelf instanceof JTextComponent) {
       String itsAttribute = itsStructure.getName();
       setBeanData(currentObject, itsAttribute, ((JTextComponent) itsSelf).getText());
     }
   }
 }
 
 public String getButtons() {
   return buttons;
 }
 
 public void setBeanData(Object it, String name, String value) {
   try {
     if (it != null) BeanUtils.setProperty(it, name, value);
   } catch (Exception e) {
     System.out.println("Failed to set " + name);
   }
 }
 
 public String getBeanData(Object it, String name) {
   String result = null;
   try {
     if (it != null) result = BeanUtils.getProperty(it, name);
   } catch (Exception e) {
     System.out.println("Failed to get " + name);
   }
   return result;
 }
 
 public String getBeanLabel(Object it, String name) {

   String result = getBeanData(it, name);
   if (result == null) return null;
   if (objectMap.containsKey(name)) {
     EntryStructure theStructure = (EntryStructure) objectMap.get(name);
     result = theStructure.getLabelData(result);
   }
   return result;
 }
 
 public Object getBeanCopy(Object datum) {
   Object it = null;
   try {
     it = overseer.cloneDatum(datum);
   } catch (Exception e) {
     e.printStackTrace();
   }
   return it;
 }
 
 public Object getNewData() {
   Object it = overseer.cloneNewDatum(datum);
   String s = null;
   int keyIndex = -1;
   try {
     for (int i=0; i<keyNames.size(); i++) {
       // Iterate through keys, setting values in the new row for each until the last
       String keyName = (String) keyNames.get(i);
       EntryStructure itsStructure = (EntryStructure) objectMap.get(keyName);
       if (i == keyNames.size() - 1) {
         // If there's a field on the form for the key, get a fresh one
         s = (String)JOptionPane.showInputDialog("Enter the new " + itsStructure.getLabel() + ":");
         // If no string was returned, give up.
         if (s == null || s.length() <= 0) return null;
         keyIndex = itsStructure.convertNameToIndex(s);
         if (keyIndex >= 0) itsStructure.convertIndexToValue(keyIndex, it);
       } else {
         // If it's a major key, set the value in the new data object
       }
     }
   } catch (Exception e) {
     e.printStackTrace();
   }
   return it;
 }
 
 public boolean confirmDelete() {
   String keyName = (String) keyNames.get(keyNames.size() - 1);
   EntryStructure itsStructure = (EntryStructure) objectMap.get(keyName);
       // If there's a field on the form for the key, get a fresh one
   return (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(frame, "Remove " + itsStructure.getLabel() + "?",
                                                 "Confirm Remove Request", JOptionPane.YES_NO_OPTION));
 }
 
 public boolean confirmDelete(String promptText) {
   return (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(frame, "Remove " + promptText + "?",
                                                 "Confirm Remove Request", JOptionPane.YES_NO_OPTION));
 }
 
 /**
   * Set initial conditions for the frame -
   */
 public void setupData() {
 }
 
 /**
   * Set initial conditions for the frame -
   */
 public void checkComponentListeners(Component theComponent, Object datum) {
//    MouseListener[] listeners = theComponent.getMouseListeners(); Java 1.4 version
   MouseListener[] listeners = (MouseListener[])(theComponent.getListeners(MouseListener.class));
   for (int i=0; i< listeners.length; i++) {
     if (listeners[i] instanceof DialogClickEditor) {
       ((DialogClickEditor) listeners[i]).setDatum(datum);
     }
   }
 }
 
 public void setFrame(JFrame theFrame) {
   this.frame = (EntryFrame) theFrame;
   Iterator it = objectMap.keySet().iterator();
   while (it.hasNext()) {
     String itsAttribute = (String) it.next();
     EntryStructure itsElement = (EntryStructure) objectMap.get(itsAttribute);
     itsElement.setFrame(theFrame);
   }
 }
 
 public void loadData(Object theData) {
   loadingData = true;
   try {
     loadDataCore(theData);
   } catch (Exception e) {
     if (DEBUG) System.out.println("Bad data load");
   }
    if (updateButton != null) {
     unsavedChanges = false;
     updateButton.setEnabled(unsavedChanges);
   }
   loadingData = false;
 }

 
 public void loadDataCore(Object theData) throws Exception {
   if (theData instanceof ListReducer) setListReducers((ListReducer) theData);
   currentObject = theData;
   datum = theData;
   Iterator it = objectMap.keySet().iterator();
   while (it.hasNext()) {
     String itsAttribute = (String) it.next();
     EntryStructure itsElement = (EntryStructure) objectMap.get(itsAttribute);
     Component itsComponent = (Component) itsElement.getTextComponent();
     Component screenComponent = (Component) listenerMap.get(itsAttribute);
     if (itsComponent == null) {
       if (screenComponent != null) {
         checkComponentListeners(screenComponent, theData);
       }
     } else {
       if (itsComponent instanceof JTextComponent) {
          JTextComponent theTextComponent = (JTextComponent) itsComponent;
          String textValue = getBeanData(currentObject, itsAttribute);
          // Here's where we get the combo label value if this TextComponent is a denatured pick list
          if (itsElement.isCustomList()) {
            textValue = itsElement.convertValueToName(theData);
          }
          theTextComponent.setText(textValue);
       } else if (itsComponent instanceof JCheckBox) {
          ((JCheckBox) itsComponent).setSelected(("Y".equals(getBeanData(currentObject, itsAttribute))));
       } else if (itsComponent instanceof JTable) {
         itsElement.setData(currentObject, contextMap);
       } else if (itsComponent instanceof JTabbedPane) {
         itsElement.setData(currentObject, contextMap);
       } else if (itsComponent instanceof JTree) {
         itsElement.setData(currentObject, contextMap);
       } else if (itsComponent instanceof JComboBox) {
          String itsType = itsElement.getType();
          if (EntryStructure.DATALIST.equals(itsType)) {
            itsElement.setFromLookup(itsType, theData);
          } else if (EntryStructure.PARAMTEXT.equals(itsType)) {
            itsElement.setFromLookup(itsType, theData);
          } else {
            if (itsElement.getListReducer() == null && (itsElement.isLocalDataList() || overseer.checkNamelist(itsType, datum) == 0)) {
              JComboBox theCombo = (JComboBox) itsComponent;
              theCombo.setSelectedIndex(-1);
              int index = itsElement.convertValueToIndex(currentObject);
              if (index < 0) {
                index = 0;
                itsElement.convertIndexToValue(index, currentObject);
              }
              if (theCombo.getItemAt(index) != null) {
                theCombo.setSelectedIndex(index);
              }
            } else {
              itsElement.setFromLookup(itsType, theData);
            }
          }
          if (itsComponent != screenComponent && screenComponent instanceof JTextComponent)
            ((JTextComponent) screenComponent).setText(itsElement.getScreenValue());
       }
     }
   }
 }
 
 public void appendCsvHeader(EntryStructureTablePane itsElement, StringBuffer content) {
   EntryTableModel model = itsElement.getTheModel();
   int columnCount = model.getColumnCount();
   for (int i=0; i < columnCount; i++) {
     if (i > 0) content.append(",");
     content.append(StringUtil.toCSV(model.getColumnName(i)));
   }
   content.append("\n");
 }
 
 public void appendCsvData(EntryStructureTablePane itsElement, StringBuffer content) {
   EntryTableModel model = itsElement.getTheModel();
   int columnCount = model.getColumnCount();
   int rowCount = model.getRowCount();
   for (int i=0; i < rowCount; i++) {
     for (int j=0; j < columnCount; j++) {
       if (j > 0) content.append(",");
         Object it = model.getValueAt(i, j);
         if (it instanceof String) {
           content.append(StringUtil.toCSV( (String) it));
         } else if (it instanceof Boolean) {
           content.append(StringUtil.toCSV( ((Boolean) it).booleanValue()));
         }
     }
     content.append("\n");
   }
 }
 
 public String getCsvContent() {
   StringBuffer result = new StringBuffer();
   Iterator it = objectMap.keySet().iterator();
   while (it.hasNext()) {
     String itsAttribute = (String) it.next();
     EntryStructure itsElement = (EntryStructure) objectMap.get(itsAttribute);
     if (itsElement instanceof EntryStructureTablePane) {
       appendCsvHeader((EntryStructureTablePane) itsElement, result);
       appendCsvData((EntryStructureTablePane) itsElement, result);
     }
   }

   return result.toString();
 }
 
 public void refreshTable(String attributeName) {
   EntryStructure itsElement = (EntryStructure) objectMap.get(attributeName);
   if (itsElement != null) {
     Component itsComponent = (Component) itsElement.getTextComponent();
     if (itsComponent instanceof JTable) {
       itsElement.setData(currentObject, contextMap);
     }
   } else {
     if (DEBUG) System.out.println("No form element ");
   }
 }
 
 public void loadText(Object theData) {
   loadingData = true;
   try {
     loadTextCore(theData);
   } catch (Exception e) {
     e.printStackTrace();
     if (DEBUG) System.out.println("Bad data load");
   }
   loadingData = false;
 }
 
 public void loadTextCore(Object theData) throws Exception {
   currentObject = theData;
   Iterator it = objectMap.keySet().iterator();
   while (it.hasNext()) {
     String itsAttribute = (String) it.next();
     EntryStructure itsElement = (EntryStructure) objectMap.get(itsAttribute);
     Component itsComponent = (Component) itsElement.getTextComponent();
     Component screenComponent = (Component) listenerMap.get(itsAttribute);
     if (itsComponent != null) {
       if (itsComponent instanceof JTextComponent) {
          JTextComponent theTextComponent = (JTextComponent) itsComponent;
          String textValue = getBeanData(currentObject, itsAttribute);
          // Here's where we get the combo label value if this TextComponent is a denatured pick list
          if (itsElement.isCustomList()) {
            textValue = itsElement.convertValueToName(theData);
          }
          theTextComponent.setText(textValue);
       } else if (itsComponent instanceof JCheckBox) {
          ((JCheckBox) itsComponent).setSelected(("Y".equals(getBeanData(currentObject, itsAttribute))));
       } else if (itsComponent instanceof JComboBox) {
          String itsType = itsElement.getType();
          if (EntryStructure.DATALIST.equals(itsType)) {
             itsElement.setFromLookup(itsType, theData);
          } else if (EntryStructure.PARAMTEXT.equals(itsType)) {
             itsElement.setFromLookup(itsType, theData);
          } else {
            JComboBox theCombo = (JComboBox) itsComponent;
            theCombo.setSelectedIndex(-1);
            int index = itsElement.convertValueToIndex(currentObject);
            if (index < 0) {
              index = 0;
              itsElement.convertIndexToValue(index, currentObject);
            }
            if (theCombo.getItemAt(index) != null) {
              theCombo.setSelectedIndex(index);
              itsElement.fireListChanged();
            }
          }
       } else if (itsComponent instanceof JTable) {
          itsElement.setData(currentObject, contextMap);
       }
     }
   }
 }
 
 public void resetTabbedPane(JTabbedPane itsComponent, Object datum, String attribute) {
   try {
   itsComponent.removeAll();
   Class contextClass = datum.getClass();
   StringBuffer workName = new StringBuffer("get");
   workName.append(attribute.substring(0,1).toUpperCase()).append(attribute.substring(1));
   Method getMethod = contextClass.getMethod(workName.toString(), new Class[] {});
   Object currentField = getMethod.invoke(datum, new Object[] {});
   } catch (Exception e) {
     e.printStackTrace();
   }
 
 }
 
 public void actionPerformed(ActionEvent e) {
   Object it = e.getSource();
   String itsValue = null;
   int itsIndex = -1;
   String itsAttribute = null;
   String itsAction = null;
   EntryStructure itsStructure = null;
   JComboBox theBox = null;
   if (loadingData) {
     return;
   }
   if (it instanceof JComboBox) {
     theBox = (JComboBox) it;
     itsValue = (String) theBox.getSelectedItem();
     itsIndex = theBox.getSelectedIndex();

   }
   if (it instanceof JCheckBox) {
     JCheckBox checkBox = (JCheckBox) it;
     if (checkBox.isSelected()) {
       itsValue = "Y";
     } else {
       itsValue = "N";
     }
   }
   if (it instanceof JMenuItem) {
     return;
   }
 
   if (selectors.containsKey(it)) {
     itsStructure = (EntryStructure) selectors.get(it);
   }
   if (itsStructure != null) {
     itsAttribute = itsStructure.getName();
     itsAction = itsStructure.getAction();
     if (theBox != null) {
       if (!itsStructure.convertIndexToValue(itsIndex, currentObject)) return;
     } else {
       setBeanData(currentObject, itsAttribute, itsValue);
     }
     if (currentObject instanceof EntryValidator)
       ((EntryValidator) currentObject).validateChanges(this, itsStructure);
   }
   if (itsAction != null) {
     doAction(itsAction, itsAttribute, itsStructure);
   } else {
     if (theBox != null) {
       itsStructure.convertIndexToValue(itsStructure.convertNameToIndex(itsValue), currentObject);
     } else {
       setBeanData(currentObject, itsAttribute, itsValue);
     }
   }
 }
 
 private void doAction(String itsAction, String itsAttribute, EntryStructure itsStructure) {
   if (itsAction == null) return;
   StringTokenizer attributes = new StringTokenizer(itsAction, ", ");
   if (attributes.hasMoreTokens()) {
     String actionAttribute = attributes.nextToken();
     String attributeValue = null;
     if ("getdata".equals(actionAttribute)) {
         actionAttribute = getBeanData(currentObject, itsAttribute);
         HashMap dataSet = new HashMap();
         try {
           JdbcDataProcessor jdp = JdbcDataProcessor.getJdp();
           if (jdp.getDBConnection() != null) {
             jdp.executeSelectBy(currentObject, dataSet, actionAttribute);
             Iterator keys = dataSet.keySet().iterator();
             if (keys.hasNext()) {
               Object newObject = dataSet.get(keys.next());
               loadData(newObject);
             }
           }
         } catch (Exception ex) {
           ex.printStackTrace();
         }
       } else if ("method".equals(actionAttribute)) {
           attributeValue = attributes.nextToken();
           if ("callback".equals(attributeValue)) {
             if (currentObject instanceof CallbackHandler) {
               if (((CallbackHandler) currentObject).callback(this, itsStructure, overseer)) {
                 loadText(currentObject);
               }
             }
           }
       } else if ("setData".equals(actionAttribute)) {
             attributeValue = attributes.nextToken();
             if (objectMap.containsKey(attributeValue)) {
               EntryStructure setStructure = (EntryStructure) objectMap.get(attributeValue);
               if (currentObject instanceof CallbackHandler) {
                 if (((CallbackHandler) currentObject).callback(this, itsStructure, overseer)) {
                   LoadWorker doIt = new LoadWorker(this, currentObject);
                   doIt.execute();
                 }
               }
             }
     } else if ("data".equals(actionAttribute)) {
       if (loadingData) {
         itsStructure.setDataValue(currentObject);
       } else {
         loadText(currentObject);
       }
     } else if ("writexml".equals(actionAttribute)) {
         try {
           XmlDataProcessor xmlProcessor = new XmlDataProcessor();
           xmlProcessor.setOwningFrame(frame);
           xmlProcessor.writeXml(currentObject);
         } catch (Exception ex) {
           ex.printStackTrace();
         }
     } else if ("validate".equals(actionAttribute)) {
        try {
          if (DEBUG) System.out.println(" - processing Validation" + currentObject.toString());
          if (currentObject instanceof ValidatingObject) {
            ValidatingObject vo = (ValidatingObject) currentObject;
            java.util.List errors = vo.doValidation();

            if (errors.size() == 0) {
              JOptionPane.showConfirmDialog(null, "No Errors Found",
                  "information", JOptionPane.OK_CANCEL_OPTION);
            } else {
              JOptionPane.showConfirmDialog(null, new JList(errors.toArray()),
                  "errors", JOptionPane.OK_CANCEL_OPTION);
            }
           }
        } catch (Exception ex) {
          ex.printStackTrace();
        }
 
     } else if ("update".equals(actionAttribute)) {
        saveChanges();
       }
     }
 }
 
 public String findAttribute(Object component) {
   if (selectors.containsKey(component)) {
      EntryStructure it = (EntryStructure) selectors.get(component);
      return it.getName();
   }
   return null;
 }
 
 public EntryStructure findElement(Object component) {
   if (selectors.containsKey(component)) {
      return (EntryStructure) selectors.get(component);
   }
   return null;
 }
 
 public EntryStructure findElementByName(String name) {
   if (objectMap.containsKey(name)) {
      return (EntryStructure) objectMap.get(name);
   }
   return null;
 }
 
 
 public boolean checkForChanges(DocumentEvent event) {
   try {
     Document d = event.getDocument();
     Object itsText = docText.get(d);
     String itsAttribute = findAttribute(itsText);
     String text = d.getText(0, d.getLength());
     if (itsText != null && itsAttribute != null) {
       if (!text.equals(getBeanData(currentObject, itsAttribute))) {
         if (currentObject instanceof EntryValidator) {
           if (((EntryValidator) currentObject).validateChanges(this, findElement(itsText), text)) {
             setBeanData(currentObject, itsAttribute, text);
             return true;
           }
         }
       }
     }
   } catch (Exception e) {
     e.printStackTrace();
   }
   return false;
 }
 
 
   public void insertUpdate(DocumentEvent e) {
     boolean newChanges = checkForChanges(e);
     if (!unsavedChanges && !loadingData) unsavedChanges = newChanges;
     if (frame != null) frame.checkUpdateButton(unsavedChanges);
     if (updateButton != null) updateButton.setEnabled(unsavedChanges);
   }
   public void removeUpdate(DocumentEvent e) {
     boolean newChanges = checkForChanges(e);
     if (!unsavedChanges && !loadingData) unsavedChanges = newChanges;
     if (frame != null) frame.checkUpdateButton(unsavedChanges);
     if (updateButton != null) updateButton.setEnabled(unsavedChanges);
   }
   public void changedUpdate(DocumentEvent e) {
     boolean newChanges = checkForChanges(e);
     if (!unsavedChanges && !loadingData) unsavedChanges = newChanges;
     if (frame != null) frame.checkUpdateButton(unsavedChanges);
     if (updateButton != null) updateButton.setEnabled(unsavedChanges);
   }
 
   public boolean setTabListener(ChangeListener listener) {
     boolean result = false;
     Iterator it = objectMap.keySet().iterator();
     while (it.hasNext()) {
       String itsAttribute = (String) it.next();
       EntryStructure itsElement = (EntryStructure) objectMap.get(itsAttribute);
       Component itsComponent = (Component) itsElement.getTextComponent();
       if (itsComponent instanceof JTabbedPane) {
         ((JTabbedPane) itsComponent).addChangeListener(listener);
         result = true;
       }
     }
     return result;
   }
 
   public boolean createTab() {
     boolean result = false;

     Iterator it = objectMap.keySet().iterator();
     while (it.hasNext()) {
       String itsAttribute = (String) it.next();
       EntryStructure itsElement = (EntryStructure) objectMap.get(itsAttribute);
       Component itsComponent = (Component) itsElement.getTextComponent();
       if (itsComponent instanceof JTabbedPane) {
         result = ((EntryStructureTabbedPane) itsElement).createTab() || result;
       }
     }
     return result;
   }
 
   public boolean removeTab(String name) {
     boolean result = false;
     Iterator it = objectMap.keySet().iterator();
     while (it.hasNext()) {
       String itsAttribute = (String) it.next();
       EntryStructure itsElement = (EntryStructure) objectMap.get(itsAttribute);
       Component itsComponent = (Component) itsElement.getTextComponent();
       if (itsComponent instanceof JTabbedPane) {
         result = ((EntryStructureTabbedPane) itsElement).removeTab(name) || result;
       }
     }
     return result;
   }
 
   public boolean reassignTab(String name, String targetName) {
     boolean result = false;
     Iterator it = objectMap.keySet().iterator();
     while (it.hasNext()) {
       String itsAttribute = (String) it.next();
       EntryStructure itsElement = (EntryStructure) objectMap.get(itsAttribute);
       Component itsComponent = (Component) itsElement.getTextComponent();
       if (itsComponent instanceof JTabbedPane) {
         result = ((EntryStructureTabbedPane) itsElement).reassignTab(name, targetName) || result;
       }
     }
     return result;
   }
 
   public void mouseEntered(MouseEvent e) {
  }
   public void mouseExited(MouseEvent e) {
  }
   public void mousePressed(MouseEvent e) {
  }
   public void mouseReleased(MouseEvent e) {
  }
 
  /** Default mouse-driven focus grabber
    */
   public void mouseClicked(MouseEvent e) {
     Component it = (Component) e.getSource();
     if (!it.hasFocus()) it.requestFocusInWindow();
   }
 
 /** Draw the structure to a printer - implementation of java.awt.Printable
   *
   **/
 
 public int print(Graphics g, java.awt.print.PageFormat format,
                  int pagenum) {
   int x = (int) format.getImageableX();
   int y = (int) format.getImageableY();
   int w = (int) format.getImageableWidth();
   int h = (int) format.getImageableHeight();
   int hMax = h * pagenum;
   JFrame myFrame = getFrame();
   int frameMax = (int) myFrame.getContentPane().getBounds().getHeight();
   if (frameMax <= hMax) return java.awt.print.Printable.NO_SUCH_PAGE;
   g.translate(x, y + hMax);
 
   myFrame.paint(g);
   return java.awt.print.Printable.PAGE_EXISTS;
 }
 
 
}

