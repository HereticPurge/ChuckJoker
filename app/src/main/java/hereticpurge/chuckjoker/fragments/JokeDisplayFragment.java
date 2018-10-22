package hereticpurge.chuckjoker.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.Random;

import hereticpurge.chuckjoker.R;
import hereticpurge.chuckjoker.gsonutils.GsonUtils;
import hereticpurge.chuckjoker.icndb.ApiCalls;
import hereticpurge.chuckjoker.icndb.ApiReference;
import hereticpurge.chuckjoker.model.JokeItem;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import timber.log.Timber;

public class JokeDisplayFragment extends Fragment {

    private TextView mJokeBodyTextView;
    private TextView mCurrentJokeNumText;

    private Button mRandomJokeButton;

    private int mCurrentDisplayIndex;

    private int DEFAULT_INDEX = 1;

    private String INDEX_SAVE_KEY = "indexSaveKey";

    public static JokeDisplayFragment createInstance() {
        return new JokeDisplayFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.joke_display_fragment_layout, container, false);

        this.setRetainInstance(true);

        mCurrentDisplayIndex = DEFAULT_INDEX;

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.getSupportActionBar().setDisplayShowHomeEnabled(true);
        activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mJokeBodyTextView = view.findViewById(R.id.joke_display_joke_body_text);
        mCurrentJokeNumText = view.findViewById(R.id.joke_display_joke_number_text);

        mRandomJokeButton = view.findViewById(R.id.joke_display_fragment_random_joke_button);
        mRandomJokeButton.setOnClickListener(v -> getRandomJoke());

        getRandomJoke();

        return view;
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        try {
            if (savedInstanceState != null) {
                mCurrentDisplayIndex = savedInstanceState.getInt(INDEX_SAVE_KEY);
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
            mCurrentJokeNumText.setText(mCurrentDisplayIndex);
            mJokeBodyTextView.setText(jokeBody);
            return true;
        }
        mJokeBodyTextView.setText(getActivity().getResources().getString(R.string.joke_body_error));
        return false;
    }

    private void getJoke(int jokeNum) {
        OkHttpClient client = new OkHttpClient();
        HttpUrl url = HttpUrl.get(ApiReference.SINGLE_JOKE_URL + jokeNum);

        ApiCalls.GET(client, url, new ApiCalls.ApiCallback<String>() {
            @Override
            public void response(int responseCode, @Nullable String s) {
                JokeItem jokeItem = GsonUtils.unpackJoke(s);
                Handler handler = new Handler(getActivity().getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (jokeItem != null) {
                            showJoke(jokeItem.getJokeBody());
                        } else {
                            // Given joke number on the API returned null so it probably doesn't exist.
                            // Instead we increment the current index and display the next joke in line.
                            getJoke(++mCurrentDisplayIndex);
                        }
                    }
                });
            }
        });
    }

    private void getRandomJoke() {
        OkHttpClient client = new OkHttpClient();
        HttpUrl url = HttpUrl.get(ApiReference.ALL_JOKES_COUNT_URL);

        ApiCalls.GET(client, url, new ApiCalls.ApiCallback<String>() {
            @Override
            public void response(int responseCode, @Nullable String s) {
                int maxJokeCount = GsonUtils.unpackTotalJokesCount(s);

                // Removed ThreadLocalRandom() usage because the numbers weren't coming back
                // random enough.  The same jokes were being displayed repeatedly.
                // int randomJokeNumber = ThreadLocalRandom.current().nextInt(0, maxJokeCount);

                Random r = new Random();
                int randNum = r.nextInt(maxJokeCount);

                mCurrentDisplayIndex = randNum;
                getJoke(randNum);
            }
        });
    }
}
