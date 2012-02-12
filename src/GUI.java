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

/**
 * Changelog
 * 2012 01 02 - Jon
 *  - Fixed File->Save menu item enabling/disabling so it is available when appropriate
 * 2012 01 02 - Jon
 *  - Added getFrame so PuzzleLoader can pass a parent window on to NewPuzzleDialog.
 * 2011 03 18 - Jon
 *  - Removed game menu handling from GUI (GUI no longer has any direct interaction with PuzzleHandler)
 * 2011 02 11 - Jon
 * - Added call to puzzleHandler.onPreview() to showPreview (see JigsawHandler notes)
 * 2010 07 29 - Jon
 * - Added logic to disable and enable menu items
 * 2010 07 08 - Jon
 * - Changed 'game' menu to 'file'
 * - Created puzzle menu for game options
 *   - Layers
 *   - Background color
 *   - Preview
 * Moved control of layers and preview to menu level
 * Integrated color chooser for background color selection and made necessary changes to PuzzleCanvas and PuzzleHandler
 * 2010 06 16 - Jon
 * Began retrofitting to use ConnectedSet - ConnectedSet requires some debugging
 * 2010 06 14 - Jon
 * Finished reintegrating into the new structure.
 * 20100611 - Jon
 * Moved tile drawing code into PuzzleCanvas
 * 20100607 - Jon
 * Upgraded menu building and handling code to use GUILoader
 * The Nimbus look and feel is now used, if available
 */

//@todo - move a lot of stuff into PuzzleLoader
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.*;

import javax.swing.UIManager;
	import javax.swing.UIManager.*;

import hulka.event.*;
//import hulka.graphics.*;
import hulka.gui.*;
import hulka.util.JVMVersion;

class GUI
{
	Dimension boardSize=null;
	JScrollPane boardPane=null;
	PuzzleCanvas boardCanvas=null;
	JFrame frame=null;
	PuzzleLoader puzzleLoader=null;
	GUILoader guiLoader=null;
	
	public static void main(String [] args)
	{
		JVMVersion version = new JVMVersion();
		if(version.check(1,6,0))
		{
			// turn on anti-aliasing for smooth fonts.
			System.setProperty( "swing.aatext", "true" );
			// try to load Nimbus look and feel
			try {
				for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
					if ("Nimbus".equals(info.getName())) {
						UIManager.setLookAndFeel(info.getClassName());
						break;
					}
				}
			} catch (Exception ex){}
			GUI ui = new GUI();
		}
		else
		{
			JOptionPane.showMessageDialog(null,
				"You must have Java version 1.6 to run this program.\nYour version is " + version.getMajor() + "." + version.getMinor() + ".",
				"Invalid JVM version", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	public GUI()
	{
		if(buildGui()) showGui();
	}
	
	public JFrame getFrame()
	{
		return guiLoader.getFrame();
	}
	
	private boolean buildGui()
	{
		boolean result = false;
		guiLoader = new GUILoader("gui.xml");
		if(guiLoader.loadGUI())
		{
			result=false;
			frame = guiLoader.getFrame();
			try
			{
				puzzleLoader=new PuzzleLoader(this);
			}catch(Exception ex){ex.printStackTrace();}
			FileMenuListener fml=new FileMenuListener(this);
			frame.addWindowListener(fml);
			guiLoader.getActionCoordinator().addActionListener("file",fml);
			
			boardPane=new JScrollPane();
			boardPane.setPreferredSize(new Dimension(550,350));
			frame.setContentPane(boardPane);
			
			frame.pack();
			frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
			guiLoader.getMenuItem("save").setEnabled(false);
			guiLoader.getMenuItem("preview").setEnabled(false);
			guiLoader.getMenuItem("color").setEnabled(false);
			guiLoader.getMenuItem("layer1").setEnabled(false);
			guiLoader.getMenuItem("layer2").setEnabled(false);
			guiLoader.getMenuItem("layer3").setEnabled(false);
			result=true;
		}
		return result;
	}
	
	public void addActionListener(String category,ActionListener listener)
	{
		guiLoader.getActionCoordinator().addActionListener(category,listener);
	}
	
	public void removeActionListener(String category,ActionListener listener)
	{
		guiLoader.getActionCoordinator().removeActionListener(category,listener);
	}

	public void setMenuEnabled(String name, boolean enabled)
	{
		guiLoader.getMenuItem(name).setEnabled(enabled);
	}

	private void showGui()
	{
		frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
		frame.setVisible(true);
		Rectangle area=null;
//Todo: find out if there is a better way to do this, rather than waiting for resize.
		//Assuming there is enough space for at least 600px across
		//Wait for resizing to occur
		while(area==null||area.width<600)
		{
			try{Thread.sleep(10);}catch(Exception ex){}
			area=SwingUtilities.calculateInnerArea(boardPane,area);
		}
		boardSize=new Dimension(area.width,area.height);
		boardCanvas=new PuzzleCanvas(boardSize);
		boardPane.setViewportView(boardCanvas);
//This might be a bit annoying.		guiLoader.showDocument("credits");
		startGame();
	}
	
	public Dimension getBoardSize()
	{
		return new Dimension(boardSize);
	}
	
	public PuzzleCanvas getBoardCanvas()
	{
		return boardCanvas;
	}

	public void startGame()
	{
		puzzleLoader.startGame();
	}
	
	public void loadGame()
	{
		puzzleLoader.loadGame();
	}
	
	public void saveGame()
	{
		puzzleLoader.saveGame();
	}
	
	public void quit()
	{
		if(puzzleLoader.closeGame())
		{
			frame.setVisible(false);
			frame.dispose();
			System.exit(0);
		}
	}
	
	public void showErrorDialog(String message,String title)
	{
			JOptionPane.showMessageDialog(frame,
				message,
				title, JOptionPane.ERROR_MESSAGE);
	}
	
	public boolean showConfirmDialog(String message,String title)
	{
		return JOptionPane.showConfirmDialog(frame,message,title,JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION;
	}
	
	public String getAppVersion()
	{
		return guiLoader.getAppVersion();
	}
	
	public String getFolderPath(String name)
	{
		return guiLoader.getFolderPath(name);
	}
}
