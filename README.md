JADE IMTP communication using WebSocket
=======================================

Description:
------------

Experimental spike for building a JADE IMTP communication through WebSocket.

This is part of the [Framework codjo.net]: http://codjo.net effort

Work in progress

Features requests:
------------------
+ Standard communication
- Replicated container feature support

TODO:
-----
* Rename class ClientEngine and ServerEngine using NetworkChannel namming space....
* Use case : decode Exception (failure) unknown in the other side ?
* Use case : what if unknwon Command (Result) error during reading
* Use case : what if we can't post command/result in the websocket ?
* Use case : what if Thread executor limit is reached ? (no more response !!)
* Use case : Same Command ID in activeCommand