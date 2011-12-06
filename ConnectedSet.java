/**
 *      ConnectedSet.java
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

import java.io.PrintStream;

/**
 * Represents a set of elements that can be connected in groups.
 */
public class ConnectedSet
{
	private int [] connectedIndex;
	private int [] connectedCount;
	private int group;
	private int nextValue;

	public ConnectedSet(int size)
	{
		connectedIndex=new int[size];
		connectedCount=new int[size];
		reset();
	}
	
	public void reset()
	{
		group=-1;
		nextValue=-1;
		for(int i=0; i<connectedIndex.length; i++)
		{
			connectedIndex[i]=i;
			connectedCount[i]=1;
		}
	}
	
	public int getGroupSize(int index)
	{
		return connectedCount[index];
	}
	
	/**
	 * Sets a group for traversal by getNext()
	 * @param index element index.
	 */
	public void setGroup(int index)
	{
		group=index;
		nextValue=index;
	}

	/**
	 * Returns the next element of the assigned group.
	 * (see setGroup)
	 * @return index of the next element, or -1 if the whole group has been traversed.
	 */
	public int getNext()
	{
		int value=nextValue;
		if(nextValue>-1)nextValue=connectedIndex[nextValue];
		if(nextValue==group)nextValue=-1;
		return value;
	}
	
	/**
	 * Determines if two elements are connected.
	 * @param index1 element index
	 * @param index2 element index
	 * @return true if the elements are connected, false otherwise.
	 */
	public boolean isConnected(int index1,int index2)
	{
		boolean result=index1==index2;
		for(int i=connectedIndex[index1];!result&&i!=index1;i=connectedIndex[i]) result=i==index2;
		return result;
	}

	/**
	 * Connects two groups.
	 * Does not work if the two elements already belong to the same group.
	 * @param i element index
	 * @param j element index
	 * @return number of elements in the resulting group.
	 */
	public int connect(int i, int j)
	{
		int totalCount=connectedCount[i]+connectedCount[j];

		//Find the bottom of both groups
		for(;connectedIndex[i]>i;i=connectedIndex[i]);
		for(;connectedIndex[j]>j;j=connectedIndex[j]);
		
		//Start at the top of both groups
		i=connectedIndex[i];
		j=connectedIndex[j];
		
		int top=-1;
		if(i>j)
		{
			top=j;
			j=connectedIndex[j];
		}
		else
		{
			top=i;
			i=connectedIndex[i];
		}
		int pos=top;
		int next=-1;

		//Continue until the bottom of both lists are reached.
		do
		{
			//The initial values and loop condition ensure that either i or j is valid.
			if(j>pos && (i>j||i<=pos))
			{
				//j is downstream and less than i or i is upstream.
				next=j;
				j=connectedIndex[j];
			}
			else
			{
				//j isn't valid, so i must be.
				next=i;
				i=connectedIndex[i];
			}
			connectedIndex[pos]=next;
			connectedCount[pos]=totalCount;
			pos=next;
		}while(i>pos||j>pos);
		
		//Connect back to the top;
		connectedIndex[pos]=top;
		connectedCount[pos]=totalCount;
		
		return totalCount;
	}
	
	/**
	 * Disconnects a single element from its group.
	 * @param i index of the element to disconnect.
	 */
	public void disconnect(int i)
	{
		if(connectedCount[i]>1)
		{
			int newCount=connectedCount[i]-1;
			int top=connectedIndex[i];

			//Disconnect the element
			connectedIndex[i]=i;
			connectedCount[i]=1;

			//Adjust the count
			int next=top;
			for(;connectedIndex[next]!=i;next=connectedIndex[next])
			{
				connectedCount[next]=newCount;
			}

			//Reconnect the broken link
			connectedIndex[next]=top;
		}
	}

	/**
	 * Removes a group of connected elements from a sorted list.
	 * The first n positions in the list will be marked with -1 (where n is the number of elements in the connected group) to indicate that those elements have been removed.
	 * This is a fairly specialized function. It makes some assumptions:
	 *  - The list is sorted by connected group size.
	 *  - Unused positions appear at the beginning of the list, and set to -1.
	 * The intention is to use this in conjunction with insertConnectedElements to remove two or more groups, connect them, and reinsert.
	 * @param index an element of the group to insert.
	 * @param sortedList list to insert the group into.
	 * @return position of the first untouched element(list size if all elements were moved or removed), or -1 if the group could not be found (this should never happen).
	 * Elements preceding the returned position have been moved or removed.
	 */
	public int removeConnectedElements(int index, int [] sortedList)
	{
		int position=-1;
		int size=connectedCount[index];
		int depth=0;
		//Assuming size is not too big
		if(size<connectedIndex.length)
		{
			//skip past empty positions
			for(depth=0;depth<sortedList.length&&sortedList[depth]<0;depth++);
			int cc=connectedCount[sortedList[depth]];
			while(cc<size&&depth+size<connectedIndex.length)
			{
				depth+=cc;
				cc=connectedCount[sortedList[depth]];
			}
		}

		//look for the element
		while(position<0&&connectedCount[sortedList[depth]]==size)
		{
			for(int i=0;i<size&&position<0;i++)
			{
				if(sortedList[i+depth]==index)
				{
					//found it, assume that this connected group is correctly positioned in sortedList
					position=depth;
				}
			}
			depth+=size;
		}
		if(position<0)
		{
			//This should never happen
			throw new IndexOutOfBoundsException("Connected group for " + index + " was not located.");
		}
		else
		{
			//Move everything down
			for(int i=depth-1; i>=size; i--)
			{
				sortedList[i]=sortedList[i-size];
			}
			//Clear the empty space
			for(int i=0; i<size; i++)
			{
				sortedList[i]=-1;
			}
		}
		return depth;
	}

	/**
	 * Inserts a group of connected elements into a sorted list.
	 * The inserted elements will always be inserted as a contiguous block in the list, in the same order they appear in the connected set.
	 * This is a fairly specialized function. It makes some assumptions:
	 *  - The list is sorted by connected group size.
	 *  - The first n positions of the list are not used, where n is the number of elements to be inserted.
	 *  - Unused positions in the list are set to -1.
	 * The intention is to use this in conjunction with removeConnectedElements to remove two or more groups, connect them, and reinsert.
	 * @param index an element of the group to insert.
	 * @param sortedList list to insert the group into.
	 * @return position of the first inserted element in sortedList.
	 */
	public int insertConnectedElements(int index, int [] sortedList)
	{
		int size=connectedCount[index];
		int depth=0;
		//Skip past empty positions
		for(depth=size;depth<sortedList.length&&sortedList[depth]<0;depth++);
		//bump smaller sets up to make space
		int cc=0;
		if(depth<sortedList.length)cc=connectedCount[sortedList[depth]];
		while(cc<size&&depth<sortedList.length)
		{
			for(int j=0; j<cc; j++)
			{
				sortedList[depth+j-size]=sortedList[depth+j];
			}
			depth+=cc;
			if(depth<sortedList.length)cc=connectedCount[sortedList[depth]];
		}

		//Move the current set into the hole left by shifting smaller sets
		int i = index;
		for(int j=0; j<size; j++)
		{
			sortedList[depth+j-size]=i;
			i = connectedIndex[i];
		}
		
		return depth-size;
	}
	
	public boolean save(PrintStream out)
	{
		
		out.println("connected:count");
		for(int i=0;i<connectedIndex.length;i++)
		{
			out.println(i+":"+connectedIndex[i]+":"+connectedCount[i]);
		}
		return true;
	}
}
