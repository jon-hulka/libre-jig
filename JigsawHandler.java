/**
 *   Copyright (C) 2010, 2011  Jonathan Hulka
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
 * Changelog:
 * 
 * 2011 12 22 - Jon
 *  - Fixed a bug in mouseReleased 'snap' loop that was preventing more than two connected sets from snapping together in some cases.
 * 
 * 2011 12 19 - Jon
 *  - Finished implementing save and load functions
 * 
 * 2011 06 06 - Jon
 *  - bug fix: if two edges (three corners) were completed together, drag allowance wasn't permitting the edge of the resulting connected set to be on the board.
 *  - bug fix: Multi-select dragging allowed pieces to 'fall off' the edge and be lost. This was a result of negative width and heights in drag allowance calculations.
 * 
 * 2011 03 18 - Jon
 * Moved ui into PuzzleHandler (common to all PuzzleHandler implementations)
 * 
 * 2011 03 15 - Jon
 * Extended multi-select functionality
 *  - shift-click on tile begins or extends selection
 * 
 * 2011 02 14 - Jon
 * Implemented extended multi-select using shift key.
 *  - drag-select with shift down extends selection
 *  - shift-click on tile extends selection
 * 
 * 2011 02 13 - Jon
 * Reintegrated selection highlighting into tile drawing.
 * 
 * 2011 02 12 - Jon
 * Modified tile rendering code to keep rotated tiles in memory for improved performance.
 * 
 * 2011 02 11 - Jon
 * Added onPreview to fix a user interface issue: simultaneous drag and preview was causing incorrect mouseCount.
 * Implemented tile rotation on click
 * Disabled arrow key scrolling due to interference with tile rotation.
 * 
 * 2011 02 08 - Jon
 * Changed tile snap algorithm: Positions are now adjusted to the largest on-board connected group.
 * 
 * 2010 08 09 - Jon
 * Adjusted for a quirk/bug:
 *   clicking on a menu, then click/dragging on the puzzle canvas fired a mouseUp with no mouseDown, causing mouseCount to become negative
 * 2010 07 08 - Jon
 * debugged second button click issues.
 *   Event handlers now keep a mouse count and only respond to the first mousePressed / last mouseReleased.
 *   As a result, second clicks have no effect. This modifies previous behaviour, where second clicks acted as a mouseReleased.
 * reworked drag constraints.
 *   Dragged tiles are now constrained by a dragAllowance rectangle that ensures each group is at least partly visible.
 * 
 * 2010 07 07 - Jon
 * Implemented layers
 * 
 * 2010 07 06 - Jon
 * Created temp variables for each function (Rectangles and Points).
 * This will require a bit more memory usage than shared temps, but will make future maintenance much easier.
 * 
 * 2010 06 28 - Jon
 * Debugged multi-select
 * 
 * 2010 06 27 - Jon
 * Modified tile dragging - now tiles cannot overlap edge of the board
 * Integrated multi-select for moving tiles
 * 
 * 2010 06 17 - Jon
 * Retrofitted to use ConnectedSet for managing connected tiles and tile snapping.
 * 
 * 2010 06 14 - Jon
 * Worked out tile placement issues. Tiles are now randomly scattered on the board.
 * Added focus request on mousedown.
 * 
 * 2010 06 11 - Jon
 * Abstracted tilemanager creation code to NewPuzzleDialog
 * This class now communicates with PuzzleCanvas, rather than GUI
 */


import java.awt.Dimension;
import java.util.Random;
import java.awt.geom.AffineTransform;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.Point;
import java.awt.Rectangle;
import hulka.event.*;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseEvent;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import hulka.tilemanager.TileManager;
import hulka.tilemanager.SquareJigsawManager;
import hulka.tilemanager.HexJigsawManager;
import hulka.util.ArrayWriter;
import hulka.util.ArrayReader;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.IOException;

import java.util.Arrays;

public class JigsawHandler extends PuzzleHandler implements MouseSensetiveShapeListener, MouseListener, MouseMotionListener, KeyListener
{
//	PuzzleCanvas ui;
	private TileManager tileManager;
	private Random random=new Random();

	//For drawing - extend the size of tiles to prevent clipping
	private int errMargin=2;
	//Distance in pixels for two tiles to 'snap' together
	private int snapThreshold=5;
	private int mouseCount=0;

	//For drawing, tileSize includes margin
	int tileSize, tileMargin;
	//For placement, margin not included
	int tileHeight,tileWidth,tileSpacingX,tileSpacingY;
	
	//Event handling
	private MouseSensetiveShapeManager boardManager;
	private MouseSensetiveTile [] tiles;

	//z-ordering
	private int [] zIndices;
	//
	private ConnectedSet connectedTiles;
	private ConnectedSet selectedTiles;
	
	private int [] layerIndices;
	private int layerCount=3;
	private int currentLayer=0;
	
	private boolean gameComplete=false;
	public boolean isGameComplete(){return gameComplete;}
	private boolean gameSaved=false;
	public boolean isGameSaved(){return gameSaved;}

	private Rectangle boardBounds=null;
	
	//for drag operations
	private int dragIndex=-1;
	private int mouseX,mouseY,adjustedMouseX,adjustedMouseY,dX,dY;
	//used by MouseSensetiveShapeListener event to signal MouseListener event
	private boolean dragging=false;
	//used to signal shift key extended selection
	private boolean extendingSelection=false;
	//boundary of the selected/dragging tileset
	private Rectangle dragBounds=new Rectangle();
	//boundary of the selection rectangle
	private Rectangle selectBounds=new Rectangle();
	//while dragging, this must stay within the play area
	private Rectangle dragAllowance=new Rectangle();
	
	//for multi-select
	private int selectedGroup=-1;

	public JigsawHandler(TileManager tileManager)
	{
		this.tileManager=tileManager;
		tiles = new MouseSensetiveTile [tileManager.getTileCount()];
		tileMargin = tileManager.getTileMargin();

		tileWidth = tileManager.getTileWidth();
		tileHeight = tileManager.getTileHeight();
		tileSpacingX = tileManager.getTileSpacingX();
		tileSpacingY = tileManager.getTileSpacingY();
		tileSize = (tileHeight>tileWidth ? tileHeight : tileWidth) + tileMargin*2;

		boardManager = new MouseSensetiveShapeManager();
	}
	
	public void connect(PuzzleCanvas canvas)
	{
		connect(canvas,true);
	}
	
