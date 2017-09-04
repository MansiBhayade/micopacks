package dev.ukanth.iconmgr;

/**
 * Created by ukanth on 17/7/17.
 */

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.List;

import dev.ukanth.iconmgr.dao.IPObj;
import dev.ukanth.iconmgr.util.LauncherHelper;
import dev.ukanth.iconmgr.util.Util;

import static dev.ukanth.iconmgr.util.Util.getCurrentLauncher;

public class IconAdapter extends RecyclerView.Adapter<IconAdapter.IconPackViewHolder> {

    private Context ctx;
    protected List<IPObj> iconPacks;

    public class IconPackViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        IPObj currentItem;
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


    IconAdapter(Context ctx, List<IPObj> ipacks) {
        this.ctx = ctx;
        this.iconPacks = ipacks;
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    @Override
    public IconPackViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.pack_card, viewGroup, false);
        IconPackViewHolder pvh = new IconPackViewHolder(v);

        return pvh;
    }

    @Override
    public void onBindViewHolder(IconPackViewHolder personViewHolder, int i) {
        personViewHolder.currentItem = iconPacks.get(i);
        personViewHolder.ipackName.setText(iconPacks.get(i).getIconName());
        if (Prefs.isTotalIcons(ctx)) {
            personViewHolder.ipackCount.setText(ctx.getString(R.string.noicons) + " " + Integer.toString(iconPacks.get(i).getTotal()));
        } else {
            personViewHolder.ipackCount.setVisibility(View.GONE);
        }
        PackageManager pm = ctx.getPackageManager();
        try {
            Drawable drawable = pm.getApplicationIcon(iconPacks.get(i).getIconPkg());
            personViewHolder.icon.setImageDrawable(resize(drawable));
        } catch (Exception exception) {
        }
    }

    private Drawable resize(Drawable image) {
        if (image instanceof BitmapDrawable) {
            Bitmap b = ((BitmapDrawable) image).getBitmap();

            int SIZE_DP = 60;
            final float scale = ctx.getResources().getDisplayMetrics().density;
            int p = (int) (SIZE_DP * scale + 0.5f);

            Bitmap bitmapResized = Bitmap.createScaledBitmap(b, p, p, false);
            return new BitmapDrawable(ctx.getResources(), bitmapResized);
        } else {
            return image;
        }
    }

    @Override
    public int getItemCount() {
        return iconPacks.size();
    }

}
