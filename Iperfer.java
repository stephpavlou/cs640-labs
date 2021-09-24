import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.ServerSocket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Scanner;
/**
 *
 * @author cmdup
 */
public class Iperfer {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        String hostname_ip = "";
        int port_num = 0;
        int time = 0;
        // TODO code application logic here
        if(!(args.length == 3 || args.length == 7)) {
            System.out.println("Error: Invalid arguments");
            System.exit(1);
        }
        if(args[0].equals("-c")) {
            if ((!args[1].equals("-h")) || (!args[3].equals("-p")) || (!args[5].equals("-t"))) {
                System.out.println("Error: Invalid arguments");
                System.exit(1);
            }
            // Check input values
            // Hostname
            try {
                
            } catch (Exception ex) {
                
            }
            // Port
            try {
                port_num = Integer.parseInt(args[4]);
                if((port_num < 1024) || (port_num > 65535)) {
                    System.out.println("Error: port number must be in the range 1024 to 65535");
                    System.exit(1);
                }
            } catch (Exception ex) {
                System.out.println("Error: port number must be a valid number");
                System.exit(1);
            }
            // Time
            // Time is assumed to mean seconds
            try {
                time = Integer.parseInt(args[6]);
                if(time < 0) {
                    System.out.println("Error: time cannot be a negative value");
                    System.exit(1);
                }
            } catch (Exception ex) {
                System.out.println("Error: time must be a valid number");
                System.exit(1);
            }
            run_client(hostname_ip, port_num, time);
        } else if(args[0].equals("-s")) {
            // Port
            try {
                port_num = Integer.parseInt(args[2]);
                if((port_num < 1024) || (port_num > 65535)) {
                    System.out.println("Error: port number must be in the range 1024 to 65535");
                    System.exit(1);
                }
            } catch (Exception ex) {
                System.out.println("Error: port number must be a valid number");
                System.exit(1);
            }
            run_server(port_num);
        } else {
            System.out.println("Error: Invalid arguments");
            System.exit(1);
        }
    }
    
    public static void run_client(String hostname_ip, int port_num, int time) {
        try{
            int num_packets = 0;
            double data_rate;
            Socket socket = new Socket(hostname_ip, port_num);
            System.out.println("Client has connected");
            PrintWriter out = new PrintWriter(socket.getOutputStream(),true);
            //String msg = "Hello World!";
            byte barray[] = new byte[1000];
            for(int i = 0; i < 1000; i++) {
                barray[i] = 0;
            }
            //ADD loop 
            long target_time = System.currentTimeMillis() + (1000 * time);
            while(System.currentTimeMillis() < target_time) {
                num_packets++;
                out.println(barray);
            }
            
            out.close();
            socket.close();
            data_rate = ((num_packets*8000)/1000000)/time;
            System.out.printf("sent=%d KB rate =%.3f Mbps", num_packets, data_rate);
        } catch(Exception ex) {
            
        }
    }
    
    public static void run_server(int port_num) {
        try{
            ServerSocket server = new ServerSocket(port_num);
            Socket socket = server.accept();
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String line = "";
            while((line = in.readLine()) != null) {
                System.out.println(line);
            }
            
            in.close();
            socket.close();
            server.close();
        } catch(Exception ex){
            
        }
       
    }
    
}
