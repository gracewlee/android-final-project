package com.sydney.myplayers;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import android.view.ViewGroup.LayoutParams;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

/*
    OUT
    3: draw         + (100,100)
    4: log out
    5: chat room
    6: play game
    7: clear        + (50, 50)
    8: answer       + answer
    9: new question
*/

public class Main2Activity extends AppCompatActivity {
    private TextView userText, questionText, scoreText;
    private EditText ansText;
    private ImageView imageView;
    private Button change, clear, sendAns, logout;
    private String mode, userID, out2, users, winner, question, score;
    private Paint paint, paint2;
    private Bitmap bitmap;
    private Canvas canvas;
    private Point point = new Point();
    private Socket clientSocket2;
    private DataOutputStream output;
    private BufferedReader input;
    private RadioGroup radioGroup;
    private RadioButton blackRB;
    private ToggleButton eraserTB;
    private int color;

    private float x, y, mx = 100, my = 100;
    private float x1, y1, x2, y2;
    private float x11, y11, x22, y22; // the actual point from other clients
    // x, y : from server and must be 0~1 (ratio)
    // x > 3 : from server and are first point
    // mx, my : draw by my user and must be 0~1 (ratio)
    // mx > 3 : tell server first point
    // x1, x2, y1, y2 : draw by user (actual point)
    // x11, x22, y11, y22 : from server (actual point)


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        mode = intent.getStringExtra("mode");
        userID = intent.getStringExtra("ID");

        if (mode.equals("chat")) {
            toChatRoom();
            out2 = "5\n";
        } else {
            toPlayGame();
            out2 = "6\n";
        }

        clientSocket2 = MainActivity.clientSocket;
        try {
            output = new DataOutputStream(clientSocket2.getOutputStream());
            input = new BufferedReader(new InputStreamReader(clientSocket2.getInputStream()));
        } catch (IOException e){
            e.printStackTrace();
        }

