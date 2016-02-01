package com.superlifesize.gifdrawabledemo;

import android.content.Context;
import android.content.res.TypedArray;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.GlideDrawableImageViewTarget;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;


public class MainActivity extends ActionBarActivity {

    public static final String TAG = MainActivity.class.getSimpleName();

    private Giphy giphy;
    private GiphyTrendingData giphyTrendingData;
    private ImageView imageView;
    private pl.droidsonroids.gif.GifImageView gifImageView;
    private Button refresh_button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = (ImageView) findViewById(R.id.glide_gif);
        gifImageView = (pl.droidsonroids.gif.GifImageView) findViewById(R.id.gif_drawable);
        refresh_button = (Button) findViewById(R.id.refresh_button);
        refresh_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                refresh();
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        getGiphy();
        loadRandomDrawable();
    }

    private void loadRandomDrawable() {
        TypedArray images = getResources().obtainTypedArray(R.array.cat_images);
        int randImage = (int) (Math.random() * images.length());
        Log.i(TAG, "Random Image: " + randImage);

        int image = images.getResourceId(randImage, R.drawable.cutecat001);
        gifImageView.setImageResource(image);
        images.recycle();
    }

    private Giphy parseTrendingData(String jsonData)throws JSONException {
        Giphy giphy = new Giphy();

        giphy.setGiphyTrendingData(getGif(jsonData));

        return giphy;
    }

    private GiphyTrendingData[] getGif(String jsonData) throws JSONException {
        JSONObject giphy = new JSONObject(jsonData);
        JSONArray data = giphy.getJSONArray("data");

        final int NUM_OF_GIFS = data.length();
        GiphyTrendingData[] gifs = new GiphyTrendingData[NUM_OF_GIFS];

        for(int i = 0; i < NUM_OF_GIFS; i++) {
            JSONObject trendingGif = data.getJSONObject(i);
            GiphyTrendingData gif = new GiphyTrendingData();

            JSONObject images = trendingGif.getJSONObject("images");
            JSONObject original = images.getJSONObject("original");

            gif.setUrl(original.getString("url"));

            gifs[i] = gif;
        }

        return gifs;
    }

    private void updateDisplay() {
        GiphyTrendingData[] gifs = giphy.getGiphyTrendingData();
        int randImage = (int) (Math.random() * gifs.length);
        GiphyTrendingData gif = gifs[randImage];
        String gifUrl = gif.getUrl();
        Log.d(TAG, "from update URL: " + gifUrl);

        Glide.with(MainActivity.this)
                .load(gifUrl)
                .thumbnail(0.1f)
                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .centerCrop()
                .into(new GlideDrawableImageViewTarget(imageView) {
                    @Override
                    public void onResourceReady(GlideDrawable resource, GlideAnimation<? super GlideDrawable> animation) {
                        super.onResourceReady(resource, animation);
                        //check isRefreshing
                    }
                });
    }

    private void getGiphy () {
        //URL Format: http://api.giphy.com/v1/gifs/search?q=cute+cat&api_key=dc6zaTOxFJmzC&limit=1&offset=0
        //Random Search URL: http://api.giphy.com/v1/gifs/random?api_key=dc6zaTOxFJmzC&tag=cute+funny+cat+kitten
        //Trending Search URL: http://api.giphy.com/v1/gifs/trending?api_key=dc6zaTOxFJmzC
        String apiKey = "dc6zaTOxFJmzC"; //Giphy's Public API Key

        String giphyUrl =
                "http://api.giphy.com/v1/gifs/trending" +
                        "?api_key=" +
                        apiKey;

        if (isNetworkAvailable()) {

            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(giphyUrl)
                    .build();

            Call call = client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Request request, IOException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
//                            toggleRefresh();
                        }
                    });
                    Log.i(TAG, "Request Failure");
                }

                @Override
                public void onResponse(Response response) throws IOException {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                        }
                    });
                    try {
                        String jsonData = response.body().string();
                        if (response.isSuccessful()) {
                            giphy = parseTrendingData(jsonData);
                            Log.v(TAG, "Giphy Gif Data from Response: " + giphy);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateDisplay();
                                }
                            });
                        } else {
                            Log.i(TAG, "Response Unsuccessful");
                        }
                    }
                    catch (IOException e) {
                        Log.e(TAG, "Exception Caught: ", e);
                    }
                    catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        } else {
            Toast.makeText(this, "Network is not available!", Toast.LENGTH_LONG).show();
        }

    }

    private boolean isNetworkAvailable() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        boolean isAvailable = false;
        if (networkInfo != null && networkInfo.isConnected()) {
            isAvailable = true;
        }
        return isAvailable;
    }

}
