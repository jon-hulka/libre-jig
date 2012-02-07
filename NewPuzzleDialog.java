/**
 *      NewPuzzleDialog.java
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

/**
 * Changelog:
 * 
 * 2011 12 19 - Jon
 *  - Finished implementing save and load functions
 * 
 * 2010 08 09 - Jon
 *  - Fixed OK button bug (button was enabled before any selections made)
 * 2010 07 26 - Jon
 * - Modified event handler code to reset piece count when other selections change.
 * - Changed default close ('x' button) behaviour to report no response (was defaulting to previous response).
 */

import hulka.util.ImageMap;
import hulka.gui.JSimpleImageCombo;
import hulka.util.MiscUtils;
import hulka.gui.UnselectedListCellRenderer;
import hulka.tilemanager.TileManager;
import hulka.tilemanager.SquareTileManager;
import hulka.tilemanager.HexSpinnerManager;
import hulka.tilemanager.SquareJigsawManager;
import hulka.tilemanager.HexJigsawManager;
import hulka.tilemanager.TileSetDescriptor;

//Components
import javax.swing.JFrame;
import javax.swing.Box;
import javax.swing.JDialog;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JComboBox;
import javax.swing.JButton;
import java.awt.Component;
import java.awt.BorderLayout;

import java.awt.Dimension;

//Image
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.Graphics;
import java.awt.Color;

//Event handling
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

//IO
import java.net.URL;
import java.net.MalformedURLException;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.IOException;

public class NewPuzzleDialog extends JDialog implements ActionListener
{
	public static final int SELECT_NONE = -1;

	public static final int PUZZLE_JIGSAW = 0;
	public static final int PUZZLE_SLIDER = 1;
	public static final int PUZZLE_SPINNER = 2;

	public static final int SHAPE_SQUARE = 0;
	public static final int SHAPE_HEX = 1;
	public static final int SHAPE_TRIANGLE = 2;
	
	public static final int RESPONSE_NONE=0;
	public static final int RESPONSE_CANCEL=1;
	public static final int RESPONSE_OK=2;
	
	private int response=RESPONSE_NONE;

	private static String TITLE="New Puzzle";
	private static int THUMB_SIZE=128;
	private static int PREVIEW_SIZE=400;
	private static String IMAGELIST_OTHER="images/folder.png";
	private static String IMAGELIST_CONFIG="pics.xml";
	private static String IMAGELIST_NULL="images/imagelistnull.png";
	private static String SHAPELIST_NULL="images/shapelistnull.png";
	private static String [] SHAPELIST={"images/square.png","images/hex.png"};
	private static String PUZZLIST_NULL="images/puzzlelistnull.png";
	private static String [] PUZZLIST={"images/jigsaw.png","images/slider.png","images/spinner.png"};
	private static String ACTION_IMAGE="image";
	private static String ACTION_TYPE="type";
	private static String ACTION_SHAPE="shape";
	private static String ACTION_PIECECOUNT="pieces";
	private static String ACTION_OK="ok";
	private static String ACTION_CANCEL="cancel";

	//Suggested piece counts for the tileManager
	private static final int [][] preferredSizes = {{12,24,48,96,250},{8,15,25},null};
	//Temporary storage for getPuzzleSizes
	private TileSetDescriptor tsDescriptor=new TileSetDescriptor();
	//Temporary storage
	private Dimension tempSize=new Dimension();

	private ImageMap imageMap=null;
	private JLabel previewPane=null;
	private ImageIcon preview=null;
	private URL imageURL=null;
	private BufferedImage puzzleImage=null;
	private Color meanColor=null;
	private PuzzleHandler puzzleHandler=null;

	private JComboBox pieceCountCombo=null;
	private JSimpleImageCombo shapeCombo=null;
	private JSimpleImageCombo imageCombo=null;
	private JSimpleImageCombo puzzleCombo=null;
	private JButton okButton=null;
	//Only to be used by actionPerformed
	private boolean actionsEnabled=true;

