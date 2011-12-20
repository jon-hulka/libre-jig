/** 
 *   Copyright (C) 2010 Jonathan Hulka (jon.hulka@gmail.com)
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

package hulka.tilemanager;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.AffineTransform;
import java.awt.Point;
import java.awt.Shape;
/**
 *  Implements TileManager functionality specific to hex shaped tiles.
 *  Tiles are oriented with vertical left and right edges.
 *  Corners are indexed clockwise from the top center.
 *  Edges (and neighbors) are indexed clockwise from the upper right (starting at corner 0).
 */
public class HexTileManager extends AbstractTileManagerImpl
{
	public static final int SIDE_COUNT=6;
	protected Point2D.Double [] maskPoints;

	protected TileSetDescriptor descriptor;
	//This will be used in some calculations
	protected static double sqrt3 = Math.sqrt(3);
	
	protected static Point [] neighborOffsetIndex = {new Point(1,-1),new Point(2,0),new Point(1,1),new Point(-1,1),new Point(-2,0),new Point(-1,-1)};

	//Temporary storage to avoid excessive heap usage
	private Point tempIndex=new Point(), tempNeighbor=new Point();

	/**
	 * To satisfy HexJigsawManager's mimimal constructor.
	 */
	protected HexTileManager(TileSetDescriptor descriptor)
	{
		super(descriptor);
		this.descriptor=descriptor;
		initMask();
	}

	public HexTileManager(int boardWidth, int boardHeight, int tilesAcross, int tilesDown, boolean fitEdgeTiles)
	{
		super(boardWidth,boardHeight,tilesAcross,tilesDown, fitEdgeTiles);
		initMask();
	}
	
	private void initMask()
	{
		maskPoints=new Point2D.Double[SIDE_COUNT];
		maskPoints[0]=new Point2D.Double(((double)descriptor.tileWidth)/2.0,0.0);
		maskPoints[1]=new Point2D.Double(((double)descriptor.tileWidth),((double)descriptor.tileHeight)/4.0);
		maskPoints[2]=new Point2D.Double(((double)descriptor.tileWidth),((double)descriptor.tileHeight)*3.0/4.0);
		maskPoints[3]=new Point2D.Double(((double)descriptor.tileWidth)/2.0,(double)descriptor.tileHeight);
		maskPoints[4]=new Point2D.Double(0.0,((double)descriptor.tileHeight)*3.0/4.0);
		maskPoints[5]=new Point2D.Double(0.0,((double)descriptor.tileHeight)/4.0);
	}
	
	public void initTileSetDescriptor(TileSetDescriptor d)
	{
		descriptor=d;
		
		//Optimal tile width
		int tW=d.boardWidth*2/(d.tilesAcross+(d.fitEdgeTiles?-1:1));
		//Optimal tile height
		int tH=d.boardHeight*4/(d.tilesDown*3+(d.fitEdgeTiles?-1:1));
		//Choose the smaller tile size
		if(((double)tH)*sqrt3 > tW*2.0){tH=(int)(tW*2.0/sqrt3);}else tW=(int)(tH*sqrt3/2.0);
		//Make tH an integral multiple of 4, to ensure accuracy calculating spacing
		tH-=tH%4;
		//Make tW an integral multiple of 2, to ensure accuracy caclulating spacing
		tW-=tW%2;
		
		d.tileWidth=tW;
		d.tileHeight=tH;
		d.tileSpacingX=tW/2;
		d.tileSpacingY=tH*3/4;
		//Don't condense this function - it relies on integer rounding
		d.tileCount=((d.tilesAcross+1)/2)*((d.tilesDown+1)/2) + (d.tilesAcross/2)*(d.tilesDown/2);
		d.rotationSteps=SIDE_COUNT;
		d.sideCount=SIDE_COUNT;
		d.heightWidthRatio=2.0/sqrt3;
		d.tileMargin=0;
	}
	
	/**
	 * Calculates a grid size (tilesAcross, tilesDown) that produces as many or more tiles than the requested value.
	 * This function is useful in determining potential values to be passed into the constructor.
	 * @param width Board width
	 * @param height Board height
	 * @param count Number of tiles to calculate for.
	 * @param fitEdgeTiles Are edge tiles to be adjusted to fit?
	 * @param d Storage for return parameters - if null a new {@link TileDescriptor} will be allocated.
	 * @return A {@link TileSetDescriptor} with boardWidth, boardHeight, tilesAcross, tilesDown and tileCount set to their calculated values.
	 * Note that the resulting tileCount may be larger than the input count value.
	 */
	public static TileSetDescriptor getBestFit(int width, int height, int count, boolean fitEdgeTiles, TileSetDescriptor d)
	{
		if(d==null)d=new TileSetDescriptor();
		d.boardWidth=width;
		d.boardHeight=height;
		d.fitEdgeTiles=fitEdgeTiles;
		//get a rough estimate
		int tA=(int)Math.sqrt(count*width/height)*2 -1;
		int tD=0;
		int tC=0;
		int edgeAdjustX=fitEdgeTiles?-1:1;
		//Optimal vertical count adjustment - may need some tweaking
		int edgeAdjustY=fitEdgeTiles?-1:1;

		while(tC<count)
		{
			//increment horizontal tile count
			tA+=1;
			int tW=2*width/(tA+edgeAdjustX);
			int tH=(int)(tW*2/sqrt3);
			//Calculate the vertical unit as tileHeight/4 (vertical spacing will be 3 units)
			int vUnit=tH/4;
			//Calculate vertical unit count (within half a unit either way)
			int vUnits=(height+vUnit/2)/vUnit;
			tD=(vUnits-edgeAdjustY)/3;
			//Don't condense this function - it relies on integer rounding
			tC=((tA+1)/2)*((tD+1)/2) + (tA/2)*(tD/2);
		}

		d.tilesAcross=tA;
		d.tilesDown=tD;
		d.tileCount=tC;
		return d;
	}

