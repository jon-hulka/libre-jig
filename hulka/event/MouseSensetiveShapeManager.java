/**
 *      MouseSensetiveShapeManager.java
 *
 *      Copyright 2005, 2009 Jonathan Hulka <jon.hulka@gmail.com>
 *      
 *      This is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      (at your option) any later version.
 *      
 *      This software is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU General Public License for more details.
 *      
 *      You should have received a copy of the GNU General Public License
 *      along with the software.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * 2010 06 29 - Jon - fixed getIntersectingShapes - z-order sorting was broken by previous changes
 * 2010-06-07 - Jon - fixed a bug that was causing an index out of bounds exception when no shapes are registered
 * 2010-01-29 - Jon - enhanced and debugged getIntersectingShapes
 * 2010-01-28 - Jon - added getIntersectingShapes to allow performance optimizations on redraw operations
 */

package hulka.event;
import java.awt.Shape;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;


/** 
 * @author Jonathan Hulka (jon.hulka@gmail.com)
 * 
 * 
 * The purpose of this class is to keep track of a number of mouse
 * sensetive shapes and pass events on behalf of each <br>
 * 
 * This class sits between a Component and its MouseSensetiveShapeListener
 * objects, passing MouseSensetiveShapeEvent notifications to its listeners. <br>
 * 
 * It must be registered with a Component through the Component.addMouseListener
 * function, and its listeners must be registered through the
 * addMouseSensetiveShapeListener function. <br>
 * 
 * In order for the MouseSensetiveShapeManager to be of any use, one or
 * more MouseSensetiveShape objects must also be registered through the
 * addShape function. <br>
 * 
 * When a mouse event occurs, the MouseSensetiveShapeManager determines
 * which MouseSensetiveShape objects' bounding rectangles contain the
 * coordinate, then checks MouseSensetiveShape.containsPoint for each to
 * determine if the coordinate falls within the shape.  It then dispatches
 * MouseSensetiveShapeEvent notifications to its listeners on behalf of
 * any shape that qualifies.
 */

public class MouseSensetiveShapeManager implements MouseListener
{
    private int [][] shapesByColumn;
    private int [][] shapesByRow;
    private int [] columnDividers;
    private int [] rowDividers;
    private MouseSensetiveShape [] shapes;
    // used by insert and delete for temporary storage
    private int [][] newIndexedShapes;
    // used by insert and delete for temporary storage
    private int [] newDividerList;
    private ArrayList <MouseSensetiveShapeListener> listeners;
    
    public MouseSensetiveShapeManager()
    {
        shapes = null;
        columnDividers = null;
        rowDividers = null;
        shapesByColumn = null;
        shapesByRow = null;
        listeners = new ArrayList<MouseSensetiveShapeListener>();
    }
    private MouseSensetiveShapeEvent buildMouseSensetiveShapeEvent(MouseEvent e)
    {
        MouseSensetiveShape [] results = null;
        MouseSensetiveShapeEvent ev = null;
        int [] affectedShapes = getAffectedShapeBounds(e.getX(), e.getY());
        if(affectedShapes != null)
        {
            int shift = 0;
            int [] hits = new int[affectedShapes.length];
            for(int i = 0; i < affectedShapes.length && !e.isConsumed(); i++)
            {
                MouseSensetiveShape shape = shapes[affectedShapes[i]];
                if(shape.containsPoint(e.getX(), e.getY()))
                {
                    hits[i + shift] = affectedShapes[i];
                }
                else
                {
                    shift --;
                }
            }
            int hitCount = affectedShapes.length + shift;
            if(hitCount > 0)
            {
                results = new MouseSensetiveShape[hitCount];
                for(int i = 0; i < hitCount; i++)
                {
                    results[i] = shapes[hits[i]];
                }
            }
        }
        if(results != null) ev = new MouseSensetiveShapeEvent(results, e);
        return ev;
    }
    public synchronized void mouseClicked(MouseEvent e)
    {
        MouseSensetiveShapeEvent ev = buildMouseSensetiveShapeEvent(e);
        for(int i = 0; ev != null && i < listeners.size() && !ev.isConsumed(); i++)
        {
            listeners.get(i).mouseClicked(ev);
        }        
    }
    public synchronized void mouseEntered(MouseEvent e){}
    public synchronized void mouseExited(MouseEvent e){}
    public synchronized void mousePressed(MouseEvent e)
    {
        MouseSensetiveShapeEvent ev = buildMouseSensetiveShapeEvent(e);
        for(int i = 0; ev != null && i < listeners.size() && !ev.isConsumed(); i++)
        {
            listeners.get(i).mousePressed(ev);
        }        
    }
    public synchronized void mouseReleased(MouseEvent e)
    {
        MouseSensetiveShapeEvent ev = buildMouseSensetiveShapeEvent(e);
        for(int i = 0; ev != null && i < listeners.size() && !ev.isConsumed(); i++)
        {
            listeners.get(i).mouseReleased(ev);
        }        
    }
    
