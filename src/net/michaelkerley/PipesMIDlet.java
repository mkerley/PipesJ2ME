import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Display;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;
import javax.microedition.rms.RecordStoreException;

/**
 * DESCRIPTION GOES HERE.
 * @version \$Id$
 */
public class PipesMIDlet extends MIDlet
{
	public static final String[] aboutText =
			{
					"", // Version placeholder - this line will be replaced at runtime
					"",
					"Written by Kornhornio",
					"http://www.kornhornio.net",
					"",
					"Concept by Ernest Pazera",
					"http://www.playdeez.com"
			};
	
//#mdebug debug
//# 	private static StringBuffer logger = new StringBuffer();
//#enddebug

	public static final Font largeFont = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD, Font.SIZE_LARGE);
	public static final Font mediumFont = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD, Font.SIZE_MEDIUM);
	public static final Font smallFont = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_SMALL);

	private PipesCanvas pipesCanvas;
	private boolean firstStart = true;
	
	private static PipesMIDlet instance;

	public PipesMIDlet()
	{
		instance = this;

		pipesCanvas = new PipesCanvas(this);
	}

	protected void startApp() throws MIDletStateChangeException
	{
//#debug debug
//# 		PipesMIDlet.getInstance().log("startApp()");
		
//#mdebug info
//# 		PipesMIDlet.getInstance().log("Screen width:  " + pipesCanvas.getWidth());
//# 		PipesMIDlet.getInstance().log("Screen height: " + pipesCanvas.getHeight());
//#enddebug
		
		try
		{
			pipesCanvas.load();
		}
		catch (Exception e)
		{
			// Loading failed; oh well
			e.printStackTrace();

			pipesCanvas.init();
		}
		
		if (firstStart)
		{
			pipesCanvas.showAbout(2000);
			Display.getDisplay(this).setCurrent(pipesCanvas);
			firstStart = false;
		}
	}

	protected void pauseApp()
	{
//#debug debug
//# 		PipesMIDlet.log("pauseApp()");
		try
		{
			pipesCanvas.save();
		}
		catch (RecordStoreException e)
		{
			// Saving failed; oh well
			e.printStackTrace();
		}
	}

	protected void destroyApp(boolean b) throws MIDletStateChangeException
	{
//#debug debug
//# 		log("destroyApp()");
		try
		{
			pipesCanvas.save();
		}
		catch (RecordStoreException e)
		{
			// Saving failed; oh well
			e.printStackTrace();
		}
	}

	public void quit()
	{
		try
		{
			destroyApp(true);
			notifyDestroyed();
		}
		catch (MIDletStateChangeException e)
		{
			e.printStackTrace();
		}
	}

	public static PipesMIDlet getInstance()
	{
		return instance;
	}
	
//#mdebug debug
//# 	public static void log(String str)
//# 	{
//# 		System.out.println(str);
//# 		logger.append(str).append('\n');
//# 	}
//# 	
//# 	public static void clearLog()
//# 	{
//# 		logger = new StringBuffer();
//# 	}
//# 	
//# 	public static String getLogContents()
//# 	{
//# 		return logger.toString();
//# 	}
//#enddebug
}