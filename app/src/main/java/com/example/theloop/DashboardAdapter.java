package com.example.theloop;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;

public class DashboardAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    static final int TYPE_HEADER = 0;
    static final int TYPE_WEATHER = 1;
    static final int TYPE_HEADLINES = 2;
    static final int TYPE_CALENDAR = 3;
    static final int TYPE_FUN_FACT = 4;
    static final int TYPE_HEALTH = 5;

    // Callbacks for data binding
    public interface Binder {
        void bindHeader(HeaderViewHolder holder);
        void bindWeather(WeatherViewHolder holder);
        void bindHeadlines(HeadlinesViewHolder holder);
        void bindCalendar(CalendarViewHolder holder);
        void bindFunFact(FunFactViewHolder holder);
        void bindHealth(HealthViewHolder holder);
    }

    private final Binder binder;
    private final String[] sectionOrder; // E.g., {"headlines", "calendar", "fun_fact", "health"}

    public DashboardAdapter(Binder binder, String[] sectionOrder) {
        this.binder = binder;
        this.sectionOrder = sectionOrder;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) return TYPE_HEADER;
        if (position == 1) return TYPE_WEATHER;

        // Remaining positions map to sectionOrder
        String section = sectionOrder[position - 2];
        return switch (section) {
            case MainActivity.SECTION_HEADLINES -> TYPE_HEADLINES;
            case MainActivity.SECTION_CALENDAR -> TYPE_CALENDAR;
            case MainActivity.SECTION_FUN_FACT -> TYPE_FUN_FACT;
            case MainActivity.SECTION_HEALTH -> TYPE_HEALTH;
            default -> -1; // Should not happen
        };
    }

    @Override
    public int getItemCount() {
        return 2 + sectionOrder.length; // Header + Weather + Sections
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        return switch (viewType) {
            case TYPE_HEADER -> new HeaderViewHolder(inflater.inflate(R.layout.card_day_ahead, parent, false));
            case TYPE_WEATHER -> new WeatherViewHolder(inflater.inflate(R.layout.card_weather, parent, false));
            case TYPE_HEADLINES -> new HeadlinesViewHolder(inflater.inflate(R.layout.card_headlines, parent, false));
            case TYPE_CALENDAR -> new CalendarViewHolder(inflater.inflate(R.layout.card_calendar, parent, false));
            case TYPE_FUN_FACT -> new FunFactViewHolder(inflater.inflate(R.layout.card_fun_fact, parent, false));
            case TYPE_HEALTH -> new HealthViewHolder(inflater.inflate(R.layout.card_health, parent, false));
            default -> throw new IllegalArgumentException("Invalid view type");
        };
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        switch (holder) {
            case HeaderViewHolder headerHolder -> binder.bindHeader(headerHolder);
            case WeatherViewHolder weatherHolder -> binder.bindWeather(weatherHolder);
            case HeadlinesViewHolder headlinesHolder -> binder.bindHeadlines(headlinesHolder);
            case CalendarViewHolder calendarHolder -> binder.bindCalendar(calendarHolder);
            case FunFactViewHolder funFactHolder -> binder.bindFunFact(funFactHolder);
            case HealthViewHolder healthHolder -> binder.bindHealth(healthHolder);
            default -> { /* No-op */ }
        }
    }

    // ViewHolders
    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        final TextView greeting;
        final TextView summary;
        final ImageButton playButton;

        HeaderViewHolder(View v) {
            super(v);
            greeting = v.findViewById(R.id.day_ahead_greeting);
            summary = v.findViewById(R.id.day_ahead_summary);
            playButton = v.findViewById(R.id.day_ahead_play_button);
        }
    }

    static class WeatherViewHolder extends RecyclerView.ViewHolder {
        final ProgressBar progressBar;
        final TextView errorText;
        final LinearLayout contentLayout;
        final ImageView icon;
        final TextView temp;
        final TextView conditions;
        final TextView highLow;
        final LinearLayout forecastContainer;
        final ForecastDayViewHolder[] forecastViews;
        final TextView location;
        final ImageView settingsIcon;

        static class ForecastDayViewHolder {
            final View parent;
            final TextView day;
            final ImageView icon;
            final TextView high;
            final TextView low;

            ForecastDayViewHolder(View v) {
                parent = v;
                day = v.findViewById(R.id.forecast_day);
                icon = v.findViewById(R.id.forecast_icon);
                high = v.findViewById(R.id.forecast_high);
                low = v.findViewById(R.id.forecast_low);
            }
        }

        WeatherViewHolder(View v) {
            super(v);
            progressBar = v.findViewById(R.id.weather_progress_bar);
            errorText = v.findViewById(R.id.weather_error_text);
            contentLayout = v.findViewById(R.id.weather_content_layout);
            icon = v.findViewById(R.id.weather_icon);
            temp = v.findViewById(R.id.current_temp);
            conditions = v.findViewById(R.id.current_conditions);
            highLow = v.findViewById(R.id.high_low_temp);
            forecastContainer = v.findViewById(R.id.daily_forecast_container);
            location = v.findViewById(R.id.weather_location);
            settingsIcon = v.findViewById(R.id.weather_settings_icon);
            forecastViews = new ForecastDayViewHolder[] {
                new ForecastDayViewHolder(v.findViewById(R.id.forecast_day_1)),
                new ForecastDayViewHolder(v.findViewById(R.id.forecast_day_2)),
                new ForecastDayViewHolder(v.findViewById(R.id.forecast_day_3)),
                new ForecastDayViewHolder(v.findViewById(R.id.forecast_day_4)),
                new ForecastDayViewHolder(v.findViewById(R.id.forecast_day_5))
            };
        }
    }

    static class HeadlinesViewHolder extends RecyclerView.ViewHolder {
        final ProgressBar progressBar;
        final TextView errorText;
        final LinearLayout container;
        final ChipGroup chipGroup;
        final HeadlineItemViewHolder[] headlineViews;

        static class HeadlineItemViewHolder {
            final View parent;
            final TextView title;
            final TextView source;

            HeadlineItemViewHolder(View v) {
                parent = v;
                title = v.findViewById(R.id.headline_title);
                source = v.findViewById(R.id.headline_source_time);
            }
        }

        HeadlinesViewHolder(View v) {
            super(v);
            progressBar = v.findViewById(R.id.headlines_progress_bar);
            errorText = v.findViewById(R.id.headlines_error_text);
            container = v.findViewById(R.id.headlines_container);
            chipGroup = v.findViewById(R.id.headlines_category_chips);
            headlineViews = new HeadlineItemViewHolder[] {
                new HeadlineItemViewHolder(v.findViewById(R.id.headline_1)),
                new HeadlineItemViewHolder(v.findViewById(R.id.headline_2)),
                new HeadlineItemViewHolder(v.findViewById(R.id.headline_3))
            };
        }
    }

    static class CalendarViewHolder extends RecyclerView.ViewHolder {
        final TextView permissionDeniedText;
        final TextView noEventsText;
        final TextView errorText;
        final LinearLayout eventsContainer;
        final CalendarEventItemViewHolder[] eventViews;

        static class CalendarEventItemViewHolder {
            final View parent;
            final TextView title;
            final TextView time;
            final TextView location;

            CalendarEventItemViewHolder(View v) {
                parent = v;
                title = v.findViewById(R.id.event_title);
                time = v.findViewById(R.id.event_time);
                location = v.findViewById(R.id.event_location);
            }
        }

        CalendarViewHolder(View v) {
            super(v);
            permissionDeniedText = v.findViewById(R.id.calendar_permission_denied_text);
            noEventsText = v.findViewById(R.id.calendar_no_events_text);
            errorText = v.findViewById(R.id.calendar_error_text);
            eventsContainer = v.findViewById(R.id.calendar_events_container);
            eventViews = new CalendarEventItemViewHolder[] {
                new CalendarEventItemViewHolder(v.findViewById(R.id.calendar_event_1)),
                new CalendarEventItemViewHolder(v.findViewById(R.id.calendar_event_2)),
                new CalendarEventItemViewHolder(v.findViewById(R.id.calendar_event_3))
            };
        }
    }

    static class FunFactViewHolder extends RecyclerView.ViewHolder {
        final TextView funFactText;

        FunFactViewHolder(View v) {
            super(v);
            funFactText = v.findViewById(R.id.fun_fact_text);
        }
    }

    static class HealthViewHolder extends RecyclerView.ViewHolder {
        final TextView stepsCount;
        final TextView permissionButton;
        final TextView errorText;
        final LinearLayout contentLayout;

        HealthViewHolder(View v) {
            super(v);
            stepsCount = v.findViewById(R.id.health_steps_count);
            permissionButton = v.findViewById(R.id.health_permission_button);
            errorText = v.findViewById(R.id.health_error_text);
            contentLayout = v.findViewById(R.id.health_content_layout);
        }
    }
}