    /**
     * Returns MouseSensetiveShapes intersecting the specified rectangle
     * The returned values will be sorted by zOrder
     * @param bounds the rectangle to check
     * @return array of shapes intersecting the rectangle, or null if there are none
     */
    public synchronized MouseSensetiveShape [] getIntersectingShapes(Rectangle bounds)
    {
		MouseSensetiveShape [] result = null;
		if(shapes != null)
		{
			ArrayList<MouseSensetiveShape> crossMatches = getIntersectingShapes(bounds.x,bounds.y,bounds.x + bounds.width - 1,bounds.y + bounds.height - 1);

			//Check if the shapes themselves overlap, only keep the ones that actually touch
			for(int index = 0; index < crossMatches.size(); )
			{
				MouseSensetiveShape shape = crossMatches.get(index);
				Area test = new Area(shape.getShape());
				//Get the intersection of the two areas
				if(test.intersects(bounds))
				{
					//Yes, keep it
					index++;
				}
				else
				{
					//No, discard it
					crossMatches.remove(index);
				}
			}

//			if(crossMatches.size()>0)
//			{
				result = crossMatches.toArray(new MouseSensetiveShape[crossMatches.size()]);
				Arrays.sort(result);
//			}
		}
		return result;
	}
	
    /**
     * Returns MouseSensetiveShapes intersecting the specified target
     * The returned values will be sorted by zOrder
     * @param target the shape to check
     * @return array of shapes intersecting target, or null if there are none
     */
    public synchronized MouseSensetiveShape [] getIntersectingShapes(MouseSensetiveShape target)
    {
		MouseSensetiveShape [] result = null;
		if(shapes != null)
		{
			int x1 = target.getX();
			int x2 = x1 + target.getWidth();
			int y1 = target.getY();
			int y2 = y1 + target.getHeight();
			
			ArrayList<MouseSensetiveShape> crossMatches = getIntersectingShapes(x1,y1,x2,y2);

			Area aTarget = new Area(target.getShape());
			Rectangle2D rTarget = aTarget.getBounds2D();

			//Check if the shapes themselves overlap, only keep the ones that actually touch
			for(int index = 0; index < crossMatches.size(); )
			{
				//There is no way to test two areas directly for intersection, so do it this way:
				MouseSensetiveShape shape = crossMatches.get(index);
				Area test = new Area(shape.getShape());
				//Get the intersection of the two areas
				test.intersect(aTarget);
				//Then see if it intersects the bounding rectangle of one
				if(test.intersects(rTarget))
				{
					//Yes, keep it
					index++;
				}
				else
				{
					//No, discard it
					crossMatches.remove(index);
				}
			}
			
			if(crossMatches.size()>0)
			{
				result = crossMatches.toArray(new MouseSensetiveShape[crossMatches.size()]);
				Arrays.sort(result);
			}
		}
		return result;
	}

