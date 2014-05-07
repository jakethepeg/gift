package au.gov.sa.environment.gift.xml;
 
import java.util.*;
 
/**
 * XML Structure - container for XML parsing attributes
 * Entries
 */
public class XmlAttribute
{
 private String type; // head,
 private String dataName;
 private String tagName;
 private String className;
 private String convertName;
 private XmlStructure childStructure;
 
 public XmlAttribute() {}
 
 public XmlAttribute(String type, String tagName) {
   this.type = type;
   this.tagName = tagName;
 }
 
        public String getType() {
                return type;
        }
 
        public String getDataName() {
                return dataName;
        }
 
        public String getTagName() {
                return tagName;
        }
 
        public String getClassName() {
                return className;
        }
 
        public String getConvertName() {
                return convertName;
        }
 
        public XmlStructure getChildStructure() {
                return childStructure;
        }
 
        public void setType(String type) {
                this.type = type;
        }
 
        public void setDataName(String dataName) {
                this.dataName = dataName;
        }
 
        public void setTagName(String tagName) {
                this.tagName = tagName;
        }
 
        public void setClassName(String className) {
                this.className = className;
        }
 
        public void setConvertName(String convertName) {
                this.convertName = convertName;
        }
 
        public void setChildStructure(XmlStructure childStructure) {
                this.childStructure = childStructure;
        }
 
 static public XmlAttribute findType(Map search, String type) {
   Iterator it = search.keySet().iterator();
   try {
     while (it.hasNext()) {
       Object nextObject = search.get(it.next());
       if (nextObject instanceof XmlAttribute) {
         XmlAttribute itsEntry = (XmlAttribute) nextObject;
         if (itsEntry.getType().equals(type))
           return itsEntry;
       }
       else {
         String className = nextObject.getClass().getName();
         String realName = Class.forName("XmlAttribute").getName();
       }
     }
   } catch (Exception e) {
     e.printStackTrace();
   }
   return null;
 }
}

