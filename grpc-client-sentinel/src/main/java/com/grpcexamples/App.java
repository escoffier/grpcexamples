package com.grpcexamples;

import java.util.Scanner;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        ServiceClient serviceClient = new ServiceClient("127.0.0.1", 50051);
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            String cnt = scanner.next();
            int count = Integer.valueOf(cnt);

            for (int i = 0; i < count; i++) {
                serviceClient.greet("robbie");
            }
        }



        System.out.println( "Hello World!" );
    }
}
