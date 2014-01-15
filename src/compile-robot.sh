rm balle/brick/*.class 
rm balle/bluetooth/*.class
rm balle/bluetooth/messages/*.class
rm balle/controller/BluetoothController.class

nxjc balle/bluetooth/messages/*.java balle/controller/Controller.java balle/brick/BrickController.java balle/brick/Roboto.java balle/brick/pid/PIDConstants.java balle/brick/pid/GoStraight.java \
&& nxjlink balle.brick.Roboto -o Roboto.nxj \
&& nxjlink balle.brick.pid.PIDConstants -o PIDConstants.nxj \
&& nxjlink balle.brick.pid.GoStraight -o GoStraight.nxj \
&& nxjpcc balle/bluetooth/Communicator.java balle/controller/BluetoothController.java balle/bluetooth/TestBluetooth.java balle/brick/pid/PIDConstants.java \

