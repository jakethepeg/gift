package au.gov.sa.environment.gift.swing;
 
import java.awt.Dimension;
import javax.swing.*;
import java.util.*;
 
/**
 * EntryStructureFramePane - Hold a set of frames picked by the object sent to it
 * Entries
 */
public class EntryStructureFramePane extends EntryStructure {
 
 private JFrame frame = null;
 private EntryStructure partner = null;
 private Object datum = null;
 
 private EntryFrameStructure childStructure = null;
 private JPanel target = null;
 
 private Map contextMap = new HashMap();
 
 private String className = null;
 private String propertyName = null;
 private String keyName = null;
 private Dimension labelSize = new Dimension(100,12);
 private List labelSizeList = new ArrayList();
 
 private List keyNames = null;
 
 
 public EntryStructureFramePane(String name, String size) {
   this.name = name;
   this.type = FRAMEPANE;
   this.size = size;
   int sizeInt = 0;
   target = new JPanel();
   target.setLayout(new BoxLayout(target, BoxLayout.Y_AXIS));
   textComponent = target;
   displayComponent = textComponent;
 
 }
 
 /**
   * Catch up on any subsidiary object creation
   */
 public void postCreate(EntryFrameStructure esf) {
 }
 
 public boolean isCustomList() {
  return false;
 }
 
 public boolean isEmbedded()   {
   return true;
 }
 
 public boolean isActionable() {
   return true;
 }
 
 public boolean isActionable(Object datum)     {
   return true;
 }
 
 public boolean isEditable()   {
   return false;
 }
 
 public boolean isEnabled()    {
   return true;
 }
 
 public boolean needsListLookup()      {
   return false;
 }
 
 public void setPartner(EntryStructure partner) {
   this.partner = partner;
 }
 
 public EntryStructure getPartner() {
   return partner;
 }
 
 public void setData(Object datum, Map contextMap) {
   if (this.contextMap != null) {
     if (contextName != null)
       contextMap.put(contextName, datum);
     this.contextMap.putAll(contextMap);
   }
   this.datum = datum;
 
   loadEmbeddedData(datum);
 }
 
 public void setAction(String action) {
   this.action = action;
   if (action != null) {
     StringTokenizer it = new StringTokenizer(action, ",");
     if (it.hasMoreTokens()) {

       className = it.nextToken();
       if (it.hasMoreTokens()) propertyName = it.nextToken();
     }
   }
 }
 
/**
  * loadEmbeddedData loads datum and its contents into the frame
  * The FramePane simply refills itself with the entry frame for its
  * object
  *
  * @param datum the data object whose ..Entry.properties define the frame
  */
 public void loadEmbeddedData(Object datum) {
 
   ApplicationFrame appFrame = ApplicationFrame.getSingleton();
   if (datum == null) {
     childStructure = null;
     target.removeAll();
   } else {
     AppFrameStructure afs = AppFrameStructure.getSingleton();
     EntryFrameStructure newStructure = afs.getObjectFrame(datum);
 
     if (newStructure != null) {
       if (newStructure == childStructure) {
         childStructure.loadData(datum);
       } else {
         childStructure = newStructure;
         target.removeAll();
         Object display = afs.getFrameContent(childStructure, target);
         childStructure.loadData(datum);
         if (target instanceof JComponent) {
           ((JComponent) target).setBorder(BorderFactory.createCompoundBorder(
             BorderFactory.createTitledBorder(childStructure.getFormTitle()),
             BorderFactory.createEmptyBorder(3, 3, 3, 3)));
         }
       }
     }
     appFrame.pack();
     appFrame.paint(appFrame.getGraphics());
   }
 
 }
}

