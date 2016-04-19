package zdl.softinfo.com.leidaviewmaster;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class MainActivity extends AppCompatActivity {
    private Dialog showLeidaDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        showLeidaDialog = new Dialog(this, R.style.CustomDialog);
        View layout = LayoutInflater.from(MainActivity.this).inflate(
                R.layout.home_shows, null);
        final LeidaView v = (LeidaView) layout.findViewById(R.id.leida);
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        v.setLayoutParams(params);
        v.setMyHead(R.mipmap.ic_launcher);
        v.setOnFinishCallback(new LeidaView.OnFinished() {
            @Override
            public void onFinish() {
                showLeidaDialog.dismiss();
                v.stopSearch();
                v.recycle();
                showLeidaDialog = null;
            }
        });
        v.startSearch();
        showLeidaDialog.setContentView(v);
        showLeidaDialog.setCancelable(false);
        showLeidaDialog.setCanceledOnTouchOutside(false);
        showLeidaDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {

            }
        });
        showLeidaDialog.show();

    }
}
