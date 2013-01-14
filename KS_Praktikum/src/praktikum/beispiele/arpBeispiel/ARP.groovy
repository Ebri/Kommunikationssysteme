package praktikum.beispiele.arpBeispiel
import jpcap.*
import jpcap.packet.*



/**
 * User: root
 * Date: 09.01.13
 * Time: 18:24
 */
public class ARP {

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
        arp.sender_protoaddr = ip.getAddress();

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

