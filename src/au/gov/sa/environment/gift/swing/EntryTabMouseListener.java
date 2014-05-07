package au.gov.sa.environment.gift.swing;
 
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
 
/**
 * EntryTabMouseListener - catch mouse events for empty tabbed pane
 * 
 */
public class EntryTabMouseListener implements MouseListener {
  private JTabbedPane tabbedPane;
 
  private void forwardEventToTab(MouseEvent e) {
    if (tabbedPane != null) {
      ChangeListener[] itsListeners = (ChangeListener[])(tabbedPane.getListeners(ChangeListener.class));
      ChangeEvent theEvent = null;
      for (int i=0; i < itsListeners.length; i++) {
        ChangeListener itsListener = itsListeners[i];
        if (itsListener instanceof EntryStructureTabbedPane) {
          theEvent = new ChangeEvent(this);
        } else {
          theEvent = new ChangeEvent(tabbedPane);
        }
        itsListener.stateChanged(theEvent);
      }
    }
  }
 
  public EntryTabMouseListener(JTabbedPane tabbedPane) {
     this.tabbedPane = tabbedPane;
  }
 
  public JTabbedPane getTabbedPane() {
     return tabbedPane;
  }
 
  public void mouseClicked(MouseEvent e) {
     forwardEventToTab(e);
  }
 
  public void mouseEntered(MouseEvent e) {
  }
 
  public void mouseExited(MouseEvent e) {
  }
 
  public void mousePressed(MouseEvent e) {
  }
 
  public void mouseReleased(MouseEvent e) {
  }
}

