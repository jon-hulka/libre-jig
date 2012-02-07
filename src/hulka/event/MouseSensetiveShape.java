/**
 *      MouseSensetiveShape.java
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
 *      
 * Note: this class has a natural ordering that is inconsistent with equals.
 * 
 * @author Jonathan Hulka (jon.hulka@gmail.com)
 * 
 * 2010-01-28 - Jon - added getShape to facilitate MouseSensetiveShapeManager.getIntersectingShapes
 */

package hulka.event;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public abstract class MouseSensetiveShape implements Comparable<MouseSensetiveShape>
{
    protected int x;
    protected int y;
    protected int width;
    protected int height;
    protected int index;
    protected int zOrder;
    /**
    * Any implementation of this class should use this constructor or call it.
    */
    public MouseSensetiveShape(int x, int y, int width, int height, int index, int zOrder)
    {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.index = index;
        this.zOrder = zOrder;
    }
    public int getX(){ return x;}
    public int getY(){ return y;}
    public int getWidth(){ return width;}
    public int getHeight(){ return height;}
    public int getIndex(){return index;}
    public int getZOrder(){return zOrder;}
    public String toString(){return "x: " + x + " y: " + y + " width"  + width + " height: " + height + " index: " + index + " zOrder: " + zOrder;}
    /**
    * Implement this function to define which points fall within the shape.
    */
    public abstract boolean containsPoint(int x, int y);
    
    /**
     * This should return the bounding shape
     */
    public abstract Shape getShape();
    
    /**
     * Implementation of natural ordering for Comparable
     */
    public int compareTo(MouseSensetiveShape shape)
    {
		return zOrder - shape.getZOrder();
	}
}
