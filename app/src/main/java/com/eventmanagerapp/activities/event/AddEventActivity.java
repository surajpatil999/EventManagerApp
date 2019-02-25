package com.eventmanagerapp.activities.event;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;

import com.eventmanagerapp.R;
import com.eventmanagerapp.activities.BaseActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class AddEventActivity extends BaseActivity implements View.OnClickListener {
    private Calendar myCalendar = Calendar.getInstance();
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy");
    private int mHour, mMinute;
    private String calledFrom = "", date_time = "";
    private TextView tvDate, tvTime;
    private EditText etTitle, etEmail;
    private Button btnAdd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_event);
        initViews();
        date_time = simpleDateFormat.format(myCalendar.getTime());
        tvDate.setText(date_time);
        showTime();
    }

    private void initViews() {
        tvDate = findViewById(R.id.tvDate);
        tvTime = findViewById(R.id.tvTime);
        etTitle = findViewById(R.id.etTitle);
        etEmail = findViewById(R.id.etEmail);
        btnAdd = findViewById(R.id.btnAdd);
        tvTime.setOnClickListener(this);
        tvDate.setOnClickListener(this);
        btnAdd.setOnClickListener(this);
    }

    private void openCalendar() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, date, myCalendar
                .get(Calendar.YEAR), myCalendar.get(Calendar.MONTH),
                myCalendar.get(Calendar.DAY_OF_MONTH));
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP)
            datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    DatePickerDialog.OnDateSetListener date = new DatePickerDialog.OnDateSetListener() {
        @Override
        public void onDateSet(DatePicker view, int year1, int monthOfYear, int dayOfMonth) {
            myCalendar.set(Calendar.YEAR, year1);
            myCalendar.set(Calendar.MONTH, monthOfYear);
            myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            date_time = simpleDateFormat.format(myCalendar.getTime());
            tvDate.setText(date_time);
        }
    };

    private void timePicker() {
        // Get Current Time
        final Calendar c = Calendar.getInstance();
        mHour = c.get(Calendar.HOUR_OF_DAY);
        mMinute = c.get(Calendar.MINUTE);
        // Launch Time Picker Dialog
        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        mHour = hourOfDay;
                        mMinute = minute;
                        String am_pm = "";
                        myCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        myCalendar.set(Calendar.MINUTE, minute);
                        if (myCalendar.getTimeInMillis() >= c.getTimeInMillis()) {
                           /* if (myCalendar.get(Calendar.AM_PM) == Calendar.AM)
                                am_pm = "AM";
                            else if (myCalendar.get(Calendar.AM_PM) == Calendar.PM)
                                am_pm = "PM";
                            String strHrsToShow = (myCalendar.get(Calendar.HOUR) == 0) ? "12" : myCalendar.get(Calendar.HOUR) + "";
                            tvTime.setText(strHrsToShow + ":" + myCalendar.get(Calendar.MINUTE) + " " + am_pm);*/
                            showTime();
                        } else {
                            //it's before current'
                            showErrorMessage(AddEventActivity.this, "Please select valid time");
                        }
                    }
                }, mHour, mMinute, false);
        timePickerDialog.show();
    }

    private void showTime()
    {
        String am_pm = "";
        if (myCalendar.get(Calendar.AM_PM) == Calendar.AM)
            am_pm = "AM";
        else if (myCalendar.get(Calendar.AM_PM) == Calendar.PM)
            am_pm = "PM";
        String strHrsToShow = (myCalendar.get(Calendar.HOUR) == 0) ? "12" : myCalendar.get(Calendar.HOUR) + "";
        tvTime.setText(strHrsToShow + ":" + myCalendar.get(Calendar.MINUTE) + " " + am_pm);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tvDate:
                openCalendar();
                break;
            case R.id.tvTime:
                timePicker();
                break;
            case R.id.btnAdd:
                sendDataBack();
                break;
        }
    }

    private void sendDataBack()
    {
        if (validation()) {
            Intent returnIntent = new Intent();
            returnIntent.putExtra("title", etTitle.getText().toString());
            returnIntent.putExtra("email", etEmail.getText().toString());
            returnIntent.putExtra("date", tvDate.getText().toString());
            returnIntent.putExtra("time", tvTime.getText().toString());
            setResult(Activity.RESULT_OK, returnIntent);
            finish();
        }
    }

    private boolean validation()
    {
        boolean flag = true;
        if (etTitle.getText().toString().length() < 1)
        {
            showErrorMessage(this, "Please enter title");
            flag = false;
        }
        else if (etEmail.getText().toString().length() < 1)
        {
            showErrorMessage(this, "Please enter email");
            flag = false;
        }
        else if (!Patterns.EMAIL_ADDRESS.matcher(etEmail.getText().toString()).matches())
        {
            showErrorMessage(this, "Please enter valid email");
            flag = false;
        }
        else if (tvTime.getText().toString().length() < 1)
        {
            showErrorMessage(this, "Please select time");
            flag = false;
        }
        else if (tvTime.getText().toString().length() < 1)
        {
            showErrorMessage(this, "Please select date");
            flag = false;
        }
        else
        {
            flag = true;
        }

        return flag;
    }
}
