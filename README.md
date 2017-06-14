# Multiagent Systems Project
University Course. Using Java Jade.

##Task
Given a graph of roads and cars in some city, make cars so that traffic load is minimized.

##Approach
There is three types of agents: 
* Road
* Car
* City

**Car** agent represents an entity that drives across the city from point *A* to point *B*.
Currently, a car supports two strategies of driving:_DUMMY_, _ITERATIVE_.
When a car drives dummy it uses Dijkstra algorithm first to compute the shortest route to destination and keeps following it regardless of traffic conditions.
When it drives iteratively it recomputes its route based on current traffic situation every *refreshPeriod* roads.
  
**Road** agent 
To be documented

**City**
Gathers statistics

##Notes
Run with:
```{sh}
mvn -Pjade-main exec:java
mvn -Pjade-agent exec:java
```

