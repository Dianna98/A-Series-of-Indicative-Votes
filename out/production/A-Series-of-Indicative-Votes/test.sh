#!/ bin/sh

java Coordinator 12345 4 A B &
echo " Waiting ␣ for ␣ coordinator ␣to␣ start ... "
sleep 5
java Participant 12345 12346 5000 0 &
sleep 1
java Participant 12345 12347 5000 0 &
sleep 1
java Participant 12345 12348 5000 0 &
sleep 1
java Participant 12345 12349 5000 0 &