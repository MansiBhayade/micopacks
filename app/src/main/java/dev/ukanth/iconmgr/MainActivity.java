package dev.ukanth.iconmgr;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.danimahardhika.android.helpers.license.LicenseHelper;

import org.greenrobot.greendao.query.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dev.ukanth.iconmgr.dao.DaoSession;
import dev.ukanth.iconmgr.dao.IPObj;
import dev.ukanth.iconmgr.dao.IPObjDao;
import dev.ukanth.iconmgr.util.LicenseCallbackHelper;
import dev.ukanth.iconmgr.util.PackageComparator;

public class MainActivity extends AppCompatActivity implements SearchView.OnQueryTextListener, SwipeRefreshLayout.OnRefreshListener {
    private RecyclerView recyclerView;
    private TextView emptyView;

    private LicenseHelper mLicenseHelper;

    public static IconAdapter getAdapter() {
        return adapter;
    }

    private static IconAdapter adapter;

    private IPObjDao ipObjDao;
    private Query<IPObj> ipObjQuery;

    public static List<IPObj> getIconPacksList() {
        return iconPacksList;
    }

    private static List<IPObj> iconPacksList;

    private SwipeRefreshLayout mSwipeLayout;

    private MaterialDialog plsWait;

    private Menu mainMenu;


    public static void setReloadTheme(boolean reloadTheme) {
        MainActivity.reloadTheme = reloadTheme;
    }

    public static void setReloadApp(boolean b) {
        MainActivity.reloadApp = b;
    }

