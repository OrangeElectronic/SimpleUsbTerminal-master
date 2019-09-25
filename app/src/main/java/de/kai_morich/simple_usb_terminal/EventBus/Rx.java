package de.kai_morich.simple_usb_terminal.EventBus;

public class Rx {
    byte[] reback=new byte[0];
    public Rx(byte[] reback){
        this.reback=reback;
    }

    public byte[] getReback() {
        return reback;
    }
}
