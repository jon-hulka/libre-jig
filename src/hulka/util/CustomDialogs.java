package hulka.util;

/*
 * CustomDialogs.java
 *       Copyright 2005, 2009 Jonathan Hulka <jon.hulka@gmail.com>
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
 * Created on Mar 12, 2005
 */

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.awt.geom.*;

public class CustomDialogs {
	
	public static final int YES = 1;
	public static final int NO = 0;
	private static int response;
	
	/**
	 * This method will generate a confirmation dialog for closing windows
	 *
	 * @param parent The frame to return control to
	 * @param title The title of the dialog popup screen
	 * @param message The message to appear in the dialog's body
	 * @return An integer value indicating which button was pressed
	 */
	public static int createConfirmDialog(JFrame parent, String title, String message)
	{
		response = -1;
		JPanel buttonPnl = new JPanel();
		JButton yesButton = new JButton("Yes");
		JButton noButton = new JButton("No");
				
		final JDialog dialog = new JDialog(parent, title, true);
	
		JLabel label = new JLabel(message);
					
		label.setHorizontalAlignment(JLabel.CENTER);

		yesButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				response = YES;
				dialog.setVisible(false);
				dialog.dispose();
			}
		});
		
		noButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				response = NO;
				dialog.setVisible(false);
				dialog.dispose();
			}
		});

		dialog.getContentPane().setLayout(new BorderLayout());

		buttonPnl.add(yesButton);
		buttonPnl.add(noButton);
				
		dialog.getContentPane().add(label,BorderLayout.CENTER);
		dialog.getContentPane().add(buttonPnl,BorderLayout.PAGE_END);
		
		dialog.setSize(new Dimension(275,125));
		dialog.setLocationRelativeTo(parent);
		dialog.setVisible(true);
		
		return response;
	}	
	
	public static void createErrorDialog(JFrame parent, String title, String message)
	{
		final JDialog dialog = new JDialog(parent, title, true);
		
		JLabel label = new JLabel(message);
						
		label.setHorizontalAlignment(JLabel.CENTER);

		JButton okButton = new JButton("OK");
		okButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				dialog.setVisible(false);
				dialog.dispose();
			}
		});
	

		JPanel labelPanel = new JPanel();
		labelPanel.add(Box.createHorizontalGlue());
		labelPanel.add(label);
		
		JPanel closePanel = new JPanel();
		closePanel.add(Box.createHorizontalGlue());
		closePanel.add(okButton);
		closePanel.setBorder(BorderFactory.createEmptyBorder(0,0,5,5));
		
		JPanel contentPane = new JPanel(new BorderLayout());
		contentPane.add(new JPanel(), BorderLayout.PAGE_START);
		contentPane.add(labelPanel, BorderLayout.CENTER);
		contentPane.add(closePanel, BorderLayout.PAGE_END);
		contentPane.setOpaque(true);
		dialog.setContentPane(contentPane);

		dialog.setSize(new Dimension(275,130));
		dialog.setLocationRelativeTo(parent);
		dialog.setVisible(true);
	}
	

	public static void createHelpDialog(JFrame parent, String title, String message)
	{
		final JDialog dialog = new JDialog(parent, title, true);

		JLabel label = new JLabel(message);
						
		label.setHorizontalAlignment(JLabel.CENTER);

		JButton okButton = new JButton("OK");
		okButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				dialog.setVisible(false);
				dialog.dispose();
			}
		});
	

		JPanel labelPanel = new JPanel();
		labelPanel.add(Box.createHorizontalGlue());
		labelPanel.add(label);
		
		JPanel closePanel = new JPanel();
		closePanel.add(Box.createHorizontalGlue());
		closePanel.add(okButton);
		closePanel.setBorder(BorderFactory.createEmptyBorder(0,0,5,5));
		
		JPanel contentPane = new JPanel(new BorderLayout());
		contentPane.add(new JPanel(), BorderLayout.PAGE_START);
		contentPane.add(labelPanel, BorderLayout.CENTER);
		contentPane.add(closePanel, BorderLayout.PAGE_END);
		contentPane.setOpaque(true);
		dialog.setContentPane(contentPane);

		dialog.setSize(new Dimension(275,130));
		dialog.setLocationRelativeTo(parent);
		dialog.setVisible(true);
	}
	
	public static void showErrorDialog(JFrame parent, String title, String message)
	{
		final JDialog dialog = new JDialog(parent, title, true);

//		JLabel label = new JLabel("<html><p align=center>"
//					+ message + "</p></html>");
		JTextArea label;
		JButton okButton;
		Container contentPane;
		FontMetrics metrics;
		
		GridBagLayout layout = new GridBagLayout();
		GridBagConstraints constraints = new GridBagConstraints();

		contentPane = dialog.getContentPane();
		contentPane.setLayout(layout);

		constraints.insets = new Insets(2,2,2,2);
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.weightx = 1;
		constraints.weighty = 1;
		constraints.fill = GridBagConstraints.BOTH;
		constraints.gridwidth = 1;
		
		label = new JTextArea();
		metrics = label.getFontMetrics(label.getFont());
		Rectangle2D bounds = metrics.getStringBounds(message, label.getGraphics());
		layout.columnWidths = new int[1];
		layout.columnWidths[0] = (int)bounds.getMaxX() + 4;
		layout.rowHeights = new int[1];
		layout.rowHeights[0] = (int)bounds.getMaxY() + 4;

		label.setText(message);
		label.setEditable(false);
		label.setAlignmentX(JTextArea.CENTER_ALIGNMENT);
		layout.setConstraints(label,constraints);
		contentPane.add(label);

		constraints.gridy ++;
		constraints.fill = GridBagConstraints.NONE;
		okButton = new JButton("OK");
		okButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e) 
			{
				dialog.setVisible(false);
				dialog.dispose();
			}
		});
		layout.setConstraints(okButton, constraints);
		contentPane.add(okButton);

		dialog.setLocationRelativeTo(parent);
//		dialog.pack();
		dialog.pack();
		dialog.setVisible(true);
	}
}
