import java.util.ArrayList;
import java.util.Random;

public class ServerThread implements Runnable {

    NetworkUtility networkUtility;
    EndDevice endDevice;

    ServerThread(NetworkUtility networkUtility, EndDevice endDevice) {
        this.networkUtility = networkUtility;
        this.endDevice = endDevice;
        System.out.println("Server Ready for client " + NetworkLayerServer.clientCount);
        NetworkLayerServer.clientCount++;
        new Thread(this).start();
    }

    @Override
    public void run() {
        /**
         * Synchronize actions with client.
         */
        networkUtility.write(this.endDevice);
        networkUtility.write(NetworkLayerServer.endDevices);
        networkUtility.write(NetworkLayerServer.deviceIDtoRouterID);
        networkUtility.write(NetworkLayerServer.routers);

        for (int i = 0; i < 100; i++){
            Packet p = (Packet)networkUtility.read();

            boolean bool = deliverPacket(p);
            if(!bool){
                //System.out.println("Failed");
                String str = "dropped packet";
                networkUtility.write(str);
            }
        }
        /*
        Tasks:
        1. Upon receiving a packet and recipient, call deliverPacket(packet)
        2. If the packet contains "SHOW_ROUTE" request, then fetch the required information
                and send back to client
        3. Either send acknowledgement with number of hops or send failure message back to client
        */
    }


    public Boolean deliverPacket(Packet p) {

        EndDevice sourceEndDevice = NetworkLayerServer.endDeviceMap.get(p.getSourceIP());
        EndDevice destEndDevice = NetworkLayerServer.endDeviceMap.get(p.getDestinationIP());

        int srcRouterId = NetworkLayerServer.interfacetoRouterID.get(sourceEndDevice.getGateway());
        int destRouterId = NetworkLayerServer.interfacetoRouterID.get(destEndDevice.getGateway());

        Router srcRouter = NetworkLayerServer.routerMap.get(srcRouterId);
        Router destRouter = NetworkLayerServer.routerMap.get(destRouterId);

        if(!srcRouter.getState() || !destRouter.getState())
            return false;

        System.out.println(srcRouterId + "-->" + destRouterId);
        //srcRouter.printRoutingTable();
        //int a = srcRouter.getRoutingTable().get(destRouterId-1).getGatewayRouterId();
        //double b = srcRouter.getRoutingTable().get(destRouterId-1).getDistance();
        //System.out.println(a +"  "+ b);

        String path ="" + srcRouterId;
        while(srcRouter.getRoutingTable().get(destRouterId-1).getGatewayRouterId() != destRouterId){

            if(srcRouter.getRoutingTable().get(destRouterId-1).getDistance() == Constants.INFINITY){
                return false;
            }
            p.hopcount++;
            path +=" " + srcRouter.getRoutingTable().get(destRouterId-1).getGatewayRouterId();
            Router prevSrc = srcRouter;
            srcRouter = NetworkLayerServer.routerMap.get(srcRouter.getRoutingTable().get(destRouterId-1).getGatewayRouterId());

            //3(a)
            if(!srcRouter.getState()) //nexthop router is down
            {
                prevSrc.getRoutingTable().get(destRouterId-1).setDistance(Constants.INFINITY);
                RouterStateChanger.islocked = true;
                NetworkLayerServer.DVR(prevSrc.getRouterId());
                RouterStateChanger.islocked = false;
                return false;
            }

            //3b
            if(srcRouter.getRoutingTable().get(prevSrc.getRouterId()-1).getDistance() == Constants.INFINITY){
                srcRouter.getRoutingTable().get(prevSrc.getRouterId()-1).setDistance(1);
                RouterStateChanger.islocked = true;
                NetworkLayerServer.DVR(srcRouter.getRouterId());
                RouterStateChanger.islocked = false;
            }
        }
        path += " "+ destRouterId;

        networkUtility.write("Successful");
        networkUtility.write(p.hopcount);
        if(p.getSpecialMessage().equals("SHOW_ROUTE")){
            networkUtility.write(path);
        }
        return true;
        /*
        1. Find the router s which has an interface
                such that the interface and source end device have same network address.
        2. Find the router d which has an interface
                such that the interface and destination end device have same network address.
        3. Implement forwarding, i.e., s forwards to its gateway router x considering d as the destination.
                similarly, x forwards to the next gateway router y considering d as the destination,
                and eventually the packet reaches to destination router d.

            3(a) If, while forwarding, any gateway x, found from routingTable of router r is in down state[x.state==FALSE]
                    (i) Drop packet
                    (ii) Update the entry with distance Constants.INFTY
                    (iii) Block NetworkLayerServer.stateChanger.t
                    (iv) Apply DVR starting from router r.
                    (v) Resume NetworkLayerServer.stateChanger.t

            3(b) If, while forwarding, a router x receives the packet from router y,
                    but routingTableEntry shows Constants.INFTY distance from x to y,
                    (i) Update the entry with distance 1
                    (ii) Block NetworkLayerServer.stateChanger.t
                    (iii) Apply DVR starting from router x.
                    (iv) Resume NetworkLayerServer.stateChanger.t

        4. If 3(a) occurs at any stage, packet will be dropped,
            otherwise successfully sent to the destination router
        */
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj); //To change body of generated methods, choose Tools | Templates.
    }
}
