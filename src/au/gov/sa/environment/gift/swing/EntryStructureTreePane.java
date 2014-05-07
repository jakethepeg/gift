package au.gov.sa.environment.gift.swing;
 
import java.awt.Dimension;
import java.awt.event.*;
import java.lang.reflect.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import java.util.*;
 
import au.gov.sa.environment.gift.jdbc.*;
 
/**
 * EntryStructureTreePane - Hold name, type, size, JComponent for a JTree field
 * Has a slightly incestuous relationship with its partner
 */
public class EntryStructureTreePane extends EntryStructure implements TreeSelectionListener {
 
 private JFrame frame = null;
 private EntryStructure partner = null;
 private Object datum = null;
 
 protected JTree target = null;
 private TreeModel itsModel = null;
 
 private Map contextMap = new HashMap();
 private String className = null;
 private String nameAttribute = null;
 private String propertyName = null;
 private Class actionClass = null;
 
 private Box displayContents = null;
 
 private String keyName = null;
 private String buttons = null;
 private String preferredWidth = "500";
 private String preferredHeight = "120";
 private Dimension tableSize = null;
 private Dimension labelSize = new Dimension(100,12);
 private List labelSizeList = new ArrayList();
 
 private List keyNames = null;
 private List sourceNames = null;
 
 protected JPopupMenu m_popup;
 protected Action m_action;
 protected TreePath m_clickedPath;
 