	//This function does most of the work for the other getIntersectingShapes functions
	private ArrayList<MouseSensetiveShape> getIntersectingShapes(int x1, int y1, int x2, int y2)
	{
		
        int row1 = findLast(y1, rowDividers);
        if(row1 >= rowDividers.length || rowDividers[row1] > y1 && row1 > 0) row1 --;
        int row2 = findLast(y2, rowDividers);
        if(row2 >= rowDividers.length || rowDividers[row2] > y2 && row2 > 0) row2 --;

        int column1 = findLast(x1,columnDividers);
        if(column1 >= columnDividers.length || columnDividers[column1] > x1 && column1 > 0) column1 --;
        int column2 = findLast(x2,columnDividers);
        if(column2 >= columnDividers.length || columnDividers[column2] > x2 && column2 > 0) column2 --;

        //Build a list of shapes that share columns with the target
        ArrayList<MouseSensetiveShape> columnMatches = new ArrayList<MouseSensetiveShape>();
        for(int i = column1; i <= column2; i++)
        {
			if(shapesByColumn[i]!=null) for(int j =0; j < shapesByColumn[i].length; j++)
			{
				if(columnMatches.indexOf(shapes[shapesByColumn[i][j]]) < 0) columnMatches.add(shapes[shapesByColumn[i][j]]);
			}
		}

		//Keep only those that also share rows, the bounding boxes of these will intersect with the target's bounding box
		ArrayList<MouseSensetiveShape> crossMatches = new ArrayList<MouseSensetiveShape>();
        for(int i = row1; i <= row2; i++)
        {
			if(shapesByRow[i] != null) for(int j = 0; j < shapesByRow[i].length; j++)
			{
				if(columnMatches.remove(shapes[shapesByRow[i][j]])) crossMatches.add(shapes[shapesByRow[i][j]]);			
			}
		}
		
		return crossMatches;
	}
	    
    /**
    * Remove a MouseSensetiveShape from the events list
    * @param shape the MouseSensetiveShape to remove
    */
    public synchronized void removeShape(MouseSensetiveShape shape)
    {
        //Find the shape based on its location
        int columnDivider = shape.x;
        int columnIndex = findFirst(columnDivider, columnDividers);
        int shapeIndex = -1;

        if(columnIndex < columnDividers.length && columnDividers[columnIndex] == columnDivider)
        {
            for(int i = 0; i < shapesByColumn[columnIndex].length && shapeIndex == -1; i++)
            {
                if(shape == shapes[shapesByColumn[columnIndex][i]]) shapeIndex = shapesByColumn[columnIndex][i];
            }
        }

        if(shapeIndex > -1)
        {
            MouseSensetiveShape newShapes[] = shapes.length==1 ? null : new MouseSensetiveShape[shapes.length -1];
            //Remove the deleted shape from the shapes list
            for(int i = 0; i < shapeIndex; i++)
            {
                newShapes[i] = shapes[i];
            }
            for(int i = shapeIndex; newShapes != null && i < newShapes.length; i++)
            {
                newShapes[i] = shapes[i + 1];
            }
//To do: change shapes to grow only, it shouldn't resize when shapes are being removed
            shapes = newShapes;
            delete(shapesByRow, rowDividers, shapeIndex);
            shapesByRow = newIndexedShapes;
            rowDividers = newDividerList;
            delete(shapesByColumn, columnDividers, shapeIndex);
            shapesByColumn = newIndexedShapes;
            columnDividers = newDividerList;
            
        }
    }

