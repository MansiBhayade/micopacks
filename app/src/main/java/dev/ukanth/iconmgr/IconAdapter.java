package dev.ukanth.iconmgr;

/**
 * Created by ukanth on 17/7/17.
 */

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.gson.Gson;

import java.util.Date;
import java.util.List;

import dev.ukanth.iconmgr.dao.IPObj;
import dev.ukanth.iconmgr.util.LauncherHelper;
import dev.ukanth.iconmgr.util.Util;

import static dev.ukanth.iconmgr.tasker.FireReceiver.TAG;
import static dev.ukanth.iconmgr.util.Util.getCurrentLauncher;

public class IconAdapter extends RecyclerView.Adapter<IconAdapter.IconPackViewHolder> {

    private Context ctx;
    protected List<IPObj> iconPacks;

    public class IconPackViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        IPObj currentItem;
        LocalIcon localIcon;
        TextView ipackName;
        TextView ipackCount;
        ImageView icon;

        public IconPackViewHolder(View view) {
            super(view);
            cardView = (CardView) view.findViewById(R.id.cv);
            ipackName = (TextView) view.findViewById(R.id.ipack_name);
            ipackCount = (TextView) view.findViewById(R.id.ipack_icon_count);
            icon = (ImageView) view.findViewById(R.id.ipack_icon);
            icon.setOnClickListener(new ImageView.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (currentItem != null && currentItem.getIconPkg() != null) {
                        Intent intent = new Intent(ctx, IconPreviewActivity.class);
                        intent.putExtra("pkg", currentItem.getIconPkg());
                        ctx.startActivity(intent);
                    }

                }
            });
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new MaterialDialog.Builder(ctx)
                            .title(ctx.getString(R.string.title) + " " + currentItem.getIconName())
                            .items(R.array.items)
                            .itemsCallback(new MaterialDialog.ListCallback() {
                                @Override
                                public void onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                                    performAction(which, currentItem);
                                }
                            })
                            .show();
                }
            });
        }
    }


    private void performAction(int which, IPObj currentItem) {
        switch (which) {
            case 0:
                determineApply(ctx, currentItem);
                break;
            case 1:
                stats(ctx, currentItem);
                break;
            case 2:
                openPlay(ctx, currentItem);
                break;
            case 3:
                openApp(ctx, currentItem);
                break;
            case 4:
                uninstall(ctx, currentItem);
                break;

        }
    }

    private void stats(Context ctx, IPObj currentItem) {
        if (currentItem != null && currentItem.getIconPkg() != null) {
            Intent intent = new Intent(ctx, DetailsActivity.class);
            intent.putExtra("pkg", currentItem.getIconPkg());
            ctx.startActivity(intent);
        }
    }


    private void openApp(Context ctx, IPObj currentItem) {
        if (currentItem != null && currentItem.getIconPkg() != null
                && Util.isPackageExisted(ctx, currentItem.getIconPkg())) {
            Intent i = ctx.getPackageManager().getLaunchIntentForPackage(currentItem.getIconPkg());
            ctx.startActivity(i);
        }
    }

    private void uninstall(Context ctx, IPObj currentItem) {
        Intent intent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setData(Uri.fromParts("package", currentItem.getIconPkg(), null));
        if (intent.resolveActivity(ctx.getPackageManager()) != null) {
            ctx.startActivity(intent);
        }
    }

    private void openPlay(Context ctx, IPObj currentItem) {
        try {
            ctx.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + currentItem.getIconPkg())));
        } catch (android.content.ActivityNotFoundException anfe) {
            ctx.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + currentItem.getIconPkg())));
        }
    }

    private void determineApply(Context ctx, IPObj currentItem) {
        String currentLauncher = getCurrentLauncher(ctx);
        if (currentLauncher != null) {
            LauncherHelper.apply(ctx, currentItem.getIconPkg(), currentLauncher);
        } else {
            Toast.makeText(ctx, ctx.getString(R.string.nodefault), Toast.LENGTH_LONG).show();
        }
    }


    IconAdapter(List<IPObj> ipacks) {
        this.iconPacks = ipacks;
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    @Override
    public IconPackViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        ctx = viewGroup.getContext();
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.pack_card, viewGroup, false);
        return new IconPackViewHolder(v);
    }

    @Override
    public void onBindViewHolder(IconPackViewHolder personViewHolder, int i) {
        //ctx = personViewHolder.itemView.getContext();
        View currentView = personViewHolder.itemView;
        currentView.setTag(personViewHolder);
        IPObj obj = iconPacks.get(i);
        personViewHolder.localIcon = new LocalIcon();
        personViewHolder.currentItem = obj;
        personViewHolder.ipackName.setText(obj.getIconName());

        StringBuilder builder = new StringBuilder();
        boolean isshown = false;

        if (Prefs.isTotalIcons(ctx)) {
            builder.append(ctx.getString(R.string.noicons) + " " + Integer.toString(obj.getTotal()));
            isshown = true;
        }
        if (Prefs.showSize(ctx)) {
            if (Prefs.isTotalIcons(ctx)) {
                builder.append(" - ");
            }
            isshown = true;
            IconAttr attr = new Gson().fromJson(obj.getAdditional(), IconAttr.class);
            builder.append(attr.getSize() + " MB");
        }
        if (Prefs.showPercentage(ctx)) {
            if (Prefs.isTotalIcons(ctx) || Prefs.showSize(ctx)) {
                builder.append(" - ");
            }
            isshown = true;
            int installed = Util.getInstalledApps(ctx).size();
            int missed = obj.getMissed();
            int themed = installed - missed;
            double percent = ((double) themed / installed) * 100;
            String result = String.format("%.2f", percent) + "%";
            builder.append(" " + result);
        }

        if (Prefs.sortBy(ctx).equals("s1")) {
            if (Prefs.isTotalIcons(ctx) || Prefs.showSize(ctx) || Prefs.showPercentage(ctx)) {
                builder.append(" - ");
            }
            builder.append(" " + Util.prettyFormat(new Date(System.currentTimeMillis() - obj.getInstallTime())));
        }

        personViewHolder.ipackCount.setText(builder.toString());

        if (!isshown) {
            personViewHolder.ipackCount.setVisibility(View.GONE);
        }

        try {
            new LoadIcon().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, personViewHolder.currentItem,
                    ctx.getPackageManager(), personViewHolder.localIcon, currentView);
        } catch (Exception r) {
        }
    }

    @Override
    public int getItemCount() {
        return iconPacks.size();
    }

    private class LoadIcon extends AsyncTask<Object, Void, View> {
        @Override
        protected View doInBackground(Object... params) {
            IPObj ipObj = (IPObj) params[0];
            final PackageManager pkgMgr = (PackageManager) params[1];
            final LocalIcon icon = (LocalIcon) params[2];
            final View viewToUpdate = (View) params[3];
            try {
                icon.setDrawable(Util.resizeImage(ctx, pkgMgr.getApplicationIcon(ipObj.getIconPkg())));
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            return viewToUpdate;
        }

        protected void onPostExecute(View viewToUpdate) {
            try {
                final IconPackViewHolder entryToUpdate = (IconPackViewHolder) viewToUpdate.getTag();
                entryToUpdate.icon.setImageDrawable(entryToUpdate.localIcon.drawable);
            } catch (Exception e) {
                Log.e(TAG, "Error showing icon", e);
            }
        }
    }

    private class LocalIcon {
        public Drawable getDrawable() {
            return drawable;
        }

        public void setDrawable(Drawable drawable) {
            this.drawable = drawable;
        }

        private Drawable drawable;
    }
}
