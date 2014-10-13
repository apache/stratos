/**
 * Created by sanjaya on 10/11/14.
 */
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class test{


    static String array1[] = new String[2];
    static double array2[] = new double[7];
    static double array3[] = new double[7];
    static  int i =0;

    public static void main(String args[]) throws IOException {


        BufferedReader stdin = new BufferedReader(new InputStreamReader(
                System.in));
        String line;


        while ((line = stdin.readLine()) != null && line.length() != 0) {

            array1 = line.split(" ");
            array2[i]=  Double.parseDouble(array1[0]);
            array3[i]=  Double.parseDouble(array1[1]);

            i++;


        }


        System.out.println(getPearsonCorrelation(array2, array3));

    }

    public static double getPearsonCorrelation(double[] scores1,double[] scores2){

        double result = 0;

        double sum_sq_x = 0;

        double sum_sq_y = 0;

        double sum_coproduct = 0;

        double mean_x = scores1[0];

        double mean_y = scores2[0];

        for(int i=2;i<scores1.length+1;i+=1){

            double sweep =Double.valueOf(i-1)/i;

            double delta_x = scores1[i-1]-mean_x;

            double delta_y = scores2[i-1]-mean_y;

            sum_sq_x += delta_x * delta_x * sweep;

            sum_sq_y += delta_y * delta_y * sweep;

            sum_coproduct += delta_x * delta_y * sweep;

            mean_x += delta_x / i;

            mean_y += delta_y / i;

        }

        double pop_sd_x = (double) Math.sqrt(sum_sq_x/scores1.length);

        double pop_sd_y = (double) Math.sqrt(sum_sq_y/scores1.length);

        double cov_x_y = sum_coproduct / scores1.length;

        result = cov_x_y / (pop_sd_x*pop_sd_y);

        return result;

    }

}