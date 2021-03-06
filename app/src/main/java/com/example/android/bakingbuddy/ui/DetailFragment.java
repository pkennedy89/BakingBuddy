package com.example.android.bakingbuddy.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.android.bakingbuddy.R;
import com.example.android.bakingbuddy.model.Recipe;
import com.example.android.bakingbuddy.model.Step;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import java.util.ArrayList;
import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by pkennedy on 3/24/18.
 */

public class DetailFragment extends Fragment implements View.OnClickListener{

    // Context
    private Context mContext;

    // Current Step
    private Step mStep;

    // Current Step arraylist position
    private int mPosition;

    // Recipe
    private Recipe mRecipe;

    // Steps in the Recipe
    private ArrayList<Step> mSteps;

    // SimpleExoPlayerView
    @BindView(R.id.player_view) SimpleExoPlayerView mPlayerView;

    // Recipe Imageview
    @BindView(R.id.iv_stepThumbnail) ImageView mStepThumbnailImageView;

    // SimpleExoPlayer
    private SimpleExoPlayer mExoPlayer;

    // Previous Button
    @Nullable
    @BindView(R.id.btn_previous) Button mPreviousButton;

    // Next Button
    @Nullable
    @BindView(R.id.btn_next) Button mNextButton;

    // Short Description
    @Nullable
    @BindView(R.id.tv_detail_short_description) TextView shortDescription;

    // Description
    @Nullable
    @BindView(R.id.tv_detail_description) TextView description;

    // Video URL
    private String mVideoUrl;

    // Current position (for OnResume)
    private long mCurrentPosition;

    // Two Pane Mode Boolean
    private boolean mTwoPane = false;

    // Dialog variable for fullscreen mode
    private Dialog mFullScreenDialog;

    // Boolean variable for fullscreen mode
    private Boolean mPlayerViewFullscreen = false;

    // Hashmap for Recipe placeholder images
    HashMap<String, Integer> placeholderMap = new HashMap<>();

    // Mandatory constructor for inflating the fragment
    public DetailFragment(){
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // Create a hashmap for the placeholder images based on recipe names
        initPlaceHolderMap();

        if (mFullScreenDialog !=null){
            mFullScreenDialog.dismiss();
        }

        // Check to see if the fragment was started in Two Pane Mode
        if(getArguments()!=null){
            mTwoPane = getArguments().getBoolean("mTwoPane");
        }

        // Inflate the fragment_detail layout
        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);

        // Bind the data
        ButterKnife.bind(this, rootView);

        // Adding this allows the Fragment to call onOptionsItemSelected correctly
        setHasOptionsMenu(true);

        // Context
        mContext = getContext();

        if(mTwoPane){

            // Get recipe and position from arguments bundle
            mRecipe = (Recipe) getArguments().getSerializable("mRecipe");
            mPosition = getArguments().getInt("mPosition");

            // Get the steps from the recipe
            mSteps = mRecipe.getSteps();

        } else {
            // Get the data from the intent that started this fragment
            Intent intent = getActivity().getIntent();
            mRecipe = (Recipe) intent.getSerializableExtra("recipe");

            // Get the steps from the recipe
            mSteps = mRecipe.getSteps();

            if(getArguments() == null){
                mPosition = (Integer) intent.getIntExtra("position",0);
            } else {
                mPosition = getArguments().getInt("position");
            }

            // Set on click listener on buttons
            mPreviousButton.setOnClickListener(this);
            mNextButton.setOnClickListener(this);

            // Display buttons
            displayButtons(mPosition);
        }

        // Get the step from position in the array
        mStep = mSteps.get(mPosition);

        // Get the video url from the step
        mVideoUrl = mStep.getVideoURL();

        //Get the thumbnail url
        String mThumbnailUrl = mStep.getThumbnailURL();

        /*
        Display mVideoUrl in SimpleExoPlayerView, display mThumbnailUrl in ImageView
         */
        if(!mVideoUrl.isEmpty()){
            // If the videoUrl is not empty, load the video
            initializePlayer(mVideoUrl);
            mPlayerView.setVisibility(View.VISIBLE);
        } else {

            // If there is no data, display the placeholder image for the recipe
            int placeholder = placeholderMap.get(mRecipe.getName());

            // Load the thumbnailUrl into an imageview
            mStepThumbnailImageView.setVisibility(View.VISIBLE);
            Glide.with(mContext)
                    .load(mThumbnailUrl)
                    .placeholder(placeholder)
                    .into(mStepThumbnailImageView);
        }

        // Check if coming from saved instance state, and track to that position
        if(savedInstanceState!=null && mExoPlayer!=null){
            long position = savedInstanceState.getLong("POS_KEY");
            float volume = savedInstanceState.getFloat("VOLUME");
            boolean playState = savedInstanceState.getBoolean("PLAY_STATE");

            mExoPlayer.seekTo(position);
            mExoPlayer.setPlayWhenReady(playState);
        }


        // Bind the description data to the respective textviews
        description.setText(mStep.getDescription());
        shortDescription.setText(mStep.getShortDescription());

        // Get orientation
        int ORIENTATION = getActivity().getResources().getConfiguration().orientation;

