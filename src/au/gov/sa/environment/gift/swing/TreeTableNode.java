package au.gov.sa.environment.gift.swing;
 
/*
 * MapTreeNode.java
 *
 *
 */
 
import java.util.*;
import javax.swing.tree.*;
 
/**
 * TreeTableNode is a utility class to hold details of a map sheet.<p>
 * <p>
 *
 */
 
public class TreeTableNode extends DefaultMutableTreeNode {
 
  public String nodeName;
  public String nodeCount="0";
  public String nodeDone="No";
 
   /**
     * Creates a TreeTableNode
     */
   public TreeTableNode() {
        nodeName = "";
   }
 
 
   /**
     * Creates a MapTreeNode with a name
     * @param nodeName names the node
     */
   public TreeTableNode(String nodeName) {
        this.nodeName = nodeName;
   }
 
   /**
     * Creates children on the root for the given mapsheet name
     * @param nodeName names the node
     */
   public void addNode(String nodeName) {
       String level1NodeName = nodeName;
       String level2NodeName = "";
       if (nodeName.length() >= 4) {
          level1NodeName = nodeName.substring(0,4);
       }
       if (nodeName.length() > 4) {
          level2NodeName = nodeName.substring(4);
       }
       Enumeration itsChildren = children();
       TreeTableNode thisChild = null;
       int i=0;
       while (itsChildren.hasMoreElements()) {
          thisChild = (TreeTableNode) itsChildren.nextElement();
          int c = thisChild.nodeName.compareTo(level1NodeName);
          if (c == 0) {
              if (level2NodeName.length() > 0) {
                 thisChild.addNode(nodeName, level2NodeName);
              }
              return;
          } else if (c > 0) {
              TreeTableNode newNode = new TreeTableNode(level1NodeName);
              insert(newNode, i);
              if (level2NodeName.length() > 0) {
                 newNode.addNode(nodeName, level2NodeName);
              }
              return;
          }
          i += 1;
       }
       thisChild = new TreeTableNode(level1NodeName);
       add(thisChild);
       if (level2NodeName.length() > 0) {
          thisChild.addNode(nodeName, level2NodeName);
       }
   }
 
   /**
     * Creates the 10k MapTreeNodes for the given mapsheet name
     * @param nodeName names the node
     * @param partName names the node
     */
   public void addNode(String nodeName, String partName) {
       int partLength = 6;
       if (partName.length() == 1) partLength = 7;
       String level1NodeName = nodeName;
       String level2NodeName = "";
       if (nodeName.length() >= partLength) {
          level1NodeName = nodeName.substring(0,partLength);
       }
       if (nodeName.length() > partLength) {
          level2NodeName = nodeName.substring(partLength);
       }
       Enumeration itsChildren = children();
       TreeTableNode thisChild = null;
       int i=0;
       while (itsChildren.hasMoreElements()) {

          thisChild = (TreeTableNode) itsChildren.nextElement();
          int c = thisChild.nodeName.compareTo(level1NodeName);
          if (c == 0) {
              if (level2NodeName.length() > 0) {
                 thisChild.addNode(nodeName, level2NodeName);
              }
              return;
          } else if (c > 0) {
              TreeTableNode newNode = new TreeTableNode(level1NodeName);
              insert(newNode, i);
              if (level2NodeName.length() > 0) {
                 newNode.addNode(nodeName, level2NodeName);
              }
              return;
          }
          i += 1;
       }
       thisChild = new TreeTableNode(level1NodeName);
       add(thisChild);
       if (level2NodeName.length() > 0) {
          thisChild.addNode(nodeName, level2NodeName);
       }
   }
 
   /**
     * Updates the child node for the given mapsheet name
     * @param nodeName names the node
     * @param mapCount new string value
     */
   public void countNode(String nodeName, String mapCount) {
       Enumeration itsChildren = children();
       TreeTableNode thisChild = null;
       while (itsChildren.hasMoreElements()) {
          thisChild = (TreeTableNode) itsChildren.nextElement();
          if (nodeName.startsWith(thisChild.nodeName)) {
             if (nodeName.equals(thisChild.nodeName)) {
                thisChild.nodeCount = mapCount;
             } else {
                try {
                   int i = Integer.parseInt(thisChild.nodeCount);
                   i += Integer.parseInt(mapCount);
                   thisChild.nodeCount = Integer.toString(i);
                } catch (Exception e) {}
                thisChild.countNode(nodeName, mapCount);
             }
             return;
          }
       }
   }
 
   /**
     * Updates the child node for the given mapsheet name
     * @param nodeName names the node
     * @param mapDone new string value for the Written column
     */
   public void writeNode(String nodeName, String mapDone) {
       Enumeration itsChildren = children();
       TreeTableNode thisChild = null;
       while (itsChildren.hasMoreElements()) {
          thisChild = (TreeTableNode) itsChildren.nextElement();
          if (nodeName.startsWith(thisChild.nodeName)) {
             if (nodeName.equals(thisChild.nodeName)) {
                thisChild.nodeDone = mapDone;
             } else {
                thisChild.nodeDone = "";
                thisChild.writeNode(nodeName, mapDone);
             }
             return;
          }
       }
   }
 
   public String toString() {
      return nodeName;
   }
 
}

