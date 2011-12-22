/**   Copyright (C) 2011 Jonathan Hulka (jon.hulka@gmail.com)
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
 * 2011 12 19 - Jon
 *  - Finished implementing save and load functions
 */
package hulka.tilemanager;

import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.Point;
import java.awt.Shape;

import java.io.PrintWriter;
import java.io.BufferedReader;
import hulka.util.ArrayReader;
import hulka.util.ArrayWriter;


/**
 * JigsawCutter consolidates the shape generation functionality of SquareJigsawManager and HexJigsawManager.
 * This should make this code more maintainable in the future, and it also allows saving and loading of jigsaw shape data to be done in one place.
 */
public abstract class JigsawCutter
{
	private TileSetDescriptor descriptor;
	private Point2D.Double [] maskPoints;
	//This will be used in some calculations
	protected static double sqrt3 = Math.sqrt(3);
	//The edges are rotated into position to simplify the corner/control point calculations, then rotated back for drawing the path
	//These transforms handle the rotations
	private AffineTransform[] reverseMaskTransform; // = new AffineTransform[NEIGHBOR_COUNT/2];
	private AffineTransform[] maskTransform; // = new AffineTransform[NEIGHBOR_COUNT/2];
	
	//Tile drawing information
	//Corners
	private int [][] cornerOffsetX;
	private int [][] cornerOffsetY;
	//Edges
	private int [][] bubbleSize;
	private int [][] bubbleDirection;
	private int [][] controlPointOffset;

	private Point cornerTileOffset[];
	private int cornerIndexOffset[];
	private Point edgeTileOffset[];





	//Temporary storage for drawing function - to avoid excessive heap usage
	private Point2D.Double [] corners, controls;
	//These will be the endpoints of the side being drawn
	private Point2D.Double a=new Point2D.Double(), b=new Point2D.Double();

	//Temporary storage to avoid excessive heap usage
	private Point tempNeighbor=new Point();
	
	/**
	 * @param descriptor
	 * @param rotationOffset rotation required to bring the first 'top' edge into a horizontal position.
	 * @param maskPoints corner points for the tile's mask, one for each corner
	 * @param cornerTileOffset array of points (one value for each corner), indicating x,y offset to the tile that 'owns' the indexed corner
	 * @param cornerIndexOffset used as the first index to the two-dimensional arrays cornerOffsetX and cornerOffsetY (redundant for square tiles, since the index will always be 0)
	 * @param edgeTileOffset, array of points (one value for each side), indicating x,y offset to the tile that 'owns' the indexed side
	 * Only tiles with an even number of sides are considered here (square and hex specifically), so the assumption is that each tile 'owns' its first sideCount/2 tiles and 'borrows' the remaining from its neighbors.
	 * The 'borrowed' side opposite a tile's 'owned' side is found by adding sideCount/2 to the side's index.
	 */
	public JigsawCutter(
		TileSetDescriptor descriptor,
		double rotationOffset,
		Point2D.Double [] maskPoints,
		Point [] cornerTileOffset,
		int [] cornerIndexOffset,
		Point [] edgeTileOffset)
	{
		this.descriptor=descriptor;
		this.maskPoints=maskPoints;
		this.cornerTileOffset=cornerTileOffset;
		this.cornerIndexOffset=cornerIndexOffset;
		this.edgeTileOffset=edgeTileOffset;


		corners = new Point2D.Double[descriptor.sideCount*4];
		controls = new Point2D.Double[descriptor.sideCount*6];

		//Set up corners and controls for the edge path - there will be 4 corners (plus 1 shared) and 6 controls on each side
		//The order is something like this:
		//corner(shared),controls[0],controls[1],corners[0] - cubic spline from the corner of the tile to the base of the stem
		//corners[0],controls[2],corners[1] - quadratic spline from the base of the stem to the center of the bubble
		//corners[1],controls[3],corners[2] - quadratic spline from the center of the bubble to the base of the stem (other side)
		//corners[2],controls[4],controls[5],corners[3] - cubic spline from the base of the stem to the corner of the tile (other side)
		//
		//controls[1], controls[2], controls[3], and controls[4] form a near triangle,
		//1 and 4 being nearly centered on the tile edge between the shared corner and corners[3]
		//and the intervening corners between the controls - this creates the bubble shape
		for(int j=0; j<descriptor.sideCount; j++)
		{
			for(int i=0; i<4; i++) corners[i*descriptor.sideCount+j]=new Point2D.Double();
			for(int i=0; i<6; i++) controls[i*descriptor.sideCount+j]=new Point2D.Double();
		}
		//The edges are rotated into position to simplify the corner/control point calculations, then rotated back for drawing the path
		//These transforms handle the rotations
		reverseMaskTransform = new AffineTransform[descriptor.sideCount/2];
		maskTransform = new AffineTransform[descriptor.sideCount/2];
		for(int i=0; i<descriptor.sideCount/2; i++)
		{
			double theta = rotationOffset + i*2.0*Math.PI/descriptor.sideCount;
			reverseMaskTransform[i] = AffineTransform.getRotateInstance(-theta,descriptor.tileWidth/2.0, descriptor.tileHeight/2.0);
			maskTransform[i] = AffineTransform.getRotateInstance(theta,descriptor.tileWidth/2.0, descriptor.tileHeight/2.0);
		}
	}
	
