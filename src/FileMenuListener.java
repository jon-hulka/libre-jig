/**
 *      GameMenuListener.java
 *      
 *      Copyright 2010 Jonathan Hulka <jon.hulka@gmail.com>
 *      
 *      This program is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      (at your option) any later version.
 *      
 *      This program is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU General Public License for more details.
 *      
 *      You should have received a copy of the GNU General Public License
 *      along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Hooks the game menu options (new, load, save and quit)
 */
public class FileMenuListener extends WindowAdapter implements ActionListener
{
	public static String ACTION_NEW="file:new";
	public static String ACTION_LOAD="file:load";
	public static String ACTION_SAVE="file:save";
	public static String ACTION_QUIT="file:quit";
	
	private GUI gui;
	
	FileMenuListener(GUI gui)
	{
		this.gui=gui;
	}
	
	public void windowClosing(WindowEvent e)
	{
		doCommand(ACTION_QUIT);
	}

	public void actionPerformed(ActionEvent e)
	{
		doCommand(e.getActionCommand());
	}
	
	public void doCommand(String cmd)
	{
		if(cmd.equals(ACTION_NEW)){gui.startGame();}
		else if(cmd.equals(ACTION_LOAD)){gui.loadGame();}
		else if(cmd.equals(ACTION_SAVE)){gui.saveGame();}
		else if(cmd.equals(ACTION_QUIT)){gui.quit();}
	}
}
