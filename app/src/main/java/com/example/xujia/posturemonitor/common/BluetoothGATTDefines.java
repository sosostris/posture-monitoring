package com.example.xujia.posturemonitor.common;

import java.util.HashMap;
import java.util.Map;

public class BluetoothGATTDefines {
    public static final Map<Integer,String> gattErrorCodeStrings;
    static {
        gattErrorCodeStrings = new HashMap<Integer, String>();
        gattErrorCodeStrings.put(0, "GATT Success");
        gattErrorCodeStrings.put(2, "GATT Read Not Permitted");
        gattErrorCodeStrings.put(3, "GATT Write Not Permitted");
        gattErrorCodeStrings.put(5, "GATT Insufficient Authentication");
        gattErrorCodeStrings.put(6, "GATT Request Not Supported");
        gattErrorCodeStrings.put(7, "GATT Invalid Offset");
        gattErrorCodeStrings.put(13, "GATT Invalid Attribute Length");
        gattErrorCodeStrings.put(15, "GATT Insufficient Encryption");
        gattErrorCodeStrings.put(143, "GATT Connection Congested");
        gattErrorCodeStrings.put(257, "GATT Failure");
    }
}