	/**
	 * Handles both setup cases - new game (tiles need to be initialized), or load game (tiles already initialized)
	 */
	private void connect(PuzzleCanvas canvas, boolean doSetup)
	{
		ui=canvas;
		boardBounds=ui.getBounds(boardBounds);
		boardBounds.x=0;
		boardBounds.y=0;
		if(doSetup)
		{
			setupTiles();
		}
		ui.setBuffers(tileManager.getTileCount(),tileSize,errMargin);
		AffineTransform trans=AffineTransform.getTranslateInstance(tileMargin,tileMargin);
		for(int i=0; i<tiles.length; i++)
		{
			ui.setTileMask(i,trans.createTransformedShape(tileManager.getTileMask(i)));
			initTileImage(i);
			if(layerIndices[i]==currentLayer)boardManager.addShape(tiles[i]);
		}
		redraw();
		boardManager.addEventListener(this);
		ui.addMouseListener(boardManager);
		ui.addMouseListener(this);
		ui.addMouseMotionListener(this);
		ui.addKeyListener(this);
	}
	
	public void disconnect()
	{
		ui.removeMouseListener(this);
		ui.removeMouseListener(boardManager);
		ui.removeMouseMotionListener(this);
		ui.removeKeyListener(this);
		ui.clearBuffers();
	}
	
	public void setLayer(int layer)
	{
		if(layer>=0&&layer<layerCount)
		{
			int sg=dragIndex>=0?dragIndex:selectedGroup>=0?selectedGroup:-1;
			//Mark selected or dragging tiles to be ignored for the moment
			if(sg>=0)
			{
				selectedTiles.setGroup(sg);
				for(int i=selectedTiles.getNext();i>=0;i=selectedTiles.getNext())
				{
					connectedTiles.setGroup(i);
					for(int j=connectedTiles.getNext();j>=0;j=connectedTiles.getNext())
						layerIndices[j]=-1;
				}
				//Don't process the click event for tile rotation
				ignoreClick=dragIndex>=0;
			}

			//remove old current layer tiles from the event manager
			for(int i=0;i<tiles.length;i++)
			{
				if(layerIndices[i]==currentLayer)boardManager.removeShape(tiles[i]);
			}

			currentLayer=layer;
			
			//insert new current layer tiles into the event manager
			for(int i=0;i<tiles.length;i++)
			{
				if(layerIndices[i]==currentLayer)boardManager.addShape(tiles[i]);
			}
			
			//Move selected or dragging tiles to the current layer
			if(sg>=0)
			{
				selectedTiles.setGroup(sg);
				for(int i=selectedTiles.getNext();i>=0;i=selectedTiles.getNext())
				{
					connectedTiles.setGroup(i);
					for(int j=connectedTiles.getNext();j>=0;j=connectedTiles.getNext())
						layerIndices[j]=currentLayer;
				}
			}
			redraw();
		}
	}
	
	private Rectangle rdBounds=null;
	public void redraw()
	{
		ui.erase();

		//Redraw the background
		if(rdBounds==null)rdBounds=new Rectangle(0,0,tileSize+errMargin*2,tileSize+errMargin*2);
		for(int i = zIndices.length - 1; i >= 0; i--)
		{
			if(zIndices[i]>=0 && layerIndices[zIndices[i]]==currentLayer)
			{
				ui.setTileIndex(zIndices[i]);
				rdBounds.x=tiles[zIndices[i]].getX()-errMargin;rdBounds.y=tiles[zIndices[i]].getY()-errMargin;
				ui.drawTile(rdBounds,null,PuzzleCanvas.DRAW_TILEBUFFER,PuzzleCanvas.DRAW_BACKGROUND);
			}
		}
		ui.clear();
		
		//Redraw the selection
		if(dragIndex>=0||selectedGroup>=0)ui.drawTile(dragBounds,null,PuzzleCanvas.DRAW_DRAGBUFFER,PuzzleCanvas.DRAW_FOREGROUND);

		ui.repaint();
	}

	private void setupTiles()
	{
		//Connected sets and index arrays are handled differently for loaded games vs new games - this is for new games
		connectedTiles=new ConnectedSet(tiles.length);
		selectedTiles=new ConnectedSet(tiles.length);
		zIndices = new int[tiles.length];
		layerIndices=new int[tiles.length];

		int rotationSteps  = tileManager.getRotationSteps();
		//initialize layers
		for(int i=0; i < zIndices.length; i++)
		{
			//All tiles to layer 0
			layerIndices[i]=0;
			zIndices[i] = i;
		}
	
		//Randomize the zIndices
		for(int i =0; i < zIndices.length; i++)
		{
			int t = zIndices[i];
			int index = random.nextInt(zIndices.length);
			zIndices[i] = zIndices[index];
			zIndices[index] = t;
		}
		
		int w=boardBounds.width-tileSize-1;
		int h=boardBounds.height-tileSize-1;
		for(int i=0;  i < zIndices.length; i++)
		{
			int x = random.nextInt(w);
			int y = random.nextInt(h);
			tileManager.rotate(zIndices[i],TileManager.SPIN_CW*random.nextInt(rotationSteps));
			tiles[zIndices[i]] = new MouseSensetiveTile(tileManager, zIndices[i], x, y,i,errMargin);
//2011 12 18 - moved into connect()
//			boardManager.addShape(tiles[zIndices[i]]);
		}
//		boardManager.addEventListener(this);
	}
	
	private Point itiPos=new Point();
	private void initTileImages(int tileIndex, ConnectedSet tileSet)
	{
		tileSet.setGroup(tileIndex);
		for(int i=tileSet.getNext(); i>=0; i=tileSet.getNext())initTileImage(i);
	}

	/**
	 * This should only be called at setup and after a tile has been rotated.
	 */
	private void initTileImage(int tileIndex)
	{
		AffineTransform rotation = tileManager.getRotationTransform(tileIndex,new AffineTransform(),errMargin + tileMargin);
		if(tileManager.getOriginalTilePosition(tileIndex, itiPos)!=null)
		{
			ui.setTileIndex(tileIndex);
			//GUI takes into account the errMargin, but adjustment must be made for tileMargin
			ui.buildTileImage(itiPos.x - tileMargin,itiPos.y - tileMargin,rotation);
		}
	}
	
	/**
	 * Places a connected tile group on the board.
	 * @param tileIndex any tile in the connected group.
	 * @param tileSet the ConnectedSet to operate on (such as connectedTiles or selectedTiles)
	 */
	private void dropConnectedTiles(int tileIndex,ConnectedSet tileSet)
	{
		int tileCount=tileSet.getGroupSize(tileIndex);
		//Add connected tiles to z-indices (they will be placed underneath smaller sets)
		int offset=tileSet.insertConnectedElements(tileIndex,zIndices);
		//Adjust z-indices for affected tiles
		for(int i=0;i<tileCount+offset;i++)
		{
			if(zIndices[i]>=0) tiles[zIndices[i]].setZOrder(i);
		}

		//insert the connected tiles into boardManager (mouse events).
		tileSet.setGroup(tileIndex);
		for(int i=tileSet.getNext(); i>=0; i=tileSet.getNext())
		{
			boardManager.addShape(tiles[i]);
		}
	}

