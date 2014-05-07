package au.gov.sa.environment.gift.swing;
 
import java.util.*;
import javax.swing.*;
 
/**
 * Dialog Handler - interface for custom input handlers
 * Needs a DefaultDialogHandler class that implements it and provides
 * standard behaviour
 */
public interface DialogHandler {
 
   /**
     * Return a dialog for a data entry task.
     *
     */
   public JDialog createDialog(JFrame frame);
 
   /**
     * Returns true if the OK button was hit and the handler has valid data.
     */
   public boolean checkEditState();
 
   /**
     * Returns the option pane selected value
     * @return String the selected value of the option pane
     */
   public String getOptionValue();
 
   /**
     * Returns true if the Cancel button was hit
     */
   public boolean checkCancelState();
 
   /** This method sets up the dialog.
     * @param values Map of attribute name, value
     */
   public void setup(Map values);
 
   /** This method returns values from the dialog.
     * @return Map attribute name <==> value
     */
   public Map returnValues();
 
   /** This method returns a new datum or null.
     * @return Object new entry
     */
   public Object createNewDatum();
 
 
}

