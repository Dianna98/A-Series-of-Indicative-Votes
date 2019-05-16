import java.io.*;
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
    private ServerSocket serverSocket;

    private class ParticipantThread extends Thread{

        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;


        ParticipantThread(Socket socket) {

            this.socket = socket;
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

            } catch (IOException e) {
            }
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
                System.out.println("PARTICIPANT " + pport + " voted "+vote+".");
                votes.put(pport,vote);

                socket.close();

                // receive votes from other participants
                Thread search = new StartSearching();
                search.start();
//                sleep(timeout);
                //search.interrupt();
                //search.

                //sleep(200);

                // send vote to the other participants
                Thread sending = new StartSending();
                sending.start();

                // after the timeout the ServerSocket that reads the votes is closed
                sleep(timeout);
                serverSocket.close();
                sleep(500);

                if (flag != 1) {
                    //System.out.println("No. votes : "+votes.size());
                    //System.out.println("No. participants : "+participants.size());

                    // receive all votes from other participants
                    Thread searchAll = new SearchAllVotes();
                    searchAll.start();

                    sleep(500);

                    Thread sendAllVotes = new SendAllVotes();
                    sendAllVotes.start();
                    sleep(timeout);
                    serverSocket.close();
                }

                sleep(timeout);

                //sleep(1000);
                socket = new Socket("localhost",cport);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()),true);

                if (flag == 2){
                    String outcome = "OUTCOME none " + pport;
                    out.println(outcome);
                    System.out.println("PARTICIPANT " + pport + " failed right before sending the output.");

                } else if (flag == 1){
                    String outcome = "OUTCOME none " + pport;
                    out.println(outcome);

                } else if (flag == 0) {
                    // decide outcome
                    String outcome = decideOutcome(votes);
                    Set<Integer> whoVoted = votes.keySet();

                    String outcomeMessage = "OUTCOME " + outcome;

                    if (outcome==null){
                        outcomeMessage = outcomeMessage + " " + removeOption(votes);
                    }
                    for (Integer p : whoVoted) {
                        outcomeMessage = outcomeMessage + " " + p;
                    }
                    System.out.println(outcomeMessage);

                   out.println(outcomeMessage);

                    System.out.println("PARTICIPANT " + pport + " did not fail.");

                    if (outcome == null){
                        sleep(1000);
                        new Participant(cport,pport,timeout,flag);
                    }
                    //System.exit(0);
                }

            } catch (IOException | InterruptedException e) {
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

    public Participant(Integer cport, Integer pport, Integer timeout, Integer flag){

        this.cport = cport;
        this.pport = pport;
        this.timeout = timeout;
        this.flag = flag;

        try {
            Socket socket = new Socket("localhost",cport);
            new ParticipantThread(socket).start();

        } catch (IOException e) {
        }

    }

    public static void main(String[] args) {

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

    // this method returns one of the least voted options
    public String removeOption(HashMap<Integer,String> votes){
        HashMap<String,Integer> stats = getVotes(votes);
        int min = Integer.MAX_VALUE;

        for (Integer v : stats.values()){
            if (v<min){
                min = v;
            }
        }

        for (Map.Entry<String,Integer> v : stats.entrySet()){
            if (v.getValue().equals(min)){
                return v.getKey();
            }
        }
        return null;
    }

    private class StartSearching extends Thread{
        StartSearching(){}

        @Override
        public void run() {
            super.run();

            try {
                serverSocket = new ServerSocket(pport);
                while (count<participants.size()){
                    Socket socket = serverSocket.accept();
                    count++;
                    new GetVotes(socket).start();
                }

            } catch (IOException e) {
            }
        }
    }

    private class GetVotes extends Thread{

        Socket socket;
        BufferedReader in;

        public GetVotes(Socket socket)  {
            this.socket = socket;
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            } catch (IOException e) {
            }
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
            }
        }
    }

    private class StartSending extends Thread{

        StartSending(){}

        @Override
        public void run() {
            super.run();

            int random = participants.size();
            int i=0;

            if (flag == 1){
                random = new Random().nextInt(participants.size()-1);
            }
            random++;
            for (Integer p : participants){
                if (i<random) {
                    try {
                        Socket socket = new Socket("localhost", p);

                        PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
                        out.println("VOTE " + pport + " " + vote);

                        sleep(500);
                        socket.close();
                    } catch (IOException | InterruptedException e) {
                    }
                } else {
                    break;
                }
                i++;
            }

            if (flag==1){
                try {
                    sleep(timeout);
                } catch (InterruptedException e) {
                }
                System.out.println("PARTICIPANT " + pport + " failed after sending his vote to " + i +" other participant(s).");
            }
        }
    }

    private class SearchAllVotes extends Thread {

        SearchAllVotes(){}

        @Override
        public void run() {
            super.run();

            try {
                serverSocket = new ServerSocket(pport);
                int c = 0;
                while (true){
                    Socket socket = serverSocket.accept();
                    count++;
                    new GetAllVotes(socket).start();
                }
            } catch (IOException e) {
            }
        }
    }

    private class GetAllVotes extends Thread {

        Socket socket;
        BufferedReader in;

        public GetAllVotes(Socket socket)  {
            this.socket = socket;
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            } catch (IOException e) {
            }
        }

        @Override
        public void run() {
            super.run();
            try {
                String message = in.readLine();
                String[] voteStmt = message.split(" ");
                System.out.println(message);

                //System.out.println(votes.toString());
                for(int i = 1; i<voteStmt.length; i=i+2) {
                    //System.out.println();
                    votes.put(Integer.parseInt(voteStmt[i]), voteStmt[i+1]);
                }

                //System.out.println(votes.toString());
            } catch (IOException e) {
            }
        }
    }

    private class SendAllVotes extends Thread {

        SendAllVotes(){}

        @Override
        public void run() {
            super.run();

            for (Integer p : participants) {
                try {
                    Socket socket = new Socket("localhost", p);
                    PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

                    String message = "VOTE";

                    for (Map.Entry<Integer,String> v : votes.entrySet()){
                        message = message + " " + v.getKey() + " " + v.getValue();
                    }

                    out.println(message);

                    sleep(500);
                    socket.close();
                } catch (IOException | InterruptedException e) {
                }

            }


        }
    }
}
