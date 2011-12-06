/**   Copyright (C) 2010  Jonathan Hulka (jon.hulka@gmail.com)
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
 * 2011 02 12 - Jon
 * - Cleaned up getTileMask - generalized the algorithm a bit to eliminate redundant code.
 * 2010 07 26 - Jon
 * - enhanced initTilesetDescriptor and getTileMask functions:
 *   corner variance is now used for optimized tile sizing and edge adjustment.
 * 2010 07 27 Jon
 * - Cleaned up and moved most of the edge clipping functionality into randomize()
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
 * Adds jigsaw shapes to HexTileManager.
 * To do - rework getTileMask:
 * - Clean up rendering
 * - Shape should be stored and copied
 */

public class HexJigsawManager extends HexTileManager
{
	//Only count direct neighbors
	private static int NEIGHBOR_COUNT = SIDE_COUNT;
	//Tweak this to get the bubbles big enough
	private static double bubbleMinFactor = 0.20;
	//Tweak this to get the bubbles small enough
	private static double bubbleMaxFactor = 0.25;
	//Tweak this to adjust the curve sharpness
	private static double controlPointVarianceFactor = 0.05;
	//Tweak this to adjust shape variance and size flexibility (Values >= 0.1 should absorb most aspect ratio differences)
	private static double cornerVarianceFactor = 0.1;
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
	private int [][] bubbleSize=new int[3][];
	private int [][] bubbleDirection=new int[3][];
	private int [][] controlPointOffset=new int[3][];
	//Indices to help with drawing - these are indexed differently, with extra tiles around the edges
	private int extTA,extTD,extTC;
	private Point cornerTileOffset[] = new Point[6];
	private int cornerIndexOffset[] = new int[6];
	private Point edgeTileOffset[] = new Point[6];

	AffineTransform[] reverseMaskTransform = new AffineTransform[NEIGHBOR_COUNT/2];
	AffineTransform[] maskTransform = new AffineTransform[NEIGHBOR_COUNT/2];


	//Temporary storage for drawing function
	Point2D.Double [] corners = new Point2D.Double[NEIGHBOR_COUNT*4];
	Point2D.Double [] controls = new Point2D.Double[NEIGHBOR_COUNT*6];
	Point2D.Double a=new Point2D.Double(), b=new Point2D.Double();

	//Temporary storage to avoid excessive heap usage
	private Point tempIndex=new Point(), tempNeighbor=new Point();

