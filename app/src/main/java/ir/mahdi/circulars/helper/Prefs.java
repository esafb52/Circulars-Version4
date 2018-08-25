package ir.mahdi.circulars.helper;

import android.content.Context;
import android.content.SharedPreferences;

public class Prefs {
    private static String SERVER = "server";

    private static String LUANCH = "run";

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences("set", 0);
    }

    public static int getSERVER(Context context) {
        return getPrefs(context).getInt(SERVER, 321);
    }


    public static int getLUANCH(Context context) {
        return getPrefs(context).getInt(LUANCH, 0);
    }

    public static void setSERVER(Context context, int value) {
        getPrefs(context).edit().putInt(SERVER, value).commit();
    }

    public static void setLUANCH(Context context, int value) {
        getPrefs(context).edit().putInt(LUANCH, value).commit();
    }
}
