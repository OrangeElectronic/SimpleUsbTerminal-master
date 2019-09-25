package de.kai_morich.simple_usb_terminal.EventBus;

public class Tx {
    byte[] reback=new byte[0];
    public Tx(byte[] reback){
        this.reback=reback;
    }

    public byte[] getReback() {
        return reback;
    }
}
