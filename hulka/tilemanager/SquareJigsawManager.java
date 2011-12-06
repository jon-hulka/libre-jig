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
 * changelog
 * 2011 12 02 - Jon
 *  - Removed redundant and confusing neighborOffsetIndex setup code from the constructor - this is handled by SquareTileManager
 * 2010 07 09 - Jon
 * - enhanced initTilesetDescriptor and randomize functions:
 *  Corner variance is now used for optimized tile sizing and edge adjustment.
 * 2010 07 26 - Jon
 * - Made tile size optimization code more consistent with HexJigsawManager
*/
package hulka.tilemanager;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.Point;
import java.awt.Shape;
import java.util.Random;

/**
 * Adds jigsaw shapes to SquareTileManager.
 * To do - make drawing functions more inline with HexJigsawManager:
 *  - eventually these should be abstracted to the point that they can be
 *    pulled out into a class of their own.
 */
public class SquareJigsawManager extends SquareTileManager
{
	//This will be used in some calculations
	protected static double sqrt3 = Math.sqrt(3);

	//Only count direct neighbors
	private static int NEIGHBOR_COUNT = SIDE_COUNT;
	//Tweak this to get the bubbles big enough
	private static double bubbleMinFactor = 0.20;
	//Tweak this to get the bubbles small enough
	private static double bubbleMaxFactor = 0.40;
	//Tweak this to adjust the curve sharpness
	private static double controlPointVarianceFactor = 0.05;
	//Tweak this to adjust shape variance and size flexibility (Values >= 0.1 should absorb most aspect ratio differences)
	private static double cornerVarianceFactor = 0.12;
	//There's a lot more to tweak in buildEdge
	
	private int controlPointVariance;
	private int cornerVariance;
	private int topVariance, bottomVariance, leftVariance, rightVariance;
	private int bubbleMin;
	private int bubbleMax;
	
	//Tile drawing information
	//Corners
	private int [][] cornerOffsetX=new int[2][];
	private int [][] cornerOffsetY=new int[2][];
	//Edges
	private int [][] bubbleSize=new int[2][];
	private int [][] bubbleDirection=new int[2][];
	private int [][] controlPointOffset=new int[2][];
	//Indices to help with drawing - these are indexed differently, with extra tiles around the edges
	private int extTA,extTD,extTC;
	private Point cornerTileOffset[] = new Point[4];
	private int cornerIndexOffset[] = new int[4];
	private Point edgeTileOffset[] = new Point[4];

	AffineTransform[] reverseMaskTransform = new AffineTransform[NEIGHBOR_COUNT/2];
	AffineTransform[] maskTransform = new AffineTransform[NEIGHBOR_COUNT/2];


	//Temporary storage for drawing function
	Point2D.Double [] corners = new Point2D.Double[NEIGHBOR_COUNT*4];
	Point2D.Double [] controls = new Point2D.Double[NEIGHBOR_COUNT*6];
	Point2D.Double a=new Point2D.Double(), b=new Point2D.Double();

	//Temporary storage to avoid excessive heap usage
	private Point tempIndex=new Point(), tempNeighbor=new Point();

