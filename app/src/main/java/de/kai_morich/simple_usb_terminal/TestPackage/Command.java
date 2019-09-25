package de.kai_morich.simple_usb_terminal.TestPackage;

import android.content.Context;
import android.support.v4.app.Fragment;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;

import de.kai_morich.simple_usb_terminal.SerialSocket;
import de.kai_morich.simple_usb_terminal.TerminalFragment;

import static de.kai_morich.simple_usb_terminal.TestPackage.FormatConvert.StringHexToByte;
import static de.kai_morich.simple_usb_terminal.TestPackage.FormatConvert.bytesToHex;
import static de.kai_morich.simple_usb_terminal.TestPackage.FormatConvert.getBit;


import android.app.Activity;
import android.support.v4.app.Fragment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;



public class Command {
    public  SerialSocket socket;
    public  String SensorModel="nodata";
    public  Fragment terminal;
    public  String AppVersion="nodata";
    public  String Lib="nodata";
    public String Rx="";
    public int IC=0;
    public String ID="";
    public String ID2="";
    public ArrayList<String>  FALSE_CHANNEL=new ArrayList<>();
//    read IC ID and Num and status
    public boolean Command12(int ic,int channel,String id){
        try{
            int check=30;
            Rx="";
            String commeand="0ASS120008CCIDXXXXF5".replace("SS",bytesToHex(new byte[]{(byte)ic})).replace("CC",bytesToHex(new byte[]{(byte)channel})).replace("ID",id);
            socket.write(StringHexToByte(getCRC16(commeand)),check);
            SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
            Date past=sdf.parse(sdf.format(new Date()));
            int fal=0;
            while(true){
                Date now=sdf.parse(sdf.format(new Date()));
                double time=getDatePoor(now,past);
                if(time>2){
                    socket.write(StringHexToByte(getCRC16(commeand)),check);
                    past=sdf.parse(sdf.format(new Date()));
                    fal++;
                }
                if(fal>3){return false;}
                if(Rx.length()==check){
                    boolean g=checkcommand(Rx.substring(10,12));
                    if(g){ID=Rx.substring(14,22);}
                    return g;
                }
            }
        }catch (Exception e){e.printStackTrace();
            return false;}
    }
public boolean Command03(){
    try{
        int check=22;
        Rx="";
        socket.write(StringHexToByte(getCRC16("0AFE03000754504D539CC8F5")),check);
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        Date past=sdf.parse(sdf.format(new Date()));
        int fal=0;
        while(true){
            Date now=sdf.parse(sdf.format(new Date()));
            double time=getDatePoor(now,past);
            if(time>2){
                socket.write(StringHexToByte(getCRC16("0AFE03000754504D539CC8F5")),check);
                past=sdf.parse(sdf.format(new Date()));
                fal++;
            }
            if(fal>3){return false;}
            if(Rx.length()==check){
         IC=((int)StringHexToByte(Rx.substring(12,14))[0])/2;
                return true;
            }
        }
    }catch (Exception e){e.printStackTrace();
        return false;}
}
    public boolean Command_11(int ic,int channel){
        try{
            int check=30;
            Rx="";
            String commeand="0ASS110004CCXXXXF5".replace("SS",bytesToHex(new byte[]{(byte)ic})).replace("CC",bytesToHex(new byte[]{(byte)channel}));
            socket.write(StringHexToByte(getCRC16(commeand)),check);
            SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
            Date past=sdf.parse(sdf.format(new Date()));
            int fal=0;
            while(true){
                Date now=sdf.parse(sdf.format(new Date()));
                double time=getDatePoor(now,past);
                if(time>2){
                    socket.write(StringHexToByte(getCRC16(commeand)),check);
                    past=sdf.parse(sdf.format(new Date()));
                    fal++;
                }
                if(fal==1){return false;}
                if(Rx.length()==check){
                    boolean g=checkcommand(Rx.substring(10,12));
                    if(g){ID=Rx.substring(14,22);}
                    return g;
                }
            }
        }catch (Exception e){e.printStackTrace();
            return false;}
    }
    public boolean Command10_00(){
        String tmp="0A0010000754504D53A0ADF5";
        try{
            socket.write(StringHexToByte(getCRC16(tmp)),0);
            return true;
        }catch (Exception e){e.printStackTrace();return false;}
    }
    public boolean Command10_01(){
        String tmp="0A0110000754504D53E77EF5";
        try{
            socket.write(StringHexToByte(getCRC16(tmp)),0);
            return true;
        }catch (Exception e){e.printStackTrace();return false;}
    }
    public boolean Command10_FE(){
        try{
            int check=(14*IC+8)*2;
            socket.write(StringHexToByte(getCRC16("0AFE10000754504D537331F5")),check);
            SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
            Date past=sdf.parse(sdf.format(new Date()));
            int fal=0;
            while(true){
                Date now=sdf.parse(sdf.format(new Date()));
                double time=getDatePoor(now,past);
                if(time>2){
                    socket.write(StringHexToByte(getCRC16("0AFE10000754504D537331F5")),check);
                    past=sdf.parse(sdf.format(new Date()));
                    fal++;
                }
                if(fal>3){return false;}
                if(Rx.length()==check&&!SensorModel.equals("nodata")&&!AppVersion.equals("nodata")&&!Lib.equals("nodata")){
                    boolean g=true;
                    for(int i=0;i<IC;i++){
                        String tmp=Rx.substring(10,Rx.length()-6);
                        if(!checkcommand(tmp.substring(i*28,i*28+2))){g=false;

                        }
                    }
                    return g;
                }
            }
        }catch (Exception e){e.printStackTrace();
            return false;}
    }