	private NewPuzzleDialog(){}
	public NewPuzzleDialog(JFrame owner) throws FileNotFoundException, URISyntaxException
	{
		super(owner, TITLE, true);
		imageMap = new ImageMap(IMAGELIST_CONFIG,10);
		buildComponents();
		pack();
		setResizable(false);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
	}
	
	public void setVisible(boolean visible)
	{
		if(!isVisible())response=RESPONSE_NONE;
		super.setVisible(visible);
	}
	
	private void buildComponents()
	{
		Box innerContent=Box.createHorizontalBox();
		Box selectionContent=Box.createVerticalBox();
		Box buttonContent=Box.createHorizontalBox();

		previewPane=new JLabel();
		imageCombo=getImageComboBox();
		Dimension size=imageCombo.getPreferredSize();
		imageCombo.setToolTipText("Choose a picture");
		imageCombo.setMinimumSize(size);
		imageCombo.setPreferredSize(size);
		imageCombo.setMaximumSize(size);
		imageCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
		int height=size.height;

		puzzleCombo=getPuzzleComboBox();
		puzzleCombo.setToolTipText("Choose a puzzle type");
		puzzleCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
		height+=puzzleCombo.getPreferredSize().height;

		shapeCombo=getShapeComboBox();
		shapeCombo.setToolTipText("Choose a tile shape");
		shapeCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
		height+=shapeCombo.getPreferredSize().height;

		pieceCountCombo = new JComboBox();
		pieceCountCombo.setToolTipText("Number of tiles");
		pieceCountCombo.setRenderer(new UnselectedListCellRenderer("Puzzle size..."));
		setPieceCounts(null);
		Dimension pcSize=pieceCountCombo.getPreferredSize();
		pcSize.width=size.width;
		pieceCountCombo.setMinimumSize(pcSize);
		pieceCountCombo.setPreferredSize(pcSize);
		pieceCountCombo.setMaximumSize(pcSize);
		pieceCountCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
		
		size=new Dimension();
		size.width=height;
		size.height=height;
		previewPane.setMinimumSize(size);
		previewPane.setMaximumSize(size);
		previewPane.setPreferredSize(size);
		preview=new ImageIcon();
		previewPane.setIcon(preview);
		
		okButton=new JButton("OK");
		JButton cancelButton=new JButton("Cancel");

		buttonContent.add(Box.createHorizontalGlue());
		buttonContent.add(okButton);
		buttonContent.add(cancelButton);
		buttonContent.add(Box.createHorizontalGlue());
		
		selectionContent.add(imageCombo);
		selectionContent.add(puzzleCombo);
		selectionContent.add(shapeCombo);
		selectionContent.add(pieceCountCombo);

		innerContent.add(selectionContent);
		innerContent.add(previewPane);

		add(innerContent);
		add(buttonContent,BorderLayout.SOUTH);
		
		imageCombo.setActionCommand(ACTION_IMAGE);
		puzzleCombo.setActionCommand(ACTION_TYPE);
		shapeCombo.setActionCommand(ACTION_SHAPE);
		pieceCountCombo.setActionCommand(ACTION_PIECECOUNT);
		okButton.setActionCommand(ACTION_OK);
		okButton.setEnabled(false);
		cancelButton.setActionCommand(ACTION_CANCEL);
		imageCombo.addActionListener(this);
		puzzleCombo.addActionListener(this);
		shapeCombo.addActionListener(this);
		pieceCountCombo.addActionListener(this);
		okButton.addActionListener(this);
		cancelButton.addActionListener(this);
	}
	
	private BufferedImage [] loadImages(String [] paths)
	{
		BufferedImage [] images = new BufferedImage[paths.length];
		for(int i=0; i<paths.length; i++)
		{
			images[i]=MiscUtils.loadImage(paths[i],THUMB_SIZE,THUMB_SIZE);
		}
		return images;
	}

