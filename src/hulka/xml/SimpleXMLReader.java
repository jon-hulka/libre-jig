/**
 * 
 *   Copyright (C) 2010  Jonathan Hulka (jon.hulka@gmail.com)
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * Changlelog:
 * 2012 02 06 - Jon
 *  - Fixed a bug that caused delimiters to be truncated and missed between buffer reads.
 */
package hulka.xml;
import java.io.InputStreamReader;
import java.util.Stack;
import java.util.regex.Matcher;

/**
 * To do change tokenValue to a StringBuilder for efficiency
 * To do add handling for XML declaration and Document type declaration - pass them on unprocessed
 * 
 * This class provides a bare bones mechanism for reading very simple XML files.
 * The purpose is to provide a light framework suitable for configuration files,
 * save files, etc.
 * 
 * Not parsed:
 * - Processing Instructions
 * - XML Declaration
 * - Document type declaration
 * - Entity references
 * - PE references
 * - CData sections
 * (any of these will cause an error)
 */

public class SimpleXMLReader
{
	private static String [] attributeNameDelimiters={SimpleXMLMatcherFactory.whitespaceStrings[0],SimpleXMLMatcherFactory.eq};
	private static String [] charDataDelimiters={SimpleXMLMatcherFactory.openETag};
	private static String [] elementStartNameDelimiters={SimpleXMLMatcherFactory.whitespaceStrings[0],SimpleXMLMatcherFactory.closeSTag,SimpleXMLMatcherFactory.closeEmptyElemTag};
	private static String [] elementEndNameDelimiters={SimpleXMLMatcherFactory.whitespaceStrings[0],SimpleXMLMatcherFactory.closeETag};
//	private static String [] whitespaceDelimiters={SimpleXMLMatcherFactory.whitespaceStrings[0]};
	private static final int STATE_ERROR=0;
	private static final int STATE_PROLOG=1;

	/**
	 * Empty element tag or element start tag opened, and element name processed
	 */
	private static final int STATE_ELEMENT_START=2;
	/**
	 * Reading element contents - start tag closed
	 */
	private static final int STATE_ELEMENT_CONTENT=3;
	/**
	 * Reading element attribute - just read name, ready for value
	 */
	private static final int STATE_ATTRIBUTE=4;
	/**
	 * Document finished
	 */
	private static final int STATE_DOC_CLOSE=-1;
	
	private SimpleXMLEncoder encoder;

	private Matcher nameMatcher, attributeValueMatcher;

	private Stack <String> elements;
	
	/**
	 * Contains part of the state information as defined by the STATE_? constants
	 * tokenType contains the remaining state information (defined by SimpleXMLToken.TYPE_? constants)
	 */
	private int state;
	/**
	 * Stores the current token type
	 */
	private int tokenType;
	/**
	 * Stores the current token value or error message
	 */
	private String tokenValue;
	
	private InputStreamReader input;
	private boolean eof;
	private StringBuilder buffer;
	/**
	 * Default number of characters to read into the buffer at a time
	 */
	private static final int bufferIncrement=30;
	
	private int lineNumber;
	
	/**
	 * Creates a new SimpleXMLReader with an InputStream for input
	 * @param in the InputStream to read from
	 */
	public SimpleXMLReader(InputStreamReader in)
	{
		encoder = new SimpleXMLEncoder();
		elements = new Stack<String>();
		input = in;
		state = STATE_PROLOG;
		tokenValue = null;
		tokenType = SimpleXMLToken.TYPE_NONE;
		buffer=new StringBuilder(bufferIncrement);
		lineNumber=1;
	}
	