	private void copyRect(Rectangle in,Rectangle out){out.x=in.x;out.width=in.width;out.y=in.y;out.height=in.height;}
	private Rectangle gdaTop=new Rectangle();
	private Rectangle gdaBottom=new Rectangle();
	private Rectangle gdaLeft=new Rectangle();
	private Rectangle gdaRight=new Rectangle();
	//Adjust as necessary. Currently one third total width or height.
	private int gdaEdgeFactor=3;
	/**
	 * Ensures that a set of connected tiles can be dragged partly off the display area without hiding all of its tiles.
	 * As long as the returned Rectangle is entirely in the display area, a reasonable piece of the connected set will be visible and available for mouse events.
	 * 
	 * @param tileIndex index of any tile in the connected set.
	 * @param tileSet the ConnectedSet to operate on.
	 * @param bounds bounding box of the tile set.
	 * @param result storage for the return value.
	 * @return bounding box of the connected set.
	 */
	private Rectangle getDragAllowance(int tileIndex,ConnectedSet tileSet,Rectangle bounds,Rectangle result)
	{
		//Adjust as necessary. Currently one half tile width or height.
		int hAllowance=tileWidth/2;
		int vAllowance=tileHeight/2;
//2011 06 06 - fixed bug - if two edges are completed together, drag allowance wasn't permitting the resulting set to be on the board.
//		int hAllowance=bounds.width/2;
//		int vAllowance=bounds.height/2;

		//Set up edge rectangles - at least one tile will intersect each of these
		//Top edge
		copyRect(bounds,gdaTop);
		gdaTop.height/=gdaEdgeFactor;
		//Bottom edge
		copyRect(gdaTop,gdaBottom);
		gdaBottom.y+=bounds.height-gdaTop.height;
		
		//Left edge
		copyRect(bounds,gdaLeft);
		gdaLeft.width/=gdaEdgeFactor;
		//Right edge
		copyRect(gdaLeft,gdaRight);
		gdaRight.x+=bounds.width-gdaLeft.width;
		
		//Start with minimum allowance
		copyRect(bounds,result);

		tileSet.setGroup(tileIndex);
		int bl=result.x;
		int br=result.x+result.width;
		int tl=bl;
		int tr=br;
		int lt=result.y;
		int lb=result.y+result.height;
		int rt=lt;
		int rb=lb;
		for(int i=tileSet.getNext();i>=0;i=tileSet.getNext())
		{
			Rectangle r = tiles[i].getBounds();

			//Figure out the allowance for this tile
			int left=r.x+r.width-hAllowance;
			int right=r.x+hAllowance;
			int top=r.y+r.height-vAllowance;
			int bottom=r.y+vAllowance;

			//See if the improved allowances apply
			if(r.intersects(gdaTop))
			{
				//This tile will be visible if the selection is dragged as far down as possible.
				//Adjust left and right margins accordingly.
				if(left>tl)tl=left;
				if(right<tr)tr=right;
			}
			if(r.intersects(gdaBottom))
			{
				//This tile will be visible if the selection is dragged as far up as possible.
				//Adjust left and right margins accordingly.
				if(left>bl)bl=left;
				if(right<br)br=right;
			}
			if(r.intersects(gdaLeft))
			{
				//This tile will be visible if the selection is dragged as far right as possible.
				//Adjust top and bottom margins accordingly.
				if(top>lt)lt=top;
				if(bottom<lb)lb=bottom;
			}
			if(r.intersects(gdaRight))
			{
				//This tile will be visible if the selection is dragged as far left as possible.
				//Adjust top and bottom margins accordingly.
				if(top>rt)rt=top;
				if(bottom<rb)rb=bottom;
			}
		}
		
		//Choose the region that is guaranteed to show something
		result.y=rt>lt?lt:rt;
		result.height=(rb<lb?lb:rb)-result.y;
		result.x=tl>bl?bl:tl;
		result.width=(tr<br?br:tr)-result.x;
		//Negative width and height will cause problems...
		correctRectangle(result,result);
		return result;
	}

	/**
	 * Removes a connected group of tiles from the board.
	 * @param tileIndex index of any tile in the connected set.
	 * @param tileSet the ConnectedSet to operate on (such as connectedTiles or selectedTiles)
	 * @param result storage for the return value.
	 * @return bounding box of the connected set.
	 */
	private Rectangle pickConnectedTiles(int tileIndex,ConnectedSet tileSet,Rectangle result)
	{
		boolean resultInitialized=false;
		int groupSize=tileSet.getGroupSize(tileIndex);

		result=getConnectedBounds(tileIndex,tileSet,result);
		//remove the connected tiles from the boardManager (mouse events)
		tileSet.setGroup(tileIndex);
		for(int i=tileSet.getNext();i>=0;i=tileSet.getNext())
		{
			boardManager.removeShape(tiles[i]);
		}
		
		//remove the connected tiles from the z-indices (they will be moved to the beginning of the list)
		int endIndex=tileSet.removeConnectedElements(tileIndex,zIndices);
		//Adjust z-indices for tiles still on the board
		for(int i=groupSize;i<endIndex;i++)
		{
			if(zIndices[i]>=0) tiles[zIndices[i]].setZOrder(i);
		}

		return result;
	}
	
	Rectangle dBounds=null;
	/**
	 * Draws tiles within the given region
	 * @param bounds the region to redraw
	 */
	private void draw(Rectangle bounds)
	{
		MouseSensetiveShape [] intersectingTiles = boardManager.getIntersectingShapes(bounds);
		if(dBounds==null)dBounds=new Rectangle(0,0,tileSize+errMargin*2,tileSize+errMargin*2);
		if(intersectingTiles != null)
		{
			for(int j = intersectingTiles.length - 1; j >= 0; j--)
			{
				int tileIndex=intersectingTiles[j].getIndex();
				if(layerIndices[tileIndex]==currentLayer)
				{
					ui.setTileIndex(tileIndex);
					dBounds.x=intersectingTiles[j].getX()-errMargin;dBounds.y=intersectingTiles[j].getY()-errMargin;
					ui.drawTile(dBounds,bounds,PuzzleCanvas.DRAW_TILEBUFFER, PuzzleCanvas.DRAW_BACKGROUND);
				}
			}
		}
	}

	private Rectangle ddbBounds=null;
	private void drawDragBuffer(Rectangle bounds,int tileIndex,ConnectedSet tileSet, boolean selected)
	{
		if(ddbBounds==null)ddbBounds=new Rectangle(0,0,tileSize+errMargin*2,tileSize+errMargin*2);
		tileSet.setGroup(tileIndex);
		for(int i=tileSet.getNext(); i>=0; i=tileSet.getNext())
		{
			ui.setTileIndex(i);
			ddbBounds.x=tiles[i].getX()-bounds.x-errMargin;ddbBounds.y=tiles[i].getY()-bounds.y-errMargin;
			ui.drawTile(ddbBounds,null,PuzzleCanvas.DRAW_TILEBUFFER,PuzzleCanvas.DRAW_DRAGBUFFER);
			if(selected)ui.drawSelected(ddbBounds.x,ddbBounds.y,PuzzleCanvas.DRAW_DRAGBUFFER);
		}
	}

