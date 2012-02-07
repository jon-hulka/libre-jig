/**
 *      MiscUtils.java
 * 
 *      These are all free to use, with no restrictions.
 * 
 * @author Jonathan Hulka (jon.hulka@gmail.com)
 * 
 */

package hulka.util;
import java.awt.image.*;
import javax.imageio.*;
import java.net.*;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;

/**
 * Contains odds and ends to simplify some common tasks.
 */
public class MiscUtils
{
	private static JFileChooser imageChooser=null;

	/**
	 * Loads an image without having to worry about exceptions.
	 * If the image won't load, a blank one is created.
	 * @param path image path
	 * @param expectedWidth width to make the blank image, if necessary.
	 * @param expectedHeight height to make the blank image, if necessary.
	 * @return the image
	 */
	public static BufferedImage loadImage(String path, int expectedWidth, int expectedHeight)
	{
		BufferedImage image = null;
		try
		{
			URL url = translateURL(path);
			image = ImageIO.read(url);
		}
		catch(Exception ex)
		{
			image = new BufferedImage(expectedWidth,expectedHeight,BufferedImage.TYPE_INT_ARGB);
		}
		return image;
	}

	/**
	 * Displays a file selection dialog, and returns the URL of the image selected.
	 * Files with extensions "jpg", "jpeg", "png", and "gif" are allowed.
	 * This function is not thread safe.
	 * @param parent parent component of the dialog, or null.
	 * @param currentDirectory starting directory, or null for default.
	 * @return the url of the selected image, null if the selection was cancelled or an error occurred.
	 */
	public static URL promptImageURL(Component parent, File currentDirectory)
	{
		if(imageChooser==null)
		{
			imageChooser = new JFileChooser();
			FileNameExtensionFilter filter = new FileNameExtensionFilter("Images", "jpg", "jpeg", "png", "gif");
			imageChooser.setFileFilter(filter);
		}
		
		if(currentDirectory!=null)
		{
			imageChooser.setCurrentDirectory(currentDirectory);
		}

		URL result=null;
		int cValue = imageChooser.showOpenDialog(parent);
		if(cValue == JFileChooser.APPROVE_OPTION)
		{
			try
			{
				result=imageChooser.getSelectedFile().toURI().toURL();
			}catch(Exception ex){ex.printStackTrace();}
		}
		return result;
	}
	
	/**
	 * Determines whether a path is absolute or relative and translates it to a URL accordingly.
	 */
	public static URL translateURL(String path)
	{
		URL result=null;
		try
		{
			//Still needs a bit of tweaking to make it work on Windows
			if("/".equals(path.substring(0,1)))
			{
				result=new URL("file:"+path);
			}
			else
			{
				//This works in or out of jar files
				result=Thread.currentThread().getContextClassLoader().getResource(path);
			}
		}catch(Exception ex){ex.printStackTrace();}
		return result;
	}
}