	public HexJigsawManager(int width, int height, int tilesAcross, int tilesDown)
	{
		super(width,height,tilesAcross,tilesDown,true);

		for(int i = 0; i < 2; i++)
		{
			cornerOffsetX[i] = new int[extTC];
			cornerOffsetY[i] = new int[extTC];
		}
		
		for(int i = 0; i < 3; i++)
		{
			bubbleSize[i] = new int[extTC];
			bubbleDirection[i] = new int[extTC];
			controlPointOffset[i] = new int[extTC];
		}

		//Edge stretching for better fit
		int topOffset=descriptor.topOffset+descriptor.tileHeight/4;
		int bottomOffset=descriptor.boardHeight-topOffset-(descriptor.tilesDown*3-1)*descriptor.tileHeight/4;
		int leftOffset=descriptor.leftOffset+descriptor.tileWidth/2;
		int rightOffset=descriptor.boardWidth-leftOffset-(descriptor.tilesAcross-1)*descriptor.tileWidth/2;
		topVariance = cornerVariance>topOffset?topOffset:cornerVariance;//-((descriptor.topOffset + descriptor.tileHeight/4) > cornerVariance ? cornerVariance : (descriptor.topOffset + descriptor.tileHeight/4));
		bottomVariance = cornerVariance>bottomOffset?bottomOffset:cornerVariance;//((descriptor.boardHeight-descriptor.topOffset + descriptor.tileHeight/4) > cornerVariance ? cornerVariance : (descriptor.boardHeight-descriptor.topOffset + descriptor.tileHeight/4));
		leftVariance = cornerVariance>leftOffset?leftOffset:cornerVariance;
		rightVariance=cornerVariance>rightOffset?rightOffset:cornerVariance;

		randomize();
		
		Point thisTile = new Point(0,0);
		cornerTileOffset[0] = thisTile;
		cornerTileOffset[1] = new Point(1,-1);
		cornerTileOffset[2] = new Point(1,1);
		for(int i = 0; i < 3; i++)
		{
			//Hex tiles 'own' edges 0, 1, and 2 and use neighbors' edges 3, 4, and 5
			//These values determine which neighbor a given edge is to be borrowed from
			edgeTileOffset[i] = thisTile;
			edgeTileOffset[i+3]=neighborOffsetIndex[i+3];
			//Hex tiles 'own' corners 0 and 3, and use neighbors' corners for 2 through 5
			//These values determines whether the tile will be using its or its neighbor's corner 0 (cornerIndexOffset[i]=0) or corner 3 (cornerIndexOffset[i]=3)
			//cornerIndexOffset is used as the first index to the two-dimensional arrays cornerOffsetX and cornerOffsetY
			cornerIndexOffset[i] = i%2;
			cornerIndexOffset[i+3] = (i+3)%2;
			cornerTileOffset[i+3] = new Point(-cornerTileOffset[i].x,-cornerTileOffset[i].y);
		}

		for(int j=0; j<NEIGHBOR_COUNT; j++)
		{
			for(int i=0; i<4; i++) corners[i*NEIGHBOR_COUNT+j]=new Point2D.Double();
			for(int i=0; i<6; i++) controls[i*NEIGHBOR_COUNT+j]=new Point2D.Double();
		}
		for(int i=0; i<NEIGHBOR_COUNT/2; i++)
		{
			double theta = i*2.0*Math.PI/NEIGHBOR_COUNT + Math.PI/6.0;
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
		//Optimal tile width
		int tW=2*d.boardWidth/(d.tilesAcross-1);
		//Optimal tile height
		int tH=4*(d.boardHeight/(d.tilesDown*3-1));

		int tW2=(int)(tH*sqrt3/2);
		//Minimum corner variance - to make sure maximum width and height are safe
		int cv=(int)((tW2>tW?tW:tW2)*cornerVarianceFactor);

		//maximum tile width, allowing for corner variance
		int maxTW=(int)(((double)descriptor.boardWidth+2.0*cv)/((double)descriptor.tilesAcross-1.0)*2.0);
		//maximum tile height, allowing for corner variance
		int maxTH=(int)(((double)descriptor.boardHeight+2.0*cv)/((double)descriptor.tilesDown*3.0-1.0)*4.0);

		//Use the smallest maximum tile size
		int maxTW2=(int)(maxTH*sqrt3/2);
		maxTW=maxTW<=maxTW2?maxTW:maxTW2;

		//Get an average tile size - This has the best chance of fitting both width and height
		tW=tW/2+(int)(tH*sqrt3);

		//Make sure tile size doesn't exceed the maximum
		tW=tW<=maxTW?tW:maxTW;
		tH=(int)(2*tW/sqrt3);

		//Make tH an integral multiple of 4, to ensure accuracy calculating spacing
		tH-=tH%4;
		//Make tW an integral multiple of 2, to ensure accuracy caclulating spacing
		tW-=tW%2;

		descriptor.tileHeight=tH;
		descriptor.tileWidth=tW;
		descriptor.tileSpacingX=tW/2;
		descriptor.tileSpacingY=tH*3/4;

		controlPointVariance = (int)(descriptor.tileWidth*controlPointVarianceFactor);
		cornerVariance = (int)(descriptor.tileWidth*cornerVarianceFactor);
		bubbleMin = (int)((descriptor.tileWidth - cornerVariance*2)*bubbleMinFactor);
		bubbleMax = (int)((descriptor.tileWidth - cornerVariance*2)*bubbleMaxFactor);
		d.tileMargin = cornerVariance + bubbleMax;

		//Special indexing
		extTA=descriptor.tilesAcross+3;
		extTD=descriptor.tilesDown+2;

		//Don't condense this function - it relies on integer rounding
		extTC=((extTA+1)/2)*((extTD+1)/2) + (extTA/2)*(extTD/2);
	}
	
	/**
	 * Special indexing includes tiles past the edge of the board
	 */
	private int getExtFlatIndex(Point tileIndex)
	{
		int result=-1;
		int x=tileIndex.x+2;
		int y=tileIndex.y+1;
		if((x+y)%2==1 && x >=0 && x<extTA && y>=0 && y<extTD)
			result=(y/2)*extTA + (y%2)*(extTA/2) + x/2;
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
		int midX=descriptor.tileWidth/2;
		int clipY=descriptor.tileHeight/4;
		Point extIndex=new Point();
		for(extIndex.y=-1; extIndex.y<=descriptor.tilesDown; extIndex.y++)
		{
			for(extIndex.x=-2; extIndex.x<=descriptor.tilesAcross; extIndex.x++)
			{
				if((extIndex.x+extIndex.y)%2==0)
				{
					//Convert to flat indexing
					int index=getExtFlatIndex(extIndex);
					for(int k=0; k < 2; k++)
					{
						cornerOffsetX[k][index] = random.nextInt(cornerVariance*2 - 1) - cornerVariance + 1;
						cornerOffsetY[k][index] = random.nextInt(cornerVariance*2 - 1) - cornerVariance + 1;
					}

					for(int k=0; k < 3; k++)
					{
						bubbleSize[k][index] = random.nextInt(bubbleMax - bubbleMin) + bubbleMin;
						bubbleDirection[k][index] = random.nextInt(2)*2-1;
						controlPointOffset[k][index] = controlPointVariance==0 ? 0 : random.nextInt(controlPointVariance*2 - 1) - controlPointVariance + 1;
					}
				
					//Handle edge clipping
					if(extIndex.x==-2)
					{
						//Outside tile touches the left edge of the board
						//Flatten the middle bubble
						bubbleSize[1][index]=0;
						controlPointOffset[1][index]=0;
					}
					else if(extIndex.x==-1)
					{
						//Outside tile clipped at left edge of the board
						//Level the right edge and adjust for sizing
						for(int k=0; k < 2; k++)
						{
							cornerOffsetX[k][index]=midX-leftVariance;
						}

						//Flatten the bubbles
						for(int k=0; k < 3; k++)
						{
							bubbleSize[k][index]=0;
							controlPointOffset[k][index]=0;
						}
					}
					else if(extIndex.x==0)
					{
						//inside Tile clipped at left edge of the board
						//adjust for sizing
						for(int k=0; k < 2; k++)
						{
							cornerOffsetX[k][index]=-leftVariance;
						}
					}
					else if(extIndex.x==descriptor.tilesAcross-2)
					{
						//Tile edges touching right edge
						//Flatten the rightmost bubble
						bubbleSize[1][index]=0;
						controlPointOffset[1][index]=0;
					}
					else if(extIndex.x==descriptor.tilesAcross-1)
					{
						//Tile corners touching right edge
						//Adjust for sizing
						for(int k=0; k < 2; k++)
						{
							cornerOffsetX[k][index]=rightVariance;
						}
						
						//Tile edges clipped at the right edge
						//Flatten the rightmost three bubbles
						for(int k=0; k < 3; k++)
						{
							bubbleSize[k][index]=0;
							controlPointOffset[k][index]=0;
						}
					}
					else if(extIndex.x==descriptor.tilesAcross)
					{
						//Tile corners clipped at right edge
						//Level the right edge and adjust for sizing
						for(int k=0; k < 2; k++)
						{
							cornerOffsetX[k][index]=rightVariance-midX;
						}
					}

					if(extIndex.y==-1)
					{
						//Tiles above the board
						//Adjust the bottom corner for sizing
						cornerOffsetY[1][index]=-topVariance;
						//Flatten the bottom bubble
						bubbleSize[2][index]=0;
						controlPointOffset[2][index]=0;
					}
					else if(extIndex.y==0)
					{
						//Tiles clipped at top
						//Level the top corner and adjust for sizing
						cornerOffsetY[0][index]=clipY-topVariance;
						//Flatten the top bubble
						bubbleSize[0][index]=0;
						controlPointOffset[0][index]=0;
					}
					else if(extIndex.y==descriptor.tilesDown-1)
					{
						//Tiles clipped at bottom
						//Level the bottom corner and adjust for sizing
						cornerOffsetY[1][index]=bottomVariance-clipY;
						//flatten the bottom bubble
						bubbleSize[2][index]=0;
						controlPointOffset[2][index]=0;
					}
					else if(extIndex.y==descriptor.tilesDown)
					{
						//Tiles below the board
						//Adjust the top corner for sizing
						cornerOffsetY[0][index]=bottomVariance;
						//Flatten the top bubble
						bubbleSize[0][index]=0;
						controlPointOffset[0][index]=0;
					}
				}
			}
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
