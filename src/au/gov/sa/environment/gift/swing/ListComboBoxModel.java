package au.gov.sa.environment.gift.swing;
 
import javax.swing.DefaultComboBoxModel;
import java.util.*;
import au.gov.sa.environment.gift.general.*;
 
/**
 * <p>Title: ListComboBoxModel</p>
 * <p>Description: Sources combo box contents from an ArrayList of objects</p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: DEH</p>
 * @author Peter Davey
 * @version 1.0
 */
 
public class ListComboBoxModel extends DefaultComboBoxModel {
 
 public static final int SHOW_CODE = 0;
 public static final int SHOW_LABEL = 1;
 public static final int SHOW_BOTH = 2;
 
 protected int showCode = SHOW_LABEL;
 protected List members = new ArrayList();
 protected String selectedString=null;
 
 public ListComboBoxModel(List members) {
    this.members = members;
 }
 
 public int getShowCode() {
    return showCode;
 }
 
 public void setShowCode(int showCode) {
    this.showCode = showCode;
 }
 
 public int getSize() {
    return members.size();
 }
 
 public String getStringFor(int index) {
    if (index >= 0 && index < members.size()) {
      Object it = members.get(index);
      if (it instanceof String) {
        return (String) it;
      } else if (it instanceof LabelValueBean) {
        return ((LabelValueBean) it).getLabel();
      }
    }
    return null;
 }
 
 public Object getElementAt(int index) {
    return getStringFor(index);
 }
 
 public int getIndexOf(Object anObject) {
    if (anObject instanceof String) {
        String it = (String) anObject;
        for (int i=0; i<members.size(); i++) {
            if (it.equals(getStringFor(i))) return i;
        }
    }
    return -1;
 }
 
 public Object getSelectedItem() {
    return selectedString;
 }
 
 public void setSelectedItem(Object anObject) {
     if (anObject instanceof String) {
        selectedString = (String) anObject;
   } else if (anObject instanceof LabelValueBean) {
     ((LabelValueBean) anObject).getLabel();
   } else selectedString = null;
 }
 
 public void setFreshElement(Object element) {
   addElement(element);
 }
 
 public void setFreshList(List newList) {
   setFreshList(newList, true);
 }
 
 public List getMembers() {
   return members;
 }
 
 public void setMembers(List members) {
     this.members = members;
 }
 
 public void fireChanged() {
     fireContentsChanged(this, 0, members.size()-1);
 }
 
 public void setFreshList(List newList, boolean update) {

    removeAllElements();
    members = newList;
    for (int i=0; i<members.size(); i++) {
       setFreshElement(members.get(i));
    }
    selectedString = null;
    if (update) fireChanged();
 }
}

