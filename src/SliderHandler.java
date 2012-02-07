/**
 *   Copyright (C) 2010  Jonathan Hulka
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
 * 2011 03 18 - Jon
 * Moved ui into PuzzleHandler (common to all PuzzleHandler implementations)
 * 
 * 2011 02 13 - Jon
 * Refactored to work with PuzzleCanvas modifications (see PuzzleCanvas 2011 02 12)
 * 2010 07 27 - Jon
 * Abstracted tilemanager creation code to NewPuzzleDialog
 * This class now communicates with PuzzleCanvas, rather than GUI
 */

import java.util.Random;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import hulka.tilemanager.*;
import java.io.PrintStream;

/**
 * To do: make sure tilemask is only acquired once
 * To do: init one AffineTransform and reuse it
 */

public class SliderHandler extends PuzzleHandler implements MouseListener
{
	private Random random;
	private Rectangle boardBounds;
	private TileManager tileManager;
	private int missingTile;
	private int tilesAcross;
	private int tileCount;
	private int tileWidth;
	private Rectangle clipRect,puzzleRect,imageRect;
	boolean solved = false;
	
	//Temporary storage to save heap abuse
	Point coords=new Point(0,0);
	int [] neighbors;
	
	public SliderHandler(TileManager manager)
	{
		tileManager=manager;
		random = new Random();
	}

	public void connect(PuzzleCanvas canvas)
	{
		ui=canvas;
		random=new Random();

		boardBounds=ui.getBounds(boardBounds);
		boardBounds.x=0;
		boardBounds.y=0;
		
		imageRect=new Rectangle();
		imageRect.width=tileManager.getBoardWidth();
		imageRect.height=tileManager.getBoardHeight();
		imageRect.x=(boardBounds.width-imageRect.width)/2;
		imageRect.y=(boardBounds.height-imageRect.height)/2;
		
		setupTiles();
		ui.addMouseListener(this);
		redraw();
	}
	
	public void disconnect()
	{
		ui.removeMouseListener(this);
	}

	private boolean checkSolved()
	{
		boolean solved = true;
		for(int i = 0; i < tileCount && solved; i++)
		{
			solved = tileManager.getOriginalTileIndex(i)==i;
		}
		return solved;
	}
	
	public boolean isGameSaved()
	{
		return false;
	}
	
	public boolean isGameComplete()
	{
		return solved;
	}

	private void setupTiles()
	{
		tileCount = tileManager.getTileCount();
		tilesAcross = tileManager.getTilesAcross();
		tileWidth = tileManager.getTileWidth();
		clipRect = new Rectangle(0,0,tileWidth,tileWidth);
		coords = tileManager.getTilePosition(0,coords);
		puzzleRect=new Rectangle(coords.x+imageRect.x,coords.y+imageRect.y,tilesAcross*tileWidth,tilesAcross*tileWidth);
		missingTile=random.nextInt(tileCount);
		mix();
		ui.setBuffers(tileCount,tileWidth,0);
		for(int i=0; i<tileCount; i++)
			initTileImage(i);
	}
	
	private void mix()
	{
		int [] tiles = new int[tileCount];
		for(int i=0; i < tileCount; i++) tiles[i]=i;
		//mix it up
		int cycles = 24/tileCount + 3;
		for(int j = 0 ; j < cycles; j++)
		{
			for(int i=0; i < tileCount; i++)
			{
				int t = random.nextInt(tileCount);
				int temp = tiles[t];
				tiles[t] = tiles[i];
				tiles[i] = temp;
			}
			for(int i=0; i < tiles.length; i++)
			{
				//Slide missingTile to the next random tile position
				coords = moveMissingTileTo(tiles[i],coords);
				//Then slide sideways from the last slide direction, to avoid moving the tiles back again
				if(coords.x==0)
				{
					//get the tile position
					coords = tileManager.getExpandedIndex(tiles[i],coords);
					//adjust tile position, sliding on the x axis
					coords.x += coords.x == 0 ? 1 : coords.x == tilesAcross - 1 ? -1 : random.nextInt(1)*2 - 1;
				}
				else
				{
					//get the tile position
					coords = tileManager.getExpandedIndex(tiles[i],coords);
					//adjust tile position, sliding on the y axis
					coords.y += coords.y == 0 ? 1 : coords.y == tilesAcross - 1 ? -1 : random.nextInt(1)*2 - 1;
				}
				slide(tileManager.getFlatIndex(coords));
			}
		}
		solved=false;
	}
	
