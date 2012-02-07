/**
 * This code module may be freely used and modified without restriction.
 *  - Jonathan Hulka (jon.hulka@gmail.com)
 * 
 * This class provides a bare bones mechanism for writing XML files.
 * 
 * The goal of the project is to make something sophisticated enough to write HTML
 * and configuration files without getting overly complex
 * 
 * Limitations:
 *  - Only elements, attributes, character data as element contents, and comments are supported at this time
 *  - Text content cannot be mixed with elements (this will eventually be implemented)
 *  - Attribute values are treated as literals (no character or entity references recognized for writing)
 *    To do - add STATE_OPEN_ATTRIBUTE - value can be written until closed
 * 
 *To do:
 * upgrade this to the point that it will handle html
 * Have output functions throw io exceptions
 */

package hulka.xml;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Stack;
public class SimpleXMLWriter
{
	/**
	 * Startup state
	 */
	private static final int STATE_INIT=0;
	/**
	 * Element start tag being defined - ready for attributes
	 */
	private static final int STATE_OPEN_ELEMENT=1;
	/**
	 * Element start tag just closed - ready for contents
	 */
	private static final int STATE_ELEMENT_OPENED=2;
	/**
	 * Element end tag just written - text content cannot be written here
	 * To do - upgrade this when mixed content is allowed
	 */
	private static final int STATE_ELEMENT_CLOSED=3;
	/**
	 * Text contents being written - elements cannot be added here
	 * To do - upgrade this when mixed content is allowed
	 */
	private static final int STATE_CONTENT_WRITTEN=4;
	/**
	 * Document is finished - nothing more to do
	 */
	private static final int STATE_FINISH=5;
	
	String status = null;
	int state;
	OutputStreamWriter output = null;
	Stack<String> elements;
	int indent;
	SimpleXMLEncoder encoder;
	String indentString;
	
	public SimpleXMLWriter(OutputStreamWriter out)
	{
		encoder = new SimpleXMLEncoder();
		output = out;
		elements = new Stack<String>();
		indent=0;
		indentString="";
		setState(STATE_INIT);
	}
	
	/**
	 * Attempts to finish the document and close the output stream.
	 * If the attempt is unsuccessful, getStatus should give some information.
	 * If the attempt is successful, the output stream will be closed, otherwise its status is uncertain.
	 * @return true if successful, false if an error occurred - set getStatus()
	 */
	public boolean close()
	{
		boolean ok = true;
		while(ok && state != STATE_FINISH && state != STATE_INIT)
		{
			ok = setState(STATE_ELEMENT_CLOSED);
		}
		if(ok)
		{
			ok = false;
			try
			{
				output.close();
				ok = true;
			}catch(Exception ex){ status=ex.getMessage(); }
		}
		return ok;
	}
	
	/**
	 * Sets the state, writing any necessary output
	 * @param newState one of STATE_OPEN_ELEMENT, STATE_ELEMENT_OPENED, STATE_ELEMENT_CLOSED, STATE_CONTENT_WRITTEN, STATE_FINISH
	 */
	private boolean setState(int newState)
	{
		boolean ok = false;
		status=null;
		if(state==STATE_FINISH)
		{
			status="Document is finished";
		}
		switch(newState)
		{
			case STATE_INIT:
				ok=true;
				break;
			case STATE_OPEN_ELEMENT:
				switch(state)
				{
					case STATE_CONTENT_WRITTEN:
						status="Cannot write mixed content - text already written";
						break;
					case STATE_OPEN_ELEMENT:
						if(!write(">\n")) break;
						indent++;
					case STATE_ELEMENT_CLOSED:
					case STATE_ELEMENT_OPENED:
					case STATE_INIT:
						if(!write(getIndent() + "<")) break;
						ok = true;
						break;
				}
				break;
			case STATE_ELEMENT_OPENED:
				switch(state)
				{
					case STATE_OPEN_ELEMENT:
						if(!write(">\n")) break;
						indent++;
						ok=true;
						break;
					case STATE_ELEMENT_CLOSED:
					case STATE_ELEMENT_OPENED:
					case STATE_INIT:
					case STATE_CONTENT_WRITTEN:
						status="No element is being defined here";
						break;
				}
				break;
			case STATE_CONTENT_WRITTEN:
				switch(state)
				{
					case STATE_ELEMENT_CLOSED:
						status="Cannot write mixed content - elements already added";
						break;
					case STATE_INIT:
						status="Cannot write content outside an element";
					case STATE_OPEN_ELEMENT:
						if(!write(">\n")) break;
						indent++;
					case STATE_ELEMENT_OPENED:
					case STATE_CONTENT_WRITTEN:
						if(!write(getIndent()));
						ok=true;
						break;
				}
				break;
			case STATE_ELEMENT_CLOSED:
				switch(state)
				{
					case STATE_INIT:
						status="Cannot close element - none opened";
						break;
					case STATE_OPEN_ELEMENT:
						if(!write(" />\n")) break;
						elements.pop();
						ok=true;
						break;
					case STATE_ELEMENT_OPENED:
					case STATE_CONTENT_WRITTEN:
					case STATE_ELEMENT_CLOSED:
						indent--;
						ok = write(getIndent() + "</" + elements.pop() + ">\n");
						if(elements.empty()) newState=STATE_FINISH;
						ok=true;
						break;
				}
				break;
			default:
				status="Undefined state";
				break;
		}
		if(ok)state=newState;
		return ok;
	}
	
