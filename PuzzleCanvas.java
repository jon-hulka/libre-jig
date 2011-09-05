/**
 *      PuzzleCanvas.java
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
 * 2011 06 06 - Jon
 * Changed dragBuffer dimensions to allow for multi-select (pieces may be further apart)
 * 2011 02 13 - Jon
 * Reintegrated selection highlighting into tile drawing.
 * 2011 02 12 - Jon
 * Modified tile rendering code to keep rotated tiles in memory for improved performance.
 * 2011 02 08 - Jon
 * Tile image rendering now uses INTERPOLATION_BICUBIC rendering hint for smoother rotation.
 * 2010 08 11 - Jon
 * Changed background color logic.
 * 2010 08 04 - Jon
 * Added setMeanColor (chooses a contrasting color for default background)
 * 2010 06 15 - Jon
 * Added select flag to buildTileImage
 * 2010 06 14 - Jon
 * Added preview dialog.
 * 2010 07 26 - Jon
 * Changed contrast color mapping (for selection box) to hsb(h+0.5,1-s,1-b) for better contrast
 */
import hulka.gui.JBufferedCanvas;
import hulka.util.MiscUtils;

//Geometry
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;

//Graphics
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.Color;
import java.awt.AlphaComposite;
import java.awt.RenderingHints;

//Events
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.ActionMap;
import javax.swing.InputMap;

import javax.swing.KeyStroke;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.JFrame;
import javax.swing.JColorChooser;
import hulka.event.KeyStrokeAction;

import java.awt.event.MouseMotionListener;
import java.awt.event.MouseListener;

public class PuzzleCanvas extends JBufferedCanvas
{
	public static final int DRAW_BACKGROUND = 0;
	public static final int DRAW_FOREGROUND = 1;
	public static final int DRAW_DRAGBUFFER = 2;
	public static final int DRAW_TILEBUFFER = 3;
	public static final int DRAW_PUZZLEIMAGE = 4;
	public static final int DRAW_ROTATECCW = 5;
	public static final int DRAW_ROTATECW = 6;
	public static final int CURSOR_SIZE = 32;

	private BufferedImage puzzleImage=null;
	private BufferedImage tileBuffer=null;
	private BufferedImage tileIntermediate=null;
	private BufferedImage dragBuffer=null;
	private BufferedImage cwImage=null;
	private BufferedImage ccwImage=null;

	private AffineTransform lightTransform = AffineTransform.getTranslateInstance(1,1);
	private AffineTransform shadowTransform = AffineTransform.getTranslateInstance(-1,-1);
	private Color selectedColor=new Color(0x07f,0x07f,0x07f,0x07f);
	private Color lightColor=new Color(0x0ff,0x0ff,0x0ff,0x060);
	private Color shadowColor=new Color(0,0,0,0x060);
	private Color transparentColor=new Color(0,0,0,0);
	private Color backgroundColor=Color.WHITE;
	private Color contrastColor=Color.BLACK;
	private float[] hsb=new float[3];
	private PreviewDialog previewDialog;

	private int tileSize;
	private int errMargin;
	private int tileCount;
	private int tileIndex;
	private Shape [] tileMasks;
	private Shape [] rotatedMasks;

	public PuzzleCanvas(Dimension size)
	{
		super(new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB));

		cwImage = MiscUtils.loadImage("images/cwcursor.png",CURSOR_SIZE,CURSOR_SIZE);
		ccwImage = MiscUtils.loadImage("images/ccwcursor.png",CURSOR_SIZE,CURSOR_SIZE);

