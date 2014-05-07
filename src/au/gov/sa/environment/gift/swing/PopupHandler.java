package au.gov.sa.environment.gift.swing;
 
import java.util.*;
 
/**
 * Popup Handler - data beans can implement this to handle their own context-sensitive menus
 * Entries
 */
public interface PopupHandler {
 
 public List getSet(EntryFrameStructure frame, String setName);
 
}

