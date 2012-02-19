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

/**
 * Changelog:
 * 
 * 2011 12 19 - Jon
 *  - Finished implementing save and load functions
 */
package hulka.tilemanager;

import java.io.PrintWriter;
import java.io.BufferedReader;
import hulka.util.ArrayReader;
import hulka.util.ArrayWriter;

/**
 * Stores the parameters required to describe a set of tiles.
 */
public class TileSetDescriptor implements Cloneable
{
	/**
	 * Display area width - initialized by {@link AbstractTileManagerImpl}'s constructor.
	 */
	public int boardWidth;
	/**
	 * Display area height - initialized by {@link AbstractTileManagerImpl}'s constructor}.
	 */
	public int boardHeight;
	/**
	 * Column count - initialized by {@link AbstractTileManagerImpl}'s constructor.
	 */
	public int tilesAcross;
	/**
	 * Row count - initialized by {@link AbstractTileManagerImpl}'s constructor.
	 */
	public int tilesDown;
	/**
	 * Edge tile fitting - initialized by {@link AbstractTileManagerImpl}'s constructor.
	 * If this is true, edge tiles will be adjusted to fit board dimensions.
	 */
	public boolean fitEdgeTiles;
	/**
	 * Offset of leftmost column from the origin - initialized by {@link AbstractTileManagerImpl}'s constructor.
	 */
	public int leftOffset;
	/**
	 * Offset of the topmost row from the origin - initialized by {@link AbstractTileManagerImpl}'s constructor.
	 */
	public int topOffset;
	/**
	 * Scale factor required to adjust tile height to the ideal ratio ({@link #heightWidthRatio}) - initialized by {@link AbstractTileManagerImpl}'s constructor.
	 */
	public double scaleFactor;
	/**
	 * Tile width - to be initialized by {@link AbstractTileManagerImpl#initTileSetDescriptor(TileSetDescriptor)}.
	 */
	public int tileWidth;
	/**
	 * Tile height - to be initialized by {@link AbstractTileManagerImpl#initTileSetDescriptor(TileSetDescriptor)}.
	 */
	public int tileHeight;
	/**
	 * Horizontal space required by a tile - to be initialized by {@link AbstractTileManagerImpl#initTileSetDescriptor(TileSetDescriptor)}.
	 */
	public int tileSpacingX;
	/**
	 * Vertical space required by a tile - to be initialized by {@link AbstractTileManagerImpl#initTileSetDescriptor(TileSetDescriptor)}.
	 */
	public int tileSpacingY;
	/**
	 * Total number of tiles - to be initialized by {@link AbstractTileManagerImpl#initTileSetDescriptor(TileSetDescriptor)}.
	 */
	public int tileCount;
	/**
	 * The number of discrete rotations in 360 degrees - to be initialized by {@link AbstractTileManagerImpl#initTileSetDescriptor(TileSetDescriptor)}.
	 */
	public int rotationSteps;
	/**
	 * The number of sides to a tile - to be initialized by {@link AbstractTileManagerImpl#initTileSetDescriptor(TileSetDescriptor)}.
	 * If all tiles are oriented the same way, this should equal rotationSteps.
	 */
	public int sideCount;
	/**
	 * Ideal height:width ratio - to be initialized by {@link AbstractTileManagerImpl#initTileSetDescriptor(TileSetDescriptor)}.
	 * Scaled to this ratio, a uniform tile's sides should be equal in length.
	 */
	public double heightWidthRatio;
	/**
	 * Margin space needed for drawing - to be initialized by {@link AbstractTileManagerImpl#initTileSetDescriptor(TileSetDescriptor)}.
	 */
	public int tileMargin;
	
	public Object clone() throws CloneNotSupportedException
	{
		return super.clone();
	}
	
	public String toString()
	{
		return "\nboardWidth " + boardWidth
				+ "\nboardHeight " + boardHeight
				+ "\ntilesAcross " + tilesAcross
				+ "\ntilesDown  " + tilesDown
				+ "\nfitEdgeTiles " + fitEdgeTiles
				+ "\nleftOffset " + leftOffset
				+ "\ntopOffset " + topOffset
				+ "\nscaleFactor " + scaleFactor
				+ "\ntileWidth " + tileWidth
				+ "\ntileHeight " + tileHeight
				+ "\ntileSpacingX " + tileSpacingX
				+ "\ntileSpacingY " + tileSpacingY
				+ "\ntileCount " + tileCount
				+ "\nrotationSteps " + rotationSteps
				+ "\nsideCount " + sideCount
				+ "\nheightWidthRatio " + heightWidthRatio
				+ "\ntileMargin " + tileMargin;
	}
	
