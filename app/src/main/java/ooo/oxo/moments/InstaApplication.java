/*
 * Moments - To the best Instagram client
 * Copyright (C) 2015  XiNGRZ <xxx@oxo.ooo>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program;  if not, see <http://www.gnu.org/licenses/>.
 */

package ooo.oxo.moments;

import android.app.Application;
import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Date;

import ooo.oxo.moments.net.TimestampTypeAdapter;
import retrofit.GsonConverterFactory;
import retrofit.Retrofit;
import retrofit.RxJavaCallAdapterFactory;

public class InstaApplication extends Application {

    private OkHttpClient httpClient;

    private Retrofit retrofit;

    public static InstaApplication from(Context context) {
        return (InstaApplication) context.getApplicationContext();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        //noinspection ConstantConditions
        if (TextUtils.isEmpty(BuildConfig.CLIENT_ID) || TextUtils.isEmpty(BuildConfig.CLIENT_SECRET)) {
            throw new IllegalStateException("Please create a \"client.properties\" in the same" +
                    " directory of this module to specify your CLIENT_ID and CLIENT_SECRET.");
        }

        InstaSharedState.createInstance(this);

        httpClient = new OkHttpClient();
        httpClient.interceptors().add(chain -> {
            Request request = chain.request();
            Log.d("OkHttp", request.toString());
            Response response = chain.proceed(request);
            Log.d("OkHttp", response.toString());
            return response;
        });

        InstaSharedState.getInstance().applyProxy();

        Gson gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .registerTypeAdapter(Date.class, new TimestampTypeAdapter())
                .create();

        retrofit = new Retrofit.Builder()
                .client(httpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .baseUrl("https://api.instagram.com/")
                .build();
    }

    public OkHttpClient getHttpClient() {
        return httpClient;
    }

    void applyHttpProxy(String host, int port) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                httpClient.setProxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port)));
                return null;
            }
        }.execute();
    }

    void removeHttpProxy() {
        httpClient.setProxy(Proxy.NO_PROXY);
    }

    public <T> T createApi(Class<T> service) {
        return retrofit.create(service);
    }

}
