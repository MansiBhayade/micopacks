package dev.ukanth.iconmgr;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.afollestad.materialdialogs.MaterialDialog;
import com.glidebitmappool.GlideBitmapPool;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import dev.ukanth.iconmgr.dao.IPObj;
import dev.ukanth.iconmgr.dao.IPObjDao;
import dev.ukanth.iconmgr.util.LauncherHelper;

/**
 * Created by ukanth on 3/9/17.
 */

public class IconPreviewActivity extends AppCompatActivity {


    private static final int READ_MEDIA= 13;

    private MaterialDialog plsWait;
    private TextView emptyView;

    private FloatingActionButton fab;

   /* private BroadcastReceiver iconViewReceiver;
    private IntentFilter filter;*/

    private LinearLayout.LayoutParams params;

    private GridLayout gridLayout;
    private BroadcastReceiver uiProgressReceiver;
    private IntentFilter uiFilter;

    IPObjDao ipObjDao = App.getInstance().getIPObjDao();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        if (Prefs.isDarkTheme()) {
            setTheme(R.style.AppTheme_Dark);
        } else {
            setTheme(R.style.AppTheme_Light);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.iconpreview);

        emptyView = (TextView) findViewById(R.id.emptypreview);
        emptyView.setVisibility(View.GONE);

        GlideBitmapPool.initialize(10 * 1024 * 1024);

        Bundle bundle = getIntent().getExtras();
        final String pkgName = bundle.getString("pkg");
        String iconName = "";
        IPObj pkgObj;
        if (ipObjDao != null) {
             pkgObj = ipObjDao.getByIconPkg(pkgName);
            if (pkgObj != null) {
                iconName = pkgObj.getIconName();
                setTitle(iconName);
            }
        }

        registerUIbroadcast();

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            String launcherPack = LauncherHelper.getLauncherPackage(getApplicationContext());
            LauncherHelper.apply(IconPreviewActivity.this, pkgName, launcherPack);
        });

        if (!Prefs.isFabShow()) {
            fab.hide();
        }

        gridLayout = (GridLayout) findViewById(R.id.iconpreview);
        int colNumber = Prefs.getCol();
        gridLayout.setColumnCount(colNumber);
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int screenWidth = metrics.widthPixels;
        params = new LinearLayout.LayoutParams(screenWidth / colNumber, screenWidth / colNumber);
        IconsPreviewLoader previewLoader = new IconsPreviewLoader(IconPreviewActivity.this, pkgName, iconName);
        if (plsWait == null && (previewLoader.getStatus() == AsyncTask.Status.PENDING ||
                previewLoader.getStatus() == AsyncTask.Status.FINISHED)) {
            previewLoader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
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
        File mediaDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (mediaDir == null) {
            Log.e("MICO", "External storage not available");
            return;
        }

        File myDir = new File(mediaDir, "micopacks/" + packageName);
        if (!myDir.exists() && !myDir.mkdirs()) {
            Log.e("MICO", "Failed to create directory: " + myDir.getAbsolutePath());
            return;
        }

        String fname = icon.getTitle() + ".png";
        File file = new File(myDir, fname);

        if (file.exists()) {
            boolean deleted = file.delete();
            if (deleted) {
                Toast.makeText(getApplicationContext(), "Image deleted successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), "Failed to delete image", Toast.LENGTH_SHORT).show();
            }
            return;
        }

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

    private boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 33) { // Since Android 13 granular permissions are used
            if (checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES)
                    == PackageManager.PERMISSION_GRANTED  ) {
                Log.v("MICO", "Permission is granted");
                return true;
            } else {

                Log.v("MICO", "Permission is revoked");
                ActivityCompat.requestPermissions(IconPreviewActivity.this, new String[]{Manifest.permission.READ_MEDIA_IMAGES }, READ_MEDIA);

                return false;
            }
        } else if (Build.VERSION.SDK_INT >= 23 && Build.VERSION.SDK_INT <= 32) {  // Versions prior to Android 13: Request READ_EXTERNAL_STORAGE permission
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v("MICO", "Permission is granted");
                return true;
            } else {

                Log.v("MICO", "Permission is revoked");
                ActivityCompat.requestPermissions(IconPreviewActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_MEDIA);

                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            Log.v("MICO", "Permission is granted");
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case READ_MEDIA: {
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

    private class IconsPreviewLoader extends AsyncTask<Void, Void, Boolean> {

        private Context mContext;
        private String packageName;
        private Set<Icon> themed_icons;
        private Set<Icon> nonthemed_icons;
        private String iconName;

        private IconsPreviewLoader(Context context, String packageName, String iconName) {
            this.mContext = context;
            this.packageName = packageName;
            this.iconName = iconName;
        }

        @Override
        protected void onPreExecute() {
            plsWait = new MaterialDialog.Builder(mContext).cancelable(false).title(mContext.getString(R.string.loading_preview)).content(R.string.please_wait_normal).progress(true, 0).show();
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            while (!isCancelled()) {
                try {
                    IconPackUtil packUtil = new IconPackUtil();
                    themed_icons = packUtil.getListIcons(packageName);
                    if (Prefs.isNonPreview()) {
                        nonthemed_icons = packUtil.getNonThemeIcons(packageName);
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
                if (Prefs.isNonPreview() && nonthemed_icons != null) {
                    List<Icon> listNonTheme = new ArrayList<Icon>(nonthemed_icons);
                    list.addAll(listNonTheme);
                }
                if (list != null && list.size() > 0) {
                    Collections.sort(list, (o1, o2) -> String.CASE_INSENSITIVE_ORDER.compare(o1.getTitle(), o2.getTitle()));

                    for (final Icon icon : list) {
                        if (icon.getIconBitmap() != null) {
                            /*RelativeLayout relativeLayout=new RelativeLayout(mContext);
                            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                            params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
                            relativeLayout.setLayoutParams(params);
                            TextView textView = new TextView(mContext);
                            textView.setText(icon.getPackageName());*/
                            ImageView image = new ImageView(mContext);
                            image.setLayoutParams(params);
                            image.setPadding(15, 15, 15, 15);
                            image.setScaleType(ImageView.ScaleType.FIT_CENTER);
                            image.setImageDrawable(new BitmapDrawable(getResources(), icon.getIconBitmap()));
                            image.setOnClickListener(v -> new MaterialDialog.Builder(mContext)
                                     .title(icon.getTitle())
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
                                    saveImage(icon, packageName);
                                }
                                return true;
                            });
                            //relativeLayout.addView(image);
                            //relativeLayout.addView(textView);
                            gridLayout.addView(image);
                        }
                    }
                    setTitle(iconName + "(" + (list.size() - 1) + "-icons)");
                    GlideBitmapPool.clearMemory();
                    //processInputs(list, res, params, gridLayout);
                } else {
                    emptyView.setVisibility(View.VISIBLE);
                    fab.hide();
                }

            }
        }



        /*public void processInputs(List<Icon> listIcons, final Resources res, final LinearLayout.LayoutParams params, final GridLayout gridLayout) {
            try {
                ExecutorService service = Executors.newFixedThreadPool(2);

                List<Future<String>> futures = new ArrayList<Future<String>>();
                for (final Icon icon : listIcons) {
                    Callable<String> callable = new Callable<String>() {
                        public String call() throws Exception {

                            return "";
                        }
                    };
                    futures.add(service.submit(callable));
                }
                service.shutdown();
                List<String> outputs = new ArrayList<String>();
                for (Future<String> future : futures) {
                    outputs.add(future.get());
                }


            } catch (Exception e) {
                Log.e("MICO", e.getMessage(), e);
            }
        }*/
    }
}
