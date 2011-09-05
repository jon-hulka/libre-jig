/**
 * 2011 03 18 - Jon Hulka
 * Changed PuzzleHandler from interface to abstract class and integrated GameMenuListener functions
 */
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.PrintStream;

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
	abstract public boolean save(PrintStream out);
	abstract public void redraw();
	/**
	 * 2011 02 11 - Jon: This is to fix a quirk in JigsawHandler - simultaneous drag and preview was causing incorrect mouseCount.
	 */
	abstract public void onPreview();
}
