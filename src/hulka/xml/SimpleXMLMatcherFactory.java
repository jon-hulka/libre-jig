/**
 * This code module may be freely used and modified without restriction.
 *  - Jonathan Hulka (jon.hulka@gmail.com)
 * 
 * This class provides text and pattern matchers to use in parsing very simple XML documents.
 * 
 */

package hulka.xml;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class SimpleXMLMatcherFactory
{
	/**
	 * Regular expression defining XML name start characters
	 * note: [x10000-xEFFFF] has been excluded, as it is not compatible with Java characters
	 */
	public static final String nameStartCharEx = "[\\p{Alpha}:_\\xC0-\\xD6\\xD8-\\xF6\\xF8-\\u02FF\\u0370-\\u037D\\u037F-\\u1FFF\\u200C-\\u200D\\u2070-\\u218F\\u2C00-\\u2FEF\\u3001-\\uD7FF\\uF900-\\uFDCF\\uFDF0-\\uFFFD]";

	/**
	 * Regular expression defining XML name characters
	 */
	public static final String nameCharEx = "[\\-\\.0-9\\xB7\\u0300-\\u036F\\u203F-\\u2040" + nameStartCharEx + "]";

	/**
	 * Regular expression defining XML names
	 */
	public static final String nameEx = nameStartCharEx + nameCharEx + "*";
	private static final Pattern namePattern = Pattern.compile(nameEx);
	
	
	public static final String openSTag="<";
	public static final String closeSTag=">";
	public static final String closeEmptyElemTag="/>";
	public static final String openETag="</";
	public static final String closeETag=">";
	
	/**
	 * This is not complete, it works for the SimpleXMLReader's purposes
	 */
	public static final String attributeValueEx = "[^<]*";
	private static final Pattern attributeValuePattern = Pattern.compile(attributeValueEx);
	

	/**
	 * Special markup characters to be escaped in character data
	 *  - escaping all of these makes life easier
	 * '&' must always be first to ensure proper encoding
	 */
	public static final char [] escapedChars = {'&'    ,'<'   ,'>'   ,'"'    ,'\''};
	/**
	 * Special markup characters as strings for convenience
	 */
	public static final String [] escapedCharStrings = {"&"    ,"<"   ,">"   ,"\""    ,"'"};
	/**
	 * Escape codes for special markup characters
	 */
	public static final String [] escapeCodes =  {"&amp;","&lt;","&gt;","&quot;","&apos;"};
	
	/**
	 * whitespace
	 */
	public static final char [] whitespaceChars = {' ', '\r', '\n', '\t'};
	
	/**
	 * whitespace as strings for convenience
	 */
	public static final String [] whitespaceStrings = {" ", "\r", "\n", "\t"};
	
	public static final char newLine = '\n';

	/**
	 * Open comment delimiter
	 */
	public static final String openComment = "<!--";

	/**
	 * Close comment delimiter
	 */
	public static final String closeComment = "-->";
	
	public static final String closeCharData = "<";
	
	public static final String eq = "=";
	
	public static final String openHexRef = "&#x";
	public static final String openDecRef = "&#";
	public static final String closeRef = ";";
	
	/**
	 * Never needs to be instantiated
	 */
	private SimpleXMLMatcherFactory(){}
	
	/**
	 * Returns a pattern matcher for XML names
	 * @param name initial input for the matcher
	 * @return a pattern matcher for XML names
	 */
	public static Matcher getNameMatcher(String value)
	{
		return namePattern.matcher(value);
	}
	
	public static Matcher getAttributeValueMatcher(String value)
	{
		return attributeValuePattern.matcher(value);
	}
}
