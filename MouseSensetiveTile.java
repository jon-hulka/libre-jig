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

import hulka.event.MouseSensetiveShape;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.Point;
import java.awt.Rectangle;
import hulka.tilemanager.TileManager;

public class MouseSensetiveTile extends MouseSensetiveShape
{
	private TileManager tileManager;
	private int tileMargin, errMargin;
	private int tileIndex;
	public MouseSensetiveTile(TileManager manager, int tileIndex, int x, int y, int zOrder, int errMargin)
	{
		super(x,y,0,0,tileIndex,zOrder);
		width = manager.getTileWidth();
		height = manager.getTileHeight();
		tileMargin = manager.getTileMargin();
		this.errMargin = errMargin;
		width=(width>height?width:height) + manager.getTileMargin()*2;
		height = width;
		this.tileIndex = tileIndex;
		this.tileManager = manager;
	}

	private MouseSensetiveTile(int x, int y, int width, int height, int index, int zOrder)
	{
		super(x,y,width,height,index,zOrder);
	}
	
	public void setZOrder(int zOrder)
	{
		this.zOrder = zOrder;
	}
	
	public int getTileIndex()
	{
		return tileIndex;
	}
	
	public void moveTo(int x, int y)
	{
		this.x = x;
		this.y = y;
	}
	
	public boolean containsPoint(int x, int y)
	{
		return getShape().contains(x,y);
	}
	
	public Shape getShape()
	{
		AffineTransform transform = tileManager.getRotationTransform(tileIndex,AffineTransform.getTranslateInstance(this.x + tileMargin + errMargin,this.y + tileMargin + errMargin),0);
		return transform.createTransformedShape(tileManager.getTileMask(tileIndex));
	}
	
	public Rectangle getBounds()
	{
		Rectangle bounds = getShape().getBounds();
		bounds.x -= errMargin;
		bounds.y -= errMargin;
		bounds.width += errMargin*2;
		bounds.height += errMargin*2;
		return bounds;
	}
}
