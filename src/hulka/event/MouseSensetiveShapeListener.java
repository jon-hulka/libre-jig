/**
 *      MouseSensetiveShapeListener.java
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
 * 
 * This class provides a set of standard functions to intercept MouseSensetiveShapeEvent notifications.  mouseEntered and mouseExited are omitted for performance reasons.
 * 
 * @author Jonathan Hulka (jon.hulka@gmail.com)
 * 
 */

package hulka.event;
 
public interface MouseSensetiveShapeListener
{
    public void mouseClicked(MouseSensetiveShapeEvent e);
    public void mousePressed(MouseSensetiveShapeEvent e);
    public void mouseReleased(MouseSensetiveShapeEvent e);
}
