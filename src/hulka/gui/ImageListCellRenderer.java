/**
 * 
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
 * Change log:
 * 2010 06 09 - Jon
 * Changed the rendering component from JLabel to JButton
 */

package hulka.gui;
import javax.swing.*;
import java.awt.*;
import java.awt.image.*;

/**
 * Renders list cells as JButtons with images.
 */

public class ImageListCellRenderer extends DefaultListCellRenderer
{
	protected ImageIcon unselectedIcon = null;
	protected ImageIcon [] listIcons = null;
	protected JButton rendererComponent = new JButton();
	Dimension preferredSize=new Dimension(0,0);

	public ImageListCellRenderer(BufferedImage defaultImg, BufferedImage [] listData)
	{
		unselectedIcon = new ImageIcon(defaultImg);
		listIcons = new ImageIcon[listData.length];
		for(int i = 0; i < listData.length; i++)
		{
			listIcons[i] = new ImageIcon(listData[i]);
		}
	}
	
	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
	{
		ImageIcon icon = (index >= 0 && index < listIcons.length) ? listIcons[index] : unselectedIcon;
		rendererComponent.setIcon(icon);
		return rendererComponent;
	}
}
