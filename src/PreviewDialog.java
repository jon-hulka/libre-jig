/**
 *      PreviewDialog.java
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

//import java.awt.event.KeyEvent;
//import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;

import javax.swing.JDialog;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import java.awt.image.BufferedImage;

public class PreviewDialog extends JDialog implements MouseListener
{
	BufferedImage image=null;
	ImageIcon icon=new ImageIcon();

	PreviewDialog()
	{
		super();
		setModal(true);
		addMouseListener(this);
		setContentPane(new JLabel(icon));
		setUndecorated(true);
	}
	
	public void setImage(BufferedImage img)
	{
		image=img;
		icon.setImage(image);
		pack();
		setLocationRelativeTo(null);
	}
	
	public void clearImage()
	{
		image=null;
		icon.setImage(null);
	}
	
	public void mouseClicked(MouseEvent e){setVisible(false);}
	public void mousePressed(MouseEvent e){}
	public void mouseReleased(MouseEvent e){}
	public void mouseEntered(MouseEvent e){}
	public void mouseExited(MouseEvent e){}
}
