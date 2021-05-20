//Work needed
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Router implements Serializable {
    private int routerId;
    private int numberOfInterfaces;
    private ArrayList<IPAddress> interfaceAddresses;//list of IP address of all interfaces of the router
    private ArrayList<RoutingTableEntry> routingTable;//used to implement DVR
    private ArrayList<Integer> neighborRouterIDs;//Contains both "UP" and "DOWN" state routers
    private Boolean state;//true represents "UP" state and false is for "DOWN" state
    private Map<Integer, IPAddress> gatewayIDtoIP;
    public Router() {
        interfaceAddresses = new ArrayList<>();
        routingTable = new ArrayList<>();
        neighborRouterIDs = new ArrayList<>();

        /**
         * 80% Probability that the router is up
         */
        Random random = new Random();
        double p = random.nextDouble();
        if(p < 0.80) state = true;
        else state = false;

        numberOfInterfaces = 0;
    }

    public Router(int routerId, ArrayList<Integer> neighborRouters, ArrayList<IPAddress> interfaceAddresses, Map<Integer, IPAddress> gatewayIDtoIP) {
        this.routerId = routerId;
        this.interfaceAddresses = interfaceAddresses;
        this.neighborRouterIDs = neighborRouters;
        this.gatewayIDtoIP = gatewayIDtoIP;
        routingTable = new ArrayList<>();

        /**
         * 80% Probability that the router is up
         */
        Random random = new Random();
        double p = random.nextDouble();
        if(p < 0.80) state = true;
        else state = false;

        numberOfInterfaces = interfaceAddresses.size();
    }

    @Override
    public String toString() {
        String string = "";
        string += "Router ID: " + routerId + "\n" + "Interfaces: \n";
        for (int i = 0; i < numberOfInterfaces; i++) {
            string += interfaceAddresses.get(i).getString() + "\t";
        }
        string += "\n" + "Neighbors: \n";
        for(int i = 0; i < neighborRouterIDs.size(); i++) {
            string += neighborRouterIDs.get(i) + "\t";
        }
        return string;
    }

    /**
     * Initialize the distance(hop count) for each router.
     * for itself, distance=0; for any connected router with state=true, distance=1; otherwise distance=Constants.INFTY;
     */
    public void initiateRoutingTable() {
        if(this.getState()) {
            for (int i = 1; i <= NetworkLayerServer.routers.size(); i++) {
                RoutingTableEntry rte;
                if (!(i == this.routerId)) {
                    float dist;
                    if (neighborRouterIDs.contains(i) && NetworkLayerServer.routers.get(i - 1).getState())
                        dist = 1;
                    else
                        dist = Constants.INFINITY;

                    if (dist == 1) {
                        rte = new RoutingTableEntry(i, dist, i);
                    } else
                        rte = new RoutingTableEntry(i, dist, 0);
                }
                else
                    rte = new RoutingTableEntry(i, 0, i);
                routingTable.add(rte);
            }
            //this.printRoutingTable();
        }
    }

    /**
     * Delete all the routingTableEntry
     */
    public void clearRoutingTable() {

        this.routingTable.clear();
        //printRoutingTable();
    }

    /**
     * Update the routing table for this router using the entries of Router neighbor
     * @param neighbor
     */
    public boolean updateRoutingTable(Router neighbor) {

        //System.out.println("Inside Update1");
        if(!this.getState() || !neighbor.getState())
            return false;
        int updateCount = 0;
        for(int i = 0; i < this.routingTable.size(); i++) {
            double d = 1 + neighbor.routingTable.get(i).getDistance();
            if(d < this.routingTable.get(i).getDistance()){
                this.routingTable.get(i).setDistance(d);
                this.routingTable.get(i).setGatewayRouterId(neighbor.getRouterId());
                updateCount++;
            }
        }
        if(updateCount > 0)
            return true;
        else
            return false;
    }

    public boolean sfupdateRoutingTable(Router neighbor) {

        if(!this.getState() || !neighbor.getState())
            return false;
        int updateCount = 0;
        for(int i = 0; i < this.routingTable.size(); i++) {
            double d = 1 + neighbor.routingTable.get(i).getDistance();

            if( this.routingTable.get(i).getGatewayRouterId() == neighbor.getRouterId() || (d < this.routingTable.get(i).getDistance() && this.getRouterId() != neighbor.routingTable.get(i).getGatewayRouterId()) ){
                this.routingTable.get(i).setDistance(d);
                this.routingTable.get(i).setGatewayRouterId(neighbor.getRouterId());
                updateCount++;
            }
        }
        if(updateCount > 0)
            return true; //
        else
            return false;
    }

    /**
     * If the state was up, down it; if state was down, up it
     */
    public void revertState() {
        state = !state;
        if(state) { initiateRoutingTable(); }
        else { clearRoutingTable(); }
       // System.out.println("Revert state change router");
       // printRoutingTable();//
    }

    public int getRouterId() {
        return routerId;
    }

    public void setRouterId(int routerId) {
        this.routerId = routerId;
    }

    public int getNumberOfInterfaces() {
        return numberOfInterfaces;
    }

    public void setNumberOfInterfaces(int numberOfInterfaces) {
        this.numberOfInterfaces = numberOfInterfaces;
    }

    public ArrayList<IPAddress> getInterfaceAddresses() {
        return interfaceAddresses;
    }

    public void setInterfaceAddresses(ArrayList<IPAddress> interfaceAddresses) {
        this.interfaceAddresses = interfaceAddresses;
        numberOfInterfaces = interfaceAddresses.size();
    }

    public ArrayList<RoutingTableEntry> getRoutingTable() {
        return routingTable;
    }

    public void addRoutingTableEntry(RoutingTableEntry entry) {
        this.routingTable.add(entry);
    }

    public ArrayList<Integer> getNeighborRouterIDs() {
        return neighborRouterIDs;
    }

    public void setNeighborRouterIDs(ArrayList<Integer> neighborRouterIDs) { this.neighborRouterIDs = neighborRouterIDs; }

    public Boolean getState() {
        return state;
    }

    public void setState(Boolean state) {
        this.state = state;
    }

    public Map<Integer, IPAddress> getGatewayIDtoIP() { return gatewayIDtoIP; }

    public void printRoutingTable() {
        System.out.println("Router " + routerId);
        System.out.println("DestID Distance Nexthop");
        for (RoutingTableEntry routingTableEntry : routingTable) {
            System.out.println(routingTableEntry.getRouterId() + "   " + routingTableEntry.getDistance() + "   " + routingTableEntry.getGatewayRouterId());
        }
        System.out.println("-----------------------");
    }
    public String strRoutingTable() {
        String string = "Router" + routerId + "\n";
        string += "DestID Distance Nexthop\n";
        for (RoutingTableEntry routingTableEntry : routingTable) {
            string += routingTableEntry.getRouterId() + " " + routingTableEntry.getDistance() + " " + routingTableEntry.getGatewayRouterId() + "\n";
        }

        string += "-----------------------\n";
        return string;
    }
}
