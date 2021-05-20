import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

//Work needed
public class Client {
    public static void main(String[] args) throws InterruptedException {
        NetworkUtility networkUtility = new NetworkUtility("127.0.0.1", 4444);
        System.out.println("Connected to server");

        float dropCount = 0;
        float hopCount = 0;

        EndDevice endDevice = (EndDevice)networkUtility.read();
        //System.out.println(endDevice.getIpAddress() +"  " + endDevice.getDeviceID() +" "+ endDevice.getGateway());

        ArrayList<EndDevice> endDevicesList = (ArrayList<EndDevice>)networkUtility.read();
        Map<Integer, Integer> deviceIDtoRouterID1 =  (Map<Integer, Integer>)networkUtility.read();
        ArrayList<Router> routersList = (ArrayList<Router>)networkUtility.read();
        //System.out.println(routersList.size());

        for (int i = 0; i < 100; i++ ){
            Random random = new Random();
            EndDevice temp =  endDevicesList.get(random.nextInt(endDevicesList.size()));
            IPAddress dest = temp.getIpAddress();

            //ensures activate destination
            int temp2 = deviceIDtoRouterID1.get(endDevice.getDeviceID());
            while (!routersList.get(temp2-1).getState()){
                System.out.println("-----State was off.");
                temp = endDevicesList.get(random.nextInt(endDevicesList.size()));
                dest = temp.getIpAddress();
                temp2 = deviceIDtoRouterID1.get(temp.getDeviceID());
            }
            //System.out.println(temp2);

            Packet packet = new Packet("Hello"+i,"",endDevice.getIpAddress(),dest);

            String retMsg1;
            if(i != 20){
                networkUtility.write(packet);

                retMsg1 = (String) networkUtility.read();
                System.out.println(retMsg1);
                if(retMsg1.equals("dropped packet") ){
                    dropCount++;
                }
                else {
                    int h = (int)networkUtility.read();
                    hopCount += h;
                    System.out.println("Hop Count: " + h);
                }
            }
            else
            {
                packet.setSpecialMessage("SHOW_ROUTE");
                networkUtility.write(packet);

                retMsg1 = (String) networkUtility.read();
                System.out.println(retMsg1);
                if(retMsg1.equals("dropped packet"))
                    dropCount++;
                else {
                    int h = (int)networkUtility.read();
                    hopCount += h;
                    System.out.println("Hop Count: " + h);
                    System.out.println("-----Route----- :"+ (String)networkUtility.read());
                }
            }
        }
        System.out.println("Avg no of hops: "+ hopCount/100);
        System.out.println("Drop rate :" + dropCount+"%");
        /*
        1. Receive EndDevice configuration from server
        2. Receive active client list from server
        3. for(int i=0;i<100;i++)
        4. {
        5.      Generate a random message
        6.      Assign a random receiver from active client list
        7.      if(i==20)
        8.      {
        9.            Send the message and recipient IP address to server and a special request "SHOW_ROUTE"
        10.           Display routing path, hop count and routing table of each router [You need to receive
                            all the required info from the server in response to "SHOW_ROUTE" request]
        11.     }
        12.     else
        13.     {
        14.           Simply send the message and recipient IP address to server.
        15.     }
        16.     If server can successfully send the message, client will get an acknowledgement along with hop count
                    Otherwise, client will get a failure message [dropped packet]
        17. }
        18. Report average number of hops and drop rate
        */
    }
}
