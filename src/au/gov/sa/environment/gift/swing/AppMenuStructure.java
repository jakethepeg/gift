package au.gov.sa.environment.gift.swing;
 
import java.io.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.*;
 
import au.gov.sa.environment.gift.jdbc.JdbcDataProcessor;
 
/**
 * <p>Title: AppMenuStructure</p>
 * <p>Description: Load menu structure from properties and express it</p>
 * <p>Copyright: Copyright (c) 2004</p>
 * <p>Company: DEH</p>
 * @author Peter Davey
 * @version 1.0
 */
 
public class AppMenuStructure implements ChangeListener {
 
 private Object datum;
 private ApplicationFrame frame;
 private List menuNames = null;
 private Map menuMap = new HashMap(); // Menu Name = ArrayList of Actions
 private ButtonGroup buttonGroup = null;
 private String thisAction = null;
 private Map menuCache = new HashMap(); // Menu Name = HashMap of Item Name = stuff
 private Map actionCache = new HashMap(); // Menu Item = Action string
 private List listenNames = new ArrayList();      // Base names of tab-linked items
 private Map listenMap = new HashMap(); // listenName = JMenuItem
 
 public AppMenuStructure(Object datum, ApplicationFrame frame) {
   this.frame = frame;
   this.datum = datum;
   if (datum != null) {
     Properties p = loadMenuProperties(datum);
     getMenuProperties(p);
   }
 }
 
 /**
   * Create the data structure
   */
 public void getMenuProperties(Properties p) {
   String itsName = null;
   String temp = null;
   StringTokenizer itsList = null;
   EntryStructure theStructure = null;
   menuNames = new ArrayList();
   String it = p.getProperty("menu");
   if (it != null) {
     itsList = new StringTokenizer(it, ",");
     while (itsList.hasMoreTokens()) {
       temp = itsList.nextToken();
       menuNames.add(temp);
       JMenu thisMenu = makeMenuStructure(temp, p);
       if (thisMenu != null) {
         menuMap.put(temp, thisMenu);
       }
     }
   }
 }
 
 private static String tokenizerNext(StringTokenizer content) {
   String result = null;
   if (content != null) {
     if (content.hasMoreTokens()) {
       result = content.nextToken();
     }
   }
   return result;
 }
 
 public JMenuItem makeButtonItem(String name, boolean selected) {
   JRadioButtonMenuItem result = new JRadioButtonMenuItem(name);
   if (buttonGroup != null) buttonGroup.add(result);
   if (selected) result.setSelected(true);
   return result;
 }
 
 public JMenuItem makeCheckBoxItem(String name, boolean selected) {
   JCheckBoxMenuItem result = new JCheckBoxMenuItem(name);
   if (selected) result.setSelected(true);
   return result;
 }
 
 public String checkActionProperty(String name, Properties p) {
   String result = p.getProperty(name + "Action");
   if (result == null) return thisAction;
   return result;
 }
 
 public JMenu makeMenuStructure(String name, Properties p) {
   JMenu menu = new JMenu(name);
   Map itsItems = new HashMap();
   buttonGroup = null;
   thisAction = checkActionProperty(name, p);
   String itsList = p.getProperty(name);
   if (itsList == null) return menu;
 

   String itsName = null;
   String itsType = null;
   JMenuItem menuItem = null;
   String itsLookup = p.getProperty(name + "Lookup");
   if (itsLookup != null) {
     List lookupResult = new ArrayList();
     JdbcDataProcessor jdp = JdbcDataProcessor.getJdp();
     if (jdp.isConnected()) {
       jdp.simpleSelectUtility(datum, itsLookup, lookupResult);
     }
     for (int i=0; i < lookupResult.size(); i++) {
       itsName = (String) lookupResult.get(i);
       menuItem = new JMenuItem(itsName);
       if (thisAction != null) actionCache.put(menuItem, thisAction);
       itsItems.put(itsName, menuItem);
       menu.add(menuItem);
     }
     menuCache.put(name, itsItems);
     return menu;
   }
     //Build a menu.
   String itsKey = null;
   String itsText = null;
   String it = null;
 
   StringTokenizer entries = new StringTokenizer(itsList, ",");
   StringTokenizer keys = null;
   StringTokenizer text = null;
   StringTokenizer types = null;
   it = p.getProperty(name + "Type");
   if (it != null) types = new StringTokenizer(it, ",");
   it = p.getProperty(name + "Keys");
   if (it != null) keys = new StringTokenizer(it, ",");
   it = p.getProperty(name + "Text");
   if (it != null) text = new StringTokenizer(it, ",");
 
   while (entries.hasMoreTokens()) {
     itsName = entries.nextToken();
     itsType = tokenizerNext(types);
     itsKey = tokenizerNext(keys);
     itsText = tokenizerNext(text);
     if ("method".equals(thisAction)) thisAction = null;
     menuItem = null;
     if ("sep".equals(itsType)) {
       menu.addSeparator();
     } else if ("method".equals(itsType)) {
       menuItem = new JMenuItem(itsName);
       thisAction = itsType;
     } else if ("tablistener".equals(itsType)) {
       menuItem = new JMenuItem(itsName);
       listenNames.add(itsName);
       listenMap.put(itsName, menuItem);
       thisAction = "method";
     } else if ("submenu".equals(itsType)) {
       menuItem = makeMenuStructure(itsName, p);
       menuMap.put(itsName, menuItem);
     } else if ("action".equals(itsType)) {
       thisAction = checkActionProperty(name, p);
       menuItem = new JMenuItem(itsName);
     } else if ("firstButton".equals(itsType)) {
       buttonGroup = new ButtonGroup();
       thisAction = checkActionProperty(name, p);
       menuItem = makeButtonItem(itsName, true);
     } else if ("button".equals(itsType)) {
       menuItem = makeButtonItem(itsName, false);
     } else if ("check".equals(itsType)) {
       menuItem = makeCheckBoxItem(itsName, false);
     } else if ("checked".equals(itsType)) {
       menuItem = makeCheckBoxItem(itsName, true);
     }
     if (menuItem != null) {
       if (thisAction != null) actionCache.put(menuItem, thisAction);
       itsItems.put(itsName, menuItem);
       menu.add(menuItem);
     }
   }
   menuCache.put(name, itsItems);
   return menu;
 }
 
 
 public List getMenuNames() {
   return menuNames;
 }
 
