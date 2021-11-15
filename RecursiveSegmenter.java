import java.util.*;
import java.io.*;
import javax.imageio.*;
import java.awt.image.*;

public class RecursiveSegmenter
{
    private static boolean[][] black;
    private static PrintStream out;

    public static void main(String[] args) throws IOException
    {
        out = new PrintStream(new File("seg/seg.txt"));
        for (int imageNumber = 3; imageNumber<=240;++imageNumber)
        {
            System.out.print(imageNumber + ": ");
            out.println(imageNumber + " # page");
            //if (imageNumber==4) System.exit(0);//Remove

            String path = String.format("Bread/hlyab%04d.jpg", imageNumber);
            final int THRESH = 350;

            BufferedImage image = ImageIO.read(new File(path));

            int h = image.getHeight();
            int w = image.getWidth();

            black = new boolean[h][w];
            boolean[][] visited = new boolean[h][w];

            System.out.println(h + " " + w);

            //BufferedImage out = image;

            int wmargin = 50;
            int hmargin = 100;

            for (int i=0;i<h;++i)
            {
                for (int j=0;j<w;++j)
                {
                    int rgb = image.getRGB(j, i);
                    int exp = blackness(rgb);
                    black[i][j] = i>hmargin+50 && j>wmargin && i+hmargin<h && j+wmargin<w && exp < THRESH;
                    // System.out.println(j + " " + i + " " + rgb);
                    // System.out.println(exp);
                    // System.out.println(j + " " + i + " " + grayToRGB(exp));
                    //if (black[i][j]) image.setRGB(j, i, rbcToVal(0, 0, 255));
                }
            }

            ArrayList<int[]> list = new ArrayList<>();

            for (int i=0;i<h;++i)
            {
                for (int j=0;j<w;++j)
                {
                    if (black[i][j] && !visited[i][j])
                    {
                        //System.out.println();
                        int[] sub = recurse(black, visited, j, i);
                        //System.out.println(Arrays.toString(sub));
                        if (sub[4]>4) list.add(sub);
                    }
                }
            }

            int i=0;
            ArrayList<int[]> line = null;
            int top = 0;
            int bot = 0;


            for (int[] box : list)
            {
                //System.out.println(++i);

                if (line==null || bot-box[1]<2 || box[3]-top>50) //split lines by overlap
                {
                    if (line!=null)
                    {
                        //if (++i==5) 
                        processLine(image, black, line);
                    }
                    line = new ArrayList<int[]>();
                    line.add(box);
                    top = box[1];
                    bot = Math.max(box[3], top+10);
                    //System.out.println(top + " " + bot);
                }
                else
                {
                    line.add(box);
                    if (box[1] < top) top = box[1];
                    if (box[3] > bot) bot = box[3];
                }
                //drawBox(image, box);
            }
            
            if (line!=null)
            {
                processLine(image, black, line);
            }

            out.println(0);

            // String opath = String.format("Pixels/Labels/%04d.png", imageNumber);

            // ImageIO.write(out, "png", new File(opath));
        }
        out.close();
    }

    public static void processLine(BufferedImage image, boolean[][] black, ArrayList<int[]> line)
    {
        Collections.sort(line, new Comparator<int[]>(){
            public int compare(int[] a, int[] b)
            {
                return a[0]-b[0];
            }
        });

        for (int i=0;i<line.size()-1;++i)
        {
            int[] cur = line.get(i);
            int[] next = line.get(i+1);
            int diff = next[0]-cur[2] + next[2]-cur[0];
            if (diff<25)
            {
                comb(cur, next);
                line.remove(i+1);
                --i;
            }
        }
        
        for (int i=0;i<line.size()-1;++i)
        {
            int[] cur = line.get(i);
            int[] next = line.get(i+1);
            int diff = next[0]-cur[2] + next[2]-cur[0];
            if (diff<25)
            {
                comb(cur, next);
                line.remove(i+1);
                --i;
            }
        }
        
        for (int i=0;i<line.size()-1;++i)
        {
            int[] cur = line.get(i);
            if (cur[2] - cur[0] >= 30 && cur[3]-cur[1] > 8)
            {
                line.remove(i);
                --i;
                ArrayList<int[]> res = splitChar(black, cur);
                for (int[] sub : res) line.add(sub);
            }
        }

        Collections.sort(line, new Comparator<int[]>(){
            public int compare(int[] a, int[] b)
            {
                return a[0]-b[0];
            }
        });

        int[] box = line.get(0);
        int[] linebox = new int[]{box[0], box[1], box[2], box[3]};

        for (int i=1;i<line.size();++i)
        {
            box = line.get(i);
            if (box[0]<linebox[0]) linebox[0] = box[0];
            if (box[1]<linebox[1]) linebox[1] = box[1];
            if (box[2]>linebox[2]) linebox[2] = box[2];
            if (box[3]>linebox[3]) linebox[3] = box[3];
            //out.println(Arrays.toString(linebox) + " " + Arrays.toString(box));
        }

        out.println(line.size() + " " + linebox[0] + " " + linebox[1] + " " + linebox[2] + " " + linebox[3] + " # line");

        for (int[] box2 : line)
        {
            out.println(box2[0] + " " + box2[1] + " " + box2[2] + " " + box2[3]);
        }

        // for (int[] box : line)
        // {
        //     drawBox(image, box);
        // }
    }

