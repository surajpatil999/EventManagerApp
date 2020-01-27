package com.itoneclick.buypassnow;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Pair;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.devexpress.dxcharts.AxisLabel;
import com.devexpress.dxcharts.AxisTitle;
import com.devexpress.dxcharts.BarSeries;
import com.devexpress.dxcharts.BarSeriesLabel;
import com.devexpress.dxcharts.BarSeriesStyle;
import com.devexpress.dxcharts.Chart;
import com.devexpress.dxcharts.ChartStyle;
import com.devexpress.dxcharts.DonutSeries;
import com.devexpress.dxcharts.Hint;
import com.devexpress.dxcharts.Legend;
import com.devexpress.dxcharts.LegendHorizontalPosition;
import com.devexpress.dxcharts.LegendOrientation;
import com.devexpress.dxcharts.LegendStyle;
import com.devexpress.dxcharts.LegendVerticalPosition;
import com.devexpress.dxcharts.NumericAxisY;
import com.devexpress.dxcharts.PieCenterTextLabel;
import com.devexpress.dxcharts.PieChart;
import com.devexpress.dxcharts.PieChartStyle;
import com.devexpress.dxcharts.PieHint;
import com.devexpress.dxcharts.PieSeriesHintOptions;
import com.devexpress.dxcharts.PieSeriesLabel;
import com.devexpress.dxcharts.PieSeriesLabelPosition;
import com.devexpress.dxcharts.PieSeriesLabelStyle;
import com.devexpress.dxcharts.SelectionBehavior;
import com.devexpress.dxcharts.SelectionChangedInfo;
import com.devexpress.dxcharts.SelectionChangedListener;
import com.devexpress.dxcharts.SeriesHintOptions;
import com.devexpress.dxcharts.SeriesLabelStyle;
import com.devexpress.dxcharts.SeriesPointInfo;
import com.devexpress.dxcharts.TextStyle;
import com.devexpress.dxcharts.TooltipHintBehavior;
import com.google.gson.Gson;
import com.itoneclick.buypassnow.dev_express.LoadingDrawable;
import com.itoneclick.buypassnow.dev_express.PieSeriesDataAdapter;
import com.itoneclick.buypassnow.dev_express.QualitativeArraySeriesDataAdapter;
import com.itoneclick.buypassnow.global.Constant;
import com.itoneclick.buypassnow.global.Function;
import com.itoneclick.buypassnow.global.GlobalMethods;
import com.itoneclick.buypassnow.model.UserReportData;
import com.itoneclick.buypassnow.model.Value;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class UserReportBarChartActivity extends AppCompatActivity {
    private int pointID = 0;
    private int[] palette;
    public static int BUYRQUESTCODE = 135;

    private List<List<Double>> combinedDataList = new ArrayList<>();
    private List<List<Double>> currentCollectionList = new ArrayList<>();
    private List<List<Double>> previousCollectionList = new ArrayList<>();
    private List<List<Double>> thresholdCollectionList = new ArrayList<>();
    private Set<String> dayNameSet = new LinkedHashSet<>();

    private SharedPreferences sharedpreference;
    private SharedPreferences sharedpreferenceFilter;
    public String userID, mStringBusinessId, venueId, currentDate, userToken;
    private boolean isThresholdPresent = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_report_bar_chart);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        sharedpreference = getSharedPreferences(Constant.PREFERANCEBPN, 0);
        sharedpreferenceFilter = getSharedPreferences(Constant.PREFERANCE_FILTER, 0);

        userID = sharedpreference.getString("pref_user_UID", "");

        if (sharedpreference.getString("pref_user_firstname", "") != null && !sharedpreference.getString(
                "pref_user_firstname", "").equalsIgnoreCase("")) {
            boolean isFb = sharedpreference.getBoolean("pref_isLoginwithFB", false);
            if (!isFb) {
                mStringBusinessId = sharedpreference.getString(Constant.PREF_BUSINESS_ACCOUNT_ID, "");
            } else {
                mStringBusinessId = sharedpreference.getString(Constant.PREF_FB_BUSINESS_ACCOUNT_ID, "");
            }
        }

        Log.e("mStringBusinessId", mStringBusinessId);
        venueId = sharedpreferenceFilter.getString("pref_Refine_VenueID", "");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        currentDate =  sdf.format(new Date());
        Log.e("UserRA:", "CurrentDate: " + currentDate);

        userToken = sharedpreference.getString("pref_user_Token", "");

        Log.e("userID", userID);
        Log.e("userToken", userToken);