	/**
	 * Parses and returns one token
	 * Tokens will not be parsed after an error is encountered
	 * If parsing is attempted past the end of the document, an error token will be generated - so checking for SimpleXMLToken.TYPE_ERROR should always prevent infinite loops
	 * @param token to store the return value in - if null a new one will be created
	 * @return the token parsed
	 */
	public SimpleXMLToken parseNext(SimpleXMLToken token)
	{
		if(token==null) token=new SimpleXMLToken();
		switch(state)
		{
			case STATE_ERROR:
				break;
			case STATE_DOC_CLOSE:
				if(tokenType==SimpleXMLToken.TYPE_DOCUMENT_END)
				{
					tokenType=SimpleXMLToken.TYPE_ERROR;
				}
				else
				{
					tokenValue="End of document";
					tokenType=SimpleXMLToken.TYPE_DOCUMENT_END;
				}
				break;
			case STATE_PROLOG:
				if(!parseElement()) setError("Unable to find root element");
				break;
			case STATE_ATTRIBUTE:
				if(!parseAttributeValue() && state!=STATE_ERROR) setError("Expected: attribute value");
				break;
			case STATE_ELEMENT_START:
				if(!closeSTag())
				{
					if(state!=STATE_ERROR && !parseAttributeName())
					{
						if(state!=STATE_ERROR) setError("Expected: "+SimpleXMLMatcherFactory.closeSTag+" or "+SimpleXMLMatcherFactory.closeEmptyElemTag+" or attribute at element tag: "+getElementName());
					}
					break;
				}//else System.out.println("STag closed");
			case STATE_ELEMENT_CONTENT:
				//Clear out comments
				while(parseComment() && state!=STATE_ERROR);
				if(state!=STATE_ERROR && !parseElementEnd())
				{
					if(state!=STATE_ERROR && !parseElement())
					{
						if(state!=STATE_ERROR && !parseCharData())
						{
							if(state!=STATE_ERROR)
							{
								if(eof)
								{
									setError("Unexpected end of document");
								}else setError("Unrecognized token");
							}//else System.out.println("error");
						}//else System.out.println("Char data parsed or error");
					}//else System.out.println("Element parsed or error");
				}//else System.out.println("ETag parsed or error");
				break;
		}
		token.type = tokenType;
		token.value = tokenValue;
		return token;
	}
	
	/**
	 * Attempt to close an element start tag
	 * This function does not generate any token information, it only modifies state information
	 * @return true if the start tag was successfully closed, false otherwise - if false, state should be checked to see if an error was encountered.
	 */
	private boolean closeSTag()
	{
		boolean result=false;
		if(expect(SimpleXMLMatcherFactory.closeSTag,true))
		{
			state=STATE_ELEMENT_CONTENT;
			result = true;
		}
		return result;
	}
	
	/**
	 * Attempts to close and empty element tag
	 * This function generates token end tag information
	 * @return true if the element was successfully ended, false otherwise - if false, state should be checked to see if an error was encountered.
	 */
	private boolean closeEmptyElemTag()
	{
		boolean result=false;
		if(expect(SimpleXMLMatcherFactory.closeEmptyElemTag,true))
		{
			tokenValue=elements.peek();
			result=endElement();
		}
		return result;
	}
	
	/**
	 * Attempts to parse a comment
	 * No tokens are generated
	 * @return true if a comment was successfully parsed, false otherwise - if false, state should be checked to see if an error was encountered.
	 */
	private boolean parseComment()
	{
		return readDelimited(SimpleXMLMatcherFactory.openComment,SimpleXMLMatcherFactory.closeComment);
	}
	
	/**
	 * Attempts to parse an element start tag or empty element tag
	 * Generates element start tag information. The tag's closing delimiter will not be parsed.
	 * @return true if and element start tag or empty element tag was successfully parsed, false otherwise - if false, state should be checked to see if an error was encountered.
	 */
	private boolean parseElement()
	{
		boolean result=false;
		if(expect(SimpleXMLMatcherFactory.openSTag,true))
		{
			if(readToken(elementStartNameDelimiters,false))
			{
				if(matchName(tokenValue))
				{
					elements.push(tokenValue);
					result=true;
					tokenType=SimpleXMLToken.TYPE_ELEMENT_START;
					state=STATE_ELEMENT_START;
				}
				else if(state != STATE_ERROR) setError("Invalid element name");
			}
			else if(state != STATE_ERROR) setError("Expected: element name");
		}
		return result;
	}
	
	/**
	 * Attempts to parse an attribute name
	 * Generates attribute name token information
	 * @return true if an attribute name was successfully parsed, false otherwise - if false, state should be checked to see if an error was encountered.
	 */
	private boolean parseAttributeName()
	{
		boolean result=false;
		if(readToken(attributeNameDelimiters,false))
		{
			if(matchName(tokenValue))
			{
				result = true;
				state=STATE_ATTRIBUTE;
				tokenType=SimpleXMLToken.TYPE_ATTRIBUTE_NAME;
			}
			else if(state!=STATE_ERROR) setError("Invalid attribute name");
		}
		return result;
	}
	
	/**
	 * Attempts to parse an attribute value
	 * Generates attribute value token information
	 * @return true if an attribute value was successfully parsed, false otherwise - if false, state should be checked to see if an error was encountered.
	 */
	private boolean parseAttributeValue()
	{
		boolean result=false;
		if(readQuotedText())
		{
			if(matchAttributeValue(tokenValue))
			{
				tokenValue=encoder.decodeCharData(tokenValue);
				if(tokenValue==null)
				{
					setError(encoder.getStatus());
				}
				else
				{
					state=STATE_ELEMENT_START;
					tokenType=SimpleXMLToken.TYPE_ATTRIBUTE_VALUE;
					result=true;
				}
			}
			else if(state!=STATE_ERROR) setError("Invalid attribute value");
		}
		else if(state!=STATE_ERROR) setError("Expected: attribute value");
		return result;
	}
	