		setMinimumSize(size);
		setMaximumSize(size);
		setFocusable(true);
		previewDialog=new PreviewDialog();
	}


	public void setPuzzleImage(BufferedImage image)
	{
		puzzleImage=image;
		previewDialog.setImage(image);
	}

	/**
	 * If the puzzle image is not loaded this will throw a NullPointerException
	 */
	public Dimension getImageSize(Dimension d)
	{
		if(d==null)d=new Dimension();
		d.width=puzzleImage.getWidth();
		d.height=puzzleImage.getHeight();
		return d;
	}

	/**
	 * Erases a section of the drawing area by setting it to the background color
	 * @param bounds the area to erase
	 */
	public void erase(Rectangle bounds)
	{
		Graphics2D g2d = (Graphics2D)getBackgroundGraphics();
		g2d.setBackground(backgroundColor);
		g2d.clearRect(bounds.x,bounds.y,bounds.width,bounds.height);
		g2d.dispose();
	}

	/**
	 * Erases the entire drawing area by setting it to the background color
	 */
	public void erase()
	{
		Graphics2D g2d = (Graphics2D)getBackgroundGraphics();
		g2d.setBackground(backgroundColor);
		g2d.clearRect(0,0,getWidth(),getHeight());
		g2d.dispose();
	}

	/**
	 * Saves a piece of image to the dragBuffer
	 * @param bounds position and size
	 * @param sourceLayer DRAW_BACKGROUND or DRAW_FOREGROUND
	 */
	public void backupImage(Rectangle bounds, int sourceLayer)
	{
		Graphics2D g2d = (Graphics2D)(dragBuffer.getGraphics());
		BufferedImage img = null;
		switch(sourceLayer)
		{
			case DRAW_BACKGROUND:
				img = getBackgroundImage();
			break;
			case DRAW_FOREGROUND:
				img = getForegroundImage();
			break;
		}
		g2d.drawImage(img,0,0,bounds.width,bounds.height,bounds.x,bounds.y,bounds.x+bounds.width,bounds.y+bounds.height,null);
		g2d.dispose();
	}

	/**
	 * Draws a tile on the drawing area.
	 * If sourceLayer is DRAW_TILEBUFFER, the current tile should be set through a call to setTileIndex first.
	 * @param bounds location and size
	 * @param clipRect the clipping region
	 * @param sourceLayer DRAW_DRAGBUFFER, DRAW_TILEBUFFER, or DRAW_PUZZLEIMAGE
	 * @param drawLayer DRAW_BACKGROUND, DRAW_FOREGROUND or DRAW_DRAGBUFFER
	 */
	public void drawTile(Rectangle bounds, Rectangle clipRect, int sourceLayer, int drawLayer)
	{
		Graphics2D g2d = null;
		BufferedImage img = null;
		int top=0;
		int left=0;
		switch(drawLayer)
		{
			case DRAW_BACKGROUND:
				g2d = (Graphics2D)getBackgroundGraphics();
			break;
			case DRAW_FOREGROUND:
				g2d = (Graphics2D)getForegroundGraphics();
			break;
			case DRAW_DRAGBUFFER:
				g2d = (Graphics2D)(dragBuffer.getGraphics());
			break;
		}
		switch(sourceLayer)
		{
			case DRAW_DRAGBUFFER:
				img = dragBuffer;
			break;
			case DRAW_TILEBUFFER:
				img = tileBuffer;
				top=tileIndex*(tileSize+errMargin*2);
			break;
			case DRAW_PUZZLEIMAGE:
				img = puzzleImage;
			break;
			case DRAW_ROTATECCW:
				img = ccwImage;
			break;
			case DRAW_ROTATECW:
				img = cwImage;
			break;
		}
		g2d.setClip(null);
		if(clipRect != null)
		{
			g2d.setClip(clipRect.x, clipRect.y, clipRect.width, clipRect.height);
		}
		g2d.drawImage(img,bounds.x,bounds.y,bounds.x+bounds.width,bounds.y+bounds.height,left,top,bounds.width+left,bounds.height+top,null);
		g2d.dispose();
	}

	/**
	 * Just using shape clipping doesn't apply antialiasing, this gives a smoother result.
	 * Applies a tileSize by tileSize clip region.
	 * @param g2d graphics context to apply clip region to.
	 * @param mask clip region.
	 * @param left x coordinate of upper left corner.
	 * @param top y coordinate of upper left corner.
	 */
	private void applyAntialiasedMask(Graphics2D g2d,Shape mask,int left,int top)
	{
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		//Clear the alpha
		g2d.setComposite(AlphaComposite.Clear);
		g2d.fillRect(left,top, tileSize, tileSize);
		//render the clip shape to alpha
		g2d.setComposite(AlphaComposite.Src);
		g2d.setColor(Color.WHITE);
		g2d.fill(mask);
		g2d.setComposite(AlphaComposite.SrcAtop);
	}

	public void drawRect(Rectangle rect, int drawLayer)
	{
		Graphics2D g2d = (Graphics2D)getForegroundGraphics();
		g2d.setColor(contrastColor);
		g2d.drawRect(rect.x,rect.y,rect.width,rect.height);
		g2d.dispose();
	}

	/**
	 * Sets the index of the current working tile.
	 */
	public void setTileIndex(int index)
	{
		tileIndex=index;
	}

	/**
	 * Call setBuffers first.
	 * Used by PuzzleHandlers for setup.
	 * This needs to be done for all tiles before buildTileImage is used.
	 * @param index tile index.
	 * @param mask unrotated tile shape.
	 */
	public void setTileMask(int index, Shape mask)
	{
		tileMasks[index]=mask;
	}
	
	/**
	 * 
	 */
	public void drawSelected(int x, int y, int drawLayer)
	{
		Graphics2D g2d=null;
		switch(drawLayer)
		{
			case DRAW_BACKGROUND:
				g2d = (Graphics2D)getBackgroundGraphics();
			break;
			case DRAW_FOREGROUND:
				g2d = (Graphics2D)getForegroundGraphics();
			break;
			case DRAW_DRAGBUFFER:
				g2d = (Graphics2D)(dragBuffer.getGraphics());
			break;
		}
		g2d.clip(null);
		g2d.clip(AffineTransform.getTranslateInstance(x,y).createTransformedShape(rotatedMasks[tileIndex]));
		g2d.setColor(selectedColor);
		g2d.fillRect(x,y,tileSize,tileSize);
		g2d.dispose();
	}

	/**
	 * Builds the image for the current tile. This should be called during setup for each tile, and when a tile is rotated.
	 * The current tile should be set through a call to setTileIndex first.
	 * @param fromX x coordinate of original tile location
	 * @param fromY y coordinate of original tile location
	 * @param rotation current tile rotation transform
	 */
	public void buildTileImage(int fromX, int fromY, AffineTransform rotation)
	{
		//Draw the tile's image
		Graphics2D gInt = (Graphics2D)tileIntermediate.getGraphics();
		//Highest quality rotation and scaling
		gInt.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);

		AffineTransform tf = gInt.getTransform();
		if(rotation != null) gInt.transform(rotation);
		int size = tileSize + errMargin*2;
		int x = fromX - errMargin;
		int y = fromY - errMargin;
		gInt.drawImage(puzzleImage,0,0,size,size,x,y,x + size,y + size, null);

		gInt.setTransform(tf);

		Rectangle unMask = new Rectangle(0,0,size,size);
		Shape rMask=tileMasks[tileIndex];
		if(rotation!=null)
		{
			rotation.translate(errMargin,errMargin);
			rMask=rotation.createTransformedShape(rMask);
		}

		gInt.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		//light hints
		Area mask = new Area(unMask);
		Shape shifted = lightTransform.createTransformedShape(rMask);
		mask.subtract(new Area(shifted));
		gInt.setColor(lightColor);
		gInt.fill(mask);

		//shadow hints
		mask = new Area(unMask);
		shifted = shadowTransform.createTransformedShape(rMask);
		mask.subtract(new Area(shifted));
		gInt.setColor(shadowColor);
		gInt.fill(mask);

		gInt.dispose();

		Graphics2D g2d = (Graphics2D)tileBuffer.getGraphics();

		rotatedMasks[tileIndex]=rMask;
		//tile shape
		int top=tileIndex*(tileSize+errMargin*2);
		AffineTransform trans=AffineTransform.getTranslateInstance(0,top);
		rMask=trans.createTransformedShape(rMask);
		applyAntialiasedMask(g2d,rMask,0,top);
		g2d.drawImage(tileIntermediate,0,top,null);
	}

	public void clearDragBuffer(int width, int height)
	{
		Graphics2D g2d = (Graphics2D)dragBuffer.getGraphics();
		g2d.setBackground(transparentColor);
		g2d.clearRect(0,0,width,height);
		g2d.dispose();
	}

	public void setBuffers(int tileCount, int tileSize, int errMargin)
	{
		int w=puzzleImage.getWidth();
		int h=puzzleImage.getHeight();
		this.tileCount=tileCount;
		this.tileIndex=0;
		this.tileSize = tileSize;
		this.errMargin=errMargin;
		//All tiles will be stored here
		tileBuffer = new BufferedImage(tileSize + errMargin*2,tileCount*(tileSize + errMargin*2),BufferedImage.TYPE_INT_ARGB);
		tileMasks=new Shape[tileCount];
		rotatedMasks=new Shape[tileCount];
		//Used for intermediate steps in drawing tiles
		tileIntermediate=new BufferedImage(tileSize + errMargin*2,tileSize + errMargin*2,BufferedImage.TYPE_INT_ARGB);
		//the dragbuffer must be able to accommodate the whole image at any rotation - use the corner to corner distance
//		int bufferSize = (int)Math.sqrt(w*w + h*h) + errMargin*2;
		//the dragbuffer must be able to accommodate the whole board (plus half a tile on each side) at any rotation
		Dimension s=getPreferredSize();
		int bufferSize=(int)Math.sqrt(s.width*s.width+s.height*s.height)+errMargin*2+tileSize;
		dragBuffer = new BufferedImage(bufferSize,bufferSize,BufferedImage.TYPE_INT_ARGB);
	}

	public void clearBuffers()
	{
		tileSize = -1;
		tileBuffer=null;
		tileMasks=null;
		rotatedMasks=null;
		tileIntermediate=null;
	}

	public void showPreview()
	{
		previewDialog.setVisible(true);
	}

	public void setMeanColor(Color color)
	{
		if(color!=null)
		{
			hsb=Color.RGBtoHSB(color.getRed(),color.getGreen(),color.getBlue(),hsb);
			float b=(hsb[2]<0.35f||hsb[2]>0.65f)?0.5f:hsb[2]+0.15f;
			setBackground(Color.getHSBColor(hsb[0]+0.5f,1.0f,b));
		}
	}

	public void setBackground(Color color)
	{
		backgroundColor=color;
		hsb=Color.RGBtoHSB(color.getRed(),color.getGreen(),color.getBlue(),hsb);
		contrastColor=Color.getHSBColor(hsb[0]+0.5f,1-hsb[1],1-hsb[2]);
	}

	public void chooseColor()
	{
		Color newColor=JColorChooser.showDialog(null, "Choose Puzzle Background", backgroundColor);
		if(newColor!=null)
		{
			setBackground(newColor);
		}
	}
}
