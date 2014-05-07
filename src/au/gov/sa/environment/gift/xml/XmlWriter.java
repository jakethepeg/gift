package au.gov.sa.environment.gift.xml;
 
import java.io.IOException;
import java.io.Writer;
 
import java.util.*;
import org.apache.commons.lang3.*;
 
/**
 * Support classes for layout of XML text files
 */
public class XmlWriter  {
 
   private Writer writer;      // underlying writer
   private Stack stack;        // of xml entity names
   private StringBuffer attrs; // current attribute string
   private boolean empty;      // is the current node empty
   private boolean closed;     // is the current node closed...
 
   private boolean wroteText; // was text the last thing output?
   private String indent;     // output this to indent one level when pretty printing
   private String newline;    // output this to end a line when pretty printing
 
   private int indentSize;
 
   private String namespace;   // the current default namespace
 
   /**
     * Create an XmlWriter on top of an existing java.io.Writer.
     */
   public XmlWriter(Writer writer) {
       this.writer = writer;
       this.closed = true;
       this.stack = new Stack();
       this.wroteText = false;
       this.newline = "\n";
       this.indent = "  ";
   }
 
   /**
     * The default namespace. Once this is set, any new entities
     * will have this namespace, regardless of scope.
     *
     * @param String name of the namespace
     */
   public void setDefaultNamespace(String namespace) {
       this.namespace = namespace;
   }
 
   public String getDefaultNamespace() {
       if(this.namespace == null) {
           return "";
       } else {
           return this.namespace;
       }
   }
 
   /**
     * Output the version, encoding and standalone nature of an xml file.
     */
   public void writeXmlVersion(String version, String encoding, String standalone) throws IOException {
       this.writer.write("<?xml version=\"");
       this.writer.write(version);
       if(encoding != null) {
           this.writer.write("\" encoding=\"");
           this.writer.write(encoding);
       }
       if(standalone != null) {
           this.writer.write("\" standalone=\"");
           this.writer.write(standalone);
       }
       this.writer.write("\"?>");
       this.writer.write(newline);
   }
 
   /**
     * Begin to write out an entity. Unlike the helper tags, this tag
     * will need to be ended with the endEntity method.
     *
     * @param name String name of tag
     */
   public void writeEntity(String name) throws IOException {
       if (! this.closed || this.wroteText) {
           writeText(newline);
       }
       for (int i = 0; i < indentSize; i++) {
           writeText(indent);
       }
 
       String openName = (this.namespace == null) ? name : this.namespace+":"+name;
       openEntity(openName);
       this.closed = false;
       indentSize++;
       this.empty = true;
       this.wroteText = false;
   }
 
   /**
     * A helper method. It writes out an entity which contains only text.
     *

     * @param name String name of tag
     * @param text String of text to go inside the tag
     */
   public void writeEntityWithText(String name, Object text) throws IOException {
       writeEntity(name);
       writeText(text);
       endEntity();
   }
   /**
     * A helper method. It writes out empty entities.
     *
     * @param name String name of tag
     */
   public void writeEmptyEntity(String name) throws IOException {
       writeEntity(name);
       endEntity();
   }
 
   /**
     * Preamble for a tag.
     *
     * @param String name of entity.
     */
   private void openEntity(String name) throws IOException {
       boolean wasClosed = this.closed;
       closeOpeningTag();
       this.closed = false;
       this.writer.write("<");
       this.writer.write(name);
       stack.add(name);
       this.empty = true;
   }
 
   // close off the opening tag
   private void closeOpeningTag() throws IOException {
       if (!this.closed) {
           writeAttributes();
           this.closed = true;
           this.writer.write(">");
       }
   }
 
   // write out all current attributes
   private void writeAttributes() throws IOException {
       if (this.attrs != null) {
           this.writer.write(this.attrs.toString());
           this.attrs.setLength(0);
           this.empty = false;
       }
   }
 
   /**
     * Append an attribute to the current entity.
     * Any xml characters in the value are escaped.
     *
     * @param String name of attribute.
     * @param Object value of attribute.
     */
   public void writeAttribute(String attr, Object value) throws IOException {
 
       // maintain api
       if (false) throw new IOException();
 
       if (this.attrs == null) {
           this.attrs = new StringBuffer();
       }
       this.attrs.append(" ");
       this.attrs.append(attr);
       this.attrs.append("=\"");
       this.attrs.append(StringEscapeUtils.escapeXml(""+value));
       this.attrs.append("\"");
   }
 
   /**
     * End the current entity. This will throw an exception
     * if it is called when there is not a currently open
     * entity.
     */
   public void endEntity() throws IOException {
       if(this.stack.empty()) {
           throw new IOException("Called endEntity too many times. ");
       }
       String name = (String)this.stack.pop();
       if (name != null) {
         if (this.empty) {
           writeAttributes();
           this.writer.write("/>");
           indentSize--;
         } else {
           indentSize--;
           if (!this.wroteText) {
             for (int i = 0; i < this.indentSize; i++) {
                 this.writer.write(indent); // Indent closing tag to proper level
             }
           }
           this.writer.write("</");
           this.writer.write(name);
           this.writer.write(">");
         }
         this.empty = false;

         this.closed = true;
       }
       writeText(newline);
       this.wroteText = false;
   }
 
   /**
     * Close this writer. Throw an exception if there are 
     * unclosed tags.
     */
   public void close() throws IOException {
       while (!this.stack.empty()) {
         endEntity();
       }
       if (!this.stack.empty()) {
           throw new IOException("Tags are not all closed. "+
               "Possibly, "+this.stack.pop()+" is unclosed. ");
       }
   }
 
   /**
     * Output body text. Any xml characters are escaped. 
     */
   public void writeText(Object text) throws IOException {
       
       closeOpeningTag();
       this.empty = false;
       this.writer.write(StringEscapeUtils.escapeXml(""+text));
       this.wroteText = true;
   }
 
   /**
     * Write out a chunk of CDATA. This helper method surrounds the 
     * passed in data with the CDATA tag.
     *
     * @param String of CDATA text.
     */
   public void writeCData(String cdata) throws IOException {
       
       writeChunk("<![CDATA[ "+cdata+" ]]>");
   }
 
   /**
     * Write out a chunk of comment. This helper method surrounds the 
     * passed in data with the xml comment tag.
     *
     * @param String of text to comment.
     */
   public void writeComment(String comment) throws IOException {
       
       writeChunk("<!--"+comment+"-->");
   }
   private void writeChunk(String data) throws IOException {
       closeOpeningTag();
       this.empty = false;
       this.writer.write(data);
   }
 
   public Writer getWriter() {
       return this.writer;
   }
 
}

