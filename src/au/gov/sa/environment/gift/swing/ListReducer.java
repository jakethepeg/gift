package au.gov.sa.environment.gift.swing;
 
import java.util.*;
 
/**
 * List Reducer - interface for lists to call back their object and validate entries
 * Entries
 */
public interface ListReducer {
 
   /**
     * Return true if this entry should be included
     */
   public boolean checkEntry(String structureName, Object entry);
   /**
     * Return a reduced list of values
     */
   public List reduceList(String structureName, List lvList, List labels, List values);
 
}

