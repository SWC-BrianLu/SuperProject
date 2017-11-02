package com.example.lubrian.superproject;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.example.lubrian.superproject.protocol.IdolData;
import com.example.lubrian.superproject.protocol.IdolService;
import com.example.lubrian.superproject.thriftManager.ThriftUtils;

import org.apache.thrift.async.AsyncMethodCallback;

public class MainActivity extends AppCompatActivity {

    private IdolService.AsyncIface asyncIface = ThriftUtils.getAsyncClient(StaticValue.SERVER_URL);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try{
            asyncIface.get("BIGBANG", new AsyncMethodCallback<IdolData>() {
                @Override
                public void onComplete(IdolData response) {
                    if(response != null){
                        Log.e("debug","get idol:" + response.name);
                    }
                }

                @Override
                public void onError(Exception exception) {
                    exception.printStackTrace();
                    Log.e("debug","onError");
                }
            });
        }catch (Exception e){
            Log.e("debug","Error");
        }
    }
}
