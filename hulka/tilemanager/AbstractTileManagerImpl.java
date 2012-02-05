/**
 *   Copyright (C) 2010 - 2012 Jonathan Hulka (jon.hulka@gmail.com)
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
 * 2012 02 02 - Jon
 *  - Added support for scaling a puzzle's dimensions when loading on a different screen resolution.
 * 
 */
package hulka.tilemanager;
import java.awt.geom.Point2D;
import java.awt.Point;
import java.awt.geom.Path2D;
import java.awt.geom.AffineTransform;

public abstract class AbstractTileManagerImpl implements TileManager
{
	private TileSetDescriptor descriptor;
	protected double rotationStep;
	/**
	 * Maps tiles to their original locations
	 */
	protected int [] originalIndex;
	protected int [] rotation;

	//Temporary storage to avoid excessive heap usage
	private Point tempIndex=new Point(), tempNeighbor=new Point();
	protected Point2D.Double center=new Point2D.Double(), originalCenter=new Point2D.Double();

	/**
	 * Provides a means to communicate basic tileset information to the constructor.
	 * The descriptor will be initialized with 'best guess' values and passed to the implementing object for fine-tuning.
	 * Properties of descriptor that have been pre-set:
	 *  - {@link TileSetDescriptor#boardWidth}
	 *  - {@link TileSetDescriptor#boardHeight}
	 *  - {@link TileSetDescriptor#tilesAcross}
	 *  - {@link TileSetDescriptor#tilesDown}
	 *  - {@link TileSetDescriptor#fitEdgeTiles}
	 * Properties of descriptor to be set:
	 *  - {@link TileSetDescriptor#tileHeight}
	 *  - {@link TileSetDescriptor#tileWidth}
	 *  - {@link TileSetDescriptor#tileSpacingX}
	 *  - {@link TileSetDescriptor#tileSpacingY}
	 *  - {@link TileSetDescriptor#tileMargin}
	 *  - {@link TileSetDescriptor#tileCount}
	 *  - {@link TileSetDescriptor#rotationSteps}
	 *  - {@link TileSetDescriptor#sideCount}
	 *  - {@link TileSetDescriptor#heightWidthRatio}
 	 * @param descriptor The {@link TileSetDescriptor} to initialize.
	 */
	public abstract void initTileSetDescriptor(TileSetDescriptor descriptor);
	public abstract Point2D.Double getScaledTileCenterOffset(int flatIndex, Point2D.Double offset);
	public abstract Point getExpandedIndex(int flatIndex, Point expandedIndex);

	/**
	 * Provides a way to scale a tileset loaded at a different resolution than it was saved at.
	 * All properties except scaleFactor, leftOffset and topOffset should be set up.
	 * @param boardWidth Width of the image.
	 * @param boardHeight Height of the image.
	 * @return {@link TileSetDescriptor} describing the adjusted dimensions. If boardWidth and boardHeight are not changed, oldDescriptor should be returned.
	 */
	public abstract TileSetDescriptor adjustTileSetDescriptor(TileSetDescriptor oldDescriptor, int boardWidth, int boardHeight);

	/**
	 * This constructor is used when loading from a file. It adjusts for display size differences.
	 * Currently supported by (Hex|Square)JigsawManager.
	 * @param oldDescriptor The descriptor at the saved resolution.
	 * @param boardWidth Width of the tile set, in pixels.
	 * @param boardHeight Height of the tile set, in pixels.
	 */
	protected AbstractTileManagerImpl(TileSetDescriptor oldDescriptor, int boardWidth, int boardHeight)
	{
		TileSetDescriptor newDescriptor=null;
		descriptor=adjustTileSetDescriptor(oldDescriptor,boardWidth,boardHeight);
		//Check if a scaling adjustment was made
		if(descriptor!=oldDescriptor)
		{
			//Calculate the error in the height/width ratio
			descriptor.scaleFactor=((double)descriptor.tileHeight)/((double)descriptor.tileWidth)/descriptor.heightWidthRatio;
			//Centre the the tileset
			descriptor.leftOffset = (descriptor.boardWidth - descriptor.tilesAcross*descriptor.tileSpacingX - descriptor.tileWidth + descriptor.tileSpacingX)/2;
			descriptor.topOffset = (descriptor.boardHeight - descriptor.tilesDown*descriptor.tileSpacingY - descriptor.tileHeight + descriptor.tileSpacingY)/2;
		}
		rotationStep=2.0*Math.PI/((double)descriptor.rotationSteps);
		originalIndex=new int[descriptor.tileCount];
		rotation = new int[descriptor.tileCount];
		for(int i=0; i<descriptor.tileCount; i++)
		{
			originalIndex[i]=i;
			rotation[i]=0;
		}
	}

