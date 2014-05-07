package au.gov.sa.environment.gift.swing;
 
import java.awt.Dimension;
import java.awt.event.MouseListener;
import java.lang.reflect.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.*;
 
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
 
import au.gov.sa.environment.gift.jdbc.*;
 
/**
 * EntryStructureTabbedPane - Hold name, type, size, JComponent for a tabbed pane field
 *
 */
public class EntryStructureTabbedPane extends EntryStructure implements ChangeListener {
 
 private JFrame frame = null;
 private EntryFrameStructure childStructure = null;
 private Object datum = null;
 private Object subDatum = null;
 private Map contentTabs = new HashMap();
 
 private Map contextMap = new HashMap();
 private String className = null;
 private String propertyName = null;
 
 private String keyName = null;
 private Dimension labelSize = new Dimension(100,12);
 
 private List keyNames = null;
 
 private String newAction = null;
 private  List sourceAttributes = null;
 private  List localAttributes = null;
 private  Map beanAttributes = null;
 private  Map values = null;
 private  Object value = null;
 
 
 public EntryStructureTabbedPane(String name, String size) {
   this.name = name;
   this.type = TABBEDPANE;
   this.size = size;
   int sizeInt = 0;
   textComponent = new JTabbedPane();
   displayComponent = textComponent;
   ((JTabbedPane) textComponent).addChangeListener(this);
 }
 
 public void makeModel(Object datum, EntryFrameStructure parentStructure, JFrame frame) {
   try {
     this.datum = datum;
     this.frame = frame;
     if (action == null) return;
 
     int lastDot = className.lastIndexOf('.');
     if (lastDot < 0) {
       String parentClass = datum.getClass().getName();
       lastDot = parentClass.lastIndexOf('.');
       if (lastDot >= 0) {
         className = parentClass.substring(0, lastDot + 1) + className;
       }
     }
     subDatum = Class.forName(className).newInstance();
     JTabbedPane target = (JTabbedPane) textComponent;
     target.add(makeNewTab());
   } catch (Exception e) {
     e.printStackTrace();
   }
 }
 
 /*
   * Make a panel containing the data from datum
   * @param datum the Object to be entabbed
   */
 public JPanel makeTab(Object datum) throws Exception {
   EntryStructureTab it = new EntryStructureTab(datum, frame, general, contextMap);
   String tabName = it.getChildStructure().getBeanLabel(datum, propertyName);
   it.setChangedData();
   JPanel contentPanel = new JPanel();
   contentPanel.setName(tabName);
   contentPanel.add(it.getDisplayContents());
   contentTabs.put(tabName, it);
   return contentPanel;
 }
 
 /*
   * Make a panel containing the data from datum
   * If New is the only tab, add a MouseListener to catch clicks and trigger the New processing
   * @result JPanel the new Tab area
   */
 public JPanel makeNewTab() throws Exception {
   EntryStructureTab it = new EntryStructureTab(subDatum, frame, general, null);
   JPanel contents = new JPanel();
   contents.setName("New");
   contents.add(it.getDisplayContents());

   contentTabs.put("New", it);
   JTabbedPane itsPane = (JTabbedPane) textComponent;
   if (itsPane.getTabCount() <= 1) {
     itsPane.addMouseListener(new EntryTabMouseListener(itsPane));
   }
   return contents;
 }
 
 /**
   * Catch up on any subsidiary object creation
   */
 public void postCreate(EntryFrameStructure esf) {
   makeModel(esf.getDatum(), esf, esf.getFrame());
 }
 
 public boolean isCustomList() {
  return false;
 }
 
 public boolean isEmbedded()   {
   return true;
 }
 
 public boolean isActionable() {
   return !(action == null || action.length() == 0);
 }
 
 public boolean isActionable(Object datum)     {
   if (action != null && action.length() > 0) {
     String actionValue = getBeanData(datum, name);
     return (actionValue == null || actionValue.length() == 0 || "0".equals(actionValue));
   }
   return false;
 }
 
 public boolean isEditable()   {
   return false;
 }
 
 public boolean isEnabled()    {
   return true;
 }
 
 public boolean needsListLookup()      {
   return false;
 }
 
 public void setFrame(JFrame theFrame) {
   this.frame = (EntryFrame) theFrame;
   if (childStructure != null) {
     childStructure.setFrame(theFrame);
   }
 }
 
