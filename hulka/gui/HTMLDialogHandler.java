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

package hulka.gui;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.JOptionPane;
import java.awt.Desktop;
import java.net.URI;

public class HTMLDialogHandler implements ActionListener, HyperlinkListener
{
	HTMLDialog dialog = null;
	HTMLDialogHandler(HTMLDialog dialog)
	{
		this.dialog = dialog;
	}
	
	public void actionPerformed(ActionEvent e)
	{
		String cmd = e.getActionCommand();
		if(cmd.equals("close")) dialog.setVisible(false);
	}
	
	public void hyperlinkUpdate(HyperlinkEvent e)
	{
		if(e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED))
		{
			URI uri = null;
			try
			{
				uri = e.getURL().toURI();
				Desktop.getDesktop().browse(uri);
			}
			catch(Exception ex)
			{
			JOptionPane.showMessageDialog(null,
				"Unable to load page" + uri==null ? "" : " at " + uri.toString(),
				"Browse error", JOptionPane.ERROR_MESSAGE);
			}
		}
	}
}
