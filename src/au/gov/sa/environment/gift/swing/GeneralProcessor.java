package au.gov.sa.environment.gift.swing;
 
import java.util.*;
import javax.swing.*;
import au.gov.sa.environment.gift.jdbc.*;
 
/**
 * General purpose driver for Swing applications
 * @author Peter Davey 
 * @version 1.0
 */
 
public class GeneralProcessor {
 
 protected static boolean doOutput = false;
 protected static String configName = null;
 protected static String appName = null;
 public static GeneralDataProcessor overseer = null;
 
 // Static class only
 private GeneralProcessor() {
 }
 
 // Make a new simple window
 protected static void doEntryFrame(Object datum) {
     EntryFrame entryFrame = new EntryFrame(datum, overseer);
     entryFrame.postCreate(datum);
     entryFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
     entryFrame.pack();
     entryFrame.setVisible(true);
 }
 
 // Make a new application window
 protected static void doApplicationFrame(Object datum) {
     ApplicationFrame appFrame = new ApplicationFrame(datum, overseer);
     if (datum instanceof AppHandler) {
       ((AppHandler) datum).register(appFrame);
     }
     appFrame.postCreate();
     appFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
     appFrame.pack();
     appFrame.setVisible(true);
 }
 
 /**
   * Takes an input string that may contain a number
   * Returns the integer value of the string or -1 if the input is not a number
   *
   * @param source String to check
   *
   * @return int the result.
   */
 public static int safeToInt(String source) {
   if (source == null) return -1;
   int inputLen = source.length();
   int result = 0;
   for (int i=0; i<inputLen; i++) {
      char it = source.charAt(i);
      if (it < '0') return -1;
      if (it > '9') return -1;
      result = result * 10 + (it - '0');
   }
   return result;
 }
 
 
 /** Main method
   *   command line parameter names the target class:
   */
 
   public static void main(String[] args) {
     Properties p = System.getProperties();
 
     if (args.length > 0) {
       configName = args[0];
       if (args.length > 1) appName = args[1];
     } else {
       System.out.println("Usage: java ... swing.GeneralProcessor <targetClass> [app]");
       System.out.println("  where <targetClass> names the bean / property set for the initial screen");
       System.out.println("eg: java ... swing.GeneralProcessor au.gov.sa.environment.saveg.gift.data.Menu");
       System.exit(0);
     }
     try {
       UIManager.setLookAndFeel(
         "com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
       overseer = new GeneralDataProcessor();
       String configHere = configName;
       Class configClass = Class.forName(configName);
       Object datum = configClass.newInstance();
       if (appName == null) {
          doEntryFrame(datum);
       } else {
         doApplicationFrame(datum);
       }
     } catch (Exception e) {
       e.printStackTrace();
     }
   }
}

