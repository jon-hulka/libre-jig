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
 * changelog:
 * 
 * 2011 03 18 - Jon
 * Created PuzzleLoader for clean implementation of save and load functions.
 */

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;

import java.awt.Dimension;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.net.URISyntaxException;

public class PuzzleLoader
{
	NewPuzzleDialog newDialog;
	JFrame frame;
	GUI gui;
	PuzzleHandler puzzleHandler=null;
	JFileChooser savedGameChooser=null;

	private PuzzleLoader(){}
	public PuzzleLoader(GUI gui) throws FileNotFoundException, URISyntaxException
	{
		newDialog=new NewPuzzleDialog(null);
		this.gui=gui;
	}

	public boolean closeGame()
	{
		boolean ok=false;
//To do: once save and load are implemented, integrate a gamesaved flag
		ok=puzzleHandler==null||puzzleHandler.isGameComplete()||puzzleHandler.isGameSaved();

		if(!ok)ok=JOptionPane.showConfirmDialog(null,"Close current game?","Closing Game.",JOptionPane.OK_CANCEL_OPTION)==JOptionPane.OK_OPTION;

		if(ok)
		{
			if(puzzleHandler!=null)
			{
				gui.removeActionListener("game",puzzleHandler);
				puzzleHandler.disconnect();
			}
			puzzleHandler=null;
			toggleMenus(null);
		}

		return ok;
	}
	
	public boolean startGame()
	{
		boolean result=false;
		newDialog.setVisible(true);
		if(newDialog.getResponse()==NewPuzzleDialog.RESPONSE_OK && closeGame())
		{
			Dimension boardSize=gui.getBoardSize();
			PuzzleCanvas boardCanvas=gui.getBoardCanvas();
			puzzleHandler=newDialog.getPuzzleHandler(boardSize);
			toggleMenus(puzzleHandler);
			boardCanvas.setPuzzleImage(newDialog.getScaledImage(boardSize));
			boardCanvas.setMeanColor(newDialog.getMeanColor());
			puzzleHandler.connect(boardCanvas);
			gui.addActionListener("game",puzzleHandler);
			result=true;
		}
		return result;
	}

	private void toggleMenus(PuzzleHandler handler)
	{
		if(handler==null)
		{
//			gui.setMenuEnabled("load",false);
			gui.setMenuEnabled("save",false);
			gui.setMenuEnabled("preview",false);
			gui.setMenuEnabled("color",false);
			gui.setMenuEnabled("layer1",false);
			gui.setMenuEnabled("layer2",false);
			gui.setMenuEnabled("layer3",false);
		}
		else
		{
			if(handler instanceof JigsawHandler)
			{
				gui.setMenuEnabled("save",true);
				gui.setMenuEnabled("layer1",true);
				gui.setMenuEnabled("layer2",true);
				gui.setMenuEnabled("layer3",true);
			}
			gui.setMenuEnabled("preview",true);
			gui.setMenuEnabled("color",true);
			
		}
	}
	
	private JFileChooser getSavedGameChooser()
	{
		if(savedGameChooser==null)
		{
			savedGameChooser = new JFileChooser();
			FileNameExtensionFilter filter = new FileNameExtensionFilter("Saved Puzzles","vtp");
			savedGameChooser.setFileFilter(filter);
			savedGameChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			savedGameChooser.setAcceptAllFileFilterUsed(false);
		}
		return savedGameChooser;
	}
	
	public void saveGame()
	{
		JFileChooser savedGameChooser = getSavedGameChooser();
		File saveFile=null;
		int cValue = savedGameChooser.showSaveDialog(frame);
		if(cValue == JFileChooser.APPROVE_OPTION)
		{
			try
			{
				saveFile=savedGameChooser.getSelectedFile();
			}catch(Exception ex){ex.printStackTrace();}
		}
		
		if(saveFile!=null)
		{
			String path=saveFile.getAbsolutePath();
			//Check the extension
			if(!path.substring(path.length()-4).equals(".vtp"))
			{
				path=path+".vtp";
				saveFile=new File(path);
			}

			if(saveFile.exists())
			{
				if(!saveFile.isFile())
				{
					gui.showErrorDialog(saveFile.getName()+" is not a file.","Save");
				}
				else if(gui.showConfirmDialog("Overwrite file " + saveFile.getName() + "?","Save"))
				{
					saveGame(saveFile);
				}
			}
			else saveGame(saveFile);

		}
	}
	
	private void saveGame(File saveFile)
	{
		try
		{
			PrintStream out=new PrintStream(saveFile);
			out.println("version:"+gui.getAppVersion());
			puzzleHandler.save(out);
			out.close();
		}
		catch(Exception ex)
		{
			gui.showErrorDialog("Unable to save "+saveFile.getName()+"\n"+ex.getMessage(),"Save");
		}
	}
}

