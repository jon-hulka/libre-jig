/**
 *      ArrayWriter.java
 *      
 *      Copyright 2010 Jonathan Hulka <jon.hulka@gmail.com>
 *      
 *      This program is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      (at your option) any later version.
 *      
 *      This program is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU General Public License for more details.
 *      
 *      You should have received a copy of the GNU General Public License
 *      along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * Changelog:
 * 2011.12.14 - Jon - Created
 */

package hulka.util;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.IOException;

/**
 * This class abstracts loading arrays. It was created to retrieve puzzle data for the libre-jigsaw project.
 */

public class ArrayReader
{
	String title=null;
	int [][] values = null;
	String [] columnNames = null;
	int columnCount = -1;
	int rowCount = -1;

	public ArrayReader(String title)
	{
		this.title=title;
	}

	/**
	 * @param columnName column to search for.
	 * @param err error stream, null to ignore errors.
	 * @return values from the searched column, null if the column was not found.
	 */
	public int [] getColumn(String columnName,PrintWriter err)
	{
		int [] result=null;
		if(columnNames!=null)
		{
			for(int i=0; result==null && i<columnNames.length; i++)
			{
				if(columnNames[i].equals(columnName))
				{
					result=values[i];
				}
			}
		}
		if(result==null && err!=null)
		{
			err.println(title + ": unable to find column '" + columnName + "'");
		}
		return result;
	}
	
	/**
	 * @param in stream to read from.
	 * @param err error reporting.
	 * @return loaded values on success, null on error.
	 */
	public boolean load(BufferedReader in, PrintWriter err)
	{
		boolean result=true;
		//Load the attributes
		StringBuilder builder = new StringBuilder();
		String line=null;

		try
		{
			line=in.readLine();
		}
		catch(IOException ex)
		{
			result=false;
			err.println("ArrayReader (" + title + "): getting list attributes: " + ex.getMessage());
		}

		String [] items=null;
		if(result && line==null)
		{
			result=false;
			err.println("ArrayReader (" + title + "): getting list attributes: Unexpected end of file.");
		}
		
		if(result)
		{
			builder.append(line);
			items=parse(builder,err,"getting list attributes");
			if(items==null)
			{
				result=false;
			}
		}
			
		if(result && items.length!=4)
		{
			result=false;
			err.println("ArrayReader (" + title + "): getting list attributes: expected 4 items (title, type, columnCount, and rowCount), found " + items.length + ".");
		}
		if(result && !title.equals(items[0]))
		{
			result=false;
			err.println("ArrayReader (" + title + "): getting list attributes: expected title '" + title + ", found " + items[0] + ".");
		}
		if(result && !"int".equals(items[1]))
		{
			result=false;
			err.println("ArrayReader (" + title + "): getting list attributes (type): expected 'int', found " + items[1] + ".");
		}
		if(result)
		{
			String dimName=null;
			try
			{
				dimName="columnCount";
				columnCount=Integer.parseInt(items[2]);
				dimName="rowCount";
				rowCount=Integer.parseInt(items[3]);
			}
			catch(NumberFormatException ex)
			{
				result=false;
				err.println("ArrayReader (" + title + "): getting list attributes (" + dimName + "): expected integer value.");
			}
		}
		
		//Load the header names
		if(result)
		{
			try
			{
				line=in.readLine();
			}
			catch(IOException ex)
			{
				result=false;
				err.println("ArrayReader (" + title + "): getting header names: " + ex.getMessage());
			}
		}
		
		if(result && line==null)
		{
			result=false;
			err.println("ArrayReader (" + title + "): getting header names: Unexpected end of file.");
		}

		if(result)
		{
			builder.append(line);
			columnNames=parse(builder,err,"getting header names");
			if(columnNames==null)
			{
				result=false;
			}
		}
		if(result && columnNames.length!=columnCount)
		{
			result=false;
			err.println("ArrayReader (" + title + "): getting header names: Expected " + columnCount + " items, found " + columnNames.length);
		}

		//Set up arrays for storage
		if(result)
		{
			values=new int[columnCount][];
			for(int i=0; i< columnCount; i++)
			{
				values[i] = new int[rowCount];
			}
		}
		
		//Load the data
		for(int j=0; result && j<rowCount; j++)
		{
			try
			{
				line=in.readLine();
			}
			catch(IOException ex)
			{
				result=false;
				err.println("ArrayReader (" + title + "): getting data (row" + j + "): " + ex.getMessage());
			}
			
			if(result && line==null)
			{
				result=false;
				err.println("ArrayReader (" + title + "): getting data (row" + j + "): Unexpected end of file.");
			}
			if(result)
			{
				builder.append(line);
				items=parse(builder,err,"getting data (row + " + j + ")");
			}
			if(result && items.length<columnCount)
			{
				result=false;
				err.println("ArrayReader (" + title + "): getting data (row" + j + "): Expecting " + columnCount + " items, found " + items.length);
			}
			for(int i=0; result && i<columnCount; i++)
			{
				try
				{
					values[i][j]=Integer.parseInt(items[i]);
				}
				catch(NumberFormatException ex)
				{
					result=false;
					err.println("ArrayReader (" + title + "): getting data (" + columnNames[i] + "row" + j + "): Expected integer value.");
				}
			}
		}
		
		if(!result)
		{
			//On error - reset everything
			title=null;
			values = null;
			columnNames = null;
			columnCount = -1;
			rowCount = -1;
		}
		return result;
	}
	
	private String [] parse(StringBuilder builder, PrintWriter err, String stage)
	{
		String [] result=null;
		if(builder.length()==0)
		{
			err.println("ArrayReader (" + title + "): " + stage + ": expecting items but found an empty line.");
		}
		else
		{
			int itemCount=1;
			int index=0;
			while(index>=0)
			{
				index=builder.indexOf(":",index);
				if(index>=0)
				{
					index++;
					itemCount++;
				}
			}
			
			//The item count should be correct here, if this fails, the code is buggy
			result=new String[itemCount];
			for(int i=0; i<itemCount; i++)
			{
				int pos=builder.indexOf(":");
				if(pos >= 0)
				{
					result[i]=builder.substring(0,pos);
					builder.delete(0,pos+1);
				}
				else
				{
					result[i]=builder.toString();
					builder.delete(0,builder.length());
				}
			}
		}
		return result;
	}
}
