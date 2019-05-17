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

    public static void main(String[] args) {

        int cport = Integer.parseInt(args[0]);
        int pport = Integer.parseInt(args[1]);
        int timeout = Integer.parseInt(args[2]);
        int flag = Integer.parseInt(args[3]);

        // create new Participant
        new Participant(cport,pport,timeout,flag);

    }

    public Participant(Integer cport, Integer pport, Integer timeout, Integer flag){

        this.cport = cport;
        this.pport = pport;
        this.timeout = timeout;
        this.flag = flag;

        try {
            // open socket to connect to the ServerSocket from the Coordinator
            Socket socket = new Socket("localhost",cport);

            // start thread that communicates with the Coordinator
            new ParticipantThread(socket).start();

        } catch (IOException e) {
        }

    }

    // this class describes the main thread that assures the correspondence with the coordinator
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
                System.out.println("PARTICIPANT "+ pport + " registered with the COORDINATOR.");
                out.println(join);

                // get DETAILS
                String message = in.readLine();
                System.out.println(message);
                System.out.println("PARTICIPANT "+ pport + " got the DETAILS from the COORDINATOR.");

                String[] details = message.split(" ");
                for (int i=1; i<details.length;i++){
                    participants.add(Integer.parseInt(details[i]));
                }

                // get VOTE_OPTIONS
                message = in.readLine();
                System.out.println(message);
                System.out.println("PARTICIPANT "+ pport + " got the VOTE_OPTIONS from the COORDINATOR.");

                String[] v = message.split(" ");
                for (int i = 1; i < v.length; i++) {

                    options.add(v[i]);

                }

                // generate random vote
                vote = randomiseVote(options);
                System.out.println("PARTICIPANT " + pport + " voted "+vote+".");
                votes.put(pport,vote);

                // close the connection with the Coordinator
                socket.close();

                // receive votes from other participants
                Thread search = new StartSearching();
                search.start();

                // send vote to the other participants
                Thread sending = new StartSending();
                sending.start();

                // after the timeout the ServerSocket that reads the votes is closed
                sleep(timeout);
                serverSocket.close();
                sleep(500);

                // if the participant did not fail during sending the vote
                if (flag != 1) {

                    // receive all lists of votes from other participants
                    Thread searchAll = new SearchAllVotes();
                    searchAll.start();

                    sleep(500);

                    // send list of all votes to the other participants
                    Thread sendAllVotes = new SendAllVotes();
                    sendAllVotes.start();

                    sleep(timeout);
                    serverSocket.close();
                }

                sleep(timeout);

                // reopen the connection with the Coordinator
                socket = new Socket("localhost",cport);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()),true);

                // if the participant fails right before sending the output, the participant sends an error signal to the Coordinator
                if (flag == 2){
                    String outcome = "OUTCOME none " + pport;
                    out.println(outcome);
                    System.out.println("PARTICIPANT " + pport + " failed right before sending the output.");

                // if the participant fails while sending his vote, then it fails sending the outcome as well
                } else if (flag == 1){
                    String outcome = "OUTCOME none " + pport;
                    out.println(outcome);

                // if the participant does not fail
                } else if (flag == 0) {
                    // then it decides the outcome
                    String outcome = decideOutcome(votes);
                    Set<Integer> whoVoted = votes.keySet();

                    String outcomeMessage = "OUTCOME " + outcome;

                    // if the outcome is null (i.e. there is a tie or no majority meet) the outcome is of form
                    // OUTCOME null <option to be removed> [<participant port>]
                    if (outcome == null) {
                        outcomeMessage = outcomeMessage + " " + removeOption(votes);
                    }

                    for (Integer p : whoVoted) {
                        outcomeMessage = outcomeMessage + " " + p;
                    }
                    System.out.println(outcomeMessage);

                    out.println(outcomeMessage);

                    System.out.println("PARTICIPANT " + pport + " sent the OUTCOME to the COORDINATOR.");

                    System.out.println("PARTICIPANT " + pport + " did not fail.");

                    // if the majority is not met while voting, the participant joins a new round
                    if (outcome == null) {
                        sleep(1000);
                        new Participant(cport, pport, timeout, flag);
                    }
                    //System.exit(0);
                }

            } catch (IOException | InterruptedException e) {
            }

        }

        // this method returns a random vote from the options list
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

    // this method returns a set of the options and the number of participants who voted that option
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

    // this method decides the outcome
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

    // this method returns one of the least voted options in order to remove it in the next round of voting
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

    // this class describes the thread which opens a new ServerSocket for receiving votes from the other participants
    private class StartSearching extends Thread{
        StartSearching(){}

        @Override
        public void run() {
            super.run();

            try {
                // create new ServerSocket that accepts from other participants
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

    // this class describes the thread which receives votes from the other participants
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
                System.out.println("PARTICIPANT " + pport + " received vote from PARTICIPANT " + vote[1] +".");

                // add the vote received in the HashMap of votes
                votes.put(Integer.parseInt(vote[1]), vote[2]);

            } catch (IOException e) {
            }
        }
    }

    // this class describes the thread which sends the vote to the other participants
    private class StartSending extends Thread{

        StartSending(){}

        @Override
        public void run() {
            super.run();

            int random = participants.size();
            int i=0;

            // if the participant is bound to fail during setp 4 (i.e. while sending his vote)
            // he fails after sending a random number of votes between 1 and the number of participants -1
            if (flag == 1){
                random = new Random().nextInt(participants.size()-1);
            }

            random++;
            for (Integer p : participants){
                if (i<random) {
                    try {
                        // create new socket to connect to other participants
                        Socket socket = new Socket("localhost", p);

                        PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
                        out.println("VOTE " + pport + " " + vote);
                        System.out.println("PARTICIPANT " + pport + " sent his vote to PARTICIPANT " + p + ".");

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

    // this class describes the thread which opens the ServerSocket for receiving the lists with new votes
    private class SearchAllVotes extends Thread {

        SearchAllVotes(){}

        @Override
        public void run() {
            super.run();

            try {
                // create new ServerSocket that accepts from other participants
                serverSocket = new ServerSocket(pport);
                while (true){
                    Socket socket = serverSocket.accept();
                    //count++;
                    new GetAllVotes(socket).start();
                }
            } catch (IOException e) {
            }
        }
    }

    // this class describes the thread which receives from the other participants the lists with the new votes from the previous round
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

                for(int i = 1; i<voteStmt.length; i=i+2) {
                    // add all votes to the HashMap of votes in order to fill in any missing votes
                    votes.put(Integer.parseInt(voteStmt[i]), voteStmt[i+1]);
                }

            } catch (IOException e) {
            }
        }
    }

    // this class describes the thread which sends the list of all the votes received in the previous round to all of the other participants
    private class SendAllVotes extends Thread {

        SendAllVotes(){}

        @Override
        public void run() {
            super.run();

            for (Integer p : participants) {
                try {
                    // create new socket to connect to other participants
                    Socket socket = new Socket("localhost", p);
                    PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

                    String message = "VOTE";

                    for (Map.Entry<Integer,String> v : votes.entrySet()){
                        message = message + " " + v.getKey() + " " + v.getValue();
                    }

                    out.println(message);

                    System.out.println("PARTICIPANT " + pport + " sent the list of votes received in the previous round to PARTICIPANT " + p + ".");

                    sleep(500);
                    socket.close();
                } catch (IOException | InterruptedException e) {
                }

            }


        }
    }
}
