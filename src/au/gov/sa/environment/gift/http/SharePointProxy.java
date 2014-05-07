package au.gov.sa.environment.gift.http;
 
 
import java.util.*;
import java.net.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;

import javax.security.auth.login.CredentialException;
import javax.security.auth.login.CredentialNotFoundException;
import javax.swing.*;

import au.gov.sa.environment.gift.jdbc.GeneralDataSet;
import au.gov.sa.environment.gift.swing.*;
import au.gov.sa.environment.gift.xml.XmlDataProcessor;

import org.apache.http.HttpEntity;
import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.auth.NTLMScheme;
import org.apache.http.impl.auth.RFC2617Scheme;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

/**
 * SharePoint list reader Proxy translates BaseMessage into HTTP GET / PUT
 * and translates HTML response back into a BaseMessage</p>
 * <p >Copyright (c) 2014</p>
 * <p>Company: DEWNR</p>
 * @author Peter Davey
 * @version 1.0
 */
 
public class SharePointProxy  {
 
 private static final boolean DEBUG=false;
 private static final int maxRetry = 5;
 private static final int retrySleep = 250;
 
 private String server = null;
 private String proxyPath = null;
 private String port = null;
 private String userName = null;
 private String passWord = null;
  
 private int serverPort = -1;
 protected JFrame theFrame = ApplicationFrame.getSingleton();
 
 //Construct the frame
 public Properties getProperties(String fileName) {

   InputStream f = null;
   Properties p = null;
   String fName = fileName + ".properties";
   String fPath = "au/gov/sa/environment/gift/http/" + fName;
   try {
     //get connection parameters
     f = this.getClass().getClassLoader().getResourceAsStream(fPath);
   } catch(Exception e) {
     System.out.println("No embedded properties file found " + fPath);
     if (DEBUG) System.out.println(e.getMessage());
     if (DEBUG) e.printStackTrace();
   }
   if (f == null) {
      try {
        f = new FileInputStream(fName);
      } catch(Exception e) {
        System.out.println("No local properties file found " + fName);
      }
   }
   if (f != null)  try {
     //get connection parameters
     p = new Properties();
     p.load(f);
     f.close();
   //  if (userName == null || passWord == null) {
  //     poseDialog();
  //   }
  }
   catch(Exception e) {
     System.out.println("Failed initializing Http access.");
     e.printStackTrace();
   }
   return p;
 }
 
 //Construct the frame
 public SharePointProxy(Object datum, String fileName) {
   if (DEBUG) {
     System.out.println("Initializing Http access for " + fileName);
   }
   InputStream f = null;
   Properties p = GeneralDataSet.loadDbProperties(fileName, null, datum);
   if (p == null) {
     System.out.println("No connection properties found");
     System.exit(0);
   }
   server = p.getProperty("server", "apps");
   port = p.getProperty("port");
   proxyPath = p.getProperty("proxypath", "/saveg/Authorisation");
   userName = System.getProperty("username");
   passWord = System.getProperty("password");
     // Authenticator.setDefault(new SdbAuthenticator(userName, passWord, theFrame));
     // Encode String
     // userPassword = userName + ":" + passWord;
     //  encoding = new sun.misc.BASE64Encoder().encode (userPassword.getBytes());
     if (port == null) {
        System.out.println("URL connection set up to http://" + server + proxyPath);
     } else {
    	 serverPort = Integer.parseInt(port);
        System.out.println("URL connection set up to http://" + server + ":" + port + proxyPath);
     }
   //  if (userName == null || passWord == null) {
  //     poseDialog();
  //   }
 }

 //Construct the frame
 public SharePointProxy(String fileName) {
   if (DEBUG) {
     System.out.println("Initializing Http access for " + fileName);
   }
   InputStream f = null;
   Properties p = getProperties(fileName);
   if (p == null) {
     System.out.println("No connection properties found");
     System.exit(0);
   }
   server = p.getProperty("server", "apps");
   port = p.getProperty("port");
   proxyPath = p.getProperty("proxypath", "/saveg/Authorisation");
   userName = System.getProperty("username");
   passWord = System.getProperty("password");
     // Authenticator.setDefault(new SdbAuthenticator(userName, passWord, theFrame));
     // Encode String
     // userPassword = userName + ":" + passWord;
     //  encoding = new sun.misc.BASE64Encoder().encode (userPassword.getBytes());
     if (port == null) {
        System.out.println("URL connection set up to http://" + server + proxyPath);
     } else {
    	 serverPort = Integer.parseInt(port);
        System.out.println("URL connection set up to http://" + server + ":" + port + proxyPath);
     }
   //  if (userName == null || passWord == null) {
  //     poseDialog();
  //   }
 }
 
