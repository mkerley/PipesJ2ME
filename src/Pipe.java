import javax.microedition.lcdui.Graphics;

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

	/** Cached value of (size + 1) - this is used a lot, so we can avoid calculating it all the time */
	private static int sizePlusOne = size + 1;

	/** Cached value of (size / 2) - this is used a lot, so we can avoid calculating it all the time */
	private static int size_2 = size / 2;

	/** Cached value of (size / 3) - this is used a lot, so we can avoid calculating it all the time */
	private static int size_3 = size / 3;

	public Pipe()
	{

	}

	public void paint(Graphics g)
	{
		paint(g, inConnectedSet ? 0x00ff00 : 0xff0000);
	}

	public void paint(Graphics g, int color)
	{
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

	public static void setSize(int size)
	{
		Pipe.size = size;
		Pipe.sizePlusOne = size + 1;
		Pipe.size_2 = size / 2;
		Pipe.size_3 = size / 3;
	}

	public boolean isInConnectedSet()
	{
		return inConnectedSet;
	}

	public void setInConnectedSet(boolean inConnectedSet)
	{
		this.inConnectedSet = inConnectedSet;
	}

	public String toString()
	{
		return "Builder{" +
				"gridX=" + gridX +
				", gridY=" + gridY +
				'}';
	}
}
