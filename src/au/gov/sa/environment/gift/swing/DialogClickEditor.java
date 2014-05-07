package au.gov.sa.environment.gift.swing;
 
import java.util.*;
 
import java.awt.event.*;
import java.lang.reflect.*;
import javax.swing.*;
 
import org.apache.commons.beanutils.BeanUtils;
 
import au.gov.sa.environment.gift.jdbc.*;
 
/**
 * <p>Title: Mouse click editor to bring up a dialog</p>
 * <p>Description: J2EE management</p>
 * <p >Copyright: Copyright (c) 2003</p>
 * <p>Company: DEH</p>
 * @author Peter Davey
 * @version 1.0
 */
 
public class DialogClickEditor implements MouseListener {
 
   // editorCache is a HashMap of <className>:DialogHandler
   protected static final Map editorCache = new HashMap();
 
   String itsClassName;
   EntryStructure itsStructure;
   Object datum;   // The object inhabiting the area that we're editing
   Map contextMap;
   JFrame parentFrame;
   EntryFrameStructure efs;
 
   List sourceAttributes = new ArrayList();
   List localAttributes = new ArrayList();
   Map beanAttributes = null;
   Map values = null;
   Object value = null;
 
   Object itsEditor = null;
   DialogHandler itsDH = null;
   JDialog dialog;
   Class itsClass;
 
   public DialogClickEditor(EntryStructure es, JFrame parentFrame, Object datum, Map contextMap, EntryFrameStructure efs) {
     this.datum = datum;
     this.contextMap = contextMap;
     this.parentFrame = parentFrame;
     this.efs = efs;
     itsStructure = es;
     StringTokenizer scanIt = new StringTokenizer(es.getAction(), ",");
     if (scanIt.hasMoreTokens()) {
       itsClassName = scanIt.nextToken();
       while (scanIt.hasMoreTokens()) {
         stashAttribute(scanIt.nextToken());
       }
     }
  }
 
   private void stashAttribute(String source) {
     int equalPos = source.indexOf('=');
     if (equalPos < 0) {
       sourceAttributes.add(source);
       localAttributes.add(source);
     } else if (equalPos == 0) {
       sourceAttributes.add(source.substring(1));
       localAttributes.add(source.substring(1));
     } else {
       sourceAttributes.add(source.substring(0, equalPos));
       localAttributes.add(source.substring(equalPos+1));
     }
 }
 
   public void postCreate() {
     try {
       StringBuffer buildName = new StringBuffer();
       String resourceName = datum.getClass().getName();
       int lastIndex = resourceName.lastIndexOf(".");
       buildName.append(resourceName.substring(0, lastIndex+1)).append(itsClassName);
       resourceName = buildName.toString();
       // Throw ClassNotFound if the class to create doesn't exist
       itsClass = Class.forName(resourceName);
       // If it isn't cached, make the dialog handler for this object
       if (editorCache.containsKey(resourceName)) {
           itsDH = (DialogHandler) editorCache.get(resourceName);
       } else {
         // Throw NoSuchMethodException if it doesn't have a conforming constructor
         Class[] parameterTypes = new Class[] {JFrame.class, Object.class, GeneralDataProcessor.class};
         Constructor createEditor = itsClass.getConstructor(parameterTypes);
         Object[] parameters = new Object[] {parentFrame, datum, itsStructure.getGeneral()};
         itsEditor = createEditor.newInstance(parameters);
         if (itsEditor instanceof DialogHandler) {
           itsDH = (DialogHandler) itsEditor;
           editorCache.put(resourceName, itsDH);
         }
       }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

 
   public void setDatum(Object datum) {
     this.datum = datum;
     postCreate();
  }
 
 private Map createValues() {
   if (itsDH == null) return null;
   values = new HashMap();
   String valueName = null;
   String localName = null;
   String contextName = null;
   String attValue = null;
   if (datum == null) return values;
   try {
     beanAttributes = BeanUtils.describe(datum);
     int dotPos = 0;
     for (int i=0; i < sourceAttributes.size(); i++) {
       valueName = (String) sourceAttributes.get(i);
       localName = (String) localAttributes.get(i);
       dotPos = valueName.lastIndexOf('.');
       if (dotPos < 0) {
         if (beanAttributes.containsKey(valueName)) {
           attValue = (String) beanAttributes.get(valueName);
         } else {
           attValue = (String) value;
         }
       } else {
         contextName = valueName.substring(0, dotPos);
         valueName = valueName.substring(dotPos + 1);
         if (contextMap != null && contextMap.containsKey(contextName)) {
           Object contextDatum = contextMap.get(contextName);
           attValue = BeanUtils.getProperty(contextDatum, valueName);
         } else {
           attValue = "";
         }
       }
       values.put(localName, attValue);
     }
   } catch (Exception e) {
     e.printStackTrace();
   }
   return values;
 }
 
 // Copy data into the editor object.
 private void getDialogData() {
   if (itsDH != null) {
     values = createValues();
     if (values != null) itsDH.setup(values);
     dialog = itsDH.createDialog(parentFrame);
     dialog.pack();
   }
 }
 
   public void mouseEntered(MouseEvent e) {
  }
   public void mouseExited(MouseEvent e) {
  }
   public void mousePressed(MouseEvent e) {
  }
   public void mouseReleased(MouseEvent e) {
  }
 
   public void mouseClicked(MouseEvent e) {
//     if (e.getButton() == MouseEvent.BUTTON1) { Java 1.3.1 omission
       //The user has clicked the cell, so
       //bring up the dialog.
       getDialogData();
       dialog.setVisible(true);
       try {
         if (itsDH != null) {
           if (itsDH.checkEditState()) {
             if (beanAttributes == null) beanAttributes = BeanUtils.describe(datum);
             values = itsDH.returnValues();
             String valueName = null;
             String localName = null;
             String attValue = null;
             int dotPos = 0;
             for (int i=0; i < sourceAttributes.size(); i++) {
               valueName = (String) sourceAttributes.get(i);
               localName = (String) localAttributes.get(i);
               dotPos = valueName.lastIndexOf('.');
               if (dotPos < 0) {
                 if (beanAttributes.containsKey(valueName)) {
                   if (values.containsKey(localName)) {
                     attValue = (String) values.get(localName);
                     BeanUtils.setProperty(datum, valueName, attValue);
                   }
                 }
               }
             }
           }
           efs.loadData(datum);
         }
       } catch (Exception ex) {
         ex.printStackTrace();
       }
//      } Java 1.3.1 omission
   }

}
 

