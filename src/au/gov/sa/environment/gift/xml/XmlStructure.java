package au.gov.sa.environment.gift.xml;
 
import java.util.*;
 
/**
 * XML Structure - container for XML parsing attributes
 * Entries
 */
public class XmlStructure
{
 private Map tags = new HashMap();
 private Map attributes = new HashMap();
 private Map data = new HashMap();
 private Object currentObject;
 
 public Map getTags() {
   return tags;
 }
 
 public Map getAttributes() {
   return attributes;
 }
 
 public Map getData() {
   return data;
 }
 
 public List getData(String forName) {
   if (!data.containsKey(forName)) data.put(forName, new ArrayList());
   return (List) data.get(forName);
 }
 
 public Object getCurrentObject()      {
         return currentObject;
 }
 
 public void setTags(Map tags) {
         this.tags = tags;
 }
 
 public void setAttributes(Map attributes)     {
         this.attributes = attributes;
 }
 
 public void resetData() {
   this.data = new HashMap();
 }
 
 public void setData(List data, String forName) {
   this.data.put(forName, data);
 }
 
 public void setCurrentObject(Object currentObject) {
         this.currentObject = currentObject;
 }
 
 public String findTagnameForType(String type) {
   XmlAttribute result = XmlAttribute.findType(tags, type);
   if (result != null) return result.getTagName();
   return null;
 }
 
 public XmlAttribute findAttributeForTagname(String tagname) {
   XmlAttribute result = null;
   if (tags.containsKey(tagname)) result = (XmlAttribute) tags.get(tagname);
   return result;
 }
}

