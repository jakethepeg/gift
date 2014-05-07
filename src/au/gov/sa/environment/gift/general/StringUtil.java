package au.gov.sa.environment.gift.general;
import java.text.*;
import java.util.*;
 
/**
 * Title: String and Date Utilities for Struts web tier
 * Description: Routines to format data for CSV file
 * Copyright: Copyright (c) 2002
 * Company: DEH
 * @author Peter Davey (Intec)
 * @version 1.0
 */
 
public final class StringUtil
{
 static final SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy");
 static final SimpleDateFormat dfTime = new SimpleDateFormat("H-m-s dd/MM/yyyy");
 static final SimpleDateFormat dfDateTime = new SimpleDateFormat("h:mm a EEE, dd MMMMMMMMM, yyyy");
 static final SimpleDateFormat dfBeanDate = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");
 static final SimpleDateFormat dfStringDate = new SimpleDateFormat("dd MMMMMMMMM yyyy");
 static final SimpleDateFormat dfMonthDate = new SimpleDateFormat("MMMMMMMMM yyyy");
 static final SimpleDateFormat dfKeyDate = new SimpleDateFormat("yyyyMM");
 static final SimpleDateFormat dfDash = new SimpleDateFormat("dd/MM/yyyy");
 static final SimpleDateFormat dfShort = new SimpleDateFormat("dd/MM/yy");
 static final SimpleDateFormat dfNoDash = new SimpleDateFormat("ddMMyyyy");
 static final SimpleDateFormat dfBasic = new SimpleDateFormat("MMM dd yyyy");
 static final SimpleDateFormat dfJdbc = new SimpleDateFormat("yyyy-MM-dd");
 static final SimpleDateFormat dfJdbcTs = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
 static final DecimalFormat decf = new DecimalFormat("#,###,##0.00");
 static final DecimalFormat decfInt = new DecimalFormat("#,###,##0");
 static final FieldPosition fPos = new FieldPosition(DateFormat.DATE_FIELD);
 static java.util.Date earliestDate=null;
 static java.util.Date latestDate=null;
 static final DecimalFormat locationFormat = new DecimalFormat("0.000");
 
 /**
   * Returns true if string is null or of zero length.
   *
   * @param str String to be examined.
   *
   * @return <code>true</code> if string is <code>null</code>
   * or of zero length, <code>false</code> otherwise.
   */
 public static boolean isNullEmpty(String str) {
   return (str == null || str.length() == 0);
 }
 
 /**
   * Returns true if string contains only letters or digits, or is null.
   *
   * @param str String to be examined.
   *
   * @return <code>true</code> if string contains only letters or digits,
   * or is <code>null</code>, <code>false</code> otherwise.
   */
 public static boolean isAlphaNumeric(String str)
 {
   if (str == null) return true;
 
   for (int cnt=0; cnt<str.length(); cnt++)
     if (!Character.isLetterOrDigit(str.charAt(cnt)))
       return false;
 
   return true;
 }
 
 /**
   * Returns specified string with whitespace removed from both ends.
   * Unlike java.lang.String.trim(), returns <code>null</code> if
   * string is <code>null</code>.
   *
   * @param str String to be parsed.
   *
   * @return String with whitespace removed from both ends, else <code>null</code>.
   */
 public static String trim(String str)
 {
   // Check for null string
   if (str == null) return null;
 
   // Strip leading whitespace characters
   int head;
   for (head=0; head < str.length(); head++)
     if (!Character.isWhitespace(str.charAt(head))) break;
 
   // Strip trailing whitespace characters
   int tail;
   for (tail=str.length(); head<tail; tail--)
     if (!Character.isWhitespace(str.charAt(tail-1))) break;
 
   return (head==tail ? null : str.substring(head, tail));
 }
 
 /**
   * Returns specified string rendered JavaScript-safe. Escapes
   * single quote, newline and return characters.
   *
   * @param str String to be parsed.
   *
   * @return JavaScript-safe string.

   */
 public static String toJSString(String str)
 {
   StringBuffer buf = new StringBuffer(str);
 
   // Escape single quote, newline and return characters
   for (int cnt=buf.length()-1; cnt>=0; cnt--) {
     char chr = buf.charAt(cnt);
     if (chr == '\'' || chr == '\n' || chr == '\r')
              buf.insert(cnt, '\\');
   }
 
   return buf.toString();
 }
 
 /**
   * Returns specified string rendered TexPress-safe. Escapes
   * single quote, double quote, newline and return characters.
   *
   * @param str String to be parsed.
   *
   * @return TexPress-safe string.
   */
 public static String toTexQLString(String str)
 {
   StringBuffer buf = new StringBuffer(str);
 
   for (int cnt=buf.length()-1; cnt>=0; cnt--) {
     char chr = buf.charAt(cnt);
     if (chr == '\'' || chr == '\"' || chr == '\n' || chr == '\r')
              buf.insert(cnt, '\\');
   }
 
   return buf.toString();
 }
 
