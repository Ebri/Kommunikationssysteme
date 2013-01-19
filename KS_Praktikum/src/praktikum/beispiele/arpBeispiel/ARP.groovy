package praktikum.beispiele.arpBeispiel
import praktikum.beispiele.utils.Utils
import jpcap.*
import jpcap.packet.*
import java.util.concurrent.LinkedBlockingQueue

/**
 * User: root
 * Date: 09.01.13
 * Time: 18:24
 */
public class ARP implements PacketReceiver{

    //Jpcap devices
    private JpcapCaptor captor
    private JpcapSender sender

    //threads
    Thread receiverThread
    Thread queueThread

    boolean stopThreads = false
    
    boolean debug = false

    // the network interface that jpcap will listen on
    // passed to the constructor
    String iface

    String ownMacAddress
    String ownIPAddress

    InetAddress ownInetAddress

    /** Broadcast MAC-Adresse */
    final String broadcastMacAddress = "ff:ff:ff:ff:ff:ff"

    LinkedBlockingQueue<Packet> recvQueue = new LinkedBlockingQueue()

    /*
    Constructor
     */
    ARP(String iface, String ownMacAddress, String ownIPAddress, boolean debug = false){
       this.iface = iface
        this.ownMacAddress = ownMacAddress
        this.debug = debug
        this.ownIPAddress = ownIPAddress
        // binary representation
        ownInetAddress = InetAddress.getByAddress(Utils.ipToByteArray(ownIPAddress))
    }

    /**
     * starts threads
     */
    public void start() {
        // when the program receives a SIGTEM (for example from a ctrl-c
        // in the command line), it will finish it's threads cleanly
        Runtime.addShutdownHook {stop()}

        captor = JpcapCaptor.openDevice(Utils.getDevice(iface),65535,false,20);
        captor.setFilter("arp",true);
        sender=captor.getJpcapSenderInstance();
        // starting the receiver thread, passing *this* as packet receiver
        // this means we have to implement public void receivePacket(Packet p)
        receiverThread = Thread.start { captor.loopPacket(-1, this) }
        queueThread = Thread.start { processQueues() }
        debug("[*] started jpcap receiver thread for Mac Address $ownMacAddress")
    }

    /**
     * stops threads
     */
    public void stop() {
        // this tells our processQueues thread to stop
        stopThreads = true
        // this tells the jpcap receiver thread to stop
        captor.breakLoop()
        debug("\n[*] waiting for threads to stop (1000ms)...")
        sleep(1000)

        // kill threads that are still alive
        if (receiverThread != null && receiverThread.alive) receiverThread.destroy()
        if (queueThread != null && queueThread.alive) queueThread.destroy()
        debug("[*] killed remaining threads.")
    }
    
    public void debug(String message) {
        if (debug) {
            println(message)
        }
    }

    public void receivePacket(Packet p) {
        debug("[R] received a packet from jpcap: " + p.toString())

        EthernetPacket recvFrame = p.datalink as EthernetPacket
        // Vergleich der Ziel-MAC-Adresse
        if (!(recvFrame.destinationAddress == ownMacAddress ||
                recvFrame.destinationAddress == broadcastMacAddress)) {
            // Paket nicht an uns und nicht an Broadcast addressiert
            debug("    [!] discarding: ethernet address mismatch")
            return
        }
        if (recvFrame.frametype != EthernetPacket.ETHERTYPE_ARP) {
            // kein ARP-Paket
            debug("    [!] discarding: not an arp packet")
            return
        }

        debug("    [*] matches our criteria => putting it in the receive queue")
        recvQueue.put(p as ARPPacket)
        debug("    [.] queue length is now " + recvQueue.size())
    }

    private void processQueues() {
        debug("[*] queue thread has started.")
        while(!stopThreads) {
            if (!recvQueue.isEmpty()) {
                debug("[*] processing an item in the receive queue")
                processARPPacket(recvQueue.take() as ARPPacket)
            }
            sleep(40)
        }
    }

