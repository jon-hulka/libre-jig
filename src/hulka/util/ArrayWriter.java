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

import java.io.PrintWriter;

/**
 * This class abstracts saving arrays. It was created to store puzzle data for the libre-jigsaw project.
 */

public class ArrayWriter
{
	int xDimension=-1;
	int yDimension=-1;
	String title;
	public ArrayWriter(int xDimension, int yDimension, String title)
	{
		this.xDimension=xDimension;
		this.yDimension=yDimension;
		this.title=title;
	}
	
	/**
	 * @param values output values ( int[xDimension][yDimension] )
	 * @param names item names ( String[xDimension] )
	 * @param out stream to write to.
	 * @param err error reporting.
	 * @return true on success, null on error.
	 */
	public boolean save(int [][] values, String [] xNames, PrintWriter out, PrintWriter err)
	{
		boolean result=true;
		//Do a quick sanity check on the arrays - This should verify that everything is in order
		if(result && xNames.length<xDimension)
		{
			result=false;
			err.println("Saving " + title + ": Expected " + xDimension + " names but found " + xNames.length + ".");
		}
		
		if(result && values.length<xDimension)
		{
			result=false;
			err.println("Saving " + title + ": Expected " + xDimension + " lists but found " + values.length + ".");
		}
		
		for(int i=0; result && i<xDimension; i++)
		{
			if(result && xNames[i]==null)
			{
				result=false;
				err.println("Saving " + title + " (xNames[" + i + "]): Expected String but found null.");
			}

			if(result && values[i].length<yDimension)
			{
				result=false;
				err.println("Saving " + title + " (list '" + xNames[i] + "'): Expected " + yDimension + " items but found " + values[i].length + ".");
			}
		}
		
		//Any critical errors should have been dealt with by now, if all checks out, print the list.
		if(result)
		{
			//List name, type and dimensions (attributes)
			out.println(title + ":int:" + xDimension + ":" + yDimension);
			//Header names
			for(int i=0; i<xDimension; i++)
			{
				if(i>0) out.print(":");
				out.print(xNames[i]);
			}
			out.println();
			//List data
			for(int j=0; j<yDimension; j++)
			{
				for(int i=0; i<xDimension; i++)
				{
					if(i>0) out.print(":");
					out.print(values[i][j]);
				}
				out.println();
			}
		}
		return result;
	}
}
