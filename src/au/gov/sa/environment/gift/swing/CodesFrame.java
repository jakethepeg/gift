package au.gov.sa.environment.gift.swing;
 
import java.awt.Component;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import java.util.*;
 
import org.apache.commons.beanutils.BeanUtils;
 
import au.gov.sa.environment.gift.jdbc.*;
 
/**
 * <p>Title: CodesFrame</p>
 * <p>Description: Allow selection, creation and edit of code table rows</p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: DEH</p>
 * @author Peter Davey
 * @version 1.0
 */
 
public class CodesFrame extends JFrame implements ActionListener, DocumentListener {
 private static final boolean DEBUG=false;
 
 private Object datum = null;
 private Map objectMap = new TreeMap();
 private List names = new ArrayList();
 private List objects = new ArrayList();
 private List formAttributes = new ArrayList();
 private List attributeTypes = new ArrayList();
 private List attributePrompts = new ArrayList();
 
 private String formTitle = "Object Entry Form";
 private String preferredWidth = "300";
 private String preferredHeight = "500";
 private String keyName = null;
 
 private Map selectors = new HashMap();
 private Map docText = new HashMap();
 private Map docLength = new HashMap();
 
 JComboBox objectSelect = null;
 
 private Box box1;
 private Box box7;
 Dimension labelSize = new Dimension(100,12);
 private JButton addButton = new JButton("Create New");
 private JButton updateButton = new JButton("Update Database");
 private JButton deleteButton = new JButton("Delete Row");
 private boolean unsavedChanges = false;
 private Object currentObject = null;
 private int currentIndex = -1;
 
 public CodesFrame() {
 }
 
