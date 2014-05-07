package au.gov.sa.environment.gift.swing;
 
import java.util.*;
import au.gov.sa.environment.gift.general.LabelValueBean;
 
/**
 * Sources combo box contents from an ArrayList of objects.
 * Allows the visible contents to switch between value, label and value: label
 * <p>Copyright: Copyright (c) 2004</p>
 * <p>Company: DEH</p>
 * @author Peter Davey
 * @version 1.0
 */
 
public class ListCompoundBoxModel extends ListComboBoxModel {
 
 public ListCompoundBoxModel(List members) {
    super(members);
 }
 
 public String getLabelFor(int index) {
   String result = null;
    if (index >= 0 && index < members.size()) {
      LabelValueBean bean = (LabelValueBean) members.get(index);
      switch (showCode) {
        case ListComboBoxModel.SHOW_CODE:
          result = bean.getValue();
          break;
        case ListComboBoxModel.SHOW_BOTH:
          String label = bean.getLabel();
          result = bean.getValue();
          if (label != null && !label.equals("null") && !label.equals(result))
            result = result + ": " + label;
          break;
        default: result = bean.getLabel();
      }
    }
    return result;
 }
 
 public String getStringFor(int index) {
    if (index >= 0 && index < members.size()) {
       return ((LabelValueBean) members.get(index)).getValue();
    }
    return null;
 }
 
 public Object getElementAt(int index) {
    return getLabelFor(index);
 }
 
 public void setSelectedItem(Object anObject) {
    if (anObject instanceof String) {
        selectedString = (String) anObject;
    } else {
        selectedString = null;
    }
 }
 
 public void setFreshElement(Object element) {
   addElement(((LabelValueBean) element).getValue());
 }
}

