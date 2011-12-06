package hulka.tilemanager;
/**
 * Stores the parameters required to describe a set of tiles.
 */
public class TileSetDescriptor implements Cloneable
{
	/**
	 * Display area width - initialized by {@link AbstractTileManagerImpl#AbstractTileManagerImpl(int,int,int,int)}.
	 */
	public int boardWidth;
	/**
	 * Display area height - initialized by {@link AbstractTileManagerImpl#AbstractTileManagerImpl(int,int,int,int)}.
	 */
	public int boardHeight;
	/**
	 * Column count - initialized by {@link AbstractTileManagerImpl#AbstractTileManagerImpl(int,int,int,int)}.
	 */
	public int tilesAcross;
	/**
	 * Row count - initialized by {@link AbstractTileManagerImpl#AbstractTileManagerImpl(int,int,int,int)}.
	 */
	public int tilesDown;
	/**
	 * Edge tile fitting - initialized by {@link AbstractTileManagerImpl#AbstractTileManagerImpl(int,int,int,int)}.
	 * If this is true, edge tiles will be adjusted to fit board dimensions.
	 */
	public boolean fitEdgeTiles;
	/**
	 * Offset of leftmost column from the origin - initialized by {@link AbstractTileManagerImpl#AbstractTileManagerImpl(int,int,int,int)}.
	 */
	public int leftOffset;
	/**
	 * Offset of the topmost row from the origin - initialized by {@link AbstractTileManagerImpl#AbstractTileManagerImpl(int,int,int,int)}.
	 */
	public int topOffset;
	/**
	 * Scale factor required to adjust tile height to the ideal ratio ({@link#heightWidthRatio}) - initialized by {@link AbstractTileManagerImpl#AbstractTileManagerImpl(int,int,int,int)}.
	 */
	public double scaleFactor;
	/**
	 * Tile width - to be initialized by {@link AbstractTileManagerImpl#getTileSetDescriptor()}.
	 */
	public int tileWidth;
	/**
	 * Tile height - to be initialized by {@link AbstractTileManagerImpl#getTileSetDescriptor()}.
	 */
	public int tileHeight;
	/**
	 * Horizontal space required by a tile - to be initialized by {@link AbstractTileManagerImpl#getTileSetDescriptor()}.
	 */
	public int tileSpacingX;
	/**
	 * Vertical space required by a tile - to be initialized by {@link AbstractTileManagerImpl#getTileSetDescriptor()}.
	 */
	public int tileSpacingY;
	/**
	 * Total number of tiles - to be initialized by {@link AbstractTileManagerImpl#getTileSetDescriptor()}.
	 */
	public int tileCount;
	/**
	 * The number of discrete rotations in 360 degrees - to be initialized by {@link AbstractTileManagerImpl#getTileSetDescriptor()}.
	 */
	public int rotationSteps;
	/**
	 * The number of sides to a tile - to be initialized by {@link AbstractTileManagerImpl#getTileSetDescriptor()}.
	 * If all tiles are oriented the same way, this should equal rotationSteps.
	 */
	public int sideCount;
	/**
	 * Ideal height:width ratio - to be initialized by {@link AbstractTileManagerImpl#getTileSetDescriptor()}.
	 * Scaled to this ratio, a uniform tile's sides should be equal in length.
	 */
	public double heightWidthRatio;
	/**
	 * Margin space needed for drawing - to be initialized by {@link AbstractTileManagerImpl#getTileSetDescriptor()}.
	 */
	public int tileMargin;
	
	public Object clone() throws CloneNotSupportedException
	{
		return super.clone();
	}
	
	public String toString()
	{
		return "\nboardWidth " + boardWidth
				+ "\nboardHeight " + boardHeight
				+ "\ntilesAcross " + tilesAcross
				+ "\ntilesDown  " + tilesDown
				+ "\nfitEdgeTiles " + fitEdgeTiles
				+ "\nleftOffset " + leftOffset
				+ "\ntopOffset " + topOffset
				+ "\nscaleFactor " + scaleFactor
				+ "\ntileWidth " + tileWidth
				+ "\ntileHeight " + tileHeight
				+ "\ntileSpacingX " + tileSpacingX
				+ "\ntileSpacingY " + tileSpacingY
				+ "\ntileCount " + tileCount
				+ "\nrotationSteps " + rotationSteps
				+ "\nsideCount " + sideCount
				+ "\nheightWidthRatio " + heightWidthRatio
				+ "\ntileMargin " + tileMargin;
	}
}
