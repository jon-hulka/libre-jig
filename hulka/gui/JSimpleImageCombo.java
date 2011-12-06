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

/**
 * Changelog:
 * 2010 05 05 - Jon
 * Added adjustment for scroll bar width in the popup menu's width.
 * 2010 06 09 - Jon
 * Changed the rendering component from JLabel to JButton (this class now extends JButton, rather than JLabel)
 * 2010 06 10 - Jon
 * Made a minor adjustment to the popup height.
 * 2010 06 11 - Jon
 * Changed valueChanged function to hide the popup menu as soon as the value is changed, in addition to on mouse click.
 * Previously, the popup would interfere with other dialogs that registered ActionListeners opened in response to ActionEvents.
 * Added clearSelection()
 */
package hulka.gui;
import java.util.ArrayList;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.awt.event.*;
import java.awt.Container;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.GridLayout;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;

/**
 * This class was created to circumvent a feature of the Aqua look and feel (Mac)
 * that limits the height of JComboBox.
 * It provides a very simple selectable list of images.
 */

public class JSimpleImageCombo extends JButton implements ListSelectionListener, MouseListener
{
	private ImageListCellRenderer renderer;
	private JList popupList;
	private JPopupMenu popupMenu;
	private ArrayList <ActionListener> listeners;
	String actionCommand = null;
	Dimension popupSize = null;
	JScrollPane scrollPane = null;
	public JSimpleImageCombo(BufferedImage [] listData, BufferedImage unselected)
	{
		super(new ImageIcon(unselected));
		renderer = new ImageListCellRenderer(unselected,listData);
		setIcon(renderer.unselectedIcon);
		popupList = new JList(listData);
		popupList.setCellRenderer(renderer);
		popupList.addListSelectionListener(this);
		addMouseListener(this);
		popupList.addMouseListener(this);
		popupMenu = new JPopupMenu();
		scrollPane = new JScrollPane(popupList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setBorder(null);
		popupMenu.add(scrollPane);
		listeners = new ArrayList<ActionListener>();
		popupSize = popupMenu.getPreferredSize();
	}
	
	public void setFocusable(boolean focusable)
	{
		super.setFocusable(focusable);
		scrollPane.setFocusable(focusable);
		popupMenu.setFocusable(focusable);
		popupList.setFocusable(focusable);
	}
	public void addActionListener(ActionListener listener)
	{
		listeners.add(listener);
	}
	
	public void removeActionListener(ActionListener listener)
	{
		listeners.remove(listener);
	}
	
	public void setActionCommand(String aCommand)
	{
		actionCommand = aCommand;
	}
	
	public void setSelectedIndex(int index)
	{
		popupList.setSelectedIndex(index);
	}
	
	public void clearSelection()
	{
		popupList.clearSelection();
	}
	
	/**
	 * This method is public as an implementation side effect. Do not call or override.
	 */
	public void valueChanged(ListSelectionEvent e)
	{
		//Only process the final event
		if(!e.getValueIsAdjusting())
		{
			popupMenu.setVisible(false);
			int index=popupList.getSelectedIndex();
			//redraw the label
			setIcon(index<0?renderer.unselectedIcon:renderer.listIcons[index]);
			//pass the event on
			ActionEvent ae = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, actionCommand);
			for(int i = listeners.size() - 1; i >= 0; i--)
			{
				listeners.get(i).actionPerformed(ae);
			}
		}
	}
	
	public int getSelectedIndex()
	{
		return popupList.getSelectedIndex();
	}

	/**
	 * This method is public as an implementation side effect. Do not call or override.
	 */
	public void mouseClicked(MouseEvent e)
	{
		if(isEnabled())
		{
			if(!popupMenu.isVisible() && e.getSource() == this)
			{
				//Determine offset within the frame's content pane
				Insets insets = null;
				int y = 0;
				Container c = this;
				do
				{
					y += c.getY();
					c = c.getParent();
					insets = c.getInsets();
					y += insets.top;
				}while(!(c instanceof JRootPane));
				
				c = ((JRootPane)c).getContentPane();
				//Determine available height
				int height = c.getHeight();
				
				popupSize.width = getWidth();

				//Determine preferred height
				insets = popupMenu.getInsets();
				popupSize.height = popupList.getPreferredSize().height + insets.top + insets.bottom;
				
				//Set height to fit
				if(popupSize.height > height)
				{
					popupSize.height = height;
					popupSize.width += scrollPane.getVerticalScrollBar().getPreferredSize().width;
				}
				int yOffset = 0;
				//Determine if the popup window should be moved to fit inside the application window
				if((y + popupSize.height) > c.getHeight())
				{
					yOffset = c.getHeight() - y - popupSize.height + c.getY();
				}
				popupMenu.setPopupSize(popupSize);
				popupMenu.show(this,0,yOffset);
			}
			else
			{
				popupMenu.setVisible(false);
			}
		}
	}

	/**
	 * This method is public as an implementation side effect. Do not call or override.
	 */
	public void mousePressed(MouseEvent e)
	{
	}

	/**
	 * This method is public as an implementation side effect. Do not call or override.
	 */
	public void mouseReleased(MouseEvent e)
	{
	}
	
	/**
	 * This method is public as an implementation side effect. Do not call or override.
	 */
	public void mouseEntered(MouseEvent e)
	{
	}

	/**
	 * This method is public as an implementation side effect. Do not call or override.
	 */
	public void mouseExited(MouseEvent e)
	{
	}
}