    public static ArrayList<int[]> splitChar(boolean[][] black, int[] chars)
    {
        ArrayList<int[]> answer = new ArrayList<>();
        
        while (chars[2] - chars[0] >= 30)
        {
            int split = 0;
            int splitCount = -1;

            for (int i=16;i<29;++i)
            {
                int x = i+chars[0];
                int count = 0;
                for (int y=chars[1];y<=chars[3];++y) if (black[y][x]) ++count;
                if (splitCount==-1 || count < splitCount)
                {
                    splitCount = count;
                    split = x;
                }
            }

            int[] toAdd = new int[]{chars[0], chars[1], split, chars[3], -1};
            answer.add(toAdd); //TODO: update pixel count?
            chars[0] = split + 1;
        }

        answer.add(chars);

        //for (int[] sub : answer) System.out.println(Arrays.toString(sub));

        return answer;
    }

    public static void drawBox(BufferedImage image, int[] coords)
    {
        int x1 = coords[0];
        int y1 = coords[1];
        int x2 = coords[2];
        int y2 = coords[3];

        drawLine(image, x1, y1, x2, y1);
        drawLine(image, x2, y1, x2, y2);
        drawLine(image, x1, y2, x2, y2);
        drawLine(image, x1, y1, x1, y2);
    }

    public static void drawLine(BufferedImage image, int x1, int y1, int x2, int y2)
    {
        if (x1==x2)
        {
            for (int y=y1;y<=y2;++y)
            {
                addRed(image, x1, y);
                //image.setRGB(x1, y, rbcToVal(255, 0, 0));
            }
        }
        else if (y1==y2)
        {
            for (int x=x1;x<=x2;++x)
            {
                addRed(image, x, y1);
                //image.setRGB(x, y1, rbcToVal(255, 0, 0));
            }
        }
    }

    public static void addRed(BufferedImage image, int x, int y)
    {
        int rgb = image.getRGB(x, y);
        int blue = black[y][x] ? 255 : 0;
        //if (red > 255) red = 255;
        image.setRGB(x, y, rbcToVal(255, 0, blue));
    }

    public static int[] recurse(boolean[][] black, boolean[][] visited, int x, int y)
    {
        int h = black.length;
        int w = black[0].length;
        //System.out.println('\t' + " " + x + " " + h);
        //if (x==1535) System.out.println(w + " " + h + " " + x + " " + y + " " + black[x][y] + " " + visited[x][y]);
        if (x<0 || x==w || y<0 || y==h || !black[y][x] || visited[y][x]) return null;
        //System.out.println("\t " + x + " " + y);
        visited[y][x] = true;

        int[] answer = new int[]{x, y, x, y, 1};

        for (int i=-1;i<=1;++i)
        {
            for (int j=-4;j<=4;++j)
            {
                comb(answer, recurse(black, visited, x+i, y+j));
            }
        }

        return answer;
    }

    public static void comb(int[] a, int[] b)
    {
        if (b==null) return;

        if (b[0]<a[0]) a[0] = b[0];
        if (b[1]<a[1]) a[1] = b[1];
        if (b[2]>a[2]) a[2] = b[2];
        if (b[3]>a[3]) a[3] = b[3];
        a[4] += b[4];
    }

    public static int blackness(int rgb)
    {
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;

        int mean = (red + green + blue) / 3;

        red-=mean;
        green-=mean;
        blue-=mean;

        if (red<0) red = -red;
        if (green<0) green = -green;
        if (blue<0) blue = -blue;

        //System.out.println(red + " " + green + " " + blue);

        return (red + green + blue) * 6 / 2 + mean / 16 * 16;
    }

    public static int addBlue(int rgb)
    {
        return (rgb & ~0xFF) | 255;
    }

    public static int grayToRGB(int gray)
    {
        return (-1<<24) | (gray<<16) | (gray<<8) | gray;
    }

    public static int rbcToVal(int r, int g, int b)
    {
        return (-1<<24) | (r<<16) | (g<<8) | b;
    }
}