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
	private PipesCanvas canvas;

	public PipesMIDlet()
	{
		canvas = new PipesCanvas(this);
	}

	protected void startApp() throws MIDletStateChangeException
	{
		try
		{
			canvas.load();
		}
		catch (Exception e)
		{
			// Loading failed; oh well
			e.printStackTrace();

			canvas.init();
		}

		Display.getDisplay(this).setCurrent(canvas);
	}

	protected void pauseApp()
	{
		try
		{
			canvas.save();
		}
		catch (RecordStoreException e)
		{
			// Saving failed; oh well
			e.printStackTrace();
		}
	}

	protected void destroyApp(boolean b) throws MIDletStateChangeException
	{
		try
		{
			canvas.save();
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
}