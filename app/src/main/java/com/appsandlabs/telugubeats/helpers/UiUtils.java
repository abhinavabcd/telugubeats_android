package com.appsandlabs.telugubeats.helpers;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Looper;
import android.support.v4.app.FragmentActivity;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.appsandlabs.telugubeats.R;
import com.appsandlabs.telugubeats.config.Config;
import com.appsandlabs.telugubeats.datalisteners.GenericListener;
import com.appsandlabs.telugubeats.models.Stream;
import com.appsandlabs.telugubeats.widgets.CustomLoadingDialog;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import com.squareup.picasso.Transformation;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;


public class UiUtils {


	private final App app;
	private final Context context;



	public enum Images {
		MAP_MARKER_ICON("icons/marker.png"),
		PLUS_ICON("icons/plus_icon.png"),
		SMILEY_ICON("icons/smiley_icon.png");


		String value = null;
		int resourceId = -1;

		Images(String value) {
			this.value = value;
		}

		Images(int resourceId) {
			this.resourceId = resourceId;
		}

		public String getValue() {
			return value;
		}

		public int getResourceId() {
			return resourceId;
		}

		public Drawable getDrawable(Context ctx) {
			return ctx.getResources().getDrawable(getResourceId());
		}

		public String getValue(Object... args) {
			return String.format(value, args);
		}
	}


	public UiUtils(App app, Context context) {
		this.app = app;
		this.context  = context;

//       animationSlideInLeft = AnimationUtils.loadAnimation(app.getContext(),
//			   R.anim.slide_in_left);
//       animationSlideInRight = AnimationUtils.loadAnimation(app.getContext(),
//			   R.anim.slide_in_right);
//  	   animationSlideOutLeft = AnimationUtils.loadAnimation(app.getContext(),
//			   R.anim.slide_out_left);
//       animationSlideOutRight = AnimationUtils.loadAnimation(app.getContext(),
//			   R.anim.slide_out_right);
//       animationSlideOutLeft.setAnimationListener(app);
//       animationSlideOutRight.setAnimationListener(app);
//       animationSlideInLeft.setAnimationListener(app);
//       animationSlideInRight.setAnimationListener(app);
	}



	public static enum UiText {
		NO_PREVIOUS_MESSAGES("No Previous Messages"),
		TEXT_LOADING("loading.."),
		INVITE_DIALOG_TITLE("Invite your Friends");

		String value = null;

		UiText(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}

		public String getValue(Object... args) {
			return String.format(value, args);
		}


	}


	private int uiBlockCount = 0;
	private CustomLoadingDialog preloader = null;
	private CharSequence preloaderText;

	public synchronized void addUiBlock() {
		try {
			if (uiBlockCount == 0) {
				preloaderText = UiText.TEXT_LOADING.getValue();
				preloader = new CustomLoadingDialog(context, preloaderText);
				preloader.show();
			}
			uiBlockCount++;
		} catch (Exception e) {
			uiBlockCount = 0;
			//older view error
		}

	}

	public synchronized void addUiBlock(String text) {
		try {
			if (uiBlockCount == 0) {
				preloaderText = text;
				preloader = new CustomLoadingDialog(context, preloaderText);
				preloader.show();
			} else {
				if (!preloaderText.toString().endsWith(text)) {
					preloaderText = preloaderText + ("\n" + text);
					preloader.setMessage(preloaderText);
				}
			}
			uiBlockCount++;
		} catch (Exception e) {
			uiBlockCount = 0;
			//older view error
		}

	}

	public synchronized boolean removeUiBlock() {
		try {
			uiBlockCount--;
			if (uiBlockCount == 0) {

				preloader.dismiss();
				return true;
			}
			return false;
		} catch (Exception e) {
			uiBlockCount = 0;
			//older view error
			return false;
		}

	}