	/**
	 * This is a randomizing helper function.
	 * Moves the blank tile to the specified index.
	 * @param tileIndex index of the location to move the blank tile.
	 * @param direction storage for the return value. Must not be null.
	 * @return Direction of the operation's last move.
	 */
	private Point moveMissingTileTo(int tileIndex, Point direction)
	{
		Point nextTile = null;
		//Final position
		Point destination = tileManager.getExpandedIndex(tileIndex,null);
		direction.x = 0;
		direction.y = 0;
		while(missingTile != tileIndex)
		{
			nextTile = tileManager.getExpandedIndex(missingTile,nextTile);
			
			direction.x=0;
			direction.y=0;
			int axis;
			if(nextTile.x == destination.x)
			{
				direction.y = destination.y > nextTile.y ? 1 : -1;
			}
			else
			{
				if(nextTile.y == destination.y)
				{
					direction.x = destination.x > nextTile.x ? 1 : -1;
				}
				else
				{
					if(random.nextInt(2)==0)
					{direction.y = destination.y > nextTile.y ? 1 : -1;}
					else
					{direction.x = destination.x > nextTile.x ? 1 : -1;}
				}
			}
			nextTile.x += direction.x;
			nextTile.y += direction.y;
			slide(tileManager.getFlatIndex(nextTile));
		}
		return direction;
	}
	
	public void redraw()
	{
		ui.erase(boardBounds);
		ui.drawTile(imageRect,null,PuzzleCanvas.DRAW_PUZZLEIMAGE,PuzzleCanvas.DRAW_BACKGROUND);
		ui.erase(puzzleRect);
		ui.clear();
		for(int i = 0; i < tileCount; i++)
		{
			int tileIndex=tileManager.getOriginalTileIndex(i);
			if(missingTile!=tileIndex)
			{
				drawTile(tileIndex);
			}
		}
		ui.repaint();
	}
	
	/**
	 * This function assumes that tileIndex is adjacent to missingTile
	 * if it isn't, the puzzle could end up in an unsolvable state
	 */
	private void slide(int tileIndex)
	{
		tileManager.swap(tileIndex,missingTile);
		missingTile = tileIndex;
	}

	private void initTileImage(int tileIndex)
	{
		Shape mask = tileManager.getTileMask(tileIndex);
		coords = tileManager.getTilePosition(tileIndex,coords);
		ui.setTileMask(tileIndex,mask);
		ui.setTileIndex(tileIndex);
		ui.buildTileImage(coords.x,coords.y,null);
	}
	
	private void drawTile(int tileIndex)
	{
		coords = tileManager.getTilePosition(tileIndex,coords);
		clipRect.x = coords.x + imageRect.x;
		clipRect.y = coords.y + imageRect.y;
		ui.setTileIndex(tileManager.getOriginalTileIndex(tileIndex));
		ui.drawTile(clipRect,null,PuzzleCanvas.DRAW_TILEBUFFER,PuzzleCanvas.DRAW_FOREGROUND);
		ui.repaint(clipRect);
	}
	
	private void clearTile(int tileIndex)
	{
		coords = tileManager.getTilePosition(tileIndex,coords);
		clipRect.x = coords.x+imageRect.x;
		clipRect.y = coords.y+imageRect.y;
		ui.clear(clipRect);
		ui.repaint(clipRect);
	}

	public void mouseClicked(MouseEvent e)
	{
		int tileIndex = tileManager.getTileAt(e.getX()-imageRect.x,e.getY()-imageRect.y);
		if(solved)
		{
			mix();
			redraw();
		}
		else if(tileIndex != -1 && missingTile != tileIndex)
		{
			neighbors = tileManager.getNeighbors(tileIndex,neighbors);
			boolean ok = false;
			//See if the clicked tile can slide
			for(int i = 0; i < neighbors.length && !ok; i++)
			{
				ok = missingTile==neighbors[i];
			}
			if(ok)
			{
				int temp = missingTile;
				slide(tileIndex);
				drawTile(temp);
				if(checkSolved())
				{
					//The puzzle is finished, draw the missing tile
					drawTile(missingTile);
					solved = true;
				}
				else
				{
					//Clear the missing tile
					clearTile(missingTile);
				}
			}
		}
	}
	public void onPreview(){}
	public void mousePressed(MouseEvent e){}
	public void mouseReleased(MouseEvent e){}
	public void mouseEntered(MouseEvent e){}
	public void mouseExited(MouseEvent e){}

	public boolean save(PrintStream out)
	{
		return false;
	}
}
