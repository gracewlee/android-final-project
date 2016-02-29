package com.sydney.myplayers;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.net.*;

/*
    OUT
    1: sign in
    2: sign up (register)
*/

public class MainActivity extends AppCompatActivity {
    protected static String ipAddress = "192.168.2.21";//"10.0.3.2"; //192.168.2.21
    protected static Socket clientSocket;
    private static boolean doFirstTime;
    private String fromServer, out, userID;
    private EditText idText, pwdText, newidText, newpwdText;
    private TextView statusText;
    private Button signinButton, registerButton, signupButton, cancelButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        jumpToLayout1();
    }

    public void jumpToLayout1(){
        setContentView(R.layout.activity_main);

        idText = (EditText) findViewById(R.id.editText);
        pwdText = (EditText) findViewById(R.id.editText2);
        signinButton = (Button) findViewById(R.id.button);
        registerButton = (Button) findViewById(R.id.button2);
        statusText = (TextView) findViewById(R.id.textView6);
        statusText.setText("Welcome");

        doFirstTime = true;

        signinButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                out = "1\n" + idText.getText().toString() + "\n" + pwdText.getText().toString() + "\n";
                statusText.setText("Connecting...Please wait");
                MyClientTask mct = new MyClientTask();
                mct.execute();

                while (fromServer == null) ;

                if (fromServer.equals("0")) { // Success, change to Main2Activity
                    userID = idText.getText().toString();
                    mct.cancel(true);
                    chooseMode();
                } else if (fromServer.equals("1")) {
                    idText.setError("No this user!");
                    idText.setText("");
                    pwdText.setText("");
                    mct.cancel(true);
                } else if (fromServer.equals("2")) {
                    pwdText.setError("Wrong Password!");
                    pwdText.setText("");
                    mct.cancel(true);
                } else if (fromServer.equals("3")) {
                    idText.setError("The ID has logged in!");
                    idText.setText("");
                    pwdText.setText("");
                    mct.cancel(true);
                } else {
                    idText.setText("fromserver: " + fromServer);
                    mct.cancel(true);
                }

            }

        });

        registerButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                jumpToLayout3();
            }
        });
    }

    public void chooseMode(){ //play game or chat room
        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
        alertDialog.setTitle("Choose Mode");
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Play Game",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent newAct = new Intent();
                        newAct.setClass(MainActivity.this, Main2Activity.class);
                        newAct.putExtra("mode", "play");
                        newAct.putExtra("ID", userID);
                        startActivity(newAct);
                        MainActivity.this.finish();
                        dialog.dismiss();
                    }
                }
        );

        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Chat Room",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent newAct = new Intent();
                        newAct.setClass(MainActivity.this, Main2Activity.class);
                        newAct.putExtra("mode", "chat");
                        newAct.putExtra("ID", userID);
                        startActivity(newAct);
                        MainActivity.this.finish();
                        dialog.dismiss();
                    }
                }
        );

        alertDialog.show();
    }

    public void jumpToLayout3(){  // register
        setContentView(R.layout.layout3);

        newidText = (EditText) findViewById(R.id.editText3);
        newpwdText = (EditText) findViewById(R.id.editText4);
        signupButton = (Button) findViewById(R.id.button3);
        cancelButton = (Button) findViewById(R.id.button4);

        signupButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v){
                out = "2\n" + newidText.getText().toString() + "\n" + newpwdText.getText().toString() + "\n";
                MyClientTask mct = new MyClientTask();
                mct.execute();
                while(fromServer == null);

                if (fromServer.equals("0")) { // Success, change to Main2Activity
                    userID = newidText.getText().toString();
                    mct.cancel(true);
                    chooseMode();
                } else if (fromServer.equals("1")) { // id already exist
                    newidText.setError("This ID has been used!");
                    newidText.setText("");
                    newpwdText.setText("");
                } else {
                    newidText.setText("fromserver: "+fromServer);
                }
            }
        });

        cancelButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v){
                jumpToLayout1();
            }
        });
    }

    public class MyClientTask extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... arg0) {

            try {
                if(doFirstTime) {
                    clientSocket = new Socket(ipAddress, 8000);
                    doFirstTime = false;
                }
                DataOutputStream output = new DataOutputStream(clientSocket.getOutputStream());
                output.writeBytes(out);
                output.flush();
                //clientSocket.shutdownOutput();

                BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                fromServer = input.readLine();

                //clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return "";
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
