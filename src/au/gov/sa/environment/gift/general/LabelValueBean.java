package au.gov.sa.environment.gift.general;
 
/**
 * <p>Title: mprt</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: DEH</p>
 * @author not attributable
 * @version 1.0
 */
 
import java.util.*;
import java.io.*;
 
/**
 * The <code>LabelValueBean</code> class is used mainly from a JSP custom tag
 * to provide a simple mechanism to store the label and value for select options
 *
 * Has static methods to make code:code list of LabelValueBeans from an Arraylist of codes
 * and code:value list from an ArrayList of codes and a HashMap of code:value
 *
 *
 */
public class LabelValueBean implements Serializable {
 
 // ----------------------------------------------------------- Constructors
 
 
 /**
   * Construct a new LabelValueBean with the specified label.
   *
   * @param label The label to be displayed to the user
   */
 public LabelValueBean(String label) {
     this.label = label;
     this.value = null;
 }
 
 /**
   * Construct a new LabelValueBean with the specified values.
   *
   * @param label The label to be displayed to the user
   * @param value The value to be returned to the server
   */
 public LabelValueBean(String label, String value) {
     this.label = label;
     this.value = value;
 }
 
 
 // ------------------------------------------------------------- Properties
 
 
 /**
   * The label to be displayed to the user.
   */
 protected String label = null;
 
 public String getLabel() {
     return (this.label);
 }
 
 
 /**
   * The value to be returned to the server.
   */
 protected String value = null;
 
 public String getValue() {
     return (this.value);
 }
 
 public void setValue(String value) {
     this.value = value;
 }
 
 
 // --------------------------------------------------------- Public Methods
 
 
 /**
   * Return true if the objects differ.
   * @param it the bean against which to check this one
   * @return true if the keys match and the data has changed
   */
 public boolean changed(LabelValueBean it) {
     String fromKey = it.getValue();
     if (fromKey == null) return false;
     if (fromKey.equals(value)) {
        String fromLabel = it.getLabel();
        if (fromLabel == null) {
           if (label == null) return false;
        } else if (fromLabel.equals(label)) return false;
        return true;
     }
     return false;
 }
 
 
 /**

   * Return a string representation of this object.
   * @return string representation of the bean
   */
 public String toString() {
     StringBuffer sb = new StringBuffer("LabelValueBean[");
     sb.append(this.label);
     sb.append(", ");
     sb.append(this.value);
     sb.append("]");
     return (sb.toString());
 }
 
 /** Return a List of LabelValueBeans for an ArrayList of string codes
   *
   * @param valueList list of code values for which to make LabelValueBeans
   * @return List of LabelValueBeans corresponding to the input
   */
 public static List makeLabelValueList(List valueList) {
    List result = new ArrayList();
    for (int i=0; i<valueList.size(); i++) {
        result.add(new LabelValueBean((String) valueList.get(i), (String) valueList.get(i)));
    }
    return result;
 }
 
 /** Return a List of LabelValueBeans for an ArrayList of string codes and its HashMap of long descriptions
   *
   * @param valueList list of code values for which to make LabelValueBeans
   * @param valueSet Map pairing label strings with code values
   * @return List of LabelValueBeans corresponding to the input
   */
 public static List makeLabelValueList(List valueList, Map valueSet) {
    List result = new ArrayList();
    for (int i=0; i<valueList.size(); i++) {
        String value = (String) valueList.get(i);
        result.add(new LabelValueBean((String) valueSet.get(value), value));
    }
    return result;
 }
 
 /** Make Lists of Labels and Values from a List of LabelValueBeans
   *
   * @param lvList list of LabelValueBeans
   * @param labelList List for label strings
   * @param valueList List for value strings
   */
 public static void remakeList(List lvList, List labelList, List valueList) {
   labelList.clear();
   valueList.clear();
   for (int i=0; i<lvList.size(); i++) {
     LabelValueBean it = (LabelValueBean) lvList.get(i);
     labelList.add(it.getLabel());
     valueList.add(it.getValue());
   }
 }
 
}