	/**
	 * Wraps the output stream's write function for convenience
	 * @param s the string to write
	 * @return true if successful, false if an error occurred - set getStatus()
	 */
	private boolean write(String s)
	{
		boolean ok = false;
		try
		{
			output.write(s.getBytes());
			ok = true;
		}catch(Exception ex){status=ex.getMessage();}
		return ok;
	}
	
	private String getIndent()
	{
		if(indent!=indentString.length())
		{
			indentString = "";
			for(int i=0; i < indent; i++)
			{
				indentString += "\t";
			}
			return indentString;
		}
		return indentString;
	}
	
	/**
	 * Opens an element's start tag.
	 * @param name the unencoded name
	 * @return true if successful, false if an error occurred - set getStatus()
	 */
	public boolean openElement(String name)
	{
		status=null;
		boolean ok = false;
		if(name == null)
		{
			status = "null value not permitted";
		}
		else
		{
			String encoded = encoder.encodeName(name);
			if(encoded==null)
			{
				status = encoder.getStatus();
			}
			else if(setState(STATE_OPEN_ELEMENT))
			{
				ok=write(encoded);
				if(ok) elements.push(encoded);
			}
		}
		return ok;
	}
	
	/**
	 * Adds an attribute to the current element.
	 * This will only succeed if the element's start tag is still open (getState() returns STATE_OPEN_ELEMENT).
	 * To do: break this up so that attribute values can be written more flexibly
	 * @param name
	 * @param value
	 * @return true if successful, false if an error occurred - set getStatus()
	 */
	public boolean addAttribute(String name, String value)
	{
		status = null;
		boolean ok = false;
		if(name==null || value==null)
		{
			status="null values not permitted here";
		}
		else if(state==STATE_OPEN_ELEMENT)
		{
			String encodedName = encoder.encodeName(name);
			if(encodedName==null)
			{
				status=encoder.getStatus();
			}
			else
			{
				String encodedValue = encoder.encodeCharData(value);
				if(encodedValue==null)
				{
					status=encoder.getStatus();
				}
				else
				{
					ok = write(' ' + encodedName + "=\"" + encodedValue + '"');
				}
			}
		}
		else
		{
			status="Unable to write attribute here - not inside element tag";
		}
		return ok;
	}
	
	/**
	 * Closes the current element
	 * @return true if successful, false if an error occurred - set getStatus()
	 */
	public boolean closeElement()
	{
		return setState(STATE_ELEMENT_CLOSED);
	}
	
	/**
	 * Returns the status of the last operation.
	 * If the operation was successful, this function will return null
	 * If the operation failed, there should be some useful information here
	 * @return status of the previous operation, or null if the operation was successful
	 */
	public String getStatus()
	{
		return status;
	}
	
	/**
	 * Encodes and adds character data to the current element.
	 * @param content
	 * @param value
	 * @return true if successful, false if an error occurred - see getStatus()
	 */
	public boolean writeContent(String content)
	{
		status = null;
		boolean ok = false;
		if(content==null)
		{
			status="null value not permitted here";
		}
		else if(setState(STATE_CONTENT_WRITTEN))
		{
			String encoded = encoder.encodeCharData(content);
			if(encoded==null)
			{
				status = encoder.getStatus();
			}
			else
			{
				ok = write(encoded + '\n');
			}
		}
		return ok;
	}
}
