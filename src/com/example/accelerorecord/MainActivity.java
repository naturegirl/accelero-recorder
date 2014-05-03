package com.example.accelerorecord;



import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends ActionBarActivity implements SensorEventListener {

    private Button newButton;
    private Button startButton;
    private Button stopButton;

    private TextView nameTv;
    private TextView xyzTv;
    private TextView infoTv;
    private TextView saveTv;

    private String letter; 		// letter we're recording
    private StringBuilder dataBuffer;	// saves what we'll write to file
    private int lineCnt;
    private int count;		// number of times same letter was recorded

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;

    private int status;

    private static final int REC_STARTED = 1;
    private static final int REC_STOPPED = 2;

    // where on the sdcard the recorded files are stored
    private static final String savedDir = "/AcceleroRecord";	


    @Override
    protected void onCreate(Bundle savedInstanceState) {
	Log.w("jenny", "here");
	super.onCreate(savedInstanceState);
	setContentView(R.layout.activity_main);

	newButton = (Button) findViewById(R.id.newButton);
	startButton = (Button) findViewById(R.id.startButton);
	stopButton = (Button) findViewById(R.id.stopButton);

	nameTv = (TextView) findViewById(R.id.filenameTextview);
	xyzTv = (TextView) findViewById(R.id.xyzTextview);
	infoTv = (TextView) findViewById(R.id.infoTextview);
	saveTv = (TextView) findViewById(R.id.saveTextview);

	// Sensor related stuff
	mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
	mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
	mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);

	newButton.setOnClickListener(new OnClickListener() {
	    @Override
	    public void onClick(View arg0) {
		// set filename and empty other views
		displayFilenameForm();
		count = 0;
	    }
	});

	startButton.setOnClickListener(new OnClickListener() {
	    @Override
	    public void onClick(View arg0) {
		// empties databuffer and turns on sensor recording
		count++;
		status = REC_STARTED;
		dataBuffer = new StringBuilder();
		lineCnt = 0;
		xyzTv.setText("recording ...\t count: " + count);
	    }
	});

	stopButton.setOnClickListener(new OnClickListener() {
	    @Override
	    public void onClick(View arg0) {
		// didn't start recording yet
		if (status != REC_STARTED) {
		    String msg = "start recording first";
		    Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
		    return;
		}
		
		// turns off sensor recording
		status = REC_STOPPED;
		Log.w("jenny", dataBuffer.toString());
		
		// display how many lines we recorded
		String msg = "recorded " + lineCnt + " lines of data points";
		infoTv.setText(msg);
		
		xyzTv.setText("");
		
		// write to file here already
		writeFile();
	    }
	});
    }

    protected void onResume() {
	super.onResume();
	mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
    }
    protected void onPause() {
	super.onPause();
	mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
	if (status == REC_STARTED) {
	    float x = event.values[0];
	    float y = event.values[1];
	    float z = event.values[2];
	    String delim = ", ";
	    String line = event.timestamp + delim + x + delim + y + delim + z+"\n";
	    dataBuffer.append(line);
	    lineCnt++;
	}
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
	// ignore
    }


    private void displayFilenameForm() {
	// AlertDialog to enter filename
	AlertDialog.Builder builder = new AlertDialog.Builder(this);
	builder.setTitle("Enter the letter to record");

	// Set up the input
	final EditText input = new EditText(this);
	// Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
	input.setInputType(InputType.TYPE_CLASS_TEXT);
	builder.setView(input);

	// Set up the buttons
	builder.setPositiveButton("OK", new DialogInterface.OnClickListener() { 
	    @Override
	    public void onClick(DialogInterface dialog, int which) {
		// adding timestamp to filename
		letter = input.getText().toString();
		nameTv.setText("Recording for letter: " + letter);
		xyzTv.setText("");
		infoTv.setText("");
		saveTv.setText("");
	    }
	});
	builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	    @Override
	    public void onClick(DialogInterface dialog, int which) {
		dialog.cancel();
	    }
	});

	builder.show();	
    }
    
    private void writeFile() {
	Log.w("jenny", "writing to file");
	try {
	    File dir = new File(Environment.getExternalStorageDirectory(), savedDir);
	    
	    if (!dir.exists() && dir.mkdirs()) {
		Log.w("jenny", "successfully created directory");
	    }

	    Long tsLong = System.currentTimeMillis()/1000;
	    String ts = tsLong.toString();
	    String filename = letter + "_" + ts +  ".csv";

	    File file = new File(dir, filename);
	    
	    if (file.exists()) {
		Log.e("jenny", "file already exists. Overwriting existing file!");
	    } else {
		file.createNewFile();
	    }
	    FileOutputStream fos;
	    byte[] data = dataBuffer.toString().getBytes();
	    fos = new FileOutputStream(file);
	    fos.write(data);
	    fos.flush();
	    fos.close();

	    String size;
	    if (data.length < 1024)
		size = data.length + " B";
	    else
		size = (data.length / 1024) + " KB";
	    String msg = "file saved to " + filename + "\nsize: " + size;
	    saveTv.setText(msg);

	} catch (FileNotFoundException e) { e.printStackTrace();
	} catch (IOException e) { e.printStackTrace(); }
    }
}
