package au.gov.sa.environment.gift.swing;
 
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
 
import au.gov.sa.environment.gift.jdbc.*;
 
/**
 * <p>Title: EntryFrame</p>
 * <p>Description: Generalised data entry frame</p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: DEH</p>
 * @author Peter Davey
 * @version 1.0
 */
 
public class EntryFrame extends JFrame implements ActionListener {
 
 protected Object datum = null;
 protected EntryFrameStructure frameStructure = null;
 
 private Box box1;
 private Box box7;
 Dimension labelSize = new Dimension(100,12);
 private JButton addButton = new JButton("Create New");
 private JButton updateButton = new JButton("Update Database");
 private JButton deleteButton = new JButton("Delete Row");
 
 public EntryFrame() {
 }
 
 /** Frame object: loads content structures from properties
  * @param datum The object which will provide <name>Entry.properties
  */
 public EntryFrame(Object datum) {
   this.datum = datum;
   frameStructure = new EntryFrameStructure(datum, this, new GeneralDataProcessor());
   this.setSize(new Dimension(Integer.parseInt(frameStructure.getPreferredWidth()), Integer.parseInt(frameStructure.getPreferredHeight())));
   setTitle(frameStructure.getFormTitle());
 
   try {
     jbInit();
   }
   catch(Exception e) {
     e.printStackTrace();
   }
   Object realData = frameStructure.checkKeyedDatum();
   if (realData == null) {
     frameStructure.loadData(datum);
   } else {
     frameStructure.loadData(realData);
   }
   this.addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
            checkChanges();
        }
    });
 }
 
 public EntryFrame(Object datum, GeneralDataProcessor overseer) {
   this.datum = datum;
   frameStructure = new EntryFrameStructure(datum, this, overseer);
   this.setSize(new Dimension(Integer.parseInt(frameStructure.getPreferredWidth()), Integer.parseInt(frameStructure.getPreferredHeight())));
   setTitle(frameStructure.getFormTitle());
 
   try {
     jbInit();
 
     this.addWindowListener(new WindowAdapter() {
          public void windowClosing(WindowEvent e) {
              checkChanges();
          }
      });
   } catch(Exception e) {
     e.printStackTrace();
   }
 }
 
 /**
   * Catch up on any subsidiary object creation
   * @param datum The data object to be loaded into the frame
   */
 public void postCreate(Object datum) {
   if (frameStructure != null) frameStructure.postCreate();
   if (datum != null) {
     frameStructure.loadData(datum);
   }
 }
 
 private void jbInit() throws Exception {
   box1 = Box.createVerticalBox();
   box7 = Box.createHorizontalBox();
   frameStructure.buildStructure(box1);
   String buttons = frameStructure.getButtons();
   if ("none".equals(buttons)) {
   } else if ("standard".equals(buttons)) {
     addButton.setActionCommand("new");
     updateButton.setActionCommand("change");
     updateButton.setEnabled(false);

     deleteButton.setActionCommand("delete");
 
     //Listen for actions on all buttons.
     addButton.addActionListener(this);
     updateButton.addActionListener(this);
     deleteButton.addActionListener(this);
     box7.add(addButton, null);
     box7.add(Box.createHorizontalStrut(5));
     box7.add(updateButton, null);
     box7.add(Box.createHorizontalStrut(5));
     box7.add(deleteButton, null);
     box1.add(Box.createVerticalStrut(5));
     box1.add(box7, null);
   } else {
     frameStructure.buildButtons(box7);
     box1.add(Box.createVerticalStrut(5));
     box1.add(box7, null);
   }
   this.getContentPane().add(box1, BorderLayout.CENTER);
 
 }
 
 public EntryFrameStructure getFrameStructure() {
   return frameStructure;
 }
 
 public void checkUpdateButton(boolean unsavedChanges) {
   updateButton.setEnabled(unsavedChanges);
 }
 
 public void saveChanges() {
   frameStructure.saveChanges();
 }
 
 
 public void checkChanges() {
   // If there are unsaved changes, offer to save them
   if (frameStructure.getUnsavedChanges()) {
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
 
 public void actionPerformed(ActionEvent e) {
   Object it = e.getSource();
   if (e.getActionCommand().equals("new")) {
     checkChanges();
//      int newMember = frameStructure.setNewData();
//      if (newMember >= 0) {
//        frameStructure.setTheData(newMember);
//      }
   }
   if (e.getActionCommand().equals("change")) {
     saveChanges();
   }
   if (e.getActionCommand().equals("delete")) {
     saveChanges();
   }
   updateButton.setEnabled(frameStructure.getUnsavedChanges());
 }
}

