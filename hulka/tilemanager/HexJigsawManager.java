/**
 *   Copyright (C) 2010, 2011 Jonathan Hulka (jon.hulka@gmail.com)
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
 * 
 * 2011 12 07 - Jon
 * - Moved generic functionality into JigsawCutter - it now handles shape generation for SquareJigsawManager and HexJigsawManager
 * 2011 02 12 - Jon
 * - Cleaned up getTileMask - generalized the algorithm a bit to eliminate redundant code.
 * 2010 07 26 - Jon
 * - enhanced initTilesetDescriptor and getTileMask functions:
 *   corner variance is now used for optimized tile sizing and edge adjustment.
 * 2010 07 27 Jon
 * - Cleaned up and moved most of the edge clipping functionality into randomize()
 */
package hulka.tilemanager;
import java.util.Random;
import java.awt.geom.Point2D;
import java.awt.Point;
import java.awt.Shape;
import java.awt.Dimension;

import java.io.PrintWriter;
import java.io.BufferedReader;

/**
 * @todo look into using margins to get more flexibility around the edges - 12-piece puzzles aren't always using the whole image.
 */
public class HexJigsawManager extends HexTileManager
{
	JigsawCutter cutter;
	//Indices to help with drawing - these are indexed differently, with extra tiles around the edges
	private int extTA,extTD,extTC;

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
	
	//Indexing corners and edges
	private Point cornerTileOffset[] = new Point[6];
	private int cornerIndexOffset[] = new int[6];
	private Point edgeTileOffset[] = new Point[6];

	//Tile drawing information - to be passed to cutter
	//Corners
	private int [][] cornerOffsetX=new int[2][];
	private int [][] cornerOffsetY=new int[2][];
	//Edges
	private int [][] bubbleSize=new int[3][];
	private int [][] bubbleDirection=new int[3][];
	private int [][] controlPointOffset=new int[3][];

	//Temporary storage to avoid excessive heap usage
	private Point tempIndex=new Point();

	/**
	 * This constructor is used for minimal setup when data is being loaded rather than generated.
	 */
	private HexJigsawManager(TileSetDescriptor descriptor)
	{
		super(descriptor);
		initCutter();

		//Since AbstractTileManagerImpl's constructor will not be called:
		rotationStep=2.0*Math.PI/((double)descriptor.rotationSteps);
		originalIndex=new int[descriptor.tileCount];
		rotation = new int[descriptor.tileCount];
		for(int i=0; i<descriptor.tileCount; i++)
		{
			originalIndex[i]=i;
			rotation[i]=0;
		}
	}

	public HexJigsawManager(int width, int height, int tilesAcross, int tilesDown)
	{
		super(width,height,tilesAcross,tilesDown,true);
		initCutter();

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


		//Set up the drawing data
		randomize();
		//And pass it to the cutter
		cutter.setDrawingData(
			cornerOffsetX,
			cornerOffsetY,
			bubbleSize,
			bubbleDirection,
			controlPointOffset);
	}
	
	//Shared by the constructors
	private void initCutter()
	{
		//Special indexing - these are used by cutter.getFlatIndex (see cutter initializatin below)
		extTA=descriptor.tilesAcross+3;
		extTD=descriptor.tilesDown+2;

		//Don't condense this function - it relies on integer rounding
		extTC=((extTA+1)/2)*((extTD+1)/2) + (extTA/2)*(extTD/2);

		//Set up corner and edge indexing
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
			//These values determines whether the tile will be using its or its neighbor's corner 0 (cornerIndexOffset[i]=0) or corner 3 (cornerIndexOffset[i]=1)
			//cornerIndexOffset is used as the first index to the two-dimensional arrays cornerOffsetX and cornerOffsetY
			cornerIndexOffset[i] = i%2;
			cornerIndexOffset[i+3] = (i+3)%2;
			cornerTileOffset[i+3] = new Point(-cornerTileOffset[i].x,-cornerTileOffset[i].y);
		}

		//Set up the JigsawCutter
		cutter=new JigsawCutter(
			descriptor,
			Math.PI/6.0,
			maskPoints,
			cornerTileOffset,
			cornerIndexOffset,
			edgeTileOffset)
		{
			public int getExtFlatIndex(Point tileIndex)
			{
				int result=-1;
				int x=tileIndex.x+2;
				int y=tileIndex.y+1;
				if((x+y)%2==1 && x >=0 && x<extTA && y>=0 && y<extTD)
					result=(y/2)*extTA + (y%2)*(extTA/2) + x/2;
				return result;
			}
		};
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
	}

	public Shape getTileMask(int flatIndex)
	{
		return cutter.getTileMask(getExpandedIndex(flatIndex,tempIndex));
	}

	private void randomize()
	{
		//Unable to move randomize into JigsawCutter - edge case handling is too different between tile shapes.
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
					int index=cutter.getExtFlatIndex(extIndex);
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
	
	public boolean save(PrintWriter out, PrintWriter err)
	{
		boolean result=true;
		if(result) result=descriptor.save(out,err);
		if(result) result=cutter.save(out,err);
		return result;
	}
	
	public static HexJigsawManager load(BufferedReader in, PrintWriter err,Dimension boardSize)
	{
		HexJigsawManager result=null;
		TileSetDescriptor descriptor=TileSetDescriptor.load(in,err);
		if(descriptor.boardWidth>boardSize.width || descriptor.boardHeight>boardSize.height)
		{
			//For now - not allowing the display area to change
			err.println("You have changed your screen settings.\nAt this time, puzzles cannot be loaded at a lower screen resolution than they were saved at.\nA puzzle scaling feature is currently under development.");
			descriptor=null;
		}
//Todo scale to fit here - it might make most sense to give descriptor the new size and let it figure out the scaling
		if(descriptor!=null)
		{
			result=new HexJigsawManager(descriptor);
			if(!result.cutter.load(in,err))
			{
				result=null;
			}
		}
//todo scale the cutter here - probably implement a scale function via descriptor
		return result;
	}
}