	private JSimpleImageCombo getShapeComboBox()
	{
		BufferedImage [] images = loadImages(SHAPELIST);
		BufferedImage img = MiscUtils.loadImage(SHAPELIST_NULL,THUMB_SIZE,THUMB_SIZE);
		JSimpleImageCombo comboBox = new JSimpleImageCombo(images, img);
		return comboBox;
	}

	private JSimpleImageCombo getPuzzleComboBox()
	{
		BufferedImage [] images = loadImages(PUZZLIST);
		BufferedImage img = MiscUtils.loadImage(PUZZLIST_NULL,THUMB_SIZE,THUMB_SIZE);
		JSimpleImageCombo comboBox = new JSimpleImageCombo(images, img);
		return comboBox;
	}

	private JSimpleImageCombo getImageComboBox()
	{
		BufferedImage [] images = new BufferedImage[imageMap.size() + 1];
		for(int i = 0; i < images.length-1; i++)
		{
			images[i]=MiscUtils.loadImage(imageMap.getThumbPath(i),THUMB_SIZE,THUMB_SIZE);
		}
		images[images.length-1]=MiscUtils.loadImage(IMAGELIST_OTHER,THUMB_SIZE,THUMB_SIZE);
		BufferedImage img=MiscUtils.loadImage(IMAGELIST_NULL,THUMB_SIZE,THUMB_SIZE);
		JSimpleImageCombo comboBox=new JSimpleImageCombo(images, img);
		return comboBox;
	}

	private boolean loadImage(URL url)
	{
		boolean result=true;
		imageURL=null;
		puzzleImage=null;
		previewPane.setVisible(false);
		if(url!=null)
		{
			try
			{
				puzzleImage = ImageIO.read(url);
			}
			catch(Exception ex)
			{
				result=false;
			}
			if(result)
			{
				if(puzzleImage!=null)
				{
					imageURL=url;
					int w=puzzleImage.getWidth();
					int h=puzzleImage.getHeight();
					int s=w>h?w:h;
					w=w*PREVIEW_SIZE/s;
					h=h*PREVIEW_SIZE/s;
					BufferedImage pv = new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
					Graphics g=pv.getGraphics();
					g.drawImage(puzzleImage,0,0,w,h,null);
					g.dispose();
					preview.setImage(pv);
					previewPane.setVisible(true);
					result=true;
				}
				else
				{
					result=false;
				}
			}
		}
		return result;
	}

	private void loadImage(int index)
	{
		puzzleImage=null;
		previewPane.setVisible(false);
		try
		{
			URL url = MiscUtils.translateURL(imageMap.getImagePath(index));
			meanColor=imageMap.getMeanColor(index);
			if(!loadImage(url))
			{
				JOptionPane.showMessageDialog(this,"Error loading image","Error",JOptionPane.ERROR_MESSAGE);
			}
		}
		catch(Exception ex)
		{
			JOptionPane.showMessageDialog(this,ex.getMessage(),"Error loading image",JOptionPane.ERROR_MESSAGE);
		}
	}

	private void clearPieceCounts()
	{
		pieceCountCombo.setSelectedIndex(-1);
		pieceCountCombo.removeAllItems();
		pieceCountCombo.setEnabled(false);
	}

	private void setPieceCounts(int [] pieceCounts)
	{
		if(pieceCounts != null)
		{
			for(int i = 0; i < pieceCounts.length; i++)
			{
				pieceCountCombo.addItem(pieceCounts[i] + " pieces");
			}
			pieceCountCombo.setSelectedIndex(-1);
			pieceCountCombo.setEnabled(true);
		}
	}

