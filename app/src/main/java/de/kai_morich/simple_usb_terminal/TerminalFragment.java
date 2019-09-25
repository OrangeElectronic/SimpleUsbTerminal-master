package de.kai_morich.simple_usb_terminal;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;

import de.kai_morich.simple_usb_terminal.EventBus.Rx;
import de.kai_morich.simple_usb_terminal.EventBus.Tx;
import de.kai_morich.simple_usb_terminal.TestPackage.Command;

import static de.kai_morich.simple_usb_terminal.TestPackage.FormatConvert.StringHexToByte;
import static de.kai_morich.simple_usb_terminal.TestPackage.RxCommand.A0X10;
import static de.kai_morich.simple_usb_terminal.TestPackage.RxCommand.RX;

public class TerminalFragment extends Fragment implements ServiceConnection, SerialListener {

    private enum Connected { False, Pending, True }

    public static final String INTENT_ACTION_GRANT_USB = BuildConfig.APPLICATION_ID + ".GRANT_USB";

    private int deviceId, portNum, baudRate;
    private String newline = "\r\n";

    private TextView receiveText;
public Command command=new Command();
    private SerialSocket socket;
    private SerialService service;
    private boolean initialStart = true;
    private Connected connected = Connected.False;
    private BroadcastReceiver broadcastReceiver;

