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

package hulka.tilemanager;
import java.awt.Point;

/**
 * Adds to TileManager the concept of vertices around which groups of tiles can be spun.
 * Vertices are points at the corners of where a number of tiles intersect.
 * Spinning a vertex rotates the surrounding tiles, in addition to 'orbiting' them through the positions around the vertex.
 */
public interface TileSpinnerManager extends TileManager
{
	public int getFlatVertexIndex(Point expandedIndex);
	public Point getExpandedVertexIndex(int flatIndex, Point expandedIndex);
	/**
	 * Returns the coordinates of the nearest vertex given pixel coordinates x and y,
	 * or null if the nearest vertex is invalid
	 * @param x graphical x coordinate
	 * @param y graphical y coordinate
	 * @return the nearest vertex to the specified position, or -1 if the vertex is invalid
	 */
	public int getNearestVertex(int x, int y);
	/**
	 * Returns the graphical position of a vertex
	 * @param flatIndex vertex index
	 * @param position storage for the return value. If null, storage will be allocated.
	 * @return the graphical position of the vertex
	 */
	public Point getVertexPosition(int flatIndex, Point position);
	/**
	 * Rotates the vertex's surrounding tiles in the direction indicated.
	 * @param flatIndex index of the vertex to rotate
	 * @param direction one of TileManager.SPIN_CW or TileManager.SPIN_CCW
	 */
	public void spin(int flatIndex, int direction);

	/**
	 * Returns indices of tiles modified in the previous operation.
	 * @param indices storage for the return value. If null, storage will be allocated.
	 * If the number of indices used in an operation is uncertain, it is best pass null on the first call to this function, then use the returned value for subsequent calls.
	 * @return an array of tile indices
	 */
	public int [] getChangedTiles(int [] indices);
	
	/**
	 * Returns the vertex count.
	 * @return the vertex count.
	 */
	public int getVertexCount();
}
