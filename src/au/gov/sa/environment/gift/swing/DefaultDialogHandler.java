package au.gov.sa.environment.gift.swing;
 
import java.awt.*;
import javax.swing.*;
import java.util.*;
 
import org.apache.commons.beanutils.PropertyUtils;
 
import au.gov.sa.environment.gift.jdbc.*;
 
/**
 * <p>Title: DefaultDialogHandler</p>
 * <p>Description: Custom editor for setting up and running a task</p>
 * <p>Copyright: Copyright (c) 2004</p>
 * <p>Company: DEH</p>
 * @author Peter Davey
 * @version 1.0
 */
 
public class DefaultDialogHandler implements DialogHandler {
 
   protected Object datum; // Reference bean
   private Map values = null;
 
   protected String btnString1 = "OK";
   protected String btnString2 = "Cancel";
   private Object[] options = {btnString1, btnString2};
 
   protected GeneralDataProcessor overseer;
   protected EntryFrameStructure frameStructure = null;
 
   protected JFrame frame;
   protected JOptionPane optionPane;
 
   protected boolean validationEnabled;
 
 /**
   * Details from data row
   */
 
 Set valueNames = new HashSet();
 
 /** Creates the GUI shown inside the frame's content pane. */
 public DefaultDialogHandler(JFrame frame, Object datum, GeneralDataProcessor overseer, String[] names) {
   this.frame = frame;
   this.datum = datum;
   this.overseer = overseer;
   frameStructure = new EntryFrameStructure(this, frame, overseer);
   frameStructure.postCreate();
   for (int i=0; i < names.length; i++) valueNames.add(names[i]);
 }
 
 /** Creates the dialog with custom button list. */
 public DefaultDialogHandler(JFrame frame, Object datum, GeneralDataProcessor overseer, String[] names, Object[] options) {
   this(frame, datum, overseer, names);
   this.options = options;
 }
 
   /**
     * Returns true if the OK button was hit and the list has a selection.
     */
   public boolean checkEditState() {
     Object optionValue = optionPane.getValue();
     if (optionValue == null || optionValue == JOptionPane.UNINITIALIZED_VALUE) return false;
     return (!"Cancel".equals(optionValue));
   }
 
   /**
     * Returns true if the Cancel button was hit
     */
   public boolean checkCancelState() {
     Object optionValue = optionPane.getValue();
     if (optionValue == null || optionValue == JOptionPane.UNINITIALIZED_VALUE) return false;
     return optionValue.equals("Cancel");
   }
 
   /**
     * Returns the option pane selected value
     * @return String the selected value of the option pane
     */
   public String getOptionValue() {
     Object optionValue = optionPane.getValue();
     if (optionValue == null || optionValue == JOptionPane.UNINITIALIZED_VALUE) return null;
     return (String) optionValue;
   }
 
   /**
     * Returns true if the OK button was hit and the list has a selection.
     */
   public void setEnabled(boolean enabled) {
     validationEnabled = enabled;
   }
 
   /**
     * Creates a pane containing the two text entry fields and
     * the species list.
     */
   public JDialog createDialog(JFrame frame, String title) {
       //Create the components.
       Box displayBox = Box.createVerticalBox();

       frameStructure.buildStructure(displayBox);
       JPanel thePanel = new JPanel(new BorderLayout());
       thePanel.add(displayBox, BorderLayout.CENTER);
 
       //Create the JOptionPane.
       optionPane = new JOptionPane(thePanel,
                                   JOptionPane.PLAIN_MESSAGE,
                                   JOptionPane.YES_NO_OPTION,
                                   null,
                                   options, // button name list
                                   options[0]); // default button
     return optionPane.createDialog(frame, title);
   }
 
   /**
     * Creates a pane containing the two text entry fields and
     * the species list.
     */
   public JDialog createDialog(JFrame frame) {
     return createDialog(frame, "Default");
   }
 
 
   /** This method sets up the dialog. */
   public void setup(Map values) {
     setup(values, true);
   }
 
   /** This method sets up the dialog. */
   public void setup(Map values, boolean load) {
     this.values = new HashMap(values);
     Iterator it = values.keySet().iterator();
     // Pull values from the map
     while (it.hasNext()) {
       String valueName = (String) it.next();
       Object value = values.get(valueName);
       if (value instanceof Integer) {
           value = value.toString();
       } else if (value instanceof Float) {
           value = value.toString();
       }
       try {
         if (valueNames.contains(valueName)) {
           PropertyUtils.setSimpleProperty(this, valueName, value);
         }
       } catch (Exception e) {
           System.out.println("***** DefaultDialogHandler.setup: exception = " + e.getMessage());
       }
     }
     if (load) {
       validationEnabled = false;
       frameStructure.loadData(this);
       validationEnabled = true;
     }
   }
 
   /** This method returns the dialog value.
    * @return Map name - value pairs matching the input set
    */
    public Map returnValues() {
     HashMap result = new HashMap();
     Iterator it = values.keySet().iterator();
     // Pull values from the map.
     while (it.hasNext()) {
       String valueName = (String) it.next();
       try {
         if (valueNames.contains(valueName)) {
           result.put(valueName, PropertyUtils.getSimpleProperty(this, valueName));
         }
       } catch (Exception e) { }
     }
     return result;
   }
 
   /** This method returns a new datum or null.
     * @return Object new entry
     */
   public Object createNewDatum() {
     return null;
   }
 
}

