package com.example.a1.emergencyapplication;

/**
 * Created by 1 on 8/11/2016.
 */
public interface Constants {
    /**
     * The minimum distance of user location from the old one,
     * to update the data of UI elements referred to location values
     * in meters
     */
    int  MIN_USER_PROXIMITY = 25;
    /**
     * duration of light signal for sending morse SOS, equivalent to point
     */
    int SHORT_MORSE_DURATION = 10;
    int LONG_MORSE_DURATION = 50 * SHORT_MORSE_DURATION;
    /**
     * minimum time interval to get new user location data
     * in milliseconds
     */
    int MIN_GPS_REQUEST_INTERVAL = 2000;
}
