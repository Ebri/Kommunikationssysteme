#!/bin/bash
sudo iptables -A INPUT -p tcp --dport 33333 -j DROP
# Starten des Beispielprogramms
sudo groovy -cp "src:libs/jpcap.jar" src/praktikum/beispiele/beispiel1/HttpBeispiel.groovy  -p "src/praktikum/beispiele/" $*
