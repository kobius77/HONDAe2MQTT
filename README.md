# HONDAeInsight going HONDAe2MQTT
A WIP Android App to read Real-Time-Data off of the Honda e via ODB2 ([thanks to the work of DanielH1987](https://github.com/DanielH1987/HONDAeInsight)) and publish it via MQTT.

first tests by repurposing existing functions:
- csv written to local storage
- API key input field used for broker connection tcp://username:password@IP_ADDRESS:PORT




from the original readme:

This App is for testing purposes only! Feel free to add features, fix bugs and create pull requests.

You'll need an OBDII-Adapter that has at least 864 bytes message buffer with Bluetooth - not BLE!
A cheap ELM327-Clone won't work because of the length auf den CAN messages the e sends.

OBDII-Adapter known to be working are:
- OBDlink MX Bluetooth
- vLinker FD
- vLinker MC
- vLinker BM+
- vLinker BM

OBDII-Adapter not working:
- ELM327-Clone

Based heavily on https://github.com/harry1453/android-bluetooth-serial example App - Thanks^2!
