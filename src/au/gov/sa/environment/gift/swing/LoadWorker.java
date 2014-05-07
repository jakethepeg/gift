package au.gov.sa.environment.gift.swing;
 
import org.jdesktop.swingworker.*;
 
public class LoadWorker extends SwingWorker {
 
 protected EntryFrameStructure frame;
 protected Object substance;
 
 @Override
 protected Object doInBackground() throws Exception {
   if (frame != null && substance != null) {
     frame.loadTextCore(substance);
   }
   return null;
 }
 
 public LoadWorker(EntryFrameStructure frame, Object substance) {
  this.frame = frame;
  this.substance = substance;
 }
 
 public EntryFrameStructure getFrame() {
   return frame;
 }
 
 public void setFrame(EntryFrameStructure frame) {
   this.frame = frame;
 }
 
 public Object getSubstance() {
   return substance;
 }
 
 public void setSubstance(Object substance) {
   this.substance = substance;
 }
 
}

