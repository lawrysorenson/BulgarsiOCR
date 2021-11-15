/* Lawry Sorenson
 * Computer Science III Honors
 * Final Project - Game
 * 12 May 2017
 */

import javax.swing.*;
import java.awt.event.*;
import java.awt.image.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import javax.imageio.*;
import java.util.*;

/**
 * The Labeler object serves at the GUI interface for the game
 */
@SuppressWarnings("serial")
public class Labeler extends JFrame implements KeyListener
{
	private long lastTick;
	private static final int TICK_FREQUENCY = 1000 / 32;

	private static int width, height;

	private ArrayList<Integer> pageNumbers;
	private ArrayList<ArrayList<ArrayList<int[]>>> boxes;
	private ArrayList<ArrayList<char[]>> text;

	private int ipage, iline, ichar;
	private BufferedImage pageImage;
	
	final int MARGIN = 20;

	private TreeMap<Character, ArrayList<BufferedImage>> charExamples;
	
	/**
	 * Creates a new Labeler object
	 */
	public Labeler()
	{
		lastTick = System.currentTimeMillis();

		charExamples = new TreeMap<>();

		try
		{
			Scanner in = new Scanner(new File("seg.txt"));
			Scanner textFile = new Scanner(new File("text.txt"), "UTF8");

			if (textFile.hasNextInt())
			{
				ipage = textFile.nextInt();
				iline = textFile.nextInt();
				ichar = textFile.nextInt();
				textFile.nextLine();
			}
			else
			{
				ipage = iline = 0;
				ichar = 1;
			}

			pageNumbers = new ArrayList<>();
			boxes = new ArrayList<>();
			text = new ArrayList<>();

			while (in.hasNextInt())
			{
				int pageNumber = in.nextInt();
				System.out.print(pageNumber + "\r");
				in.nextLine();
				pageNumbers.add(pageNumber);

				BufferedImage thisPage = getPageImage(pageNumber);

				ArrayList<ArrayList<int[]>> lines = new ArrayList<>();
				ArrayList<char[]> lineText = new ArrayList<>();

				while (true)
				{
					int chars = in.nextInt();
					if (chars==0) break;

					ArrayList<int[]> charList = new ArrayList<>();

					for (int i=0;i<=chars;++i)
					{
						charList.add(new int[]{in.nextInt(), in.nextInt(), in.nextInt(), in.nextInt()});
						in.nextLine();
					}

					lines.add(charList);

					char[] lineChars = (textFile.hasNextLine() ? textFile.nextLine() : "").toCharArray();
					char[] fullChars = new char[chars];
					for (int i=0;i<chars;++i)
					{
						fullChars[i] = i < lineChars.length ? lineChars[i] : ' ';
					}
					lineText.add(fullChars);

					for (int i=0;i<lineChars.length;++i)
					{
						if (lineChars[i] != ' ')
						{
							addCharExample(thisPage, charList.get(i+1), lineChars[i]);
						}
					}
				}

				boxes.add(lines);
				text.add(lineText);

				//for (var page : boxes) for (var line : page) for (var box : line) System.out.println(Arrays.toString(box));
			}

			in.close();
		}
		catch (IOException e)
		{

		}

		LoadPageImage();
		
		setExtendedState(JFrame.MAXIMIZED_BOTH);
		setUndecorated(true);
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addKeyListener(this);
		setFocusTraversalKeysEnabled(false);
		getContentPane().setCursor(Toolkit.getDefaultToolkit().createCustomCursor(new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB), new Point(0, 0), "blank"));
		
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		width = screen.width;
		height = screen.height;
		