	/**
	 * Attempts to parse an element end tag
	 * Generates element end tag token information
	 * if an end tag is encountered, it is checked against its matching start tag
	 * @return true if an element end tag was parsed, false otherwise - if false, state should be checked to see if an error was encountered.
	 */
	private boolean parseElementEnd()
	{
		boolean result=false;
		if(expect(SimpleXMLMatcherFactory.openETag,true))
		{
			if(readToken(elementEndNameDelimiters,false))
			{
				if(expect(SimpleXMLMatcherFactory.closeETag,true))
				{
					result=endElement();
				}
				else if(state!=STATE_ERROR) setError("Expected: '" + SimpleXMLMatcherFactory.closeETag + "'");
			}
			else if(state!=STATE_ERROR) setError("Unable to parse element end tag");
		}
		return result;
	}
	
	/**
	 * Attempts to parse character data
	 * Generates character data token information
	 * @return true if character data was parsed, false otherwise - if false, state should be checked to see if an error was encountered.
	 */
	private boolean parseCharData()
	{
		boolean result=false;
		if(readToken(charDataDelimiters,false))
		{
			result=true;
			tokenType=SimpleXMLToken.TYPE_CHARACTER_DATA;
			tokenValue=encoder.decodeCharData(tokenValue);
		}
		return result;
	}

	/**
	 * Returns the current element's name
	 * @return the name of the current element, or null if there are none
	 */
	public String getElementName()
	{
		String result = null;
		if(!elements.empty())
		{
			result = elements.peek();
		}
		return result;
	}
	
	/**
	 * Checks for the indicated token
	 * @param value the token to check for
	 * @param consume indicates whether the token should be consumed, if found
	 * @return true if the token was found, false otherwise - if false, state should be checked to see if an error was encountered.
	 */
	private boolean expect(String value, boolean consume)
	{
		boolean result=false;
		if(loadBuffer(value.length()))
		{
			int offset=buffer.charAt(0)==SimpleXMLMatcherFactory.whitespaceChars[0]?1:0;
			result=true;
			for(int i=0; i < value.length() && result; i++)
			{
				result=value.charAt(i)==buffer.charAt(i+offset);
			}
			if(result && consume) buffer.delete(0,value.length()+offset);
		}
		return result;
	}

	/**
	 * Attempts to load at least the specified number of characters into the buffer, not counting leading whitespace.
	 * whitespace sequences are converted to single spaces on the fly
	 * If there is an error, state will be set to STATE_ERROR
	 * if the end of document was reached, eof will be set to true
	 * @param length minimum number of characters to load
	 * @return true on success, false if the requested number of characters could not be loaded - if false, state should be checked to see if an error was encountered.
	 */
	private boolean loadBuffer(int length)
	{
		boolean result=false;
		int prevChar=-1;
		if(buffer.length()>0) prevChar=(int)buffer.charAt(buffer.length()-1);
		int theChar=-1;
		int whitespaceOffset=buffer.length()==0?0:buffer.charAt(0)==' '?1:0;
		while((buffer.length()-whitespaceOffset)<length && !eof)
		{
			do
			{
				try
				{
					theChar=input.read();
					if((char)theChar==SimpleXMLMatcherFactory.newLine)lineNumber++;
					//Replace whitespace characters
					for(int i=1; i < SimpleXMLMatcherFactory.whitespaceChars.length; i++)
					{
						if((char)theChar==SimpleXMLMatcherFactory.whitespaceChars[i]) theChar=(int)(SimpleXMLMatcherFactory.whitespaceChars[0]);
					}
					if(theChar==-1)eof=true;
				}catch(Exception ex){setError("File i/o error");}
			}while(state!=STATE_ERROR && (char)prevChar==SimpleXMLMatcherFactory.whitespaceChars[0] && theChar==prevChar); //Condense whitespace characters
			if(!eof && state!=STATE_ERROR)
			{
				//Set the buffered value and index
				buffer.append((char)theChar);
				//Recalculate for leading whitespace - if necessary
				if(buffer.length()==1)whitespaceOffset=buffer.charAt(0)==' '?1:0;
				prevChar=theChar;
			}
		}
		result=(buffer.length()-whitespaceOffset)>=length;
		return result;
	}
	