	public void mouseClicked(MouseSensetiveShapeEvent e)
	{}
	
	private boolean isTileSelected(int tileIndex)
	{
		boolean result=false;
		selectedTiles.setGroup(selectedGroup);
		for(int i=selectedTiles.getNext();i>=0&&!result;i=selectedTiles.getNext()) result=connectedTiles.isConnected(tileIndex,i);
		return result;
	}
 
	/**
	 * Indicates a mousedown event on a tile.
	 */
    public void mousePressed(MouseSensetiveShapeEvent e)
    {
		MouseEvent mouseEvent=e.getMouseEvent();
		int tileIndex=((MouseSensetiveTile)((MouseSensetiveShape [])(e.getSource()))[0]).getIndex();
		adjustedMouseX = mouseEvent.getX();
		adjustedMouseY = mouseEvent.getY();
		mouseX=adjustedMouseX;
		mouseY=adjustedMouseY;

		ui.requestFocusInWindow();

		if(mouseCount==0)
		{
			boolean processed=false;
			if(selectedGroup<0 && mouseEvent.isShiftDown())
			{
				//Select a single tile
				setSelection(tileIndex);
				//Prepare to repaint the affected area
				ui.clear(dragBounds);
				//Redraw the foreground (drag) buffer
				ui.drawTile(dragBounds,null,PuzzleCanvas.DRAW_DRAGBUFFER,PuzzleCanvas.DRAW_FOREGROUND);
				ui.repaint(dragBounds);
			}
			if(selectedGroup>=0)
			{
				processed=true;
				if(isTileSelected(tileIndex))
				{
					//Dragging a multi-select
					//Let the other mousePressed handler know that this has been caught
					dragging=true;
				}
				else if(mouseEvent.isShiftDown())
				{
					//Extending a multi-select
					extendingSelection=true;
					//Add a single tile to the selection
					setSelection(tileIndex);
					extendingSelection=false;
					//Prepare to repaint the affected area
					ui.clear(dragBounds);
					//Redraw the foreground (drag) buffer
					ui.drawTile(dragBounds,null,PuzzleCanvas.DRAW_DRAGBUFFER,PuzzleCanvas.DRAW_FOREGROUND);
					ui.repaint(dragBounds);
					//Let the other mousePressed handler know that this has been caught
					dragging=true;
				}
				else
				{
					//Deselecting a multi-select in order to drag a single connected set
					clearSelection();
					processed=false;
				}
			}
			if(!processed)
			{
				//Dragging a single connected set
				dragIndex=tileIndex;
				//Remove connected tiles from the background
				dragBounds=pickConnectedTiles(dragIndex,connectedTiles,dragBounds);
				dragAllowance=getDragAllowance(dragIndex,connectedTiles,dragBounds,dragAllowance);
				//Clean up - draw the background onto the foreground
				ui.erase(dragBounds);
				draw(dragBounds);
				ui.clear(dragBounds);
				ui.clearDragBuffer(dragBounds.width,dragBounds.height);
				drawDragBuffer(dragBounds,dragIndex,connectedTiles,false);	
				ui.drawTile(dragBounds,null,PuzzleCanvas.DRAW_DRAGBUFFER,PuzzleCanvas.DRAW_FOREGROUND);
				//Update the graphics
				ui.repaint(dragBounds);
			}
		}
		e.consume();
	}
    public void mouseReleased(MouseSensetiveShapeEvent e){}
	
	private boolean ignoreClick=false;
	public void mouseClicked(MouseEvent e)
	{
		if(!e.isShiftDown()&&!ignoreClick)
		{
			//Simulate the sequence of events to rotate a puzzle piece
			boardManager.mousePressed(e);
			mousePressed(e);
			keyPressed(new KeyEvent(ui, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, e.getButton()==MouseEvent.BUTTON1?KeyEvent.VK_LEFT:KeyEvent.VK_RIGHT,KeyEvent.CHAR_UNDEFINED));
			mouseReleased(e);
		}
		ignoreClick=false;
	}
	public void mouseEntered(MouseEvent e){}
	public void mouseExited(MouseEvent e){}
	public void mousePressed(MouseEvent e)
	{
		if(mouseCount==0)
		{
			//event not handled by other mousePressed
			if(dragIndex<0)
			{
				if(!dragging&&selectedGroup>=0)
				{
					//Shift key extends the selection
					if(!e.isShiftDown())
					{
						clearSelection();
					}else extendingSelection=true;
				}
				if(selectedGroup<0||extendingSelection)
				{
					selectBounds.x=e.getX();
					selectBounds.y=e.getY();
					selectBounds.width=0;
					selectBounds.height=0;
				}
			}
		}
		mouseCount++;
	}
	
	/**
	 * In case a piece is being dragged when the preview screen comes up.
	 */
	public void onPreview()
	{
		//Finish off any mouse-related operations
		if(mouseCount>0)
		{
			mouseCount=0;
			mouseReleased(new MouseEvent(ui,MouseEvent.MOUSE_RELEASED,System.currentTimeMillis(),0,mouseX,mouseY,1,false));
		}
	}
	
	private Point cnPos=new Point();
	/**
	 * Checks if two tiles are in their correct relative positions, within the limits of the snap threshold
	 * @param tile1 tile to check against
	 * @param tile2 tile to check
	 * @param correction storage for the return value
	 * @return The x and y translation factors required to correct tile2's position relative to tile1
	 */
	private Point checkNeighbors(MouseSensetiveTile tile1, MouseSensetiveTile tile2,Point correction)
	{
		Point result = null;
		//Get pixel offset
		int dX = (tile2.getX() - tile1.getX());
		int signX = dX<0?-1:1;
		//convert to index offset
		dX = (dX + signX*(tileSpacingX/2))/(tileSpacingX);
		
		//Get pixel offset
		int dY = (tile2.getY() - tile1.getY());
		int signY = dY<0?-1:1;
		//Convert to index offset
		dY = (dY + signY*(tileSpacingY/2))/(tileSpacingY);
		//Check if it is the correct neighbor for its position
		cnPos.x=dX;cnPos.y=dY;
		int index = tileManager.getNeighborIndex(tile1.getTileIndex(),cnPos);
		int index2 = tile2.getTileIndex();
		if(index >=0 && index==index2 && tileManager.getRotation(tile1.getTileIndex()) == tileManager.getRotation(index2))
		{
			//There is a neighbor on that side, it's index matches, and rotation is the same
			int adjustX = tile2.getX() - dX*tileSpacingX - tile1.getX();
			int adjustY = tile2.getY() - dY*tileSpacingY - tile1.getY();
			//Check if the tiles are close enough to 'snap'
			if(-snapThreshold < adjustX && adjustX < snapThreshold && -snapThreshold < adjustY && adjustY < snapThreshold)
			{
				result=correction;
				result.x=adjustX;
				result.y=adjustY;
			}
		}
		return result;
	}
	
