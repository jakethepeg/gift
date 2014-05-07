package au.gov.sa.environment.gift.swing;
 
/*
 * EntryTreeModel.java
 *
 *
 */
 
import javax.swing.tree.*;
 
/**
 * EntryTreeModel is a TreeTableModel with an update method.<p>
 *
 */
 
public class EntryTreeModel extends DefaultTreeModel {
 
 /**
   * Creates a MapTreeModel
   */
 public EntryTreeModel() {
   super(null);
 }
 
 public EntryTreeModel(TreeNode root) {
   super(root);
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
 
}

