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
 * 2010 08 04 Jon
 *  - Added mean color to image specification (xml)
 *  - Added getMeanColor(int)
 * 2010 06 09  Jon
 *  - Moved from puzzle games collection to package hulka.gui
 */


package hulka.util;
import hulka.xml.SimpleXMLReader;
import hulka.xml.SimpleXMLToken;
//import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
//import java.net.URL;
//import java.io.FileNotFoundException;
//import java.net.URISyntaxException;
import java.awt.Color;

public class ImageMap
{
	ArrayList <String> thumbPaths;
	ArrayList <String> imagePaths;
	ArrayList <Color> meanColors;

	/**
	 * Reads an XML file from the specified path.
	 * @param path relative path of the file to read<br />
	 * 1 root element &lt;imagelist&gt;<br />
	 * Contents of &lt;imagelist&gt;:<br />
	 * 1.1 One or more &lt;image&gt; elements<br />
	 * Contents of &lt;image&gt;:<br />
	 * 1.1.1 One &lt;imagepath&gt; (text)<br />
	 * 1.1.2 One &lt;thumbpath&gt; (text)<br />
	 * 1.1.3 Optionally one &lt;mean&gt;<br />
	 * Contents of &lt;mean&gt;<br />
	 * 1.1.3.1 One &lt;r&gt; (integer)<br />
	 * 1.1.3.2 One &lt;g&gt; (integer)<br />
	 * 1.1.3.3 One &lt;b&gt; (integer)<br />
	 * @param initialCapacity initial capacity of the ArrayLists used to hold keys and values. If the number of formats stored in the file is known, use it here.
	 */
	public ImageMap(String path, int initialCapacity)
	{
		InputStreamReader isReader=new InputStreamReader(Thread.currentThread().getContextClassLoader().getResourceAsStream(path));
		SimpleXMLReader reader = new SimpleXMLReader(isReader);

		thumbPaths = new ArrayList<String>(initialCapacity);
		imagePaths = new ArrayList<String>(initialCapacity);
		meanColors = new ArrayList<Color>(initialCapacity);
		
		readPaths(reader);
		try{ isReader.close(); }catch(Exception ex){ex.printStackTrace();}
	}

	private void readPaths(SimpleXMLReader reader)
	{
		SimpleXMLToken token = null;
		String level1Parent="imagelist";
		String level2Parent="image";
		int depth=0;
		String parent=null;
		String imagePath=null;
		String thumbPath=null;
		int r=0;
		int b=0;
		int g=0;
		do
		{
			token = reader.parseNext(token);
			switch(token.type)
			{
				case SimpleXMLToken.TYPE_ERROR:
					System.err.println(token.value);
				case SimpleXMLToken.TYPE_DOCUMENT_END:
					token=null;
					break;
				case SimpleXMLToken.TYPE_ELEMENT_START:
					switch(depth)
					{
						case 0:
							//Look for the root <imagelist> element
							parent=token.value;
							if(parent==null||!parent.equals(level1Parent))
							{
								token=null;
								System.err.println("Parsing formats: expected root element <formatlist>");
							}
							break;
						case 1:
							//Look for <image> element
							parent=parent==null?null:token.value.equals(level2Parent)?level2Parent:null;
							break;
						case 2:
							//Look for <imagepath> or <thumbpath> or <mean>
							parent=parent==null?null:token.value;
							break;
						case 3:
							//Look for <red> or <green> or <blue>
							parent=parent!=null?parent.equals("mean")?token.value:parent:null;
							break;
						default:
							//No elements should be defined at this depth, ignore
							break;
					}
					depth++;
					break;
				case SimpleXMLToken.TYPE_CHARACTER_DATA:
					if(depth==4)
					{
						if(parent!=null)
						{
							if(parent.equals("r")){try{r=Integer.parseInt(token.value);}catch(Exception ex){}}
							else if(parent.equals("g")){try{g=Integer.parseInt(token.value);}catch(Exception ex){}}
							else if(parent.equals("b")){try{b=Integer.parseInt(token.value);}catch(Exception ex){}}
						}
					}
					if(depth==3)
					{
						//Assign values for level 2 elements
						if(parent!=null)
						{
							if(parent.equals("imagepath")){imagePath=token.value;}
							else if(parent.equals("thumbpath")){thumbPath=token.value;}
						}
					}
					break;
				case SimpleXMLToken.TYPE_ELEMENT_END:
					depth--;
					if(parent!=null)
					{
						if(depth==1)
						{
							parent=level1Parent;
							//Process the listing
							if(imagePath!=null && thumbPath!=null)
							{
								imagePaths.add(imagePath);
								thumbPaths.add(thumbPath);
								meanColors.add(new Color(r,g,b));
							}else System.err.println("Parsing image list: insufficient information");
							imagePath=null;
							thumbPath=null;
							r=0;
							g=0;
							b=0;
						}
//						else parent=depth==2?level2Parent:depth>=3?parent:null;
						else parent=depth==2?level2Parent:(depth==3&&(parent.equals("r")||parent.equals("g")||parent.equals("b")))?"mean":depth>=3?parent:null;
					}
					//Outside the root element - nothing more to do.
					if(depth<1) token=null;
					break;
			}
		}while(token != null);
	}

	/**
	 * Returns the number of formats listed.
	 * @return the number of formats listed.
	 */
	public int size()
	{
		return thumbPaths.size();
	}
	
	/**
	 * Finds a mean color by index.
	 * @param index index of the item to query.
	 * @return mean color, or null if the index is out of range.
	 */
	public Color getMeanColor(int index)
	{
		Color result = null;
		if(index >=0 && index < imagePaths.size())
		{
			result = meanColors.get(index);
		}
		
		return result;
	}
	
	/**
	 * Finds an image path by index.
	 * @param index index of the item to query.
	 * @return image path, or null if the index is out of range.
	 */
	public String getImagePath(int index)
	{
		String result = null;
		if(index >=0 && index < imagePaths.size())
		{
			result = imagePaths.get(index);
		}
		
		return result;
	}

	/**
	 * Finds an image path by thumbnail path.
	 * @param thumbPath thumbnail path.
	 * @return image path, or null if the thumbnail is not listed.
	 */
	public String getImagePath(String thumbPath)
	{
		return getImagePath(thumbPaths.indexOf(thumbPath));
	}
	
	/**
	 * Finds a thumbnail path by index.
	 * @param index index of the item to query.
	 * @return thumbnail path, or null if the index is out of range.
	 */
	public String getThumbPath(int index)
	{
		String result = null;
		if(index >=0 && index < thumbPaths.size())
		{
			result = thumbPaths.get(index);
		}
		return result;
	}
	
	/**
	 * Finds a thumbnail path by image path.
	 * @param imagePath image path.
	 * @return thumbnail path, or null if the image path is not listed.
	 */
	public String getThumbPath(String imagePath)
	{
		return getThumbPath(thumbPaths.indexOf(imagePath));
	}
}

