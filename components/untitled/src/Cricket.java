import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Cricket {

    static String[] days = { "mon", "tues", "wed", "thu", "fri", "sat", "sun" };
    static int[] day30 = { 4, 6, 9, 11 };
    static int[] day31 = { 1, 3,5,7,8,10,12};

    public static int getMonth() {


        int dayOne = 1;
        String curMon = "Jan";
        String curDay = "wed";
        int curYr = 2014;

        int count = 1;
        int newDay = 29;
        boolean monExits = false;


        if (!monExits) {
            if (curYr % 4 == 0) {

            } else {
                count--;
            }
        }
        return count;
    }



    public static void main(String[] args) throws IOException {


		boolean monExits = false;
		 BufferedReader inp = new BufferedReader( new
         InputStreamReader(System.in));
        int T = Integer.parseInt(inp.readLine());

		 for (int i = 2014; i < 2014 + T; i++) {

             for(int month=1; month<=12; month++){

                 //if mont has 30 days

                 for (int a : day30) {
                     if (a == month) {
                       //  count++;
                         monExits = true;

                     }
                 }
                 for (int a : day31) {
                     if (a == month) {
                       //  count=count+2;
                         monExits = true;



                     }
                 }

                 if(monExits==false){

                     //day 29,30

                 }



             }




		 }


		/*
		 * countDays();
		 * 
		 * HashMap<Integer, String[]> months = new HashMap<>(); months.put(30,
		 * new String[] { "Apr", "June", "Sep", "Nov" }); months.put(31, new
		 * String[] { "Jan", "Mar", "May", "July", "Aug", "Oct", "Dec" });
		 * 
		 * System.out.println(months.get(30));
		 */

    }
}