	public SquareJigsawManager(int width, int height, int tilesAcross, int tilesDown)
	{
		super(width,height,tilesAcross,tilesDown);
		for(int i = 0; i < 2; i++)
		{
			cornerOffsetX[i] = new int[extTC];
			cornerOffsetY[i] = new int[extTC];
		}
		
		for(int i = 0; i < 2; i++)
		{
			bubbleSize[i] = new int[extTC];
			bubbleDirection[i] = new int[extTC];
			controlPointOffset[i] = new int[extTC];
		}

		//Edge stretching for better fit
		int rightOffset=descriptor.boardWidth-descriptor.leftOffset-descriptor.tileWidth*descriptor.tilesAcross;
		int bottomOffset=descriptor.boardHeight-descriptor.topOffset-descriptor.tileHeight*descriptor.tilesDown;
		topVariance = cornerVariance>descriptor.topOffset?descriptor.topOffset:cornerVariance;
		bottomVariance = cornerVariance>bottomOffset?bottomOffset:cornerVariance;
		leftVariance = cornerVariance>descriptor.leftOffset?descriptor.leftOffset:cornerVariance;
		rightVariance=cornerVariance>rightOffset?rightOffset:cornerVariance;

		randomize();
		
		Point thisTile = new Point(0,0);
		//Square tiles 'own' corner 0 and use neighbors' corners 1 through 3
		//These values determine which neighbor a given corner is to be borrowed from
		cornerTileOffset[0] = thisTile;
		cornerTileOffset[1] = new Point(1,0);
		cornerTileOffset[2] = new Point(1,1);
		cornerTileOffset[3] = new Point(0,1);
		for(int i = 0; i < 2; i++)
		{
			//Square tiles 'own' edges 0 and 1 and use neighbors' edges 2 and 3
			//These values determine which neighbor a given edge is to be borrowed from
			edgeTileOffset[i] = thisTile;
			edgeTileOffset[i+2]=neighborOffsetIndex[i+2];
			//This is insignificant for square tiles, since corner 0 is always used. It is here for compatibility with the hex tile algorithm
			cornerIndexOffset[i] = 0;
			cornerIndexOffset[i+2] = 0;
		}

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
		for(int j=0; j<NEIGHBOR_COUNT; j++)
		{
			for(int i=0; i<4; i++) corners[i*NEIGHBOR_COUNT+j]=new Point2D.Double();
			for(int i=0; i<6; i++) controls[i*NEIGHBOR_COUNT+j]=new Point2D.Double();
		}
		//The edges are rotated into position to simplify the corner/control point calculations, then rotated back for drawing the path
		//These transforms handle the rotations
		for(int i=0; i<NEIGHBOR_COUNT/2; i++)
		{
			double theta = i*2.0*Math.PI/NEIGHBOR_COUNT;
			reverseMaskTransform[i] = AffineTransform.getRotateInstance(-theta,descriptor.tileWidth/2.0, descriptor.tileHeight/2.0);
			maskTransform[i] = AffineTransform.getRotateInstance(theta,descriptor.tileWidth/2.0, descriptor.tileHeight/2.0);
		}
	}
	
	/**
	 * Overridden for tileMargin adjustment and tile size optimizing
	 */
	public void initTileSetDescriptor(TileSetDescriptor d)
	{
		super.initTileSetDescriptor(d);

		//Adjust width and height for best fit; cornerVariance gives some allowance for stretching and squeezing edges.
		//First split the difference between optimal width and optimal height
		int tW=(descriptor.boardWidth+descriptor.boardHeight)/(descriptor.tilesAcross+descriptor.tilesDown);
		//Then make sure the result will fit inside the board boundaries.
		int maxTW=(int)((double)descriptor.boardWidth/((double)descriptor.tilesAcross-2.0*cornerVarianceFactor));
		int maxTH=(int)((double)descriptor.boardHeight/((double)descriptor.tilesDown-2.0*cornerVarianceFactor));
		tW=tW<=maxTW?(tW<=maxTH?tW:maxTH):(maxTW<=maxTH?maxTW:maxTH);
		descriptor.tileWidth=tW;
		descriptor.tileHeight=tW;
		descriptor.tileSpacingX=tW;
		descriptor.tileSpacingY=tW;

		controlPointVariance = (int)(descriptor.tileWidth*controlPointVarianceFactor);
		cornerVariance = (int)(descriptor.tileWidth*cornerVarianceFactor);
		bubbleMin = (int)((descriptor.tileWidth - cornerVariance*2)*bubbleMinFactor);
		bubbleMax = (int)((descriptor.tileWidth - cornerVariance*2)*bubbleMaxFactor);
		d.tileMargin = cornerVariance + bubbleMax;

		//Special indexing - pad all edges by one tile - this is necessary so that edge tiles have neighbors
		extTA=descriptor.tilesAcross+2;
		extTD=descriptor.tilesDown+2;
		extTC=extTA*extTD;
	}
	
	/**
	 * Special indexing includes tiles past the edge of the board
	 */
	private int getExtFlatIndex(Point tileIndex)
	{
		int result=-1;
		//pad the edges
		int x=tileIndex.x+1;
		int y=tileIndex.y+1;
		if(x >=0 && x < extTA && y >= 0 && y < extTD)
			result=y*extTA + x;
		return result;
	}
	