		setVisible(true);
	}

	private void addCharExample(BufferedImage fullPage, int[] box, char c)
	{
		ArrayList<BufferedImage> examples = charExamples.get(c);
		if (examples == null)
		{
			examples = new ArrayList<>();
			charExamples.put(c, examples);
		}
		else if (examples.size() >= 200) return; //only store 20 examples

		BufferedImage image = extractBoxFromPage(fullPage, box);
		double maxSim = -100;
		for (var ei : examples)
		{
			double sim = compareImages(ei, image);
			if (sim > maxSim) sim = maxSim;
		}
		if (examples.size()==0 || maxSim < 0.8) examples.add(image);
	}

	private char findBestMatch(BufferedImage charImage)
	{
		char bestChar = ' ';
		double bestSim = -100;

		for (var item : charExamples.entrySet())
		{
			char c = item.getKey();
			for (var image : item.getValue())
			{
				double sim = compareImages(image, charImage);
				//System.out.println(sim);
				if (sim > bestSim)
				{
					bestSim = sim;
					bestChar = c;
					//System.out.println(c + " " + sim);
				}
			}
		}
		System.out.println(bestChar + " " + bestSim);
		return bestSim > 0.6 ? bestChar: ' ';
	}

	private static double compareImages(BufferedImage a, BufferedImage b)
	{
		int WHITE = 0xFFFFFF;
		int mwidth = a.getWidth();
		int mheight = a.getHeight();
		int maxw = mwidth;
		int maxh = mheight;
		if (b.getWidth() < mwidth) mwidth = b.getWidth();
		if (b.getHeight() < mheight) mheight = b.getHeight();
		if (b.getWidth() > maxw) maxw = b.getWidth();
		if (b.getHeight() > mheight) mheight = b.getHeight();

		double sim = 0;
		int total = 0;

		for (int x=0;x<maxw;++x)
		{
			for (int y=0;y<maxh;++y)
			{
				double sub = compareRBG(x < a.getWidth() && y < a.getHeight() ? a.getRGB(x, y) : WHITE,
										x < b.getWidth() && y < b.getHeight() ? b.getRGB(x, y) : WHITE);
				if (sub!=0)
				{
					++total;
					if (sub>0) sim++;
				}
				
			}
		}
		return sim / total;
	}

	private static double compareRBG(int a, int b)
	{
		int ag = (a >> 16 & 0xFF) + (a >> 8 & 0xFF) + (a & 0xFF);
		int bg = (b >> 16 & 0xFF) + (b >> 8 & 0xFF) + (b & 0xFF);
		double ablack = (255 * 3 - ag) / 255.0 / 3;
		double bblack = (255 * 3 - bg) / 255.0 / 3;
		double comblack = ablack * bblack;
		return comblack > 0.64 ? 1 : Math.max(ablack, bblack) > 0.8 ? -1 : 0;
	}

	private static BufferedImage extractBoxFromPage(BufferedImage image, int[] box)
	{
		return image.getSubimage(box[0], box[1], box[2]-box[0]+1, box[3]-box[1]+1);
	}

	private BufferedImage getPageImage(int page)
	{
		String path = String.format("../Bread/hlyab%04d.jpg", page);
		
		try
		{
			return ImageIO.read(new File(path));
		}
		catch (IOException e)
		{
			return null;
		}
	}

	private void LoadPageImage()
	{
		int pageNumber = pageNumbers.get(ipage);
		pageImage = getPageImage(pageNumber);
	}
		
	/**
	 * Draws the game to the screen
	 * @param g the graphics object
	 */
	public void paint(Graphics g)
	{
		Image off = createImage(Labeler.width, Labeler.height);
		Graphics2D buf = (Graphics2D)off.getGraphics();	
		
		buf.setFont(new Font("Courier New", Font.PLAIN, 20));
		buf.setColor(Color.BLACK);
		buf.drawString(String.format("Page %d Line %d Character %d", pageNumbers.get(ipage), iline+1, ichar), 650, 100);

		if (pageImage != null)
		{
			var line = boxes.get(ipage).get(iline);
			int[] lineBox = line.get(0).clone();

			lineBox[0] -= MARGIN;
			lineBox[1] -= MARGIN;
			lineBox[2] += MARGIN;
			lineBox[3] += MARGIN;

			int lineWidth = lineBox[2] - lineBox[0] + 1;
			int lineHeight = lineBox[3] - lineBox[1] + 1;

			int startX = width - lineWidth >> 1;
			int startY = (height >> 1) - lineHeight;

			buf.drawImage(pageImage.getSubimage(lineBox[0], lineBox[1], lineWidth, lineHeight), null, startX, startY);

			int textY = startY + lineHeight;

			int numChars = line.size()-1;
			char[] chars = text.get(ipage).get(iline);

			for (int i=0;i<numChars;++i)
			{
				String c = "" + chars[i];
				int[] box = line.get(i+1);

				int charX = box[0] - lineBox[0] + startX;
				int charY = textY + 20;

				if (ichar-1 == i)
				{
					buf.setColor(Color.RED);
					buf.drawRect(charX, startY + box[1] - lineBox[1], box[2] - box[0] + 1, box[3] - box[1] + 1);
				}

				buf.setColor(Color.BLACK);
				buf.drawString(c, charX, charY);
			}
		}

			// buf.setColor(Color.BLACK);
			// buf.drawRect(startX + 5, startY + 5, menuWidth-10, menuHeight-10);

			// buf.setFont(new Font("Lucida Console", Font.PLAIN, 16));
			// buf.setColor(Color.WHITE);
			// buf.drawString("Lawry Sorenson", width-140, height-8);
		g.drawImage(off, 0, 0, null);
	}
	
	/**
	 * Advances the Game each tick
	 */
	public void run()
	{
		while (true)
		{
			lastTick = System.currentTimeMillis();
			repaint();

			try
			{
				Thread.sleep(TICK_FREQUENCY);
			}
			catch (InterruptedException e)
			{
			}
		}
	}

	public void moveRight()
	{
		if (ichar < boxes.get(ipage).get(iline).size() - 1) ++ichar;
		else
		{
			moveDown();
		}
	}

	public void moveDown()
	{
		if (iline < boxes.get(ipage).size() - 1)
		{
			ichar = 1;
			++iline;
		}
		else if (ipage < boxes.size() - 1)
		{
			ichar = 1;
			++ipage;
			iline = 0;
			LoadPageImage();
		}
		predictChars();
	}

	private void predictChars()
	{
		char[] chars = text.get(ipage).get(iline);
		var charBoxes = boxes.get(ipage).get(iline);

		for (int i=0;i<chars.length;++i)
		{
			if (chars[i] == ' ') //Default char, doesn't occur in text
			{
				int[] box = charBoxes.get(i+1);
				chars[i] = findBestMatch(extractBoxFromPage(pageImage, box));
				//System.exit(0);
			}
		}
	}

	public void moveUp()
	{
		if (iline > 0)
		{
			ichar = 1;
			--iline;
		}
		else if (ipage > 0)
		{
			ichar = 1;
			--ipage;
			LoadPageImage();
			iline = boxes.get(ipage).size() - 1;
		}
	}

	public void save()
	{
		try
		{
			PrintStream out = new PrintStream(new File("text.txt"), "UTF8");

			out.println(ipage + " " + iline + " " + ichar);

			int count = 0;
			for (var page : text) for (var line : page)
			{
				for (char c : line) if (c!=' ') ++count;
				out.println(line);
			}

			out.close();
			
			System.out.println(count + " characters labeled");
		}
		catch (IOException e)
		{

		}
	}
	
	/**
	 * Directs key pressed events
	 * @param e the key event
	 */
	public void keyPressed(KeyEvent e)
	{
		if (e.getKeyCode() == 27)
		{
			save();
			System.exit(0); //Escape
		}
		else if (e.getKeyCode() == 39) //Right
		{
			moveRight();
		}
		else if (e.getKeyCode() == 37 || e.getKeyCode() == 8) //Left or Backspace
		{
			if (ichar > 1) --ichar;
			else
			{
				moveUp();
				ichar = boxes.get(ipage).get(iline).size() - 1;
			}
		}
		else if (e.getKeyCode() == 40) //Down
		{
			moveDown();
		}
		else if (e.getKeyCode() == 38) //Up
		{
			moveUp();
		}
		else if ((int)e.getKeyChar() > 32 && (int)e.getKeyChar() < 65535)
		{
			char c = e.getKeyChar();
			if (c == '=') c = (char)1131; //lower big yus
			else if (c== '+') c = (char)1130; //upper big yus
			else if (c== '/') c = (char)1123; //lower yat
			else if (c== '"') c = (char)1122; //upper yat
			else if (c=='_') c = (char)8212; //emdash
			char[] lineText= text.get(ipage).get(iline);
			lineText[ichar-1] = c;
			moveRight();
		}
		else if (e.getKeyCode() == 9) //Tab
		{
			char[] lineText= text.get(ipage).get(iline);
			for (int i=0;i<lineText.length;++i)
			{
				if (lineText[i] == ' ')
				{
					ichar = i+1;
					break;
				}
			}
		}
		//else System.out.println(e);
	}
	
	/**
	 * Handles and directs key released event
	 * @param e the key event
	 */
	public void keyReleased(KeyEvent e)
	{
	}
	
	/**
	 * Handles and directs key typed events
	 * @param e the key event
	 */
	public void keyTyped(KeyEvent e)
	{
	}
	
	/**
	 * Returns the width of the screen
	 * @return the width of the screen
	 */
	public static int getScreenWidth()
	{
		return width;
	}
	
	/**
	 * Returns the height of the screen
	 * @return the height of the screen
	 */
	public static int getScreenHeight()
	{
		return height;
	}
	
	/**
	 * Starts the Labeler when run
	 * @param args not used
	 */
	public static void main(String[] args)
	{
		new Labeler().run();
	}
}