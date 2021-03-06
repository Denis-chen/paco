package com.pacoapp.paco.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.pacoapp.paco.R;
import com.pacoapp.paco.UserPreferences;
import com.pacoapp.paco.model.Event;
import com.pacoapp.paco.model.Experiment;
import com.pacoapp.paco.model.ExperimentProviderUtil;
import com.pacoapp.paco.net.NetworkUtil;
import com.pacoapp.paco.net.SyncService;
import com.pacoapp.paco.os.RingtoneUtil;
import com.pacoapp.paco.sensors.android.diagnostics.DiagnosticReport;
import com.pacoapp.paco.sensors.android.diagnostics.DiagnosticsReporter;
import com.pacoapp.paco.sensors.android.diagnostics.UnsyncedEventDiagnostic;

public class TroubleshootingActivity extends ActionBarActivity {

  private UserPreferences userPrefs;
  private DiagnosticReport diagnosticsReport;
  private TextView resultsTextView;
  private Button forceSyncButton;
  private ExperimentProviderUtil experimentProviderUtil;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_troubleshooting);

    userPrefs = new UserPreferences(getApplicationContext());

    ActionBar actionBar = getSupportActionBar();
    actionBar.setLogo(R.drawable.ic_launcher);
    actionBar.setDisplayUseLogoEnabled(true);
    actionBar.setDisplayShowHomeEnabled(true);
    actionBar.setBackgroundDrawable(new ColorDrawable(0xff4A53B3));
    actionBar.setDisplayHomeAsUpEnabled(true);

    resultsTextView = (TextView)findViewById(R.id.resultsTextView2);
    final Button sendLogButton = (Button)findViewById(R.id.sendPacoLogButton);
    sendLogButton.setEnabled(false);
    sendLogButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        launchLogSender();
      }
    });

    Button checkSetupButton = (Button)findViewById(R.id.checkSetupButton);
    checkSetupButton.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {
        runTests();
        sendLogButton.setEnabled(true);
      }
    });

    forceSyncButton = (Button)findViewById(R.id.forceSyncButton);
    forceSyncButton.setEnabled(false);
    
    forceSyncButton.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {
        startService(new Intent(TroubleshootingActivity.this, SyncService.class));
      }
    });
    
    
  }



  protected void runTests() {
    diagnosticsReport = new DiagnosticsReporter().runTests(this);
    String res = diagnosticsReport.toString();
    resultsTextView.setText(res);    
    
    forceSyncButton.setEnabled(NetworkUtil.isConnected(this) && hasUnsyncedEvents());
      
  }



  private boolean hasUnsyncedEvents() {
    // TODO make diagnostics cleaner and use that value
    ExperimentProviderUtil ep = new ExperimentProviderUtil(this);
    List<Event> events = ep.getEventsNeedingUpload();
    return events != null && events.size() > 0;
  }



  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
      int id = item.getItemId();
      if (id == android.R.id.home) {
        finish();
        return true;
      }
      return super.onOptionsItemSelected(item);
  }

  protected void launchDebug() {
    Intent startIntent = new Intent(this, ESMSignalViewer.class);
    startActivity(startIntent);
  }

  private void launchLogSender() {
    String res = getString(R.string.troubleshooting_no_diagnostics_report_message);
    if (diagnosticsReport != null) {
      res = diagnosticsReport.toString();
    }
    
    String log = readLog();
    experimentProviderUtil = new ExperimentProviderUtil(this);
    List<Experiment> experiments = experimentProviderUtil.getJoinedExperiments();
    String email = null;
    if (experiments != null && experiments.size() == 1) {
      email = experiments.get(0).getExperimentDAO().getContactEmail();
      if (Strings.isNullOrEmpty(email)) {
        email = experiments.get(0).getExperimentDAO().getCreator();
      }
    }
    createEmailIntent(res + "\n\n" + log, email);
  }


  private void createEmailIntent(String log, String email) {
    Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
    emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.email_subject_paco_feedback));
    emailIntent.setType("plain/text");
    emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, log);
    if (!Strings.isNullOrEmpty(email)) {
      emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{email});
    }
    startActivity(emailIntent);
  }

  private String readLog() {
    StringBuilder log = new StringBuilder();
    try {
      Process process = Runtime.getRuntime().exec("logcat -d");
      BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

      String line;
      while ((line = bufferedReader.readLine()) != null) {
        log.append(line).append("\n");
      }
    } catch (IOException e) {
      return null;
    }
    return log.toString();
  }

  @SuppressLint("NewApi")
  private void launchAccountChooser() {
    Intent intent = new Intent(this, SplashActivity.class);
    intent.putExtra(SplashActivity.EXTRA_CHANGING_EXISTING_ACCOUNT, true);
    startActivity(intent);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);    
  }


  @Override
  protected void onResume() {
    super.onResume();
  }


  @Override
  protected void onPause() {
    super.onPause();
  }


}
