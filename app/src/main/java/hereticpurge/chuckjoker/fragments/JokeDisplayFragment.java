package hereticpurge.chuckjoker.fragments;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.analytics.Tracker;

import hereticpurge.chuckjoker.ChuckJokerApplication;
import hereticpurge.chuckjoker.R;
import hereticpurge.chuckjoker.fragments.fragmentutils.LoadingSpinner;
import hereticpurge.chuckjoker.icndb.ApiClient;
import hereticpurge.chuckjoker.icndb.ApiJokeCountItem;
import hereticpurge.chuckjoker.icndb.ApiJokeItem;
import hereticpurge.chuckjoker.icndb.ApiReference;
import hereticpurge.chuckjoker.model.JokeItem;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import timber.log.Timber;

public class JokeDisplayFragment extends Fragment {

    private TextView mJokeBodyTextView;
    private TextView mCurrentJokeNumText;

    private Button mRandomJokeButton;

    private int mCurrentDisplayIndex;

    private int DEFAULT_INDEX = 1;

    private int mTotalJokesAvailable = 0;

    private String INDEX_SAVE_KEY = "indexSaveKey";

    private LoadingSpinner mLoadingSpinner;

    ApiClient mApiClient;

    private Tracker mTracker;
    private static final String TAG = "JokeDisplayFragment";

    public static JokeDisplayFragment createInstance() {
        return new JokeDisplayFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.joke_display_fragment_layout, container, false);

        this.setRetainInstance(true);

        mCurrentDisplayIndex = DEFAULT_INDEX;

        mLoadingSpinner = new LoadingSpinner(this);

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.getSupportActionBar().setDisplayShowHomeEnabled(true);
        activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Retrofit retrofit = new Retrofit.Builder().baseUrl(ApiReference.ICNDB_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        mApiClient = retrofit.create(ApiClient.class);

        mJokeBodyTextView = view.findViewById(R.id.joke_display_joke_body_text);
        mCurrentJokeNumText = view.findViewById(R.id.joke_display_joke_number_text);

        initCheckPreferences();
        initGetTotalJokes();

        mRandomJokeButton = view.findViewById(R.id.joke_display_fragment_random_joke_button);
        mRandomJokeButton.setOnClickListener(v -> getRandomJoke());

        if (savedInstanceState == null) {
            getRandomJoke();
        }

        if (getActivity() != null) {
            // Google Analytics tracker.
            mTracker = ((ChuckJokerApplication) getActivity().getApplication()).getDefaultTracker();
        }

        view.setOnTouchListener(new SimpleSwipeListener());

        return view;
    }

    private void initGetTotalJokes() {
        Call<ApiJokeCountItem> call = mApiClient.getJokecount();
        call.enqueue(new Callback<ApiJokeCountItem>() {
            @Override
            public void onResponse(Call<ApiJokeCountItem> call, Response<ApiJokeCountItem> response) {
                mTotalJokesAvailable = response.body().getValue();
            }

            @Override
            public void onFailure(Call<ApiJokeCountItem> call, Throwable t) {
                Timber.d(t);
            }
        });
    }

    private void initCheckPreferences() {
        String jokeNumKey = getResources().getString(R.string.pref_show_joke_num_key);
        if (PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean(jokeNumKey, false)) {
            mCurrentJokeNumText.setVisibility(View.VISIBLE);
        }

    }