        outputThread(); // new id in
        inputThread(); // read input from server forever
    }

    public void toChatRoom(){ // chat room
        //activity_main2 layout
        setContentView(R.layout.activity_main2);
        setTitle("Chat Room ID: " + userID);
        userText = (TextView) findViewById(R.id.textView5);
        imageView = (ImageView) findViewById(R.id.imageView);
        change = (Button) findViewById(R.id.button5);
        clear = (Button) findViewById(R.id.button10);
        logout = (Button) findViewById(R.id.button6);
        radioGroup = (RadioGroup) findViewById(R.id.radiogroup);
        blackRB = (RadioButton) findViewById(R.id.radioButton);
        blackRB.setChecked(true);

        change.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                out2 = "6\n"; // change to play game
                outputThread();
                toPlayGame();
            }
        });

        clear.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                out2 = "7\n";  // case 7 : clear
                outputThread();
                canvas.drawColor(Color.WHITE);
                imageView.invalidate();
            }
        });

        logout.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                logoutAlert();
            }
        });

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.radioButton) { // black
                    paint.setColor(Color.BLACK);
                    paint.setStrokeWidth(3);
                } else if (checkedId == R.id.radioButton2) { //eraser
                    paint.setColor(Color.WHITE);
                    paint.setStrokeWidth(20);
                } else if (checkedId == R.id.radioButton3) { //blue
                    paint.setColor(Color.BLUE);
                    paint.setStrokeWidth(3);
                } else { // red
                    paint.setColor(Color.RED);
                    paint.setStrokeWidth(3);
                }
            }
        });

        iniDraw(); // initial Drawing
    }

    public void toPlayGame(){
        //layout2
        setContentView(R.layout.layout2);
        setTitle("Play Game ID: " + userID);
        userText = (TextView) findViewById(R.id.textView7);
        questionText = (TextView) findViewById(R.id.textView8);
        scoreText = (TextView) findViewById(R.id.textView9);
        ansText = (EditText) findViewById(R.id.editText5);
        imageView = (ImageView) findViewById(R.id.imageView2);
        change = (Button) findViewById(R.id.button7);
        clear = (Button) findViewById(R.id.button8);
        sendAns = (Button) findViewById(R.id.button11);
        logout = (Button) findViewById(R.id.button9);
        eraserTB = (ToggleButton) findViewById(R.id.toggleButton);

        ansText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionID, KeyEvent e) {
                out2 = "8\n" + ansText.getText() + "\n";
                outputThread();
                return false;
            }
        });

        change.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                out2 = "5\n"; // change to chat room
                outputThread();
                toChatRoom();
            }
        });

        clear.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                out2 = "7\n";
                outputThread();
                canvas.drawColor(Color.WHITE);
                imageView.invalidate();
            }
        });

        sendAns.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                out2 = "8\n" + ansText.getText() + "\n";
                outputThread();
            }
        });

        logout.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                logoutAlert();
            }
        });

        eraserTB.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (eraserTB.isChecked()) {
                    paint.setColor(Color.WHITE);
                    paint.setStrokeWidth(20);
                } else {
                    paint.setColor(Color.BLACK);
                    paint.setStrokeWidth(3);
                }
            }
        });

        iniDraw(); // initial Drawing
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(3);
        paint2.setColor(Color.BLACK);
        paint2.setStrokeWidth(3);
    }

    public void iniDraw(){
        paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3);
        paint.setAntiAlias(true);
        paint.setTextSize(20);

        // for other users
        paint2 = new Paint();
        paint2.setColor(Color.BLACK);
        paint2.setStyle(Paint.Style.STROKE);
        paint2.setStrokeWidth(3);
        paint2.setAntiAlias(true);
        paint2.setTextSize(20);

        getWindowManager().getDefaultDisplay().getSize(point);
        bitmap = Bitmap.createBitmap(point.x, point.x, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);

        imageView.setImageBitmap(bitmap);
        LayoutParams params = imageView.getLayoutParams();
        params.width = point.x;
        params.height = point.x;
        imageView.setLayoutParams(params);

        imageView.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {

                    case MotionEvent.ACTION_DOWN:
                        // get first point
                        x1 = event.getX(); // first point
                        y1 = event.getY();
                        mx = x1 / point.x + 10;
                        my = y1 / point.x;

                        // send point to server
                        out2 = "3\n" + mx + "\n" + my + "\n" + paint.getColor() + "\n";
                        outputThread();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        // get next point
                        x2 = event.getX(); // next point
                        y2 = event.getY();
                        mx = x2 / point.x;
                        my = y2 / point.x;

                        // send point to server
                        out2 = "3\n" + mx + "\n" + my + "\n" + paint.getColor() + "\n";
                        outputThread();

                        // drawLine
                        canvas.drawLine(x1, y1, x2, y2, paint);
                        imageView.invalidate();
                        x1 = x2;
                        y1 = y2;
                        break;
                } // end switch

                return true;
            } // end onTouch
        }); // end imageView
    }

    public void outputThread(){ // output data
        new Thread() {
            @Override
            public void run() {
                try {
                    output.writeBytes(out2);
                    output.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public void inputThread(){ // read in data
        new Thread() {
            public void run() {
                while (true) {
                    try {
                        users = input.readLine();
                        if(users != null) {
                            x = Float.parseFloat(input.readLine());
                            y = Float.parseFloat(input.readLine());
                            color = Integer.parseInt(input.readLine());

                            // other info
                            if (x == 30) { // someone win
                                winner = input.readLine();
                                question = input.readLine();
                            } else if (x == 60) { // draw question
                                question = input.readLine();
                            } else if (x == 70) { // guess question
                                question = input.readLine();
                                score = input.readLine();
                            } else if (x == 75){ // after someone win (case 9)
                                question = input.readLine();
                            } else if (x == 40 || x == 90) {// score
                                score = input.readLine();
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (NumberFormatException e){
                        e.printStackTrace();
                    }

                    updateUI();
                } // end while
            } // end run()
        }.start();
    }

    public void updateUI(){
        runOnUiThread(new Runnable() { //here to update ui
            @Override
            public void run() {
                userText.setText("Users Online: " + users);
                if (color != 300) {
                    paint2.setColor(color);
                    paint2.setStrokeWidth(3);
                    if (color == Color.WHITE) {
                        paint2.setStrokeWidth(20);
                    }
                }

                if (x == 100) {// do nothing

                } else if (x == 90) {
                    scoreText.setText("Score: " + score);

                } else if (x == 80) { // wait for people to play game
                    questionText.setText("Wait...");
                    change.setEnabled(true);
                    clear.setEnabled(true);
                    sendAns.setEnabled(false);
                    logout.setEnabled(true);
                    ansText.setEnabled(false);
                    imageView.setEnabled(true);
                    eraserTB.setEnabled(true);

                } else if (x == 70 || x == 75) { // guess question
                    questionText.setText("(" + question.length() + " words)");
                    change.setEnabled(true);
                    clear.setEnabled(false);
                    sendAns.setEnabled(true);
                    logout.setEnabled(true);
                    ansText.setEnabled(true);
                    imageView.setEnabled(false);
                    eraserTB.setEnabled(false);
                    if(x == 70) {
                        scoreText.setText("Score: " + score);
                    }
                } else if (x == 60) { // draw question
                    questionText.setText(question);
                    change.setEnabled(false);
                    clear.setEnabled(true);
                    sendAns.setEnabled(false);
                    logout.setEnabled(false);
                    ansText.setEnabled(false);
                    imageView.setEnabled(true);

                } else if (x == 50) { // clear
                    canvas.drawColor(Color.WHITE);
                    imageView.invalidate();

                } else if (x == 40) { // wrong answer
                    ansText.setText("");
                    ansText.setError("Oops! You're Wrong :( ");
                    scoreText.setText("Score: " + score);

                } else if (x == 30) { // correct answer
                    correctAlert();

                } else if (x > 1) { // first point // x>3
                    x11 = (x - 10) * (point.x);
                    y11 = y * (point.x);

                } else if (x < 1) { // next point
                    x22 = x * (point.x);
                    y22 = y * (point.x);
                    canvas.drawLine(x11, y11, x22, y22, paint2);
                    imageView.invalidate();
                    x11 = x22;
                    y11 = y22;

                } else {
                    Log.i("else", "x=" + x);
                }
                //end if()

            }// end run() of runOnUiThread
        }); // end runOnUiThread
    }


    public void correctAlert(){
        questionText.setText("Wait...");
        ansText.setText("");
        canvas.drawColor(Color.WHITE);
        imageView.invalidate();

        AlertDialog alertDialog = new AlertDialog.Builder(Main2Activity.this).create();
        alertDialog.setTitle("Correct Answer");
        String s = winner + " wins the game!\n";
        s += "The answer is " + question + "\n";
        s += "I am " + userID;
        alertDialog.setMessage(s);
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if(userID.equals(winner)){
                            out2 = "9\n";
                            score = "" + (Integer.parseInt(score) + 5);
                            Log.i("score",""+score);
                            scoreText.setText("Score: " + score);
                            outputThread();
                        }
                        dialog.dismiss();
                    }
                }
        );
        alertDialog.show();
    }

    public void logoutAlert(){
        AlertDialog alertDialog = new AlertDialog.Builder(Main2Activity.this).create();
        alertDialog.setTitle("Want to Logout?");
        //alertDialog.setMessage();
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Sure",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        out2 = "4\n";  // case 4 : tell server the client left, point (100,100) : do nothing
                        outputThread();
                        Intent newAct = new Intent();
                        newAct.setClass(Main2Activity.this, MainActivity.class);
                        startActivity(newAct);
                        Main2Activity.this.finish();
                        dialog.dismiss();
                    }
                }
        );

        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "CANCEL",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }
        );

        alertDialog.show();
    }
    /****
    public class MyClientTask2 extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... arg0) {

            try {
                // tell server new id into chat room or log out
                output = new DataOutputStream(clientSocket2.getOutputStream());
                output.writeBytes(out2);
                output.flush();

                BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket2.getInputStream()));
                users = input.readLine();

            } catch (IOException e) {
                e.printStackTrace();
            }
            return users;
        }

        @Override
        protected void onPostExecute(String s){
            userText.setText("Users Online: " + s);
        }
    } // end MyClientTask2
    */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main2, menu);
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
