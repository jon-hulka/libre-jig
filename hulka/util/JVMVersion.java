/**
 * Provides a simple interface for retrieving and checking the JVM version
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
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.import java.awt.*;
 */

package hulka.util;

public class JVMVersion
{
	private int maj = 0;
	private int min = 0;
	private int rev = 0;
	private int pos = 0;
	
	public JVMVersion()
	{
		try
		{
			String jVersion = System.getProperty("java.version");
			maj = Integer.parseInt(extractInt(jVersion));
			jVersion = jVersion.substring(pos + 1);
			min = Integer.parseInt(extractInt(jVersion));
			jVersion = jVersion.substring(pos + 1);
			rev = Integer.parseInt(extractInt(jVersion));
		}
		catch(Exception ex){}
	}
	
	public int getMajor(){return maj;}
	public int getMinor(){return min;}
	public int getRevision(){return rev;}
	
	/**
	 * Checks JVM version against the supplied parameters
	 * @param major major version number
	 * @param minor minor version number
	 * @param revision version revision number
	 * @return true if the JVM version is greater than or equal to the specified version, false if it is less
	 */
	public boolean check(int major, int minor, int revision)
	{
		boolean ok = true;
		if(maj < major)
		{
			ok = false;
		}
		else if(maj==major)
		{
			if(min < minor)
			{
				ok = false;
			}
			else if(min==minor)
			{
				ok = rev >= revision;
			}
		}
		return ok;
	}

	private String extractInt(String input)
	{
		char ch = '0';
		pos = -1;
		while(ch >= '0' && ch <= '9')
		{
			pos ++;
			ch = pos < input.length() ? input.charAt(pos) : 'x';
		}
		return input.substring(0,pos);
	}
}
