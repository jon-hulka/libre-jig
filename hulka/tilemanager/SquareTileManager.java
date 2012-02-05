/**
 *   Copyright (C) 2010 - 2012  Jonathan Hulka
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
 * changelog:
 * 2010 07 09 - Jon
 *  - fixed tileHeight calculation in initTilesetDescriptor: changed from boardHeight/tilesAcross to boardHeigh/tilesDown
 */
package hulka.tilemanager;
import java.awt.geom.Point2D;
import java.awt.Point;
import java.awt.Shape;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import java.awt.Dimension;
public class SquareTileManager extends AbstractTileManagerImpl
{
	protected static final int SIDE_COUNT=4;
	protected Point2D.Double [] maskPoints;
	protected TileSetDescriptor descriptor;

	//Temporary storage to avoid excessive heap usage
	private Point tempIndex=new Point(), tempNeighbor=new Point();

	protected static Point [] neighborOffsetIndex = {new Point(0,-1),new Point(1,0),new Point(0,1),new Point(-1,0)};

	public SquareTileManager(int boardWidth, int boardHeight, int tilesAcross, int tilesDown)
	{
		super(boardWidth,boardHeight,tilesAcross,tilesDown,true);
		initMask();
	}
	
	private void initMask()
	{
		AffineTransform rotator = AffineTransform.getRotateInstance(2.0*Math.PI/((double)SIDE_COUNT),((double)descriptor.tileWidth)/2.0,((double)descriptor.tileWidth)/2.0);
		maskPoints=new Point2D.Double[SIDE_COUNT];
//2011.12.03 - maskPoints was never used before, so this error has not been discovered
//		maskPoints[0]=new Point2D.Double(((double)descriptor.tileWidth)/2.0,0.0);
		maskPoints[0]=new Point2D.Double(0.0,0.0);
		for(int i=1; i<SIDE_COUNT; i++)
		{
			maskPoints[i]=(Point2D.Double)rotator.transform(maskPoints[i-1],null);
		}
	}
	public void initTileSetDescriptor(TileSetDescriptor d)
	{
		descriptor=d;
		d.tileWidth=d.boardWidth/d.tilesAcross;
		d.tileHeight=d.boardHeight/d.tilesDown;
		if(d.tileHeight>d.tileWidth){d.tileHeight=d.tileWidth;}else d.tileWidth=d.tileHeight;
		d.tileSpacingX=d.tileWidth;
		d.tileSpacingY=d.tileWidth;
		d.tileCount=d.tilesAcross*d.tilesDown;
		d.rotationSteps=SIDE_COUNT;
		d.sideCount=SIDE_COUNT;
		d.heightWidthRatio=1.0;
		d.tileMargin=0;
	}

	public Point2D.Double getScaledTileCenterOffset(int flatIndex, Point2D.Double offset)
	{
		if(offset==null)offset=new Point2D.Double();
		offset.x=((double)descriptor.tileWidth)/2.0;
		offset.y=((double)descriptor.tileHeight)/2.0;
		return offset;
	}

	public Shape getTileMask(int flatIndex)
	{
		return new Rectangle2D.Double(0,0,descriptor.tileWidth,descriptor.tileHeight);
	}
	
	public Path2D getTileBorder(int flatIndex, int side)
	{
		Path2D.Double theSide = null;
		if(side>=0&&side<SIDE_COUNT)
		{
			theSide = new Path2D.Double();
			theSide.moveTo(maskPoints[side].x,maskPoints[side].y);
			theSide.lineTo(maskPoints[(side+1)%maskPoints.length].x,maskPoints[(side+1)%maskPoints.length].y);
		}
		return theSide;
	}
	
	public Point getBorderLeft(int flatIndex, int direction, Point position)
	{
		if(direction>=0 && direction<SIDE_COUNT)
		{
			if(position==null) position=new Point();
			position.x=(int)maskPoints[direction].x;
			position.y=(int)maskPoints[direction].y;
		}else position=null;
		return position;
	}

	public Point getBorderRight(int flatIndex, int direction, Point position)
	{
		if(direction>=0 && direction<SIDE_COUNT)
		{
			if(position==null) position=new Point();
			position.x=(int)maskPoints[(direction+1)%SIDE_COUNT].x;
			position.y=(int)maskPoints[(direction+1)%SIDE_COUNT].y;
		}else position=null;
		return position;
	}

	public Point getExpandedIndex(int flatIndex, Point expandedIndex)
	{
		if(flatIndex>=0 && flatIndex<descriptor.tileCount)
		{
			if(expandedIndex==null)expandedIndex=new Point();
			expandedIndex.x=flatIndex%descriptor.tilesAcross;
			expandedIndex.y=flatIndex/descriptor.tilesAcross;
		}else expandedIndex=null;
		return expandedIndex;
	}

