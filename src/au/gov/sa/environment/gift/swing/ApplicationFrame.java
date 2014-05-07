package au.gov.sa.environment.gift.swing;
 
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
 
import org.apache.commons.beanutils.BeanUtils;
 
import au.gov.sa.environment.gift.jdbc.*;
 
/**
 * <p>Title: ApplicationFrame</p>
 * <p>Description: Application wrapper for frames - sets up menu bar and frame cache</p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: DEH</p>
 * @author Peter Davey
 * @version 1.0
 */
 
public class ApplicationFrame extends EntryFrame
                             implements ActionListener, ItemListener {
 
 private static ApplicationFrame singleton = null;
 private static boolean appChanges = false;
 private Object appObject;
 private Object appTarget;
 private GeneralDataProcessor overseer;
 private JScrollPane scrollPane;
 private AppMenuStructure appMenus;
 private AppFrameStructure appFrames;
 private String frameName;
 private String firstAction = null;
 private JMenuBar menuBar;
 private JTextArea output;
 private String newline = "\n";
 
 public ApplicationFrame() {
 }
 
 public ApplicationFrame(Object appObject, GeneralDataProcessor overseer) {
   this.appObject = appObject;
   this.overseer = overseer;
   if (appObject instanceof AppHandler) {
     this.appTarget = ((AppHandler) appObject).targetDatum();
   }
   appMenus = new AppMenuStructure(appObject, this);
 
   appFrames = AppFrameStructure.getSingleton(appObject, this, overseer);
   frameName = appFrames.getFirstName();
   frameStructure = appFrames.getFirstFrame();
   firstAction = appFrames.getFirstAction();
   Container contentPane = getContentPane();
 
   if (frameStructure != null) {
     this.setSize(new Dimension(Integer.parseInt(frameStructure.
                                                 getPreferredWidth()),
                                Integer.
                                parseInt(frameStructure.getPreferredHeight())));
     setTitle(frameStructure.getFormTitle());
     Component frameBox = AppFrameStructure.getSingleton().getFrameContent(frameStructure);
     scrollPane = new JScrollPane(frameBox);
     contentPane.add(scrollPane, BorderLayout.CENTER);
     frameStructure.setTabListener(appMenus);
   } else {
 
     //Add regular components to the window, using the default BorderLayout.
     output = new JTextArea(5, 30);
     output.setEditable(false);
     scrollPane = new JScrollPane(output);
     contentPane.add(scrollPane, BorderLayout.CENTER);
   }
 
   //Create the menu bar.
   menuBar = new JMenuBar();
   setJMenuBar(menuBar);
   List menuNames = appMenus.getMenuNames();
   for (int i=0; i < menuNames.size(); i++) {
     menuBar.add(appMenus.createMenu((String) menuNames.get(i), this));
   }
 
   this.addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
            checkChanges();
            System.exit(0);
       }
    });
    singleton = this;
    if (frameStructure != null) {
      if (appObject instanceof AppHandler) {
        AppHandler appHandler = (AppHandler) appObject;
        appHandler.register(this);
        appHandler.loadApp(frameStructure, overseer);
      }
 
 
 

    }
 }
 
 /**
   * Catch up on any subsidiary object creation
   * The data object to be loaded into the frame is set up by constructor
   */
 public void postCreate() {
   Iterator it = appFrames.getFrames();
   while (it.hasNext()) {
     ((EntryFrameStructure) appFrames.getFrame(it.next())).postCreate();
   }
   if (appObject instanceof CallbackHandler && firstAction != null) {
     ((CallbackHandler) appObject).callback(frameStructure, new EntryStructure(firstAction, EntryStructure.BUTTON, null), overseer);
   }
   Object datum = (appTarget == null) ? appObject : appTarget;
   if (datum != null && frameStructure != null) {
     appFrames.setFrameObject(frameName, datum);
     frameStructure.loadData(datum);
     frameStructure.setTabListener(appMenus);
     if (appTarget instanceof EntryValidator)
       ( (EntryValidator) appTarget).setEnabled(true);
   }
 }
 
 /**
   * Retrieve the reference object for the application
   * @return Object The reference data object
   */
 public Object getAppTarget() {
   return appTarget;
 }
 
 /**
   * Retrieve the reference object for a frame
   * @param frameName Identifies the frame
   * @return Object The reference data object
   */
 public Object getFrameTarget(String frameName) {
   return appFrames.getFrameObject(frameName);
 }
 
 /**
   * Store a reference object for the application
   * @param target The reference data object
   */
 public void setAppTarget(Object target) {
   this.appTarget = target;
 }
 
 /**
   * Retrieve the current frame name
   * @return String Name of the current frame
   */
 public String getFrameName() {
   return frameName;
 }
 
 public void setFrame(String frameName, Object onDatum) {
   appFrames.setFrameObject(frameName, onDatum);
   setFrame(frameName);
 }
 
 public void setFrame(String frameName) {
   if (frameName != null) this.frameName = frameName;
   Box frameBox = null;
   EntryFrameStructure newStructure = appFrames.getNamedFrame(this.frameName);
   if (newStructure == null) return;
   if (newStructure != frameStructure) {
     appMenus.stateChanged(null);
     appMenus.setSelectedMenuItem("View", frameName, true);
     frameStructure = newStructure;
     scrollPane.getViewport().setView((Component) appFrames.getFrameContent(frameStructure));
   }
   Object frameDatum = appFrames.getFrameObject(frameName);
   if (appObject instanceof AppHandler) {
      frameDatum = ( (AppHandler) appObject).changeTarget(frameDatum);
      appFrames.setFrameObject(frameName, frameDatum);
   }
   frameStructure.loadData(frameDatum);
   if (frameDatum instanceof EntryValidator) ((EntryValidator) frameDatum).setEnabled(true);
   pack();
 }
 
 public void saveChanges() {
   frameStructure.saveChanges();
 }
 
 
 public void checkChanges() {
   // If there are unsaved changes, offer to save them
   if (appChanges || frameStructure.getUnsavedChanges()) {
     int n = JOptionPane.showConfirmDialog(
                         this, "There are unsaved changes to the current record"
                         + ". \nDo you want to save them?",
                         "Save Changes?",
                         JOptionPane.YES_NO_OPTION);
     if (n == JOptionPane.YES_OPTION) {
        saveChanges();
     }

     appChanges = false;
   }
 }
 
 public static void setAppChanges(boolean newChanges) {
     appChanges = appChanges || newChanges;
 }
 
 public void actionPerformed(ActionEvent e) {
   JMenuItem source = (JMenuItem)(e.getSource());
   String action = appMenus.getMenuAction(source);
   if (output != null) {
     String s = "Action event detected."
         + newline
         + "    Event source: " + source.getText()
         + " (an instance of " + getClassName(source) + ")";
     output.append(s + newline);
     output.append("Menu structure has action " + action + newline);
   }
   try {
     if (action != null) {
       StringTokenizer itsList = new StringTokenizer(action, ",");
       String it = null;
       String currentValue = source.getText();
       while (itsList.hasMoreTokens()) {
         it = itsList.nextToken();
         if ("set".equals(it)) {
           BeanUtils.setProperty(appObject, itsList.nextToken(), currentValue);
         }
         else if ("method".equals(it)) {
           Object appCallback = (appTarget instanceof CallbackHandler) ? appTarget : appObject;
           if (appCallback instanceof CallbackHandler) {
             it = currentValue;
             if (itsList.hasMoreTokens()) it = itsList.nextToken();
             EntryStructure simpleStructure = new EntryStructure(it, "hidden", "0");
             ((CallbackHandler) appCallback).callback(frameStructure, simpleStructure, overseer);
           }
         }
         else if ("setFrame".equals(it)) {
           it = currentValue;
           if (itsList.hasMoreTokens()) it = itsList.nextToken();
           setFrame(it);
         }
         else if ("select".equals(it)) {
 
         }
       }
     }
   } catch (Exception ex) {
     ex.printStackTrace();
   }
 }
 
   public void itemStateChanged(ItemEvent e) {
       JMenuItem source = (JMenuItem)(e.getSource());
       String s = "Item event detected."
                  + newline
                  + "    Event source: " + source.getText()
                  + " (an instance of " + getClassName(source) + ")"
                  + newline
                  + "    New state: "
                  + ((e.getStateChange() == ItemEvent.SELECTED) ?
                    "selected":"unselected");
       output.append(s + newline);
   }
 
   // Returns just the class name -- no package info.
   protected String getClassName(Object o) {
       String classString = o.getClass().getName();
       int dotIndex = classString.lastIndexOf(".");
       return classString.substring(dotIndex+1);
   }
 
 public static ApplicationFrame getSingleton() {
   return singleton;
 }
}