	public Shape getTileMask(int flatIndex)
	{
		//This algorithm works for even-sided tiles (square and hex)
		//Each tile 'owns' half its edges while the other half are 'owned' by neighboring tiles.
		//Triangular tiles will require a different approach
		Point tileIndex = getExpandedIndex(flatIndex,tempIndex);
		int extIndex=getExtFlatIndex(tileIndex);
		//Position the corners
		for(int i = 0; i < NEIGHBOR_COUNT; i++)
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

		for(int i = 0; i < NEIGHBOR_COUNT; i++)
		{
			int index=-1;
			if(i>=NEIGHBOR_COUNT/2)
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
			int topIndex=i%(NEIGHBOR_COUNT/2);
			a = (Point2D.Double)reverseMaskTransform[topIndex].transform(corners[i*4],a);
			b = (Point2D.Double)reverseMaskTransform[topIndex].transform(corners[((i+1)%NEIGHBOR_COUNT)*4],b);
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

		for(int i=0; i < NEIGHBOR_COUNT; i++)
		{
			path.curveTo((float)controls[i*6].x,(float)controls[i*6].y,(float)controls[i*6+1].x,(float)controls[i*6+1].y,(float)corners[i*4 + 1].x,(float)corners[i*4 + 1].y);
			path.quadTo((float)controls[i*6+2].x,(float)controls[i*6+2].y,(float)corners[i*4 + 2].x,(float)corners[i*4 + 2].y);
			path.quadTo((float)controls[i*6+3].x,(float)controls[i*6+3].y,(float)corners[i*4 + 3].x,(float)corners[i*4 + 3].y);
			path.curveTo((float)controls[i*6+4].x,(float)controls[i*6+4].y,(float)controls[i*6+5].x,(float)controls[i*6+5].y,(float)corners[((i+1)%NEIGHBOR_COUNT)*4].x,(float)corners[((i+1)%NEIGHBOR_COUNT)*4].y);
		}
		
		return new Area(path);
		
	}
	
	private void buildEdge(Point2D.Double [] corners, Point2D.Double [] controls, int index, double x1, double y1, double x2, double y2, double cp, double bubbleSize, double bubbleDirection)
	{
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

	private void randomize()
	{
		Random random = new Random();
		Point extIndex=new Point();
		for(extIndex.y=-1; extIndex.y<=descriptor.tilesDown; extIndex.y++)
		{
			for(extIndex.x=-1; extIndex.x<=descriptor.tilesAcross; extIndex.x++)
			{
				//Convert to flat indexing
				int index=getExtFlatIndex(extIndex);
				//Introduce some random variance to the corners
				cornerOffsetX[0][index] = random.nextInt(cornerVariance*2 - 1) - cornerVariance + 1;
				cornerOffsetY[0][index] = random.nextInt(cornerVariance*2 - 1) - cornerVariance + 1;

				for(int k=0; k < 2; k++)
				{
					bubbleSize[k][index] = random.nextInt(bubbleMax - bubbleMin) + bubbleMin;
					bubbleDirection[k][index] = random.nextInt(2)*2-1;
					controlPointOffset[k][index] = controlPointVariance==0 ? 0 : random.nextInt(controlPointVariance*2 - 1) - controlPointVariance + 1;
				}

				if(extIndex.x==-1 || extIndex.x==descriptor.tilesAcross-1)
				{
					//Tile's 'owned' edge touches the left or right edge of the board
					//Flatten the bubble
					bubbleSize[1][index]=0;
					controlPointOffset[1][index]=0;
				}

				if(extIndex.y==0 || extIndex.y==descriptor.tilesDown)
				{
					//Tile's 'owned' edge touches top or bottom of the board
					//Flatten the bubble
					bubbleSize[0][index]=0;
					controlPointOffset[0][index]=0;
				}
			}
		}
		
		//Fix up the board's right and left edges
		for(extIndex.y=-1; extIndex.y<=descriptor.tilesDown; extIndex.y++)
		{
			//Left edge
			extIndex.x=0;
			//Convert to flat indexing
			int index=getExtFlatIndex(extIndex);
			cornerOffsetX[0][index]=leftVariance;

			//Right edge
			extIndex.x=descriptor.tilesAcross;
			//Convert to flat indexing
			index=getExtFlatIndex(extIndex);
			cornerOffsetX[0][index]=rightVariance;
		}

		//Fix up the board's top and bottom edges
		for(extIndex.x=-1; extIndex.x<=descriptor.tilesAcross; extIndex.x++)
		{
			//Left edge
			extIndex.y=0;
			//Convert to flat indexing
			int index=getExtFlatIndex(extIndex);
			cornerOffsetY[0][index]=leftVariance;

			//Right edge
			extIndex.y=descriptor.tilesDown;
			//Convert to flat indexing
			index=getExtFlatIndex(extIndex);
			cornerOffsetY[0][index]=rightVariance;
		}
	}
	
//	public boolean save(PrintStream out)
//	{
/*		//Tile drawing information
		//Corners
		private int [][] cornerOffsetX=new int[2][];
		private int [][] cornerOffsetY=new int[2][];
		//Edges
		private int [][] bubbleSize=new int[3][];
		private int [][] bubbleDirection=new int[3][];
		private int [][] controlPointOffset=new int[3][];
		//Indices to help with drawing - these are indexed differently, with extra tiles around the edges
		private int extTA,extTD,extTC;
*///	}
}