 /**
   * Returns specified string rendered CSV-safe. Rules are:
   * <ul>
   * <li>Return empty string if string is <code>null</code>.
   * <li>Double quote double quotes (eg. " -> "").
   * <li>Pre/post-fix double quotes if string contains NL, LF or comma chars.
   * </ul>
   *
   * @param str String to be parsed.
   *
   * @return CSV-safe string.
   */
 public static String toCSV(String str)
 {
   // Check for null string
   if (str == null) return "";
 
   // If string contains no NL, LF, or comma characters
   if (str.indexOf('\n')<0 && str.indexOf('\r')<0 && str.indexOf(',')<0)
     return str;
 
   // ...else...
   StringBuffer buf = new StringBuffer(str.trim());
 
   // Double quote double quotes eg. " -> ""
   for (int cnt=buf.length()-1; cnt>=0; cnt--) {
     char chr = buf.charAt(cnt);
     if (chr == '"')
       buf.insert(cnt, chr);
   }
 
   // Double quote string buffer
   buf.insert(0, "\"").append("\"");
 
   return buf.toString();
 }
 
   /**
     * Returns specified java.util.Date rendered in CSV-form.
     *
     * @param str Date to be parsed.
     *
     * @return CSV-safe string.
     */
   public static String toCSV(java.util.Date str)
   {
     // Check for null string
     if (str == null) return "";
 
     StringBuffer buf = new StringBuffer();
     df.format(str, buf, fPos);
 
     return buf.toString();
   }
 
     /**
       * Returns "true" or "false" string for the given boolean.
       *
       * @param flag Boolean to be represented
       *
       * @return CSV-safe string.
       */
     public static String toCSV(boolean flag) {
       // Check for null string

       if (flag) return "yes";
 
       return "no";
     }
 
 /**
   * Parses the given string into a java.util.Date
   *
   * @param str Date to be parsed.
   *
   * @return the Date if valid, or null
   */
 public static java.util.Date toDate(String str)
 {
    try {
       if (earliestDate == null) earliestDate = dfNoDash.parse("01011800");
       if (latestDate == null) latestDate = dfNoDash.parse("31122999");
    } catch (Exception e) {
       System.out.println("Date initialisation error" + e.getMessage());
       e.printStackTrace();
       return null;
    }
   // Check for null string
   if ((str == null) || (str.length() == 0)) return null;
   java.util.Date result = null;
   try {
      if (str.indexOf("/") > 0) {
        result = dfDash.parse(str);
      } else if (str.indexOf("-") > 0) {
        result = df.parse(str);
      } else if (str.indexOf(" ") > 0) {
        result = dfBasic.parse(str);
      } else {
        result = dfNoDash.parse(str);
      }
   } catch (ParseException e) {
   }
   if (result == null) {
     // If that didn't work, try checking for short years
     try {
        if (str.indexOf("/") > 0) {
          result = dfShort.parse(str);
        }
     } catch (ParseException e) {
     }
   }
   if (result != null) {
      if (result.before(earliestDate)) {
        result = null;
      } else if (result.after(latestDate)) {
        result = null;
      }
   }
   return result;
 }
 
 /**
   * Parses the given string into a java.util.Date,
   * and then into a JDBC-form date string (yyyy-MM-dd)
   *
   * @param str Date to be parsed.
   *
   * @return JDBC-form date string.
   */
 public static String toJdbcDate(String str)
 {
   // Check the incoming date string
   java.util.Date theDate = toDate(str);
   if (theDate == null) return null;
   return dfJdbc.format(theDate);
 }
 
 /**
   * Returns the given date as a JDBC-form date string (yyyy-MM-dd)
   *
   * @param theDate Date to be parsed.
   *
   * @return JDBC-form date string.
   */
 public static String toJdbcDate(java.util.Date theDate)
 {
   if (theDate == null) return null;
   return dfJdbc.format(theDate);
 }
 
   /**
     * Parses the given string into a java.util.Date,
     * and then into a display-form date string (dd-MMM-yyyy)
     *
     * @param str Date to be parsed.
     *
     * @return display-form date string.
     */
   public static String toDisplayDate(String str)
   {
     // Check the incoming date string
     java.util.Date theDate = null;
     try {
       theDate = dfBeanDate.parse(str);
     } catch (Exception e) {}

     if (theDate == null) return null;
     return df.format(theDate);
   }
 