	/**
	 * Gets a list of tile counts for the given puzzle type and shape
	 */
	private int [] getPuzzleSizes(int puzzleIndex,int shapeIndex,int width,int height)
	{
		int [] result = null;
		switch(puzzleIndex)
		{
			case PUZZLE_JIGSAW:
				result=new int[preferredSizes[puzzleIndex].length];
				for(int i=0;i<preferredSizes[puzzleIndex].length;i++)
				{
					tsDescriptor=shapeIndex==SHAPE_SQUARE
						?SquareJigsawManager.getBestFit(width,height,preferredSizes[puzzleIndex][i],true,tsDescriptor)
						:HexJigsawManager.getBestFit(width,height,preferredSizes[puzzleIndex][i],true,tsDescriptor);
					result[i]=tsDescriptor.tileCount;
				}
				break;
			default:
				result=preferredSizes[puzzleIndex];
				break;
		}
		return result;
	}

	private void validateIndices()
	{
		int puzzleIndex=puzzleCombo.getSelectedIndex();
		if(puzzleIndex!=SELECT_NONE)
		{
			switch(puzzleIndex)
			{
				case PUZZLE_SLIDER:
					shapeCombo.setSelectedIndex(SHAPE_SQUARE);
					shapeCombo.setEnabled(false);
					okButton.setEnabled((pieceCountCombo.getSelectedIndex()!=SELECT_NONE));
					break;
				case PUZZLE_SPINNER:
					shapeCombo.setSelectedIndex(SHAPE_HEX);
					shapeCombo.setEnabled(false);
					okButton.setEnabled(true);
					break;
				default:
					shapeCombo.setEnabled(true);
					okButton.setEnabled((pieceCountCombo.getSelectedIndex()!=SELECT_NONE));
			}

			int shapeIndex=shapeCombo.getSelectedIndex();

			if(puzzleImage!=null && shapeIndex!=-1 && pieceCountCombo.getItemCount()==0)
			{
				//Populate piece counts
				setPieceCounts(getPuzzleSizes(puzzleIndex,shapeIndex,puzzleImage.getWidth(),puzzleImage.getHeight()));
			}
		}
		else okButton.setEnabled(false);
	}
	
	public int getResponse()
	{
		return response;
	}

	/**
	 * Get the maximum scaled size (preserving aspect ratio) for puzzleImage to fit within bounds
	 */
	private Dimension getBestFit(Dimension bounds, Dimension result)
	{
		if(result==null)result=new Dimension();
		int width=puzzleImage.getWidth();
		int height=puzzleImage.getHeight();
		int w=0;
		int h=0;
		if(bounds!=null)
		{
			//Scale to fit the window
			w=width>bounds.width?bounds.width:width;
			h=height>bounds.height?bounds.height:height;
			double rw=(double)w/(double)width;
			double rh=(double)h/(double)height;
			if(rw<rh){h=height*w/width;}
			else{w=width*h/height;}
		}
		result.width=w;
		result.height=h;
		return result;
	}
	/**
	 * Returns the selected image, scaled to fit the given bounds.
	 */
	public BufferedImage getScaledImage(Dimension bounds)
	{
		tempSize=getBestFit(bounds,tempSize);
		BufferedImage result=new BufferedImage(tempSize.width,tempSize.height,BufferedImage.TYPE_INT_ARGB);
		Graphics g=result.getGraphics();
		g.drawImage(puzzleImage,0,0,tempSize.width,tempSize.height,null);
		g.dispose();
		return result;
	}
	
	
	public PuzzleHandler getPuzzleHandler(Dimension bounds)
	{
		tempSize=getBestFit(bounds,tempSize);

		PuzzleHandler handler=null;
		TileManager manager=null;
		int pieceCountIndex=pieceCountCombo.getSelectedIndex();
		switch(puzzleCombo.getSelectedIndex())
		{
			case PUZZLE_JIGSAW:
				switch(shapeCombo.getSelectedIndex())
				{
					case SHAPE_SQUARE:
						tsDescriptor=SquareJigsawManager.getBestFit(tempSize.width,tempSize.height,preferredSizes[PUZZLE_JIGSAW][pieceCountIndex],true,tsDescriptor);
						manager=new SquareJigsawManager(tempSize.width,tempSize.height,tsDescriptor.tilesAcross,tsDescriptor.tilesDown);
						break;
					case SHAPE_HEX:
						tsDescriptor=HexJigsawManager.getBestFit(tempSize.width,tempSize.height,preferredSizes[PUZZLE_JIGSAW][pieceCountIndex],true,tsDescriptor);
						manager=new HexJigsawManager(tempSize.width,tempSize.height,tsDescriptor.tilesAcross,tsDescriptor.tilesDown);
						break;
				}
				handler=new JigsawHandler(manager);
				break;
			case PUZZLE_SLIDER:
				int tilesAcross=(int)(Math.sqrt(preferredSizes[PUZZLE_SLIDER][pieceCountIndex])+1);
				manager=new SquareTileManager(tempSize.width,tempSize.height,tilesAcross,tilesAcross);
				handler=new SliderHandler(manager);
				break;
			case PUZZLE_SPINNER:
				HexSpinnerManager hsManager=new HexSpinnerManager(tempSize.width,tempSize.height);
				handler=new SpinnerHandler(hsManager);
				break;
		}
		return handler;
	}
	
