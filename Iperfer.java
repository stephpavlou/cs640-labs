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
        String acceptable_chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789.-";
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
                if(hostname_ip.length() > 255) {
                    System.out.println("Error: hostname must be lesss than 254 ASCII characters");
                    System.exit(1);
                }
                
                for(int i = 0; i < hostname_ip.length(); i++) {
                    boolean good = false;
                    for(int j = 0; j < acceptable_chars.length();j++) {
                        if(hostname_ip.charAt(i) == acceptable_chars.charAt(j)){
                            good = true;
                            break;
                        }
                    }
                    if(!good) {
                        System.out.println("Error: hostname must only contain valid characters");
                        System.exit(1);
                    }
                }
                
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
            //System.out.printf("hostname_ip: "+ hostname_ip +", port_num: %d, time: %d", port_num, time);
            long num_packets = 0;
            double data_rate;
            Socket socket = new Socket(hostname_ip, port_num);
            System.out.println("Client has connected");
            PrintWriter out = new PrintWriter(socket.getOutputStream(),true);
            //String msg = "Hello World!";
            byte byte_array[] = new byte[1000];
            for(int i = 0; i < 1000; i++) {
                byte_array[i] = 0;
            }
            String msg = new String(byte_array);
            //System.out.println("msg: " + msg);
            //System.out.printf("msg.length(): %d\n", msg.length());
            
            long target_time = System.currentTimeMillis() + (1000 * time);
            while(System.currentTimeMillis() < target_time) {
                num_packets++;
                out.println(msg);
            }
            
            out.close();
            socket.close();
            data_rate = ((num_packets*8000)/1000000)/((double)time);
            System.out.printf("sent=%d KB rate =%.3f Mbps", num_packets, data_rate);
        } catch(Exception ex) {
            
        }
    }
    
    public static void run_server(int port_num) {
        try{
            ServerSocket server = new ServerSocket(port_num);
            Socket socket = server.accept();
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            long num_bytes = 0;
            long start_time = -1;
            long end_time = 0;
            String line = "";
            byte[] data;
            while((line = in.readLine()) != null) {
                if(start_time == -1) {
                    start_time = System.currentTimeMillis();
                }
                data = line.getBytes();
                num_bytes += data.length;
                //System.out.println("received: " + line);
                //System.out.printf("Length of byte array received: %d", data.length);
            }
            end_time = System.currentTimeMillis();
            long elapsed_time = end_time - start_time;
            double elapsed_time_seconds = elapsed_time / 1000;
            double rate_received = ((num_bytes * 8)/1000000)/elapsed_time_seconds;
            
            in.close();
            socket.close();
            server.close();
            System.out.printf("received=%d KB rate =%.3f Mbps", (num_bytes/1000), rate_received);
        } catch(Exception ex){
            
        }
       
    }
    
}
