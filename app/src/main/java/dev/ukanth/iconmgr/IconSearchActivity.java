package dev.ukanth.iconmgr;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.afollestad.materialdialogs.MaterialDialog;
import com.glidebitmappool.GlideBitmapPool;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dev.ukanth.iconmgr.dao.DaoSession;
import dev.ukanth.iconmgr.dao.IPObj;
import dev.ukanth.iconmgr.dao.IPObjDao;

/**
 * Created by ukanth on 3/9/17.
 */

public class IconSearchActivity extends AppCompatActivity {

    private static final int WRITE_EXTERNAL_STORAGE = 12;

    private MaterialDialog plsWait;

    private LinearLayout.LayoutParams params;

    private GridLayout gridLayout;
    private List<IPObj> objList;

    private SearchView mSearchView;

    private BroadcastReceiver uiProgressReceiver;
    private IntentFilter uiFilter;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_search, menu);
        MenuItem searchItem = menu.findItem(R.id.i_search);
        mSearchView = (SearchView) searchItem.getActionView();
        setupSearchView(searchItem);

        return true;
    }

    private void registerUIbroadcast() {
        uiFilter = new IntentFilter("UPDATEUI");

        uiProgressReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                byte[] byteArray = intent.getByteArrayExtra("image");
                Bitmap bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
                ImageView image = new ImageView(getApplicationContext());
                image.setLayoutParams(params);
                image.setPadding(15, 15, 15, 15);
                image.setScaleType(ImageView.ScaleType.FIT_CENTER);
                image.setImageDrawable(new BitmapDrawable(getResources(), bitmap));
                gridLayout.addView(image);
            }
        };
    }

    private void setupSearchView(MenuItem searchItem) {
        mSearchView.setIconifiedByDefault(false);
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                gridLayout.removeAllViews();
                searchIcons(s);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                if (s.length() == 0) {
                    gridLayout.removeAllViews();
                }
                return false;
            }
        });
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        if (Prefs.isDarkTheme()) {
            setTheme(R.style.AppTheme_Dark);
        } else {
            setTheme(R.style.AppTheme_Light);
        }

        GlideBitmapPool.initialize(10 * 1024 * 1024);


        super.onCreate(savedInstanceState);
        setContentView(R.layout.iconsearch);


        gridLayout = (GridLayout) findViewById(R.id.iconsearchpreview);
        int colNumber = 4;
        gridLayout.setColumnCount(colNumber);

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int screenWidth = metrics.widthPixels;
        params = new LinearLayout.LayoutParams(screenWidth / colNumber, screenWidth / colNumber);

        //SearchView searchView = findViewById(R.id.search);


        App app = ((App) getApplicationContext());
        DaoSession daoSession = app.getDaoSession();
        IPObjDao ipObjDao = daoSession.getIPObjDao();

        objList = ipObjDao.loadAll();

        registerUIbroadcast();
    }

    private void searchIcons(String query) {
        if (!query.isEmpty()) {
            query = query.toLowerCase();
        }
        IconsPreviewLoader previewLoader = new IconsPreviewLoader(IconSearchActivity.this, query);
        if (plsWait == null && (previewLoader.getStatus() == AsyncTask.Status.PENDING ||
                previewLoader.getStatus() == AsyncTask.Status.FINISHED)) {
            previewLoader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(uiProgressReceiver, uiFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(uiProgressReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void saveImage(Icon icon, String packageName) {
        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root + "/micopacks/" + packageName);
        myDir.mkdirs();
        String fname = icon.getTitle() + ".png";
        File file = new File(myDir, fname);
        if (file.exists())
            file.delete();
        try {
            FileOutputStream out = new FileOutputStream(file);
            icon.getIconBitmap().compress(Bitmap.CompressFormat.PNG, 85, out);
            out.flush();
            out.close();
            Toast.makeText(getApplicationContext(), "Saved successfully: " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e("MICO", e.getMessage(), e);
        }
    }


    private class IconsPreviewLoader extends AsyncTask<Void, Void, Boolean> {

        private Context mContext;
        private String query;
        private List<Icon> themed_icons = new ArrayList<>();

        private IconsPreviewLoader(Context context, String query) {
            this.query = query;
            this.mContext = context;
        }

        @Override
        protected void onPreExecute() {
            plsWait = new MaterialDialog.Builder(mContext).cancelable(false).title(mContext.getString(R.string.searching)).content(R.string.please_wait_normal).progress(true, 0).show();
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            while (!isCancelled()) {
                try {
                    IconPackUtil packUtil = new IconPackUtil();
                    for (int i = 0; i < objList.size(); i++) {
                        final int pos = i;
                        runOnUiThread(() -> {
                            plsWait.setTitle(mContext.getString(R.string.searching) + (" " + pos + "/" + objList.size()));
                        });
                        themed_icons.addAll(packUtil.getFilterIcons(objList.get(i).getIconPkg(), query));
                    }
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
            return false;
        }


        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);

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

            gridLayout.removeAllViews();


            if (themed_icons != null) {
                List<Icon> list = new ArrayList<Icon>(themed_icons);
                if (list != null && list.size() > 0) {
                    Collections.sort(list, (o1, o2) -> String.CASE_INSENSITIVE_ORDER.compare(o1.getTitle(), o2.getTitle()));

                    for (final Icon icon : list) {
                        if (icon.getIconBitmap() != null) {
                           /* RelativeLayout relativeLayout=new RelativeLayout(mContext);
                            relativeLayout.setLayoutParams(params);
                            TextView textView = new TextView(mContext);
                            textView.setText(icon.getPackageName());*/
                            ImageView image = new ImageView(mContext);
                            image.setLayoutParams(params);
                            image.setPadding(15, 15, 15, 15);
                            image.setScaleType(ImageView.ScaleType.FIT_CENTER);
                            image.setImageDrawable(new BitmapDrawable(getResources(), icon.getIconBitmap()));
                            image.setOnClickListener(v -> new MaterialDialog.Builder(mContext)
                                    .title(icon.getTitle() + "\n" +" " + getAppNameByPackage(icon.getPackageName()) + " ")
                                    .positiveText(R.string.save)
                                    .onPositive((dialog, which) -> {
                                        if (isStoragePermissionGranted()) {
                                            saveImage(icon, icon.getPackageName());
                                            dialog.dismiss();
                                        }
                                    })
                                    .negativeText(R.string.close)
                                    .icon(new BitmapDrawable(getResources(), icon.getIconBitmap()))
                                    .show());
                            image.setOnLongClickListener(view -> {

                                if (isStoragePermissionGranted()) {
                                    saveImage(icon, icon.getPackageName());
                                }
                                return true;
                            });
                            //relativeLayout.addView(image);
                            //relativeLayout.addView(textView);
                            gridLayout.addView(image);
                        }
                    }
                    GlideBitmapPool.clearMemory();
                    //processInputs(list, res, params, gridLayout);
                } else {
                }
            }
        }
    }

    private boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v("MICO", "Permission is granted");
                return true;
            } else {

                Log.v("MICO", "Permission is revoked");
                ActivityCompat.requestPermissions(IconSearchActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_EXTERNAL_STORAGE);
                return false;
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            Log.v("MICO", "Permission is granted");
            return true;
        }
    }


    private String getAppNameByPackage(String packageName){

        final PackageManager pm = getApplicationContext().getPackageManager();
        ApplicationInfo ai;
        try {
            ai = pm.getApplicationInfo(packageName, 0);
        } catch (final PackageManager.NameNotFoundException e) {
            ai = null;
        }
        return (String) (ai != null ? pm.getApplicationLabel(ai) : packageName);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case WRITE_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    Toast.makeText(getApplicationContext(), "Permission granted", Toast.LENGTH_SHORT).show();
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(getApplicationContext(), "Permission denied", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }

}