 public CodesFrame(Object datum) {
   this.datum = datum;
   Properties p = loadFormProperties(datum);
   for (Enumeration psm = p.propertyNames() ; psm.hasMoreElements() ;) {
     String itsName = (String) psm.nextElement();
     String it = p.getProperty(itsName);
     String temp = null;
     StringTokenizer itsList = null;
     if ("formtitle".equals(itsName)) {
       formTitle = it;
     } else if ("keyname".equals(itsName)) {
       keyName = it;
     } else if ("order".equals(itsName)) {
       itsList = new StringTokenizer(it, ", ");
       while (itsList.hasMoreTokens()) {
         temp = itsList.nextToken();
         formAttributes.add(temp);
         attributePrompts.add(temp);
       }
     } else if ("types".equals(itsName)) {
       itsList = new StringTokenizer(it, ", ");
       while (itsList.hasMoreTokens()) attributeTypes.add(itsList.nextToken());
     } else if ("preferredwidth".equals(itsName)) {
       preferredWidth = it;
     } else if ("preferredheight".equals(itsName)) {
       preferredHeight = it;
     } else {
       int i = formAttributes.indexOf(itsName);
       if (i >= 0) {
         attributePrompts.set(i, it);
       }
     }
   }
   this.setSize(new Dimension(Integer.parseInt(preferredWidth), Integer.parseInt(preferredHeight)));
   setTitle(formTitle);
   JdbcDataProcessor.getJdp().getDataFromDb(objectMap, datum);
   names = new ArrayList(objectMap.keySet());
   objects = new ArrayList(objectMap.values());
 
   try {

     jbInit();
   }
   catch(Exception e) {
     e.printStackTrace();
   }
   if (names.size() > 0) {
     setTheData(0);
   }
   this.addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
            checkChanges();
        }
    });
 }
 
 private JLabel newLabel(String labelText) throws Exception {
   JLabel result = new JLabel();
   result.setMinimumSize(labelSize);
   result.setPreferredSize(labelSize);
   result.setHorizontalAlignment(SwingConstants.RIGHT);
   result.setText(labelText);
   return result;
 }
 
 private Box newEntryLine(int index) throws Exception {
   Box result = Box.createHorizontalBox();
   String attributeName = (String) formAttributes.get(index);
   String labelText = (String) attributePrompts.get(index);
   String objectType = (String) attributeTypes.get(index);
   Component component = null;
   JScrollPane descriptionPane = null;
   if (objectType.equals("names")) {
     ListComboBoxModel personSelectModel = new ListComboBoxModel(names);
     JComboBox combo = new JComboBox(personSelectModel);
     combo.addActionListener(this);
     component = combo;
     if (attributeName.equals(keyName)) objectSelect = combo;
   } else if (objectType.equals("text")) {
     JTextField jText1 = new JTextField(16);
     // Listen for changes to text fields
     Document d = jText1.getDocument();
     d.addDocumentListener(this);
     docText.put(d, jText1);
     component = jText1;
   } else if (objectType.equals("textbox")) {
     JTextArea descriptionText = new JTextArea();
     descriptionPane = new JScrollPane(descriptionText);
     descriptionText.setLineWrap(true);
     descriptionText.setWrapStyleWord(true);
     descriptionPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
     descriptionPane.setPreferredSize(new Dimension(350, 250));
     descriptionPane.setBorder(
           BorderFactory.createCompoundBorder(
               BorderFactory.createCompoundBorder(
                               BorderFactory.createTitledBorder(labelText),
                               BorderFactory.createEmptyBorder(5,5,5,5)),
               descriptionPane.getBorder()));
      // Listen for changes to text fields
     Document d = descriptionText.getDocument();
     d.addDocumentListener(this);
     docText.put(d, descriptionText);
     component = descriptionText;
   } else {
     component = null;
   }
   if (component != null) selectors.put(formAttributes.get(index), component);
   if (objectType.equals("textbox")) {
     result.add(Box.createGlue(), null);
     result.add(descriptionPane, null);
   } else {
     result.add(newLabel(labelText), null);
     result.add(Box.createHorizontalStrut(5));
     result.add(component, null);
   }
   result.add(Box.createGlue(), null);
   return result;
 }
 
 private void jbInit() throws Exception {
   box1 = Box.createVerticalBox();
   box7 = Box.createHorizontalBox();
   for (int i=0; i < formAttributes.size(); i++) {
     box1.add(Box.createVerticalStrut(5));
     box1.add(newEntryLine(i), null);
   }
   addButton.setActionCommand("new");
   updateButton.setActionCommand("change");
   updateButton.setEnabled(false);
   deleteButton.setActionCommand("delete");
   box7.add(addButton, null);
   box7.add(Box.createHorizontalStrut(5));
   box7.add(updateButton, null);
   box7.add(Box.createHorizontalStrut(5));
   box7.add(deleteButton, null);
   box1.add(Box.createVerticalStrut(5));
   box1.add(box7, null);
   this.getContentPane().add(box1, BorderLayout.CENTER);
 
   //Listen for actions on all buttons.
   addButton.addActionListener(this);

   updateButton.addActionListener(this);
   deleteButton.addActionListener(this);
 
 }
 /**
   * Read the form layout properties
   * @param datum JavaBean identifying the form properties file
   * @return Properties the form layout properties file
   */
 public Properties loadFormProperties(Object datum) {
 
   String className = datum.getClass().getName();
   String objectName = null;
   int lastIndex = className.lastIndexOf(".");
   if (lastIndex > 0) {
     objectName = className.substring(lastIndex+1);
     className = className.substring(0, lastIndex+1);
   }
 
   // check if there's a properties file
   if (DEBUG) System.out.println("Loading properties for " + objectName);
 
   StringBuffer resourceName = new StringBuffer(className.replace('.', File.separatorChar));
   resourceName.append(className).append("Layout.properties");
   InputStream f = null;
   // Start with the default properties set
   Properties p = new Properties();
   try {
     f = this.getClass().getClassLoader().getResourceAsStream(resourceName.toString());
     if (f == null) {
       if (DEBUG) System.out.println("*** No properties found for " + className);
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
   Iterator it = selectors.keySet().iterator();
   while (it.hasNext()) {
     String itsAttribute = (String) it.next();
     Component itsComponent = (Component) selectors.get(itsAttribute);
     if (itsComponent instanceof JTextComponent) {
        setBeanData(currentObject, itsAttribute, ((JTextComponent) itsComponent).getText());
     }
   }
   JdbcDataProcessor.getJdp().saveChanges(currentObject);
   objects.set(currentIndex, currentObject);
   unsavedChanges = false;
 }
 
 
 public void checkChanges() {
   // If there are unsaved changes, offer to save them
   if (unsavedChanges) {
     int n = JOptionPane.showConfirmDialog(
                         this, "There are unsaved changes to the current record"
                         + ". \nDo you want to save them?",
                         "Save Changes?",
                         JOptionPane.YES_NO_OPTION);
     if (n == JOptionPane.YES_OPTION) {
        saveChanges();
     }
   }
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
 
 public Object getBeanCopy(Object datum) {
   Object it = null;
   try {
     it = BeanUtils.cloneBean(datum);
   } catch (Exception e) {
     e.printStackTrace();
   }
   return it;
 }
 

 public int setNewData() {
   int result = -1;
   Object it = getBeanCopy(datum);
   String s = (String)JOptionPane.showInputDialog("Enter the key for the new row:");
 
   //If a string was returned, say so.
   if ((s != null) && (s.length() > 0)) {
     if (names.indexOf(s) < 0) {
       setBeanData(it, keyName, s);
       names.add(s);
       objects.add(it);
       objectSelect.addItem(s);
       result = names.size() - 1;
       objectSelect.setSelectedIndex(result);
 
     }
   }
   return result;
 }
 
 public void setTheData(int index) {
   // If there are unsaved changes, offer to save them
   checkChanges();
   currentIndex = index;
   try {
     currentObject = getBeanCopy(objects.get(index));
   } catch (Exception e) {
     e.printStackTrace();
   }
   Iterator it = selectors.keySet().iterator();
   while (it.hasNext()) {
     String itsAttribute = (String) it.next();
     Component itsComponent = (Component) selectors.get(itsAttribute);
     if (itsComponent instanceof JTextComponent) {
        JTextComponent theTextComponent = (JTextComponent) itsComponent;
        theTextComponent.setText(getBeanData(currentObject, itsAttribute));
     } else if (itsComponent instanceof JComboBox) {
       try {
        JComboBox theCombo = (JComboBox) itsComponent;
        ComboBoxModel theModel = theCombo.getModel();
        if (theModel instanceof ListComboBoxModel) {
          theCombo.setSelectedIndex(((ListComboBoxModel) theModel).getIndexOf(BeanUtils.getProperty(currentObject, itsAttribute)));
        }
       } catch (Exception e) {
         e.printStackTrace();
       }
     }
   }
 
   unsavedChanges = false;
   updateButton.setEnabled(false);
 }
 
 public void actionPerformed(ActionEvent e) {
   if (DEBUG) System.out.println("Performing " + e.getActionCommand());
   Object it = e.getSource();
   if (it instanceof JComboBox) {
     JComboBox theBox = (JComboBox) it;
     String itsValue = (String) theBox.getSelectedItem();
     int i = theBox.getSelectedIndex();
     if (it == objectSelect) {
       setTheData(i);
     }
   } else {
     if (e.getActionCommand().equals("new")) {
       checkChanges();
       int newMember = setNewData();
       if (newMember >= 0) {
         setTheData(newMember);
         unsavedChanges = true;
       }
     }
     if (e.getActionCommand().equals("change")) {
       saveChanges();
     }
     if (e.getActionCommand().equals("delete")) {
       saveChanges();
     }
   }
   updateButton.setEnabled(unsavedChanges);
 }
 
 
 public boolean checkForChanges(DocumentEvent event) {
   try {
     Document d = event.getDocument();
     Object itsText = docText.get(d);
     String text = d.getText(0, d.getLength());
     if (DEBUG) System.out.println("Checking " + text + " for changes");
     if (itsText != null) {
       Iterator it = selectors.keySet().iterator();
       while (it.hasNext()) {
         String itsAttribute = (String) it.next();
         Component itsComponent = (Component) selectors.get(itsAttribute);
         if (itsComponent == itsText) {
           return !text.equals(getBeanData(currentObject, itsAttribute));
         }
       }
     }
   } catch (Exception e) {

     e.printStackTrace();
   }
   return false;
 }
 
 
   public void insertUpdate(DocumentEvent e) {
     if (!unsavedChanges) unsavedChanges = checkForChanges(e);
     updateButton.setEnabled(unsavedChanges);
   }
   public void removeUpdate(DocumentEvent e) {
     if (!unsavedChanges) unsavedChanges = checkForChanges(e);
     updateButton.setEnabled(unsavedChanges);
   }
   public void changedUpdate(DocumentEvent e) {
     if (!unsavedChanges) unsavedChanges = checkForChanges(e);
     updateButton.setEnabled(unsavedChanges);
   }
 
}

