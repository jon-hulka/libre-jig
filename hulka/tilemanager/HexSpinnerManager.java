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
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.import java.awt.*;
 */
package hulka.tilemanager;
import java.awt.Point;
/**
 * Extends HexTileManager's functionality with the functions defined
 * in TileSpinnerManager. Each vertex is positioned at the centre of three tiles.
 *    
 * For now, this class is hardcoded to always represent exactly seven tiles.
 */
public class HexSpinnerManager extends HexTileManager implements TileSpinnerManager
{
	//Temporary storage to make rotate operations simpler
	private int [][] newRotation = new int[3][2];
	private int [][] newLocation = new int[3][2];
	private int spinVertex = -1;
	private static int TILE_COUNT=7;
	private static int VERTEX_COUNT=6;
	
	//for calculating nearest vertex
	private int xOffsetAdjustment, yOffsetAdjustment;
	//The superclass will have a different descriptor
	private TileSetDescriptor descriptor2;

	//Temporary storage to avoid excessive heap usage
	private Point tempIndex=new Point(), tempIndex2=new Point(), tempNeighbor=new Point();
	
	public HexSpinnerManager(int width, int height)
	{
		super(width, height, 5,3,false);
		yOffsetAdjustment = -descriptor2.topOffset - descriptor.tileHeight/2;
		xOffsetAdjustment = -descriptor2.leftOffset - descriptor2.tileSpacingX*3/2;
	}

	public void initTileSetDescriptor(TileSetDescriptor d)
	{
		descriptor2=d;
		TileSetDescriptor other=null;
		try{other=(TileSetDescriptor)d.clone();}catch(CloneNotSupportedException ex){ex.printStackTrace();}
		other.tilesAcross++;
		other.boardWidth=other.boardWidth*7/6;
		super.initTileSetDescriptor(other);
		
		d.tileWidth=other.tileWidth;
		d.tileHeight=other.tileHeight;
		d.tileSpacingX=other.tileSpacingX;
		d.tileSpacingY=other.tileSpacingY;
		d.tileCount=TILE_COUNT;
		d.rotationSteps=other.rotationSteps;
		d.sideCount=other.sideCount;
		d.heightWidthRatio=other.heightWidthRatio;
		d.tileMargin=other.tileMargin;
	}
	
	public int getFlatVertexIndex(Point expandedIndex)
	{
		int result=-1;
		if(expandedIndex.x>=0 && expandedIndex.x<3 && expandedIndex.y>=0 && expandedIndex.y<2)
		{
			result=expandedIndex.x+expandedIndex.y*3;
		}
		return result;
	}
	
	public Point getExpandedVertexIndex(int flatIndex, Point expandedIndex)
	{
		if(flatIndex<6)
		{
			if(expandedIndex==null)expandedIndex=new Point();
			expandedIndex.x=flatIndex%3;
			expandedIndex.y=flatIndex/3;
		}
		else expandedIndex=null;
		return expandedIndex;
	}
	
	/**
	 * Overridden to adjust for two ignored tiles.
	 */
	public int getFlatIndex(Point expandedIndex)
	{
		//Shift input value right one position
		tempIndex.x=expandedIndex.x+1;
		tempIndex.y=expandedIndex.y;
		int result=super.getFlatIndex(tempIndex);
		//Shift output value left one position
		if(result>-1 && result!=6)
		{
			result-=1+result/6;
		}
		else result=-1;
		return result;
	}
	
	/**
	 * Overridden to adjust for two ignored tiles.
	 */
	public Point getExpandedIndex(int flatIndex, Point expandedIndex)
	{
		//Shift input value right one position
		if(super.getExpandedIndex(flatIndex+1+flatIndex/5,tempIndex)!=null && tempIndex.x>0)
		{
			if(expandedIndex==null)
			{
				expandedIndex=new Point(tempIndex.x-1,tempIndex.y);
			}
			else
			{
				expandedIndex.x=tempIndex.x-1;
				expandedIndex.y=tempIndex.y;
			}
		}else expandedIndex=null;
		return expandedIndex;
	}
		
	/**
	 * Overridden to adjust for two ignored tiles.
	 */
	public int getNeighborIndex(int flatIndex, Point offset)
	{
		int result=super.getNeighborIndex(flatIndex+1+flatIndex/5,offset);
		if(result>-1 && result!=6)
		{
			result-=1+result/6;
		}
		else result=-1;
		return result;
	}

