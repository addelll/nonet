package com.keiferstone.nonet;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.widget.Toast;

import java.lang.ref.WeakReference;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;

import static com.keiferstone.nonet.ConnectionStatus.*;

public class Monitor {
    private WeakReference<Context> contextRef;
    private Configuration configuration;
    private boolean poll;
    private Toast toast;
    private Snackbar snackbar;
    private Callback callback;
    private Handler handler;
    private Observable<Integer> observable;

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
                    callback.onConnectionEvent(connectionStatus);
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

    /**
     * Observe this {@link Monitor} for connectivity events. When there is a connectivity event,
     * the {@link ConnectionStatus} will be emitted to {@link io.reactivex.Observer#onNext(Object)}.
     *
     * @return An {@link Observable} that emits a {@link ConnectionStatus} on connectivity events.
     */
    public Observable<Integer> observe() {
        createObservable();
        return observable;
    }

    private void createObservable() {
        if (observable == null) {
            observable = Observable.create(new ObservableOnSubscribe<Integer>() {
                @Override
                public void subscribe(ObservableEmitter<Integer> e) throws Exception {
                    callback = new ObservableCallbackInterceptor(callback, e);
                }
            });
        }
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
            monitor.toast = ToastFactory.getToast(monitor.getContext());
            return this;
        }

        /**
         * Show a {@link Toast} with the specified message when there is no connectivity.
         *
         * @param message The message to show.
         *
         * @return This {@link Monitor.Builder}.
         */
        public Builder toast(String message) {
            monitor.toast = ToastFactory.getToast(monitor.getContext(), message);
            return this;
        }

        /**
         * Show a {@link Toast} with the specified message when there is no connectivity.
         *
         * @param messageRes The message to show.
         *
         * @return This {@link Monitor.Builder}.
         */
        public Builder toast(@StringRes int messageRes) {
            monitor.toast = ToastFactory.getToast(monitor.getContext(), messageRes);
            return this;
        }

        /**
         * Show a custom {@link Toast} when there is no connectivity.
         *
         * @param toast The {@link Toast} to show.
         *
         * @return This {@link Monitor.Builder}.
         */
        public Builder toast(Toast toast) {
            monitor.toast = toast;
            return this;
        }

        /**
         * Show the default {@link Snackbar} message when there is no connectivity.
         *
         * @return This {@link Monitor.Builder}.
         */
        public Builder snackbar() {
            monitor.snackbar = SnackbarFactory.getSnackbar(monitor.getContext());
            return this;
        }

        /**
         * Show a {@link Snackbar} with the specified message when there is no connectivity.
         *
         * @param message The message to show.
         *
         * @return This {@link Monitor.Builder}.
         */
        public Builder snackbar(String message) {
            monitor.snackbar = SnackbarFactory.getSnackbar(monitor.getContext(), message);
            return this;
        }

        /**
         * Show a {@link Snackbar} with the specified message when there is no connectivity.
         *
         * @param messageRes The message to show.
         *
         * @return This {@link Monitor.Builder}.
         */
        public Builder snackbar(@StringRes int messageRes) {
            monitor.snackbar = SnackbarFactory.getSnackbar(monitor.getContext(), messageRes);
            return this;
        }

        /**
         * Show a custom {@link Snackbar} when there is no connectivity.
         *
         * @param snackbar The {@link Snackbar} to show.
         *
         * @return This {@link Monitor.Builder}.
         */
        public Builder snackbar(Snackbar snackbar) {
            monitor.snackbar = snackbar;
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
        void onConnectionEvent(@ConnectionStatus int connectionStatus);
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
