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
	 * Provides a means to communicate basic tileset information to the constructor
	 * @return a {@link TileSetDescriptor} with relevant fields initialized
	 */
	public abstract void initTileSetDescriptor(TileSetDescriptor descriptor);
	public abstract Point2D.Double getScaledTileCenterOffset(int flatIndex, Point2D.Double offset);
	public abstract Point getExpandedIndex(int flatIndex, Point expandedIndex);
	
	/**
	 * @param tilesAcross
	 * @param tilesDown
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
