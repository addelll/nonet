package com.keiferstone.nonet;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.Toast;

import java.lang.ref.WeakReference;

import static com.keiferstone.nonet.ConnectionStatus.*;

public class Monitor {
    private WeakReference<Context> contextRef;
    private Configuration configuration;
    private boolean poll;
    private Toast toast;
    private Snackbar snackbar;
    private Callback callback;
    private Handler handler;

    Monitor(Context context) {
        contextRef = new WeakReference<>(context);
        configuration = new Configuration();
        poll = false;
        toast = null;
        snackbar = null;
        callback = null;
        handler = new Handler();
    }

    void start() {
        registerConnectivityReceiver();
        schedulePollTask();
    }

    void stop() {
        unregisterConnectivityReceiver();
        cancelPollTask();
    }

    void poll() {
        PollTask.run(configuration, new PollTask.OnPollCompletedListener() {
            @Override
            public void onPollCompleted(@ConnectionStatus int connectionStatus) {
                if (callback != null) {
                    callback.onConnectionChanged(connectionStatus);
                }

                if (connectionStatus == DISCONNECTED) {
                    if (toast != null) {
                        toast.show();
                    }
                    if (snackbar != null) {
                        snackbar.show();
                    }
                } else {
                    if (toast != null) {
                        toast.cancel();
                    }
                    if (snackbar != null) {
                        snackbar.dismiss();
                    }
                }
            }
        });
    }

    private void registerConnectivityReceiver() {
        Context context = getContext();
        if (context != null) {
            context.registerReceiver(connectivityReceiver, ConnectivityReceiver.getIntentFilter());
        }
    }

    private void unregisterConnectivityReceiver() {
        Context context = getContext();
        if (context != null) {
            context.unregisterReceiver(connectivityReceiver);
        }
    }

    private void schedulePollTask() {
        if (poll) {
            int pollFrequency = configuration.getPollFrequency();
            if (pollFrequency > 0) {
                handler.postDelayed(pollTaskRunnable, pollFrequency * 1000);
            }
        }
    }

    private void cancelPollTask() {
        handler.removeCallbacks(pollTaskRunnable);
    }

    @Nullable
    private Context getContext() {
        if (contextRef != null) {
            return contextRef.get();
        }

        return null;
    }

    @Nullable
    private Toast getDefaultToast() {
        Context context = getContext();
        if (context != null) {
            return Toast.makeText(context, R.string.no_server_connection_message, Toast.LENGTH_SHORT);
        }

        return null;
    }

    @Nullable
    private Snackbar getDefaultSnackbar() {
        Context context = getContext();
        if (context != null && context instanceof Activity) {
            View view = ((Activity) context).findViewById(android.R.id.content);
            if (view != null) {
                return Snackbar.make(view, R.string.no_server_connection_message, Snackbar.LENGTH_INDEFINITE);
            }
        }

        return null;
    }

    public static class Builder {
        Monitor monitor;

        Builder(Context context) {
            monitor = new Monitor(context);
        }

        /**
         * Set the configuration for this {@link Monitor}.
         *
         * @param configuration The configuration to set.
         *
         * @return This {@link Monitor.Builder}.
         */
        public Builder configure(Configuration configuration) {
            monitor.configuration = configuration;
            return this;
        }

        /**
         * Enable polling for this monitor.
         *
         * @return This {@link Monitor.Builder}.
         */
        public Builder poll() {
            monitor.poll = true;
            return this;
        }

        /**
         * Show the default {@link Toast} message when there is no connectivity.
         *
         * @return This {@link Monitor.Builder}.
         */
        public Builder toast() {
            monitor.toast = monitor.getDefaultToast();
            return this;
        }

        /**
         * Show the default {@link Snackbar} message when there is no connectivity.
         *
         * @return This {@link Monitor.Builder}.
         */
        public Builder snackbar() {
            monitor.snackbar = monitor.getDefaultSnackbar();
            return this;
        }

        /**
         * Set a {@link Callback} to be invoked when there is a connectivity event.
         *
         * @param callback The callback to set.
         *
         * @return This {@link Monitor.Builder}.
         */
        public Builder callback(Callback callback) {
            monitor.callback = callback;
            return this;
        }

        /**
         * Start monitoring network connectivity.
         *
         * @return The {@link Monitor}
         */
        public Monitor start() {
            monitor.start();
            return monitor;
        }
    }

    public interface Callback {
        void onConnectionChanged(@ConnectionStatus int connectionStatus);
    }


    private ConnectivityReceiver connectivityReceiver = new ConnectivityReceiver() {
        @Override
        public void onConnectivityChanged(@ConnectionStatus int connectionStatus) {
            // Don't really care what connectionStatus is, poll no matter what.
            poll();
        }
    };

    private Runnable pollTaskRunnable = new Runnable() {
        @Override
        public void run() {
            poll();
            schedulePollTask();
        }
    };
}
