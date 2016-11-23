package io.sodaoud.eventtracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import io.sodaoud.eventtracker.util.Constants;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static io.sodaoud.eventtracker.util.Constants.JSON_CONNENTION;
import static io.sodaoud.eventtracker.util.Constants.JSON_META;

/**
 * Created by sofiane on 11/19/16.
 */
public class EventTracker {

    private final String TAG = EventTracker.class.getName();
    /**
     * The constant POST_TYPE_PERIODIC. If used the events are delivered periodically {@link #setPeriodicTime(int)}
     */
    public static final int POST_TYPE_PERIODIC = 1;
    /**
     * The constant POST_TYPE_NUMBER. If used the events are delivered when their count is greater than a certain value {@link #setNumberOfEventsToPost(int)}
     */
    public static final int POST_TYPE_NUMBER = 2;

    private static EventTracker instance;
    private static Pattern pattern = Pattern.compile("^ev_[A-Za-z0-9]+$");

    /**
     * Gets instance.
     *
     * @return the instance
     */
    public static EventTracker getInstance() {
        return instance;
    }

    /**
     * Init event tracker.
     *
     * @param context the context
     * @param apiKey  the api key
     * @return the event tracker instance
     */
    public static EventTracker init(Context context, String apiKey) {
        instance = new EventTracker(context, apiKey);
        return instance;
    }

    private String apiKey;
    private Context context;

    private DBHelper dbHelper;


    private BroadcastReceiver connectivityReceiver;
    private Runnable networkTask;
    private Handler networkHandler;
    private Handler databaseHandler;

    private final ConnectivityManager connectivityManager;

    private OkHttpClient client;// USED ONLY WHEN THE APPLICATION ADD IT AS A DEPENDENCY

    private boolean isUploading;

    private int postType = POST_TYPE_NUMBER;

    private int numberOfEventsToPost = 1;
    private int periodicTime = 10 * 60 * 1000;

