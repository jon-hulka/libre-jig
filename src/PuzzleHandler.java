/**
 *   Copyright (C) 2011  Jonathan Hulka
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
 * 
 * 2011 12 19 - Jon
 *  - Finished implementing save and load functions
 * 
 * 2011 03 18 - Jon Hulka
 * Changed PuzzleHandler from interface to abstract class and integrated GameMenuListener functions
 */
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.IOException;

public abstract class PuzzleHandler implements ActionListener
{
	public static String ACTION_PREVIEW="game:preview";
	public static String ACTION_COLOR="game:color";
	public static String ACTION_LAYER1="game:layer1";
	public static String ACTION_LAYER2="game:layer2";
	public static String ACTION_LAYER3="game:layer3";
	
	protected PuzzleCanvas ui;

	public void actionPerformed(ActionEvent e)
	{
		String cmd=e.getActionCommand();
		if(cmd.equals(ACTION_PREVIEW))
		{
			onPreview();
			ui.showPreview();
		}
		else if(cmd.equals(ACTION_COLOR))
		{
			ui.chooseColor();
			redraw();
		}
		else if(cmd.equals(ACTION_LAYER1)){setLayer(0);}
		else if(cmd.equals(ACTION_LAYER2)){setLayer(1);}
		else if(cmd.equals(ACTION_LAYER3)){setLayer(2);}
	}
	
	private void setLayer(int layer)
	{
		if(this instanceof JigsawHandler)
		{
			((JigsawHandler)this).setLayer(layer);
		}
	}

	abstract public void connect(PuzzleCanvas canvas);
	abstract public void disconnect();
	abstract public boolean isGameComplete();
	abstract public boolean isGameSaved();
	
	public static PuzzleHandler load(PuzzleCanvas boardCanvas, BufferedReader in, PrintWriter err)
	{
		PuzzleHandler result=null;
		String puzzleType=null;
		try
		{
			puzzleType=in.readLine();
			if(puzzleType==null)
			{
				err.println("PuzzleHandler.load(): Unexpected end of file.");
			}
		}
		catch(IOException ex)
		{
			puzzleType=null;
			err.println("PuzzleHandler.load(): " + ex.getMessage());
		}

		if(puzzleType!=null && "JigsawHandler".equals(puzzleType))
		{
			result=JigsawHandler.load(boardCanvas,in,err);
		}
		else
		{
			err.println("Puzzle type unknown or missing.");
		}
		return result;
	}

	abstract public void redraw();
	/**
	 * 2011 02 11 - Jon: This is to fix a quirk in JigsawHandler - simultaneous drag and preview was causing incorrect mouseCount.
	 */
	abstract public void onPreview();
}
