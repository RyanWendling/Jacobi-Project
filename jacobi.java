/* Ryan Wendling  
322 Project
This jacobi project updates a two dimensional array where we initially only have the perimeter values. 
Through the use of many iterations, two arrays, a threshold - with checker, we are able to have
the values "bleed" into the array and give us an accurate representation of our entire data model.
MUST USE ODD NUMBER OF THREADS
*/

import java.io.*;
import java.util.Scanner;  
import java.lang.*;
import java.util.Arrays;
  
class jacobi { 

   volatile static double[][] inMatrix  = new double[2048][2048]; 
   volatile static double[][] outMatrix = new double[2048][2048];
   static double threshold = .0001;
 

   // Number of threads
   // This function "parses" the command line options and returns the number of
   // requested threads.  This is a separate function for (a) readability and (b)
   // to enable the variable in main to be final.  The final property allows the
   // anonymous class extended from Thread to capture the num_threads variable. 
   private static int number_of_threads(String args[]) {
      int num_threads = 1;
      if (args.length == 1)  {
         num_threads = Integer.parseInt(args[0]);
      }
      return num_threads;
   }
     
  
   public static void main (String args[]) {
     
      final int num_threads = number_of_threads(args);
      int height = 2048/num_threads;
      double[] maxDiffCollection = new double[num_threads];
      //use one of these arrays for each dissemination barrier, every "flag" should start at 0.
      int[] arriveFlag = new int[num_threads];
      int[] arriveFlag2 = new int[num_threads];
      int[] arriveFlag3 = new int[num_threads];
      int[] arriveFlag4 = new int[num_threads];
      double piece = Math.log(num_threads) / Math.log(2);
      double stages = Math.ceil(piece);
      System.out.println("stages for barrier logic:  " + stages);
      System.out.println("number of threads:  " + num_threads);     
      Thread[] threads = new Thread[num_threads];
      try {
          Scanner input = new Scanner (new File("input.mtx"));
          for (int i = 0; i < 2048; i++) {
              for (int j = 0; j < 2048; j++) {
              inMatrix[i][j] = (Double.parseDouble(input.next()));
              outMatrix[i][j] = inMatrix[i][j];   
              }
          }                 
      } catch (FileNotFoundException e) { 
          // in case of error, always log error!
          System.err.println("had trouble reading file");
          e.printStackTrace();   
      }

      //Start keeping track of time
      long start = System.nanoTime();    
    for(int i = 0; i < num_threads; ++i) {
      final int I = i;
      threads[i] = new Thread() {
          public void run() {
             
      int firstRow;
      int lastRow;    
      double maxDifference = 0;
      double localMaxDifference = 0;
      //divies up a section for each thread to compute
      if (num_threads > 1) {
         firstRow = (I * height) + 1;
         lastRow = firstRow + height - 1;   
      } else {
         firstRow = 1;
         lastRow = 2047;
      }
      if (I == (num_threads - 1)) {
         lastRow = lastRow - 1;
      }
                     
      do {
         maxDifference = 0.0;
         localMaxDifference = 0;

         //dissemination barrier   
         for (int i = 1; i <= stages; i++) {
            arriveFlag[I] = arriveFlag[I] + 1;
            //determine neighbor
            double initD = Math.pow(2, (i - 1));
            int init = (int)initD;
            int j = (I + init) % (num_threads);
            while (arriveFlag[I] > arriveFlag[j]) {
               //busy wait;
               try {
                  Thread.sleep(1);       
               } catch(InterruptedException ex) {
                  Thread.currentThread().interrupt();
               }
            }    
         }  
          
         // compute values for all new interior points
         for (int i = firstRow; i <= lastRow; i++) {
            for (int j = 1; j < 2047; j++) {
               outMatrix[i][j] = (inMatrix[i - 1][j] + inMatrix[i + 1][j] + inMatrix[i][j - 1] + inMatrix[i][j + 1]) * .25;
            }
         }
         
         //dissemination barrier
         for (int i = 1; i <= stages; i++) {
            arriveFlag2[I] = arriveFlag2[I] + 1;
            //determine neighbor
            double initD = Math.pow(2, (i - 1));
            int init = (int)initD;
            int j = (I + init) % (num_threads);
            while (arriveFlag2[I] > arriveFlag2[j]) {
               //busy wait;
               try {
                  Thread.sleep(1);      
               } catch(InterruptedException ex) {
                  Thread.currentThread().interrupt();
               }
            }    
         }     
         
         // compute maximum difference for my strip
          for (int i = firstRow; i <= lastRow; i++) {
            for (int j = 1; j < 2047; j++) {
               localMaxDifference = Math.max(localMaxDifference, Math.abs(outMatrix[i][j] - inMatrix[i][j]));
            }   
         } 
         maxDiffCollection[I] = localMaxDifference; 
                
         //dissemination barrier       
         for (int i = 1; i <= stages; i++) {
            arriveFlag3[I] = arriveFlag3[I] + 1;
            //determine neighbor
            double initD = Math.pow(2, (i - 1));
            int init = (int)initD;
            int j = (I + init) % (num_threads);
            while (arriveFlag3[I] > arriveFlag3[j]) {
               //busy wait;
               try {
                  Thread.sleep(1);          
               } catch(InterruptedException ex) {
                  Thread.currentThread().interrupt();
               }
            }    
         }
                       
         // compute maximum difference overall
         for( int i = 0; i < maxDiffCollection.length; i++) {
             if (maxDifference < maxDiffCollection[i]) {
               maxDifference = maxDiffCollection[i];
            } 
         }                 
         
         for (int i = firstRow; i <= lastRow; i++) {
            for (int j = 1; j < 2047; j++) {
               inMatrix[i][j] = outMatrix[i][j];
            }
         }
         
         //dissemination barrier             
         for (int i = 1; i <= stages; i++) {
            arriveFlag4[I] = arriveFlag4[I] + 1;
            //determine neighbor
            double initD = Math.pow(2, (i - 1));
            int init = (int)initD;
            int j = (I + init) % (num_threads);
            while (arriveFlag4[I] > arriveFlag4[j]) {
               //busy wait;
               try {
                  Thread.sleep(1);      
               } catch(InterruptedException ex) {
                  Thread.currentThread().interrupt();
               }
            }    
         }      
         Arrays.fill(maxDiffCollection, 0);                     
      } while (maxDifference > threshold);


      }
      };
      threads[i].start();
    }
    
     
    for(int i = 0; i < num_threads; ++i) {
      try {
        threads[i].join();
      } catch (InterruptedException e){
      }
    } 
       
    System.out.println();    
    //finish keeping track of time and print said time.    
    long end = System.nanoTime();
    System.out.println(end-start);
    
    
    //writes output file
    try {
      PrintWriter writer = new PrintWriter("myoutput.txt", "UTF-8");
    
      for (int i = 0; i < 2048; i++) {
      writer.println();
         for (int j = 0; j < 2048; j++) {
           writer.print(outMatrix[i][j]); 
           writer.print(" ");  
         }
      } 
      
    } catch (FileNotFoundException | UnsupportedEncodingException error) {
      System.out.println("something went wrong here!");
      error.printStackTrace();
    }    
   
    
   }
}