    private EventTracker(Context context, String apiKey) {
        this.context = context.getApplicationContext();
        this.apiKey = apiKey;

        try {
            Class.forName("okhttp3.OkHttpClient");
            client = new OkHttpClient.Builder()
                    .connectTimeout(2, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .build();
        } catch (ClassNotFoundException e) {
            Log.d(TAG, "OkHttp Not Found");
        }

        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        initDatabase();
        initHandlers();

        isUploading = false;
    }

    /**
     * Sets OkHttp3 client.
     *
     * @param client the client
     */
    public void setClient(OkHttpClient client) {
        this.client = client;
    }

    /**
     * Sets post type. either
     *
     * @param postType the post type
     */
    public void setPostType(int postType) {
        this.postType = postType;
        if (postType == POST_TYPE_PERIODIC) {
            initPeriodicUpload();
        }
    }

    /**
     * Sets periodic time.
     *
     * @param periodicTime the periodic time
     */
    public void setPeriodicTime(int periodicTime) {
        this.periodicTime = periodicTime;
        if (postType == POST_TYPE_PERIODIC) {
            initPeriodicUpload();
        }
    }

    /**
     * Sets number of events to post.
     *
     * @param numberOfEventsToPost the number of events to post
     */
    public void setNumberOfEventsToPost(int numberOfEventsToPost) {
        boolean postNow = false;
        if (numberOfEventsToPost < this.numberOfEventsToPost) {
            postNow = true;
        }
        this.numberOfEventsToPost = numberOfEventsToPost;
        if (postNow)
            checkAndPost();

    }

    private void initHandlers() {
        HandlerThread thread = new HandlerThread("networkThread");
        thread.start();
        networkHandler = new Handler(thread.getLooper());
        thread = new HandlerThread("databaseThread");
        thread.start();
        databaseHandler = new Handler(thread.getLooper());
    }

    private void initPeriodicUpload() {
        if (postType == POST_TYPE_PERIODIC) {
            if (networkTask != null)
                networkHandler.removeCallbacks(networkTask);
            networkTask = new Runnable() {
                @Override
                public void run() {
                    NetworkInfo info = connectivityManager.getActiveNetworkInfo();
                    if (info != null) {
                        doPostEvents(info.getTypeName().toLowerCase());
                        networkHandler.postDelayed(networkTask, periodicTime);
                    } else {
                        registerConnectivityReceiver();
                    }
                }
            };
            networkHandler.postDelayed(networkTask, periodicTime);
        }
    }

    private void initDatabase() {
        dbHelper = new DBHelper(context);
    }

    /**
     * Send event.
     *
     * @param name the name
     */
    public void sendEvent(@NonNull String name) {
        sendEvent(name, null);
    }

    /**
     * Send event.
     *
     * @param name   the name
     * @param params the params
     */
    public void sendEvent(@NonNull final String name, @Nullable final Map<String, String> params) {
        if (!validateName(name)) {
            Log.e(TAG, String.format("The name \'%s\' does not match the pattern", name));
            return;
        }

        databaseHandler.post(new Runnable() {
            @Override
            public void run() {
                String data = (params == null) ? null : new JSONObject(params).toString(); // get a string such as: {"key1":"Value1","key2": "value2"}
                long unixTime = System.currentTimeMillis() / 1000L;
                long result = dbHelper.saveEvent(name, data, unixTime);
                if (result > -1) {
                    checkAndPost();
                }
            }
        });
    }

    /**
     * Validate name boolean.
     *
     * @param name the name
     * @return the boolean
     */
    public boolean validateName(@NonNull String name) {
        return pattern.matcher(name).matches();
    }

    private void checkAndPost() {
        if (postType == POST_TYPE_NUMBER && dbHelper.eventsCount() >= numberOfEventsToPost && !isUploading) {
            if (networkTask != null) {
                networkHandler.removeCallbacks(networkTask);
            }
            final NetworkInfo info = connectivityManager.getActiveNetworkInfo();
            if (info == null) {
                registerConnectivityReceiver();
            } else {
                networkTask = new Runnable() {
                    @Override
                    public void run() {
                        doPostEvents(info.getTypeName().toLowerCase());
                    }
                };
                networkHandler.post(networkTask);
            }
        }
    }

    private void registerConnectivityReceiver() {
        if (connectivityReceiver == null) {
            connectivityReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    NetworkInfo info = connectivityManager.getActiveNetworkInfo();
                    if (info != null) {
                        doPostEvents(info.getTypeName().toLowerCase());
                        if (postType == POST_TYPE_PERIODIC) {
                            initPeriodicUpload();
                        }
                        context.unregisterReceiver(connectivityReceiver);
                        connectivityReceiver = null;
                    } else {
                        Log.i(TAG, "Still no connection");
                        // DO NOTHING
                    }
                }
            };
            context.registerReceiver(connectivityReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION), null, networkHandler);
        }
    }

    private void doPostEvents(String connectionInfo) {
        isUploading = true;
        List<JSONObject> events = dbHelper.getEvents();
        if (events != null && !events.isEmpty()) {
            for (JSONObject event : events) {
                try {
                    event.getJSONObject(JSON_META).put(JSON_CONNENTION, connectionInfo);
                    if (client != null) {
                        postUsingOkHttp(event);
                    } else {
                        postUsingURLConnection(event);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        isUploading = false;
    }

    private void postUsingURLConnection(JSONObject event) {
        try {
            URL url = new URL(Constants.URL);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(2000);
            conn.setRequestMethod("POST");
            conn.setRequestProperty(Constants.HEADER_API_KEY, apiKey);
            conn.setRequestProperty("Content-Type", "application/json");

            conn.connect();

            DataOutputStream out = new DataOutputStream(conn.getOutputStream());
            out.writeBytes(event.toString());
            out.flush();
            out.close();

            int response = conn.getResponseCode();
            Log.d(TAG, "The response is: " + response);

            if (response == 201) {
                removeEventFromDB(event);
            }
            if (response == 400) {
                InputStream is = conn.getErrorStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder builder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line).append("\n");
                }
                reader.close();
                Log.e(TAG, "Event not created \n" + builder.toString());
            }
        } catch (java.net.SocketTimeoutException e) {
            Log.e(TAG, "Server is not responding");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void postUsingOkHttp(JSONObject event) {
        try {
            RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), event.toString());
            Request request = new Request.Builder()
                    .url(Constants.URL)
                    .addHeader(Constants.HEADER_API_KEY, apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();
            Response response = client.newCall(request).execute();

            if (response.code() == 201) {
                removeEventFromDB(event);
            }
            Log.e(TAG, "Event not created \n" + response.body().string());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void removeEventFromDB(final JSONObject event) {
        databaseHandler.post(new Runnable() {
            @Override
            public void run() {
                dbHelper.removeEvent(event);
            }
        });
    }

}
