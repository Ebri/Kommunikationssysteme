#!/usr/bin/env groovy
// Kopiert von praktikum.beispiele.beispiel1.httpBeispiel

package praktikum.beispiele.arpBeispiel

class arpBeispiel {

    //========================================================================================================//
    // Konfiguration der Ablaufumgebung ANFANG
    //========================================================================================================//

    /** Name der Konfigurationsdatei für die Ablaufumgebung */
    static final String confFile = "env.conf"

    /** Hier muß eine der Ablaufumgebungen eingestellt werden, die in der Konfigurationsdatei "env.conf"
     * definiert sind (z.B. "home" oder "uni").
     * Der Eintrag wird durch den optionalen Kommandozeilenparameter "e" überschrieben.
     * Beim Start mit start.sh <br/>
     * . start.sh [-e environment]<br/>
     * oder als jar-file:<br/>
     * unzip KS_Praktikum.jar; . startbin.sh [-e environment]
     */
    static String environment = "wlan"

    /** Pfad zur Konfigurationsdatei für die Ablaufumgebung <br/>
     * Der Eintrag wird durch den Kommandozeilenparameter "p" überschrieben.<br/>
     * */
    static String confFileName = "KS_Praktikum/src/praktikum/beispiele/"

    //========================================================================================================//
    // Konfiguration der Ablaufumgebung ENDE
    //========================================================================================================//

    static void main(String[] args) {
        boolean debug = false

        // Behandlung von Kommandozeilenoptionen
        def cli = new CliBuilder(usage: 'java | groovy ... arpBeispiel:')
        cli.h(required: false, 'Hilfe anzeigen')
        cli.e(required: false, args: 1, 'Name der zu verwendenden Ablaufumgebung')
        cli.p(required: false, args: 1, "Pfad zu \'env.conf\', also z.B. \'praktikum/beispiele/\', nur beim Start von der Kommandozeile")
        cli.d(required: false, args: 1, "Debug-Ausgaben aktivieren")
        cli.t(required: true, args: 1, "IP-Adresse, der ein ARP-Request geschickt werden soll")

        // Kommandozeilenparameter parsen
        def options = cli.parse(args)

        // Hilfe anzeigen
        if (options.h) {
            cli.usage()
            return
        }

        if (options.d) {
            debug = true
        }

        // Ablaufumgebung setzen
        if (options.e) {
            environment = options.e
        }

        // Pfad zur Konfigurationsdatei der Ablaufumgebungen setzen
        confFileName += confFile
        if (options.p) {
            confFileName = options.p + confFile
        }

        File confFile = new File(confFileName)
        ConfigObject config = new ConfigSlurper(environment).parse(confFile.toURL())

        // ------------------------------------------------------------------------- //

        // Den Netzwerkstack initialisieren
        ARP arp = new ARP(config.deviceName, config.ownMacAddress,
                config.ownIPAddress, debug)

        arp.start()

        // no need to stop the software, it can be quit with ctrl-c

        sleep(3000)

        println("*** Control: Sending ARP request for $options.t")
        if (arp.readArpCache(options.t) == null) {
            arp.request(options.t)
        }
    }
}

//========================================================================================================//
// HttpBeispiel-Klasse ENDE
//========================================================================================================//

//========================================================================================================//
// Hauptprogramm ENDE
//========================================================================================================//
