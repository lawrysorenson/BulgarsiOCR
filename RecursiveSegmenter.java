import java.util.*;
import java.io.*;
import javax.imageio.*;
import java.awt.image.*;

public class RecursiveSegmenter
{
    public static void main(String[] args) throws IOException
    {
        String path = "Bread/hlyab0170.jpg";
        final int THRESH = 350;

        BufferedImage image = ImageIO.read(new File(path));

        int h = image.getHeight();
        int w = image.getWidth();

        boolean[][] black = new boolean[h][w];

        System.out.println(h + " " + w);

        BufferedImage out = image;

        for (int i=0;i<h;++i)
        {
            for (int j=0;j<w;++j)
            {
                int rgb = out.getRGB(j, i);
                int exp = blackness(rgb);
                int b = exp < THRESH ? 0 : 255;
                // System.out.println(j + " " + i + " " + rgb);
                // System.out.println(exp);
                // System.out.println(j + " " + i + " " + grayToRGB(exp));
                out.setRGB(j, i, grayToRGB(b));
            }
        }

        String opath = "out.jpg";

        ImageIO.write(out, "JPG", new File(opath));
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
}