 public EntryStructureTreePane(String name, String size, String icons) {
   this.name = name;
   this.type = TREEPANE;
   this.size = size;
   int sizeInt = 0;
   target = new JTree(new EntryTreeModel(new DefaultMutableTreeNode()));
   textComponent = target;
   displayComponent = new JScrollPane(target);
   ((JScrollPane) displayComponent).setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
   target.addTreeSelectionListener(this);
 
   if (icons != null) {
     EntryStructureTreeCellRenderer renderer = new EntryStructureTreeCellRenderer(icons);
     target.setCellRenderer(renderer);
   }
 
   m_popup = new JPopupMenu();
   m_action = new AbstractAction() {
     public void actionPerformed(ActionEvent e) {
       if (m_clickedPath == null) return;
       if (target.isExpanded(m_clickedPath))
         target.collapsePath(m_clickedPath);
       else
         target.expandPath(m_clickedPath);
     }
   };
   m_popup.add(m_action);
   m_popup.addSeparator();
 
   Action a1 = new AbstractAction("Delete") {
     public void actionPerformed(ActionEvent e) {
       target.repaint();
       if (m_clickedPath == null) return;
       Object it = m_clickedPath.getLastPathComponent();
       if (it instanceof DefaultMutableTreeNode) {
         DefaultMutableTreeNode itsNode = (DefaultMutableTreeNode) it;
         int childCount = itsNode.getChildCount();
         if (childCount > 0) {
           JOptionPane.showMessageDialog(
               frame, "Delete contents first",
               "Info", JOptionPane.INFORMATION_MESSAGE);
           return;
         } else {
           DefaultMutableTreeNode rootNode = null;
           Object root = target.getModel().getRoot();
           Object deletand = itsNode.getUserObject();
           if (root instanceof DefaultMutableTreeNode) {
             rootNode = (DefaultMutableTreeNode) root;
             root = rootNode.getUserObject();
           }
           if (root instanceof ValidatingObject) {
             if (((ValidatingObject) root).validateRemoval(deletand)) {

               loadEmbeddedData(root, rootNode, sourceNames);
               ((EntryTreeModel) target.getModel()).updateTree();
               if (partner instanceof EntryStructureFramePane) {
                 ((EntryStructureFramePane) partner).loadEmbeddedData(null);
               }
             }
           }
         }
       }
     }
   };
   m_popup.add(a1);
   target.add(m_popup);
   target.addMouseListener(new PopupTrigger());
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
 
   target = (JTree) textComponent;
   itsModel = target.getModel();
   DefaultMutableTreeNode targetRoot = (DefaultMutableTreeNode) itsModel.getRoot();
   loadEmbeddedData(datum, targetRoot, sourceNames);
   TreeCellRenderer renderer = target.getCellRenderer();
   if (renderer instanceof EntryStructureTreeCellRenderer) {
     ((EntryStructureTreeCellRenderer) renderer).setCellIcons(datum);
   }
   if (order != null) {
     StringTokenizer scanOrder = new StringTokenizer(order,",");
     while (scanOrder.hasMoreTokens()) {
       String it = scanOrder.nextToken();
       if ("noTop".equals(it)) {
         target.setRootVisible(false);
       } else if ("first".equals(it)) {
         TreePath top = new TreePath(itsModel.getRoot());
         TreePath next = top.pathByAddingChild(targetRoot.getFirstChild());
         target.expandPath(next);
       }
     }
   }
   if (itsModel instanceof EntryTreeModel) {
     ((EntryTreeModel) itsModel).updateTree();
   }
   target.validate();
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
 
 public void setSource(String source) {
   this.source = source;
   if (source != null) {
     StringTokenizer it = new StringTokenizer(source, ",");
     sourceNames = new ArrayList();
     while (it.hasMoreTokens()) sourceNames.add(it.nextToken());
   }
 }
 
 /**
   * Return an attribute from a data object
   * @param datum the object containing the attribute
   * @param attribute the attribute name
   */
 public Object getContainedData(Object datum, String attribute) throws Exception {
   StringBuffer workName = new StringBuffer("get");
   workName.append(attribute.substring(0,1).toUpperCase()).append(attribute.substring(1));
   Class parentClass = datum.getClass();
   Method getMethod = parentClass.getMethod(workName.toString(), new Class[] {});
   return getMethod.invoke(datum, new Object[] {});
 }
 
/**
  * loadEmbeddedData loads datum and its contents into the jTree
  *
  * @param datum the parent data object
  * @param itsRoot the tree node it is to start from
  * @param sourceNames if we want to be selective about which contained structures get into the tree
  */
 public void loadEmbeddedData(Object datum, DefaultMutableTreeNode itsRoot, List sourceNames) {
   itsRoot.removeAllChildren();
   itsRoot.setUserObject(datum);
   try {
     List contained = general.getContainedNames(datum);
     if (sourceNames != null) contained.retainAll(sourceNames);
     String attribute = null;
     Object contents = null;
     List contentList = null;
     for (int i=0; i < contained.size(); i++) {
       attribute = (String) contained.get(i);
       contents = getContainedData(datum, attribute);
       if (contents instanceof List) {
         Iterator it = ((List) contents).iterator();
         while (it.hasNext()) {
           Object thisDatum = it.next();
           DefaultMutableTreeNode itsChild = new DefaultMutableTreeNode(thisDatum);
           itsRoot.add(itsChild);
           loadEmbeddedData(thisDatum, itsChild, null);
         }
       } else {
         DefaultMutableTreeNode itsChild = new DefaultMutableTreeNode(contents);
         itsRoot.add(itsChild);
         loadEmbeddedData(contents, itsChild, null);
       }
     }
   } catch (Exception e) {
     e.printStackTrace();
   }
 
 }
 
 public void valueChanged(TreeSelectionEvent evt) {
     // Get the node
     TreePath newPath = evt.getPath();
     DefaultMutableTreeNode newNode = (DefaultMutableTreeNode) newPath.getLastPathComponent();
     Object newDatum = newNode.getUserObject();
     if (partner instanceof EntryStructureFramePane) {
       ((EntryStructureFramePane) partner).loadEmbeddedData(newDatum);
     }
 }
 
 class PopupTrigger extends MouseAdapter {
   public void mouseReleased(MouseEvent e) {
     if (e.isPopupTrigger()) {
       int x = e.getX();
       int y = e.getY();
       TreePath path = target.getPathForLocation(x, y);
       if (path != null) {
         if (target.isExpanded(path))
           m_action.putValue(Action.NAME, "Collapse");
         else
           m_action.putValue(Action.NAME, "Expand");
         m_popup.show(target, x, y);
         m_clickedPath = path;
       }
     }
   }
 }
}