 // Construct an URLConnection from the message and call the AuthorisationServlet
 public SPMessage performFunction(SPMessage theMessage) {
//     CredentialsProvider credsProvider = new BasicCredentialsProvider();
//     credsProvider.setCredentials(
  //           new AuthScope("localhost", 443),
 //            new UsernamePasswordCredentials(userName, passWord));
     CloseableHttpClient httpclient = HttpClients.custom().build();
     try {
         HttpGet httpget = new HttpGet(new URL("http", server, serverPort, proxyPath).toString());
     

         System.out.println("executing request" + httpget.getRequestLine());
         CloseableHttpResponse response = httpclient.execute(httpget);
             HttpEntity entity = response.getEntity();

             System.out.println("----------------------------------------");
             System.out.println(response.getStatusLine());
             if (entity != null) {
                 System.out.println("Response content length: " + entity.getContentLength());
             }
             
             InputStream content = entity.getContent();
             XmlDataProcessor xml = new XmlDataProcessor();
             theMessage.setTarget(xml.readXml(theMessage.getTarget(), content));

             EntityUtils.consume(entity);
         } catch (Exception e) {
             System.out.println("SharePointProxy.performFunction got an exception");
              e.printStackTrace();
             System.out.println(e.getMessage());
             e.printStackTrace();
             JOptionPane.showMessageDialog(theFrame, "HTTP GET exception " + e.getMessage()
                                                              + "\n - processing abandoned ",
                          "HTTP Connection Failure", JOptionPane.ERROR_MESSAGE);
             System.exit(0);
         }
   return theMessage;
 }
 
 // Construct an URLConnection from the message and call the AuthorisationServlet
 public SPMessage performFunction(SPMessage theMessage, String parameter) {
//     CredentialsProvider credsProvider = new BasicCredentialsProvider();
//     credsProvider.setCredentials(
  //           new AuthScope("localhost", 443),
 //            new UsernamePasswordCredentials(userName, passWord));
     CloseableHttpClient httpclient = HttpClients.custom().build();
     try {
         HttpGet httpget = new HttpGet(new URL("http", server, serverPort, proxyPath).toString() + "?owner=" + parameter);
     
         System.out.println("executing request" + httpget.getRequestLine());
         CloseableHttpResponse response = httpclient.execute(httpget);
             HttpEntity entity = response.getEntity();

             System.out.println("----------------------------------------");
             System.out.println(response.getStatusLine());
             if (entity != null) {
                 System.out.println("Response content length: " + entity.getContentLength());
             }
             
             InputStream content = entity.getContent();
             XmlDataProcessor xml = new XmlDataProcessor();
             theMessage.setTarget(xml.readXml(theMessage.getTarget(), content));

             EntityUtils.consume(entity);
         } catch (Exception e) {
             System.out.println("SharePointProxy.performFunction got an exception");
              e.printStackTrace();
             System.out.println(e.getMessage());
             e.printStackTrace();
             JOptionPane.showMessageDialog(theFrame, "HTTP GET exception " + e.getMessage()
                                                              + "\n - processing abandoned ",
                          "HTTP Connection Failure", JOptionPane.ERROR_MESSAGE);
             System.exit(0);
         }
   return theMessage;
 }
 
 // Construct an HttpMethod from the message and call the SDBProxyServlet
 public void processLine(SPMessage theMessage, String line) {
   int state = 0;
   int start = 0;
   int end = 0;
   String attribute = null;
   String value = null;
   while (state >= 0) {
     switch (state) {
     case 0: start = line.indexOf('>');
             state = (start > 0) ? 1 : -1;
             break;
     case 1: end = line.indexOf('=', start);
             state = (end > 0) ? 2 : -1;
             break;
     case 2: attribute = line.substring(start+1, end);
             start = end + 1;
             end = line.indexOf('<', start);
             state = (end > 0) ? 3 : -1;
             break;
     case 3: value = line.substring(start, end);
             state = -1;
             break;
     case 4: state = -1; break;
     }
     if ("access".equals(attribute)) theMessage.setAuthority(value);
     if ("database".equals(attribute)) theMessage.setDatabaseString(value);
     if ("version".equals(attribute)) theMessage.setVersion(value);
   }
 }
 