 public Map getCachedMenu(String name) {
   if (menuCache.containsKey(name)) {
     return (Map) menuCache.get(name);
   }
   return null;
 }
 
 public void setSelectedMenuItem(String menu, String name, boolean truth) {
   if (menuCache.containsKey(menu)) {
     Map entries = (Map) menuCache.get(menu);
     if (entries.containsKey(name)) {
       Object it = entries.get(name);
       if (it instanceof JMenuItem) {
         ((JMenuItem) it).setSelected(truth);
       }

     }
   }
 }
 
 public String getMenuAction(JMenuItem forItem) {
   String result = null;
   Object it = actionCache.get(forItem);
   if (it instanceof String) {
     result = (String) it;
   }
   return result;
 }
 
 public JMenu createMenu(String menuName, ApplicationFrame listener) {
   //Build a menu.
   JMenu menu = null;
   Object it = menuMap.get(menuName);
   String itsName = null;
   if (it instanceof JMenu) {
     menu = (JMenu) it;
     it = menuCache.get(menuName);
     if (it instanceof Map) {
       Map itemMap = (Map) it;
       Iterator itemList = itemMap.keySet().iterator();
       while (itemList.hasNext()) {
         itsName = (String) itemList.next();
         it = itemMap.get(itsName);
         if (it instanceof JRadioButtonMenuItem) {
           ( (JRadioButtonMenuItem) it).addActionListener(listener);
         }
         else if (it instanceof JCheckBoxMenuItem) {
           ( (JCheckBoxMenuItem) it).addItemListener(listener);
         } else if (it instanceof JMenu) {
           createMenu(itsName, listener);
         } else if (it instanceof JMenuItem) {
           ( (JMenuItem) it).addActionListener(listener);
         }
       }
     }
   }
   return menu;
 }
 
 /**
   * Read the menu setup properties
   * @param datum JavaBean identifying the menu properties file
   * @return Properties the menu setup properties file
   */
 public Properties loadMenuProperties(Object datum) {
 
   String resourceName = datum.getClass().getName();
   String className = null;
   int lastIndex = resourceName.lastIndexOf(".");
   if (lastIndex > 0) className = resourceName.substring(lastIndex+1);
 
   StringBuffer buildName = new StringBuffer();
   resourceName = resourceName.substring(0, lastIndex+1).replace('.', '/');
   buildName.append(resourceName);
 
   // check if there's a properties file
   buildName.append(className).append("Menu.properties");
   InputStream f = null;
   // Start with the default properties set
   Properties p = new Properties();
   try {
     f = this.getClass().getClassLoader().getResourceAsStream(buildName.toString());
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
 
 public void stateChanged(ChangeEvent e) {
   if (listenNames.size() == 0) return;
   String tabTitle = "New";
   if (e != null) {
     Object changeObject = e.getSource();
     if (! (changeObject instanceof JTabbedPane))
       return;
     JTabbedPane target = (JTabbedPane) changeObject;
     int tabIndex = target.getSelectedIndex();
     if (tabIndex < 0) {
       if (target.getTabCount() == 0)
         return;
       tabIndex = 0;
     }
     tabTitle = target.getTitleAt(tabIndex);
   }
   for (int i=0; i < listenNames.size(); i++) {
     String baseName = (String) listenNames.get(i);
     JMenuItem it = (JMenuItem) listenMap.get(baseName);
     if ("New".equals(tabTitle)) {
       it.setText(baseName);
       it.setEnabled(false);
     }

     else {
       it.setText(baseName + " " + tabTitle);
       it.setEnabled(true);
     }
   }
 }
 
}