	/**
	 * @param cornerOffsetX corner offsets as a two dimensional array; [number of corners 'owned' by each tile (1 for square, 2 for hex)] by [number of tiles (extended index)]
	 * @param cornerOffsetY corner offsets, as a two dimensional array; same index as cornerOffsetX
	 * @param bubbleSize bubble sizes in pixels, as a two dimensional array; [number of sides 'owned' by each tile (2 for square, 3 for hex)] by [number of tiles (extended index)]
	 * @param bubbleDirection bubble directions (-1 or 1), as a two dimensional array; same index as bubbleSize
	 * @param controlPointOffset, bubble center control point offset; same index as bubbleSize
	 */
	public void setDrawingData(
		int [][] cornerOffsetX,
		int [][] cornerOffsetY,
		int [][] bubbleSize,
		int [][] bubbleDirection,
		int [][] controlPointOffset)
	{
		this.cornerOffsetX=cornerOffsetX;
		this.cornerOffsetY=cornerOffsetY;
		this.bubbleSize=bubbleSize;
		this.bubbleDirection=bubbleDirection;
		this.controlPointOffset=controlPointOffset;
	}
/**	
	private boolean saveDataItem(PrintWriter out, int [][] item, String name)
	{
		boolean result=true;
		int len0=item.length;
		out.println(name+":"+len0);
		for(int i=0; i<len0; i++)
		{
			int len1=item[i].length;
			out.println(name+":"+i+":"+len1);
			for(int j=0; j<len1; j++)
			{
				out.print(item[i][j]+":");
			}
		}
		return result;
	}
**/
	public boolean save(PrintWriter out, PrintWriter err)
	{
		int sideIndexCount=bubbleSize.length;
		int cornerIndexCount=cornerOffsetX.length;
		int itemCount=bubbleSize[0].length;
		String [] names = new String[sideIndexCount*3 + cornerIndexCount*2];
		int [][] values = new int[names.length][];
		int i=0;
		//Put everything into one big array
		for(int j=0; j<cornerIndexCount; j++)
		{
			names[i]="cornerOffsetX["+j+"]";
			values[i]=cornerOffsetX[j];
			i++;
			names[i]="cornerOffsetY["+j+"]";
			values[i]=cornerOffsetY[j];
			i++;
		}
		for(int j=0; j<sideIndexCount; j++)
		{
			names[i]="bubbleSize["+j+"]";
			values[i]=bubbleSize[j];
			i++;
			names[i]="bubbleDirection["+j+"]";
			values[i]=bubbleDirection[j];
			i++;
			names[i]="controlPointOffset["+j+"]";
			values[i]=controlPointOffset[j];
			i++;
		}
		//And save it
		return new ArrayWriter(names.length,bubbleSize[0].length,"JigsawCutter").save(values,names,out,err);
	}

