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
    private HashMap<Integer,String> votes = new HashMap<>();
    private Integer count = 0;
    private String vote;

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

                String[] v = message.split(" ");
                for (int i = 1; i < v.length; i++) {

                    options.add(v[i]);

                }

                // generate random vote
                vote = randomiseVote(options);
                System.out.println("This participant voted "+vote+".");
                votes.put(pport,vote);

                socket.close();

                // receive votes from other participants
                Thread search = new StartSearching();
                search.start();
                sleep(timeout);
                //search.interrupt();

                //sleep(200);

                // send vote to the other participants
                Thread sending = new StartSending();
                sending.start();
                sleep(timeout);

                //search.interrupt();

                //sleep(1000);

                if (flag == 2){
                    System.out.println("The participant failed right before sending the output.");

                } else if (flag == 0) {
                    // decide outcome
                    String outcome = decideOutcome(votes);
                    Set<Integer> whoVoted = votes.keySet();

                    String outcomeMessage = "OUTCOME " + outcome;
                    for (Integer p : whoVoted) {
                        outcomeMessage = outcomeMessage + " " + p;
                    }
                    System.out.println(outcomeMessage);

                    socket = new Socket("localhost",cport);
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()),true);
                    out.println(outcomeMessage);

//                    new SearchAllVotes().start();
//                    sleep(200);
//                    new SendAllVotes();


                    System.out.println("The participant did not fail.");
                }

            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }

        }

        public String randomiseVote(Set<String> options){
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

    public HashMap<String,Integer> getVotes (HashMap<Integer,String> votes){
        HashMap<String,Integer> out = new HashMap<>();
        int nr;

        for (String v : votes.values()){
            if (out.containsKey(v)){
                nr = out.get(v);
                out.put(v,nr+1);
            } else {
                out.put(v,1);
            }
        }
        return out;
    }

    public String decideOutcome(HashMap<Integer,String> votes){
        int parts = votes.size();
        int majority = parts/2+1;
        HashMap<String,Integer> stats = getVotes(votes);

        for (Map.Entry<String,Integer> v : stats.entrySet()){
            if (v.getValue()>=majority){
                return v.getKey();
            }
        }

        return null;
    }

    public Set<Integer> getMajority(HashMap<Integer,String> votes){
        String vote = decideOutcome(votes);
        Set<Integer> out = new HashSet<>();

        if (vote!=null) {
            for (Map.Entry<Integer, String> v : votes.entrySet()) {
                if (v.getValue().equals(vote)) {
                    out.add(v.getKey());
                }
            }
        }
        return out;
    }


    private class StartSearching extends Thread{
        StartSearching(){}

        @Override
        public void run() {
            super.run();

            try {
                ServerSocket serverSocket = new ServerSocket(pport);
                while (count<participants.size()){
                    Socket socket = serverSocket.accept();
                    count++;
                    new GetVotes(socket).start();
                }
                //sleep(500);
                serverSocket.close();
                count=0;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class GetVotes extends Thread{

        Socket socket;
        BufferedReader in;

        public GetVotes(Socket socket) throws IOException {
            this.socket = socket;
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }

        @Override
        public void run() {
            super.run();
            try {
                String message = in.readLine();
                String[] vote = message.split(" ");
                System.out.println(message);
                votes.put(Integer.parseInt(vote[1]), vote[2]);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class StartSending extends Thread{

        @Override
        public void run() {
            super.run();

            int random = participants.size();
            int i=0;

            if (flag == 1){
                random = new Random().nextInt(participants.size());
            }
            for (Integer p : participants){
                if (i<random) {
                    try {
                        Socket socket = new Socket("localhost", p);

                        PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
                        out.println("VOTE " + pport + " " + vote);

                        sleep(500);
                        socket.close();
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    break;
                }
                i++;
            }

            if (flag==1){
                System.out.println("The participant failed after sending vote to " + i +" other participant(s).");
            }
        }
    }

    private class SearchAllVotes extends Thread {

        SearchAllVotes(){}

        @Override
        public void run() {
            super.run();

            try {
                ServerSocket serverSocket = new ServerSocket(pport);
                while (count<participants.size()){
                    Socket socket = serverSocket.accept();
                    count++;
                    new GetAllVotes(socket).start();
                }
                sleep(500);
                serverSocket.close();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private class GetAllVotes extends Thread {

        Socket socket;
        BufferedReader in;

        public GetAllVotes(Socket socket) throws IOException {
            this.socket = socket;
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }

        @Override
        public void run() {
            super.run();
            try {
                String message = in.readLine();
                String[] vote = message.split(" ");
                System.out.println(message);

                votes = new HashMap<>();

                for(int i = 1; i<vote.length; i=i+2){

                }
                //votes.put(Integer.parseInt(vote[1]), vote[2]);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
