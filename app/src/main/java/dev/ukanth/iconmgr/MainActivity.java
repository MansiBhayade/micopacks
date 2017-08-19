package dev.ukanth.iconmgr;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import org.greenrobot.greendao.query.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dev.ukanth.iconmgr.dao.DaoSession;
import dev.ukanth.iconmgr.dao.IPObj;
import dev.ukanth.iconmgr.dao.IPObjDao;
import dev.ukanth.iconmgr.util.PackageComparator;

public class MainActivity extends AppCompatActivity implements SearchView.OnQueryTextListener, SwipeRefreshLayout.OnRefreshListener {
    private RecyclerView recyclerView;
    private TextView emptyView;

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

        mSwipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        mSwipeLayout.setOnRefreshListener(this);

        loadApp();

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

    private void loadApp() {
        LoadAppList getAppList = new LoadAppList();
        if (plsWait == null && (getAppList.getStatus() == AsyncTask.Status.PENDING ||
                getAppList.getStatus() == AsyncTask.Status.FINISHED)) {
            getAppList.setContext(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    @Override
    public void onRefresh() {
        loadApp();
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
                   /* case "s3":
                        mainMenu.findItem(R.id.sort_percent).setChecked(true);
                        break;*/
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
            case R.id.sort_count:
                Prefs.sortBy(getApplicationContext(), "s2");
                item.setChecked(true);
                loadApp();
                return true;
            case R.id.sort_alpha:
                Prefs.sortBy(getApplicationContext(), "s0");
                item.setChecked(true);
                loadApp();
                return true;
            case R.id.sort_lastupdate:
                Prefs.sortBy(getApplicationContext(), "s1");
                item.setChecked(true);
                loadApp();
                return true;
            /*case R.id.sort_percent:
                Prefs.sortBy(getApplicationContext(), "s3");
                item.setChecked(true);
                loadApp();
                return true;*/
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

        public LoadAppList setContext(Context context) {
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

            // query all notes, sorted a-z by their text
            ipObjQuery = ipObjDao.queryBuilder().orderAsc(IPObjDao.Properties.IconName).build();
            List<IPObj> iPacksList = ipObjQuery.list();
            if (iPacksList.size() == 0) {
                IconPackManager iconPackManager = new IconPackManager(getApplicationContext());
                iconPacksList = iconPackManager.updateIconPacks(ipObjDao, true);
            } else {
                iconPacksList = iPacksList;
            }
            if (isCancelled())
                return null;
            //publishProgress(-1);
            return null;
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
                //mSwipeLayout.setRefreshing(false);

                Collections.sort(iconPacksList, new PackageComparator().setCtx(getApplicationContext()));
                adapter = new IconAdapter(MainActivity.this, iconPacksList);
                recyclerView.setAdapter(adapter);

                if (iconPacksList.isEmpty()) {
                    recyclerView.setVisibility(View.GONE);
                    emptyView.setVisibility(View.VISIBLE);
                } else {
                    recyclerView.setVisibility(View.VISIBLE);
                    emptyView.setVisibility(View.GONE);
                }
                Log.i("MICO", "Total time:" + (System.currentTimeMillis() - startTime) / 1000 + " sec");
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
