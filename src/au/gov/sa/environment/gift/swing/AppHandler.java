package au.gov.sa.environment.gift.swing;
 
import au.gov.sa.environment.gift.jdbc.*;
 
/**
 * Callback Handler - data beans can implement this to handle their own commands
 * Entries
 */
public interface AppHandler {
 
 public void register(ApplicationFrame frame);
 
 // Set up initial conditions for application's starting frame
 public void loadApp(EntryFrameStructure frame, GeneralDataProcessor overseer);
 
 public Object targetDatum();
 
 public Object changeTarget(Object target);
 
 public String requiresAction();
 
}

