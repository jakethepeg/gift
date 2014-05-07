package au.gov.sa.environment.gift.http;
 
public class BaseMessage {
 private String messageType;
 private String messageQuery;
 private String databaseString;
 private String authority;
 private String version;
 
 public String getMessageQuery() {
   return messageQuery;
 }
 public void setMessageQuery(String messageQuery) {
   this.messageQuery = messageQuery;
 }
 public String getMessageType() {
   return messageType;
 }
 public void setMessageType(String messageType) {
   this.messageType = messageType;
 }
 public String getAuthority() {
   return authority;
 }
 public void setAuthority(String authority) {
   this.authority = authority;
 }
 public String getDatabaseString() {
   return databaseString;
 }
 public void setDatabaseString(String databaseString) {
   this.databaseString = databaseString;
 }
 public String getVersion() {
   return version;
 }
 public void setVersion(String version) {
   this.version = version;
 }
 
}

