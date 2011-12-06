package hulka.gui;
import javax.swing.*;
import java.awt.*;
import java.awt.image.*;

public class UnselectedListCellRenderer extends DefaultListCellRenderer
{
	private String defaultText;
	public UnselectedListCellRenderer(String defaultTxt)
	{
		defaultText = defaultTxt;
	}
	
	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
	{
		Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		if(value==null)
		{
			((JLabel)c).setText(defaultText);
		}
		return c;
	}
}