	public int getFlatIndex(Point tileIndex)
	{
		int result=-1;
		if(tileIndex.x >=0 && tileIndex.x < descriptor.tilesAcross && tileIndex.y >= 0 && tileIndex.y < descriptor.tilesDown)
			result=tileIndex.y*descriptor.tilesAcross + tileIndex.x;
		return result;
	}

	public int getTileAt(int x, int y)
	{
		int result=-1;
		int column = (x - descriptor.leftOffset)/descriptor.tileWidth;
		int row = (y - descriptor.topOffset)/descriptor.tileHeight;
		if(column >=0 && column < descriptor.tilesAcross && row >= 0 && row < descriptor.tilesDown)
		{
			result=column+row*descriptor.tilesAcross;
		}
		return result;
	}

	public int [] getNeighbors(int flatIndex, int [] neighbors)
	{
		if(getExpandedIndex(flatIndex,tempIndex)!=null)
		{
			if(neighbors==null)neighbors=new int[SIDE_COUNT];
			for(int i = 0; i < SIDE_COUNT; i++)
			{
				tempNeighbor.x=tempIndex.x+neighborOffsetIndex[i].x;
				tempNeighbor.y=tempIndex.y+neighborOffsetIndex[i].y;
				neighbors[i]=getFlatIndex(tempNeighbor);
			}
		}else neighbors=null;
		return neighbors;
	}

	public int getNeighborIndex(int flatIndex, Point offset)
	{
		int result=-1;
		getExpandedIndex(flatIndex,tempIndex);
		//Quick sanity check, since neighbors will always be in the same row or within 1 column
		if(offset.x==0||offset.y==0)
		{
			//Find which neighborOffsetIndex this point references
			int i = 0;
			for(; i < SIDE_COUNT && !offset.equals(neighborOffsetIndex[i]); i++);
			result = getNeighborIndex(flatIndex,i);
		}
		return result;
	}
	
	public int getNeighborIndex(int flatIndex, int direction)
	{
		int result=-1;
		if(direction>=0 && direction < SIDE_COUNT)
		{
			//Adjust for rotation
			int index = (direction - rotation[flatIndex]);
			while(index < 0) index += descriptor.rotationSteps;
			index %= SIDE_COUNT;
			tempNeighbor.x=tempIndex.x + neighborOffsetIndex[index].x;
			tempNeighbor.y=tempIndex.y + neighborOffsetIndex[index].y;
			result=getFlatIndex(tempNeighbor);
		}
		return result;
	}

	/**
	 * Calculates a grid size (tilesAcross, tilesDown) that produces as many or more tiles than the requested value.
	 * This function is useful in determining potential values to be passed into the constructor.
	 * @param width Board width
	 * @param height Board height
	 * @param count Number of tiles to calculate for.
	 * @param fitEdgeTiles This is here for consistency, it doesn't apply to square tiles.
	 * @param d Storage for return parameters - if null a new {@link TileDescriptor} will be allocated.
	 * @return A {@link TileSetDescriptor} with boardWidth set to width, boardHeight set to height, tilesAcross and tilesDown and tileCount set to their calculated values.
	 * Note that the resulting tileCount may be larger than the input count value.
	 */
	public static TileSetDescriptor getBestFit(int width, int height, int count, boolean fitEdgeTiles, TileSetDescriptor d)
	{
		if(d==null)d=new TileSetDescriptor();
		d.boardWidth=width;
		d.boardHeight=height;
		d.fitEdgeTiles=fitEdgeTiles;
		d.tilesAcross=(int)Math.sqrt(count*width/height) - 1;
		d.tilesDown = 0;
		d.tileCount=0;
		while(d.tileCount<count)
		{
			d.tilesAcross++;
			int tH = width/d.tilesAcross;
			//Calculate to fit within half a tile either way
			d.tilesDown=(height + tH/2)/tH;
			d.tileCount=d.tilesAcross*d.tilesDown;
		}
		return d;
	}

	/**
	 * For SquareJigsawManager's 'scaling' constructor
	 */
	protected SquareTileManager(TileSetDescriptor oldDescriptor, int boardWidth, int boardHeight)
	{
		super(oldDescriptor,boardWidth,boardHeight);
		initMask();
	}
	public TileSetDescriptor adjustTileSetDescriptor(TileSetDescriptor oldDescriptor, int boardWidth, int boardHeight)
	{
		//Load and save not available for this implementation.
		return null;
	}
}
