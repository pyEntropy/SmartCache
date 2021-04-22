package com.retrofitdemo;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

import dimitrovskif.smartcache.BasicCaching;
import dimitrovskif.smartcache.SmartCall;
import dimitrovskif.smartcache.SmartCallFactory;
import dimitrovskif.smartcache.SmartNetwork;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;

public class MainActivity extends Activity {
    static class Comment {
        public String email;
        public String body;
    }

    public interface CommentAPI {
        @GET("comments")
        SmartCall<List<Comment>> getComments();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://mockbin.org/bin/ec4a0020-37d0-4221-b4fd-7002000b2019/")
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(SmartCallFactory.createBasic(this))
                .build();

        CommentAPI service = retrofit.create(CommentAPI.class);

        Runnable loadComments = () -> {
            service.getComments().enqueue(new Callback<List<Comment>>() {
                @Override
                public void onResponse(Call<List<Comment>> call, Response<List<Comment>> response) {
                    if(SmartNetwork.isResponseFromNetwork(response)) {
                        swipeRefreshLayout.setRefreshing(false);
                    }

                    List<Comment> comments = response.body();
                    if (comments != null) {
                        runOnUiThread(() -> {
                            commentAdapter.addAll(comments);
                        });
                    }
                }

                @Override
                public void onFailure(Call<List<Comment>> call, Throwable t) {
                    Log.w("Filip", t.getMessage());
                }
            });
        };

        loadComments.run();

        this.swipeRefreshLayout = findViewById(R.id.swipe_refresh);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            commentAdapter.clear();
            loadComments.run();
        });
        swipeRefreshLayout.setRefreshing(true);

        this.commentAdapter = new CommentAdapter(this);
        ListView commentListView = findViewById(R.id.demoList);
        commentListView.setAdapter(commentAdapter);
    }

    private CommentAdapter commentAdapter = null;
    private SwipeRefreshLayout swipeRefreshLayout = null;

    private static class CommentAdapter extends ArrayAdapter<Comment>{
        CommentAdapter(Context ctx) {
            super(ctx, R.layout.comment_row, R.id.comment_text);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = super.getView(position, convertView, parent);
            Comment comm = getItem(position);
            ((TextView)row.findViewById(R.id.comment_text)).setText(comm.body);
            ((TextView)row.findViewById(R.id.email)).setText(comm.email);
            return row;
        }
    }
}