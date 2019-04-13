package quizapp.com.movieappstage1;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.github.ivbaranov.mfb.MaterialFavoriteButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import quizapp.com.movieappstage1.adapter.TrailerAdapter;
import quizapp.com.movieappstage1.api.Client;
import quizapp.com.movieappstage1.api.Service;
import quizapp.com.movieappstage1.data.FavoriteDbHelper;
import quizapp.com.movieappstage1.model.Movie;
import quizapp.com.movieappstage1.model.Trailer;
import quizapp.com.movieappstage1.model.TrailerResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DetailActivity extends AppCompatActivity {
    TextView nameOfMovie, plotSynopsis, userRating, releaseDate;
    ImageView imageView;
    private RecyclerView recyclerView;
    private TrailerAdapter adapter;
    private List<Trailer> trailerList;
    private FavoriteDbHelper favoriteDbHelper;
    private Movie favorite;
    private final AppCompatActivity activity = DetailActivity.this;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        Toolbar toolbar = findViewById(R.id.toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        initCollapsingToolBar();

        imageView = findViewById(R.id.thumbnail_image_holder);
        nameOfMovie = findViewById(R.id.title);
        plotSynopsis = findViewById(R.id.plotsynopsis);
        userRating = findViewById(R.id.userrating);
        releaseDate = findViewById(R.id.releasedate);


        Intent intentThatStartedThisActivity = getIntent();
        if (intentThatStartedThisActivity.hasExtra("original_title")) {

            String thumbnail = Objects.requireNonNull(getIntent().getExtras()).getString("poster_path");
            String movieName = getIntent().getExtras().getString("original_title");
            String synopsis = getIntent().getExtras().getString("overview");
            String rating = getIntent().getExtras().getString("vote_average");
            String dateofRelease = getIntent().getExtras().getString("release_date");


            String poster = " http://image.tmdb.org/t/p/w500" + thumbnail;

            Glide.with(this)
                    .load(thumbnail)
                    .into(imageView);


            nameOfMovie.setText(movieName);
            plotSynopsis.setText(synopsis);
            userRating.setText(rating);
            releaseDate.setText(dateofRelease);
        } else {
            Toast.makeText(this, "No API Data", Toast.LENGTH_SHORT).show();
        }

        MaterialFavoriteButton materialFavoriteButton =
                findViewById(R.id.favorite_button);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        materialFavoriteButton.setOnFavoriteChangeListener(
                new MaterialFavoriteButton.OnFavoriteChangeListener() {
                    @Override
                    public void onFavoriteChanged(MaterialFavoriteButton buttonView, boolean favorite) {

                        if (favorite) {
                            SharedPreferences.Editor editor = getSharedPreferences("com.quizapp.movieappstage1.DetailActivity", MODE_PRIVATE).edit();

                            editor.putBoolean("Favorite Added", true);
                            editor.commit();
                            saveFavorite();
                            Snackbar.make(buttonView, "Added to Favorite", Snackbar.LENGTH_SHORT).show();
                        }else{
                            int moive_id = getIntent().getExtras().getInt("id");
                            favoriteDbHelper = new FavoriteDbHelper(DetailActivity.this);
                            favoriteDbHelper.deleteFavorite(moive_id);

                            SharedPreferences.Editor editor = getSharedPreferences("com.quizapp.movieappstage1.DetailActivity", MODE_PRIVATE).edit();
                            editor.putBoolean("Favorite Removed", true);
                            editor.commit();
                            Snackbar.make(buttonView, "Removed from Favorite", Snackbar.LENGTH_SHORT).show();

                        }

                    }
                }
        );

        initViews();

    }


        private void initCollapsingToolBar() {
    final CollapsingToolbarLayout collapsingToolbarLayout =
            findViewById(R.id.collapsing_toolbar);
        collapsingToolbarLayout.setTitle(" ");
        AppBarLayout appBarLayout = findViewById(R.id.appbar);
            appBarLayout.setExpanded(true);

                appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
                    boolean isShow = false;
                    int scrollRange = -1;

                    @Override
                    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                        if (scrollRange == -1) {
                            scrollRange = appBarLayout.getTotalScrollRange();

                        }

                        if (scrollRange + verticalOffset == 0) {
                            collapsingToolbarLayout.setTitle(getString(R.string.movie_details));
                            isShow = true;
                        } else if (isShow) {
                            collapsingToolbarLayout.setTitle(" ");
                            isShow = false;
                        }


                    }
                });


}

    private void initViews() {
        trailerList = new ArrayList<>();
        adapter = new TrailerAdapter(this, trailerList);

        recyclerView = findViewById(R.id.recycler_view1);
        RecyclerView.LayoutManager sLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setAdapter(adapter);
        adapter.notifyDataSetChanged();

        loadJSON();

    }

    private void loadJSON() {
        int movie_id = getIntent().getExtras().getInt("id");

        try {
            Client Client = new Client();
            Service apiService;
            apiService = quizapp.com.movieappstage1.api.Client.getClient().create(Service.class);
            Call<TrailerResponse> call = apiService.getMovieTrailer(movie_id, BuildConfig.THE_MOVIE_DB_API_TOKEN);
            call.enqueue(new Callback<TrailerResponse>() {
                @Override
                public void onResponse(Call<TrailerResponse> call, Response<TrailerResponse> response) {

                    List<Trailer> trailer = response.body().getResults();
                    recyclerView.setAdapter(new TrailerAdapter(getApplicationContext(), trailer));
                    recyclerView.smoothScrollToPosition(0);
                }

                @Override
                public void onFailure(Call<TrailerResponse> call, Throwable t) {

                    Log.d("Error", t.getMessage());
                    Toast.makeText(DetailActivity.this, "Error fetching trailer data", Toast.LENGTH_SHORT).show();


                }
            });
        }
        catch (Exception e) {
            Log.d("Error", e.getMessage());
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    public void saveFavorite() {
        favoriteDbHelper = new FavoriteDbHelper(activity);
        favorite = new Movie();
        int movie_id = getIntent().getExtras().getInt("id");
        String rate = getIntent().getExtras().getString("vote_average");
        String poster = getIntent().getExtras().getString("poster_path");

        favorite.setId(movie_id);
        favorite.setOriginalTitle(nameOfMovie.getText().toString().trim());
        favorite.setPosterPath(poster);
        favorite.setVoteAverage((int) Double.parseDouble(rate));
        favorite.setOverview(plotSynopsis.getText().toString().trim());

        favoriteDbHelper.addFavorite(favorite);

    }

}