	private static enum Property{BOARD_WIDTH, BOARD_HEIGHT, TILES_ACROSS, TILES_DOWN, FIT_EDGE_TILES, LEFT_OFFSET, TOP_OFFSET, SCALE_FACTOR, TILE_WIDTH, TILE_HEIGHT, TILE_SPACING_X, TILE_SPACING_Y, TILE_COUNT, ROTATION_STEPS, SIDE_COUNT, HEIGHT_WIDTH_RATIO, TILE_MARGIN};
	private static String [] propertyNames={"boardWidth", "boardHeight", "tilesAcross", "tilesDown", "fitEdgeTiles", "leftOffset", "topOffset", "scaleFactor", "tileWidth", "tileHeight", "tileSpacingX", "tileSpacingY", "tileCount", "rotationSteps", "sideCount", "heightWidthRatio", "tileMargin"};
	public boolean save(PrintWriter out, PrintWriter err)
	{
		//Since ArrayWriter deals with int values, fitEdgeTiles, scaleFactor, and HeightWidthRatio must be converted appropriately
		int [][] values = {{boardWidth}, {boardHeight}, {tilesAcross}, {tilesDown}, {fitEdgeTiles?0:1}, {leftOffset}, {topOffset}, {(int)(scaleFactor*0x01000000)}, {tileWidth}, {tileHeight}, {tileSpacingX}, {tileSpacingY}, {tileCount}, {rotationSteps}, {sideCount}, {(int)(heightWidthRatio*0x01000000)}, {tileMargin}};
		return new ArrayWriter(values.length,1,"TileSetDescriptor").save(values,propertyNames,out,err);
	}

	public static TileSetDescriptor load(BufferedReader in, PrintWriter err)
	{
		TileSetDescriptor result=null;
		ArrayReader reader=new ArrayReader("TileSetDescriptor");
		if(reader.load(in,err))
		{
			TileSetDescriptor d=new TileSetDescriptor();
			boolean ok=true;
			for(Property p: Property.values())
			{
				int [] values=reader.getColumn(propertyNames[p.ordinal()],err);
				ok=values!=null;
				if(!ok)
				{
					break;
				}
				else switch(p)
				{
					case BOARD_WIDTH: d.boardWidth = values[0]; break;
					case BOARD_HEIGHT: d.boardHeight = values[0]; break;
					case TILES_ACROSS: d.tilesAcross = values[0]; break;
					case TILES_DOWN: d.tilesDown = values[0]; break;
					//Convert back to int
					case FIT_EDGE_TILES: d.fitEdgeTiles = (values[0]==0?false:true); break;
					case LEFT_OFFSET: d.leftOffset = values[0]; break;
					case TOP_OFFSET: d.topOffset = values[0]; break;
					//Convert back to double
					case SCALE_FACTOR: d.scaleFactor = ((double)values[0])/((double)0x01000000); break;
					case TILE_WIDTH: d.tileWidth = values[0]; break;
					case TILE_HEIGHT: d.tileHeight = values[0]; break;
					case TILE_SPACING_X: d.tileSpacingX = values[0]; break;
					case TILE_SPACING_Y: d.tileSpacingY = values[0]; break;
					case TILE_COUNT: d.tileCount = values[0]; break;
					case ROTATION_STEPS: d.rotationSteps = values[0]; break;
					case SIDE_COUNT: d.sideCount = values[0]; break;
					//Convert back to double
					case HEIGHT_WIDTH_RATIO: d.heightWidthRatio = ((double)values[0])/((double)0x01000000); break;
					case TILE_MARGIN: d.tileMargin = values[0]; break;
					default:
						ok=false;
						err.println("TileSetDescriptor: unknown property: " + p.toString() + ".");
						break;
				}
			}
			if(ok) result=d;
		}
		return result;
	}
}