	public boolean load(BufferedReader in, PrintWriter err)
	{
		//This is the only load function that doesn't create an instance.
		//JigsawCutter is abstract, so the specific instance must be created by the calling class.
		boolean result=true;
		ArrayReader reader = new ArrayReader("JigsawCutter");
		result=reader.load(in,err);
		int cornerIndexCount=0;
		int sideIndexCount=0;
		if(result)
		{
			//Find out how many corners are indexed - don't output to error here
			for(;reader.getColumn("cornerOffsetX[" + cornerIndexCount + "]",null)!=null;cornerIndexCount++);
			//Make sure there is at least one
			if(cornerIndexCount==0)
			{
				result=false;
				err.println("JigsawCutter.load(): Missing corner offsets");
			}
		}
		
		if(result)
		{
			//Load up the corners
			cornerOffsetX=new int[cornerIndexCount][];
			cornerOffsetY=new int[cornerIndexCount][];
			for(int i=0; result && i<cornerIndexCount; i++)
			{
				cornerOffsetX[i]=reader.getColumn("cornerOffsetX["+i+"]",err);
				cornerOffsetY[i]=reader.getColumn("cornerOffsetY["+i+"]",err);
				result=cornerOffsetX[i]!=null && cornerOffsetY[i]!=null;
			}
		}
		
		if(result)
		{
			//Find out how many sides are indexed
			for(;reader.getColumn("bubbleSize[" + sideIndexCount + "]",null)!=null;sideIndexCount++);
			//Make sure there is at least one
			if(sideIndexCount==0)
			{
				result=false;
				err.println("JigsawCutter.load(): Missing corner offsets");
			}
		}
		
		if(result)
		{
			//Load up the sides
			bubbleSize=new int[sideIndexCount][];
			bubbleDirection=new int[sideIndexCount][];
			controlPointOffset=new int[sideIndexCount][];
			for(int i=0; result && i<sideIndexCount; i++)
			{
				bubbleSize[i]=reader.getColumn("bubbleSize["+i+"]",err);
				bubbleDirection[i]=reader.getColumn("bubbleDirection["+i+"]",err);
				controlPointOffset[i]=reader.getColumn("controlPointOffset["+i+"]",err);
				result=bubbleSize[i]!=null && bubbleDirection[i]!=null && controlPointOffset[i]!=null;
			}
		}
		return result;
	}

	/**
	 * Special indexing includes tiles past the edge of the board.
	 */
	public abstract int getExtFlatIndex(Point tileIndex);

	public Shape getTileMask(Point tileIndex)
	{
		//This algorithm works for even-sided tiles (square and hex)
		//Each tile 'owns' half its edges while the other half are 'owned' by neighboring tiles.
		//Triangular tiles will require a different approach
		int extIndex=getExtFlatIndex(tileIndex);
		//Position the corners
		for(int i = 0; i < descriptor.sideCount; i++)
		{
			tempNeighbor.x = tileIndex.x + cornerTileOffset[i].x;
			tempNeighbor.y = tileIndex.y + cornerTileOffset[i].y;
			
			int index=getExtFlatIndex(tempNeighbor);
			int indexOffset=cornerIndexOffset[i];
			int xOffset = cornerOffsetX[indexOffset][index];
			int yOffset = cornerOffsetY[indexOffset][index];

			//Apply offsets
			corners[i*4].x=maskPoints[i].x + xOffset;
			corners[i*4].y=maskPoints[i].y + yOffset;
		}
		for(int i = 0; i < descriptor.sideCount; i++)
		{
			int index=-1;
			if(i>=descriptor.sideCount/2)
			{
				//This is a 'bottom' edge - use the 'top' edge of a neighboring tile
				tempNeighbor.x = tileIndex.x + edgeTileOffset[i].x;
				tempNeighbor.y = tileIndex.y + edgeTileOffset[i].y;
				index=getExtFlatIndex(tempNeighbor);
			}
			else
			{
				//This is a 'top' edge - it belongs to the current tile
				index=extIndex;
			}
			//Rotate the corners into position for computing the bubble
			//Index of the 'top' edge will determine the rotation angle required, so the corresponding 'top' index must be used for 'bottom' edges
			int topIndex=i%(descriptor.sideCount/2);
			a = (Point2D.Double)reverseMaskTransform[topIndex].transform(corners[i*4],a);
			b = (Point2D.Double)reverseMaskTransform[topIndex].transform(corners[((i+1)%descriptor.sideCount)*4],b);
			double cp,bSize,bDirection;
			if(bubbleSize[topIndex][index]==0&&controlPointOffset[topIndex][index]==0)
			{
				//Flat edge - put all the control points at the corners
				for(int j=0;j<6;j++)
				{
					controls[i*6+j].x=b.x;
					controls[i*6+j].y=b.y;
				}
				for(int j=1;j<4;j++)
				{
					corners[i*4+j].x=b.x;
					corners[i*4+j].y=b.y;
				}
			}
			else
			{
				//Bubble edge - set up and call buildEdge
				cp = (a.x+b.x)/2 + controlPointOffset[topIndex][index];
				bSize = bubbleSize[topIndex][index];
				bDirection = bubbleDirection[topIndex][index];
				buildEdge(corners, controls, i, a.x, a.y, b.x, b.y, cp, bSize, bDirection);
			}
			
			//Rotate all the corners and controls back into position for drawing
			for(int j = 0; j < 6; j++)
			{
				maskTransform[topIndex].transform(controls[i*6+j],controls[i*6+j]);
			}
			for(int j = 1; j < 4; j++)
			{
				maskTransform[topIndex].transform(corners[i*4+j],corners[i*4+j]);
			}
		}

		//Draw the path
		Path2D.Double path = new Path2D.Double();
		path.moveTo((float)corners[0].x,(float)corners[0].y);

		for(int i=0; i < descriptor.sideCount; i++)
		{
			path.curveTo((float)controls[i*6].x,(float)controls[i*6].y,(float)controls[i*6+1].x,(float)controls[i*6+1].y,(float)corners[i*4 + 1].x,(float)corners[i*4 + 1].y);
			path.quadTo((float)controls[i*6+2].x,(float)controls[i*6+2].y,(float)corners[i*4 + 2].x,(float)corners[i*4 + 2].y);
			path.quadTo((float)controls[i*6+3].x,(float)controls[i*6+3].y,(float)corners[i*4 + 3].x,(float)corners[i*4 + 3].y);
			path.curveTo((float)controls[i*6+4].x,(float)controls[i*6+4].y,(float)controls[i*6+5].x,(float)controls[i*6+5].y,(float)corners[((i+1)%descriptor.sideCount)*4].x,(float)corners[((i+1)%descriptor.sideCount)*4].y);
		}
		
		return new Area(path);
	}