    /**
    * helper function for RemoveShape
    */
    private void delete(int [][] indexedShapes, int [] dividerList, int shapeIndex)
    {
        newIndexedShapes = new int[indexedShapes.length - 2][];
        newDividerList = new int[newIndexedShapes.length];
        int iShift = 0;
        for(int i = 0; i < indexedShapes.length; i++)
        {
            int jShift = 0;
            for(int j = 0; indexedShapes[i] != null && j < indexedShapes[i].length; j++)
            {
                indexedShapes[i][j + jShift] = indexedShapes[i][j];
                if(indexedShapes[i][j] == shapeIndex)
                {
                    //skip over this shape and shift the others down
                    jShift --;
                }
                else if(indexedShapes[i][j] > shapeIndex)
                {
                    //adjust the index
                    indexedShapes[i][j + jShift] --;
                }
            }
            
            if(jShift < 0)
            {
                if(iShift == 0)
                {
                   //this is the first time the deleted shape is encountered
                   //Remove the column/row and shift the others down
                   iShift --;
                }
                else if(i < indexedShapes.length - 1)
                //check for the special case where the column/row to be deleted is the last one
                {
                    int newLength = indexedShapes[i].length + jShift;
                    int [] newList = null;
                    if(newLength > 0)
                    {
                        newList = new int[newLength];
                        for(int j = 0; j < newLength; j++)
                        {
                            newList[j] = indexedShapes[i][j];
                        }
                    }
                    newIndexedShapes[i + iShift] = newList;
                    newDividerList[i + iShift] = dividerList[i];
                }
            }
            else
            {
                if(iShift == -1)
                {
                    //this should be just after the last time the deleted shape is encountered
                    //remove the column/row and shift the others down
                    iShift --;
                }
                else
                {
                    newIndexedShapes[i + iShift] = indexedShapes[i];
                    newDividerList[i + iShift] = dividerList[i];
                }
            }
        }    
    }    

    /**
    * Add a MouseSensetiveShape to listen for mouse events on
    * @param shape the MouseSensetiveShape to add
    */
    public synchronized void addShape(MouseSensetiveShape shape)
    {
        int left = shape.x;
        int right = shape.x + shape.width + 1;
        int top = shape.y;
        int bottom = shape.y + shape.height + 1;
        if(shapes == null)
        {
            //Special case, the lists are empty
            shapes = new MouseSensetiveShape[1];
            shapes[0] = shape;
            columnDividers = new int[2];
            columnDividers[0] = left;
            columnDividers[1] = right;
            rowDividers = new int[2];
            rowDividers[0] = top;
            rowDividers[1] = bottom;
            shapesByColumn = new int[2][];
            shapesByColumn[0] = new int[1];
            shapesByColumn[0][0] = 0;
            shapesByColumn[1] = null;
            shapesByRow = new int[2][];
            shapesByRow[0] = new int[1];
            shapesByRow[0][0] = 0;
            shapesByRow[1] = null;
            
        }
        else
        {
            //add new shape to list
            int index = shapes.length;
            MouseSensetiveShape newShapes[] = new MouseSensetiveShape[index + 1];
            for(int i = 0; i < index; i++)
            {
                newShapes[i] = shapes[i];
            }
            newShapes[index] = shape;
            shapes = newShapes;
            //find column position
            int leftIndex = findFirst(left, columnDividers);
            //find next column position
            int rightIndex = findFirst(right, columnDividers);
            //insert new columns
            insert(shapesByColumn, columnDividers,left, right, leftIndex, rightIndex, index);
            shapesByColumn = newIndexedShapes;
            columnDividers = newDividerList;
            //find row position
            int topIndex = findFirst(top, rowDividers);
            //find next row position
            int bottomIndex = findFirst(bottom, rowDividers);
            //insert new rows
            insert(shapesByRow, rowDividers,top, bottom, topIndex, bottomIndex, index);
            shapesByRow = newIndexedShapes;
            rowDividers = newDividerList;
        }
    }
    