	public Point getExpandedIndex(int flatIndex, Point expandedIndex)
	{
		if(flatIndex>=0 && flatIndex<descriptor.tileCount)
		{
			if(expandedIndex==null)expandedIndex=new Point();
			expandedIndex.x=(flatIndex%descriptor.tilesAcross)*2;
			expandedIndex.y=(flatIndex/descriptor.tilesAcross)*2;
			if(expandedIndex.x>=descriptor.tilesAcross)
			{
				expandedIndex.y++;
				expandedIndex.x-=descriptor.tilesAcross-1 + descriptor.tilesAcross%2;
			}
		}else expandedIndex=null;
		return expandedIndex;
	}
	
	public int getFlatIndex(Point tileIndex)
	{
		int result=-1;
		if((tileIndex.x+tileIndex.y)%2==0 && tileIndex.x >=0 && tileIndex.x<descriptor.tilesAcross && tileIndex.y>=0 && tileIndex.y<descriptor.tilesDown)
			result=(tileIndex.y/2)*(descriptor.tilesAcross/2) + ((tileIndex.y+1)/2)*((descriptor.tilesAcross+1)/2) + tileIndex.x/2;
		return result;
	}
	
	public int getTileAt(int x, int y)
	{
		int subWidth=descriptor.tileWidth/2;
		int subHeight=descriptor.tileHeight/4;
		int result=-1;
		int adjustedX=x-descriptor.topOffset+(descriptor.fitEdgeTiles?subWidth:0);
		int adjustedY=y-descriptor.leftOffset+(descriptor.fitEdgeTiles?subHeight:0);
		//Calculate the row in quarter tiles
		int row=adjustedY/subHeight;
		//Calculate the column in half tiles
		int column=adjustedX/subWidth;
		int subRow=row%3;
		row/=3;
		if(subRow==0||subRow==4)
		{
			//Top and bottom of tile are angled, determine which side the point falls on
			int xOffset=adjustedX%subWidth;
			int yOffset=adjustedY%subHeight;
			row -=(yOffset*subWidth-((column+row)%2==0?subWidth-xOffset:xOffset)*subHeight)>0?0:-1;
		}
		column=row%2+column/2;
		tempIndex.x=column;
		tempIndex.y=row;
		return getFlatIndex(tempIndex);
	}

	public Shape getTileMask(int flatIndex)
	{
		Path2D.Double mask = new Path2D.Double();
		mask.moveTo(maskPoints[0].x,maskPoints[0].y);
		for(int i=1; i<maskPoints.length;i++)
		{
			mask.lineTo(maskPoints[i].x,maskPoints[i].y);
		}
		mask.closePath();
		return mask;
	}

	public Point2D.Double getScaledTileCenterOffset(int flatIndex, Point2D.Double offset)
	{
		if(offset==null)offset=new Point2D.Double();
		offset.x=((double)descriptor.tileWidth)/2.0;
		offset.y=((double)descriptor.tileWidth)/sqrt3;
		return offset;
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
		//Quick sanity check, since neighbors will always be in the same row or within 1 column
		if(getExpandedIndex(flatIndex,tempIndex)!=null&&(offset.x==1||offset.x==-1||offset.y==0))
		{
			//Find which neighborOffsetIndex this point references
			int i = 0;
			for(; i < descriptor.sideCount && !offset.equals(neighborOffsetIndex[i]); i++);
			result=getNeighborIndex(flatIndex,i);
		}
		return result;
	}
	
	public int getNeighborIndex(int flatIndex, int direction)
	{
		int result=-1;
		if(direction>=0 && direction<descriptor.rotationSteps && getExpandedIndex(flatIndex,tempIndex)!=null)
		{
			//Adjust for rotation
			int index = (direction - rotation[flatIndex]);
			while(index < 0) index += descriptor.rotationSteps;
			index %= descriptor.sideCount;
			tempNeighbor.x=tempIndex.x + neighborOffsetIndex[index].x;
			tempNeighbor.y=tempIndex.y + neighborOffsetIndex[index].y;
			result=getFlatIndex(tempNeighbor);
		}
		return result;
	}
}
	