//        parseJsonData();
        new GetUserData().execute();
    }

    private void initCharts() {
        this.palette = getResources().getIntArray(R.array.chart_palette_selection);
        ((TextView) findViewById(R.id.detail_title_text_view_master)).setText("Income of the week, day wise");
        final PieChart chartMaster = findViewById(R.id.chart_master);
        final Chart chartDetail = findViewById(R.id.chart_detail);
        chartMaster.setLoadingDrawable(new LoadingDrawable(this));
        chartDetail.setLoadingDrawable(new LoadingDrawable(this));

        DonutSeries donutSeries = new DonutSeries();
        donutSeries.setDisplayName("Sales Comparison");
        donutSeries.setData(new PieSeriesDataAdapter(getDaysData()));
        donutSeries.setLabel(new PieSeriesLabel());

        //To customise text of label
        TextStyle textStyle = new TextStyle();
//        textStyle.setColor(Color.BLACK);
//        textStyle.setSize(30f);
        textStyle.setTypeface(Typeface.DEFAULT_BOLD);
        donutSeries.getLabel().setStyle(new PieSeriesLabelStyle());
        donutSeries.getLabel().getStyle().setTextStyle(textStyle);

        donutSeries.getLabel().setTextPattern("{L}: {V}");
        donutSeries.getLabel().setPosition(PieSeriesLabelPosition.INSIDE);
        donutSeries.setCenterLabel(new PieCenterTextLabel("Total\n{TV}"));

        chartMaster.addSeries(donutSeries);
        Legend legend = new Legend();
        chartMaster.setLegend(legend);
        LegendStyle legendStyle = new LegendStyle();
        legend.setStyle(legendStyle);
        int orientation = getResources().getConfiguration().orientation;
        int screenDensity = (int) getResources().getDisplayMetrics().density;
        legend.setHorizontalPosition(LegendHorizontalPosition.RIGHT);
        legend.setVerticalPosition(LegendVerticalPosition.BOTTOM);
        legend.setOrientation(LegendOrientation.TOP_TO_BOTTOM);
        legend.setVisible(false);
//        legend.setOrientation(orientation == Configuration.ORIENTATION_LANDSCAPE ? LegendOrientation.TOP_TO_BOTTOM : LegendOrientation.LEFT_TO_RIGHT);
//        legend.setVerticalPosition(orientation == Configuration.ORIENTATION_LANDSCAPE ? LegendVerticalPosition.CENTER : LegendVerticalPosition.BOTTOM_OUTSIDE);
//        legend.setHorizontalPosition(orientation == Configuration.ORIENTATION_LANDSCAPE ? LegendHorizontalPosition.RIGHT_OUTSIDE : LegendHorizontalPosition.CENTER);

        PieChartStyle chartStyle = new PieChartStyle();
        chartMaster.setStyle(chartStyle);
        chartStyle.setPalette(palette);
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            chartStyle.setPadding(20 * screenDensity, 15 * screenDensity, 5 * screenDensity, 25 * screenDensity);
            legendStyle.setPadding(0, null, null, null);
        } else {
            chartStyle.setPadding(20 * screenDensity, 15 * screenDensity, 20 * screenDensity, 10 * screenDensity);
            legendStyle.setPadding(null, 5 * screenDensity, null, null);
        }

        //To show hint on click of slice
        chartMaster.setHint(new PieHint());

        chartMaster.setSelectionBehavior(SelectionBehavior.EXPLODE);
        chartMaster.setSelected(0, pointID, true);
        chartMaster.setSelectionChangedListener(new SelectionChangedListener() {
            @Override
            public void onSelectionChanged(SelectionChangedInfo selectionChangedInfo) {
                SeriesPointInfo selectedInfo = selectionChangedInfo.getSelectedInfo();
                SeriesPointInfo deselectedInfo = selectionChangedInfo.getDeselectedInfo();
                if (selectedInfo != null && selectedInfo.getDataPointIndices() != null && selectedInfo.getDataPointIndices().length > 0) {
                    pointID = selectedInfo.getDataPointIndices()[0];

                    combinedDataList.clear();
                    combinedDataList.add(currentCollectionList.get(pointID));
                    if (isThresholdPresent)
                        combinedDataList.add(thresholdCollectionList.get(pointID));
                    combinedDataList.add(previousCollectionList.get(pointID));

                    onChangePointID(chartDetail);
                } else if (deselectedInfo != null && deselectedInfo.getDataPointIndices() != null && deselectedInfo.getDataPointIndices().length > 0) {
                    pointID = -1;
                    onChangePointID(chartDetail);
                }
            }
        });

        chartDetail.setAxisY(new NumericAxisY());
        chartDetail.getAxisY().setLabel(new AxisLabel());
        chartDetail.getAxisY().setAlwaysShowZeroLevel(false);

        chartDetail.setLegend(new Legend());
        Hint hint = new Hint();
        hint.setBehavior(new TooltipHintBehavior());
        chartDetail.setHint(hint);

        ChartStyle chartStyle1 = new ChartStyle();
        chartDetail.setStyle(chartStyle1);
        chartStyle1.setPalette(getResources().getIntArray(R.array.bar_chart_palette));

        combinedDataList.add(currentCollectionList.get(pointID));
        if (isThresholdPresent)
            combinedDataList.add(thresholdCollectionList.get(pointID));
        combinedDataList.add(previousCollectionList.get(pointID));

        chartDetail.getAxisY().setTitle(new AxisTitle("Income, rupees"));
        chartDetail.getLegend().setHorizontalPosition(LegendHorizontalPosition.RIGHT);
        chartDetail.getLegend().setVerticalPosition(LegendVerticalPosition.TOP);
        chartDetail.getLegend().setOrientation(LegendOrientation.TOP_TO_BOTTOM);

        onChangePointID(chartDetail);
    }

    private void onChangePointID(Chart chartDetail) {
        chartDetail.removeAllSeries();
        for (int i = 0; i < initializeCategoryNames().size(); i++) {
            BarSeries series = new BarSeries();
            series.setDisplayName(initializeCategoryNames().get(i));
            series.setData(new QualitativeArraySeriesDataAdapter(initializeData().get(i)));
            series.setLabel(new BarSeriesLabel());
            series.getLabel().setVisible(true);

            //To customise text of label
            TextStyle textStyle = new TextStyle();
//            textStyle.setColor(Color.BLACK);
//            textStyle.setSize(25f);
            textStyle.setTypeface(Typeface.DEFAULT_BOLD);
            series.getLabel().setStyle(new SeriesLabelStyle());
            series.getLabel().getStyle().setTextStyle(textStyle);

            chartDetail.addSeries(series);
            series.setHintOptions(new SeriesHintOptions());
            series.getHintOptions().setPointTextPattern("{A}, {S}:\n{V$#,###}");

        }
        if (pointID != -1)
            ((TextView) findViewById(R.id.detail_title_text_view_detail)).setText((getDays().get(pointID)) + " Income, Hour Wise");
        else
            ((TextView) findViewById(R.id.detail_title_text_view_detail)).setText("");
    }

    final List<String> initializeCategoryNames() {
        if (isThresholdPresent)
            return Arrays.asList("Current", "Threshold", "Previous");
        else
            return Arrays.asList("Current", "Previous");
    }

    List<List<Pair<String, Double>>> initializeData() {
        List<List<Pair<String, Double>>> result = new ArrayList<>();
        List<String> args = Arrays.asList("06:00 AM", "12:00 PM", "06:00 PM", "12:00 AM");
        List<List<Double>> vals = combinedDataList;
        for (int i = 0; i < vals.size(); i++) {
            List<Pair<String, Double>> r = new ArrayList<>();
            for (int j = 0; j < vals.get(i).size(); j++) {
                r.add(new Pair<>(args.get(j), vals.get(i).get(j)));
            }
            result.add(r);
        }
        return result;
    }

    /*************Data Set Up For Pie Chart********************/
    public List<Pair<String, Double>> getDaysData() {
        List<Pair<String, Double>> result = new ArrayList<>();
        List<String> args = getDays();
        Log.e("UserRA",  "Day List Size: " + args.size());
        Log.e("UserRA",  "Value List Size: " + initializeRegionSummaryValues().size());
        List<Double> vals = initializeRegionSummaryValues();
        for (int i = 0; i < vals.size(); i++) {
            result.add(new Pair<>(args.get(i), vals.get(i)));
        }
        return result;
    }

    private List<String> getDays() {
        return new ArrayList<>(dayNameSet);
    }

    private List<List<Double>> initializeCurrentValues() {
        return Arrays.asList(
                Arrays.asList(50D, 15D, 20D, 25D),
                Arrays.asList(30D, 18D, 22D, 25D),
                Arrays.asList(20D, 44D, 25D, 25D),
                Arrays.asList(12D, 22D, 26D, 25D),
                Arrays.asList(25D, 40D, 28D, 25D),
                Arrays.asList(40D, 30D, 50D, 25D),
                Arrays.asList(35D, 25D, 20D, 25D)
        );
    }

    private List<List<Double>> initializePreValues() {
        return Arrays.asList(
                Arrays.asList(30D, 10D, 20D, 25D),
                Arrays.asList(34D, 20D, 22D, 25D),
                Arrays.asList(20D, 44D, 25D, 35D),
                Arrays.asList(12D, 22D, 26D, 15D),
                Arrays.asList(15D, 40D, 28D, 45D),
                Arrays.asList(30D, 30D, 50D, 50D),
                Arrays.asList(25D, 25D, 20D, 34D)
        );
    }

    private List<List<Double>> initializeThresholdValues() {
        return Arrays.asList(
                Arrays.asList(20D, 15D, 20D, 25D),
                Arrays.asList(22D, 18D, 22D, 25D),
                Arrays.asList(25D, 44D, 25D, 25D),
                Arrays.asList(43D, 22D, 26D, 25D),
                Arrays.asList(27D, 40D, 28D, 25D),
                Arrays.asList(44D, 30D, 50D, 25D),
                Arrays.asList(32D, 25D, 20D, 25D)
        );
    }

    private List<Double> initializeRegionSummaryValues() {
        List<Double> result = new ArrayList<>();
//        List<List<Double>> values = initializeCurrentValues();
        List<List<Double>> values = currentCollectionList;
        for (List<Double> region : values) {
            double sum = 0;
            for (Double v : region)
                sum += v;
            result.add(sum);
        }
        return result;
    }

    private void parseJsonData(UserReportData userReportData) {
        List<Double> monCurColList = new ArrayList<>();
        List<Double> tueCurColList = new ArrayList<>();
        List<Double> wedCurColList = new ArrayList<>();
        List<Double> thurCurColList = new ArrayList<>();
        List<Double> friCurColList = new ArrayList<>();
        List<Double> satCurColList = new ArrayList<>();
        List<Double> sunCurColList = new ArrayList<>();

        List<Double> monPreColList = new ArrayList<>();
        List<Double> tuePreColList = new ArrayList<>();
        List<Double> wedPreColList = new ArrayList<>();
        List<Double> thurPreColList = new ArrayList<>();
        List<Double> friPreColList = new ArrayList<>();
        List<Double> satPreColList = new ArrayList<>();
        List<Double> sunPreColList = new ArrayList<>();

        List<Double> monThreColList = new ArrayList<>();
        List<Double> tueThreColList = new ArrayList<>();
        List<Double> wedThreColList = new ArrayList<>();
        List<Double> thurThreColList = new ArrayList<>();
        List<Double> friThreColList = new ArrayList<>();
        List<Double> satThreColList = new ArrayList<>();
        List<Double> sunThreColList = new ArrayList<>();

        boolean bolMon = false, bolTue = false, bolWed = false, bolThur = false, bolFri = false, bolSat = false, bolSun = false;

        for (int i = 0; i < 4; i++) {
            monCurColList.add(0D);
            tueCurColList.add(0D);
            wedCurColList.add(0D);
            thurCurColList.add(0D);
            friCurColList.add(0D);
            satCurColList.add(0D);
            sunCurColList.add(0D);

            monPreColList.add(0D);
            tuePreColList.add(0D);
            wedPreColList.add(0D);
            thurPreColList.add(0D);
            friPreColList.add(0D);
            satPreColList.add(0D);
            sunPreColList.add(0D);

            monThreColList.add(0D);
            tueThreColList.add(0D);
            wedThreColList.add(0D);
            thurThreColList.add(0D);
            friThreColList.add(0D);
            satThreColList.add(0D);
            sunThreColList.add(0D);
        }

        for (Value value : userReportData.getValues()) {

            String strDay = "";
            switch (value.getDay()) {
                case "Monday":
                    strDay = "Mon";
                    break;
                case "Tuesday":
                    strDay = "Tue";
                    break;
                case "Wednesday":
                    strDay = "Wed";
                    break;
                case "Thursday":
                    strDay = "Thur";
                    break;
                case "Friday":
                    strDay = "Fri";
                    break;
                case "Saturday":
                    strDay = "Sat";
                    break;
                case "Sunday":
                    strDay = "Sun";
                    break;
            }

//            dayNameSet.add(value.getDay());
            dayNameSet.add(strDay);

            switch (value.getDay()) {
                case "Monday":
                    bolMon = true;
                    switch (value.getTime()) {
                        case "06:00 AM":
                            monCurColList.set(0, Double.valueOf(value.getCollection()));
                            if (value.getPreCollection() != null)
                                monPreColList.set(0, Double.valueOf(value.getPreCollection()));
                            if (value.getThreshold() != null)
                                monThreColList.set(0, Double.valueOf(value.getThreshold()));
                            break;
                        case "12:00 PM":
                            monCurColList.set(1, Double.valueOf(value.getCollection()));
                            if (value.getPreCollection() != null)
                                monPreColList.set(1, Double.valueOf(value.getPreCollection()));
                            if (value.getThreshold() != null)
                                monThreColList.set(1, Double.valueOf(value.getThreshold()));
                            break;
                        case "06:00 PM":
                            monCurColList.set(2, Double.valueOf(value.getCollection()));
                            if (value.getPreCollection() != null)
                                monPreColList.set(2, Double.valueOf(value.getPreCollection()));
                            if (value.getThreshold() != null)
                                monThreColList.set(2, Double.valueOf(value.getThreshold()));
                            break;
                        case "12:00 AM":
                            monCurColList.set(3, Double.valueOf(value.getCollection()));
                            if (value.getPreCollection() != null)
                                monPreColList.set(3, Double.valueOf(value.getPreCollection()));
                            if (value.getThreshold() != null)
                                monThreColList.set(3, Double.valueOf(value.getThreshold()));
                            break;
                    }
                    break;
                case "Tuesday":
                    bolTue = true;
                    switch (value.getTime()) {
                        case "06:00 AM":
                            tueCurColList.set(0, Double.valueOf(value.getCollection()));
                            if (value.getPreCollection() != null)
                                tuePreColList.set(0, Double.valueOf(value.getPreCollection()));
                            if (value.getThreshold() != null)
                                tueThreColList.set(0, Double.valueOf(value.getThreshold()));
                            break;
                        case "12:00 PM":
                            tueCurColList.set(1, Double.valueOf(value.getCollection()));
                            if (value.getPreCollection() != null)
                                tuePreColList.set(1, Double.valueOf(value.getPreCollection()));
                            if (value.getThreshold() != null)
                                tueThreColList.set(1, Double.valueOf(value.getThreshold()));
                            break;
                        case "06:00 PM":
                            tueCurColList.set(2, Double.valueOf(value.getCollection()));
                            if (value.getPreCollection() != null)
                                tuePreColList.set(2, Double.valueOf(value.getPreCollection()));
                            if (value.getThreshold() != null)
                                tueThreColList.set(2, Double.valueOf(value.getThreshold()));
                            break;
                        case "12:00 AM":
                            tueCurColList.set(3, Double.valueOf(value.getCollection()));
                            if (value.getPreCollection() != null)
                                tuePreColList.set(3, Double.valueOf(value.getPreCollection()));
                            if (value.getThreshold() != null)
                                tueThreColList.set(3, Double.valueOf(value.getThreshold()));
                            break;
                    }
                    break;
                case "Wednesday":
                    bolWed = true;
                    switch (value.getTime()) {
                        case "06:00 AM":
                            wedCurColList.set(0, Double.valueOf(value.getCollection()));
                            if (value.getPreCollection() != null)
                                wedPreColList.set(0, Double.valueOf(value.getPreCollection()));
                            if (value.getThreshold() != null)
                                wedThreColList.set(0, Double.valueOf(value.getThreshold()));
                            break;
                        case "12:00 PM":
                            wedCurColList.set(1, Double.valueOf(value.getCollection()));
                            if (value.getPreCollection() != null)
                                wedPreColList.set(1, Double.valueOf(value.getPreCollection()));
                            if (value.getThreshold() != null)
                                wedThreColList.set(1, Double.valueOf(value.getThreshold()));
                            break;
                        case "06:00 PM":
                            wedCurColList.set(2, Double.valueOf(value.getCollection()));
                            if (value.getPreCollection() != null)
                                wedPreColList.set(2, Double.valueOf(value.getPreCollection()));
                            if (value.getThreshold() != null)
                                wedThreColList.set(2, Double.valueOf(value.getThreshold()));
                            break;
                        case "12:00 AM":
                            wedCurColList.set(3, Double.valueOf(value.getCollection()));
                            if (value.getPreCollection() != null)
                                wedPreColList.set(3, Double.valueOf(value.getPreCollection()));
                            if (value.getThreshold() != null)
                                wedThreColList.set(3, Double.valueOf(value.getThreshold()));
                            break;
                    }
                    break;
                case "Thursday":
                    bolThur = true;
                    switch (value.getTime()) {
                        case "06:00 AM":
                            thurCurColList.set(0, Double.valueOf(value.getCollection()));
                            if (value.getPreCollection() != null)
                                thurPreColList.set(0, Double.valueOf(value.getPreCollection()));
                            if (value.getThreshold() != null)
                                thurThreColList.set(0, Double.valueOf(value.getThreshold()));
                            break;
                        case "12:00 PM":
                            thurCurColList.set(1, Double.valueOf(value.getCollection()));
                            if (value.getPreCollection() != null)
                                thurPreColList.set(1, Double.valueOf(value.getPreCollection()));
                            if (value.getThreshold() != null)
                                thurThreColList.set(1, Double.valueOf(value.getThreshold()));
                            break;
                        case "06:00 PM":
                            thurCurColList.set(2, Double.valueOf(value.getCollection()));
                            if (value.getPreCollection() != null)
                                thurPreColList.set(2, Double.valueOf(value.getPreCollection()));
                            if (value.getThreshold() != null)
                                thurThreColList.set(2, Double.valueOf(value.getThreshold()));
                            break;
                        case "12:00 AM":
                            thurCurColList.set(3, Double.valueOf(value.getCollection()));
                            if (value.getPreCollection() != null)
                                thurPreColList.set(3, Double.valueOf(value.getPreCollection()));
                            if (value.getThreshold() != null)
                                thurThreColList.set(3, Double.valueOf(value.getThreshold()));
                            break;
                    }
                    break;
                case "Friday":
                    bolFri = true;
                    switch (value.getTime()) {
                        case "06:00 AM":
                            friCurColList.set(0, Double.valueOf(value.getCollection()));
                            if (value.getPreCollection() != null)
                                friPreColList.set(0, Double.valueOf(value.getPreCollection()));
                            if (value.getThreshold() != null)
                                friThreColList.set(0, Double.valueOf(value.getThreshold()));
                            break;
                        case "12:00 PM":
                            friCurColList.set(1, Double.valueOf(value.getCollection()));
                            if (value.getPreCollection() != null)
                                friPreColList.set(1, Double.valueOf(value.getPreCollection()));
                            if (value.getThreshold() != null)
                                friThreColList.set(1, Double.valueOf(value.getThreshold()));
                            break;
                        case "06:00 PM":
                            friCurColList.set(2, Double.valueOf(value.getCollection()));
                            if (value.getPreCollection() != null)
                                friPreColList.set(2, Double.valueOf(value.getPreCollection()));
                            if (value.getThreshold() != null)
                                friThreColList.set(2, Double.valueOf(value.getThreshold()));
                            break;
                        case "12:00 AM":
                            friCurColList.set(3, Double.valueOf(value.getCollection()));
                            if (value.getPreCollection() != null)
                                friPreColList.set(3, Double.valueOf(value.getPreCollection()));
                            if (value.getThreshold() != null)
                                friThreColList.set(3, Double.valueOf(value.getThreshold()));
                            break;
                    }
                    break;
                case "Saturday":
                    bolSat = true;
                    switch (value.getTime()) {
                        case "06:00 AM":
                            satCurColList.set(0, Double.valueOf(value.getCollection()));
                            if (value.getPreCollection() != null)
                                satPreColList.set(0, Double.valueOf(value.getPreCollection()));
                            if (value.getThreshold() != null)
                                satThreColList.set(0, Double.valueOf(value.getThreshold()));
                            break;
                        case "12:00 PM":
                            satCurColList.set(1, Double.valueOf(value.getCollection()));
                            if (value.getPreCollection() != null)
                                satPreColList.set(1, Double.valueOf(value.getPreCollection()));
                            if (value.getThreshold() != null)
                                satThreColList.set(1, Double.valueOf(value.getThreshold()));
                            break;
                        case "06:00 PM":
                            satCurColList.set(2, Double.valueOf(value.getCollection()));
                            if (value.getPreCollection() != null)
                                satPreColList.set(2, Double.valueOf(value.getPreCollection()));
                            if (value.getThreshold() != null)
                                satThreColList.set(2, Double.valueOf(value.getThreshold()));
                            break;
                        case "12:00 AM":
                            satCurColList.set(3, Double.valueOf(value.getCollection()));
                            if (value.getPreCollection() != null)
                                satPreColList.set(3, Double.valueOf(value.getPreCollection()));
                            if (value.getThreshold() != null)
                                satThreColList.set(3, Double.valueOf(value.getThreshold()));
                            break;
                    }
                    break;
                case "Sunday":
                    bolSun = true;
                    switch (value.getTime()) {
                        case "06:00 AM":
                            sunCurColList.set(0, Double.valueOf(value.getCollection()));
                            if (value.getPreCollection() != null)
                                sunPreColList.set(0, Double.valueOf(value.getPreCollection()));
                            if (value.getThreshold() != null)
                                sunThreColList.set(0, Double.valueOf(value.getThreshold()));
                            break;
                        case "12:00 PM":
                            sunCurColList.set(1, Double.valueOf(value.getCollection()));
                            if (value.getPreCollection() != null)
                                sunPreColList.set(1, Double.valueOf(value.getPreCollection()));
                            if (value.getThreshold() != null)
                                sunThreColList.set(1, Double.valueOf(value.getThreshold()));
                            break;
                        case "06:00 PM":
                            sunCurColList.set(2, Double.valueOf(value.getCollection()));
                            if (value.getPreCollection() != null)
                                sunPreColList.set(2, Double.valueOf(value.getPreCollection()));
                            if (value.getThreshold() != null)
                                sunThreColList.set(2, Double.valueOf(value.getThreshold()));
                            break;
                        case "12:00 AM":
                            sunCurColList.set(3, Double.valueOf(value.getCollection()));
                            if (value.getPreCollection() != null)
                                sunPreColList.set(3, Double.valueOf(value.getPreCollection()));
                            if (value.getThreshold() != null)
                                sunThreColList.set(3, Double.valueOf(value.getThreshold()));
                            break;
                    }
                    break;
            }
        }

        for (Double va : wedCurColList) {
            Log.e("UserRA", "Wed value: " + va);
        }

        for (String str : new ArrayList<>(dayNameSet)) {
            Log.e("UserRA", "Day: " + str);
        }

        //To hide threshold bar
//        if (userReportData.getValues().get(0).getThreshold() == null)
//            isThresholdPresent = false;

        switch (userReportData.getValues().get(0).getDay()) {
            case "Monday":
                currentCollectionList.add(monCurColList);
                currentCollectionList.add(sunCurColList);
                currentCollectionList.add(satCurColList);
                currentCollectionList.add(friCurColList);
                currentCollectionList.add(thurCurColList);
                currentCollectionList.add(wedCurColList);
                currentCollectionList.add(tueCurColList);

                previousCollectionList.add(monPreColList);
                previousCollectionList.add(sunPreColList);
                previousCollectionList.add(satPreColList);
                previousCollectionList.add(friPreColList);
                previousCollectionList.add(thurPreColList);
                previousCollectionList.add(wedPreColList);
                previousCollectionList.add(tuePreColList);

                thresholdCollectionList.add(monThreColList);
                thresholdCollectionList.add(sunThreColList);
                thresholdCollectionList.add(satThreColList);
                thresholdCollectionList.add(friThreColList);
                thresholdCollectionList.add(thurThreColList);
                thresholdCollectionList.add(wedThreColList);
                thresholdCollectionList.add(tueThreColList);

                break;
            case "Tuesday":
                currentCollectionList.add(tueCurColList);
                currentCollectionList.add(monCurColList);
                currentCollectionList.add(sunCurColList);
                currentCollectionList.add(satCurColList);
                currentCollectionList.add(friCurColList);
                currentCollectionList.add(thurCurColList);
                currentCollectionList.add(wedCurColList);

                previousCollectionList.add(tuePreColList);
                previousCollectionList.add(monPreColList);
                previousCollectionList.add(sunPreColList);
                previousCollectionList.add(satPreColList);
                previousCollectionList.add(friPreColList);
                previousCollectionList.add(thurPreColList);
                previousCollectionList.add(wedPreColList);

                thresholdCollectionList.add(tueThreColList);
                thresholdCollectionList.add(monThreColList);
                thresholdCollectionList.add(sunThreColList);
                thresholdCollectionList.add(satThreColList);
                thresholdCollectionList.add(friThreColList);
                thresholdCollectionList.add(thurThreColList);
                thresholdCollectionList.add(wedThreColList);

                break;
            case "Wednesday":
                currentCollectionList.add(wedCurColList);
                currentCollectionList.add(tueCurColList);
                currentCollectionList.add(monCurColList);
                currentCollectionList.add(sunCurColList);
                currentCollectionList.add(satCurColList);
                currentCollectionList.add(friCurColList);
                currentCollectionList.add(thurCurColList);

                previousCollectionList.add(wedPreColList);
                previousCollectionList.add(tuePreColList);
                previousCollectionList.add(monPreColList);
                previousCollectionList.add(sunPreColList);
                previousCollectionList.add(satPreColList);
                previousCollectionList.add(friPreColList);
                previousCollectionList.add(thurPreColList);

                thresholdCollectionList.add(wedThreColList);
                thresholdCollectionList.add(tueThreColList);
                thresholdCollectionList.add(monThreColList);
                thresholdCollectionList.add(sunThreColList);
                thresholdCollectionList.add(satThreColList);
                thresholdCollectionList.add(friThreColList);
                thresholdCollectionList.add(thurThreColList);

                break;
            case "Thursday":
                currentCollectionList.add(thurCurColList);
                currentCollectionList.add(wedCurColList);
                currentCollectionList.add(tueCurColList);
                currentCollectionList.add(monCurColList);
                currentCollectionList.add(sunCurColList);
                currentCollectionList.add(satCurColList);
                currentCollectionList.add(friCurColList);

                previousCollectionList.add(thurPreColList);
                previousCollectionList.add(wedPreColList);
                previousCollectionList.add(tuePreColList);
                previousCollectionList.add(monPreColList);
                previousCollectionList.add(sunPreColList);
                previousCollectionList.add(satPreColList);
                previousCollectionList.add(friPreColList);

                thresholdCollectionList.add(thurThreColList);
                thresholdCollectionList.add(wedThreColList);
                thresholdCollectionList.add(tueThreColList);
                thresholdCollectionList.add(monThreColList);
                thresholdCollectionList.add(sunThreColList);
                thresholdCollectionList.add(satThreColList);
                thresholdCollectionList.add(friThreColList);

                break;
            case "Friday":
                currentCollectionList.add(friCurColList);
                currentCollectionList.add(thurCurColList);
                currentCollectionList.add(wedCurColList);
                currentCollectionList.add(tueCurColList);
                currentCollectionList.add(monCurColList);
                currentCollectionList.add(sunCurColList);
                currentCollectionList.add(satCurColList);

                previousCollectionList.add(friPreColList);
                previousCollectionList.add(thurPreColList);
                previousCollectionList.add(wedPreColList);
                previousCollectionList.add(tuePreColList);
                previousCollectionList.add(monPreColList);
                previousCollectionList.add(sunPreColList);
                previousCollectionList.add(satPreColList);

                thresholdCollectionList.add(friThreColList);
                thresholdCollectionList.add(thurThreColList);
                thresholdCollectionList.add(wedThreColList);
                thresholdCollectionList.add(tueThreColList);
                thresholdCollectionList.add(monThreColList);
                thresholdCollectionList.add(sunThreColList);
                thresholdCollectionList.add(satThreColList);

                break;
            case "Saturday":
                currentCollectionList.add(satCurColList);
                currentCollectionList.add(friCurColList);
                currentCollectionList.add(thurCurColList);
                currentCollectionList.add(wedCurColList);
                currentCollectionList.add(tueCurColList);
                currentCollectionList.add(monCurColList);
                currentCollectionList.add(sunCurColList);

                previousCollectionList.add(satPreColList);
                previousCollectionList.add(friPreColList);
                previousCollectionList.add(thurPreColList);
                previousCollectionList.add(wedPreColList);
                previousCollectionList.add(tuePreColList);
                previousCollectionList.add(monPreColList);
                previousCollectionList.add(sunPreColList);

                thresholdCollectionList.add(satThreColList);
                thresholdCollectionList.add(friThreColList);
                thresholdCollectionList.add(thurThreColList);
                thresholdCollectionList.add(wedThreColList);
                thresholdCollectionList.add(tueThreColList);
                thresholdCollectionList.add(monThreColList);
                thresholdCollectionList.add(sunThreColList);

                break;
            case "Sunday":
                currentCollectionList.add(sunCurColList);
                currentCollectionList.add(satCurColList);
                currentCollectionList.add(friCurColList);
                currentCollectionList.add(thurCurColList);
                currentCollectionList.add(wedCurColList);
                currentCollectionList.add(tueCurColList);
                currentCollectionList.add(monCurColList);

                previousCollectionList.add(sunPreColList);
                previousCollectionList.add(satPreColList);
                previousCollectionList.add(friPreColList);
                previousCollectionList.add(thurPreColList);
                previousCollectionList.add(wedPreColList);
                previousCollectionList.add(tuePreColList);
                previousCollectionList.add(monPreColList);

                thresholdCollectionList.add(sunThreColList);
                thresholdCollectionList.add(satThreColList);
                thresholdCollectionList.add(friThreColList);
                thresholdCollectionList.add(thurThreColList);
                thresholdCollectionList.add(wedThreColList);
                thresholdCollectionList.add(tueThreColList);
                thresholdCollectionList.add(monThreColList);

                break;
        }

        if (!bolMon) {
            currentCollectionList.remove(monCurColList);
            previousCollectionList.remove(monPreColList);
            thresholdCollectionList.remove(monThreColList);
        }

        if (!bolTue) {
            currentCollectionList.remove(tueCurColList);
            previousCollectionList.remove(tuePreColList);
            thresholdCollectionList.remove(tueThreColList);
        }

        if (!bolWed) {
            currentCollectionList.remove(wedCurColList);
            previousCollectionList.remove(wedPreColList);
            thresholdCollectionList.remove(wedThreColList);
        }

        if (!bolThur) {
            currentCollectionList.remove(thurCurColList);
            previousCollectionList.remove(thurPreColList);
            thresholdCollectionList.remove(thurThreColList);
        }

        if (!bolFri) {
            currentCollectionList.remove(friCurColList);
            previousCollectionList.remove(friPreColList);
            thresholdCollectionList.remove(friThreColList);
        }

        if (!bolSat) {
            currentCollectionList.remove(satCurColList);
            previousCollectionList.remove(satPreColList);
            thresholdCollectionList.remove(satThreColList);
        }

        if (!bolSun) {
            currentCollectionList.remove(sunCurColList);
            previousCollectionList.remove(sunPreColList);
            thresholdCollectionList.remove(sunThreColList);
        }

        initCharts();
    }

    class GetUserData extends AsyncTask<Void, Void, String> {

        public GetUserData() {
        }

        @Override
        protected String doInBackground(Void... params) {
            // return callPass();
            String userresponse;

//            String URL = Constant.API_GET_USER_REPORT_DATA + "2019/07/31" + "&userId=" + mStringBusinessId + "&Locationid=" + venueId;
            String URL = Constant.API_GET_USER_REPORT_DATA + currentDate + "&userId=" + mStringBusinessId + "&Locationid=" + venueId;
            userresponse = Function.performGETCallWithAutho(URL, userToken);
            Log.e("URL", URL);
            return userresponse;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            GlobalMethods.showProgress(UserReportBarChartActivity.this, "Loading...");
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            if (result != null && !result.equals("")) {
                Log.e("PassesResult", result);
                try {
                    JSONObject jsonObject = new JSONObject(result);
                    if (jsonObject != null && !jsonObject.isNull("Message")) {
                        Log.d("Message", jsonObject.getString("Message"));
                        if (jsonObject.getString("Message").equals("Authorization has been denied for this request.")) {
                            alertUnauthorizedUser();
                        } else if (jsonObject.getString("Message").equals("An error has occurred.")) {
                            Toast.makeText(UserReportBarChartActivity.this, getString(R.string.fragment_passes_screen_str_somthing_wrong), Toast.LENGTH_LONG).show();
                        }
                    } else {
                        UserReportData userReportData = new Gson().fromJson(result, UserReportData.class);
                        if(userReportData.getValues().size() > 0)
                            parseJsonData(userReportData);
                        else
                            Toast.makeText(UserReportBarChartActivity.this, "Don't have data for current day", Toast.LENGTH_LONG).show();
                    }
                    GlobalMethods.dismissProgress();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void alertUnauthorizedUser() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle(getString(R.string.fragment_passes_screen_str_unauthorized_user));
        alertDialogBuilder.setMessage(getString(R.string.fragment_passes_screen_str_authorized_denied)).setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Boolean pref_isLoginwithFB = sharedpreference.getBoolean("pref_isLoginwithFB", false);
                        if (pref_isLoginwithFB) {
                        }
                        SharedPreferences.Editor editor = sharedpreference.edit();
                        editor.clear();
                        editor.commit();
                        Intent i = new Intent(UserReportBarChartActivity.this, LoginScreenAuthentication.class);
                        i.putExtra("isFromBuyDealDetailScreen", false);
                        startActivityForResult(i, BUYRQUESTCODE);
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
