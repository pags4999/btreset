package com.example.btreset;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.TextView;

public class MainActivity extends Activity {
    private TextView tv;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        tv = new TextView(this);
        tv.setTextSize(28);
        tv.setGravity(Gravity.CENTER);
        setContentView(tv);

        if (Build.VERSION.SDK_INT >= 31
                && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
        } else {
            startReset();
        }
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] p, int[] r) {
        if (r.length > 0 && r[0] == PackageManager.PERMISSION_GRANTED) startReset();
        else { say("Нет разрешения Bluetooth"); }
    }

    private void say(final String s) {
        runOnUiThread(() -> tv.setText(s));
    }

    private void startReset() {
        new Thread(() -> {
            try {
                BluetoothAdapter a = BluetoothAdapter.getDefaultAdapter();
                if (a == null) { say("Bluetooth не найден"); return; }

                say("Выключаю Bluetooth...\nПульт на время отключится");
                a.disable();
                for (int i = 0; i < 20 && a.getState() != BluetoothAdapter.STATE_OFF; i++)
                    Thread.sleep(500);

                // Попытка перезапустить аудиосервер (работает только с root)
                say("Перезапускаю аудио (если есть root)...");
                try {
                    Process p = Runtime.getRuntime().exec(new String[]{"su", "-c",
                            "killall audioserver mediaserver android.hardware.audio@2.0-service 2>/dev/null"});
                    p.waitFor();
                } catch (Exception ignored) { }
                Thread.sleep(2000);

                say("Включаю Bluetooth...");
                a.enable();
                for (int i = 0; i < 30 && a.getState() != BluetoothAdapter.STATE_ON; i++)
                    Thread.sleep(500);

                say("Готово.\nЕсли пульт не отвечает, нажмите на нём любую кнопку\nи подождите несколько секунд");
                Thread.sleep(8000);
                runOnUiThread(this::finish);
            } catch (Exception e) {
                say("Ошибка: " + e.getMessage());
            }
        }).start();
    }
}
