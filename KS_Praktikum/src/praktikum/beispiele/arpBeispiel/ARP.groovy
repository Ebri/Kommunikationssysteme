package praktikum.beispiele.arpBeispiel
import jpcap.*
import jpcap.packet.*

/**
 * User: root
 * Date: 09.01.13
 * Time: 18:24
 */
public class ARP {


    static NetworkInterface getNetworkInterface(InetAddress ip) throws IllegalArgumentException {
        NetworkInterface[] devices = JpcapCaptor.getDeviceList();

        for (NetworkInterface d : devices) {
            for (NetworkInterfaceAddress addr : d.addresses) {
                if (addr.address instanceof Inet4Address) {
                    byte[] byteIP = ip.getAddress();
                    byte[] subnet = addr.subnet.getAddress();
                    byte[] byteIfaceAddress = addr.address.getAddress();
                    for (int i=0; i < 4; i++){
                        byteIP[i] = (byte)(byteIP[i]&subnet[i]);
                        byteIfaceAddress[i] = (byte)(byteIfaceAddress[i]&subnet[i]);
                    }
                    if (Arrays.equals(byteIP,byteIfaceAddress)) {
                        return d;
                    }
                }
            }
        }
        throw new IllegalArgumentException(ip + "is not a local address");
    }

    static InetAddress getOwnIP(NetworkInterface device) {
        for (NetworkInterfaceAddress addr : device.addresses) {
            if (addr.address instanceof Inet4Address) {
                return addr.address;
            }
        }
        return null;
    }

    static void request(InetAddress ip, JpcapSender sender) {
        NetworkInterface device = getNetworkInterface(ip);

        InetAddress srcip = getOwnIP(device);

        byte[] broadcast = [(byte) 255, (byte) 255, (byte) 255,(byte) 255, (byte) 255, (byte) 255];

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

        EthernetPacket ether=new EthernetPacket();
        ether.frametype=EthernetPacket.ETHERTYPE_ARP;
        ether.src_mac=device.mac_address;
        ether.dst_mac=broadcast;
        arp.datalink = ether;

        sender.sendPacket(arp);
    }

    static void reply(InetAddress ip, byte[] macAddress, JpcapSender sender) {
        NetworkInterface device = getNetworkInterface(ip);
        InetAddress ownIP = getOwnIP(device);

        ARPPacket arp = new ARPPacket();
        arp.hardtype = ARPPacket.HARDTYPE_ETHER;
        arp.prototype = ARPPacket.PROTOTYPE_IP;
        arp.operation = ARPPacket.ARP_REPLY;
        arp.hlen = 6;
        arp.plen = 4;
        arp.sender_hardaddr = device.mac_address;
        arp.sender_protoaddr = ownIP.getAddress();
        arp.target_hardaddr = macAddress;
        arp.sender_protoaddr = ip.getAddress();

        EthernetPacket ether = new EthernetPacket();
        ether.frametype = EthernetPacket.ETHERTYPE_ARP;
        ether.src_mac = device.mac_address;
        ether.dst_mac = macAddress;
        arp.datalink = ether;

        sender.sendPacket(arp);
    }

}

