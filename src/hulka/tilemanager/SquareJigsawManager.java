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
 * 
 * 2012 02 23 - Jon
 * Added getOldBoardSize to allow save file dimensions to be passed out (see notes in JigsawHandler)
 * 
 * 2012 02 02 - Jon
 *  - Implemented scaling when loading a puzzle on a different screen resolution.
 * 
 * 2011 12 19 - Jon
 *  - Finished implementing save and load functions
 * 
 * 2011 12 07 - Jon
 * - Moved generic functionality into JigsawCutter - it now handles shape generation for SquareJigsawManager and HexJigsawManager
 * 2011 12 02 - Jon
 * - Removed redundant and confusing neighborOffsetIndex setup code from the constructor - this is handled by SquareTileManager
 * 2010 07 09 - Jon
 * - enhanced initTilesetDescriptor and randomize functions:
 *  Corner variance is now used for optimized tile sizing and edge adjustment.
 * 2010 07 26 - Jon
 * - Made tile size optimization code more consistent with HexJigsawManager
 */
package hulka.tilemanager;
import java.util.Random;
import java.awt.geom.Point2D;
import java.awt.Point;
import java.awt.Shape;
import java.awt.Dimension;

import java.io.PrintWriter;
import java.io.BufferedReader;

public class SquareJigsawManager extends SquareTileManager
{
	JigsawCutter cutter;
	//Only count direct neighbors
	private static int NEIGHBOR_COUNT = SIDE_COUNT;
	//Tweak this to get the bubbles big enough
	private static double bubbleMinFactor = 0.30;
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

	//Temporary storage to avoid excessive heap usage
	private Point tempIndex=new Point();

	public SquareJigsawManager(int width, int height, int tilesAcross, int tilesDown)
	{
		super(width,height,tilesAcross,tilesDown);
		initCutter();

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
		//Special indexing - pad all edges by one tile - this is necessary so that edge tiles have neighbors
		extTA=descriptor.tilesAcross+2;
		extTD=descriptor.tilesDown+2;
		extTC=extTA*extTD;

		//Set up corner and edge indexing
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
			//This is insignificant for square tiles, since corner 0 is always used. It is here for compatibility with the more generic algorithm
			cornerIndexOffset[i] = 0;
			cornerIndexOffset[i+2] = 0;
		}

