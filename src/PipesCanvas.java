import javax.microedition.lcdui.*;
import javax.microedition.rms.InvalidRecordIDException;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import javax.microedition.rms.RecordStoreNotFoundException;
import java.io.IOException;
import java.util.*;

class PipesCanvas extends Canvas implements CommandListener
{
	private Pipe[][] pipes;
	private PipesMIDlet midlet;
	private int cursorX, cursorY;

	private Image offscreenGraphics;
	private Image imgYouWin;

	private Random random = new Random();
	private Stack connectedPipes;

	private int mode = MODE_GAME;

	/** For use in {@link #checkConnections()} */
	private Stack toBeChecked;

	private Command rotate, quit, reset, resize, ok, about, help;

	private int rows = 8;
	private int cols = 8;

	private static final int MODE_GAME = 0;
	private static final int MODE_YOU_WIN = 1;
	private static final int MODE_RESIZE = 2;
	private static final int MODE_ABOUT = 3;
    private static final int MODE_GAME_OVER = 4;

	private static final String STORE_NAME = "PipesStore";
	private static final int STORE_SIZE_RECORD = 1;
	private static final int STORE_PIPES_RECORD = 2;

	private static final String MESSAGE_GAME_OVER = "You Win!";
	private static final Font FONT_GAME_OVER = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD | Font.STYLE_ITALIC, Font.SIZE_LARGE);
	private static final int COLOR_GAME_OVER = 0xff0000;
	private static final int COLOR_GAME_OVER_SHADOW = 0x666666;
	
	private static final long DISPLAY_YOU_WIN_MILLISECONDS = 2000;
	
	private static final String HELP_ALERT_TITLE = "Pipes Help...";
	private static final String HELP_ALERT_TEXT =
			"Rules:\n" +
			" * To win the game, you must connect all the pipe sections. Connected pipes will turn green.\n" +
			" * There are no impossible puzzles; every game can be solved.\n" +
			" * When the puzzle is solved, there will be no dangling sections of pipe. For example, if there is a straight piece along the bottom edge of the board, it MUST be turned so that it runs left-to-right.\n" +
			"\n" +
			"Controls:\n" +
			" * Joystick - Move cursor\n" +
			" * OK/center button - Rotate clockwise\n" +
			"\n" +
			"Alternate controls:\n" +
			" * 2/4/6/8 - Move cursor\n" +
			" * 3/5 - Rotate clockwise\n" +
			" * 1 - Rotate counter-clockwise";
	
	public static final int RESIZE_TEXT_COLOR = 0xffffff;
	public static final int RESIZE_GRIDLINE_COLOR = RESIZE_TEXT_COLOR;
	public static final int CURSOR_COLOR = 0xffff00;
	public static final int GRIDLINE_COLOR = 0x646464;
	public static final int DIM_GRIDLINE_COLOR = 0x323232;

	public PipesCanvas(PipesMIDlet midlet)
	{
		this.midlet = midlet;

		offscreenGraphics = Image.createImage(getWidth(), getHeight());

		// Game mode commands
		rotate = new Command("Rotate", Command.SCREEN, 1);
		reset = new Command("Reset", Command.SCREEN, 2);
		resize = new Command("Resize", Command.SCREEN, 3);
		
		help = new Command("Help...", Command.SCREEN, 4);
		about = new Command("About...", Command.SCREEN, 5);
		
		quit = new Command("Quit", Command.SCREEN, 6);

		// Resize mode commands
		ok = new Command("OK", Command.OK, 1);

		setCommandListener(this);

		imgYouWin = loadImage(new String[] {"/you_win_large.png", "/you_win_medium.png", "/you_win_small.png"}, getWidth(), getHeight());
	}

	/**
	 * Loads an image from a set of filenames to fit within a given dimension.  The filenames should be in descending
	 * order based on image size.  For example, an appropriate list of filenames would be
	 * <code>{"large.png", "medium.png", "small.png"}</code>.  The corresponding images will be loaded sequentially
	 *  until one is found that fits the required dimensions.
	 * @param filenames filenames to be tried
	 * @param maxWidth maximum width
	 * @param maxHeight maximum height
	 * @return the image that fit the dimensions (or <code>null</code> if none fit or if there were problems
	 * loading the image files)
	 */
	private static Image loadImage(String[] filenames, int maxWidth, int maxHeight)
	{
		if (filenames == null || filenames.length == 0)
		{
			// Garbage in, garbage out
			return null;
		}

		Image img;
		for (int i = 0; i < filenames.length; ++i)
		{
			// Nullify any existing image so it can be cleaned up
			img = null;

			// Load the new image
			try
			{
				img = Image.createImage(filenames[i]);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}

			// Check if loading was successful, and whether the dimensions fit
			if (img != null && img.getWidth() <= maxWidth && img.getHeight() <= maxHeight)
			{
				return img;
			}
		}

		// None of the images worked
		return null;
	}

	public void init()
	{
		initPipeSize();

		buildPipes();
		scramblePipes();
		checkConnections();

		setMode(MODE_GAME);
	}

	private void assertValidCursor()
	{
		if (cursorX < 0)
		{
			cursorX = 0;
		}
		if (cursorX >= cols)
		{
			cursorX = cols - 1;
		}
		if (cursorY < 0)
		{
			cursorY = 0;
		}
		if (cursorY >= rows)
		{
			cursorY = rows - 1;
		}
	}

	private void initPipeSize()
	{
		int sizeX = (getWidth() - 1) / cols;
		int sizeY = (getHeight() - 1) / rows;

		while (sizeX % 3 != 1)
		{
			--sizeX;
		}
		while (sizeY % 3 != 1)
		{
			--sizeY;
		}

		int size = Math.min(sizeX, sizeY);
		Pipe.setSize(size - 1);
	}

	private void initPipes()
	{
		Pipe b;
		pipes = new Pipe[cols][rows];

		for (int x = 0; x < cols; ++x)
		{
			for (int y = 0; y < rows; ++y)
			{
				//noinspection ObjectAllocationInLoop
				b = pipes[x][y] = new Pipe();
				b.setGridX(x);
				b.setGridY(y);
			}
		}

		connectedPipes = new Stack();
		toBeChecked = new Stack();
	}

	protected void paint(Graphics g)
	{
		Graphics offscreen = offscreenGraphics.getGraphics();
		paintPipes(offscreen);

		if (mode == MODE_ABOUT)
		{
			paintAbout(offscreen);
		}
		else if (mode == MODE_YOU_WIN)
		{
			if (imgYouWin != null)
			{
				offscreen.drawImage(imgYouWin, getWidth() / 2, getHeight() / 2, Graphics.HCENTER | Graphics.VCENTER);
			}
			else
			{
				// Draw some game over message
				int x = (getWidth() - FONT_GAME_OVER.stringWidth(MESSAGE_GAME_OVER)) / 2;
				int y = (getHeight() - FONT_GAME_OVER.getHeight()) / 2;

				offscreen.setFont(FONT_GAME_OVER);
				offscreen.setColor(COLOR_GAME_OVER_SHADOW);
				offscreen.drawString(MESSAGE_GAME_OVER, x + 1, y + 1, Graphics.TOP | Graphics.LEFT);
				offscreen.setColor(COLOR_GAME_OVER);
				offscreen.drawString(MESSAGE_GAME_OVER, x - 1, y - 1, Graphics.TOP | Graphics.LEFT);
			}
		}
		g.drawImage(offscreenGraphics, 0, 0, Graphics.LEFT | Graphics.TOP);
	}

	private void paintPipes(Graphics g)
	{
		// Black background
		g.setColor(0);
		g.fillRect(0, 0, getWidth(), getHeight());

		// Calculate offset so that grid will be centered on the screen
		int gridWidth = cols * (Pipe.getSize() + 1) + 1;
		int gridHeight = rows * (Pipe.getSize() + 1) + 1;
		int xOffset = (getWidth() - gridWidth) / 2;
		int yOffset = (getHeight() - gridHeight) / 2;

		g.translate(xOffset, yOffset);

		if (mode != MODE_RESIZE)
		{
			// Pipes
			for (int x = 0; x < cols; ++x)
			{
				for (int y = 0; y < rows; ++y)
				{
					pipes[x][y].paint(g, mode != MODE_ABOUT);
				}
			}

			// Gray grid
			if (mode != MODE_ABOUT)
				g.setColor(GRIDLINE_COLOR);
			else
				g.setColor(DIM_GRIDLINE_COLOR);
		}
		else
		{
			// White grid
			g.setColor(RESIZE_GRIDLINE_COLOR);
		}

		// Horizontal grid lines
		for (int i = 0; i <= rows * (Pipe.getSize() + 1); i += Pipe.getSize() + 1)
		{
			g.drawLine(0, i, cols * (Pipe.getSize() + 1), i);
		}

		// Vertical grid lines
		for (int i = 0; i <= cols * (Pipe.getSize() + 1); i += Pipe.getSize() + 1)
		{
			g.drawLine(i, 0, i, rows * (Pipe.getSize() + 1));
		}

		if (mode == MODE_GAME)
		{
			// Draw cursor
			g.setColor(CURSOR_COLOR); // yellow
			g.drawRect(
					cursorX * (Pipe.getSize() + 1),
					cursorY * (Pipe.getSize() + 1),
					Pipe.getSize() + 1,
					Pipe.getSize() + 1);
		}

		g.translate(-xOffset, -yOffset);

		if (mode == MODE_RESIZE)
		{
			// Show the grid size
			String msg = cols + " x " + rows;
			Font font = Font.getDefaultFont();
			g.setFont(font);
			int msgWidth = font.stringWidth(msg);
			int msgHeight = font.getHeight();
			int msgXOffset = (getWidth() - msgWidth) / 2;
			int msgYOffset = (getHeight() - msgHeight) / 2;

			g.setColor(0); // Black background
			g.fillRect(msgXOffset - 2, msgYOffset - 2, msgWidth + 4, msgHeight + 4);
			g.setColor(RESIZE_TEXT_COLOR); // White text
			g.drawString(msg, msgXOffset, msgYOffset, Graphics.LEFT | Graphics.TOP);
		}
	}

	private void paintAbout(Graphics g)
	{
		int y = 0;
		String line;

		// White text
		g.setColor(0xffffff);

		for (int i = 0; i < PipesMIDlet.aboutText.length; ++i)
		{
			if (i == 0)
			{
				line = "Pipes " + PipesMIDlet.getInstance().getAppProperty("MIDlet-Version");
//#debug debug
//# 				line += "d"; // Add a debug marker to the version (ie. 1.0.10d)
			}
			else
				line = PipesMIDlet.aboutText[i];

			if (PipesMIDlet.largeFont.stringWidth(line) < getWidth())
				g.setFont(PipesMIDlet.largeFont);
			else if (PipesMIDlet.mediumFont.stringWidth(line) < getWidth())
				g.setFont(PipesMIDlet.mediumFont);
			else
				// Hope this works
				g.setFont(PipesMIDlet.smallFont);

			g.drawString(line, getWidth() / 2, y, Graphics.TOP | Graphics.HCENTER);
			y += g.getFont().getHeight();
		}
	}

	protected void keyRepeated(int i)
	{
		keyPressed(i);
	}

	protected void keyPressed(int i)
	{
		switch (mode)
		{
		case MODE_GAME:
			keyPressedGame(i);
			break;
		case MODE_YOU_WIN:
			keyPressedYouWin(i);
			break;
		case MODE_GAME_OVER:
			keyPressedGameOver(i);
			break;
		case MODE_RESIZE:
			keyPressedResize(i);
			break;
		case MODE_ABOUT:
			setMode(MODE_GAME);
			repaint();
			break;
		}
	}

	private void keyPressedGame(int i)
	{
		switch (getGameAction(i))
		{
		case UP:
			--cursorY;
			if (cursorY < 0)
			{
				cursorY = rows - 1;
			}
			repaint();
			break;
		case LEFT:
			--cursorX;
			if (cursorX < 0)
			{
				cursorX = cols - 1;
			}
			repaint();
			break;
		case DOWN:
			++cursorY;
			if (cursorY >= rows)
			{
				cursorY = 0;
			}
			repaint();
			break;
		case RIGHT:
			++cursorX;
			if (cursorX >= cols)
			{
				cursorX = 0;
			}
			repaint();
			break;
		case FIRE:
			pipes[cursorX][cursorY].rotate(true);
			checkConnections();
			repaint();
			break;
		default:
			//noinspection NestedSwitchStatement
			switch (i)
			{
			case KEY_NUM1:
				pipes[cursorX][cursorY].rotate(false);
				checkConnections();
				repaint();
				break;
			case KEY_NUM3:
				pipes[cursorX][cursorY].rotate(true);
				checkConnections();
				repaint();
				break;
			} // switch (i)
		} // switch (getGameAction(i))
	}
	
	private void keyPressedYouWin(int i)
	{
		// Duplicate behavior
		keyPressedGameOver(i);
	}

	private void keyPressedGameOver(int i)
	{
		commandAction(reset, this);
	}

	private void keyPressedResize(int i)
	{
		switch (getGameAction(i))
		{
		case UP:
			if (rows > 2)
			{
				--rows;
				initPipeSize();
				repaint();
			}
			break;
		case LEFT:
			if (cols > 2)
			{
				--cols;
				initPipeSize();
				repaint();
			}
			break;
		case DOWN:
			++rows;
			initPipeSize();
			if (Pipe.getSize() < 3)
			{
				--rows;
				initPipeSize();
			}
			repaint();
			break;
		case RIGHT:
			++cols;
			initPipeSize();
			if (Pipe.getSize() < 3)
			{
				--cols;
				initPipeSize();
			}
			repaint();
			break;
		case FIRE:
			commandAction(ok, this);
			break;
		} // switch (getGameAction(i))
	}

	private Pipe getPipe(int x, int y)
	{
		if (x < 0 || x >= cols || y < 0 || y >= rows)
		{
			return null;
		}
		else
		{
			return pipes[x][y];
		}
	}

	private void buildPipes()
	{
		initPipes();

		// Start with a random pipe segment
		Vector connected = new Vector(rows * cols);
		connected.addElement(pipes[Math.abs(random.nextInt()) % cols][Math.abs(random.nextInt()) % rows]);

		// Loop while there are some unconnected pipes
		Pipe p, p2 = null;
		int direction, reverseDirection = 0;
		while (connected.size() < connected.capacity())
		{
			p = (Pipe) connected.elementAt(Math.abs(random.nextInt()) % connected.size());
			direction = 1 << Math.abs(random.nextInt()) % 4;

			switch (direction)
			{
			case Pipe.CONN_UP:
				p2 = getPipe(p.getGridX(), p.getGridY() - 1);
				reverseDirection = Pipe.CONN_DOWN;
				break;
			case Pipe.CONN_DOWN:
				p2 = getPipe(p.getGridX(), p.getGridY() + 1);
				reverseDirection = Pipe.CONN_UP;
				break;
			case Pipe.CONN_LEFT:
				p2 = getPipe(p.getGridX() - 1, p.getGridY());
				reverseDirection = Pipe.CONN_RIGHT;
				break;
			case Pipe.CONN_RIGHT:
				p2 = getPipe(p.getGridX() + 1, p.getGridY());
				reverseDirection = Pipe.CONN_LEFT;
				break;
			}

			if (p2 != null && p2.getConnections() == 0)
			{
				p.setConnected(direction, true);
				p2.setConnected(reverseDirection, true);
				connected.addElement(p2);
				repaint();
			}
		}
	}

	private void scramblePipes()
	{
		Pipe p;
		int rotations;

		for (int x = 0; x < cols; ++x)
		{
			for (int y = 0; y < rows; ++y)
			{
				p = pipes[x][y];
				rotations = Math.abs(random.nextInt()) % 20;
				for (int i = 0; i < rotations; ++i)
				{
					p.rotate(true);
				}
			}
		}
	}

	public void commandAction(Command command, Displayable displayable)
	{
		if (command == rotate)
		{
			pipes[cursorX][cursorY].rotate(true);
			checkConnections();
			repaint();
		}
		else if (command == reset)
		{
			init();
		}
		else if (command == quit)
		{
			midlet.quit();
		}
		else if (command == resize)
		{
			setMode(MODE_RESIZE);
			repaint();
		}
		else if (command == ok)
		{
			setMode(MODE_GAME);
			repaint();
		}
		else if (command == about)
		{
			setMode(MODE_ABOUT);
			repaint();
		}
		else if (command == help)
		{
			Alert alert = new Alert(HELP_ALERT_TITLE, HELP_ALERT_TEXT, null, null);
			alert.setTimeout(Alert.FOREVER);
			Display.getDisplay(PipesMIDlet.getInstance()).setCurrent(alert);
		}
	}

	public void checkConnections()
	{
		Pipe p, p2;
		int x, y;

		// Set all pipes to be disconnected
		while (connectedPipes.size() > 0)
		{
			((Pipe) connectedPipes.pop()).setInConnectedSet(false);
//			connectedPipes.removeElementAt(0);
		}

		p = pipes[cols / 2][rows / 2];
		connectedPipes.addElement(p);
		toBeChecked.addElement(p);
		p.setInConnectedSet(true);

		while (!toBeChecked.empty())
		{
			p = (Pipe) toBeChecked.pop();
			x = p.getGridX();
			y = p.getGridY();

			// Check which surrounding pipes are connected
			if (p.isConnected(Pipe.CONN_UP))
			{
				p2 = getPipe(x, y - 1);
				if (p2 != null && p2.isConnected(Pipe.CONN_DOWN) && !p2.isInConnectedSet())
				{
					connectedPipes.addElement(p2);
					toBeChecked.addElement(p2);
					p2.setInConnectedSet(true);
				}
			}

			if (p.isConnected(Pipe.CONN_DOWN))
			{
				p2 = getPipe(x, y + 1);
				if (p2 != null && p2.isConnected(Pipe.CONN_UP) && !p2.isInConnectedSet())
				{
					connectedPipes.addElement(p2);
					toBeChecked.addElement(p2);
					p2.setInConnectedSet(true);
				}
			}

			if (p.isConnected(Pipe.CONN_LEFT))
			{
				p2 = getPipe(x - 1, y);
				if (p2 != null && p2.isConnected(Pipe.CONN_RIGHT) && !p2.isInConnectedSet())
				{
					connectedPipes.addElement(p2);
					toBeChecked.addElement(p2);
					p2.setInConnectedSet(true);
				}
			}

			if (p.isConnected(Pipe.CONN_RIGHT))
			{
				p2 = getPipe(x + 1, y);
				if (p2 != null && p2.isConnected(Pipe.CONN_LEFT) && !p2.isInConnectedSet())
				{
					connectedPipes.addElement(p2);
					toBeChecked.addElement(p2);
					p2.setInConnectedSet(true);
				}
			}
		}

		if (connectedPipes.size() == (rows * cols))
		{
			// Winner!
			setMode(MODE_YOU_WIN);
		}
	}

	public int getMode()
	{
		return mode;
	}

	public void setMode(int mode)
	{
		int oldMode = this.mode;
		this.mode = mode;
		
		// First remove all commands
		removeCommand(ok);
		removeCommand(rotate);
		removeCommand(reset);
		removeCommand(resize);
		removeCommand(quit);
		removeCommand(about);
		removeCommand(help);

		switch (mode)
		{
		case MODE_GAME:
			addCommand(rotate);
			addCommand(reset);
			addCommand(resize);
			addCommand(help);
			addCommand(about);
			addCommand(quit);
			assertValidCursor();

			if (oldMode != MODE_ABOUT && oldMode != MODE_GAME)
			{
				init();
			}

			checkConnections();
			break;
		case MODE_YOU_WIN:
			new Timer().schedule(new HideYouWinTask(), DISPLAY_YOU_WIN_MILLISECONDS);
			break;
		case MODE_GAME_OVER:
			// No commands to show
			break;
		case MODE_RESIZE:
			addCommand(ok);
			break;
		case MODE_ABOUT:
			addCommand(ok);
			break;
		}
	}

	public void save() throws RecordStoreException
	{
		RecordStore rs = null;
		try
		{
			rs = RecordStore.openRecordStore(STORE_NAME, true);

			// Save stuff
			byte[] size = new byte[2];
			size[0] = (byte)cols;
			size[1] = (byte)rows;

			try
			{
				rs.setRecord(STORE_SIZE_RECORD, size, 0, size.length);
			}
			catch (InvalidRecordIDException e)
			{
				// This is the first time saving - we have to add the record first to create a space for it
				rs.addRecord(size, 0, size.length);
			}

			byte[] pipesBytes = new byte[rows * cols];
			int i = 0;

			for (int y = 0; y < rows; ++y)
			{
				for (int x = 0; x < cols; ++x)
				{
					pipesBytes[i] = pipes[x][y].getConnections();
					++i;
				}
			}

			try
			{
				rs.setRecord(STORE_PIPES_RECORD, pipesBytes, 0, pipesBytes.length);
			}
			catch (InvalidRecordIDException e)
			{
				// This is the first time saving
				rs.addRecord(pipesBytes, 0, pipesBytes.length);
			}

			rs.closeRecordStore();
		}
		catch (Throwable t)
		{
			// Saved data is probably corrupt; delete it
			try
			{
				rs.closeRecordStore();
			}
			catch (Throwable t2)
			{
				t2.printStackTrace();
			}

			try
			{
				RecordStore.deleteRecordStore(STORE_NAME);
			}
			catch (Throwable t2)
			{
				t2.printStackTrace();
			}
		}
	}

	public void load() throws RecordStoreException
	{
		RecordStore rs;
		boolean success = true;

		try
		{
			rs = RecordStore.openRecordStore(STORE_NAME, false);

			try
			{
				// Load the board size
				byte[] size = rs.getRecord(STORE_SIZE_RECORD);
				cols = (int) size[0];
				rows = (int) size[1];

				if (cols > 0 && rows > 0)
				{
					initPipeSize();

					// Load the board contents
					try
					{
						byte[] pipesBytes = rs.getRecord(STORE_PIPES_RECORD);

						initPipes();

						int i = 0;
						for (int y = 0; y < rows; ++y)
						{
							for (int x = 0; x < cols; ++x)
							{
								pipes[x][y].setConnections(pipesBytes[i]);
								++i;
//								disconnected.addElement(pipes[x][y]);

								if (pipes[x][y].getConnections() == 0)
								{
									success = false;
								}
							}
						}

						if (success)
						{
							checkConnections();
						}
					}
					catch (InvalidRecordIDException e)
					{
						// Oh well, guess we won't be loading any data
						success = false;
					}
				}
				else
				{
					success = false;
				}
			}
			catch (Exception e)
			{
				try
				{
					rs.closeRecordStore();
				}
				catch (Throwable t)
				{
					t.printStackTrace();
				}

				try
				{
					RecordStore.deleteRecordStore(STORE_NAME);
				}
				catch (Throwable t)
				{
					t.printStackTrace();
				}
			}
			finally
			{
				try
				{
					rs.closeRecordStore();
				}
				catch (Throwable t)
				{
					t.printStackTrace();
				}
			}
		}
		catch (RecordStoreNotFoundException e)
		{
			// No big deal; we just won't load anything
			success = false;
		}

		if (!success)
		{
			cols = rows = 8;
			init();
		}
	}

	public void showAbout(long time)
	{
		setMode(MODE_ABOUT);
		repaint();
		new Timer().schedule(new CloseAboutTask(), time);
	}

	private class CloseAboutTask extends TimerTask
	{
		public void run()
		{
			if (mode == MODE_ABOUT)
			{
				setMode(MODE_GAME);
				repaint();
			}
		}
	}
	
	private class HideYouWinTask extends TimerTask
	{
		public void run()
		{
			if (mode == MODE_YOU_WIN)
			{
				setMode(MODE_GAME_OVER);
				repaint();
			}
		}
	}
}