     /**
       * Parses the given java.util.Date
       * into a JDBC-form timestamp string (yyyy-MM-dd hh24:mm:ss.f)
       *
       * @param theDate Date to be parsed.
       *
       * @return JDBC-form date string.
       */
     public static String toJdbcTimestamp(java.util.Date theDate)
     {
       // Check the incoming date string
       if (theDate == null) return null;
       return dfJdbcTs.format(theDate);
     }
 
     /**
       * Uses the given string as a format code
       * and returns the current system date formatted appropriately
       *
       * @param theCode Format into which to parse the current date.
       *
       * @return Properly formatted current date.
       */
     public static String toFormattedDate(String theCode)
     {
       // Check the incoming format string
       try {
         if (theCode != null) {
           SimpleDateFormat it = new SimpleDateFormat(theCode);
           return it.format(new Date());
         }
       }  catch (Exception e) {
       }
       return null;
     }
 
     /**
       * Uses the given string as a format code
       * and returns the current system date formatted appropriately
       *
       * @param theCode Format into which to parse the current date.
       *
       * @return Properly formatted current date.
       */
     public static String toApproximateDate(String theDate)
     {
       // The incoming string may be a real date or a year-month only
       StringBuffer result = new StringBuffer();
       try {
         if (theDate != null) {
           java.util.Date realDate = toDate(theDate);
           if (realDate == null) {
              realDate = toDate(theDate + "01");
              if (realDate != null) {
                result.append("for ").append(dfMonthDate.format(realDate).toUpperCase());
              } else {
                result.append("around ").append(dfStringDate.format(realDate).toUpperCase());
              }
           }
         }
       }  catch (Exception e) {
       }
       return result.toString();
     }
 
     /**
       * Return a Date which represents the yyyy-mm-dd part of rawDate
       *
       * @param rawDate The date, possibly with time
       *
       * @return boolean truth value
       */
     public static Date cleanDate(Date rawDate) {
       Date result = null;
       try {
                String clean = toJdbcDate(rawDate);
                if (clean != null) result = dfJdbc.parse(clean);
            } catch (Exception e) { }
       return result;
     }
     
     /**
       * Compares the date portion of two dates that may be Timestamps
       *
       * @param firstDate Return true if this date is earlier
       * @param secondDate than this one
       *
       * @return boolean truth value
       */
     public static boolean dateBefore(Date firstDate, Date secondDate) {
       Date firstClean = cleanDate(firstDate);
       Date secondClean = cleanDate(secondDate);
       if (firstClean == null || secondClean == null) return false;
       return firstClean.before(secondClean);
     }
     

     /**
       * Compares the date portion of two dates that may be Timestamps
       *
       * @param firstDate Return true if this date is equal
       * @param secondDate to this one
       *
       * @return boolean true if dates are equal
       */
     public static boolean dateEqual(Date firstDate, Date secondDate) {
       Date firstClean = cleanDate(firstDate);
       Date secondClean = cleanDate(secondDate);
       if (firstClean == null || secondClean == null) return false;
       return firstClean.equals(secondClean);
     }
 
     /**
       * Returns a string representation of the first integer parameter
       * padded with leading spaces to the width of the second integer parameter
       *
       * @param theValue Argument to be formatted.
       * @param theWidth Argument providing format width.
       *
       * @return Properly formatted integer.
       */
     public static String toFormattedInt(int theValue, int theWidth)
     {
       StringBuffer result = new StringBuffer();
       result.append(theValue);
       int howWide = Integer.toString(theWidth).length();
       while (howWide > result.length()) {
           result.insert(0, '0');
       }
       return result.toString();
     }
 
 /**
   * Returns the given date as a date - time string
   *
   * @param dateTime Date to be parsed.
   *
   * @return string.
   */
 public static String toDateTimeString(java.util.Date dateTime)
 {
   if (dateTime == null) return "";
   return dfDateTime.format(dateTime);
 }
 
 /**
   * Returns the given date as a date string
   *
   * @param date Date to be parsed.
   *
   * @return string.
   */
 public static String toDateString(java.util.Date date)
 {
   if (date == null) return "";
   return df.format(date);
 }
 
 /**
   * Returns the given date as a yyyyMM string
   *
   * @param date Date to be parsed.
   *
   * @return string.
   */
 public static String toKeyString(java.util.Date date)
 {
   if (date == null) return "";
   return dfKeyDate.format(date);
 }
 
 protected static String dollarValue(int dollars)
 {
   if(dollars == Integer.MIN_VALUE)
     return "";
   else
     return "$" + dollars;
 }
 
 public static String valueString(int dollars, int valueClass,
                                      String classDecoded,
                                      String classLabel)
 {
   if(dollars == Integer.MIN_VALUE) {
     return "";
   } else {
     if(valueClass == Integer.MIN_VALUE || valueClass == 0)
       return dollarValue(dollars);
     else
       return dollarValue(dollars) + " (" + classLabel + ") " + classDecoded;
   }
 }
 