		//Set up the JigsawCutter
		cutter=new JigsawCutter(
			descriptor,
			0.0,
			maskPoints,
			cornerTileOffset,
			cornerIndexOffset,
			edgeTileOffset)
		{
			public int getExtFlatIndex(Point tileIndex)
			{
				int result=-1;
				//pad the edges
				int x=tileIndex.x+1;
				int y=tileIndex.y+1;
				if(x >=0 && x < extTA && y >= 0 && y < extTD)
					result=y*extTA + x;
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
	}

	public Shape getTileMask(int flatIndex)
	{
		return cutter.getTileMask(getExpandedIndex(flatIndex,tempIndex));
	}

	private void randomize()
	{
		//Unable to move randomize into JigsawCutter - edge case handling is too different between tile shapes.
		Random random = new Random();
		Point extIndex=new Point();
		for(extIndex.y=-1; extIndex.y<=descriptor.tilesDown; extIndex.y++)
		{
			for(extIndex.x=-1; extIndex.x<=descriptor.tilesAcross; extIndex.x++)
			{
				//Convert to flat indexing
				int index=cutter.getExtFlatIndex(extIndex);
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
			int index=cutter.getExtFlatIndex(extIndex);
			cornerOffsetX[0][index]=-leftVariance;

			//Right edge
			extIndex.x=descriptor.tilesAcross;
			//Convert to flat indexing
			index=cutter.getExtFlatIndex(extIndex);
			cornerOffsetX[0][index]=rightVariance;
		}

		//Fix up the board's top and bottom edges
		for(extIndex.x=-1; extIndex.x<=descriptor.tilesAcross; extIndex.x++)
		{
			//Left edge
			extIndex.y=0;
			//Convert to flat indexing
			int index=cutter.getExtFlatIndex(extIndex);
			cornerOffsetY[0][index]=-topVariance;

			//Right edge
			extIndex.y=descriptor.tilesDown;
			//Convert to flat indexing
			index=cutter.getExtFlatIndex(extIndex);
			cornerOffsetY[0][index]=bottomVariance;
		}
	}
	
	public boolean save(PrintWriter out, PrintWriter err)
	{
		boolean result=true;
		if(result) result=descriptor.save(out,err);
		if(result) result=cutter.save(out,err);
		return result;
	}

	/**
	 * The 'scaling' constructor.
	 */
	protected SquareJigsawManager(TileSetDescriptor oldDescriptor, int boardWidth, int boardHeight)
	{
		super(oldDescriptor,boardWidth,boardHeight);
	}

	public static SquareJigsawManager load(BufferedReader in, PrintWriter err,Dimension boardSize)
	{
		SquareJigsawManager result=null;
		TileSetDescriptor oldDescriptor=TileSetDescriptor.load(in,err);
		if(oldDescriptor!=null)
		{
			//Use the 'scaling' constructor
			result=new SquareJigsawManager(oldDescriptor, boardSize.width, boardSize.height);
			result.oldBoardSize=new Dimension(oldDescriptor.boardWidth, oldDescriptor.boardHeight);
			result.initCutter();
			if(!result.cutter.load(in,err,oldDescriptor.tileWidth,oldDescriptor.tileHeight,result.descriptor.tileWidth,result.descriptor.tileHeight))
			{
				result=null;
			}
		}
		return result;
	}

	/**
	 * This is specific to loading
	 */
	public TileSetDescriptor adjustTileSetDescriptor(TileSetDescriptor oldDescriptor, int boardWidth, int boardHeight)
	{
		if(boardWidth!=oldDescriptor.boardWidth||boardHeight!=oldDescriptor.boardHeight)
		{
			//Board size has changed - create a new descriptor to reflect the changes
			descriptor = new TileSetDescriptor();
			descriptor.boardWidth=boardWidth;
			descriptor.boardHeight=boardHeight;
			descriptor.tilesAcross=oldDescriptor.tilesAcross;
			descriptor.tilesDown=oldDescriptor.tilesDown;
			descriptor.fitEdgeTiles=oldDescriptor.fitEdgeTiles;
			descriptor.tileCount=oldDescriptor.tileCount;
			descriptor.rotationSteps=oldDescriptor.rotationSteps;
			descriptor.sideCount=oldDescriptor.sideCount;
			descriptor.heightWidthRatio=oldDescriptor.heightWidthRatio;
			//Best width
			int tW=oldDescriptor.tileWidth*descriptor.boardWidth/oldDescriptor.boardWidth;
			//Best height
			int tH=oldDescriptor.tileHeight*descriptor.boardHeight/oldDescriptor.boardHeight;
			//Use the smaller tile size
			tW=tW<=tH?tW:tH;
			descriptor.tileWidth=tW;
			descriptor.tileHeight=tW;
			descriptor.tileSpacingX=tW;
			descriptor.tileSpacingY=tW;
			//This will degrade if a puzzle is repeatedly saved and loaded at different resolutions
			descriptor.tileMargin=(int)(oldDescriptor.tileMargin*tW/((int)((float)oldDescriptor.tileWidth+0.5)));
			
		}
		else
		{
			descriptor=oldDescriptor;
		}
		return descriptor;
	}

	private Dimension oldBoardSize=null;
	/**
	 * If this SquareJigsawManager results from calling the {@link #load(BufferedReader, PrintWriter,Dimension)}, returns the old tile set dimensions.
	 * @return boardSize from the save file, before scaling, or null if this TileManager was not loaded from a file.
	 */
	public Dimension getOldBoardSize()
	{
		return oldBoardSize;
	}
}
