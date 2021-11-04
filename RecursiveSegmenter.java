import java.util.*;
import java.io.*;
import javax.imageio.*;
import java.awt.image.*;

public class RecursiveSegmenter
{
    public static void main(String[] args) throws IOException
    {
        String path = "Bread/hlyab0040.jpg";
        final int THRESH = 350;

        BufferedImage image = ImageIO.read(new File(path));

        int h = image.getHeight();
        int w = image.getWidth();

        boolean[][] black = new boolean[h][w];
        boolean[][] visited = new boolean[h][w];

        System.out.println(h + " " + w);

        BufferedImage out = image;

        int wmargin = 50;
        int hmargin = 200;

        for (int i=0;i<h;++i)
        {
            for (int j=0;j<w;++j)
            {
                int rgb = image.getRGB(j, i);
                int exp = blackness(rgb);
                black[i][j] = i>hmargin && j>wmargin && i+hmargin<h && j+wmargin<w && exp < THRESH;
                // System.out.println(j + " " + i + " " + rgb);
                // System.out.println(exp);
                // System.out.println(j + " " + i + " " + grayToRGB(exp));
                image.setRGB(j, i, grayToRGB(black[i][j] ? 0 : 255));
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

            if (line==null || bot-box[1]<2 || box[3]-top>70) //split lines by overlap
            {
                if (line!=null)
                {
                    if (++i==8) processLine(image, line);
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

        String opath = "out.jpg";

        ImageIO.write(out, "JPG", new File(opath));

    }

    public static void processLine(BufferedImage image, ArrayList<int[]> line)
    {
        System.out.println(line.size());
        //int i = 0;
        for (int[] box : line)
        {
            drawBox(image, box);
            //if (++i==10) break;
        }
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
                image.setRGB(x1, y, rbcToVal(255, 0, 0));
            }
        }
        else if (y1==y2)
        {
            for (int x=x1;x<=x2;++x)
            {
                image.setRGB(x, y1, rbcToVal(255, 0, 0));
            }
        }
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

    public static int grayToRGB(int gray)
    {
        return (-1<<24) | (gray<<16) | (gray<<8) | gray;
    }

    public static int rbcToVal(int r, int g, int b)
    {
        return (-1<<24) | (r<<16) | (g<<8) | b;
    }
}