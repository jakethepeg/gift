package au.gov.sa.environment.gift.swing;
 
import javax.swing.*;
import java.util.*;
 
import au.gov.sa.environment.gift.jdbc.*;
 
/**
 * EntryStructureTab - Hold the structure for a page of a tabbed pane
 * Builds the EntryFrameStructure for its object and makes its Swing structure
 */
public class EntryStructureTab {
 
 private JFrame frame = null;
 private EntryFrameStructure childStructure = null;
 private Object datum = null;
 private Box displayContents = null;
 
 
 public EntryStructureTab(Object datum, JFrame frame, GeneralDataProcessor overseer, Map contextMap) {
   this.datum = datum;
   this.frame = frame;
   childStructure = new EntryFrameStructure(datum, frame, overseer);
   if (contextMap != null) childStructure.setContextMap(contextMap);
   childStructure.postCreate();
   displayContents = Box.createVerticalBox();
   childStructure.buildStructure(displayContents);
 }
 public JFrame getFrame() {
   return frame;
 }
 public EntryFrameStructure getChildStructure() {
   return childStructure;
 }
 public Object getDatum() {
   return datum;
 }
 public Box getDisplayContents() {
   return displayContents;
 }
 public void setFrame(JFrame frame) {
   this.frame = frame;
 }
 public void setChildStructure(EntryFrameStructure childStructure) {
   this.childStructure = childStructure;
 }
 public void setDatum(Object datum) {
   this.datum = datum;
 }
 public void setChangedData() {
   if (datum != null && childStructure != null) {
     childStructure.loadData(datum);
   }
 }
 public void setDisplayContents(Box displayContents) {
   this.displayContents = displayContents;
 }
}

