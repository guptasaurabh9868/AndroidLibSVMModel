package com.mobilecomputing.guptas.a173059002_hw5;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.nbsp.materialfilepicker.MaterialFilePicker;
import com.nbsp.materialfilepicker.ui.FilePickerActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Pattern;

import umich.cse.yctung.androidlibsvm.LibSVM;

public class MainActivity extends AppCompatActivity {

    final String TAG = "MainActivity";
    Button trainFilePicker, testFilePicker, TrainModel, TestModel;
    TextView trainFilePath, testFilePath, trainprogresscomplete, testprogresscomplete, result;
    ProgressBar trainProgress, testProgress;
    Handler handler;
    LibSVM svm;
    String file1, file2;
    boolean running = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        trainFilePicker = findViewById(R.id.trainFilePicker);
        trainFilePath = findViewById(R.id.trainFile);
        TrainModel = findViewById(R.id.Train);
        trainProgress = findViewById(R.id.progressTraining);
        trainprogresscomplete = findViewById(R.id.trainprogressComplete);
        testFilePicker = findViewById(R.id.testFilePicker);
        testFilePath = findViewById(R.id.testFile);
        TestModel = findViewById(R.id.Predict);
        testProgress = findViewById(R.id.progressTesting);
        testprogresscomplete = findViewById(R.id.testprogressComplete);
        result = findViewById(R.id.result);