	/**
	 * Adjusts the position of a connected group of tiles.
	 * @param tileSet the ConnectedSet to operate on (such as connectedTiles or selectedTiles)
	 * @param group index of any tile in the connected group.
	 * @param offset adjustment value.
	 */
	private void adjustTiles(ConnectedSet tileSet, int group, Point offset)
	{
		tileSet.setGroup(group);
		for(int i=tileSet.getNext();i>=0;i=tileSet.getNext())
		{
			tiles[i].moveTo(tiles[i].getX()+offset.x,tiles[i].getY()+offset.y);
		}
	}

	//Since this function will be called so often, keep its variables around.
	private Rectangle mrBounds=new Rectangle();
	private Rectangle mrBounds2=new Rectangle();
	private Point mrPos=new Point();
	private Point mrAnchor=new Point();
	public void mouseReleased(MouseEvent e)
	{
		mouseCount--;
		//There is a quirk:
		//If you click on a menu, then click/drag on the puzzle canvas, the mouseDown event is not fired, but the mouseUp is, causing mouseCount to be less than zero
		if(mouseCount<=0)
		{
			mouseCount=0;
			int tileCount;
			if(dragIndex>=0)
			{
				selectedTiles.reset();
				mrBounds=getConnectedBounds(dragIndex,connectedTiles,mrBounds);
				//Check each connected tile for neighbors
				int avgXOffset=0;
				int avgYOffset=0;
				connectedTiles.setGroup(dragIndex);
				Point snapOffset=null;
				//Loop through the whole connected set
				for(int i=connectedTiles.getNext(); i>=0; i=connectedTiles.getNext())
				{
					MouseSensetiveShape [] intersectingTiles = boardManager.getIntersectingShapes(tiles[i].getBounds());
					if(intersectingTiles != null)
					{
						//Start with the largest connected group
						for(int j=intersectingTiles.length-1; j>=0; j--)
						{
							//Find out if this tile lines up as a neighbor
							Point adjustment = checkNeighbors(tiles[i],(MouseSensetiveTile)intersectingTiles[j],mrPos);
							if(adjustment != null)
							{
								int jIndex=((MouseSensetiveTile)intersectingTiles[j]).getIndex();
								//Remove the tile and its neighbors from the board
								mrBounds.add(pickConnectedTiles(jIndex,connectedTiles,mrBounds2));

								//Snap everything to the largest set
								if(snapOffset==null)
								{
									mrAnchor.x=mrPos.x;
									mrAnchor.y=mrPos.y;
									snapOffset=mrAnchor;
									//the dragIndex group is not adjusted here because it would interfere with the outer loop connectedTiles.getNext() operation
								}
								else
								{
									adjustment.x=snapOffset.x-adjustment.x;
									adjustment.y=snapOffset.y-adjustment.y;
									//This will mess up the outer loop connectedTiles.getNext() operation...
									adjustTiles(connectedTiles,jIndex,adjustment);
									//... so restore it here - go back to the beginning...
									connectedTiles.setGroup(dragIndex);
									//... and step through to the current position
									for(int tmp=connectedTiles.getNext(); tmp>=0 && tmp!=i; tmp=connectedTiles.getNext());
								}

								//Add the tile and its neighbors to the list of tile sets that will be snapped together
								//selectedTiles is not being used for anything else at the moment...
								selectedTiles.connect(dragIndex,jIndex);
								if(j > 0)
								{
									//Reevaluate intersecting tiles - some may no longer be on the board
									intersectingTiles = boardManager.getIntersectingShapes(tiles[dragIndex].getBounds());
									j = intersectingTiles==null ? 0 : intersectingTiles.length;
								}
							}
						}
					}
				}

				//Adjust the dragIndex group
				if(snapOffset!=null)adjustTiles(connectedTiles,dragIndex,snapOffset);
				//snap the sets together
				int prevSet=-1;
				selectedTiles.setGroup(dragIndex);
				for(int i=selectedTiles.getNext(); i>=0; i=selectedTiles.getNext())
				{
					if(prevSet >=0) connectedTiles.connect(prevSet,i);
					prevSet = i;
				}
				
				tileCount=connectedTiles.getGroupSize(dragIndex);

				if(tileCount==tiles.length)
				{
					finishGame();
				}
				else
				{
					//Put the tiles back on the board
					dropConnectedTiles(dragIndex,connectedTiles);
					ui.erase(mrBounds);
					draw(mrBounds);
					ui.clear(mrBounds);
					ui.repaint(mrBounds);
					dragIndex=-1;
				}
			}
			else if(selectedGroup<0||extendingSelection)
			{
				//Multi-select
				mrBounds=correctRectangle(selectBounds,mrBounds);
				//Select tiles within the drag area
				setSelection(mrBounds);
				//Make sure the border is erased
				mrBounds.x-=1;mrBounds.y-=1;mrBounds.width+=2;mrBounds.height+=2;
				mrBounds.add(dragBounds);
				//Prepare to repaint the affected area
				ui.clear(mrBounds);
				//Redraw the foreground (drag) buffer
				ui.drawTile(dragBounds,null,PuzzleCanvas.DRAW_DRAGBUFFER,PuzzleCanvas.DRAW_FOREGROUND);
				ui.repaint(mrBounds);
			}
			//reset the multi-select drag signal
			dragging=false;
			//reset the shift key extend signal
			extendingSelection=false;
		}
	}
	
	private Rectangle ssDragBounds=new Rectangle();
	private Rectangle ssDragAllowance=new Rectangle();
	/**
	 * sets or extends the selection
	 * dragBounds will be modified
	 * Selection is different from basic dragging
	 *  - The selected tiles remain "off" the board (in the foreground) between drags
	 *  - Event handling is not disabled for the selected tiles
	 */
	private void setSelection(Rectangle bounds)
	{
		int z=setSelectionPre();
		boolean boundsSet=z>=0;

		MouseSensetiveShape [] intersectingTiles=boardManager.getIntersectingShapes(bounds);
		if(intersectingTiles!=null) for(int i=intersectingTiles.length-1; i>=0; i--)
		{
			//Add connected group to the selection
			int index=((MouseSensetiveTile)intersectingTiles[i]).getIndex();
			z+=setSelectionMid(index,boundsSet);
			boundsSet=z>=0;

			if(i > 0)
			{
				//Reevaluate intersecting tiles - some may no longer be on the board
				intersectingTiles = boardManager.getIntersectingShapes(bounds);
				i=intersectingTiles==null?0:intersectingTiles.length;
			}
		}
		
		setSelectionPost(z);
	}
	
	/**
	 * Adds a single connected group to the selection.
	 */
	private void setSelection(int tileIndex)
	{
		int z=setSelectionPre();
		boolean boundsSet=z>=0;
		z+=setSelectionMid(tileIndex,boundsSet);
		boundsSet=z>=0;
		setSelectionPost(z);
	}
	
