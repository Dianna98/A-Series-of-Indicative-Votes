import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

public class Participant {

    protected int cport,pport,timeout,failurecond;
    protected Set<Integer> details = new HashSet<>();
    protected Set<String> opts = new HashSet<>();

    public Participant(int cport, int pport, int timeout, int failurecond) throws IOException {
        this.cport = cport;
        this.pport = pport;
        this.timeout = timeout;
        this.failurecond = failurecond;

        Socket socket = new Socket("localhost", cport);
        new ParticipantSend(socket).start();
        new GetDetails(socket).start();
        new GetVoteOptions(socket).start();

    }

    public static void main(String[] args) throws IOException {
//        Scanner sc=new Scanner(System.in);
//        String s = sc.nextLine();
//        args = s.split(" ");
        args = new String[] {"12345","12346","5000","0","A"};

        int cport = Integer.parseInt(args[0]);
        int pport = Integer.parseInt(args[1]);
        int timeout = Integer.parseInt(args[2]);
        int failurecond = Integer.parseInt(args[3]);

        new Participant(cport,pport,timeout,failurecond);
    }

    public class ParticipantSend extends Thread {

        Socket socket;
        PrintWriter out;
        BufferedReader in;

        public ParticipantSend(Socket socket) throws IOException {
            this.socket = socket;

            out = new PrintWriter(socket.getOutputStream());
            in = new BufferedReader(new InputStreamReader((socket.getInputStream())));
        }

        @Override
        public void run() {
            super.run();
            out.write("JOIN " + pport);
            System.out.println(pport+" sent JOIN protocol");
        }
    }

    public class GetDetails extends Thread{

        Socket socket;
        PrintWriter out;
        BufferedReader in;

        public GetDetails(Socket socket) throws IOException {
            this.socket = socket;

            out = new PrintWriter(socket.getOutputStream());
            in = new BufferedReader(new InputStreamReader((socket.getInputStream())));

        }

        @Override
        public void run() {
            super.run();

            try {
                String message = in.readLine();
                String[] protocol = message.split(" ");

                if (protocol[0]=="DETAILS"){
                    System.out.println(pport+" got Details from coordinator");
                    for(int i=1; i<protocol.length; i++){
                        details.add(Integer.valueOf(protocol[i]));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public class GetVoteOptions extends Thread{

        Socket socket;
        PrintWriter out;
        BufferedReader in;

        public GetVoteOptions(Socket socket) throws IOException {
            this.socket = socket;

            out = new PrintWriter(socket.getOutputStream());
            in = new BufferedReader(new InputStreamReader((socket.getInputStream())));

        }

        @Override
        public void run() {
            super.run();

            try {
                String message = in.readLine();
                String[] protocol = message.split(" ");

                if (protocol[0]=="VOTE_OPTIONS"){
                    System.out.println(pport+" got Vote Options list from coordinator");
                    for(int i=1; i<protocol.length; i++){
                        opts.add(protocol[i]);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
