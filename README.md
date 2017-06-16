# Multiagent Systems Project
University Course. Using Java Jade.

## Task
Given a graph of roads and cars in some city, make cars so that traffic load is minimized. More formally, if a car starts driving a road, the road's workload increases by some constant *C*. When a car leaves a road, the road's workload decreases in the opposite way. The more the workload of a road is, the longer a car is driving it.

## Approach
There is three types of agents: 
* Road
* Car
* City

**Car** agent represents an entity that drives across the city from point *A* to point *B*.
Currently, a car supports two strategies of driving: _DUMMY_, _ITERATIVE_.
When a car drives dummy it uses Dijkstra algorithm first to compute the shortest route to destination and keeps following it regardless of traffic conditions.
When it drives iteratively it recomputes its route based on current traffic situation every *refreshPeriod* roads.
  
**Road** agent represents a road entity. Roads can communicate between each other and broadcast their workload changes to those who is interested.

### Agent communication
Provided with basic behaviours of roads and cars the typical communication protocol is the following: a car issues a request to a road proposing itself for driving the road. The road agent receives an occupattion proposal of a car and checks whether this proposal is first there for the car. If so, it immediately responds with *ACCEPT* message, informing car what workload it is going to go through. If not, the road checks whether the car actually can turn to this road using the fact that car proposal includes the following turn information: left road (null in case of first proposal) and road wished to be occupied (null in case of last proposal). Wished road is typically equal to the road that receives the proposal.
The wished road asks the left road whether the proposing car is empowered to switch roads at the time of request. If left road accepts switching it decreases its workload and responds positively to asking road. Based on the response the asking road decides if it accepts the newcoming car, increases workload in such case, and responds to the car. A road broadcasts its workload changes to all interested cars, so that they can rebuild their route using actual information about traffic conditions.

**City**
Monitors the state of the city and gathers statistics about car trips.

## Notes
Run with:
```{sh}
mvn -Pjade-main exec:java
mvn -Pjade-agent exec:java
```

