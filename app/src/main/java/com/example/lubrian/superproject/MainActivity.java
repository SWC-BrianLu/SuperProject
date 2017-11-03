package com.example.lubrian.superproject;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.lubrian.superproject.protocol.IdolData;
import com.example.lubrian.superproject.protocol.IdolService;
import com.example.lubrian.superproject.protocol.ResYN;
import com.example.lubrian.superproject.thriftManager.ThriftUtils;

import org.apache.thrift.async.AsyncMethodCallback;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private IdolService.AsyncIface asyncIface = ThriftUtils.getAsyncClient(StaticValue.SERVER_URL);

    private EditText et_name,et_debutDate,et_company,et_type;
    private Button btn_send;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        View view = LayoutInflater.from(this).inflate(R.layout.activity_main,null);

        InitView(view);
    }

    private void InitView(View view){
        et_name = (EditText)view.findViewById(R.id.et_name);
        et_company = (EditText)view.findViewById(R.id.et_company);
        et_debutDate = (EditText) view.findViewById(R.id.et_debutDate);
        et_type = (EditText) view.findViewById(R.id.et_type);
        btn_send = (Button) view.findViewById(R.id.btn_send);
        btn_send.setOnClickListener(this);
    }

    private void addIdolData(IdolData idolData){
        try{
            asyncIface.addIdol(idolData, new AsyncMethodCallback<ResYN>() {
                @Override
                public void onComplete(ResYN response) {
                    if (response.success){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this,"新增成功",Toast.LENGTH_SHORT).show();
                            }
                        });
                    }else{
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this,"新增失敗",Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }

                @Override
                public void onError(Exception exception) {
                    exception.printStackTrace();
                    Log.e("debug","onError");
                }
            });
        }catch (Exception e){
            e.printStackTrace();
            Log.e("debug","Error");
        }
    }

    private void getIdolData(){
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
            e.printStackTrace();
            Log.e("debug","Error");
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_send:
            {
                IdolData idolData = new IdolData();
                idolData.name = et_name.getText().toString();
                idolData.debutDate = System.currentTimeMillis();
                idolData.company = et_company.getText().toString();
                idolData.type = et_type.getText().toString();
                addIdolData(idolData);
            }
            break;
        }
    }
}