        handler = new Handler();

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
        }

        final Thread train_thread = new Thread(new Runnable() {
            @Override
            public void run() {
                LibSVM svm = LibSVM.getInstance();
                File root = Environment.getExternalStorageDirectory();
                File dir = new File(root.getAbsolutePath() + "/mobileComputing");

                file1 = (String) trainFilePath.getText();
                String path = dir.toString() + "/";
                String train_file = path + "train.txt";
                File file2 = new File(train_file);
                convert_data(file1, file2);

                svm.scale(path + "train.txt", path + "scaled_train.txt");
                svm.train("-t 2 " + path + "scaled_train.txt " + path + "model");

                handler.post(new Runnable() {
                    @Override
                    public void run() {

                        trainProgress.setVisibility(View.INVISIBLE);
                        trainprogresscomplete.setText("Training Complete");
                        trainprogresscomplete.setVisibility(View.VISIBLE);
                    }
                });


            }
        });

        trainFilePicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                new MaterialFilePicker()
                        .withActivity(MainActivity.this)
                        .withRequestCode(1000)
                        .withFilter(Pattern.compile(".*\\.csv$")) // Filtering files and directories by file name using regexp
                        .withFilterDirectories(false) // Set directories filterable (false by default)
                        .withHiddenFiles(true) // Show hidden files and folders
                        .start();
            }
        });

        TrainModel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (trainFilePath.getVisibility() == View.VISIBLE) {
                    train_thread.start();
                    testprogresscomplete.setVisibility(View.INVISIBLE);
                    trainProgress.setVisibility(View.VISIBLE);

                } else {
                    Toast.makeText(getApplicationContext(), " Please Select a Train File First", Toast.LENGTH_SHORT).show();
                }
            }

        });


        testFilePicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new MaterialFilePicker()
                        .withActivity(MainActivity.this)
                        .withRequestCode(1001)
                        .withFilter(Pattern.compile(".*\\.csv$")) // Filtering files and directories by file name using regexp
                        .withFilterDirectories(false) // Set directories filterable (false by default)
                        .withHiddenFiles(false) // Show hidden files and folders
                        .start();
            }
        });

        final Thread test_thread = new Thread(new Runnable() {
            @Override
            public void run() {
                LibSVM svm = LibSVM.getInstance();
                File root = Environment.getExternalStorageDirectory();
                File dir = new File(root.getAbsolutePath() + "/mobileComputing");

                file1 = (String) testFilePath.getText();
                String path = dir.toString() + "/";
                String test_file = path + "test.txt";
                File file2 = new File(test_file);
                final int total_sample = convert_data(file1, file2);

                svm.scale(path + "test.txt", path + "scaled_test.txt");
                svm.predict(path + "scaled_test.txt " + path + "model " + path + "result.txt");

                final int correct_sample = append_data(file1, path, path + "result.txt");


                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        testProgress.setVisibility(View.INVISIBLE);
                        testprogresscomplete.setText("Testing Complete");
                        testprogresscomplete.setVisibility(View.VISIBLE);
                        result.setText("Accuracy: " + (float) (correct_sample * 100.0 / total_sample) + "  ( " + correct_sample + " / " + total_sample + " )");
                        result.setVisibility(View.VISIBLE);

                    }
                });


            }
        });

        TestModel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (View.VISIBLE == trainprogresscomplete.getVisibility()) {
                    if (View.VISIBLE == testFilePath.getVisibility()) {
                        testprogresscomplete.setVisibility(View.INVISIBLE);
                        test_thread.start();
                        testProgress.setVisibility(View.VISIBLE);
                    } else {
                        Toast.makeText(getApplicationContext(), "Please Select a Test File !!", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Please Train the Model First!!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1000 && resultCode == RESULT_OK) {
            String filePath = data.getStringExtra(FilePickerActivity.RESULT_FILE_PATH);
            // Do anything with file
            trainFilePath.setText(filePath);
            trainFilePath.setVisibility(View.VISIBLE);
        }
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            String filePath = data.getStringExtra(FilePickerActivity.RESULT_FILE_PATH);
            // Do anything with file
            testFilePath.setText(filePath);
            testFilePath.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1001:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission Granted!!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Permission Rejected!! Application may misbehave.", Toast.LENGTH_SHORT).show();
                    finish();
                }
        }


    }

    private int convert_data(String file1, File file2) {

        int iteration = 0;
        try (FileReader fr = new FileReader(file1);
             FileWriter fw = new FileWriter(file2)) {
            BufferedReader bufferedReader = new BufferedReader(fr);
            String line;
            while ((line = bufferedReader.readLine()) != null) {

                String[] fields = line.split(",");
                String newLine = null;
                iteration++;
                if (iteration == 1 || iteration == 2)
                    continue;

                //Assigning labels
                String label = fields[6];
                Log.d(TAG, label);
                if ("Stationary".equalsIgnoreCase(label)) {
                    newLine = "0";
                } else newLine = "1";

                //Assigning AccelX
                String accelx = fields[3].trim();
                newLine += " 1:" + accelx;

                //Assigning AccelY
                String accely = fields[4].trim();
                newLine += " 2:" + accely;

                //Assigning AccelZ
                String accelz = fields[5].trim();
                newLine += " 3:" + accelz;
                newLine += "\n";
                fw.write(newLine);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return iteration;
    }

    private int append_data(String original_file, String path, String result_file) {

        int correct = 0, wrong = 0;
        try (FileWriter fw = new FileWriter(path + "Output.csv");
             FileReader file2_r = new FileReader(result_file);
             FileReader orig_r = new FileReader(original_file)) {
            BufferedReader result_bufferedReader = new BufferedReader(file2_r);
            BufferedReader orig_bufferedReader = new BufferedReader(orig_r);
            String result_line, orig_line;
            int iteration = 0;
            while ((orig_line = orig_bufferedReader.readLine()) != null) {

                String[] fields = orig_line.split(",");
                String newLine;
                if (iteration == 0) {
                    newLine = orig_line;
                    newLine += "\n";
                } else if (iteration == 1) {
                    newLine = orig_line;
                    newLine += ", Prediction\n";
                } else {
                    result_line = result_bufferedReader.readLine();
                    newLine = orig_line;
                    if (result_line.equalsIgnoreCase("0")) {
                        newLine += ", stationary\n";
                        result_line = "stationary";
                    } else {
                        newLine += ", walking\n";
                        result_line = "walking";
                    }
                    if (fields[6].trim().equalsIgnoreCase(result_line.trim()))
                        correct++;
                    else
                        wrong++;
                }
                fw.write(newLine);
                iteration++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return correct;
    }

}