    /**
    * helper function to insert a new row or column
    */
    private void insert(int [][] indexedShapes, int [] dividerList, int thisPosition, int nextPosition, int thisIndex, int nextIndex, int shapeIndex)
    {
        int dividerCount = dividerList.length;
        int indexOffset = 0;
        
        newIndexedShapes = new int[dividerList.length + 2][];
        newDividerList = new int[dividerList.length + 2];

        //Copy rows or columns that come before the affected shape
        for(int i = 0; i < thisIndex; i++)
        {
            newDividerList[i + indexOffset] = dividerList[i];
            newIndexedShapes[i + indexOffset] = indexedShapes[i];
        }
       
         //Insert the new divider for the start position
        newDividerList[thisIndex + indexOffset] = thisPosition;
        
        //Determine which row/column this divider splits
        int sharedIndex = thisIndex - 1;
        if(thisIndex < dividerList.length && dividerList[thisIndex] == thisPosition)
        {
            sharedIndex ++;
        }
        
        if(sharedIndex >= 0)
        {
            newIndexedShapes[thisIndex + indexOffset] = insertShapeIndex(shapeIndex, indexedShapes[sharedIndex]);
        }
        else
        {
            //There is no column before this one
            newIndexedShapes[thisIndex + indexOffset] = new int[1];
            newIndexedShapes[thisIndex + indexOffset][0] = shapeIndex;
        }
        indexOffset++;

       
        //insert new shape into affected range of rows/columns
        for(int i = thisIndex; i < nextIndex; i++)
        {
            newDividerList[i + indexOffset] = dividerList[i];
            
            newIndexedShapes[i + indexOffset] = insertShapeIndex(shapeIndex, indexedShapes[i]);
        }

        //Insert the new divider for the end position
        newDividerList[nextIndex + indexOffset] = nextPosition;
        //It will contain everything that was previously in the row/column that has been split...
        if(nextIndex < dividerList.length && dividerList[nextIndex] == nextPosition)
        {
            //Unless the row/column was not split because the divider fell on a boundary...
            //In which case, copy from the next row/column
            newIndexedShapes[nextIndex + indexOffset] = copy(indexedShapes[nextIndex]);
        }
        else if(nextIndex > 0)
        {
            newIndexedShapes[nextIndex + indexOffset] = copy(indexedShapes[nextIndex - 1]);
        }
        else
        {
            //Or there was no row/column to split because the new row/column comes before any others
            newIndexedShapes[nextIndex + indexOffset] = null;
        }
        indexOffset++;
        
        //No changes to make to rows or columns after the affected range
        for(int i = nextIndex; i < dividerList.length; i++)
        {
            newDividerList[i + indexOffset] = dividerList[i];
            newIndexedShapes[i + indexOffset] = indexedShapes[i];
        }
       
    }

    private int[] copy(int[] oldArray)
    {
        int [] newArray;
        if(oldArray == null)
        {
            newArray = null;
        }
        else
        {
            newArray = new int[oldArray.length];
            for(int i = 0; i < oldArray.length; i++)
            {
                newArray[i] = oldArray[i];
            }
        }
        return newArray;
    }
    
    private int[] insertShapeIndex(int index, int[] oldArray)
    {
        int [] newArray;
        if(oldArray == null)
        {
            newArray = new int[1];
            newArray[0] = index;
        }
        else
        {
            newArray = new int[oldArray.length + 1];
            int indexOffset = 0;
            int zOrder = shapes[index].getZOrder();

            for(int i = 0; i < oldArray.length; i++)
            {
                if(indexOffset == 0 && shapes[oldArray[i]].getZOrder() >= zOrder)
                {
                    newArray[i] = index;
                    indexOffset = 1;
                }
                newArray[i + indexOffset] = oldArray[i];
            }
            if(indexOffset == 0)
            {
                newArray[oldArray.length] = index;
            }
        }
        return newArray;
    }
    
    /**
    * given an array of integers sorted in ascending order, finds an element equal to the search value, or the first element greater than the search value.  The array elements need not be unique.
    * @param value the value to search for
    * @param list an array of integers sorted in ascending order
    * @return an element equal to the search value if it exists, otherwise the first element greater than the search value, or -1 if the list is empty
    *
    */
    private int find(int value, int [] list)
    {
		int index=-1;
		if(list.length > 0)
		{
			int top = list.length - 1;
			int bottom = 0;
			index = top/2;
			while(top != bottom)
			{
				if(list[index] < value)
				{
					bottom = index + 1;
					if(bottom > top) bottom = top;
				}
				else if(list[index] > value)
				{
					top = index - 1;
					if(top < bottom) top = bottom;
				}
				else
				{
					top = index;
					bottom = index;
				}
				index = (top + bottom)/2;
			}
			if(list[index] < value) index++;
		}
        return index;
    }
    