	@SuppressLint("NewApi")
	public static void setBg(View view, Drawable drawable) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			view.setBackground(drawable);
		} else {
			view.setBackgroundDrawable(drawable);
		}
	}

	@SuppressLint("NewApi")
	public void setBg(final View view, String url) {
		getRequestCreatorTask(context, url, false).onSuccess(new Continuation<RequestCreator, Void>() {
			@Override
			public Void then(Task<RequestCreator> task) throws Exception {
				view.setBackground(new BitmapDrawable(view.getContext().getResources(), task.getResult().get()));
				;
				return null;
			}
		}, Task.UI_THREAD_EXECUTOR);
	}

	public Timer setInterval(final FragmentActivity activity , int millis, final GenericListener<Integer> listener) {
		// TODO Auto-generated constructor stub
		Timer timer = (new Timer());
		timer.schedule(new TimerTask() {
			int count = 0;

			@Override
			public void run() {
				// TODO: NullPointerException after when pressing back button to exit quiz
				if (activity != null)
					(activity).runOnUiThread(new Runnable() {

												 @Override
												 public void run() {
													 listener.onData(++count);
												 }
											 }
					);
				else {
					Log.e(Config.ERR_LOG_TAG, "changes");
					this.cancel();
				}
			}
		}, 0, millis);
		return timer;
	}


	public static void sendSMS(Context context, String phoneNumber, String text) {
		Uri smsUri = Uri.parse("tel:+" + phoneNumber);
		Intent intent = new Intent(Intent.ACTION_VIEW, smsUri);
		intent.putExtra("sms_body", text);
		intent.setType("vnd.android-dir/mms-sms");
		context.startActivity(intent);
	}


	public static void shareText(Activity A, String message, String phoneNumber) {
		Intent sendIntent = new Intent();
		sendIntent.setAction(Intent.ACTION_SEND);
		sendIntent.putExtra(Intent.EXTRA_TEXT, message);
		if (phoneNumber != null) {
			sendIntent.putExtra(Intent.EXTRA_PHONE_NUMBER, phoneNumber);
			sendIntent.putExtra("address", phoneNumber);
		}
		sendIntent.setType("text/plain");
		A.startActivity(Intent.createChooser(sendIntent, UiUtils.UiText.INVITE_DIALOG_TITLE.getValue()));
	}

	public static String formatRemainingTime(double timeRemainingInMillis) {
		long secondsInMilli = 1000;
		long minutesInMilli = secondsInMilli * 60;
		long hoursInMilli = minutesInMilli * 60;
		long daysInMilli = hoursInMilli * 24;

		String ret = "";
		long elapsedDays = (long) (timeRemainingInMillis / daysInMilli);
		timeRemainingInMillis = timeRemainingInMillis % daysInMilli;
		if (elapsedDays > 0) ret += elapsedDays + "days ";

		long elapsedHours = (long) (timeRemainingInMillis / hoursInMilli);
		timeRemainingInMillis = timeRemainingInMillis % hoursInMilli;
		if (elapsedDays > 0 || elapsedHours > 0) ret += elapsedHours + "hours ";

		long elapsedMinutes = (long) (timeRemainingInMillis / minutesInMilli);
		timeRemainingInMillis = timeRemainingInMillis % minutesInMilli;
		if (elapsedDays > 0 || elapsedHours > 0 || elapsedMinutes > 0)
			ret += elapsedMinutes + "min ";

		long elapsedSeconds = (long) (timeRemainingInMillis / secondsInMilli);
		if (elapsedDays > 0 || elapsedHours > 0 || elapsedMinutes > 0 || elapsedSeconds > 0)
			ret += elapsedSeconds + "sec";


		return ret;
	}




	public static Task<RequestCreator> getRequestCreatorTask(final Context context , final String assetPath, final boolean downloadToAssets) {
		return Task.call(new Callable<RequestCreator>() {
			@Override
			public RequestCreator call() throws Exception {

				if (assetPath.startsWith("http://") || assetPath.startsWith("https://")) {
					return Picasso.with(context).load(assetPath);//.error(R.drawable.error_image);
				}
//				try {
//					InputStream ims = app.getContext().getAssets().open("images/" + assetPath); //assets folder
//					ims.close();
//					return Picasso.with(app.getContext()).load("file:///android_asset/images/" + assetPath).error(R.drawable.error_image);
//				} catch (IOException e) {
//					Log.d(Config.QUIZAPP_ERR_LOG_TAG, "failed to load from assets");
//					e.printStackTrace();
//				}
//				File file = new File(app.getContext().getFilesDir().getParentFile().getPath()+"/images/"+assetPath);
//				if(file.exists()){
//					return Picasso.with(app.getContext()).load(file).error(R.drawable.error_image);
//				}

//				Log.d(Config.QUIZAPP_ERR_LOG_TAG, "loading from CDN");

				RequestCreator requestCreator = Picasso.with(context).load(ServerCalls.CDN_PATH + assetPath);//.error(R.drawable.error_image);

//				if(downloadToAssets) {
//					try {
//						Bitmap bitmap = requestCreator.get();
//						File saveImageFile = new File(app.getContext().getFilesDir().getParentFile().getPath() + "/images/" + assetPath);
//						saveImageFile.createNewFile();
//						FileOutputStream ostream = new FileOutputStream(saveImageFile);
//						bitmap.compress(assetPath.endsWith(".png") ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG, 75, ostream);
//						ostream.close();
//					}
//					catch (Exception e){
//					}
//				}
				return requestCreator;
			}
		});
	}

	public static Task<RequestCreator> getRequestCreatorTask(final Context context, final String assetPath) {
		return Task.callInBackground(new Callable<RequestCreator>() {
			@Override
			public RequestCreator call() throws Exception {

				if (assetPath.startsWith("http://") || assetPath.startsWith("https://")) {
					return Picasso.with(context).load(assetPath);//.error(R.drawable.error_image);
				}
				RequestCreator requestCreator = Picasso.with(context).load(ServerCalls.CDN_PATH + assetPath);//.error(R.drawable.error_image);


				return requestCreator;
			}
		});
	}

	public static int generateRandomColor(int mix) {
		Random random = new Random();
		int red = random.nextInt(256);
		int green = random.nextInt(256);
		int blue = random.nextInt(256);

		// mix the color
		red = (red + Color.red(mix)) / 2;
		green = (green + Color.green(mix)) / 2;
		blue = (blue + Color.blue(mix)) / 2;

		int color = Color.argb(255, red, green, blue);
		return color;
	}


	public static void into(RequestCreator requestCreator, ImageView imageView, Callback callback) {
		boolean mainThread = Looper.myLooper() == Looper.getMainLooper();
		if (mainThread) {
			requestCreator.into(imageView, callback);
		} else {
			try {
				Bitmap bitmap = requestCreator.get();
				imageView.setImageBitmap(bitmap);
				if (callback != null) {
					callback.onSuccess();
				}
			} catch (IOException e) {
				if (callback != null) {
					callback.onError();
				}
			}
		}
	}


	public static boolean loadImageIntoViewDoInBackground(Context ctx, final ImageView imgView, final String assetPath, final boolean downloadToAssets) {
		return loadImageIntoViewDoInBackground(ctx, imgView, assetPath, downloadToAssets, -1, -1, null);
	}

	public static boolean loadImageIntoViewDoInBackground(Context ctx, final ImageView imgView, final String assetPath, final boolean downloadToAssets, int width, int height, GenericListener<Boolean> completedLoadingImage) {
		if (assetPath == null || assetPath.isEmpty())
			return false;
//		try{
		if (assetPath.startsWith("http://") || assetPath.startsWith("https://")) {
			if (width > 0 && height > 0)
				into(Picasso.with(ctx).load(assetPath).resize(width, height), imgView, null);
			else
				into(Picasso.with(ctx).load(assetPath), imgView, null);
			return true;
		}

//		    InputStream ims = ctx.getAssets().open("images/"+assetPath); //assets folder
//			if(width>0 && height>0)
//				into(Picasso.with(ctx).load("file:///android_asset/images/"+assetPath).resize(width, height),imgView, null);
//			else
//				into(Picasso.with(ctx).load("file:///android_asset/images/" + assetPath), imgView, null);
//			return true;
//		}
//		catch(IOException ex) {//files in SD card
//			File file = new File(ctx.getFilesDir().getParentFile().getPath()+"/images/"+assetPath);
//			if(file.exists()){
//				into(Picasso.with(ctx).load(file).fit().centerCrop() , imgView, null);
//			}
//			else{
//				if(downloadToAssets){//from cdn //TODO: convert this for synchronous use
//					imgView.setTag(new LoadAndSave(imgView, file, assetPath, downloadToAssets, completedLoadingImage));
//					if(width>0 && height>0)
//						Picasso.with(ctx).load(ServerCalls.CDN_PATH+assetPath).error(R.drawable.error_image).resize(width , height).into((LoadAndSave)imgView.getTag());
//					else{
//						Picasso.with(ctx).load(ServerCalls.CDN_PATH+assetPath).error(R.drawable.error_image).into((LoadAndSave)imgView.getTag());
//					}
//				}
//				else{
		into(Picasso.with(ctx).load(ServerCalls.CDN_PATH + assetPath), imgView, null);//directly
//				}
//			}
//		}
//		catch (Exception e) {
//			return false;
//		}
		return true;
	}

	public static Task<RequestCreator> loadImageIntoView(Context ctx, final ImageView imgView, final String assetPath, final boolean downloadToAssets) {
		return loadImageIntoView(ctx, imgView, assetPath, downloadToAssets, null);
	}


	public static Task<RequestCreator> loadImageIntoView(Context ctx, final ImageView imgView, final String assetPath, final boolean downloadToAssets, Transformation t) {
		return loadImageIntoView(ctx, imgView, assetPath, downloadToAssets, -1, -1, t);
	}

	public static Task<RequestCreator> loadImageIntoView(Context ctx, final ImageView imgView, final String assetPath, final boolean downloadToAssets, final int width, final int height, final Transformation transformation) {
		return getRequestCreatorTask(ctx , assetPath, downloadToAssets).onSuccess(new Continuation<RequestCreator, RequestCreator>() {

			@Override
			public RequestCreator then(Task<RequestCreator> task) throws Exception {
				RequestCreator requestCreator = task.getResult();
				if (transformation != null)
					requestCreator.transform(transformation);

				if (width > 0 && height > 0)
					requestCreator.resize(width, height);

				requestCreator.into(imgView);
				return requestCreator;
			}
		}, Task.UI_THREAD_EXECUTOR);
	}


	public void loadImageAsBg(final View view, final String assetPath, boolean downloadToAssets) {
		if (assetPath == null || assetPath.isEmpty())
			return;
		Task.callInBackground(new Callable<Bitmap>() {
			@Override
			public Bitmap call() throws Exception {
				RequestCreator requestCreator = null;
				if (assetPath.startsWith("http://") || assetPath.startsWith("https://")) {
					requestCreator = Picasso.with(context).load(assetPath);//.error(R.drawable.error_image);
				}
//				try {
//					InputStream ims = app.getContext().getAssets().open("images/" + assetPath); //assets folder
//					ims.close();
//					requestCreator = Picasso.with(app.getContext()).load("file:///android_asset/images/" + assetPath).error(R.drawable.error_image);
//				} catch (IOException e) {
//					Log.d(Config.QUIZAPP_ERR_LOG_TAG, "failed to load from assets");
//					e.printStackTrace();
//				}
//				File file = new File(app.getContext().getFilesDir().getParentFile().getPath()+"/images/"+assetPath);
//				if(file.exists()){
//					return Picasso.with(app.getContext()).load(file).error(R.drawable.error_image);
//				}
				if (requestCreator == null)
					requestCreator = Picasso.with(context).load(ServerCalls.CDN_PATH + assetPath);//.error(R.drawable.error_image);
				return requestCreator.get();
			}
		}).onSuccess(new Continuation<Bitmap, Void>() {
			@Override
			public Void then(Task<Bitmap> task) throws Exception {
				if (task.getResult() != null)
					UiUtils.setBg(view, new BitmapDrawable(view.getResources(), task.getResult()));
				;
				return null;
			}
		}, Task.UI_THREAD_EXECUTOR);
	}