    private static boolean reloadTheme = false;
    private static boolean reloadApp = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Prefs.isDarkTheme(getApplicationContext())) {
            setTheme(R.style.AppTheme_Dark);
        }

        setContentView(R.layout.content_main);

        DaoSession daoSession = ((App) getApplication()).getDaoSession();
        ipObjDao = daoSession.getIPObjDao();

        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        emptyView = (TextView) findViewById(R.id.empty_view);


        iconPacksList = new ArrayList<>();

        LinearLayoutManager llm = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(llm);
        recyclerView.setHasFixedSize(true);

        adapter = new IconAdapter(MainActivity.this, iconPacksList);
        recyclerView.setAdapter(adapter);

        mSwipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        mSwipeLayout.setOnRefreshListener(this);

        loadApp(false);
        if (BuildConfig.LICENSE) {
            startLicenseCheck();
        }
    }

    private void startLicenseCheck() {

        byte[] salt = new byte[]{
                -11, 115, 10, -19, -33,
                -12, 18, -24, 21, 68,
                -15, -45, 97, -17, -16,
                -13, -11, 12, -14, 81
        };

        String licenseKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEApXY+Hz2FyJ7rgvDjNiisklEMS6o0fRtQHgPi8uDpJxhr5IrOBu0LE8utemYXZYkYU8Hx4dhFr/lcgXJf9Sg6XXMybSwq0mS/N6OFAhI6Mo9Hjaw7sKfmf/8ogyMMQ0s88qjE4A7J0Eu8I12Bw0e2zPSb3Nz/oi3Wz9G0weGf6lNAqcrGaZwxSN/5fVOjy5fafKlH52Iln0t2GSuW97yiakD2XERTeQGlpTq5Dm7Lp4Ve4SqfmFi9m9w5PKLZJgkotFPcH8VsZgqElAwM3UK0Q4+J1TvBeQxugZHI6Uc5vUJeFvPpL8lGK80Dh16Z4kMcJyJsZpjFz6aoI2VdFrNhkQIDAQAB";

        if (Prefs.isFirstTime(getApplicationContext())) {
            mLicenseHelper = new LicenseHelper(this);
            mLicenseHelper.run(licenseKey, salt, new LicenseCallbackHelper(this));
            return;
        }

        if (!Prefs.isPS(getApplicationContext())) {
            if (!Prefs.isPS(getApplicationContext())) {
                new MaterialDialog.Builder(MainActivity.this)
                        .title(R.string.license_check)
                        .content(getString(R.string.license_check_failed))
                        .positiveText(R.string.close)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                dialog.dismiss();
                                finish();
                            }
                        })
                        .cancelable(false)
                        .canceledOnTouchOutside(false)
                        .show();

            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mLicenseHelper != null) {
            mLicenseHelper.destroy();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (reloadTheme) {
            reloadTheme = false;
            restartActivity();
        }
        if (reloadApp) {
            reloadApp = false;
            restartActivity();
        }
    }

    private void restartActivity() {
        Intent i = getBaseContext().getPackageManager().getLaunchIntentForPackage(getBaseContext().getPackageName());
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
    }

    private void loadApp(boolean forceLoad) {
        LoadAppList getAppList = new LoadAppList();
        if (plsWait == null && (getAppList.getStatus() == AsyncTask.Status.PENDING ||
                getAppList.getStatus() == AsyncTask.Status.FINISHED)) {
            getAppList.setContext(forceLoad, this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    @Override
    public void onRefresh() {
        loadApp(true);
        mSwipeLayout.setRefreshing(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);

        mainMenu = menu;
        //make sure we update sort entry
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (Prefs.sortBy(getApplicationContext())) {
                    case "s0":
                        mainMenu.findItem(R.id.sort_alpha).setChecked(true);
                        break;
                    case "s1":
                        mainMenu.findItem(R.id.sort_lastupdate).setChecked(true);
                        break;
                    case "s2":
                        mainMenu.findItem(R.id.sort_count).setChecked(true);
                        break;
                }
            }
        });

        final MenuItem searchItem = menu.findItem(R.id.action_search);
        final SearchView searchView;


        if (searchItem != null) {
            searchView = (SearchView) searchItem.getActionView();
            searchView.setOnQueryTextListener(this);
            searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (!hasFocus) {
                        searchView.setIconified(true);
                    }
                }
            });
        }
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.pref:
                showPreference();
                return true;
            case R.id.about:
                showAbout();
                return true;
            case R.id.changelog:
                showChangelog();
                return true;
            case R.id.help:
                showHelp();
                return true;
            case R.id.report:
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("message/rfc822");

                String version = "";
                PackageInfo pInfo = null;
                try {
                    pInfo = getApplicationContext().getPackageManager().getPackageInfo(getPackageName(), 0);
                    version = pInfo.versionName;
                } catch (PackageManager.NameNotFoundException e) {

                }
                i.putExtra(Intent.EXTRA_EMAIL, new String[]{"uzoftinc@gmail.com"});
                i.putExtra(Intent.EXTRA_SUBJECT, "Report Issue: " + getString(R.string.app_name) + " " + version);
                i.putExtra(Intent.EXTRA_TEXT, "");
                try {
                    startActivity(Intent.createChooser(i, "Send mail..."));
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(MainActivity.this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
                }
                return true;
            case R.id.sort_alpha:
                Prefs.sortBy(getApplicationContext(), "s0");
                item.setChecked(true);
                loadApp(false);
                return true;
            case R.id.sort_lastupdate:
                Prefs.sortBy(getApplicationContext(), "s1");
                item.setChecked(true);
                loadApp(false);
                return true;
            case R.id.sort_count:
                Prefs.sortBy(getApplicationContext(), "s2");
                item.setChecked(true);
                loadApp(false);
                return true;
            case R.id.sort_size:
                Prefs.sortBy(getApplicationContext(), "s3");
                item.setChecked(true);
                loadApp(false);
                return true;
            case R.id.sort_percent:
                Prefs.sortBy(getApplicationContext(), "s4");
                item.setChecked(true);
                loadApp(false);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showChangelog() {
        new MaterialDialog.Builder(this)
                .title(R.string.app_name)
                .customView(R.layout.activity_changelog, false)
                .positiveText(R.string.ok)
                .show();
    }

    private void showPreference() {
        Intent myIntent = new Intent(MainActivity.this, SettingsActivity.class);
        startActivity(myIntent);
    }

    private void showHelp() {
        new MaterialDialog.Builder(this)
                .title(R.string.app_name)
                .content(R.string.about_help)
                .positiveText(R.string.ok)
                .show();
    }

    private void showAbout() {
        String version = "";
        PackageInfo pInfo = null;
        try {
            pInfo = getApplicationContext().getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {

        }

        new MaterialDialog.Builder(this)
                .title(getApplicationContext().getString(R.string.app_name) + " " + version)
                .content(R.string.about_content)
                .positiveText(R.string.ok)
                .show();
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String query) {
        List<IPObj> filteredModelList = filter(query);

        Collections.sort(new ArrayList(filteredModelList), new PackageComparator().setCtx(getApplicationContext()));
        adapter = new IconAdapter(MainActivity.this, filteredModelList);
        recyclerView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        return true;
    }

    private List<IPObj> filter(String query) {
        List<IPObj> filteredPack = new ArrayList<>();
        if (query.length() >= 1) {
            for (IPObj ipack : iconPacksList) {
                if (ipack.getIconName().toLowerCase().contains(query.toLowerCase())) {
                    filteredPack.add(ipack);
                }
            }
        }
        return filteredPack.size() > 0 ? filteredPack : iconPacksList;
    }

    public class LoadAppList extends AsyncTask<Void, Integer, Void> {

        Context context = null;
        long startTime;
        boolean forceLoad = false;


        public LoadAppList setContext(boolean forceLoad, Context context) {
            this.forceLoad = forceLoad;
            this.context = context;
            return this;
        }

        @Override
        protected void onPreExecute() {
            plsWait = new MaterialDialog.Builder(context).cancelable(false).title(context.getString(R.string.loading)).content(R.string.please_wait).progress(true, 0).show();
            startTime = System.currentTimeMillis();
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                IconPackManager iconPackManager = new IconPackManager(getApplicationContext());
                iconPacksList = iconPackManager.updateIconPacks(ipObjDao, true);
                if (isCancelled())
                    return null;
                return null;
            } catch (SQLiteException sqe) {
                sqe.printStackTrace();
                return null;
            }

        }

        @Override
        protected void onPostExecute(Void result) {
            try {
                try {
                    if (plsWait != null && plsWait.isShowing()) {
                        plsWait.dismiss();
                    }
                } catch (final IllegalArgumentException e) {
                    // Handle or log or ignore
                } catch (final Exception e) {
                    // Handle or log or ignore
                } finally {
                    plsWait.dismiss();
                    plsWait = null;
                }
                mSwipeLayout.setRefreshing(false);
                if (iconPacksList != null && !iconPacksList.isEmpty()) {
                    recyclerView.setVisibility(View.VISIBLE);
                    emptyView.setVisibility(View.GONE);
                    Collections.sort(iconPacksList, new PackageComparator().setCtx(getApplicationContext()));
                    adapter = new IconAdapter(MainActivity.this, iconPacksList);
                    recyclerView.setAdapter(adapter);
                    adapter.notifyDataSetChanged();
                } else {
                    recyclerView.setVisibility(View.GONE);
                    emptyView.setVisibility(View.VISIBLE);
                }
                //Log.i("MICO", "Total time:" + (System.currentTimeMillis() - startTime) / 1000 + " sec");
            } catch (Exception e) {
                // nothing
                if (plsWait != null) {
                    plsWait.dismiss();
                    plsWait = null;
                }
                mSwipeLayout.setRefreshing(false);
            }
        }
    }
}
