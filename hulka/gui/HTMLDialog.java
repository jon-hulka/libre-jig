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
import javax.swing.*;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Graphics2D;
import java.net.URL;

public class HTMLDialog extends JDialog
{
	public HTMLDialog(JFrame owner, String title, URL page)
	{
		super(owner,title,true);
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		JPanel contentPane = (JPanel)getContentPane();

		GridBagLayout layout = new GridBagLayout();
		contentPane.setLayout(layout);
		GridBagConstraints constraints = new GridBagConstraints();

		
		constraints.gridx=0;
		constraints.gridy=0;
		constraints.weightx=1;
		constraints.weighty=1;
		constraints.gridwidth = 1;
		constraints.gridheight = 1;
		constraints.fill = GridBagConstraints.BOTH;
		constraints.insets = new Insets(5,5,5,5);
 
		JEditorPane htmlPane = new JTextPane();
		htmlPane.setEditable(false);
		JScrollPane htmlScrollPane = new JScrollPane();
  
		try
		{
			htmlPane.setPage(page);
		} 
		catch (Exception ex)
		{
			JOptionPane.showMessageDialog(null,
				"Unable to load page at " + page,
				"Page load error", JOptionPane.ERROR_MESSAGE);
			
		}
//		htmlPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		layout.setConstraints(htmlScrollPane,constraints);
		contentPane.add(htmlScrollPane);
		htmlScrollPane.setViewportView(htmlPane);

		JButton continueButton = new JButton("OK");
		continueButton.setToolTipText("ok");
		continueButton.setActionCommand("close");
		constraints.fill = GridBagConstraints.NONE;

		constraints.gridy++;
		constraints.weighty=0;
		constraints.weightx=0;

		layout.setConstraints(continueButton,constraints);
		contentPane.add(continueButton);
		HTMLDialogHandler h = new HTMLDialogHandler(this);
		htmlPane.addHyperlinkListener(h);
		continueButton.addActionListener(h);
		
	}
}
