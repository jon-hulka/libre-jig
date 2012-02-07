/*
 *      MouseSensetiveShapeEvent.java
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
 * 
 * @author Jonathan Hulka (jon.hulka@gmail.com)
 * 
 * 
 */

package hulka.event;

import java.util.EventObject;
import java.awt.event.MouseEvent;


public class MouseSensetiveShapeEvent extends EventObject
{
    private MouseEvent mouseEvent;
    private MouseSensetiveShapeEvent(MouseSensetiveShape [] source)
    {
        super(source);
    }
    public MouseSensetiveShapeEvent(MouseSensetiveShape [] source, MouseEvent mouseEvent)
    {
        super(source);
        this.mouseEvent = mouseEvent;
    }
    
    /**
    * Provides a list of MouseSensetiveShape objects originating this event.  In case of stacked objects, they will be listed in ascending z order.
    */
    public MouseSensetiveShape [] getMouseSensetiveShapes()
    {
        return (MouseSensetiveShape [])source;
    }
    
    /**
    * Provides access to the originating MouseEvent
    */
    public MouseEvent getMouseEvent()
    {
        return mouseEvent;
    }
    
    /**
    * Returns whether or not this event has been consumed.
    */
    public boolean isConsumed()
    {
        return mouseEvent.isConsumed();
    }
    
    /**
    * Consumes the event so that the MouseSensetiveShapeManager will not continue processing it.
    */
    public void consume()
    {
        mouseEvent.consume();
    }

}
