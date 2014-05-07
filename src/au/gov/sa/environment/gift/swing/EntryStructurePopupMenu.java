package au.gov.sa.environment.gift.swing;
 
import java.awt.Dimension;
import java.awt.event.ActionListener;
import javax.swing.*;
import java.util.*;
 
import au.gov.sa.environment.gift.general.*;
 
/**
 * EntryStructurePopupMenu - Decodes action into a string of menu entries.
 * Implements as a JButton, when clicked pops up the menu. Menu actions
 * go back into its structure.
 */
public class EntryStructurePopupMenu extends EntryStructure {
 
 private Object datum;
 private JPopupMenu itsMenu;
 private List menuText = new ArrayList();
 private List menuAction = new ArrayList();
 
 public EntryStructurePopupMenu(String name, String size) {
   this.name = name;
   this.type = POPUP;
   this.size = size;
   this.target = name;
   this.valueAttribute = name;
   int sizeInt = StringUtil.safeToNum(size);
   if (sizeInt < 0) sizeInt = 0;
   JButton actionButton = new JButton();
   if (sizeInt > 0) actionButton.setMinimumSize(new Dimension(sizeInt, buttonHeight));
   textComponent = actionButton;
   displayComponent = actionButton;
   itsMenu = new JPopupMenu();
 }
 
 /**
   * Catch up on any subsidiary object creation
   */
 public void postCreate(EntryFrameStructure efs) {
 
   JMenuItem menuItem;
   // Add entries to the popup menu.
   for (int i=0; i < menuAction.size(); i++) {
     menuItem = new JMenuItem((String) menuText.get(i));
     menuItem.addActionListener(efs);
     itsMenu.add(menuItem);
   }
 }
 
 public boolean isCustomList() {
   return false;
 }
 
 public boolean isEmbedded()   {
   return false;
 }
 
 public boolean isActionable() {
   return !(action == null || action.length() == 0);
 }
 
 public boolean isActionable(Object datum)     {
   return true;
 }
 
 public boolean isEditable()   {
   return false;
  }
 
  public boolean isEnabled()   {
   return true;
  }
 
  public boolean needsListLookup()     {
    return false;
 }
 
 public void getMenu(JPopupMenu theMenu, ActionListener listener) {
 
   JMenuItem menuItem;
   // Add entries to the popup menu.
   for (int i=0; i < menuAction.size(); i++) {
     menuItem = new JMenuItem((String) menuText.get(i));
     menuItem.addActionListener(listener);
     theMenu.add(menuItem);
   }
 }
 
 public void setData(Object datum, Map contextMap) {
 }
 
 public void setAction(String action) {
   this.action = action;
   String token;
   if (action != null) {
     StringTokenizer it = new StringTokenizer(action, ",");
     while (it.hasMoreTokens()) {
       token = it.nextToken();
       int equalPos = token.indexOf('=');

       if (equalPos < 0) {
         menuAction.add(token);
         menuText.add(token);
       } else if (equalPos == 0) {
         token = token.substring(1);
         menuAction.add(token);
         menuText.add(token);
       } else {
         menuAction.add(token.substring(0,equalPos));
         menuText.add(token.substring(equalPos+1));
       }
     }
   }
 }
 
}