 /**
  * Returns a string space-extended on the left to the indicated length
   *
   * @param source String to extend.

   * @param toLen Desired length
   *
   * @return extended string.
   */
 public static String padStringLeft(String source, int toLen) {
    StringBuffer result = new StringBuffer();
    if (source != null) {
       result.append(source);
    }
    while (result.length() < toLen) result.insert(0, ' ');
    return result.toString();
 }
 
 /**
   * Returns a float formatted to 0.000 and space-extended on the left
   * to the indicated length
   *
   * @param source float to format
   * @param toLen desired length
   *
   * @return extended string
   */
  public static String padStringLeft(float source, int toLen) {
     StringBuffer result = new StringBuffer();
     result.append(locationFormat.format(source));
     while (result.length() < toLen) result.insert(0, ' ');
     return result.toString();
  }
 
 /**
   * Returns a string space-extended on the right to the indicated length
   *
   * @param source String to extend.
   * @param toLen Desired length
   *
   * @return extended string.
   */
 public static String padStringRight(String source, int toLen) {
    StringBuffer result = new StringBuffer();
    if (source != null) {
       result.append(source);
    }
    while (result.length() < toLen) result.append(' ');
    return result.toString();
 }
 
 /**
   * Returns a substring or an empty string if the input contains no data for the substring
   *
   * @param source String to check
   * @param fromPos Desired startposition
   * @param toPos Desired length
   *
   * @return the required substring.
   */
 public static String safeSubString(String source, int fromPos, int toPos) {
   int inputLen = source.length();
   String result = "";
   if (inputLen > fromPos) {
     if (toPos == -1 || inputLen < toPos) toPos = inputLen;
     result = source.substring(fromPos,toPos);
   }
    return result;
 }
 
 /**
   * Takes an input string that may contain an index relative to 1
   * Returns a zero-relative int or -1 if the input is not a number
   *
   * @param source String to check
   *
   * @return the result.
   */
 public static int safeToInt(String source) {
   if (source == null) return -1;
   int inputLen = source.length();
   int result = 0;
   for (int i=0; i<inputLen; i++) {
      char it = source.charAt(i);
      if (it == '.' && result > 0) return result;
      if (it < '0') return -1;
      if (it > '9') return -1;
      result = result * 10 + (it - '0');
   }
   return result - 1;
 }
 
 /**
   * Takes an input string that may contain an integer string
   * Returns the integer or -1 if the input is not a number
   *
   * @param source String to check
   * @return the result.
   */
 public static int safeToNum(String source) {
   if (source == null) return -1;
   int inputLen = source.length();
   int result = 0;
   for (int i=0; i<inputLen; i++) {
      char it = source.charAt(i);

      if (it < '0') return -1;
      if (it > '9') return -1;
      result = result * 10 + (it - '0');
   }
   return result;
 }
 
 /**
   * Takes a pair of input strings that may contain integers
   * Returns the sum of the two
   *
   * @param source1 First string to check and add
   * @param source2 Second string to check and add
   * @return the result.
   */
 public static int safeAdd(String source1, String source2) {
   return safeToNum(source1) + safeToNum(source2);
 }
 
 /**
   * Takes an input string that may contain an double string
   * Returns the integer or -1 if the input is not a number
   *
   * @param source String to check
   * @return the result.
   */
 public static double safeToDouble(String source) {
   if (source == null) return -1;
   double result = 0.0;
   StringBuffer buf = new StringBuffer();
   for(int i=0;i<source.length();i++){
     if(source.charAt(i)!=',')
     buf.append(source.charAt(i));
   }
   source = buf.toString();
   try{
     result = Double.parseDouble(source);
   }catch(NumberFormatException e){
      result = -1;
   }
   return result;
 }
 
 
 /**
   * Converts a float to a decimal number string
   *
   * @param source Float to convert
   *
   * @return extended string.
   */
 public static String convertFloat(float source) {
    return decf.format(source);
 }
 
 /**
   * Converts a float to a decimal number string
   *
   * @param source Float to convert
   *
   * @return extended string.
   */
 public static String convertFloatWhole(float source) {
    return decfInt.format(Math.abs(source + 0.5));
 }
 /**
   * Converts a double to a decimal number string
   *
   * @param source Double to convert
   *
   * @return extended string.
   */
 public static String convertDouble(double source) {
    return decf.format(source);
 }
 /**
   * Converts a double to a decimal number string.
   * If there is no decimal part, the number appears like an integer.
   *
   * @param source Double to convert
   *
   * @return extended string.
   */
 public static String convertSimpleDouble(double source) {
   String result = decf.format(source);
   if (result.endsWith(".00")) return result.substring(0,result.length() - 3);
    return result;
 }
}