//	public double getLevelFromPoints(double points){
//		return points;
////		2+n/3
////		increment: 3 3 4 4 4 5 5 5 6 6 6 7 7 7
////		sigma(3+k/3)
////		3 + k
////		3 6 10 14 18 23 28 33 39 45 51 58 65 72
////		3*n+(n/3)0 0 1 2 3 5 7 9 12 15 18 22 26)   (3*n+(n/3)+ N-1 shit)
////		2+(0 0 1 1 1 3 3 3 6 6 6 ) = 3+9*(1+2+3 ..) 3+9*(n*(n-1))/2 ;; 400+3*(1 2 3) (level-2)*(level-3)/2+(level-2)
////		200 400 700 1000 1300 1800 2300 2800 3600 4400 5200 ..
////		2 4 7 10 13 18 23 18 36 44 52
//	}

//	public double getPointsFromLevel(double level){
//		return 100*(2*level + (level*level - level)/6);
//	}

	float oneDp = -1;

	public float getInDp(int i) {
		if (oneDp == -1) {
			oneDp = context.getResources().getDimension(R.dimen.one_dp);
		}
		return i * oneDp;
	}

	float oneSp = -1;

	public float getInSp(int i) {
		if (oneSp == -1) {
			oneSp = context.getResources().getDimension(R.dimen.one_sp);
		}
		return i * oneSp;
	}

	public int dp2px(int dp) {
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
				context.getResources().getDisplayMetrics());
	}

	public static ListView setListViewHeightBasedOnChildren2(ListView myListView) {
		ListAdapter myListAdapter = myListView.getAdapter();
		if (myListAdapter == null || myListAdapter.getCount() == 0) {
			//do nothing return null
			return myListView;
		}
		//set listAdapter in loop for getting final size
		int totalHeight = 0;
		for (int size = 0; size < myListAdapter.getCount(); size++) {
			View listItem = myListAdapter.getView(size, null, myListView);
			listItem.measure(0, 0);
			totalHeight += listItem.getMeasuredHeight();
		}
		//setting listview item in chatListAdapter
		ViewGroup.LayoutParams params = myListView.getLayoutParams();
		params.height = totalHeight + (myListView.getDividerHeight() * (myListAdapter.getCount() - 1));
		myListView.setLayoutParams(params);
		return myListView;
	}

	public void blickAnimation(View view) {
		final Animation animation = new AlphaAnimation(1, 0); // Change alpha from fully visible to invisible
		animation.setDuration(500); // duration - half a second
		animation.setInterpolator(new LinearInterpolator()); // do not alter animation rate
		animation.setRepeatCount(Animation.INFINITE); // Repeat animation infinitely
		animation.setRepeatMode(Animation.REVERSE); // Reverse animation at the end so the button will fade back in
		view.startAnimation(animation);
	}

	protected void makeLinkClickable(SpannableStringBuilder strBuilder, final URLSpan span, final GenericListener<String> clickListener) {
		int start = strBuilder.getSpanStart(span);
		int end = strBuilder.getSpanEnd(span);
		int flags = strBuilder.getSpanFlags(span);
		ClickableSpan clickable = new ClickableSpan() {
			public void onClick(View view) {
				if (clickListener != null)
					clickListener.onData(span.getURL());
				else {
					genericLinkClickListener(span.getURL());
				}
			}

			@Override
			public void updateDrawState(TextPaint ds) {
				ds.setColor(Color.BLACK);

			}
		};
		strBuilder.setSpan(clickable, start, end, flags);
		strBuilder.removeSpan(span);
	}

	protected void genericLinkClickListener(String url) {
		// TODO Auto-generated method stub

	}

	public void setTextViewHTML(TextView text, String html, GenericListener<String> clickListener) {
		CharSequence sequence = Html.fromHtml(html);
		text.setMovementMethod(LinkMovementMethod.getInstance());
		SpannableStringBuilder strBuilder = new SpannableStringBuilder(sequence);
		URLSpan[] urls = strBuilder.getSpans(0, sequence.length(), URLSpan.class);
		for (URLSpan span : urls) {
			makeLinkClickable(strBuilder, span, clickListener);
		}
		text.setText(strBuilder);
	}


	public static Point getScreenDimetions(Activity activity) {
		WindowManager w = activity.getWindowManager();
		Point point = new Point();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
			Point size = new Point();
			w.getDefaultDisplay().getSize(size);
		} else {
			Display d = w.getDefaultDisplay();
			point.x = d.getWidth();
			point.y = d.getHeight();
		}
		return point;
	}

	public void populateViews(FragmentActivity activity , LinearLayout linearLayout, View[] views, Context context, View extraView) {
		extraView.measure(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

		// kv : May need to replace 'getSherlockActivity()' with 'this' or 'getActivity()'
		Display display = activity.getWindowManager().getDefaultDisplay();
		linearLayout.removeAllViews();
		int maxWidth = display.getWidth() - extraView.getMeasuredWidth() - 20;

		linearLayout.setOrientation(LinearLayout.VERTICAL);

		LinearLayout.LayoutParams params;
		LinearLayout newLL = new LinearLayout(context);
		newLL.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		newLL.setGravity(Gravity.LEFT);
		newLL.setOrientation(LinearLayout.HORIZONTAL);

		int widthSoFar = 0;

		for (int i = 0; i < views.length; i++) {
			LinearLayout LL = new LinearLayout(context);
			LL.setOrientation(LinearLayout.HORIZONTAL);
			LL.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
			LL.setLayoutParams(new ListView.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

			views[i].measure(0, 0);
			params = new LinearLayout.LayoutParams(views[i].getMeasuredWidth(), LayoutParams.WRAP_CONTENT);
			params.setMargins(5, 0, 5, 0);

			LL.addView(views[i], params);
			LL.measure(0, 0);
			widthSoFar += views[i].getMeasuredWidth();
			if (widthSoFar >= maxWidth) {
				linearLayout.addView(newLL);

				newLL = new LinearLayout(context);
				newLL.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
				newLL.setOrientation(LinearLayout.HORIZONTAL);
				newLL.setGravity(Gravity.LEFT);
				params = new LinearLayout.LayoutParams(LL.getMeasuredWidth(), LL.getMeasuredHeight());
				newLL.addView(LL, params);
				widthSoFar = LL.getMeasuredWidth();
			} else {
				newLL.addView(LL);
			}
		}
		linearLayout.addView(newLL);
	}

	public static int getColorFromResource(Context context , int id) {
		return context.getResources().getColor(id);
	}


	public Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
		int width = bm.getWidth();
		int height = bm.getHeight();
		float scaleWidth = ((float) newWidth) / width;
		float scaleHeight = ((float) newHeight) / height;
		// CREATE A MATRIX FOR THE MANIPULATION
		Matrix matrix = new Matrix();
		// RESIZE THE BIT MAP
		matrix.postScale(scaleWidth, scaleHeight);

		// "RECREATE" THE NEW BITMAP
		Bitmap resizedBitmap = Bitmap.createBitmap(
				bm, 0, 0, width, height, matrix, false);
		bm.recycle();
		return resizedBitmap;
	}

	public Bitmap fastblur(Bitmap sentBitmap, float scale, int radius) {

		int width = Math.min(50, Math.round(sentBitmap.getWidth() * scale));
		int height = Math.round(sentBitmap.getHeight() * (width * 1.0f) / sentBitmap.getWidth());
		Bitmap resizedBitmap = getResizedBitmap(sentBitmap, width, height);

		Bitmap bitmap = resizedBitmap.copy(sentBitmap.getConfig(), true);

		if (radius < 1) {
			return (null);
		}

		int w = bitmap.getWidth();
		int h = bitmap.getHeight();

		int[] pix = new int[w * h];
		Log.e("pix", w + " " + h + " " + pix.length);
		bitmap.getPixels(pix, 0, w, 0, 0, w, h);

		int wm = w - 1;
		int hm = h - 1;
		int wh = w * h;
		int div = radius + radius + 1;

		int r[] = new int[wh];
		int g[] = new int[wh];
		int b[] = new int[wh];
		int rsum, gsum, bsum, x, y, i, p, yp, yi, yw;
		int vmin[] = new int[Math.max(w, h)];

		int divsum = (div + 1) >> 1;
		divsum *= divsum;
		int dv[] = new int[256 * divsum];
		for (i = 0; i < 256 * divsum; i++) {
			dv[i] = (i / divsum);
		}

		yw = yi = 0;

		int[][] stack = new int[div][3];
		int stackpointer;
		int stackstart;
		int[] sir;
		int rbs;
		int r1 = radius + 1;
		int routsum, goutsum, boutsum;
		int rinsum, ginsum, binsum;

		for (y = 0; y < h; y++) {
			rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
			for (i = -radius; i <= radius; i++) {
				p = pix[yi + Math.min(wm, Math.max(i, 0))];
				sir = stack[i + radius];
				sir[0] = (p & 0xff0000) >> 16;
				sir[1] = (p & 0x00ff00) >> 8;
				sir[2] = (p & 0x0000ff);
				rbs = r1 - Math.abs(i);
				rsum += sir[0] * rbs;
				gsum += sir[1] * rbs;
				bsum += sir[2] * rbs;
				if (i > 0) {
					rinsum += sir[0];
					ginsum += sir[1];
					binsum += sir[2];
				} else {
					routsum += sir[0];
					goutsum += sir[1];
					boutsum += sir[2];
				}
			}
			stackpointer = radius;

			for (x = 0; x < w; x++) {

				r[yi] = dv[rsum];
				g[yi] = dv[gsum];
				b[yi] = dv[bsum];

				rsum -= routsum;
				gsum -= goutsum;
				bsum -= boutsum;

				stackstart = stackpointer - radius + div;
				sir = stack[stackstart % div];

				routsum -= sir[0];
				goutsum -= sir[1];
				boutsum -= sir[2];

				if (y == 0) {
					vmin[x] = Math.min(x + radius + 1, wm);
				}
				p = pix[yw + vmin[x]];

				sir[0] = (p & 0xff0000) >> 16;
				sir[1] = (p & 0x00ff00) >> 8;
				sir[2] = (p & 0x0000ff);

				rinsum += sir[0];
				ginsum += sir[1];
				binsum += sir[2];

				rsum += rinsum;
				gsum += ginsum;
				bsum += binsum;

				stackpointer = (stackpointer + 1) % div;
				sir = stack[(stackpointer) % div];

				routsum += sir[0];
				goutsum += sir[1];
				boutsum += sir[2];

				rinsum -= sir[0];
				ginsum -= sir[1];
				binsum -= sir[2];

				yi++;
			}
			yw += w;
		}
		for (x = 0; x < w; x++) {
			rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
			yp = -radius * w;
			for (i = -radius; i <= radius; i++) {
				yi = Math.max(0, yp) + x;

				sir = stack[i + radius];

				sir[0] = r[yi];
				sir[1] = g[yi];
				sir[2] = b[yi];

				rbs = r1 - Math.abs(i);

				rsum += r[yi] * rbs;
				gsum += g[yi] * rbs;
				bsum += b[yi] * rbs;

				if (i > 0) {
					rinsum += sir[0];
					ginsum += sir[1];
					binsum += sir[2];
				} else {
					routsum += sir[0];
					goutsum += sir[1];
					boutsum += sir[2];
				}

				if (i < hm) {
					yp += w;
				}
			}
			yi = x;
			stackpointer = radius;
			for (y = 0; y < h; y++) {
				// Preserve alpha channel: ( 0xff000000 & pix[yi] )
				pix[yi] = (0xff000000 & pix[yi]) | (dv[rsum] << 16) | (dv[gsum] << 8) | dv[bsum];

				rsum -= routsum;
				gsum -= goutsum;
				bsum -= boutsum;

				stackstart = stackpointer - radius + div;
				sir = stack[stackstart % div];

				routsum -= sir[0];
				goutsum -= sir[1];
				boutsum -= sir[2];

				if (x == 0) {
					vmin[y] = Math.min(y + r1, hm) * w;
				}
				p = x + vmin[y];

				sir[0] = r[p];
				sir[1] = g[p];
				sir[2] = b[p];

				rinsum += sir[0];
				ginsum += sir[1];
				binsum += sir[2];

				rsum += rinsum;
				gsum += ginsum;
				bsum += binsum;

				stackpointer = (stackpointer + 1) % div;
				sir = stack[stackpointer];

				routsum += sir[0];
				goutsum += sir[1];
				boutsum += sir[2];

				rinsum -= sir[0];
				ginsum -= sir[1];
				binsum -= sir[2];

				yi += w;
			}
		}

		Log.e("pix", w + " " + h + " " + pix.length);
		bitmap.setPixels(pix, 0, w, 0, 0, w, h);

		return (bitmap);
	}


	public void getBitmapFromURL(final String imageUrl, final GenericListener<Bitmap> genericListener) {
		Task.callInBackground(new Callable<Bitmap>() {
			@Override
			public Bitmap call() throws Exception {
				return getBitmapFromURL(imageUrl);
			}
		}).onSuccess(new Continuation<Bitmap, Object>() {
			@Override
			public Object then(Task<Bitmap> task) throws Exception {
				if(task.getResult()!=null)
					genericListener.onData(task.getResult());
				return null;
			}
		});
	}

	public Bitmap getBitmapFromURL(String src) {
		try {
			URL url = new URL(src);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setDoInput(true);
			connection.connect();
			InputStream input = connection.getInputStream();
			Bitmap myBitmap = BitmapFactory.decodeStream(input);
			return myBitmap;
		} catch (IOException e) {
			// Log exception
			return null;
		}
	}

	public void scrollToBottom(final ListView listView) {
		listView.post(new Runnable() {
			@Override
			public void run() {
				// Select the last row so it will scroll into view...
				if(listView.getAdapter()!=null)
					listView.setSelection(listView.getAdapter().getCount() - 1);
			}
		});
	}


	public void scrollToBottom(final ScrollView scroll){
		scroll.post(new Runnable() {
			@Override
			public void run() {
				scroll.scrollTo(0, scroll.getBottom() + dp2px(450));
			}
		});
	}



	public void promptInput(FragmentActivity activity , String title, int charLimit, String prevStatus, String okText , final GenericListener<String> dataInputListener) {
		final Dialog prompt = new Dialog(context,R.style.CustomDialogTheme3);
		ImageView closeButton;
		TextView titleView;
		final EditText messageContent;
		TextView okButton;
		LinearLayout baseLayout = (LinearLayout)activity.getLayoutInflater().inflate(R.layout.input_prompt, null);

		closeButton = (ImageView) baseLayout.findViewById(R.id.close_button);
		titleView = (TextView) baseLayout.findViewById(R.id.title);
		titleView.setText(title);
		messageContent = (EditText) baseLayout.findViewById(R.id.messageContent);
		messageContent.setText(prevStatus);
		okButton = (Button) baseLayout.findViewById(R.id.ok_button);
		okButton.setText(okText);
		closeButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				prompt.dismiss();
			}
		});
		okButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dataInputListener.onData(messageContent.getText().toString());
				prompt.dismiss();
			}
		});
		prompt.setContentView(baseLayout);
		prompt.show();
	}


	public void promptCreateStreamInput(FragmentActivity activity, String title, final GenericListener<Stream> streamCreateListener) {
		final Dialog prompt = new Dialog(context,R.style.CustomDialogTheme3);

		TextView titleView;
		LinearLayout baseLayout = (LinearLayout)activity.getLayoutInflater().inflate(R.layout.create_stream, null);

		titleView = (TextView) baseLayout.findViewById(R.id.title);
		titleView.setText(title);

		final EditText streamTitleTextInput = (EditText) baseLayout.findViewById(R.id.stream_title);
		final EditText streamSubTitleTextInput = (EditText) baseLayout.findViewById(R.id.stream_subtitle);

		Button okButton = (Button) baseLayout.findViewById(R.id.ok);
		Button cancelButton = (Button) baseLayout.findViewById(R.id.cancel);

		cancelButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				prompt.dismiss();
			}
		});

		okButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Stream s = new Stream();
				s.title = String.valueOf(streamTitleTextInput.getText());
				s.additionalInfo = String.valueOf(streamSubTitleTextInput.getText());
				if(s.title.trim().isEmpty()){

				}

				streamCreateListener.onData(s);
				prompt.dismiss();
			}
		});

		prompt.setContentView(baseLayout);
		prompt.show();
	}





}
