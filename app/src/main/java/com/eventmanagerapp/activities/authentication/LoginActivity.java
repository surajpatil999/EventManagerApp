package com.eventmanagerapp.activities.authentication;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.eventmanagerapp.R;
import com.eventmanagerapp.RoomDatabase.LoginTable;
import com.eventmanagerapp.ViewModel.LoginViewModel;
import com.eventmanagerapp.activities.BaseActivity;
import com.eventmanagerapp.activities.event.MainActivity;

import java.util.List;
import java.util.Objects;

public class LoginActivity extends BaseActivity implements View.OnClickListener{
    private EditText etPassword, etEmail;
    private Button btnLogin, btnInsertData;
    private LoginViewModel loginViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        initViews();

        loginViewModel = ViewModelProviders.of(LoginActivity.this).get(LoginViewModel.class);
    }

    private void initViews() {
        etPassword = findViewById(R.id.etPassword);
        etEmail = findViewById(R.id.etEmail);
        btnLogin = findViewById(R.id.btnLogin);
        btnInsertData = findViewById(R.id.btnInsertData);
        btnLogin.setOnClickListener(this);
        btnInsertData.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnLogin:
                doLogin();
                break;
            case R.id.btnInsertData:
                insertData();
                break;
        }
    }

    private void doLogin()
    {

        loginViewModel.getGetAllData().observe(this, new Observer<List<LoginTable>>() {
            @Override
            public void onChanged(@Nullable List<LoginTable> data) {

                try {
                    if (validation() && etEmail.getText().toString().equals((Objects.requireNonNull(data).get(0).getEmail()))
                            && etPassword.getText().toString().equals((Objects.requireNonNull(data.get(0).getPassword()))))
                    {
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    }
                    else
                    {
                        showErrorMessage(LoginActivity.this, "Please check you credentials");
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

    }

    private void insertData()
    {
        LoginTable data = new LoginTable();

        if (validation()) {
            data.setEmail(etEmail.getText().toString());
            data.setPassword(etPassword.getText().toString());
            loginViewModel.insert(data);
            clearData();
            btnInsertData.setVisibility(View.INVISIBLE);
        }
    }

    private boolean validation()
    {
        boolean flag;
        if (etEmail.getText().toString().length() < 1)
        {
            showErrorMessage(this, "Please enter email");
            flag = false;
        }
        else if (!Patterns.EMAIL_ADDRESS.matcher(etEmail.getText().toString()).matches())
        {
            showErrorMessage(this, "Please enter valid email");
            flag = false;
        }
        else if (etPassword.getText().toString().length() < 1)
        {
            showErrorMessage(this, "Please enter password");
            flag = false;
        }
        else
        {
            flag = true;
        }

        return flag;
    }

    private void clearData()
    {
        etEmail.setText("");
        etPassword.setText("");
    }
}
