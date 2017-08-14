package dev.ukanth.iconmgr.tasker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import org.greenrobot.greendao.query.Query;

import java.util.List;
import java.util.Random;

import dev.ukanth.iconmgr.App;
import dev.ukanth.iconmgr.Prefs;
import dev.ukanth.iconmgr.R;
import dev.ukanth.iconmgr.dao.DaoSession;
import dev.ukanth.iconmgr.dao.IPObj;
import dev.ukanth.iconmgr.dao.IPObjDao;
import dev.ukanth.iconmgr.util.Util;

public class LocaleEdit extends AppCompatActivity {
    private boolean mIsCancelled = false;
    private IPObjDao ipObjDao;
    private Query<IPObj> ipObjQuery;


    protected void onCreate(Bundle paramBundle) {

        if(Prefs.isDarkTheme(getApplicationContext())) {
            setTheme(R.style.AppTheme_Dark);
        }
        super.onCreate(paramBundle);

        BundleScrubber.scrub(getIntent());
        BundleScrubber.scrub(getIntent().getBundleExtra(
                com.twofortyfouram.locale.Intent.EXTRA_BUNDLE));

        setContentView(R.layout.tasker_main);

        DaoSession daoSession = ((App) getApplication()).getDaoSession();
        ipObjDao = daoSession.getIPObjDao();

        ipObjQuery = ipObjDao.queryBuilder().orderAsc(IPObjDao.Properties.IconName).build();


        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        RadioGroup groupPacks = (RadioGroup) findViewById(R.id.radioPacks);
        List<IPObj> iPacksList = ipObjQuery.list();

        for (IPObj pack : iPacksList) {
            RadioButton button = new RadioButton(this);
            int uid = Util.getUid(getApplicationContext(),pack.getIconPkg());
            button.setId(uid);
            button.setText(pack.getIconName()+ ":" + pack.getIconPkg());
            button.setTextSize(24);
            groupPacks.addView(button);
        }

        setupTitleApi11();

        if (null == paramBundle) {
            final Bundle forwardedBundle = getIntent().getBundleExtra(
                    com.twofortyfouram.locale.Intent.EXTRA_BUNDLE);
            if (PluginBundleManager.isBundleValid(forwardedBundle)) {
                String index = forwardedBundle.getString(PluginBundleManager.BUNDLE_EXTRA_STRING_MESSAGE);
                if (index.contains(":")) {
                    index = index.split(":")[2];
                }
                if (index != null) {
                    RadioButton btn = (RadioButton) findViewById(Integer.parseInt(index));
                    if (btn != null) {
                        btn.setChecked(true);
                    }
                }
            }
        }

    }

    private void setupTitleApi11() {
        CharSequence callingApplicationLabel = null;
        try {
            callingApplicationLabel = getPackageManager().getApplicationLabel(
                    getPackageManager().getApplicationInfo(getCallingPackage(),
                            0));
        } catch (final NameNotFoundException e) {
        }
        if (null != callingApplicationLabel) {
            setTitle(callingApplicationLabel);
        }
    }

    protected void onPause() {
        super.onPause();
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.twofortyfouram_locale_menu_dontsave:
                mIsCancelled = true;
                finish();
                return true;
            case R.id.twofortyfouram_locale_menu_save:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.tasker_menu, menu);
        return true;
    }

    @Override
    public void finish() {
        if (mIsCancelled) {
            setResult(RESULT_CANCELED);
        } else {
            RadioGroup group = (RadioGroup) findViewById(R.id.radioPacks);
            int selectedId = group.getCheckedRadioButtonId();
            RadioButton radioButton = (RadioButton) findViewById(selectedId);
            String action = "";
            if( radioButton.getText() != null) {
                action = radioButton.getText().toString();
            }
            final Intent resultIntent = new Intent();
            resultIntent.putExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE, PluginBundleManager.generateBundle(getApplicationContext(), action + ":" + selectedId));
            resultIntent.putExtra(com.twofortyfouram.locale.Intent.EXTRA_STRING_BLURB, action);
            setResult(RESULT_OK, resultIntent);
        }
        super.finish();
    }
}
