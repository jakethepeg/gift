package au.gov.sa.environment.gift.swing;
 
/*
 * TreeTableModelImpl.java
 *
 *
 */
 
/**
 * TreeTableModelImpl is a TreeTableModel for generic implementations.<p>
 *
 */
 
public class TreeTableModelImpl extends AbstractTreeTableModel {
 
   // Names of the columns.
   static protected String[]  cNames = {"Name", "Marks", "Written"};
 
   // Types of the columns.
   static protected Class[]  cTypes = { TreeTableModel.class,
                                         String.class, String.class};
 
 
   /** Returns true if links are to be descended. */
   protected boolean                descendLinks;
 
 
 
   /**
     * Creates a TreeTableModelImpl
     */
   public TreeTableModelImpl() {
        this("Root");
   }
 
   /**
     * Creates a MapTreeModel for <code>mapKey</code>.
     */
   public TreeTableModelImpl(String mapKey) {
        super(null);
        root = new TreeTableNode(mapKey);
   }
 
   //
   // The TreeModel interface
   //
 
   /**
     * Returns the number of children of <code>node</code>.
     */
   public int getChildCount(Object node) {
       TreeTableNode theNode = (TreeTableNode) node;
        return theNode.getChildCount();
   }
 
   /**
     * Returns the child of <code>node</code> at index <code>i</code>.
     */
   public Object getChild(Object node, int i) {
       TreeTableNode theNode = (TreeTableNode) node;
        return theNode.getChildAt(i);
   }
 
 
   //
   //  The TreeTableNode interface.
   //
 
   /**
     * Returns the number of columns.
     */
   public int getColumnCount() {
        return cNames.length;
   }
 
   /**
     * Returns the name for a particular column.
     */
   public String getColumnName(int column) {
        return cNames[column];
   }
 
   /**
     * Returns the class for the particular column.
     */
   public Class getColumnClass(int column) {
        return cTypes[column];
   }
 
   /**
     * Returns the value of the particular column.
     */
   public Object getValueAt(Object node, int column) {
        TreeTableNode it = ((TreeTableNode)node);
 
       switch(column) {
            case 0:
                return it.nodeName;
            case 1:
                return it.nodeCount;

            case 2:
                return it.nodeDone;
        }
 
        return null;
   }
 
   //
   // Some convenience methods.
   //
 
   /**
     * If <code>newValue</code> is true, links are descended. Odd results
     * may happen if you set this while other threads are loading.
     */
   public void updateTree() {
      fireTreeStructureChanged(root, new Object[] {root}, null, null);
   }
 
   /**
     * If <code>newValue</code> is true, links are descended. Odd results
     * may happen if you set this while other threads are loading.
     */
   public void updateTreeData() {
      fireTreeNodesChanged(root, new Object[] {root}, null, null);
   }
 
   /**
     * If <code>newValue</code> is true, links are descended. Odd results
     * may happen if you set this while other threads are loading.
     */
   public void setDescendsLinks(boolean newValue) {
        descendLinks = newValue;
   }
 
   /**
     * Returns true if links are to be automatically descended.
     */
   public boolean getDescendsLinks() {
        return descendLinks;
   }
 
}

