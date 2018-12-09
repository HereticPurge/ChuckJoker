package hereticpurge.chuckjoker.model;

import android.content.Context;

import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.TreeMap;

import hereticpurge.chuckjoker.R;
import hereticpurge.chuckjoker.apiservice.ApiClient;
import hereticpurge.chuckjoker.apiservice.apimodel.ApiJokeCountItem;
import hereticpurge.chuckjoker.apiservice.apimodel.ApiJokeItem;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

public class JokeController extends Observable {

    private static JokeController sJokeController;

    private int mCurrentJokeId;

    private int mTotalJokesAvailable;
    private boolean jokeCountIsSet = false;

    private JokeItem mCurrentJoke;

    private ApiClient mApiClient;

    private Map<Integer, JokeItem> jokeCache;

    private JokeController() {

        jokeCache = new TreeMap<>();
    }

    public static JokeController getJokeController() {
        if (sJokeController == null) {
            sJokeController = new JokeController();
        }
        return sJokeController;
    }

    public void setApiClient(ApiClient apiClient, Context context) {
        if (mApiClient == null) {
            mApiClient = apiClient;
            sJokeController.loadJoke(context, 1);
        }
    }

    public void setTotalJokeCount() {
        if (!jokeCountIsSet) {
            Call<ApiJokeCountItem> call = mApiClient.getJokeCount();
            call.enqueue(new Callback<ApiJokeCountItem>() {
                @Override
                public void onResponse(Call<ApiJokeCountItem> call, Response<ApiJokeCountItem> response) {
                    mTotalJokesAvailable = response.body().getValue();
                    jokeCountIsSet = true;
                }

                @Override
                public void onFailure(Call<ApiJokeCountItem> call, Throwable t) {
                    Timber.d(t);
                }
            });
        }
    }

    public JokeItem getCurrentJoke() {
        return mCurrentJoke;
    }

    public int getCurrentJokeId() {
        return mCurrentJokeId;
    }

    public void loadJoke(Context context, int id) {

        if (jokeCache.containsKey(id)) {
            setCurrentJoke(jokeCache.get(id));
            return;
        }

        Call<ApiJokeItem> call = mApiClient.getJoke(String.valueOf(id));
        call.enqueue(new Callback<ApiJokeItem>() {
            @Override
            public void onResponse(Call<ApiJokeItem> call, Response<ApiJokeItem> response) {
                setCurrentJoke(response.body().getValue());
            }

            @Override
            public void onFailure(Call<ApiJokeItem> call, Throwable t) {
                Timber.d("JOKE LOAD FAILURE");
                JokeItem errorJokeItem = new JokeItem();
                errorJokeItem.setId(id);
                errorJokeItem.setJoke(context.getResources().getString(R.string.joke_error_missing_joke));
                setCurrentJoke(errorJokeItem);
                Timber.d(t);
            }
        });
    }

    public void loadRandomJoke(Context context) {
        Call<ApiJokeItem> call = mApiClient.getRandomJoke();
        call.enqueue(new Callback<ApiJokeItem>() {
            @Override
            public void onResponse(Call<ApiJokeItem> call, Response<ApiJokeItem> response) {
                setCurrentJoke(response.body().getValue());
            }

            @Override
            public void onFailure(Call<ApiJokeItem> call, Throwable t) {
                JokeItem errorJokeItem = new JokeItem();
                errorJokeItem.setId(mCurrentJokeId);
                errorJokeItem.setJoke(context.getResources().getString(R.string.joke_error_missing_joke));
                setCurrentJoke(errorJokeItem);
                Timber.d(t);
            }
        });
    }

    private void setCurrentJoke(JokeItem jokeItem) {

        if (!jokeCache.containsKey(jokeItem.getId())) {
            jokeCache.put(jokeItem.getId(), jokeItem);
        }

        mCurrentJoke = jokeItem;
        mCurrentJokeId = mCurrentJoke.getId();
        setChanged();
        notifyObservers(mCurrentJoke);
    }

    public void nextJoke(Context context) {
        if (mCurrentJokeId < mTotalJokesAvailable) {
            loadJoke(context, ++mCurrentJokeId);
        }
    }

    public void previousJoke(Context context) {
        if (mCurrentJokeId > 1) {
            loadJoke(context, --mCurrentJokeId);
        }
    }

    @Override
    public synchronized void addObserver(Observer o) {
        super.addObserver(o);

        if (mCurrentJoke != null) {
            o.update(this, mCurrentJoke);
        }
    }
}
