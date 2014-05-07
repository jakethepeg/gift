package au.gov.sa.environment.gift.jdbc;
 
import java.util.*;
import java.io.*;
 
/**
 * <p>Title: Generic filename filter</p>
 * <p>Description: Used to find property files</p>
 * <p >Copyright: Copyright (c) 2003</p>
 * <p>Company: DEH</p>
 * @author Peter Davey
 * @version 1.0
 */
 
public class GeneralFileFilter extends javax.swing.filechooser.FileFilter {
 
 protected String matchTest = null;
 protected String startTest = null;
 protected String endTest = null;
 protected List testValues = new ArrayList();
 protected String description = "Flexible matching filter";
 
 /** Constructor parameter provides match keys for file name
   *  in a comma-separated stream. Special purpose formats:
   *  &lt;string Starts with string
   *  =string Name is equal to string
   *  string&gt; Ends with string
   *  @param keys comma-separated list of test values
   */
 public GeneralFileFilter(String keys) {
   StringTokenizer keyList = new StringTokenizer(keys, ",");
   while (keyList.hasMoreTokens()) {
     String it = keyList.nextToken();
     if (it.startsWith("<")) {
       startTest = it.substring(1);
     } else if (it.startsWith("=")) {
       matchTest = it.substring(1);
     } else if (it.endsWith(">")) {
       endTest = it.substring(0,it.length() - 1);
     } else {
       testValues.add(it);
     }
   }
 }
 
 public boolean accept(File f) {
        if (f.isDirectory()) return true;
        String name = f.getName();
   if (matchTest != null) {
     return matchTest.equals(name);
   }
   if (startTest != null) {
     if (!name.startsWith(startTest)) return false;
   }
   if (endTest != null) {
     if (!name.endsWith(endTest)) return false;
   }
   for (int i=0; i < testValues.size(); i++) {
     String testValue = (String) testValues.get(i);
     if (name.indexOf(testValue) < 0) return false;
   }
   return true;
 }
 
 public String getDescription () {
          return description;
 }
 public void setDescription(String description) {
          this.description = description;
 }
}