 // Construct an HttpMethod from the message and call the SDBProxyServlet
 public BaseMessage performPostFunction(BaseMessage theMessage) {
   HttpURLConnection sessionClient = null;
   int responseCode = -1;
   try {
     if (port != null) serverPort = Integer.parseInt(port);
     StringBuffer requestBuffer = new StringBuffer();
     requestBuffer.append("?request=").append(theMessage.getMessageType());
     requestBuffer.append("&query=").append(theMessage.getMessageQuery());
 
     URL sdbUrl = new URL("http", server, serverPort, proxyPath + requestBuffer.toString());
     if (DEBUG) System.out.println(sdbUrl.toString());
     sessionClient = (HttpURLConnection) sdbUrl.openConnection();
     sessionClient.setDoInput(true);
     sessionClient.setDoOutput(true);
     sessionClient.setRequestMethod("POST");

 
     // If a bad HTTP response comes back, wait a little and retry
     int retryCount = 0;
     while (responseCode != HttpURLConnection.HTTP_OK) {
        sessionClient.connect();
        responseCode = sessionClient.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
           System.out.println("PUT response code is " + Integer.toString(responseCode));
           retryCount += 1;
           if (retryCount > maxRetry) {
              System.out.println("HTTP PUT response code is " + Integer.toString(responseCode));
              System.out.println("Max retrys exceeded - processing abandoned ");
              JOptionPane.showMessageDialog(theFrame, "HTTP PUT response code is " + Integer.toString(responseCode)
                                                      + "\nMax retrys exceeded - processing abandoned ",
                  "HTTP Connection Failure", JOptionPane.ERROR_MESSAGE);
              System.exit(0);
           }
           Thread.sleep(retrySleep);
        }
     }
 
     InputStream content = sessionClient.getInputStream();
     StringBuffer input = new StringBuffer();
     BufferedReader in = new BufferedReader (new InputStreamReader (content));
     String line;
     while ((line = in.readLine()) != null) {
       input.append(line);
     }
 
     if (DEBUG) System.out.println(input.toString());
 
   } catch (Exception e) {
     System.out.println("EJBProxy.performPostFunction threw an exception");
     e.printStackTrace();
     JOptionPane.showMessageDialog(theFrame, "HTTP PUT exception " + e.getMessage()
                                                      + "\n - processing abandoned ",
                  "HTTP Connection Failure", JOptionPane.ERROR_MESSAGE);
     System.exit(0);
   }
   return theMessage;
 }
 
 public void poseDialog() {
   final TextField userNameEntry = new TextField();
   final TextField passWordEntry = new TextField();
   final JDialog jd = new JDialog (theFrame, "SAVEG - Log in required", true);
   jd.setLocationRelativeTo(null);
   jd.setLayout (new GridLayout (0, 2));
   Label jl = new Label ("User name");
   jd.add (jl);
   userNameEntry.setBackground (Color.lightGray);
   jd.add (userNameEntry);
   Label j2 = new Label ("Password");
   jd.add (j2);
   passWordEntry.setEchoChar ('*');
   passWordEntry.setBackground (Color.lightGray);
   jd.add (passWordEntry);
   Button jb = new Button ("OK");
   jd.add (jb);
   jb.addActionListener (new ActionListener() {
     public void actionPerformed (ActionEvent e) {
       userName = userNameEntry.getText();
       passWord = passWordEntry.getText();
       jd.dispose();
     }
   });
   Button jc = new Button ("Cancel");
   jd.add (jc);
   jc.addActionListener (new ActionListener() {
     public void actionPerformed (ActionEvent e) {
       jd.dispose();
       System.exit(0);
     }
   });
   jd.pack();
   jd.setVisible(true);
 }
 
 public final class SavegCredentials implements CredentialsProvider {
 
   private String username=null;
   private String password=null;
 
   protected SavegCredentials(String username, String password) {
     this.username = username;
     this.password = password;
   }
 
   public Credentials getCredentials(
         final AuthScheme authscheme,
         final String host,
         int port,
         boolean proxy)
         throws CredentialException
     {
         if (authscheme == null) {
             return null;
         }
         if ((this.username == null) || (this.password == null)) {
             return null;

         }
         if (authscheme instanceof NTLMScheme) {
		     return new NTCredentials(username, password, host, "IssRealm");    
		 } else
		 if (authscheme instanceof RFC2617Scheme) {
		     return new UsernamePasswordCredentials(username, password);    
		 } else {
		     throw new CredentialException("Unsupported authentication scheme: " +
		         authscheme.getSchemeName());
		 }
   }

@Override
public void clear() {
	// TODO Auto-generated method stub
	
}

@Override
public Credentials getCredentials(AuthScope arg0) {
	// TODO Auto-generated method stub
	return null;
}

@Override
public void setCredentials(AuthScope arg0, Credentials arg1) {
	// TODO Auto-generated method stub
	
}
 }
}

