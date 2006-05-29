import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

/**
 * DESCRIPTION GOES HERE.
 * @version \$Id$
 */
public class Pipe
{
	private int x, y;
	private int gridX, gridY;
	private byte connections = 0;
	private boolean inConnectedSet = false;

	public static final int CONN_UP = 1;
	public static final int CONN_RIGHT = 2;
	public static final int CONN_DOWN = 4;
	public static final int CONN_LEFT = 8;
	private static final int CONN_OVERFLOW = 16;

	private static int size = 9;
	
	private static final int[] bitmapSizes = { 42, 21, 15, 9 }; // Descending order
	private static Image[] disconnectedBmps = null;
	private static Image[] connectedBmps = null;
	private static boolean bmpsLoaded = false;
	private static final int NUM_BMPS = 16;

	/** Cached value of (size + 1) - this is used a lot, so we can avoid calculating it all the time */
	private static int sizePlusOne = size + 1;

	/** Cached value of (size / 2) - this is used a lot, so we can avoid calculating it all the time */
	private static int size_2 = size / 2;

	/** Cached value of (size / 3) - this is used a lot, so we can avoid calculating it all the time */
	private static int size_3 = size / 3;
	public static final int CONNECTED_COLOR = 0x00ff00;
	public static final int DISCONNECTED_COLOR = 0xff0000;
	public static final int DIM_CONNECTED_COLOR = 0x006600;
	public static final int DIM_DISCONNECTED_COLOR = 0x660000;

	public Pipe()
	{

	}

	public void paint(Graphics g, boolean bright)
	{
		if (bright)
			paint(g, inConnectedSet ? CONNECTED_COLOR : DISCONNECTED_COLOR, true);
		else
			paint(g, inConnectedSet ? DIM_CONNECTED_COLOR : DIM_DISCONNECTED_COLOR, false);
	}
	
	public void paint(Graphics g, int color, boolean preferBitmap)
	{
		if (preferBitmap && bmpsLoaded)
		{
			// Try to use a bitmap
			if (color == CONNECTED_COLOR)
				g.drawImage(connectedBmps[connections], x, y, Graphics.TOP | Graphics.LEFT);
			else
				g.drawImage(disconnectedBmps[connections], x, y, Graphics.TOP | Graphics.LEFT);
			
			return;
		}
		
		// Not using a bitmap - draw it the old-fashioned way
		g.setColor(0x000000); // black background
		g.fillRect(x, y, size, size);

		g.setColor(color);
		g.fillArc(x + size_3, y + size_3, size_3, size_3, 0, 360); // center ball

		if (isConnected(CONN_UP))
		{
			g.fillRect(x + size_3, y, size_3, size_2);
		}
		if (isConnected(CONN_DOWN))
		{
			g.fillRect(x + size_3, y + size_2, size_3, size_2 + 1);
		}
		if (isConnected(CONN_RIGHT))
		{
			g.fillRect(x + size_2, y + size_3, size_2 + 1, size_3);
		}
		if (isConnected(CONN_LEFT))
		{
			g.fillRect(x, y + size_3, size_2, size_3);
		}
	}

	public void setConnected(int dir, boolean connected)
	{
		if (connected)
		{
			connections |= dir;
		}
		else
		{
			connections &= (~dir);
		}
	}

	public void rotate(boolean clockwise)
	{
		if (clockwise)
		{
			connections <<= 1;
			// Handle bit overshifting
			if (isConnected(CONN_OVERFLOW))
			{
				setConnected(CONN_OVERFLOW, false);
				setConnected(CONN_UP, true);
			}
		}
		else
		{
			// Handle bit overshifting
			if (isConnected(CONN_UP))
			{
				setConnected(CONN_OVERFLOW, true);
			}
			connections >>= 1;
		}
	}
	
	private static int getBestBitmapSize(int preferredSize)
	{
		for (int i = 0; i < bitmapSizes.length; ++i)
		{
			if (bitmapSizes[i] <= preferredSize)
			{
				return bitmapSizes[i];
			}
		}
		
		// Couldn't find a good size
		return 0;
	}
	
	/**
	 * Loads bitmaps at the appropriate size.
	 * @return Actual size loaded (0 if loading failed, or no appropriate size
	 *         was available)
	 */
	public static int loadBitmaps()
	{
		//#debug debug
//# 		PipesMIDlet.log("loadBitmaps()");
		
		if (size > 0)
		{
			// Load this size
			try
			{
				Image allPipes = Image.createImage("/" + size + ".png");

				connectedBmps = new Image[NUM_BMPS];
				disconnectedBmps = new Image[NUM_BMPS];

				for (int n = 0; n < NUM_BMPS; ++n)
				{
					disconnectedBmps[n] = extractImage(allPipes, n * size, 0, size, size);
					connectedBmps[n] = extractImage(allPipes, n * size, size, size, size);
				}

				bmpsLoaded = true;
				return size;
			}
			catch (Exception e)
			{
				// Error reading image file
				e.printStackTrace();
				bmpsLoaded = false;
				return 0;
			}
		}
		
		// Didn't find a suitable size, or something failed
		bmpsLoaded = false;
		return 0;
	}
	
	private static Image extractImage(Image source, int xOffset, int yOffset, int width, int height)
	{
		Image img = Image.createImage(width, height);
		img.getGraphics().drawImage(source, -xOffset, -yOffset, Graphics.TOP | Graphics.LEFT);
		return img;
	}

	public boolean isConnected(int dir)
	{
		return (connections & dir) == dir;
	}

	public int getX()
	{
		return x;
	}

	public void setX(int x)
	{
		this.x = x;
	}

	public int getY()
	{
		return y;
	}

	public void setY(int y)
	{
		this.y = y;
	}

	public int getGridX()
	{
		return gridX;
	}

	public void setGridX(int gridX)
	{
		this.gridX = gridX;
		setX(gridX * sizePlusOne + 1);
	}

	public int getGridY()
	{
		return gridY;
	}

	public void setGridY(int gridY)
	{
		this.gridY = gridY;
		setY(gridY * sizePlusOne + 1);
	}

	public byte getConnections()
	{
		return connections;
	}

	public void setConnections(byte connections)
	{
		this.connections = connections;
	}

	public static int getSize()
	{
		return size;
	}

	public static int setSize(int preferredSize)
	{
		Pipe.size = preferredSize;
		
		// Downsize to an appropriate bitmap size
		int bestSize = getBestBitmapSize(preferredSize);
//#debug debug
//# 		PipesMIDlet.log("Best size: " + bestSize);
		if (bestSize > 0)
			Pipe.size = bestSize;
		
		Pipe.sizePlusOne = Pipe.size + 1;
		Pipe.size_2 = Pipe.size / 2;
		Pipe.size_3 = Pipe.size / 3;
		
		return Pipe.size;
	}

	public boolean isInConnectedSet()
	{
		return inConnectedSet;
	}

	public void setInConnectedSet(boolean inConnectedSet)
	{
		this.inConnectedSet = inConnectedSet;
	}
}
