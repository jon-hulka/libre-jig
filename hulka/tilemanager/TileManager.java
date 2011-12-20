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
import java.awt.geom.Point2D;
import java.awt.Point;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.AffineTransform;

public interface TileManager
{
	/**
	 * Clockwise rotation.
	 */
	public static final int SPIN_CW = 1;
	/**
	 * Counter clockwise rotation.
	 */
	public static final int SPIN_CCW = -1;
	/**
	 * No rotation.
	 */
	public static final int SPIN_NONE = 0;
	
	/**
	 * 
	 */
	public int getBoardWidth();
	/**
	 * 
	 */
	public int getBoardHeight();
	/**
	 * Maps a 2-dimension tile index to a unique one-dimensional index in the range 0 to getTileCount() - 1.
	 * This function is the inverse of {@link #getExpandedIndex(int, Point)}.
	 * For valid values of tileIndex, getExpandedIndex(getFlatIndex(tileIndex)) must equal tileIndex
	 * @param tileIndex 2-dimensional index of the tile
	 * @return the flattened index, or -1 if tileIndex is invalid.
	 */
	public int getFlatIndex(Point tileIndex);
	
	/**
	 * Converts a flattened tile index to its 2-dimensional equivalent.
	 * This function is the inverse of {@link # getFlatIndex(Point)}
	 * For values of flatIndex in the range of 0 to getTileCount(), getFlatIndex(getExpandedIndex(flatIndex,aPoint)) must equal flatIndex
	 * The result is stored in the second parameter and returned for convenience.
	 * @param flatIndex flattened index.
	 * @param tileIndex storage for the return value, or null
	 * @return the 2-dimensional tile index, or null if flatIndex is not in the correct range.
	 */
	public Point getExpandedIndex(int flatIndex, Point expandedIndex);

	/**
	 * Returns the indexed tile's neighbors.
	 * The number of elements returned must always be returned in the same order.
	 * @param flatIndex the tile index - flattened
	 * @param neighbors an array to put the indices into, or null.
	 * If neighbors is null, a new array should be allocated.
	 * @return the list of neighbor tiles, with indices of -1 where a side has no neighbor, or null if flatIndex is invalid.
	 */
	public int [] getNeighbors(int flatIndex, int [] neighbors);

	public int getTileCount();
	public int getTilesAcross();
	public int getTilesDown();

	/**
	 * Rotates a tile.
	 * @param flatIndex flattened index of the tile to rotate
	 * @param direction one of {@link #SPIN_CW}, {@link #SPIN_CCW}, {@link #SPIN_NONE} (may be multiplied for convenience, ie SPIN_CW*2)
	 */
	public void rotate(int flatIndex, int direction);
	
	public void swap(int flatIndex1, int flatIndex2);
	
	/**
	 * Returns the number of discrete steps a tile has been rotated.
	 * @param flatIndex tile index
	 * @return number of steps
	 */
	public int getRotationCount(int flatIndex);
	
	/**
	 * Returns the rotation of a tile.
	 * @param flatIndex tile index
	 * @return rotation in radians
	 */
	public double getRotation(int flatIndex);
	
	/**
	 * Returns the original index of a tile.
	 * @param flatIndex the index of the location to check.
	 * @return the index of the tile occupying the specified location.
	 */
	public int getOriginalTileIndex(int flatIndex);

	/**
	 * Returns the offset from the tile's position to its center, scaled to its ideal width:height ratio.
	 * This function is implementation specific, since it depends on tile shape.
	 */
	public Point2D.Double getScaledTileCenterOffset(int flatIndex, Point2D.Double offset);

	/**
	 * Returns the shape that borders the specified tile, unpositioned and unrotated.
	 * This function is implementation-specific, since it depends on tile shape
	 */
	public Shape getTileMask(int flatIndex);
	
	/**
	 * @deprecated There may never be a need for this function.
	 */
	public Path2D getTileBorder(int flatIndex, int side);

	/**
	 * Returns the amount of margin space needed to draw the tile.
	 * For example, a jigsaw tile has some overlapping parts that extend into its neighbors.
	 */
	public int getTileMargin();

	/**
	 * Returns the index of the tile at the graphical coordinates indicated.
	 */
	public int getTileAt(int x, int y);

	/**
	 * Returns the graphical coordinates for the upper left corner of the tile's current position.
	 */
	public Point getTilePosition(int flatIndex, Point position);
	
	/**
	 * Returns graphical coordinates for the upper left corner of the tile's original position.
	 * @param flatIndex tile index
	 * @param position storage for the return value, or null
	 * @return the original position of the specified tile
	 */
	public Point getOriginalTilePosition(int flatIndex, Point position);
	
	/**
	 * Returns the graphics transformation required to rotate a tile to its current orientation
	 * @param flatIndex tile index
	 * @param transform the graphics transformation to use as a starting point, if null a new one will be created.
	 * @param errMargin The number of pixels left around the edge of the tile's bounding rectangle
	 */
	public AffineTransform getRotationTransform(int flatIndex, AffineTransform transform, int errMargin);

	/**
	 * Returns tile width.
	 */
	public int getTileWidth();
	
	/**
	 * Returns tile height.
	 */
	public int getTileHeight();
	
	/**
	 * Returns the space required by a column of tiles.
	 */
	public int getTileSpacingX();

	/**
	 * Returns the space required by a row of tiles.
	 */
	public int getTileSpacingY();
	
	/**
	 * Returns the offset from the origin to the leftmost column.
	 */
	public int getLeftOffset();
	
	/**
	 * Returns the offset from the origin to the topmost row.
	 */
	public int getTopOffset();

	/**
	 * Returns the scale correction factor.
	 * Scaling on the y-axis by this value should set the tile to its 'ideal' width:height ratio, with all sides being of equal length.
	 */
	public double getHeightScaleFactor();

	/**
	 * Returns the index of the neighboring position in the indicated direction, taking into account tile rotation.
	 * @param flatIndex tile index.
	 * @param offset neighbor's two-dimensional offset.
	 * @return index of the neighbor, or -1 if there is no neighbor in the given position.
	 */
	public int getNeighborIndex(int flatIndex, Point offset);
	
	/**
	 * Returns the index of the neighboring position in the indicated direction, taking into account tile rotation.
	 * @param flatIndex tile index.
	 * @param direction direction to search. This should be in the range of 0 to {@link getRotationSteps()} - 1.
	 * @return index of the neighbor, or -1 if there is no neighbor in the given direction.
	 */
	public int getNeighborIndex(int flatIndex, int direction);
	
	/**
	 * Returns the number of sides per tile.
	 * @return the number of sides per tile.
	 */
	public int getSideCount();

	/**
	 * Returns the number of steps in a 360 degree rotation.
	 * @return number of steps in a full rotation.
	 */
	public int getRotationSteps();

	/**
	 * Returns the angle of a single rotation step.
	 * @return the angle of a single rotation step in radians.
	 */
	public double getRotationStep();

	/**
	 * Returns the left end of a tile's border.
	 * @param flatIndex tile index.
	 * @param direction direction of the border.
	 * @param position storage for the return value, if null a new Point will be allocated.
	 * @return position of the left end, or null if the tile has no border in the given direction.
	 */
	public Point getBorderLeft(int flatIndex, int direction, Point position);

	/**
	 * Returns the right end of a tile's border.
	 * @param flatIndex tile index.
	 * @param direction direction of the border.
	 * @param position storage for the return value, if null a new Point will be allocated.
	 * @return position of the left end, or null if the tile has no border in the given direction.
	 */
	public Point getBorderRight(int flatIndex, int direction, Point position);
}