    private void processARPPacket(ARPPacket p) {
        // at this point we already know that this packet is addressed either
        // to us or to broadcast, so it concerns us.

        InetAddress target_protoaddr = InetAddress.getByAddress(p.target_protoaddr)
        debug("[.] target_hardaddr is $p.target_hardaddr")
        debug("[.] target_protoaddr is $target_protoaddr")
        debug("[.] ownInetAddress is $ownInetAddress")
        if (p.operation == ARPPacket.ARP_REQUEST &&
            p.targetProtocolAddress == ownInetAddress) {
            // this is an ARP request, and it requests our MAC
            // we will send a reply
            println("[*] ARP request from $p.senderProtocolAddress ($p.senderHardwareAddress)")
            reply(p.senderProtocolAddress, p.sender_hardaddr, sender)
            return
        }

        if (p.operation == ARPPacket.ARP_REPLY &&
            p.targetHardwareAddress == ownMacAddress) {
            // this is an ARP reply, and it is for us
            println("[*] ARP reply from $p.senderProtocolAddress ($p.senderHardwareAddress)")
        }
    }


    // meines Erachtens ist diese Methode obsolet (wird durch ARP.request ersetzt)
    /**
     * requests the mac-adress from the connected lan-device
     * @param ip IP for which the MAC is to be requested
     * @return the MAC-Adress to the given ip
     * @throws IOException
     */
    public byte[] arpRequest(InetAddress ip) throws java.io.IOException{
        //find network interface
        NetworkInterface[] devices=JpcapCaptor.getDeviceList();
        NetworkInterface device=null;

loop:	for(NetworkInterface d:devices){
            for(NetworkInterfaceAddress addr:d.addresses){
                if(!(addr.address instanceof Inet4Address)) continue;
                byte[] bip=ip.getAddress();
                byte[] subnet=addr.subnet.getAddress();
                byte[] bif=addr.address.getAddress();
                for(int i=0;i<4;i++){
                    bip[i]=(byte)(bip[i]&subnet[i]);
                    bif[i]=(byte)(bif[i]&subnet[i]);
                }
                if(Arrays.equals(bip,bif)){
                    device=d;
                    break loop;
                }
            }
        }

        if(device==null)
            throw new IllegalArgumentException(ip+" is not a local address");



        InetAddress srcip=null;
        for(NetworkInterfaceAddress addr:device.addresses)
            if(addr.address instanceof Inet4Address){
                srcip=addr.address;
                break;
            }

        byte[] broadcast = [(byte) 255, (byte) 255, (byte) 255,(byte) 255, (byte) 255, (byte) 255];

        ARPPacket arp=new ARPPacket();
        arp.hardtype=ARPPacket.HARDTYPE_ETHER;
        arp.prototype=ARPPacket.PROTOTYPE_IP;
        arp.operation=ARPPacket.ARP_REQUEST;
        arp.hlen=6;
        arp.plen=4;
        arp.sender_hardaddr=device.mac_address;
        arp.sender_protoaddr=srcip.getAddress();
        arp.target_hardaddr=broadcast;
        arp.target_protoaddr=ip.getAddress();

        EthernetPacket ether=new EthernetPacket();
        ether.frametype=EthernetPacket.ETHERTYPE_ARP;
        ether.src_mac=device.mac_address;
        ether.dst_mac=broadcast;
        arp.datalink=ether;

        sender.sendPacket(arp);

        while(true){
            ARPPacket p=(ARPPacket)captor.getPacket();
            if(p==null){
                throw new IllegalArgumentException(ip+" is not a local address");
            }
            if(Arrays.equals(p.target_protoaddr,srcip.getAddress())){
                return p.sender_hardaddr;
            }
        }
    }


    /**
     * Findet das NetzwerkInterface über welches die gegebene IP-Adresse erreichbar ist.
     *
     * @param ip Die IP zur gesuchen MAC
     * @return Das gesuchte NetzwerkInterface
     * @throws IllegalArgumentException wenn keine gültige (nicht in einem angeschlossenen Subnetz zu findende) IP angegeben wurde.
     */
    static NetworkInterface getNetworkInterface(InetAddress ip) throws IllegalArgumentException {
        NetworkInterface[] devices = JpcapCaptor.getDeviceList();

        for (NetworkInterface d : devices) {
            for (NetworkInterfaceAddress addr : d.addresses) {
                if (addr.address instanceof Inet4Address) {
                    byte[] byteIP = ip.getAddress();
                    byte[] subnet = addr.subnet.getAddress();
                    byte[] byteIfaceAddress = addr.address.getAddress();

                    // Es wird der Netzwerkteil der IP berechnet.
                    for (int i=0; i < 4; i++){
                        byteIP[i] = (byte)(byteIP[i]&subnet[i]);
                        byteIfaceAddress[i] = (byte)(byteIfaceAddress[i]&subnet[i]);
                    }

                    // Prüfen, ob das Netzwerk das gleiche ist.
                    if (Arrays.equals(byteIP,byteIfaceAddress)) {
                        return d;
                    }
                }
            }
        }
        throw new IllegalArgumentException(ip + "is not a local address");
    }

