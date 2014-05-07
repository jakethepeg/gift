package au.gov.sa.environment.gift.swing;
 
import java.awt.event.*;
import javax.swing.*;
 
/**
 * EntryTabMouseListener - catch mouse events for empty tabbed pane
 * 
 */
public class EntryTableMouseListener implements MouseListener {
  private JTable table;
 
  private void forwardEventToTab(MouseEvent e) {
//      if (tabbedPane != null) tabbedPane.fireStateChanged();
  }
 
  public EntryTableMouseListener(JTable table) {
     this.table = table;
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