	/**
	 * Common setup code for setSelection functions
	 * @return the number of tiles currently selected.
	 */
	private int setSelectionPre()
	{
		int z=-1;
		if(extendingSelection)
		{
			//Retrieve previous bounds 2011 06 06 - removed this, does it serve a purpose
//			copyRect(ssDragBounds,dragBounds);
//			copyRect(ssDragAllowance,dragAllowance);
			//Remove already selected tiles from the boardManager to avoid re-processing
			selectedTiles.setGroup(selectedGroup);
			for(int i=selectedTiles.getNext();i>=0;i=selectedTiles.getNext())
			{
				z+=connectedTiles.getGroupSize(i);
				//remove the connected tiles
				connectedTiles.setGroup(i);
				for(int j=connectedTiles.getNext();j>=0;j=connectedTiles.getNext())
				{
					boardManager.removeShape(tiles[j]);
				}
			}
		}
		else
		{
			selectedTiles.reset();
			selectedGroup=-1;
		}
		return z;
	}
	
	/**
	 * Common code for setSelection functions
	 * @param index index of connected tile set to add to selection
	 * @param boundsSet indicates whether the bounds variables have been initialized (tiles already selected)
	 * @return z number of tiles added to the selection
	 */
	private int setSelectionMid(int index,boolean boundsSet)
	{
		int z=connectedTiles.getGroupSize(index);
		if(selectedGroup<0){selectedGroup=index;}
		else{selectedTiles.connect(index,selectedGroup);}

		//Remove connected group from the board and calculate bounds
		if(!boundsSet)
		{
			dragBounds=pickConnectedTiles(index,connectedTiles,dragBounds);
			dragAllowance=getDragAllowance(index,connectedTiles,dragBounds,dragAllowance);
			boundsSet=true;
		}
		else
		{
			ssDragBounds=pickConnectedTiles(index,connectedTiles,ssDragBounds);
			dragBounds.add(ssDragBounds);
			ssDragAllowance=getDragAllowance(index,connectedTiles,ssDragBounds,ssDragAllowance);
			dragAllowance.add(ssDragAllowance);
		}
		return z;
	}
	
	/**
	 * Common cleanup code for setSelection functions
	 * @param z number of currently selected tiles
	 */
	 private void setSelectionPost(int z)
	 {
		//Redraw the play area with selected tiles removed
		ui.erase(dragBounds);
		draw(dragBounds);
		//Set up the drag buffer
		ui.clearDragBuffer(dragBounds.width,dragBounds.height);
		selectedTiles.setGroup(selectedGroup);
		for(int i=selectedTiles.getNext();i>=0;i=selectedTiles.getNext())
		{
			//Draw each selected tileset to the drag buffer
			drawDragBuffer(dragBounds,i,connectedTiles,true);
			connectedTiles.setGroup(i);
			for(int j=connectedTiles.getNext();j>=0;j=connectedTiles.getNext())
			{
				//Adjust z-order for tiles removed from the board (all selected tiles)
				tiles[j].setZOrder(z--);
				//Re-enable events
				boardManager.addShape(tiles[j]);
			}
		}
		//Keep drag bounds in case of extended selection
		copyRect(dragBounds,ssDragBounds);
		copyRect(dragAllowance,ssDragAllowance);
	}

	private void clearSelection()
	{
		selectedTiles.setGroup(selectedGroup);
		//Process each connected group
		for(int i=selectedTiles.getNext();i>=0;i=selectedTiles.getNext())
		{
			connectedTiles.setGroup(i);
			for(int j=connectedTiles.getNext();j>=0;j=connectedTiles.getNext())
			{
				boardManager.removeShape(tiles[j]);
			}
			//Place the tile back on the board
			dropConnectedTiles(i,connectedTiles);
		}
		ui.erase(dragBounds);
		draw(dragBounds);
		ui.clear(dragBounds);
		ui.repaint(dragBounds);
		selectedGroup=-1;
	}
	
	/**
	 * Displays the finished puzzle right side up and centered.
	 */
	private void finishGame()
	{
		Rectangle bounds=new Rectangle();
		ui.removeMouseListener(this);
		ui.removeMouseMotionListener(this);
		gameComplete=true;
		int rs=tileManager.getRotationSteps();
		int rotationSteps=tileManager.getRotationCount(0);
		bounds=getConnectedBounds(0,connectedTiles,bounds);

		adjustedMouseX=bounds.x;
		adjustedMouseY=bounds.y;
		if(rotationSteps>0) moveTiles(0,connectedTiles,adjustedMouseX,adjustedMouseY,TileManager.SPIN_CCW*rotationSteps);

		//In case someone solves the puzzle sideways
		//rerender rotated tiles
		if(rotationSteps!=0)initTileImages(0,connectedTiles);

		dragBounds=getConnectedBounds(0,connectedTiles,dragBounds);
		ui.clearDragBuffer(dragBounds.width,dragBounds.height);
		drawDragBuffer(dragBounds,0,connectedTiles,false);

		dragBounds.x=(boardBounds.width-tileManager.getBoardWidth())/2;
		dragBounds.y=(boardBounds.height-tileManager.getBoardHeight())/2;

		ui.erase(bounds);
		ui.clear(bounds);

		ui.drawTile(dragBounds,null,PuzzleCanvas.DRAW_DRAGBUFFER,PuzzleCanvas.DRAW_FOREGROUND);

		bounds.add(dragBounds);
		ui.repaint(bounds);
	}
	
	
	/**
	 * Returns the bounding box of the connected set
	 * @param tileIndex index of a tile belonging to the connected set
	 * @param tileSet ConnectedSet defining the group of tiles
	 * @param result storage for the return value
	 */
	private Rectangle getConnectedBounds(int tileIndex,ConnectedSet tileSet,Rectangle result)
	{
		boolean resultInitialized=false;
		tileSet.setGroup(tileIndex);
		//Find the bounding box of the connected set
		for(int i=tileSet.getNext(); i>=0; i=tileSet.getNext())
		{
			Rectangle bounds = tiles[i].getBounds();
			if(result!=null)
			{
				if(resultInitialized)
				{
					result.add(bounds);
				}
				else
				{
					resultInitialized=true;
					copyRect(bounds,result);
				}
			}
		}
		if(result!=null)
		{
			result.x -= errMargin;
			result.y -= errMargin;
			result.width += errMargin*2;
			result.height += errMargin*2;
		}
		return result;
	}
	
