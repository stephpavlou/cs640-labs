#!/bin/bash
python lab1_topo.py;
h1 java Iperfer -s -p 5000 >> throughput_Q3_s1.txt &
h4 java Iperfer -c -h 10.0.0.1 -p 5000 -t 30 >> throughput_Q3_c1.txt &
h7 java Iperfer -s -p 5000 >> throughput_Q3_s2.txt &
h9 java Iperfer -c -h 10.0.0.7 -p 5000 -t 30 >> throughput_Q3_c2.txt ;
cat throughput_Q3_c1.txt >> throughput_Q3_1.txt ;
cat throughput_Q3_s1.txt >> throughput_Q3_1.txt ;
cat throughput_Q3_c2.txt >> throughput_Q3_2.txt ;
cat throughput_Q3_s2.txt >> throughput_Q3_2.txt