    public TerminalFragment() {
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction().equals(INTENT_ACTION_GRANT_USB)) {
                    Boolean granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);
                    connect(granted);
                }
            }
        };
    }

    /*
     * Lifecycle
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        command.terminal=this;
        setRetainInstance(true);
        EventBus.getDefault().register(this);
        deviceId = getArguments().getInt("device");
        portNum = getArguments().getInt("port");
        baudRate = getArguments().getInt("baud");
    }

    @Override
    public void onDestroy() {
        if (connected != Connected.False)
            disconnect();
        getActivity().stopService(new Intent(getActivity(), SerialService.class));
        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();
        if(service != null)
            service.attach(this);
        else
            getActivity().startService(new Intent(getActivity(), SerialService.class)); // prevents service destroy on unbind from recreated activity caused by orientation change
    }

    @Override
    public void onStop() {
        if(service != null && !getActivity().isChangingConfigurations())
            service.detach();
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @SuppressWarnings("deprecation") // onAttach(context) was added with API 23. onAttach(activity) works for all API versions
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        getActivity().bindService(new Intent(getActivity(), SerialService.class), this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDetach() {
        try { getActivity().unbindService(this); } catch(Exception ignored) {}
        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(broadcastReceiver, new IntentFilter(INTENT_ACTION_GRANT_USB));
        if(initialStart && service !=null) {
            initialStart = false;
            getActivity().runOnUiThread(this::connect);
        }
    }

    @Override
    public void onPause() {
        getActivity().unregisterReceiver(broadcastReceiver);
        super.onPause();
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        service = ((SerialService.SerialBinder) binder).getService();
        if(initialStart && isResumed()) {
            initialStart = false;
            getActivity().runOnUiThread(this::connect);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        service = null;
    }

    /*
     * UI
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_terminal, container, false);
        receiveText = view.findViewById(R.id.receive_text);                          // TextView performance decreases with number of spans
        receiveText.setTextColor(getResources().getColor(R.color.colorRecieveText)); // set as default color to reduce number of spans
        receiveText.setMovementMethod(ScrollingMovementMethod.getInstance());
        TextView sendText = view.findViewById(R.id.send_text);
        View sendBtn = view.findViewById(R.id.send_btn);
        sendBtn.setOnClickListener(v -> send(sendText.getText().toString()));
        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_terminal, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        command.socket=socket;
        switch (id){
            case R.id.readsensor:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        command.Command03();
                    }
                }).start();
                return true;
            case R.id.clearapp:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        command.Command10_00();
                    }
                }).start();
                return true;
            case R.id.clear:

                receiveText.setText("");
                return true;
            case R.id.newline:
                String[] newlineNames = getResources().getStringArray(R.array.newline_names);
                String[] newlineValues = getResources().getStringArray(R.array.newline_values);
                int pos = java.util.Arrays.asList(newlineValues).indexOf(newline);
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle("Newline");
                builder.setSingleChoiceItems(newlineNames, pos, (dialog, item1) -> {
                    newline = newlineValues[item1];
                    dialog.dismiss();
                });
                builder.create().show();
                return true;
            case R.id.ProgramSensor:
                new Thread(new Runnable() {
                    @Override
                    public void run() {

                    }
                }).start();
                command.Command10_01();
                return true;
            case R.id.ReadSensorID:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        command.Command10_FE();
                    }
                }).start();

                return true;
            case R.id.mainflow:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        command.Command14();
                    }
                }).start();

                return true;
            case R.id.writesensorID:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        command.Command17();
                    }
                }).start();

                return true;
            case R.id.LoadData:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        command.LogData("SI2056.s19");
                    }
                }).start();

               return true;
            case R.id.LoadData2:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        command.LogData("BEA001.s19");
                    }
                }).start();
                return true;
            case R.id.Program:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        boolean condition=command.ProgramStep("SI2056.s19");
                          handler.post(new Runnable() {
                              @Override
                              public void run() {
                                  if(condition){
                                      receiveText.append("燒錄成功");
                                  }else{
                                      for(String a:command.FALSE_CHANNEL){
                                          receiveText.append("channel"+a+"燒錄失敗\n");
                                      }

                                  }
                              }
                          });
                    }
                }).start();

                return true;
            case R.id.Command_15:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        command.Command15();
                    }
                }).start();
                return true;
            case R.id.Command_11:

                    new Thread(new Runnable() {
                        @Override
                        public void run() {

                            boolean Ch1=command.Command_11(0,1);
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
//                                    receiveText.append(command.ID);
                                    receiveText.append("\nid為"+command.ID);
                                }
                            });

                    }}).start();

                return true;
            case R.id.Command_12:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        boolean a=command.Command12(0,0,"12345678");
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                if(a){
                                    receiveText.append("\n燒錄成功id為"+command.ID);
                                }else{
                                    receiveText.append("\n燒錄失敗");
                                }
                            }
                        });

                    }
                }).start();
                return true;
        }
        if (id == R.id.clear) {
            receiveText.setText("");
            return true;
        } else if (id ==R.id.newline) {
            String[] newlineNames = getResources().getStringArray(R.array.newline_names);
            String[] newlineValues = getResources().getStringArray(R.array.newline_values);
            int pos = java.util.Arrays.asList(newlineValues).indexOf(newline);
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Newline");
            builder.setSingleChoiceItems(newlineNames, pos, (dialog, item1) -> {
                newline = newlineValues[item1];
                dialog.dismiss();
            });
            builder.create().show();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }
private Handler handler=new Handler();
    /*
     * Serial + UI
     */
    private void connect() {
        connect(null);
    }

    private void connect(Boolean permissionGranted) {
        UsbDevice device = null;
        UsbManager usbManager = (UsbManager) getActivity().getSystemService(Context.USB_SERVICE);
        for(UsbDevice v : usbManager.getDeviceList().values())
            if(v.getDeviceId() == deviceId)
                device = v;
        if(device == null) {
            status("connection failed: device not found");
            return;
        }
        UsbSerialDriver driver = UsbSerialProber.getDefaultProber().probeDevice(device);
        if(driver == null) {
            driver = CustomProber.getCustomProber().probeDevice(device);
        }
        if(driver == null) {
            status("connection failed: no driver for device");
            return;
        }
        if(driver.getPorts().size() < portNum) {
            status("connection failed: not enough ports at device");
            return;
        }
        UsbSerialPort usbSerialPort = driver.getPorts().get(portNum);
        UsbDeviceConnection usbConnection = usbManager.openDevice(driver.getDevice());
        if(usbConnection == null && permissionGranted == null) {
            if (!usbManager.hasPermission(driver.getDevice())) {
                PendingIntent usbPermissionIntent = PendingIntent.getBroadcast(getActivity(), 0, new Intent(INTENT_ACTION_GRANT_USB), 0);
                usbManager.requestPermission(driver.getDevice(), usbPermissionIntent);
                return;
            }
        }
        if(usbConnection == null) {
            if (!usbManager.hasPermission(driver.getDevice()))
                status("connection failed: permission denied");
            else
                status("connection failed: open failed");
            return;
        }

        connected = Connected.Pending;
        try {
            socket = new SerialSocket();
            service.connect(this, "Connected");
            socket.connect(getContext(), service, usbConnection, usbSerialPort, baudRate);
            // usb connect is not asynchronous. connect-success and connect-error are returned immediately from socket.connect
            // for consistency to bluetooth/bluetooth-LE app use same SerialListener and SerialService classes
            onSerialConnect();
        } catch (Exception e) {
            onSerialConnectError(e);
        }
    }

    private void disconnect() {
        connected = Connected.False;
        service.disconnect();
        socket.disconnect();
        socket = null;
    }

    public void send(String str) {
        if(connected != Connected.True) {
            Toast.makeText(getActivity(), "not connected", Toast.LENGTH_SHORT).show();
            return;
        }
        try {

            SpannableStringBuilder spn = new SpannableStringBuilder(str);
            spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorSendText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            socket.write(StringHexToByte(str),0);
            receiveText.append("TX:"+spn+"\n\n");
        } catch (Exception e) {
            onSerialIoError(e);
        }
    }

    private void receive(byte[] data) {
                  receiveText.append("RX:"+RX(data,this)+"\n\n");
    }
    private static String bytesToHex(byte[] hashInBytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : hashInBytes) {
//            System.out.println(b);
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
    private void status(String str) {
        SpannableStringBuilder spn = new SpannableStringBuilder(str+'\n');
        spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorStatusText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        receiveText.append(spn);
    }

    /*
     * SerialListener
     */
    @Override
    public void onSerialConnect() {
        status("connected");
        connected = Connected.True;
    }

    @Override
    public void onSerialConnectError(Exception e) {
        status("connection failed:   " + e.getMessage());
        disconnect();
    }

        @Override
        public void onSerialRead(byte[] data) {
            receive(data);
        }

    @Override
    public void onSerialIoError(Exception e) {
        status("connection lost: " + e.getMessage());
        disconnect();
    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void Event(Rx a){
        try{
            SpannableStringBuilder spn = new SpannableStringBuilder(bytesToHex(a.getReback()));
            spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorSendText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            receiveText.append("\nRX:"+spn+"\n\n");
        }catch (Exception e){Log.w("error",e.getMessage());}
    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void Event(Tx a){
        try{
            SpannableStringBuilder spn = new SpannableStringBuilder(bytesToHex(a.getReback()));
            spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorRecieveText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            receiveText.append("\nTX:"+spn+"\n\n");
        }catch (Exception e){Log.w("error",e.getMessage());}
    }
}