	/**
	 * @param boardWidth Width of the play area, in pixels.
	 * @param boardHeight Height of the play area, in pixels.
	 * @param tilesAcross Column count - not necessarily the number of tiles in each row.
	 * @param tilesDown Row count - not necessarily the number of tiles in each column.
	 * @param fitEdgeTiles Indicates whether edge tiles will be adjusted to fit the board.
	 */
	public AbstractTileManagerImpl(int boardWidth, int boardHeight, int tilesAcross, int tilesDown, boolean fitEdgeTiles)
	{
		descriptor = new TileSetDescriptor();
		descriptor.boardWidth=boardWidth;
		descriptor.boardHeight=boardHeight;
		descriptor.tilesAcross=tilesAcross;
		descriptor.tilesDown=tilesDown;
		descriptor.fitEdgeTiles=fitEdgeTiles;

		initTileSetDescriptor(descriptor);
		descriptor.scaleFactor=((double)descriptor.tileHeight)/((double)descriptor.tileWidth)/descriptor.heightWidthRatio;
		descriptor.leftOffset = (descriptor.boardWidth - descriptor.tilesAcross*descriptor.tileSpacingX - descriptor.tileWidth + descriptor.tileSpacingX)/2;
		descriptor.topOffset = (descriptor.boardHeight - descriptor.tilesDown*descriptor.tileSpacingY - descriptor.tileHeight + descriptor.tileSpacingY)/2;

		rotationStep=2.0*Math.PI/((double)descriptor.rotationSteps);
		originalIndex=new int[descriptor.tileCount];
		rotation = new int[descriptor.tileCount];
		for(int i=0; i<descriptor.tileCount; i++)
		{
			originalIndex[i]=i;
			rotation[i]=0;
		}
	}
	/**
	 * Rotate a tile.
	 * @param tileIndex the tiles to rotate
	 * @param direction one of TileManager.SPIN_CW or TileManager.SPIN_CCW
	 * @param steps the number of discrete steps to rotate
	 */

	public void rotate(int flatIndex, int direction)
	{
		rotation[flatIndex]+=direction;
		while(rotation[flatIndex]<0)rotation[flatIndex]+=descriptor.rotationSteps;
		rotation[flatIndex]%=descriptor.rotationSteps;
	}
	
	public void swap(int flatIndex1, int flatIndex2)
	{
		int t=originalIndex[flatIndex1];
		originalIndex[flatIndex1]=originalIndex[flatIndex2];
		originalIndex[flatIndex2]=t;
		t=rotation[flatIndex1];
		rotation[flatIndex1]=rotation[flatIndex2];
		rotation[flatIndex2]=t;
	}
	
	public int getRotationCount(int flatIndex)
	{
		return rotation[flatIndex];
	}
	
	public double getRotation(int flatIndex)
	{
		return rotationStep*rotation[flatIndex];
	}
	
	public int getOriginalTileIndex(int flatIndex)
	{
		return originalIndex[flatIndex];
	}

/*--geometry--*/
	
	/**
	 * Returns the graphical coordinates for the upper left corner of the tile's current position.
	 */
	public Point getTilePosition(int flatIndex, Point position)
	{
		if(getExpandedIndex(flatIndex,tempIndex)!=null)
		{
			if(position==null)position=new Point();
			position.x = descriptor.leftOffset + tempIndex.x*descriptor.tileSpacingX;
			position.y = descriptor.topOffset + tempIndex.y*descriptor.tileSpacingY;
		}else position=null;
		return position;
	}	
	
	/**
	 * Returns graphical coordinates for the upper left corner of the tile's original position.
	 * @param flatIndex index of the tile
	 */
	public Point getOriginalTilePosition(int flatIndex, Point position)
	{
		return getTilePosition(originalIndex[flatIndex], position);
	}
	
	/**
	 * Returns the graphics transformation required to rotate a tile to its current orientation
	 * @param flatIndex tile index
	 * @param transform the graphics transformation to use as a starting point
	 * @param margin the number of pixels around the outside of the tile's space
	 * @return the original transformation, rotated
	 */
	public AffineTransform getRotationTransform(int flatIndex, AffineTransform transform, int margin)
	{
		if(rotation[flatIndex] > 0)
		{
			center = getScaledTileCenterOffset(flatIndex,center);
			originalCenter = getScaledTileCenterOffset(originalIndex[flatIndex],originalCenter);
			//Scale to adjust for stretch
			transform.scale(1.0,descriptor.scaleFactor);
			//center the tile for its new orientation
			transform.translate(center.x-originalCenter.x,center.y - originalCenter.y);
			//apply rotation around original center
			transform.rotate(rotation[flatIndex]*rotationStep, originalCenter.x + margin, originalCenter.y + margin);
			//Scale back to display dimensions
			transform.scale(1.0,1.0/descriptor.scaleFactor);
		}

		return transform;
	}

	public int getBoardWidth(){return descriptor.boardWidth;}
	public int getBoardHeight(){return descriptor.boardHeight;}
	public int getTileCount(){return descriptor.tileCount;}
	public int getTilesAcross(){return descriptor.tilesAcross;}
	public int getTilesDown(){return descriptor.tilesDown;}
	public int getTileWidth(){return descriptor.tileWidth;}
	public int getTileHeight(){return descriptor.tileHeight;}
	public int getTileSpacingX(){return descriptor.tileSpacingX;}
	public int getTileSpacingY(){return descriptor.tileSpacingY;}
	public int getLeftOffset(){return descriptor.leftOffset;}
	public int getTopOffset(){return descriptor.topOffset;}
	public double getHeightScaleFactor(){return descriptor.scaleFactor;}
	public int getTileMargin(){return descriptor.tileMargin;}
	public int getSideCount(){return descriptor.sideCount;}
	public int getRotationSteps(){return descriptor.rotationSteps;}
	public double getRotationStep(){return rotationStep;}
}