    /**
     * Gibt die eigene IP Adresse an dem NetzwerkInterface zurück
     * @param device Das Interface zu welchem die IP gesucht wird.
     * @return die gesuchte Adresse oder null wenn keine gefunden wurde.
     */
    static InetAddress getOwnIP(NetworkInterface device) {
        for (NetworkInterfaceAddress addr : device.addresses) {
            if (addr.address instanceof Inet4Address) {
                return addr.address;
            }
        }
        return null;
    }

    void request(String ip) {
        request(InetAddress.getByAddress(Utils.ipToByteArray(ip)), sender)
    }

    /**
     * Sendet einen ARP-Request an die angegebene IP Adresse.
     * @param ip Die IP zu der die MAC Adresse gesucht wird.
     * @param sender Die Senderinstanz von Jpcap.
     */
    static void request(InetAddress ip, JpcapSender sender) {

        //Suche das Interface welches die gegebene IP erreicht.
        NetworkInterface device = getNetworkInterface(ip);

        //Suche die eigene IP an dem Interface.
        InetAddress srcip = getOwnIP(device);

        byte[] broadcast = [(byte) 255, (byte) 255, (byte) 255,(byte) 255, (byte) 255, (byte) 255];


        // Erstelle das ARP Packet
        ARPPacket arp = new ARPPacket();
        arp.hardtype=ARPPacket.HARDTYPE_ETHER;
        arp.prototype=ARPPacket.PROTOTYPE_IP;
        arp.operation=ARPPacket.ARP_REQUEST;
        arp.hlen=6;
        arp.plen=4;
        arp.sender_hardaddr=device.mac_address;
        arp.sender_protoaddr=srcip.getAddress();
        arp.target_hardaddr=broadcast;
        arp.target_protoaddr=ip.getAddress();

        //Erstelle das Ethernet Packet (Schicht 2)
        EthernetPacket ether=new EthernetPacket();
        ether.frametype=EthernetPacket.ETHERTYPE_ARP;
        ether.src_mac=device.mac_address;
        ether.dst_mac=broadcast;
        arp.datalink = ether;

        //Sendet das Packet über die angegebene Jpcap Instanz.
        // TODO: laut ethercap geht das Paket korrekt raus, wir empfangen aber keine Antwort
        println("[*] sending ARP request packet to $ip")
        println("    sender_hardaddr = $arp.senderHardwareAddress")
        println("    sender_protoaddr = $arp.senderProtocolAddress")
        sender.sendPacket(arp);
    }

    /**
     * Sendet einen ARP Reply
     * @param ip Die IP an den der Reply gesendet werden soll.
     * @param macAddress die Mac Adresse des Empfängers.
     * @param sender Die Senderinstanz von Jpcap.
     */
    static void reply(InetAddress ip, byte[] macAddress, JpcapSender sender) {
        //Finde das Interface über welches die angegebene IP erreichbar ist.
        NetworkInterface device = getNetworkInterface(ip);
        //Finde die eigene IP
        InetAddress ownIP = getOwnIP(device);


        //Erstelle das ARP Packet.
        ARPPacket arp = new ARPPacket();
        arp.hardtype = ARPPacket.HARDTYPE_ETHER;
        arp.prototype = ARPPacket.PROTOTYPE_IP;
        arp.operation = ARPPacket.ARP_REPLY;
        arp.hlen = 6;
        arp.plen = 4;

        //Die eigene MAC-Adresse die dem Empfänger gesendet wird.
        arp.sender_hardaddr = device.mac_address;
        arp.sender_protoaddr = ownIP.getAddress();
        arp.target_hardaddr = macAddress;
        arp.target_protoaddr = ip.getAddress();

        //Erstelle das Ethernet Packet (Schicht 2)
        EthernetPacket ether = new EthernetPacket();
        ether.frametype = EthernetPacket.ETHERTYPE_ARP;
        ether.src_mac = device.mac_address;
        ether.dst_mac = macAddress;
        arp.datalink = ether;

        //Sende das Packet über die angegebene Jpcap Senderinstanz.
        sender.sendPacket(arp);
    }

}