 public void setData(Object datum, Map contextMap) {
   if (this.contextMap != null) {
     if (contextName != null)
       contextMap.put(contextName, datum);
     this.contextMap.putAll(contextMap);
   }
   this.datum = datum;
   loadEmbeddedData(datum, name);
 }
 
 public void setAction(String action) {
   this.action = action;
   if (action != null) {
     StringTokenizer it = new StringTokenizer(action, ",");
     if (it.hasMoreTokens()) {
       className = it.nextToken();
       if (it.hasMoreTokens()) propertyName = it.nextToken();
     }
     if (it.hasMoreTokens()) {
       newAction = it.nextToken();
       sourceAttributes = new ArrayList();
       localAttributes = new ArrayList();
       while (it.hasMoreTokens()) {
         stashAttribute(it.nextToken());
       }
     }
   }
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
 
 /* Add a new element to the collection for the tabbed pane
   * If an order is specified, fit the element to be added into the order

   * @param subDatum object to be added
   * @return int tab index of the added pane
   */
 public int addDatum(Object subDatum) {
   int result = -1;
   if (subDatum == null) return result;
 
   String tabName = null;
   try {
     tabName = BeanUtils.getProperty(subDatum, propertyName);
   } catch (Exception e) {
     e.printStackTrace();
     return -1;
   }
   java.util.List contents = general.getContainedData(datum, name);
 
   JTabbedPane target = (JTabbedPane) textComponent;
   List orderList = new ArrayList();
   List existsList = new ArrayList();
   String orderName = null;
   if (order != null) {
     StringTokenizer itsList = new StringTokenizer(order, ",");
     if (itsList.hasMoreTokens()) {
       orderName = itsList.nextToken();
       while (itsList.hasMoreTokens()) {
         orderList.add(itsList.nextToken());
         existsList.add(null);
       }
     }
   }
 
   int tabCount = target.getTabCount();
   int tabPosition = 0;
   String existingName;
   for (int i=0; i < tabCount; i++) {
     existingName = target.getTitleAt(i);
     if (existingName.equals(tabName)) return -1;
     tabPosition = orderList.indexOf(existingName);
     if (tabPosition >= 0) existsList.set(tabPosition, existingName);
   }
   tabPosition = orderList.indexOf(tabName);
   result = (tabPosition < 0 && contents != null) ? contents.size() : 0;
   for (int i=0; i < tabPosition; i++) {
     if (existsList.get(i) != null) result += 1;
   }
 
   if (contents != null) {
     if (result >= contents.size()) {
       contents.add(subDatum);
     } else {
       contents.add(result, subDatum);
     }
   }
   try {
     target.add(makeTab(subDatum), result);
   } catch (Exception e) {
     e.printStackTrace();
     return -1;
   }
 
   return result;
 }
 
/**
  * loadEmbeddedData plugs the Collection on datum identified by attribute
  * into the tabbed pane
  * @param datum the parent data object
  * @param attribute name of the collection attribute
  */
 public void loadEmbeddedData(Object datum, String attribute) {
   JTabbedPane target = (JTabbedPane) textComponent;
   target.removeChangeListener(this);
   target.removeAll();
   contentTabs.clear();
   try {
     List contents = general.getContainedData(datum, attribute);
     if (contents == null) return;
     Iterator it = contents.iterator();
     while (it.hasNext()) {
       Object thisDatum = it.next();
       target.add(makeTab(thisDatum));
     }
      if (subDatum != null) {
          target.add(makeNewTab());
      }
      if (contentTabs.size() > 0) {
       EntryStructureTab theTab = (EntryStructureTab) contentTabs.get(target.getTitleAt(0));
       if (theTab == null) return;
       theTab.getChildStructure().loadData(theTab.getDatum());
       if (contentTabs.size() > 1) removeNewListener();
       if (theTab.getDatum() instanceof EntryValidator)
         ( (EntryValidator) theTab.getDatum()).setEnabled(true);
       target.setSelectedIndex(0);
     }
   } catch (Exception e) {
     e.printStackTrace();
   }
   target.addChangeListener(this);
 }
 

 public void removeNewListener() {
   JTabbedPane itsPane = (JTabbedPane) textComponent;
   MouseListener[] itsListeners = (MouseListener[])(itsPane.getListeners(MouseListener.class));
   for (int i=0; i < itsListeners.length; i++) {
     if (itsListeners[i] instanceof EntryTabMouseListener)
       itsPane.removeMouseListener(itsListeners[i]);
   }
 
 }
 
 public boolean hasNewListener() {
   JTabbedPane itsPane = (JTabbedPane) textComponent;
   MouseListener[] itsListeners = (MouseListener[])(itsPane.getListeners(MouseListener.class));
   for (int i=0; i < itsListeners.length; i++) {
     if (itsListeners[i] instanceof EntryTabMouseListener) return true;
   }
   return false;
 }
 
 public DialogHandler checkDH() {
   DialogHandler itsDH = null;
   try {
     StringBuffer buildName = new StringBuffer();
     String resourceName = datum.getClass().getName();
     int lastIndex = resourceName.lastIndexOf(".");
     buildName.append(resourceName.substring(0, lastIndex+1)).append(newAction);
     resourceName = buildName.toString();
     // Throw ClassNotFound if the class to create doesn't exist
     Class itsClass = Class.forName(resourceName);
     // Throw NoSuchMethodException if it doesn't have a conforming constructor
     Class[] parameterTypes = new Class[] {JFrame.class, Object.class, GeneralDataProcessor.class};
     Constructor createEditor = itsClass.getConstructor(parameterTypes);
     Object[] parameters = new Object[] {frame, datum, getGeneral()};
     Object itsEditor = createEditor.newInstance(parameters);
     if (itsEditor instanceof DialogHandler) {
       itsDH = (DialogHandler) itsEditor;
     }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return itsDH;
  }
 
 private Map createValues(DialogHandler itsDH, Object datum) {
   if (itsDH == null) return null;
   Map values = new HashMap();
   if (datum == null) return values;
 
   String valueName = null;
   String localName = null;
   String contextName = null;
   Object attValue = null;
   try {
     beanAttributes = BeanUtils.describe(datum);
     int dotPos = 0;
     for (int i=0; i < sourceAttributes.size(); i++) {
       valueName = (String) sourceAttributes.get(i);
       localName = (String) localAttributes.get(i);
       dotPos = valueName.lastIndexOf('.');
       if (dotPos < 0) {
         if (beanAttributes.containsKey(valueName)) {
           attValue = beanAttributes.get(valueName);
         } else {
           attValue = null;
         }
       } else {
         contextName = valueName.substring(0, dotPos);
         valueName = valueName.substring(dotPos + 1);
         if (contextMap != null && contextMap.containsKey(contextName)) {
           Object contextDatum = contextMap.get(contextName);
           attValue = PropertyUtils.getProperty(contextDatum, valueName);
         } else {
           attValue = null;
         }
       }
       values.put(localName, attValue);
     }
   } catch (Exception e) {
     e.printStackTrace();
   }
   return values;
 }
 
 public void stateChanged(ChangeEvent e) {
   Object changeObject = e.getSource();
   if (changeObject instanceof EntryTabMouseListener) {
     changeObject = ((EntryTabMouseListener) changeObject).getTabbedPane();
   } else {
     if (hasNewListener()) return;
   }
   if (!(changeObject instanceof JTabbedPane)) return;
   JTabbedPane target = (JTabbedPane) changeObject;
   int tabIndex = target.getSelectedIndex();
   if (tabIndex < 0) {
     if (target.getTabCount() == 0) return;
     tabIndex = 0;
   }
   String tabTitle = target.getTitleAt(tabIndex);
   EntryStructureTab theTab = (EntryStructureTab) contentTabs.get(tabTitle);
   if (theTab != null) {

     if ("New".equals(tabTitle)) {
       int newIndex = -1;
       if (newAction == null) {
         newIndex = addDatum(theTab.getChildStructure().getNewData());
       } else {
         DialogHandler itsDH = checkDH();
         if (itsDH != null) {
           Object datum = itsDH.createNewDatum();
           Map values = createValues(itsDH, datum);
           if (values != null) itsDH.setup(values);
           JDialog dialog = itsDH.createDialog(frame);
           dialog.pack();
           dialog.setVisible(true);
           try {
             if (itsDH.checkCancelState()) {
               target.setSelectedIndex(0);
               return;
             }
             if (itsDH.checkEditState()) {
               if (beanAttributes == null) beanAttributes = BeanUtils.describe(datum);
               values = itsDH.returnValues();
               String valueName = null;
               String localName = null;
               String attValue = null;
               int dotPos = 0;
               for (int i=0; i < sourceAttributes.size(); i++) {
                 valueName = (String) sourceAttributes.get(i);
                 localName = (String) localAttributes.get(i);
                 dotPos = valueName.lastIndexOf('.');
                 if (dotPos < 0 && beanAttributes.containsKey(valueName) && values.containsKey(localName)) {
                   attValue = (String) values.get(localName);
                   BeanUtils.setProperty(datum, valueName, attValue);
                   ApplicationFrame.setAppChanges(true);
                 }
               }
             }
           } catch (Exception ex) {
             ex.printStackTrace();
           }
           newIndex = addDatum(datum);
         }
       }
       if (newIndex >= 0) {
         removeNewListener();
         target.setSelectedIndex(newIndex);
         theTab = (EntryStructureTab) contentTabs.get(target.getTitleAt(newIndex));
       }
     } else {
       theTab.getChildStructure().loadData(theTab.getDatum());
     }
     if (theTab != null && theTab.getDatum() instanceof EntryValidator)
       ( (EntryValidator) theTab.getDatum()).setEnabled(true);
   }
 }
 
 public boolean createTab() {
 
     boolean result = false;
   JTabbedPane target = (JTabbedPane) textComponent;
   int tabIndex = target.getSelectedIndex();
   if (tabIndex < 0) {
     if (target.getTabCount() == 0) return false;
     tabIndex = 0;
   }
   String tabTitle = target.getTitleAt(tabIndex);
   EntryStructureTab theTab = (EntryStructureTab) contentTabs.get(tabTitle);
 
   int newIndex = -1;
 
         DialogHandler itsDH = checkDH();
         if (itsDH != null) {
           Object datum = itsDH.createNewDatum();
           Map values = createValues(itsDH, datum);
           if (values != null) itsDH.setup(values);
           JDialog dialog = itsDH.createDialog(frame);
           dialog.pack();
           dialog.setVisible(true);
           try {
             if (itsDH.checkCancelState()) {
               target.setSelectedIndex(0);
               return result;
             }
             if (itsDH.checkEditState()) {
               if (beanAttributes == null) beanAttributes = BeanUtils.describe(datum);
               values = itsDH.returnValues();
               String valueName = null;
               String localName = null;
               String attValue = null;
               int dotPos = 0;
               for (int i=0; i < sourceAttributes.size(); i++) {
                 valueName = (String) sourceAttributes.get(i);
                 localName = (String) localAttributes.get(i);
                 dotPos = valueName.lastIndexOf('.');
                 if (dotPos < 0 && beanAttributes.containsKey(valueName) && values.containsKey(localName)) {
                   attValue = (String) values.get(localName);
                   BeanUtils.setProperty(datum, valueName, attValue);
                   ApplicationFrame.setAppChanges(true);
                 }
               }
             }

           } catch (Exception ex) {
             ex.printStackTrace();
           }
           newIndex = addDatum(datum);
         }
       if (newIndex >= 0) {
         removeNewListener();
         target.setSelectedIndex(newIndex);
         theTab = (EntryStructureTab) contentTabs.get(target.getTitleAt(newIndex));
       }
         
     if (theTab != null && theTab.getDatum() instanceof EntryValidator)
       ( (EntryValidator) theTab.getDatum()).setEnabled(true);
 
     return true;
 }
 
 public boolean removeTab(String removeName) {
 
   JTabbedPane target = (JTabbedPane) textComponent;
   int tabIndex = target.getSelectedIndex();
   if (tabIndex < 0) return false;
   java.util.List contents = general.getContainedData(datum, name);
   if (contents == null) return false;
 
   String tabTitle = target.getTitleAt(tabIndex);
   if (!tabTitle.equals(removeName)) return false;
   // OK, we've got everything. Now do the remove
   target.remove(tabIndex);
   contents.remove(tabIndex);
   contentTabs.remove(tabTitle);
   if (target.getTabCount() > 0) target.setSelectedIndex(0);
   ApplicationFrame.setAppChanges(true);
   return true;
 
 }
 
 public boolean reassignTab(String reassignName, String targetName) {
   JTabbedPane target = (JTabbedPane) textComponent;
   int tabIndex = target.getSelectedIndex();
   if (tabIndex < 0) return false;
   java.util.List contents = general.getContainedData(datum, name);
   if (contents == null) return false;
 
   String tabTitle = target.getTitleAt(tabIndex);
   if (!tabTitle.equals(reassignName)) return false;
   // OK, we've got everything. Now do the reassign
  // target.remove(tabIndex);
  // contents.remove(tabIndex);
  // contentTabs.remove(tabTitle);
   if (target.getTabCount() > 0) target.setSelectedIndex(0);
   return true;
 }
}

