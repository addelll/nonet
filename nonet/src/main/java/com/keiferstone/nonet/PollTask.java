package com.keiferstone.nonet;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static com.keiferstone.nonet.ConnectionStatus.*;

class PollTask extends AsyncTask<Void, Void, Integer> {
    private static final String TAG = PollTask.class.getSimpleName();

    private Configuration configuration;
    private OnPollCompletedListener listener;
    private OkHttpClient client;

    static void run(Configuration configuration, OnPollCompletedListener listener) {
        PollTask pollTask = new PollTask(configuration, listener);
        pollTask.execute();
    }

    private PollTask(Configuration configuration, OnPollCompletedListener listener) {
        this.configuration = configuration;
        this.listener = listener;

        client = new OkHttpClient.Builder()
                .connectTimeout(configuration.getTimeout(), TimeUnit.SECONDS)
                .build();
    }

    @ConnectionStatus
    @Override
    protected Integer doInBackground(Void... params) {
        Request request = configuration.getRequest();
        if (request == null) {
            request = new Request.Builder()
                    .url(configuration.getEndpoint())
                    .build();
        }

        Response response;
        try {
            response = client.newCall(request).execute();
        } catch (IOException e) {
            e.printStackTrace();
            return DISCONNECTED;
        }

        if (response != null) {
            if (response.isSuccessful()) {
                return CONNECTED;
            }
        }

        return DISCONNECTED;
    }

    @Override
    protected void onPostExecute(@ConnectionStatus Integer integer) {
        Log.d(TAG, "Poll result: " + integer);

        if (listener != null) {
            listener.onPollCompleted(integer);
        }
    }

    interface OnPollCompletedListener {
        void onPollCompleted(@ConnectionStatus int connectionStatus);
    }
}
