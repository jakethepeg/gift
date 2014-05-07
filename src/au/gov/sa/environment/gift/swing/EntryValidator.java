package au.gov.sa.environment.gift.swing;
 
/** 
 * Entry Validator - indicator for cover description within stratum
 * Entries
 */
public interface EntryValidator {
        
        public void setEnabled(boolean enabled);
        public boolean validateChanges(EntryFrameStructure structure, EntryStructure element);
        public boolean validateChanges(EntryFrameStructure structure, EntryStructure element, String text);
 
}