        if(!mTwoPane && Configuration.ORIENTATION_LANDSCAPE == ORIENTATION && mExoPlayer!=null){
            initFullscreenDialog();
            openFullscreenDialog();
        }


        return rootView;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_previous:
                mPosition--;
                break;
            case R.id.btn_next:
                mPosition++;
                break;
        }

        // If ExoPlayer is initialized, release
        if(mExoPlayer != null){
            releasePlayer();
        }

        // Create new intent
        Class destinationClass = DetailActivity.class;
        Intent intent = new Intent(mContext, destinationClass);

        Bundle bundle = new Bundle();
        bundle.putSerializable("recipe", mRecipe);
        bundle.putInt("position", mPosition);

        intent.putExtras(bundle);
        intent.setFlags(intent.getFlags() | Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivityForResult(intent, 1);
    }

    private void initializePlayer(String mediaUrl) {
        if(mExoPlayer == null){
            // Create an instance of the ExoPlayer
            TrackSelector trackSelector = new DefaultTrackSelector();
            LoadControl loadControl = new DefaultLoadControl();
            mExoPlayer = ExoPlayerFactory.newSimpleInstance(mContext, trackSelector, loadControl);
            mPlayerView.setPlayer(mExoPlayer);

            // Prepare the media source
            String userAgent = Util.getUserAgent(mContext, "BakingBuddy");
            MediaSource mediaSource = new ExtractorMediaSource(Uri.parse(mediaUrl),
                    new DefaultDataSourceFactory(mContext, userAgent),
                    new DefaultExtractorsFactory(),
                    null,
                    null);
            mExoPlayer.prepare(mediaSource);
            mExoPlayer.setPlayWhenReady(true);
        }
    }

    private void releasePlayer(){
        if(mExoPlayer != null){
            mExoPlayer.stop();
            mExoPlayer.release();
            mExoPlayer = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        releasePlayer();
    }

    @Override
    public void onStop() {
        super.onStop();
        releasePlayer();
    }

    private void returnRecipe(){
        Intent returnIntent = new Intent();
        returnIntent.putExtra("recipe", mRecipe);
        getActivity().setResult(Activity.RESULT_OK, returnIntent);
        getActivity().finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                returnRecipe();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Set the title bar in DetailActivity for phone mode
        if(!mTwoPane){
            ((DetailActivity) getActivity()).setActionBarTitle(mRecipe.getName());
        }

        // Initialize the player
        if(!mVideoUrl.isEmpty()){
            initializePlayer(mVideoUrl);
            mPlayerView.setVisibility(View.VISIBLE);
        }

        if(mCurrentPosition != 0){
            mExoPlayer.seekTo(mCurrentPosition);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if(mExoPlayer != null) {
            mCurrentPosition = mExoPlayer.getCurrentPosition();
        }
    }

    private void displayButtons(int position){

        // Make buttons visible
        mPreviousButton.setVisibility(View.VISIBLE);
        mNextButton.setVisibility(View.VISIBLE);

        if(position == 0){
            mPreviousButton.setVisibility(View.INVISIBLE);
            mNextButton.setVisibility(View.VISIBLE);
        } else if(position == mSteps.size() - 1){
            mPreviousButton.setVisibility(View.VISIBLE);
            mNextButton.setVisibility(View.INVISIBLE);
        } else {
            mPreviousButton.setVisibility(View.VISIBLE);
            mNextButton.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if(mExoPlayer != null){
            long position = mExoPlayer.getCurrentPosition();
            outState.putLong("POS_KEY", position);

            float volume = mExoPlayer.getVolume();
            outState.putFloat("VOLUME", volume);

            boolean playState = mExoPlayer.getPlayWhenReady();
            outState.putBoolean("PLAY_STATE", playState);

            super.onSaveInstanceState(outState);

        }

    }

    private void initFullscreenDialog(){
        mFullScreenDialog = new Dialog(mContext, android.R.style.Theme_NoTitleBar_Fullscreen) {
            public void onBackPressed() {
                if (mPlayerViewFullscreen){
                    closeFullscreenDialog();
                }
                super.onBackPressed();
            }
        };
    }

    private void openFullscreenDialog(){
        ((ViewGroup) mPlayerView.getParent()).removeView(mPlayerView);
        mFullScreenDialog.addContentView(mPlayerView,
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mPlayerViewFullscreen = true;
        mFullScreenDialog.show();
    }

    private void closeFullscreenDialog(){
        ((ViewGroup) mPlayerView.getParent()).removeView(mPlayerView);
        ((FrameLayout) getActivity().findViewById(R.id.fl_player_view)).addView(mPlayerView);
        mPlayerViewFullscreen = false;
        mFullScreenDialog.dismiss();
    }

    private void initPlaceHolderMap(){
        placeholderMap.put("Nutella Pie", R.drawable.nutella_pie);
        placeholderMap.put("Brownies", R.drawable.brownies);
        placeholderMap.put("Yellow Cake", R.drawable.yellow_cake);
        placeholderMap.put("Cheesecake", R.drawable.cheesecake);
    }
}
