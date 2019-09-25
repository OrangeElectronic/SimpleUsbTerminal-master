package de.kai_morich.simple_usb_terminal.TestPackage;

import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;

import java.util.ArrayList;

import de.kai_morich.simple_usb_terminal.R;
import de.kai_morich.simple_usb_terminal.TerminalFragment;

import static de.kai_morich.simple_usb_terminal.TestPackage.FormatConvert.bytesToHex;


public class RxCommand {
    //    public static String GetText(byte data[]){
//
//    }
    public static ArrayList<String> A0X10(byte data[]){
if(data.length>21){
    String a=bytesToHex(data);
    a=a.substring(10,a.length()-6);
    ArrayList<String> re=new ArrayList<String>();
    for(int i=0;i<a.length()/28;i++){
        String SENSOR_MODEL=a.substring(i*28+2,i*28+6);
        String APP_VERSION=a.substring(i*28+6,i*28+8);
        String LIB_VERSION=a.substring(i*28+12,i*28+14);
        String STATION=a.substring(i*28+26,i*28+28);
        re.add(SENSOR_MODEL);
        re.add(APP_VERSION);
        re.add(LIB_VERSION);
        re.add(STATION);
    }
    return re;
}else{
    String a=bytesToHex(data);
    String SENSOR_MODEL=a.substring(12,16);
    String APP_VERSION=a.substring(16,18);
    String LIB_VERSION=a.substring(22,24);
    ArrayList<String> re=new ArrayList<String>();
    re.add(SENSOR_MODEL);
    re.add(APP_VERSION);
    re.add(LIB_VERSION);
    return re;
}


    }
    public static String RX(byte[] data, TerminalFragment activity){
        activity.command.Rx=bytesToHex(data);
        if(data.length==21&&data[2]==0x10&&data[20]==0x0A){
            ArrayList<String> A0X10=A0X10(data);
            String spn ="SensorModel:"+A0X10.get(0)+"\nAppVersion:"+A0X10.get(1)+"\nLib:"+A0X10.get(2);
            activity.command.SensorModel=A0X10.get(0);
            activity.command.AppVersion=A0X10.get(1);
            activity.command.Lib=A0X10.get(2);
            return spn; }
        if(data.length>21&&data[1]==(byte)0xFE&&data[data.length-1]==(byte)0x0A&&data[2]==0x10){
            ArrayList<String> A0X10=A0X10(data);
            String spn ="";
            String tmpSensorModel="";
            String tmpAppVersion="";
            String tmpLib="";
           for(int i=0;i<A0X10.size()/4;i++){
               spn=spn+"SensorModel:"+A0X10.get(i*4)+"\nAppVersion:"+A0X10.get(i*4+1)+"\nLib:"+A0X10.get(i*4+2)+"\n"+"Station:"+A0X10.get(i*4+3)+"\n\n";
               if(i==0){
                   tmpSensorModel=A0X10.get(0);
                   tmpAppVersion=A0X10.get(1);
                   tmpLib=A0X10.get(2);
               }else{
                   if(!tmpSensorModel.equals(A0X10.get(i*4))&& !tmpAppVersion.equals(A0X10.get(i*4+1))&& !tmpLib.equals(A0X10.get(i*4+2))){
                       tmpSensorModel="noequal";
                       tmpAppVersion="noequal";
                       tmpLib="noequal";
                       spn=spn+"SensorModel:"+A0X10.get(i*4)+"\nAppVersion:"+A0X10.get(i*4+1)+"\nLib:"+A0X10.get(i*4+2)+"\n"+"Station:"+A0X10.get(i*4+3)+"\n不一樣\n";
                   }
               }
           }
            activity.command.SensorModel=tmpSensorModel;
            activity.command.AppVersion=tmpAppVersion;
            activity.command.Lib=tmpLib;
            return spn;
        }

        return bytesToHex(data);
    }

}