    /**
    * given an array of integers sorted in ascending order, finds the last element equal to the search value, or the first element greater than the search value.  The array elements need not be unique.
    * @param value the value to search for
    * @param list an array of integers sorted in ascending order
    * @return the last element equal to the search value if it exists, otherwise the first element greater than the search value, or -1 if the list is empty
    */
    private int findLast(int value, int [] list)
    {
        int index;
        for(index = find(value, list); index>=0 && index < list.length - 1 && list[index + 1] <= value; index ++);
        return index;
    }
    
    /**
    * given an array of integers sorted in ascending order, finds the first element greater than or equal to the search value.  The array elements need not be unique.
    * @param value the value to search for
    * @param list an array of integers sorted in ascending order
    * @return the first element greater than or equal to the search value
    */
    private int findFirst(int value, int [] list)
    {
        int index;
        for(index = find(value, list); index > 0 && list[index - 1] >= value; index --);
        return index;
    }

    private int [] getAffectedShapeBounds(int x, int y)
    {
        int [] affectedShapes = null;
        int column = findLast(x,columnDividers);
        if(column >= 0 && (column >= columnDividers.length || columnDividers[column] > x)) column --;
        int row = findLast(y, rowDividers);
        if(row >= 0 && (row >= rowDividers.length || rowDividers[row] > y)) row --;

        if(column >= 0 && row >= 0 && column < columnDividers.length && row < rowDividers.length && shapesByColumn[column] != null && shapesByRow[row] != null)
        {
            int [] list = new int[Math.min(shapesByColumn[column].length, shapesByRow[row].length)];
            int index = 0;
            for(int i = 0; i < shapesByRow[row].length; i ++)
            {
                for(int j = 0; j < shapesByColumn[column].length; j++)
                {
                    if(shapesByColumn[column][j] == shapesByRow[row][i])
                    {
                        list[index] = shapesByColumn[column][j];
                        index++;
                    }
                }
            }
            if(index == 0) return null;

            affectedShapes = new int[index];
            for(int i = 0; i < index; i++)
            {
                affectedShapes[i] = list[i];
            }
        }
        return affectedShapes;
    }
    
    public String toString()
    {
        String returnValue;
        if(shapesByColumn == null)
        {
            returnValue = "no shapes";
        }
        else
        {
            returnValue = "shapesByColumn:";
            for(int i = 0; i < shapesByColumn.length; i++)
            {
                returnValue += "\n";
                if(shapesByColumn[i] == null)
                {
                    returnValue += "<null>";
                }
                else
                {
                    for(int j = 0; j < shapesByColumn[i].length; j++)
                    {
                        returnValue += shapesByColumn[i][j] + " ";
                    }
                }
            }
            returnValue += "\nshapesByRow:";
            for(int i = 0; i < shapesByRow.length; i++)
            {
                returnValue += "\n";
                if(shapesByRow[i] == null)
                {
                    returnValue += "<null>";
                }
                else
                {
                    for(int j = 0; j < shapesByRow[i].length; j++)
                    {
                        returnValue += shapesByRow[i][j] + " ";
                    }
                }
            }
            returnValue += "\ncolumnDividers:\n";
            for(int i = 0; i < columnDividers.length; i++)
            {
                returnValue += columnDividers[i] + " ";
            }
            returnValue += "\nrowDividers:\n";
            for(int i = 0; i < rowDividers.length; i++)
            {
                returnValue += rowDividers[i] + " ";
            }
            returnValue += "\nshapes:";
            for(int i = 0; i < shapes.length; i++)
            {
                returnValue += "\n" + i + " x: " + shapes[i].x + " y: " + shapes[i].y + " width: " + shapes[i].width + " x: " + shapes[i].height;
            }
        }
        return returnValue;
    }
    
    public synchronized void addEventListener(MouseSensetiveShapeListener listener)
    {
        listeners.add(listener);
    }
    
    public synchronized void removeEventListener(MouseSensetiveShapeListener listener)
    {
        listeners.remove(listener);
    }
}
