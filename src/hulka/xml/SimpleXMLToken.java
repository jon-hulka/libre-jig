/**
 * This code module may be freely used and modified without restriction.
 *  - Jonathan Hulka (jon.hulka@gmail.com)
 * 
 * This class describes some basic XML tokens.
 * Not included:
 * - Processing Instructions
 * - XML Declaration
 * - Document type declaration
 * - References
 */

package hulka.xml;

public class SimpleXMLToken
{
	/**
	 * End of document
	 */
	public static final int TYPE_DOCUMENT_END = -1;
	
	/**
	 * 
	 */
	public static final int TYPE_NONE = -2;

	/**
	 * Used to indicate that an error was encountered.
	 * The value string will contain a more detailed description.
	 */
	public static final int TYPE_ERROR = 0;

	/**
	 * Element start tag or empty element - value contains element name
	 */
	public static final int TYPE_ELEMENT_START=9;
	/**
	 * Element end tag or empty element - there should always be a TYPE_ELEMENT_END token for every TYPE_ELEMENT_START token
	 */
	public static final int TYPE_ELEMENT_END=10;
	/**
	 * Character data - decoded
	 */
	public static final int TYPE_CHARACTER_DATA=11;

	/**
	 * Element attribute name - should always be followed by a TYPE_ATTRIBUTE_VALUE token
	 */
	public static final int TYPE_ATTRIBUTE_NAME=12;
	/**
	 * Element attribute value - decoded
	 */
	public static final int TYPE_ATTRIBUTE_VALUE=13;
	
	/**
	 * Token type, one of the values defined by the TYPE_? constants
	 */
	public int type;
	
	/**
	 * Token value, contains a name or value depending on the token type
	 */
	public String value;
	
	public String toString()
	{
		String result = null;
		switch(type)
		{
			case TYPE_DOCUMENT_END:
				result = "DOCUMENT_END ";
				break;
			case TYPE_ERROR:
				result = "ERROR ";
				break;
			case TYPE_NONE:
				result = "NONE ";
				break;
			case TYPE_ELEMENT_START:
				result = "ELEMENT_START ";
				break;
			case TYPE_ELEMENT_END:
				result = "ELEMENT_END ";
				break;
			case TYPE_ATTRIBUTE_NAME:
				result = "ATTRIBUTE_NAME ";
				break;
			case TYPE_ATTRIBUTE_VALUE:
				result = "ATTRIBUTE_VALUE ";
				break;
			case TYPE_CHARACTER_DATA:
				result = "CHARACTER DATA ";
				break;
		}
		return result + value;
	}
}
