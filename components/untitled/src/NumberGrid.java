import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;


public class NumberGrid {


    public static void main(String args[]) throws IOException {


        BufferedReader stdin = new BufferedReader(new InputStreamReader(
                System.in));
        String line;

        String firstLineArray[] = new String[4];


        boolean readFirstLine = true;


        ArrayList<int[]> arrayList = new ArrayList<int[]>();

        while ((line = stdin.readLine()) != null && line.length() != 0) {

            if (readFirstLine) {
                firstLineArray = line.split(" ");
                readFirstLine = false;
            } else {

                String[] strings = line.split(" ");
                int[] ints = new int[strings.length];
                for (int i = 0; i < strings.length; i++) {
                    ints[i] = Integer.parseInt(strings[i]);
                }
                arrayList.add(ints);
            }

        }

        int rows = Integer.parseInt(firstLineArray[0]);
        int coloms = Integer.parseInt(firstLineArray[1]);
        int height = Integer.parseInt(firstLineArray[2]);
        int width = Integer.parseInt(firstLineArray[3]);


        int total = 0;
        int newTotal = 0;


        //for each row
        for (int YPosition = 0; YPosition + height <= rows; YPosition++) {

           for (int XPosition =0; XPosition+width <= coloms ; XPosition++) {
                        newTotal = 0;

                        for (int j = YPosition; j < YPosition + height; j++) {
                            for (int k = XPosition; k < XPosition + width; k++) {

                                newTotal += arrayList.get(j)[k];


                            }

                        }
                        if(total<newTotal){
                            total = newTotal;
                        }

                    }
        }

        //change height into width and width into height and check again

        int temp;
        temp = height;
        height = width;
        width = temp;

        //for each row
        for (int YPosition = 0; YPosition + height <= rows; YPosition++) {

            for (int XPosition =0; XPosition+width <= coloms ; XPosition++) {
                newTotal = 0;

                for (int j = YPosition; j < YPosition + height; j++) {
                    for (int k = XPosition; k < XPosition + width; k++) {

                        newTotal += arrayList.get(j)[k];

                    }

                }
                if(total<newTotal){
                    total = newTotal;
                }

            }
        }

       System.out.println(total);
    }


}
