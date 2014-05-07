package au.gov.sa.environment.gift.swing;
 
import au.gov.sa.environment.gift.jdbc.*;
 
/**
 * Callback Handler - data beans can implement this to handle their own commands
 * Entries
 */
public interface CallbackHandler {
 
 public boolean callback(EntryFrameStructure frame, EntryStructure element, GeneralDataProcessor overseer);
 
}