    public boolean Command14(){
        try{
            int check=(8+6*IC)*2;
            socket.write(StringHexToByte(getCRC16("0AFE14000D4F52414E474554504D53XXXXF5")),check);
            SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
            Date past=sdf.parse(sdf.format(new Date()));
            int fal=0;
            while(true){
                Date now=sdf.parse(sdf.format(new Date()));
                double time=getDatePoor(now,past);
                if(time>2){
                    socket.write(StringHexToByte(getCRC16("0AFE14000D4F52414E474554504D53XXXXF5")),check);
                    past=sdf.parse(sdf.format(new Date()));
                    fal++;
                }
                if(fal>3){return false;}
                if(Rx.length()==check){
                    boolean g=true;
                for(int i=0;i<IC;i++){
                    String tmp=Rx.substring(10,Rx.length()-6);
                    if(checkcommand(tmp.substring(i*12,i*12+2))){g=false;}
                }
                return g;
                }
            }
        }catch (Exception e){e.printStackTrace();
            return false;}
    }
 public boolean checkcommand(String a){
  if(getBit(StringHexToByte(a)[0]).substring(7,8).equals("0")){return true;}else{return false;}
 }
    public boolean Command17(){
        try{
            int check=(8+7*(IC-1))*2;
            socket.write(StringHexToByte(getCRC16("0AFE1700094F52414E4745A7C4F5")),check);
            SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
            Date past=sdf.parse(sdf.format(new Date()));
            int fal=0;
            while(true){
                Date now=sdf.parse(sdf.format(new Date()));
                double time=getDatePoor(now,past);
                if(time>5){
                    socket.write(StringHexToByte(getCRC16("0AFE1700094F52414E4745A7C4F5")),check);
                    past=sdf.parse(sdf.format(new Date()));
                    fal++;
                }
                if(fal>3){return false;}
                if(Rx.length()==check){
                    boolean g=true;
                    for(int i=0;i<IC-1;i++){
                        String tmp=Rx.substring(10,Rx.length()-6);
                        if(!checkcommand(tmp.substring(i*14,i*14+2))){
                            g=false;
                        }
                    }
                    return g;
                }
            }
        }catch (Exception e){e.printStackTrace();
            return false;}
    }
    public boolean Command15(){
        try{
            Rx="";
            FALSE_CHANNEL=new ArrayList<>();
            int check=(8+(7*2*IC))*2;
            socket.write(StringHexToByte(getCRC16("0AFE1500080154504D534505F5")),check);
            SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
            Date past=sdf.parse(sdf.format(new Date()));
            int fal=0;
            while(true){
                Date now=sdf.parse(sdf.format(new Date()));
                double time=getDatePoor(now,past);
                if(time>15){
                    socket.write(StringHexToByte(getCRC16("0AFE1500080154504D534505F5")),check);
                    past=sdf.parse(sdf.format(new Date()));
                    fal++;
                }
                if(fal>3){return false;}
                if(Rx.length()==check){
                    boolean g=true;
                    for(int i=0;i<IC*2;i++){
                        String tmp=Rx.substring(10,Rx.length()-6);
                        if(!checkcommand(tmp.substring(i*14,i*14+2))){g=false;
                            FALSE_CHANNEL.add(tmp.substring(i*14+2,i*14+4));
                        }
                    }
                    return g;
                }
            }
        }catch (Exception e){e.printStackTrace();
            return false;}
    }
//Clear APP端 sensor code
    public  boolean ClearSensor(){
        try{
            socket.write(StringHexToByte(getCRC16("0A0014000D4F52414E474554504D53XXXXF5")),28);
            SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
            Date past=sdf.parse(sdf.format(new Date()));
            int fal=0;
            while(!Rx.substring(4,6).equals("14")){
                Date now=sdf.parse(sdf.format(new Date()));
                double time=getDatePoor(now,past);
                if(time>1){
                    socket.write(StringHexToByte(getCRC16("0A0014000D4F52414E474554504D53XXXXF5")),28);
                    past=sdf.parse(sdf.format(new Date()));
                    fal++;
                }
                if(fal>3){return false;}
            }
            return true;
        }catch (Exception e){e.printStackTrace();
            return false;}
    }
    //ProgramSensor
    public  String getCRC16(String source) {
        int crc = 0x0000; 		 	 // 初始值
        int polynomial = 0x1021;	         // 校验公式 0001 0000 0010 0001
        byte[] bytes = StringHexToByte(source.substring(2, source.length()-6));  //把普通字符串转换成十六进制字符串

        for (byte b : bytes) {
            for (int i = 0; i < 8; i++) {
                boolean bit = ((b >> (7 - i) & 1) == 1);
                boolean c15 = ((crc >> 15 & 1) == 1);
                crc <<= 1;
                if (c15 ^ bit)
                    crc ^= polynomial;
            }
        }
        crc &= 0xffff;
        StringBuffer result = new StringBuffer(Integer.toHexString(crc));
        while (result.length() < 4) {		 //CRC检验一般为4位，不足4位补0
            result.insert(0, "0");
        }
        return source.replace("XXXX", result.toString().toUpperCase());}

