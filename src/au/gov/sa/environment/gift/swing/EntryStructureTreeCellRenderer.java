package au.gov.sa.environment.gift.swing;
 
import java.awt.*;
import javax.swing.*;
import javax.swing.tree.*;
import java.util.*;
 
/**
 * EntryStructureTreeCellRenderer - manage custom icons for tree pane
 * 
 */
public class EntryStructureTreeCellRenderer extends DefaultTreeCellRenderer {
   static final Map iconCache = new HashMap();
 
   /** The EntryStructureTreeCellRenderer gets the class,icon location
     * list from the <name>Icons entry in the properties file.
     * If the icon files are to be found on this objects path, they
     * are loaded now. Otherwise, each setData() call will try again
     * on the new data objects path
     * @param icons Comma-separated list of className,iconPath...
     */
   public EntryStructureTreeCellRenderer(String icons) {
     StringTokenizer it = new StringTokenizer(icons, ",");
     while (it.hasMoreTokens()) {
       String classKey = it.nextToken();
       if (it.hasMoreTokens()) {
         iconCache.put(classKey, it.nextToken());
       }
     }
     setCellIcons(this);
   }
 
   /** setCellIcons looks for unloaded icon files on this objects path,
     * and loads them if they can be found
     * @param datum Object on whose path icons may be found...
     */
   public void setCellIcons(Object datum) {
     Iterator it = iconCache.keySet().iterator();
     while (it.hasNext()) {
       String itsKey = (String) it.next();
       Object itsValue = iconCache.get(itsKey);
       if (itsValue instanceof String) {
         java.net.URL imgURL = datum.getClass().getResource((String) itsValue);
         if (imgURL != null) {
           iconCache.put(itsKey, new ImageIcon(imgURL));
         }
       }
     }
   }
 
   public Component getTreeCellRendererComponent(
                       JTree tree,
                       Object value,
                       boolean sel,
                       boolean expanded,
                       boolean leaf,
                       int row,
                       boolean hasFocus) {
 
       super.getTreeCellRendererComponent(
                       tree, value, sel,
                       expanded, leaf, row,
                       hasFocus);
       String className = null;
       Object it = null;
       if (value instanceof DefaultMutableTreeNode) {
         it = ((DefaultMutableTreeNode) value).getUserObject();
         if (it != null) {
           className = it.getClass().getName();
           int lastDot = className.lastIndexOf(".");
           if (lastDot >= 0)
             className = className.substring(lastDot + 1);
         }
       }
       if (className != null && iconCache.containsKey(className)) {
           it = iconCache.get(className);
           if (it instanceof Icon) setIcon((Icon) it);
       }
       setToolTipText(null); //no tool tip
 
       return this;
   }
 
}

