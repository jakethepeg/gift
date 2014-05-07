package au.gov.sa.environment.gift.general;
 
 
/**
 * The <code>CodeSetDescriptorBean</code> class drives JSP behaviour for code sets
 * allowing a single JSP to manage tables with different keys and argument lengths
 *
 * @author Peter Davey
 * @version 1.0
 *
 */
public class CodeSetDescriptorBean {
 
 /**
   * The name of the code set.
   */
 protected String label = null;
 
 /**
   * The display name of the code set.
   */
 protected String display = null;
 
 /**
   * The max length for the label.
   */
 protected int labelLength=0;
 
 /**
   * The max length for the value.
   */
 protected int valueLength=0;
 
 // ----------------------------------------------------------- Constructors
 
 /**
   * Construct a new CodeSetDescriptorBean with the specified label.
   *
   * @param label The label to be displayed to the user
   */
 public CodeSetDescriptorBean(String label) {
     this.label = label;
     this.display = label;
     this.labelLength = 4;
     this.valueLength = 30;
 }
 
 /**
   * Construct a new CodeSetDescriptorBean with the specified values.
   *
   * @param label The table name for the code set
   * @param display The descriptive name for the code set
   * @param labelLength maximum code length for input
   * @param valueLength maximum long text length for input
   */
 public CodeSetDescriptorBean(String label, String display, int labelLength, int valueLength) {
     this.label = label;
     this.display = display;
     this.labelLength = labelLength;
     this.valueLength = valueLength;
 }
 
 // ------------------------------------------------------------- Properties
 
 
 /**
   * The label to be displayed to the user.
   * @return table name for the code set
   */
 
 public String getLabel() {
     return (this.label);
 }
 
 /**
   * The label to be displayed to the user.
   * @return descriptive name for the code set table
   */
 
 public String getDisplay() {
     return (this.display);
 }
 
 
 /**
   * The value to be returned to the server.
   * @return integer max code length
   */
 
 public int getLabelLength() {
     return this.labelLength;
 }
 
 /**
   * The value to be returned to the server.
   * @return integer max long name length
   */
 
 public int getValueLength() {
     return this.valueLength;

 }
 
}

