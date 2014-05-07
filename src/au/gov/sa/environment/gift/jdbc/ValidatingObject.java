package au.gov.sa.environment.gift.jdbc;
 
import java.util.*;
 
/** 
 * ValidatingObject - interface method called when a Validate button is pressed
 */
public interface ValidatingObject {
 
        public List doValidation() ;
 
 /*
   * Remove an object from this data structure.
   * @param datum an object to remove from the structure
   * @return boolean true if the datum has been removed
   */
        public boolean validateRemoval(Object datum) ;
        
}

