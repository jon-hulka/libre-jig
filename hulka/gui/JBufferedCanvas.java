/**
 *      JBufferedCanvas.java
 *      
 *      Copyright 2008, 2010 Jonathan Hulka <jon.hulka@gmail.com>
 *      
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
 *      
 */


/**
 * Change log:
 * 2010 06 11 - Jon
 * Moved to package hulka.gui
 * Deprecated clearRect and clearAll - replaced with clear(Rect) and clear() to make more uniform with the update functions.
 */
package hulka.gui;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.Image;
import java.awt.Dimension;
import java.awt.Rectangle;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

public class JBufferedCanvas extends JPanel
{
    private JBufferedCanvas(){}
    private BufferedImage backgroundImage;
    private BufferedImage offscreenImage;
    
    //Temporary storage to avoid heap use
    private Rectangle cB=new Rectangle();
    private Dimension size=new Dimension();

    public JBufferedCanvas(BufferedImage backgroundImage)
    {
        super();
        reset(backgroundImage);
    }
    
    /**
     * Replaces existing background and foreground images, and resets the preferred size.
     * This is fairly expensive.
     */
    public void reset(BufferedImage backgroundImage)
    {
        this.backgroundImage = backgroundImage;
        size.width=backgroundImage.getWidth();
        size.height=backgroundImage.getHeight();
        setPreferredSize(size);
//        setMinimumSize(size);
//        setMaximumSize(size);
        offscreenImage = new BufferedImage(size.width, size.height, backgroundImage.getType());
        offscreenImage.getGraphics().drawImage(backgroundImage, 0, 0, null);
	}
	
    public void paint(Graphics g)
    {
        cB = g.getClipBounds(cB);
        g.drawImage(offscreenImage, cB.x, cB.y, cB.x + cB.width, cB.y + cB.height, cB.x, cB.y, cB.x + cB.width, cB.y + cB.height, null);
//        System.out.println("painting " + g.getClipBounds());      
    }
    
    public BufferedImage getBackgroundImage()
    {
		return backgroundImage;
	}

	public BufferedImage getForegroundImage()
	{
		return offscreenImage;
	}

    /**
     * Gets the offscreen image graphics.
     * After drawing, the affected areas can be updated with a call to repaint.
     */
    public Graphics getForegroundGraphics()
    {
        return offscreenImage.getGraphics();
    }

	/**
	 * Gets the background image graphics.
	 * After drawing, the affected areas can be updated with a call to clear, then repaint.
	 */
    public Graphics getBackgroundGraphics()
    {
        return backgroundImage.getGraphics();
    }
    
    /**
     * Draws an area of the background image to the corresponding area of the offscreen image
     * @param x left side of the area to clear
     * @param y top of the area to clear
     * @param width width of the area to clear
     * @param height height of the area to clear
     */
    public void clear(Rectangle r)
    {
        offscreenImage.getGraphics().drawImage(backgroundImage, r.x, r.y, r.x + r.width, r.y + r.height, r.x, r.y, r.x + r.width, r.y + r.height, this);
	}
	
	/**
	 * Draws the background image to the offscreen image
	 */
	public void clear()
	{
		offscreenImage.getGraphics().drawImage(backgroundImage,0,0, this);
	}

	/**
	 * @deprecated use clear(Rectangle) instead
	 */
    public void clearRect(int x, int y, int width, int height)
    {
        offscreenImage.getGraphics().drawImage(backgroundImage, x, y, x + width, y + height, x, y, x + width, y + height, this);
    }
    
    /**
     * @deprecated use clear() instead
     */
    public void clearAll()
    {
		offscreenImage.getGraphics().drawImage(backgroundImage,0,0, this);
	}
    
    /**
     * @deprecated use getForegroundGraphics().drawImage
     */
    public void drawImage(Image image, int x, int y)
    {
        offscreenImage.getGraphics().drawImage(image, x, y, this);
    }
    
}
