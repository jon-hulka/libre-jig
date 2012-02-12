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
 * 2012 02 02 - Jon
 *  - Changed the compatible version, save file format has changed.
 * 
 * 2012 01 02 - Jon
 *  - Fixed File->Save menu item enabling/disabling so it is available when appropriate
 * 
 * 2011 12 19 - Jon
 *  - Finished implementing save and load functions
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
import java.awt.Color;

import java.io.FileNotFoundException;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.FileReader;
import java.net.URISyntaxException;

import hulka.util.ArrayWriter;
import hulka.util.ArrayReader;

public class PuzzleLoader
{
	private NewPuzzleDialog newDialog;
	private JFrame frame;
	GUI gui;
	private PuzzleHandler puzzleHandler=null;
	private JFileChooser savedGameChooser=null;
	//Save file extension
	private static final String SAVE_FILE_EXTENSION="ljf";
	//Earliest game version save files are compatible with
	//Change this only if save file formats change
	private static final int [] SAVE_COMPATIBLE_VERSION={2012,01,31};

	private PuzzleLoader(){}
	public PuzzleLoader(GUI gui) throws FileNotFoundException, URISyntaxException
	{
		newDialog=new NewPuzzleDialog(gui.getFrame(),gui.getFolderPath("data"));
		this.gui=gui;
	}

	public boolean closeGame()
	{
		boolean ok=false;
//To do: once save and load are implemented, integrate a gamesaved flag
		ok=puzzleHandler==null||puzzleHandler.isGameComplete()||puzzleHandler.isGameSaved();

		if(!ok)ok=JOptionPane.showConfirmDialog(gui.getFrame(),"Close current game?","Closing Game.",JOptionPane.OK_CANCEL_OPTION)==JOptionPane.OK_OPTION;

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
	
	public void loadGame()
	{
		JFileChooser savedGameChooser = getSavedGameChooser();
		File saveFile=null;
		int cValue = savedGameChooser.showOpenDialog(frame);
		if(cValue == JFileChooser.APPROVE_OPTION)
		{
			try
			{
				saveFile=savedGameChooser.getSelectedFile();
			}catch(Exception ex){ex.printStackTrace();}
		}
		
		if(saveFile!=null)
		{
			if(saveFile.exists())
			{
				if(!saveFile.isFile())
				{
					gui.showErrorDialog(saveFile.getName()+" is not a file.","Load");
				}
				else if(closeGame())
				{
					loadGame(saveFile);
				}
			}
			else
			{
				gui.showErrorDialog(saveFile.getName()+" does not exist.","Load");
			}
		}

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
			else
			{
				gui.setMenuEnabled("save",false);
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
			FileNameExtensionFilter filter = new FileNameExtensionFilter("Saved Puzzles",SAVE_FILE_EXTENSION);
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
			if(!path.substring(path.length()-4).equals("."+SAVE_FILE_EXTENSION))
			{
				path=path+"."+SAVE_FILE_EXTENSION;
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
					try{saveFile.delete();}catch(Exception ex){/*No harm trying...*/}
					saveGame(saveFile);
				}
			}
			else saveGame(saveFile);

		}
	}
	
	private void saveGame(File saveFile)
	{
		boolean result=true;
		StringWriter errorMessage=new StringWriter();
		PrintWriter err=new PrintWriter(errorMessage);
		try
		{
			PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(saveFile)));
			//Not used for anything at the moment, just nice to know what version is being used
			out.println("version:"+gui.getAppVersion());
			Color backgroundColor=gui.getBoardCanvas().getBackground();
			//Save compatible version
			result=new ArrayWriter(3,1,"compatibleVersion").save(
				new int [][] {{SAVE_COMPATIBLE_VERSION[0]},{SAVE_COMPATIBLE_VERSION[1]},{SAVE_COMPATIBLE_VERSION[2]}},
				new String [] {"y","m","d"},
				out,err);
			//Save background color
			if(result) result=new ArrayWriter(3,1,"backgroundColor").save(
				new int [][] {{backgroundColor.getRed()},{backgroundColor.getGreen()},{backgroundColor.getBlue()}},
				new String [] {"r","g","b"},
				out,err);
			if(result) result = newDialog.save(out,err);
			if(result) result = ((JigsawHandler)puzzleHandler).save(out,err);
			out.close();
			if(!result)
			{
				try{saveFile.delete();}catch(Exception ex){/*No harm trying...*/}
			}
		}
		catch(Exception ex)
		{
			result=false;
			err.println("Unable to save "+saveFile.getName()+" in PuzzleLoader\n"+ex.getMessage());
		}
		err.close();

		if(!result)
		{
			gui.showErrorDialog(errorMessage.toString() ,"Save error");
		}
	}
	
	private void loadGame(File loadFile)
	{
		boolean result=true;
		ArrayReader reader=null;

		StringWriter errorMessage=new StringWriter();
		PrintWriter err=new PrintWriter(errorMessage);
		String line=null;
		String color=null;
		String compatibleVersion=null;
		BufferedReader in=null;
		
		Color backgroundColor=null;

		try
		{
			in = new BufferedReader(new FileReader(loadFile));

			//Version
			if(result)
			{
				line=in.readLine();
				result=line!=null;
			}
			
			if(!result)
			{
				err.println("PuzzleLoader: Unexpected end of file.");
			}
		}
		catch(IOException ex)
		{
			result=false;
			err.println("PuzzleLoader: Unable to read file: " + ex.getMessage());
		}

		//Compatible version
		if(result)
		{
			reader = new ArrayReader("compatibleVersion");
			result = reader.load(in,err);
		}
		
		int [][] values = new int[3][];
		String [] columnNames={"y","m","d"};
		for(int i=0; result && i<3; i++)
		{
			values[i] = reader.getColumn(columnNames[i],err);
			result=values[i]!=null;
		}
		if(result && (values[0][0]!=SAVE_COMPATIBLE_VERSION[0] || values[1][0]!=SAVE_COMPATIBLE_VERSION[1] || values[2][0]!=SAVE_COMPATIBLE_VERSION[2]))
		{
			result=false;
			err.println("PuzzleLoader: The saved file is incompatible with this version.");
		}
		
		//Background color
		if(result)
		{
			reader = new ArrayReader("backgroundColor");
			result = reader.load(in,err);
		}
		
		columnNames= new String []{"r","g","b"};
		for(int i=0; result && i<3; i++)
		{
			values[i] = reader.getColumn(columnNames[i],err);
			result=values[i]!=null;
		}
		
		if(result) backgroundColor=new Color(values[0][0],values[1][0],values[2][0]);
		
		if(result) result = newDialog.load(in,err);
		
		if(result)
		{
			Dimension boardSize=gui.getBoardSize();
			PuzzleCanvas boardCanvas=gui.getBoardCanvas();
			boardCanvas.setPuzzleImage(newDialog.getScaledImage(boardSize));
			boardCanvas.setBackground(backgroundColor);
			puzzleHandler=PuzzleHandler.load(boardCanvas,in,err);
			toggleMenus(puzzleHandler);
			gui.addActionListener("game",puzzleHandler);
			result=puzzleHandler!=null;
		}

		
		if(!result)
		{
			gui.showErrorDialog(errorMessage.toString() ,"Load error");
		}
	}
}

