/**
 * To do - replace String manipulation with StringBuilder code
 * This code module may be freely used and modified without restriction.
 *  - Jonathan Hulka (jon.hulka@gmail.com)
 * 
 * This class provides a bare bones mechanism for encoding XML data.
 * 
 * The goal of this project is to make something sophisticated enough to encode HTML
 * and configuration files without getting overly complex
 * 
 * Limitations:
 *  - Only names, character data, and comments are supported at this time
 * 
 *To do:
 * Add a function to encode a character as escaped (getEscaped(char))
 */

package hulka.xml;
import java.util.regex.Matcher;

public class SimpleXMLEncoder
{
	private Matcher nameMatcher = null;
	private Matcher commentMatcher = null;
	private String status = null;
	
	/**
	 * Encodes a string of character data by escaping the special characters
	 * @param value the character data to encode
	 * @return the input string with special characters escaped
	 */
	public String encodeCharData(String value)
	{
		String result = value;
		for(int i=0; i < SimpleXMLMatcherFactory.escapedChars.length; i++)
		{
			result = result.replaceAll(SimpleXMLMatcherFactory.escapedCharStrings[i], SimpleXMLMatcherFactory.escapeCodes[i]);
		}
		return result;
	}
	
	/**
	 * Decoses a string of character data by converting escaped characters
	 * @param value the character data to decode
	 * @return the decoded value, or null if an error was encountered
	 */
	public String decodeCharData(String value)
	{
		boolean ok=true;
		String result = value;
		//replace hex references
		int start=result.indexOf(SimpleXMLMatcherFactory.openHexRef);
		while(start>=0&&ok)
		{
			int next = result.indexOf(SimpleXMLMatcherFactory.closeRef,start);
			String ref = result.substring(start,next);
			String strValue = ref.substring(SimpleXMLMatcherFactory.openHexRef.length(), ref.length() - SimpleXMLMatcherFactory.closeRef.length());
			try
			{
				char charValue = (char)Integer.parseInt(strValue,16);
				result = result.replaceAll(ref,Character.toString(charValue));
			}catch(Exception ex)
			{
				result = null;
				ok = false;
				status = ex.getMessage();
			}
			start=result.indexOf(SimpleXMLMatcherFactory.openHexRef);
		}
		//replace decimal references
		if(ok) start=result.indexOf(SimpleXMLMatcherFactory.openDecRef);
		while(start>=0&&ok)
		{
			int next = result.indexOf(SimpleXMLMatcherFactory.closeRef,start);
			String ref = result.substring(start,next);
			String strValue = ref.substring(SimpleXMLMatcherFactory.openDecRef.length(), ref.length() - SimpleXMLMatcherFactory.closeRef.length());
			try
			{
				char charValue = (char)Integer.parseInt(strValue,16);
				result = result.replaceAll(ref,Character.toString(charValue));
			}catch(Exception ex)
			{
				result = null;
				ok = false;
				status = ex.getMessage();
			}
			start=result.indexOf(SimpleXMLMatcherFactory.openDecRef);
		}
		//replace special characters
		for(int i=SimpleXMLMatcherFactory.escapeCodes.length-1; i>=0 && ok; i--)
		{
			result = result.replaceAll(SimpleXMLMatcherFactory.escapeCodes[i],SimpleXMLMatcherFactory.escapedCharStrings[i]);
		}
		return result;
	}
	
	public String getStatus()
	{
		return status;
	}
	
	public String encodeName(String name)
	{
		String result = null;
		status = null;
		if(nameMatcher==null)
		{
			nameMatcher = SimpleXMLMatcherFactory.getNameMatcher(name);
		}
		else nameMatcher.reset(name);

		if(nameMatcher.matches())
		{
			result = name;
		}
		else
		{
			status = "invalid name";
		}
		
		return result;
	}

	/**
	 * Verifies that the input string is valid comment text, and formats it as a comment
	 * @param text comment text
	 * @return on success, the comment text enclosed in comment tags - on failure, null
	 */
/*	public String encodeComment(String text)
	{
		String result = null;
		status = null;
		if(nameMatcher==null)
		{
			commentMatcher = SimpleXMLMatcherFactory.getCommentMatcher(text);
		}
		else commentMatcher.reset(text);

		if(commentMatcher.matches())
		{
			result = SimpleXMLMatcherFactory.openComment + text + SimpleXMLMatcherFactory.closeComment;
		}
		else
		{
			status = "invalid comment text";
		}
		
		return result;
	}*/

}