	private Rectangle mdBounds=new Rectangle();
	private Rectangle mdBounds2=new Rectangle();
	public void mouseDragged(MouseEvent e)
	{
		ignoreClick=false;
		mouseX = e.getX();
		mouseY = e.getY();
		if(dragIndex>=0)
		{
			//Move connected group
			copyRect(dragBounds,mdBounds);
			moveTiles(dragIndex,connectedTiles,mouseX,mouseY,0);
			ui.clear(mdBounds);
			ui.drawTile(dragBounds,null,PuzzleCanvas.DRAW_DRAGBUFFER,PuzzleCanvas.DRAW_FOREGROUND);
			mdBounds.add(dragBounds);
			ui.repaint(mdBounds);
		}
		else if(selectedGroup>=0&&!extendingSelection)
		{
			//Move multi-selection
			copyRect(dragBounds,mdBounds);
			adjustDragCoords(mouseX,mouseY);
			moveSelectedTiles(dX,dY);
			ui.clear(mdBounds);
			ui.drawTile(dragBounds,null,PuzzleCanvas.DRAW_DRAGBUFFER,PuzzleCanvas.DRAW_FOREGROUND);
			mdBounds.add(dragBounds);
			ui.repaint(mdBounds);
		}
		else
		{
			//Adjust multi-select rectangle
			mdBounds=correctRectangle(selectBounds,mdBounds);
			mdBounds.x-=1;mdBounds.y-=1;mdBounds.width+=2;mdBounds.height+=2;
			selectBounds.width=mouseX-selectBounds.x;
			selectBounds.height=mouseY-selectBounds.y;
			mdBounds2=correctRectangle(selectBounds,mdBounds2);
			ui.clear(mdBounds);
			ui.drawRect(mdBounds2,PuzzleCanvas.DRAW_FOREGROUND);
			mdBounds2.x-=1;mdBounds2.y-=1;mdBounds2.width+=2;mdBounds2.height+=2;
			mdBounds.add(mdBounds2);
			if(selectedGroup>=0)ui.drawTile(dragBounds,mdBounds,PuzzleCanvas.DRAW_DRAGBUFFER,PuzzleCanvas.DRAW_FOREGROUND);
			ui.repaint(mdBounds);
		}
	}
	
	private Point mtPoint=new Point();
	/**
	 * Positions and rotates a set of tiles, adjusting the dragbuffer and dragbounds accordingly
	 */
	private void moveTiles(int tileIndex,ConnectedSet tileSet,int x,int y,int rotateDirection)
	{
		if(rotateDirection!=0)
		{
			double rotationStep = tileManager.getRotationStep();
			//Rotate around the center of the selection
			AffineTransform rot = AffineTransform.getRotateInstance(rotateDirection*rotationStep,adjustedMouseX,adjustedMouseY);
			
			int offs=tileSize/2 + errMargin;
			tileSet.setGroup(tileIndex);
			for(int i=tileSet.getNext(); i>=0; i=tileSet.getNext())
			{
				tileManager.rotate(tiles[i].getTileIndex(),rotateDirection);
				mtPoint.x=tiles[i].getX() + offs;mtPoint.y=tiles[i].getY() + offs;
				rot.transform(mtPoint,mtPoint);
				tiles[i].moveTo(mtPoint.x - offs,mtPoint.y - offs);
			}

			int x1 = tiles[tileIndex].getX();
			int y1 = tiles[tileIndex].getY();
			//Realign tiles to fix rotation error
			tileSet.setGroup(tileIndex);
			boolean boundsSet=false;
			for(int i=tileSet.getNext(); i>=0; i=tileSet.getNext())
			{
				int x2 = tiles[i].getX();
				int y2 = tiles[i].getY();
				int dX = (x2 - x1 + (x2>x1 ? 1 : -1)*tileSpacingX/2)/tileSpacingX;
				int dY = (y2 - y1 + (y2>y1 ? 1 : -1)*tileSpacingY/2)/tileSpacingY;
				tiles[i].moveTo(x1 + dX*tileSpacingX,y1 + dY*tileSpacingY);
			}
			initTileImages(tileIndex,tileSet);
			dragBounds=getConnectedBounds(tileIndex,tileSet,dragBounds);
			dragAllowance=getDragAllowance(tileIndex,tileSet,dragBounds,dragAllowance);
			ui.clearDragBuffer(dragBounds.width+errMargin*2,dragBounds.height+errMargin*2);
			drawDragBuffer(dragBounds,dragIndex,connectedTiles,false);
		}

		adjustDragCoords(x,y);

		tileSet.setGroup(tileIndex);
		for(int i=tileSet.getNext(); i>=0; i=tileSet.getNext())
		{
			tiles[i].moveTo(tiles[i].getX() + dX, tiles[i].getY() + dY);
		}
	}
	
	/**
	 * Moves multi-selected tiles.
	 */
	 private void moveSelectedTiles(int dX, int dY)
	 {
		selectedTiles.setGroup(selectedGroup);
		for(int i=selectedTiles.getNext();i>=0;i=selectedTiles.getNext())
		{
			connectedTiles.setGroup(i);
			for(int j=connectedTiles.getNext();j>=0;j=connectedTiles.getNext())
			{
				boardManager.removeShape(tiles[j]);
				tiles[j].moveTo(tiles[j].getX() + dX, tiles[j].getY() + dY);
				boardManager.addShape(tiles[j]);
			}
		}
	 }
	private void adjustDragCoords(int x, int y)
	{
		int minX=adjustedMouseX-dragAllowance.x;
		int minY=adjustedMouseY-dragAllowance.y;
		int maxX=boardBounds.width+adjustedMouseX-dragAllowance.x-dragAllowance.width;
		int maxY=boardBounds.height+adjustedMouseY-dragAllowance.y-dragAllowance.height;
		x = x<minX?minX:x<maxX?x:maxX;
		y = y<minY?minY:y<maxY?y:maxY;
		dX = x - adjustedMouseX;
		dY = y - adjustedMouseY;
		adjustedMouseX = x;
		adjustedMouseY = y;
		dragBounds.x+=dX;
		dragBounds.y+=dY;
		dragAllowance.x+=dX;
		dragAllowance.y+=dY;
		//For multi-select (setSelection)
		ssDragBounds.x+=dX;
		ssDragBounds.y+=dY;
		ssDragAllowance.x+=dX;
		ssDragAllowance.y+=dY;
	}

	/**
	 * Rearranges corners so width and height are positive
	 */
	private Rectangle correctRectangle(Rectangle toCorrect,Rectangle result)
	{
		int x=(toCorrect.width<0?toCorrect.x+toCorrect.width:toCorrect.x)-1;
		int width=(toCorrect.width<0?toCorrect.width*-1:toCorrect.width)+2;
		int y=(toCorrect.height<0?toCorrect.y+toCorrect.height:toCorrect.y)-1;
		int height=(toCorrect.height<0?toCorrect.height*-1:toCorrect.height)+2;
		result.x=x;
		result.y=y;
		result.width=width;
		result.height=height;
		return result;
	}


	public void mouseMoved(MouseEvent e){}
	