	public Color getMeanColor()
	{
		return meanColor;
	}

	public void actionPerformed(ActionEvent e)
	{
		if(actionsEnabled)
		{
			actionsEnabled=false;

			String cmd=e.getActionCommand();
			if(cmd.equals(ACTION_IMAGE))
			{
				int index=imageCombo.getSelectedIndex();
				if(index==imageMap.size())
				{
					meanColor=new Color(128,128,128);
					imageCombo.clearSelection();
					if(!loadImage(MiscUtils.promptImageURL(null,null)))
					{
						JOptionPane.showMessageDialog(this,"Error loading image","Error",JOptionPane.ERROR_MESSAGE);
					}
				}
				else if(index>=0)
				{
					loadImage(index);
				}
				else
				{
					loadImage(null);
				}
				clearPieceCounts();
				validateIndices();
			}
			else if(cmd.equals(ACTION_TYPE))
			{
				clearPieceCounts();
				validateIndices();
			}
			else if(cmd.equals(ACTION_SHAPE))
			{
				clearPieceCounts();
				validateIndices();
			}
			else if(cmd.equals(ACTION_PIECECOUNT))
			{
				validateIndices();
			}
			else if(cmd.equals(ACTION_OK))
			{
				response=RESPONSE_OK;
				setVisible(false);
			}
			else if(cmd.equals(ACTION_CANCEL))
			{
				response=RESPONSE_CANCEL;
				setVisible(false);
			}

			actionsEnabled=true;
		}
	}
	
	public boolean save(PrintWriter out, PrintWriter err)
	{
		boolean result=true;
		if(imageURL==null)
		{
			result=false;
			err.println("NewPuzzleDialog.save: imageURL is null.");
		}
		
		if(result)
		{
			out.println("NewPuzzleDialog");
			out.println(imageURL.toString());
		}
		return result;
	}
	
	public boolean load(BufferedReader in, PrintWriter err)
	{
		boolean result=true;
		String line=null;
		URL url=null;
		try
		{
			line=in.readLine();
			result=line!=null;
			line=in.readLine();
			result=line!=null;
			if(!result)
			{
				err.println("NewPuzzleDialog.load: Unexpected end of file.");
			}
		}
		catch(IOException ex)
		{
			result=false;
			err.println("NewPuzzleDialog.load: " + ex.getMessage());
		}

		if(result)
		{
			try
			{
				url=new URL(line);
			}
			catch(MalformedURLException ex)
			{
				result=false;
				err.println("NewPuzzleDialog.load: Invalid image file URL: " + line);
			}
		}
		if(result && !loadImage(url))
		{
			err.println("Error loading image");
		}
		return result;
	}
}