	/**
	 * Overridden to adjust for two ignored tiles.
	 */
	public int [] getNeighbors(int flatIndex, int [] neighbors)
	{
		int [] result=super.getNeighbors(flatIndex+1+flatIndex/5, neighbors);
		for(int i=0; result!=null && i<result.length; i++)
		{
			if(result[i]>-1&&result[i]!=6)
			{
				result[i]-=1+result[i]/6;
			}else result[i]=-1;
		}
		return result;
	}

	/**
	 * Overridden to adjust for two ignored tiles.
	 */
	public int getTileAt(int x, int y)
	{
		int result=super.getTileAt(x+descriptor2.tileSpacingX,y);
		if(result>-1 && result!=6)
		{
			result-=1+result/6;
		}
		else result=-1;
		return result;
	}

	public int getVertexCount(){return VERTEX_COUNT;}

	public int getNearestVertex(int x, int y)
	{
		int result=-1;
		int yOff = y + yOffsetAdjustment;
		int xOff = x + xOffsetAdjustment;
		if(yOff>=0 && xOff>=0)
		{
			int row = (yOff)/descriptor2.tileSpacingY;
			int column = (xOff)/descriptor2.tileSpacingX;

			//Verify the vertex is valid
			if(row <2 && column <3)
			{
				result=column+row*3;
			}

		}
		return result;
	}

	public Point getVertexPosition(int flatIndex, Point position)
	{
		if(flatIndex>=0 && flatIndex <6)
		{
			if(position==null)position=new Point();
			position.x=descriptor2.leftOffset + descriptor2.tileWidth + (flatIndex%3)*descriptor2.tileSpacingX;
			position.y=descriptor2.topOffset + descriptor2.tileHeight*3/4 + descriptor2.tileSpacingY*(flatIndex/3) + descriptor2.tileHeight/4*((flatIndex+1)%2);
		}
		else position=null;
		return position;
	}
	
	public void spin(int flatIndex, int direction)
	{
		if(flatIndex>=0&&flatIndex<6)
		{
			spinVertex=flatIndex;
			getExpandedVertexIndex(flatIndex,tempIndex);
			int x=tempIndex.x;
			int y=tempIndex.y;
			for(int j = 0; j <= 1; j++)
			{
				for(int i = -1; i <= 1; i++)
				{
					int tX = x + i + 1;
					int tY = y + j;
					//Check if this tile exists
					if((tX + tY)%2==1)
					{
						//Correct for indexing
						int newI = i + 1;
						int newJ = j;
						int ySide = -2*j + 1;//-1 or 1
						if(i==0) //only one cell in the row
						{
							newJ += ySide;
							newI += ySide*direction;
						}
						else if(ySide*direction==i) //leading edge of the row
						{
							//move up or down
							newJ += ySide;
							newI -= ySide*direction;
							
						}
						else //back of row
						{
							//move sideways
							newI += ySide*direction*2;
						}
						tempIndex.x=tX;
						tempIndex.y=tY;
						int tI=getFlatIndex(tempIndex);
						//Populate the temp values
						newRotation[newI][newJ] = (rotation[tI] + direction*2);
						while(newRotation[newI][newJ] < 0) newRotation[newI][newJ] += descriptor2.rotationSteps;
						newRotation[newI][newJ]%=descriptor2.rotationSteps;
						newLocation[newI][newJ] = originalIndex[tI];
					}
				}
			}
			for(int j = 0; j <= 1; j++)
			{
				for(int i = -1; i <= 1; i++)
				{
					int tX = x + i + 1;
					int tY = y + j;
					if((tX + tY)%2==1)
					{
						tempIndex.x=tX;
						tempIndex.y=tY;
						int tI=getFlatIndex(tempIndex);
						//Set the tile values
						rotation[tI] = newRotation[i + 1][j];
						originalIndex[tI] = newLocation[i + 1][j];
					}
				}
			}
		}
	}
	
	public int [] getChangedTiles(int [] indices)
	{
		//Must use tempIndex2 here because getFlatIndex changes tempIndex
		if(getExpandedVertexIndex(spinVertex,tempIndex2)!=null)
		{
			if(indices==null) indices=new int[3];
			
			int index = 0;
			for(int j = 0; j <= 1; j++)
			{
				for(int i = -1; i <= 1; i++)
				{
					tempNeighbor.x= tempIndex2.x + i + 1;
					tempNeighbor.y = tempIndex2.y + j;
					if((tempNeighbor.y+tempNeighbor.x)%2==1)
					{
						indices[index] = getFlatIndex(tempNeighbor);
						index ++;
					}
				}
			}
		}
		return indices;
	}
}
