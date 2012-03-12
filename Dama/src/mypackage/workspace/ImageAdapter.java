package mypackage.workspace;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

public class ImageAdapter extends BaseAdapter {

	private Context context;

	private Drawable[] images;

	public ImageAdapter(Context context, Drawable[] images) {
		this.context = context;
		this.images = images;
	}

	@Override
	public int getCount() {
		return images.length;
	}

	@Override
	public Object getItem(int position) {
		return images[position];
	}

	@Override
	public long getItemId(int position) {
		return images[position].hashCode();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ImageView imageView;
		if (convertView == null) {
			imageView = new ImageView(context);
			imageView.setScaleType(ScaleType.CENTER_CROP);
		} else {
			imageView = (ImageView) convertView;
		}
		imageView.setImageDrawable(images[position]);
		return imageView;
	}

}
