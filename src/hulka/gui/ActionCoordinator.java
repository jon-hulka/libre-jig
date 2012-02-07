/** 
 *   Copyright (C) 2010  Jonathan Hulka (jon.hulka@gmail.com)
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
package hulka.gui;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.ArrayList;
import javax.swing.JFrame;

/**
 * Handles predefined gui actions and acts as a dispatcher for additional
 * actions.<br>
 * 
 * This class is meant to work in conjunction with {@link GUILoader} and
 * an instance should be aquired through
 * {@link GUILoader#getActionCoordinator}. An ActionCoordinator obtained
 * this way will already receive menu events.<br>
 * 
 * ActionCommand strings using the category:command format allow
 * ActionListeners to be registered for specific categories.
 * (See {@link #addActionListener(String, ActionListener)})<br>
 * 
 * This class provides a mechanism for centralized dispatching of events,
 * and incurs a small amount of overhead in consequence.<br><br>
 *
 * The predefined actions are:<br>
 * application:exit<br>
 * application:minimize<br>
 * application:maximize<br>
 * application:restore<br>
 * application:hide<br>
 * application:show<br>
 * showdoc:docname<br>
 * docname specified in the gui definition file ie
 * <hr>
 * <blockquote><pre>
 * &lt;gui&gt;
 *     &lt;document&gt;
 *         &lt;name&gt;docname&lt;/name&gt;
 *         &lt;path&gt;docpath/doc.html&lt;/path&gt;
 *         &lt;/document&gt;
 * &lt;gui&gt;
 * </pre></blockquote>
 * <hr><br>
 * 
 * Changelog:
 * 2011 03 18 - Jon
 * - Added removeActionListener
 */
public class ActionCoordinator implements ActionListener
{
	private GUILoader guiLoader = null;
	private HashMap<String,ArrayList<ActionListener>> actionListeners;
	
	private ActionCoordinator(){}
	
	/**
	 * Creates a new ActionCoordinator.
	 * Instances of this class are created by {@link GUILoader} and should
	 * be retrieved using {@link GUILoader#getActionCoordinator()}
	 * @param g the associated GUILoader
	 */
	protected ActionCoordinator(GUILoader g)
	{
		actionListeners=new HashMap<String,ArrayList<ActionListener>>();
		guiLoader = g;
	}
	
	/**
	 * Registers an ActionListener to receive event notifications.
	 * The registered listener will only be notified of events matching
	 * the specified category. If category is null, the listener will be
	 * notified of all unrecognized (not predefined) events.<br>
	 * Predefined events will never be passed on to a listener.<br>
	 * Category-specific listeners will be notified before general ones
	 * regardless of the order registered.
	 * @param category event category (first half of the category:command pair for category-specific listeners) or null for general listeners.
	 * @param l the ActionListener to register
	 */
	public void addActionListener(String category, ActionListener l)
	{
		ArrayList<ActionListener> listeners=actionListeners.get(category);
		if(listeners==null)
		{
			listeners=new ArrayList<ActionListener>();
			actionListeners.put(category,listeners);
		}
		listeners.add(l);
	}
	
	/**
	 * De-registers an ActionListener from receiving event notifications.
	 * @param category event category (first half of the category:command pair for category-specific listeners) or null for general listeners.
	 * @param l the ActionListener to remove.
	 * @return true if the ActionListener was removed, false if not found.
	 */
	public boolean removeActionListener(String category, ActionListener l)
	{
		boolean result=false;
		ArrayList<ActionListener> listeners=actionListeners.get(category);
		if(listeners!=null)
		{
			result=listeners.remove(l);
		}
		return result;
	}
	
	/**
	 * Handles predefined action events and passes unrecognized events on to registered listeners.
	 */
	public void actionPerformed(ActionEvent e)
	{
		String cmd = e.getActionCommand();
		String category=null;
		boolean recognized=true;
		if(cmd!=null)
		{
			int pos=cmd.indexOf((int)':');
			if(pos>-1)
			{
				category=cmd.substring(0,pos);
				String command=cmd.substring(pos+1);
				if(category.equals("application"))
				{
					if(command.equals("exit")){guiLoader.exit(0);}
					else if(command.equals("maximize")){guiLoader.getFrame().setExtendedState(JFrame.MAXIMIZED_BOTH);}
					else if(command.equals("minimize")){guiLoader.getFrame().setExtendedState(JFrame.ICONIFIED);}
					else if(command.equals("restore")){guiLoader.getFrame().setExtendedState(JFrame.NORMAL);}
					else if(command.equals("hide")){guiLoader.getFrame().setVisible(false);}
					else if(command.equals("show")){guiLoader.getFrame().setVisible(true);}
					else recognized=false;
				}
				else if(category.equals("showdoc"))
				{
					guiLoader.showDocument(command);
				}else recognized=false;
			}else recognized=false;
		}else recognized=false;

		if(!recognized)
		{
			//category:command not recognized or not present
			//notify other ActionListeners
			ArrayList<ActionListener>listeners;
			if(category!=null)
			{
				//Notify the relevant category-specific listeners
				listeners=actionListeners.get(category);
				if(listeners!=null)
				{
					for(int i=0; i < listeners.size(); i++)
					{
						listeners.get(i).actionPerformed(e);
					}
				}
			}

			//Notify the general listeners
			listeners=actionListeners.get(null);
			if(listeners!=null)
			{
				for(int i=0; i < listeners.size(); i++)
				{
					listeners.get(i).actionPerformed(e);
				}
			}
		}
	}
}
