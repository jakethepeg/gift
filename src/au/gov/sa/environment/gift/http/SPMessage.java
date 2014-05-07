package au.gov.sa.environment.gift.http;

import java.util.List;

public class SPMessage {

 private String messageType;
 private String messageQuery;
 private String databaseString;
 private String authority;
 private String version;
 private Object target;
 private List data;
 
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
public List getData() {
	return data;
}
public void setData(List data) {
	this.data = data;
}
public Object getTarget() {
	return target;
}
public void setTarget(Object target) {
	this.target = target;
}
 
}