    public  String AddCommand(String data,int Long){
        StringBuffer length = new StringBuffer(Integer.toHexString(data.length()/2+5));
        while (length.length() < 4) {		 //CRC检验一般为4位，不足4位补0
            length.insert(0, "0");
        }
        StringBuffer row = new StringBuffer(Integer.toHexString(Long));
        while (row.length() < 2) {		 //CRC检验一般为4位，不足4位补0
            row.insert(0, "0");
        }
        String TmpCommand="0A0013"+length.toString()+row+data+data.substring(18, 20)+"XXXXF5";
        return getCRC16(TmpCommand);
    }
    public boolean  LogData(final String filename){
        Rx="nodata";
        try{
            InputStreamReader fr = new InputStreamReader(terminal.getResources().getAssets().open(filename));
            BufferedReader br = new BufferedReader(fr);
            StringBuilder sb = new StringBuilder();
            int ln=2048;
            while (br.ready()) {
                String s=br.readLine();
                sb.append(s);
            }
            int Long=0;
            if(sb.length()%ln == 0){Long=sb.length()/ln;
            }else{Long=sb.length()/ln+1;}
            for(int i=0;i<Long;i++){
                if(i==Long-1){  socket.write(StringHexToByte(AddCommand(sb.substring(i*ln, sb.length()),i)),28);
                }else{
                    socket.write(StringHexToByte(AddCommand(sb.substring(i*ln, i*ln+ln),i)),28);
                }
                SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
                Date past=sdf.parse(sdf.format(new Date()));
                while(!Rx.substring(4,6).equals("13")){
                    Date now=sdf.parse(sdf.format(new Date()));
                    double time=getDatePoor(now,past);
                    if(time>3){
                        return false;
                    }
                }
                Rx="nodata";
            }
            fr.close();
            return true;
        }catch (Exception e){e.printStackTrace();

            return false;}

    }
    //取得時間差
//    SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//    Date d1=sdf.parse("2019-06-12 16:26:10");
//    Date d2=sdf.parse(sdf.format(new Date()));
//     System.out.println(getDatePoor(d1,d2));
    public  int getDatePoor(Date endDate, Date nowDate) {
        long diff = endDate.getTime() - nowDate.getTime();
        long sec = diff/1000;
        return (int)sec;
    }
    public  boolean CheckS19(String filename){
        try{
            InputStreamReader fr = new InputStreamReader(terminal.getResources().getAssets().open(filename));
            BufferedReader br = new BufferedReader(fr);
            String a=br.readLine();
            String b=br.readLine();
            if(SensorModel.equals(a.substring(4, 8))&&AppVersion.equals(a.substring(8, 10))&&Lib.equals(b.substring(0, 2))){
                fr.close();
                return true;
            }else{
                fr.close();
                return false;}
        }catch (Exception e){e.printStackTrace();return false;}
    }
    public boolean ProgramStep(final String filename) {
        ClearChech();
        try {
            if(!Command03()){return false;}
            if(!Command10_FE()){return false;}
            if (CheckS19(filename)) {
                if(Command15()){return true;}else{return false;}
            } else {
                if(!Command14()){return false;}
                if(!LogData(filename)){return false;}
                if(!Command17()){return false;}
                if(Command15()){return true;}else{return false;}
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    public void ClearChech(){
        SensorModel="nodata";
        AppVersion="nodata";
        Lib="nodata";
        Rx="nodata";
    }
}