	/**
	 * Reads text between two delimiters. The delimiters are consumed
	 * If the opening delimiter is not encountered, nothing is done
	 * tokenValue will be set to the delimited text (without delimiters) on success, or to an appropriate error message on error
	 * state will be set to STATE_ERROR on error
	 * @param openDelimiter the opening delimiter
	 * @param closeDelimiter the closing delimiter
	 * @return true if a matching token was parsed, false otherwise - if false, state should be checked to see if an error was encountered.
	 */
	private boolean readDelimited(String openDelimiter, String closeDelimiter)
	{
		boolean result=false;
		if(expect(openDelimiter,true))
		{
			String [] delim = new String[1];
			delim[0] = closeDelimiter;
			result=readToken(delim, false);
			if(result) result=expect(closeDelimiter,true);
		}
		return result;
	}
	
	/**
	 * Reads the next token end-delimited by any of the specified strings. The delimiter is not consumed.
	 * @param delimiters an array of possible end delimiters
	 * @param allowEOF indicates whether the token can be terminated by the end of file
	 * @return true if a token was read, false otherwise - if false, state should be checked to see if an error was encountered.
	 */
	private boolean readToken(String [] delimiters, boolean allowEOF)
	{
		boolean result = false;
		//This many characters will be left in the buffer to prevent truncating delimiters.
		//The value should be larger than any possible delimiter.
		int padLength=5;
		tokenValue="";
		do
		{
			//Get something in the buffer
			loadBuffer(bufferIncrement);
			//Search for the delimiter in the buffer
			int index = -1;
			int offset = -1;
			for(int i=0; i < delimiters.length; i++)
			{
				int thisOffset = buffer.indexOf(delimiters[i]);
				if(thisOffset>=0 && (index == -1 || thisOffset<offset))
				{
					offset = thisOffset;
					index = i;
				}
			}
			//Append buffer data to token value
			if(offset==-1)
			{
				if(eof)
				{
					//Use up the buffer, if necessary
					tokenValue += buffer;
					buffer.delete(0,buffer.length());
				}
				else if(buffer.length()>=padLength)
				{
					//Leave some in the buffer to prevent truncating delimiters
					tokenValue+=buffer.substring(0,buffer.length()-padLength);
					buffer.delete(0,buffer.length()-padLength);
				}
			}
			else
			{
				if(offset > 0) tokenValue += buffer.substring(0,offset);
				buffer.delete(0,offset);
				result=true;
			}
		}while(!result && !eof && state!=STATE_ERROR);
		
		if(!result && state!=STATE_ERROR && !allowEOF)
		{
			String err = "Unexpected end of document: expected";
			for(int i=0; i < delimiters.length; i++)
			{
				if(i > 0) err += " or";
				err += " '" + delimiters[i] + "'";
			}
			setError(err);
		}
		return result;
	}
	
	/**
	 * Reads quoted text into tokenValue
	 * @return true if quoted text was successfully read, false otherwise - if false, state should be checked to see if an error was encountered.
	 */
	private boolean readQuotedText()
	{
		String singleQ = "'";
		String doubleQ = "\"";
		boolean result = false;
		if(!expect(singleQ,false))
		{
			if(!expect(doubleQ,false))
			{
				setError("Expected: \" or '");
			}else result=readDelimited(doubleQ,doubleQ);
		}else result=readDelimited(singleQ,singleQ);
		return result;
	}

	/**
	 * Helper function to check names
	 */
	private boolean matchName(String token)
	{
		if(nameMatcher==null)
		{
			nameMatcher=SimpleXMLMatcherFactory.getNameMatcher(token);
		}
		else nameMatcher.reset(token);
		return nameMatcher.matches();
	}

	/**
	 * Helper function to check attribute values
	 */
	private boolean matchAttributeValue(String token)
	{
		if(attributeValueMatcher==null)
		{
			attributeValueMatcher=SimpleXMLMatcherFactory.getAttributeValueMatcher(token);
		}
		else attributeValueMatcher.reset(token);
		return attributeValueMatcher.matches();
	}

	/**
	 * Helper function to clean up at the end of an element
	 * Validates for matching start tag and adjusts the state accordingly
	 */
	private boolean endElement()
	{
		boolean result = false;
		if(tokenValue.equals(elements.peek()))
		{
			tokenType=SimpleXMLToken.TYPE_ELEMENT_END;
			elements.pop();
			result = true;
			if(elements.empty())
			{
				state=STATE_DOC_CLOSE;
			}else state=STATE_ELEMENT_CONTENT;
		}else setError("Unmatched element start tag. Expected '" + SimpleXMLMatcherFactory.openETag + elements.peek() + SimpleXMLMatcherFactory.closeETag + "'");
		return result;
	}

	/**
	 * Helper function to save numerous lines of code
	 */
	private void setError(String message)
	{
		state=STATE_ERROR;
		tokenType=SimpleXMLToken.TYPE_ERROR;
		tokenValue=lineNumber + " " + message;
	}
}