	private void buildEdge(Point2D.Double [] corners, Point2D.Double [] controls, int index, double x1, double y1, double x2, double y2, double cp, double bubbleSize, double bubbleDirection)
	{
		//The edge is assumed to be horizontal - getTileMask handles the rotation
		//       _
		//______\ /_______
		//
		// or
		//______   _______
		//      /_\
		//
		double direction = x2 - x1 > 0 ? 1 : -1;
		double midY = (y1 + y2)/2.0;
		//Base control point for bubble
		double midX = cp;
		double cpStemY1 = (y1*0.65 + y2*0.35);//y1 + yDiff;
		double cpStemY2 = (y2*0.65 + y1*0.35);//yDiff;
		//Outer bubble point
		double bubbleY = bubbleDirection*bubbleSize + midY;
		//Bubble control points will almost form a triangle - this makes the sides nearly even
		double temp = direction*Math.abs(bubbleY - midY)/sqrt3;
		double cpBubbleX1 = midX - temp;
		double cpBubbleX2 = midX + temp;
		//Stem points
		double stemX1 = (cpBubbleX1*0.25 + midX*0.75);
		double stemX2 = (cpBubbleX2*0.25 + midX*0.75);
		//Move the stem points away from the base line (up or down)
		double stemY1 = (bubbleY*0.25 + cpStemY1*0.75);
		double stemY2 = (bubbleY*0.25 + cpStemY2*0.75);

		controls[index*6].x=cp*0.6 + x1*0.4;
		controls[index*6].y=y1;
		controls[index*6+1].x=midX;
		controls[index*6+1].y=cpStemY1;
		corners[index*4 + 1].x=stemX1;
		corners[index*4 + 1].y=stemY1;

		controls[index*6+2].x=cpBubbleX1;
		controls[index*6+2].y=bubbleY;
		corners[index*4 + 2].x=midX;
		corners[index*4 + 2].y=bubbleY;
		
		controls[index*6+3].x=cpBubbleX2;
		controls[index*6+3].y=bubbleY;
		corners[index*4 + 3].x=stemX2;
		corners[index*4 + 3].y=stemY2;

		controls[index*6+4].x=midX;
		controls[index*6+4].y=cpStemY2;
		controls[index*6+5].x=cp*0.6 + x2*0.4;
		controls[index*6+5].y=y2;
	}
}
