package dev.ukanth.iconmgr;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import dev.ukanth.iconmgr.util.Util;

/**
 * Created by ukanth on 17/7/17.
 */

public class IconPackUtil {
    private Resources iconPackres = null;

    @NonNull
    public int calcTotal(@NonNull Context mContext, String packageName) {
        Set icons = new HashSet();
        XmlPullParser parser = getXmlParser(mContext, packageName, "drawable");
        try {
            if (parser != null) {
                int eventType = parser.getEventType();
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG) {
                        icons.add(parser.getAttributeValue(null, "drawable"));
                    }
                    eventType = parser.next();
                }
            }
        } catch (XmlPullParserException | IOException e) {
        }
        return icons.size();
    }

    private XmlPullParser getXmlParser(Context mContext, String packageName, String type) {
        XmlPullParser parser = null;
        try {
            PackageManager pm = mContext.getPackageManager();
            iconPackres = pm.getResourcesForApplication(packageName);

            int appfilter = iconPackres.getIdentifier(type, "xml", packageName);
            if (appfilter > 0) {
                parser = iconPackres.getXml(appfilter);
            } else {
                try {
                    InputStream appfilterstream = iconPackres.getAssets().open(type + ".xml");
                    XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                    factory.setNamespaceAware(true);
                    parser = factory.newPullParser();
                    parser.setInput(appfilterstream, "utf-8");
                } catch (IOException e1) {
                }
            }

        } catch (PackageManager.NameNotFoundException | XmlPullParserException e) {

        }
        return parser;

    }


    private Bitmap loadBitmap(String drawableName, String packageName) {

        int id = iconPackres.getIdentifier(drawableName, "drawable", packageName);
        if (id > 0) {
            Drawable bitmap = iconPackres.getDrawable(id);
            if (bitmap instanceof BitmapDrawable) {
                Bitmap bit = ((BitmapDrawable) bitmap).getBitmap();
                return getResizedBitmap(bit, 256, 256, true);
            }
        }
        return null;
    }

    public Bitmap getResizedBitmap(Bitmap image, int bitmapWidth, int bitmapHeight, boolean filter) {
        return Bitmap.createScaledBitmap(image, bitmapWidth, bitmapHeight, filter);
    }

    public Set<Icon> getIcons(Context mContext, String packageName) {
        Set<Icon> mBackImages = new HashSet<Icon>();

        Bitmap mMaskImage;
        Bitmap mFrontImage;
        try {
            XmlPullParser xpp = getXmlParser(mContext, packageName, "appfilter");
            while (xpp.getEventType() != XmlPullParser.END_DOCUMENT) {
                if (xpp.getEventType() == XmlPullParser.START_TAG) {
                    if (xpp.getName().equals("iconback")) {
                        for (int i = 0; i < xpp.getAttributeCount(); i++) {
                            if (xpp.getAttributeName(i).startsWith("img")) {
                                String drawableName = xpp.getAttributeValue(i);
                                Bitmap iconback = loadBitmap(drawableName, packageName);
                                if (iconback != null)
                                    mBackImages.add(new Icon(drawableName, iconback));
                            }
                        }
                    } else if (xpp.getName().equals("iconmask")) {
                        if (xpp.getAttributeCount() > 0 && xpp.getAttributeName(0).equals("img1")) {
                            String drawableName = xpp.getAttributeValue(0);
                            mMaskImage = loadBitmap(drawableName, packageName);
                            if (mMaskImage != null)
                                mBackImages.add(new Icon(drawableName, mMaskImage));
                        }
                    } else if (xpp.getName().equals("iconupon")) {
                        if (xpp.getAttributeCount() > 0 && xpp.getAttributeName(0).equals("img1")) {
                            String drawableName = xpp.getAttributeValue(0);
                            mFrontImage = loadBitmap(drawableName, packageName);
                            if (mFrontImage != null)
                                mBackImages.add(new Icon(drawableName, mFrontImage));

                        }
                    }
                }
                xpp.next();
            }
        } catch (XmlPullParserException | IOException e) {
        }
        return mBackImages;
    }

    class Attrb {
        String key;
        String value;

        Attrb(String k, String v) {
            this.key = k;
            this.value = v;
        }
    }

    public Set<Icon> getListIcons(Context mContext, String packageName) {
        Set<Icon> icons = new HashSet<>();
        Key key = Key.ACTIVITY;
        List<Attrb> items = new ArrayList<>();
        try {
            XmlPullParser xpp = getXmlParser(mContext, packageName, "appfilter");
            while (xpp.getEventType() != XmlPullParser.END_DOCUMENT) {
                if (xpp.getEventType() == XmlPullParser.START_TAG) {
                    if (xpp.getName().equals("item")) {
                        String sKey = xpp.getAttributeValue(null, key.getKey());
                        sKey = sKey.replace("ComponentInfo{", "").replace("}", "");
                        if (sKey != null) {
                            String name = xpp.getAttributeValue(null, "drawable");
                            items.add(new Attrb(sKey, name));
                        }
                    }
                }
                xpp.next();
            }
            icons = processXpp(mContext, packageName, items);
        } catch (Exception e) {
            Log.e("MICO", e.getMessage());
            e.printStackTrace();
        }
        return icons;
    }

    public Set<Icon> processXpp(final Context mContext, final String packageName, List<Attrb> input) {
        try {
            ExecutorService service = Executors.newFixedThreadPool(2);
            final List<ResolveInfo> listPackages = Util.getInstalledApps(mContext);
            List<Future<Icon>> futures = new ArrayList<Future<Icon>>();
            for (final Attrb attr : input) {
                Callable<Icon> callable = new Callable<Icon>() {
                    public Icon call() throws Exception {
                        if (isSupported(mContext, packageName, listPackages, attr.key)) {
                            Bitmap iconBitmap = loadBitmap(attr.value, packageName);
                            if (iconBitmap != null) {
                                return new Icon(attr.value, iconBitmap);
                            }
                        }
                        return new Icon("");
                    }
                };
                futures.add(service.submit(callable));
            }
            service.shutdown();

            Set<Icon> outputs = new HashSet<>();
            for (Future<Icon> future : futures) {
                outputs.add(future.get());
            }
            return outputs;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new HashSet<>();
    }


    public HashMap<String, String> getAppFilter(String packageName, Context mContext, Key key) {
        HashMap<String, String> activities = new HashMap<>();

        try {
            XmlPullParser xpp = getXmlParser(mContext, packageName, "appfilter");
            while (xpp.getEventType() != XmlPullParser.END_DOCUMENT) {
                if (xpp.getEventType() == XmlPullParser.START_TAG) {
                    if (xpp.getName().equals("item")) {
                        String sKey = xpp.getAttributeValue(null, key.getKey());
                        String sValue = xpp.getAttributeValue(null, key.getValue());

                        if (sKey != null && sValue != null) {
                            activities.put(
                                    sKey.replace("ComponentInfo{", "").replace("}", ""),
                                    sValue.replace("ComponentInfo{", "").replace("}", ""));
                        } else {
                        }
                    }
                }
                xpp.next();
            }
        } catch (XmlPullParserException | IOException e) {
        }
        return activities;
    }


    public static String getOtherAppLocaleName(@NonNull Context context, @NonNull Locale locale, @NonNull String packageName) {
        try {
            PackageManager packageManager = context.getPackageManager();
            ApplicationInfo info = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA);

            Resources res = packageManager.getResourcesForApplication(packageName);
            Context otherAppContext = context.createPackageContext(packageName, Context.CONTEXT_IGNORE_SECURITY);
            Configuration configuration = new Configuration();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                configuration = res.getConfiguration();
                configuration.setLocale(locale);
                return otherAppContext.createConfigurationContext(configuration).getString(info.labelRes);
            }
            configuration.locale = locale;
            res.updateConfiguration(configuration, context.getResources().getDisplayMetrics());
            return res.getString(info.labelRes);
        } catch (Exception e) {
        }
        return null;
    }

    public boolean isSupported(@NonNull Context context, String currentPackage, List<ResolveInfo> listPackages, String currentActivity) {
        for (ResolveInfo app : listPackages) {
            String activity = app.activityInfo.packageName + "/" + app.activityInfo.name;
            if (currentActivity.equals(activity)) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    public List<String> getMissingApps(@NonNull Context context, String currentPackage, List<ResolveInfo> listPackages) {
        List<String> requests = new ArrayList<>();
        PackageManager packageManager = context.getPackageManager();
        HashMap<String, String> appFilter = getAppFilter(currentPackage, context, Key.ACTIVITY);
        for (ResolveInfo app : listPackages) {
            String packageName = app.activityInfo.packageName;
            String activity = packageName + "/" + app.activityInfo.name;
            String value = appFilter.get(activity);
            if (value == null) {
                String name = getOtherAppLocaleName(context, new Locale("en"), packageName);
                if (name == null) {
                    name = app.activityInfo.loadLabel(packageManager).toString();
                }
                requests.add(name);
            }
        }
        return requests;
    }


    public static Set<String> getInstalledIconPacks(Context mContext) {

        HashSet returnList = new HashSet<>();
        PackageManager pm = mContext.getPackageManager();
        int flags = PackageManager.GET_META_DATA |
                PackageManager.GET_SHARED_LIBRARY_FILES;

        List<ApplicationInfo> packages = pm.getInstalledApplications(flags);
        ArrayList<String> packageList = new ArrayList<>();
        for (ApplicationInfo info : packages) {
            if ((info.flags & ApplicationInfo.FLAG_SYSTEM) == 1) {
                // System application
                packageList.add(info.packageName);
            } else {
                // Installed by user
                packageList.add(info.packageName);
            }
        }
        List<ResolveInfo> rinfo = pm.queryIntentActivities(new Intent("com.gau.go.launcherex.theme"), PackageManager.GET_META_DATA);
        rinfo.addAll(pm.queryIntentActivities(new Intent("com.novalauncher.THEME"), PackageManager.GET_META_DATA));
        rinfo.addAll(pm.queryIntentActivities(new Intent("org.adw.launcher.THEMES"), PackageManager.GET_META_DATA));

        for (ResolveInfo ri : rinfo) {
            returnList.add(ri.activityInfo.packageName);
        }
        return returnList;
    }

    public enum Key {
        ACTIVITY("component", "drawable"),
        DRAWABLE("drawable", "component");

        private String key;
        private String value;

        Key(String key, String value) {
            this.key = key;
            this.value = value;
        }

        private String getKey() {
            return key;
        }

        private String getValue() {
            return value;
        }
    }

    private Bitmap maskImage(Bitmap original, Bitmap mask) {
        Bitmap result = Bitmap.createBitmap(mask.getWidth(), mask.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas mCanvas = new Canvas(result);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        mCanvas.drawBitmap(original, 0, 0, null);
        mCanvas.drawBitmap(mask, 0, 0, paint);
        paint.setXfermode(null);
        return result;
    }

    public Set<Icon> getNonThemeIcons(Context context, String currentPackage) {
        Set<Icon> icons = new HashSet<>();
        PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> listPackages = Util.getInstalledApps(context);
        HashMap<String, String> appFilter = getAppFilter(currentPackage, context, Key.ACTIVITY);
        for (ResolveInfo app : listPackages) {
            String packageName = app.activityInfo.packageName;
            String activity = packageName + "/" + app.activityInfo.name;
            String value = appFilter.get(activity);
            //not themes icons
            if (value == null) {
                String name = app.activityInfo.loadLabel(packageManager).toString();
                Drawable drawable = app.activityInfo.loadIcon(packageManager);
                Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
                bitmap = getResizedBitmap(bitmap, 256, 256, true);
                //try to mask icon
                if (bitmap != null) {
                    icons.add(new Icon(name, bitmap));
                }
            }
        }
        return icons;
    }
}