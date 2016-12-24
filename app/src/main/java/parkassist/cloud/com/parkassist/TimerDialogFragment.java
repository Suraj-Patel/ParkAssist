package parkassist.cloud.com.parkassist;

import android.app.AlarmManager;
import android.app.DialogFragment;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.icu.util.Calendar;
import android.icu.util.GregorianCalendar;
import android.icu.util.TimeZone;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TimePicker;

import java.security.Timestamp;
import java.util.Date;
import java.util.Locale;

import static android.app.AlarmManager.RTC_WAKEUP;

/**
 * Created by Tejas Shah on 12/21/2016.
 */

public class TimerDialogFragment extends DialogFragment {

    private GregorianCalendar gdate = new GregorianCalendar(TimeZone.getTimeZone("America/New_York"));
    Timestamp ts ;
    private int a_year,a_month,a_day;
    final Calendar calendar = Calendar.getInstance(Locale.getDefault());
    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.timer_fragment, container, false);
        getDialog().setTitle("Simple Dialog");

        DatePicker datepicker = (DatePicker)view.findViewById(R.id.date_parkTimer);
        final TimePicker timepicker = (TimePicker)view.findViewById(R.id.time_ParkTimer);
        final Button btn_setTimer = (Button)view.findViewById(R.id.btn_setTimer);


        int year = calendar.get(Calendar.YEAR);
        final int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DATE);

        datepicker.init(year, month, day, new DatePicker.OnDateChangedListener() {
            @Override
            public void onDateChanged(DatePicker datePicker, int year, int month, int day) {
                //gdate.set(year,month,day);
                a_year = year;
                a_month = month;
                a_day = day;
                datePicker.setVisibility(View.INVISIBLE);
                timepicker.setVisibility(View.VISIBLE);
                btn_setTimer.setVisibility(View.VISIBLE);
            }
        });

        datepicker.setMinDate(System.currentTimeMillis() - 1000);
        datepicker.setMaxDate(new Date().getTime() + (1000*60*60*24*7));

        btn_setTimer.setOnClickListener(new View.OnClickListener() {
            @Override

            public void onClick(View view) {
                calendar.setTimeZone(TimeZone.getTimeZone("America/New_York"));
                calendar.set(a_year,a_month,a_day,timepicker.getHour(),timepicker.getMinute());
                start(view);
                ParkSearch parkSearchActivity = (ParkSearch) getContext();
                Snackbar.make(parkSearchActivity.findViewById(R.id.fab),"Parking Alarm Set", Snackbar.LENGTH_LONG).show();
                dismiss();
            }
        });
        return view;


    }

    public void start(View view) {
        Context context = getContext();

        Intent alarmIntent = new Intent(getActivity(), AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getActivity(), 1, alarmIntent, 0);

        AlarmManager manager = (AlarmManager)context.getSystemService(context.ALARM_SERVICE);
        AlarmManager.AlarmClockInfo clockInfo = manager.getNextAlarmClock();
        Snackbar.make(view,String.valueOf(clockInfo.getTriggerTime()), Snackbar.LENGTH_LONG).show();
        long alarmtime = calendar.getTimeInMillis();
        manager.setExact(RTC_WAKEUP,alarmtime, pendingIntent);

    }

}
