import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Participant {

    private Integer cport;
    private Integer pport;
    private Integer timeout;
    private Integer flag;
    private Set<String> options = new HashSet<>();
    private Set<Integer> participants = new HashSet<>();
    private List<String> votes = new ArrayList<>();
    private Integer count = 0;

    private class ParticipantThread extends Thread{

        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;


        ParticipantThread(Socket socket) throws IOException {

            this.socket = socket;
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()),true);

        }

        public void run() {

            try {

                // send JOIN
                String join = "JOIN " + pport;
                out.println(join);

                // get DETAILS
                String message = in.readLine();
                System.out.println(message);

                String[] details = message.split(" ");
                for (int i=1; i<details.length;i++){
                    participants.add(Integer.parseInt(details[i]));
                }

                // get VOTE_OPTIONS
                message = in.readLine();
                System.out.println(message);

                String[] votes = message.split(" ");
                for (int i = 1; i < votes.length; i++) {

                    options.add(votes[i]);

                }

                // generate random vote
                String vote = vote(options);

                socket.close();
                // send vote to other participants
                for(Integer p : participants){
                    Socket s = setSocket(pport,p);
                    new SendVote(s,vote).start();
                    s.close();
                }


//                }

            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        public String vote (Set<String> options){
            int item = new Random().nextInt(options.size());
            int i = 0;
            for(String v : options)
            {
                if (i == item)
                    return v;
                i++;
            }
            return null;
        }

    }

    public Socket setSocket(Integer pport, Integer cport) throws IOException {
        Socket socket = new Socket();
        InetSocketAddress local = new InetSocketAddress("localhost",pport);
        InetSocketAddress connection = new InetSocketAddress("localhost",cport);
        socket.bind(local);
        socket.connect(connection);
        return socket;
    }

    public Participant(Integer cport, Integer pport, Integer timeout, Integer flag) throws IOException {

        this.cport = cport;
        this.pport = pport;
        this.timeout = timeout;
        this.flag = flag;

        Socket socket = new Socket();
        InetSocketAddress local = new InetSocketAddress("localhost",pport);
        InetSocketAddress connection = new InetSocketAddress("localhost",cport);
        socket.bind(local);
        socket.connect(connection);

        new ParticipantThread(socket).start();
    }

    public static void main(String[] args) throws IOException {

        int cport = Integer.parseInt(args[0]);
        int pport = Integer.parseInt(args[1]);
        int timeout = Integer.parseInt(args[2]);
        int flag = Integer.parseInt(args[3]);

        new Participant(cport,pport,timeout,flag);

    }

    private class SendVote extends Thread{

        Socket socket;
        String vote;
        PrintWriter out;

        public SendVote(Socket socket, String vote) throws IOException {
            this.socket = socket;
            this.vote = vote;

            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
        }

        @Override
        public void run() {
            super.run();
            String message = "VOTE " + pport + " " + vote;
            System.out.println("SENT : "+ message);
            //System.out.println(socket.toString());
            out.println(message);
            //out.close();

//            try {
//                new GetVote(socket).run();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }

//            try {
//                socket.close();
//                System.out.println("Socket closed");
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
        }
    }

    private class GetVote extends Thread {

        Socket socket;
        BufferedReader in;

        public GetVote(Socket socket) throws IOException {
            this.socket = socket;
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        }

        @Override
        public void run() {
            super.run();

            try {
                //System.out.println(socket.toString());
                String message = in.readLine();
                //System.out.println("WTF?!");
                System.out.println(message);

                String[] vote = message.split(" ");
                votes.add(vote[2]);

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}
