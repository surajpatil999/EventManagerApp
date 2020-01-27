package com.itoneclick.buypassnow;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.Result;
import com.itoneclick.buypassnow.customview.CameraSelectorDialogFragment;
import com.itoneclick.buypassnow.global.Constant;
import com.itoneclick.buypassnow.global.Function;
import com.itoneclick.buypassnow.global.GlobalMethods;
import com.itoneclick.buypassnow.services.BluetoothService;
import com.itoneclick.buypassnow.society.activities.EnterPinActivity;
import com.itoneclick.buypassnow.society.bluetooth_le.BluetoothLEActivity;
import com.itoneclick.buypassnow.society.bluetooth_le.SerialListener;
import com.itoneclick.buypassnow.society.bluetooth_le.SerialService;
import com.itoneclick.buypassnow.society.bluetooth_le.SerialSocket;
import com.itoneclick.buypassnow.society.models.QRPinResponse;
import com.itoneclick.buypassnow.society.network.ApiService;
import com.itoneclick.buypassnow.society.network.ApiUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;
import java.util.TimeZone;

import me.dm7.barcodescanner.zxing.ZXingScannerView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class ActivityQrCodeReader extends AppCompatActivity implements ZXingScannerView.ResultHandler,
        CameraSelectorDialogFragment.CameraSelectorDialogListener, ServiceConnection, SerialListener {
    //Qr Scanner
//    private ZBarScannerView mZBarScannerView;
    private ImageView mImageViewBack;
    private SharedPreferences mSharedPreference;
    //Bluetooth
    private BluetoothService mService;
    private BluetoothDevice mBluetoothDevice;
    //Requests for activity result
    private int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    //Basic Strings
    private String mStringParkingStartTime = "";
    private String mStringVehicleNo = "";
    private String mStringBusinessTitle = "";
    private String mStringValetLocationName = "";
    private String mStringLocationName = "";
    private String mStringReceiptNumber = "";
    private String mStringTotalHour = "";
    private String mStringCharges = "";
    private String mStringDealTitle = "";
    private String mStringDealID = "";
    private String mStringPassesId = "";
    private String mStringPaymentTransactionId = "";

    private boolean mBooleanIsSafeToBackPress = false;

    private boolean mIntIsPrinting = false;

    private ZXingScannerView mZBarScannerView;

    private Dialog mDialogBoomBarrier;
    private int mIntIsBoomBarrier = 0;

    private String mStringPaymentStatus = "";
    private String mStringCustomerName = "";
    private String mStringPurchaseDate = "";
    private String mStringFlatNo = "";
    private String mStringBlockNo = "";

    private Dialog mDialogCustomer;
    private MediaPlayer mMediaPlayerScanSuccess;
    private boolean mBooleanIsBusinessAdmin = false;
    private boolean mBooleanIsUser = false;
    private boolean mBooleanIsEntry = false;
    private boolean mIntIsMember = false;
    private String mStringMobileNo = "";
    private String mStringAccountId = "";
    private boolean mIsValidated = false;
    private ImageView mImageViewSwitch;
    private int mCameraId = -1;
    private boolean isBackPressed = false;
    private String userId;
    private boolean isSociety, isBluetooth;

    private enum Connected {False, Pending, True}

    private String deviceAddress;
    private SerialSocket socket;
    private SerialService service;
    private boolean initialStart = true;
    private Connected connected = Connected.False;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_qr_code_reader);

        mImageViewBack = findViewById(R.id.activity_qr_code_reader_screen_iv_header_back);
        mImageViewSwitch = findViewById(R.id.activity_qr_code_reader_screen_iv_header_switch);
        mZBarScannerView = findViewById(R.id.qrdecoderview);
        mSharedPreference = getSharedPreferences(Constant.PREFERANCEBPN, 0);

        userId = mSharedPreference.getString("pref_user_UID", "");
        isSociety = mSharedPreference.getBoolean("pref_is_society", false);
        isBluetooth = mSharedPreference.getBoolean("pref_is_bluetooth", false);

        mImageViewBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        mImageViewSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment cFragment = CameraSelectorDialogFragment.newInstance(ActivityQrCodeReader.this, mCameraId);
                cFragment.show(getSupportFragmentManager(), "camera_selector");
            }
        });
        setStatusBarColor();

        if (isSociety && isBluetooth) {
            bindService(new Intent(ActivityQrCodeReader.this, SerialService.class), ActivityQrCodeReader.this, Context.BIND_AUTO_CREATE);
        }
    }

    // Qr code response will come here
    @Override
    public void handleResult(Result result) {
        Log.d("QrCodeResult", result.getText());
        if (!GlobalMethods.isNetworkStatusAvialable(ActivityQrCodeReader.this)) {
            GlobalMethods.isNetworkConnection(ActivityQrCodeReader.this);
        } else {
            if (isSociety) {
                sendQrCodeToServer(result.getText());
            } else
                new sendQrCodeApi(result.getText()).execute(null, null, null);
        }
    }

    @Override
    public void onCameraSelected(int cameraId) {
        mZBarScannerView.stopCamera();
        mCameraId = cameraId;
        BPNApplication.setCameraID(cameraId);
        mZBarScannerView.startCamera(mCameraId);
    }

    class sendQrCodeApi extends AsyncTask<Void, Void, String> {
        String userresponse = "";
        String qrCode = "";

        sendQrCodeApi(String qrCode) {
            this.qrCode = qrCode;
        }

        @Override
        protected String doInBackground(Void... params) {
            Log.e("QrCode ", "isSociety: " + isSociety);
            userresponse = Function.performGETCall(Constant.SEND_QR_CODE + qrCode + "&UserId=" + userId);
            Log.e("QrCode Request--", Constant.SEND_QR_CODE + qrCode + "&UserId=" + userId);
            return userresponse;
        }

        @Override
        protected void onPreExecute() {
            // showProgress();
            super.onPreExecute();
            GlobalMethods.hide_keyboard(ActivityQrCodeReader.this);
            GlobalMethods.showProgress(ActivityQrCodeReader.this, getResources().getString(R.string.str_send_qr_code));
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            GlobalMethods.dismissProgress();

            if (result != null && !result.equals("") && !result.equals("null")) {
                Log.e("QrCode Response--", result);

                JSONObject response = null;
                try {
                    response = new JSONObject(result);

                    if (response.getString("Message").equalsIgnoreCase("Success")) {

                        GlobalMethods.callLogAPI(userId, true, true, "QR Code","Scanned successfully", "QR code scanned successfully");
                        mMediaPlayerScanSuccess = MediaPlayer.create(ActivityQrCodeReader.this, R.raw.scansuccess);

                        mMediaPlayerScanSuccess.start();

                        mStringVehicleNo = response.getString("PUVehicleNo");
                        mStringMobileNo = response.getString("PUMobile");
                        mStringCustomerName = response.getString("UserName");
                        mBooleanIsBusinessAdmin = response.getBoolean("ISBusinAdmin");
                        mBooleanIsUser = response.getBoolean("IsUser");

                        mIntIsMember = response.optBoolean("IsMember");

                        if (mIntIsMember) {
                            if (response.optString("BlockNo") != null && !response.optString("BlockNo").equalsIgnoreCase("")) {
                                mStringBlockNo = response.optString("BlockNo");
                            } else {
                                mStringBlockNo = "NA";
                            }

                            if (response.optString("FlatNo") != null && !response.optString("FlatNo").equalsIgnoreCase("")) {
                                mStringFlatNo = response.optString("FlatNo");
                            } else {
                                mStringFlatNo = "NA";
                            }

                            if (mBooleanIsBusinessAdmin) {
                                openCustomerInfoDialog();

                            } else {
                                if (mMediaPlayerScanSuccess != null)
                                    mMediaPlayerScanSuccess.release();
                                recreate();
                            }
                        } else {

                            initTransactionId();
                            JSONObject jsonDeal = response.getJSONObject("Deal");
                            JSONObject jsonAccount = jsonDeal.getJSONObject("Account");
                            JSONObject jsonBusinessLocation = jsonDeal.getJSONObject("BusinessLocation");


                            mStringPassesId = response.getString("Id");
                            mStringPurchaseDate = response.getString("PurchaseDate");


                            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                            Date newDate = null;
                            try {
                                newDate = format.parse(mStringPurchaseDate);
                            } catch (ParseException e) {
                                e.printStackTrace();
                                recreate();

                            }

                            format = new SimpleDateFormat("MMM dd,yyyy hh:mm aa");
                            mStringPurchaseDate = format.format(newDate);

                            //it supposed to be here
                            // mStringPaymentTransactionId=response.getString("TransactionId");


                            mStringReceiptNumber = response.getString("ReceiptNumber");
                            mStringParkingStartTime = response.getString("ParkingStartTime");
                            mStringBusinessTitle = jsonAccount.getString("BusinessName");
                            mStringPaymentStatus = response.getString("PaymentStatus");
//                        mStringCustomerName=jsonAccount.getString("Firstname")+" "+jsonAccount.getString("Lastname");
                            //it will be used in future
                            //mStringValetLocationName=jsonAccount.getString("ValetLocation");
                            mStringLocationName = jsonBusinessLocation.getString("Title");
                            mStringCharges = response.getString("ParkingBalance");
                            mStringTotalHour = response.getString("TotalParkingTime");
                            mIsValidated = response.getBoolean("IsValidated");
                            mIntIsPrinting = response.optBoolean("IsPrinting");

                            try {
                                mIntIsBoomBarrier = jsonDeal.getInt("IsBoomBarrier");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            mStringDealTitle = response.getString("DealTitle");
                            if (mIsValidated) {
                                Toast.makeText(ActivityQrCodeReader.this, "Already Validated", Toast.LENGTH_SHORT).show();
                                recreate();
//                            mZBarScannerView.startCamera();

//                            onBackPressed();

                            } else {

                                if (mBooleanIsBusinessAdmin && mBooleanIsUser) {
                                    openCustomerInfoDialog();
                                } else {
                                    if (mMediaPlayerScanSuccess != null)
                                        mMediaPlayerScanSuccess.release();
//                                mZBarScannerView.startCamera();

//                                onBackPressed();

//                                    if (!GlobalMethods.isNetworkStatusAvialable(ActivityQrCodeReader.this)) {
//                                        GlobalMethods.isNetworkConnection(ActivityQrCodeReader.this);
//                                    } else {
//                                        if (!mBooleanIsEntry) {
//                                            new getBuyDeals().execute(null, null, null);
//                                        }
//                                    }
                                }


                            }
                            if (!mIsValidated) {
                                if (mBooleanIsUser) {
                                    Toast.makeText(ActivityQrCodeReader.this, getResources().getString(R.string.deal_validate_str_pass_validated_user), Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(ActivityQrCodeReader.this, getResources().getString(R.string.deal_validate_str_pass_validated_not_user), Toast.LENGTH_SHORT).show();
                                }

                                if (mIntIsPrinting && !mBooleanIsUser) {
                                    mService = new BluetoothService(ActivityQrCodeReader.this, mHandler);
                                    if (!mService.isBTopen()) {
                                        Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                                        startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                                    } else {
                                        mBluetoothDevice = mService.getDevByName();
                                        if (mBluetoothDevice != null) {
                                            mService.connect(mBluetoothDevice);
                                        } else {
                                            Intent serverIntent = new Intent(ActivityQrCodeReader.this, DeviceListActivity.class);      //��������һ����Ļ
                                            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
                                        }
                                    }
                                }
//                                if (!GlobalMethods.isNetworkStatusAvialable(ActivityQrCodeReader.this)) {
//                                    GlobalMethods.isNetworkConnection(ActivityQrCodeReader.this);
//                                } else {
//                                    new getBuyDeals().execute(null, null, null);
//                                }
                            } else {
                                Toast.makeText(ActivityQrCodeReader.this, "Already Validated", Toast.LENGTH_SHORT).show();
                                recreate();

                            }
                        }
                    } else {
                        if (response.getString("Message").equalsIgnoreCase("Payment is pending") || response.getString("Message").equalsIgnoreCase("QR Code is already Scanned")) {
                            mMediaPlayerScanSuccess = MediaPlayer.create(ActivityQrCodeReader.this, R.raw.scannedalreadynew);

                            mMediaPlayerScanSuccess.start();
                            Toast.makeText(
                                    ActivityQrCodeReader.this,
                                    getResources().getString(R.string.str_already_qr),
                                    Toast.LENGTH_LONG).show();
                            recreate();
                            GlobalMethods.callLogAPI(userId, false, true, "QR Code","Already scanned", getResources().getString(R.string.str_already_qr));
                        } else if (response.getString("Message").equalsIgnoreCase("Parking Time has not start yet")) {
                            Toast.makeText(
                                    ActivityQrCodeReader.this,
                                    getResources().getString(R.string.str_parking_not_start_qr),
                                    Toast.LENGTH_LONG).show();
                            recreate();
                            GlobalMethods.callLogAPI(userId, false, true, "QR Code", getResources().getString(R.string.str_parking_not_start_qr), getResources().getString(R.string.str_parking_not_start_qr));
                        } else if (response.getString("Message").equalsIgnoreCase("Pass is Expired")) {
                            mMediaPlayerScanSuccess = MediaPlayer.create(ActivityQrCodeReader.this, R.raw.passexpired);
                            mMediaPlayerScanSuccess.start();
                            Toast.makeText(
                                    ActivityQrCodeReader.this, "Pass is Expired",
                                    Toast.LENGTH_LONG).show();
                            recreate();
                            GlobalMethods.callLogAPI(userId, false, true, "QR Code", "Pass is Expired", "Pass is Expired");
                        } else {
                            mMediaPlayerScanSuccess = MediaPlayer.create(ActivityQrCodeReader.this, R.raw.scannedalreadynew);
                            mMediaPlayerScanSuccess.start();

                            Toast.makeText(
                                    ActivityQrCodeReader.this,
                                    getResources().getString(R.string.str_invalid_qr),
                                    Toast.LENGTH_LONG).show();
                            recreate();
                            GlobalMethods.callLogAPI(userId, false, true, "QR Code", getResources().getString(R.string.str_invalid_qr), getResources().getString(R.string.str_invalid_qr));
                        }
                    }


                } catch (JSONException e) {
                    e.printStackTrace();

                    Toast.makeText(ActivityQrCodeReader.this, "Something Went Wrong", Toast.LENGTH_SHORT).show();
                    recreate();
//                    onBackPressed();

                }


            } else {
                mMediaPlayerScanSuccess = MediaPlayer.create(ActivityQrCodeReader.this, R.raw.scanfail);
                mMediaPlayerScanSuccess.start();

                Toast.makeText(
                        ActivityQrCodeReader.this,
                        getResources().getString(R.string.str_invalid_qr),
                        Toast.LENGTH_LONG).show();
                recreate();
                GlobalMethods.callLogAPI(userId, false, true, "QR Code", getResources().getString(R.string.str_invalid_qr), getResources().getString(R.string.str_invalid_qr));
//                onBackPressed();
            }
        }
    }

    private void sendQrCodeToServer(String qrCodeData) {
        GlobalMethods.showProgress(this, "Loading...");
        ApiService apiService = ApiUtils.getApiService();
        apiService.sendQrCodeToServer(qrCodeData, userId).enqueue(new Callback<QRPinResponse>() {
            @Override
            public void onResponse(Call<QRPinResponse> call, Response<QRPinResponse> response) {
                Log.e("GF", "sendQrCodeToServer Response Code: " + response.code());
                if (response.isSuccessful() && response.code() == 200) {
                    parseResponse(response.body());
                }
                GlobalMethods.dismissProgress();
            }

            @Override
            public void onFailure(Call<QRPinResponse> call, Throwable t) {
                GlobalMethods.dismissProgress();
                Log.e("MF", "Error: " + t.toString());
            }
        });
    }

    private void parseResponse(QRPinResponse qrPinResponse) {
        if (qrPinResponse != null) {
            Log.d("EPA", "Message: " + qrPinResponse.getMessage());
            boolean isOTP = mSharedPreference.getBoolean("pref_is_otp", false);
            if ("Success".equalsIgnoreCase(qrPinResponse.getMessage())) {
                GlobalMethods.callLogAPI(userId, true, true, "QR Code", "QR Code scanned successfully", "QR Code scanned successfully");
                openMemberInfoDialogForSociety(qrPinResponse);
                if (isBluetooth)
                    sendDataToDevice();
            } else if ("Qrcode scanned successfully for Guest".equalsIgnoreCase(qrPinResponse.getMessage())) {
                GlobalMethods.callLogAPI(userId, true, true, "QR Code", "QR Code scanned successfully for Guest", "QR Code scanned successfully for Guest");
                openMemberInfoDialogForSociety(qrPinResponse);
                if (isOTP) {
                    if (socket != null) {
                        disconnect();
                        service = null;
                    }
                } else {
                    if (isBluetooth)
                        sendDataToDevice();
                }
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        //Do something after 4sec
                        try {
                            if (isOTP) {
                                goToNextScreen();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }, 4000);
            } else {
                if (qrPinResponse.getMessage() != null) {
                    GlobalMethods.showToast(this, qrPinResponse.getMessage());
                    GlobalMethods.callLogAPI(userId, false, true, "QR Code", qrPinResponse.getMessage(), qrPinResponse.getMessage());
                }
                else {
                    GlobalMethods.showToast(this, getString(R.string.str_invalid_qr));
                    GlobalMethods.callLogAPI(userId, false, true, "QR Code", getString(R.string.str_invalid_qr), getString(R.string.str_invalid_qr));
                }
            }
        } else {
            GlobalMethods.showToast(this, getString(R.string.str_invalid_qr));
            GlobalMethods.callLogAPI(userId, false, true, "QR Code", getString(R.string.str_invalid_qr), getString(R.string.str_invalid_qr));
        }
    }

    public void sendDataToDevice() {
        if (connected != Connected.True) {
            GlobalMethods.showToast(this, "Not connected to device, please check bluetooth device settings again");
            GlobalMethods.callLogAPI(userId, false, true, "Bluetooth", "Not Connected", "Not connected to device, please check bluetooth device settings again");
            startActivityForResult(new Intent(this, BluetoothLEActivity.class), 777);
            return;
        }

        try {
            byte[] data = "A".getBytes();
            socket.write(data);
            GlobalMethods.callLogAPI(userId, true, true, "Bluetooth", "Sent message 'A' to bluetooth device", "Sent message to bluetooth device");
        } catch (Exception e) {
            onSerialIoError(e);
        }
    }

    private void goToNextScreen() {
        Intent intent = new Intent(ActivityQrCodeReader.this, EnterPinActivity.class);
        intent.putExtra("showOTPLayout", true);
        startActivity(intent);
    }

    public void openMemberInfoDialogForSociety(QRPinResponse qrPinResponse) {
        mDialogCustomer = new Dialog(this);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        mDialogCustomer.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mDialogCustomer.setContentView(R.layout.raw_customer_info_popup);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        mDialogCustomer.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        mDialogCustomer.setCancelable(false);
        TextView mTextViewCustomerName = mDialogCustomer.findViewById(R.id.raw_customer_info_popup_tv_customer_name);
        TextView mTextViewDealName = mDialogCustomer.findViewById(R.id.raw_customer_info_popup_tv_deal_name);
        TextView mTextViewBusinessName = mDialogCustomer.findViewById(R.id.raw_customer_info_popup_tv_business_name);
        TextView mTextViewBusinessNamelbl = mDialogCustomer.findViewById(R.id.raw_customer_info_popup_tv_label_business_name);
        TextView mTextViewPurchaseDate = mDialogCustomer.findViewById(R.id.raw_customer_info_popup_tv_purchase_date);
        TextView mTextViewPaymentStatus = mDialogCustomer.findViewById(R.id.raw_customer_info_popup_tv_payment_status);
        TextView mTextViewDialogTitle = mDialogCustomer.findViewById(R.id.raw_customer_info_popup_tv_header_title);
        TextView mTextViewLabelCustomerName = mDialogCustomer.findViewById(R.id.raw_customer_info_tv_label_customer_name);
        TextView mTextViewLabelDealName = mDialogCustomer.findViewById(R.id.raw_customer_info_popup_tv_label_deal_name);
        TextView mTextViewLabelPurchaseDate = mDialogCustomer.findViewById(R.id.raw_customer_info_popup_tv_label_purchase_date);
        TextView mTextViewLabelFlatNoLabel = mDialogCustomer.findViewById(R.id.raw_customer_info_popup_tv_label_flat_no);
        TextView mTextViewLabelFlatNoValue = mDialogCustomer.findViewById(R.id.raw_customer_info_popup_tv_flat_no_value);
        TextView mTextViewLabelBlockNoValue = mDialogCustomer.findViewById(R.id.raw_customer_info_popup_tv_block_no_value);
        TextView mTextViewLabelBlockNolabel = mDialogCustomer.findViewById(R.id.raw_customer_info_popup_tv_label_block_no);
        LinearLayout mLinearLayoutPaymentStatus = mDialogCustomer.findViewById(R.id.raw_customer_info_popup_ll_payment_status);
        LinearLayout mLinearLayoutBlockNoMain = mDialogCustomer.findViewById(R.id.block_no_holder);
        LinearLayout mLinearLayoutFlatNoMain = mDialogCustomer.findViewById(R.id.flat_no_holder);
        LinearLayout mLinearLayoutBusinessMain = mDialogCustomer.findViewById(R.id.business_holder);
        LinearLayout mLinearLayoutDealNameMain = mDialogCustomer.findViewById(R.id.deal_name_holder);

        mLinearLayoutDealNameMain.setVisibility(View.GONE);
        mLinearLayoutBusinessMain.setVisibility(View.GONE);
        mLinearLayoutBlockNoMain.setVisibility(View.VISIBLE);
        mLinearLayoutFlatNoMain.setVisibility(View.VISIBLE);
        mTextViewLabelDealName.setText("Mobile No.");
        mTextViewLabelPurchaseDate.setText("Vehicle No.");
        mTextViewLabelCustomerName.setText("Name");
        mTextViewCustomerName.setText(qrPinResponse.getUserName());
        mTextViewDealName.setText(qrPinResponse.getPUMobile());
        mTextViewBusinessName.setText("NA");
        mTextViewDialogTitle.setText("Details");
        mTextViewLabelFlatNoValue.setText(String.valueOf(qrPinResponse.getFlatNo()));
        mTextViewLabelBlockNoValue.setText(String.valueOf(qrPinResponse.getBlockNo()));
        mTextViewPurchaseDate.setText(qrPinResponse.getPUVehicleNo());
        mLinearLayoutPaymentStatus.setVisibility(View.GONE);

        mDialogCustomer.show();

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //Do something after 4000ms
                try {
                    mDialogCustomer.dismiss();
                    finish();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 4000);
    }

    public void openCustomerInfoDialog() {
        mDialogCustomer = new Dialog(this);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        mDialogCustomer.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mDialogCustomer.setContentView(R.layout.raw_customer_info_popup);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        mDialogCustomer.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        mDialogCustomer.setCancelable(false);
        TextView mTextViewCustomerName = mDialogCustomer.findViewById(R.id.raw_customer_info_popup_tv_customer_name);
        TextView mTextViewDealName = mDialogCustomer.findViewById(R.id.raw_customer_info_popup_tv_deal_name);
        TextView mTextViewBusinessName = mDialogCustomer.findViewById(R.id.raw_customer_info_popup_tv_business_name);
        TextView mTextViewBusinessNamelbl = mDialogCustomer.findViewById(R.id.raw_customer_info_popup_tv_label_business_name);
        TextView mTextViewPurchaseDate = mDialogCustomer.findViewById(R.id.raw_customer_info_popup_tv_purchase_date);
        TextView mTextViewPaymentStatus = mDialogCustomer.findViewById(R.id.raw_customer_info_popup_tv_payment_status);
        TextView mTextViewDialogTitle = mDialogCustomer.findViewById(R.id.raw_customer_info_popup_tv_header_title);
        TextView mTextViewLabelCustomerName = mDialogCustomer.findViewById(R.id.raw_customer_info_tv_label_customer_name);
        TextView mTextViewLabelDealName = mDialogCustomer.findViewById(R.id.raw_customer_info_popup_tv_label_deal_name);
        TextView mTextViewLabelPurchaseDate = mDialogCustomer.findViewById(R.id.raw_customer_info_popup_tv_label_purchase_date);
        TextView mTextViewLabelFlatNoLabel = mDialogCustomer.findViewById(R.id.raw_customer_info_popup_tv_label_flat_no);
        TextView mTextViewLabelFlatNoValue = mDialogCustomer.findViewById(R.id.raw_customer_info_popup_tv_flat_no_value);
        TextView mTextViewLabelBlockNolabel = mDialogCustomer.findViewById(R.id.raw_customer_info_popup_tv_block_no_value);
        TextView mTextViewLabelBlockNoValue = mDialogCustomer.findViewById(R.id.raw_customer_info_popup_tv_label_block_no);
        LinearLayout mLinearLayoutPaymentStatus = mDialogCustomer.findViewById(R.id.raw_customer_info_popup_ll_payment_status);
        LinearLayout mLinearLayoutBlockNoMain = mDialogCustomer.findViewById(R.id.block_no_holder);
        LinearLayout mLinearLayoutFlatNoMain = mDialogCustomer.findViewById(R.id.flat_no_holder);
        LinearLayout mLinearLayoutBusinessMain = mDialogCustomer.findViewById(R.id.business_holder);
        LinearLayout mLinearLayoutDealNameMain = mDialogCustomer.findViewById(R.id.deal_name_holder);

        if (mIntIsMember) {
            mLinearLayoutDealNameMain.setVisibility(View.GONE);
            mLinearLayoutBusinessMain.setVisibility(View.GONE);
            mLinearLayoutBlockNoMain.setVisibility(View.VISIBLE);
            mLinearLayoutFlatNoMain.setVisibility(View.VISIBLE);
            mTextViewLabelDealName.setText("Mobile No.");
            mTextViewLabelPurchaseDate.setText("Vehicle No.");
            mTextViewCustomerName.setText(mStringCustomerName);
            mTextViewDealName.setText(mStringMobileNo);
            mTextViewBusinessName.setText("NA");
            mTextViewDialogTitle.setText("Member Info");
            mTextViewLabelFlatNoValue.setText(mStringFlatNo);
            mTextViewLabelBlockNoValue.setText(mStringBlockNo);
            mTextViewPurchaseDate.setText(mStringVehicleNo);
            mLinearLayoutPaymentStatus.setVisibility(View.GONE);

        } else {
            mLinearLayoutBusinessMain.setVisibility(View.VISIBLE);
            mLinearLayoutDealNameMain.setVisibility(View.VISIBLE);
            mLinearLayoutBlockNoMain.setVisibility(View.GONE);
            mLinearLayoutFlatNoMain.setVisibility(View.GONE);
            mTextViewCustomerName.setText(mStringCustomerName);
            mTextViewDealName.setText(mStringDealTitle);
            mTextViewBusinessName.setText(mStringBusinessTitle + "," + mStringLocationName);
            mTextViewDialogTitle.setText("Customer Info");
            mTextViewPurchaseDate.setText(mStringPurchaseDate);
            mTextViewPaymentStatus.setText(mStringPaymentStatus);
            mLinearLayoutPaymentStatus.setVisibility(View.VISIBLE);
        }
//        mImageViewDialogQrCode.setImageResource(passesList.get(mPosition).get_QRCode());
        mDialogCustomer.show();

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //Do something after 100ms
                try {
                    mDialogCustomer.dismiss();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (mMediaPlayerScanSuccess != null)
                    mMediaPlayerScanSuccess.release();

//                Toast.makeText(ActivityQrCodeReader.this, getResources().getString(R.string.deal_validate_str_pass_validated), Toast.LENGTH_SHORT).show();
//                onBackPressed();
                recreate();


//                if (!GlobalMethods.isNetworkStatusAvialable(ActivityQrCodeReader.this)) {
//                    GlobalMethods.isNetworkConnection(ActivityQrCodeReader.this);
//                } else {
//                    new getBuyDeals().execute(null, null, null);
//                }

            }
        }, 4000);


    }

    class getBuyDeals extends AsyncTask<Void, Void, String> {

        String mStringUserID = "";

        public getBuyDeals() {
            mStringUserID = mSharedPreference.getString("pref_user_UID", "");
        }

        @Override
        protected String doInBackground(Void... params) {
            String userresponse;
            JSONObject json = new JSONObject();
            Log.d("URL", Constant.API_VALIDATEPASSPARKING + mStringPassesId + "/validatepassandsplitforremeningbalance?transactionId=" + mStringPaymentTransactionId + "&paymentMode=" + "ByCash" + "&userId=" + mStringUserID);
            userresponse = Function.performPostCall(
                    Constant.API_VALIDATEPASSPARKING + mStringPassesId + "/validatepassandsplitforremeningbalance?transactionId=" + mStringPaymentTransactionId + "&paymentMode=" + "ByCash" + "&userId=" + mStringUserID, json);
            return userresponse;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            GlobalMethods.showProgress(ActivityQrCodeReader.this,
                    getString(R.string.deal_detail_screen_str_buy_deals));
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (result != null && !result.equals("")) {
                GlobalMethods.Print("Qr parkingValidate" + result);
                try {
                    JSONObject jsonObject = new JSONObject(result);
                    if (jsonObject != null && !jsonObject.isNull("Message")) {
                        Log.d("Message", jsonObject.getString("Message"));
                        if (jsonObject
                                .getString("Message")
                                .equals(getResources().getString(R.string.str_authorization_denied))) {
//                            alertUnauthorizedUser();
                            Toast.makeText(ActivityQrCodeReader.this, getResources().getString(R.string.str_authorization_denied), Toast.LENGTH_SHORT).show();
                            recreate();

                        } else if (jsonObject.getString("Message").equals(
                                getResources().getString(R.string.str_error_occured))) {
                            Toast.makeText(
                                    ActivityQrCodeReader.this,
                                    getResources().getString(R.string.fragment_passes_screen_str_somthing_wrong),
                                    Toast.LENGTH_SHORT).show();
                            recreate();
                        }
                    } else {
                        Toast.makeText(ActivityQrCodeReader.this, getResources().getString(R.string.deal_validate_str_pass_validated), Toast.LENGTH_SHORT).show();

                        if (mBooleanIsUser) {
                            Toast.makeText(ActivityQrCodeReader.this, getResources().getString(R.string.deal_validate_str_pass_validated_user), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(ActivityQrCodeReader.this, getResources().getString(R.string.deal_validate_str_pass_validated_not_user), Toast.LENGTH_SHORT).show();
                        }

                        if (mIntIsPrinting && !mBooleanIsUser) {
                            mService = new BluetoothService(ActivityQrCodeReader.this, mHandler);
                            if (!mService.isBTopen()) {
                                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                                startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                            } else {
                                mBluetoothDevice = mService.getDevByName();
                                if (mBluetoothDevice != null) {
                                    mService.connect(mBluetoothDevice);
                                } else {
                                    Intent serverIntent = new Intent(ActivityQrCodeReader.this, DeviceListActivity.class);      //��������һ����Ļ
                                    startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
                                }
                            }
                        } else {
//                            if(mIntIsBoomBarrier==1){
//                            openBoomBarrierDialog();
//                            }else{
                            recreate();
//                            }

                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    recreate();

                }

            } else {
                Toast.makeText(
                        ActivityQrCodeReader.this,
                        getString(R.string.fragment_passes_screen_str_somthing_wrong),
                        Toast.LENGTH_SHORT).show();
//                onBackPressed();

                recreate();

            }
            GlobalMethods.dismissProgress();
        }
    }

    private void initTransactionId() {
        String mStringUserID = mSharedPreference.getString("pref_user_UID", "");
        Calendar cal1 = Calendar.getInstance();
        cal1.setTimeZone(TimeZone.getTimeZone("GMT"));
        mStringPaymentTransactionId = "BPN_" + mStringUserID + "_" + mStringDealID + "_" + (cal1.getTimeInMillis() / 1000);
    }

    private void openBoomBarrierDialog() {

        if (mDialogBoomBarrier == null) {
            mDialogBoomBarrier = new Dialog(ActivityQrCodeReader.this);
            mDialogBoomBarrier.requestWindowFeature(Window.FEATURE_NO_TITLE);
            mDialogBoomBarrier.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
            mDialogBoomBarrier.setContentView(R.layout.dialog_boom_barrier_new);
            mDialogBoomBarrier.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            mDialogBoomBarrier.setCancelable(false);
        }

        TextView mTextViewOpen = mDialogBoomBarrier.findViewById(R.id.dialog_boom_barrier_tv_open);
        TextView mTextViewClose = mDialogBoomBarrier.findViewById(R.id.dialog_boom_barrier_tv_close);
        ImageView mImageViewCancel = mDialogBoomBarrier.findViewById(R.id.dialog_boom_barrier_iv_cancel);

        mTextViewOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new sendBoomBarrierDetails(1).execute(null, null, null);
            }
        });

        mTextViewClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new sendBoomBarrierDetails(0).execute(null, null, null);
            }
        });

        mImageViewCancel.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("WrongConstant")
            @Override
            public void onClick(View v) {
                mDialogBoomBarrier.dismiss();
                startActivity(new Intent(ActivityQrCodeReader.this, HomeScreen.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
                finish();
            }
        });

        if (!mDialogBoomBarrier.isShowing())
            mDialogBoomBarrier.show();
    }

    class sendBoomBarrierDetails extends AsyncTask<Void, Void, String> {


        private int mIntBarrier;

        public sendBoomBarrierDetails(int mIntBarrier) {
            this.mIntBarrier = mIntBarrier;

        }

        @Override
        protected String doInBackground(Void... params) {
            String userresponse;

            GlobalMethods.Print("BoomBarriers Url: " + Constant.API_BOOMBARRIER + "?passId=" + mStringPassesId);

            userresponse = Function.performGETCall(
                    Constant.API_BOOMBARRIER + "?passId=" + mStringPassesId);
            return userresponse;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (mIntBarrier == 1) {
                GlobalMethods.showProgress(ActivityQrCodeReader.this, "OPENING BARRIERS...");
            } else {
                GlobalMethods.showProgress(ActivityQrCodeReader.this, "CLOSING BARRIERS...");
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            GlobalMethods.dismissProgress();
            if (mIntBarrier == 1) {
                Toast.makeText(ActivityQrCodeReader.this, "BARRIER OPENED SUCCESSFULLY", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(ActivityQrCodeReader.this, "BARRIER CLOSED SUCCESSFULLY", Toast.LENGTH_SHORT).show();
            }
        }
    }


    //Static strings are there because it is handler so it may cause exception
    // Handler for handling printer output
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BluetoothService.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    GlobalMethods.dismissProgress();
                                    Toast.makeText(ActivityQrCodeReader.this, "Connection successful",
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                            GlobalMethods.callLogAPI(userId, true, true, "Bluetooth","Connected to printer", "Connection successful, Printing Receipt");
                            printReceipt();
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            Log.d("tagConnectCheck", "Connecting to the device..");
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    GlobalMethods.showProgress(ActivityQrCodeReader.this, "Connecting to the device..");
                                }
                            });
                            break;
                        case BluetoothService.STATE_LISTEN:
                        case BluetoothService.STATE_NONE:
                            Log.d("tagConnectCheck", "None");
                            break;
                    }
                    break;
                case BluetoothService.MESSAGE_CONNECTION_LOST:
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            GlobalMethods.dismissProgress();
                        }
                    });
                    mService.stop();
                    GlobalMethods.callLogAPI(userId, false, true, "Bluetooth","Connection lost with printer", "Connection lost with printer");
                    break;
                case BluetoothService.MESSAGE_UNABLE_CONNECT:     //�޷������豸
                    Toast.makeText(ActivityQrCodeReader.this, "Unable to connect device",
                            Toast.LENGTH_SHORT).show();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!isFinishing()) {
                                GlobalMethods.dismissProgress();
//                                if (mBooleanIsSafeToBackPress) {
//                                    if(mIntIsBoomBarrier==1){
//                                        openBoomBarrierDialog();
//                                    }else{
                                onBackPressed();
//                                recreate();

//                                    }
//                                }
                            }
                        }
                    });
                    mService.stop();
                    GlobalMethods.callLogAPI(userId, false, true, "Bluetooth","Unable to connect to printer device", "Unable to connect to device");
                    break;
            }
        }
    };

    private void printReceipt() {
        try {
            byte[] ESC_ALIGN_CENTER = new byte[]{0x1b, 'a', 0x01}; // Center Aling
            mService.write(ESC_ALIGN_CENTER);
            byte[] bb3 = new byte[]{0x1B, 0x21, 0x10};
            mService.write(bb3);
            mService.sendMessage("BuyPassNow\n", "GBK");
            mService.write(new byte[]{0x1B, 0x40});

            byte[] ESC_ALIGN_LEFT = new byte[]{0x1b, 'a', 0x00}; // Center Aling
            mService.write(ESC_ALIGN_LEFT);
            Calendar mDate = Calendar.getInstance();
            String mStrStartDate = "";
            String date = "";


            if (mStringParkingStartTime != null && !mStringParkingStartTime.equalsIgnoreCase("") && !mStringParkingStartTime.equalsIgnoreCase("null")) {
                mStrStartDate = mStringParkingStartTime;

                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                Date newDate = null;
                try {
                    newDate = format.parse(mStrStartDate);

                } catch (ParseException e) {
                    e.printStackTrace();
                    mService.stop();
                }
                format = new SimpleDateFormat("MMM dd,yyyy hh:mm a");

                date = format.format(newDate);

            } else {
                date = getResources().getString(R.string.str_notavailable);
            }
            SimpleDateFormat format = new SimpleDateFormat("MMM dd,yyyy hh:mm a");

            String currentDateTimeString = format.format(mDate.getTime());
            if (mStringVehicleNo != null && !mStringVehicleNo.equalsIgnoreCase("null") && !mStringVehicleNo.equalsIgnoreCase("")) {
            } else {
                mStringVehicleNo = getResources().getString(R.string.str_notavailable);
            }

            if (mStringBusinessTitle != null && !mStringBusinessTitle.equalsIgnoreCase("null") && !mStringBusinessTitle.equalsIgnoreCase("")) {
            } else {
                mStringBusinessTitle = getResources().getString(R.string.str_notavailable);
            }
            if (mStringValetLocationName != null && !mStringValetLocationName.equalsIgnoreCase("null") && !mStringValetLocationName.equalsIgnoreCase("")) {
            } else {
                mStringValetLocationName = getResources().getString(R.string.str_notavailable);
            }

            if (mStringLocationName != null && !mStringLocationName.equalsIgnoreCase("null") && !mStringLocationName.equalsIgnoreCase("")) {
            } else {
                mStringLocationName = getResources().getString(R.string.str_notavailable);
            }

            if (mStringReceiptNumber != null && !mStringReceiptNumber.equalsIgnoreCase("null") && !mStringReceiptNumber.equalsIgnoreCase("")) {
            } else {
                mStringReceiptNumber = getResources().getString(R.string.str_notavailable);
            }

            if (mStringDealTitle != null && !mStringDealTitle.equalsIgnoreCase("null") && !mStringDealTitle.equalsIgnoreCase("")) {
            } else {
                mStringDealTitle = getResources().getString(R.string.str_notavailable);
            }
            //**Static Strings Are Mandatory Here**//
            //We cannot use this strings in Strings.xml file because spaces are not happening in this file during printing
            String msg = this.mStringBusinessTitle + ", " + this.mStringLocationName + "\nD&T     : " + currentDateTimeString + "\nRCPT.NO : " + this.mStringReceiptNumber + "\n" + this.mStringDealTitle + "\nVEH.NO  : " + this.mStringVehicleNo + "\n";
            if (!this.mStringAccountId.equalsIgnoreCase(Constant.ALANKAR_PASS_ID)) {
                msg = msg + "LOC.ID  : " + this.mStringValetLocationName + "\n";
            }
            msg = msg + "IN D&T  : " + date + "\nOUT D&T : " + currentDateTimeString + "\nHours   : " + this.mStringTotalHour + "\nCharges : Rs." + this.mStringCharges + " Paid\n";

            System.out.print("\n" + msg);
//            byte[] cc = new byte[]{0x1B,0x21,0x03};
//            mService.write(cc);

            mService.sendMessage(msg, "GBK");
            mService.write(ESC_ALIGN_CENTER);
//            mService.write(bb3);
            mService.sendMessage("FACILITATION CHARGES.\nTHANK YOU.VISIT AGAIN\n\n\n", "GBK");

            mService.stop();
//            if(mIntIsBoomBarrier==1){
//                openBoomBarrierDialog();
//            }
//            else{
            startActivity(new Intent(ActivityQrCodeReader.this, HomeScreen.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
//            }
        } catch (Exception e) {
            mService.stop();
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        isBackPressed = true;
        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CONNECT_DEVICE) {
            if (resultCode == Activity.RESULT_OK) {
                if (data != null && data.getExtras() != null) {//�ѵ�������б��е�ĳ���豸��
                    String address = data.getExtras()
                            .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);  //��ȡ�б������豸��mac��ַ
                    if (address != null && address.contains(":")) {
                        mBluetoothDevice = mService.getDevByMac(address);
                        if (mBluetoothDevice != null) {
                            mService.connect(mBluetoothDevice);
                        }
                    } else {

                        mBluetoothDevice = mService.getDevByName();
                        if (mBluetoothDevice != null) {
                            mService.connect(mBluetoothDevice);
                        } else {
                            Intent serverIntent = new Intent(ActivityQrCodeReader.this, DeviceListActivity.class);      //��������һ����Ļ
                            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
                            if (!getClass().getSimpleName().equals("ActivityQrCodeReader")) {
                                finish();
                            }
                        }
                    }
                }
            }
        } else if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(ActivityQrCodeReader.this, "Bluetooth successfully enabled", Toast.LENGTH_LONG).show();
                mBluetoothDevice = mService.getDevByName();
                if (mBluetoothDevice != null) {
                    mService.connect(mBluetoothDevice);
                } else {
                    Intent serverIntent = new Intent(ActivityQrCodeReader.this, DeviceListActivity.class);      //��������һ����Ļ
                    startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
                }
            } else {
//                if(mIntIsBoomBarrier==1){
//                    openBoomBarrierDialog();
//                }else {
                startActivity(new Intent(ActivityQrCodeReader.this, HomeScreen.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
                finish();
//                }
            }
        } else if (isSociety && requestCode == 777) {
            if (resultCode == Activity.RESULT_OK) {
                if (data != null && data.getExtras() != null) {
                    deviceAddress = data.getExtras().getString("bAddress");
                }
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mBooleanIsSafeToBackPress = false;

        mZBarScannerView.setResultHandler(this); // Register ourselves as a handler for scan results.
        mCameraId = BPNApplication.getCameraID();
        System.out.println("CAMERAID...." + mCameraId);
        mZBarScannerView.startCamera(mCameraId);// Start camera on resume

        if (isSociety && isBluetooth) {
            if (initialStart && service != null) {
                initialStart = false;
                runOnUiThread(this::connect);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mZBarScannerView.stopCamera();           // Stop camera on pause
        mBooleanIsSafeToBackPress = false;
    }

    public void setStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(ActivityQrCodeReader.this, R.color.colorPrimaryDark));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBooleanIsSafeToBackPress = false;
        System.out.println("DESTROYED....");
        if (isBackPressed) {
            BPNApplication.setCameraID(0);
            isBackPressed = false;
        }

        if (isSociety && isBluetooth) {
            if (connected != Connected.False) {
                if (socket != null)
                    disconnect();
            }
            stopService(new Intent(ActivityQrCodeReader.this, SerialService.class));
            unbindService(ActivityQrCodeReader.this);
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        mBooleanIsSafeToBackPress = true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        mBooleanIsSafeToBackPress = false;

        if (isSociety && isBluetooth) {
            if (service != null)
                service.attach(ActivityQrCodeReader.this);
            else {
                startService(new Intent(ActivityQrCodeReader.this, SerialService.class)); // prevents service destroy on unbind from recreated activity caused by orientation change
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mBooleanIsSafeToBackPress = false;

        if (isSociety && isBluetooth) {
            if (service != null && !isChangingConfigurations())
                service.detach();

            initialStart = true; //Made true here to connect to device when first selected from device list after enabling bluetooth.
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        service = ((SerialService.SerialBinder) binder).getService();
        if (initialStart) {
            initialStart = false;
            runOnUiThread(this::connect);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        service = null;
    }

    /*
     * SerialListener
     */
    @Override
    public void onSerialConnect() {
        GlobalMethods.showToast(this, "connected to bluetooth device");
        connected = Connected.True;
    }

    @Override
    public void onSerialConnectError(Exception e) {
        GlobalMethods.showToast(this, "connection failed: " + e.getMessage());
        GlobalMethods.callLogAPI(userId, false, true, "Bluetooth","connection failed", "Bluetooth connection failed");
        if (socket != null)
            disconnect();
    }

    @Override
    public void onSerialRead(byte[] data) {
//        receive(data);
    }

    @Override
    public void onSerialIoError(Exception e) {
        GlobalMethods.showToast(this, "connection lost: " + e.getMessage());
        GlobalMethods.callLogAPI(userId, false, true, "Bluetooth","connection lost", "Bluetooth connection lost");
        if (socket != null)
            disconnect();
    }

    /*
     * Serial + UI
     */
    private void connect() {
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothDevice tem_dev = null;
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            if (pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                    if (device.getName().contains(Constant.BLUETOOTH_LE_DEVICE_NAME)) {
                        tem_dev = device;
                        break;
                    }
                }
            }

            if (tem_dev != null) {
                Log.e("AQCR", "Connected Device: " + tem_dev);
                BluetoothDevice device = bluetoothAdapter.getRemoteDevice(tem_dev.getAddress());
                String deviceName = device.getName() != null ? device.getName() : device.getAddress();
                GlobalMethods.showToast(this, "connecting...");
                connected = Connected.Pending;
                socket = new SerialSocket();
                service.connect(this, "Connected to " + deviceName);
                socket.connect(this, service, device);
                GlobalMethods.callLogAPI(userId, true, true, "Bluetooth","Connection successful", "Connected to " + deviceName);
            } else {
                if (TextUtils.isEmpty(deviceAddress)) {
                    if (BPNApplication.bluetoothDeviceNotFound) {
                        BPNApplication.bluetoothDeviceNotFound = false;
                        GlobalMethods.showToast(this, "Bluetooth device is offline, please check settings");
                    }
                    else
                        startActivityForResult(new Intent(this, BluetoothLEActivity.class), 777);
                }
                else {
                    BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
                    String deviceName = device.getName() != null ? device.getName() : device.getAddress();
                    GlobalMethods.showToast(this, "connecting...");
                    connected = Connected.Pending;
                    socket = new SerialSocket();
                    service.connect(this, "Connected to " + deviceName);
                    socket.connect(this, service, device);
                    GlobalMethods.callLogAPI(userId, true, true, "Bluetooth","Connection successful", "Connected to " + deviceName);
                }
            }
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
}
