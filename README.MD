# Safe Environmental Envelopes of Discrete Systems

This repository contains a tool for calculating transition-based robustness of discrete systems.
This tool is the artifact for the paper "Safe Environmental Envelopes of Discrete Systems", accepted to CAV'23.


## Structure and Content

This project contains the following folders:
- bin: contains the executable jar for our tool: LTS-Robustness.jar.
- benchmarks: contains the case studies from our paper, as well as scripts to run them.
- robustifier: contains the source files we used to build this project.


## Set Up

No set up is required; we have already built the tool and packaged it into bin/LTS-Robustness.jar.


## Requirements

Our tool requires Java version >= 11. We have tested the tool under two different Java environments:
```
openjdk 11.0.18 2023-01-17
OpenJDK Runtime Environment (build 11.0.18+10-post-Ubuntu-0ubuntu122.04)
OpenJDK 64-Bit Server VM (build 11.0.18+10-post-Ubuntu-0ubuntu122.04, mixed mode, sharing)
```
and
```
java 18.0.2.1 2022-08-18
Java(TM) SE Runtime Environment (build 18.0.2.1+1-1)
Java HotSpot(TM) 64-Bit Server VM (build 18.0.2.1+1-1, mixed mode, sharing)
```


## Replicating Results from the Paper

We have provided two shell scripts for replicating the results from Table 1 in the paper.
Both scripts are located in the benchmarks/ folder:
```
cd benchmarks
```
To run the heuristic-based algorithm ("Wall Heur" in Table 1):
```
./run_heur.sh
```
To run the plain algorithm ("Wall Plain" in Table 1):
```
./run_plain.sh
```