    @Override
    public void onResume() {
//        need a new tracking id for this app before enabling the actual analytics tracking.
//        mTracker.setScreenName(TAG);
//        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
        super.onResume();
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        try {
            if (savedInstanceState != null) {
                mCurrentDisplayIndex = savedInstanceState.getInt(INDEX_SAVE_KEY);
                getJoke(mCurrentDisplayIndex);
            }
        } catch (NullPointerException e) {
            // Error loading the save state so we log and do nothing while letting the default state load
            Timber.d("NullPointerException while loading saved instance state");
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(INDEX_SAVE_KEY, mCurrentDisplayIndex);
    }

    private boolean showJoke(String jokeBody) {
        // Boolean return values here just in case they are needed later.
        if (jokeBody != null && !jokeBody.equals("")) {
            mCurrentJokeNumText.setText(String.valueOf(mCurrentDisplayIndex));
            mJokeBodyTextView.setText(jokeBody);
            mLoadingSpinner.hideLoadingSpinner();
            return true;
        }
        mJokeBodyTextView.setText(getActivity().getResources().getString(R.string.joke_body_error));
        mLoadingSpinner.hideLoadingSpinner();
        return false;
    }

    private void getJoke(int jokeNum) {
        mLoadingSpinner.showLoadingSpinner();
        Call<ApiJokeItem> call = mApiClient.getJoke(String.valueOf(jokeNum));
        call.enqueue(new Callback<ApiJokeItem>() {
            @Override
            public void onResponse(Call<ApiJokeItem> call, Response<ApiJokeItem> response) {
                JokeItem jokeItem = response.body().getValue();

                if (jokeItem != null) {
                    mCurrentDisplayIndex = jokeItem.getId();
                    showJoke(jokeItem.getJoke());
                }
            }

            @Override
            public void onFailure(Call<ApiJokeItem> call, Throwable t) {
                Timber.d(t);
            }
        });
    }

    private void getRandomJoke() {
        Call<ApiJokeItem> call = mApiClient.getRandomJoke();
        call.enqueue(new Callback<ApiJokeItem>() {
            @Override
            public void onResponse(Call<ApiJokeItem> call, Response<ApiJokeItem> response) {
                JokeItem jokeItem = response.body().getValue();

                if (jokeItem != null) {
                    mCurrentDisplayIndex = jokeItem.getId();
                    showJoke(jokeItem.getJoke());
                }
            }

            @Override
            public void onFailure(Call<ApiJokeItem> call, Throwable t) {
                Timber.d(t);
            }
        });
    }

    class SimpleSwipeListener implements View.OnTouchListener {

        private float xStart = 0;
        private float xStop = 0;
        private float yStart = 0;
        private float yStop = 0;

        // the min distance a user must swipe across the screen for this listener to register it as
        // a swipe.
        final float SWIPE_X_MIN_DISTANCE = 200;

        // the max y distance (up / down) a user is allowed to press before the event won't be
        // recognized.
        final float SWIPE_Y_MAX_DISTANCE = 500;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            Timber.d("Caught motion event.");
            // send the click to the view so that other methods (accessibility stuff) can see
            // that an event occured and where.
            // v.performClick();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // store the starting position
                    xStart = event.getX();
                    yStart = event.getY();
                    Timber.d("X POSITION: " + Float.toString(xStart));
                    Timber.d("Y POSITION: " + Float.toString(yStart));
                    break;

                case MotionEvent.ACTION_UP:
                    // store the stop position
                    xStop = event.getX();
                    yStop = event.getY();

                    // getting the absolute values to determine swipe distance.
                    float xTrans = Math.abs(xStop - xStart);
                    float yTrans = Math.abs(yStop - yStart);

                    // making sure the motion was intentional by ensuring motion distances.
                    if (xTrans > SWIPE_X_MIN_DISTANCE && yTrans < SWIPE_Y_MAX_DISTANCE) {
                        if (xStart > xStop) {
                            Timber.d("Swipe from right to left");
                            if (JokeDisplayFragment.this.mCurrentDisplayIndex <
                                    JokeDisplayFragment.this.mTotalJokesAvailable) {

                                getJoke(++mCurrentDisplayIndex);
                            }
                        } else {
                            Timber.d("Swipe from left to right");
                            getJoke(--mCurrentDisplayIndex);
                        }
                    }
                    Timber.d("----------  MOTION EVENT BREAK  ----------");
                    break;
            }
            // tell the system that the event was handled.
            return true;
        }
    }
}
