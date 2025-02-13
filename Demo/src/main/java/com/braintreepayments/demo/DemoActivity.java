package com.braintreepayments.demo;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.ArrayAdapter;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.braintreepayments.api.BraintreeClient;

import java.util.Arrays;
import java.util.List;

public class DemoActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback, ActionBar.OnNavigationListener {

    private BraintreeClient braintreeClient;
    private AppBarConfiguration appBarConfiguration;

    private SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_demo);

        setupActionBar();
        setProgressBarIndeterminateVisibility(true);

        registerSharedPreferencesListener();
    }

    public BraintreeClient getBraintreeClient() {
        // lazily instantiate braintree client in case the demo has been reset
        if (braintreeClient == null) {
            boolean useAppLink = Settings.getPayPalLinkType(this).equals(getString(R.string.paypal_app_link));
            Uri appLinkUri = null;
            if (useAppLink) {
                appLinkUri = Uri.parse("https://mobile-sdk-demo-site-838cead5d3ab.herokuapp.com/");
            }
            if (Settings.useTokenizationKey(this) && Settings.showCheckoutExperience(this)) {
                String tokenizationKey = Settings.getPayPalCheckoutTokenizationKey(this);
                braintreeClient = new BraintreeClient(this, tokenizationKey);
            } else if (Settings.useTokenizationKey(this)) {
                String tokenizationKey = Settings.getTokenizationKey(this);
                braintreeClient = new BraintreeClient(this, tokenizationKey, null, appLinkUri);
            } else {
                braintreeClient =
                    BraintreeClientFactory.createBraintreeClientWithAuthorizationProvider(this, appLinkUri);
            }
        }
        return braintreeClient;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    private NavController getNavController() {
        Fragment navHostFragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        return NavHostFragment.findNavController(navHostFragment);
    }

    private void setupActionBar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        NavController navController = getNavController();
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupWithNavController(toolbar, navController, appBarConfiguration);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.environments, android.R.layout.simple_spinner_dropdown_item);
            actionBar.setListNavigationCallbacks(adapter, this);

            List<String> envs = Arrays.asList(getResources().getStringArray(R.array.environments));
            actionBar.setSelectedNavigationItem(envs.indexOf(Settings.getEnvironment(this)));
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = getNavController();
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    private void performReset() {
        setProgressBarIndeterminateVisibility(true);
        braintreeClient = null;
    }

    public void showDialog(String message) {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean didHandleSelection = false;

        int itemId = item.getItemId();
        if (itemId == R.id.reset) {
            performReset();
            didHandleSelection = true;
        } else if (itemId == R.id.settings) {
            NavController navController = getNavController();
            navController.navigate(R.id.open_settings_fragment);
            didHandleSelection = true;
        }
        return didHandleSelection;
    }

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        String env = getResources().getStringArray(R.array.environments)[itemPosition];
        if (!Settings.getEnvironment(this).equals(env)) {
            Settings.setEnvironment(this, env);
            performReset();
        }
        return true;
    }

    private void registerSharedPreferencesListener() {
        sharedPreferenceChangeListener = (sharedPreferences, s) -> {
            // reset api client
            DemoApplication.resetApiClient();
            performReset();
        };
        Settings.getPreferences(this)
                .registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    }
}