	private Rectangle kpBounds=new Rectangle();
	public void keyPressed(KeyEvent e)
	{
		int keyCode=e.getKeyCode();
		switch(keyCode)
		{
			case KeyEvent.VK_LEFT:
			case KeyEvent.VK_RIGHT:
				if(dragIndex>=0)
				{
					copyRect(dragBounds,kpBounds);
					moveTiles(dragIndex,connectedTiles,mouseX,mouseY,keyCode==KeyEvent.VK_LEFT?TileManager.SPIN_CCW:TileManager.SPIN_CW);
					ui.clear(kpBounds);
					ui.drawTile(dragBounds,null,PuzzleCanvas.DRAW_DRAGBUFFER,PuzzleCanvas.DRAW_FOREGROUND);
					kpBounds.add(dragBounds);
					ui.repaint(kpBounds);
				}
			case KeyEvent.VK_UP:
			case KeyEvent.VK_DOWN:
				//Disable scrolling for arrow keys - just change the event codes
				e.setKeyCode(KeyEvent.VK_UNDEFINED);
				e.setKeyChar(KeyEvent.CHAR_UNDEFINED);
				break;
		}
	}

	public void keyReleased(KeyEvent e){}
	
	public void keyTyped(KeyEvent e){}

	public boolean save(PrintWriter out, PrintWriter err)
	{
		boolean result=true;
		//This is for PuzzleHandler.load to identify puzzle type
		out.println("JigsawHandler");

		//Save the TileManager
		if(tileManager instanceof SquareJigsawManager)
		{
			out.println("SquareJigsawManager");
			result = ((SquareJigsawManager)tileManager).save(out,err);
		}
		else if(tileManager instanceof HexJigsawManager)
		{
			out.println("HexJigsawManager");
			result = ((HexJigsawManager)tileManager).save(out,err);
		}
		else result = false;

		if(result)
		{
			int [][] data = new int[5][];
			for(int i=0; i<3; i++)
			{
				data[i]=new int[tiles.length];
			}
			String [] names = {"x","y","rotation","layer","zIndex"};
			for(int i=0; i<tiles.length; i++)
			{
				data[0][i]=tiles[i].getX();
				data[1][i]=tiles[i].getY();
				data[2][i]=tileManager.getRotationCount(i);
			}
			data[3]=layerIndices;
			data[4]=zIndices;
			result = new ArrayWriter(5,tiles.length,"JigsawHandler").save(data,names,out,err);
		}

		//Save connected sets
		if(result) result = connectedTiles.save(out,err);

		return result;

/*
		boolean result=true;
		//Identify to PuzzleHandler's load function
		out.println("JigsawHandler");

		//Save the TileManager
		if(tileManager instanceof SquareJigsawManager)
		{
			result = ((SquareJigsawManager)tileManager).save(out,err);
		}
		else if(tileManager instanceof HexJigsawManager)
		{
			result = ((HexJigsawManager)tileManager).save(out,err);
		}
		else result = false;
		
		//Save tile properties
		if(result)
		{
			//Selected tiles are being saved with incorrect coordinates - for now, just clear the selection
			//@todo see if this can be addressed more elegantly (??? save - clear - restore ???)
			clearSelection();
			out.println("i:x:y:rotation:layer:zIndex");
			for(int i=0; i<tiles.length; i++)
			{
				out.println(i+":"+tiles[i].getX()+":"+tiles[i].getY()+":"+tileManager.getRotationCount(i)+":"+layerIndices[i]+":"+zIndices[i]);
			}
		}
		
		//Save connected sets
		if(result) result = connectedTiles.save(out,err);

		return result;
*/
	}

	public static JigsawHandler load(PuzzleCanvas boardCanvas, BufferedReader in, PrintWriter err)
	{
		JigsawHandler result=null;
		ArrayReader reader=null;
		Dimension boardSize=boardCanvas.getSize();
		
		//Default scale factor - This will be used if the puzzle is saved, then loaded at a different screen resolution.
		double scaleFactor=1.0;

		TileManager tileManager=null;
		String managerType=null;
		try
		{
			managerType=in.readLine();
			if(managerType==null)
			{
				err.println("JigsawHandler.load(): Unexpected end of file.");
			}
		}
		catch(IOException ex)
		{
			managerType=null;
			err.println("JigsawHandler.load(): " + ex.getMessage());
		}
		
		if("HexJigsawManager".equals(managerType))
		{
			tileManager=HexJigsawManager.load(in,err,boardSize);
		}
		else if("SquareJigsawManager".equals(managerType))
		{
			tileManager=SquareJigsawManager.load(in,err,boardSize);
		}
		else
		{
			err.println("Tile shape unknown or missing.");
		}
		
		if(tileManager!=null)
		{
			reader=new ArrayReader("JigsawHandler");
			if(reader.load(in,err))
			{
				result=new JigsawHandler(tileManager);
			}
		}
		
		if(result!=null)
		{
			result.selectedTiles = new ConnectedSet(result.tiles.length);
			result.zIndices=null;
			result.layerIndices=null;
			int [] x = null;
			int [] y = null;
			int [] rotation = null;
			
			if(result!=null)
			{
				result.zIndices = reader.getColumn("zIndex",err);
				if(result.zIndices==null) result=null;
			}

			if(result!=null)
			{
				result.layerIndices = reader.getColumn("layer",err);
				if(result.layerIndices==null) result=null;
			}
			
			if(result != null)
			{
				x = reader.getColumn("x",err);
				if(x==null) result=null;
			}

			if(result != null)
			{
				y = reader.getColumn("y",err);
				if(y==null) result=null;
			}

			if(result != null)
			{
				rotation = reader.getColumn("rotation",err);
				if(rotation==null) result=null;
			}

			if(result!=null && x.length != result.tiles.length)
			{
				result=null;
				err.println("JigsawHandler.load(): expected " + result.tiles.length + " rows, found " + x.length + ".");
			}
			
			if(result != null)
			{
//todo scale x and y here (if necessary) to fit scaled tile sizes - this has to happen when scaling is implemented for JigsawManagers
// - Find upper, lower, left, and right bounds of each connected set
// - Scale the center point
				for(int i=0; i<result.tiles.length; i++)
				{
					//Set up tiles
					result.tiles[i]=new MouseSensetiveTile(tileManager, i,
						x[i], //x
						y[i], //y
						-1, //z-index is reverse indexed, it will be handled later
						result.errMargin);
					tileManager.rotate(i,TileManager.SPIN_CW*rotation[i]); //rotation
				}
				//Now do z-index
				for(int i=0; result!=null && i<result.zIndices.length; i++)
				{
					result.tiles[result.zIndices[i]].setZOrder(i);
				}
			}
		}

		if(result!=null)
		{
			//Load connected sets
			ConnectedSet connectedTiles=ConnectedSet.load(in,err);
			if(connectedTiles!=null)
			{
				result.connectedTiles=connectedTiles;
			}
			else
			{
				result=null;
			}
		}
//todo clean up connected set scaling errors here (if scaling is necessary)

		if(result!=null)
		{
			result.connect(boardCanvas,false);
		}
		return result;
	}

}
