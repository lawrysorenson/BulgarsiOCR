import java.util.*;
import java.io.*;

public class Unique
{
    public static void main(String[] args) throws IOException
    {
        Scanner in = new Scanner(new File("text.txt"), "UTF8");
        //System.out.println(in.nextLine());

        Set<Character> set = new TreeSet<>();

        while (in.hasNextLine())
        {
            String line = in.nextLine();
            //System.out.println(line);
            for (char c : line.toCharArray()) set.add(c);
        }

        in.close();

        PrintStream out = new PrintStream(new File("chars.txt"), "UTF8");
        for (char c : set)
        {
            if (c!=' ' && c!='*') out.print(c);
        }
        out.close();
    }
}